package com.viciousliqx.viciousbars;

/**
 * Which edge of the panel a FIXED bar is docked to. Orientation is derived
 * from this rather than being a separate setting: LEFT/RIGHT bars are
 * vertical, TOP/BOTTOM bars are horizontal.
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
