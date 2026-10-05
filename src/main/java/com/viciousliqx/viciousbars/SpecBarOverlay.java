package com.viciousliqx.viciousbars;

import java.awt.Color;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.VarPlayer;
import net.runelite.api.gameval.InterfaceID;

@Singleton
class SpecBarOverlay extends VitalBarOverlay
{
	private static final int MAX_PERCENT = 100;

	// VarPlayer 301 - not a named RuneLite constant. Captured via
	// SpecialAttackSniffer: flipped 0 -> 1 in the exact same tick as a real
	// "Use Special Attack" click, while every other varbit/varp that changed
	// in that window was a continuously-ticking counter unrelated to the
	// click. Only one real click's worth of data so far (arming, not
	// un-arming) - worth confirming both directions once tested in-game.
	private static final int SPECIAL_ATTACK_QUEUED_VARP = 301;

	private final Client client;
	private final ViciousBarsConfig config;
	private final OpponentTracker opponentTracker;

	@Inject
	private SpecBarOverlay(Client client, ViciousBarsConfig config, OpponentTracker opponentTracker)
	{
		super(client);
		this.client = client;
		this.config = config;
		this.opponentTracker = opponentTracker;
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
	protected int getDockOffset()
	{
		return config.dockOffset();
	}

	@Override
	protected Justification getDockSlotOrientation()
	{
		return config.specDockJustification();
	}

	@Override
	protected boolean isClampSlot()
	{
		return config.specClampSlot();
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

	private boolean isQueued()
	{
		return client.getVarpValue(SPECIAL_ATTACK_QUEUED_VARP) != 0;
	}

	@Override
	protected Color getColor()
	{
		return isQueued() ? config.specQueuedColor() : config.specBarColor();
	}

	@Override
	protected Justification getJustification()
	{
		return config.specJustification();
	}

	@Override
	protected int getContentOffset()
	{
		return config.specContentOffset();
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
	protected int getBorderWidth()
	{
		return config.borderWidth();
	}

	@Override
	protected int getOpacity()
	{
		return config.specOpacity();
	}

	@Override
	protected int getOrbWidgetComponent()
	{
		return InterfaceID.Orbs.ORB_SPECENERGY;
	}

	@Override
	protected boolean isIconLit()
	{
		return opponentTracker.isInCombat() && getCurrent() >= config.specActivationThreshold();
	}

	@Override
	protected boolean isWarningEnabled()
	{
		return config.specWarningEnabled();
	}

	@Override
	protected int getWarningThreshold()
	{
		return config.specWarningThreshold();
	}

	@Override
	protected Color getWarningColor()
	{
		return config.specWarningColor();
	}

	@Override
	protected int getWarningOpacity()
	{
		return config.specWarningOpacity();
	}
}
