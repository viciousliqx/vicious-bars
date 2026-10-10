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
import net.runelite.api.Prayer;
import net.runelite.api.Skill;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.plugins.itemstats.ItemStatChangesService;

/**
 * Prayer's vial bar - see VialOverlay for all the shared logic. Confirmed
 * via F8's widget dump that Prayer's native geometry is pixel-identical to
 * HP's (same backing sprite 1071, icon/number at the same relative
 * positions), so none of VialOverlay's dialed-in constants needed
 * retuning - only the orb, skill, and fill color differ here. Active/
 * inactive coloring restored from v1 (see git log before the full-reset
 * "gut pass"), upgraded from v1's QUICKPRAYER_ACTIVE varbit (only reflected
 * the Quick Prayers toggle) to checking every individual prayer.
 */
@Singleton
class PrayerVialOverlay extends VialOverlay
{
	@Inject
	private PrayerVialOverlay(Client client, CompactOrbsManager manager, CompactOrbsConfig config, WidgetManager widgetManager, SpriteManager spriteManager, ItemStatChangesService itemStatService)
	{
		super(client, manager, config, widgetManager, spriteManager, itemStatService);
	}

	@Override
	protected TargetWidget orbContainer()
	{
		return Orbs.PRAYER_ORB_CONTAINER;
	}

	@Override
	protected CompactOrbsConfig.BarOrientation orientation()
	{
		return config.prayerOrientation();
	}

	@Override
	protected int currentValue()
	{
		return client.getBoostedSkillLevel(Skill.PRAYER);
	}

	@Override
	protected int maxValue()
	{
		return client.getRealSkillLevel(Skill.PRAYER);
	}

	@Override
	protected Color fillColor()
	{
		return isActive() ? config.prayerActiveColor() : config.prayerInactiveColor();
	}

	private boolean isActive()
	{
		for (Prayer prayer : Prayer.values())
		{
			if (client.isPrayerActive(prayer))
			{
				return true;
			}
		}
		return false;
	}

	@Override
	protected Double regenPercentage()
	{
		return manager.prayerRegenPercentage;
	}

	@Override
	protected boolean regenEnabled()
	{
		return config.prayerShowRegen();
	}

	@Override
	protected Color regenHighlightColor()
	{
		return config.prayerRegenColor();
	}

	@Override
	protected Double drainPercentage()
	{
		return manager.prayerDrainPercentage;
	}

	@Override
	protected boolean drainEnabled()
	{
		return config.prayerShowDrain();
	}

	@Override
	protected Color drainHighlightColor()
	{
		// Drain only ever runs while active (isActive() must be true for
		// drainPercentage() to be non-null), so this is always
		// prayerActiveColor() in practice - using fillColor() directly
		// keeps it automatically consistent rather than a redundant slider.
		return fillColor();
	}

	@Override
	protected ThresholdMode thresholdMode()
	{
		return config.prayerThresholdMode();
	}

	@Override
	protected int thresholdValue()
	{
		return config.prayerThreshold();
	}

	@Override
	protected Color thresholdColor()
	{
		return config.prayerThresholdColor();
	}

	@Override
	protected boolean thresholdAbove()
	{
		return config.prayerThresholdDirection() == ThresholdDirection.AT_OR_ABOVE;
	}

	@Override
	protected int length()
	{
		return config.prayerLength();
	}

	@Override
	protected int width()
	{
		return config.prayerWidth();
	}

	@Override
	protected int horizontalOffset()
	{
		return config.prayerHorizontalOffset();
	}

	@Override
	protected int verticalOffset()
	{
		return config.prayerVerticalOffset();
	}

	@Override
	protected int frameThickness()
	{
		return config.prayerFrameThickness();
	}

	@Override
	protected int frameShadowWidth()
	{
		return config.prayerFrameShadowWidth();
	}

	@Override
	protected int barOpacity()
	{
		return config.prayerBarOpacity();
	}

	@Override
	protected int frameOpacity()
	{
		return config.prayerFrameOpacity();
	}

	@Override
	protected CompactOrbsConfig.RegenDrainPriority regenDrainPriority()
	{
		return config.prayerRegenDrainPriority();
	}

	@Override
	protected int regenDrainRingSize()
	{
		return config.prayerRegenDrainRingSize();
	}

	@Override
	protected boolean reverseBarFill()
	{
		return config.prayerReverseBarFill();
	}

	@Override
	protected boolean reverseRegenRing()
	{
		return config.prayerReverseRegenRing();
	}

	@Override
	protected boolean reverseDrainRing()
	{
		return config.prayerReverseDrainRing();
	}

	@Override
	protected boolean hideDepletionEffect()
	{
		return config.prayerHideDepletionEffect();
	}

	@Override
	protected int previewAmount()
	{
		if (!config.prayerShowHealPreview())
		{
			return 0;
		}
		return getRestoreValue(Skill.PRAYER.getName());
	}

	@Override
	protected Color previewFullColor()
	{
		return config.prayerHealPreviewFullColor();
	}

	@Override
	protected Color previewPartialColor()
	{
		return config.prayerHealPreviewPartialColor();
	}
}
