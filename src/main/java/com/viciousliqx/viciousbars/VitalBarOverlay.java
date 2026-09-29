package com.viciousliqx.viciousbars;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import net.runelite.api.Client;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
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

	protected VitalBarOverlay(Client client)
	{
		this.client = client;
		setPosition(OverlayPosition.DYNAMIC);
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
		}
		else
		{
			orientation = getOrientation();
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

		drawIconAndCounter(graphics, width, height, orientation, current);

		return new Dimension(width, height);
	}

	private void drawIconAndCounter(Graphics2D graphics, int width, int height, Orientation orientation, int current)
	{
		boolean showIcon = isShowIcon();
		BufferedImage icon = showIcon ? getIcon() : null;
		boolean showCounter = isShowCounter();

		if (icon == null && !showCounter)
		{
			return;
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

		if (orientation == Orientation.VERTICAL)
		{
			int y = along;
			if (icon != null)
			{
				graphics.drawImage(icon, (width - ICON_SIZE) / 2, y, ICON_SIZE, ICON_SIZE, null);
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
				graphics.drawImage(icon, x, (height - ICON_SIZE) / 2, ICON_SIZE, ICON_SIZE, null);
				x += ICON_SIZE + PADDING;
			}
			if (counterText != null)
			{
				graphics.setColor(Color.WHITE);
				int textY = (height - textHeight) / 2 + textHeight - graphics.getFontMetrics().getDescent();
				graphics.drawString(counterText, x, textY);
			}
		}
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
