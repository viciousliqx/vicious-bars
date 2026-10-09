package com.viciousliqx.viciousbars;

import com.compactorbs.CompactOrbsConfig;
import com.compactorbs.CompactOrbsConfig.ThresholdDirection;
import com.compactorbs.CompactOrbsConfig.ThresholdMode;
import com.compactorbs.CompactOrbsManager;
import com.compactorbs.widget.TargetWidget;
import com.compactorbs.widget.WidgetManager;
import com.compactorbs.widget.elements.Orbs;
import java.awt.Color;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.client.game.SpriteManager;

/**
 * Run energy's vial bar - see VialOverlay for all the shared logic.
 * Confirmed via F8's widget dump that Run's native geometry is
 * pixel-identical to HP's (same backing sprite 1071, icon/number at the
 * same relative positions). client.getEnergy() returns 0-10000 (not
 * 0-100 as first assumed) - confirmed live: without the /100, the bar
 * read as permanently full, since any real value over 100 just clamped
 * to max every frame.
 */
@Singleton
class RunVialOverlay extends VialOverlay
{
	@Inject
	private RunVialOverlay(Client client, CompactOrbsManager manager, CompactOrbsConfig config, WidgetManager widgetManager, SpriteManager spriteManager)
	{
		super(client, manager, config, widgetManager, spriteManager);
	}

	@Override
	protected TargetWidget orbContainer()
	{
		return Orbs.RUN_ORB_CONTAINER;
	}

	@Override
	protected CompactOrbsConfig.BarOrientation orientation()
	{
		return config.runOrientation();
	}

	@Override
	protected int currentValue()
	{
		return client.getEnergy() / 100;
	}

	@Override
	protected int maxValue()
	{
		return 100;
	}

	@Override
	protected Color fillColor()
	{
		return isActive() ? config.runActiveColor() : config.runDefaultColor();
	}

	private boolean isActive()
	{
		return client.getVarpValue(VarPlayerID.OPTION_RUN) != 0;
	}

	@Override
	protected Double regenPercentage()
	{
		// Run energy regen changed in OSRS's 2025 rework to a smooth
		// per-tick fractional gain rather than discrete periodic jumps -
		// no tick-tracking needed, just the sub-percent progress toward the
		// next whole percentage point client.getEnergy() (0-10000) hasn't
		// crossed yet.
		return (client.getEnergy() % 100) / 100.0;
	}

	@Override
	protected boolean regenEnabled()
	{
		return config.runShowRegen();
	}

	@Override
	protected Color regenHighlightColor()
	{
		return config.runRegenColor();
	}

	@Override
	protected ThresholdMode thresholdMode()
	{
		return config.runThresholdMode();
	}

	@Override
	protected int thresholdValue()
	{
		return config.runThreshold();
	}

	@Override
	protected Color thresholdColor()
	{
		return config.runThresholdColor();
	}

	@Override
	protected boolean thresholdAbove()
	{
		return config.runThresholdDirection() == ThresholdDirection.AT_OR_ABOVE;
	}

	@Override
	protected int length()
	{
		return config.runLength();
	}

	@Override
	protected int width()
	{
		return config.runWidth();
	}

	@Override
	protected int horizontalOffset()
	{
		return config.runHorizontalOffset();
	}

	@Override
	protected int verticalOffset()
	{
		return config.runVerticalOffset();
	}

	@Override
	protected int frameThickness()
	{
		return config.runFrameThickness();
	}

	@Override
	protected int frameShadowWidth()
	{
		return config.runFrameShadowWidth();
	}

	@Override
	protected int barOpacity()
	{
		return config.runBarOpacity();
	}

	@Override
	protected int frameOpacity()
	{
		return config.runFrameOpacity();
	}

	@Override
	protected CompactOrbsConfig.RegenDrainPriority regenDrainPriority()
	{
		return config.runRegenDrainPriority();
	}

	@Override
	protected int regenDrainRingSize()
	{
		return config.runRegenDrainRingSize();
	}

	@Override
	protected boolean reverseBarFill()
	{
		return config.runReverseBarFill();
	}

	@Override
	protected boolean reverseRegenRing()
	{
		return config.runReverseRegenRing();
	}

	@Override
	protected boolean reverseDrainRing()
	{
		return config.runReverseDrainRing();
	}
}
