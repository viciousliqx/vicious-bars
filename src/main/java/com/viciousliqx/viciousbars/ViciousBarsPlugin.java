package com.viciousliqx.viciousbars;

import com.google.inject.Provides;
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
	// Kept through the orb-code reset specifically to watch Compact Orbs
	// operate on the real widget tree - it just draws whatever's there live,
	// no dependency on our own growth code (which this reset removed).
	@Inject
	private DebugWidgetBoundsOverlay debugWidgetBoundsOverlay;

	// TEMPORARY - diagnostic only, see MinimapDragDiagnostic's javadoc. Same
	// reasoning - a per-tick log of the real widget tree, independent of
	// whatever plugin (ours or Compact Orbs) is actually touching it.
	@Inject
	private MinimapDragDiagnostic minimapDragDiagnostic;

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

		// TEMPORARY - undo the debug minimap-graphic hide, in case the
		// plugin stops while that toggle is on.
		Widget mapMinimap = client.getWidget(InterfaceID.ToplevelPreEoc.MAP_MINIMAP);
		if (mapMinimap != null)
		{
			mapMinimap.setHidden(false);
		}

		log.debug("Vicious Bars stopped!");
	}

	// TEMPORARY - debug-only, see ViciousBarsConfig#debugHideMinimapGraphic.
	@Subscribe
	public void onBeforeRender(BeforeRender event)
	{
		Widget mapMinimap = client.getWidget(InterfaceID.ToplevelPreEoc.MAP_MINIMAP);
		if (mapMinimap != null && mapMinimap.isHidden() != config.debugHideMinimapGraphic())
		{
			mapMinimap.setHidden(config.debugHideMinimapGraphic());
		}
	}

	@Provides
	ViciousBarsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ViciousBarsConfig.class);
	}
}
