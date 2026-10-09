package com.viciousliqx.viciousbars;

/**
 * Restored from v1 (see git log before the full-reset "gut pass") -
 * unchanged. Which edge of the inventory/tabs panel a Docked aux bar is
 * anchored to. Orientation is derived from this rather than being a
 * separate setting: LEFT/RIGHT bars are vertical, TOP/BOTTOM bars are
 * horizontal.
 */
public enum DockSide
{
	LEFT,
	RIGHT,
	TOP,
	BOTTOM;

	public Orientation orientation()
	{
		return (this == LEFT || this == RIGHT) ? Orientation.VERTICAL : Orientation.HORIZONTAL;
	}
}
