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
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * TEMPORARY diagnostic overlay - outlines the real orbs and every ancestor
 * up to MAP_CONTAINER, walked dynamically via getParent() (not hardcoded
 * IDs - the Widget Inspector showed the orbs' real direct parent is
 * Orbs.UNIVERSE, not the outer ToplevelPreEoc.ORBS slot we assumed before),
 * so we can see exactly which widget is actually growing at each level.
 * Not meant to ship - rip this out once the minimap work is sorted.
 */
@Singleton
class DebugWidgetBoundsOverlay extends Overlay
{
	private static final Color ORB_COLOR = Color.CYAN;
	private static final Color[] ANCESTOR_COLORS = {
		Color.YELLOW, Color.ORANGE, Color.MAGENTA, Color.GREEN, Color.PINK, Color.WHITE,
	};

	private static final int[] ORB_COMPONENTS = {
		InterfaceID.Orbs.ORB_HEALTH,
		InterfaceID.Orbs.ORB_PRAYER,
		InterfaceID.Orbs.ORB_RUNENERGY,
		InterfaceID.Orbs.ORB_SPECENERGY,
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
		graphics.setStroke(new BasicStroke(1));

		for (int componentId : ORB_COMPONENTS)
		{
			Widget orb = client.getWidget(componentId);
			if (orb == null || orb.isHidden())
			{
				continue;
			}
			outline(graphics, orb, ORB_COLOR, null);
		}

		Widget anyOrb = client.getWidget(InterfaceID.Orbs.ORB_HEALTH);
		Widget current = anyOrb != null ? anyOrb.getParent() : null;
		for (int depth = 0; current != null && depth < ANCESTOR_COLORS.length; depth++)
		{
			if (!current.isHidden())
			{
				outline(graphics, current, ANCESTOR_COLORS[depth],
					"id=" + current.getId() + " " + current.getOriginalX() + "," + current.getOriginalY()
						+ " " + current.getWidth() + "x" + current.getHeight());
			}
			if (current.getId() == InterfaceID.ToplevelPreEoc.MAP_CONTAINER)
			{
				break;
			}
			current = current.getParent();
		}

		return null;
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
