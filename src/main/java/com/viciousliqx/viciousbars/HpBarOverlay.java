package com.viciousliqx.viciousbars;

import java.awt.Color;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.api.VarPlayer;
import net.runelite.api.gameval.SpriteID;
import net.runelite.client.game.SpriteManager;

class HpBarOverlay extends VitalBarOverlay
{
	private static final Color NORMAL = new Color(220, 40, 40);
	private static final Color POISONED = new Color(90, 170, 60);
	private static final Color ENVENOMED = new Color(60, 120, 40);
	private static final Color DISEASED = new Color(150, 110, 40);

	private final Client client;
	private final ViciousBarsConfig config;
	private final SpriteManager spriteManager;

	@Inject
	private HpBarOverlay(Client client, ViciousBarsConfig config, SpriteManager spriteManager)
	{
		this.client = client;
		this.config = config;
		this.spriteManager = spriteManager;
	}

	@Override
	protected boolean shouldRender()
	{
		return config.hpEnabled() && config.hpPlacementMode() == PlacementMode.FLOATING;
	}

	@Override
	protected int getCurrent()
	{
		return client.getBoostedSkillLevel(Skill.HITPOINTS);
	}

	@Override
	protected int getMax()
	{
		return client.getRealSkillLevel(Skill.HITPOINTS);
	}

	@Override
	protected Color getColor()
	{
		int poison = client.getVarpValue(VarPlayer.POISON);
		if (poison >= 1_000_000)
		{
			return ENVENOMED;
		}
		if (poison > 0)
		{
			return POISONED;
		}
		if (client.getVarpValue(VarPlayer.DISEASE_VALUE) > 0)
		{
			return DISEASED;
		}
		return NORMAL;
	}

	@Override
	protected boolean isShowIcon()
	{
		return config.hpShowIcon();
	}

	@Override
	protected BufferedImage getIcon()
	{
		return spriteManager.getSprite(SpriteID.OrbIcon.HITPOINTS, 0);
	}

	@Override
	protected boolean isShowCounter()
	{
		return config.hpShowCounter();
	}

	@Override
	protected Justification getJustification()
	{
		return config.hpJustification();
	}

	@Override
	protected Orientation getOrientation()
	{
		return config.hpFloatingOrientation();
	}
}
