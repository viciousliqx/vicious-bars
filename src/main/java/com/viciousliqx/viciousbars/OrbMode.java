package com.viciousliqx.viciousbars;

/**
 * How a bar relates to its vanilla minimap orb equivalent - purely a display
 * choice, not an interaction one. Only applies to bars that actually have a
 * vanilla orb (HP, Prayer, Special Attack, Run) - Warmth and Enemy HP have
 * no orb to hide.
 *
 * There is no click-through mode. A MIRRORED mode (real orb untouched, bar
 * icon also independently clickable) and a click-through variant of MERGED
 * were both built and torn out after RuneLite's own Hub reviewers confirmed
 * (Discord #development) that letting a plugin-drawn element trigger a real
 * orb's action - by any mechanism, not just client.menuAction() - counts as
 * "adding new menu entries which cause actions to be sent to the server"
 * and isn't allowed, regardless of implementation. See
 * vicious_bars_dev_talk.txt for the full investigation.
 */
public enum OrbMode
{
	/** Orb untouched, bar is purely a bar. */
	VANILLA,
	/** Real orb hidden while this bar is rendering; bar is purely a display, same as Vanilla otherwise. */
	MERGED,
	/** Real orb repositioned to sit at this bar's icon position - still the real, fully clickable orb, just moved. */
	ALIGNED
}
