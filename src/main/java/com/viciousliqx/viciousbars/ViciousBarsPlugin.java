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
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

/**
 * FULL RESET IN PROGRESS - see vicious_bars_dev_talk.txt / git log for the
 * bars feature this plugin used to have (fully recoverable from history,
 * nothing lost). Deliberately stripped down to a single, isolated piece:
 * replicate Compact Orbs' own minimap-hide mechanism, with nothing else
 * going on - no bars, no docking, no config beyond the one toggle below -
 * so this foundation can be gotten right on its own before anything else
 * is layered back on top of it.
 */
@Slf4j
@PluginDescriptor(
	name = "Vicious Bars"
)
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
	private OrbZoneDebugOverlay orbZoneDebugOverlay;

	// Lazily-saved real state of the minimap's actual graphic/mask widget,
	// so the hide below can be reversed exactly. Confirmed against Compact
	// Orbs' real source: it targets InterfaceID.ToplevelPreEoc.MINIMAP
	// specifically, by flipping its WidgetType from GRAPHIC to LAYER and
	// clearing its sprite - not a hide, not a resize, nothing about the
	// surrounding hierarchy ever changes.
	private boolean minimapRenderingSaved;
	private int savedMinimapType;
	private int savedMinimapContentType;
	private int savedMinimapSpriteId;

	// The first hide alone left the full circular frame/border visible -
	// MINIMAP (the content-type swap above) is only the TERRAIN graphic.
	// Confirmed against Compact Orbs' real source (CompactOrbsManager#
	// setupMinimapContainer, hiding its Minimap.COMPONENTS group): the
	// actual frame/border is a SEPARATE widget, MAP_MINIMAP_GRAPHIC9 (their
	// own constant for it is even just named "MINIMAP", confusingly
	// distinct from their "MINIMAP_MASK" which is our MINIMAP target
	// above) - plus six invisible click-blocker widgets, MAP_NOCLICK_0
	// through MAP_NOCLICK_5. All seven are hidden with a plain
	// setHidden(true)/false, no type/sprite tricks needed for these.
	private static final int[] EXTRA_HIDE_TARGETS = {
		InterfaceID.ToplevelPreEoc.MAP_MINIMAP_GRAPHIC9,
		InterfaceID.ToplevelPreEoc.MAP_NOCLICK_0,
		InterfaceID.ToplevelPreEoc.MAP_NOCLICK_1,
		InterfaceID.ToplevelPreEoc.MAP_NOCLICK_2,
		InterfaceID.ToplevelPreEoc.MAP_NOCLICK_3,
		InterfaceID.ToplevelPreEoc.MAP_NOCLICK_4,
		InterfaceID.ToplevelPreEoc.MAP_NOCLICK_5,
	};

	private void setMinimapRenderingHidden(boolean hidden)
	{
		Widget minimap = client.getWidget(InterfaceID.ToplevelPreEoc.MINIMAP);
		if (minimap != null)
		{
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

		for (int componentId : EXTRA_HIDE_TARGETS)
		{
			Widget widget = client.getWidget(componentId);
			if (widget != null)
			{
				widget.setHidden(hidden);
			}
		}
	}

	@Override
	protected void startUp() throws Exception
	{
		eventBus.register(this);
		overlayManager.add(orbZoneDebugOverlay);
		log.debug("Vicious Bars started (minimap-replacement foundation only)");
	}

	@Override
	protected void shutDown() throws Exception
	{
		eventBus.unregister(this);
		overlayManager.remove(orbZoneDebugOverlay);
		setMinimapRenderingHidden(false);
		log.debug("Vicious Bars stopped");
	}

	@Subscribe
	public void onBeforeRender(BeforeRender event)
	{
		setMinimapRenderingHidden(config.hideMinimap());
	}

	@Provides
	ViciousBarsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ViciousBarsConfig.class);
	}
}
