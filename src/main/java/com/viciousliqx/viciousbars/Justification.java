package com.viciousliqx.viciousbars;

/**
 * Where the icon/counter cluster sits along a bar's length, independent of
 * orientation - same pattern as {@link DockSide#orientation()}. The
 * rendering layer maps these onto real screen positions: START is left on a
 * horizontal bar or top on a vertical bar, END is right or bottom.
 */
public enum Justification
{
	START,
	CENTER,
	END
}
