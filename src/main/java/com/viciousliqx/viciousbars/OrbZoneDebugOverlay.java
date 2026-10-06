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
 * Outlines every real orb plus its full ancestor chain up to MAP_CONTAINER,
 * so the real "zone" the orbs live in - and how big it actually is - is
 * visible while the minimap-replacement foundation gets built. Checks both
 * widget interfaces (Orbs and, if the user's also hit vanilla's own
 * minimize button, OrbsNomap) since they use entirely different component
 * ids. Not meant to ship - this whole plugin is a scoped foundation piece
 * right now, see ViciousBarsPlugin's javadoc.
 */
@Singleton
class OrbZoneDebugOverlay extends Overlay
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
		InterfaceID.OrbsNomap.ORB_HEALTH,
		InterfaceID.OrbsNomap.ORB_PRAYER,
		InterfaceID.OrbsNomap.ORB_RUNENERGY,
		InterfaceID.OrbsNomap.ORB_SPECENERGY,
	};

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
	private OrbZoneDebugOverlay(Client client)
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

		Widget w = anyOrb.getParent();
		int depth = 0;
		while (w != null && depth < CHAIN_COLORS.length)
		{
			if (!w.isHidden())
			{
				String label = depth + ":" + knownName(w.getId()) + " " + w.getWidth() + "x" + w.getHeight();
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
