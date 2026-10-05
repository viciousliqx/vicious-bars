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
	// TEMPORARY - how far past the target point to extend ORBS/MAP_CONTAINER,
	// see onBeforeRender()'s javadoc. Not a config value - purely experimental
	// while we figure out the real approach for this.
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
	// Rip both this field and the overlay/add/remove lines out once the
	// minimap work is sorted.
	@Inject
	private DebugWidgetBoundsOverlay debugWidgetBoundsOverlay;

	// TEMPORARY - see onBeforeRender()'s javadoc. Saved once, lazily, the
	// first time either container actually gets grown.
	private boolean containersExpanded;
	private int savedMapContainerWidth;
	private int savedMapContainerHeight;
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

		// Merged mode hides the real orb, Aligned mode moves it, while we're
		// running - make sure none of them are left hidden or displaced
		// behind us if the plugin stops.
		hpBarOverlay.unhideRealOrb();
		prayerBarOverlay.unhideRealOrb();
		specBarOverlay.unhideRealOrb();
		runBarOverlay.unhideRealOrb();

		hpBarOverlay.restoreRealOrbPosition();
		prayerBarOverlay.restoreRealOrbPosition();
		specBarOverlay.restoreRealOrbPosition();
		runBarOverlay.restoreRealOrbPosition();

		restoreContainerSizes();

		log.debug("Vicious Bars stopped!");
	}

	/**
	 * TEMPORARY - experimental, see the in-game testing note in
	 * vicious_bars_dev_talk.txt. Grows the real ORBS and MAP_CONTAINER
	 * widgets' own bounding box (never the minimap/compass's own size or
	 * position within it - those keep their current local offsets, so the
	 * visible circle doesn't move) downward/rightward only as far as
	 * whichever Aligned-mode bar currently needs reaching, using the same
	 * setOriginalWidth/Height()+revalidate() pattern as everywhere else.
	 * This is what lets {@link VitalBarOverlay#alignRealOrb} actually reach a
	 * Docked bar that's far from the orb's vanilla position, instead of
	 * stalling on the parent-bounds clamp the way it did before this existed.
	 */
	@Subscribe
	public void onBeforeRender(BeforeRender event)
	{
		Point[] targets = {
			hpBarOverlay.getLastIconCanvasCenter(),
			prayerBarOverlay.getLastIconCanvasCenter(),
			specBarOverlay.getLastIconCanvasCenter(),
			runBarOverlay.getLastIconCanvasCenter(),
		};

		Widget mapContainer = client.getWidget(InterfaceID.ToplevelPreEoc.MAP_CONTAINER);
		Widget orbsContainer = client.getWidget(InterfaceID.ToplevelPreEoc.ORBS);

		boolean anyTarget = false;
		for (Point p : targets)
		{
			if (p != null)
			{
				anyTarget = true;
				break;
			}
		}

		if (!anyTarget || mapContainer == null || orbsContainer == null)
		{
			restoreContainerSizes();
			return;
		}

		if (!containersExpanded)
		{
			savedMapContainerWidth = mapContainer.getOriginalWidth();
			savedMapContainerHeight = mapContainer.getOriginalHeight();
			savedOrbsWidth = orbsContainer.getOriginalWidth();
			savedOrbsHeight = orbsContainer.getOriginalHeight();
			containersExpanded = true;
		}

		// Take the max across every bar currently targeting a position, not
		// just the first one found - two Aligned bars in different dock
		// slots need the container to reach the FARTHER of the two, not
		// whichever bar's overlay happened to render first this frame.
		net.runelite.api.Point mapCanvas = mapContainer.getCanvasLocation();
		int neededWidth = savedMapContainerWidth;
		int neededHeight = savedMapContainerHeight;
		for (Point p : targets)
		{
			if (p == null)
			{
				continue;
			}
			neededWidth = Math.max(neededWidth, p.x - mapCanvas.getX() + CONTAINER_MARGIN);
			neededHeight = Math.max(neededHeight, p.y - mapCanvas.getY() + CONTAINER_MARGIN);
		}

		boolean changed = false;
		if (mapContainer.getOriginalWidth() != neededWidth)
		{
			mapContainer.setOriginalWidth(neededWidth);
			changed = true;
		}
		if (mapContainer.getOriginalHeight() != neededHeight)
		{
			mapContainer.setOriginalHeight(neededHeight);
			changed = true;
		}

		// ORBS is itself positioned at some local (x, y) inside MAP_CONTAINER
		// - the space actually available to it is the container's new size
		// minus that offset, not the container's full size.
		int orbsNeededWidth = Math.max(savedOrbsWidth, neededWidth - orbsContainer.getOriginalX());
		int orbsNeededHeight = Math.max(savedOrbsHeight, neededHeight - orbsContainer.getOriginalY());
		if (orbsContainer.getOriginalWidth() != orbsNeededWidth)
		{
			orbsContainer.setOriginalWidth(orbsNeededWidth);
			changed = true;
		}
		if (orbsContainer.getOriginalHeight() != orbsNeededHeight)
		{
			orbsContainer.setOriginalHeight(orbsNeededHeight);
			changed = true;
		}

		if (changed)
		{
			mapContainer.revalidate();
			orbsContainer.revalidate();
		}
	}

	private void restoreContainerSizes()
	{
		if (!containersExpanded)
		{
			return;
		}
		Widget mapContainer = client.getWidget(InterfaceID.ToplevelPreEoc.MAP_CONTAINER);
		if (mapContainer != null)
		{
			mapContainer.setOriginalWidth(savedMapContainerWidth);
			mapContainer.setOriginalHeight(savedMapContainerHeight);
			mapContainer.revalidate();
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
