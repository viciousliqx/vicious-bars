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
import com.viciousliqx.viciousbars.CounterFormat;
import com.viciousliqx.viciousbars.DockSide;
import com.viciousliqx.viciousbars.Justification;
import com.viciousliqx.viciousbars.Orientation;
import com.viciousliqx.viciousbars.PlacementMode;
import com.viciousliqx.viciousbars.WarmthVisibility;
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

	/** Per-bar threshold-glow mode - see the per-bar "Threshold mode" config items below. */
	enum ThresholdMode
	{
		DISABLED,
		ACTIVE,
		COMBAT_ONLY
	}

	/** Per-bar threshold-glow direction - see the per-bar "Threshold direction" config items below. */
	enum ThresholdDirection
	{
		AT_OR_ABOVE,
		AT_OR_BELOW
	}

	/** Per-bar regen/drain ring dominance - see the per-bar "Regen/drain priority" config items below. Replaces the old plain drainRingDominant() boolean. */
	enum RegenDrainPriority
	{
		REGEN,
		DRAIN
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

	// Empty on purpose - RuneLite's config panel has no real nested
	// sections, so this simulates one: an always-expanded label with no
	// items of its own, immediately followed by the actual (collapsed)
	// sections it's grouping - Global Bar Config through Run Energy below.
	@ConfigSection(
		name = "Main Bar Configuration",
		description = "Everything through Run Energy below configures the interactive main bars (tied to the real HP/Prayer/Spec/Run orbs) - expand a section as needed.",
		position = 0
	)
	String mainBarConfig = "mainBarConfig";

	@ConfigSection(
		name = "Zone Configuration",
		description = "",
		closedByDefault = true,
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

	@ConfigSection(
		name = "Hitpoints",
		description = "",
		closedByDefault = true,
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

	@ConfigItem(
		keyName = ConfigKeys.HP_THRESHOLD_MODE,
		name = "Threshold mode",
		description = "Disabled: off. Active: the icon pulses once the threshold condition below is met. Combat only: same, but only while you're in combat.",
		section = hpSection,
		position = 7
	)
	default ThresholdMode hpThresholdMode()
	{
		return ThresholdMode.DISABLED;
	}

	@ConfigItem(
		keyName = ConfigKeys.HP_THRESHOLD_DIRECTION,
		name = "Threshold direction",
		description = "At or below: warns when hitpoints drop too low (the usual case). At or above: warns when overhealed/too high instead (e.g. to avoid wasting an incoming heal).",
		section = hpSection,
		position = 8
	)
	default ThresholdDirection hpThresholdDirection()
	{
		return ThresholdDirection.AT_OR_BELOW;
	}

	@Range(min = 0, max = 100)
	@ConfigItem(
		keyName = ConfigKeys.HP_THRESHOLD,
		name = "Threshold",
		description = "Hitpoints percentage the icon pulses at, per Threshold direction above.",
		section = hpSection,
		position = 9
	)
	default int hpThreshold()
	{
		return 25;
	}

	@ConfigItem(
		keyName = ConfigKeys.HP_THRESHOLD_COLOR,
		name = "Threshold glow color",
		description = "Color the icon pulses in once the threshold above is reached.",
		section = hpSection,
		position = 10
	)
	default Color hpThresholdColor()
	{
		return new Color(220, 30, 30, 160);
	}

	@ConfigItem(
		keyName = ConfigKeys.HP_HIDE_AUX_WHEN_MAIN_SHOWN,
		name = "Hide matching aux bar",
		description = "While this main bar is shown (Compact mode active), hide the auxiliary HP bar too. Turn off to run both at once.",
		section = hpSection,
		position = 11
	)
	default boolean hpHideAuxWhenMainShown()
	{
		return true;
	}

	@ConfigItem(
		keyName = ConfigKeys.HP_ORIENTATION,
		name = "Orientation",
		description = "Which way this bar extends from its mount point (the Orb/Vial offsets in Global Bar Config) - West is the default and matches vanilla's own native collar direction; East/North mirror it with a flipped/rotated collar; South is the original hand-built vertical layout.",
		section = hpSection,
		position = 12
	)
	default BarOrientation hpOrientation()
	{
		return BarOrientation.WEST;
	}

	@Range(min = 20, max = 400)
	@ConfigItem(
		keyName = ConfigKeys.HP_LENGTH,
		name = "Length",
		description = "How long the bar is, end to end.",
		section = hpSection,
		position = 13
	)
	default int hpLength()
	{
		return 60;
	}

	@Range(min = 10, max = 40)
	@ConfigItem(
		keyName = ConfigKeys.HP_WIDTH,
		name = "Width",
		description = "How thick the bar is, across its short axis.",
		section = hpSection,
		position = 14
	)
	default int hpWidth()
	{
		return 20;
	}

	@ConfigItem(
		keyName = ConfigKeys.HP_HORIZONTAL_OFFSET,
		name = "Orb/Vial horizontal offset",
		description = "Mounts the bar this many pixels right (+) or left (-) of the orb icon's own center, before it extends outward per Orientation above.",
		section = hpSection,
		position = 15
	)
	default int hpHorizontalOffset()
	{
		return 0;
	}

	@ConfigItem(
		keyName = ConfigKeys.HP_VERTICAL_OFFSET,
		name = "Orb/Vial vertical offset",
		description = "Mounts the bar this many pixels down (+) or up (-) of the orb icon's own center, before it extends outward per Orientation above.",
		section = hpSection,
		position = 16
	)
	default int hpVerticalOffset()
	{
		return 0;
	}

	@Range(min = 1, max = 10)
	@ConfigItem(
		keyName = ConfigKeys.HP_FRAME_THICKNESS,
		name = "Frame thickness",
		description = "Thickness of the stone frame ring. The vial fill always fills whatever's left inside it.",
		section = hpSection,
		position = 17
	)
	default int hpFrameThickness()
	{
		return 2;
	}

	@Range(min = 1, max = 10)
	@ConfigItem(
		keyName = ConfigKeys.HP_FRAME_SHADOW_WIDTH,
		name = "Frame shadow width",
		description = "Stroke width of the dark outline traced around the whole bar's outer edge, independent of Frame thickness above.",
		section = hpSection,
		position = 18
	)
	default int hpFrameShadowWidth()
	{
		return 1;
	}

	@Range(min = 0, max = 100)
	@ConfigItem(
		keyName = ConfigKeys.HP_BAR_OPACITY,
		name = "Bar opacity",
		description = "Opacity of the vial fill. The regen/drain rings read more opaque than this. Doesn't affect the frame - see Frame opacity.",
		section = hpSection,
		position = 19
	)
	default int hpBarOpacity()
	{
		return 100;
	}

	@Range(min = 0, max = 100)
	@ConfigItem(
		keyName = ConfigKeys.HP_FRAME_OPACITY,
		name = "Frame opacity",
		description = "Opacity of the stone frame ring, independent of Bar opacity above.",
		section = hpSection,
		position = 20
	)
	default int hpFrameOpacity()
	{
		return 100;
	}

	@ConfigItem(
		keyName = ConfigKeys.HP_REGEN_DRAIN_PRIORITY,
		name = "Regen/drain priority",
		description = "Which ring draws on top (and reads more opaque) when this bar has both a regen ring and a drain ring showing at once.",
		section = hpSection,
		position = 21
	)
	default RegenDrainPriority hpRegenDrainPriority()
	{
		return RegenDrainPriority.DRAIN;
	}

	@Range(min = 1, max = 10)
	@ConfigItem(
		keyName = ConfigKeys.HP_REGEN_DRAIN_RING_SIZE,
		name = "Drain/regen ring size",
		description = "Thickness of the regen/drain outline traced along the inside of the vial cavity.",
		section = hpSection,
		position = 22
	)
	default int hpRegenDrainRingSize()
	{
		return 1;
	}

	@ConfigItem(
		keyName = ConfigKeys.HP_REVERSE_BAR_FILL,
		name = "Reverse bar fill",
		description = "Flips which end the vial fill empties from first - normally the near-icon end, with the remaining \"liquid\" receding toward the far end as the value drops.",
		section = hpSection,
		position = 23
	)
	default boolean hpReverseBarFill()
	{
		return false;
	}

	@ConfigItem(
		keyName = ConfigKeys.HP_REVERSE_REGEN_RING,
		name = "Reverse regen ring",
		description = "Flips which end the regen ring grows from, independently of the bar fill above.",
		section = hpSection,
		position = 24
	)
	default boolean hpReverseRegenRing()
	{
		return false;
	}

	@ConfigItem(
		keyName = ConfigKeys.HP_REVERSE_DRAIN_RING,
		name = "Reverse drain ring",
		description = "Flips which end the drain ring shrinks toward, independently of the bar fill and regen ring above.",
		section = hpSection,
		position = 25
	)
	default boolean hpReverseDrainRing()
	{
		return false;
	}

	@ConfigSection(
		name = "Prayer",
		description = "",
		closedByDefault = true,
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

	@ConfigItem(
		keyName = ConfigKeys.PRAYER_THRESHOLD_MODE,
		name = "Threshold mode",
		description = "Disabled: off. Active: the icon pulses once the threshold condition below is met. Combat only: same, but only while you're in combat.",
		section = prayerSection,
		position = 5
	)
	default ThresholdMode prayerThresholdMode()
	{
		return ThresholdMode.DISABLED;
	}

	@ConfigItem(
		keyName = ConfigKeys.PRAYER_THRESHOLD_DIRECTION,
		name = "Threshold direction",
		description = "At or below: warns when prayer points drop too low (the usual case). At or above: warns when too high instead.",
		section = prayerSection,
		position = 6
	)
	default ThresholdDirection prayerThresholdDirection()
	{
		return ThresholdDirection.AT_OR_BELOW;
	}

	@Range(min = 0, max = 100)
	@ConfigItem(
		keyName = ConfigKeys.PRAYER_THRESHOLD,
		name = "Threshold",
		description = "Prayer points percentage the icon pulses at, per Threshold direction above.",
		section = prayerSection,
		position = 7
	)
	default int prayerThreshold()
	{
		return 25;
	}

	@ConfigItem(
		keyName = ConfigKeys.PRAYER_THRESHOLD_COLOR,
		name = "Threshold glow color",
		description = "Color the icon pulses in once the threshold above is reached.",
		section = prayerSection,
		position = 8
	)
	default Color prayerThresholdColor()
	{
		return new Color(220, 30, 30, 160);
	}

	@ConfigItem(
		keyName = ConfigKeys.PRAYER_HIDE_AUX_WHEN_MAIN_SHOWN,
		name = "Hide matching aux bar",
		description = "While this main bar is shown (Compact mode active), hide the auxiliary Prayer bar too. Turn off to run both at once.",
		section = prayerSection,
		position = 9
	)
	default boolean prayerHideAuxWhenMainShown()
	{
		return true;
	}

	@ConfigItem(
		keyName = ConfigKeys.PRAYER_ORIENTATION,
		name = "Orientation",
		description = "Which way this bar extends from its mount point (the Orb/Vial offsets in Global Bar Config) - West is the default and matches vanilla's own native collar direction; East/North mirror it with a flipped/rotated collar; South is the original hand-built vertical layout.",
		section = prayerSection,
		position = 10
	)
	default BarOrientation prayerOrientation()
	{
		return BarOrientation.WEST;
	}

	@Range(min = 20, max = 400)
	@ConfigItem(
		keyName = ConfigKeys.PRAYER_LENGTH,
		name = "Length",
		description = "How long the bar is, end to end.",
		section = prayerSection,
		position = 11
	)
	default int prayerLength()
	{
		return 60;
	}

	@Range(min = 10, max = 40)
	@ConfigItem(
		keyName = ConfigKeys.PRAYER_WIDTH,
		name = "Width",
		description = "How thick the bar is, across its short axis.",
		section = prayerSection,
		position = 12
	)
	default int prayerWidth()
	{
		return 20;
	}

	@ConfigItem(
		keyName = ConfigKeys.PRAYER_HORIZONTAL_OFFSET,
		name = "Orb/Vial horizontal offset",
		description = "Mounts the bar this many pixels right (+) or left (-) of the orb icon's own center, before it extends outward per Orientation above.",
		section = prayerSection,
		position = 13
	)
	default int prayerHorizontalOffset()
	{
		return 0;
	}

	@ConfigItem(
		keyName = ConfigKeys.PRAYER_VERTICAL_OFFSET,
		name = "Orb/Vial vertical offset",
		description = "Mounts the bar this many pixels down (+) or up (-) of the orb icon's own center, before it extends outward per Orientation above.",
		section = prayerSection,
		position = 14
	)
	default int prayerVerticalOffset()
	{
		return 0;
	}

	@Range(min = 1, max = 10)
	@ConfigItem(
		keyName = ConfigKeys.PRAYER_FRAME_THICKNESS,
		name = "Frame thickness",
		description = "Thickness of the stone frame ring. The vial fill always fills whatever's left inside it.",
		section = prayerSection,
		position = 15
	)
	default int prayerFrameThickness()
	{
		return 2;
	}

	@Range(min = 1, max = 10)
	@ConfigItem(
		keyName = ConfigKeys.PRAYER_FRAME_SHADOW_WIDTH,
		name = "Frame shadow width",
		description = "Stroke width of the dark outline traced around the whole bar's outer edge, independent of Frame thickness above.",
		section = prayerSection,
		position = 16
	)
	default int prayerFrameShadowWidth()
	{
		return 1;
	}

	@Range(min = 0, max = 100)
	@ConfigItem(
		keyName = ConfigKeys.PRAYER_BAR_OPACITY,
		name = "Bar opacity",
		description = "Opacity of the vial fill. The regen/drain rings read more opaque than this. Doesn't affect the frame - see Frame opacity.",
		section = prayerSection,
		position = 17
	)
	default int prayerBarOpacity()
	{
		return 100;
	}

	@Range(min = 0, max = 100)
	@ConfigItem(
		keyName = ConfigKeys.PRAYER_FRAME_OPACITY,
		name = "Frame opacity",
		description = "Opacity of the stone frame ring, independent of Bar opacity above.",
		section = prayerSection,
		position = 18
	)
	default int prayerFrameOpacity()
	{
		return 100;
	}

	@ConfigItem(
		keyName = ConfigKeys.PRAYER_REGEN_DRAIN_PRIORITY,
		name = "Regen/drain priority",
		description = "Which ring draws on top (and reads more opaque) when this bar has both a regen ring and a drain ring showing at once.",
		section = prayerSection,
		position = 19
	)
	default RegenDrainPriority prayerRegenDrainPriority()
	{
		return RegenDrainPriority.DRAIN;
	}

	@Range(min = 1, max = 10)
	@ConfigItem(
		keyName = ConfigKeys.PRAYER_REGEN_DRAIN_RING_SIZE,
		name = "Drain/regen ring size",
		description = "Thickness of the regen/drain outline traced along the inside of the vial cavity.",
		section = prayerSection,
		position = 20
	)
	default int prayerRegenDrainRingSize()
	{
		return 1;
	}

	@ConfigItem(
		keyName = ConfigKeys.PRAYER_REVERSE_BAR_FILL,
		name = "Reverse bar fill",
		description = "Flips which end the vial fill empties from first - normally the near-icon end, with the remaining \"liquid\" receding toward the far end as the value drops.",
		section = prayerSection,
		position = 21
	)
	default boolean prayerReverseBarFill()
	{
		return false;
	}

	@ConfigItem(
		keyName = ConfigKeys.PRAYER_REVERSE_REGEN_RING,
		name = "Reverse regen ring",
		description = "Flips which end the regen ring grows from, independently of the bar fill above.",
		section = prayerSection,
		position = 22
	)
	default boolean prayerReverseRegenRing()
	{
		return false;
	}

	@ConfigItem(
		keyName = ConfigKeys.PRAYER_REVERSE_DRAIN_RING,
		name = "Reverse drain ring",
		description = "Flips which end the drain ring shrinks toward, independently of the bar fill and regen ring above.",
		section = prayerSection,
		position = 23
	)
	default boolean prayerReverseDrainRing()
	{
		return false;
	}

	@ConfigSection(
		name = "Special Attack",
		description = "",
		closedByDefault = true,
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

	@ConfigItem(
		keyName = ConfigKeys.SPEC_THRESHOLD_MODE,
		name = "Threshold mode",
		description = "Disabled: off. Active: the icon pulses once the threshold condition below is met. Combat only: same, but only while you're in combat.",
		section = specSection,
		position = 4
	)
	default ThresholdMode specThresholdMode()
	{
		return ThresholdMode.DISABLED;
	}

	@ConfigItem(
		keyName = ConfigKeys.SPEC_THRESHOLD_DIRECTION,
		name = "Threshold direction",
		description = "At or above: \"ready to use\" alert once energy reaches the threshold (matches v1's original Spec threshold feature, and the usual case). At or below: warns when energy is too low instead.",
		section = specSection,
		position = 5
	)
	default ThresholdDirection specThresholdDirection()
	{
		return ThresholdDirection.AT_OR_ABOVE;
	}

	@Range(min = 0, max = 100)
	@ConfigItem(
		keyName = ConfigKeys.SPEC_THRESHOLD,
		name = "Threshold",
		description = "Special Attack energy percentage the icon pulses at, per Threshold direction above.",
		section = specSection,
		position = 6
	)
	default int specThreshold()
	{
		return 50;
	}

	@ConfigItem(
		keyName = ConfigKeys.SPEC_THRESHOLD_COLOR,
		name = "Threshold glow color",
		description = "Color the icon pulses in once the threshold above is reached.",
		section = specSection,
		position = 7
	)
	default Color specThresholdColor()
	{
		return new Color(255, 215, 0, 160);
	}

	@ConfigItem(
		keyName = ConfigKeys.SPEC_HIDE_AUX_WHEN_MAIN_SHOWN,
		name = "Hide matching aux bar",
		description = "While this main bar is shown (Compact mode active), hide the auxiliary Special Attack bar too. Turn off to run both at once.",
		section = specSection,
		position = 8
	)
	default boolean specHideAuxWhenMainShown()
	{
		return true;
	}

	@ConfigItem(
		keyName = ConfigKeys.SPEC_ORIENTATION,
		name = "Orientation",
		description = "Which way this bar extends from its mount point (the Orb/Vial offsets in Global Bar Config) - West is the default and matches vanilla's own native collar direction; East/North mirror it with a flipped/rotated collar; South is the original hand-built vertical layout.",
		section = specSection,
		position = 9
	)
	default BarOrientation specOrientation()
	{
		return BarOrientation.WEST;
	}

	@Range(min = 20, max = 400)
	@ConfigItem(
		keyName = ConfigKeys.SPEC_LENGTH,
		name = "Length",
		description = "How long the bar is, end to end.",
		section = specSection,
		position = 10
	)
	default int specLength()
	{
		return 60;
	}

	@Range(min = 10, max = 40)
	@ConfigItem(
		keyName = ConfigKeys.SPEC_WIDTH,
		name = "Width",
		description = "How thick the bar is, across its short axis.",
		section = specSection,
		position = 11
	)
	default int specWidth()
	{
		return 20;
	}

	@ConfigItem(
		keyName = ConfigKeys.SPEC_HORIZONTAL_OFFSET,
		name = "Orb/Vial horizontal offset",
		description = "Mounts the bar this many pixels right (+) or left (-) of the orb icon's own center, before it extends outward per Orientation above.",
		section = specSection,
		position = 12
	)
	default int specHorizontalOffset()
	{
		return 0;
	}

	@ConfigItem(
		keyName = ConfigKeys.SPEC_VERTICAL_OFFSET,
		name = "Orb/Vial vertical offset",
		description = "Mounts the bar this many pixels down (+) or up (-) of the orb icon's own center, before it extends outward per Orientation above.",
		section = specSection,
		position = 13
	)
	default int specVerticalOffset()
	{
		return 0;
	}

	@Range(min = 1, max = 10)
	@ConfigItem(
		keyName = ConfigKeys.SPEC_FRAME_THICKNESS,
		name = "Frame thickness",
		description = "Thickness of the stone frame ring. The vial fill always fills whatever's left inside it.",
		section = specSection,
		position = 14
	)
	default int specFrameThickness()
	{
		return 2;
	}

	@Range(min = 1, max = 10)
	@ConfigItem(
		keyName = ConfigKeys.SPEC_FRAME_SHADOW_WIDTH,
		name = "Frame shadow width",
		description = "Stroke width of the dark outline traced around the whole bar's outer edge, independent of Frame thickness above.",
		section = specSection,
		position = 15
	)
	default int specFrameShadowWidth()
	{
		return 1;
	}

	@Range(min = 0, max = 100)
	@ConfigItem(
		keyName = ConfigKeys.SPEC_BAR_OPACITY,
		name = "Bar opacity",
		description = "Opacity of the vial fill. The regen/drain rings read more opaque than this. Doesn't affect the frame - see Frame opacity.",
		section = specSection,
		position = 16
	)
	default int specBarOpacity()
	{
		return 100;
	}

	@Range(min = 0, max = 100)
	@ConfigItem(
		keyName = ConfigKeys.SPEC_FRAME_OPACITY,
		name = "Frame opacity",
		description = "Opacity of the stone frame ring, independent of Bar opacity above.",
		section = specSection,
		position = 17
	)
	default int specFrameOpacity()
	{
		return 100;
	}

	@ConfigItem(
		keyName = ConfigKeys.SPEC_REGEN_DRAIN_PRIORITY,
		name = "Regen/drain priority",
		description = "Which ring draws on top (and reads more opaque) when this bar has both a regen ring and a drain ring showing at once.",
		section = specSection,
		position = 18
	)
	default RegenDrainPriority specRegenDrainPriority()
	{
		return RegenDrainPriority.DRAIN;
	}

	@Range(min = 1, max = 10)
	@ConfigItem(
		keyName = ConfigKeys.SPEC_REGEN_DRAIN_RING_SIZE,
		name = "Drain/regen ring size",
		description = "Thickness of the regen/drain outline traced along the inside of the vial cavity.",
		section = specSection,
		position = 19
	)
	default int specRegenDrainRingSize()
	{
		return 1;
	}

	@ConfigItem(
		keyName = ConfigKeys.SPEC_REVERSE_BAR_FILL,
		name = "Reverse bar fill",
		description = "Flips which end the vial fill empties from first - normally the near-icon end, with the remaining \"liquid\" receding toward the far end as the value drops.",
		section = specSection,
		position = 20
	)
	default boolean specReverseBarFill()
	{
		return false;
	}

	@ConfigItem(
		keyName = ConfigKeys.SPEC_REVERSE_REGEN_RING,
		name = "Reverse regen ring",
		description = "Flips which end the regen ring grows from, independently of the bar fill above.",
		section = specSection,
		position = 21
	)
	default boolean specReverseRegenRing()
	{
		return false;
	}

	@ConfigItem(
		keyName = ConfigKeys.SPEC_REVERSE_DRAIN_RING,
		name = "Reverse drain ring",
		description = "Flips which end the drain ring shrinks toward, independently of the bar fill and regen ring above.",
		section = specSection,
		position = 22
	)
	default boolean specReverseDrainRing()
	{
		return false;
	}

	@ConfigSection(
		name = "Run Energy",
		description = "",
		closedByDefault = true,
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

	@ConfigItem(
		keyName = ConfigKeys.RUN_THRESHOLD_MODE,
		name = "Threshold mode",
		description = "Disabled: off. Active: the icon pulses once the threshold condition below is met. Combat only: same, but only while you're in combat.",
		section = runSection,
		position = 4
	)
	default ThresholdMode runThresholdMode()
	{
		return ThresholdMode.DISABLED;
	}

	@ConfigItem(
		keyName = ConfigKeys.RUN_THRESHOLD_DIRECTION,
		name = "Threshold direction",
		description = "At or below: warns when run energy drops too low (the usual case). At or above: warns when too high instead.",
		section = runSection,
		position = 5
	)
	default ThresholdDirection runThresholdDirection()
	{
		return ThresholdDirection.AT_OR_BELOW;
	}

	@Range(min = 0, max = 100)
	@ConfigItem(
		keyName = ConfigKeys.RUN_THRESHOLD,
		name = "Threshold",
		description = "Run energy percentage the icon pulses at, per Threshold direction above.",
		section = runSection,
		position = 6
	)
	default int runThreshold()
	{
		return 20;
	}

	@ConfigItem(
		keyName = ConfigKeys.RUN_THRESHOLD_COLOR,
		name = "Threshold glow color",
		description = "Color the icon pulses in once the threshold above is reached.",
		section = runSection,
		position = 7
	)
	default Color runThresholdColor()
	{
		return new Color(220, 30, 30, 160);
	}

	@ConfigItem(
		keyName = ConfigKeys.RUN_HIDE_AUX_WHEN_MAIN_SHOWN,
		name = "Hide matching aux bar",
		description = "While this main bar is shown (Compact mode active), hide the auxiliary Run Energy bar too. Turn off to run both at once.",
		section = runSection,
		position = 8
	)
	default boolean runHideAuxWhenMainShown()
	{
		return true;
	}

	@ConfigItem(
		keyName = ConfigKeys.RUN_ORIENTATION,
		name = "Orientation",
		description = "Which way this bar extends from its mount point (the Orb/Vial offsets in Global Bar Config) - West is the default and matches vanilla's own native collar direction; East/North mirror it with a flipped/rotated collar; South is the original hand-built vertical layout.",
		section = runSection,
		position = 9
	)
	default BarOrientation runOrientation()
	{
		return BarOrientation.WEST;
	}

	@Range(min = 20, max = 400)
	@ConfigItem(
		keyName = ConfigKeys.RUN_LENGTH,
		name = "Length",
		description = "How long the bar is, end to end.",
		section = runSection,
		position = 10
	)
	default int runLength()
	{
		return 60;
	}

	@Range(min = 10, max = 40)
	@ConfigItem(
		keyName = ConfigKeys.RUN_WIDTH,
		name = "Width",
		description = "How thick the bar is, across its short axis.",
		section = runSection,
		position = 11
	)
	default int runWidth()
	{
		return 20;
	}

	@ConfigItem(
		keyName = ConfigKeys.RUN_HORIZONTAL_OFFSET,
		name = "Orb/Vial horizontal offset",
		description = "Mounts the bar this many pixels right (+) or left (-) of the orb icon's own center, before it extends outward per Orientation above.",
		section = runSection,
		position = 12
	)
	default int runHorizontalOffset()
	{
		return 0;
	}

	@ConfigItem(
		keyName = ConfigKeys.RUN_VERTICAL_OFFSET,
		name = "Orb/Vial vertical offset",
		description = "Mounts the bar this many pixels down (+) or up (-) of the orb icon's own center, before it extends outward per Orientation above.",
		section = runSection,
		position = 13
	)
	default int runVerticalOffset()
	{
		return 0;
	}

	@Range(min = 1, max = 10)
	@ConfigItem(
		keyName = ConfigKeys.RUN_FRAME_THICKNESS,
		name = "Frame thickness",
		description = "Thickness of the stone frame ring. The vial fill always fills whatever's left inside it.",
		section = runSection,
		position = 14
	)
	default int runFrameThickness()
	{
		return 2;
	}

	@Range(min = 1, max = 10)
	@ConfigItem(
		keyName = ConfigKeys.RUN_FRAME_SHADOW_WIDTH,
		name = "Frame shadow width",
		description = "Stroke width of the dark outline traced around the whole bar's outer edge, independent of Frame thickness above.",
		section = runSection,
		position = 15
	)
	default int runFrameShadowWidth()
	{
		return 1;
	}

	@Range(min = 0, max = 100)
	@ConfigItem(
		keyName = ConfigKeys.RUN_BAR_OPACITY,
		name = "Bar opacity",
		description = "Opacity of the vial fill. The regen/drain rings read more opaque than this. Doesn't affect the frame - see Frame opacity.",
		section = runSection,
		position = 16
	)
	default int runBarOpacity()
	{
		return 100;
	}

	@Range(min = 0, max = 100)
	@ConfigItem(
		keyName = ConfigKeys.RUN_FRAME_OPACITY,
		name = "Frame opacity",
		description = "Opacity of the stone frame ring, independent of Bar opacity above.",
		section = runSection,
		position = 17
	)
	default int runFrameOpacity()
	{
		return 100;
	}

	@ConfigItem(
		keyName = ConfigKeys.RUN_REGEN_DRAIN_PRIORITY,
		name = "Regen/drain priority",
		description = "Which ring draws on top (and reads more opaque) when this bar has both a regen ring and a drain ring showing at once.",
		section = runSection,
		position = 18
	)
	default RegenDrainPriority runRegenDrainPriority()
	{
		return RegenDrainPriority.DRAIN;
	}

	@Range(min = 1, max = 10)
	@ConfigItem(
		keyName = ConfigKeys.RUN_REGEN_DRAIN_RING_SIZE,
		name = "Drain/regen ring size",
		description = "Thickness of the regen/drain outline traced along the inside of the vial cavity.",
		section = runSection,
		position = 19
	)
	default int runRegenDrainRingSize()
	{
		return 1;
	}

	@ConfigItem(
		keyName = ConfigKeys.RUN_REVERSE_BAR_FILL,
		name = "Reverse bar fill",
		description = "Flips which end the vial fill empties from first - normally the near-icon end, with the remaining \"liquid\" receding toward the far end as the value drops.",
		section = runSection,
		position = 20
	)
	default boolean runReverseBarFill()
	{
		return false;
	}

	@ConfigItem(
		keyName = ConfigKeys.RUN_REVERSE_REGEN_RING,
		name = "Reverse regen ring",
		description = "Flips which end the regen ring grows from, independently of the bar fill above.",
		section = runSection,
		position = 21
	)
	default boolean runReverseRegenRing()
	{
		return false;
	}

	@ConfigItem(
		keyName = ConfigKeys.RUN_REVERSE_DRAIN_RING,
		name = "Reverse drain ring",
		description = "Flips which end the drain ring shrinks toward, independently of the bar fill and regen ring above.",
		section = runSection,
		position = 22
	)
	default boolean runReverseDrainRing()
	{
		return false;
	}

	// Empty on purpose - same simulated-nesting trick as Main/Aux Bar
	// Configuration above.
	@ConfigSection(
		name = "Hotkey Config",
		description = "",
		position = 15
	)
	String hotkeyConfig = "hotkeyConfig";

	@ConfigSection(
		name = "Hotkey",
		description = "",
		closedByDefault = true,
		position = 16
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

	// ==================================================================
	// Auxiliary bars - restored from v1 (see git log before the full-reset
	// "gut pass"): free-floating/Docked overlays with no real widget
	// involvement, shown by default whenever the main bars are hidden.
	// ==================================================================

	// Empty on purpose - same simulated-nesting trick as Main Bar
	// Configuration above: an always-expanded label with no items of its
	// own, immediately followed by the actual (collapsed) aux sections it's
	// grouping - Global Aux Bar Config through Aux Enemy Hitpoints below.
	@ConfigSection(
		name = "Aux Bar Configuration",
		description = "Everything through Aux Enemy Hitpoints below configures the free-floating/Docked auxiliary bars, shown by default whenever the main bars are hidden - expand a section as needed.",
		position = 7
	)
	String auxBarConfig = "auxBarConfig";

	@ConfigSection(
		name = "Global Aux Bar Config",
		description = "Shared settings for every auxiliary bar below.",
		closedByDefault = true,
		position = 8
	)
	String auxShared = "auxShared";

	@Range(min = 0, max = 10)
	@ConfigItem(
		keyName = ConfigKeys.AUX_FRAME_WIDTH,
		name = "Frame thickness",
		description = "Thickness of the stone frame ring, shared across every aux bar.",
		section = auxShared,
		position = 0
	)
	default int auxFrameWidth()
	{
		return 2;
	}

	@Range(min = 1, max = 10)
	@ConfigItem(
		keyName = ConfigKeys.AUX_FRAME_SHADOW_WIDTH,
		name = "Frame shadow width",
		description = "Stroke width of the dark outline traced around the whole bar's outer edge, independent of Frame thickness above - shared across every aux bar.",
		section = auxShared,
		position = 1
	)
	default int auxFrameShadowWidth()
	{
		return 1;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_REGEN_DRAIN_PRIORITY,
		name = "Regen/drain priority",
		description = "Which ring draws on top (and reads more opaque) when an aux bar has both a regen ring and a drain ring showing at once - shared across every aux bar.",
		section = auxShared,
		position = 2
	)
	default RegenDrainPriority auxRegenDrainPriority()
	{
		return RegenDrainPriority.DRAIN;
	}

	@Range(min = 0, max = 50)
	@ConfigItem(
		keyName = ConfigKeys.AUX_DOCK_PADDING,
		name = "Dock padding",
		description = "Gap in pixels between stacked aux bars sharing the same Docked side and slot order.",
		section = auxShared,
		position = 3
	)
	default int auxDockPadding()
	{
		return 4;
	}

	@Range(min = 0, max = 100)
	@ConfigItem(
		keyName = ConfigKeys.AUX_DOCK_OFFSET,
		name = "Dock offset",
		description = "Gap in pixels between the inventory panel itself and the first (slot 1) aux bar Docked to each side.",
		section = auxShared,
		position = 4
	)
	default int auxDockOffset()
	{
		return 4;
	}

	@Range(min = 1, max = 10)
	@ConfigItem(
		keyName = ConfigKeys.AUX_REGEN_OUTLINE_WIDTH,
		name = "Regen/drain outline width",
		description = "Thickness of the regen/drain outline traced along the inside of the vial cavity, shared across every aux bar.",
		section = auxShared,
		position = 5
	)
	default int auxRegenOutlineWidth()
	{
		return 1;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_REVERSE_BAR_FILL,
		name = "Reverse bar fill",
		description = "Flips which end every aux bar's vial fill empties from first.",
		section = auxShared,
		position = 6
	)
	default boolean auxReverseBarFill()
	{
		return false;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_REVERSE_REGEN_RING,
		name = "Reverse regen ring",
		description = "Flips which end every aux bar's regen ring grows from, independently of the bar fill above.",
		section = auxShared,
		position = 7
	)
	default boolean auxReverseRegenRing()
	{
		return false;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_REVERSE_DRAIN_RING,
		name = "Reverse drain ring",
		description = "Flips which end every aux bar's drain ring shrinks toward, independently of the bar fill and regen ring above.",
		section = auxShared,
		position = 8
	)
	default boolean auxReverseDrainRing()
	{
		return false;
	}

	@ConfigSection(
		name = "Aux Hitpoints",
		description = "",
		closedByDefault = true,
		position = 9
	)
	String auxHpSection = "auxHpSection";

	@ConfigItem(
		keyName = ConfigKeys.AUX_HP_ENABLED,
		name = "Enabled",
		description = "Shows the auxiliary HP bar whenever the main bars are hidden (or always, if its matching main bar's own coupling toggle is off).",
		section = auxHpSection,
		position = 0
	)
	default boolean auxHpEnabled()
	{
		return true;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_HP_PLACEMENT_MODE,
		name = "Placement",
		description = "Docked: pinned to a side of the inventory panel. Floating: drag it anywhere (Alt+click and drag, like any RuneLite overlay).",
		section = auxHpSection,
		position = 1
	)
	default PlacementMode auxHpPlacementMode()
	{
		return PlacementMode.DOCKED;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_HP_DOCK_SIDE,
		name = "Dock side",
		description = "Which side of the inventory panel this bar docks to - only while Placement is Docked.",
		section = auxHpSection,
		position = 2
	)
	default DockSide auxHpDockSide()
	{
		return DockSide.LEFT;
	}

	@Range(min = 1, max = 10)
	@ConfigItem(
		keyName = ConfigKeys.AUX_HP_DOCK_SLOT,
		name = "Dock slot",
		description = "Stacking order among other bars Docked to the same side - slot 1 sits closest to the panel.",
		section = auxHpSection,
		position = 3
	)
	default int auxHpDockSlot()
	{
		return 2;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_HP_DOCK_JUSTIFICATION,
		name = "Dock justification",
		description = "Where along the panel's edge this bar sits - only while Placement is Docked.",
		section = auxHpSection,
		position = 4
	)
	default Justification auxHpDockJustification()
	{
		return Justification.START;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_HP_CLAMP_TO_PANEL,
		name = "Clamp to panel length",
		description = "Stretches this bar to the full length of the inventory panel instead of its own configured Length - only while Placement is Docked.",
		section = auxHpSection,
		position = 5
	)
	default boolean auxHpClampToPanel()
	{
		return true;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_HP_FLOATING_ORIENTATION,
		name = "Floating orientation",
		description = "Only while Placement is Floating - Docked bars derive orientation from their Dock side instead.",
		section = auxHpSection,
		position = 6
	)
	default Orientation auxHpFloatingOrientation()
	{
		return Orientation.VERTICAL;
	}

	@Range(min = 4, max = 64)
	@ConfigItem(
		keyName = ConfigKeys.AUX_HP_THICKNESS,
		name = "Thickness",
		description = "Width of a vertical bar, or height of a horizontal one.",
		section = auxHpSection,
		position = 7
	)
	default int auxHpThickness()
	{
		return 8;
	}

	@Range(min = 8, max = 300)
	@ConfigItem(
		keyName = ConfigKeys.AUX_HP_LENGTH,
		name = "Length",
		description = "Height of a vertical bar, or width of a horizontal one. Ignored while Clamp to panel length is on.",
		section = auxHpSection,
		position = 8
	)
	default int auxHpLength()
	{
		return 32;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_HP_SHOW_ICON,
		name = "Show icon",
		description = "Draws the Hitpoints orb's own icon on the bar.",
		section = auxHpSection,
		position = 9
	)
	default boolean auxHpShowIcon()
	{
		return true;
	}

	@Range(min = 8, max = 40)
	@ConfigItem(
		keyName = ConfigKeys.AUX_HP_ICON_SIZE,
		name = "Icon size",
		description = "",
		section = auxHpSection,
		position = 10
	)
	default int auxHpIconSize()
	{
		return 18;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_HP_SHOW_COUNTER,
		name = "Show counter",
		description = "Draws the current hitpoints value as text on the bar.",
		section = auxHpSection,
		position = 11
	)
	default boolean auxHpShowCounter()
	{
		return true;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_HP_CONTENT_JUSTIFICATION,
		name = "Content justification",
		description = "Where the icon/counter cluster sits along the bar.",
		section = auxHpSection,
		position = 12
	)
	default Justification auxHpContentJustification()
	{
		return Justification.CENTER;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_HP_CONTENT_OFFSET,
		name = "Content offset",
		description = "Nudges the icon/counter cluster along the bar by this many pixels - positive toward the far end.",
		section = auxHpSection,
		position = 13
	)
	default int auxHpContentOffset()
	{
		return 0;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_HP_REVERSE_CONTENT,
		name = "Reverse content order",
		description = "Draws the counter before the icon instead of the default icon-then-counter.",
		section = auxHpSection,
		position = 14
	)
	default boolean auxHpReverseContent()
	{
		return false;
	}

	@Range(min = 0, max = 100)
	@ConfigItem(
		keyName = ConfigKeys.AUX_HP_OPACITY,
		name = "Opacity",
		description = "0 = fully transparent, 100 = fully opaque. Scales both the background and the fill color.",
		section = auxHpSection,
		position = 15
	)
	default int auxHpOpacity()
	{
		return 100;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_HP_NORMAL_COLOR,
		name = "Normal color",
		description = "Bar color while healthy.",
		section = auxHpSection,
		position = 16
	)
	default Color auxHpNormalColor()
	{
		return new Color(220, 40, 40);
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_HP_POISONED_COLOR,
		name = "Poisoned color",
		description = "Bar color while poisoned.",
		section = auxHpSection,
		position = 17
	)
	default Color auxHpPoisonedColor()
	{
		return new Color(90, 170, 60);
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_HP_ENVENOMED_COLOR,
		name = "Envenomed color",
		description = "Bar color while envenomed.",
		section = auxHpSection,
		position = 18
	)
	default Color auxHpEnvenomedColor()
	{
		return new Color(60, 120, 40);
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_HP_DISEASED_COLOR,
		name = "Diseased color",
		description = "Bar color while diseased.",
		section = auxHpSection,
		position = 19
	)
	default Color auxHpDiseasedColor()
	{
		return new Color(150, 110, 40);
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_HP_SHOW_REGEN,
		name = "Show regen",
		description = "Highlight the frame ring, growing toward the next hitpoint regenerated.",
		section = auxHpSection,
		position = 20
	)
	default boolean auxHpShowRegen()
	{
		return true;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_HP_REGEN_COLOR,
		name = "Regen ring color",
		description = "Regen outline color while Normal or Diseased.",
		section = auxHpSection,
		position = 21
	)
	default Color auxHpRegenColor()
	{
		return new Color(220, 40, 40);
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_HP_SHOW_DRAIN,
		name = "Show drain",
		description = "Highlight the frame ring while poisoned or envenomed, counting down to the next damage tick.",
		section = auxHpSection,
		position = 22
	)
	default boolean auxHpShowDrain()
	{
		return true;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_HP_THRESHOLD_MODE,
		name = "Threshold mode",
		description = "Disabled: off. Active: the bar pulses once the threshold condition below is met. Combat only: same, but only while in combat.",
		section = auxHpSection,
		position = 23
	)
	default ThresholdMode auxHpThresholdMode()
	{
		return ThresholdMode.DISABLED;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_HP_THRESHOLD_DIRECTION,
		name = "Threshold direction",
		description = "At or below: warns when hitpoints drop too low. At or above: warns when overhealed/too high instead.",
		section = auxHpSection,
		position = 24
	)
	default ThresholdDirection auxHpThresholdDirection()
	{
		return ThresholdDirection.AT_OR_BELOW;
	}

	@Range(min = 0, max = 100)
	@ConfigItem(
		keyName = ConfigKeys.AUX_HP_THRESHOLD,
		name = "Threshold",
		description = "Hitpoints percentage the bar pulses at, per Threshold direction above.",
		section = auxHpSection,
		position = 25
	)
	default int auxHpThreshold()
	{
		return 25;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_HP_THRESHOLD_COLOR,
		name = "Threshold glow color",
		description = "",
		section = auxHpSection,
		position = 26
	)
	default Color auxHpThresholdColor()
	{
		return new Color(220, 30, 30, 160);
	}

	@ConfigSection(
		name = "Aux Prayer",
		description = "",
		closedByDefault = true,
		position = 10
	)
	String auxPrayerSection = "auxPrayerSection";

	@ConfigItem(
		keyName = ConfigKeys.AUX_PRAYER_ENABLED,
		name = "Enabled",
		description = "Shows the auxiliary Prayer bar whenever the main bars are hidden (or always, if its matching main bar's own coupling toggle is off).",
		section = auxPrayerSection,
		position = 0
	)
	default boolean auxPrayerEnabled()
	{
		return true;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_PRAYER_PLACEMENT_MODE,
		name = "Placement",
		description = "Docked: pinned to a side of the inventory panel. Floating: drag it anywhere.",
		section = auxPrayerSection,
		position = 1
	)
	default PlacementMode auxPrayerPlacementMode()
	{
		return PlacementMode.DOCKED;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_PRAYER_DOCK_SIDE,
		name = "Dock side",
		description = "Only while Placement is Docked.",
		section = auxPrayerSection,
		position = 2
	)
	default DockSide auxPrayerDockSide()
	{
		return DockSide.LEFT;
	}

	@Range(min = 1, max = 10)
	@ConfigItem(
		keyName = ConfigKeys.AUX_PRAYER_DOCK_SLOT,
		name = "Dock slot",
		description = "Stacking order among other bars Docked to the same side.",
		section = auxPrayerSection,
		position = 3
	)
	default int auxPrayerDockSlot()
	{
		return 1;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_PRAYER_DOCK_JUSTIFICATION,
		name = "Dock justification",
		description = "",
		section = auxPrayerSection,
		position = 4
	)
	default Justification auxPrayerDockJustification()
	{
		return Justification.START;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_PRAYER_CLAMP_TO_PANEL,
		name = "Clamp to panel length",
		description = "",
		section = auxPrayerSection,
		position = 5
	)
	default boolean auxPrayerClampToPanel()
	{
		return true;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_PRAYER_FLOATING_ORIENTATION,
		name = "Floating orientation",
		description = "Only while Placement is Floating.",
		section = auxPrayerSection,
		position = 6
	)
	default Orientation auxPrayerFloatingOrientation()
	{
		return Orientation.VERTICAL;
	}

	@Range(min = 4, max = 64)
	@ConfigItem(
		keyName = ConfigKeys.AUX_PRAYER_THICKNESS,
		name = "Thickness",
		description = "",
		section = auxPrayerSection,
		position = 7
	)
	default int auxPrayerThickness()
	{
		return 8;
	}

	@Range(min = 8, max = 300)
	@ConfigItem(
		keyName = ConfigKeys.AUX_PRAYER_LENGTH,
		name = "Length",
		description = "Ignored while Clamp to panel length is on.",
		section = auxPrayerSection,
		position = 8
	)
	default int auxPrayerLength()
	{
		return 32;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_PRAYER_SHOW_ICON,
		name = "Show icon",
		description = "",
		section = auxPrayerSection,
		position = 9
	)
	default boolean auxPrayerShowIcon()
	{
		return true;
	}

	@Range(min = 8, max = 40)
	@ConfigItem(
		keyName = ConfigKeys.AUX_PRAYER_ICON_SIZE,
		name = "Icon size",
		description = "",
		section = auxPrayerSection,
		position = 10
	)
	default int auxPrayerIconSize()
	{
		return 18;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_PRAYER_SHOW_COUNTER,
		name = "Show counter",
		description = "",
		section = auxPrayerSection,
		position = 11
	)
	default boolean auxPrayerShowCounter()
	{
		return true;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_PRAYER_CONTENT_JUSTIFICATION,
		name = "Content justification",
		description = "",
		section = auxPrayerSection,
		position = 12
	)
	default Justification auxPrayerContentJustification()
	{
		return Justification.CENTER;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_PRAYER_CONTENT_OFFSET,
		name = "Content offset",
		description = "",
		section = auxPrayerSection,
		position = 13
	)
	default int auxPrayerContentOffset()
	{
		return 0;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_PRAYER_REVERSE_CONTENT,
		name = "Reverse content order",
		description = "",
		section = auxPrayerSection,
		position = 14
	)
	default boolean auxPrayerReverseContent()
	{
		return false;
	}

	@Range(min = 0, max = 100)
	@ConfigItem(
		keyName = ConfigKeys.AUX_PRAYER_OPACITY,
		name = "Opacity",
		description = "",
		section = auxPrayerSection,
		position = 15
	)
	default int auxPrayerOpacity()
	{
		return 100;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_PRAYER_ACTIVE_COLOR,
		name = "Active color",
		description = "Bar color while any prayer is active.",
		section = auxPrayerSection,
		position = 16
	)
	default Color auxPrayerActiveColor()
	{
		return new Color(60, 170, 220);
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_PRAYER_INACTIVE_COLOR,
		name = "Inactive color",
		description = "Bar color while no prayer is active.",
		section = auxPrayerSection,
		position = 17
	)
	default Color auxPrayerInactiveColor()
	{
		return new Color(25, 35, 80);
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_PRAYER_SHOW_REGEN,
		name = "Show regen",
		description = "Highlight the frame ring while a Prayer regeneration potion's effect is active.",
		section = auxPrayerSection,
		position = 18
	)
	default boolean auxPrayerShowRegen()
	{
		return true;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_PRAYER_REGEN_COLOR,
		name = "Regen ring color",
		description = "",
		section = auxPrayerSection,
		position = 19
	)
	default Color auxPrayerRegenColor()
	{
		return new Color(60, 170, 220);
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_PRAYER_SHOW_DRAIN,
		name = "Show drain",
		description = "Highlight the frame ring while any prayer is active, counting down to the next prayer point drained.",
		section = auxPrayerSection,
		position = 20
	)
	default boolean auxPrayerShowDrain()
	{
		return true;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_PRAYER_THRESHOLD_MODE,
		name = "Threshold mode",
		description = "",
		section = auxPrayerSection,
		position = 21
	)
	default ThresholdMode auxPrayerThresholdMode()
	{
		return ThresholdMode.DISABLED;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_PRAYER_THRESHOLD_DIRECTION,
		name = "Threshold direction",
		description = "",
		section = auxPrayerSection,
		position = 22
	)
	default ThresholdDirection auxPrayerThresholdDirection()
	{
		return ThresholdDirection.AT_OR_BELOW;
	}

	@Range(min = 0, max = 100)
	@ConfigItem(
		keyName = ConfigKeys.AUX_PRAYER_THRESHOLD,
		name = "Threshold",
		description = "",
		section = auxPrayerSection,
		position = 23
	)
	default int auxPrayerThreshold()
	{
		return 25;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_PRAYER_THRESHOLD_COLOR,
		name = "Threshold glow color",
		description = "",
		section = auxPrayerSection,
		position = 24
	)
	default Color auxPrayerThresholdColor()
	{
		return new Color(220, 30, 30, 160);
	}

	@ConfigSection(
		name = "Aux Special Attack",
		description = "",
		closedByDefault = true,
		position = 11
	)
	String auxSpecSection = "auxSpecSection";

	@ConfigItem(
		keyName = ConfigKeys.AUX_SPEC_ENABLED,
		name = "Enabled",
		description = "Shows the auxiliary Special Attack bar whenever the main bars are hidden (or always, if its matching main bar's own coupling toggle is off).",
		section = auxSpecSection,
		position = 0
	)
	default boolean auxSpecEnabled()
	{
		return true;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_SPEC_PLACEMENT_MODE,
		name = "Placement",
		description = "",
		section = auxSpecSection,
		position = 1
	)
	default PlacementMode auxSpecPlacementMode()
	{
		return PlacementMode.DOCKED;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_SPEC_DOCK_SIDE,
		name = "Dock side",
		description = "",
		section = auxSpecSection,
		position = 2
	)
	default DockSide auxSpecDockSide()
	{
		return DockSide.RIGHT;
	}

	@Range(min = 1, max = 10)
	@ConfigItem(
		keyName = ConfigKeys.AUX_SPEC_DOCK_SLOT,
		name = "Dock slot",
		description = "",
		section = auxSpecSection,
		position = 3
	)
	default int auxSpecDockSlot()
	{
		return 1;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_SPEC_DOCK_JUSTIFICATION,
		name = "Dock justification",
		description = "",
		section = auxSpecSection,
		position = 4
	)
	default Justification auxSpecDockJustification()
	{
		return Justification.START;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_SPEC_CLAMP_TO_PANEL,
		name = "Clamp to panel length",
		description = "",
		section = auxSpecSection,
		position = 5
	)
	default boolean auxSpecClampToPanel()
	{
		return true;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_SPEC_FLOATING_ORIENTATION,
		name = "Floating orientation",
		description = "",
		section = auxSpecSection,
		position = 6
	)
	default Orientation auxSpecFloatingOrientation()
	{
		return Orientation.VERTICAL;
	}

	@Range(min = 4, max = 64)
	@ConfigItem(
		keyName = ConfigKeys.AUX_SPEC_THICKNESS,
		name = "Thickness",
		description = "",
		section = auxSpecSection,
		position = 7
	)
	default int auxSpecThickness()
	{
		return 8;
	}

	@Range(min = 8, max = 300)
	@ConfigItem(
		keyName = ConfigKeys.AUX_SPEC_LENGTH,
		name = "Length",
		description = "",
		section = auxSpecSection,
		position = 8
	)
	default int auxSpecLength()
	{
		return 32;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_SPEC_SHOW_ICON,
		name = "Show icon",
		description = "",
		section = auxSpecSection,
		position = 9
	)
	default boolean auxSpecShowIcon()
	{
		return true;
	}

	@Range(min = 8, max = 40)
	@ConfigItem(
		keyName = ConfigKeys.AUX_SPEC_ICON_SIZE,
		name = "Icon size",
		description = "",
		section = auxSpecSection,
		position = 10
	)
	default int auxSpecIconSize()
	{
		return 18;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_SPEC_SHOW_COUNTER,
		name = "Show counter",
		description = "",
		section = auxSpecSection,
		position = 11
	)
	default boolean auxSpecShowCounter()
	{
		return true;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_SPEC_CONTENT_JUSTIFICATION,
		name = "Content justification",
		description = "",
		section = auxSpecSection,
		position = 12
	)
	default Justification auxSpecContentJustification()
	{
		return Justification.CENTER;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_SPEC_CONTENT_OFFSET,
		name = "Content offset",
		description = "",
		section = auxSpecSection,
		position = 13
	)
	default int auxSpecContentOffset()
	{
		return 0;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_SPEC_REVERSE_CONTENT,
		name = "Reverse content order",
		description = "",
		section = auxSpecSection,
		position = 14
	)
	default boolean auxSpecReverseContent()
	{
		return false;
	}

	@Range(min = 0, max = 100)
	@ConfigItem(
		keyName = ConfigKeys.AUX_SPEC_OPACITY,
		name = "Opacity",
		description = "",
		section = auxSpecSection,
		position = 15
	)
	default int auxSpecOpacity()
	{
		return 100;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_SPEC_DEFAULT_COLOR,
		name = "Default color",
		description = "Bar color while Special Attack isn't queued.",
		section = auxSpecSection,
		position = 16
	)
	default Color auxSpecDefaultColor()
	{
		return new Color(220, 40, 180);
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_SPEC_ACTIVE_COLOR,
		name = "Active color",
		description = "Bar color while Special Attack is queued.",
		section = auxSpecSection,
		position = 17
	)
	default Color auxSpecActiveColor()
	{
		return new Color(255, 255, 255);
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_SPEC_SHOW_REGEN,
		name = "Show regen",
		description = "",
		section = auxSpecSection,
		position = 18
	)
	default boolean auxSpecShowRegen()
	{
		return true;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_SPEC_REGEN_COLOR,
		name = "Regen ring color",
		description = "",
		section = auxSpecSection,
		position = 19
	)
	default Color auxSpecRegenColor()
	{
		return new Color(220, 40, 180);
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_SPEC_THRESHOLD_MODE,
		name = "Threshold mode",
		description = "",
		section = auxSpecSection,
		position = 20
	)
	default ThresholdMode auxSpecThresholdMode()
	{
		return ThresholdMode.DISABLED;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_SPEC_THRESHOLD_DIRECTION,
		name = "Threshold direction",
		description = "At or above: \"ready to use\" alert (the usual case). At or below: warns when energy is too low instead.",
		section = auxSpecSection,
		position = 21
	)
	default ThresholdDirection auxSpecThresholdDirection()
	{
		return ThresholdDirection.AT_OR_ABOVE;
	}

	@Range(min = 0, max = 100)
	@ConfigItem(
		keyName = ConfigKeys.AUX_SPEC_THRESHOLD,
		name = "Threshold",
		description = "",
		section = auxSpecSection,
		position = 22
	)
	default int auxSpecThreshold()
	{
		return 50;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_SPEC_THRESHOLD_COLOR,
		name = "Threshold glow color",
		description = "",
		section = auxSpecSection,
		position = 23
	)
	default Color auxSpecThresholdColor()
	{
		return new Color(255, 215, 0, 160);
	}

	@ConfigSection(
		name = "Aux Run Energy",
		description = "",
		closedByDefault = true,
		position = 12
	)
	String auxRunSection = "auxRunSection";

	@ConfigItem(
		keyName = ConfigKeys.AUX_RUN_ENABLED,
		name = "Enabled",
		description = "Shows the auxiliary Run Energy bar whenever the main bars are hidden (or always, if its matching main bar's own coupling toggle is off).",
		section = auxRunSection,
		position = 0
	)
	default boolean auxRunEnabled()
	{
		return true;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_RUN_PLACEMENT_MODE,
		name = "Placement",
		description = "",
		section = auxRunSection,
		position = 1
	)
	default PlacementMode auxRunPlacementMode()
	{
		return PlacementMode.DOCKED;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_RUN_DOCK_SIDE,
		name = "Dock side",
		description = "",
		section = auxRunSection,
		position = 2
	)
	default DockSide auxRunDockSide()
	{
		return DockSide.RIGHT;
	}

	@Range(min = 1, max = 10)
	@ConfigItem(
		keyName = ConfigKeys.AUX_RUN_DOCK_SLOT,
		name = "Dock slot",
		description = "",
		section = auxRunSection,
		position = 3
	)
	default int auxRunDockSlot()
	{
		return 2;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_RUN_DOCK_JUSTIFICATION,
		name = "Dock justification",
		description = "",
		section = auxRunSection,
		position = 4
	)
	default Justification auxRunDockJustification()
	{
		return Justification.START;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_RUN_CLAMP_TO_PANEL,
		name = "Clamp to panel length",
		description = "",
		section = auxRunSection,
		position = 5
	)
	default boolean auxRunClampToPanel()
	{
		return true;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_RUN_FLOATING_ORIENTATION,
		name = "Floating orientation",
		description = "",
		section = auxRunSection,
		position = 6
	)
	default Orientation auxRunFloatingOrientation()
	{
		return Orientation.VERTICAL;
	}

	@Range(min = 4, max = 64)
	@ConfigItem(
		keyName = ConfigKeys.AUX_RUN_THICKNESS,
		name = "Thickness",
		description = "",
		section = auxRunSection,
		position = 7
	)
	default int auxRunThickness()
	{
		return 8;
	}

	@Range(min = 8, max = 300)
	@ConfigItem(
		keyName = ConfigKeys.AUX_RUN_LENGTH,
		name = "Length",
		description = "",
		section = auxRunSection,
		position = 8
	)
	default int auxRunLength()
	{
		return 32;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_RUN_SHOW_ICON,
		name = "Show icon",
		description = "",
		section = auxRunSection,
		position = 9
	)
	default boolean auxRunShowIcon()
	{
		return true;
	}

	@Range(min = 8, max = 40)
	@ConfigItem(
		keyName = ConfigKeys.AUX_RUN_ICON_SIZE,
		name = "Icon size",
		description = "",
		section = auxRunSection,
		position = 10
	)
	default int auxRunIconSize()
	{
		return 18;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_RUN_SHOW_COUNTER,
		name = "Show counter",
		description = "",
		section = auxRunSection,
		position = 11
	)
	default boolean auxRunShowCounter()
	{
		return true;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_RUN_CONTENT_JUSTIFICATION,
		name = "Content justification",
		description = "",
		section = auxRunSection,
		position = 12
	)
	default Justification auxRunContentJustification()
	{
		return Justification.CENTER;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_RUN_CONTENT_OFFSET,
		name = "Content offset",
		description = "",
		section = auxRunSection,
		position = 13
	)
	default int auxRunContentOffset()
	{
		return 0;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_RUN_REVERSE_CONTENT,
		name = "Reverse content order",
		description = "",
		section = auxRunSection,
		position = 14
	)
	default boolean auxRunReverseContent()
	{
		return false;
	}

	@Range(min = 0, max = 100)
	@ConfigItem(
		keyName = ConfigKeys.AUX_RUN_OPACITY,
		name = "Opacity",
		description = "",
		section = auxRunSection,
		position = 15
	)
	default int auxRunOpacity()
	{
		return 100;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_RUN_DEFAULT_COLOR,
		name = "Default color",
		description = "Bar color while Run is toggled off.",
		section = auxRunSection,
		position = 16
	)
	default Color auxRunDefaultColor()
	{
		return new Color(220, 160, 40);
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_RUN_ACTIVE_COLOR,
		name = "Active color",
		description = "Bar color while Run is toggled on.",
		section = auxRunSection,
		position = 17
	)
	default Color auxRunActiveColor()
	{
		return new Color(212, 175, 55);
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_RUN_SHOW_REGEN,
		name = "Show regen",
		description = "",
		section = auxRunSection,
		position = 18
	)
	default boolean auxRunShowRegen()
	{
		return true;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_RUN_REGEN_COLOR,
		name = "Regen ring color",
		description = "",
		section = auxRunSection,
		position = 19
	)
	default Color auxRunRegenColor()
	{
		return new Color(220, 160, 40);
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_RUN_THRESHOLD_MODE,
		name = "Threshold mode",
		description = "",
		section = auxRunSection,
		position = 20
	)
	default ThresholdMode auxRunThresholdMode()
	{
		return ThresholdMode.DISABLED;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_RUN_THRESHOLD_DIRECTION,
		name = "Threshold direction",
		description = "",
		section = auxRunSection,
		position = 21
	)
	default ThresholdDirection auxRunThresholdDirection()
	{
		return ThresholdDirection.AT_OR_BELOW;
	}

	@Range(min = 0, max = 100)
	@ConfigItem(
		keyName = ConfigKeys.AUX_RUN_THRESHOLD,
		name = "Threshold",
		description = "",
		section = auxRunSection,
		position = 22
	)
	default int auxRunThreshold()
	{
		return 20;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_RUN_THRESHOLD_COLOR,
		name = "Threshold glow color",
		description = "",
		section = auxRunSection,
		position = 23
	)
	default Color auxRunThresholdColor()
	{
		return new Color(220, 30, 30, 160);
	}

	@ConfigSection(
		name = "Aux Warmth",
		description = "",
		closedByDefault = true,
		position = 13
	)
	String auxWarmthSection = "auxWarmthSection";

	@ConfigItem(
		keyName = ConfigKeys.AUX_WARMTH_ENABLED,
		name = "Enabled",
		description = "",
		section = auxWarmthSection,
		position = 0
	)
	default boolean auxWarmthEnabled()
	{
		return true;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_WARMTH_VISIBILITY,
		name = "Visibility",
		description = "Hide: never show. Show always: always show. Show dynamically: only inside the Wintertodt prison.",
		section = auxWarmthSection,
		position = 1
	)
	default WarmthVisibility auxWarmthVisibility()
	{
		return WarmthVisibility.SHOW_DYNAMICALLY;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_WARMTH_PLACEMENT_MODE,
		name = "Placement",
		description = "",
		section = auxWarmthSection,
		position = 2
	)
	default PlacementMode auxWarmthPlacementMode()
	{
		return PlacementMode.FLOATING;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_WARMTH_DOCK_SIDE,
		name = "Dock side",
		description = "",
		section = auxWarmthSection,
		position = 3
	)
	default DockSide auxWarmthDockSide()
	{
		return DockSide.TOP;
	}

	@Range(min = 1, max = 10)
	@ConfigItem(
		keyName = ConfigKeys.AUX_WARMTH_DOCK_SLOT,
		name = "Dock slot",
		description = "",
		section = auxWarmthSection,
		position = 4
	)
	default int auxWarmthDockSlot()
	{
		return 1;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_WARMTH_DOCK_JUSTIFICATION,
		name = "Dock justification",
		description = "",
		section = auxWarmthSection,
		position = 5
	)
	default Justification auxWarmthDockJustification()
	{
		return Justification.CENTER;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_WARMTH_CLAMP_TO_PANEL,
		name = "Clamp to panel length",
		description = "",
		section = auxWarmthSection,
		position = 6
	)
	default boolean auxWarmthClampToPanel()
	{
		return false;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_WARMTH_FLOATING_ORIENTATION,
		name = "Floating orientation",
		description = "",
		section = auxWarmthSection,
		position = 7
	)
	default Orientation auxWarmthFloatingOrientation()
	{
		return Orientation.HORIZONTAL;
	}

	@Range(min = 4, max = 64)
	@ConfigItem(
		keyName = ConfigKeys.AUX_WARMTH_THICKNESS,
		name = "Thickness",
		description = "",
		section = auxWarmthSection,
		position = 8
	)
	default int auxWarmthThickness()
	{
		return 8;
	}

	@Range(min = 8, max = 300)
	@ConfigItem(
		keyName = ConfigKeys.AUX_WARMTH_LENGTH,
		name = "Length",
		description = "",
		section = auxWarmthSection,
		position = 9
	)
	default int auxWarmthLength()
	{
		return 32;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_WARMTH_SHOW_ICON,
		name = "Show icon",
		description = "Draws the Firemaking skill icon on the bar - Warmth has no vanilla orb of its own.",
		section = auxWarmthSection,
		position = 10
	)
	default boolean auxWarmthShowIcon()
	{
		return true;
	}

	@Range(min = 8, max = 40)
	@ConfigItem(
		keyName = ConfigKeys.AUX_WARMTH_ICON_SIZE,
		name = "Icon size",
		description = "",
		section = auxWarmthSection,
		position = 11
	)
	default int auxWarmthIconSize()
	{
		return 18;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_WARMTH_SHOW_COUNTER,
		name = "Show counter",
		description = "",
		section = auxWarmthSection,
		position = 12
	)
	default boolean auxWarmthShowCounter()
	{
		return true;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_WARMTH_CONTENT_JUSTIFICATION,
		name = "Content justification",
		description = "",
		section = auxWarmthSection,
		position = 13
	)
	default Justification auxWarmthContentJustification()
	{
		return Justification.CENTER;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_WARMTH_CONTENT_OFFSET,
		name = "Content offset",
		description = "",
		section = auxWarmthSection,
		position = 14
	)
	default int auxWarmthContentOffset()
	{
		return 0;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_WARMTH_REVERSE_CONTENT,
		name = "Reverse content order",
		description = "",
		section = auxWarmthSection,
		position = 15
	)
	default boolean auxWarmthReverseContent()
	{
		return false;
	}

	@Range(min = 0, max = 100)
	@ConfigItem(
		keyName = ConfigKeys.AUX_WARMTH_OPACITY,
		name = "Opacity",
		description = "",
		section = auxWarmthSection,
		position = 16
	)
	default int auxWarmthOpacity()
	{
		return 100;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_WARMTH_COLOR,
		name = "Color",
		description = "",
		section = auxWarmthSection,
		position = 17
	)
	default Color auxWarmthColor()
	{
		return new Color(230, 110, 30);
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_WARMTH_THRESHOLD_MODE,
		name = "Threshold mode",
		description = "Disabled: off. Active: the bar pulses once the threshold condition below is met (e.g. a low-warmth warning). Combat only: same, but only while in combat.",
		section = auxWarmthSection,
		position = 18
	)
	default ThresholdMode auxWarmthThresholdMode()
	{
		return ThresholdMode.DISABLED;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_WARMTH_THRESHOLD_DIRECTION,
		name = "Threshold direction",
		description = "",
		section = auxWarmthSection,
		position = 19
	)
	default ThresholdDirection auxWarmthThresholdDirection()
	{
		return ThresholdDirection.AT_OR_BELOW;
	}

	@Range(min = 0, max = 100)
	@ConfigItem(
		keyName = ConfigKeys.AUX_WARMTH_THRESHOLD,
		name = "Threshold",
		description = "",
		section = auxWarmthSection,
		position = 20
	)
	default int auxWarmthThreshold()
	{
		return 25;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_WARMTH_THRESHOLD_COLOR,
		name = "Threshold glow color",
		description = "",
		section = auxWarmthSection,
		position = 21
	)
	default Color auxWarmthThresholdColor()
	{
		return new Color(60, 140, 220, 160);
	}

	@ConfigSection(
		name = "Aux Enemy Hitpoints",
		description = "",
		closedByDefault = true,
		position = 14
	)
	String auxEnemyHpSection = "auxEnemyHpSection";

	@ConfigItem(
		keyName = ConfigKeys.AUX_ENEMY_HP_ENABLED,
		name = "Enabled",
		description = "Shows your current combat target's health bar.",
		section = auxEnemyHpSection,
		position = 0
	)
	default boolean auxEnemyHpEnabled()
	{
		return true;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_ENEMY_HP_PLACEMENT_MODE,
		name = "Placement",
		description = "",
		section = auxEnemyHpSection,
		position = 1
	)
	default PlacementMode auxEnemyHpPlacementMode()
	{
		return PlacementMode.DOCKED;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_ENEMY_HP_DOCK_SIDE,
		name = "Dock side",
		description = "",
		section = auxEnemyHpSection,
		position = 2
	)
	default DockSide auxEnemyHpDockSide()
	{
		return DockSide.TOP;
	}

	@Range(min = 1, max = 10)
	@ConfigItem(
		keyName = ConfigKeys.AUX_ENEMY_HP_DOCK_SLOT,
		name = "Dock slot",
		description = "",
		section = auxEnemyHpSection,
		position = 3
	)
	default int auxEnemyHpDockSlot()
	{
		return 1;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_ENEMY_HP_DOCK_JUSTIFICATION,
		name = "Dock justification",
		description = "",
		section = auxEnemyHpSection,
		position = 4
	)
	default Justification auxEnemyHpDockJustification()
	{
		return Justification.CENTER;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_ENEMY_HP_CLAMP_TO_PANEL,
		name = "Clamp to panel length",
		description = "",
		section = auxEnemyHpSection,
		position = 5
	)
	default boolean auxEnemyHpClampToPanel()
	{
		return true;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_ENEMY_HP_FLOATING_ORIENTATION,
		name = "Floating orientation",
		description = "",
		section = auxEnemyHpSection,
		position = 6
	)
	default Orientation auxEnemyHpFloatingOrientation()
	{
		return Orientation.HORIZONTAL;
	}

	@Range(min = 4, max = 64)
	@ConfigItem(
		keyName = ConfigKeys.AUX_ENEMY_HP_THICKNESS,
		name = "Thickness",
		description = "",
		section = auxEnemyHpSection,
		position = 7
	)
	default int auxEnemyHpThickness()
	{
		return 8;
	}

	@Range(min = 8, max = 300)
	@ConfigItem(
		keyName = ConfigKeys.AUX_ENEMY_HP_LENGTH,
		name = "Length",
		description = "",
		section = auxEnemyHpSection,
		position = 8
	)
	default int auxEnemyHpLength()
	{
		return 32;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_ENEMY_HP_SHOW_ICON,
		name = "Show icon",
		description = "Draws the Hitpoints orb's own icon on the bar - no \"enemy hp\" orb exists, this is our own cosmetic choice for visual consistency.",
		section = auxEnemyHpSection,
		position = 9
	)
	default boolean auxEnemyHpShowIcon()
	{
		return true;
	}

	@Range(min = 8, max = 40)
	@ConfigItem(
		keyName = ConfigKeys.AUX_ENEMY_HP_ICON_SIZE,
		name = "Icon size",
		description = "",
		section = auxEnemyHpSection,
		position = 10
	)
	default int auxEnemyHpIconSize()
	{
		return 18;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_ENEMY_HP_SHOW_COUNTER,
		name = "Show counter",
		description = "",
		section = auxEnemyHpSection,
		position = 11
	)
	default boolean auxEnemyHpShowCounter()
	{
		return true;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_ENEMY_HP_COUNTER_FORMAT,
		name = "Counter format",
		description = "Fraction: the game's own coarse health scale (e.g. 27/30, rarely a real HP number). Percentage: derived from that scale instead.",
		section = auxEnemyHpSection,
		position = 12
	)
	default CounterFormat auxEnemyHpCounterFormat()
	{
		return CounterFormat.FRACTION;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_ENEMY_HP_CONTENT_JUSTIFICATION,
		name = "Content justification",
		description = "",
		section = auxEnemyHpSection,
		position = 13
	)
	default Justification auxEnemyHpContentJustification()
	{
		return Justification.CENTER;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_ENEMY_HP_CONTENT_OFFSET,
		name = "Content offset",
		description = "",
		section = auxEnemyHpSection,
		position = 14
	)
	default int auxEnemyHpContentOffset()
	{
		return 0;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_ENEMY_HP_REVERSE_CONTENT,
		name = "Reverse content order",
		description = "",
		section = auxEnemyHpSection,
		position = 15
	)
	default boolean auxEnemyHpReverseContent()
	{
		return false;
	}

	@Range(min = 0, max = 100)
	@ConfigItem(
		keyName = ConfigKeys.AUX_ENEMY_HP_OPACITY,
		name = "Opacity",
		description = "",
		section = auxEnemyHpSection,
		position = 16
	)
	default int auxEnemyHpOpacity()
	{
		return 100;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_ENEMY_HP_COLOR,
		name = "Color",
		description = "",
		section = auxEnemyHpSection,
		position = 17
	)
	default Color auxEnemyHpColor()
	{
		return new Color(220, 40, 40);
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_ENEMY_HP_THRESHOLD_MODE,
		name = "Threshold mode",
		description = "Disabled: off. Active: the bar pulses once the threshold condition below is met (e.g. a low-target-health alert). Combat only: same, but only while in combat.",
		section = auxEnemyHpSection,
		position = 18
	)
	default ThresholdMode auxEnemyHpThresholdMode()
	{
		return ThresholdMode.DISABLED;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_ENEMY_HP_THRESHOLD_DIRECTION,
		name = "Threshold direction",
		description = "",
		section = auxEnemyHpSection,
		position = 19
	)
	default ThresholdDirection auxEnemyHpThresholdDirection()
	{
		return ThresholdDirection.AT_OR_BELOW;
	}

	@Range(min = 0, max = 100)
	@ConfigItem(
		keyName = ConfigKeys.AUX_ENEMY_HP_THRESHOLD,
		name = "Threshold",
		description = "",
		section = auxEnemyHpSection,
		position = 20
	)
	default int auxEnemyHpThreshold()
	{
		return 25;
	}

	@ConfigItem(
		keyName = ConfigKeys.AUX_ENEMY_HP_THRESHOLD_COLOR,
		name = "Threshold glow color",
		description = "",
		section = auxEnemyHpSection,
		position = 21
	)
	default Color auxEnemyHpThresholdColor()
	{
		return new Color(220, 30, 30, 160);
	}

	@ConfigSection(
		name = "Tutorial",
		description = "",
		closedByDefault = true,
		position = 17
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

	@ConfigSection(
		name = "Testing",
		description = "Temporary dial-in sliders - not meant to stay long-term.",
		closedByDefault = true,
		position = 18
	)
	String testing = "testing";

	@ConfigItem(
		keyName = ConfigKeys.NORTH_NUMBER_X_OFFSET,
		name = "North number X offset",
		description = "Nudges our self-drawn North number left (-) or right (+) of its default centered position above the icon.",
		section = testing,
		position = 0
	)
	default int northNumberXOffset()
	{
		return 0;
	}

	@ConfigItem(
		keyName = ConfigKeys.NORTH_NUMBER_Y_OFFSET,
		name = "North number Y offset",
		description = "Nudges our self-drawn North number up (-) or down (+) of its default position above the icon.",
		section = testing,
		position = 1
	)
	default int northNumberYOffset()
	{
		return 0;
	}

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
