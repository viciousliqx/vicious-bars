package com.viciousliqx.viciousbars;

import java.awt.Color;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.Skill;
import net.runelite.api.VarPlayer;
import net.runelite.api.gameval.SpriteID;
import net.runelite.client.game.SpriteManager;

@Singleton
class HpBarOverlay extends VitalBarOverlay
{
	private static final Color NORMAL = new Color(220, 40, 40);
	private static final Color POISONED = new Color(90, 170, 60);
	private static final Color ENVENOMED = new Color(60, 120, 40);
	private static final Color DISEASED = new Color(150, 110, 40);

	// Captured via a real MenuOptionClicked event from manually clicking the
	// vanilla orb while poisoned: action=CC_OP id=1 param0=-1 option='Cure'
	// target=''. The orb has no click action at all while not poisoned -
	// that's real vanilla behavior (not something we gate ourselves), so
	// firing this when not poisoned relies on the game's own script behind
	// this component being a no-op, same as every other orb we've wired up.
	private static final int HP_ORB_COMPONENT = (160 << 16) | 9;

	private final Client client;
	private final ViciousBarsConfig config;
	private final SpriteManager spriteManager;

	@Inject
	private HpBarOverlay(Client client, ViciousBarsConfig config, SpriteManager spriteManager)
	{
		super(client, spriteManager);
		this.client = client;
		this.config = config;
		this.spriteManager = spriteManager;
	}

	@Override
	protected boolean shouldRender()
	{
		return config.hpEnabled();
	}

	@Override
	protected PlacementMode getPlacementMode()
	{
		return config.hpPlacementMode();
	}

	@Override
	protected DockSide getDockSide()
	{
		return config.hpDockSide();
	}

	@Override
	protected int getDockSlot()
	{
		return config.hpDockSlot();
	}

	@Override
	protected int getDockPadding()
	{
		return config.dockPadding();
	}

	@Override
	protected int getDockOffset()
	{
		return config.dockOffset();
	}

	@Override
	protected Justification getDockSlotOrientation()
	{
		return config.hpDockJustification();
	}

	@Override
	protected boolean isClampSlot()
	{
		return config.hpClampSlot();
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
	protected Color getColor()
	{
		int poison = client.getVarpValue(VarPlayer.POISON);
		if (poison >= 1_000_000)
		{
			return ENVENOMED;
		}
		if (poison > 0)
		{
			return POISONED;
		}
		if (client.getVarpValue(VarPlayer.DISEASE_VALUE) > 0)
		{
			return DISEASED;
		}
		return NORMAL;
	}

	@Override
	protected boolean isShowIcon()
	{
		return config.hpShowIcon();
	}

	@Override
	protected BufferedImage getIcon()
	{
		return spriteManager.getSprite(SpriteID.OrbIcon.HITPOINTS, 0);
	}

	@Override
	protected boolean isShowCounter()
	{
		return config.hpShowCounter();
	}

	@Override
	protected Justification getJustification()
	{
		return config.hpJustification();
	}

	@Override
	protected int getContentOffset()
	{
		return config.hpContentOffset();
	}

	@Override
	protected Orientation getOrientation()
	{
		return config.hpFloatingOrientation();
	}

	@Override
	protected int getThickness()
	{
		return config.hpThickness();
	}

	@Override
	protected int getLength()
	{
		return config.hpLength();
	}

	@Override
	protected int getBorderWidth()
	{
		return config.borderWidth();
	}

	@Override
	protected int getOpacity()
	{
		return config.hpOpacity();
	}

	@Override
	protected OrbMode getOrbMode()
	{
		return config.hpOrbMode();
	}

	@Override
	protected void onOrbActivate()
	{
		client.menuAction(-1, HP_ORB_COMPONENT, MenuAction.CC_OP, 1, -1, "Cure", "");
	}
}
