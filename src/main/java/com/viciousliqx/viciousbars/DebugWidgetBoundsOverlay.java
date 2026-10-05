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
 * TEMPORARY diagnostic overlay - outlines the real orb/minimap widget tree
 * live in-game so we can see exactly where the containers that clamp
 * Aligned-mode repositioning actually are, before deciding what to resize or
 * move. Not meant to ship - rip this out once the minimap work is sorted.
 */
@Singleton
class DebugWidgetBoundsOverlay extends Overlay
{
	private static final Color ORB_COLOR = Color.CYAN;
	private static final Color ORB_PARENT_COLOR = Color.YELLOW;
	private static final Color MAP_CONTAINER_COLOR = Color.MAGENTA;

	private static final int[] ORB_COMPONENTS = {
		InterfaceID.Orbs.ORB_HEALTH,
		InterfaceID.Orbs.ORB_PRAYER,
		InterfaceID.Orbs.ORB_RUNENERGY,
		InterfaceID.Orbs.ORB_SPECENERGY,
	};

	private final Client client;
	private final HpBarOverlay hpBarOverlay;
	private final PrayerBarOverlay prayerBarOverlay;
	private final SpecBarOverlay specBarOverlay;
	private final RunBarOverlay runBarOverlay;

	@Inject
	private DebugWidgetBoundsOverlay(Client client, HpBarOverlay hpBarOverlay, PrayerBarOverlay prayerBarOverlay,
		SpecBarOverlay specBarOverlay, RunBarOverlay runBarOverlay)
	{
		this.client = client;
		this.hpBarOverlay = hpBarOverlay;
		this.prayerBarOverlay = prayerBarOverlay;
		this.specBarOverlay = specBarOverlay;
		this.runBarOverlay = runBarOverlay;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		graphics.setColor(Color.WHITE);
		graphics.drawString("HP target: " + hpBarOverlay.getLastIconCanvasCenter(), 10, 400);
		graphics.drawString("Prayer target: " + prayerBarOverlay.getLastIconCanvasCenter(), 10, 415);
		graphics.drawString("Spec target: " + specBarOverlay.getLastIconCanvasCenter(), 10, 430);
		graphics.drawString("Run target: " + runBarOverlay.getLastIconCanvasCenter(), 10, 445);

		graphics.setStroke(new BasicStroke(1));

		Widget orbsParent = null;
		for (int componentId : ORB_COMPONENTS)
		{
			Widget orb = client.getWidget(componentId);
			if (orb == null || orb.isHidden())
			{
				continue;
			}
			outline(graphics, orb, ORB_COLOR, null);
			if (orbsParent == null)
			{
				orbsParent = orb.getParent();
			}
		}

		if (orbsParent != null)
		{
			outline(graphics, orbsParent, ORB_PARENT_COLOR,
				"orb parent " + orbsParent.getWidth() + "x" + orbsParent.getHeight());
		}

		Widget mapContainer = client.getWidget(InterfaceID.ToplevelPreEoc.MAP_CONTAINER);
		if (mapContainer != null && !mapContainer.isHidden())
		{
			outline(graphics, mapContainer, MAP_CONTAINER_COLOR,
				"MAP_CONTAINER " + mapContainer.getWidth() + "x" + mapContainer.getHeight());
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
