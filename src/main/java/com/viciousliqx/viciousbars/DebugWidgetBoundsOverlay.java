package com.viciousliqx.viciousbars;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetPositionMode;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * TEMPORARY diagnostic overlay. Outlines every real orb (cyan), then walks
 * the orb's FULL ancestor chain from its direct parent up to the root -
 * every level, not just MAP_CONTAINER - drawing each in its own color and
 * labeling it with whatever MinimapDragDiagnostic's own well-known IDs
 * recognize it as (else just its raw id), its live size/canvas position,
 * and its real X/Y position modes. Any ancestor whose own bounds don't
 * fully contain the orb's bounds is flagged CLIPPING and drawn with a
 * thicker stroke - that's exactly what silently eats clicks even when the
 * orb itself looks fine, so it's the thing worth watching for while
 * dragging the map toward a spasm. Not meant to ship - rip this out once
 * the minimap work is sorted.
 */
@Singleton
class DebugWidgetBoundsOverlay extends Overlay
{
	private static final Color ORB_COLOR = Color.CYAN;

	private static final Color[] CHAIN_COLORS = {
		Color.YELLOW, Color.MAGENTA, Color.ORANGE, Color.GREEN,
		Color.PINK, Color.RED, new Color(120, 180, 255), Color.WHITE,
	};

	private static final int[] ORB_COMPONENTS = {
		InterfaceID.Orbs.ORB_HEALTH,
		InterfaceID.Orbs.ORB_PRAYER,
		InterfaceID.Orbs.ORB_RUNENERGY,
		InterfaceID.Orbs.ORB_SPECENERGY,
	};

	// Same widgets MinimapDragDiagnostic already tracks, so labels in both
	// line up with each other and with the log output.
	private static final int[] KNOWN_IDS = {
		InterfaceID.ToplevelPreEoc.MAP_CONTAINER,
		InterfaceID.ToplevelPreEoc.ORBS,
		InterfaceID.ToplevelPreEoc.MAP_MINIMAP,
		InterfaceID.ToplevelPreEoc.MINIMAP,
		InterfaceID.ToplevelPreEoc.COMPASSCLICK,
		InterfaceID.ToplevelPreEoc.GAMEFRAME,
		InterfaceID.ToplevelPreEoc.TLI_LISTENER,
		InterfaceID.ToplevelPreEoc.CONTROL,
	};

	private static final String[] KNOWN_NAMES = {
		"MAP_CONTAINER", "ORBS", "MAP_MINIMAP", "MINIMAP", "COMPASSCLICK",
		"GAMEFRAME", "TLI_LISTENER", "CONTROL",
	};

	private final Client client;

	@Inject
	private DebugWidgetBoundsOverlay(Client client)
	{
		this.client = client;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		Widget anyOrb = null;
		graphics.setStroke(new BasicStroke(1));
		for (int componentId : ORB_COMPONENTS)
		{
			Widget orb = client.getWidget(componentId);
			if (orb == null || orb.isHidden())
			{
				continue;
			}
			outline(graphics, orb, ORB_COLOR, null);
			if (anyOrb == null)
			{
				anyOrb = orb;
			}
		}

		if (anyOrb == null)
		{
			return null;
		}

		Point orbLoc = anyOrb.getCanvasLocation();
		Rectangle orbBounds = orbLoc == null ? null
			: new Rectangle(orbLoc.getX(), orbLoc.getY(), anyOrb.getWidth(), anyOrb.getHeight());

		Widget w = anyOrb.getParent();
		int depth = 0;
		while (w != null && depth < CHAIN_COLORS.length)
		{
			if (!w.isHidden())
			{
				Point loc = w.getCanvasLocation();
				boolean clips = orbBounds != null && loc != null
					&& !new Rectangle(loc.getX(), loc.getY(), w.getWidth(), w.getHeight()).contains(orbBounds);

				String label = depth + ":" + knownName(w.getId())
					+ " " + w.getWidth() + "x" + w.getHeight()
					+ " @" + (loc != null ? loc.getX() + "," + loc.getY() : "?")
					+ " x=" + modeName(w.getXPositionMode(), true)
					+ " y=" + modeName(w.getYPositionMode(), false)
					+ (clips ? " CLIPPING" : "");

				graphics.setStroke(new BasicStroke(clips ? 3 : 1));
				outline(graphics, w, CHAIN_COLORS[depth % CHAIN_COLORS.length], label);
			}
			w = w.getParent();
			depth++;
		}

		return null;
	}

	private String knownName(int id)
	{
		for (int i = 0; i < KNOWN_IDS.length; i++)
		{
			if (KNOWN_IDS[i] == id)
			{
				return KNOWN_NAMES[i];
			}
		}
		return "id=" + id;
	}

	private String modeName(int mode, boolean xAxis)
	{
		if (mode == WidgetPositionMode.ABSOLUTE_LEFT)
		{
			return xAxis ? "LEFT" : "TOP";
		}
		if (mode == WidgetPositionMode.ABSOLUTE_CENTER)
		{
			return "CENTER";
		}
		if (mode == WidgetPositionMode.ABSOLUTE_RIGHT)
		{
			return xAxis ? "RIGHT" : "BOTTOM";
		}
		return "mode(" + mode + ")";
	}

	private void outline(Graphics2D graphics, Widget widget, Color color, String label)
	{
		Point loc = widget.getCanvasLocation();
		if (loc == null)
		{
			return;
		}
		Rectangle bounds = new Rectangle(loc.getX(), loc.getY(), widget.getWidth(), widget.getHeight());
		graphics.setColor(color);
		graphics.draw(bounds);
		if (label != null)
		{
			graphics.drawString(label, bounds.x, Math.max(10, bounds.y - 2));
		}
	}
}
