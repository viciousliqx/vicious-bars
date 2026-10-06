package com.viciousliqx.viciousbars;

import com.compactorbs.CompactOrbsPlugin;
import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class ViciousBarsPluginTest
{
	public static void main(String[] args) throws Exception
	{
		// TEMPORARY - Compact Orbs loaded alongside ours as a verbatim-copied
		// baseline to gut down to what we need, per the "copy wholesale,
		// then gut" strategy - see com/compactorbs's own source comments and
		// compact_orbs_walkthrough.md at the hangar root. Remove this line
		// once gutting starts producing something that replaces it.
		ExternalPluginManager.loadBuiltin(CompactOrbsPlugin.class);
		ExternalPluginManager.loadBuiltin(ViciousBarsPlugin.class);
		RuneLite.main(args);
	}
}
