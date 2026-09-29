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
	private static final int PANEL_MARGIN = 4;

	private PanelDock()
	{
	}

	/** Null if the panel widget isn't currently present/visible (e.g. not logged in). */
	static Point position(Client client, DockSide side, int slot, int thickness, int padding)
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

		switch (side)
		{
			case LEFT:
				return new Point(panelX - PANEL_MARGIN - thickness - outward, panelY);
			case RIGHT:
				return new Point(panelX + panelWidth + PANEL_MARGIN + outward, panelY);
			case TOP:
				return new Point(panelX, panelY - PANEL_MARGIN - thickness - outward);
			case BOTTOM:
			default:
				return new Point(panelX, panelY + panelHeight + PANEL_MARGIN + outward);
		}
	}
}
