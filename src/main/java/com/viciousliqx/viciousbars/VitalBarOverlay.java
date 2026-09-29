package com.viciousliqx.viciousbars;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Shared drawing + floating-placement logic for every vital bar. FIXED
 * placement (docked to a panel edge) isn't implemented yet - only FLOATING
 * bars render for now, so a bar's Placement config must be set to Floating
 * to see anything on screen at this stage. Subclasses supply the live data
 * (current/max, color, icon, styling) for one specific bar.
 */
public abstract class VitalBarOverlay extends Overlay
{
	private static final int THICKNESS = 24;
	private static final int LENGTH = 150;
	private static final Color BACKGROUND = new Color(0, 0, 0, 150);
	private static final int ICON_SIZE = 18;
	private static final int PADDING = 3;

	protected VitalBarOverlay()
	{
		setPosition(OverlayPosition.DYNAMIC);
		setMovable(true);
	}

	protected abstract boolean shouldRender();

	protected abstract int getCurrent();

	protected abstract int getMax();

	protected abstract Color getColor();

	protected abstract boolean isShowIcon();

	/** Null if disabled, unavailable, or not yet loaded. */
	protected abstract BufferedImage getIcon();

	protected abstract boolean isShowCounter();

	protected abstract Justification getJustification();

	protected abstract Orientation getOrientation();

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!shouldRender())
		{
			return null;
		}

		Orientation orientation = getOrientation();
		int width = orientation == Orientation.VERTICAL ? THICKNESS : LENGTH;
		int height = orientation == Orientation.VERTICAL ? LENGTH : THICKNESS;

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
