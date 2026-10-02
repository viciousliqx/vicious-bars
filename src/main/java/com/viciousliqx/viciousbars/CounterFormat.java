package com.viciousliqx.viciousbars;

/**
 * How Enemy Hitpoints' counter displays the game's own coarse health ratio -
 * the raw scale the game actually sends (e.g. "27/30", never a real HP
 * number for most monsters) versus a derived percentage.
 */
public enum CounterFormat
{
	FRACTION,
	PERCENTAGE
}
