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
 * Run Energy's auxiliary bar - see HpAuxBarOverlay/AuxBarOverlay for the
 * shared design. State detection duplicates RunVialOverlay's own; regen
 * percentage tracking is computed fresh here too (RunVialOverlay's own
 * regenPercentage() is a direct client.getEnergy() read, not a tick-tracked
 * manager field, so there's nothing stateful to share). No drain mechanic.
 */
@Singleton
class RunAuxBarOverlay extends AuxBarOverlay
{
	private final SpriteManager spriteManager;

	@Inject
	private RunAuxBarOverlay(Client client, CompactOrbsManager manager, CompactOrbsConfig config, SpriteManager spriteManager)
	{
		super(client, manager, config);
		this.spriteManager = spriteManager;
	}

	@Override
	protected boolean shouldRender()
	{
		if (!config.auxRunEnabled())
		{
			return false;
		}
		return !(manager.isCompactLayout() && config.runHideAuxWhenMainShown());
	}

	@Override
	protected PlacementMode getPlacementMode()
	{
		return config.auxRunPlacementMode();
	}

	@Override
	protected DockSide getDockSide()
	{
		return config.auxRunDockSide();
	}

	@Override
	protected int getDockSlot()
	{
		return config.auxRunDockSlot();
	}

	@Override
	protected Justification getDockJustification()
	{
		return config.auxRunDockJustification();
	}

	@Override
	protected boolean isClampToPanel()
	{
		return config.auxRunClampToPanel();
	}

	@Override
	protected Orientation getFloatingOrientation()
	{
		return config.auxRunFloatingOrientation();
	}

	@Override
	protected int getThickness()
	{
		return config.auxRunThickness();
	}

	@Override
	protected int getLength()
	{
		return config.auxRunLength();
	}

	@Override
	protected int getCurrent()
	{
		return client.getEnergy() / 100;
	}

	@Override
	protected int getMax()
	{
		return 100;
	}

	private boolean isActive()
	{
		return client.getVarpValue(VarPlayerID.OPTION_RUN) != 0;
	}

	@Override
	protected Color fillColor()
	{
		return isActive() ? config.auxRunActiveColor() : config.auxRunDefaultColor();
	}

	@Override
	protected boolean isShowIcon()
	{
		return config.auxRunShowIcon();
	}

	@Override
	protected BufferedImage getIcon()
	{
		return spriteManager.getSprite(SpriteID.OrbIcon.RUN, 0);
	}

	@Override
	protected int getIconSize()
	{
		return config.auxRunIconSize();
	}

	@Override
	protected boolean isShowCounter()
	{
		return config.auxRunShowCounter();
	}

	@Override
	protected Justification getContentJustification()
	{
		return config.auxRunContentJustification();
	}

	@Override
	protected int getContentOffset()
	{
		return config.auxRunContentOffset();
	}

	@Override
	protected boolean isReverseContent()
	{
		return config.auxRunReverseContent();
	}

	@Override
	protected int getOpacity()
	{
		return config.auxRunOpacity();
	}

	@Override
	protected Double regenPercentage()
	{
		return (client.getEnergy() % 100) / 100.0;
	}

	@Override
	protected boolean regenEnabled()
	{
		return config.auxRunShowRegen();
	}

	@Override
	protected Color regenHighlightColor()
	{
		return config.auxRunRegenColor();
	}

	@Override
	protected ThresholdMode thresholdMode()
	{
		return config.auxRunThresholdMode();
	}

	@Override
	protected int thresholdValue()
	{
		return config.auxRunThreshold();
	}

	@Override
	protected Color thresholdColor()
	{
		return config.auxRunThresholdColor();
	}

	@Override
	protected boolean thresholdAbove()
	{
		return config.auxRunThresholdDirection() == ThresholdDirection.AT_OR_ABOVE;
	}
}
