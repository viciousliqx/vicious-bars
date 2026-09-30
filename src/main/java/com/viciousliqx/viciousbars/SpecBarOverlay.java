package com.viciousliqx.viciousbars;

import java.awt.Color;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.VarPlayer;
import net.runelite.api.gameval.SpriteID;
import net.runelite.client.game.SpriteManager;

@Singleton
class SpecBarOverlay extends VitalBarOverlay
{
	private static final Color COLOR = new Color(220, 170, 40);
	private static final int MAX_PERCENT = 100;

	// Captured via a real MenuOptionClicked event from manually clicking the
	// vanilla orb.
	private static final int SPECIAL_ATTACK_ORB_COMPONENT = (160 << 16) | 36;

	private final Client client;
	private final ViciousBarsConfig config;
	private final SpriteManager spriteManager;

	@Inject
	private SpecBarOverlay(Client client, ViciousBarsConfig config, SpriteManager spriteManager)
	{
		super(client);
		this.client = client;
		this.config = config;
		this.spriteManager = spriteManager;
	}

	@Override
	protected boolean shouldRender()
	{
		return config.specEnabled();
	}

	@Override
	protected PlacementMode getPlacementMode()
	{
		return config.specPlacementMode();
	}

	@Override
	protected DockSide getDockSide()
	{
		return config.specDockSide();
	}

	@Override
	protected int getDockSlot()
	{
		return config.specDockSlot();
	}

	@Override
	protected int getDockPadding()
	{
		return config.dockPadding();
	}

	@Override
	protected int getCurrent()
	{
		// VarPlayer.SPECIAL_ATTACK_PERCENT is 0-1000; convert to a 0-100 percentage.
		return client.getVarpValue(VarPlayer.SPECIAL_ATTACK_PERCENT) / 10;
	}

	@Override
	protected int getMax()
	{
		return MAX_PERCENT;
	}

	@Override
	protected Color getColor()
	{
		return COLOR;
	}

	@Override
	protected boolean isShowIcon()
	{
		return config.specShowIcon();
	}

	@Override
	protected BufferedImage getIcon()
	{
		return spriteManager.getSprite(SpriteID.OrbIcon.SPECIAL, 0);
	}

	@Override
	protected boolean isShowCounter()
	{
		return config.specShowCounter();
	}

	@Override
	protected Justification getJustification()
	{
		return config.specJustification();
	}

	@Override
	protected Orientation getOrientation()
	{
		return config.specFloatingOrientation();
	}

	@Override
	protected int getThickness()
	{
		return config.specThickness();
	}

	@Override
	protected int getLength()
	{
		return config.specLength();
	}

	@Override
	protected int getOpacity()
	{
		return config.specOpacity();
	}

	@Override
	protected OrbMode getOrbMode()
	{
		return config.specOrbMode();
	}

	@Override
	protected void onOrbActivate()
	{
		// Exact parameters captured from a real click on the vanilla orb via
		// MenuOptionClicked: action=CC_OP id=1 param0=-1 option='Use'
		// target='<col=ff9040>Special Attack</col>' (the color tag is the
		// real, literal target string the game itself uses).
		client.menuAction(-1, SPECIAL_ATTACK_ORB_COMPONENT, MenuAction.CC_OP, 1, -1, "Use", "<col=ff9040>Special Attack</col>");
	}
}
