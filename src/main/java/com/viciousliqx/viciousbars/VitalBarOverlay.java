package com.viciousliqx.viciousbars;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
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
 *
 * For bars with an Orb mode (HP/Prayer/Spec/Run), the icon's current
 * on-screen rectangle is tracked every frame so {@link OrbClickListener} can
 * hit-test real mouse clicks against it and invoke {@link #onOrbActivate()}.
 */
public abstract class VitalBarOverlay extends Overlay
{
	private static final int BACKGROUND_BASE_ALPHA = 150;
	private static final int ICON_SIZE = 18;
	private static final int PADDING = 3;

	private final Client client;
	// Written on the client/render thread, read from the AWT event thread by
	// OrbClickListener - needs volatile for cross-thread visibility, plain
	// field writes aren't guaranteed to ever become visible on another thread.
	private volatile Rectangle iconBounds;

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

	protected abstract int getDockOffset();

	protected abstract Justification getDockSlotOrientation();

	protected abstract boolean isClampSlot();

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

	/** 0-100. Scales both the background and the fill color's alpha. */
	protected abstract int getOpacity();

	/** VANILLA for bars with no orb (Warmth, Enemy HP). */
	protected abstract OrbMode getOrbMode();

	/** Called when the icon is clicked while Orb mode is Merged or Mirrored. No-op for bars with no orb. */
	protected abstract void onOrbActivate();

	/** Current on-screen icon rectangle, or null if no icon is currently drawn. Used by {@link OrbClickListener}. */
	Rectangle getIconBounds()
	{
		return iconBounds;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!shouldRender())
		{
			iconBounds = null;
			return null;
		}

		PlacementMode placementMode = getPlacementMode();
		setMovable(placementMode == PlacementMode.FLOATING);

		Orientation orientation;
		Point origin;
		int length = getLength();
		if (placementMode == PlacementMode.FIXED)
		{
			DockSide side = getDockSide();
			orientation = side.orientation();

			if (isClampSlot())
			{
				int panelLength = PanelDock.panelLength(client, side);
				if (panelLength >= 0)
				{
					length = panelLength;
				}
			}

			Point pos = PanelDock.position(client, side, getDockSlot(), getThickness(), getDockPadding(), getDockOffset(),
				length, getDockSlotOrientation());
			if (pos == null)
			{
				// Panel widget not present (wrong layout, or not logged in yet) - nothing to dock to.
				iconBounds = null;
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

		int width = orientation == Orientation.VERTICAL ? getThickness() : length;
		int height = orientation == Orientation.VERTICAL ? length : getThickness();

		float opacityFraction = Math.max(0, Math.min(getOpacity(), 100)) / 100f;

		graphics.setColor(new Color(0, 0, 0, Math.round(BACKGROUND_BASE_ALPHA * opacityFraction)));
		graphics.fillRect(0, 0, width, height);

		int max = Math.max(getMax(), 1);
		int current = Math.max(0, Math.min(getCurrent(), max));
		double ratio = (double) current / max;

		Color fillColor = getColor();
		graphics.setColor(new Color(fillColor.getRed(), fillColor.getGreen(), fillColor.getBlue(),
			Math.round(255 * opacityFraction)));
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
		iconBounds = iconLocal != null
			? new Rectangle(origin.x + iconLocal.x, origin.y + iconLocal.y, ICON_SIZE, ICON_SIZE)
			: null;

		return new Dimension(width, height);
	}

	/** Returns the icon's local (bar-relative) top-left position, or null if no icon was drawn. */
	private Point drawIconAndCounter(Graphics2D graphics, int width, int height, Orientation orientation, int current)
	{
		OrbMode orbMode = getOrbMode();
		// The icon IS the click target in Merged/Mirrored mode, so it can't be
		// turned off in those modes regardless of the Show Icon setting.
		boolean showIcon = isShowIcon() || orbMode == OrbMode.MERGED || orbMode == OrbMode.MIRRORED;
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
