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
import net.runelite.api.widgets.WidgetType;
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
	// How far past the farthest bar target to extend each ancestor, see
	// onBeforeRender()'s javadoc.
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

	// Populated lazily, the first time the chain actually gets grown; one
	// entry per ancestor widget touched, holding what to restore it to on
	// the way out (toggle off, or plugin stop).
	private final List<GrownWidget> grownWidgets = new ArrayList<>();

	private static final class GrownWidget
	{
		final Widget widget;
		final int originalX;
		final int originalY;
		final int originalWidth;
		final int originalHeight;
		// Captured ONCE, before this widget was ever grown: its own initial
		// canvas edges relative to its PARENT's initial canvas position.
		// Meaningless (left as 0) for MAP_CONTAINER, which is self-positioned
		// instead - see growToCoverTargets()'s javadoc for why these two
		// cases need different anchor math entirely.
		final int initialLeftOffsetFromParent;
		final int initialRightOffsetFromParent;
		final int initialTopOffsetFromParent;
		final int initialBottomOffsetFromParent;

		GrownWidget(Widget widget, Widget parent)
		{
			this.widget = widget;
			this.originalX = widget.getOriginalX();
			this.originalY = widget.getOriginalY();
			this.originalWidth = widget.getOriginalWidth();
			this.originalHeight = widget.getOriginalHeight();

			if (parent != null)
			{
				net.runelite.api.Point c = widget.getCanvasLocation();
				net.runelite.api.Point p = parent.getCanvasLocation();
				this.initialLeftOffsetFromParent = c.getX() - p.getX();
				this.initialRightOffsetFromParent = (c.getX() + widget.getWidth()) - p.getX();
				this.initialTopOffsetFromParent = c.getY() - p.getY();
				this.initialBottomOffsetFromParent = (c.getY() + widget.getHeight()) - p.getY();
			}
			else
			{
				this.initialLeftOffsetFromParent = 0;
				this.initialRightOffsetFromParent = 0;
				this.initialTopOffsetFromParent = 0;
				this.initialBottomOffsetFromParent = 0;
			}
		}
	}

	// Lazily-saved real state of the minimap's actual graphic/mask widget,
	// so the hide below can be reversed exactly. Same trick Compact Orbs'
	// real source uses - confirmed it targets InterfaceID.ToplevelPreEoc.
	// MINIMAP specifically (not the MAP_MINIMAP container we were hiding via
	// setHidden() before), by flipping its WidgetType from GRAPHIC to LAYER
	// and clearing its sprite, rather than hiding/resizing anything. Nothing
	// about its size, position, or the surrounding hierarchy is ever
	// touched, which is exactly why it doesn't interact with the growth
	// mechanism below at all.
	private boolean minimapRenderingSaved;
	private int savedMinimapType;
	private int savedMinimapContentType;
	private int savedMinimapSpriteId;

	private void setMinimapRenderingHidden(boolean hidden)
	{
		Widget minimap = client.getWidget(InterfaceID.ToplevelPreEoc.MINIMAP);
		if (minimap == null)
		{
			return;
		}

		if (hidden)
		{
			if (!minimapRenderingSaved)
			{
				savedMinimapType = minimap.getType();
				savedMinimapContentType = minimap.getContentType();
				savedMinimapSpriteId = minimap.getSpriteId();
				minimapRenderingSaved = true;
			}
			minimap.setType(WidgetType.LAYER);
			minimap.setContentType(0);
			minimap.setSpriteId(-1);
		}
		else if (minimapRenderingSaved)
		{
			minimap.setType(savedMinimapType);
			minimap.setContentType(savedMinimapContentType);
			minimap.setSpriteId(savedMinimapSpriteId);
			minimapRenderingSaved = false;
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

		// Every orb bar pulls its real orb onto itself while docking is on -
		// make sure none of them are left displaced behind us if the plugin
		// stops.
		hpBarOverlay.restoreRealOrbPosition();
		prayerBarOverlay.restoreRealOrbPosition();
		specBarOverlay.restoreRealOrbPosition();
		runBarOverlay.restoreRealOrbPosition();

		restoreGrownWidgets();
		setMinimapRenderingHidden(false);

		log.debug("Vicious Bars stopped!");
	}

	/**
	 * Grows every real ancestor from an orb's actual parent up to
	 * MAP_CONTAINER so nothing in the chain clips the others, letting each
	 * orb's own {@link VitalBarOverlay#alignRealOrb} reach all the way to
	 * its bar. Walked dynamically via repeated {@code getParent()} rather
	 * than hardcoded IDs/depth - the minimized OrbsNomap interface has an
	 * extra level the normal one doesn't, so a fixed depth silently misses
	 * a level (ORBS itself was missed this way once already).
	 * <p>
	 * {@link #growToCoverTargets} only ever writes width/height, never X/Y -
	 * confirmed position writes on this chain are inert. It's anchor-aware:
	 * MAP_CONTAINER (the one manually-draggable top-level panel) trusts its
	 * own live canvas location, since that's externally tracked and immune
	 * to its own writes; every ordinary nested widget instead derives its
	 * stable edge from its PARENT's live position plus its own initial
	 * offset from that parent, since those genuinely do react to their own
	 * size and reading their own canvas location directly created a
	 * confirmed runaway feedback loop. Recomputed fresh every frame, never
	 * from last frame's already-grown size, so it shrinks back down again
	 * once a target moves closer, and manually dragging the minimap
	 * mid-session recalibrates on its own next frame.
	 */
	@Subscribe
	public void onBeforeRender(BeforeRender event)
	{
		boolean enabled = config.dockOrbsToBars();
		setMinimapRenderingHidden(enabled);

		if (!enabled)
		{
			restoreGrownWidgets();
			return;
		}

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

		// A user who ALSO hits the vanilla minimap's own minimize button
		// (separate from our toggle above, which uses the GRAPHIC/LAYER
		// trick and never touches this) swaps in a completely different
		// widget interface - InterfaceID.OrbsNomap instead of Orbs, own
		// distinct component ids - so the normal lookup alone would go null
		// and silently stop growing anything. Falling back keeps this
		// working either way; orbParent still resolves dynamically via
		// getParent() same as always.
		Widget anyOrb = client.getWidget(InterfaceID.Orbs.ORB_HEALTH);
		if (anyOrb == null)
		{
			anyOrb = client.getWidget(InterfaceID.OrbsNomap.ORB_HEALTH);
		}
		if (!anyBarTarget || anyOrb == null)
		{
			restoreGrownWidgets();
			return;
		}

		// MAP_CONTAINER is self-positioned (see growToCoverTargets()'s
		// javadoc) and irrelevant/hidden once the map is minimized - nothing
		// to grow there in that case, and no reason to even try.
		Widget mapContainer = client.getWidget(InterfaceID.ToplevelPreEoc.MAP_CONTAINER);
		boolean changed = false;
		if (mapContainer != null && !mapContainer.isHidden())
		{
			changed |= growToCoverTargets(mapContainer, null, barTargets);
		}

		// Every ordinary level between the orb and MAP_CONTAINER clips its
		// descendants to its own bounds, so all of them need to grow -
		// collected first, then processed outermost-first (nearest
		// MAP_CONTAINER) so each inner call reads its own parent's
		// ALREADY-updated position from earlier in this same frame.
		List<Widget> chain = new ArrayList<>();
		Widget w = anyOrb.getParent();
		while (w != null && w != mapContainer && !w.isHidden())
		{
			chain.add(w);
			w = w.getParent();
		}
		for (int i = chain.size() - 1; i >= 0; i--)
		{
			Widget widget = chain.get(i);
			changed |= growToCoverTargets(widget, widget.getParent(), barTargets);
		}

		// TEMPORARY - debug-only. Dumps the full ancestor chain's live sizes
		// in one line, but ONLY on a frame where something actually got
		// resized.
		if (changed)
		{
			logChainSizes(anyOrb);
		}
	}

	/**
	 * Grows {@code widget}'s own local width/height (NEVER X/Y - confirmed
	 * via direct testing that position writes on this chain are inert) so
	 * its real canvas rect covers every non-null target point (with a
	 * margin).
	 * <p>
	 * Two genuinely different cases, distinguished by whether {@code parent}
	 * is null:
	 * <p>
	 * {@code parent == null} - MAP_CONTAINER only, the one SELF-POSITIONED
	 * widget in this chain. Once the user has manually repositioned this
	 * panel, the client tracks a stored absolute canvas position immune to
	 * layout-driven recompute from a plain size change - confirmed via the
	 * log: it sat frozen at the same canvas X for ten straight frames while
	 * its width alone climbed by a fixed ~34px every single one, regardless
	 * of its declared ABSOLUTE_RIGHT mode. So its own live
	 * {@code getCanvasLocation()}, read fresh every frame, IS the one safe,
	 * convergent anchor here (it only moves on a genuine external drag) -
	 * always grow the far edge out from it.
	 * <p>
	 * {@code parent != null} - every ordinary nested widget in the chain
	 * (the orb's real parent, any level above it, the ORBS slot). These
	 * genuinely DO follow their declared position mode's live formula -
	 * confirmed the hard way: the minimized interface's extra CONTENT level
	 * (id=58654722) is ABSOLUTE_RIGHT and its canvasLeft recedes in lockstep
	 * with its own width (right edge constant at the same value every
	 * frame), the literal OPPOSITE of MAP_CONTAINER's behavior. Reading
	 * {@code widget.getCanvasLocation()} directly here would feed our own
	 * last write back in as "the anchor" every frame - the same runaway
	 * shape, just triggered by the opposite assumption. The fix: derive the
	 * stable edge from the PARENT's live position (read fresh - already
	 * updated this frame, since the caller processes outermost-first) plus
	 * this widget's own OWN initial offset from that parent, captured once
	 * before it was ever grown. That's immune to the widget's own size
	 * entirely, while still tracking genuine parent movement (e.g. the user
	 * dragging the map) on its own.
	 */
	private boolean growToCoverTargets(Widget widget, Widget parent, Point[] targets)
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
			saved = new GrownWidget(widget, parent);
			grownWidgets.add(saved);
		}

		// Never ask for more than the real client viewport can show -
		// "spasm near the screen edge" is consistent with the engine itself
		// clamping a widget's actual rendered bounds to the visible canvas,
		// which would make our own write and the next frame's readback
		// disagree, oscillating. Clamping our own target to a bit inside the
		// real canvas bounds avoids ever asking for something the engine
		// would silently clamp anyway.
		int canvasWidth = client.getCanvasWidth();
		int canvasHeight = client.getCanvasHeight();
		int edgeMargin = 10;

		boolean changed = false;

		if (parent == null)
		{
			net.runelite.api.Point canvas = widget.getCanvasLocation();
			int currentLeft = canvas.getX();
			int currentTop = canvas.getY();

			int desiredRight = currentLeft + saved.originalWidth;
			for (Point p : targets)
			{
				if (p != null)
				{
					desiredRight = Math.max(desiredRight, p.x + CONTAINER_MARGIN);
				}
			}
			desiredRight = Math.min(desiredRight, canvasWidth - edgeMargin);
			int newWidth = Math.max(saved.originalWidth, desiredRight - currentLeft);
			if (widget.getOriginalWidth() != newWidth)
			{
				widget.setOriginalWidth(newWidth);
				changed = true;
			}

			int desiredBottom = currentTop + saved.originalHeight;
			for (Point p : targets)
			{
				if (p != null)
				{
					desiredBottom = Math.max(desiredBottom, p.y + CONTAINER_MARGIN);
				}
			}
			desiredBottom = Math.min(desiredBottom, canvasHeight - edgeMargin);
			int newHeight = Math.max(saved.originalHeight, desiredBottom - currentTop);
			if (widget.getOriginalHeight() != newHeight)
			{
				widget.setOriginalHeight(newHeight);
				changed = true;
			}
		}
		else
		{
			net.runelite.api.Point parentCanvas = parent.getCanvasLocation();

			if (widget.getXPositionMode() == WidgetPositionMode.ABSOLUTE_RIGHT)
			{
				int stableRight = parentCanvas.getX() + saved.initialRightOffsetFromParent;
				int desiredLeft = stableRight - saved.originalWidth;
				for (Point p : targets)
				{
					if (p != null)
					{
						desiredLeft = Math.min(desiredLeft, p.x - CONTAINER_MARGIN);
					}
				}
				desiredLeft = Math.max(desiredLeft, edgeMargin);
				int newWidth = Math.max(saved.originalWidth, stableRight - desiredLeft);
				if (widget.getOriginalWidth() != newWidth)
				{
					widget.setOriginalWidth(newWidth);
					changed = true;
				}
			}
			else
			{
				int stableLeft = parentCanvas.getX() + saved.initialLeftOffsetFromParent;
				int desiredRight = stableLeft + saved.originalWidth;
				for (Point p : targets)
				{
					if (p != null)
					{
						desiredRight = Math.max(desiredRight, p.x + CONTAINER_MARGIN);
					}
				}
				desiredRight = Math.min(desiredRight, canvasWidth - edgeMargin);
				int newWidth = Math.max(saved.originalWidth, desiredRight - stableLeft);
				if (widget.getOriginalWidth() != newWidth)
				{
					widget.setOriginalWidth(newWidth);
					changed = true;
				}
			}

			if (widget.getYPositionMode() == WidgetPositionMode.ABSOLUTE_BOTTOM)
			{
				int stableBottom = parentCanvas.getY() + saved.initialBottomOffsetFromParent;
				int desiredTop = stableBottom - saved.originalHeight;
				for (Point p : targets)
				{
					if (p != null)
					{
						desiredTop = Math.min(desiredTop, p.y - CONTAINER_MARGIN);
					}
				}
				desiredTop = Math.max(desiredTop, edgeMargin);
				int newHeight = Math.max(saved.originalHeight, stableBottom - desiredTop);
				if (widget.getOriginalHeight() != newHeight)
				{
					widget.setOriginalHeight(newHeight);
					changed = true;
				}
			}
			else
			{
				int stableTop = parentCanvas.getY() + saved.initialTopOffsetFromParent;
				int desiredBottom = stableTop + saved.originalHeight;
				for (Point p : targets)
				{
					if (p != null)
					{
						desiredBottom = Math.max(desiredBottom, p.y + CONTAINER_MARGIN);
					}
				}
				desiredBottom = Math.min(desiredBottom, canvasHeight - edgeMargin);
				int newHeight = Math.max(saved.originalHeight, desiredBottom - stableTop);
				if (widget.getOriginalHeight() != newHeight)
				{
					widget.setOriginalHeight(newHeight);
					changed = true;
				}
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
