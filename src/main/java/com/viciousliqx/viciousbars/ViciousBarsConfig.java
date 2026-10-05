package com.viciousliqx.viciousbars;

import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup(ViciousBarsConfig.GROUP)
public interface ViciousBarsConfig extends Config
{
	String GROUP = "vicious-bars";

	// ------------------------------------------------------------------
	// Hitpoints
	// ------------------------------------------------------------------

	@ConfigSection(
		name = "Hitpoints",
		description = "The Hitpoints bar",
		position = 0,
		closedByDefault = true
	)
	String hpSection = "hpSection";

	@ConfigItem(
		keyName = "hpEnabled",
		name = "Enabled",
		description = "Show the Hitpoints bar",
		section = hpSection,
		position = 0
	)
	default boolean hpEnabled()
	{
		return true;
	}

	@ConfigItem(
		keyName = "hpShowHealPreview",
		name = "Show heal preview",
		description = "While hovering a food/potion item, previews how much it would heal by showing the extra amount in a lighter shade beyond your current Hitpoints. Requires the Item Stats plugin to be enabled - it's what actually knows how much each item heals.",
		section = hpSection,
		position = 1
	)
	default boolean hpShowHealPreview()
	{
		return true;
	}

	@ConfigItem(
		keyName = "hpWarningEnabled",
		name = "Warning pulse",
		description = "Pulses the empty part of the vial once Hitpoints drops to or below the Warning threshold.",
		section = hpSection,
		position = 2
	)
	default boolean hpWarningEnabled()
	{
		return false;
	}

	@ConfigItem(
		keyName = "hpWarningThreshold",
		name = "Warning threshold",
		description = "Pulse activates once Hitpoints drops to or below this percentage.",
		section = hpSection,
		position = 3
	)
	@Range(min = 0, max = 100)
	default int hpWarningThreshold()
	{
		return 25;
	}

	@ConfigItem(
		keyName = "hpWarningColor",
		name = "Warning color",
		description = "Color the empty part of the vial pulses while the Warning threshold is active.",
		section = hpSection,
		position = 4
	)
	default Color hpWarningColor()
	{
		return new Color(220, 30, 30);
	}

	@ConfigItem(
		keyName = "hpWarningOpacity",
		name = "Warning opacity",
		description = "0-100, the pulse's peak opacity - it breathes between 0 and this value, not a flat level.",
		section = hpSection,
		position = 5
	)
	@Range(min = 0, max = 100)
	default int hpWarningOpacity()
	{
		return 100;
	}

	@ConfigItem(
		keyName = "hpPlacementMode",
		name = "Bar type",
		description = "Docked: docked to a side of the panel. Floating: drag anywhere, like Custom Vital Bars.",
		section = hpSection,
		position = 6
	)
	default PlacementMode hpPlacementMode()
	{
		return PlacementMode.FIXED;
	}

	@ConfigItem(
		keyName = "hpDockSide",
		name = "Dock side",
		description = "Which side of the panel to dock to when Bar type is Docked. Determines orientation: left/right = vertical, top/bottom = horizontal.",
		section = hpSection,
		position = 7
	)
	default DockSide hpDockSide()
	{
		return DockSide.LEFT;
	}

	@ConfigItem(
		keyName = "hpDockSlot",
		name = "Dock slot",
		description = "Order among other bars docked to the same side. 1 = closest to the panel corner, higher numbers further out.",
		section = hpSection,
		position = 8
	)
	@Range(min = 1)
	default int hpDockSlot()
	{
		return 1;
	}

	@ConfigItem(
		keyName = "hpDockJustification",
		name = "Dock slot orientation",
		description = "Where this bar sits along the panel's length when Bar type is Docked. Start (default) = flush with the panel's top/left corner, today's behavior. End = flush with the bottom/right corner. Center = centered. Give two bars the same Dock side + Dock slot with Start/End (and a Length under half the panel's own length, or Clamp slot off) to have them share a slot.",
		section = hpSection,
		position = 9
	)
	default Justification hpDockJustification()
	{
		return Justification.START;
	}

	@ConfigItem(
		keyName = "hpClampSlot",
		name = "Clamp slot",
		description = "When on, this bar's Length is overridden to match the panel's own dimension (height for left/right Dock sides, width for top/bottom) instead of using its own Length setting. Leave off to size this bar manually - needed if it shares a Dock slot with another bar via Dock slot orientation.",
		section = hpSection,
		position = 10
	)
	default boolean hpClampSlot()
	{
		return false;
	}

	@ConfigItem(
		keyName = "hpFloatingOrientation",
		name = "Floating orientation",
		description = "Bar orientation when Bar type is Floating.",
		section = hpSection,
		position = 11
	)
	default Orientation hpFloatingOrientation()
	{
		return Orientation.VERTICAL;
	}

	@ConfigItem(
		keyName = "hpShowIcon",
		name = "Show icon",
		description = "Show the Hitpoints orb icon on the bar.",
		section = hpSection,
		position = 13
	)
	default boolean hpShowIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "hpIconSize",
		name = "Icon size",
		description = "Pixel size (width and height) of the icon on the bar.",
		section = hpSection,
		position = 14
	)
	@Range(min = 1)
	default int hpIconSize()
	{
		return 18;
	}

	@ConfigItem(
		keyName = "hpShowCounter",
		name = "Show counter",
		description = "Show the current Hitpoints value as text on the bar.",
		section = hpSection,
		position = 15
	)
	default boolean hpShowCounter()
	{
		return true;
	}

	@ConfigItem(
		keyName = "hpJustification",
		name = "Content justification",
		description = "Where the icon/counter sit along the bar. Start = left (horizontal) or top (vertical). End = right (horizontal) or bottom (vertical).",
		section = hpSection,
		position = 16
	)
	default Justification hpJustification()
	{
		return Justification.CENTER;
	}

	@ConfigItem(
		keyName = "hpContentOffset",
		name = "Content offset",
		description = "Nudges the icon/counter along the bar by this many pixels - positive moves toward the far end, negative moves back toward the near end. For small fixes without having to change Content justification.",
		section = hpSection,
		position = 17
	)
	default int hpContentOffset()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "hpReverseContent",
		name = "Reverse content order",
		description = "Draw the counter before the icon (counter above icon on a vertical bar, counter left of icon on a horizontal bar) instead of the default icon-then-counter.",
		section = hpSection,
		position = 18
	)
	default boolean hpReverseContent()
	{
		return false;
	}

	@ConfigItem(
		keyName = "hpThickness",
		name = "Thickness",
		description = "The bar's short dimension in pixels (width if vertical, height if horizontal).",
		section = hpSection,
		position = 19
	)
	@Range(min = 1)
	default int hpThickness()
	{
		return 24;
	}

	@ConfigItem(
		keyName = "hpLength",
		name = "Length",
		description = "The bar's long dimension in pixels (height if vertical, width if horizontal).",
		section = hpSection,
		position = 20
	)
	@Range(min = 1)
	default int hpLength()
	{
		return 150;
	}

	@ConfigItem(
		keyName = "hpOpacity",
		name = "Opacity",
		description = "0 = fully transparent, 100 = fully opaque. Scales both the background and the fill color.",
		section = hpSection,
		position = 21
	)
	@Range(min = 0, max = 100)
	default int hpOpacity()
	{
		return 100;
	}

	// ------------------------------------------------------------------
	// Prayer
	// ------------------------------------------------------------------

	@ConfigSection(
		name = "Prayer",
		description = "The Prayer bar",
		position = 1,
		closedByDefault = true
	)
	String prayerSection = "prayerSection";

	@ConfigItem(
		keyName = "prayerEnabled",
		name = "Enabled",
		description = "Show the Prayer bar",
		section = prayerSection,
		position = 0
	)
	default boolean prayerEnabled()
	{
		return true;
	}

	@ConfigItem(
		keyName = "prayerMetronomeEnabled",
		name = "Metronome",
		description = "Shows a small marker sweeping across the bar once per game tick, with the icon flashing each time it completes a lap - a timing aid for prayer flicking.",
		section = prayerSection,
		position = 1
	)
	default boolean prayerMetronomeEnabled()
	{
		return false;
	}

	@ConfigItem(
		keyName = "prayerMetronomeOnlyInCombat",
		name = "Metronome only in combat",
		description = "Only show the metronome while you're in combat (same combat detection the Spec bar's Activation threshold glow uses) - hidden the rest of the time even if Metronome is on.",
		section = prayerSection,
		position = 2
	)
	default boolean prayerMetronomeOnlyInCombat()
	{
		return false;
	}

	@ConfigItem(
		keyName = "prayerMetronomeReversed",
		name = "Reverse metronome direction",
		description = "Sweep the marker from the far end of the bar back toward the near end (e.g. bottom-to-top on a vertical bar) instead of the default near-to-far.",
		section = prayerSection,
		position = 3
	)
	default boolean prayerMetronomeReversed()
	{
		return false;
	}

	@ConfigItem(
		keyName = "prayerWarningEnabled",
		name = "Warning pulse",
		description = "Pulses the empty part of the vial once Prayer points drops to or below the Warning threshold.",
		section = prayerSection,
		position = 4
	)
	default boolean prayerWarningEnabled()
	{
		return false;
	}

	@ConfigItem(
		keyName = "prayerWarningThreshold",
		name = "Warning threshold",
		description = "Pulse activates once Prayer points drops to or below this percentage.",
		section = prayerSection,
		position = 5
	)
	@Range(min = 0, max = 100)
	default int prayerWarningThreshold()
	{
		return 25;
	}

	@ConfigItem(
		keyName = "prayerWarningColor",
		name = "Warning color",
		description = "Color the empty part of the vial pulses while the Warning threshold is active.",
		section = prayerSection,
		position = 6
	)
	default Color prayerWarningColor()
	{
		return new Color(220, 30, 30);
	}

	@ConfigItem(
		keyName = "prayerWarningOpacity",
		name = "Warning opacity",
		description = "0-100, the pulse's peak opacity - it breathes between 0 and this value, not a flat level.",
		section = prayerSection,
		position = 7
	)
	@Range(min = 0, max = 100)
	default int prayerWarningOpacity()
	{
		return 100;
	}

	@ConfigItem(
		keyName = "prayerPlacementMode",
		name = "Bar type",
		description = "Docked: docked to a side of the panel. Floating: drag anywhere, like Custom Vital Bars.",
		section = prayerSection,
		position = 8
	)
	default PlacementMode prayerPlacementMode()
	{
		return PlacementMode.FIXED;
	}

	@ConfigItem(
		keyName = "prayerDockSide",
		name = "Dock side",
		description = "Which side of the panel to dock to when Bar type is Docked. Determines orientation: left/right = vertical, top/bottom = horizontal.",
		section = prayerSection,
		position = 9
	)
	default DockSide prayerDockSide()
	{
		return DockSide.LEFT;
	}

	@ConfigItem(
		keyName = "prayerDockSlot",
		name = "Dock slot",
		description = "Order among other bars docked to the same side. 1 = closest to the panel corner, higher numbers further out.",
		section = prayerSection,
		position = 10
	)
	@Range(min = 1)
	default int prayerDockSlot()
	{
		return 2;
	}

	@ConfigItem(
		keyName = "prayerDockJustification",
		name = "Dock slot orientation",
		description = "Where this bar sits along the panel's length when Bar type is Docked. Start (default) = flush with the panel's top/left corner, today's behavior. End = flush with the bottom/right corner. Center = centered. Give two bars the same Dock side + Dock slot with Start/End (and a Length under half the panel's own length, or Clamp slot off) to have them share a slot.",
		section = prayerSection,
		position = 11
	)
	default Justification prayerDockJustification()
	{
		return Justification.START;
	}

	@ConfigItem(
		keyName = "prayerClampSlot",
		name = "Clamp slot",
		description = "When on, this bar's Length is overridden to match the panel's own dimension (height for left/right Dock sides, width for top/bottom) instead of using its own Length setting. Leave off to size this bar manually - needed if it shares a Dock slot with another bar via Dock slot orientation.",
		section = prayerSection,
		position = 12
	)
	default boolean prayerClampSlot()
	{
		return false;
	}

	@ConfigItem(
		keyName = "prayerFloatingOrientation",
		name = "Floating orientation",
		description = "Bar orientation when Bar type is Floating.",
		section = prayerSection,
		position = 13
	)
	default Orientation prayerFloatingOrientation()
	{
		return Orientation.VERTICAL;
	}

	@ConfigItem(
		keyName = "prayerShowIcon",
		name = "Show icon",
		description = "Show the Prayer orb icon on the bar.",
		section = prayerSection,
		position = 15
	)
	default boolean prayerShowIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "prayerIconSize",
		name = "Icon size",
		description = "Pixel size (width and height) of the icon on the bar.",
		section = prayerSection,
		position = 16
	)
	@Range(min = 1)
	default int prayerIconSize()
	{
		return 18;
	}

	@ConfigItem(
		keyName = "prayerShowCounter",
		name = "Show counter",
		description = "Show the current Prayer value as text on the bar.",
		section = prayerSection,
		position = 17
	)
	default boolean prayerShowCounter()
	{
		return true;
	}

	@ConfigItem(
		keyName = "prayerJustification",
		name = "Content justification",
		description = "Where the icon/counter sit along the bar. Start = left (horizontal) or top (vertical). End = right (horizontal) or bottom (vertical).",
		section = prayerSection,
		position = 18
	)
	default Justification prayerJustification()
	{
		return Justification.CENTER;
	}

	@ConfigItem(
		keyName = "prayerContentOffset",
		name = "Content offset",
		description = "Nudges the icon/counter along the bar by this many pixels - positive moves toward the far end, negative moves back toward the near end. For small fixes without having to change Content justification.",
		section = prayerSection,
		position = 19
	)
	default int prayerContentOffset()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "prayerReverseContent",
		name = "Reverse content order",
		description = "Draw the counter before the icon (counter above icon on a vertical bar, counter left of icon on a horizontal bar) instead of the default icon-then-counter.",
		section = prayerSection,
		position = 20
	)
	default boolean prayerReverseContent()
	{
		return false;
	}

	@ConfigItem(
		keyName = "prayerThickness",
		name = "Thickness",
		description = "The bar's short dimension in pixels (width if vertical, height if horizontal).",
		section = prayerSection,
		position = 21
	)
	@Range(min = 1)
	default int prayerThickness()
	{
		return 24;
	}

	@ConfigItem(
		keyName = "prayerLength",
		name = "Length",
		description = "The bar's long dimension in pixels (height if vertical, width if horizontal).",
		section = prayerSection,
		position = 22
	)
	@Range(min = 1)
	default int prayerLength()
	{
		return 150;
	}

	@ConfigItem(
		keyName = "prayerActiveColor",
		name = "Active color",
		description = "Bar color while Quick Prayers is active.",
		section = prayerSection,
		position = 23
	)
	default Color prayerActiveColor()
	{
		return new Color(60, 170, 220);
	}

	@ConfigItem(
		keyName = "prayerInactiveColor",
		name = "Inactive color",
		description = "Bar color while Quick Prayers is inactive - matches the orb's own darker navy when off.",
		section = prayerSection,
		position = 24
	)
	default Color prayerInactiveColor()
	{
		return new Color(25, 35, 80);
	}

	@ConfigItem(
		keyName = "prayerOpacity",
		name = "Opacity",
		description = "0 = fully transparent, 100 = fully opaque. Scales both the background and the fill color.",
		section = prayerSection,
		position = 25
	)
	@Range(min = 0, max = 100)
	default int prayerOpacity()
	{
		return 100;
	}

	// ------------------------------------------------------------------
	// Special Attack
	// ------------------------------------------------------------------

	@ConfigSection(
		name = "Special Attack",
		description = "The Special Attack bar",
		position = 2,
		closedByDefault = true
	)
	String specSection = "specSection";

	@ConfigItem(
		keyName = "specEnabled",
		name = "Enabled",
		description = "Show the Special Attack bar",
		section = specSection,
		position = 0
	)
	default boolean specEnabled()
	{
		return true;
	}

	@ConfigItem(
		keyName = "specThreshold",
		name = "Activation threshold",
		description = "When Special Attack reaches this percentage, the icon lights up - only while you're in combat. Separate from the Warning threshold below, which fires on a LOW charge instead.",
		section = specSection,
		position = 1
	)
	@Range(min = 0, max = 100)
	default int specActivationThreshold()
	{
		return 50;
	}

	@ConfigItem(
		keyName = "specWarningEnabled",
		name = "Warning pulse",
		description = "Pulses the empty part of the vial once Special Attack drops to or below the Warning threshold.",
		section = specSection,
		position = 2
	)
	default boolean specWarningEnabled()
	{
		return false;
	}

	@ConfigItem(
		keyName = "specWarningThreshold",
		name = "Warning threshold",
		description = "Pulse activates once Special Attack drops to or below this percentage.",
		section = specSection,
		position = 3
	)
	@Range(min = 0, max = 100)
	default int specWarningThreshold()
	{
		return 25;
	}

	@ConfigItem(
		keyName = "specWarningColor",
		name = "Warning color",
		description = "Color the empty part of the vial pulses while the Warning threshold is active.",
		section = specSection,
		position = 4
	)
	default Color specWarningColor()
	{
		return new Color(220, 30, 30);
	}

	@ConfigItem(
		keyName = "specWarningOpacity",
		name = "Warning opacity",
		description = "0-100, the pulse's peak opacity - it breathes between 0 and this value, not a flat level.",
		section = specSection,
		position = 5
	)
	@Range(min = 0, max = 100)
	default int specWarningOpacity()
	{
		return 100;
	}

	@ConfigItem(
		keyName = "specPlacementMode",
		name = "Bar type",
		description = "Docked: docked to a side of the panel. Floating: drag anywhere, like Custom Vital Bars.",
		section = specSection,
		position = 6
	)
	default PlacementMode specPlacementMode()
	{
		return PlacementMode.FIXED;
	}

	@ConfigItem(
		keyName = "specDockSide",
		name = "Dock side",
		description = "Which side of the panel to dock to when Bar type is Docked. Determines orientation: left/right = vertical, top/bottom = horizontal.",
		section = specSection,
		position = 7
	)
	default DockSide specDockSide()
	{
		return DockSide.RIGHT;
	}

	@ConfigItem(
		keyName = "specDockSlot",
		name = "Dock slot",
		description = "Order among other bars docked to the same side. 1 = closest to the panel corner, higher numbers further out.",
		section = specSection,
		position = 8
	)
	@Range(min = 1)
	default int specDockSlot()
	{
		return 1;
	}

	@ConfigItem(
		keyName = "specDockJustification",
		name = "Dock slot orientation",
		description = "Where this bar sits along the panel's length when Bar type is Docked. Start (default) = flush with the panel's top/left corner, today's behavior. End = flush with the bottom/right corner. Center = centered. Give two bars the same Dock side + Dock slot with Start/End (and a Length under half the panel's own length, or Clamp slot off) to have them share a slot.",
		section = specSection,
		position = 9
	)
	default Justification specDockJustification()
	{
		return Justification.START;
	}

	@ConfigItem(
		keyName = "specClampSlot",
		name = "Clamp slot",
		description = "When on, this bar's Length is overridden to match the panel's own dimension (height for left/right Dock sides, width for top/bottom) instead of using its own Length setting. Leave off to size this bar manually - needed if it shares a Dock slot with another bar via Dock slot orientation.",
		section = specSection,
		position = 10
	)
	default boolean specClampSlot()
	{
		return false;
	}

	@ConfigItem(
		keyName = "specFloatingOrientation",
		name = "Floating orientation",
		description = "Bar orientation when Bar type is Floating.",
		section = specSection,
		position = 11
	)
	default Orientation specFloatingOrientation()
	{
		return Orientation.VERTICAL;
	}

	@ConfigItem(
		keyName = "specShowIcon",
		name = "Show icon",
		description = "Show the Special Attack orb icon on the bar.",
		section = specSection,
		position = 13
	)
	default boolean specShowIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "specIconSize",
		name = "Icon size",
		description = "Pixel size (width and height) of the icon on the bar.",
		section = specSection,
		position = 14
	)
	@Range(min = 1)
	default int specIconSize()
	{
		return 18;
	}

	@ConfigItem(
		keyName = "specShowCounter",
		name = "Show counter",
		description = "Show the current Special Attack value as text on the bar.",
		section = specSection,
		position = 15
	)
	default boolean specShowCounter()
	{
		return true;
	}

	@ConfigItem(
		keyName = "specJustification",
		name = "Content justification",
		description = "Where the icon/counter sit along the bar. Start = left (horizontal) or top (vertical). End = right (horizontal) or bottom (vertical).",
		section = specSection,
		position = 16
	)
	default Justification specJustification()
	{
		return Justification.CENTER;
	}

	@ConfigItem(
		keyName = "specContentOffset",
		name = "Content offset",
		description = "Nudges the icon/counter along the bar by this many pixels - positive moves toward the far end, negative moves back toward the near end. For small fixes without having to change Content justification.",
		section = specSection,
		position = 17
	)
	default int specContentOffset()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "specReverseContent",
		name = "Reverse content order",
		description = "Draw the counter before the icon (counter above icon on a vertical bar, counter left of icon on a horizontal bar) instead of the default icon-then-counter.",
		section = specSection,
		position = 18
	)
	default boolean specReverseContent()
	{
		return false;
	}

	@ConfigItem(
		keyName = "specThickness",
		name = "Thickness",
		description = "The bar's short dimension in pixels (width if vertical, height if horizontal).",
		section = specSection,
		position = 19
	)
	@Range(min = 1)
	default int specThickness()
	{
		return 24;
	}

	@ConfigItem(
		keyName = "specLength",
		name = "Length",
		description = "The bar's long dimension in pixels (height if vertical, width if horizontal).",
		section = specSection,
		position = 20
	)
	@Range(min = 1)
	default int specLength()
	{
		return 150;
	}

	@ConfigItem(
		keyName = "specBarColor",
		name = "Bar color",
		description = "Bar color when Special Attack isn't queued.",
		section = specSection,
		position = 21
	)
	default Color specBarColor()
	{
		return new Color(220, 170, 40);
	}

	@ConfigItem(
		keyName = "specQueuedColor",
		name = "Attack queued color",
		description = "Bar color when Special Attack is queued (armed for your next hit) - matches the orb's own color change when selected.",
		section = specSection,
		position = 22
	)
	default Color specQueuedColor()
	{
		return new Color(255, 255, 255);
	}

	@ConfigItem(
		keyName = "specOpacity",
		name = "Opacity",
		description = "0 = fully transparent, 100 = fully opaque. Scales both the background and the fill color.",
		section = specSection,
		position = 23
	)
	@Range(min = 0, max = 100)
	default int specOpacity()
	{
		return 100;
	}

	// ------------------------------------------------------------------
	// Run Energy
	// ------------------------------------------------------------------

	@ConfigSection(
		name = "Run Energy",
		description = "The Run Energy bar",
		position = 3,
		closedByDefault = true
	)
	String runSection = "runSection";

	@ConfigItem(
		keyName = "runEnabled",
		name = "Enabled",
		description = "Show the Run Energy bar",
		section = runSection,
		position = 0
	)
	default boolean runEnabled()
	{
		return true;
	}

	@ConfigItem(
		keyName = "runWarningEnabled",
		name = "Warning pulse",
		description = "Pulses the empty part of the vial once Run Energy drops to or below the Warning threshold.",
		section = runSection,
		position = 1
	)
	default boolean runWarningEnabled()
	{
		return false;
	}

	@ConfigItem(
		keyName = "runWarningThreshold",
		name = "Warning threshold",
		description = "Pulse activates once Run Energy drops to or below this percentage.",
		section = runSection,
		position = 2
	)
	@Range(min = 0, max = 100)
	default int runWarningThreshold()
	{
		return 25;
	}

	@ConfigItem(
		keyName = "runWarningColor",
		name = "Warning color",
		description = "Color the empty part of the vial pulses while the Warning threshold is active.",
		section = runSection,
		position = 3
	)
	default Color runWarningColor()
	{
		return new Color(220, 30, 30);
	}

	@ConfigItem(
		keyName = "runWarningOpacity",
		name = "Warning opacity",
		description = "0-100, the pulse's peak opacity - it breathes between 0 and this value, not a flat level.",
		section = runSection,
		position = 4
	)
	@Range(min = 0, max = 100)
	default int runWarningOpacity()
	{
		return 100;
	}

	@ConfigItem(
		keyName = "runPlacementMode",
		name = "Bar type",
		description = "Docked: docked to a side of the panel. Floating: drag anywhere, like Custom Vital Bars.",
		section = runSection,
		position = 5
	)
	default PlacementMode runPlacementMode()
	{
		return PlacementMode.FIXED;
	}

	@ConfigItem(
		keyName = "runDockSide",
		name = "Dock side",
		description = "Which side of the panel to dock to when Bar type is Docked. Determines orientation: left/right = vertical, top/bottom = horizontal.",
		section = runSection,
		position = 6
	)
	default DockSide runDockSide()
	{
		return DockSide.RIGHT;
	}

	@ConfigItem(
		keyName = "runDockSlot",
		name = "Dock slot",
		description = "Order among other bars docked to the same side. 1 = closest to the panel corner, higher numbers further out.",
		section = runSection,
		position = 7
	)
	@Range(min = 1)
	default int runDockSlot()
	{
		return 2;
	}

	@ConfigItem(
		keyName = "runDockJustification",
		name = "Dock slot orientation",
		description = "Where this bar sits along the panel's length when Bar type is Docked. Start (default) = flush with the panel's top/left corner, today's behavior. End = flush with the bottom/right corner. Center = centered. Give two bars the same Dock side + Dock slot with Start/End (and a Length under half the panel's own length, or Clamp slot off) to have them share a slot.",
		section = runSection,
		position = 8
	)
	default Justification runDockJustification()
	{
		return Justification.START;
	}

	@ConfigItem(
		keyName = "runClampSlot",
		name = "Clamp slot",
		description = "When on, this bar's Length is overridden to match the panel's own dimension (height for left/right Dock sides, width for top/bottom) instead of using its own Length setting. Leave off to size this bar manually - needed if it shares a Dock slot with another bar via Dock slot orientation.",
		section = runSection,
		position = 9
	)
	default boolean runClampSlot()
	{
		return false;
	}

	@ConfigItem(
		keyName = "runFloatingOrientation",
		name = "Floating orientation",
		description = "Bar orientation when Bar type is Floating.",
		section = runSection,
		position = 10
	)
	default Orientation runFloatingOrientation()
	{
		return Orientation.VERTICAL;
	}

	@ConfigItem(
		keyName = "runShowIcon",
		name = "Show icon",
		description = "Show the Run Energy orb icon on the bar.",
		section = runSection,
		position = 12
	)
	default boolean runShowIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "runIconSize",
		name = "Icon size",
		description = "Pixel size (width and height) of the icon on the bar.",
		section = runSection,
		position = 13
	)
	@Range(min = 1)
	default int runIconSize()
	{
		return 18;
	}

	@ConfigItem(
		keyName = "runShowCounter",
		name = "Show counter",
		description = "Show the current Run Energy value as text on the bar.",
		section = runSection,
		position = 14
	)
	default boolean runShowCounter()
	{
		return true;
	}

	@ConfigItem(
		keyName = "runJustification",
		name = "Content justification",
		description = "Where the icon/counter sit along the bar. Start = left (horizontal) or top (vertical). End = right (horizontal) or bottom (vertical).",
		section = runSection,
		position = 15
	)
	default Justification runJustification()
	{
		return Justification.CENTER;
	}

	@ConfigItem(
		keyName = "runContentOffset",
		name = "Content offset",
		description = "Nudges the icon/counter along the bar by this many pixels - positive moves toward the far end, negative moves back toward the near end. For small fixes without having to change Content justification.",
		section = runSection,
		position = 16
	)
	default int runContentOffset()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "runReverseContent",
		name = "Reverse content order",
		description = "Draw the counter before the icon (counter above icon on a vertical bar, counter left of icon on a horizontal bar) instead of the default icon-then-counter.",
		section = runSection,
		position = 17
	)
	default boolean runReverseContent()
	{
		return false;
	}

	@ConfigItem(
		keyName = "runThickness",
		name = "Thickness",
		description = "The bar's short dimension in pixels (width if vertical, height if horizontal).",
		section = runSection,
		position = 18
	)
	@Range(min = 1)
	default int runThickness()
	{
		return 24;
	}

	@ConfigItem(
		keyName = "runLength",
		name = "Length",
		description = "The bar's long dimension in pixels (height if vertical, width if horizontal).",
		section = runSection,
		position = 19
	)
	@Range(min = 1)
	default int runLength()
	{
		return 150;
	}

	@ConfigItem(
		keyName = "runActiveColor",
		name = "Active color",
		description = "Bar color while Run is toggled on - defaults to the gold/yellow of the vanilla run orb.",
		section = runSection,
		position = 20
	)
	default Color runActiveColor()
	{
		return new Color(212, 175, 55);
	}

	@ConfigItem(
		keyName = "runInactiveColor",
		name = "Inactive color",
		description = "Bar color while Run is toggled off.",
		section = runSection,
		position = 21
	)
	default Color runInactiveColor()
	{
		return new Color(130, 130, 130);
	}

	@ConfigItem(
		keyName = "runOpacity",
		name = "Opacity",
		description = "0 = fully transparent, 100 = fully opaque. Scales both the background and the fill color.",
		section = runSection,
		position = 22
	)
	@Range(min = 0, max = 100)
	default int runOpacity()
	{
		return 100;
	}

	// ------------------------------------------------------------------
	// Warmth (Wintertodt only - no vanilla orb, so no orb mode)
	// ------------------------------------------------------------------

	@ConfigSection(
		name = "Warmth",
		description = "The Warmth bar (Wintertodt only)",
		position = 4,
		closedByDefault = true
	)
	String warmthSection = "warmthSection";

	@ConfigItem(
		keyName = "warmthEnabled",
		name = "Enabled",
		description = "Show the Warmth bar",
		section = warmthSection,
		position = 0
	)
	default boolean warmthEnabled()
	{
		return false;
	}

	@ConfigItem(
		keyName = "warmthVisibility",
		name = "Visibility",
		description = "Hide: never show. Always Show: show regardless of location. Show Dynamically: only while inside the Wintertodt prison, matching the vanilla meter.",
		section = warmthSection,
		position = 1
	)
	default WarmthVisibility warmthVisibility()
	{
		return WarmthVisibility.SHOW_DYNAMICALLY;
	}

	@ConfigItem(
		keyName = "warmthWarningEnabled",
		name = "Warning pulse",
		description = "Pulses the empty part of the vial once Warmth drops to or below the Warning threshold.",
		section = warmthSection,
		position = 2
	)
	default boolean warmthWarningEnabled()
	{
		return false;
	}

	@ConfigItem(
		keyName = "warmthWarningThreshold",
		name = "Warning threshold",
		description = "Pulse activates once Warmth drops to or below this percentage.",
		section = warmthSection,
		position = 3
	)
	@Range(min = 0, max = 100)
	default int warmthWarningThreshold()
	{
		return 25;
	}

	@ConfigItem(
		keyName = "warmthWarningColor",
		name = "Warning color",
		description = "Color the empty part of the vial pulses while the Warning threshold is active.",
		section = warmthSection,
		position = 4
	)
	default Color warmthWarningColor()
	{
		return new Color(220, 30, 30);
	}

	@ConfigItem(
		keyName = "warmthWarningOpacity",
		name = "Warning opacity",
		description = "0-100, the pulse's peak opacity - it breathes between 0 and this value, not a flat level.",
		section = warmthSection,
		position = 5
	)
	@Range(min = 0, max = 100)
	default int warmthWarningOpacity()
	{
		return 100;
	}

	@ConfigItem(
		keyName = "warmthPlacementMode",
		name = "Bar type",
		description = "Docked: docked to a side of the panel. Floating: drag anywhere, like Custom Vital Bars.",
		section = warmthSection,
		position = 6
	)
	default PlacementMode warmthPlacementMode()
	{
		return PlacementMode.FIXED;
	}

	@ConfigItem(
		keyName = "warmthDockSide",
		name = "Dock side",
		description = "Which side of the panel to dock to when Bar type is Docked. Determines orientation: left/right = vertical, top/bottom = horizontal.",
		section = warmthSection,
		position = 7
	)
	default DockSide warmthDockSide()
	{
		return DockSide.LEFT;
	}

	@ConfigItem(
		keyName = "warmthDockSlot",
		name = "Dock slot",
		description = "Order among other bars docked to the same side. 1 = closest to the panel corner, higher numbers further out.",
		section = warmthSection,
		position = 8
	)
	@Range(min = 1)
	default int warmthDockSlot()
	{
		return 3;
	}

	@ConfigItem(
		keyName = "warmthDockJustification",
		name = "Dock slot orientation",
		description = "Where this bar sits along the panel's length when Bar type is Docked. Start (default) = flush with the panel's top/left corner, today's behavior. End = flush with the bottom/right corner. Center = centered. Give two bars the same Dock side + Dock slot with Start/End (and a Length under half the panel's own length, or Clamp slot off) to have them share a slot.",
		section = warmthSection,
		position = 9
	)
	default Justification warmthDockJustification()
	{
		return Justification.START;
	}

	@ConfigItem(
		keyName = "warmthClampSlot",
		name = "Clamp slot",
		description = "When on, this bar's Length is overridden to match the panel's own dimension (height for left/right Dock sides, width for top/bottom) instead of using its own Length setting. Leave off to size this bar manually - needed if it shares a Dock slot with another bar via Dock slot orientation.",
		section = warmthSection,
		position = 10
	)
	default boolean warmthClampSlot()
	{
		return false;
	}

	@ConfigItem(
		keyName = "warmthFloatingOrientation",
		name = "Floating orientation",
		description = "Bar orientation when Bar type is Floating.",
		section = warmthSection,
		position = 11
	)
	default Orientation warmthFloatingOrientation()
	{
		return Orientation.VERTICAL;
	}

	@ConfigItem(
		keyName = "warmthShowIcon",
		name = "Show icon",
		description = "Show an icon on the bar. Warmth has no vanilla orb icon, so this borrows the Firemaking skill icon instead.",
		section = warmthSection,
		position = 12
	)
	default boolean warmthShowIcon()
	{
		return false;
	}

	@ConfigItem(
		keyName = "warmthIconSize",
		name = "Icon size",
		description = "Pixel size (width and height) of the icon on the bar.",
		section = warmthSection,
		position = 13
	)
	@Range(min = 1)
	default int warmthIconSize()
	{
		return 18;
	}

	@ConfigItem(
		keyName = "warmthShowCounter",
		name = "Show counter",
		description = "Show the current Warmth percentage as text on the bar.",
		section = warmthSection,
		position = 14
	)
	default boolean warmthShowCounter()
	{
		return true;
	}

	@ConfigItem(
		keyName = "warmthJustification",
		name = "Content justification",
		description = "Where the icon/counter sit along the bar. Start = left (horizontal) or top (vertical). End = right (horizontal) or bottom (vertical).",
		section = warmthSection,
		position = 15
	)
	default Justification warmthJustification()
	{
		return Justification.CENTER;
	}

	@ConfigItem(
		keyName = "warmthContentOffset",
		name = "Content offset",
		description = "Nudges the icon/counter along the bar by this many pixels - positive moves toward the far end, negative moves back toward the near end. For small fixes without having to change Content justification.",
		section = warmthSection,
		position = 16
	)
	default int warmthContentOffset()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "warmthReverseContent",
		name = "Reverse content order",
		description = "Draw the counter before the icon (counter above icon on a vertical bar, counter left of icon on a horizontal bar) instead of the default icon-then-counter.",
		section = warmthSection,
		position = 17
	)
	default boolean warmthReverseContent()
	{
		return false;
	}

	@ConfigItem(
		keyName = "warmthThickness",
		name = "Thickness",
		description = "The bar's short dimension in pixels (width if vertical, height if horizontal).",
		section = warmthSection,
		position = 18
	)
	@Range(min = 1)
	default int warmthThickness()
	{
		return 24;
	}

	@ConfigItem(
		keyName = "warmthLength",
		name = "Length",
		description = "The bar's long dimension in pixels (height if vertical, width if horizontal).",
		section = warmthSection,
		position = 19
	)
	@Range(min = 1)
	default int warmthLength()
	{
		return 150;
	}

	@ConfigItem(
		keyName = "warmthOpacity",
		name = "Opacity",
		description = "0 = fully transparent, 100 = fully opaque. Scales both the background and the fill color.",
		section = warmthSection,
		position = 20
	)
	@Range(min = 0, max = 100)
	default int warmthOpacity()
	{
		return 100;
	}

	// ------------------------------------------------------------------
	// Enemy Hitpoints (current target only - no vanilla orb, so no bauble mode)
	// ------------------------------------------------------------------

	@ConfigSection(
		name = "Enemy Hitpoints",
		description = "Current target's Hitpoints bar",
		position = 5,
		closedByDefault = true
	)
	String enemyHpSection = "enemyHpSection";

	@ConfigItem(
		keyName = "enemyHpEnabled",
		name = "Enabled",
		description = "Show the current target's Hitpoints bar",
		section = enemyHpSection,
		position = 0
	)
	default boolean enemyHpEnabled()
	{
		return false;
	}

	@ConfigItem(
		keyName = "enemyHpWarningEnabled",
		name = "Warning pulse",
		description = "Pulses the empty part of the vial once the target's Hitpoints drops to or below the Warning threshold.",
		section = enemyHpSection,
		position = 1
	)
	default boolean enemyHpWarningEnabled()
	{
		return false;
	}

	@ConfigItem(
		keyName = "enemyHpWarningThreshold",
		name = "Warning threshold",
		description = "Pulse activates once the target's Hitpoints drops to or below this percentage.",
		section = enemyHpSection,
		position = 2
	)
	@Range(min = 0, max = 100)
	default int enemyHpWarningThreshold()
	{
		return 25;
	}

	@ConfigItem(
		keyName = "enemyHpWarningColor",
		name = "Warning color",
		description = "Color the empty part of the vial pulses while the Warning threshold is active.",
		section = enemyHpSection,
		position = 3
	)
	default Color enemyHpWarningColor()
	{
		return new Color(220, 30, 30);
	}

	@ConfigItem(
		keyName = "enemyHpWarningOpacity",
		name = "Warning opacity",
		description = "0-100, the pulse's peak opacity - it breathes between 0 and this value, not a flat level.",
		section = enemyHpSection,
		position = 4
	)
	@Range(min = 0, max = 100)
	default int enemyHpWarningOpacity()
	{
		return 100;
	}

	@ConfigItem(
		keyName = "enemyHpPlacementMode",
		name = "Bar type",
		description = "Docked: docked to a side of the panel. Floating: drag anywhere, like Custom Vital Bars.",
		section = enemyHpSection,
		position = 5
	)
	default PlacementMode enemyHpPlacementMode()
	{
		return PlacementMode.FLOATING;
	}

	@ConfigItem(
		keyName = "enemyHpDockSide",
		name = "Dock side",
		description = "Which side of the panel to dock to when Bar type is Docked. Determines orientation: left/right = vertical, top/bottom = horizontal.",
		section = enemyHpSection,
		position = 6
	)
	default DockSide enemyHpDockSide()
	{
		return DockSide.BOTTOM;
	}

	@ConfigItem(
		keyName = "enemyHpDockSlot",
		name = "Dock slot",
		description = "Order among other bars docked to the same side. 1 = closest to the panel corner, higher numbers further out.",
		section = enemyHpSection,
		position = 7
	)
	@Range(min = 1)
	default int enemyHpDockSlot()
	{
		return 1;
	}

	@ConfigItem(
		keyName = "enemyHpDockJustification",
		name = "Dock slot orientation",
		description = "Where this bar sits along the panel's length when Bar type is Docked. Start (default) = flush with the panel's top/left corner, today's behavior. End = flush with the bottom/right corner. Center = centered. Give two bars the same Dock side + Dock slot with Start/End (and a Length under half the panel's own length, or Clamp slot off) to have them share a slot.",
		section = enemyHpSection,
		position = 8
	)
	default Justification enemyHpDockJustification()
	{
		return Justification.START;
	}

	@ConfigItem(
		keyName = "enemyHpClampSlot",
		name = "Clamp slot",
		description = "When on, this bar's Length is overridden to match the panel's own dimension (height for left/right Dock sides, width for top/bottom) instead of using its own Length setting. Leave off to size this bar manually - needed if it shares a Dock slot with another bar via Dock slot orientation.",
		section = enemyHpSection,
		position = 9
	)
	default boolean enemyHpClampSlot()
	{
		return false;
	}

	@ConfigItem(
		keyName = "enemyHpFloatingOrientation",
		name = "Floating orientation",
		description = "Bar orientation when Bar type is Floating.",
		section = enemyHpSection,
		position = 10
	)
	default Orientation enemyHpFloatingOrientation()
	{
		return Orientation.HORIZONTAL;
	}

	@ConfigItem(
		keyName = "enemyHpShowIcon",
		name = "Show icon",
		description = "Show an icon on the bar. Enemy Hitpoints has no vanilla orb of its own, so this borrows the Hitpoints orb icon.",
		section = enemyHpSection,
		position = 11
	)
	default boolean enemyHpShowIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "enemyHpIconSize",
		name = "Icon size",
		description = "Pixel size (width and height) of the icon on the bar.",
		section = enemyHpSection,
		position = 12
	)
	@Range(min = 1)
	default int enemyHpIconSize()
	{
		return 18;
	}

	@ConfigItem(
		keyName = "enemyHpShowCounter",
		name = "Show counter",
		description = "Show the target's current Hitpoints value as text on the bar.",
		section = enemyHpSection,
		position = 13
	)
	default boolean enemyHpShowCounter()
	{
		return true;
	}

	@ConfigItem(
		keyName = "enemyHpCounterFormat",
		name = "Counter format",
		description = "Fraction: the bare ratio the game actually sends (e.g. 27/30 - never a real HP number for most monsters). Percentage: that same ratio converted to a rounded percent.",
		section = enemyHpSection,
		position = 14
	)
	default CounterFormat enemyHpCounterFormat()
	{
		return CounterFormat.FRACTION;
	}

	@ConfigItem(
		keyName = "enemyHpJustification",
		name = "Content justification",
		description = "Where the icon/counter sit along the bar. Start = left (horizontal) or top (vertical). End = right (horizontal) or bottom (vertical).",
		section = enemyHpSection,
		position = 15
	)
	default Justification enemyHpJustification()
	{
		return Justification.CENTER;
	}

	@ConfigItem(
		keyName = "enemyHpContentOffset",
		name = "Content offset",
		description = "Nudges the icon/counter along the bar by this many pixels - positive moves toward the far end, negative moves back toward the near end. For small fixes without having to change Content justification.",
		section = enemyHpSection,
		position = 16
	)
	default int enemyHpContentOffset()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "enemyHpReverseContent",
		name = "Reverse content order",
		description = "Draw the counter before the icon (counter above icon on a vertical bar, counter left of icon on a horizontal bar) instead of the default icon-then-counter.",
		section = enemyHpSection,
		position = 17
	)
	default boolean enemyHpReverseContent()
	{
		return false;
	}

	@ConfigItem(
		keyName = "enemyHpThickness",
		name = "Thickness",
		description = "The bar's short dimension in pixels (width if vertical, height if horizontal).",
		section = enemyHpSection,
		position = 18
	)
	@Range(min = 1)
	default int enemyHpThickness()
	{
		return 24;
	}

	@ConfigItem(
		keyName = "enemyHpLength",
		name = "Length",
		description = "The bar's long dimension in pixels (height if vertical, width if horizontal).",
		section = enemyHpSection,
		position = 19
	)
	@Range(min = 1)
	default int enemyHpLength()
	{
		return 150;
	}

	@ConfigItem(
		keyName = "enemyHpOpacity",
		name = "Opacity",
		description = "0 = fully transparent, 100 = fully opaque. Scales both the background and the fill color.",
		section = enemyHpSection,
		position = 20
	)
	@Range(min = 0, max = 100)
	default int enemyHpOpacity()
	{
		return 100;
	}

	// ------------------------------------------------------------------
	// Bar Frame (shared across every bar regardless of placement)
	// ------------------------------------------------------------------

	@ConfigSection(
		name = "Bar Frame",
		description = "The stone-toned frame drawn around every bar",
		position = 6,
		closedByDefault = true
	)
	String barFrameSection = "barFrameSection";

	@ConfigItem(
		keyName = "borderWidth",
		name = "Border width",
		description = "Thickness in pixels of the stone frame around every bar. The frame is drawn at the bar's existing Thickness/Length, not added on top of it, so increasing this shrinks the active fill area slightly rather than making the bar bigger.",
		section = barFrameSection,
		position = 0
	)
	@Range(min = 0)
	default int borderWidth()
	{
		return 2;
	}

	// ------------------------------------------------------------------
	// Fixed Layout (shared across every bar in Fixed placement - Floating
	// will get its own equivalent section if/when it needs shared settings)
	// ------------------------------------------------------------------

	@ConfigSection(
		name = "Fixed Layout",
		description = "Settings shared across every bar using Fixed placement",
		position = 7,
		closedByDefault = true
	)
	String fixedLayoutSection = "fixedLayoutSection";

	@ConfigItem(
		keyName = "dockPadding",
		name = "Dock padding",
		description = "Gap in pixels between bars stacked on the same dock side.",
		section = fixedLayoutSection,
		position = 0
	)
	@Range(min = 0)
	default int dockPadding()
	{
		return 2;
	}

	@ConfigItem(
		keyName = "dockOffset",
		name = "Dock offset",
		description = "Gap in pixels between the panel itself and the first (slot 1) bar docked to each side.",
		section = fixedLayoutSection,
		position = 1
	)
	@Range(min = 0)
	default int dockOffset()
	{
		return 4;
	}
}
