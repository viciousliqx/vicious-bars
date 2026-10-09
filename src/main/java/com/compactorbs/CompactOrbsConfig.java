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
import java.awt.Color;
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

	// Each direction mounts the bar at a point offset from the orb icon's
	// own center (the Orb/Vial horizontal + vertical offsets below - plain
	// mountX/mountY added to iconCenterX/Y), then extends the bar outward
	// from that mount point. WEST is the default: vanilla's own horizontal
	// orb row already has each icon's native backing/tab pointing west
	// (left), so WEST needs no collar replacement at all - the real one
	// already connects. EAST mirrors it with a horizontally-flipped collar
	// copy. SOUTH is the original hand-tuned layout (vanilla never lays
	// anything out vertically for us, hence it alone repositions the
	// number/icon - see VialOverlay#renderSouth()). NORTH mirrors SOUTH
	// with an oppositely-rotated collar copy. WEST/EAST/NORTH all
	// deliberately leave the icon/number wherever vanilla already put them.
	enum BarOrientation
	{
		WEST,
		EAST,
		NORTH,
		SOUTH
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

	// First section in the panel, deliberately - everything here is
	// dev-only/experimental, kept separate from the permanent settings
	// below it so it's obvious at a glance which is which.
	@ConfigSection(
		name = "Testing",
		description = "Dev-only, experimental settings - not permanent, goes away once each one settles.",
		position = 0
	)
	String testing = "testing";

	@ConfigSection(
		name = "Zone Configuration",
		description = "",
		position = 1
	)
	String layout = "layout";

	// Dropped from the panel - verticalYAdjustment (Vertical offset) stayed
	// 0 the whole time it was visible; verticalAnchor (Vertical anchor)
	// stayed Top; horizontalAnchor (Compass fix) was never touched and -
	// per the keeper finding below, now moved here - never affected orb
	// position at all for our hardcoded layout; disableReordering (Disable
	// orb reordering) had no perceptible live effect either. All four
	// hardcoded at their CompactOrbsManager call sites instead (isAnchorTop/
	// Bottom/Left/Right, allowReordering) - same "already were" pattern as
	// layout()/enableNoClickthrough(). The enum TYPES (HorizontalAnchor/
	// VerticalAnchor) and these four ConfigKeys stay - migrateConfigs()
	// still references them for legacy key migration, a different concern
	// from whether a user-facing panel control exists.
	//
	// Confirmed dead no-op for Orbs.UNIVERSE on our hardcoded HORIZONTAL_WIDE
	// layout - OrbContainerOffset.xOffset() only adds getRightOffset(),
	// which is a constant 0 for this layout; Left has no branch at all. The
	// ONLY place this still does anything is CompassOffset.xOffset()'s
	// HORIZONTAL_WIDE-right branch, which nudges the compass replacement
	// graphic to compensate for hidden toggle icons. See
	// compact_orbs_walkthrough.md's horizontal-anchor keeper finding.

	@ConfigItem(
		keyName = ConfigKeys.HIDE_MINIMAP_WITH_SIDE_PANEL,
		name = "Hide orbs with inventory tab",
		description = "Hide the compact layout when the side panel tab is hidden (only works in resizable-modern).",
		section = layout,
		position = 7
	)
	default boolean hideMinimapWithSidePanel() { return false; }

	// No longer dev-only/temp - a confirmed staple now. F7's diagnostic
	// dump (see ViciousBarsPlugin) confirmed getOrbsParent() and
	// getMapParent() are two separate widgets nested one inside the other
	// at the EXACT same native size (228x207, original==current - plain
	// absolute sizing, safe to edit), while the orb's own direct parent
	// (ORBS_UNIVERSE) is a THIRD, different widget - original width 0,
	// size resolved some other way at render time - which is why editing
	// IT directly blew up to the screen edges from +1 and needed a
	// hide/show to visually settle. Widening just these two safe widgets
	// together is the whole fix; ORBS_UNIVERSE is left alone entirely now.
	@Range(min = 0, max = 300)
	@ConfigItem(
		keyName = ConfigKeys.BAR_ZONE_WIDTH_OFFSET,
		name = "Bar zone width",
		description = "Extra pixels added to both getOrbsParent() and getMapParent() (confirmed to be the same native size, nested widgets) - this is the actual boundary DragListener clamps orb positions against, and widening both together gives the bars more room without touching the orb's own relatively-sized parent.",
		section = layout,
		position = 8
	)
	default int barZoneWidthOffset()
	{
		return 0;
	}

	// Vertical counterpart to Bar zone width above. Same two widgets
	// (getOrbsParent(), getMapParent()), same reasoning, just the height
	// field instead of width - lets the zone grow/shrink independently on
	// each axis to fit whatever layout a user picks.
	@Range(min = 0, max = 300)
	@ConfigItem(
		keyName = ConfigKeys.BAR_ZONE_HEIGHT_OFFSET,
		name = "Bar zone height",
		description = "Extra pixels added to both getOrbsParent() and getMapParent()'s height, same mechanism as Bar zone width.",
		section = layout,
		position = 9
	)
	default int barZoneHeightOffset()
	{
		return 0;
	}

	// "Top level" (Z-order) feature tried and rolled back - drawing our own
	// copy of the logout X above the inventory panel while hiding the real
	// one via opacity worked visually, but ONLY where nothing else real
	// overlapped it. Dragged onto the toolbar's blank slot, clicks died
	// entirely - confirmed via a widget-bounds dump (F6 in ViciousBarsPlugin)
	// that SIDE_STATIC_LAYER (the whole tab-icon row's background, id
	// 10747998) sits in front of the real X in the native click hit-test
	// and has noClickThrough=true, swallowing the click before it ever
	// reaches the (still fully listener-active) real X underneath. That
	// flag isn't specific to the empty slot - it's load-bearing for the
	// WHOLE tab row, so flipping it off to let our click through would also
	// make every bit of blank space around every other tab icon click
	// through to the 3D world. Not worth that global side effect for this
	// one slot - rolled back rather than ship it. LogoutXOverlay.java
	// deleted; ConfigKeys.LOGOUT_X_ON_TOP removed too.

	// No longer dev-only/temp - confirmed working baseline (anchor-point
	// mounting + four-direction orientation) as of live testing. This
	// section is the single master layout control for every bar (HP,
	// Prayer, Run, Spec) - they all read the exact same CompactOrbsConfig
	// instance, so there's no per-bar config surface, just this one.
	@ConfigSection(
		name = "Global Bar Config",
		description = "Controls every bar (HP, Prayer, Run, Spec) identically - one shared layout, not a per-bar setting.",
		position = 2
	)
	String bar = "bar";

	@ConfigItem(
		keyName = ConfigKeys.BAR_ORIENTATION,
		name = "Bar orientation",
		description = "Which way the bar extends from its mount point (the Orb/Vial offsets below) - West is the default and matches vanilla's own native collar direction; East/North mirror it with a flipped/rotated collar; South is the original hand-built vertical layout.",
		section = bar,
		position = 0
	)
	default BarOrientation barOrientation()
	{
		return BarOrientation.WEST;
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

	@Range(min = 10, max = 40)
	@ConfigItem(
		keyName = ConfigKeys.BAR_WIDTH,
		name = "Width",
		description = "How thick the bar is, across its short axis.",
		section = bar,
		position = 2
	)
	default int barWidth()
	{
		return 20;
	}

	// Mounts the bar's anchor point this many pixels right (+) or left (-)
	// of the orb icon's own center - the bar then extends outward from
	// that point in whichever direction Bar orientation picks. Dynamically
	// capped per-orientation to the bar's own length/width - see
	// VialOverlay#clampOffset()'s javadoc.
	@Range(min = -120, max = 120)
	@ConfigItem(
		keyName = ConfigKeys.BAR_HORIZONTAL_OFFSET,
		name = "Orb/Vial horizontal offset",
		description = "Mounts the bar this many pixels right (+) or left (-) of the orb icon's own center, before it extends outward in whichever direction Bar orientation picks.",
		section = bar,
		position = 3
	)
	default int barHorizontalOffset()
	{
		return 0;
	}

	// Same reasoning as Orb/Vial horizontal offset above.
	@Range(min = -120, max = 120)
	@ConfigItem(
		keyName = ConfigKeys.BAR_VERTICAL_OFFSET,
		name = "Orb/Vial vertical offset",
		description = "Mounts the bar this many pixels down (+) or up (-) of the orb icon's own center, before it extends outward in whichever direction Bar orientation picks.",
		section = bar,
		position = 4
	)
	default int barVerticalOffset()
	{
		return 0;
	}

	// The inner vial fill has no slider of its own - it always fills
	// whatever's left inside the frame.
	@Range(min = 1, max = 10)
	@ConfigItem(
		keyName = ConfigKeys.BAR_FRAME_WIDTH,
		name = "Frame thickness",
		description = "Thickness of the stone frame ring. The vial fill always fills whatever's left inside it.",
		section = bar,
		position = 5
	)
	default int barFrameWidth()
	{
		return 2;
	}

	@Range(min = 0, max = 100)
	@ConfigItem(
		keyName = ConfigKeys.BAR_OPACITY,
		name = "Opacity",
		description = "Opacity of the vial fill. The regen outline (below) always reads 30% more opaque than this. Doesn't affect the frame - see Frame opacity.",
		section = bar,
		position = 6
	)
	default int barOpacity()
	{
		return 100;
	}

	@Range(min = 0, max = 100)
	@ConfigItem(
		keyName = ConfigKeys.BAR_FRAME_OPACITY,
		name = "Frame opacity",
		description = "Opacity of the stone frame ring, independent of the vial fill's own Opacity above.",
		section = bar,
		position = 7
	)
	default int barFrameOpacity()
	{
		return 100;
	}

	@ConfigItem(
		keyName = ConfigKeys.DRAIN_RING_DOMINANT,
		name = "Drain ring takes precedence",
		description = "When a bar has both a regen ring and a drain ring showing at once, the drain ring is drawn on top (and reads more opaque) by default. Turn off to have the regen ring take precedence instead.",
		section = bar,
		position = 8
	)
	default boolean drainRingDominant()
	{
		return true;
	}

	@Range(min = 1, max = 10)
	@ConfigItem(
		keyName = ConfigKeys.BAR_REGEN_OUTLINE_WIDTH,
		name = "Regen outline width",
		description = "Thickness of the regen-progress outline traced along the inside of the vial cavity.",
		section = bar,
		position = 9
	)
	default int barRegenOutlineWidth()
	{
		return 1;
	}

	@ConfigSection(
		name = "Hitpoints",
		description = "",
		position = 3
	)
	String hpSection = "hpSection";

	@ConfigItem(
		keyName = ConfigKeys.HP_SHOW_REGEN,
		name = "Show regen",
		description = "Highlight the frame ring, growing toward the next hitpoint regenerated - halves in period under Rapid Heal, off entirely at full hitpoints.",
		section = hpSection,
		position = 0
	)
	default boolean hpShowRegen()
	{
		return true;
	}

	@ConfigItem(
		keyName = ConfigKeys.HP_SHOW_DRAIN,
		name = "Show drain",
		description = "Highlight the frame ring while poisoned or envenomed, counting down to the next damage tick - starts full right after a hit and drains to empty right before the next one, the inverse of the regen ring above.",
		section = hpSection,
		position = 1
	)
	default boolean hpShowDrain()
	{
		return true;
	}

	@ConfigItem(
		keyName = ConfigKeys.HP_NORMAL_COLOR,
		name = "Normal color",
		description = "Bar color while healthy.",
		section = hpSection,
		position = 2
	)
	default Color hpNormalColor()
	{
		return new Color(220, 40, 40);
	}

	@ConfigItem(
		keyName = ConfigKeys.HP_POISONED_COLOR,
		name = "Poisoned color",
		description = "Bar color while poisoned.",
		section = hpSection,
		position = 3
	)
	default Color hpPoisonedColor()
	{
		return new Color(90, 170, 60);
	}

	@ConfigItem(
		keyName = ConfigKeys.HP_ENVENOMED_COLOR,
		name = "Envenomed color",
		description = "Bar color while envenomed (lethal poison - won't naturally wear off).",
		section = hpSection,
		position = 4
	)
	default Color hpEnvenomedColor()
	{
		return new Color(60, 120, 40);
	}

	@ConfigItem(
		keyName = ConfigKeys.HP_DISEASED_COLOR,
		name = "Diseased color",
		description = "Bar color while diseased.",
		section = hpSection,
		position = 5
	)
	default Color hpDiseasedColor()
	{
		return new Color(150, 110, 40);
	}

	@ConfigItem(
		keyName = ConfigKeys.HP_REGEN_COLOR,
		name = "Regen ring color",
		description = "Regen outline color, growing toward the next hitpoint regenerated. Independent of the drain ring below, which has its own color (locked to the Poisoned/Envenomed color above) and its own toggle.",
		section = hpSection,
		position = 6
	)
	default Color hpRegenColor()
	{
		return new Color(220, 40, 40);
	}

	@ConfigSection(
		name = "Prayer",
		description = "",
		position = 4
	)
	String prayerSection = "prayerSection";

	@ConfigItem(
		keyName = ConfigKeys.PRAYER_SHOW_REGEN,
		name = "Show regen",
		description = "Highlight the frame ring while a Prayer regeneration potion's effect is active, growing toward its next restored prayer point - prayer has no passive regen otherwise, so the ring stays off outside that potion's effect.",
		section = prayerSection,
		position = 0
	)
	default boolean prayerShowRegen()
	{
		return true;
	}

	@ConfigItem(
		keyName = ConfigKeys.PRAYER_SHOW_DRAIN,
		name = "Show drain",
		description = "Highlight the frame ring while any prayer is active, counting down to the next prayer point drained - starts full right after a point drains and empties right before the next one, the inverse of the regen ring above. Paces itself off the real gap between your last two point losses rather than a formula, so it takes one point loss after activating a prayer (or changing gear) to start showing.",
		section = prayerSection,
		position = 1
	)
	default boolean prayerShowDrain()
	{
		return true;
	}

	@ConfigItem(
		keyName = ConfigKeys.PRAYER_ACTIVE_COLOR,
		name = "Active color",
		description = "Bar color while any prayer is active.",
		section = prayerSection,
		position = 2
	)
	default Color prayerActiveColor()
	{
		return new Color(60, 170, 220);
	}

	@ConfigItem(
		keyName = ConfigKeys.PRAYER_INACTIVE_COLOR,
		name = "Inactive color",
		description = "Bar color while no prayer is active.",
		section = prayerSection,
		position = 3
	)
	default Color prayerInactiveColor()
	{
		return new Color(25, 35, 80);
	}

	@ConfigItem(
		keyName = ConfigKeys.PRAYER_REGEN_COLOR,
		name = "Regen ring color",
		description = "Regen outline color while a Prayer regeneration potion's effect is active.",
		section = prayerSection,
		position = 4
	)
	default Color prayerRegenColor()
	{
		return new Color(60, 170, 220);
	}

	@ConfigSection(
		name = "Special Attack",
		description = "",
		position = 5
	)
	String specSection = "specSection";

	@ConfigItem(
		keyName = ConfigKeys.SPEC_SHOW_REGEN,
		name = "Show regen",
		description = "Highlight the frame ring, growing toward the next 10% of special attack energy restored - halves in period while wearing a Lightbearer, resets at full energy.",
		section = specSection,
		position = 0
	)
	default boolean specShowRegen()
	{
		return true;
	}

	@ConfigItem(
		keyName = ConfigKeys.SPEC_DEFAULT_COLOR,
		name = "Default color",
		description = "Bar color while Special Attack isn't queued.",
		section = specSection,
		position = 1
	)
	default Color specDefaultColor()
	{
		return new Color(220, 40, 180);
	}

	@ConfigItem(
		keyName = ConfigKeys.SPEC_ACTIVE_COLOR,
		name = "Active color",
		description = "Bar color while Special Attack is queued (armed for your next hit).",
		section = specSection,
		position = 2
	)
	default Color specActiveColor()
	{
		return new Color(255, 255, 255);
	}

	@ConfigItem(
		keyName = ConfigKeys.SPEC_REGEN_COLOR,
		name = "Regen ring color",
		description = "Regen outline color, growing toward the next 10% of special attack energy restored.",
		section = specSection,
		position = 3
	)
	default Color specRegenColor()
	{
		return new Color(220, 40, 180);
	}

	@ConfigSection(
		name = "Run Energy",
		description = "",
		position = 6
	)
	String runSection = "runSection";

	@ConfigItem(
		keyName = ConfigKeys.RUN_SHOW_REGEN,
		name = "Show regen",
		description = "Highlight the frame ring with run energy's continuous per-tick regen - tracks smoothly rather than jumping, matching OSRS's 2025 run energy rework.",
		section = runSection,
		position = 0
	)
	default boolean runShowRegen()
	{
		return true;
	}

	@ConfigItem(
		keyName = ConfigKeys.RUN_DEFAULT_COLOR,
		name = "Default color",
		description = "Bar color while Run is toggled off.",
		section = runSection,
		position = 1
	)
	default Color runDefaultColor()
	{
		return new Color(220, 160, 40);
	}

	@ConfigItem(
		keyName = ConfigKeys.RUN_ACTIVE_COLOR,
		name = "Active color",
		description = "Bar color while Run is toggled on.",
		section = runSection,
		position = 2
	)
	default Color runActiveColor()
	{
		return new Color(212, 175, 55);
	}

	@ConfigItem(
		keyName = ConfigKeys.RUN_REGEN_COLOR,
		name = "Regen ring color",
		description = "Regen outline color, tracking run energy's continuous per-tick regen.",
		section = runSection,
		position = 3
	)
	default Color runRegenColor()
	{
		return new Color(220, 160, 40);
	}

	@ConfigSection(
		name = "Hotkey",
		description = "",
		closedByDefault = true,
		position = 7
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
		name = "Orb Layout",
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
