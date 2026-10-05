package com.viciousliqx.viciousbars;

import com.google.inject.Provides;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.BeforeRender;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetPositionMode;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.itemstats.ItemStatPlugin;
import net.runelite.client.ui.overlay.OverlayManager;

@Slf4j
@PluginDescriptor(
	name = "Vicious Bars"
)
// Same dependency core Status Bars itself declares for its own heal-restore
// preview - ItemStatChangesService (the thing that actually computes "this
// food heals X Hitpoints") is only bound/available via Guice while this
// plugin is enabled, so the HP bar's preview simply has nothing to show if
// the user doesn't have Item Stats turned on.
@PluginDependency(ItemStatPlugin.class)
public class ViciousBarsPlugin extends Plugin
{
	// Experimental - how far past the farthest bar target to extend each
	// ancestor, see onBeforeRender()'s javadoc.
	private static final int CONTAINER_MARGIN = 20;

	@Inject
	private Client client;

	@Inject
	private ViciousBarsConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private EventBus eventBus;

	@Inject
	private OpponentTracker opponentTracker;

	@Inject
	private HpBarOverlay hpBarOverlay;

	@Inject
	private PrayerBarOverlay prayerBarOverlay;

	@Inject
	private SpecBarOverlay specBarOverlay;

	@Inject
	private RunBarOverlay runBarOverlay;

	@Inject
	private WarmthBarOverlay warmthBarOverlay;

	@Inject
	private EnemyHpBarOverlay enemyHpBarOverlay;

	// TEMPORARY - diagnostic only, see DebugWidgetBoundsOverlay's javadoc.
	@Inject
	private DebugWidgetBoundsOverlay debugWidgetBoundsOverlay;

	// TEMPORARY - diagnostic only, see MinimapDragDiagnostic's javadoc.
	@Inject
	private MinimapDragDiagnostic minimapDragDiagnostic;

	// Experimental - see onBeforeRender()'s javadoc. Populated lazily, the
	// first time the chain actually gets grown; one entry per ancestor
	// widget touched, holding what to restore it to on the way out.
	private final List<GrownWidget> grownWidgets = new ArrayList<>();

	private static final class GrownWidget
	{
		final Widget widget;
		final int originalX;
		final int originalY;
		final int originalWidth;
		final int originalHeight;

		GrownWidget(Widget widget)
		{
			this.widget = widget;
			this.originalX = widget.getOriginalX();
			this.originalY = widget.getOriginalY();
			this.originalWidth = widget.getOriginalWidth();
			this.originalHeight = widget.getOriginalHeight();
		}
	}

	@Override
	protected void startUp() throws Exception
	{
		eventBus.register(opponentTracker);
		eventBus.register(prayerBarOverlay);
		eventBus.register(this);
		eventBus.register(minimapDragDiagnostic);

		overlayManager.add(hpBarOverlay);
		overlayManager.add(prayerBarOverlay);
		overlayManager.add(specBarOverlay);
		overlayManager.add(runBarOverlay);
		overlayManager.add(warmthBarOverlay);
		overlayManager.add(enemyHpBarOverlay);
		overlayManager.add(debugWidgetBoundsOverlay);

		log.debug("Vicious Bars started!");
	}

	@Override
	protected void shutDown() throws Exception
	{
		overlayManager.remove(hpBarOverlay);
		overlayManager.remove(prayerBarOverlay);
		overlayManager.remove(specBarOverlay);
		overlayManager.remove(runBarOverlay);
		overlayManager.remove(warmthBarOverlay);
		overlayManager.remove(enemyHpBarOverlay);
		overlayManager.remove(debugWidgetBoundsOverlay);

		eventBus.unregister(prayerBarOverlay);
		eventBus.unregister(opponentTracker);
		eventBus.unregister(this);
		eventBus.unregister(minimapDragDiagnostic);
		opponentTracker.reset();

		// Every orb bar pulls its real orb onto itself while running - make
		// sure none of them are left displaced behind us if the plugin stops.
		hpBarOverlay.restoreRealOrbPosition();
		prayerBarOverlay.restoreRealOrbPosition();
		specBarOverlay.restoreRealOrbPosition();
		runBarOverlay.restoreRealOrbPosition();

		restoreGrownWidgets();

		log.debug("Vicious Bars stopped!");
	}

	/**
	 * Experimental - grows every real ancestor from an orb's actual parent
	 * (confirmed via the Widget Inspector to be Orbs.UNIVERSE, NOT the outer
	 * ToplevelPreEoc.ORBS slot we were resizing before - growing the wrong
	 * widget meant the real clamp in {@link VitalBarOverlay#alignRealOrb}
	 * never actually got more room) up to MAP_CONTAINER, so nothing in the
	 * chain clips the others. Walked dynamically via repeated
	 * {@code getParent()} rather than hardcoded IDs, since we were wrong
	 * about which widget matters once already.
	 *
	 * Also fixes a real anchoring bug: MAP_CONTAINER's X position mode is
	 * ABSOLUTE_RIGHT (confirmed via direct query), meaning its real canvas
	 * left edge is {@code parentRight - originalX - width} - growing width
	 * alone therefore extends the box LEFTWARD (right edge fixed), the
	 * opposite of what naive left-anchored math assumes. Every widget in the
	 * chain is now grown via {@link #growToCoverTargets}, which checks each
	 * one's own X/Y position mode and adjusts originalX/Y together with
	 * width/height so it can extend in whichever direction is actually
	 * needed, regardless of which edge it's really anchored to.
	 *
	 * All of this is computed fresh from each widget's live
	 * {@code getCanvasLocation()} every single frame - never a cached
	 * baseline - so manually dragging the minimap mid-session recalibrates
	 * on its own next frame.
	 */
	@Subscribe
	public void onBeforeRender(BeforeRender event)
	{
		Point[] barTargets = {
			hpBarOverlay.getLastIconCanvasCenter(),
			prayerBarOverlay.getLastIconCanvasCenter(),
			specBarOverlay.getLastIconCanvasCenter(),
			runBarOverlay.getLastIconCanvasCenter(),
		};

		boolean anyBarTarget = false;
		for (Point p : barTargets)
		{
			if (p != null)
			{
				anyBarTarget = true;
				break;
			}
		}

		Widget anyOrb = client.getWidget(InterfaceID.Orbs.ORB_HEALTH);
		Widget orbParent = anyOrb != null ? anyOrb.getParent() : null;

		if (!anyBarTarget || orbParent == null)
		{
			restoreGrownWidgets();
			return;
		}

		// Walk up from the orb's real parent to MAP_CONTAINER, growing each
		// ancestor in turn. Capped at a handful of levels as a sanity bound
		// in case MAP_CONTAINER isn't actually in this chain for some reason.
		Widget current = orbParent;
		for (int depth = 0; current != null && depth < 6; depth++)
		{
			growToCoverTargets(current, barTargets);
			if (current.getId() == InterfaceID.ToplevelPreEoc.MAP_CONTAINER)
			{
				break;
			}
			current = current.getParent();
		}
	}

	/**
	 * Grows {@code widget}'s own local X/Y/width/height so its real canvas
	 * rect covers every non-null target point (with a margin), respecting
	 * whichever edge its own position mode actually anchors to - see this
	 * method's use in {@link #onBeforeRender} for why that matters. Saves
	 * the widget's original bounds the first time it's touched, via
	 * {@link #grownWidgets}, so {@link #restoreGrownWidgets} can undo it.
	 */
	private void growToCoverTargets(Widget widget, Point[] targets)
	{
		GrownWidget saved = null;
		for (GrownWidget g : grownWidgets)
		{
			if (g.widget == widget)
			{
				saved = g;
				break;
			}
		}
		if (saved == null)
		{
			saved = new GrownWidget(widget);
			grownWidgets.add(saved);
		}

		net.runelite.api.Point canvas = widget.getCanvasLocation();
		int currentLeft = canvas.getX();
		int currentTop = canvas.getY();
		int currentRight = currentLeft + widget.getWidth();
		int currentBottom = currentTop + widget.getHeight();

		int desiredLeft = currentLeft;
		int desiredTop = currentTop;
		int desiredRight = currentRight;
		int desiredBottom = currentBottom;

		for (Point p : targets)
		{
			if (p == null)
			{
				continue;
			}
			desiredLeft = Math.min(desiredLeft, p.x - CONTAINER_MARGIN);
			desiredTop = Math.min(desiredTop, p.y - CONTAINER_MARGIN);
			desiredRight = Math.max(desiredRight, p.x + CONTAINER_MARGIN);
			desiredBottom = Math.max(desiredBottom, p.y + CONTAINER_MARGIN);
		}

		boolean changed = false;

		int newWidth = Math.max(saved.originalWidth, desiredRight - desiredLeft);
		if (widget.getXPositionMode() == WidgetPositionMode.ABSOLUTE_RIGHT)
		{
			int rightShift = desiredRight - currentRight;
			if (rightShift != 0)
			{
				widget.setOriginalX(widget.getOriginalX() - rightShift);
				changed = true;
			}
		}
		else if (desiredLeft != currentLeft)
		{
			widget.setOriginalX(widget.getOriginalX() + (desiredLeft - currentLeft));
			changed = true;
		}
		if (widget.getOriginalWidth() != newWidth)
		{
			widget.setOriginalWidth(newWidth);
			changed = true;
		}

		int newHeight = Math.max(saved.originalHeight, desiredBottom - desiredTop);
		if (widget.getYPositionMode() == WidgetPositionMode.ABSOLUTE_BOTTOM)
		{
			int bottomShift = desiredBottom - currentBottom;
			if (bottomShift != 0)
			{
				widget.setOriginalY(widget.getOriginalY() - bottomShift);
				changed = true;
			}
		}
		else if (desiredTop != currentTop)
		{
			widget.setOriginalY(widget.getOriginalY() + (desiredTop - currentTop));
			changed = true;
		}
		if (widget.getOriginalHeight() != newHeight)
		{
			widget.setOriginalHeight(newHeight);
			changed = true;
		}

		if (changed)
		{
			widget.revalidate();
		}
	}

	private void restoreGrownWidgets()
	{
		for (GrownWidget g : grownWidgets)
		{
			g.widget.setOriginalX(g.originalX);
			g.widget.setOriginalY(g.originalY);
			g.widget.setOriginalWidth(g.originalWidth);
			g.widget.setOriginalHeight(g.originalHeight);
			g.widget.revalidate();
		}
		grownWidgets.clear();
	}

	@Provides
	ViciousBarsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ViciousBarsConfig.class);
	}
}
