package com.viciousliqx.viciousbars;

import java.awt.Color;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.Skill;
import net.runelite.api.Varbits;
import net.runelite.api.events.GameTick;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.game.SpriteManager;

@Singleton
class PrayerBarOverlay extends VitalBarOverlay
{
	// Captured via a real MenuOptionClicked event from manually clicking the
	// vanilla orb - NOT (160<<16)|18 from Movable Orbs, which turned out to be
	// the orb's container, not the actual clickable sub-widget.
	private static final int PRAYER_ORB_COMPONENT = (160 << 16) | 20;

	// Sane fallback before we've observed a real tick's length, and a ceiling
	// on how long an observed gap can be before we distrust it (e.g. logging
	// in, or the game window losing focus) rather than let one freak reading
	// make the metronome crawl for a full cycle.
	private static final long DEFAULT_TICK_NANOS = 600_000_000L;
	private static final long MAX_PLAUSIBLE_TICK_NANOS = DEFAULT_TICK_NANOS * 5;
	private static final long FLASH_DURATION_NANOS = 150_000_000L;

	private final Client client;
	private final ViciousBarsConfig config;
	private final SkillIconManager skillIconManager;
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
	private PrayerBarOverlay(Client client, ViciousBarsConfig config, SkillIconManager skillIconManager, OpponentTracker opponentTracker,
		SpriteManager spriteManager)
	{
		super(client, spriteManager);
		this.client = client;
		this.config = config;
		this.skillIconManager = skillIconManager;
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
	protected boolean isShowIcon()
	{
		return config.prayerShowIcon();
	}

	@Override
	protected BufferedImage getIcon()
	{
		// No orb icon sprite exists for Prayer (confirmed against RuneLite's own
		// Status Bars source, which falls back to the skill icon for the same reason).
		return skillIconManager.getSkillImage(Skill.PRAYER, true);
	}

	@Override
	protected boolean isShowCounter()
	{
		return config.prayerShowCounter();
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

	@Override
	protected OrbMode getOrbMode()
	{
		return config.prayerOrbMode();
	}

	@Override
	protected void onOrbActivate()
	{
		// Exact parameters captured from a real click on the vanilla orb via
		// MenuOptionClicked: action=CC_OP id=1 param0=-1 option=Activate/
		// Deactivate target=Quick-prayers.
		String option = isActive() ? "Deactivate" : "Activate";
		client.menuAction(-1, PRAYER_ORB_COMPONENT, MenuAction.CC_OP, 1, -1, option, "Quick-prayers");
	}
}
