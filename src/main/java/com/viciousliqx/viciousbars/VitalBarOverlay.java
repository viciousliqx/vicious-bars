package com.viciousliqx.viciousbars;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.geom.Area;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import net.runelite.api.Client;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetPositionMode;
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
 * Orb mode (HP/Prayer/Spec/Run) only ever affects whether the real vanilla
 * orb is shown/hidden ({@link #getOrbWidgetComponent()} /
 * {@link #syncOrbState()}) or repositioned ({@link #alignRealOrb(Point)}) -
 * bars have no click-through functionality. RuneLite's own Hub reviewers
 * confirmed real-time via Discord that replicating a real widget's action
 * from a plugin-drawn element - by any mechanism, not just
 * client.menuAction() - counts as "adding new menu entries which cause
 * actions to be sent to the server" and isn't allowed, after a full night's
 * investigation found no compliant way to do it (see vicious_bars_dev_talk.txt
 * for the full story). Repositioning the orb (ALIGNED mode) is a different
 * thing entirely - the orb is never given a new action, it's still the same
 * real orb doing the same real thing, just moved, the same technique the
 * Hub-accepted Orb Mover plugin already uses. Only supported for FIXED
 * placement for now - a FLOATING bar has no single screen position to park
 * the orb at.
 */
public abstract class VitalBarOverlay extends Overlay
{
	private static final int BACKGROUND_BASE_ALPHA = 150;
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
	// One full breathe (0 -> peak -> 0) per period - purely wall-clock driven,
	// so the Warning pulse needs no stored animation state and every bar's
	// pulse free-runs independently in sync with real time.
	private static final long WARNING_PULSE_PERIOD_MS = 1200;

	private final Client client;

	// Saved once per bar, the first time this bar's orb gets moved for
	// ALIGNED mode - same restore-on-the-way-out shape as Orb Mover's own
	// RestoreEntry, just scoped to a single orb instead of a list of them.
	private boolean orbPositionSaved;
	private int savedOrbX;
	private int savedOrbY;
	private int savedOrbXMode;
	private int savedOrbYMode;

	// This frame's real canvas-space orb target under ALIGNED mode, or null -
	// read by ViciousBarsPlugin to size the shared ORBS/MAP_CONTAINER
	// widgets large enough for alignRealOrb()'s own clamp to actually reach.
	private Point lastIconCanvasCenter;

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

	/** Pixel size (width and height) the icon is drawn at. */
	protected abstract int getIconSize();

	protected abstract boolean isShowCounter();

	protected abstract Justification getJustification();

	/** Nudges the icon/counter cluster along the bar by this many pixels - positive toward the far end, negative toward the near end. */
	protected abstract int getContentOffset();

	/** True to draw the counter before the icon (reversed stacking order) instead of the default icon-then-counter. */
	protected abstract boolean isReverseContent();

	protected abstract Orientation getOrientation();

	protected abstract int getThickness();

	protected abstract int getLength();

	/** Thickness in pixels of the stone frame drawn around the bar - shared across every bar. */
	protected abstract int getBorderWidth();

	/** 0-100. Scales both the background and the fill color's alpha. */
	protected abstract int getOpacity();

	/** VANILLA for bars with no orb (Warmth, Enemy HP). */
	protected abstract OrbMode getOrbMode();

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

	/** Extra amount to preview-fill beyond the current value - e.g. HP's heal-on-hover preview. 0 for bars without one. */
	protected int getPreviewAmount()
	{
		return 0;
	}

	/** True only while this bar's Warning pulse is turned on. No-op (always false) for bars that haven't opted in. */
	protected boolean isWarningEnabled()
	{
		return false;
	}

	/** 0-100. The pulse activates once current/max drops to or below this percentage. Only meaningful when {@link #isWarningEnabled()}. */
	protected int getWarningThreshold()
	{
		return 0;
	}

	/** Color the empty part of the vial pulses while the Warning threshold is active. */
	protected Color getWarningColor()
	{
		return Color.RED;
	}

	/** 0-100, the pulse's peak opacity - it breathes between 0 and this value over {@link #WARNING_PULSE_PERIOD_MS}, not a flat level like the bar's own Opacity. */
	protected int getWarningOpacity()
	{
		return 0;
	}

	/** How the counter text is formatted from the raw current/max values. Plain current value by default - e.g. Enemy HP overrides this for its fraction/percentage choice. */
	protected String formatCounter(int current, int max)
	{
		return Integer.toString(current);
	}

	/** The real orb's own container widget - hidden while Orb mode is Merged. -1 for bars with no orb. */
	protected int getOrbWidgetComponent()
	{
		return -1;
	}

	/** Unconditionally un-hides the real orb, regardless of Orb mode - called on plugin shutdown so we never leave a vanilla orb hidden behind us. No-op for bars with no orb. */
	void unhideRealOrb()
	{
		int component = getOrbWidgetComponent();
		if (component < 0)
		{
			return;
		}
		Widget orb = client.getWidget(component);
		if (orb != null)
		{
			orb.setHidden(false);
		}
	}

	/**
	 * Hides the real orb's whole container ({@link #getOrbWidgetComponent()})
	 * while this bar is rendering with Orb mode Merged; shown otherwise. No
	 * bar currently needs to override this - it's protected rather than
	 * private only so a future bar with different needs could. No-op for
	 * bars with no orb.
	 */
	protected void syncOrbState()
	{
		int component = getOrbWidgetComponent();
		if (component < 0)
		{
			return;
		}
		Widget orb = client.getWidget(component);
		if (orb != null)
		{
			orb.setHidden(shouldRender() && getOrbMode() == OrbMode.MERGED);
		}
	}

	/**
	 * Moves the real orb to sit centered on {@code iconCanvasCenter} (a real
	 * canvas-space point). Same real mechanism the Hub-accepted Orb Mover
	 * plugin uses: switch both position modes to absolute, write the target
	 * through {@code setOriginalX/Y}, then {@code revalidate()} - clamped to
	 * the orb's own parent bounds exactly like Orb Mover clamps, so this
	 * never asks the orb to move somewhere its parent container doesn't
	 * reach. The orb's original position is captured once, lazily, the
	 * first time it actually gets moved.
	 */
	private void alignRealOrb(Point iconCanvasCenter)
	{
		int component = getOrbWidgetComponent();
		if (component < 0)
		{
			return;
		}
		Widget orb = client.getWidget(component);
		if (orb == null)
		{
			return;
		}
		Widget parent = orb.getParent();
		if (parent == null)
		{
			return;
		}

		if (!orbPositionSaved)
		{
			savedOrbX = orb.getOriginalX();
			savedOrbY = orb.getOriginalY();
			savedOrbXMode = orb.getXPositionMode();
			savedOrbYMode = orb.getYPositionMode();
			orbPositionSaved = true;
		}

		if (orb.getXPositionMode() != WidgetPositionMode.ABSOLUTE_LEFT)
		{
			orb.setXPositionMode(WidgetPositionMode.ABSOLUTE_LEFT);
		}
		if (orb.getYPositionMode() != WidgetPositionMode.ABSOLUTE_TOP)
		{
			orb.setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP);
		}

		net.runelite.api.Point parentCanvas = parent.getCanvasLocation();
		int targetX = iconCanvasCenter.x - parentCanvas.getX() - orb.getWidth() / 2;
		int targetY = iconCanvasCenter.y - parentCanvas.getY() - orb.getHeight() / 2;

		int maxX = parent.getWidth() - orb.getWidth();
		int maxY = parent.getHeight() - orb.getHeight();
		if (maxX >= 0)
		{
			targetX = Math.max(0, Math.min(targetX, maxX));
		}
		if (maxY >= 0)
		{
			targetY = Math.max(0, Math.min(targetY, maxY));
		}

		if (orb.getOriginalX() != targetX || orb.getOriginalY() != targetY)
		{
			orb.setOriginalX(targetX);
			orb.setOriginalY(targetY);
			orb.revalidate();
		}
	}

	/**
	 * Restores the real orb to wherever it was before ALIGNED first moved
	 * it. Safe to call unconditionally every frame mode isn't ALIGNED - a
	 * no-op once nothing's saved. Also called unconditionally from
	 * {@link ViciousBarsPlugin#shutDown()} so a moved orb never gets left
	 * behind if the plugin stops.
	 */
	void restoreRealOrbPosition()
	{
		if (!orbPositionSaved)
		{
			return;
		}
		int component = getOrbWidgetComponent();
		if (component >= 0)
		{
			Widget orb = client.getWidget(component);
			if (orb != null)
			{
				orb.setXPositionMode(savedOrbXMode);
				orb.setYPositionMode(savedOrbYMode);
				orb.setOriginalX(savedOrbX);
				orb.setOriginalY(savedOrbY);
				orb.revalidate();
			}
		}
		orbPositionSaved = false;
	}

	/** This frame's real canvas-space orb target under ALIGNED mode, or null if not applicable this frame (mode isn't Aligned, bar isn't Docked, or isn't rendering). */
	Point getLastIconCanvasCenter()
	{
		return lastIconCanvasCenter;
	}

	/** The icon's local top-left anchor point, independent of whether an icon/counter are actually shown - a stable "icon slot" ALIGNED mode can target regardless of Show icon/Show counter. */
	private Point iconAnchor(int width, int height, Orientation orientation)
	{
		int iconSize = getIconSize();
		int along = along(getJustification(), orientation == Orientation.VERTICAL ? height : width, iconSize) + getContentOffset();
		return orientation == Orientation.VERTICAL
			? new Point((width - iconSize) / 2, along)
			: new Point(along, (height - iconSize) / 2);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		syncOrbState();

		if (!shouldRender())
		{
			restoreRealOrbPosition();
			return null;
		}

		PlacementMode placementMode = getPlacementMode();
		setMovable(placementMode == PlacementMode.FLOATING);
		if (placementMode == PlacementMode.FIXED)
		{
			// RuneLite's OverlayRenderer pre-translates the graphics we're
			// given by the overlay's stored preferred location (set by
			// dragging it while Floating) BEFORE calling render(). Clearing
			// preferredLocation alone isn't enough though - when it's null,
			// the renderer falls back to the overlay's own getBounds() x/y
			// instead, and that Rectangle gets permanently written with the
			// dragged position every frame while movable (the renderer does
			// it itself: overlay.getBounds().setLocation(point)), so it just
			// keeps perpetuating the same stale value forever otherwise.
			// Resetting both is what actually breaks the loop.
			setPreferredLocation(null);
			getBounds().setLocation(0, 0);
		}

		Orientation orientation;
		int length = getLength();
		// Only set for FIXED placement - the real canvas-space top-left this
		// bar is drawn at. ALIGNED mode needs this to know where on screen to
		// park the orb; a FLOATING bar has no equivalent single fixed point
		// (the overlay renderer translates the graphics instead of handing
		// us a coordinate), so ALIGNED is a no-op there for now.
		Point fixedPos = null;
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
				restoreRealOrbPosition();
				return null;
			}
			fixedPos = pos;
			// Non-movable DYNAMIC overlays aren't auto-translated by the overlay
			// renderer the way movable/dragged ones are (confirmed against Status
			// Bars' own source, which draws at absolute coordinates for exactly
			// this reason) - so we translate ourselves to reach the same effect.
			graphics.translate(pos.x, pos.y);
		}
		else
		{
			orientation = getOrientation();
			// No explicit translate needed here - the overlay renderer
			// already translated the graphics context to this bar's dragged
			// position before calling render() (that's how movable overlays
			// position themselves).
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

		// Icon + counter draw last, on top of the frame AND the vial, sized
		// against the bar's full outer footprint rather than the vial's
		// inset interior - a label pinned to the front of the whole
		// assembly, same as a real orb's icon sitting on its meter, not
		// squeezed inside the fill. Same returned Dimension either way, so
		// PanelDock and dock-slot stacking never see a difference.
		int max = Math.max(getMax(), 1);
		int current = Math.max(0, Math.min(getCurrent(), max));
		Color iconHighlight = resolveIconHighlight();
		drawIconAndCounter(graphics, width, height, orientation, current, max, iconHighlight);

		if (fixedPos != null && getOrbMode() == OrbMode.ALIGNED)
		{
			Point anchor = iconAnchor(width, height, orientation);
			int iconSize = getIconSize();
			lastIconCanvasCenter = new Point(fixedPos.x + anchor.x + iconSize / 2, fixedPos.y + anchor.y + iconSize / 2);
			alignRealOrb(lastIconCanvasCenter);
		}
		else
		{
			lastIconCanvasCenter = null;
			restoreRealOrbPosition();
		}

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

		// Warning pulse draws across the FULL vial, before anything else -
		// the preview and normal fill below both draw solid colors on top of
		// it up to their own extent, so the pulse only ever actually shows
		// through in whatever's left empty, with no separate clipping needed.
		if (isWarningEnabled() && isWarningActive(current, max))
		{
			Color warningColor = getWarningColor();
			float pulseAlpha = warningPulseAlpha(getWarningOpacity());
			vialGraphics.setColor(new Color(warningColor.getRed(), warningColor.getGreen(), warningColor.getBlue(),
				Math.round(255 * pulseAlpha * opacityFraction)));
			vialGraphics.fillRect(0, 0, innerWidth, innerHeight);
		}

		// Preview fill draws FIRST, to the full current+preview extent, in a
		// lightened shade - then the normal fill draws over it up to just
		// current, so the lightened shade only ever shows in the extra
		// region beyond what you actually have right now. 0 for every bar
		// except HP (hovering a food item), so this is a no-op everywhere else.
		int previewAmount = Math.max(0, getPreviewAmount());
		if (previewAmount > 0)
		{
			double previewRatio = (double) Math.min(max, current + previewAmount) / max;
			Color previewColor = lighten(fillColor, 0.55f);
			vialGraphics.setColor(new Color(previewColor.getRed(), previewColor.getGreen(), previewColor.getBlue(),
				Math.round(255 * opacityFraction)));
			if (orientation == Orientation.VERTICAL)
			{
				int previewHeight = (int) Math.round(previewRatio * innerHeight);
				vialGraphics.fillRect(0, innerHeight - previewHeight, innerWidth, previewHeight);
			}
			else
			{
				int previewWidth = (int) Math.round(previewRatio * innerWidth);
				vialGraphics.fillRect(0, 0, previewWidth, innerHeight);
			}
		}

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

	private boolean isWarningActive(int current, int max)
	{
		return max > 0 && (100.0 * current / max) <= getWarningThreshold();
	}

	/** Breathes 0 -> peak -> 0 once per {@link #WARNING_PULSE_PERIOD_MS}, driven purely by wall-clock time. */
	private float warningPulseAlpha(int peakOpacity)
	{
		double phase = (System.currentTimeMillis() % WARNING_PULSE_PERIOD_MS) / (double) WARNING_PULSE_PERIOD_MS;
		double pulse = (1 - Math.cos(phase * 2 * Math.PI)) / 2;
		return (float) (pulse * Math.max(0, Math.min(peakOpacity, 100)) / 100f);
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

	private void drawIconAndCounter(Graphics2D graphics, int width, int height, Orientation orientation, int current, int max,
		Color iconHighlight)
	{
		BufferedImage icon = isShowIcon() ? getIcon() : null;
		boolean showCounter = isShowCounter();

		if (icon == null && !showCounter)
		{
			return;
		}

		int iconSize = getIconSize();
		String counterText = showCounter ? formatCounter(current, max) : null;
		graphics.setFont(FontManager.getRunescapeSmallFont());
		int textWidth = counterText != null ? graphics.getFontMetrics().stringWidth(counterText) : 0;
		int textHeight = counterText != null ? graphics.getFontMetrics().getHeight() : 0;

		int clusterLength = icon != null ? iconSize : 0;
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
		boolean reverse = isReverseContent();

		if (orientation == Orientation.VERTICAL)
		{
			int y = along;
			if (reverse && counterText != null)
			{
				graphics.setColor(Color.WHITE);
				graphics.drawString(counterText, (width - textWidth) / 2, y + textHeight - graphics.getFontMetrics().getDescent());
				y += textHeight + PADDING;
			}
			if (icon != null)
			{
				int iconX = (width - iconSize) / 2;
				if (iconHighlight != null)
				{
					graphics.setColor(iconHighlight);
					graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
					graphics.fillOval(iconX - ICON_GLOW_MARGIN, y - ICON_GLOW_MARGIN,
						iconSize + 2 * ICON_GLOW_MARGIN, iconSize + 2 * ICON_GLOW_MARGIN);
				}
				graphics.drawImage(icon, iconX, y, iconSize, iconSize, null);
				y += iconSize + PADDING;
			}
			if (!reverse && counterText != null)
			{
				graphics.setColor(Color.WHITE);
				graphics.drawString(counterText, (width - textWidth) / 2, y + textHeight - graphics.getFontMetrics().getDescent());
			}
		}
		else
		{
			int x = along;
			if (reverse && counterText != null)
			{
				graphics.setColor(Color.WHITE);
				int textY = (height - textHeight) / 2 + textHeight - graphics.getFontMetrics().getDescent();
				graphics.drawString(counterText, x, textY);
				x += textWidth + PADDING;
			}
			if (icon != null)
			{
				int iconY = (height - iconSize) / 2;
				if (iconHighlight != null)
				{
					graphics.setColor(iconHighlight);
					graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
					graphics.fillOval(x - ICON_GLOW_MARGIN, iconY - ICON_GLOW_MARGIN,
						iconSize + 2 * ICON_GLOW_MARGIN, iconSize + 2 * ICON_GLOW_MARGIN);
				}
				graphics.drawImage(icon, x, iconY, iconSize, iconSize, null);
				x += iconSize + PADDING;
			}
			if (!reverse && counterText != null)
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

	private static Color lighten(Color color, float fraction)
	{
		int r = color.getRed() + Math.round((255 - color.getRed()) * fraction);
		int g = color.getGreen() + Math.round((255 - color.getGreen()) * fraction);
		int b = color.getBlue() + Math.round((255 - color.getBlue()) * fraction);
		return new Color(r, g, b, color.getAlpha());
	}
}
