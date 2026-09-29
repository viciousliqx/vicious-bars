package com.viciousliqx.viciousbars;

/**
 * How a bar relates to its vanilla minimap orb equivalent. Only applies to
 * bars that actually have a vanilla orb (HP, Prayer, Special Attack, Run) -
 * Warmth and Enemy HP have no orb to relocate/merge/mirror.
 */
public enum BaubleMode
{
	/** Orb untouched, bar is purely a bar. */
	VANILLA,
	/** Orb moves to sit at the top of the bar, keeps its vanilla click behavior. */
	RELOCATED,
	/** No separate orb rendered anywhere; the bar itself is the sole click target. */
	MERGED,
	/** Orb stays exactly where/how it is today AND the bar is also an independent click target. */
	MIRRORED
}
