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
 * Special attack's vial bar - see VialOverlay for all the shared logic.
 * Confirmed via F8's widget dump that Spec's native geometry is
 * pixel-identical to HP's (same backing sprite 1071, icon/number at the
 * same relative positions). VarPlayerID.SA_ENERGY (id 300 - confirmed by
 * decompiling the deprecated VarPlayer enum's actual constant value;
 * SA_ATTACK, id 301, is a tempting same-prefix red herring that's
 * actually SPECIAL_ATTACK_ENABLED, a boolean toggle, not the percent) is
 * stored x10 (0-1000 for 0-100%).
 */
@Singleton
class SpecVialOverlay extends VialOverlay
{
	@Inject
	private SpecVialOverlay(Client client, CompactOrbsManager manager, CompactOrbsConfig config, WidgetManager widgetManager, SpriteManager spriteManager)
	{
		super(client, manager, config, widgetManager, spriteManager);
	}

	@Override
	protected TargetWidget orbContainer()
	{
		return Orbs.SPEC_ORB_CONTAINER;
	}

	@Override
	protected CompactOrbsConfig.BarOrientation orientation()
	{
		return config.specOrientation();
	}

	@Override
	protected int currentValue()
	{
		return client.getVarpValue(VarPlayerID.SA_ENERGY) / 10;
	}

	@Override
	protected int maxValue()
	{
		return 100;
	}

	@Override
	protected Color fillColor()
	{
		return isActive() ? config.specActiveColor() : config.specDefaultColor();
	}

	private boolean isActive()
	{
		return client.getVarpValue(VarPlayerID.SA_ATTACK) != 0;
	}

	@Override
	protected Double regenPercentage()
	{
		return manager.specRegenPercentage;
	}

	@Override
	protected boolean regenEnabled()
	{
		return config.specShowRegen();
	}

	@Override
	protected Color regenHighlightColor()
	{
		return config.specRegenColor();
	}

	@Override
	protected ThresholdMode thresholdMode()
	{
		return config.specThresholdMode();
	}

	@Override
	protected int thresholdValue()
	{
		return config.specThreshold();
	}

	@Override
	protected Color thresholdColor()
	{
		return config.specThresholdColor();
	}

	@Override
	protected boolean thresholdAbove()
	{
		return config.specThresholdDirection() == ThresholdDirection.AT_OR_ABOVE;
	}

	@Override
	protected int length()
	{
		return config.specLength();
	}

	@Override
	protected int width()
	{
		return config.specWidth();
	}

	@Override
	protected int horizontalOffset()
	{
		return config.specHorizontalOffset();
	}

	@Override
	protected int verticalOffset()
	{
		return config.specVerticalOffset();
	}

	@Override
	protected int frameThickness()
	{
		return config.specFrameThickness();
	}

	@Override
	protected int frameShadowWidth()
	{
		return config.specFrameShadowWidth();
	}

	@Override
	protected int barOpacity()
	{
		return config.specBarOpacity();
	}

	@Override
	protected int frameOpacity()
	{
		return config.specFrameOpacity();
	}

	@Override
	protected CompactOrbsConfig.RegenDrainPriority regenDrainPriority()
	{
		return config.specRegenDrainPriority();
	}

	@Override
	protected int regenDrainRingSize()
	{
		return config.specRegenDrainRingSize();
	}

	@Override
	protected boolean reverseBarFill()
	{
		return config.specReverseBarFill();
	}

	@Override
	protected boolean reverseRegenRing()
	{
		return config.specReverseRegenRing();
	}

	@Override
	protected boolean reverseDrainRing()
	{
		return config.specReverseDrainRing();
	}

	@Override
	protected boolean hideDepletionEffect()
	{
		return config.specHideDepletionEffect();
	}
}
