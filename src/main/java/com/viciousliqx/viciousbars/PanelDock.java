package com.viciousliqx.viciousbars;

import java.awt.Point;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;

/**
 * Computes fixed-dock screen positions relative to the inventory/tabs panel.
 *
 * The widget below was picked empirically, not by name: Status Bars' own
 * Viewport enum lists four candidate widgets for four different client
 * layouts (ToplevelOsrsStretch/RESIZED_BOX, ToplevelPreEoc/RESIZED_BOTTOM,
 * Toplevel/FIXED, Bankside/FIXED_BANK), and their names don't reliably say
 * which one is "modern" vs "classic" - probing all three non-bank candidates
 * live in a real resizable-modern client showed ToplevelPreEoc.SIDE_BACKGROUND
 * is the one that's actually present and visible; the other two were always
 * null. Going with what was verified at runtime over what the name implied.
 *
 * Unlike Status Bars (which docks exactly 2 fixed-width bars and bakes its
 * own bar width into hand-picked offset constants), our bars have
 * configurable thickness/length, so slot 1 is anchored a small fixed margin
 * from the panel edge and additional slots stack outward by
 * thickness + padding, rather than reusing Status Bars' literal offsets.
 */
final class PanelDock
{
	private PanelDock()
	{
	}

	/** -1 if the panel widget isn't currently present/visible (e.g. not logged in). */
	static int panelLength(Client client, DockSide side)
	{
		Widget panel = client.getWidget(InterfaceID.ToplevelPreEoc.SIDE_BACKGROUND);
		if (panel == null || panel.isHidden())
		{
			return -1;
		}

		return side.orientation() == Orientation.VERTICAL ? panel.getHeight() : panel.getWidth();
	}

	/** Null if the panel widget isn't currently present/visible (e.g. not logged in). */
	static Point position(Client client, DockSide side, int slot, int thickness, int padding, int offset,
		int length, Justification dockJustification)
	{
		Widget panel = client.getWidget(InterfaceID.ToplevelPreEoc.SIDE_BACKGROUND);
		if (panel == null || panel.isHidden())
		{
			return null;
		}

		net.runelite.api.Point panelLocation = panel.getCanvasLocation();
		int panelX = panelLocation.getX();
		int panelY = panelLocation.getY();
		int panelWidth = panel.getWidth();
		int panelHeight = panel.getHeight();

		int outward = (Math.max(slot, 1) - 1) * (thickness + padding);
		int panelAlongLength = side.orientation() == Orientation.VERTICAL ? panelHeight : panelWidth;
		int along = along(dockJustification, panelAlongLength, length);

		switch (side)
		{
			case LEFT:
				return new Point(panelX - offset - thickness - outward, panelY + along);
			case RIGHT:
				return new Point(panelX + panelWidth + offset + outward, panelY + along);
			case TOP:
				return new Point(panelX + along, panelY - offset - thickness - outward);
			case BOTTOM:
			default:
				return new Point(panelX + along, panelY + panelHeight + offset + outward);
		}
	}

	// Same semantics as VitalBarOverlay's icon/counter justification, applied
	// to the bar's own position along the panel's length instead - START
	// (default) reproduces the original always-flush-with-the-corner
	// behavior exactly, so existing configs don't shift.
	private static int along(Justification justification, int panelLength, int barLength)
	{
		switch (justification)
		{
			case END:
				return panelLength - barLength;
			case CENTER:
				return (panelLength - barLength) / 2;
			case START:
			default:
				return 0;
		}
	}
}
