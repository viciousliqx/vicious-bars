package com.viciousliqx.viciousbars;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class ViciousBarsPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(ViciousBarsPlugin.class);
		RuneLite.main(args);
	}
}
