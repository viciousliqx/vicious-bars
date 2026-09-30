package com.viciousliqx.viciousbars;

import java.awt.Color;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.gameval.SpriteID;
import net.runelite.client.game.SpriteManager;

class RunBarOverlay extends VitalBarOverlay
{
	private static final Color COLOR = new Color(70, 180, 90);
	private static final int MAX_PERCENT = 100;

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
		return COLOR;
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
	protected OrbMode getOrbMode()
	{
		return config.runOrbMode();
	}

	@Override
	protected void onOrbActivate()
	{
		// TODO: toggle-run action, implemented after Prayer is proven.
	}
}
