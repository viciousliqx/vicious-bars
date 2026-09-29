package com.viciousliqx.viciousbars;

import java.time.Duration;
import java.time.Instant;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.InteractingChanged;
import net.runelite.client.eventbus.Subscribe;

/**
 * Tracks the local player's current combat target for the Enemy Hitpoints
 * bar, same pattern as RuneLite's built-in OpponentInfo plugin: keep the
 * last opponent for a few seconds after interaction stops so the bar
 * doesn't flicker away the instant a fight ends.
 */
@Singleton
public class OpponentTracker
{
	private static final Duration LINGER = Duration.ofSeconds(5);

	private final Client client;

	private NPC lastOpponent;
	private Instant lastInteractionTime;

	@Inject
	private OpponentTracker(Client client)
	{
		this.client = client;
	}

	@Subscribe
	public void onInteractingChanged(InteractingChanged event)
	{
		if (event.getSource() != client.getLocalPlayer())
		{
			return;
		}

		Actor target = event.getTarget();
		lastInteractionTime = Instant.now();
		lastOpponent = target instanceof NPC ? (NPC) target : null;
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (lastOpponent != null
			&& client.getLocalPlayer().getInteracting() == null
			&& Duration.between(lastInteractionTime, Instant.now()).compareTo(LINGER) > 0)
		{
			lastOpponent = null;
		}
	}

	public NPC getOpponent()
	{
		return lastOpponent;
	}

	public void reset()
	{
		lastOpponent = null;
		lastInteractionTime = null;
	}
}
