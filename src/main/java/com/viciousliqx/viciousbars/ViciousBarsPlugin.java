package com.viciousliqx.viciousbars;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.BeforeRender;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
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

	// Lazily-saved real state of the minimap's actual graphic/mask widget,
	// so the hide below can be reversed exactly. Same trick Compact Orbs'
	// real source uses - confirmed it targets InterfaceID.ToplevelPreEoc.
	// MINIMAP specifically, by flipping its WidgetType from GRAPHIC to LAYER
	// and clearing its sprite, rather than hiding/resizing anything. Nothing
	// about its size, position, or the surrounding hierarchy is ever touched
	// - MAP_CONTAINER and the orb cluster stay at their own natural size
	// always now, since VitalBarOverlay anchors bars to wherever each real
	// orb already sits instead of growing anything to reach the inventory
	// panel (the old growth mechanism this plugin used to carry - see git
	// history / vicious_bars_dev_talk.txt for why that approach was dropped).
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

		setMinimapRenderingHidden(false);

		log.debug("Vicious Bars stopped!");
	}

	@Subscribe
	public void onBeforeRender(BeforeRender event)
	{
		setMinimapRenderingHidden(config.dockOrbsToBars());
	}

	@Provides
	ViciousBarsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ViciousBarsConfig.class);
	}
}
