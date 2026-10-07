package com.viciousliqx.viciousbars;

import com.compactorbs.CompactOrbsConfig;
import com.compactorbs.CompactOrbsManager;
import com.compactorbs.widget.TargetWidget;
import com.compactorbs.widget.WidgetManager;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetPositionMode;
import net.runelite.api.widgets.WidgetType;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Shared logic for every vial bar (HP first, Prayer confirmed to share
 * identical native widget geometry via F8's dump - same backing sprite
 * 1071, same icon/number positions - so this base class carries all of it;
 * each subclass only supplies which orb, which skill, and what color).
 * See HpVialOverlay's own (now-historical) javadoc in git history for the
 * full account of how this design was arrived at empirically: the stone
 * frame + vial look, the collar (the orb's rotated backing sprite, its
 * round body sitting entirely above the bar's top edge while its tab hangs
 * down as a connecting "handle"), and the real border-kit art
 * (InterfaceID.ToplevelPreEoc.SIDE_CONTAINER, found via ViciousBarsPlugin's
 * temporary F9 dump after Steelborder and a hand-drawn bevel both turned
 * out wrong).
 *
 * Positioning only ever READS the orb's live canvas location - never
 * writes to it. That's deliberate: it's the same parent-derived,
 * never-self-referential read pattern confirmed safe earlier this
 * project (see compact_orbs_walkthrough.md), and it means the bar just
 * follows wherever Compact Orbs' own layout/edit-mode logic has already
 * put the orb, with zero positioning conflict between the two systems.
 */
abstract class VialOverlay extends Overlay
{
	private static final int FRAME_ARC = 6;
	private static final float INNER_ARC_FRACTION = 0.75f;
	private static final int BACKGROUND_ALPHA = 150;

	// Dialed in live via temporary config sliders on the HP bar first
	// (barNumberHorizontalOffset/barNumberVerticalOffset), settled at 4/4,
	// then hardcoded and the config removed - confirmed via F8 that
	// Prayer's native number/icon geometry is pixel-identical to HP's, so
	// these carry over unchanged rather than needing their own dial-in.
	private static final int NUMBER_HORIZONTAL_OFFSET = 4;
	private static final int NUMBER_VERTICAL_OFFSET = 4;

	// Dialed in alongside the above: left padding bumped up and right
	// padding trimmed down by the same amount, shifting the whole
	// icon+number content cluster right to actually center it in the
	// (now content-hugging) container instead of leaving it left-heavy.
	private static final int CONTENT_LEFT_PADDING = 4;
	private static final int CONTENT_RIGHT_PADDING = 0;

	// Small manual correction on top of the sprite-measured centering
	// (rotatedBackingBodyRadius) - confirmed live as still "1-2px off"
	// after that fix, nudging the collar (and, to stay consistent, the
	// bar's top edge right below it) down by this much.
	private static final int COLLAR_VERTICAL_NUDGE = 1;

	// Fallback-only now - used when the real border kit below isn't loaded
	// yet. SpriteID.Steelborder (310-313) was tried first and confirmed to
	// be the wrong asset (git history, VitalBarOverlay); a hand-drawn
	// bevel gradient was tried after and looked worse than this flat fill.
	private static final Color FRAME_FILL_COLOR = new Color(96, 82, 64);
	private static final Color FRAME_OUTLINE_COLOR = new Color(20, 17, 14);

	private static final int BORDER_TOP_LEFT = 824;
	private static final int BORDER_TOP_RIGHT = 825;
	private static final int BORDER_BOTTOM_LEFT = 826;
	private static final int BORDER_BOTTOM_RIGHT = 827;
	private static final int BORDER_TOP = 820;
	private static final int BORDER_BOTTOM = 822;
	private static final int BORDER_LEFT = 821;
	private static final int BORDER_RIGHT = 823;

	protected final Client client;
	protected final CompactOrbsManager manager;
	protected final CompactOrbsConfig config;
	protected final WidgetManager widgetManager;
	private final SpriteManager spriteManager;

	// Confirmed live (logged each piece's WidgetType): the backing/tab
	// piece is WidgetType.GRAPHIC with modelId -1, not MODEL - so
	// Widget#setRotationX/Y/Z (which only the client's MODEL rendering
	// path consumes) would be a silent no-op on it. Rotating it for real
	// means drawing our own rotated copy of its sprite and hiding the
	// real one - which is also why the icon never needs touching: it's a
	// separate real widget we just never hide or draw over, so it stays
	// upright for free instead of needing an explicit counter-rotation.
	private BufferedImage rotatedBacking;
	private int rotatedBackingSpriteId = -1;
	private int rotatedBackingBodyRadius;

	// Cached once per sprite id - these are static decorative art, never
	// change for the life of the client.
	private final Map<Integer, BufferedImage> borderSprites = new HashMap<>();

	// The level number isn't one of the orb container's named sub-ids
	// (backing/indicator/button/icon/empty) - confirmed via F8's widget
	// dump it's a plain untracked TEXT child, sitting to the LEFT of the
	// icon by default (the horizontal "default" orb layout, not ours).
	// Saved once per widget instance, same instance-identity reasoning as
	// rotatedBackingSpriteId - minimizing swaps interfaces entirely (new
	// Widget objects), so re-capturing on a mismatch avoids ever applying
	// a stale saved position to the wrong instance.
	private Widget numberWidget;
	private int numberOriginalX;
	private int numberOriginalY;
	private int numberOriginalXMode;
	private int numberOriginalYMode;

	// The container itself (34px tall natively) clips its children to its
	// own bounds - confirmed live (screenshot): the icon's bottom edge
	// already sits at y=30, leaving only ~4px before the repositioned
	// number (13px tall) gets cut off. Grown just enough to fit it, same
	// save/restore-on-toggle-off treatment as the number's position -
	// saved against the CONTAINER's own instance, not the number's, since
	// that's whose height is actually being changed.
	private Widget containerWidget;
	private int containerOriginalHeight;
	private int containerOriginalWidth;

	// The icon isn't drawn alone - F8's dump showed several sibling
	// graphics (indicator, the "empty"-fill wrapper, a hover-state
	// wrapper) all stacked at the SAME x as the icon, presumably swapped
	// in/out by the game depending on orb state. All of them - whichever
	// happens to be visible at the moment - need to move together, or
	// whichever one the game shows next would still be sitting at the old
	// x. Captured once (same original-x snapshot, before any edits) the
	// first time this container instance is seen.
	private Widget[] contentWidgets;
	private int[] contentOriginalX;

	protected VialOverlay(Client client, CompactOrbsManager manager, CompactOrbsConfig config, WidgetManager widgetManager, SpriteManager spriteManager)
	{
		this.client = client;
		this.manager = manager;
		this.config = config;
		this.widgetManager = widgetManager;
		this.spriteManager = spriteManager;
		setPosition(OverlayPosition.DYNAMIC);
		setMovable(false);
	}

	/** The real orb this bar docks to. */
	protected abstract TargetWidget orbContainer();

	/** 0-max current value (HP, prayer points, etc.) for the vial fill. */
	protected abstract int currentValue();

	/** Max value for the vial fill - must be >= 1. */
	protected abstract int maxValue();

	/** The vial's fill color - the one thing that visually distinguishes each bar. */
	protected abstract Color fillColor();

	@Override
	public final Dimension render(Graphics2D graphics)
	{
		if (!manager.isCompactLayout())
		{
			restoreBacking();
			restoreNumber();
			return null;
		}

		Widget orb = widgetManager.getTargetWidget(orbContainer());
		if (orb == null)
		{
			restoreBacking();
			restoreNumber();
			return null;
		}

		int width = config.barWidth();
		int height = config.barLength();

		Point orbCanvas = orb.getCanvasLocation();
		int orbCenterX = orbCanvas.getX() + orb.getWidth() / 2;
		int orbCenterY = orbCanvas.getY() + orb.getHeight() / 2;

		// Centered on the ICON's own canvas center, not the container's -
		// the icon is the thing the collar needs to fit snugly around
		// ("the icon is its identifier"), and there's no guarantee it sits
		// exactly at the container's center (padding, native layout quirks).
		Widget icon = client.getWidget(orbContainer().getIconId());

		if (icon != null)
		{
			repositionNumber(orb, icon);
		}

		// The bar is centered on that SAME icon-center point too, not just
		// the collar - the collar's drawn width is purely the round body's
		// diameter (the tab only extends it vertically, post-rotation), so
		// this gives an equal, symmetric overhang on both sides by
		// construction ("the rounded orb extending slightly past the
		// bar's width" - the collar's shoulders peeking out evenly left
		// and right, not an exact edge match). Falls back to the orb
		// container's own center if the icon isn't resolvable this frame.
		Point iconCanvas = icon != null ? icon.getCanvasLocation() : null;
		int iconCenterX = iconCanvas != null ? iconCanvas.getX() + icon.getWidth() / 2 : orbCenterX;
		int iconCenterY = iconCanvas != null ? iconCanvas.getY() + icon.getHeight() / 2 : orbCenterY;

		int barX = iconCenterX - width / 2 + config.barHorizontalOffset();
		// The bar's top starts exactly where the collar's round BODY ends
		// (iconCenterY + its radius), not where the collar's center is -
		// the body sits entirely above the bar, while the tab (the
		// "handle" hanging below the body) overlaps down into the bar's
		// top. +COLLAR_VERTICAL_NUDGE to stay flush with the same nudge
		// applied to the collar's own draw position.
		int barY = iconCenterY + rotatedBackingBodyRadius + COLLAR_VERTICAL_NUDGE + config.barVerticalOffset();

		// A separate, never-translated copy of the graphics context - the
		// collar is drawn through it AFTER the frame/vial below (see the
		// call near the end of this method), in absolute canvas space, so
		// it paints over the bar rather than getting covered by it. Only
		// the tab actually overlaps the bar at all (the body sits fully
		// above barY), but the whole collar stays unobstructed either way.
		Graphics2D absoluteGraphics = (Graphics2D) graphics.create();

		// See old VitalBarOverlay's comment on this exact pair of calls
		// (git history) - a non-movable DYNAMIC overlay still needs its
		// stored bounds cleared each frame, or RuneLite's OverlayRenderer
		// re-applies a stale location on top of our own translate.
		setPreferredLocation(null);
		getBounds().setLocation(0, 0);
		graphics.translate(barX, barY);

		int max = Math.max(maxValue(), 1);
		int current = Math.max(0, Math.min(currentValue(), max));

		int frameWidth = config.barFrameWidth();
		int innerWidth = width - 2 * frameWidth;
		int innerHeight = height - 2 * frameWidth;
		int innerArc = Math.round(innerWidth * INNER_ARC_FRACTION);

		drawFrame(graphics, width, height, frameWidth, innerWidth, innerHeight, innerArc);
		drawVial(graphics, frameWidth, innerWidth, innerHeight, innerArc, current, max);

		drawRotatedBacking(absoluteGraphics, icon, client.getWidget(orbContainer().getBackingId()), iconCenterX, iconCenterY);
		absoluteGraphics.dispose();

		return new Dimension(width, height);
	}

	/**
	 * The frame ring between the bar's outer footprint and its inner vial.
	 * The vintage flat/rounded frame is ALWAYS drawn first - its rounded
	 * capsule ends are the whole point of keeping it, the border kit's
	 * pieces are flat-rectangular and have no equivalent - then the real
	 * border-kit art (corners + edges, each stretched to fit - the kit is
	 * sized for the full inventory panel, far bigger than our bar, so
	 * stretching rather than tiling avoids visible seams at this scale) is
	 * drawn on top of it wherever loaded, bridging the vintage frame's
	 * rounded vial-end look into something that reads as part of the same
	 * rectangular GUI the rest of the game's panels use.
	 */
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
		graphics.draw(new RoundRectangle2D.Float(0, 0, width - 1, height - 1, FRAME_ARC, FRAME_ARC));
		graphics.draw(new RoundRectangle2D.Float(frameWidth, frameWidth, innerWidth - 1, innerHeight - 1, innerArc, innerArc));

		drawRealBorderFrame(graphics, width, height, frameWidth);
	}

	/** Draws whichever border-kit pieces are loaded on top of the vintage frame already drawn - a no-op per piece still missing from the sprite cache. */
	private void drawRealBorderFrame(Graphics2D graphics, int width, int height, int frameWidth)
	{
		BufferedImage topLeft = getBorderSprite(BORDER_TOP_LEFT);
		BufferedImage topRight = getBorderSprite(BORDER_TOP_RIGHT);
		BufferedImage bottomLeft = getBorderSprite(BORDER_BOTTOM_LEFT);
		BufferedImage bottomRight = getBorderSprite(BORDER_BOTTOM_RIGHT);
		BufferedImage top = getBorderSprite(BORDER_TOP);
		BufferedImage bottom = getBorderSprite(BORDER_BOTTOM);
		BufferedImage left = getBorderSprite(BORDER_LEFT);
		BufferedImage right = getBorderSprite(BORDER_RIGHT);

		if (topLeft == null || topRight == null || bottomLeft == null || bottomRight == null
			|| top == null || bottom == null || left == null || right == null)
		{
			return;
		}

		// frameWidth doubles as the corner's own scale control, rather than
		// adding a second slider - corners can't exceed half the bar's own
		// footprint either way, or opposite corners would overlap.
		int cornerSize = Math.min(frameWidth * 5, Math.min(width, height) / 2);
		int edgeSpanX = Math.max(0, width - 2 * cornerSize);
		int edgeSpanY = Math.max(0, height - 2 * cornerSize);

		// This kit is sized for the full inventory panel (its edges are
		// 140/211px long natively) squeezed down to a bar a fraction of
		// that size, in both directions at once - default nearest-neighbor
		// scaling makes that compression look rough. Bilinear smooths it.
		graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

		graphics.drawImage(topLeft, 0, 0, cornerSize, cornerSize, null);
		graphics.drawImage(topRight, width - cornerSize, 0, cornerSize, cornerSize, null);
		graphics.drawImage(bottomLeft, 0, height - cornerSize, cornerSize, cornerSize, null);
		graphics.drawImage(bottomRight, width - cornerSize, height - cornerSize, cornerSize, cornerSize, null);

		graphics.drawImage(top, cornerSize, 0, edgeSpanX, frameWidth, null);
		graphics.drawImage(bottom, cornerSize, height - frameWidth, edgeSpanX, frameWidth, null);
		graphics.drawImage(left, 0, cornerSize, frameWidth, edgeSpanY, null);
		graphics.drawImage(right, width - frameWidth, cornerSize, frameWidth, edgeSpanY, null);
	}

	private BufferedImage getBorderSprite(int spriteId)
	{
		return borderSprites.computeIfAbsent(spriteId, id -> spriteManager.getSprite(id, 0));
	}

	/**
	 * Hides the real backing (stone orb + tab) widget and draws a 90°
	 * counter-clockwise-rotated copy of its own sprite in its place, so the
	 * tab that normally protrudes left now points down - acting as a
	 * "collar" sitting ABOVE the bar (see barY in render()), with only its
	 * tab/handle actually overlapping the bar's top. Called through a
	 * separate, never-translated copy of the graphics context AFTER the
	 * frame/vial are drawn (see render()), so the whole collar - tab
	 * included - stays fully visible, painted on top rather than under.
	 * Everything else on the real orb (icon, button hitbox, indicator) is
	 * left completely alone - in particular the icon, which is a separate
	 * real widget, so it stays upright with no counter-rotation needed,
	 * and stays real/clickable with whatever menu entries it already has -
	 * nothing new added here.
	 *
	 * Positioned on the ICON's own canvas center (not the container's,
	 * and not backing.getCanvasLocation()) - the icon is the thing the
	 * collar needs to fit snugly around ("the icon is its identifier"),
	 * and there's no guarantee it sits exactly at the container's center
	 * (padding, native layout quirks). backing.getCanvasLocation() was
	 * tried first and confirmed live (screenshot) to drift from the
	 * container under drag - the container moves correctly (the bar
	 * tracks it fine), but backing's own canvas location doesn't follow
	 * along, so trusting it left the rotated image stranded back at the
	 * cluster's original spot. backing is only ever used here for its
	 * sprite art, never its own position.
	 */
	private void drawRotatedBacking(Graphics2D graphics, Widget icon, Widget backing, int iconCenterX, int iconCenterY)
	{
		if (icon == null || backing == null)
		{
			return;
		}

		BufferedImage rotated = getRotatedBacking(backing.getSpriteId());
		if (rotated == null)
		{
			return;
		}

		widgetManager.setHidden(orbContainer().getBackingId(), true);

		// X uses the rotated image's own width/2 - the round body fills
		// that dimension edge-to-edge with no margin (it maps from the
		// source's height, which the body also fills exactly), so the
		// bounding box's horizontal center and the body's true center
		// coincide. Y can't use the same trick: the tab's extra length
		// only exists along this axis post-rotation, so the body's true
		// vertical center is offset from the bounding box's by half that
		// extra length - rotatedBackingBodyRadius (half the SOURCE height,
		// measured directly off the sprite: 57x34, body fills the full
		// 34) already accounts for this, where rotated.getHeight()/2
		// (half of 57) doesn't.
		int drawX = iconCenterX - rotated.getWidth() / 2;
		int drawY = iconCenterY - rotatedBackingBodyRadius + COLLAR_VERTICAL_NUDGE;
		graphics.drawImage(rotated, drawX, drawY, null);
	}

	/** Restores the real backing widget's own visibility - safe to call unconditionally any time we're not drawing a replacement for it. */
	private void restoreBacking()
	{
		widgetManager.setHidden(orbContainer().getBackingId(), false);
	}

	void shutDown()
	{
		restoreBacking();
		restoreNumber();
	}

	/**
	 * Moves the real level number from its default spot (to the LEFT of
	 * the icon, the horizontal "default" orb layout - see F8's widget dump)
	 * to sit centered below the icon instead, matching our vertical bar.
	 * Position-only, same as everything else this overlay touches - the
	 * number is still the same real, fully functional widget, just
	 * relocated, never hidden or given new behavior.
	 */
	private void repositionNumber(Widget container, Widget icon)
	{
		Widget number = findNumberWidget(container);
		if (number == null)
		{
			return;
		}

		if (numberWidget != number)
		{
			numberOriginalX = number.getOriginalX();
			numberOriginalY = number.getOriginalY();
			numberOriginalXMode = number.getXPositionMode();
			numberOriginalYMode = number.getYPositionMode();
			numberWidget = number;
		}

		shiftContentLeft(container, icon);

		if (number.getXPositionMode() != WidgetPositionMode.ABSOLUTE_LEFT)
		{
			number.setXPositionMode(WidgetPositionMode.ABSOLUTE_LEFT);
		}
		if (number.getYPositionMode() != WidgetPositionMode.ABSOLUTE_TOP)
		{
			number.setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP);
		}

		int targetX = icon.getRelativeX() + icon.getWidth() / 2 - number.getWidth() / 2 + NUMBER_HORIZONTAL_OFFSET;
		int targetY = icon.getRelativeY() + icon.getHeight() + 2 + NUMBER_VERTICAL_OFFSET;

		if (number.getOriginalX() != targetX || number.getOriginalY() != targetY)
		{
			number.setOriginalX(targetX);
			number.setOriginalY(targetY);
			number.revalidate();
		}

		// Height: grown just enough that the repositioned number (now
		// sitting past the container's native bottom edge) isn't clipped.
		// Width: trimmed to just past whichever sits further right, the
		// icon or the (number-offset-shiftable) number itself - now that
		// shiftContentLeft() above has already pulled the icon (and its
		// layered siblings) over near x=0, icon.getRelativeX() here already
		// reflects the new, narrower position.
		int iconRightEdge = icon.getRelativeX() + icon.getWidth();
		int numberRightEdge = targetX + number.getWidth();
		resizeContainer(container, Math.max(iconRightEdge, numberRightEdge) + CONTENT_RIGHT_PADDING, targetY + number.getHeight() + 2);
	}

	/**
	 * Shifts the icon and every sibling sharing its original x (the
	 * layered indicator/empty-fill/hover graphics - see field comment)
	 * over to a small left padding, closing up the dead space the number
	 * used to occupy beside them. Captured and shifted as one group so
	 * whichever sibling the game happens to be showing right now still
	 * lines up with the others.
	 */
	private void shiftContentLeft(Widget container, Widget icon)
	{
		if (contentWidgets == null || containerWidget != container)
		{
			int originalIconX = icon.getOriginalX();
			List<Widget> matches = new ArrayList<>();
			for (Widget child : container.getStaticChildren())
			{
				if (child != null && child != numberWidget && child.getOriginalX() == originalIconX)
				{
					matches.add(child);
				}
			}
			contentWidgets = matches.toArray(new Widget[0]);
			contentOriginalX = new int[contentWidgets.length];
			for (int i = 0; i < contentWidgets.length; i++)
			{
				contentOriginalX[i] = contentWidgets[i].getOriginalX();
			}
		}

		int targetX = CONTENT_LEFT_PADDING;
		for (Widget content : contentWidgets)
		{
			if (content.getOriginalX() != targetX)
			{
				content.setOriginalX(targetX);
				content.revalidate();
			}
		}
	}

	private void resizeContainer(Widget container, int requiredWidth, int requiredHeight)
	{
		if (containerWidget != container)
		{
			containerOriginalWidth = container.getOriginalWidth();
			containerOriginalHeight = container.getOriginalHeight();
			containerWidget = container;
		}

		int targetWidth = Math.min(containerOriginalWidth, Math.max(1, requiredWidth));
		int targetHeight = Math.max(containerOriginalHeight, requiredHeight);
		boolean changed = false;

		if (container.getOriginalWidth() != targetWidth)
		{
			container.setOriginalWidth(targetWidth);
			changed = true;
		}
		if (container.getOriginalHeight() != targetHeight)
		{
			container.setOriginalHeight(targetHeight);
			changed = true;
		}
		if (changed)
		{
			container.revalidate();
		}
	}

	/** Safe to call unconditionally any time we're not actively repositioning the number - a no-op once nothing's saved. */
	private void restoreNumber()
	{
		if (containerWidget != null)
		{
			containerWidget.setOriginalWidth(containerOriginalWidth);
			containerWidget.setOriginalHeight(containerOriginalHeight);
			containerWidget.revalidate();
			containerWidget = null;
		}

		if (contentWidgets != null)
		{
			for (int i = 0; i < contentWidgets.length; i++)
			{
				contentWidgets[i].setOriginalX(contentOriginalX[i]);
				contentWidgets[i].revalidate();
			}
			contentWidgets = null;
			contentOriginalX = null;
		}

		if (numberWidget == null)
		{
			return;
		}

		numberWidget.setXPositionMode(numberOriginalXMode);
		numberWidget.setYPositionMode(numberOriginalYMode);
		numberWidget.setOriginalX(numberOriginalX);
		numberWidget.setOriginalY(numberOriginalY);
		numberWidget.revalidate();
		numberWidget = null;
	}

	/** The orb container's only TEXT-type child - confirmed via F8's dump there's exactly one (the level number), nothing else in this tree is WidgetType.TEXT. */
	private static Widget findNumberWidget(Widget container)
	{
		Widget[] children = container.getStaticChildren();
		if (children == null)
		{
			return null;
		}
		for (Widget child : children)
		{
			if (child != null && child.getType() == WidgetType.TEXT)
			{
				return child;
			}
		}
		return null;
	}

	private BufferedImage getRotatedBacking(int spriteId)
	{
		if (rotatedBacking != null && rotatedBackingSpriteId == spriteId)
		{
			return rotatedBacking;
		}

		BufferedImage source = spriteManager.getSprite(spriteId, 0);
		if (source == null)
		{
			return null;
		}

		rotatedBacking = rotateCounterClockwise90(source);
		rotatedBackingBodyRadius = source.getHeight() / 2;
		rotatedBackingSpriteId = spriteId;
		return rotatedBacking;
	}

	/**
	 * Rotates an image 90° counter-clockwise (as it actually appears on
	 * screen). Derived directly from the pixel mapping (x,y) -> (y, w-1-x)
	 * for a source of width w - verified against the real goal: a pixel on
	 * the source's LEFT edge (small x) lands near the BOTTOM of the result
	 * (y' = w-1-x, large), matching "tab pointing left" -> "tab pointing
	 * down". Java2D's rotate(theta) is clockwise on screen for positive
	 * theta (screen y points down, which flips the usual math convention),
	 * so the AffineTransform below is built from that mapping directly
	 * rather than via rotate(), to avoid getting the sign backwards.
	 */
	private static BufferedImage rotateCounterClockwise90(BufferedImage source)
	{
		int w = source.getWidth();
		int h = source.getHeight();

		BufferedImage rotated = new BufferedImage(h, w, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = rotated.createGraphics();
		g.drawImage(source, new AffineTransform(0, -1, 1, 0, 0, w - 1), null);
		g.dispose();

		return rotated;
	}

	/** Background + colored current/max fill, confined to the inner capsule. */
	private void drawVial(Graphics2D graphics, int frameWidth, int innerWidth, int innerHeight, int innerArc, int current, int max)
	{
		Graphics2D vialGraphics = (Graphics2D) graphics.create();
		vialGraphics.translate(frameWidth, frameWidth);
		vialGraphics.setClip(new RoundRectangle2D.Float(0, 0, innerWidth, innerHeight, innerArc, innerArc));

		vialGraphics.setColor(new Color(0, 0, 0, BACKGROUND_ALPHA));
		vialGraphics.fillRect(0, 0, innerWidth, innerHeight);

		double ratio = (double) current / max;
		int fillHeight = (int) Math.round(ratio * innerHeight);
		vialGraphics.setColor(fillColor());
		vialGraphics.fillRect(0, innerHeight - fillHeight, innerWidth, fillHeight);

		vialGraphics.dispose();
	}
}
