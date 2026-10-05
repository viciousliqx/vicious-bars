package com.viciousliqx.viciousbars;

import com.google.inject.Provides;
import java.awt.Point;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.BeforeRender;
import net.runelite.api.events.GameTick;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
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

	// Experimental - see onBeforeRender()'s javadoc.
	private boolean mapContainerMoved;

	@Override
	protected void startUp() throws Exception
	{
		eventBus.register(opponentTracker);
		eventBus.register(prayerBarOverlay);
		eventBus.register(this);

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
		opponentTracker.reset();

		// Merged mode hides the real orb while we're running - make sure none
		// of them are left hidden behind us if the plugin stops.
		hpBarOverlay.unhideRealOrb();
		prayerBarOverlay.unhideRealOrb();
		specBarOverlay.unhideRealOrb();
		runBarOverlay.unhideRealOrb();

		restoreMapContainer();

		log.debug("Vicious Bars stopped!");
	}

	/**
	 * Experimental - single-purpose: dock the real MAP_CONTAINER widget
	 * (minimap, compass, and orbs together) to the panel, using the exact
	 * same {@link PanelDock} math a bar uses, via the Minimap dock
	 * side/offset config. Uses {@code setForcedPosition()} rather than
	 * {@code setOriginalX/Y()+revalidate()} - this is the same real API
	 * RuneLite's own Compact Orbs uses for its minimap overlay, and almost
	 * certainly what backs the vanilla client's own "hold Alt and drag to
	 * reposition an interface" feature: an explicit override meant to
	 * survive whatever native layout pass runs afterward, rather than a
	 * plain position write that pass might just recompute over. No
	 * resizing, no per-orb logic - just a position, same as a bar gets.
	 * Untested yet whether this also carries the minimized (no-map)
	 * orb-only state along with it, or whether that needs its own separate
	 * handling - that's the next thing to check.
	 */
	@Subscribe
	public void onBeforeRender(BeforeRender event)
	{
		Widget mapContainer = client.getWidget(InterfaceID.ToplevelPreEoc.MAP_CONTAINER);

		if (!config.minimapDockEnabled() || mapContainer == null)
		{
			restoreMapContainer();
			return;
		}

		mapContainerMoved = true;

		Point dockPos = PanelDock.position(client, config.minimapDockSide(), 1,
			mapContainer.getOriginalWidth(), config.dockPadding(), config.minimapDockOffset(),
			mapContainer.getOriginalHeight(), Justification.START);

		if (dockPos == null)
		{
			// Panel not present (wrong layout, not logged in yet) - nothing to dock to.
			restoreMapContainer();
			return;
		}

		Widget mapParent = mapContainer.getParent();
		net.runelite.api.Point mapParentCanvas = mapParent != null
			? mapParent.getCanvasLocation()
			: new net.runelite.api.Point(0, 0);
		int localX = dockPos.x - mapParentCanvas.getX();
		int localY = dockPos.y - mapParentCanvas.getY();

		log.debug("minimap dock: dockPos={} localX={} localY={} canvasLocation={}",
			dockPos, localX, localY, mapContainer.getCanvasLocation());

		mapContainer.setForcedPosition(localX, localY);
	}

	// TEMPORARY - diagnostic only. Logs MAP_CONTAINER's real fields every
	// tick regardless of our own code, so a manual real Alt+drag of the
	// minimap (with Dock minimap turned OFF, so our own writes don't
	// interfere) shows exactly what actually changes during a genuine drag.
	@Subscribe
	public void onGameTick(GameTick event)
	{
		Widget mapContainer = client.getWidget(InterfaceID.ToplevelPreEoc.MAP_CONTAINER);
		if (mapContainer == null)
		{
			return;
		}
		log.debug("minimap tick: originalX={} originalY={} xMode={} yMode={} canvasLocation={}",
			mapContainer.getOriginalX(), mapContainer.getOriginalY(),
			mapContainer.getXPositionMode(), mapContainer.getYPositionMode(),
			mapContainer.getCanvasLocation());
	}

	private void restoreMapContainer()
	{
		if (!mapContainerMoved)
		{
			return;
		}
		Widget mapContainer = client.getWidget(InterfaceID.ToplevelPreEoc.MAP_CONTAINER);
		if (mapContainer != null)
		{
			// Same clear-and-revalidate pair Compact Orbs' own MinimapOverlay
			// uses to hand a forced-position widget back to normal layout.
			mapContainer.setForcedPosition(-1, -1);
			mapContainer.revalidate();
		}
		mapContainerMoved = false;
	}

	@Provides
	ViciousBarsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ViciousBarsConfig.class);
	}
}
