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

import java.awt.Color;
import net.runelite.api.ScriptID;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.SpriteID;
import net.runelite.api.gameval.VarClientID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.ui.JagexColors;

public class CompactOrbsConstants
{
	public static final class ConfigGroup
	{
		// Retargeted to Vicious Bars' own existing config group, so this
		// whole copied-and-being-gutted system integrates as THIS plugin's
		// real config (shows up under "Vicious Bars" in the plugin panel),
		// not as a separate parallel "Compact Orbs" entry.
		public static final String GROUP_NAME = "vicious-bars";
		public static final int CONFIG_VERSION = 2;

		public static final class Core
		{
			public static final String WIKI = "wiki";
			public static final String MINIMAP = "minimap";
			public static final String RUNELITE = "runelite";
		}
	}

	public static final class ConfigKeys
	{
		public static final String CONFIG_VERSION = "configVersion";
		public static final String MINIMAP = "hideMinimap";
		public static final String COMPASS = "hideCompass";
		public static final String HOTKEY_KEYBIND = "hotkeyKeybind";
		public static final String HOTKEY_TOGGLE_OPTION = "hotkeyToggleOption";

		//not a real config choice - internal bookkeeping key only, see
		//HideOrbRegistry's registration of this for why it still exists
		public static final String MINIMAP_TOGGLE_BUTTON = "hideMinimapButton";

		public static final String ORB_LAYOUT = "orbLayout";
		public static final String VERTICAL_ANCHOR = "verticalAnchor";
		public static final String HORIZONTAL_ANCHOR = "horizontalAnchor";
		public static final String VERTICAL_Y_ADJUSTMENT = "verticalYAdjustment";
		public static final String DISABLE_REORDERING = "disableReordering";
		public static final String ENABLE_NO_CLICKTHROUGH = "enableNoClickthrough";
		public static final String HIDE_MINIMAP_WITH_SIDE_PANEL = "hideMinimapWithSidePanel";
		public static final String HIDE_HP = "hideHp";
		public static final String HIDE_PRAYER = "hidePrayer";
		public static final String HIDE_RUN = "hideRun";
		public static final String HIDE_SPEC = "hideSpec";
		public static final String HIDE_XP = "hideXp";
		public static final String HIDE_WORLD = "hideWorld";
		public static final String HIDE_ACTIVITY = "hideActivity";
		public static final String HIDE_STORE = "hideStore";
		public static final String HIDE_WIKI = "hideWiki";
		public static final String HIDE_LOGOUT_X = "hideLogoutX";
		public static final String HIDE_GRID = "hideGrid";

		// Vicious Bars' own, new to this plugin (not inherited from Compact
		// Orbs). Every one of these was originally a single Global Bar
		// Config setting shared across all four main bars (same era as
		// orientation() - see git log) - promoted to fully independent
		// per-bar settings, one key per bar, each bar's own default
		// matching what the old shared setting used to default to.
		public static final String HP_LENGTH = "hpLength";
		public static final String PRAYER_LENGTH = "prayerLength";
		public static final String SPEC_LENGTH = "specLength";
		public static final String RUN_LENGTH = "runLength";
		public static final String HP_WIDTH = "hpWidth";
		public static final String PRAYER_WIDTH = "prayerWidth";
		public static final String SPEC_WIDTH = "specWidth";
		public static final String RUN_WIDTH = "runWidth";
		public static final String HP_HORIZONTAL_OFFSET = "hpHorizontalOffset";
		public static final String PRAYER_HORIZONTAL_OFFSET = "prayerHorizontalOffset";
		public static final String SPEC_HORIZONTAL_OFFSET = "specHorizontalOffset";
		public static final String RUN_HORIZONTAL_OFFSET = "runHorizontalOffset";
		public static final String HP_VERTICAL_OFFSET = "hpVerticalOffset";
		public static final String PRAYER_VERTICAL_OFFSET = "prayerVerticalOffset";
		public static final String SPEC_VERTICAL_OFFSET = "specVerticalOffset";
		public static final String RUN_VERTICAL_OFFSET = "runVerticalOffset";
		public static final String HP_FRAME_THICKNESS = "hpFrameThickness";
		public static final String PRAYER_FRAME_THICKNESS = "prayerFrameThickness";
		public static final String SPEC_FRAME_THICKNESS = "specFrameThickness";
		public static final String RUN_FRAME_THICKNESS = "runFrameThickness";
		public static final String HP_FRAME_SHADOW_WIDTH = "hpFrameShadowWidth";
		public static final String PRAYER_FRAME_SHADOW_WIDTH = "prayerFrameShadowWidth";
		public static final String SPEC_FRAME_SHADOW_WIDTH = "specFrameShadowWidth";
		public static final String RUN_FRAME_SHADOW_WIDTH = "runFrameShadowWidth";
		public static final String HP_BAR_OPACITY = "hpBarOpacity";
		public static final String PRAYER_BAR_OPACITY = "prayerBarOpacity";
		public static final String SPEC_BAR_OPACITY = "specBarOpacity";
		public static final String RUN_BAR_OPACITY = "runBarOpacity";
		public static final String HP_FRAME_OPACITY = "hpFrameOpacity";
		public static final String PRAYER_FRAME_OPACITY = "prayerFrameOpacity";
		public static final String SPEC_FRAME_OPACITY = "specFrameOpacity";
		public static final String RUN_FRAME_OPACITY = "runFrameOpacity";
		public static final String HP_REGEN_DRAIN_PRIORITY = "hpRegenDrainPriority";
		public static final String PRAYER_REGEN_DRAIN_PRIORITY = "prayerRegenDrainPriority";
		public static final String SPEC_REGEN_DRAIN_PRIORITY = "specRegenDrainPriority";
		public static final String RUN_REGEN_DRAIN_PRIORITY = "runRegenDrainPriority";
		public static final String HP_REGEN_DRAIN_RING_SIZE = "hpRegenDrainRingSize";
		public static final String PRAYER_REGEN_DRAIN_RING_SIZE = "prayerRegenDrainRingSize";
		public static final String SPEC_REGEN_DRAIN_RING_SIZE = "specRegenDrainRingSize";
		public static final String RUN_REGEN_DRAIN_RING_SIZE = "runRegenDrainRingSize";

		// Each of the three filling elements (main vial fill, regen ring,
		// drain ring) normally empties/grows from the near-icon end first,
		// per orientation (see drawVial()'s own javadoc) - these flip that
		// per element, independently of each other, of orientation, and
		// (now) of the other main bars.
		public static final String HP_REVERSE_BAR_FILL = "hpReverseBarFill";
		public static final String PRAYER_REVERSE_BAR_FILL = "prayerReverseBarFill";
		public static final String SPEC_REVERSE_BAR_FILL = "specReverseBarFill";
		public static final String RUN_REVERSE_BAR_FILL = "runReverseBarFill";
		public static final String HP_REVERSE_REGEN_RING = "hpReverseRegenRing";
		public static final String PRAYER_REVERSE_REGEN_RING = "prayerReverseRegenRing";
		public static final String SPEC_REVERSE_REGEN_RING = "specReverseRegenRing";
		public static final String RUN_REVERSE_REGEN_RING = "runReverseRegenRing";
		public static final String HP_REVERSE_DRAIN_RING = "hpReverseDrainRing";
		public static final String PRAYER_REVERSE_DRAIN_RING = "prayerReverseDrainRing";
		public static final String SPEC_REVERSE_DRAIN_RING = "specReverseDrainRing";
		public static final String RUN_REVERSE_DRAIN_RING = "runReverseDrainRing";

		// Per-bar regen-highlight toggles (Hitpoints/Prayer/Special
		// Attack/Run Energy config sections).
		public static final String HP_SHOW_REGEN = "hpShowRegen";
		public static final String HP_SHOW_DRAIN = "hpShowDrain";
		public static final String PRAYER_SHOW_REGEN = "prayerShowRegen";
		public static final String PRAYER_SHOW_DRAIN = "prayerShowDrain";
		public static final String SPEC_SHOW_REGEN = "specShowRegen";
		public static final String RUN_SHOW_REGEN = "runShowRegen";

		// Per-bar state fill colors - restored from v1 (see git log before
		// the full-reset "gut pass"), now driven by the same real
		// varp/varbit signals v1 captured and confirmed in-game.
		public static final String HP_NORMAL_COLOR = "hpNormalColor";
		public static final String HP_POISONED_COLOR = "hpPoisonedColor";
		public static final String HP_ENVENOMED_COLOR = "hpEnvenomedColor";
		public static final String HP_DISEASED_COLOR = "hpDiseasedColor";
		public static final String PRAYER_ACTIVE_COLOR = "prayerActiveColor";
		public static final String PRAYER_INACTIVE_COLOR = "prayerInactiveColor";
		public static final String SPEC_DEFAULT_COLOR = "specDefaultColor";
		public static final String SPEC_ACTIVE_COLOR = "specActiveColor";
		public static final String RUN_DEFAULT_COLOR = "runDefaultColor";
		public static final String RUN_ACTIVE_COLOR = "runActiveColor";

		// Per-bar regen-ring colors - independent of the fill color above
		// (defaults match each bar's own default/normal fill, but either
		// can be changed without affecting the other). HP is the exception:
		// while poisoned/envenomed, HpVialOverlay hijacks this and draws the
		// ring in whatever color the fill itself is using (Poisoned/
		// Envenomed above) instead of hpRegenColor, so the countdown ring
		// reads as part of the poison state rather than a separate choice.
		public static final String HP_REGEN_COLOR = "hpRegenColor";
		public static final String PRAYER_REGEN_COLOR = "prayerRegenColor";
		public static final String SPEC_REGEN_COLOR = "specRegenColor";
		public static final String RUN_REGEN_COLOR = "runRegenColor";

		// Per-bar threshold-glow settings, restored from v1 (see git log
		// before the full-reset "gut pass") - v1 only ever had this for
		// Spec, generalized to all four bars here. Direction is per-bar
		// configurable (e.g. HP defaults to "running low" but can be
		// flipped to warn when overhealed/too high instead).
		public static final String HP_THRESHOLD_MODE = "hpThresholdMode";
		public static final String HP_THRESHOLD_DIRECTION = "hpThresholdDirection";
		public static final String HP_THRESHOLD = "hpThreshold";
		public static final String HP_THRESHOLD_COLOR = "hpThresholdColor";
		public static final String PRAYER_THRESHOLD_MODE = "prayerThresholdMode";
		public static final String PRAYER_THRESHOLD_DIRECTION = "prayerThresholdDirection";
		public static final String PRAYER_THRESHOLD = "prayerThreshold";
		public static final String PRAYER_THRESHOLD_COLOR = "prayerThresholdColor";
		public static final String SPEC_THRESHOLD_MODE = "specThresholdMode";
		public static final String SPEC_THRESHOLD_DIRECTION = "specThresholdDirection";
		public static final String SPEC_THRESHOLD = "specThreshold";
		public static final String SPEC_THRESHOLD_COLOR = "specThresholdColor";
		public static final String RUN_THRESHOLD_MODE = "runThresholdMode";
		public static final String RUN_THRESHOLD_DIRECTION = "runThresholdDirection";
		public static final String RUN_THRESHOLD = "runThreshold";
		public static final String RUN_THRESHOLD_COLOR = "runThresholdColor";

		// Per-main-bar coupling with that bar's own aux counterpart - see
		// the Aux Bars keys below. Default on: when main-bars mode is
		// active, the matching aux bar auto-hides; turn off to run both at
		// once for that particular bar.
		public static final String HP_HIDE_AUX_WHEN_MAIN_SHOWN = "hpHideAuxWhenMainShown";
		public static final String PRAYER_HIDE_AUX_WHEN_MAIN_SHOWN = "prayerHideAuxWhenMainShown";
		public static final String SPEC_HIDE_AUX_WHEN_MAIN_SHOWN = "specHideAuxWhenMainShown";
		public static final String RUN_HIDE_AUX_WHEN_MAIN_SHOWN = "runHideAuxWhenMainShown";

		// ------------------------------------------------------------------
		// Auxiliary bars - restored from v1 (see git log before the
		// full-reset "gut pass"): free-floating/Docked overlays with no real
		// widget involvement at all (no repositioning, no interactivity -
		// that's the main bars' job above), shown by default whenever the
		// main bars are hidden (stock Compact Orbs behavior - vanilla
		// minimap until the user enables Compact mode). HP/Prayer/Spec/Run
        // aux bars read the same CompactOrbsManager regen/drain fields their
		// main-bar counterparts do; Warmth/Enemy HP have no main-bar
		// counterpart and never existed as anything but an aux bar, even in
		// v1.
		// ------------------------------------------------------------------

		// Shared across every aux bar - same three v1 kept shared
		// (borderWidth/dockPadding/dockOffset) plus the newer v2-parity
		// shared settings (frame opacity split, regen outline width,
		// regen/drain priority, reverse-direction toggles).
		public static final String AUX_FRAME_WIDTH = "auxFrameWidth";
		public static final String AUX_FRAME_SHADOW_WIDTH = "auxFrameShadowWidth";

		// Was a straight read of the main bars' own (now-removed, promoted
		// to per-bar) drainRingDominant() boolean - aux bars need their own
		// copy of this concept now, upgraded to match the main bars' newer
		// enum-based naming (Regen/drain priority) rather than reintroducing
		// the old boolean.
		public static final String AUX_REGEN_DRAIN_PRIORITY = "auxRegenDrainPriority";
		public static final String AUX_DOCK_PADDING = "auxDockPadding";
		public static final String AUX_DOCK_OFFSET = "auxDockOffset";
		public static final String AUX_REGEN_OUTLINE_WIDTH = "auxRegenOutlineWidth";
		public static final String AUX_REVERSE_BAR_FILL = "auxReverseBarFill";
		public static final String AUX_REVERSE_REGEN_RING = "auxReverseRegenRing";
		public static final String AUX_REVERSE_DRAIN_RING = "auxReverseDrainRing";

		// Hitpoints (aux)
		public static final String AUX_HP_ENABLED = "auxHpEnabled";
		public static final String AUX_HP_PLACEMENT_MODE = "auxHpPlacementMode";
		public static final String AUX_HP_DOCK_SIDE = "auxHpDockSide";
		public static final String AUX_HP_DOCK_SLOT = "auxHpDockSlot";
		public static final String AUX_HP_DOCK_JUSTIFICATION = "auxHpDockJustification";
		public static final String AUX_HP_CLAMP_TO_PANEL = "auxHpClampToPanel";
		public static final String AUX_HP_FLOATING_ORIENTATION = "auxHpFloatingOrientation";
		public static final String AUX_HP_THICKNESS = "auxHpThickness";
		public static final String AUX_HP_LENGTH = "auxHpLength";
		public static final String AUX_HP_SHOW_ICON = "auxHpShowIcon";
		public static final String AUX_HP_ICON_SIZE = "auxHpIconSize";
		public static final String AUX_HP_SHOW_COUNTER = "auxHpShowCounter";
		public static final String AUX_HP_CONTENT_JUSTIFICATION = "auxHpContentJustification";
		public static final String AUX_HP_CONTENT_OFFSET = "auxHpContentOffset";
		public static final String AUX_HP_REVERSE_CONTENT = "auxHpReverseContent";
		public static final String AUX_HP_OPACITY = "auxHpOpacity";
		public static final String AUX_HP_NORMAL_COLOR = "auxHpNormalColor";
		public static final String AUX_HP_POISONED_COLOR = "auxHpPoisonedColor";
		public static final String AUX_HP_ENVENOMED_COLOR = "auxHpEnvenomedColor";
		public static final String AUX_HP_DISEASED_COLOR = "auxHpDiseasedColor";
		public static final String AUX_HP_SHOW_REGEN = "auxHpShowRegen";
		public static final String AUX_HP_REGEN_COLOR = "auxHpRegenColor";
		public static final String AUX_HP_SHOW_DRAIN = "auxHpShowDrain";
		public static final String AUX_HP_THRESHOLD_MODE = "auxHpThresholdMode";
		public static final String AUX_HP_THRESHOLD_DIRECTION = "auxHpThresholdDirection";
		public static final String AUX_HP_THRESHOLD = "auxHpThreshold";
		public static final String AUX_HP_THRESHOLD_COLOR = "auxHpThresholdColor";

		// Prayer (aux)
		public static final String AUX_PRAYER_ENABLED = "auxPrayerEnabled";
		public static final String AUX_PRAYER_PLACEMENT_MODE = "auxPrayerPlacementMode";
		public static final String AUX_PRAYER_DOCK_SIDE = "auxPrayerDockSide";
		public static final String AUX_PRAYER_DOCK_SLOT = "auxPrayerDockSlot";
		public static final String AUX_PRAYER_DOCK_JUSTIFICATION = "auxPrayerDockJustification";
		public static final String AUX_PRAYER_CLAMP_TO_PANEL = "auxPrayerClampToPanel";
		public static final String AUX_PRAYER_FLOATING_ORIENTATION = "auxPrayerFloatingOrientation";
		public static final String AUX_PRAYER_THICKNESS = "auxPrayerThickness";
		public static final String AUX_PRAYER_LENGTH = "auxPrayerLength";
		public static final String AUX_PRAYER_SHOW_ICON = "auxPrayerShowIcon";
		public static final String AUX_PRAYER_ICON_SIZE = "auxPrayerIconSize";
		public static final String AUX_PRAYER_SHOW_COUNTER = "auxPrayerShowCounter";
		public static final String AUX_PRAYER_CONTENT_JUSTIFICATION = "auxPrayerContentJustification";
		public static final String AUX_PRAYER_CONTENT_OFFSET = "auxPrayerContentOffset";
		public static final String AUX_PRAYER_REVERSE_CONTENT = "auxPrayerReverseContent";
		public static final String AUX_PRAYER_OPACITY = "auxPrayerOpacity";
		public static final String AUX_PRAYER_ACTIVE_COLOR = "auxPrayerActiveColor";
		public static final String AUX_PRAYER_INACTIVE_COLOR = "auxPrayerInactiveColor";
		public static final String AUX_PRAYER_SHOW_REGEN = "auxPrayerShowRegen";
		public static final String AUX_PRAYER_REGEN_COLOR = "auxPrayerRegenColor";
		public static final String AUX_PRAYER_SHOW_DRAIN = "auxPrayerShowDrain";
		public static final String AUX_PRAYER_THRESHOLD_MODE = "auxPrayerThresholdMode";
		public static final String AUX_PRAYER_THRESHOLD_DIRECTION = "auxPrayerThresholdDirection";
		public static final String AUX_PRAYER_THRESHOLD = "auxPrayerThreshold";
		public static final String AUX_PRAYER_THRESHOLD_COLOR = "auxPrayerThresholdColor";

		// Special Attack (aux)
		public static final String AUX_SPEC_ENABLED = "auxSpecEnabled";
		public static final String AUX_SPEC_PLACEMENT_MODE = "auxSpecPlacementMode";
		public static final String AUX_SPEC_DOCK_SIDE = "auxSpecDockSide";
		public static final String AUX_SPEC_DOCK_SLOT = "auxSpecDockSlot";
		public static final String AUX_SPEC_DOCK_JUSTIFICATION = "auxSpecDockJustification";
		public static final String AUX_SPEC_CLAMP_TO_PANEL = "auxSpecClampToPanel";
		public static final String AUX_SPEC_FLOATING_ORIENTATION = "auxSpecFloatingOrientation";
		public static final String AUX_SPEC_THICKNESS = "auxSpecThickness";
		public static final String AUX_SPEC_LENGTH = "auxSpecLength";
		public static final String AUX_SPEC_SHOW_ICON = "auxSpecShowIcon";
		public static final String AUX_SPEC_ICON_SIZE = "auxSpecIconSize";
		public static final String AUX_SPEC_SHOW_COUNTER = "auxSpecShowCounter";
		public static final String AUX_SPEC_CONTENT_JUSTIFICATION = "auxSpecContentJustification";
		public static final String AUX_SPEC_CONTENT_OFFSET = "auxSpecContentOffset";
		public static final String AUX_SPEC_REVERSE_CONTENT = "auxSpecReverseContent";
		public static final String AUX_SPEC_OPACITY = "auxSpecOpacity";
		public static final String AUX_SPEC_DEFAULT_COLOR = "auxSpecDefaultColor";
		public static final String AUX_SPEC_ACTIVE_COLOR = "auxSpecActiveColor";
		public static final String AUX_SPEC_SHOW_REGEN = "auxSpecShowRegen";
		public static final String AUX_SPEC_REGEN_COLOR = "auxSpecRegenColor";
		public static final String AUX_SPEC_THRESHOLD_MODE = "auxSpecThresholdMode";
		public static final String AUX_SPEC_THRESHOLD_DIRECTION = "auxSpecThresholdDirection";
		public static final String AUX_SPEC_THRESHOLD = "auxSpecThreshold";
		public static final String AUX_SPEC_THRESHOLD_COLOR = "auxSpecThresholdColor";

		// Run Energy (aux)
		public static final String AUX_RUN_ENABLED = "auxRunEnabled";
		public static final String AUX_RUN_PLACEMENT_MODE = "auxRunPlacementMode";
		public static final String AUX_RUN_DOCK_SIDE = "auxRunDockSide";
		public static final String AUX_RUN_DOCK_SLOT = "auxRunDockSlot";
		public static final String AUX_RUN_DOCK_JUSTIFICATION = "auxRunDockJustification";
		public static final String AUX_RUN_CLAMP_TO_PANEL = "auxRunClampToPanel";
		public static final String AUX_RUN_FLOATING_ORIENTATION = "auxRunFloatingOrientation";
		public static final String AUX_RUN_THICKNESS = "auxRunThickness";
		public static final String AUX_RUN_LENGTH = "auxRunLength";
		public static final String AUX_RUN_SHOW_ICON = "auxRunShowIcon";
		public static final String AUX_RUN_ICON_SIZE = "auxRunIconSize";
		public static final String AUX_RUN_SHOW_COUNTER = "auxRunShowCounter";
		public static final String AUX_RUN_CONTENT_JUSTIFICATION = "auxRunContentJustification";
		public static final String AUX_RUN_CONTENT_OFFSET = "auxRunContentOffset";
		public static final String AUX_RUN_REVERSE_CONTENT = "auxRunReverseContent";
		public static final String AUX_RUN_OPACITY = "auxRunOpacity";
		public static final String AUX_RUN_DEFAULT_COLOR = "auxRunDefaultColor";
		public static final String AUX_RUN_ACTIVE_COLOR = "auxRunActiveColor";
		public static final String AUX_RUN_SHOW_REGEN = "auxRunShowRegen";
		public static final String AUX_RUN_REGEN_COLOR = "auxRunRegenColor";
		public static final String AUX_RUN_THRESHOLD_MODE = "auxRunThresholdMode";
		public static final String AUX_RUN_THRESHOLD_DIRECTION = "auxRunThresholdDirection";
		public static final String AUX_RUN_THRESHOLD = "auxRunThreshold";
		public static final String AUX_RUN_THRESHOLD_COLOR = "auxRunThresholdColor";

		// Warmth (aux) - no main-bar counterpart
		public static final String AUX_WARMTH_ENABLED = "auxWarmthEnabled";
		public static final String AUX_WARMTH_VISIBILITY = "auxWarmthVisibility";
		public static final String AUX_WARMTH_PLACEMENT_MODE = "auxWarmthPlacementMode";
		public static final String AUX_WARMTH_DOCK_SIDE = "auxWarmthDockSide";
		public static final String AUX_WARMTH_DOCK_SLOT = "auxWarmthDockSlot";
		public static final String AUX_WARMTH_DOCK_JUSTIFICATION = "auxWarmthDockJustification";
		public static final String AUX_WARMTH_CLAMP_TO_PANEL = "auxWarmthClampToPanel";
		public static final String AUX_WARMTH_FLOATING_ORIENTATION = "auxWarmthFloatingOrientation";
		public static final String AUX_WARMTH_THICKNESS = "auxWarmthThickness";
		public static final String AUX_WARMTH_LENGTH = "auxWarmthLength";
		public static final String AUX_WARMTH_SHOW_ICON = "auxWarmthShowIcon";
		public static final String AUX_WARMTH_ICON_SIZE = "auxWarmthIconSize";
		public static final String AUX_WARMTH_SHOW_COUNTER = "auxWarmthShowCounter";
		public static final String AUX_WARMTH_CONTENT_JUSTIFICATION = "auxWarmthContentJustification";
		public static final String AUX_WARMTH_CONTENT_OFFSET = "auxWarmthContentOffset";
		public static final String AUX_WARMTH_REVERSE_CONTENT = "auxWarmthReverseContent";
		public static final String AUX_WARMTH_OPACITY = "auxWarmthOpacity";
		public static final String AUX_WARMTH_COLOR = "auxWarmthColor";
		public static final String AUX_WARMTH_THRESHOLD_MODE = "auxWarmthThresholdMode";
		public static final String AUX_WARMTH_THRESHOLD_DIRECTION = "auxWarmthThresholdDirection";
		public static final String AUX_WARMTH_THRESHOLD = "auxWarmthThreshold";
		public static final String AUX_WARMTH_THRESHOLD_COLOR = "auxWarmthThresholdColor";

		// Enemy Hitpoints (aux) - no main-bar counterpart
		public static final String AUX_ENEMY_HP_ENABLED = "auxEnemyHpEnabled";
		public static final String AUX_ENEMY_HP_PLACEMENT_MODE = "auxEnemyHpPlacementMode";
		public static final String AUX_ENEMY_HP_DOCK_SIDE = "auxEnemyHpDockSide";
		public static final String AUX_ENEMY_HP_DOCK_SLOT = "auxEnemyHpDockSlot";
		public static final String AUX_ENEMY_HP_DOCK_JUSTIFICATION = "auxEnemyHpDockJustification";
		public static final String AUX_ENEMY_HP_CLAMP_TO_PANEL = "auxEnemyHpClampToPanel";
		public static final String AUX_ENEMY_HP_FLOATING_ORIENTATION = "auxEnemyHpFloatingOrientation";
		public static final String AUX_ENEMY_HP_THICKNESS = "auxEnemyHpThickness";
		public static final String AUX_ENEMY_HP_LENGTH = "auxEnemyHpLength";
		public static final String AUX_ENEMY_HP_SHOW_ICON = "auxEnemyHpShowIcon";
		public static final String AUX_ENEMY_HP_ICON_SIZE = "auxEnemyHpIconSize";
		public static final String AUX_ENEMY_HP_SHOW_COUNTER = "auxEnemyHpShowCounter";
		public static final String AUX_ENEMY_HP_COUNTER_FORMAT = "auxEnemyHpCounterFormat";
		public static final String AUX_ENEMY_HP_CONTENT_JUSTIFICATION = "auxEnemyHpContentJustification";
		public static final String AUX_ENEMY_HP_CONTENT_OFFSET = "auxEnemyHpContentOffset";
		public static final String AUX_ENEMY_HP_REVERSE_CONTENT = "auxEnemyHpReverseContent";
		public static final String AUX_ENEMY_HP_OPACITY = "auxEnemyHpOpacity";
		public static final String AUX_ENEMY_HP_COLOR = "auxEnemyHpColor";
		public static final String AUX_ENEMY_HP_THRESHOLD_MODE = "auxEnemyHpThresholdMode";
		public static final String AUX_ENEMY_HP_THRESHOLD_DIRECTION = "auxEnemyHpThresholdDirection";
		public static final String AUX_ENEMY_HP_THRESHOLD = "auxEnemyHpThreshold";
		public static final String AUX_ENEMY_HP_THRESHOLD_COLOR = "auxEnemyHpThresholdColor";

		// Real, permanent setting - not dev-only. Vertical vs horizontal,
		// and (for horizontal) which side of the icon the bar docks to - now
		// per-bar instead of shared, one key per main bar.
		public static final String HP_ORIENTATION = "hpOrientation";
		public static final String PRAYER_ORIENTATION = "prayerOrientation";
		public static final String SPEC_ORIENTATION = "specOrientation";
		public static final String RUN_ORIENTATION = "runOrientation";

		// Also temporary - one slider, widening getOrbsParent() AND
		// getMapParent() together. F7's diagnostic dump confirmed these
		// two are separate, nested widgets at the exact same native size
		// (both plain absolute sizing - safe to edit), while the orb's own
		// direct parent (ORBS_UNIVERSE) is a third, different widget sized
		// some other way at render time (original width 0) - editing IT
		// directly is what caused the screen-edge blowup and the
		// hide/show-to-settle glitch, so it's left alone now. Lives in the
		// Layout section now (not Bar) - it governs the drag zone, not any
		// one bar's own look.
		public static final String BAR_ZONE_WIDTH_OFFSET = "barZoneWidthOffset";

		// Also temporary - vertical counterpart to BAR_ZONE_WIDTH_OFFSET,
		// same two widgets, height instead of width.
		public static final String BAR_ZONE_HEIGHT_OFFSET = "barZoneHeightOffset";

		// Also temporary, same reasoning as BAR_WIDTH - the HP level number
		// (repositioned below the icon, see HpVialOverlay) isn't perfectly
		// centered under it natively; this nudges it left (-) or right (+)
		// to compensate.
		// BAR_NUMBER_HORIZONTAL_OFFSET/BAR_NUMBER_VERTICAL_OFFSET were here
		// temporarily for dialing in the HP number's position - settled at
		// 4/4 and hardcoded directly in HpVialOverlay, config removed.

		// Temporary again - NORTH's self-drawn number (see VialOverlay's
		// drawNorthNumber()) needs re-dialing in now that it's drawn by us
		// instead of repositioning the real widget.
		public static final String NORTH_NUMBER_X_OFFSET = "northNumberXOffset";
		public static final String NORTH_NUMBER_Y_OFFSET = "northNumberYOffset";

		//edit-modes custom positioning
		public static final String CUSTOM_LAYOUT_PREFIX = "custom_layout_";
		public static final String VANILLA_LAYOUT_PREFIX = "vanilla_layout_";
		public static final String FIXED_LAYOUT_PREFIX = "fixed_layout_";

		public static final class Core
		{
			public static final String WIKI_PLUGIN = ConfigGroup.Core.WIKI + "plugin";
			public static final String SHOW_WIKI_MINIMAP_BUTTON = "showWikiMinimapButton";

			public static final String MINIMAP_PLUGIN = ConfigGroup.Core.MINIMAP + "plugin";
			public static final String HIDE_MINIMAP = "hideMinimap";

			public static final String SNAPCORNER_PREFIX = "snapcorner_";
			public static final String SNAPCORNER_CONFIG_LOCATION = "_location";
		}
	}

	public static final class Enum
	{
		//contains the toplevel component ids (key: widget, value: widget)
		public static final int TOPLEVEL_COMPONENTS = 1131;
	}

	public static final class Varbit
	{
		public static final int CUTSCENE_STATUS = VarbitID.CUTSCENE_STATUS;
		public static final int MINIMAP_TOGGLE = VarbitID.MINIMAP_TOGGLE;

		//used to determine classic-resizable display mode
		public static final int RESIZABLE_STONE_ARRANGEMENT = VarbitID.RESIZABLE_STONE_ARRANGEMENT;

		//orb in-game settings
		public static final int ACTIVITY_ORB_TOGGLE = VarbitID.OPTION_CONTENT_RECOMMENDER_HIDE;
		public static final int STORE_ORB_TOGGLE = VarbitID.TLI_STOREBUTTON_TOGGLE_DESKTOP;
		public static final int WIKI_ICON_TOGGLE = VarbitID.WIKI_ICON_DISABLED;
	}

	public static final class VarClient
	{
		//which side panel tab is currently active (-1 is hidden)
		public static final int SIDE_PANEL_ID = VarClientID.TOPLEVEL_PANEL;
	}

	public static final class VarPlayer
	{
		//var trigger for the compass menu options
		public static final int MAP_FLAGS_CACHED = VarPlayerID.MAP_FLAGS_CACHED;
	}

	public static final class Script
	{
		//trigger remapping without the need for a valid scriptId
		public static final int FORCE_UPDATE = -1;

		//scripts related to noclickthrough updating
		public static final int ORBS_UPDATE_HEALTH = 446;
		public static final int ORBS_UPDATE_SPECENERGY = 2069;

		//orb specific updating
		public static final int ORBS_UPDATE_STORE = 2396;
		public static final int ORBS_UPDATE_ACTIVITY = 2480;
		public static final int WIKI_ICON_INIT = 3304;
		public static final int WIKI_ICON_UPDATE = ScriptID.WIKI_ICON_UPDATE;
		public static final int WORLD_MAP_UPDATE = 1700;
		//public static final int GRID_MASTER_ORB_UPDATE = 8222;

		//world map tooltip
		public static final int TOOLTIP_MOUSE_RELEASE = 837;

		//display mode change? used to cancel edit-mode when display mode change has fired (drop down)
		public static final int TOPLEVEL_SUBCHANGE = 903;

		//configure the current displays chat/side panel (init?)
		public static final int PROC_TOPLEVEL_SUBCHANGE = 908;

		//does a lot of top level widget state management
		public static final int TOPLEVEL_REDRAW = ScriptID.TOPLEVEL_REDRAW;

		//handles the menu op for the side panel tabs
		public static final int TOPLEVEL_SIDEBUTTON_OP = 914;

		//update the side panel tab icon/stones state
		public static final int TOPLEVEL_SIDE_CUSTOMIZE = 919;

		//on window resize, updates clickmasks/positions for the store/wiki and hides/shows the minimap (varbit checks)
		public static final int TOPLEVEL_RESIZE_CUSTOMIZE = 920;

		//detached minimap scripts for the compass menu op
		public static final int TOPLEVEL_COMPASS_OP = 1050;
		public static final int TOPLEVEL_COMPASS_SETOPS = 7044;

		//cs2
		public static final int COMSUBID1 = -2147483643;
		public static final int OPINDEX0 = -2147483644;
		public static final int COMPONENT0 = -2147483645;
	}

	public static final class Layout
	{
		public static final class Original
		{
			public static final int MAP_CONTAINER_WIDTH = 211;
			public static final int MAP_CONTAINER_HEIGHT = 207;

			public static final int ORBS_CONTAINER_WIDTH = 207;
			public static final int ORBS_CONTAINER_HEIGHT = 197;

			public static final int XP_DROPS_X = 0;
			public static final int XP_DROPS_Y = 17;

			public static final int HP_ORB_X = 0;
			public static final int HP_ORB_Y = 37;

			public static final int PRAYER_ORB_X = 0;
			public static final int PRAYER_ORB_Y = 71;

			public static final int RUN_ORB_X = 10;
			public static final int RUN_ORB_Y = 103;

			public static final int SPEC_ORB_X = 32;
			public static final int SPEC_ORB_Y = 128;

			public static final int STORE_ORB_X = 85;
			public static final int STORE_ORB_Y = 143;

			public static final int ACTIVITY_ORB_X = 55;
			public static final int ACTIVITY_ORB_Y = 162;

			public static final int WORLD_MAP_X = 0;
			public static final int WORLD_MAP_Y = 115;

			public static final int WIKI_ICON_X = 0;
			public static final int WIKI_ICON_Y = 135;

			public static final int WIKI_HEIGHT = 34;

			public static final int LOGOUT_X = 2;
			public static final int LOGOUT_Y = 2;

			public static final int COMPASS_X = 34;
			public static final int COMPASS_Y = 5;
			public static final int COMPASS_DIMENSION = 35;

			public static final int COMPASS_OP_X = COMPASS_X - 2;
			public static final int COMPASS_OP_Y = COMPASS_Y - 2;

			public static final int MINIMAP_X = 6;
			public static final int MINIMAP_Y = 8;
			public static final int MINIMAP_DIMENSION = 152;
		}

		public static final class Vertical
		{
			public static final int LAYOUT_ID = 0;

			public static final int MAP_CONTAINER_WIDTH = 96;
			public static final int MAP_CONTAINER_HEIGHT = 193;

			public static final int XP_DROPS_X = 7;
			public static final int XP_DROPS_Y = 42;

			public static final int HP_ORB_X = 39;
			public static final int HP_ORB_Y = 39;

			public static final int PRAYER_ORB_X = 39;
			public static final int PRAYER_ORB_Y = 73;

			public static final int RUN_ORB_X = 39;
			public static final int RUN_ORB_Y = 107;

			public static final int SPEC_ORB_X = 39;
			public static final int SPEC_ORB_Y = 141;

			public static final int STORE_ORB_X = 3;
			public static final int STORE_ORB_Y = 101;

			public static final int ACTIVITY_ORB_X = 3;
			public static final int ACTIVITY_ORB_Y = 136;

			public static final int WORLD_MAP_X = 5;
			public static final int WORLD_MAP_Y = 70;

			public static final int WIKI_ICON_X = 38;
			public static final int WIKI_ICON_Y = 178;

			public static final int LOGOUT_X = 71;
			public static final int LOGOUT_Y = 2;

			public static final int COMPASS_X = Original.COMPASS_X - 14;
			public static final int COMPASS_Y = 1;
		}

		public static final class Horizontal
		{
			public static final int LAYOUT_ID = 1;

			public static final int MAP_CONTAINER_WIDTH = 209;
			public static final int MAP_CONTAINER_HEIGHT = 102;

			public static final int XP_DROPS_X = 179;
			public static final int XP_DROPS_Y = 25;

			public static final int HP_ORB_X = 35;
			public static final int HP_ORB_Y = 33;

			public static final int PRAYER_ORB_X = 35;
			public static final int PRAYER_ORB_Y = 68;

			public static final int RUN_ORB_X = 93;
			public static final int RUN_ORB_Y = 33;

			public static final int SPEC_ORB_X = 93;
			public static final int SPEC_ORB_Y = 68;

			public static final int STORE_ORB_X = 0;
			public static final int STORE_ORB_Y = 34;

			public static final int ACTIVITY_ORB_X = 0;
			public static final int ACTIVITY_ORB_Y = 68;

			public static final int WORLD_MAP_X = 146;
			public static final int WORLD_MAP_Y = 16;

			public static final int WIKI_ICON_X = 151;
			public static final int WIKI_ICON_Y = 88;

			public static final int LOGOUT_X = 182;
			public static final int LOGOUT_Y = 1;

			public static final int COMPASS_X = Original.COMPASS_X + 115;
			public static final int COMPASS_Y = Original.COMPASS_Y + 41;
		}

		public static final class HorizontalWide
		{
			public static final int LAYOUT_ID = 2;

			public static final int MAP_CONTAINER_WIDTH = 228;
			public static final int MAP_CONTAINER_HEIGHT = 79;

			public static final int XP_DROPS_X = 75;
			public static final int XP_DROPS_Y = 12;

			public static final int HP_ORB_X = 0;
			public static final int HP_ORB_Y = 45;

			public static final int PRAYER_ORB_X = 57;
			public static final int PRAYER_ORB_Y = 45;

			public static final int RUN_ORB_X = 114;
			public static final int RUN_ORB_Y = 45;

			public static final int SPEC_ORB_X = 171;
			public static final int SPEC_ORB_Y = 45;

			public static final int STORE_ORB_X = 37;
			public static final int STORE_ORB_Y = 10;

			public static final int ACTIVITY_ORB_X = 0;
			public static final int ACTIVITY_ORB_Y = 10;

			public static final int WORLD_MAP_X = 167;
			public static final int WORLD_MAP_Y = 0;

			public static final int WIKI_ICON_X = 162;
			public static final int WIKI_ICON_Y = 31;

			public static final int LOGOUT_X = 200;
			public static final int LOGOUT_Y = 2;

			public static final int COMPASS_X = Original.COMPASS_X + 76;
			public static final int COMPASS_Y = 1;
		}

		public static final class Custom
		{
			public static final int LAYOUT_ID = 3;
		}

		public static final class MinimapOverlay
		{
			public static final int CONTAINER_WIDTH = 182;
			public static final int CONTAINER_HEIGHT = 166;
			public static final int[] NO_CLICK_Y = {4, 44, 100, 125, 140, 155};
			public static final int[] NO_CLICK_WIDTH = {178, 166, 161, 151, 141, 121};
			public static final int[] NO_CLICK_HEIGHT = {40, 56, 25, 15, 15, 11};

			public static final int MINIMAP_CONTENT = 1338;
			public static final int COMPASS_CONTENT = 1339;
		}

		//minimap toggle button location - always below the map now, the only
		//placement choice Vicious Bars kept
		public static final int BELOW_MAP_MINIMAP_BUTTON_X = 120;
		public static final int BELOW_MAP_MINIMAP_BUTTON_Y = 155;

		//vanilla/"original" Y fallback for the toggle button's own SetValue
		//table (Button.java) - unrelated to the placement choice above,
		//kept as-is
		public static final int DEFAULT_MINIMAP_BUTTON_Y = 176;

		//toggle button dimensions
		public static final int TOGGLE_BUTTON_SIZE = 17;

		//compass frame dimensions
		public static final int COMPASS_FRAME_SIZE = 43;

		//misc widget dimensions
		public static final int COMPASS_SIZE = 36;
		public static final int LOGOUT_X_WIDTH = 26;
		public static final int LOGOUT_X_HEIGHT = 23;

		//toggle button opacity when onMouseOver || onMouseLeave
		public static final int OPACITY = 0;
		public static final int OPACITY_HOVER = 130;

		public static final int ORBS_CONTAINER_OFFSET_Y = 10;
		public static final int LOGOUT_X_ICON_OPACITY = 100;
		public static final int SPEC_ORB_INDICATOR_OPACITY = 50;
		public static final int EDIT_MODE_HIDDEN_OPACITY = 160;
		public static final int EDIT_MODE_BACKGROUND_OPACITY = 220;
	}

	public static final class MenuOp
	{
		public static final String SHOW = "Show";
		public static final String HIDE = "Hide";

		public static final String MINIMAP_OP = "Minimap";

		public static final String LOGOUT_OP = "Logout";
		public static final String WORLD_SWITCHER_OP = "World switcher";

		public static final String ENABLE = "Enable";
		public static final String EDIT_MODE = "edit mode";

		public static final String SAVE = "Save";
		public static final String CHANGES = "changes";

		public static final String RESET = "Reset";
		public static final String RESET_ALL = RESET + " all positions";

		public static final Color MENU_TARGET = JagexColors.MENU_TARGET;
		public static final Color RED = JagexColors.CHAT_FC_TEXT_TRANSPARENT_BACKGROUND;

		public static final int OP_INDEX_0 = 0;
		public static final int HANDLER_TOGGLE_OP_INDEX = 6;
		public static final int RESET_POSITION_OP_INDEX = 7;
		public static final int RESET_ALL_OP_INDEX = 8;
		public static final int EDIT_MODE_OP_INDEX = 9;
	}

	public static final class Sprite
	{
		//orb related sprites
		public static final int FRAME = SpriteID.OrbFrame.FRAME;
		public static final int FRAME_HOVERED = SpriteID.OrbFrame.FRAME_HOVERED;

		//for edit-mode
		public static final int FIXED_MINIMAP_MASK = SpriteID.FIXED_MAP_MASK;

		//minimap overlay sprites
		public static final int COMPASS_MASK = SpriteID.RESIZE_COMPASS_MASK;
		public static final int MINIMAP_MASK = SpriteID.RESIZE_MAP_MASK;
		public static final int MINIMAP_FRAME = SpriteID.OSRS_STRETCH_MAPSURROUND;
		public static final int LOGOUT_X_BUTTON = SpriteID.CloseButtons._7;
		public static final int COMPASS_FRAME = SpriteID.COMPASS_OUTLINE;

		//toggle button sprites
		public static final int HIDDEN = SpriteID.GroundItemsVisibility._1;
		public static final int VISIBLE = SpriteID.GroundItemsVisibility._0;
	}

	public static final class Widgets
	{
		public static final class MinimapOverlay
		{
			//seems BuffBar could be used for the leagues relic display?
			//can instead use StatBoostsHud (seems to be mobile/steam only)
			//script can stay the same as entry since it fires frequently
			public static final int UNIVERSE = InterfaceID.StatBoostsHud.UNIVERSE;//InterfaceID.BuffBar.UNIVERSE;
		}

		//orb widget ids
		public static final class Orb
		{
			public static final int UNIVERSE = InterfaceID.Orbs.UNIVERSE;

			public static final int XP_DROPS = InterfaceID.Orbs.XP_DROPS;

			public static final int HP_ORB = InterfaceID.Orbs.ORB_HEALTH;
			public static final int HP_ORB_BACKING = InterfaceID.Orbs.HEALTH_BACKING;
			public static final int HP_ORB_BUTTON = InterfaceID.Orbs.HEALTHBUTTON;
			public static final int HP_ORB_INDICATOR = InterfaceID.Orbs.HEALTH_INDICATOR;
			public static final int HP_ORB_ICON = InterfaceID.Orbs.ORB_HEALTH_HEART_ICON;
			public static final int HP_ORB_EMPTY = InterfaceID.Orbs.HEALTH_EMPTY_CONTENTS;

			public static final int PRAY_ORB = InterfaceID.Orbs.ORB_PRAYER;
			public static final int PRAY_ORB_BACKING = InterfaceID.Orbs.PRAYER_BACKING;
			public static final int PRAY_ORB_BUTTON = InterfaceID.Orbs.PRAYERBUTTON;
			public static final int PRAY_ORB_INDICATOR = InterfaceID.Orbs.PRAYER_INDICATOR;
			public static final int PRAY_ORB_ICON = InterfaceID.Orbs.PRAYER_ICON;
			public static final int PRAY_ORB_EMPTY = InterfaceID.Orbs.ORB_PRAYER_EMPTY_GRAPHIC0;

			public static final int RUN_ORB = InterfaceID.Orbs.ORB_RUNENERGY;
			public static final int RUN_ORB_BACKING = InterfaceID.Orbs.RUNENERGY_BACKING;
			public static final int RUN_ORB_BUTTON = InterfaceID.Orbs.RUNBUTTON;
			public static final int RUN_ORB_INDICATOR = InterfaceID.Orbs.RUNENERGY_INDICATOR;
			public static final int RUN_ORB_ICON = InterfaceID.Orbs.RUNENERGY_ICON;
			public static final int RUN_ORB_EMPTY = InterfaceID.Orbs.ORB_RUNENERGY_EMPTY_GRAPHIC0;

			public static final int SPEC_ORB = InterfaceID.Orbs.ORB_SPECENERGY;
			public static final int SPEC_ORB_BACKING = InterfaceID.Orbs.SPECENERGY_BACKING;
			public static final int SPEC_ORB_BUTTON = InterfaceID.Orbs.SPECBUTTON;
			public static final int SPEC_ORB_INDICATOR = InterfaceID.Orbs.SPECENERGY_INDICATOR;
			public static final int SPEC_ORB_ICON = InterfaceID.Orbs.SPECENERGY_ICON;
			public static final int SPEC_ORB_EMPTY = InterfaceID.Orbs.ORB_SPECENERGY_EMPTY_GRAPHIC0;

			public static final int ACTIVITY_ORB = InterfaceID.Orbs.ORB_CONTENTRECOM;
			public static final int ACTIVITY_ORB_BACKING = InterfaceID.Orbs.CR_BACKING;
			public static final int ACTIVITY_ORB_INDICATOR = InterfaceID.Orbs.CR_INDICATOR;
			public static final int ACTIVITY_ORB_ICON = InterfaceID.Orbs.CR_ICON;

			public static final int STORE_ORB = InterfaceID.Orbs.ORB_STORE;
			public static final int STORE_ORB_BACKING = InterfaceID.Orbs.STORE_BACKING;
			public static final int STORE_ORB_INDICATOR = InterfaceID.Orbs.STORE_INDICATOR;
			public static final int STORE_ORB_ICON = InterfaceID.Orbs.STORE_ICON;

			public static final int WIKI_ICON = InterfaceID.Orbs.WIKI;
			public static final int WIKI_CONTAINER_VANILLA = InterfaceID.Orbs.WIKI_ICON;
			public static final int WIKI_ICON_VANILLA = InterfaceID.Orbs.WIKI_ICON_GRAPHIC;

			public static final int WORLD_MAP = InterfaceID.Orbs.ORB_WORLDMAP;
			public static final int WORLD_MAP_BACKING = InterfaceID.Orbs.WORLDMAP_BACKING;
			public static final int WORLD_MAP_ICON = InterfaceID.Orbs.WORLDMAP;
			public static final int WORLD_MAP_TOOLTIP = InterfaceID.Orbs.TOOLTIP;
		}

		//fixed mode widget ids
		public static final class Fixed
		{
			public static final int MAP_CONTAINER = InterfaceID.Toplevel.MAPCONTAINER;
			public static final int MINIMAP_MASK = InterfaceID.Toplevel.MINIMAP;
			public static final int ORBS = InterfaceID.Toplevel.ORBS;
		}

		//classic-resizable widget ids
		public static final class Classic
		{
			public static final int MAP_CONTAINER = InterfaceID.ToplevelOsrsStretch.MAP_CONTAINER;
			public static final int ORBS = InterfaceID.ToplevelOsrsStretch.ORBS;
			public static final int MAP_NOCLICK_0 = InterfaceID.ToplevelOsrsStretch.MAP_NOCLICK_0;
			public static final int MAP_NOCLICK_1 = InterfaceID.ToplevelOsrsStretch.MAP_NOCLICK_1;
			public static final int MAP_NOCLICK_2 = InterfaceID.ToplevelOsrsStretch.MAP_NOCLICK_2;
			public static final int MAP_NOCLICK_3 = InterfaceID.ToplevelOsrsStretch.MAP_NOCLICK_3;
			public static final int MAP_NOCLICK_4 = InterfaceID.ToplevelOsrsStretch.MAP_NOCLICK_4;
			public static final int MAP_NOCLICK_5 = InterfaceID.ToplevelOsrsStretch.MAP_NOCLICK_5;
			public static final int MINIMAP_MASK = InterfaceID.ToplevelOsrsStretch.MINIMAP;
			public static final int MINIMAP = InterfaceID.ToplevelOsrsStretch.MAP_MINIMAP_GRAPHIC9;
			public static final int COMPASS = InterfaceID.ToplevelOsrsStretch.MAP_MINIMAP_GRAPHIC6;
			public static final int COMPASS_OPTIONS = InterfaceID.ToplevelOsrsStretch.COMPASSCLICK;
			public static final int COMPASS_PARENT = InterfaceID.ToplevelOsrsStretch.MAP_MINIMAP;
			public static final int SIDE_TOP = InterfaceID.ToplevelOsrsStretch.SIDE_TOP;
		}

		//modern-resizable widget ids
		public static final class Modern
		{
			public static final int MAP_CONTAINER = InterfaceID.ToplevelPreEoc.MAP_CONTAINER;
			public static final int ORBS = InterfaceID.ToplevelPreEoc.ORBS;
			public static final int MAP_NOCLICK_0 = InterfaceID.ToplevelPreEoc.MAP_NOCLICK_0;
			public static final int MAP_NOCLICK_1 = InterfaceID.ToplevelPreEoc.MAP_NOCLICK_1;
			public static final int MAP_NOCLICK_2 = InterfaceID.ToplevelPreEoc.MAP_NOCLICK_2;
			public static final int MAP_NOCLICK_3 = InterfaceID.ToplevelPreEoc.MAP_NOCLICK_3;
			public static final int MAP_NOCLICK_4 = InterfaceID.ToplevelPreEoc.MAP_NOCLICK_4;
			public static final int MAP_NOCLICK_5 = InterfaceID.ToplevelPreEoc.MAP_NOCLICK_5;
			public static final int MINIMAP_MASK = InterfaceID.ToplevelPreEoc.MINIMAP;
			public static final int MINIMAP = InterfaceID.ToplevelPreEoc.MAP_MINIMAP_GRAPHIC9;
			public static final int COMPASS = InterfaceID.ToplevelPreEoc.MAP_MINIMAP_GRAPHIC6;
			public static final int COMPASS_OPTIONS = InterfaceID.ToplevelPreEoc.COMPASSCLICK;
			public static final int COMPASS_PARENT = InterfaceID.ToplevelPreEoc.MAP_MINIMAP;
			public static final int LOGOUT_X_ICON = InterfaceID.ToplevelPreEoc.ICON10;
			public static final int LOGOUT_X_STONE = InterfaceID.ToplevelPreEoc.STONE10;
			public static final int SIDE_MOVABLE = InterfaceID.ToplevelPreEoc.SIDE_MOVABLE;
		}
	}
}
