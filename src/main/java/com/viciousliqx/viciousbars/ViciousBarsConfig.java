package com.viciousliqx.viciousbars;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

/**
 * Deliberately minimal while the minimap-replacement foundation is being
 * built in isolation - see ViciousBarsPlugin's javadoc.
 */
@ConfigGroup(ViciousBarsConfig.GROUP)
public interface ViciousBarsConfig extends Config
{
	String GROUP = "vicious-bars";

	@ConfigItem(
		keyName = "hideMinimap",
		name = "Hide minimap",
		description = "Hides the round minimap drawing surface using the same type-swap trick Compact Orbs uses (not a resize or a real hide - just stops it drawing), leaving the real orbs in place and fully clickable.",
		position = 0
	)
	default boolean hideMinimap()
	{
		return false;
	}
}
