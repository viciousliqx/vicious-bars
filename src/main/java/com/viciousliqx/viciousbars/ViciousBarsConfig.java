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
		keyName = "hpPlacementMode",
		name = "Placement",
		description = "Fixed: docked to a side of the panel. Floating: drag anywhere, like Custom Vital Bars.",
		section = hpSection,
		position = 1
	)
	default PlacementMode hpPlacementMode()
	{
		return PlacementMode.FIXED;
	}

	@ConfigItem(
		keyName = "hpDockSide",
		name = "Dock side",
		description = "Which side of the panel to dock to when Placement is Fixed. Determines orientation: left/right = vertical, top/bottom = horizontal.",
		section = hpSection,
		position = 2
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
		position = 3
	)
	@Range(min = 1)
	default int hpDockSlot()
	{
		return 1;
	}

	@ConfigItem(
		keyName = "hpFloatingOrientation",
		name = "Floating orientation",
		description = "Bar orientation when Placement is Floating.",
		section = hpSection,
		position = 4
	)
	default Orientation hpFloatingOrientation()
	{
		return Orientation.VERTICAL;
	}

	@ConfigItem(
		keyName = "hpOrbMode",
		name = "Orb mode",
		description = "How this bar relates to the vanilla Hitpoints orb (Vanilla/Merged/Mirrored).",
		section = hpSection,
		position = 5
	)
	default OrbMode hpOrbMode()
	{
		return OrbMode.VANILLA;
	}

	@ConfigItem(
		keyName = "hpShowIcon",
		name = "Show icon",
		description = "Show the Hitpoints orb icon on the bar.",
		section = hpSection,
		position = 6
	)
	default boolean hpShowIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "hpShowCounter",
		name = "Show counter",
		description = "Show the current Hitpoints value as text on the bar.",
		section = hpSection,
		position = 7
	)
	default boolean hpShowCounter()
	{
		return true;
	}

	@ConfigItem(
		keyName = "hpJustification",
		name = "Justification",
		description = "Where the icon/counter sit along the bar. Start = left (horizontal) or top (vertical). End = right (horizontal) or bottom (vertical).",
		section = hpSection,
		position = 8
	)
	default Justification hpJustification()
	{
		return Justification.CENTER;
	}

	@ConfigItem(
		keyName = "hpThickness",
		name = "Thickness",
		description = "The bar's short dimension in pixels (width if vertical, height if horizontal).",
		section = hpSection,
		position = 9
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
		position = 10
	)
	@Range(min = 1)
	default int hpLength()
	{
		return 150;
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
		keyName = "prayerPlacementMode",
		name = "Placement",
		description = "Fixed: docked to a side of the panel. Floating: drag anywhere, like Custom Vital Bars.",
		section = prayerSection,
		position = 1
	)
	default PlacementMode prayerPlacementMode()
	{
		return PlacementMode.FIXED;
	}

	@ConfigItem(
		keyName = "prayerDockSide",
		name = "Dock side",
		description = "Which side of the panel to dock to when Placement is Fixed. Determines orientation: left/right = vertical, top/bottom = horizontal.",
		section = prayerSection,
		position = 2
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
		position = 3
	)
	@Range(min = 1)
	default int prayerDockSlot()
	{
		return 2;
	}

	@ConfigItem(
		keyName = "prayerFloatingOrientation",
		name = "Floating orientation",
		description = "Bar orientation when Placement is Floating.",
		section = prayerSection,
		position = 4
	)
	default Orientation prayerFloatingOrientation()
	{
		return Orientation.VERTICAL;
	}

	@ConfigItem(
		keyName = "prayerOrbMode",
		name = "Orb mode",
		description = "How this bar relates to the vanilla Prayer orb (Vanilla/Merged/Mirrored). Merged/Mirrored open quick-prayers on click, same as the orb.",
		section = prayerSection,
		position = 5
	)
	default OrbMode prayerOrbMode()
	{
		return OrbMode.VANILLA;
	}

	@ConfigItem(
		keyName = "prayerShowIcon",
		name = "Show icon",
		description = "Show the Prayer orb icon on the bar.",
		section = prayerSection,
		position = 6
	)
	default boolean prayerShowIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "prayerShowCounter",
		name = "Show counter",
		description = "Show the current Prayer value as text on the bar.",
		section = prayerSection,
		position = 7
	)
	default boolean prayerShowCounter()
	{
		return true;
	}

	@ConfigItem(
		keyName = "prayerJustification",
		name = "Justification",
		description = "Where the icon/counter sit along the bar. Start = left (horizontal) or top (vertical). End = right (horizontal) or bottom (vertical).",
		section = prayerSection,
		position = 8
	)
	default Justification prayerJustification()
	{
		return Justification.CENTER;
	}

	@ConfigItem(
		keyName = "prayerThickness",
		name = "Thickness",
		description = "The bar's short dimension in pixels (width if vertical, height if horizontal).",
		section = prayerSection,
		position = 9
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
		position = 10
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
		position = 11
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
		position = 12
	)
	default Color prayerInactiveColor()
	{
		return new Color(25, 35, 80);
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
		keyName = "specPlacementMode",
		name = "Placement",
		description = "Fixed: docked to a side of the panel. Floating: drag anywhere, like Custom Vital Bars.",
		section = specSection,
		position = 1
	)
	default PlacementMode specPlacementMode()
	{
		return PlacementMode.FIXED;
	}

	@ConfigItem(
		keyName = "specDockSide",
		name = "Dock side",
		description = "Which side of the panel to dock to when Placement is Fixed. Determines orientation: left/right = vertical, top/bottom = horizontal.",
		section = specSection,
		position = 2
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
		position = 3
	)
	@Range(min = 1)
	default int specDockSlot()
	{
		return 1;
	}

	@ConfigItem(
		keyName = "specFloatingOrientation",
		name = "Floating orientation",
		description = "Bar orientation when Placement is Floating.",
		section = specSection,
		position = 4
	)
	default Orientation specFloatingOrientation()
	{
		return Orientation.VERTICAL;
	}

	@ConfigItem(
		keyName = "specOrbMode",
		name = "Orb mode",
		description = "How this bar relates to the vanilla Special Attack orb (Vanilla/Merged/Mirrored). Merged/Mirrored activate special attack on click, same as the orb.",
		section = specSection,
		position = 5
	)
	default OrbMode specOrbMode()
	{
		return OrbMode.VANILLA;
	}

	@ConfigItem(
		keyName = "specShowIcon",
		name = "Show icon",
		description = "Show the Special Attack orb icon on the bar.",
		section = specSection,
		position = 6
	)
	default boolean specShowIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "specShowCounter",
		name = "Show counter",
		description = "Show the current Special Attack value as text on the bar.",
		section = specSection,
		position = 7
	)
	default boolean specShowCounter()
	{
		return true;
	}

	@ConfigItem(
		keyName = "specJustification",
		name = "Justification",
		description = "Where the icon/counter sit along the bar. Start = left (horizontal) or top (vertical). End = right (horizontal) or bottom (vertical).",
		section = specSection,
		position = 8
	)
	default Justification specJustification()
	{
		return Justification.CENTER;
	}

	@ConfigItem(
		keyName = "specThickness",
		name = "Thickness",
		description = "The bar's short dimension in pixels (width if vertical, height if horizontal).",
		section = specSection,
		position = 9
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
		position = 10
	)
	@Range(min = 1)
	default int specLength()
	{
		return 150;
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
		keyName = "runPlacementMode",
		name = "Placement",
		description = "Fixed: docked to a side of the panel. Floating: drag anywhere, like Custom Vital Bars.",
		section = runSection,
		position = 1
	)
	default PlacementMode runPlacementMode()
	{
		return PlacementMode.FIXED;
	}

	@ConfigItem(
		keyName = "runDockSide",
		name = "Dock side",
		description = "Which side of the panel to dock to when Placement is Fixed. Determines orientation: left/right = vertical, top/bottom = horizontal.",
		section = runSection,
		position = 2
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
		position = 3
	)
	@Range(min = 1)
	default int runDockSlot()
	{
		return 2;
	}

	@ConfigItem(
		keyName = "runFloatingOrientation",
		name = "Floating orientation",
		description = "Bar orientation when Placement is Floating.",
		section = runSection,
		position = 4
	)
	default Orientation runFloatingOrientation()
	{
		return Orientation.VERTICAL;
	}

	@ConfigItem(
		keyName = "runOrbMode",
		name = "Orb mode",
		description = "How this bar relates to the vanilla Run Energy orb (Vanilla/Merged/Mirrored). Merged/Mirrored toggle run on click, same as the orb.",
		section = runSection,
		position = 5
	)
	default OrbMode runOrbMode()
	{
		return OrbMode.VANILLA;
	}

	@ConfigItem(
		keyName = "runShowIcon",
		name = "Show icon",
		description = "Show the Run Energy orb icon on the bar.",
		section = runSection,
		position = 6
	)
	default boolean runShowIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "runShowCounter",
		name = "Show counter",
		description = "Show the current Run Energy value as text on the bar.",
		section = runSection,
		position = 7
	)
	default boolean runShowCounter()
	{
		return true;
	}

	@ConfigItem(
		keyName = "runJustification",
		name = "Justification",
		description = "Where the icon/counter sit along the bar. Start = left (horizontal) or top (vertical). End = right (horizontal) or bottom (vertical).",
		section = runSection,
		position = 8
	)
	default Justification runJustification()
	{
		return Justification.CENTER;
	}

	@ConfigItem(
		keyName = "runThickness",
		name = "Thickness",
		description = "The bar's short dimension in pixels (width if vertical, height if horizontal).",
		section = runSection,
		position = 9
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
		position = 10
	)
	@Range(min = 1)
	default int runLength()
	{
		return 150;
	}

	@ConfigItem(
		keyName = "runColor",
		name = "Color",
		description = "Bar color - defaults to the gold/yellow of the vanilla run orb.",
		section = runSection,
		position = 11
	)
	default Color runColor()
	{
		return new Color(212, 175, 55);
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
		keyName = "warmthPlacementMode",
		name = "Placement",
		description = "Fixed: docked to a side of the panel. Floating: drag anywhere, like Custom Vital Bars.",
		section = warmthSection,
		position = 2
	)
	default PlacementMode warmthPlacementMode()
	{
		return PlacementMode.FIXED;
	}

	@ConfigItem(
		keyName = "warmthDockSide",
		name = "Dock side",
		description = "Which side of the panel to dock to when Placement is Fixed. Determines orientation: left/right = vertical, top/bottom = horizontal.",
		section = warmthSection,
		position = 3
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
		position = 4
	)
	@Range(min = 1)
	default int warmthDockSlot()
	{
		return 3;
	}

	@ConfigItem(
		keyName = "warmthFloatingOrientation",
		name = "Floating orientation",
		description = "Bar orientation when Placement is Floating.",
		section = warmthSection,
		position = 5
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
		position = 6
	)
	default boolean warmthShowIcon()
	{
		return false;
	}

	@ConfigItem(
		keyName = "warmthShowCounter",
		name = "Show counter",
		description = "Show the current Warmth percentage as text on the bar.",
		section = warmthSection,
		position = 7
	)
	default boolean warmthShowCounter()
	{
		return true;
	}

	@ConfigItem(
		keyName = "warmthJustification",
		name = "Justification",
		description = "Where the icon/counter sit along the bar. Start = left (horizontal) or top (vertical). End = right (horizontal) or bottom (vertical).",
		section = warmthSection,
		position = 8
	)
	default Justification warmthJustification()
	{
		return Justification.CENTER;
	}

	@ConfigItem(
		keyName = "warmthThickness",
		name = "Thickness",
		description = "The bar's short dimension in pixels (width if vertical, height if horizontal).",
		section = warmthSection,
		position = 9
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
		position = 10
	)
	@Range(min = 1)
	default int warmthLength()
	{
		return 150;
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
		keyName = "enemyHpPlacementMode",
		name = "Placement",
		description = "Fixed: docked to a side of the panel. Floating: drag anywhere, like Custom Vital Bars.",
		section = enemyHpSection,
		position = 1
	)
	default PlacementMode enemyHpPlacementMode()
	{
		return PlacementMode.FLOATING;
	}

	@ConfigItem(
		keyName = "enemyHpDockSide",
		name = "Dock side",
		description = "Which side of the panel to dock to when Placement is Fixed. Determines orientation: left/right = vertical, top/bottom = horizontal.",
		section = enemyHpSection,
		position = 2
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
		position = 3
	)
	@Range(min = 1)
	default int enemyHpDockSlot()
	{
		return 1;
	}

	@ConfigItem(
		keyName = "enemyHpFloatingOrientation",
		name = "Floating orientation",
		description = "Bar orientation when Placement is Floating.",
		section = enemyHpSection,
		position = 4
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
		position = 5
	)
	default boolean enemyHpShowIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "enemyHpShowCounter",
		name = "Show counter",
		description = "Show the target's current Hitpoints value as text on the bar.",
		section = enemyHpSection,
		position = 6
	)
	default boolean enemyHpShowCounter()
	{
		return true;
	}

	@ConfigItem(
		keyName = "enemyHpJustification",
		name = "Justification",
		description = "Where the icon/counter sit along the bar. Start = left (horizontal) or top (vertical). End = right (horizontal) or bottom (vertical).",
		section = enemyHpSection,
		position = 7
	)
	default Justification enemyHpJustification()
	{
		return Justification.CENTER;
	}

	@ConfigItem(
		keyName = "enemyHpThickness",
		name = "Thickness",
		description = "The bar's short dimension in pixels (width if vertical, height if horizontal).",
		section = enemyHpSection,
		position = 8
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
		position = 9
	)
	@Range(min = 1)
	default int enemyHpLength()
	{
		return 150;
	}

	// ------------------------------------------------------------------
	// Fixed Layout (shared across every bar in Fixed placement - Floating
	// will get its own equivalent section if/when it needs shared settings)
	// ------------------------------------------------------------------

	@ConfigSection(
		name = "Fixed Layout",
		description = "Settings shared across every bar using Fixed placement",
		position = 6,
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
}
