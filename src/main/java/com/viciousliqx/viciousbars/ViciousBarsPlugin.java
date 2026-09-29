package com.viciousliqx.viciousbars;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

/**
 * Scaffold only - no bars are rendered yet. This exists so the config panel
 * (see {@link ViciousBarsConfig}) can be inspected in a running client while
 * the overlay/rendering work is designed and built out.
 */
@Slf4j
@PluginDescriptor(
	name = "Vicious Bars"
)
public class ViciousBarsPlugin extends Plugin
{
	@Inject
	private ViciousBarsConfig config;

	@Override
	protected void startUp() throws Exception
	{
		log.debug("Vicious Bars started!");
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.debug("Vicious Bars stopped!");
	}

	@Provides
	ViciousBarsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ViciousBarsConfig.class);
	}
}
