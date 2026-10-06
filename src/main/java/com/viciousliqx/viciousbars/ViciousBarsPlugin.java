package com.viciousliqx.viciousbars;

import com.compactorbs.CompactOrbsConfig;
import com.compactorbs.CompactOrbsConfig.HotkeyOptions;
import com.compactorbs.CompactOrbsConfig.TogglePlacement;
import com.compactorbs.CompactOrbsConstants.ConfigGroup;
import static com.compactorbs.CompactOrbsConstants.ConfigGroup.GROUP_NAME;
import com.compactorbs.CompactOrbsConstants.ConfigKeys;
import static com.compactorbs.CompactOrbsConstants.Layout.EDIT_MODE_HIDDEN_OPACITY;
import com.compactorbs.CompactOrbsConstants.Script;
import com.compactorbs.CompactOrbsConstants.VarClient;
import com.compactorbs.CompactOrbsConstants.Varbit;
import com.compactorbs.CompactOrbsConstants.Widgets.Orb;
import com.compactorbs.CompactOrbsManager;
import com.compactorbs.widget.WidgetManager;
import com.compactorbs.widget.elements.Compass;
import com.compactorbs.widget.elements.Orbs;
import com.compactorbs.widget.layout.HideOrbRegistry;
import com.compactorbs.widget.layout.edit.DragListener;
import com.compactorbs.widget.layout.edit.DragState;
import com.compactorbs.widget.layout.edit.EditManager;
import com.compactorbs.widget.layout.slot.SlotManager;
import com.google.inject.Provides;
import java.awt.event.KeyEvent;
import java.util.Locale;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.events.VarClientIntChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.PluginChanged;
import net.runelite.client.events.ProfileChanged;
import net.runelite.client.input.KeyListener;
import net.runelite.client.input.KeyManager;
import net.runelite.client.input.MouseManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * FULL RESET IN PROGRESS - see vicious_bars_dev_talk.txt / git log for the
 * original bars feature (fully recoverable from history, nothing lost).
 * <p>
 * This plugin is currently the verbatim-copied Compact Orbs engine
 * (its-cue/compact-orbs, BSD-2-Clause - see com.compactorbs's own source
 * comments and compact_orbs_walkthrough.md at the hangar root), wired
 * directly into OUR plugin identity rather than running as a separate
 * parallel plugin: CompactOrbsConstants.ConfigGroup.GROUP_NAME was
 * retargeted to "vicious-bars" (this plugin's own existing config group),
 * so CompactOrbsConfig IS this plugin's real, live config now - not a
 * second entry in the plugin panel.
 * <p>
 * Being gutted down feature-by-feature from here with context applied to
 * each removal, not blanket deletion - e.g. the layout-preset SYSTEM
 * (Minimap/Orbs/Offset/Slot machinery) stays, since Vicious Bars still
 * needs it to place things; only the multi-layout CONFIG CHOICE goes,
 * since Vicious Bars will only ever have one single layout, not four.
 * Already removed before reaching this integrated state: the detached
 * minimap overlay feature, the wiki-plugin-conflict chat warning, the dead
 * "Detached Minimap" hotkey option, and orb-swapping in full (Compact Orbs'
 * own feature - letting you reassign which named orb sits in which slot
 * instead of freely dragging it. Vicious Bars is about bars, not
 * rearranging orbs, so keeping this would make us a redundant subset of
 * the real Compact Orbs rather than something distinct - see git history
 * for exact diffs). Store/activity orb hide toggles turned out to be
 * ordinary vanilla icons riding the same generic per-orb hide/reorder
 * system HP/Prayer/Run/Spec already need, so nothing to cut there. Still
 * to be picked off: the multi-layout config surface mentioned above.
 */
@Slf4j
@PluginDescriptor(
	name = "Vicious Bars"
)
public class ViciousBarsPlugin extends Plugin implements KeyListener
{
	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private CompactOrbsConfig config;

	@Inject
	private CompactOrbsManager manager;

	@Inject
	private ConfigManager configManager;

	@Inject
	private KeyManager keyManager;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private WidgetManager widgetManager;

	@Inject
	private SlotManager slotManager;

	@Inject
	private HideOrbRegistry orbRegistry;

	@Inject
	private EditManager editManager;

	@Inject
	private DragListener dragListener;

	@Inject
	private DragState dragState;

	@Inject
	private MouseManager mouseManager;

	@Inject
	private OrbZoneDebugOverlay orbZoneDebugOverlay;

	@Override
	protected void startUp() throws Exception
	{
		manager.migrateConfigs();
		overlayManager.add(orbZoneDebugOverlay);
		keyManager.registerKeyListener(this);
		mouseManager.registerMouseListener(dragListener);
		orbRegistry.registerAll();

		if (!manager.isLoggedIn())
		{
			manager.isLoggingIn = true;
		}

		manager.isUpdatingProfile = false;
		manager.isEditingLayout = false;
		manager.hideWorldMap = config.hideWorld();
		manager.hideLogoutX = config.hideLogout();
		manager.enableNoClickThrough = true;
		manager.snapCornerRepositioned = manager.isSnapCornerRepositioned();

		clientThread.invoke(() ->
		{
			slotManager.init();

			if (manager.isLoggedIn())
			{
				manager.update(Script.FORCE_UPDATE);
				manager.hideAllOrbsByConfig();
				manager.hideMinimapOnTabClose(config.hideMinimapWithSidePanel());
			}
		});

		log.debug("Vicious Bars started (Compact Orbs engine, retargeted config group)");
	}

	@Override
	protected void shutDown() throws Exception
	{
		overlayManager.remove(orbZoneDebugOverlay);
		keyManager.unregisterKeyListener(this);
		mouseManager.unregisterMouseListener(dragListener);
		clientThread.invoke(manager::reset);
		log.debug("Vicious Bars stopped");
	}

	@Provides
	CompactOrbsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CompactOrbsConfig.class);
	}

	@Subscribe
	public void onClientTick(ClientTick event)
	{
		if (!manager.isEditingLayout)
		{
			return;
		}

		dragListener.updateDrag();

		if (client.getMouseCurrentButton() == 0 && dragState.wasDragging)
		{
			dragListener.finalizeDrag();
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.HOPPING ||
			event.getGameState() == GameState.LOGIN_SCREEN ||
			event.getGameState() == GameState.LOGGING_IN)
		{
			manager.isLoggingIn = true;
			manager.wikiPluginBannerExists = false;
			manager.clearCustomChildren();
		}
		else if (manager.isLoggedIn() && manager.isLoggingIn)
		{
			manager.isLoggingIn = false;
			manager.createCustomChildren();
		}
	}

	@Subscribe(priority = -1.0f)
	public void onScriptPostFired(ScriptPostFired event)
	{
		int scriptId = event.getScriptId();

		if (manager.isEditingLayout &&
			(scriptId == Script.TOPLEVEL_REDRAW ||
				scriptId == Script.PROC_TOPLEVEL_SUBCHANGE ||
				scriptId == Script.TOPLEVEL_SIDE_CUSTOMIZE ||
				scriptId == Script.WIKI_ICON_INIT ||
				scriptId == Script.WORLD_MAP_UPDATE ||
				scriptId == Script.WIKI_ICON_UPDATE ||
				scriptId == Script.ORBS_UPDATE_STORE ||
				scriptId == Script.ORBS_UPDATE_ACTIVITY))
		{
			widgetManager.remapTargets(
				Orbs.WIKI_ICON_CONTAINER,
				Orbs.STORE_ORB_CONTAINER,
				Orbs.ACTIVITY_ORB_CONTAINER);

			widgetManager.setTargetsHidden(false, Compass.values());
			if (manager.isCompactLayout())
			{
				if (manager.compassFrame != null)
				{
					manager.compassFrame.setHidden(false);
				}
			}
			return;
		}

		switch (scriptId)
		{
			case Script.TOPLEVEL_SUBCHANGE:
				if (manager.isEditingLayout)
				{
					editManager.toggleEditMode(false);
				}
				break;

			case Script.TOPLEVEL_REDRAW:
			case Script.TOPLEVEL_RESIZE_CUSTOMIZE:
			case Script.PROC_TOPLEVEL_SUBCHANGE:
			case Script.TOPLEVEL_SIDE_CUSTOMIZE:
				manager.hideLogout();
				manager.hideMinimapOnTabClose(config.hideMinimapWithSidePanel());
				break;

			case Script.ORBS_UPDATE_SPECENERGY:
				if (manager.isEditingLayout && config.hideSpec())
				{
					widgetManager.setTargetOpacity(Orbs.SPEC_ORB_CONTAINER, EDIT_MODE_HIDDEN_OPACITY);
				}
			case Script.ORBS_UPDATE_HEALTH:
				if (!manager.isCompactLayout())
				{
					manager.updateNoClickThrough();
				}
				break;

			case Script.WORLD_MAP_UPDATE:
				if (manager.hideWorldMap)
				{
					widgetManager.remapTargets(Orbs.WORLD_MAP_CONTAINER);
					return;
				}
			case Script.ORBS_UPDATE_STORE:
			case Script.ORBS_UPDATE_ACTIVITY:
			case Script.WIKI_ICON_INIT:
			case Script.WIKI_ICON_UPDATE:
				if (!manager.isMinimapMinimized())
				{
					manager.update(scriptId);

					if (scriptId == Script.WIKI_ICON_INIT || scriptId == Script.WIKI_ICON_UPDATE)
					{
						manager.updateWikiBannerVisibility(config.hideWiki());
					}
				}
				break;
		}
	}

	@Subscribe
	public void onScriptPreFired(ScriptPreFired event)
	{
		int scriptId = event.getScriptId();

		if (scriptId == Orbs.WORLD_MAP_TOOLTIP.getScriptId())
		{
			int id = client.getIntStack()[2];
			int tooltipId = Orbs.WORLD_MAP_TOOLTIP.getComponentId();
			if (id == tooltipId)
			{
				boolean hidden = manager.isCompactLayout() || manager.hideWorldMap;
				widgetManager.setHidden(tooltipId, hidden);
			}
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		switch (event.getVarbitId())
		{
			case Varbit.CUTSCENE_STATUS:
				manager.isCutsceneActive = manager.isCutsceneActive();
				break;

			case Varbit.MINIMAP_TOGGLE:
				widgetManager.remapTargets(Orbs.LOGOUT_X_ICON, Orbs.LOGOUT_X_STONE);
				manager.updateCustomChildren();
				manager.setupMinimapContainer(false);
				break;

			case Varbit.STORE_ORB_TOGGLE:
			case Varbit.ACTIVITY_ORB_TOGGLE:
				if (manager.isEditingLayout)
				{
					editManager.toggleEditMode(false);
				}

				if (manager.allowReordering())
				{
					widgetManager.remapTargets(Orbs.values());

					if (config.minimapTogglePlacement() == TogglePlacement.BELOW_MAP)
					{
						manager.updateMinimapToggleButton();
					}
				}
				break;
		}
	}

	@Subscribe
	public void onVarClientIntChanged(VarClientIntChanged event)
	{
		if (event.getIndex() == VarClient.SIDE_PANEL_ID)
		{
			if (manager.isEditingLayout &&
				config.hideMinimapWithSidePanel() && manager.isSidePanelHidden())
			{
				editManager.toggleEditMode(false);
			}
		}
	}

	@Subscribe(priority = -1.0f)
	public void onWidgetLoaded(WidgetLoaded event)
	{
		int id = event.getGroupId();
		if (id == Orb.UNIVERSE >> 16)
		{
			manager.update(Script.FORCE_UPDATE);
			manager.hideAllOrbsByConfig();
		}
	}

	@Subscribe(priority = -1.0f)
	public void onConfigChanged(ConfigChanged event)
	{
		String group = event.getGroup();
		String key = event.getKey();

		if (group.equals(ConfigGroup.Core.RUNELITE))
		{
			if (key.equals(ConfigKeys.Core.SNAPCORNER_PREFIX + OverlayPosition.CANVAS_TOP_RIGHT + ConfigKeys.Core.SNAPCORNER_CONFIG_LOCATION))
			{
				manager.snapCornerRepositioned = event.getNewValue() != null;
			}
		}

		if (group.equals(ConfigGroup.Core.MINIMAP))
		{
			if (key.equals(ConfigKeys.Core.HIDE_MINIMAP))
			{
				clientThread.invokeLater(() -> manager.hideMinimapOnTabClose(config.hideMinimapWithSidePanel()));
			}
		}

		if (group.equals(ConfigGroup.Core.WIKI))
		{
			if (key.equals(ConfigKeys.Core.SHOW_WIKI_MINIMAP_BUTTON))
			{
				clientThread.invokeLater(() ->
				{
					if (manager.isEditingLayout)
					{
						editManager.toggleEditMode(false);
					}
					widgetManager.remapTargets(Orbs.WIKI_ICON_CONTAINER);
					manager.updateWikiBannerVisibility(config.hideWiki());
					manager.updateCustomChildren();
				});
			}
		}

		if (!group.equals(GROUP_NAME))
		{
			return;
		}

		if (orbRegistry.isHideConfig(key))
		{
			if (!manager.isEditingLayout)
			{
				clientThread.invokeLater(() ->
				{
					manager.hideOrbByConfig(key);

					if (!manager.isFixedMode())
					{
						manager.rebuildLayout();
					}
				});
			}
			return;
		}

		if (manager.isEditingLayout && !manager.isUpdatingProfile)
		{
			clientThread.invokeLater(() ->
			{
				if (!key.contains(manager.getCurrentPrefix()))
				{
					editManager.toggleEditMode(false);
				}
			});
		}

		switch (key)
		{
			case ConfigKeys.COMPASS:
				clientThread.invokeLater(() ->
				{
					manager.setupMinimapContainer(false);
					widgetManager.setTargetsHidden(manager.isCompassHidden(), Compass.values());
					widgetManager.remapTargets(Compass.values());
					widgetManager.remapTargets(Orbs.values());
				});
				break;

			case ConfigKeys.MINIMAP_BUTTON_PLACEMENT:
				clientThread.invokeLater(manager::updateMinimapToggleButton);
				break;

			case ConfigKeys.RIGHT_CLICK_TOGGLE_BUTTONS:
				clientThread.invokeLater(manager::updateCustomChildren);
				break;

			case ConfigKeys.ENABLE_NO_CLICKTHROUGH:
				clientThread.invokeLater(() ->
				{
					manager.enableNoClickThrough = true;
					manager.updateNoClickThrough();

					if (!manager.isCompactLayout())
					{
						widgetManager.remapTargets(Orbs.XP_DROPS_CONTAINER);
					}
				});
				break;

			case ConfigKeys.HIDE_MINIMAP_WITH_SIDE_PANEL:
				clientThread.invokeLater(() -> manager.hideMinimapOnTabClose(config.hideMinimapWithSidePanel()));
				break;

			case ConfigKeys.ORB_LAYOUT:
			case ConfigKeys.VERTICAL_Y_ADJUSTMENT:
			case ConfigKeys.HORIZONTAL_ANCHOR:
			case ConfigKeys.VERTICAL_ANCHOR:
			case ConfigKeys.DISABLE_REORDERING:
			case ConfigKeys.LEAVE_EMPTY_SPACE:
				clientThread.invokeLater(() ->
				{
					if (!manager.isFixedMode())
					{
						manager.rebuildLayout();
					}
				});
				break;
		}
	}

	@Override
	public void resetConfiguration()
	{
		manager.isUpdatingProfile = true;
		clientThread.invoke(() -> manager.rebuild(true));
	}

	@Subscribe
	public void onProfileChanged(ProfileChanged event)
	{
		manager.migrateConfigs();

		manager.isUpdatingProfile = true;
		clientThread.invoke(() -> manager.rebuild(false));
	}

	@Subscribe
	public void onPluginChanged(PluginChanged event)
	{
		String name = event.getPlugin().getName().toLowerCase(Locale.ROOT);
		switch (name)
		{
			case ConfigGroup.Core.WIKI:
				clientThread.invokeLater(() ->
				{
					if (manager.isEditingLayout)
					{
						editManager.toggleEditMode(false);
					}
					else
					{
						manager.rebuildLayout();
					}
					manager.updateWikiBannerVisibility(config.hideWiki());
				});
				break;

			case ConfigGroup.Core.MINIMAP:
				clientThread.invokeLater(() -> manager.hideMinimapOnTabClose(config.hideMinimapWithSidePanel()));
				break;
		}
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		manager.addCustomMenuEntries(event.getMenuEntry());
	}

	@Override
	public void keyTyped(KeyEvent e)
	{
	}

	@Override
	public void keyPressed(KeyEvent e)
	{
		if (manager.isEditingLayout)
		{
			if (e.getKeyCode() == KeyEvent.VK_ESCAPE || config.hotkeyKeybind().matches(e))
			{
				if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
				{
					e.consume();
				}
				clientThread.invokeLater(() -> editManager.toggleEditMode(false));
			}
			return;
		}

		if (!config.hotkeyKeybind().matches(e))
		{
			return;
		}

		if (manager.isCutsceneActive)
		{
			return;
		}

		if (config.toggleOption() == HotkeyOptions.MINIMAP_BUTTON
			|| manager.isFixedMode() && config.toggleOption() != HotkeyOptions.EDIT_MODE)
		{
			manager.saveConfig(ConfigKeys.MINIMAP_TOGGLE_BUTTON, !config.hideMinimapToggle());
			return;
		}

		switch (config.toggleOption())
		{
			case MINIMAP:
				clientThread.invokeLater(manager::onMinimapToggle);
				break;

			case EDIT_MODE:
				clientThread.invokeLater(() -> editManager.toggleEditMode(true));
				break;
		}
	}

	@Override
	public void keyReleased(KeyEvent e)
	{
	}
}
