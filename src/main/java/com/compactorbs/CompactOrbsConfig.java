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

import static com.compactorbs.CompactOrbsConstants.ConfigGroup.GROUP_NAME;
import com.compactorbs.CompactOrbsConstants.ConfigKeys;
import java.awt.event.KeyEvent;
import lombok.Getter;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Keybind;
import net.runelite.client.config.Range;

@ConfigGroup(GROUP_NAME)
public interface CompactOrbsConfig extends Config
{
	enum HorizontalAnchor
	{
		LEFT, RIGHT;

		public boolean isLeft()
		{
			return this == LEFT;
		}

		public boolean isRight()
		{
			return this == RIGHT;
		}
	}

	enum VerticalAnchor
	{
		TOP, BOTTOM;

		public boolean isTop()
		{
			return this == TOP;
		}

		public boolean isBottom()
		{
			return this == BOTTOM;
		}
	}

	@Getter
	enum HotkeyOptions
	{
		MINIMAP,
		EDIT_MODE
	}

	// Vicious Bars only ever has ONE layout (hardcoded as
	// CompactOrbsLayout.HORIZONTAL_WIDE directly in
	// CompactOrbsManager#getCurrentLayout()) and always prevents orb
	// clickthrough (hardcoded true at its two call sites) - no config
	// surface for either anymore. Deliberately NOT a no-@ConfigItem default
	// method here: RuneLite's config proxy returns null (not the
	// interface's own default body) for a method with no @ConfigItem,
	// which NPEs the instant anything calls it - confirmed live, broke
	// plugin startup entirely. Hardcoding at the call site instead avoids
	// the Config interface for these two entirely.

	@ConfigSection(
		name = "Layout Options",
		description = "",
		position = 1
	)
	String layout = "layout";

	@Range(min = 0, max = 200)
	@ConfigItem(
		keyName = ConfigKeys.VERTICAL_Y_ADJUSTMENT,
		name = "Vertical offset",
		description = "Reduce the vertical position of the current layout by the set value (min: 0, max:200)",
		section = layout,
		position = 1
	)
	default int verticalYAdjustment()
	{
		return 0;
	}

	@ConfigItem(
		keyName = ConfigKeys.VERTICAL_ANCHOR,
		name = "Vertical anchor",
		description = "Where the layout will snap to, and the direction orbs move while reordering",
		section = layout,
		position = 2
	)
	default VerticalAnchor verticalAnchor()
	{
		return VerticalAnchor.BOTTOM;
	}

	// Confirmed dead no-op for Orbs.UNIVERSE on our hardcoded HORIZONTAL_WIDE
	// layout - OrbContainerOffset.xOffset() only adds getRightOffset(),
	// which is a constant 0 for this layout; Left has no branch at all. The
	// ONLY place this still does anything is CompassOffset.xOffset()'s
	// HORIZONTAL_WIDE-right branch, which nudges the compass replacement
	// graphic to compensate for hidden toggle icons - hence the renamed
	// label below. See compact_orbs_walkthrough.md's horizontal-anchor
	// keeper finding. Once a design settles, this can still be hardcoded
	// away the same way layout()/enableNoClickthrough() were.
	@ConfigItem(
		keyName = ConfigKeys.HORIZONTAL_ANCHOR,
		name = "Compass fix",
		description = "Has no effect on orb position - only nudges the compass replacement graphic to compensate for hidden toggle icons (minimap button, XP drops, world map, wiki)",
		section = layout,
		position = 3
	)
	default HorizontalAnchor horizontalAnchor()
	{
		return HorizontalAnchor.RIGHT;
	}

	@ConfigItem(
		keyName = ConfigKeys.DISABLE_REORDERING,
		name = "Disable orb reordering",
		description = "Disable the reordering logic when orbs are hidden",
		section = layout,
		position = 4
	)
	default boolean disableReordering()
	{
		return false;
	}

	@ConfigItem(
		keyName = ConfigKeys.LEAVE_EMPTY_SPACE,
		name = "Leave empty space",
		description = "Prevent other elements from shifting towards orbs that have been reordered (retains the gap between)",
		section = layout,
		position = 5
	)
	default boolean leaveEmptySpace()
	{
		return false;
	}

	@ConfigItem(
		keyName = ConfigKeys.HIDE_MINIMAP_WITH_SIDE_PANEL,
		name = "Hide orbs with inventory tab",
		description = "Hide the compact layout when the side panel tab is hidden (only works in resizable-modern).",
		section = layout,
		position = 7
	)
	default boolean hideMinimapWithSidePanel() { return false; }

	@ConfigSection(
		name = "Bar",
		description = "",
		position = 2
	)
	String bar = "bar";

	// Dev-only, temporary - exists purely so the width can be dialed in
	// live in-game instead of guessed blind through code changes. Once a
	// value is settled it gets hardcoded in HpVialOverlay and this item
	// removed entirely, the same way ORB_LAYOUT/ENABLE_NO_CLICKTHROUGH
	// already were - the stone cap's own fixed size means width isn't
	// meant to be a real per-user setting.
	@Range(min = 10, max = 40)
	@ConfigItem(
		keyName = ConfigKeys.BAR_WIDTH,
		name = "Width (temp)",
		description = "Dev-only - dial this in, then report the final value back to hardcode. Goes away once settled.",
		section = bar,
		position = 0
	)
	default int barWidth()
	{
		return 20;
	}

	@Range(min = 20, max = 400)
	@ConfigItem(
		keyName = ConfigKeys.BAR_LENGTH,
		name = "Length",
		description = "How long the bar is, end to end.",
		section = bar,
		position = 1
	)
	default int barLength()
	{
		return 60;
	}

	// Dev-only, temporary - same reasoning as Width above.
	@Range(min = -120, max = 40)
	@ConfigItem(
		keyName = ConfigKeys.BAR_VERTICAL_OFFSET,
		name = "Orb/Vial offset (temp)",
		description = "Dev-only - nudges the bar down (+) away from the collar, or up (-) into more overlap with its tab. Goes away once settled.",
		section = bar,
		position = 2
	)
	default int barVerticalOffset()
	{
		return 0;
	}

	// Dev-only, temporary - same reasoning as Orb/Vial offset above.
	@Range(min = -40, max = 40)
	@ConfigItem(
		keyName = ConfigKeys.BAR_HORIZONTAL_OFFSET,
		name = "Orb/Bar horizontal offset (temp)",
		description = "Dev-only - nudges the bar left (-) or right (+), independent of the collar, which stays centered on the icon regardless. Goes away once settled.",
		section = bar,
		position = 4
	)
	default int barHorizontalOffset()
	{
		return 0;
	}


	// Dev-only, temporary - same reasoning as Width above. The inner vial
	// fill has no slider of its own - it always fills whatever's left
	// inside the frame, so this one control governs both.
	@Range(min = 1, max = 10)
	@ConfigItem(
		keyName = ConfigKeys.BAR_FRAME_WIDTH,
		name = "Frame width (temp)",
		description = "Dev-only - thickness of the stone frame ring. The vial fill always fills whatever's left inside it. Goes away once settled.",
		section = bar,
		position = 3
	)
	default int barFrameWidth()
	{
		return 2;
	}

	@ConfigSection(
		name = "Hotkey",
		description = "",
		closedByDefault = true,
		position = 3
	)
	String hotkey = "hotkey";

	@ConfigItem(
		keyName = ConfigKeys.HOTKEY_KEYBIND,
		name = "Keybind",
		description = "Keybind used to toggle the option selected option <br>" +
			"-Warning: a modified is recommended (e.g. Shift, Ctrl, Alt)",
		section = hotkey,
		position = 0
	)
	default Keybind hotkeyKeybind()
	{
		return new Keybind(KeyEvent.VK_INSERT, KeyEvent.SHIFT_DOWN_MASK);
	}

	@ConfigItem(
		keyName = ConfigKeys.HOTKEY_TOGGLE_OPTION,
		name = "Select toggle",
		description = "Select what the hotkey will control",
		section = hotkey,
		position = 1
	)
	default HotkeyOptions toggleOption()
	{
		return HotkeyOptions.MINIMAP;
	}

	@ConfigSection(
		name = "Orb Visibility",
		description = "",
		closedByDefault = true,
		position = 100
	)
	String hideAndSwapUpdate = "hideAndSwapUpdate";
	@ConfigItem(
		keyName = "hideAndSwapUpdate",
		name = "<html>Orb hiding has been moved to the in-game Edit-mode. <br/> <br/>" +
			"Enable Edit-mode (either works): <br/>" +
			"- Right-click the toggle-button (eye) <br/>" +
			"- Configure the Hotkey for Edit-mode </html>",
		description = "",
		section = hideAndSwapUpdate
	)
	void hideAndSwapUpdate();

	//hidden from the config panel
	@ConfigItem(
		keyName = ConfigKeys.MINIMAP,
		name = "Hide minimap",
		description = "",
		hidden = true
	)
	default boolean hideMinimap()
	{
		return false;
	}

	@ConfigItem(
		keyName = ConfigKeys.COMPASS,
		name = "Hide compass",
		description = "",
		hidden = true
	)
	default boolean hideCompass()
	{
		return false;
	}

	//orb hiding
	@ConfigItem(
		keyName = ConfigKeys.HIDE_HP,
		name = "Hide Hp",
		description = "",
		hidden = true
	)
	default boolean hideHp()
	{
		return false;
	}

	@ConfigItem(
		keyName = ConfigKeys.HIDE_PRAYER,
		name = "Hide Prayer",
		description = "",
		hidden = true
	)
	default boolean hidePray()
	{
		return false;
	}

	@ConfigItem(
		keyName = ConfigKeys.HIDE_RUN,
		name = "Hide Run",
		description = "",
		hidden = true
	)
	default boolean hideRun()
	{
		return false;
	}

	@ConfigItem(
		keyName = ConfigKeys.HIDE_SPEC,
		name = "Hide Special",
		description = "",
		hidden = true
	)
	default boolean hideSpec()
	{
		return false;
	}

	@ConfigItem(
		keyName = ConfigKeys.HIDE_XP,
		name = "Hide XP",
		description = "",
		hidden = true
	)
	default boolean hideXp()
	{
		return false;
	}

	@ConfigItem(
		keyName = ConfigKeys.HIDE_WORLD,
		name = "Hide World Map",
		description = "",
		hidden = true
	)
	default boolean hideWorld()
	{
		return false;
	}

	@ConfigItem(
		keyName = ConfigKeys.HIDE_STORE,
		name = "Hide Store",
		description = "",
		hidden = true
	)
	default boolean hideStore()
	{
		return false;
	}

	@ConfigItem(
		keyName = ConfigKeys.HIDE_ACTIVITY,
		name = "Hide Activity Advisor",
		description = "",
		hidden = true
	)
	default boolean hideActivity()
	{
		return false;
	}

	@ConfigItem(
		keyName = ConfigKeys.HIDE_WIKI,
		name = "Hide Wiki banner",
		description = "",
		hidden = true
	)
	default boolean hideWiki()
	{
		return false;
	}

	@ConfigItem(
		keyName = ConfigKeys.HIDE_LOGOUT_X,
		name = "Hide Logout-X",
		description = "",
		hidden = true
	)
	default boolean hideLogout()
	{
		return false;
	}

	@ConfigItem(
		keyName = ConfigKeys.HIDE_GRID,
		name = "Hide Grid Master (Legacy)",
		description = "",
		hidden = true
	)
	default boolean hideGrid()
	{
		return false;
	}

}
