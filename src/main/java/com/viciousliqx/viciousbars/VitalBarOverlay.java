package com.viciousliqx.viciousbars;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Area;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import net.runelite.api.Client;
import net.runelite.api.gameval.SpriteID;
import net.runelite.client.game.SpriteManager;
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
	// Real game sprites (net.runelite.api.gameval.SpriteID.Steelborder/
	// Steelborder2) - the actual riveted-metal corner pieces the modern
	// inventory panel itself uses, confirmed against RuneLite's own named
	// gameval constants, not a guessed id. Native size is 25x30 - sized for
	// the panel's own much thicker border, not our comparatively thin bars -
	// so these draw scaled down to a small accent rather than at native size.
	private static final int CORNER_ACCENT_WIDTH = 10;
	private static final int CORNER_ACCENT_HEIGHT = 12;

	private final Client client;
	private final SpriteManager spriteManager;
	// Written on the client/render thread, read from the AWT event thread by
	// OrbClickListener - needs volatile for cross-thread visibility, plain
	// field writes aren't guaranteed to ever become visible on another thread.
	private volatile Rectangle iconBounds;

	protected VitalBarOverlay(Client client, SpriteManager spriteManager)
	{
		this.client = client;
		this.spriteManager = spriteManager;
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

	/** Nudges the icon/counter cluster along the bar by this many pixels - positive toward the far end, negative toward the near end. */
	protected abstract int getContentOffset();

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

		// Clip everything to the bar's own rectangle - the one safety net
		// every phase below shares, so nothing we draw can ever bleed past
		// the bar's declared footprint.
		graphics.setClip(0, 0, width, height);

		int borderWidth = Math.max(0, getBorderWidth());
		int innerWidth = Math.max(1, width - 2 * borderWidth);
		int innerHeight = Math.max(1, height - 2 * borderWidth);
		int innerShortAxis = orientation == Orientation.VERTICAL ? innerWidth : innerHeight;
		int innerArc = Math.round(innerShortAxis * INNER_ARC_FRACTION);

		// Three phases, back to front: the stone frame, the vial inside it,
		// then the icon/counter on top of both. Each phase only needs to
		// know about the one below it - the frame doesn't know the vial's
		// Opacity, the vial doesn't know the icon exists at all.
		drawFrame(graphics, width, height, borderWidth, innerWidth, innerHeight, innerArc);
		drawVial(graphics, orientation, borderWidth, innerWidth, innerHeight, innerArc);
		// Corner accents draw AFTER the vial, not between frame and vial -
		// their 10x12 footprint reaches past the frame's own border width
		// into the vial's own area, so drawing them first just meant the
		// vial's background painted over the part that overlapped,
		// looking like the accent was sitting underneath the bar instead
		// of decorating its corner.
		drawCornerAccents(graphics, width, height);

		// Icon + counter draw last, on top of the frame AND the vial, sized
		// against the bar's full outer footprint rather than the vial's
		// inset interior - a label pinned to the front of the whole
		// assembly, same as a real orb's icon sitting on its meter, not
		// squeezed inside the fill. Same returned Dimension either way, so
		// PanelDock and dock-slot stacking never see a difference.
		int current = Math.max(0, Math.min(getCurrent(), Math.max(getMax(), 1)));
		Color iconHighlight = resolveIconHighlight();
		Point iconLocal = drawIconAndCounter(graphics, width, height, orientation, current, iconHighlight);
		iconBounds = iconLocal != null
			? new Rectangle(origin.x + iconLocal.x, origin.y + iconLocal.y, ICON_SIZE, ICON_SIZE)
			: null;

		return new Dimension(width, height);
	}

	private Color resolveIconHighlight()
	{
		if (isMetronomeEnabled() && isMetronomeFlashing())
		{
			return METRONOME_FLASH_COLOR;
		}
		if (isIconLit())
		{
			return ICON_LIT_COLOR;
		}
		return null;
	}

	/**
	 * The stone-toned ring between the bar's outer footprint and its inner
	 * vial. Only ever a RING, never a filled rect with the vial drawn over
	 * it - the vial's own area is never painted by this method at all, so
	 * once the vial fades (its Opacity drops) what shows through is the
	 * real game behind it, not our own stone color. Always fully opaque,
	 * independent of the bar's own Opacity.
	 */
	private void drawFrame(Graphics2D graphics, int width, int height, int borderWidth, int innerWidth, int innerHeight, int innerArc)
	{
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
		// have half its stroke fall outside our own clip and get cut off.
		graphics.setColor(FRAME_OUTLINE_COLOR);
		graphics.draw(new RoundRectangle2D.Float(0, 0, width - 1, height - 1, FRAME_ARC, FRAME_ARC));
		graphics.draw(new RoundRectangle2D.Float(borderWidth, borderWidth, innerWidth - 1, innerHeight - 1, innerArc, innerArc));
	}

	/**
	 * Decorative metal corner accents using the real game's own Steelborder
	 * sprites - the same pieces the modern inventory panel's border is built
	 * from. Purely additive on top of the stone frame, scaled well down from
	 * their native 25x30 size since that's proportioned for the panel's own
	 * much thicker border, not our bars.
	 */
	private void drawCornerAccents(Graphics2D graphics, int width, int height)
	{
		drawCornerAccent(graphics, SpriteID.Steelborder.TOP_LEFT, 0, 0);
		drawCornerAccent(graphics, SpriteID.Steelborder.TOP_RIGHT, width - CORNER_ACCENT_WIDTH, 0);
		drawCornerAccent(graphics, SpriteID.Steelborder.BOTTOM_LEFT, 0, height - CORNER_ACCENT_HEIGHT);
		drawCornerAccent(graphics, SpriteID.Steelborder.BOTTOM_RIGHT, width - CORNER_ACCENT_WIDTH, height - CORNER_ACCENT_HEIGHT);
	}

	private void drawCornerAccent(Graphics2D graphics, int spriteId, int x, int y)
	{
		BufferedImage sprite = spriteManager.getSprite(spriteId, 0);
		if (sprite != null)
		{
			graphics.drawImage(sprite, x, y, CORNER_ACCENT_WIDTH, CORNER_ACCENT_HEIGHT, null);
		}
	}

	/**
	 * Background + colored fill + metronome marker, confined to the inner
	 * capsule. The only phase that fades with the bar's own Opacity. Drawn
	 * on a scratch copy of the graphics context (translated + clipped to
	 * the capsule) so none of that state leaks back out to the caller -
	 * the icon/counter phase that follows needs the ORIGINAL untranslated,
	 * unclipped (beyond the bar's own footprint) context.
	 */
	private void drawVial(Graphics2D graphics, Orientation orientation, int borderWidth, int innerWidth, int innerHeight, int innerArc)
	{
		Graphics2D vialGraphics = (Graphics2D) graphics.create();
		vialGraphics.translate(borderWidth, borderWidth);
		vialGraphics.setClip(new RoundRectangle2D.Float(0, 0, innerWidth, innerHeight, innerArc, innerArc));

		float opacityFraction = Math.max(0, Math.min(getOpacity(), 100)) / 100f;

		vialGraphics.setColor(new Color(0, 0, 0, Math.round(BACKGROUND_BASE_ALPHA * opacityFraction)));
		vialGraphics.fillRect(0, 0, innerWidth, innerHeight);

		int max = Math.max(getMax(), 1);
		int current = Math.max(0, Math.min(getCurrent(), max));
		double ratio = (double) current / max;

		Color fillColor = getColor();
		vialGraphics.setColor(new Color(fillColor.getRed(), fillColor.getGreen(), fillColor.getBlue(),
			Math.round(255 * opacityFraction)));
		if (orientation == Orientation.VERTICAL)
		{
			int fillHeight = (int) Math.round(ratio * innerHeight);
			vialGraphics.fillRect(0, innerHeight - fillHeight, innerWidth, fillHeight);
		}
		else
		{
			int fillWidth = (int) Math.round(ratio * innerWidth);
			vialGraphics.fillRect(0, 0, fillWidth, innerHeight);
		}

		if (isMetronomeEnabled())
		{
			drawMetronomeMarker(vialGraphics, innerWidth, innerHeight, orientation);
		}

		vialGraphics.dispose();
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
		Color iconHighlight)
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

		// No capsule curve to clear anymore - the icon/counter draw against
		// the bar's full outer rectangle (plain corners from this content's
		// point of view, since the outer clip set in render() is a plain
		// rect, not the vial's rounded one), so a flat PADDING is all that's
		// needed here. Content offset is a manual nudge on top of that,
		// positive toward the far end - lets a specific bar's icon be
		// fine-tuned a few px without touching Content justification itself.
		int along = along(getJustification(), orientation == Orientation.VERTICAL ? height : width, clusterLength) + getContentOffset();
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
					graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
					graphics.fillOval(iconX - ICON_GLOW_MARGIN, y - ICON_GLOW_MARGIN,
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
					graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
					graphics.fillOval(x - ICON_GLOW_MARGIN, iconY - ICON_GLOW_MARGIN,
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
