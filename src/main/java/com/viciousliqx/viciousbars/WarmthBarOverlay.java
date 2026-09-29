package com.viciousliqx.viciousbars;

import java.awt.Color;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.api.Varbits;
import net.runelite.client.game.SkillIconManager;

class WarmthBarOverlay extends VitalBarOverlay
{
	// Confirmed via oldschool.runescape.wiki/w/Warmth - the Wintertodt prison region.
	private static final int WINTERTODT_REGION = 6462;

	private static final Color COLOR = new Color(230, 110, 30);
	private static final int MAX_PERCENT = 100;

	private final Client client;
	private final ViciousBarsConfig config;
	private final SkillIconManager skillIconManager;

	@Inject
	private WarmthBarOverlay(Client client, ViciousBarsConfig config, SkillIconManager skillIconManager)
	{
		this.client = client;
		this.config = config;
		this.skillIconManager = skillIconManager;
	}

	@Override
	protected boolean shouldRender()
	{
		if (!config.warmthEnabled() || config.warmthPlacementMode() != PlacementMode.FLOATING)
		{
			return false;
		}

		switch (config.warmthVisibility())
		{
			case HIDE:
				return false;
			case SHOW_ALWAYS:
				return true;
			case SHOW_DYNAMICALLY:
			default:
				return client.getLocalPlayer() != null
					&& client.getLocalPlayer().getWorldLocation().getRegionID() == WINTERTODT_REGION;
		}
	}

	@Override
	protected int getCurrent()
	{
		// Varbits.WINTERTODT_WARMTH is 0-1000; convert to a 0-100 percentage.
		return client.getVarbitValue(Varbits.WINTERTODT_WARMTH) / 10;
	}

	@Override
	protected int getMax()
	{
		return MAX_PERCENT;
	}

	@Override
	protected Color getColor()
	{
		return COLOR;
	}

	@Override
	protected boolean isShowIcon()
	{
		return config.warmthShowIcon();
	}

	@Override
	protected BufferedImage getIcon()
	{
		// Warmth has no vanilla orb of its own - deliberately borrowing the
		// Firemaking skill icon since it's thematically the closest real icon.
		return skillIconManager.getSkillImage(Skill.FIREMAKING, true);
	}

	@Override
	protected boolean isShowCounter()
	{
		return config.warmthShowCounter();
	}

	@Override
	protected Justification getJustification()
	{
		return config.warmthJustification();
	}

	@Override
	protected Orientation getOrientation()
	{
		return config.warmthFloatingOrientation();
	}
}
