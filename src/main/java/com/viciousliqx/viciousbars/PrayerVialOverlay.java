package com.viciousliqx.viciousbars;

import com.compactorbs.CompactOrbsConfig;
import com.compactorbs.CompactOrbsManager;
import com.compactorbs.widget.TargetWidget;
import com.compactorbs.widget.WidgetManager;
import com.compactorbs.widget.elements.Orbs;
import java.awt.Color;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.client.game.SpriteManager;

/**
 * Prayer's vial bar - see VialOverlay for all the shared logic. Confirmed
 * via F8's widget dump that Prayer's native geometry is pixel-identical to
 * HP's (same backing sprite 1071, icon/number at the same relative
 * positions), so none of VialOverlay's dialed-in constants needed
 * retuning - only the orb, skill, and fill color differ here.
 */
@Singleton
class PrayerVialOverlay extends VialOverlay
{
	private static final Color FILL_COLOR = new Color(60, 110, 220);

	@Inject
	private PrayerVialOverlay(Client client, CompactOrbsManager manager, CompactOrbsConfig config, WidgetManager widgetManager, SpriteManager spriteManager)
	{
		super(client, manager, config, widgetManager, spriteManager);
	}

	@Override
	protected TargetWidget orbContainer()
	{
		return Orbs.PRAYER_ORB_CONTAINER;
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
		return FILL_COLOR;
	}
}
