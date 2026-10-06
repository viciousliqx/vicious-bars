package com.viciousliqx.viciousbars;

import java.awt.Color;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.api.Varbits;
import net.runelite.api.events.GameTick;
import net.runelite.client.eventbus.Subscribe;

@Singleton
class PrayerBarOverlay extends VitalBarOverlay
{
	// Sane fallback before we've observed a real tick's length, and a ceiling
	// on how long an observed gap can be before we distrust it (e.g. logging
	// in, or the game window losing focus) rather than let one freak reading
	// make the metronome crawl for a full cycle.
	private static final long DEFAULT_TICK_NANOS = 600_000_000L;
	private static final long MAX_PLAUSIBLE_TICK_NANOS = DEFAULT_TICK_NANOS * 5;
	private static final long FLASH_DURATION_NANOS = 150_000_000L;

	private final Client client;
	private final ViciousBarsConfig config;
	private final OpponentTracker opponentTracker;

	// Metronome timing state - GameTick is the only authoritative signal we
	// get, so the marker's SPEED is a best guess (based on the last real
	// tick's measured length) but the FLASH always fires exactly on a real
	// tick, never on the interpolated guess, so the one cue that actually
	// matters for flicking timing is never subject to drift.
	private long lastTickNanos = System.nanoTime();
	private long tickDurationEstimateNanos = DEFAULT_TICK_NANOS;
	private long flashUntilNanos;

	@Inject
	private PrayerBarOverlay(Client client, ViciousBarsConfig config, OpponentTracker opponentTracker)
	{
		super(client);
		this.client = client;
		this.config = config;
		this.opponentTracker = opponentTracker;
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		long now = System.nanoTime();
		long observed = now - lastTickNanos;
		if (observed > 0 && observed < MAX_PLAUSIBLE_TICK_NANOS)
		{
			tickDurationEstimateNanos = observed;
		}
		lastTickNanos = now;
		flashUntilNanos = now + FLASH_DURATION_NANOS;
	}

	@Override
	protected boolean isMetronomeEnabled()
	{
		if (!config.prayerMetronomeEnabled())
		{
			return false;
		}
		return !config.prayerMetronomeOnlyInCombat() || opponentTracker.isInCombat();
	}

	@Override
	protected float getMetronomeFraction()
	{
		return (float) (System.nanoTime() - lastTickNanos) / tickDurationEstimateNanos;
	}

	@Override
	protected boolean isMetronomeFlashing()
	{
		return System.nanoTime() < flashUntilNanos;
	}

	@Override
	protected boolean isMetronomeReversed()
	{
		return config.prayerMetronomeReversed();
	}

	@Override
	protected boolean shouldRender()
	{
		return config.prayerEnabled();
	}

	@Override
	protected boolean isWarningEnabled()
	{
		return config.prayerWarningEnabled();
	}

	@Override
	protected int getWarningThreshold()
	{
		return config.prayerWarningThreshold();
	}

	@Override
	protected Color getWarningColor()
	{
		return config.prayerWarningColor();
	}

	@Override
	protected int getWarningOpacity()
	{
		return config.prayerWarningOpacity();
	}

	@Override
	protected PlacementMode getPlacementMode()
	{
		return config.prayerPlacementMode();
	}

	@Override
	protected DockSide getDockSide()
	{
		return config.prayerDockSide();
	}

	@Override
	protected int getDockSlot()
	{
		return config.prayerDockSlot();
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
		return config.prayerDockJustification();
	}

	@Override
	protected boolean isClampSlot()
	{
		return config.prayerClampSlot();
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
		return client.getVarbitValue(Varbits.QUICK_PRAYER) != 0;
	}

	@Override
	protected Color getColor()
	{
		return isActive() ? config.prayerActiveColor() : config.prayerInactiveColor();
	}

	@Override
	protected Justification getJustification()
	{
		return config.prayerJustification();
	}

	@Override
	protected int getContentOffset()
	{
		return config.prayerContentOffset();
	}

	@Override
	protected Orientation getOrientation()
	{
		return config.prayerFloatingOrientation();
	}

	@Override
	protected int getThickness()
	{
		return config.prayerThickness();
	}

	@Override
	protected int getLength()
	{
		return config.prayerLength();
	}

	@Override
	protected int getBorderWidth()
	{
		return config.borderWidth();
	}

	@Override
	protected int getOpacity()
	{
		return config.prayerOpacity();
	}
}
