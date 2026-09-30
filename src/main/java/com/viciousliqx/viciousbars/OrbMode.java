package com.viciousliqx.viciousbars;

/**
 * How a bar relates to its vanilla minimap orb equivalent. Only applies to
 * bars that actually have a vanilla orb (HP, Prayer, Special Attack, Run) -
 * Warmth and Enemy HP have no orb to merge/mirror.
 *
 * A fourth mode, RELOCATED (physically move the real orb widget to sit at
 * the bar), was dropped - the widget is clipped to a small native parent
 * container near the minimap and can't be freely repositioned anywhere on
 * screen. See vicious_bars_dev_talk.txt for the full investigation.
 */
public enum OrbMode
{
	/** Orb untouched, bar is purely a bar. */
	VANILLA,
	/** No separate orb rendered anywhere; the bar itself is the sole click target. */
	MERGED,
	/** Orb stays exactly where/how it is today AND the bar is also an independent click target. */
	MIRRORED
}
