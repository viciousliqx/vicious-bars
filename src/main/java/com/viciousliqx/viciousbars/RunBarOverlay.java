package com.viciousliqx.viciousbars;

import java.awt.Color;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.gameval.SpriteID;
import net.runelite.client.game.SpriteManager;

@Singleton
class RunBarOverlay extends VitalBarOverlay
{
	private static final int MAX_PERCENT = 100;

	// Captured via a real MenuOptionClicked event from manually clicking the
	// vanilla orb.
	private static final int RUN_ORB_COMPONENT = (160 << 16) | 28;

	// VarPlayer 173 "TOGGLE_RUN" - wiki-documented (oldschool.runescape.wiki/w/RuneScape:Varplayer/173),
	// no named constant in RuneLite's own VarPlayer enum, so queried by raw id.
	private static final int TOGGLE_RUN_VARP = 173;

	private final Client client;
	private final ViciousBarsConfig config;
	private final SpriteManager spriteManager;

	@Inject
	private RunBarOverlay(Client client, ViciousBarsConfig config, SpriteManager spriteManager)
	{
		super(client);
		this.client = client;
		this.config = config;
		this.spriteManager = spriteManager;
	}

	@Override
	protected boolean shouldRender()
	{
		return config.runEnabled();
	}

	@Override
	protected PlacementMode getPlacementMode()
	{
		return config.runPlacementMode();
	}

	@Override
	protected DockSide getDockSide()
	{
		return config.runDockSide();
	}

	@Override
	protected int getDockSlot()
	{
		return config.runDockSlot();
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
		return config.runDockJustification();
	}

	@Override
	protected boolean isClampSlot()
	{
		return config.runClampSlot();
	}

	@Override
	protected int getCurrent()
	{
		// client.getEnergy() is 0-10000; convert to a 0-100 percentage for display.
		return client.getEnergy() / 100;
	}

	@Override
	protected int getMax()
	{
		return MAX_PERCENT;
	}

	@Override
	protected Color getColor()
	{
		return client.getVarpValue(TOGGLE_RUN_VARP) != 0 ? config.runActiveColor() : config.runInactiveColor();
	}

	@Override
	protected boolean isShowIcon()
	{
		return config.runShowIcon();
	}

	@Override
	protected BufferedImage getIcon()
	{
		// No dedicated "run" orb icon - core Status Bars uses the walk/boot icon
		// for this bar too, so we match that exact choice.
		return spriteManager.getSprite(SpriteID.OrbIcon.WALK, 0);
	}

	@Override
	protected boolean isShowCounter()
	{
		return config.runShowCounter();
	}

	@Override
	protected Justification getJustification()
	{
		return config.runJustification();
	}

	@Override
	protected Orientation getOrientation()
	{
		return config.runFloatingOrientation();
	}

	@Override
	protected int getThickness()
	{
		return config.runThickness();
	}

	@Override
	protected int getLength()
	{
		return config.runLength();
	}

	@Override
	protected int getBorderWidth()
	{
		return config.borderWidth();
	}

	@Override
	protected int getOpacity()
	{
		return config.runOpacity();
	}

	@Override
	protected OrbMode getOrbMode()
	{
		return config.runOrbMode();
	}

	@Override
	protected void onOrbActivate()
	{
		// Exact parameters captured from a real click on the vanilla orb via
		// MenuOptionClicked: action=CC_OP id=1 param0=-1 option='Toggle Run'
		// target=''.
		client.menuAction(-1, RUN_ORB_COMPONENT, MenuAction.CC_OP, 1, -1, "Toggle Run", "");
	}
}
