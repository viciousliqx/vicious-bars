package com.viciousliqx.viciousbars;

import java.awt.Point;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;

/**
 * Restored from v1 (see git log before the full-reset "gut pass") -
 * unchanged. Computes Docked screen positions relative to the inventory/tabs
 * panel (InterfaceID.ToplevelPreEoc.SIDE_BACKGROUND - verified present at
 * runtime in a real resizable-modern client; already used elsewhere in this
 * codebase, e.g. ViciousBarsPlugin's own widget dumps).
 *
 * Aux bars have configurable thickness/length, so slot 1 is anchored a small
 * fixed margin from the panel edge and additional slots stack outward by
 * thickness + padding.
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

	// Same semantics as AuxBarOverlay's icon/counter justification, applied to
	// the bar's own position along the panel's length instead - START
	// (default) keeps the bar flush with the corner.
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
