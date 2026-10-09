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
import net.runelite.api.Prayer;
import net.runelite.api.Skill;
import net.runelite.api.gameval.SpriteID;
import net.runelite.client.game.SpriteManager;

/**
 * Prayer's auxiliary bar - see HpAuxBarOverlay/AuxBarOverlay for the shared
 * design. State detection duplicates PrayerVialOverlay's own; regen/drain
 * percentage tracking reads the same CompactOrbsManager fields the main bar
 * does.
 */
@Singleton
class PrayerAuxBarOverlay extends AuxBarOverlay
{
	private final SpriteManager spriteManager;

	@Inject
	private PrayerAuxBarOverlay(Client client, CompactOrbsManager manager, CompactOrbsConfig config, SpriteManager spriteManager)
	{
		super(client, manager, config);
		this.spriteManager = spriteManager;
	}

	@Override
	protected boolean shouldRender()
	{
		if (!config.auxPrayerEnabled())
		{
			return false;
		}
		return !(manager.isCompactLayout() && config.prayerHideAuxWhenMainShown());
	}

	@Override
	protected PlacementMode getPlacementMode()
	{
		return config.auxPrayerPlacementMode();
	}

	@Override
	protected DockSide getDockSide()
	{
		return config.auxPrayerDockSide();
	}

	@Override
	protected int getDockSlot()
	{
		return config.auxPrayerDockSlot();
	}

	@Override
	protected Justification getDockJustification()
	{
		return config.auxPrayerDockJustification();
	}

	@Override
	protected boolean isClampToPanel()
	{
		return config.auxPrayerClampToPanel();
	}

	@Override
	protected Orientation getFloatingOrientation()
	{
		return config.auxPrayerFloatingOrientation();
	}

	@Override
	protected int getThickness()
	{
		return config.auxPrayerThickness();
	}

	@Override
	protected int getLength()
	{
		return config.auxPrayerLength();
	}

	@Override
	protected int getCurrent()
	{
		return client.getBoostedSkillLevel(Skill.PRAYER);
	}

	@Override
	protected int getMax()
	{
		return client.getRealSkillLevel(Skill.PRAYER);
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
	protected Color fillColor()
	{
		return isActive() ? config.auxPrayerActiveColor() : config.auxPrayerInactiveColor();
	}

	@Override
	protected boolean isShowIcon()
	{
		return config.auxPrayerShowIcon();
	}

	@Override
	protected BufferedImage getIcon()
	{
		return spriteManager.getSprite(SpriteID.OrbIcon.PRAYER, 0);
	}

	@Override
	protected int getIconSize()
	{
		return config.auxPrayerIconSize();
	}

	@Override
	protected boolean isShowCounter()
	{
		return config.auxPrayerShowCounter();
	}

	@Override
	protected Justification getContentJustification()
	{
		return config.auxPrayerContentJustification();
	}

	@Override
	protected int getContentOffset()
	{
		return config.auxPrayerContentOffset();
	}

	@Override
	protected boolean isReverseContent()
	{
		return config.auxPrayerReverseContent();
	}

	@Override
	protected int getOpacity()
	{
		return config.auxPrayerOpacity();
	}

	@Override
	protected Double regenPercentage()
	{
		return manager.prayerRegenPercentage;
	}

	@Override
	protected boolean regenEnabled()
	{
		return config.auxPrayerShowRegen();
	}

	@Override
	protected Color regenHighlightColor()
	{
		return config.auxPrayerRegenColor();
	}

	@Override
	protected Double drainPercentage()
	{
		return manager.prayerDrainPercentage;
	}

	@Override
	protected boolean drainEnabled()
	{
		return config.auxPrayerShowDrain();
	}

	@Override
	protected Color drainHighlightColor()
	{
		return fillColor();
	}

	@Override
	protected ThresholdMode thresholdMode()
	{
		return config.auxPrayerThresholdMode();
	}

	@Override
	protected int thresholdValue()
	{
		return config.auxPrayerThreshold();
	}

	@Override
	protected Color thresholdColor()
	{
		return config.auxPrayerThresholdColor();
	}

	@Override
	protected boolean thresholdAbove()
	{
		return config.auxPrayerThresholdDirection() == ThresholdDirection.AT_OR_ABOVE;
	}
}
