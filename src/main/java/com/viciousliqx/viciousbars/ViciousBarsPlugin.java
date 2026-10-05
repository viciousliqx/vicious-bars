package com.viciousliqx.viciousbars;

import com.google.inject.Provides;
import java.awt.Point;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.BeforeRender;
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
	// Experimental - how far past the dock target point to extend ORBS, see
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

	// Experimental - see onBeforeRender()'s javadoc. Saved once, lazily, the
	// first time ORBS actually gets grown.
	private boolean containersExpanded;
	private int savedOrbsWidth;
	private int savedOrbsHeight;

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

		// Every orb bar pulls its real orb onto itself while running - make
		// sure none of them are left displaced behind us if the plugin stops.
		hpBarOverlay.restoreRealOrbPosition();
		prayerBarOverlay.restoreRealOrbPosition();
		specBarOverlay.restoreRealOrbPosition();
		runBarOverlay.restoreRealOrbPosition();

		restoreContainerSizes();

		log.debug("Vicious Bars stopped!");
	}

	/**
	 * Experimental - grows only the real ORBS widget's own bounding box
	 * (never MAP_CONTAINER - confirmed via direct in-game testing that
	 * growing MAP_CONTAINER stretches/displaces its OTHER children too: the
	 * compass face, XP orb, scroll/wiki orb, and apparently the live
	 * minimap render itself, none of which use fixed pixel sizes relative
	 * to it. ORBS only holds the four orb icons, so growing just it keeps
	 * the reach isolated away from anything else in the cluster - also
	 * never touches its own position, for the same reason MAP_CONTAINER's
	 * position isn't reachable: see vicious_bars_dev_talk.txt) just far
	 * enough to reach two possible targets: the Minimap dock position
	 * (computed via the same {@link PanelDock} math a bar uses, from the
	 * Minimap dock side/offset config) and whichever orb bar's own icon
	 * position is farthest. Either alone would do the individual-orb clamp
	 * in {@link VitalBarOverlay#alignRealOrb} no good if the other needed
	 * more room, so both feed the same growth target.
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

		Widget mapContainer = client.getWidget(InterfaceID.ToplevelPreEoc.MAP_CONTAINER);
		Widget orbsContainer = client.getWidget(InterfaceID.ToplevelPreEoc.ORBS);

		boolean anyBarTarget = false;
		for (Point p : barTargets)
		{
			if (p != null)
			{
				anyBarTarget = true;
				break;
			}
		}

		Point dockTarget = null;
		if (config.minimapDockEnabled() && mapContainer != null)
		{
			dockTarget = PanelDock.position(client, config.minimapDockSide(), 1,
				mapContainer.getOriginalWidth(), config.dockPadding(), config.minimapDockOffset(),
				mapContainer.getOriginalHeight(), Justification.START);
		}

		if ((!anyBarTarget && dockTarget == null) || mapContainer == null || orbsContainer == null)
		{
			restoreContainerSizes();
			return;
		}

		if (!containersExpanded)
		{
			savedOrbsWidth = orbsContainer.getOriginalWidth();
			savedOrbsHeight = orbsContainer.getOriginalHeight();
			containersExpanded = true;
		}

		// MAP_CONTAINER itself is never resized - confirmed via direct
		// in-game testing that at least some of its OTHER children (the
		// compass face, XP orb, scroll/wiki orb, and apparently the live
		// minimap render itself) aren't using fixed pixel sizes relative to
		// it, so growing it stretched/displaced all of them. ORBS only
		// holds the four orb icons, so growing just it (testing now whether
		// MAP_CONTAINER even clips children past its own bounds) should
		// isolate the reach entirely away from anything else in the cluster.
		net.runelite.api.Point orbsCanvas = orbsContainer.getCanvasLocation();
		int neededWidth = savedOrbsWidth;
		int neededHeight = savedOrbsHeight;

		if (dockTarget != null)
		{
			neededWidth = Math.max(neededWidth, dockTarget.x - orbsCanvas.getX() + CONTAINER_MARGIN);
			neededHeight = Math.max(neededHeight, dockTarget.y - orbsCanvas.getY() + CONTAINER_MARGIN);
		}
		for (Point p : barTargets)
		{
			if (p == null)
			{
				continue;
			}
			neededWidth = Math.max(neededWidth, p.x - orbsCanvas.getX() + CONTAINER_MARGIN);
			neededHeight = Math.max(neededHeight, p.y - orbsCanvas.getY() + CONTAINER_MARGIN);
		}

		boolean changed = false;
		if (orbsContainer.getOriginalWidth() != neededWidth)
		{
			orbsContainer.setOriginalWidth(neededWidth);
			changed = true;
		}
		if (orbsContainer.getOriginalHeight() != neededHeight)
		{
			orbsContainer.setOriginalHeight(neededHeight);
			changed = true;
		}

		if (changed)
		{
			orbsContainer.revalidate();
		}
	}

	private void restoreContainerSizes()
	{
		if (!containersExpanded)
		{
			return;
		}
		Widget orbsContainer = client.getWidget(InterfaceID.ToplevelPreEoc.ORBS);
		if (orbsContainer != null)
		{
			orbsContainer.setOriginalWidth(savedOrbsWidth);
			orbsContainer.setOriginalHeight(savedOrbsHeight);
			orbsContainer.revalidate();
		}
		containersExpanded = false;
	}

	@Provides
	ViciousBarsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ViciousBarsConfig.class);
	}
}
