package com.viciousliqx.viciousbars;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.BeforeRender;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.SpriteID;
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
 * <p>
 * The hide mechanism here is ported feature-for-feature from Compact Orbs'
 * real source (its-cue/compact-orbs, BSD-2-Clause) after a full line-by-line
 * read documented in compact_orbs_walkthrough.md at the hangar root - not
 * re-derived from guessing. Two corrections that read came from: (1) their
 * REGULAR hide-minimap toggle uses a plain setHidden(true) on seven widgets,
 * not the GRAPHIC<->LAYER content-type trick (that trick is specific to
 * their separate edit-mode code path); (2) they don't preserve the real
 * compass ring when hiding those seven widgets - they hide it right along
 * with everything else, then draw their OWN small independent replacement
 * ring graphic on top.
 */
@Slf4j
@PluginDescriptor(
	name = "Vicious Bars"
)
public class ViciousBarsPlugin extends Plugin
{
	// Compact Orbs' CompassFrame: 43x43, SpriteID.COMPASS_OUTLINE, drawn as
	// a child of COMPASS_PARENT (= InterfaceID.ToplevelPreEoc.MAP_MINIMAP,
	// confirmed against their CompactOrbsConstants.java) at the default
	// (0,0) local position - which lands it exactly over the real compass
	// needle for free, since that's the same container the needle itself
	// already lives in.
	private static final int COMPASS_FRAME_SIZE = 43;

	// The seven widgets Compact Orbs' real hide-minimap toggle plain-hides
	// (CompactOrbsManager#setupMinimapContainer -> Minimap.COMPONENTS):
	// the terrain graphic, the circular frame/border (which also backs the
	// real compass ring - hence the replacement graphic below), and six
	// invisible click-blocker regions left over from the full-size map.
	private static final int[] HIDE_TARGETS = {
		InterfaceID.ToplevelPreEoc.MINIMAP,
		InterfaceID.ToplevelPreEoc.MAP_MINIMAP_GRAPHIC9,
		InterfaceID.ToplevelPreEoc.MAP_NOCLICK_0,
		InterfaceID.ToplevelPreEoc.MAP_NOCLICK_1,
		InterfaceID.ToplevelPreEoc.MAP_NOCLICK_2,
		InterfaceID.ToplevelPreEoc.MAP_NOCLICK_3,
		InterfaceID.ToplevelPreEoc.MAP_NOCLICK_4,
		InterfaceID.ToplevelPreEoc.MAP_NOCLICK_5,
	};

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

	// Our own replacement compass ring, created once lazily and then just
	// shown/hidden - same shape as Compact Orbs' own compassFrame field.
	private Widget compassFrame;

	private void setMinimapHidden(boolean hidden)
	{
		for (int componentId : HIDE_TARGETS)
		{
			Widget widget = client.getWidget(componentId);
			if (widget != null)
			{
				widget.setHidden(hidden);
			}
		}

		updateCompassFrame(hidden);
	}

	private void updateCompassFrame(boolean minimapHidden)
	{
		if (compassFrame == null && minimapHidden)
		{
			Widget parent = client.getWidget(InterfaceID.ToplevelPreEoc.MAP_MINIMAP);
			if (parent == null)
			{
				return;
			}
			compassFrame = parent.createChild(-1, WidgetType.GRAPHIC)
				.setOriginalWidth(COMPASS_FRAME_SIZE)
				.setOriginalHeight(COMPASS_FRAME_SIZE)
				.setSpriteId(SpriteID.COMPASS_OUTLINE)
				.setOpacity(0)
				.setHidden(false);

			// Compact Orbs leaves this at the default (0,0) local position,
			// which only lands on the real compass because their
			// COMPASS_PARENT's own origin happens to coincide with it in
			// their layout. Confirmed via live testing that assumption
			// doesn't hold here - centered it on the real COMPASSCLICK
			// widget's own canvas center instead, converted into local
			// coordinates relative to this same parent, which is robust
			// regardless of how the two are actually nested.
			Widget compassClick = client.getWidget(InterfaceID.ToplevelPreEoc.COMPASSCLICK);
			if (compassClick != null)
			{
				net.runelite.api.Point parentCanvas = parent.getCanvasLocation();
				net.runelite.api.Point clickCanvas = compassClick.getCanvasLocation();
				int centerX = clickCanvas.getX() + compassClick.getWidth() / 2;
				int centerY = clickCanvas.getY() + compassClick.getHeight() / 2;
				compassFrame.setOriginalX(centerX - parentCanvas.getX() - COMPASS_FRAME_SIZE / 2);
				compassFrame.setOriginalY(centerY - parentCanvas.getY() - COMPASS_FRAME_SIZE / 2);
			}

			compassFrame.revalidate();
		}

		if (compassFrame != null)
		{
			compassFrame.setHidden(!minimapHidden);
		}
	}

	private void clearCompassFrame()
	{
		if (compassFrame != null)
		{
			compassFrame.setHidden(true);
			compassFrame = null;
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
		setMinimapHidden(false);
		clearCompassFrame();
		log.debug("Vicious Bars stopped");
	}

	@Subscribe
	public void onBeforeRender(BeforeRender event)
	{
		setMinimapHidden(config.hideMinimap());
	}

	@Provides
	ViciousBarsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ViciousBarsConfig.class);
	}
}
