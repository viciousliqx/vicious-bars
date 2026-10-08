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

/** HP's vial bar - see VialOverlay for all the shared logic (frame, collar, vial fill, number repositioning). */
@Singleton
class HpVialOverlay extends VialOverlay
{
	private static final Color FILL_COLOR = new Color(220, 40, 40);

	@Inject
	private HpVialOverlay(Client client, CompactOrbsManager manager, CompactOrbsConfig config, WidgetManager widgetManager, SpriteManager spriteManager)
	{
		super(client, manager, config, widgetManager, spriteManager);
	}

	@Override
	protected TargetWidget orbContainer()
	{
		return Orbs.HP_ORB_CONTAINER;
	}

	@Override
	protected int currentValue()
	{
		return client.getBoostedSkillLevel(Skill.HITPOINTS);
	}

	@Override
	protected int maxValue()
	{
		return client.getRealSkillLevel(Skill.HITPOINTS);
	}

	@Override
	protected Color fillColor()
	{
		return FILL_COLOR;
	}

	@Override
	protected Double regenPercentage()
	{
		return manager.hpRegenPercentage;
	}

	@Override
	protected boolean regenEnabled()
	{
		return config.hpShowRegen();
	}
}
