package com.viciousliqx.viciousbars;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.RoundRectangle2D;
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
	private static final Color METRONOME_MARKER_COLOR = new Color(255, 255, 255, 200);
	private static final int METRONOME_MARKER_THICKNESS = 3;
	private static final Color METRONOME_FLASH_COLOR = new Color(255, 255, 255, 220);
	private static final Color ICON_LIT_COLOR = new Color(255, 215, 0, 220);
	private static final int ICON_GLOW_MARGIN = 3;
	// Approximated stone/granite look, not an extracted game texture - see
	// dev talk for the real sprite groups found (steelborder, v2_stone_borders/
	// pre_eoc_stones) and why a hand-picked color was chosen over assembling
	// their multi-piece 9-slice art for v1.
	private static final Color FRAME_FILL_COLOR = new Color(96, 82, 64);
	private static final Color FRAME_OUTLINE_COLOR = new Color(20, 17, 14);
	// A small fixed chamfer, not a fraction of the bar's own size - the real
	// inventory panel's corners are a small, consistent notch regardless of
	// the panel's overall size, not a dramatic circular round, so matching
	// that look means a fixed pixel value rather than something that scales
	// with Thickness the way the inner vial's rounding does.
	private static final int FRAME_ARC = 6;
	// Inner "vial" rounding is 75% of a full capsule (25% less than the true
	// semicircle-ended shape) per feedback that full capsule ends were too
	// round.
	private static final float INNER_ARC_FRACTION = 0.75f;

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

	/** Thickness in pixels of the stone frame drawn around the bar - shared across every bar. */
	protected abstract int getBorderWidth();

	/** 0-100. Scales both the background and the fill color's alpha. */
	protected abstract int getOpacity();

	/** VANILLA for bars with no orb (Warmth, Enemy HP). */
	protected abstract OrbMode getOrbMode();

	/** Called when the icon is clicked while Orb mode is Merged or Mirrored. No-op for bars with no orb. */
	protected abstract void onOrbActivate();

	// Metronome is Prayer-only for now - concrete no-op defaults here rather
	// than forcing the other 5 bars to override methods they'll never use.

	/** True only for bars with a metronome, and only while its config toggle is on. */
	protected boolean isMetronomeEnabled()
	{
		return false;
	}

	/** 0-1, how far through the current game tick we are. Only meaningful when {@link #isMetronomeEnabled()}. */
	protected float getMetronomeFraction()
	{
		return 0f;
	}

	/** True for a brief window right after a real game tick, to flash the icon. */
	protected boolean isMetronomeFlashing()
	{
		return false;
	}

	/** True to sweep the marker from the far end of the bar back toward the near end instead of near-to-far. */
	protected boolean isMetronomeReversed()
	{
		return false;
	}

	/** True to highlight the icon with a steady (non-blinking) overlay - e.g. Spec's threshold glow. No-op for bars without one. */
	protected boolean isIconLit()
	{
		return false;
	}

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

		// Clip everything we draw to the bar's own rectangle, so nothing -
		// now or in any future effect - can bleed past its edges.
		graphics.setClip(0, 0, width, height);

		float opacityFraction = Math.max(0, Math.min(getOpacity(), 100)) / 100f;

		// Stone-toned frame, drawn at the bar's full declared footprint so
		// dock-slot stacking math (which assumes Thickness/Length IS the
		// bar's full visual size) stays exactly correct - the active
		// content below is inset inward by borderWidth instead of the frame
		// being added on top of that footprint.
		//
		// The frame is only ever the RING between the outer shape and the
		// inner capsule - not a filled rect with the vial drawn on top of
		// it - so the vial's own area is never painted by us at all before
		// the vial itself draws there. That matters once the vial's Opacity
		// drops below 100: what shows through is the real game behind it,
		// not our own stone color bleeding through underneath. The frame
		// itself always stays fully opaque regardless of the bar's Opacity -
		// only the vial fades.
		int borderWidth = Math.max(0, getBorderWidth());
		int innerWidth = Math.max(1, width - 2 * borderWidth);
		int innerHeight = Math.max(1, height - 2 * borderWidth);

		int innerShortAxis = orientation == Orientation.VERTICAL ? innerWidth : innerHeight;
		int innerArc = Math.round(innerShortAxis * INNER_ARC_FRACTION);

		RoundRectangle2D.Float outerShape = new RoundRectangle2D.Float(0, 0, width, height, FRAME_ARC, FRAME_ARC);
		RoundRectangle2D.Float innerShapeAtInset = new RoundRectangle2D.Float(
			borderWidth, borderWidth, innerWidth, innerHeight, innerArc, innerArc);

		Area frameRing = new Area(outerShape);
		frameRing.subtract(new Area(innerShapeAtInset));

		graphics.setColor(FRAME_FILL_COLOR);
		graphics.fill(frameRing);

		// Stroke shapes are inset by 1px on the far edge from their fill
		// counterparts - a 1px-wide stroke is centered ON the path, so a
		// path running exactly along the bar's own right/bottom edge would
		// have half its stroke fall outside our own clip and get cut off
		// (confirmed - that's exactly what was happening, right/bottom edges
		// losing their outline while left/top kept theirs).
		graphics.setColor(FRAME_OUTLINE_COLOR);
		graphics.draw(new RoundRectangle2D.Float(0, 0, width - 1, height - 1, FRAME_ARC, FRAME_ARC));
		graphics.draw(new RoundRectangle2D.Float(borderWidth, borderWidth, innerWidth - 1, innerHeight - 1, innerArc, innerArc));

		graphics.translate(borderWidth, borderWidth);
		// Clipping to the capsule shape (rather than a plain rect) means the
		// background/fill/marker/icon below don't need their own rounding
		// logic - they're just drawn as plain rects and the clip crops them
		// to the capsule for free.
		graphics.setClip(new RoundRectangle2D.Float(0, 0, innerWidth, innerHeight, innerArc, innerArc));

		graphics.setColor(new Color(0, 0, 0, Math.round(BACKGROUND_BASE_ALPHA * opacityFraction)));
		graphics.fillRect(0, 0, innerWidth, innerHeight);

		int max = Math.max(getMax(), 1);
		int current = Math.max(0, Math.min(getCurrent(), max));
		double ratio = (double) current / max;

		Color fillColor = getColor();
		graphics.setColor(new Color(fillColor.getRed(), fillColor.getGreen(), fillColor.getBlue(),
			Math.round(255 * opacityFraction)));
		if (orientation == Orientation.VERTICAL)
		{
			int fillHeight = (int) Math.round(ratio * innerHeight);
			graphics.fillRect(0, innerHeight - fillHeight, innerWidth, fillHeight);
		}
		else
		{
			int fillWidth = (int) Math.round(ratio * innerWidth);
			graphics.fillRect(0, 0, fillWidth, innerHeight);
		}

		if (isMetronomeEnabled())
		{
			drawMetronomeMarker(graphics, innerWidth, innerHeight, orientation);
		}

		Color iconHighlight = null;
		if (isMetronomeEnabled() && isMetronomeFlashing())
		{
			iconHighlight = METRONOME_FLASH_COLOR;
		}
		else if (isIconLit())
		{
			iconHighlight = ICON_LIT_COLOR;
		}

		Point iconLocal = drawIconAndCounter(graphics, innerWidth, innerHeight, orientation, current, iconHighlight, innerArc);
		iconBounds = iconLocal != null
			? new Rectangle(origin.x + borderWidth + iconLocal.x, origin.y + borderWidth + iconLocal.y, ICON_SIZE, ICON_SIZE)
			: null;

		return new Dimension(width, height);
	}

	/** Sweeps a thin marker along the bar's length, position driven by how far through the current tick we are. */
	private void drawMetronomeMarker(Graphics2D graphics, int width, int height, Orientation orientation)
	{
		float fraction = Math.max(0f, Math.min(1f, getMetronomeFraction()));
		if (isMetronomeReversed())
		{
			fraction = 1f - fraction;
		}
		graphics.setColor(METRONOME_MARKER_COLOR);
		if (orientation == Orientation.VERTICAL)
		{
			int y = Math.round(fraction * (height - METRONOME_MARKER_THICKNESS));
			graphics.fillRect(0, y, width, METRONOME_MARKER_THICKNESS);
		}
		else
		{
			int x = Math.round(fraction * (width - METRONOME_MARKER_THICKNESS));
			graphics.fillRect(x, 0, METRONOME_MARKER_THICKNESS, height);
		}
	}

	/** Returns the icon's local (bar-relative) top-left position, or null if no icon was drawn. */
	private Point drawIconAndCounter(Graphics2D graphics, int width, int height, Orientation orientation, int current,
		Color iconHighlight, int capsuleArc)
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

		// The capsule's ends curve inward, so content starting right at the
		// bar's edge (Start/End justification) would have its corners cut
		// off by that curve rather than our own frame - floor the margin at
		// the curve's own radius so content always starts in the capsule's
		// straight section instead.
		int minPadding = Math.max(PADDING, capsuleArc / 2);
		int along = along(getJustification(), orientation == Orientation.VERTICAL ? height : width, clusterLength, minPadding);
		Point iconLocal = null;

		if (orientation == Orientation.VERTICAL)
		{
			int y = along;
			if (icon != null)
			{
				int iconX = (width - ICON_SIZE) / 2;
				if (iconHighlight != null)
				{
					graphics.setColor(iconHighlight);
					graphics.fillRect(iconX - ICON_GLOW_MARGIN, y - ICON_GLOW_MARGIN,
						ICON_SIZE + 2 * ICON_GLOW_MARGIN, ICON_SIZE + 2 * ICON_GLOW_MARGIN);
				}
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
				if (iconHighlight != null)
				{
					graphics.setColor(iconHighlight);
					graphics.fillRect(x - ICON_GLOW_MARGIN, iconY - ICON_GLOW_MARGIN,
						ICON_SIZE + 2 * ICON_GLOW_MARGIN, ICON_SIZE + 2 * ICON_GLOW_MARGIN);
				}
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

	private static int along(Justification justification, int totalLength, int clusterLength, int padding)
	{
		switch (justification)
		{
			case START:
				return padding;
			case END:
				return totalLength - clusterLength - padding;
			case CENTER:
			default:
				return (totalLength - clusterLength) / 2;
		}
	}
}
