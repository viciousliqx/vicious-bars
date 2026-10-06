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

		// TEMPORARY - undo the debug minimap-graphic hide, in case the
		// plugin stops while that toggle is on.
		Widget mapMinimap = client.getWidget(InterfaceID.ToplevelPreEoc.MAP_MINIMAP);
		if (mapMinimap != null)
		{
			mapMinimap.setHidden(false);
		}

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
	 * {@link #growToCoverTargets} only ever writes width/height, never X/Y -
	 * confirmed position writes on this chain are inert, so an earlier
	 * attempt to compensate MAP_CONTAINER's real ABSOLUTE_RIGHT X-anchoring
	 * by also writing originalX did nothing, leaving width to grow
	 * unbounded with no stable reference. It's anchor-aware instead: whichever
	 * edge a pure size change actually leaves fixed (confirmed different per
	 * widget) is what growth is measured from, recomputed fresh from live
	 * canvas location every frame - never last frame's already-grown size -
	 * so it can shrink back down again, not just ratchet upward, and so
	 * manually dragging the minimap mid-session recalibrates on its own
	 * next frame.
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

		// Every level in the real chain clips its descendants to its own
		// bounds, so all three - MAP_CONTAINER, ORBS (sits directly inside
		// it), and the orb's real parent id=10485760 (sits directly inside
		// ORBS) - need to grow. Order matters: MAP_CONTAINER is x=RIGHT, so
		// growing IT shifts its OWN canvasLeft, which cascades down and
		// shifts where ORBS and orbParent sit too. logChainSizes caught this
		// directly - growing inner-to-outer (as this used to) meant each
		// inner level's math ran against last frame's STALE inherited
		// position, so by the time MAP_CONTAINER grew and shifted everything
		// under it, the inner levels' right edges had already receded from
		// their targets again next frame, demanding more growth, which
		// demanded more from MAP_CONTAINER - a genuine feedback spiral
		// (confirmed in the log: MAP_CONTAINER's width climbing by a fixed
		// ~31px every single frame before suddenly collapsing and
		// restarting). Growing outer-to-inner instead means each inner
		// level's growToCoverTargets call reads its parent's
		// ALREADY-updated position from earlier in this same frame -
		// revalidate() is synchronous, so this converges within one frame
		// instead of lagging and spiraling across frames.
		Widget mapContainer = client.getWidget(InterfaceID.ToplevelPreEoc.MAP_CONTAINER);
		boolean changed = false;
		if (mapContainer != null)
		{
			changed |= growToCoverTargets(mapContainer, barTargets);
		}
		Widget orbsSlot = client.getWidget(InterfaceID.ToplevelPreEoc.ORBS);
		if (orbsSlot != null)
		{
			changed |= growToCoverTargets(orbsSlot, barTargets);
		}
		changed |= growToCoverTargets(orbParent, barTargets);

		// TEMPORARY - debug-only. Dumps the full ancestor chain's live sizes
		// in one line, but ONLY on a frame where something actually got
		// resized - logging every frame regardless would be thousands of
		// identical lines a minute while nothing's happening. A spasm shows
		// up as a burst of these lines in a tight run of frames; whichever
		// widget's size is flipping back and forth between two values across
		// that burst is the one still missing from growToCoverTargets.
		if (changed)
		{
			logChainSizes(anyOrb);
		}

		// TEMPORARY - debug-only, see ViciousBarsConfig#debugHideMinimapGraphic.
		Widget mapMinimap = client.getWidget(InterfaceID.ToplevelPreEoc.MAP_MINIMAP);
		if (mapMinimap != null && mapMinimap.isHidden() != config.debugHideMinimapGraphic())
		{
			mapMinimap.setHidden(config.debugHideMinimapGraphic());
		}
	}

	/**
	 * Grows {@code widget}'s own local width/height (NEVER X/Y - confirmed
	 * via direct testing that position writes on this chain are inert, same
	 * as every earlier attempt to move MAP_CONTAINER; last round's attempt
	 * to compensate the right-anchor by writing originalX did nothing
	 * useful and just let width grow unbounded) so its real canvas rect
	 * covers every non-null target point (with a margin).
	 * <p>
	 * Critically, this is computed from whichever edge is actually STABLE
	 * under a pure size change, per the widget's own real position mode -
	 * ABSOLUTE_LEFT/TOP keeps the left/top edge fixed as width/height grow
	 * (normal case), but ABSOLUTE_RIGHT/BOTTOM keeps the opposite edge fixed
	 * instead (confirmed on MAP_CONTAINER: {@code canvasLeft = canvasRight -
	 * width}, so canvasRight, not canvasLeft, is what stays put). Using the
	 * stable edge as the one fixed reference point - recomputed fresh every
	 * frame from live canvas location, never from last frame's already-grown
	 * size - is what lets this shrink back down again once a target moves
	 * closer, instead of only ever ratcheting upward.
	 */
	private boolean growToCoverTargets(Widget widget, Point[] targets)
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

		// Never ask for more than the real client viewport can show -
		// reported "spasm near the screen edge" is consistent with the
		// engine itself clamping a widget's actual rendered bounds to the
		// visible canvas, which would make our own write and the next
		// frame's readback disagree, oscillating. Clamping our own target
		// to a bit inside the real canvas bounds avoids ever asking for
		// something the engine would silently clamp anyway.
		int canvasWidth = client.getCanvasWidth();
		int canvasHeight = client.getCanvasHeight();
		int edgeMargin = 10;

		boolean changed = false;

		if (widget.getXPositionMode() == WidgetPositionMode.ABSOLUTE_RIGHT)
		{
			int stableRight = currentLeft + widget.getWidth();
			int desiredLeft = stableRight - saved.originalWidth;
			for (Point p : targets)
			{
				if (p != null)
				{
					desiredLeft = Math.min(desiredLeft, p.x - CONTAINER_MARGIN);
				}
			}
			desiredLeft = Math.max(desiredLeft, edgeMargin);
			int newWidth = stableRight - desiredLeft;
			if (widget.getOriginalWidth() != newWidth)
			{
				widget.setOriginalWidth(newWidth);
				changed = true;
			}
		}
		else
		{
			int desiredRight = currentLeft + saved.originalWidth;
			for (Point p : targets)
			{
				if (p != null)
				{
					desiredRight = Math.max(desiredRight, p.x + CONTAINER_MARGIN);
				}
			}
			desiredRight = Math.min(desiredRight, canvasWidth - edgeMargin);
			int newWidth = desiredRight - currentLeft;
			if (widget.getOriginalWidth() != newWidth)
			{
				widget.setOriginalWidth(newWidth);
				changed = true;
			}
		}

		if (widget.getYPositionMode() == WidgetPositionMode.ABSOLUTE_BOTTOM)
		{
			int stableBottom = currentTop + widget.getHeight();
			int desiredTop = stableBottom - saved.originalHeight;
			for (Point p : targets)
			{
				if (p != null)
				{
					desiredTop = Math.min(desiredTop, p.y - CONTAINER_MARGIN);
				}
			}
			desiredTop = Math.max(desiredTop, edgeMargin);
			int newHeight = stableBottom - desiredTop;
			if (widget.getOriginalHeight() != newHeight)
			{
				widget.setOriginalHeight(newHeight);
				changed = true;
			}
		}
		else
		{
			int desiredBottom = currentTop + saved.originalHeight;
			for (Point p : targets)
			{
				if (p != null)
				{
					desiredBottom = Math.max(desiredBottom, p.y + CONTAINER_MARGIN);
				}
			}
			desiredBottom = Math.min(desiredBottom, canvasHeight - edgeMargin);
			int newHeight = desiredBottom - currentTop;
			if (widget.getOriginalHeight() != newHeight)
			{
				widget.setOriginalHeight(newHeight);
				changed = true;
			}
		}

		if (changed)
		{
			widget.revalidate();
		}
		return changed;
	}

	// TEMPORARY - debug-only, see onBeforeRender()'s call site.
	private String knownName(int id)
	{
		if (id == InterfaceID.ToplevelPreEoc.MAP_CONTAINER) return "MAP_CONTAINER";
		if (id == InterfaceID.ToplevelPreEoc.ORBS) return "ORBS";
		if (id == InterfaceID.ToplevelPreEoc.MAP_MINIMAP) return "MAP_MINIMAP";
		if (id == InterfaceID.ToplevelPreEoc.MINIMAP) return "MINIMAP";
		if (id == InterfaceID.ToplevelPreEoc.COMPASSCLICK) return "COMPASSCLICK";
		if (id == InterfaceID.ToplevelPreEoc.GAMEFRAME) return "GAMEFRAME";
		if (id == InterfaceID.ToplevelPreEoc.TLI_LISTENER) return "TLI_LISTENER";
		if (id == InterfaceID.ToplevelPreEoc.CONTROL) return "CONTROL";
		return "id=" + id;
	}

	// TEMPORARY - debug-only. Walks the SAME chain DebugWidgetBoundsOverlay
	// draws (orb's direct parent up to the root) and dumps every level's
	// live size/canvas position/position-mode in one log.debug line.
	private void logChainSizes(Widget anyOrb)
	{
		if (anyOrb == null)
		{
			return;
		}
		StringBuilder sb = new StringBuilder("chain:");
		net.runelite.api.Point orbLoc = anyOrb.getCanvasLocation();
		sb.append(" ORB ").append(anyOrb.getWidth()).append('x').append(anyOrb.getHeight())
			.append('@').append(orbLoc == null ? "?" : orbLoc.getX() + "," + orbLoc.getY());

		Widget w = anyOrb.getParent();
		int depth = 0;
		while (w != null && depth < 10)
		{
			net.runelite.api.Point loc = w.getCanvasLocation();
			sb.append(" | ").append(depth).append(':').append(knownName(w.getId()))
				.append(' ').append(w.getWidth()).append('x').append(w.getHeight())
				.append('@').append(loc == null ? "?" : loc.getX() + "," + loc.getY())
				.append(" xMode=").append(w.getXPositionMode())
				.append(" yMode=").append(w.getYPositionMode());
			w = w.getParent();
			depth++;
		}
		log.debug(sb.toString());
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
