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
import net.runelite.api.gameval.SpriteID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.client.game.SpriteManager;

/**
 * Special Attack's auxiliary bar - see HpAuxBarOverlay/AuxBarOverlay for the
 * shared design. State detection duplicates SpecVialOverlay's own; regen
 * percentage tracking reads the same CompactOrbsManager field the main bar
 * does. No drain mechanic (matches the main bar - special attack doesn't
 * passively drain over time outside of use).
 */
@Singleton
class SpecAuxBarOverlay extends AuxBarOverlay
{
	private final SpriteManager spriteManager;

	@Inject
	private SpecAuxBarOverlay(Client client, CompactOrbsManager manager, CompactOrbsConfig config, SpriteManager spriteManager)
	{
		super(client, manager, config);
		this.spriteManager = spriteManager;
	}

	@Override
	protected boolean shouldRender()
	{
		if (!config.auxSpecEnabled())
		{
			return false;
		}
		return !(manager.isCompactLayout() && config.specHideAuxWhenMainShown());
	}

	@Override
	protected PlacementMode getPlacementMode()
	{
		return config.auxSpecPlacementMode();
	}

	@Override
	protected DockSide getDockSide()
	{
		return config.auxSpecDockSide();
	}

	@Override
	protected int getDockSlot()
	{
		return config.auxSpecDockSlot();
	}

	@Override
	protected Justification getDockJustification()
	{
		return config.auxSpecDockJustification();
	}

	@Override
	protected boolean isClampToPanel()
	{
		return config.auxSpecClampToPanel();
	}

	@Override
	protected Orientation getFloatingOrientation()
	{
		return config.auxSpecFloatingOrientation();
	}

	@Override
	protected int getThickness()
	{
		return config.auxSpecThickness();
	}

	@Override
	protected int getLength()
	{
		return config.auxSpecLength();
	}

	@Override
	protected int getCurrent()
	{
		return client.getVarpValue(VarPlayerID.SA_ENERGY) / 10;
	}

	@Override
	protected int getMax()
	{
		return 100;
	}

	private boolean isActive()
	{
		return client.getVarpValue(VarPlayerID.SA_ATTACK) != 0;
	}

	@Override
	protected Color fillColor()
	{
		return isActive() ? config.auxSpecActiveColor() : config.auxSpecDefaultColor();
	}

	@Override
	protected boolean isShowIcon()
	{
		return config.auxSpecShowIcon();
	}

	@Override
	protected BufferedImage getIcon()
	{
		return spriteManager.getSprite(SpriteID.OrbIcon.SPECIAL, 0);
	}

	@Override
	protected int getIconSize()
	{
		return config.auxSpecIconSize();
	}

	@Override
	protected boolean isShowCounter()
	{
		return config.auxSpecShowCounter();
	}

	@Override
	protected Justification getContentJustification()
	{
		return config.auxSpecContentJustification();
	}

	@Override
	protected int getContentOffset()
	{
		return config.auxSpecContentOffset();
	}

	@Override
	protected boolean isReverseContent()
	{
		return config.auxSpecReverseContent();
	}

	@Override
	protected int getOpacity()
	{
		return config.auxSpecOpacity();
	}

	@Override
	protected Double regenPercentage()
	{
		return manager.specRegenPercentage;
	}

	@Override
	protected boolean regenEnabled()
	{
		return config.auxSpecShowRegen();
	}

	@Override
	protected Color regenHighlightColor()
	{
		return config.auxSpecRegenColor();
	}

	@Override
	protected ThresholdMode thresholdMode()
	{
		return config.auxSpecThresholdMode();
	}

	@Override
	protected int thresholdValue()
	{
		return config.auxSpecThreshold();
	}

	@Override
	protected Color thresholdColor()
	{
		return config.auxSpecThresholdColor();
	}

	@Override
	protected boolean thresholdAbove()
	{
		return config.auxSpecThresholdDirection() == ThresholdDirection.AT_OR_ABOVE;
	}
}
