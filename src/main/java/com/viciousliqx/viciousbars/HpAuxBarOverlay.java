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
import net.runelite.api.gameval.SpriteID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.client.game.SpriteManager;

/**
 * HP's auxiliary bar - a free-floating/Docked overlay with no real widget
 * involvement, shown whenever the main (orb-anchored) bars are hidden - see
 * AuxBarOverlay for the shared drawing/placement logic. State detection
 * duplicates HpVialOverlay's own (cheap client reads, not worth sharing),
 * but regen/drain PERCENTAGE tracking reads the exact same
 * CompactOrbsManager fields the main bar does - no duplicated tick-tracking.
 */
@Singleton
class HpAuxBarOverlay extends AuxBarOverlay
{
	private static final int VENOM_THRESHOLD = 1_000_000;

	private final SpriteManager spriteManager;

	@Inject
	private HpAuxBarOverlay(Client client, CompactOrbsManager manager, CompactOrbsConfig config, SpriteManager spriteManager)
	{
		super(client, manager, config);
		this.spriteManager = spriteManager;
	}

	@Override
	protected boolean shouldRender()
	{
		if (!config.auxHpEnabled())
		{
			return false;
		}
		return !(manager.isCompactLayout() && config.hpHideAuxWhenMainShown());
	}

	@Override
	protected PlacementMode getPlacementMode()
	{
		return config.auxHpPlacementMode();
	}

	@Override
	protected DockSide getDockSide()
	{
		return config.auxHpDockSide();
	}

	@Override
	protected int getDockSlot()
	{
		return config.auxHpDockSlot();
	}

	@Override
	protected Justification getDockJustification()
	{
		return config.auxHpDockJustification();
	}

	@Override
	protected boolean isClampToPanel()
	{
		return config.auxHpClampToPanel();
	}

	@Override
	protected Orientation getFloatingOrientation()
	{
		return config.auxHpFloatingOrientation();
	}

	@Override
	protected int getThickness()
	{
		return config.auxHpThickness();
	}

	@Override
	protected int getLength()
	{
		return config.auxHpLength();
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
	protected Color fillColor()
	{
		int poison = client.getVarpValue(VarPlayerID.POISON);
		if (poison >= VENOM_THRESHOLD)
		{
			return config.auxHpEnvenomedColor();
		}
		if (poison > 0)
		{
			return config.auxHpPoisonedColor();
		}
		if (client.getVarpValue(VarPlayerID.DISEASE) > 0)
		{
			return config.auxHpDiseasedColor();
		}
		return config.auxHpNormalColor();
	}

	@Override
	protected boolean isShowIcon()
	{
		return config.auxHpShowIcon();
	}

	@Override
	protected BufferedImage getIcon()
	{
		return spriteManager.getSprite(SpriteID.OrbIcon.HITPOINTS, 0);
	}

	@Override
	protected int getIconSize()
	{
		return config.auxHpIconSize();
	}

	@Override
	protected boolean isShowCounter()
	{
		return config.auxHpShowCounter();
	}

	@Override
	protected Justification getContentJustification()
	{
		return config.auxHpContentJustification();
	}

	@Override
	protected int getContentOffset()
	{
		return config.auxHpContentOffset();
	}

	@Override
	protected boolean isReverseContent()
	{
		return config.auxHpReverseContent();
	}

	@Override
	protected int getOpacity()
	{
		return config.auxHpOpacity();
	}

	@Override
	protected Double regenPercentage()
	{
		return manager.hpRegenPercentage;
	}

	@Override
	protected boolean regenEnabled()
	{
		return config.auxHpShowRegen();
	}

	@Override
	protected Color regenHighlightColor()
	{
		return config.auxHpRegenColor();
	}

	@Override
	protected Double drainPercentage()
	{
		return manager.hpDrainPercentage;
	}

	@Override
	protected boolean drainEnabled()
	{
		return config.auxHpShowDrain();
	}

	@Override
	protected Color drainHighlightColor()
	{
		// Same hijack HpVialOverlay uses: locked to whatever color the fill
		// itself is using for the current poison state.
		return fillColor();
	}

	@Override
	protected ThresholdMode thresholdMode()
	{
		return config.auxHpThresholdMode();
	}

	@Override
	protected int thresholdValue()
	{
		return config.auxHpThreshold();
	}

	@Override
	protected Color thresholdColor()
	{
		return config.auxHpThresholdColor();
	}

	@Override
	protected boolean thresholdAbove()
	{
		return config.auxHpThresholdDirection() == ThresholdDirection.AT_OR_ABOVE;
	}
}
