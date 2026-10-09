/*
 * Copyright (c) 2025, cue <https://github.com/its-cue>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.compactorbs;

import com.compactorbs.CompactOrbsConfig.HorizontalAnchor;
import com.compactorbs.CompactOrbsConfig.HotkeyOptions;
import com.compactorbs.CompactOrbsConfig.VerticalAnchor;
import com.compactorbs.CompactOrbsConstants.ConfigGroup;
import com.compactorbs.CompactOrbsConstants.ConfigKeys;
import com.compactorbs.CompactOrbsConstants.Layout;
import com.compactorbs.CompactOrbsConstants.MenuOp;
import com.compactorbs.CompactOrbsConstants.Script;
import com.compactorbs.CompactOrbsConstants.VarClient;
import com.compactorbs.CompactOrbsConstants.Varbit;
import com.compactorbs.CompactOrbsConstants.Widgets.Classic;
import com.compactorbs.CompactOrbsConstants.Widgets.Modern;
import com.compactorbs.util.MigrateConfig;
import com.compactorbs.util.ValueKey;
import com.compactorbs.widget.TargetWidget;
import com.compactorbs.widget.WidgetManager;
import com.compactorbs.widget.elements.Button;
import com.compactorbs.widget.elements.Compass;
import com.compactorbs.widget.elements.Minimap;
import com.compactorbs.widget.elements.Orbs;
import com.compactorbs.widget.layout.HideOrbConfig;
import com.compactorbs.widget.layout.HideOrbRegistry;
import com.compactorbs.widget.layout.edit.Binding;
import com.compactorbs.widget.layout.edit.BindingManager;
import com.compactorbs.widget.layout.edit.DragState;
import com.compactorbs.widget.layout.edit.EditManager;
import com.compactorbs.widget.layout.slot.SlotManager;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Menu;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.JavaScriptCallback;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetPositionMode;
import net.runelite.api.widgets.WidgetSizeMode;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.Keybind;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.util.ColorUtil;

@Slf4j
@Singleton
public class CompactOrbsManager
{
	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ConfigManager configManager;

	@Inject
	private CompactOrbsConfig config;

	@Inject
	private WidgetManager widgetManager;

	@Inject
	private SlotManager slotManager;

	@Inject
	private HideOrbRegistry orbHidden;

	@Inject
	private EditManager editManager;

	@Inject
	private DragState dragState;

	@Inject
	private BindingManager bindingManager;

	public boolean wikiPluginBannerExists;

	public boolean snapCornerRepositioned;
	public boolean isUpdatingProfile;
	public boolean isEditingLayout;
	public boolean hideWorldMap;
	public boolean hideLogoutX;
	public boolean enableNoClickThrough;
	public boolean isLoggingIn;
	public boolean isCutsceneActive;

	// Written every game tick by ViciousBarsPlugin#onGameTick() off its own
	// InteractingChanged tracking (with a lingering grace period after a
	// fight ends, so this doesn't flicker off mid-fight) - read by every
	// VialOverlay's threshold-glow check for ThresholdMode.COMBAT_ONLY.
	public boolean inCombat;

	// Same tracking as inCombat above, narrowed to the NPC case only (null
	// if the current/lingering target is a player, or there's no target at
	// all) - read by EnemyHpAuxBarOverlay. Restored from v1's
	// OpponentTracker#getOpponent() (see git log before the full-reset
	// "gut pass").
	public NPC combatTargetNpc;

	// Written every game tick by ViciousBarsPlugin#onGameTick(), read by
	// Hp/SpecVialOverlay to draw the frame-ring regen highlight - same
	// plain-field-on-manager pattern as hideWorldMap/hideLogoutX above, not
	// event-driven on this side since the overlay renders every frame
	// regardless of tick cadence.
	public double hpRegenPercentage;
	public double prayerRegenPercentage;
	public double specRegenPercentage;

	// Separate from hpRegenPercentage above - HP can be gaining and losing
	// value on completely independent schedules (its own hitpoint regen
	// tick vs a poison/envenom damage tick), so this needs its own field
	// rather than overloading the regen one. Null (not just 0) while
	// nothing's currently draining, since HpVialOverlay's drain ring should
	// be fully absent then, not just invisible at 0% progress.
	public Double hpDrainPercentage;

	// Same reasoning as hpDrainPercentage above - Prayer's own passive
	// drain-while-active mechanic runs on a completely different cadence
	// than prayerRegenPercentage's potion-driven restore, so it needs its
	// own field too. Null while no prayer is active (nothing draining).
	public Double prayerDrainPercentage;

	public Widget compassFrame;
	public Widget minimapButton;
	private final Map<TargetWidget, Widget> noClickThroughChildren = new HashMap<>();

	//update on startup, onWidgetLoaded, and onScriptPostFired
	public void update(int scriptId)
	{
		createCustomChildren();

		if (scriptId == Script.FORCE_UPDATE)
		{
			rebuildLayout();
			return;
		}

		widgetManager.remapTargetsByScriptId(scriptId, Orbs.values());
		updateNoClickThrough();
	}

	//rebuild the layout from state/config
	public void rebuildLayout()
	{
		updateCustomChildren();

		if (isFixedMode())
		{
			widgetManager.remapTargets(Orbs.values());
			updateNoClickThrough();
			return;
		}

		setupMinimapContainer(false);
		widgetManager.setTargetsHidden(isCompactLayout() && isCompassHidden(), Compass.values());
		widgetManager.remapTargets(Compass.values());
		widgetManager.remapTargets(Orbs.values());
		updateNoClickThrough();

		if (!isClassicResizable() && !isFixedMode())
		{
			hideLogout();
		}
	}

	//only called on plugin shutdown
	public void reset()
	{
		if (isEditingLayout)
		{
			//save configs on shutdown
			isUpdatingProfile = false;
			editManager.toggleEditMode(false);
		}

		hideWorldMap = false;
		hideLogoutX = false;
		enableNoClickThrough = false;
		isUpdatingProfile = false;

		bindingManager.clear();
		orbHidden.clear();

		clearCustomChildren();

		resetVisibility();
		resetPositioning();
		resetNoClickThrough();

		//save for last to prevent potential NPE with offsets
		slotManager.clear();
	}

	//used for profile swap/full config reset
	public void rebuild(boolean reset)
	{
		if (isEditingLayout)
		{
			//do not save configs
			editManager.toggleEditMode(false);
		}

		hideWorldMap = config.hideWorld();
		hideLogoutX = config.hideLogout();
		enableNoClickThrough = true;
		isUpdatingProfile = false;

		//update based on config
		hideAllOrbsByConfig();
		slotManager.update();

		if (reset)
		{
			resetSavedPositionConfigs();
		}
		rebuildLayout();
	}

	private void resetVisibility()
	{
		widgetManager.setTargetsHidden(false, Orbs.values());
		widgetManager.setTargetsHidden(false, Compass.values());
		hideMinimapOnTabClose(false);
		updateWikiBannerVisibility(false);
		widgetManager.setTargetsHidden(hideStonesAndIcons(), Orbs.LOGOUT_X_ICON, Orbs.LOGOUT_X_STONE);
	}

	private void resetPositioning()
	{
		if (isFixedMode())
		{
			widgetManager.remapTargets(true, Script.FORCE_UPDATE, Orbs.WORLD_MAP_CONTAINER);
		}

		setupMinimapContainer(true);

		widgetManager.remapTargets(true, Script.FORCE_UPDATE, Orbs.values());
		widgetManager.remapTargets(true, Script.FORCE_UPDATE, Compass.values());
	}

	private void resetNoClickThrough()
	{
		for (TargetWidget orb : Orbs.SWAPPABLE_ORBS)
		{
			widgetManager.setNoClickThrough(orb.getComponentId(), false);
			handleVanillaNoClickThrough(orb, false);
			widgetManager.clearChild(noClickThroughChildren.remove(orb));
		}
	}

	//toggle the minimap visibility, and update related widgets when using the custom toggle button
	public void onMinimapToggle()
	{
		saveConfig(ConfigKeys.MINIMAP, !isMinimapHidden());
		rebuildLayout();
		hideMinimapOnTabClose(config.hideMinimapWithSidePanel());
	}

	public void setupMinimapContainer(boolean toDefault)
	{
		widgetManager.setTargetsHidden(!toDefault && isCompactLayout(), Minimap.COMPONENTS);
		widgetManager.remapTargets(toDefault, Script.FORCE_UPDATE, Minimap.CONTAINERS);
		widgetManager.revalidate(Minimap.COMPONENTS);

		//TODO - seems like revalidation causes the width/height issue (on the orbsContainer)
		//Orb.UNIVERSE parent width/height mode seems to cause an issue when setting dimensions
		//causing the Orbs.UNIVERSE container be set to the dimensions of the tli (GAMEFRAME)
		//this should alleviate that by setting the size temporarily until restored by a clientscript (clicking a side panel tab, etc)
		if (!isFixedMode() && !isMinimapMinimized())
		{
			Widget orbsContainer = client.getWidget(Minimap.ORBS_UNIVERSE.getComponentId());
			Widget parent = client.getWidget((isClassicResizable() ? Minimap.CLASSIC_ORBS_CONTAINER : Minimap.MODERN_ORBS_CONTAINER).getComponentId());
			if (orbsContainer != null && parent != null)
			{
				widgetManager.updateValue(orbsContainer::getWidth, orbsContainer::setWidth, parent.getWidth());
				widgetManager.updateValue(orbsContainer::getHeight, orbsContainer::setHeight, parent.getHeight());
			}
		}
	}

	public void hideMinimapOnTabClose(boolean hide)
	{
		if (!isFixedMode() && !isClassicResizable())
		{
			//wait for the wiki plugin to create its banner
			// - side panel appears to be flagged as 'closed' on world-hop(other?), which will hide the minimap container
			//   and cause the wiki plugin to return early when it tries to create its widget
			if (isWikiPluginConfigEnabled() && !wikiPluginBannerExists)
			{
				Widget container = widgetManager.getTargetWidget(Orbs.WIKI_ICON_CONTAINER);
				if (container == null)
				{
					return;
				}

				if (!widgetManager.hasChildren(container))
				{
					return;
				}

				wikiPluginBannerExists = true;
			}

			updateLogoutXPosition();

			if (isCutsceneActive || isMinimapMinimized() || isMinimapPluginConfigEnabled())
			{
				return;
			}

			boolean hidden = hide && isCompactLayout() && isSidePanelHidden();

			//only hide the parent not the children (do not use setTargetsHidden/setHidden(target...))
			widgetManager.setHidden(Minimap.MODERN_MAP_MINIMAP.getComponentId(), hidden);
			widgetManager.setHidden(Minimap.MODERN_ORBS_CONTAINER.getComponentId(), hidden);
		}
	}

	public boolean hideMinimapWithSidePanel()
	{
		return config.hideMinimapWithSidePanel();
	}

	public void updateNoClickThrough()
	{
		if (isFixedMode())
		{
			return;
		}

		boolean noClick = enableNoClickThrough;

		for (TargetWidget orb : Orbs.SWAPPABLE_ORBS)
		{
			widgetManager.setNoClickThrough(orb.getComponentId(), noClick && isCompactLayout());

			if (!isCompactLayout())
			{
				handleVanillaNoClickThrough(orb, noClick);
			}
			else
			{
				widgetManager.clearChild(noClickThroughChildren.remove(orb));
			}
		}
	}

	//TODO
	//in non-compact layouts, delegate the noClickThrough flag to the button widget instead of the layer/backing,
	//since under certain configurations when orb swapping, it may prevent a click where they overlap
	//if noClickThrough is enabled, and the button is hidden - create a noClick child in its place
	private void handleVanillaNoClickThrough(TargetWidget target, boolean noClickThrough)
	{
		Widget layer = client.getWidget(target.getComponentId());
		Widget backing = client.getWidget(target.getBackingId());
		Widget button = client.getWidget(target.getButtonId());
		if (layer == null || backing == null || button == null)
		{
			return;
		}

		widgetManager.setNoClickThrough(backing.getId(), !noClickThrough);
		widgetManager.setNoClickThrough(button.getId(), noClickThrough);

		if (!button.isHidden())
		{
			widgetManager.clearChild(noClickThroughChildren.remove(target));
			return;
		}

		Widget noClick = noClickThroughChildren.get(target);
		if (noClick == null)
		{
			noClick = widgetManager.createNoClick(layer, button);
			noClick.setNoClickThrough(true);
			noClick.revalidate();

			noClickThroughChildren.put(target, noClick);
		}
	}

	//create the compass frame and toggle buttons, clearing them if the parent id changed,
	//and only creating widgets if missing from the current parent
	public void createCustomChildren()
	{
		Widget parent = widgetManager.getOrbsParent();
		if (parent == null)
		{
			return;
		}

		if (minimapButton != null && minimapButton.getParent() != parent)
		{
			minimapButton = null;
		}

		if (minimapButton == null)
		{
			minimapButton = widgetManager.createMinimapButton(parent);
			minimapButton.setAction(MenuOp.OP_INDEX_0, buildToggleOp(isMinimapHidden(), MenuOp.MINIMAP_OP));
			minimapButton.setOnOpListener(
				(JavaScriptCallback) e ->
				{
					switch (e.getOp() - 1)
					{
						case MenuOp.OP_INDEX_0:
							if (!isFixedMode())
							{
								onMinimapToggle();
								minimapButton.setSpriteId(widgetManager.getSpriteId(!isMinimapHidden()));
							}
							break;

						case MenuOp.EDIT_MODE_OP_INDEX:
							if (isFixedMode())
							{
								editManager.toggleEditMode(true);
							}
							break;
					}
				}
			);
			minimapButton.setOnMouseOverListener(
				(JavaScriptCallback) e ->
				{
					minimapButton.setOpacity(Layout.OPACITY_HOVER);
				}
			);
			minimapButton.setOnMouseLeaveListener(
				(JavaScriptCallback) e ->
					minimapButton.setOpacity(Layout.OPACITY)
			);
		}

		//compass frame moved to the compass container
		parent = client.getWidget(isClassicResizable() ? Classic.COMPASS_PARENT : Modern.COMPASS_PARENT);
		if (parent == null)
		{
			if (compassFrame != null)
			{
				compassFrame = null;
			}
			return;
		}

		if (compassFrame != null && compassFrame.getParent() != parent || isFixedMode())
		{
			compassFrame = null;
		}

		if (compassFrame == null && !isFixedMode())
		{
			compassFrame = widgetManager.createCompassFrame(parent);
		}

		updateCustomChildren();
	}

	public void updateCustomChildren()
	{
		updateCompassFrameChild();
		updateMinimapToggleButton();
	}

	private void updateCompassFrameChild()
	{
		if (compassFrame == null)
		{
			return;
		}

		compassFrame.setHidden(!isMinimapHidden() || isCompassHidden() || isMinimapMinimized());
	}

	public void updateMinimapToggleButton()
	{
		if (minimapButton == null)
		{
			return;
		}

		minimapButton.setHidden(isMinimapMinimized());

		int index = MenuOp.OP_INDEX_0;
		if (isFixedMode())
		{
			index = MenuOp.EDIT_MODE_OP_INDEX;
		}
		minimapButton.setAction(
			index == MenuOp.OP_INDEX_0
				? MenuOp.EDIT_MODE_OP_INDEX
				: MenuOp.OP_INDEX_0, "");

		minimapButton.setAction(index,
			isFixedMode()
				? buildEditOp(false)
				: buildToggleOp(isMinimapHidden(), MenuOp.MINIMAP_OP));

		minimapButton.setNoClickThrough(true);
		widgetManager.remapTargets(
			Button.MINIMAP_BUTTON_MODERN,
			Button.MINIMAP_BUTTON_CLASSIC,
			Button.MINIMAP_BUTTON_FIXED
		);
	}

	//clear any created children and reset previous parent id
	public void clearCustomChildren()
	{
		widgetManager.clearChild(minimapButton);
		widgetManager.clearChild(compassFrame);

		minimapButton = null;
		compassFrame = null;
	}

	public void updateWikiBannerVisibility(boolean hidden)
	{
		Widget container = widgetManager.getTargetWidget(Orbs.WIKI_ICON_CONTAINER);
		if (container == null)
		{
			return;
		}

		Widget banner = widgetManager.getTargetWidget(Orbs.WIKI_VANILLA_CONTAINER);
		if (widgetManager.hasChildren(container))
		{
			banner = container.getChild(0);
		}

		if (banner == null)
		{
			return;
		}

		//vanilla banner should be hidden if the in-game setting is disabled
		if (isWikiBannerDisabled() && !widgetManager.hasChildren(container) && !isEditingLayout)
		{
			hidden = true;
		}

		banner.setHidden(hidden);
	}

	public String buildEditOp(boolean hidden)
	{
		return (hidden ? MenuOp.SAVE : MenuOp.ENABLE) + " "
			+ (hidden ? MenuOp.CHANGES : MenuOp.EDIT_MODE);
	}

	public String buildToggleOp(boolean hidden, String target)
	{
		return buildMenuOp(hidden ? MenuOp.SHOW : MenuOp.HIDE, target);
	}

	public String buildMenuOp(String action, String target)
	{
		return action + " " + ColorUtil.wrapWithColorTag(target, MenuOp.MENU_TARGET);
	}

	public void addCustomMenuEntries(MenuEntry entry)
	{
		if (entry.getType() != MenuAction.CC_OP)
		{
			return;
		}

		Menu menu = client.getMenu();
		Widget widget = entry.getWidget();
		if (widget == null)
		{
			return;
		}

		if (widget == minimapButton)
		{
			if (!isFixedMode())
			{
				menu.createMenuEntry(-2)
					.setOption(buildEditOp(false))
					.setForceLeftClick(false)
					.setDeprioritized(false)
					.setType(MenuAction.RUNELITE_LOW_PRIORITY)
					.onClick(e ->
						editManager.toggleEditMode(true)
					);
			}
		}
	}

	// Hardcoded here rather than routed through the Config interface -
	// RuneLite's config proxy returns null (not the interface's own
	// default method body) for a method with no @ConfigItem, which NPEs
	// on unboxing the moment anything calls it. Confirmed live: this is
	// exactly what broke plugin startup the first time this was tried via
	// config.layout() with the @ConfigItem removed.
	//
	// CUSTOM (not a preset like HORIZONTAL_WIDE) is the right hardcode for
	// Vicious Bars: each bar anchors to its own orb's natural position
	// (see HpVialOverlay), so there's no grid of slots to lay out - and
	// isCustomLayout() (EditManager's gate for whether an orb's edit-mode
	// handler is draggable at all) is only ever true when this is CUSTOM.
	public CompactOrbsLayout getCurrentLayout()
	{
		return CompactOrbsLayout.CUSTOM;
	}

	//invert for readability
	public boolean isFixedMode()
	{
		return !client.isResized();
	}

	public boolean isClassicResizable()
	{
		if (!isLoggedIn() || isFixedMode())
		{
			return false;
		}

		final int classicArrangement = 0;
		return client.getVarbitValue(Varbit.RESIZABLE_STONE_ARRANGEMENT) == classicArrangement;
	}

	public boolean isLoggedIn()
	{
		return client.getGameState() == GameState.LOGGED_IN;
	}

	public boolean isCompactLayout()
	{
		return !isFixedMode() && isMinimapHidden() && !isMinimapMinimized();
	}

	public boolean isCustomLayout()
	{
		return getCurrentLayout().isCustom() && isCompactLayout();
	}

	public boolean isVanillaCustom()
	{
		return !isCompactLayout() && !isMinimapMinimized();
	}

	// Hardcoded - see CompactOrbsConfig's own comment on the four removed
	// Layout Options items (Vertical offset/anchor, Compass fix, Disable
	// orb reordering) for why. Left (not Right) and Top (not Bottom) match
	// what was actually live and confirmed-stable; reordering was never
	// disabled.
	public boolean isAnchorLeft()
	{
		return true;
	}

	public boolean isAnchorRight()
	{
		return false;
	}

	public boolean isAnchorTop()
	{
		return true;
	}

	public boolean isAnchorBottom()
	{
		return false;
	}

	public boolean allowReordering()
	{
		return true;
	}

	public boolean isMinimapHidden()
	{
		if (isFixedMode())
		{
			return false;
		}

		return config.hideMinimap();
	}

	public boolean isCompassHidden()
	{
		return config.hideCompass();
	}

	public boolean isXpDropHidden()
	{
		return config.hideXp();
	}

	public boolean isWikiHidden()
	{
		return config.hideWiki();
	}

	public boolean isStoreHidden()
	{
		return config.hideStore();
	}

	public boolean shouldOffsetXpOrb()
	{
		return !isFixedMode() && enableNoClickThrough;
	}

	//prevent unintended state changes for the logout-x since it is treated as a side icon/stone, i.e. don't unhide when it should be hidden
	public void hideLogout()
	{
		if (isFixedMode() || isMinimapMinimized())
		{
			return;
		}

		boolean hidden = hideLogoutX || hideStonesAndIcons();

		widgetManager.setTargetsHidden(hidden, Orbs.LOGOUT_X_STONE, Orbs.LOGOUT_X_ICON);
	}

	public void updateLogoutXPosition()
	{
		if (isFixedMode() || isMinimapMinimized())
		{
			return;
		}
		widgetManager.remapTargets(Orbs.LOGOUT_X_STONE, Orbs.LOGOUT_X_ICON);
	}

	//similar to how the cs2 script hides/shows the logout-x by referencing the containers hidden state
	private boolean hideStonesAndIcons()
	{
		Widget w = client.getWidget(isClassicResizable() ? Classic.SIDE_TOP : Modern.SIDE_MOVABLE);
		if (w == null)
		{
			return false;
		}

		return w.isHidden();
	}

	public boolean isActivityOrbDisabled()
	{
		final int activityOrbVisible = 0;
		return client.getVarbitValue(Varbit.ACTIVITY_ORB_TOGGLE) != activityOrbVisible;
	}

	public boolean isStoreOrbDisabled()
	{
		final int storeOrbVisible = 1;
		return client.getVarbitValue(Varbit.STORE_ORB_TOGGLE) != storeOrbVisible;
	}

	public boolean isWikiBannerDisabled()
	{
		final int wikiIconVisible = 0;
		return client.getVarbitValue(Varbit.WIKI_ICON_TOGGLE) != wikiIconVisible;
	}

	public boolean isMinimapMinimized()
	{
		//login screen NPE when 'loading interfaces %' occurs post update for the MINIMAP_TOGGLE varbit?
		if (!isLoggedIn())
		{
			return false;
		}

		final int minimapMinimized = 1;
		return client.getVarbitValue(Varbit.MINIMAP_TOGGLE) == minimapMinimized;
	}

	public boolean isCutsceneActive()
	{
		final int cutsceneActive = 1;
		return client.getVarbitValue(Varbit.CUTSCENE_STATUS) == cutsceneActive;
	}

	public boolean isSidePanelHidden()
	{
		if (!isLoggedIn())
		{
			return false;
		}

		final int sidePanelHidden = -1;
		return client.getVarcIntValue(VarClient.SIDE_PANEL_ID) == sidePanelHidden;
	}

	public int getLayoutXOffset()
	{
		return isCompactLayout() && isAnchorRight()
			? getCurrentLayout().getRightOffset() : 0;
	}

	public int getLayoutYOffset()
	{
		return isCompactLayout() && isAnchorBottom()
			? getCurrentLayout().getBottomOffset() : 0;
	}

	// isAnchorBottom() is hardcoded false (see that method's own comment) -
	// every caller of clampVerticalY() already gates its own call behind
	// an isAnchorBottom() check of its own, so this was already permanently
	// unreachable at the call sites; simplified to match rather than kept
	// as dead code referencing the now-removed verticalYAdjustment() config.
	public int clampVerticalY()
	{
		return 0;
	}

	public void hideAllOrbsByConfig()
	{
		orbHidden.configKeys().forEach(this::hideOrbByConfig);
	}

	public void hideOrbByConfig(String key)
	{
		if (isUpdatingProfile || isEditingLayout || key.equals(ConfigKeys.COMPASS))
		{
			return;
		}

		switch (key)
		{
			case ConfigKeys.HIDE_WORLD:
				hideWorldMap = config.hideWorld();
				widgetManager.remapTargets(Orbs.WORLD_MAP_CONTAINER);
				break;

			case ConfigKeys.HIDE_WIKI:
				updateWikiBannerVisibility(config.hideWiki());

				if (getCurrentLayout().isHorizontal() && isMinimapHidden())
				{
					updateMinimapToggleButton();
				}
				break;

			case ConfigKeys.HIDE_LOGOUT_X:
				hideLogoutX = config.hideLogout();

				if (!isClassicResizable())
				{
					hideLogout();
					updateLogoutXPosition();
				}
				break;

			default:
				HideOrbConfig orbConfig = orbHidden.getByConfig(key);
				if (orbConfig != null)
				{
					widgetManager.setTargetsHidden(
						orbConfig.getGetter().get(),
						orbConfig.getTargets()
					);
				}

				if (key.equals(ConfigKeys.HIDE_STORE)
					&& !isMinimapHidden()
					&& isLoggedIn())
				{
					updateMinimapToggleButton();
				}
				break;
		}
	}

	public boolean isSnapCornerRepositioned()
	{
		final java.awt.Point p = configManager.getConfiguration(
			ConfigGroup.Core.RUNELITE,
			ConfigKeys.Core.SNAPCORNER_PREFIX + OverlayPosition.CANVAS_TOP_RIGHT + ConfigKeys.Core.SNAPCORNER_CONFIG_LOCATION,
			java.awt.Point.class);

		return p != null;
	}

	public boolean isWikiPluginConfigEnabled()
	{
		return isPluginConfigEnabled(
			ConfigKeys.Core.WIKI_PLUGIN,
			ConfigGroup.Core.WIKI, ConfigKeys.Core.SHOW_WIKI_MINIMAP_BUTTON);
	}

	public boolean isMinimapPluginConfigEnabled()
	{
		return isPluginConfigEnabled(
			ConfigKeys.Core.MINIMAP_PLUGIN,
			ConfigGroup.Core.MINIMAP, ConfigKeys.Core.HIDE_MINIMAP);
	}

	private boolean isPluginConfigEnabled(String pluginKey, String configGroup, String configKey)
	{
		boolean isPluginEnabled = Boolean.TRUE.equals(
			configManager.getConfiguration(ConfigGroup.Core.RUNELITE, pluginKey, Boolean.class));

		boolean isConfigEnabled = Boolean.TRUE.equals(
			configManager.getConfiguration(configGroup, configKey, Boolean.class));

		return isPluginEnabled && isConfigEnabled;
	}

	public boolean useSavedPosition(Widget widget, int index)
	{
		if (isCompactLayout())
		{
			return false;
		}

		if (widget == null)
		{
			return false;
		}

		for (TargetWidget[] targets : EditManager.EDIT_TARGETS)
		{
			boolean classic = targets.length > 1 && isClassicResizable() && targets[1] != null;
			boolean fixed = targets.length > 2 && isFixedMode() && targets[2] != null;

			final TargetWidget target =
				fixed ? targets[2] : classic ? targets[1] : targets[0];

			if (editManager.blockEditing(target))
			{
				continue;
			}

			if (widget.getId() == target.getComponentId()
				&& index == target.getArrayId())
			{
				return true;
			}
		}

		return false;
	}

	public void resetTargetsSavedPosition(Binding binding, boolean remap)
	{
		if (isFixedMode())
		{
			clearSavedPosition(binding.getFixed());
			if (binding.getFixed() == null)
			{
				clearSavedPosition(binding.getModern());
				clearSavedPosition(binding.getRelated());
			}
		}
		else
		{
			clearSavedPosition(binding.getClassic());
			clearSavedPosition(binding.getModern());
			clearSavedPosition(binding.getRelated());
		}

		if (remap)
		{
			TargetWidget target = binding.get(this);
			if (target == null)
			{
				return;
			}

			widgetManager.remapTargets(target);

			Widget bound = widgetManager.getTargetWidget(target);
			if (bound != null)
			{
				Widget handler = binding.getHandler();
				if (handler != null)
				{
					int x = editManager.setHandlerX(bound, handler.getParent());
					int y = editManager.setHandlerY(bound, handler.getParent());

					binding.getHandler().setOriginalX(x);
					binding.getHandler().setOriginalY(y);
					binding.getHandler().revalidate();

					if (binding.getRelated() != null)
					{
						Widget related = widgetManager.getTargetWidget(binding.getRelated());
						related.setOriginalX(x);
						related.setOriginalY(y);
						related.revalidate();
					}
				}
			}
		}
	}

	private void clearSavedPosition(TargetWidget target)
	{
		if (target == null)
		{
			return;
		}

		String x = getSavedKey(target, ValueKey.X);
		String y = getSavedKey(target, ValueKey.Y);

		configManager.unsetConfiguration(ConfigGroup.GROUP_NAME, x);
		configManager.unsetConfiguration(ConfigGroup.GROUP_NAME, y);
	}

	//remap should only be false for profile swaps / full config resets
	public void resetAllSavedPositions(boolean remap)
	{
		for (Binding binding : bindingManager.all())
		{
			resetTargetsSavedPosition(binding, remap);
		}

		if (!remap)
		{
			//may be unnecessary since bindings are cleared on the next edit-mode
			bindingManager.clear();
		}
	}

	public void resetSavedPositionConfigs()
	{
		String[] prefixes =
			{
				ConfigKeys.CUSTOM_LAYOUT_PREFIX,
				ConfigKeys.VANILLA_LAYOUT_PREFIX,
				ConfigKeys.FIXED_LAYOUT_PREFIX
			};

		for (TargetWidget[] targets : EditManager.EDIT_TARGETS)
		{
			for (TargetWidget target : targets)
			{
				if (target == null)
				{
					continue;
				}

				for (String prefix : prefixes)
				{
					configManager.unsetConfiguration(ConfigGroup.GROUP_NAME,
						buildSavedKey(prefix, target.getComponentId(), target.getArrayId(), ValueKey.X));

					configManager.unsetConfiguration(ConfigGroup.GROUP_NAME,
						buildSavedKey(prefix, target.getComponentId(), target.getArrayId(), ValueKey.Y));
				}
			}
		}
	}

	public int getSavedPosition(Widget widget, int index, ValueKey key)
	{
		if ((isCompactLayout() && !getCurrentLayout().isCustom()))
		{
			return -1;
		}
		else if (widget.getId() == Orbs.WORLD_MAP_CONTAINER.getComponentId())
		{
			if (hideWorldMap)
			{
				return -1;
			}
		}
		else if (widget.getId() == Orbs.LOGOUT_X_ICON.getComponentId() ||
			widget.getId() == Orbs.LOGOUT_X_STONE.getComponentId())
		{
			if (hideLogoutX)
			{
				return -1;
			}
		}

		String configKey = buildSavedKey(getCurrentPrefix(), widget.getId(), index, key);
		Integer value = configManager.getConfiguration(ConfigGroup.GROUP_NAME, configKey, Integer.class);
		return value != null ? value : -1;
	}

	public void saveCurrentLayoutPosition(Widget bound, Binding binding)
	{
		if (bound == null || binding == null)
		{
			return;
		}

		int x = bound.getOriginalX();
		int y = bound.getOriginalY();

		if (isFixedMode())
		{
			savePosition(binding.getFixed(), x, y);
			if (binding.getFixed() == null)
			{
				savePosition(binding.getModern(), x, y);
			}
			return;
		}

		savePosition(binding.getClassic(), x, y);
		savePosition(binding.getModern(), x, y);
		savePosition(binding.getRelated(), x, y);
	}

	private void savePosition(TargetWidget target, int x, int y)
	{
		if (target != null)
		{
			saveConfig(getSavedKey(target, com.compactorbs.util.ValueKey.X), x);
			saveConfig(getSavedKey(target, com.compactorbs.util.ValueKey.Y), y);
		}
	}

	private String getSavedKey(TargetWidget target, ValueKey key)
	{
		int id = target.getComponentId();
		int index = target.getArrayId();
		return buildSavedKey(getCurrentPrefix(), id, index, key);
	}

	private String buildSavedKey(String prefix, int componentId, int index, ValueKey key)
	{
		String suffix = key == com.compactorbs.util.ValueKey.X ? "_x" : "_y";
		String id = componentId + "_" + index;
		return prefix + id + suffix;
	}

	public String getCurrentPrefix()
	{
		return isCompactLayout() ? ConfigKeys.CUSTOM_LAYOUT_PREFIX :
			!isFixedMode() ? ConfigKeys.VANILLA_LAYOUT_PREFIX : ConfigKeys.FIXED_LAYOUT_PREFIX;
	}

	public <T> void saveConfig(String key, T value)
	{
		configManager.setConfiguration(ConfigGroup.GROUP_NAME, key, value);
	}

	public void migrateConfigs()
	{
		Integer version = configManager.getConfiguration(
			ConfigGroup.GROUP_NAME, ConfigKeys.CONFIG_VERSION, Integer.class);

		if (version == null)
		{
			version = 0;
		}

		if (version < ConfigGroup.CONFIG_VERSION)
		{
			migrateConfigs(
				new MigrateConfig<>(
					"hotkeyToggle",
					ConfigKeys.HOTKEY_KEYBIND,
					Keybind.class,
					Function.identity()
				),
				new MigrateConfig<>(
					"hotkeyMinimap",
					ConfigKeys.HOTKEY_TOGGLE_OPTION,
					Boolean.class,
					enabled ->
					{
						if (enabled)
						{
							return HotkeyOptions.MINIMAP;
						}
						return null;
					}
				),
				new MigrateConfig<>(
					"verticalPosition",
					ConfigKeys.HORIZONTAL_ANCHOR,
					HorizontalAnchor.class,
					HorizontalAnchor::name
				),
				new MigrateConfig<>(
					"horizontalPosition",
					ConfigKeys.VERTICAL_ANCHOR,
					VerticalAnchor.class,
					VerticalAnchor::name
				),
				new MigrateConfig<>(
					"enableVerticalHeightOffset",
					ConfigKeys.VERTICAL_Y_ADJUSTMENT,
					Boolean.class,
					enabled ->
					{
						if (enabled && getCurrentLayout().isVertical())
						{
							//max offset possible from the old config
							return 35;
						}
						return null;
					})
			);

			removeOldConfigs();

			saveConfig(ConfigKeys.CONFIG_VERSION, ConfigGroup.CONFIG_VERSION);
		}
	}

	private void migrateConfigs(MigrateConfig<?, ?>... configs)
	{
		for (MigrateConfig<?, ?> config : configs)
		{
			if (config.write(configManager))
			{
				config.unset(configManager);
			}
		}
	}

	private static final String[] REMOVED_KEYS = {
		//removed in 1.1 (d2f5247)
		"hideToggle",

		//removed in 1.7.5 (e60c595)
		"enableWorldMapOverlay",
		"enableXPDropOverlay",

		//removed in 1.9.0 (83a3a2d)
		"hideCompassButton"
	};

	private void removeOldConfigs()
	{
		for (String key : REMOVED_KEYS)
		{
			configManager.unsetConfiguration(ConfigGroup.GROUP_NAME, key);
		}
	}
}
