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
 * Tracks the local player's current combat target, same pattern as
 * RuneLite's built-in OpponentInfo plugin: keep the last target for a few
 * seconds after interaction stops so dependent bars don't flicker away the
 * instant a fight ends.
 *
 * Tracks any Actor (NPC or player) for general "am I in combat" purposes,
 * but {@link #getOpponent()} only ever exposes the NPC case - that's the
 * only one the Enemy Hitpoints bar can draw anything meaningful for.
 */
@Singleton
public class OpponentTracker
{
	private static final Duration LINGER = Duration.ofSeconds(5);

	private final Client client;

	private Actor lastOpponentActor;
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

		lastOpponentActor = event.getTarget();
		lastInteractionTime = Instant.now();
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (lastOpponentActor != null
			&& client.getLocalPlayer().getInteracting() == null
			&& Duration.between(lastInteractionTime, Instant.now()).compareTo(LINGER) > 0)
		{
			lastOpponentActor = null;
		}
	}

	/** Null unless the current (or recently-lingering) target is specifically an NPC - used by the Enemy Hitpoints bar. */
	public NPC getOpponent()
	{
		return lastOpponentActor instanceof NPC ? (NPC) lastOpponentActor : null;
	}

	/** True while fighting anything - NPC or player - with the same lingering grace period as {@link #getOpponent()}. */
	public boolean isInCombat()
	{
		return lastOpponentActor != null;
	}

	public void reset()
	{
		lastOpponentActor = null;
		lastInteractionTime = null;
	}
}
