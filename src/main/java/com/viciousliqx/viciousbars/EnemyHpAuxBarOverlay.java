package com.viciousliqx.viciousbars;

import com.compactorbs.CompactOrbsConfig;
import com.compactorbs.CompactOrbsConfig.ThresholdDirection;
import com.compactorbs.CompactOrbsConfig.ThresholdMode;
import com.compactorbs.CompactOrbsManager;
import java.awt.Color;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.gameval.SpriteID;
import net.runelite.client.game.SpriteManager;

/**
 * Enemy Hitpoints - restored from v1 (see git log before the full-reset
 * "gut pass"). Shows the current combat target's own health bar via the
 * same NPC.getHealthRatio()/getHealthScale() API RuneLite's own built-in NPC
 * health overlay uses - a long-established, Hub-accepted display category,
 * not a boss-mechanic indicator. No vanilla orb of its own (hence aux-only).
 * Borrows the HP orb sprite for its icon, same cosmetic choice v1 made.
 */
@Singleton
class EnemyHpAuxBarOverlay extends AuxBarOverlay
{
	private final SpriteManager spriteManager;

	@Inject
	private EnemyHpAuxBarOverlay(Client client, CompactOrbsManager manager, CompactOrbsConfig config, SpriteManager spriteManager)
	{
		super(client, manager, config);
		this.spriteManager = spriteManager;
	}

	private NPC target()
	{
		return manager.combatTargetNpc;
	}

	@Override
	protected boolean shouldRender()
	{
		if (!config.auxEnemyHpEnabled())
		{
			return false;
		}

		NPC npc = target();
		// -1 means the server never sent health data for this NPC - nothing
		// meaningful to draw, same case RuneLite's own NPC health overlay skips.
		return npc != null && npc.getHealthRatio() >= 0 && npc.getHealthScale() > 0;
	}

	@Override
	protected PlacementMode getPlacementMode()
	{
		return config.auxEnemyHpPlacementMode();
	}

	@Override
	protected DockSide getDockSide()
	{
		return config.auxEnemyHpDockSide();
	}

	@Override
	protected int getDockSlot()
	{
		return config.auxEnemyHpDockSlot();
	}

	@Override
	protected Justification getDockJustification()
	{
		return config.auxEnemyHpDockJustification();
	}

	@Override
	protected boolean isClampToPanel()
	{
		return config.auxEnemyHpClampToPanel();
	}

	@Override
	protected Orientation getFloatingOrientation()
	{
		return config.auxEnemyHpFloatingOrientation();
	}

	@Override
	protected int getThickness()
	{
		return config.auxEnemyHpThickness();
	}

	@Override
	protected int getLength()
	{
		return config.auxEnemyHpLength();
	}

	@Override
	protected int getCurrent()
	{
		NPC npc = target();
		return npc != null ? npc.getHealthRatio() : 0;
	}

	@Override
	protected int getMax()
	{
		NPC npc = target();
		return npc != null ? npc.getHealthScale() : 1;
	}

	@Override
	protected Color fillColor()
	{
		return config.auxEnemyHpColor();
	}

	@Override
	protected boolean isShowIcon()
	{
		return config.auxEnemyHpShowIcon();
	}

	@Override
	protected BufferedImage getIcon()
	{
		return spriteManager.getSprite(SpriteID.OrbIcon.HITPOINTS, 0);
	}

	@Override
	protected int getIconSize()
	{
		return config.auxEnemyHpIconSize();
	}

	@Override
	protected boolean isShowCounter()
	{
		return config.auxEnemyHpShowCounter();
	}

	@Override
	protected String formatCounter(int current, int max)
	{
		if (config.auxEnemyHpCounterFormat() == CounterFormat.PERCENTAGE)
		{
			return Math.round(100f * current / max) + "%";
		}
		return current + "/" + max;
	}

	@Override
	protected Justification getContentJustification()
	{
		return config.auxEnemyHpContentJustification();
	}

	@Override
	protected int getContentOffset()
	{
		return config.auxEnemyHpContentOffset();
	}

	@Override
	protected boolean isReverseContent()
	{
		return config.auxEnemyHpReverseContent();
	}

	@Override
	protected int getOpacity()
	{
		return config.auxEnemyHpOpacity();
	}

	@Override
	protected ThresholdMode thresholdMode()
	{
		return config.auxEnemyHpThresholdMode();
	}

	@Override
	protected int thresholdValue()
	{
		return config.auxEnemyHpThreshold();
	}

	@Override
	protected Color thresholdColor()
	{
		return config.auxEnemyHpThresholdColor();
	}

	@Override
	protected boolean thresholdAbove()
	{
		return config.auxEnemyHpThresholdDirection() == ThresholdDirection.AT_OR_ABOVE;
	}
}
