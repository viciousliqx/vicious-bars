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
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.client.game.SpriteManager;

/**
 * HP's vial bar - see VialOverlay for all the shared logic (frame, collar,
 * vial fill, number repositioning). Poison/envenom/disease detection restored
 * from v1 (see git log before the full-reset "gut pass") - same
 * VarPlayerID.POISON/DISEASE signals RuneLite's own core Poison plugin uses.
 */
@Singleton
class HpVialOverlay extends VialOverlay
{
	// Matches RuneLite's own PoisonPlugin: VarPlayerID.POISON stores a
	// combined poison/venom counter, >= this threshold means envenomed
	// rather than merely poisoned.
	private static final int VENOM_THRESHOLD = 1_000_000;

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
		int poison = client.getVarpValue(VarPlayerID.POISON);
		if (poison >= VENOM_THRESHOLD)
		{
			return config.hpEnvenomedColor();
		}
		if (poison > 0)
		{
			return config.hpPoisonedColor();
		}
		if (client.getVarpValue(VarPlayerID.DISEASE) > 0)
		{
			return config.hpDiseasedColor();
		}
		return config.hpNormalColor();
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

	@Override
	protected Color regenHighlightColor()
	{
		return config.hpRegenColor();
	}

	@Override
	protected Double drainPercentage()
	{
		// Only poisoned/envenomed have a drain to show - diseased is left
		// on regenPercentage()/regenHighlightColor() (unmodified HP regen)
		// until disease's own mechanics are confirmed in-game.
		return manager.hpDrainPercentage;
	}

	@Override
	protected boolean drainEnabled()
	{
		return config.hpShowDrain();
	}

	@Override
	protected Color drainHighlightColor()
	{
		// Locked to whatever color the fill itself is using for the current
		// poison state, rather than its own independent slider - so the
		// countdown reads as part of the poison state, not a separate choice.
		return fillColor();
	}
}
