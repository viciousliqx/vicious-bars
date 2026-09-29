package com.viciousliqx.viciousbars;

import java.awt.Color;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import net.runelite.api.NPC;
import net.runelite.api.gameval.SpriteID;
import net.runelite.client.game.SpriteManager;

class EnemyHpBarOverlay extends VitalBarOverlay
{
	private static final Color COLOR = new Color(220, 40, 40);

	private final ViciousBarsConfig config;
	private final OpponentTracker opponentTracker;
	private final SpriteManager spriteManager;

	@Inject
	private EnemyHpBarOverlay(ViciousBarsConfig config, OpponentTracker opponentTracker, SpriteManager spriteManager)
	{
		this.config = config;
		this.opponentTracker = opponentTracker;
		this.spriteManager = spriteManager;
	}

	private NPC opponent()
	{
		return opponentTracker.getOpponent();
	}

	@Override
	protected boolean shouldRender()
	{
		if (!config.enemyHpEnabled() || config.enemyHpPlacementMode() != PlacementMode.FLOATING)
		{
			return false;
		}

		NPC target = opponent();
		// -1 means the server never sent health data for this NPC - nothing
		// meaningful to draw, same case core's NPC health overlay skips.
		return target != null && target.getHealthRatio() >= 0 && target.getHealthScale() > 0;
	}

	@Override
	protected int getCurrent()
	{
		NPC target = opponent();
		return target != null ? target.getHealthRatio() : 0;
	}

	@Override
	protected int getMax()
	{
		NPC target = opponent();
		return target != null ? target.getHealthScale() : 1;
	}

	@Override
	protected Color getColor()
	{
		return COLOR;
	}

	@Override
	protected boolean isShowIcon()
	{
		return config.enemyHpShowIcon();
	}

	@Override
	protected BufferedImage getIcon()
	{
		// No "enemy hp" orb exists - deliberately reusing the Hitpoints orb icon
		// for visual consistency, our own cosmetic choice rather than a vanilla one.
		return spriteManager.getSprite(SpriteID.OrbIcon.HITPOINTS, 0);
	}

	@Override
	protected boolean isShowCounter()
	{
		return config.enemyHpShowCounter();
	}

	@Override
	protected Justification getJustification()
	{
		return config.enemyHpJustification();
	}

	@Override
	protected Orientation getOrientation()
	{
		return config.enemyHpFloatingOrientation();
	}

	@Override
	protected int getThickness()
	{
		return config.enemyHpThickness();
	}

	@Override
	protected int getLength()
	{
		return config.enemyHpLength();
	}
}
