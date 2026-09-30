package com.viciousliqx.viciousbars;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import net.runelite.api.Client;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Shared drawing + placement logic for every vital bar. FLOATING bars are
 * draggable (alt-click, standard RuneLite overlay behavior) with a
 * persisted position; FIXED bars are pinned every frame to a position
 * computed by {@link PanelDock} and can't be dragged. Subclasses supply the
 * live data (current/max, color, icon, styling) for one specific bar.
 */
public abstract class VitalBarOverlay extends Overlay
{
	private static final Color BACKGROUND = new Color(0, 0, 0, 150);
	private static final int ICON_SIZE = 18;
	private static final int PADDING = 3;

	private final Client client;
	private final OrbRelocator orbRelocator;

	protected VitalBarOverlay(Client client, OrbRelocator orbRelocator)
	{
		this.client = client;
		this.orbRelocator = orbRelocator;
		setPosition(OverlayPosition.DYNAMIC);
		// UNDER_WIDGETS ("under all interfaces, but above overheads") rather than
		// the more common ABOVE_WIDGETS, specifically so a Relocated orb widget
		// sitting at our icon's position renders on TOP of our bar instead of
		// getting painted over by our own background fill.
		setLayer(OverlayLayer.UNDER_WIDGETS);
	}

	protected abstract boolean shouldRender();

	protected abstract PlacementMode getPlacementMode();

	protected abstract DockSide getDockSide();

	protected abstract int getDockSlot();

	protected abstract int getDockPadding();

	protected abstract int getCurrent();

	protected abstract int getMax();

	protected abstract Color getColor();

	protected abstract boolean isShowIcon();

	/** Null if disabled, unavailable, or not yet loaded. */
	protected abstract BufferedImage getIcon();

	protected abstract boolean isShowCounter();

	protected abstract Justification getJustification();

	protected abstract Orientation getOrientation();

	protected abstract int getThickness();

	protected abstract int getLength();

	/** VANILLA for bars with no orb (Warmth, Enemy HP). */
	protected abstract OrbMode getOrbMode();

	/** Packed widget component id (groupId << 16 | childId) for this bar's vanilla orb, or -1 if it has none. */
	protected abstract int getOrbComponentId();

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!shouldRender())
		{
			return null;
		}

		PlacementMode placementMode = getPlacementMode();
		setMovable(placementMode == PlacementMode.FLOATING);

		Orientation orientation;
		Point origin;
		if (placementMode == PlacementMode.FIXED)
		{
			DockSide side = getDockSide();
			orientation = side.orientation();

			Point pos = PanelDock.position(client, side, getDockSlot(), getThickness(), getDockPadding());
			if (pos == null)
			{
				// Panel widget not present (wrong layout, or not logged in yet) - nothing to dock to.
				return null;
			}
			// Non-movable DYNAMIC overlays aren't auto-translated by the overlay
			// renderer the way movable/dragged ones are (confirmed against Status
			// Bars' own source, which draws at absolute coordinates for exactly
			// this reason) - so we translate ourselves to reach the same effect.
			graphics.translate(pos.x, pos.y);
			origin = pos;
		}
		else
		{
			orientation = getOrientation();
			// The overlay renderer already translated the graphics context to
			// here (that's how movable overlays position themselves) and
			// recorded it in our bounds before calling render().
			origin = getBounds().getLocation();
		}

		int width = orientation == Orientation.VERTICAL ? getThickness() : getLength();
		int height = orientation == Orientation.VERTICAL ? getLength() : getThickness();

		graphics.setColor(BACKGROUND);
		graphics.fillRect(0, 0, width, height);

		int max = Math.max(getMax(), 1);
		int current = Math.max(0, Math.min(getCurrent(), max));
		double ratio = (double) current / max;

		graphics.setColor(getColor());
		if (orientation == Orientation.VERTICAL)
		{
			int fillHeight = (int) Math.round(ratio * height);
			graphics.fillRect(0, height - fillHeight, width, fillHeight);
		}
		else
		{
			int fillWidth = (int) Math.round(ratio * width);
			graphics.fillRect(0, 0, fillWidth, height);
		}

		Point iconLocal = drawIconAndCounter(graphics, width, height, orientation, current);

		updateOrbRelocation(iconLocal, origin);

		return new Dimension(width, height);
	}

	private void updateOrbRelocation(Point iconLocal, Point origin)
	{
		int componentId = getOrbComponentId();
		if (componentId < 0)
		{
			return;
		}

		if (getOrbMode() == OrbMode.RELOCATED && iconLocal != null)
		{
			orbRelocator.relocate(client, componentId, origin.x + iconLocal.x, origin.y + iconLocal.y);
		}
		else
		{
			orbRelocator.release(client, componentId);
		}
	}

	/** Returns the icon's local (bar-relative) top-left position, or null if no icon was drawn. */
	private Point drawIconAndCounter(Graphics2D graphics, int width, int height, Orientation orientation, int current)
	{
		boolean showIcon = isShowIcon();
		BufferedImage icon = showIcon ? getIcon() : null;
		boolean showCounter = isShowCounter();

		if (icon == null && !showCounter)
		{
			return null;
		}

		String counterText = showCounter ? Integer.toString(current) : null;
		graphics.setFont(FontManager.getRunescapeSmallFont());
		int textWidth = counterText != null ? graphics.getFontMetrics().stringWidth(counterText) : 0;
		int textHeight = counterText != null ? graphics.getFontMetrics().getHeight() : 0;

		int clusterLength = icon != null ? ICON_SIZE : 0;
		if (counterText != null)
		{
			clusterLength += (icon != null ? PADDING : 0) + (orientation == Orientation.VERTICAL ? textHeight : textWidth);
		}

		int along = along(getJustification(), orientation == Orientation.VERTICAL ? height : width, clusterLength);
		Point iconLocal = null;

		if (orientation == Orientation.VERTICAL)
		{
			int y = along;
			if (icon != null)
			{
				int iconX = (width - ICON_SIZE) / 2;
				graphics.drawImage(icon, iconX, y, ICON_SIZE, ICON_SIZE, null);
				iconLocal = new Point(iconX, y);
				y += ICON_SIZE + PADDING;
			}
			if (counterText != null)
			{
				graphics.setColor(Color.WHITE);
				graphics.drawString(counterText, (width - textWidth) / 2, y + textHeight - graphics.getFontMetrics().getDescent());
			}
		}
		else
		{
			int x = along;
			if (icon != null)
			{
				int iconY = (height - ICON_SIZE) / 2;
				graphics.drawImage(icon, x, iconY, ICON_SIZE, ICON_SIZE, null);
				iconLocal = new Point(x, iconY);
				x += ICON_SIZE + PADDING;
			}
			if (counterText != null)
			{
				graphics.setColor(Color.WHITE);
				int textY = (height - textHeight) / 2 + textHeight - graphics.getFontMetrics().getDescent();
				graphics.drawString(counterText, x, textY);
			}
		}

		return iconLocal;
	}

	private static int along(Justification justification, int totalLength, int clusterLength)
	{
		switch (justification)
		{
			case START:
				return PADDING;
			case END:
				return totalLength - clusterLength - PADDING;
			case CENTER:
			default:
				return (totalLength - clusterLength) / 2;
		}
	}
}
