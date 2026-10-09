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
import net.runelite.api.Skill;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.game.SkillIconManager;

/**
 * Warmth - restored from v1 (see git log before the full-reset "gut pass").
 * No vanilla orb of its own (hence aux-only, no main-bar counterpart to
 * couple against) and only meaningful inside the Wintertodt prison, so its
 * own visibility rule (Hide/Always/Dynamic-by-region) layers on top of the
 * plain enabled toggle every other aux bar has. Borrows the Firemaking
 * skill icon, same cosmetic choice v1 made - thematically the closest real
 * icon, not a vanilla Warmth icon (none exists).
 */
@Singleton
class WarmthAuxBarOverlay extends AuxBarOverlay
{
	// Confirmed via oldschool.runescape.wiki/w/Warmth - the Wintertodt prison region.
	private static final int WINTERTODT_REGION = 6462;
	private static final int MAX_PERCENT = 100;

	private final SkillIconManager skillIconManager;

	@Inject
	private WarmthAuxBarOverlay(Client client, CompactOrbsManager manager, CompactOrbsConfig config, SkillIconManager skillIconManager)
	{
		super(client, manager, config);
		this.skillIconManager = skillIconManager;
	}

	@Override
	protected boolean shouldRender()
	{
		if (!config.auxWarmthEnabled())
		{
			return false;
		}

		switch (config.auxWarmthVisibility())
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
	protected PlacementMode getPlacementMode()
	{
		return config.auxWarmthPlacementMode();
	}

	@Override
	protected DockSide getDockSide()
	{
		return config.auxWarmthDockSide();
	}

	@Override
	protected int getDockSlot()
	{
		return config.auxWarmthDockSlot();
	}

	@Override
	protected Justification getDockJustification()
	{
		return config.auxWarmthDockJustification();
	}

	@Override
	protected boolean isClampToPanel()
	{
		return config.auxWarmthClampToPanel();
	}

	@Override
	protected Orientation getFloatingOrientation()
	{
		return config.auxWarmthFloatingOrientation();
	}

	@Override
	protected int getThickness()
	{
		return config.auxWarmthThickness();
	}

	@Override
	protected int getLength()
	{
		return config.auxWarmthLength();
	}

	@Override
	protected int getCurrent()
	{
		// VarbitID.WINT_WARMTH (legacy name Varbits.WINTERTODT_WARMTH) is
		// 0-1000; convert to a 0-100 percentage.
		return client.getVarbitValue(VarbitID.WINT_WARMTH) / 10;
	}

	@Override
	protected int getMax()
	{
		return MAX_PERCENT;
	}

	@Override
	protected Color fillColor()
	{
		return config.auxWarmthColor();
	}

	@Override
	protected boolean isShowIcon()
	{
		return config.auxWarmthShowIcon();
	}

	@Override
	protected BufferedImage getIcon()
	{
		return skillIconManager.getSkillImage(Skill.FIREMAKING, true);
	}

	@Override
	protected int getIconSize()
	{
		return config.auxWarmthIconSize();
	}

	@Override
	protected boolean isShowCounter()
	{
		return config.auxWarmthShowCounter();
	}

	@Override
	protected Justification getContentJustification()
	{
		return config.auxWarmthContentJustification();
	}

	@Override
	protected int getContentOffset()
	{
		return config.auxWarmthContentOffset();
	}

	@Override
	protected boolean isReverseContent()
	{
		return config.auxWarmthReverseContent();
	}

	@Override
	protected int getOpacity()
	{
		return config.auxWarmthOpacity();
	}

	@Override
	protected ThresholdMode thresholdMode()
	{
		return config.auxWarmthThresholdMode();
	}

	@Override
	protected int thresholdValue()
	{
		return config.auxWarmthThreshold();
	}

	@Override
	protected Color thresholdColor()
	{
		return config.auxWarmthThresholdColor();
	}

	@Override
	protected boolean thresholdAbove()
	{
		return config.auxWarmthThresholdDirection() == ThresholdDirection.AT_OR_ABOVE;
	}
}
