package com.viciousliqx.viciousbars;

import com.compactorbs.CompactOrbsConfig;
import com.compactorbs.CompactOrbsConfig.ThresholdMode;
import com.compactorbs.CompactOrbsManager;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
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
 * Shared drawing + Docked/Floating placement logic for every auxiliary bar -
 * restored from v1's VitalBarOverlay (see git log before the full-reset
 * "gut pass"), with v1's orb-docking dropped entirely: aux bars never touch
 * a real widget at all (no repositioning, no alignment, no interactivity -
 * that's the main bars' job, see VialOverlay), they just draw their own
 * icon and fill at a free-floating or Docked position. Also drops v1's old
 * single-color/single-warning-pulse/metronome hooks in favor of the SAME
 * state/regen/drain/threshold contract VialOverlay uses, since aux bars want
 * full parity with the main bars' current feature set, not v1's older one.
 *
 * Subclasses whose bar type has a main-bar counterpart (HP/Prayer/Spec/Run)
 * read the exact same CompactOrbsManager fields their VialOverlay
 * counterpart does - no duplicated tick-tracking, only a different drawing
 * layer. Warmth/Enemy HP have no main-bar counterpart and compute their own
 * values directly.
 */
public abstract class AuxBarOverlay extends Overlay
{
	private static final int BACKGROUND_ALPHA = 150;
	private static final int PADDING = 3;
	// Same hand-picked stone-toned approximation as the main bars' frame -
	// see VialOverlay's own FRAME_FILL_COLOR/FRAME_OUTLINE_COLOR comment.
	private static final Color FRAME_FILL_COLOR = new Color(96, 82, 64);
	private static final Color FRAME_OUTLINE_COLOR = new Color(20, 17, 14);
	private static final int FRAME_ARC = 6;
	private static final float INNER_ARC_FRACTION = 0.75f;
	private static final int PULSE_PERIOD_MILLIS = 1200;
	private static final float PULSE_MIN_FRACTION = 0.3f;

	protected final Client client;
	protected final CompactOrbsManager manager;
	protected final CompactOrbsConfig config;

	protected AuxBarOverlay(Client client, CompactOrbsManager manager, CompactOrbsConfig config)
	{
		this.client = client;
		this.manager = manager;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
	}

	/** Per-bar enabled toggle, main/aux coupling, and (for Warmth/Enemy HP) their own dynamic visibility rule. */
	protected abstract boolean shouldRender();

	protected abstract PlacementMode getPlacementMode();

	protected abstract DockSide getDockSide();

	protected abstract int getDockSlot();

	protected abstract Justification getDockJustification();

	/** True to stretch this bar to the full length of the inventory panel instead of its own configured Length - only meaningful while Docked. */
	protected abstract boolean isClampToPanel();

	/** Only meaningful while Floating - Docked bars derive orientation from their DockSide instead. */
	protected abstract Orientation getFloatingOrientation();

	protected abstract int getThickness();

	protected abstract int getLength();

	protected abstract int getCurrent();

	protected abstract int getMax();

	protected abstract Color fillColor();

	protected boolean isShowIcon()
	{
		return true;
	}

	/** Null if disabled, unavailable, or not yet loaded. */
	protected BufferedImage getIcon()
	{
		return null;
	}

	protected abstract int getIconSize();

	protected abstract boolean isShowCounter();

	/** Plain current value by default - Enemy Hitpoints overrides this for its fraction/percentage choice. */
	protected String formatCounter(int current, int max)
	{
		return Integer.toString(current);
	}

	protected abstract Justification getContentJustification();

	protected abstract int getContentOffset();

	protected abstract boolean isReverseContent();

	/** 0-100. Scales both the background and the fill color's alpha. */
	protected abstract int getOpacity();

	/** Progress (0.0-1.0) toward this bar's next restoration tick, or null if nothing to show - same contract as VialOverlay#regenPercentage(). */
	protected Double regenPercentage()
	{
		return null;
	}

	protected boolean regenEnabled()
	{
		return false;
	}

	protected Color regenHighlightColor()
	{
		return fillColor().brighter();
	}

	/** Progress (1.0-0.0) until this bar's next expected drain tick, or null if nothing's draining - same contract as VialOverlay#drainPercentage(). */
	protected Double drainPercentage()
	{
		return null;
	}

	protected boolean drainEnabled()
	{
		return false;
	}

	protected Color drainHighlightColor()
	{
		return fillColor().brighter();
	}

	protected ThresholdMode thresholdMode()
	{
		return ThresholdMode.DISABLED;
	}

	protected int thresholdValue()
	{
		return 0;
	}

	protected Color thresholdColor()
	{
		return Color.WHITE;
	}

	/** True if the glow fires at-or-ABOVE thresholdValue(); false for at-or-BELOW - same contract as VialOverlay#thresholdAbove(). */
	protected boolean thresholdAbove()
	{
		return false;
	}

	private boolean isThresholdActive()
	{
		ThresholdMode mode = thresholdMode();
		if (mode == ThresholdMode.DISABLED)
		{
			return false;
		}
		if (mode == ThresholdMode.COMBAT_ONLY && !manager.inCombat)
		{
			return false;
		}

		int max = Math.max(getMax(), 1);
		int current = Math.max(0, Math.min(getCurrent(), max));
		int percentage = (int) Math.round(100.0 * current / max);

		return thresholdAbove() ? percentage >= thresholdValue() : percentage <= thresholdValue();
	}

	@Override
	public final Dimension render(Graphics2D graphics)
	{
		if (!shouldRender())
		{
			return null;
		}

		PlacementMode placementMode = getPlacementMode();
		setMovable(placementMode == PlacementMode.FLOATING);

		Orientation orientation;
		int length = getLength();

		if (placementMode == PlacementMode.DOCKED)
		{
			DockSide side = getDockSide();
			orientation = side.orientation();

			if (isClampToPanel())
			{
				int panelLength = PanelDock.panelLength(client, side);
				if (panelLength >= 0)
				{
					length = panelLength;
				}
			}

			Point pos = PanelDock.position(client, side, getDockSlot(), getThickness(), config.auxDockPadding(), config.auxDockOffset(),
				length, getDockJustification());
			if (pos == null)
			{
				// Panel widget not present (wrong layout, or not logged in yet).
				return null;
			}

			// Same reasoning as VialOverlay's own render() - a non-movable
			// DYNAMIC overlay still needs its stored bounds cleared each
			// frame, or RuneLite's OverlayRenderer re-applies a stale
			// location on top of our own translate.
			setPreferredLocation(null);
			getBounds().setLocation(0, 0);
			graphics.translate(pos.x, pos.y);
		}
		else
		{
			orientation = getFloatingOrientation();
			// No explicit translate needed - the overlay renderer already
			// translated the graphics context to this bar's dragged position
			// before calling render() (standard RuneLite movable-overlay
			// behavior, same as any draggable overlay).
		}

		int width = orientation == Orientation.VERTICAL ? getThickness() : length;
		int height = orientation == Orientation.VERTICAL ? length : getThickness();

		graphics.setClip(0, 0, width, height);

		int frameWidth = Math.max(0, config.auxFrameWidth());
		int innerWidth = Math.max(1, width - 2 * frameWidth);
		int innerHeight = Math.max(1, height - 2 * frameWidth);
		int innerShortAxis = orientation == Orientation.VERTICAL ? innerWidth : innerHeight;
		int innerArc = Math.round(innerShortAxis * INNER_ARC_FRACTION);

		drawFrame(graphics, width, height, frameWidth, innerWidth, innerHeight, innerArc);
		drawBarPulse(graphics, orientation, frameWidth, innerWidth, innerHeight, innerArc);
		drawVial(graphics, orientation, frameWidth, innerWidth, innerHeight, innerArc);
		drawIconAndCounter(graphics, width, height, orientation);

		return new Dimension(width, height);
	}

	/** The stone-toned ring between the bar's outer footprint and its inner vial - always fully opaque, independent of this bar's own Opacity (matches VialOverlay's drawFrame()). */
	private void drawFrame(Graphics2D graphics, int width, int height, int frameWidth, int innerWidth, int innerHeight, int innerArc)
	{
		RoundRectangle2D.Float outerShape = new RoundRectangle2D.Float(0, 0, width, height, FRAME_ARC, FRAME_ARC);
		RoundRectangle2D.Float innerShapeAtInset = new RoundRectangle2D.Float(
			frameWidth, frameWidth, innerWidth, innerHeight, innerArc, innerArc);

		Area frameRing = new Area(outerShape);
		frameRing.subtract(new Area(innerShapeAtInset));

		graphics.setColor(FRAME_FILL_COLOR);
		graphics.fill(frameRing);

		graphics.setColor(FRAME_OUTLINE_COLOR);
		graphics.setStroke(new BasicStroke(Math.max(1, config.auxFrameShadowWidth())));
		graphics.draw(new RoundRectangle2D.Float(0, 0, width - 1, height - 1, FRAME_ARC, FRAME_ARC));
		graphics.setStroke(new BasicStroke(1));
		graphics.draw(new RoundRectangle2D.Float(frameWidth, frameWidth, innerWidth - 1, innerHeight - 1, innerArc, innerArc));
	}

	/**
	 * Breathing threshold-glow pulse, drawn BEHIND the vial fill (same
	 * z-order decision as VialOverlay's own drawBarPulse(), confirmed live
	 * there to read better than painting over the fill) so it shines
	 * through the fill's own translucent layers instead of being masked.
	 */
	private void drawBarPulse(Graphics2D graphics, Orientation orientation, int frameWidth, int innerWidth, int innerHeight, int innerArc)
	{
		if (!isThresholdActive())
		{
			return;
		}

		double phase = (System.currentTimeMillis() % PULSE_PERIOD_MILLIS) / (double) PULSE_PERIOD_MILLIS;
		float pulseFraction = PULSE_MIN_FRACTION + (1f - PULSE_MIN_FRACTION) * (float) (0.5 + 0.5 * Math.sin(phase * 2 * Math.PI));

		Color base = thresholdColor();
		int alpha = Math.round(base.getAlpha() * pulseFraction);

		Graphics2D pulseGraphics = (Graphics2D) graphics.create();
		pulseGraphics.translate(frameWidth, frameWidth);
		pulseGraphics.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), alpha));
		pulseGraphics.fill(new RoundRectangle2D.Float(0, 0, innerWidth, innerHeight, innerArc, innerArc));
		pulseGraphics.dispose();
	}

	/** Background + colored current/max fill + regen/drain outlines, confined to the inner capsule. The only phase that fades with this bar's own Opacity. */
	private void drawVial(Graphics2D graphics, Orientation orientation, int frameWidth, int innerWidth, int innerHeight, int innerArc)
	{
		float opacityFraction = Math.max(0, Math.min(getOpacity(), 100)) / 100f;

		Graphics2D vialGraphics = (Graphics2D) graphics.create();
		vialGraphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacityFraction));
		vialGraphics.translate(frameWidth, frameWidth);
		vialGraphics.setClip(new RoundRectangle2D.Float(0, 0, innerWidth, innerHeight, innerArc, innerArc));

		vialGraphics.setColor(new Color(0, 0, 0, BACKGROUND_ALPHA));
		vialGraphics.fillRect(0, 0, innerWidth, innerHeight);

		int max = Math.max(getMax(), 1);
		int current = Math.max(0, Math.min(getCurrent(), max));
		double ratio = (double) current / max;

		boolean reverse = config.auxReverseBarFill();
		vialGraphics.setColor(fillColor());
		if (orientation == Orientation.VERTICAL)
		{
			int fillHeight = (int) Math.round(ratio * innerHeight);
			int fillY = reverse ? 0 : innerHeight - fillHeight;
			vialGraphics.fillRect(0, fillY, innerWidth, fillHeight);
		}
		else
		{
			int fillWidth = (int) Math.round(ratio * innerWidth);
			int fillX = reverse ? innerWidth - fillWidth : 0;
			vialGraphics.fillRect(fillX, 0, fillWidth, innerHeight);
		}

		vialGraphics.dispose();

		drawOutlines(graphics, orientation, frameWidth, innerWidth, innerHeight, innerArc);
	}

	/** Draws the regen and drain outlines in whichever order makes the dominant one land on top - config.auxRegenDrainPriority(), shared across every aux bar (own setting now, no longer reading the main bars' - now per-bar - setting). */
	private void drawOutlines(Graphics2D graphics, Orientation orientation, int frameWidth, int innerWidth, int innerHeight, int innerArc)
	{
		if (config.auxRegenDrainPriority() == CompactOrbsConfig.RegenDrainPriority.DRAIN)
		{
			drawRegenOutline(graphics, orientation, frameWidth, innerWidth, innerHeight, innerArc);
			drawDrainOutline(graphics, orientation, frameWidth, innerWidth, innerHeight, innerArc);
		}
		else
		{
			drawDrainOutline(graphics, orientation, frameWidth, innerWidth, innerHeight, innerArc);
			drawRegenOutline(graphics, orientation, frameWidth, innerWidth, innerHeight, innerArc);
		}
	}

	private void drawRegenOutline(Graphics2D graphics, Orientation orientation, int frameWidth, int innerWidth, int innerHeight, int innerArc)
	{
		if (!regenEnabled())
		{
			return;
		}
		Double percentage = regenPercentage();
		if (percentage == null)
		{
			return;
		}
		float opacityFraction = Math.min(100, Math.max(0, Math.min(getOpacity(), 100)) + 30) / 100f;
		drawOutline(graphics, orientation, frameWidth, innerWidth, innerHeight, innerArc, percentage, regenHighlightColor(), opacityFraction, config.auxReverseRegenRing());
	}

	private void drawDrainOutline(Graphics2D graphics, Orientation orientation, int frameWidth, int innerWidth, int innerHeight, int innerArc)
	{
		if (!drainEnabled())
		{
			return;
		}
		Double percentage = drainPercentage();
		if (percentage == null)
		{
			return;
		}
		float opacityFraction = Math.min(100, Math.max(0, Math.min(getOpacity(), 100)) + 50) / 100f;
		drawOutline(graphics, orientation, frameWidth, innerWidth, innerHeight, innerArc, percentage, drainHighlightColor(), opacityFraction, config.auxReverseDrainRing());
	}

	/** Shared clip-and-stroke primitive behind both drawRegenOutline() and drawDrainOutline() - generalized over Orientation rather than 4 fixed anchor directions, since aux bars can be either. */
	private void drawOutline(Graphics2D graphics, Orientation orientation, int frameWidth, int innerWidth, int innerHeight, int innerArc,
		double percentage, Color color, float opacityFraction, boolean reverse)
	{
		Graphics2D outlineGraphics = (Graphics2D) graphics.create();
		outlineGraphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacityFraction));
		outlineGraphics.translate(frameWidth, frameWidth);
		outlineGraphics.setColor(color);
		outlineGraphics.setStroke(new BasicStroke(Math.max(1, config.auxRegenOutlineWidth())));

		if (orientation == Orientation.VERTICAL)
		{
			int growthHeight = (int) Math.round(percentage * innerHeight);
			int growthY = reverse ? 0 : innerHeight - growthHeight;
			outlineGraphics.clip(new Rectangle(0, growthY, innerWidth, growthHeight));
		}
		else
		{
			int growthLength = (int) Math.round(percentage * innerWidth);
			int growthX = reverse ? innerWidth - growthLength : 0;
			outlineGraphics.clip(new Rectangle(growthX, 0, growthLength, innerHeight));
		}

		outlineGraphics.draw(new RoundRectangle2D.Float(0, 0, innerWidth - 1, innerHeight - 1, innerArc, innerArc));
		outlineGraphics.dispose();
	}

	private void drawIconAndCounter(Graphics2D graphics, int width, int height, Orientation orientation)
	{
		BufferedImage icon = isShowIcon() ? getIcon() : null;
		boolean showCounter = isShowCounter();
		if (icon == null && !showCounter)
		{
			return;
		}

		int max = Math.max(getMax(), 1);
		int current = Math.max(0, Math.min(getCurrent(), max));

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

		int along = along(getContentJustification(), orientation == Orientation.VERTICAL ? height : width, clusterLength) + getContentOffset();
		boolean reverseContent = isReverseContent();

		if (orientation == Orientation.VERTICAL)
		{
			int y = along;
			if (reverseContent && counterText != null)
			{
				graphics.setColor(Color.WHITE);
				graphics.drawString(counterText, (width - textWidth) / 2, y + textHeight - graphics.getFontMetrics().getDescent());
				y += textHeight + PADDING;
			}
			if (icon != null)
			{
				int iconX = (width - iconSize) / 2;
				graphics.drawImage(icon, iconX, y, iconSize, iconSize, null);
				y += iconSize + PADDING;
			}
			if (!reverseContent && counterText != null)
			{
				graphics.setColor(Color.WHITE);
				graphics.drawString(counterText, (width - textWidth) / 2, y + textHeight - graphics.getFontMetrics().getDescent());
			}
		}
		else
		{
			int x = along;
			if (reverseContent && counterText != null)
			{
				graphics.setColor(Color.WHITE);
				int textY = (height - textHeight) / 2 + textHeight - graphics.getFontMetrics().getDescent();
				graphics.drawString(counterText, x, textY);
				x += textWidth + PADDING;
			}
			if (icon != null)
			{
				int iconY = (height - iconSize) / 2;
				graphics.drawImage(icon, x, iconY, iconSize, iconSize, null);
				x += iconSize + PADDING;
			}
			if (!reverseContent && counterText != null)
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
