package com.viciousliqx.viciousbars;

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
		keyName = "hpBaubleMode",
		name = "Bauble mode",
		description = "How this bar relates to the vanilla Hitpoints orb (Vanilla/Relocated/Merged/Mirrored).",
		section = hpSection,
		position = 5
	)
	default BaubleMode hpBaubleMode()
	{
		return BaubleMode.VANILLA;
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
		keyName = "prayerBaubleMode",
		name = "Bauble mode",
		description = "How this bar relates to the vanilla Prayer orb (Vanilla/Relocated/Merged/Mirrored). Merged/Mirrored open quick-prayers on click, same as the orb.",
		section = prayerSection,
		position = 5
	)
	default BaubleMode prayerBaubleMode()
	{
		return BaubleMode.VANILLA;
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
		keyName = "specBaubleMode",
		name = "Bauble mode",
		description = "How this bar relates to the vanilla Special Attack orb (Vanilla/Relocated/Merged/Mirrored). Merged/Mirrored activate special attack on click, same as the orb.",
		section = specSection,
		position = 5
	)
	default BaubleMode specBaubleMode()
	{
		return BaubleMode.VANILLA;
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
		keyName = "runBaubleMode",
		name = "Bauble mode",
		description = "How this bar relates to the vanilla Run Energy orb (Vanilla/Relocated/Merged/Mirrored). Merged/Mirrored toggle run on click, same as the orb.",
		section = runSection,
		position = 5
	)
	default BaubleMode runBaubleMode()
	{
		return BaubleMode.VANILLA;
	}

	// ------------------------------------------------------------------
	// Warmth (Wintertodt only - no vanilla orb, so no bauble mode)
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
}
