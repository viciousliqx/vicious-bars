package com.viciousliqx.viciousbars;

public enum PlacementMode
{
	FIXED,
	FLOATING;

	// FIXED displays as "Docked" to match the Dock side/slot/offset/padding
	// naming used everywhere else - kept as the enum constant FIXED rather
	// than renamed, so anyone's already-saved config isn't silently reset.
	@Override
	public String toString()
	{
		return this == FIXED ? "Docked" : "Floating";
	}
}
