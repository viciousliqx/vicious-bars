package com.viciousliqx.viciousbars;

import com.compactorbs.CompactOrbsConfig;
import com.compactorbs.CompactOrbsConfig.ThresholdMode;
import com.compactorbs.CompactOrbsManager;
import com.compactorbs.widget.TargetWidget;
import com.compactorbs.widget.WidgetManager;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
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
import net.runelite.api.gameval.SpriteID;
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
 *
 * Four cardinal orientations (CompactOrbsConfig.BarOrientation): the bar
 * mounts at a point offset from the icon's own center (Orb/Vial
 * horizontal + vertical offsets - plain mountX/mountY added to
 * iconCenterX/Y) and extends outward from there. The collar (the real
 * backing widget's sprite) always stays tied to the icon's actual,
 * unmoved center though, never to the mount point - it's a fixed-size
 * connector, not an elastic one, so it's drawn snug against the real orb
 * regardless of how far the user has mounted the bar away from it.
 * SOUTH is the original, hand-tuned layout (vanilla never lays anything
 * out vertically for us, hence it alone repositions the number/icon).
 * WEST/EAST/NORTH all deliberately leave the icon/number wherever
 * vanilla's own default horizontal orb row already put them.
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

	// EAST's own number placement - dialed in live via temporary config
	// sliders (eastNumberXOffset/eastNumberYOffset), settled at -2/4, then
	// hardcoded and the config removed, same workflow as the SOUTH-only
	// pair above.
	private static final int EAST_NUMBER_X_OFFSET = -2;
	private static final int EAST_NUMBER_Y_OFFSET = 4;

	// Confirmed live via F8 + targeted testing: a -2,-2 nudge on the icon
	// group DID reach the widgets correctly (verified in the dump), but
	// made no visible difference, because drawRotatedNorthBacking() below
	// computes the collar's position FROM the icon's live canvas location -
	// nudging the icon just drags the collar along with it by the same
	// amount, so their RELATIVE alignment never changes. Reverted; the
	// real fix is NORTH_COLLAR_X/Y_NUDGE below instead, same lesson EAST's
	// own collar nudge already taught.

	// Small manual correction on NORTH's rotated-collar draw position -
	// same reasoning as EAST_COLLAR_X_NUDGE below, mirrored for the
	// rotated (not flipped) collar. -1 was also backwards here (same
	// sign-inversion EAST's own collar nudge hit) - flipped to +1.
	private static final int NORTH_COLLAR_X_NUDGE = 1;
	private static final int NORTH_COLLAR_Y_NUDGE = 1;

	// Small manual correction on EAST's flipped-collar draw position - the
	// one piece of EAST's layout with no existing nudge constant, unlike
	// NORTH/SOUTH's COLLAR_VERTICAL_NUDGE. -1 moved it RIGHT instead of
	// left (confirmed live) - sign was backwards, flipped to +1.
	private static final int EAST_COLLAR_X_NUDGE = 1;

	// Dialed in alongside the above: left padding bumped up and right
	// padding trimmed down by the same amount, shifting the whole
	// icon+number content cluster right to actually center it in the
	// (now content-hugging) container instead of leaving it left-heavy.
	private static final int CONTENT_LEFT_PADDING = 4;

	private static final int CONTENT_RIGHT_PADDING = 0;

	// No longer used by SOUTH (see its own barY/drawY comments) - this +1
	// was eating into the real collar's own top border, confirmed via a
	// zoomed screenshot pixel-matched against the raw source sprite. Kept
	// only for NORTH, which applies it alongside NORTH_COLLAR_Y_NUDGE
	// (+1) to net exactly zero - NORTH needing a same-size opposite
	// correction to look right is what originally pointed at this
	// constant being wrong for SOUTH too, rather than SOUTH-correct and
	// NORTH needing the fix.
	private static final int COLLAR_VERTICAL_NUDGE = 1;

	// Small along-axis gap between the icon's own edge and the bar's near
	// end for WEST/EAST, dialed in live the same way the vertical collar's
	// gap always was (via the existing offset sliders) rather than derived
	// precisely. EAST adds the flipped collar's own body radius on top of
	// this, same reasoning as the rotated-collar gap NORTH/SOUTH use.
	private static final int HORIZONTAL_GAP = 2;

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
	// path consumes) would be a silent no-op on it. Rotating/flipping it
	// for real means drawing our own transformed copy of its sprite and
	// hiding the real one - which is also why the icon never needs
	// touching: it's a separate real widget we just never hide or draw
	// over, so it stays upright for free instead of needing an explicit
	// counter-transform.
	//
	// Four caches below, one per direction that needs a replacement
	// collar - WEST is the odd one out: the source sprite's tab already
	// points west (left) unrotated, so WEST never replaces anything at
	// all and has no cache of its own.
	private BufferedImage rotatedSouthBacking;
	private int rotatedSouthBackingSpriteId = -1;
	private int rotatedSouthBackingBodyRadius;

	private BufferedImage rotatedNorthBacking;
	private int rotatedNorthBackingSpriteId = -1;
	private int rotatedNorthBackingBodyRadius;

	private BufferedImage flippedEastBacking;
	private int flippedEastBackingSpriteId = -1;
	private int flippedEastBackingBodyRadius;

	// Cached once per sprite id - these are static decorative art, never
	// change for the life of the client.
	private final Map<Integer, BufferedImage> borderSprites = new HashMap<>();

	// The level number isn't one of the orb container's named sub-ids
	// (backing/indicator/button/icon/empty) - confirmed via F8's widget
	// dump it's a plain untracked TEXT child, sitting to the LEFT of the
	// icon by default (the horizontal "default" orb layout, not ours).
	// Saved once per widget instance, same instance-identity reasoning as
	// the collar caches above - minimizing swaps interfaces entirely (new
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

	// Every orb container carries a SECOND real interactive widget besides
	// the icon/number - confirmed via F8's dump on all four orbs (HP,
	// Prayer, Spec, Run), always type=0 with no sprite/text of its own, at
	// a native X (~3) distinct from the icon's own native X (~27) - so
	// shiftContent()'s own icon-sibling-group matching correctly leaves it
	// alone. On Prayer this happens to be the real "Quick-prayers" toggle
	// (confirmed via its native "Activate"/"Setup" actions), but it's NOT
	// Prayer-specific - HP/Spec/Run all have the identical structural
	// widget, just without a Quick-prayers-style action attached. First
	// found and fixed for Prayer alone (searching by its "Activate"
	// action) - confirmed live that Spec Attack still had the exact same
	// click misalignment NORTH's own resize didn't touch, since nothing
	// ever adjusted this second widget for orbs that don't have that
	// action. Found structurally instead now (by native X, not by
	// action text) so the same fix applies to all four uniformly.
	private Widget extraWidget;
	private int extraOriginalX;
	private int extraOriginalY;
	private int extraOriginalWidth;
	private int extraOriginalHeight;

	// The real depletion-mask widget (see hideDepletionEffect()'s own doc
	// comment) - only its hidden state is ever touched, never its own
	// position/size, so this just needs to remember which widget instance
	// it is to restore hidden=false on cleanup, not a full original-value
	// snapshot like extraWidget's own pair above.
	private Widget depletionWidget;

	// Null until the first render() call - deliberately NOT defaulted to
	// one of the four real orientations, so the very first frame never
	// looks like a "switch" and triggers a pointless restore-then-reapply.
	private CompactOrbsConfig.BarOrientation lastOrientation;

	// Collar-drawing used to happen inline, right inside render(), via a
	// throwaway untranslated Graphics2D copy - confirmed live (screenshot)
	// that this made each bar's Z-order relative to a DIFFERENT bar's
	// collar depend on arbitrary overlay registration order rather than
	// what's actually on screen (one bar's own frame/vial could cover
	// another bar's collar). Since overlay priority applies to a whole
	// Overlay instance and all four bars share this same class, the fix
	// is a second, separately-prioritized overlay (OrbCollarOverlay) that
	// draws every bar's collar in one later pass, guaranteed after every
	// bar's own frame/vial regardless of which orb each belongs to. This
	// just captures WHAT to draw during the normal render() pass; the
	// actual drawing happens in drawCollar(), called by that overlay.
	private enum CollarKind { NONE, SOUTH, EAST, NORTH }
	private CollarKind pendingCollarKind = CollarKind.NONE;
	private Widget pendingCollarIcon;
	private Widget pendingCollarBacking;
	private int pendingCollarIconCenterX;
	private int pendingCollarIconCenterY;

	// The icon isn't drawn alone - F8's dump showed several sibling
	// graphics (indicator, the "empty"-fill wrapper, a hover-state
	// wrapper) all stacked at the SAME x as the icon, presumably swapped
	// in/out by the game depending on orb state. All of them - whichever
	// happens to be visible at the moment - need to move together, or
	// whichever one the game shows next would still be sitting at the old
	// x. Captured once (same original-x snapshot, before any edits) the
	// first time this container instance is seen - tracked by contentWidget
	// below, its OWN staleness check, deliberately separate from
	// containerWidget (resizeContainer()/growContainerWidth()'s field):
	// NORTH's shiftContent() call never touches width/height at all, so it
	// never sets containerWidget - reusing that field as this cache's
	// staleness check would re-snapshot every single frame for NORTH,
	// which silently corrupts contentOriginalX into the already-shifted
	// position instead of the true native one by the second frame -
	// confirmed live as West breaking after merely visiting North, since
	// restoreNumber() then "restores" to that corrupted value instead of
	// native. Confirmed via code review once the live symptom (icon out of
	// alignment with the real, untouched stone cap) pointed at a drifted
	// baseline, not a positioning formula bug.
	private Widget[] contentWidgets;
	private int[] contentOriginalX;
	private int[] contentOriginalY;
	private int[] contentOriginalXMode;
	private int[] contentOriginalYMode;
	private Widget contentWidget;

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

	/** Which way this bar extends from its mount point - per-bar, not shared, so each of HP/Prayer/Spec/Run can pick its own. */
	protected abstract CompactOrbsConfig.BarOrientation orientation();

	/**
	 * Suppresses the real orb's own native depletion mask (the
	 * SpriteID.OrbFiller.EMPTY-sprited overlay the game itself grows
	 * over the icon as the stat drains - confirmed live by comparing F8
	 * dumps at different HP values: a 26x0 wrapper at 96 HP, 26x3 at 84)
	 * - per-bar, since our own bar already shows the same information
	 * and the real orb is meant to just be the bar's label, not a
	 * second copy of the bar's own job.
	 */
	protected abstract boolean hideDepletionEffect();

	// Every setting below was promoted from a single Global Bar Config
	// shared across all four bars to a fully independent per-bar one
	// (same shape as orientation() above) - each bar's own default value
	// matches what the old shared setting used to default to, so a fresh
	// install still looks uniform out of the box, but each bar is now
	// freely customizable on its own from there.

	/** How long the bar is, end to end. */
	protected abstract int length();

	/** How thick the bar is, across its short axis. */
	protected abstract int width();

	/** Mounts the bar this many pixels right (+) or left (-) of the orb icon's own center, before it extends outward per orientation(). */
	protected abstract int horizontalOffset();

	/** Mounts the bar this many pixels down (+) or up (-) of the orb icon's own center, before it extends outward per orientation(). */
	protected abstract int verticalOffset();

	/** Thickness of the stone frame ring - the vial fill always fills whatever's left inside it. */
	protected abstract int frameThickness();

	/** Stroke width of the dark outline traced around the whole bar's outer edge, independent of frameThickness(). */
	protected abstract int frameShadowWidth();

	/** 0-100. Opacity of the vial fill - the regen/drain rings read more opaque than this (see regenOpacityFraction()/drainOpacityFraction()). Doesn't affect the frame - see frameOpacity(). */
	protected abstract int barOpacity();

	/** 0-100. Opacity of the stone frame ring, independent of barOpacity(). */
	protected abstract int frameOpacity();

	/** Which ring (regen or drain) draws on top, and reads more opaque, whenever both are visible at once on this bar. */
	protected abstract CompactOrbsConfig.RegenDrainPriority regenDrainPriority();

	/** Thickness of the regen/drain outline traced along the inside of the vial cavity. */
	protected abstract int regenDrainRingSize();

	/** Flips which end the vial fill empties from first - normally the near-icon end. */
	protected abstract boolean reverseBarFill();

	/** Flips which end the regen ring grows from, independently of reverseBarFill(). */
	protected abstract boolean reverseRegenRing();

	/** Flips which end the drain ring shrinks toward, independently of reverseBarFill() and reverseRegenRing(). */
	protected abstract boolean reverseDrainRing();

	/** 0-max current value (HP, prayer points, etc.) for the vial fill. */
	protected abstract int currentValue();

	/** Max value for the vial fill - must be >= 1. */
	protected abstract int maxValue();

	/** The vial's fill color - the one thing that visually distinguishes each bar. */
	protected abstract Color fillColor();

	/**
	 * Progress (0.0-1.0) toward this bar's next restoration tick, or null if
	 * this bar has nothing to show. Drawn as a growing outline traced along
	 * the inside of the vial cavity, not a separate
	 * orb ring or a frame-ring highlight (both tried, both dropped) - see
	 * drawVialRegenOutline()/drawVialRegenOutlineHorizontal(). In practice
	 * every bar currently returns a real 0.0-1.0 value rather than null
	 * (Prayer included - see PrayerVialOverlay), but the null path stays
	 * supported for any future bar that has nothing to report.
	 */
	protected Double regenPercentage()
	{
		return null;
	}

	/** Per-bar config toggle for the regen highlight - default off until a bar opts in. */
	protected boolean regenEnabled()
	{
		return false;
	}

	/** Highlight color for the regen progress fill - defaults to a brighter version of the bar's own fill color. */
	protected Color regenHighlightColor()
	{
		return fillColor().brighter();
	}

	/**
	 * Progress (1.0-0.0) until this bar's next expected drain tick, or null
	 * if nothing's currently draining - the mirror image of
	 * regenPercentage(): starts full right after a drain and counts DOWN to
	 * empty right before the next one, instead of growing up from empty.
	 * Genuinely separate from regen rather than a shared field, since a bar
	 * can gain and lose value on completely independent schedules (HP's
	 * poison/envenom tick vs its own hitpoint regen tick) - drawn as its own
	 * outline (drawVialDrainOutline()/drawVialDrainOutlineHorizontal()),
	 * overlaid on the regen ring per drainTakesPrecedence().
	 */
	protected Double drainPercentage()
	{
		return null;
	}

	/** Per-bar config toggle for the drain highlight - default off until a bar opts in. */
	protected boolean drainEnabled()
	{
		return false;
	}

	/** Highlight color for the drain progress fill - defaults to a brighter version of the bar's own fill color, same fallback as regenHighlightColor(). */
	protected Color drainHighlightColor()
	{
		return fillColor().brighter();
	}

	/**
	 * Threshold-glow mode - restored from v1 (see git log before the
	 * full-reset "gut pass"), which only ever had this for Spec as a
	 * "ready to use" alert (ACTIVE/COMBAT_ONLY, above-threshold - see
	 * thresholdAbove()). Generalized here to all four bars; defaults off
	 * until a bar opts in, same pattern as regenEnabled()/drainEnabled().
	 */
	protected ThresholdMode thresholdMode()
	{
		return ThresholdMode.DISABLED;
	}

	/** 0-100 percentage the threshold glow triggers at - see thresholdAbove() for which direction. */
	protected int thresholdValue()
	{
		return 0;
	}

	/** Color the icon pulses in once the threshold condition is met. */
	protected Color thresholdColor()
	{
		return Color.WHITE;
	}

	/** True if the glow fires at-or-ABOVE thresholdValue() (e.g. Spec's "charged enough" alert); false for at-or-BELOW (e.g. HP/Prayer/Run's "running low" warning). */
	protected boolean thresholdAbove()
	{
		return false;
	}

	/** Whether the threshold condition is currently met, given thresholdMode()'s gating (disabled/always/combat-only) - drives the bar pulse in drawBarPulse(). */
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

		int max = Math.max(maxValue(), 1);
		int current = Math.max(0, Math.min(currentValue(), max));
		int percentage = (int) Math.round(100.0 * current / max);

		return thresholdAbove() ? percentage >= thresholdValue() : percentage <= thresholdValue();
	}

	@Override
	public final Dimension render(Graphics2D graphics)
	{
		// Reset every frame, only set below if this frame actually has a
		// collar to draw - otherwise a bar that stops rendering (main
		// bars toggled off, orb not found, orientation switched to one
		// with no collar) would leave the separate collar pass drawing a
		// stale leftover from whenever it last ran. See the field comment
		// on pendingCollarKind for why collar-drawing moved out of this
		// method entirely.
		pendingCollarKind = CollarKind.NONE;

		if (!manager.isCompactLayout())
		{
			restoreBacking();
			restoreNumber();
			return null;
		}

		// CompactOrbsManager#hideMinimapOnTabClose() hides getOrbsParent()
		// (the shared ancestor ALL four orbs live under) whenever the side
		// panel/inventory tab closes - that stops the GAME's own native
		// rendering of everything inside it, real backing included, but has
		// zero effect on OUR OWN Graphics2D drawing, which only ever checks
		// isCompactLayout() above. Confirmed live (screenshots): without
		// this, the bar (and for SOUTH/EAST/NORTH, the self-drawn collar
		// too) kept showing regardless - the whole point of hiding orbs
		// with the inventory tab is that EVERYTHING disappears together,
		// not just the real widgets we don't draw ourselves.
		Widget orbsParent = widgetManager.getOrbsParent();
		if (orbsParent != null && orbsParent.isHidden())
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

		CompactOrbsConfig.BarOrientation orientation = orientation();

		// Switching orientation mid-session otherwise leaks whatever the
		// PREVIOUS orientation touched into the new one - confirmed live
		// (screenshot): EAST's width-grow was still active after switching
		// to NORTH, since NORTH never grows/shrinks width itself and
		// nothing ever told the old grow to let go. restoreNumber() already
		// resets every orientation-specific change this class makes
		// (container width/height, the content shift, NORTH's container/
		// icon Y-shift, the number itself) - it just needs to actually run
		// once AT the switch, not only when the icon can't be found.
		if (orientation != lastOrientation)
		{
			restoreBacking();
			restoreNumber();
			lastOrientation = orientation;
		}

		Widget icon = client.getWidget(orbContainer().getIconId());
		applyDepletionHiding(orb, icon);

		if (orientation == CompactOrbsConfig.BarOrientation.SOUTH)
		{
			return renderSouth(graphics, orb);
		}

		switch (orientation)
		{
			case EAST:
				return renderEast(graphics, orb, icon);
			case NORTH:
				return renderNorth(graphics, orb, icon);
			case WEST:
			default:
				return renderWest(graphics, orb, icon);
		}
	}

	/** The original, hand-built layout - bar hangs below the icon, which (unlike the other three) vanilla never laid out for us, hence the number/content repositioning below. */
	private Dimension renderSouth(Graphics2D graphics, Widget orb)
	{
		int width = width();
		int height = length();

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

		// Cross-axis offset (horizontal, perpendicular to SOUTH's own
		// vertical extension) capped to the bar's own width/thickness;
		// along-axis offset (vertical, the direction the bar extends in)
		// capped to its length - see clampOffset()'s own javadoc.
		int barX = iconCenterX - width / 2 + clampOffset(horizontalOffset(), width);
		// The bar's top starts exactly where the collar's round BODY ends
		// (iconCenterY + its radius), not where the collar's center is -
		// the body sits entirely above the bar, while the tab (the
		// "handle" hanging below the body) overlaps down into the bar's
		// top. No COLLAR_VERTICAL_NUDGE here (unlike before) - confirmed
		// live (zoomed screenshot, pixel-inspected against the raw source
		// sprite) that the +1 it applied was eating into the collar's own
		// top border, which the source art actually has - NORTH needed a
		// separate nudge specifically to CANCEL this same constant back
		// to net zero to look right, which is what finally pointed at it
		// here too, rather than it being SOUTH-correct and NORTH being the
		// one that needed correcting.
		int barY = iconCenterY + rotatedSouthBackingBodyRadius + clampOffset(verticalOffset(), height);

		// See old VitalBarOverlay's comment on this exact pair of calls
		// (git history) - a non-movable DYNAMIC overlay still needs its
		// stored bounds cleared each frame, or RuneLite's OverlayRenderer
		// re-applies a stale location on top of our own translate.
		setPreferredLocation(null);
		getBounds().setLocation(0, 0);
		graphics.translate(barX, barY);

		int max = Math.max(maxValue(), 1);
		int current = Math.max(0, Math.min(currentValue(), max));

		int frameWidth = frameThickness();
		int innerWidth = width - 2 * frameWidth;
		int innerHeight = height - 2 * frameWidth;
		int innerArc = Math.round(innerWidth * INNER_ARC_FRACTION);

		drawFrame(graphics, width, height, frameWidth, innerWidth, innerHeight, innerArc);
		drawBarPulse(graphics, frameWidth, innerWidth, innerHeight, innerArc);
		drawVial(graphics, frameWidth, innerWidth, innerHeight, innerArc, current, max, true);
		drawVialOutlines(graphics, frameWidth, innerWidth, innerHeight, innerArc, true);

		pendingCollarKind = CollarKind.SOUTH;
		pendingCollarIcon = icon;
		pendingCollarBacking = client.getWidget(orbContainer().getBackingId());
		pendingCollarIconCenterX = iconCenterX;
		pendingCollarIconCenterY = iconCenterY;

		return new Dimension(width, height);
	}

	/**
	 * Bar extends LEFT from the mount point. The source collar sprite's
	 * tab already points west (left) unrotated - exactly right already -
	 * so this is the one direction that never replaces the real backing
	 * at all, just leaves it fully alone.
	 */
	private Dimension renderWest(Graphics2D graphics, Widget orb, Widget icon)
	{
		restoreNumber();
		restoreBacking();

		Point orbCanvas = orb.getCanvasLocation();
		int orbCenterX = orbCanvas.getX() + orb.getWidth() / 2;
		int orbCenterY = orbCanvas.getY() + orb.getHeight() / 2;

		Point iconCanvas = icon != null ? icon.getCanvasLocation() : null;
		int iconCenterX = iconCanvas != null ? iconCanvas.getX() + icon.getWidth() / 2 : orbCenterX;
		int iconCenterY = iconCanvas != null ? iconCanvas.getY() + icon.getHeight() / 2 : orbCenterY;

		int length = length();
		int thickness = width();

		// The mount point - plain offset from the icon's own center,
		// capped dynamically to the bar's own footprint (see
		// clampOffset()'s javadoc): along-axis (horizontal, the direction
		// WEST extends in) to length, cross-axis (vertical) to thickness.
		// The bar extends outward from here; the collar (left alone above -
		// its native, unrotated sprite already points the right way) always
		// stays tied to iconCenterX/Y instead, never to this point, since
		// it's a fixed-size connector that can't stretch to meet a
		// freely-repositioned bar.
		int anchorX = iconCenterX + clampOffset(horizontalOffset(), length);
		int anchorY = iconCenterY + clampOffset(verticalOffset(), thickness);

		int barX = anchorX - HORIZONTAL_GAP - length;
		int barY = anchorY - thickness / 2;

		Graphics2D absoluteGraphics = (Graphics2D) graphics.create();

		setPreferredLocation(null);
		getBounds().setLocation(0, 0);
		graphics.translate(barX, barY);

		int max = Math.max(maxValue(), 1);
		int current = Math.max(0, Math.min(currentValue(), max));

		int frameWidth = frameThickness();
		int innerLength = length - 2 * frameWidth;
		int innerThickness = thickness - 2 * frameWidth;
		int innerArc = Math.round(innerThickness * INNER_ARC_FRACTION);

		drawFrame(graphics, length, thickness, frameWidth, innerLength, innerThickness, innerArc);
		drawBarPulse(graphics, frameWidth, innerLength, innerThickness, innerArc);
		drawVialHorizontal(graphics, frameWidth, innerLength, innerThickness, innerArc, current, max, false);
		drawVialOutlinesHorizontal(graphics, frameWidth, innerLength, innerThickness, innerArc, false);

		absoluteGraphics.dispose();

		return new Dimension(length, thickness);
	}

	/** Bar extends RIGHT from the mount point - mirror of renderWest(), with a horizontally-flipped collar replacing the real one (whose native tab points the wrong way for this direction). Unlike WEST, vanilla's native number position (left of icon) doesn't suit this direction, so it gets repositioned too - see repositionNumberEast(). */
	private Dimension renderEast(Graphics2D graphics, Widget orb, Widget icon)
	{
		if (icon != null)
		{
			repositionNumberEast(orb, icon);
		}
		else
		{
			restoreNumber();
		}

		Point orbCanvas = orb.getCanvasLocation();
		int orbCenterX = orbCanvas.getX() + orb.getWidth() / 2;
		int orbCenterY = orbCanvas.getY() + orb.getHeight() / 2;

		Point iconCanvas = icon != null ? icon.getCanvasLocation() : null;
		int iconCenterX = iconCanvas != null ? iconCanvas.getX() + icon.getWidth() / 2 : orbCenterX;
		int iconCenterY = iconCanvas != null ? iconCanvas.getY() + icon.getHeight() / 2 : orbCenterY;

		int length = length();
		int thickness = width();

		// Along-axis (horizontal, the direction EAST extends in) capped to
		// length, cross-axis (vertical) capped to thickness - see
		// clampOffset()'s javadoc.
		int anchorX = iconCenterX + clampOffset(horizontalOffset(), length);
		int anchorY = iconCenterY + clampOffset(verticalOffset(), thickness);

		Widget backing = client.getWidget(orbContainer().getBackingId());
		boolean drawingFlippedCollar = backing != null && getFlippedEastBacking(backing.getSpriteId()) != null;
		if (drawingFlippedCollar)
		{
			widgetManager.setHidden(orbContainer().getBackingId(), true);
		}
		else
		{
			restoreBacking();
		}

		int gap = HORIZONTAL_GAP + (drawingFlippedCollar ? flippedEastBackingBodyRadius : 0);
		int barX = anchorX + gap;
		int barY = anchorY - thickness / 2;

		setPreferredLocation(null);
		getBounds().setLocation(0, 0);
		graphics.translate(barX, barY);

		int max = Math.max(maxValue(), 1);
		int current = Math.max(0, Math.min(currentValue(), max));

		int frameWidth = frameThickness();
		int innerLength = length - 2 * frameWidth;
		int innerThickness = thickness - 2 * frameWidth;
		int innerArc = Math.round(innerThickness * INNER_ARC_FRACTION);

		drawFrame(graphics, length, thickness, frameWidth, innerLength, innerThickness, innerArc);
		drawBarPulse(graphics, frameWidth, innerLength, innerThickness, innerArc);
		drawVialHorizontal(graphics, frameWidth, innerLength, innerThickness, innerArc, current, max, true);
		drawVialOutlinesHorizontal(graphics, frameWidth, innerLength, innerThickness, innerArc, true);

		if (drawingFlippedCollar)
		{
			pendingCollarKind = CollarKind.EAST;
			pendingCollarIcon = icon;
			pendingCollarBacking = backing;
			pendingCollarIconCenterX = iconCenterX;
			pendingCollarIconCenterY = iconCenterY;
		}

		return new Dimension(length, thickness);
	}

	/** Bar extends UP from the mount point - mirror of renderSouth(), with an oppositely-rotated collar (tab pointing up instead of down) replacing the real one. Like EAST (and unlike WEST), vanilla's native number position doesn't suit this direction either, so it gets repositioned too - see repositionNumberNorth(). */
	private Dimension renderNorth(Graphics2D graphics, Widget orb, Widget icon)
	{
		if (icon != null)
		{
			repositionNumberNorth(orb, icon);
		}
		else
		{
			restoreNumber();
		}

		Point orbCanvas = orb.getCanvasLocation();
		int orbCenterX = orbCanvas.getX() + orb.getWidth() / 2;
		int orbCenterY = orbCanvas.getY() + orb.getHeight() / 2;

		Point iconCanvas = icon != null ? icon.getCanvasLocation() : null;
		int iconCenterX = iconCanvas != null ? iconCanvas.getX() + icon.getWidth() / 2 : orbCenterX;
		int iconCenterY = iconCanvas != null ? iconCanvas.getY() + icon.getHeight() / 2 : orbCenterY;

		int length = length();
		int thickness = width();

		// Along-axis (vertical, the direction NORTH extends in) capped to
		// length, cross-axis (horizontal) capped to thickness - mirrors
		// SOUTH's own clamp, see clampOffset()'s javadoc.
		int anchorX = iconCenterX + clampOffset(horizontalOffset(), thickness);
		int anchorY = iconCenterY + clampOffset(verticalOffset(), length);

		Widget backing = client.getWidget(orbContainer().getBackingId());
		boolean drawingRotatedCollar = backing != null && getRotatedNorthBacking(backing.getSpriteId()) != null;
		if (drawingRotatedCollar)
		{
			widgetManager.setHidden(orbContainer().getBackingId(), true);
		}
		else
		{
			restoreBacking();
		}

		int gap = (drawingRotatedCollar ? rotatedNorthBackingBodyRadius : 0) + COLLAR_VERTICAL_NUDGE;
		int barX = anchorX - thickness / 2;
		int barY = anchorY - gap - length;

		setPreferredLocation(null);
		getBounds().setLocation(0, 0);
		graphics.translate(barX, barY);

		int max = Math.max(maxValue(), 1);
		int current = Math.max(0, Math.min(currentValue(), max));

		int frameWidth = frameThickness();
		int innerWidth = thickness - 2 * frameWidth;
		int innerHeight = length - 2 * frameWidth;
		int innerArc = Math.round(innerWidth * INNER_ARC_FRACTION);

		drawFrame(graphics, thickness, length, frameWidth, innerWidth, innerHeight, innerArc);
		drawBarPulse(graphics, frameWidth, innerWidth, innerHeight, innerArc);
		drawVial(graphics, frameWidth, innerWidth, innerHeight, innerArc, current, max, false);
		drawVialOutlines(graphics, frameWidth, innerWidth, innerHeight, innerArc, false);

		if (drawingRotatedCollar)
		{
			pendingCollarKind = CollarKind.NORTH;
			pendingCollarIcon = icon;
			pendingCollarBacking = backing;
			pendingCollarIconCenterX = iconCenterX;
			pendingCollarIconCenterY = iconCenterY;
		}

		return new Dimension(thickness, length);
	}

	/**
	 * Dynamically caps a mount-point offset to the bar's own current
	 * footprint, rather than a fixed design-time range - bound is whichever
	 * of length/thickness applies to that axis for the active orientation
	 * (see each render* method's own call site). Keeps the bar from being
	 * mounted arbitrarily far from the icon as Length/Width get dialed in
	 * or changed, without needing a second, orientation-aware @Range on
	 * the config items themselves (RuneLite's Range is a fixed, static
	 * bound - it can't vary with another config value at runtime).
	 */
	private static int clampOffset(int offset, int bound)
	{
		return Math.max(-bound, Math.min(bound, offset));
	}

	/** Global Bar Config's opacity slider, as a 0.0-1.0 composite fraction. Vial fill only - see frameOpacityFraction() for the frame ring. */
	private float barOpacityFraction()
	{
		return barOpacity() / 100f;
	}

	/** Global Bar Config's frame opacity slider, as a 0.0-1.0 composite fraction - independent of the vial fill's own barOpacityFraction(). */
	private float frameOpacityFraction()
	{
		return frameOpacity() / 100f;
	}

	/** The regen outline always reads 30 percentage points more opaque than the bar itself, capped at fully opaque. */
	private float regenOpacityFraction()
	{
		return Math.min(100, barOpacity() + 30) / 100f;
	}

	/** The drain outline reads even more opaque than regen's own boost - it's meant to read as the dominant ring whenever both are visible together. */
	private float drainOpacityFraction()
	{
		return Math.min(100, barOpacity() + 50) / 100f;
	}

	private static final int PULSE_PERIOD_MILLIS = 1200;
	private static final float PULSE_MIN_FRACTION = 0.3f;

	/**
	 * Breathing pulse (not a hard blink) over thresholdColor()'s own alpha,
	 * washed over just the inner vial cavity - NOT the frame (tried the
	 * full outer footprint first, read as too much frame involvement) -
	 * same inset/shape drawVial() itself fills. Drawn BEHIND the vial fill
	 * (called right after drawFrame(), before drawVial() - see each
	 * render*() call site) rather than on top, so it shines through the
	 * fill's own translucent background/current-value color instead of
	 * masking it. Tried highlighting the icon before that (v1's own
	 * approach, since it drew its own icon bitmap and could tint it
	 * directly), but this engine keeps the real native icon widget in
	 * place instead of drawing one, and a halo on it read fine for v1's
	 * hand-built icon but not here. graphics must already be
	 * bar-relative/translated (same space drawFrame()/drawVial() use), not
	 * absolute canvas coordinates. Wall-clock timed (not tick-based) so
	 * the pulse stays smooth regardless of game tick rate.
	 */
	private void drawBarPulse(Graphics2D graphics, int frameWidth, int innerWidth, int innerHeight, int innerArc)
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
		Graphics2D frameGraphics = (Graphics2D) graphics.create();
		frameGraphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, frameOpacityFraction()));

		RoundRectangle2D.Float outerShape = new RoundRectangle2D.Float(0, 0, width, height, FRAME_ARC, FRAME_ARC);
		RoundRectangle2D.Float innerShapeAtInset = new RoundRectangle2D.Float(
			frameWidth, frameWidth, innerWidth, innerHeight, innerArc, innerArc);

		Area frameRing = new Area(outerShape);
		frameRing.subtract(new Area(innerShapeAtInset));

		frameGraphics.setColor(FRAME_FILL_COLOR);
		frameGraphics.fill(frameRing);

		frameGraphics.setColor(FRAME_OUTLINE_COLOR);
		frameGraphics.setStroke(new BasicStroke(Math.max(1, frameShadowWidth())));
		frameGraphics.draw(new RoundRectangle2D.Float(0, 0, width - 1, height - 1, FRAME_ARC, FRAME_ARC));
		frameGraphics.setStroke(new BasicStroke(1));
		frameGraphics.draw(new RoundRectangle2D.Float(frameWidth, frameWidth, innerWidth - 1, innerHeight - 1, innerArc, innerArc));

		drawRealBorderFrame(frameGraphics, width, height, frameWidth);
		frameGraphics.dispose();
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
	 * Called by OrbCollarOverlay, a separate overlay given a higher
	 * priority than this one specifically so every bar's collar draws
	 * after every bar's own frame/vial, regardless of which of the four
	 * this one is - see the field comment on pendingCollarKind. No-op if
	 * this frame's render() didn't leave a collar pending (WEST never
	 * does, and SOUTH/EAST/NORTH only do when a valid rotated sprite was
	 * actually found).
	 */
	void drawCollar(Graphics2D graphics)
	{
		if (pendingCollarKind == CollarKind.NONE)
		{
			return;
		}

		Graphics2D collarGraphics = (Graphics2D) graphics.create();
		switch (pendingCollarKind)
		{
			case SOUTH:
				drawRotatedSouthBacking(collarGraphics, pendingCollarIcon, pendingCollarBacking, pendingCollarIconCenterX, pendingCollarIconCenterY);
				break;
			case EAST:
				drawFlippedEastBacking(collarGraphics, pendingCollarIcon, pendingCollarBacking, pendingCollarIconCenterX, pendingCollarIconCenterY);
				break;
			case NORTH:
				drawRotatedNorthBacking(collarGraphics, pendingCollarIcon, pendingCollarBacking, pendingCollarIconCenterX, pendingCollarIconCenterY);
				break;
			case NONE:
				break;
		}
		collarGraphics.dispose();
	}

	/**
	 * Hides the real backing (stone orb + tab) widget and draws a 90°
	 * counter-clockwise-rotated copy of its own sprite in its place, so the
	 * tab that normally protrudes left now points down - acting as a
	 * "collar" sitting ABOVE the bar (see barY in renderSouth()), with only
	 * its tab/handle actually overlapping the bar's top. Called via
	 * drawCollar(), from OrbCollarOverlay's own later render pass, so the
	 * whole collar - tab included - stays fully visible, painted on top of
	 * every bar's frame/vial rather than getting covered by one. Everything else on
	 * the real orb (icon, button hitbox, indicator) is left completely
	 * alone - in particular the icon, which is a separate real widget, so
	 * it stays upright with no counter-rotation needed, and stays
	 * real/clickable with whatever menu entries it already has - nothing
	 * new added here.
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
	private void drawRotatedSouthBacking(Graphics2D graphics, Widget icon, Widget backing, int iconCenterX, int iconCenterY)
	{
		if (icon == null || backing == null)
		{
			return;
		}

		BufferedImage rotated = getRotatedSouthBacking(backing.getSpriteId());
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
		// extra length - rotatedSouthBackingBodyRadius (half the SOURCE
		// height, measured directly off the sprite: 57x34, body fills the
		// full 34) already accounts for this, where rotated.getHeight()/2
		// (half of 57) doesn't.
		int drawX = iconCenterX - rotated.getWidth() / 2;
		int drawY = iconCenterY - rotatedSouthBackingBodyRadius;
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
	 * relocated, never hidden or given new behavior. SOUTH-only - see the
	 * class javadoc for why the other three directions never call this.
	 * <p>
	 * The container gets a hard dimension swap (swapContainerToVertical())
	 * instead of a computed minimal fit now - same reasoning as NORTH's own
	 * swap, see that method's doc comment.
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

		int iconNativeX = icon.getOriginalX();

		swapContainerToVertical(container);
		int verticalWidth = containerOriginalHeight;
		int verticalHeight = containerOriginalWidth;

		shiftContent(container, icon, (verticalWidth - icon.getWidth()) / 2, false);

		// SOUTH never needed to touch the icon's own Y before (it just
		// stays at its pure native position) - +1 dialed in live against
		// the real widget (the config-slider version read as "-1" visually
		// during testing, but the sign flipped once hardcoded - +1 is the
		// value that's actually confirmed correct here). Shifts ONLY the
		// icon itself, not the whole contentWidgets sibling group -
		// confirmed live the other layered siblings (mostly hidden or
		// zero-height state sprites anyway) were never the ones out of
		// place, just this one.
		int contentYOffset = 1;
		for (int i = 0; i < contentWidgets.length; i++)
		{
			if (contentWidgets[i] != icon)
			{
				continue;
			}
			int targetContentY = contentOriginalY[i] + contentYOffset;
			if (icon.getOriginalY() != targetContentY)
			{
				icon.setOriginalY(targetContentY);
				icon.revalidate();
			}
			break;
		}

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

		swapExtraWidgetToVertical(container, iconNativeX, verticalWidth, verticalHeight);
	}

	/**
	 * EAST counterpart to repositionNumber() - moves the real level number
	 * to sit beside the icon on the bar's own side (right) instead of
	 * vanilla's native left, vertically centered on the icon.
	 * <p>
	 * Originally skipped shiftContentLeft() entirely, reasoning "nothing on
	 * the right needs closing up" - wrong framing. The number VACATES the
	 * LEFT side (vanilla's native number-left-of-icon layout), the same gap
	 * SOUTH's shiftContentLeft() exists to close - EAST just never closed
	 * it, so the container carried BOTH the old, now-empty number slot on
	 * the left AND the new number space added on the right, confirmed live
	 * (screenshot) as a container box visibly "too big for just the orb."
	 * Calling shiftContentLeft() here first - exact same mechanism SOUTH
	 * already uses - reclaims that left-side space before the number's
	 * target X is computed, so growContainerWidth() below only ever grows
	 * past the container's true native footprint when the icon+number
	 * genuinely don't fit inside it, not because of a stale unused gap.
	 */
	private void repositionNumberEast(Widget container, Widget icon)
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

		shiftContent(container, icon, CONTENT_LEFT_PADDING, false);

		if (number.getXPositionMode() != WidgetPositionMode.ABSOLUTE_LEFT)
		{
			number.setXPositionMode(WidgetPositionMode.ABSOLUTE_LEFT);
		}
		if (number.getYPositionMode() != WidgetPositionMode.ABSOLUTE_TOP)
		{
			number.setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP);
		}

		int targetX = icon.getRelativeX() + icon.getWidth() + EAST_NUMBER_X_OFFSET;
		int targetY = icon.getRelativeY() + icon.getHeight() / 2 - number.getHeight() / 2 + EAST_NUMBER_Y_OFFSET;

		if (number.getOriginalX() != targetX || number.getOriginalY() != targetY)
		{
			number.setOriginalX(targetX);
			number.setOriginalY(targetY);
			number.revalidate();
		}

		growContainerWidth(container, targetX + number.getWidth() + CONTENT_RIGHT_PADDING);
	}

	/**
	 * NORTH counterpart to repositionNumber() - the real level number
	 * needs to sit above the icon, but the container clips children to
	 * [0, height) so a negative-Y real number simply vanishes rather than
	 * merely overlapping. Earlier versions solved that by moving the real
	 * CONTAINER's own Y to carve out room above - confirmed live,
	 * repeatedly, that doing so was the root of every North-specific
	 * problem: it fought the drag system during editing (orb stuck),
	 * produced a visible jump reconciling the editing/non-editing states
	 * on exit, and left the real click hitbox drifting from the visuals
	 * (the native hitbox follows the container's own bounds).
	 * <p>
	 * The actual fix doesn't need to give up the real number at all -
	 * SOUTH/EAST never touch the container's Y because they only ever
	 * grow it DOWNWARD (new content fits below the icon, extending the
	 * bottom edge, which never requires moving the top). NORTH can use
	 * that exact same safe operation, just inverted: grow the container
	 * downward same as SOUTH/EAST do, but put the NUMBER at the newly
	 * available top (relative Y=0, never negative, never clipped) and
	 * push the ICON (and its layered siblings) down into the space below
	 * it instead - the icon ends up a small, fixed distance lower than
	 * vanilla's native spot, but that offset is constant and never
	 * differs between editing and non-editing, so there's nothing to
	 * desync and nothing fighting the drag system, since the container's
	 * Y is never written at all. Uses swapContainerToVertical() - a hard
	 * width↔height swap rather than a computed minimal fit - so NORTH and
	 * SOUTH's containers are provably the same geometry as the real
	 * horizontal native size, just rotated, with nothing to independently
	 * drift out of sync between them.
	 */
	private void repositionNumberNorth(Widget container, Widget icon)
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

		if (number.isHidden())
		{
			number.setHidden(false);
		}

		// Captured before shiftContent() below moves it - findExtraWidget()
		// needs the icon's TRUE native X to tell it apart from the icon's
		// own layered siblings (which share its native X and get shifted
		// together), not whatever shiftContent() just moved it to.
		int iconNativeX = icon.getOriginalX();

		swapContainerToVertical(container);
		int verticalWidth = containerOriginalHeight;
		int verticalHeight = containerOriginalWidth;

		shiftContent(container, icon, (verticalWidth - icon.getWidth()) / 2, true);

		if (number.getXPositionMode() != WidgetPositionMode.ABSOLUTE_LEFT)
		{
			number.setXPositionMode(WidgetPositionMode.ABSOLUTE_LEFT);
		}
		if (number.getYPositionMode() != WidgetPositionMode.ABSOLUTE_TOP)
		{
			number.setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP);
		}

		int margin = number.getHeight() + 2;

		// +7 dialed in live (temporary config slider, same workflow as
		// EAST_NUMBER_X_OFFSET/Y_OFFSET's own history) against the hard-
		// swapped container's actual rendered position - the computed
		// margin alone left the icon group sitting too high, confirmed
		// via a zoomed screenshot showing the collar sticking out past
		// the purple drag-zone outline's top edge.
		int contentYOffset = 7;

		// Push the icon group down by margin - the inverse of the
		// contentOriginalY shift this same mechanism used when the
		// container itself moved; now the container's top stays put and
		// the icon moves instead, opening clean non-negative space above
		// it for the number. The container's own height is now a fixed
		// hard-swapped size (see swapContainerToVertical()) rather than
		// computed to fit this exactly, so there's always comfortable
		// headroom below the icon too.
		for (int i = 0; i < contentWidgets.length; i++)
		{
			Widget content = contentWidgets[i];
			int targetContentY = contentOriginalY[i] + margin + contentYOffset;
			if (content.getOriginalY() != targetContentY)
			{
				content.setOriginalY(targetContentY);
				content.revalidate();
			}
		}

		// X=-5, Y=10 both dialed in live the same way.
		int targetX = icon.getRelativeX() + icon.getWidth() / 2 - number.getWidth() / 2 - 5;
		int targetY = 10;

		if (number.getOriginalX() != targetX || number.getOriginalY() != targetY)
		{
			number.setOriginalX(targetX);
			number.setOriginalY(targetY);
			number.revalidate();
		}

		swapExtraWidgetToVertical(container, iconNativeX, verticalWidth, verticalHeight);
	}

	/**
	 * Finds the orb's second real interactive widget (see the field
	 * comment on extraWidget) by its native X, distinct from the icon's -
	 * more robust than matching on action text (Prayer's "Activate" only
	 * exists for Prayer; this has to work for HP/Spec/Run too, which carry
	 * the identical structural widget without that action).
	 */
	private static Widget findExtraWidget(Widget container, int iconNativeX)
	{
		Widget[] children = container.getStaticChildren();
		if (children == null)
		{
			return null;
		}
		for (Widget child : children)
		{
			if (child != null && child.getType() == WidgetType.LAYER
				&& child.getOriginalX() != iconNativeX)
			{
				return child;
			}
		}
		return null;
	}

	/**
	 * Hides (or restores) the real orb's native depletion mask - see
	 * hideDepletionEffect()'s own doc comment. Runs unconditionally every
	 * frame regardless of orientation (called once from the shared
	 * render() dispatcher, before any orientation-specific branch), since
	 * the real widget being drained is a property of the ORB itself, not
	 * of however our own bar happens to be oriented.
	 */
	private void applyDepletionHiding(Widget container, Widget icon)
	{
		if (icon == null)
		{
			if (depletionWidget != null)
			{
				depletionWidget.setHidden(false);
				depletionWidget = null;
			}
			return;
		}

		Widget depletion = findDepletionWidget(container, icon.getOriginalX());
		if (depletion == null)
		{
			return;
		}

		depletionWidget = depletion;
		boolean hide = hideDepletionEffect();
		if (depletion.isHidden() != hide)
		{
			depletion.setHidden(hide);
		}
	}

	/**
	 * The real depletion-mask widget is a plain LAYER sharing the icon's
	 * own native X (part of the same sibling cluster shiftContent()
	 * matches), whose one static child is sprited
	 * SpriteID.OrbFiller.EMPTY - confirmed via F8 dumps at different HP
	 * values that THIS specific wrapper's own height (not the icon's)
	 * grows as the stat drains (26x0 at 96 HP, 26x3 at 84), and that the
	 * same EMPTY sprite ID is shared identically across all four orbs,
	 * not just HP.
	 */
	private static Widget findDepletionWidget(Widget container, int iconNativeX)
	{
		Widget[] children = container.getStaticChildren();
		if (children == null)
		{
			return null;
		}
		for (Widget child : children)
		{
			if (child == null || child.getType() != WidgetType.LAYER || child.getOriginalX() != iconNativeX)
			{
				continue;
			}
			Widget[] nested = child.getStaticChildren();
			if (nested == null)
			{
				continue;
			}
			for (Widget n : nested)
			{
				if (n != null && n.getSpriteId() == SpriteID.OrbFiller.EMPTY)
				{
					return child;
				}
			}
		}
		return null;
	}

	/**
	 * Shifts the icon and every sibling sharing its original x (the
	 * layered indicator/empty-fill/hover graphics - see field comment)
	 * to targetX as one group, so whichever sibling the game happens to be
	 * showing right now still lines up with the others. Captured once per
	 * container instance.
	 * <p>
	 * targetX varies by caller: EAST passes CONTENT_LEFT_PADDING, since it
	 * also shrinks/caps the container to hug the content afterward, making
	 * a small fixed left padding read as "snug." SOUTH/NORTH both now hard
	 * swap the container to a fixed, generously-sized width instead (see
	 * swapContainerToVertical()), so they compute a true centered targetX
	 * from that known width rather than using a fixed padding constant
	 * that only made sense against a minimal-fit box.
	 */
	private void shiftContent(Widget container, Widget icon, int targetX, boolean forceTopLeftAnchor)
	{
		if (contentWidgets == null || contentWidget != container)
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
			contentOriginalY = new int[contentWidgets.length];
			contentOriginalXMode = new int[contentWidgets.length];
			contentOriginalYMode = new int[contentWidgets.length];
			for (int i = 0; i < contentWidgets.length; i++)
			{
				contentOriginalX[i] = contentWidgets[i].getOriginalX();
				contentOriginalY[i] = contentWidgets[i].getOriginalY();
				contentOriginalXMode[i] = contentWidgets[i].getXPositionMode();
				contentOriginalYMode[i] = contentWidgets[i].getYPositionMode();
			}
			contentWidget = container;
		}

		// forceTopLeftAnchor - NORTH-only (confirmed live, screenshot):
		// its hard-swapped, now-taller container pushed the icon group out
		// past its OWN bottom edge instead of leaving the expected
		// headroom below it, matching what a non-ABSOLUTE_TOP/LEFT native
		// anchor on these specific widgets would produce when the
		// container's height changes out from under them. Confirmed live
		// a SECOND time (screenshot) that applying this same force
		// unconditionally to SOUTH/EAST too - which never had that
		// problem and were already correct - introduced a NEW 1-2px
		// vertical shift there instead, so it's opt-in per caller now
		// rather than automatic for every orientation that calls this.
		for (Widget content : contentWidgets)
		{
			if (forceTopLeftAnchor)
			{
				if (content.getXPositionMode() != WidgetPositionMode.ABSOLUTE_LEFT)
				{
					content.setXPositionMode(WidgetPositionMode.ABSOLUTE_LEFT);
				}
				if (content.getYPositionMode() != WidgetPositionMode.ABSOLUTE_TOP)
				{
					content.setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP);
				}
			}
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

	/**
	 * Hard dimension swap for a vertical orientation (NORTH/SOUTH) - the
	 * container ends up EXACTLY width↔height of its true native size, no
	 * computed shrink/grow fit. Confirmed live that computing a
	 * minimal-fit size independently per orientation was how a stale
	 * manual pixel nudge went unnoticed for so long (see
	 * COLLAR_VERTICAL_NUDGE's own history) - a hard swap means vertical
	 * and horizontal orientations are PROVABLY the same geometry, just
	 * rotated, with nothing to independently drift out of sync. Reuses
	 * the same containerWidget/containerOriginalWidth/Height fields
	 * resizeContainer() and growContainerWidth() already share, so
	 * restoreNumber() needs no separate handling for this.
	 */
	private void swapContainerToVertical(Widget container)
	{
		if (containerWidget != container)
		{
			containerOriginalWidth = container.getOriginalWidth();
			containerOriginalHeight = container.getOriginalHeight();
			containerWidget = container;
		}

		boolean changed = false;
		if (container.getOriginalWidth() != containerOriginalHeight)
		{
			container.setOriginalWidth(containerOriginalHeight);
			changed = true;
		}
		if (container.getOriginalHeight() != containerOriginalWidth)
		{
			container.setOriginalHeight(containerOriginalWidth);
			changed = true;
		}
		if (changed)
		{
			container.revalidate();
		}
	}

	/**
	 * Same hard-swap principle as swapContainerToVertical(), applied to
	 * the orb's second real interactive widget (see the field comment on
	 * extraWidget) - swapped to its own true native width↔height (not
	 * clamped to the icon's footprint, an earlier compromise from when
	 * the container was only grown to a tight computed fit and a literal
	 * swap didn't have room to land) and centered in the now
	 * hard-swapped, generously-sized container. iconNativeX is the
	 * icon's PRE-shiftContent() original X - see repositionNumberNorth()
	 * for why this has to be captured before shiftContent() runs.
	 */
	private void swapExtraWidgetToVertical(Widget container, int iconNativeX, int containerWidth, int containerHeight)
	{
		Widget extra = findExtraWidget(container, iconNativeX);
		if (extra == null)
		{
			return;
		}

		if (extraWidget != extra)
		{
			extraOriginalX = extra.getOriginalX();
			extraOriginalY = extra.getOriginalY();
			extraOriginalWidth = extra.getOriginalWidth();
			extraOriginalHeight = extra.getOriginalHeight();
			extraWidget = extra;
		}

		int targetWidth = extraOriginalHeight;
		int targetHeight = extraOriginalWidth;
		int targetX = (containerWidth - targetWidth) / 2;
		int targetY = (containerHeight - targetHeight) / 2;

		boolean changed = false;
		if (extra.getOriginalWidth() != targetWidth)
		{
			extra.setOriginalWidth(targetWidth);
			changed = true;
		}
		if (extra.getOriginalHeight() != targetHeight)
		{
			extra.setOriginalHeight(targetHeight);
			changed = true;
		}
		if (extra.getOriginalX() != targetX)
		{
			extra.setOriginalX(targetX);
			changed = true;
		}
		if (extra.getOriginalY() != targetY)
		{
			extra.setOriginalY(targetY);
			changed = true;
		}
		if (changed)
		{
			extra.revalidate();
		}
	}

	/**
	 * EAST-only counterpart to resizeContainer() - grows width only, never
	 * shrinks it (no SOUTH-style content shift happens on this side to
	 * justify trimming down), and never touches height at all. Shares the
	 * same containerWidget/containerOriginalWidth/containerOriginalHeight
	 * fields as resizeContainer() - restoreNumber() already restores both
	 * generically, and containerOriginalWidth is only ever snapshotted
	 * once per widget instance, so switching orientations mid-session
	 * always grows/shrinks from the true native baseline, never a
	 * previously-adjusted value.
	 */
	private void growContainerWidth(Widget container, int requiredWidth)
	{
		if (containerWidget != container)
		{
			containerOriginalWidth = container.getOriginalWidth();
			containerOriginalHeight = container.getOriginalHeight();
			containerWidget = container;
		}

		int targetWidth = Math.max(containerOriginalWidth, requiredWidth);
		if (container.getOriginalWidth() != targetWidth)
		{
			container.setOriginalWidth(targetWidth);
			container.revalidate();
		}
	}

	/** Safe to call unconditionally any time we're not actively repositioning the number - a no-op once nothing's saved. */
	private void restoreNumber()
	{
		if (depletionWidget != null)
		{
			depletionWidget.setHidden(false);
			depletionWidget = null;
		}

		if (extraWidget != null)
		{
			extraWidget.setOriginalX(extraOriginalX);
			extraWidget.setOriginalY(extraOriginalY);
			extraWidget.setOriginalWidth(extraOriginalWidth);
			extraWidget.setOriginalHeight(extraOriginalHeight);
			extraWidget.revalidate();
			extraWidget = null;
		}

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
				contentWidgets[i].setXPositionMode(contentOriginalXMode[i]);
				contentWidgets[i].setYPositionMode(contentOriginalYMode[i]);
				contentWidgets[i].setOriginalX(contentOriginalX[i]);
				contentWidgets[i].setOriginalY(contentOriginalY[i]);
				contentWidgets[i].revalidate();
			}
			contentWidgets = null;
			contentOriginalX = null;
			contentOriginalY = null;
			contentOriginalXMode = null;
			contentOriginalYMode = null;
			contentWidget = null;
		}

		if (numberWidget == null)
		{
			return;
		}

		// NORTH never moves the number's position at all (it's hidden
		// instead - see repositionNumberNorth()), but it does hide it, so
		// unhiding it unconditionally here is required for NORTH and a
		// harmless no-op for every other orientation, which never hide it.
		numberWidget.setHidden(false);
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

	private BufferedImage getRotatedSouthBacking(int spriteId)
	{
		if (rotatedSouthBacking != null && rotatedSouthBackingSpriteId == spriteId)
		{
			return rotatedSouthBacking;
		}

		BufferedImage source = spriteManager.getSprite(spriteId, 0);
		if (source == null)
		{
			return null;
		}

		rotatedSouthBacking = rotateCounterClockwise90(source);
		rotatedSouthBackingBodyRadius = source.getHeight() / 2;
		rotatedSouthBackingSpriteId = spriteId;
		return rotatedSouthBacking;
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

	private BufferedImage getRotatedNorthBacking(int spriteId)
	{
		if (rotatedNorthBacking != null && rotatedNorthBackingSpriteId == spriteId)
		{
			return rotatedNorthBacking;
		}

		BufferedImage source = spriteManager.getSprite(spriteId, 0);
		if (source == null)
		{
			return null;
		}

		rotatedNorthBacking = rotateClockwise90(source);
		rotatedNorthBackingBodyRadius = source.getHeight() / 2;
		rotatedNorthBackingSpriteId = spriteId;
		return rotatedNorthBacking;
	}

	/**
	 * Mirror image of rotateCounterClockwise90() - 90° the OTHER way, so
	 * the tab (naturally pointing left, small x) lands near the TOP of the
	 * result instead of the bottom: pixel mapping (x,y) -> (h-1-y, x) for
	 * a source of height h, giving "tab pointing left" -> "tab pointing
	 * up", the NORTH counterpart to CCW90's "tab pointing down".
	 */
	private static BufferedImage rotateClockwise90(BufferedImage source)
	{
		int w = source.getWidth();
		int h = source.getHeight();

		BufferedImage rotated = new BufferedImage(h, w, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = rotated.createGraphics();
		g.drawImage(source, new AffineTransform(0, 1, -1, 0, h - 1, 0), null);
		g.dispose();

		return rotated;
	}

	/**
	 * Draws the rotated collar for NORTH - counterpart to
	 * drawRotatedSouthBacking(), mirrored vertically: the body (diameter =
	 * source height, same as SOUTH) now sits at the BOTTOM of the rotated
	 * image instead of the top (since the tab - pointing up this time -
	 * occupies the top portion instead), so drawY is anchored from the
	 * bottom of the rotated image instead of the top.
	 */
	private void drawRotatedNorthBacking(Graphics2D graphics, Widget icon, Widget backing, int iconCenterX, int iconCenterY)
	{
		if (icon == null || backing == null)
		{
			return;
		}

		BufferedImage rotated = getRotatedNorthBacking(backing.getSpriteId());
		if (rotated == null)
		{
			return;
		}

		int drawX = iconCenterX - rotated.getWidth() / 2 + NORTH_COLLAR_X_NUDGE;
		int drawY = iconCenterY - rotated.getHeight() + rotatedNorthBackingBodyRadius - COLLAR_VERTICAL_NUDGE + NORTH_COLLAR_Y_NUDGE;
		graphics.drawImage(rotated, drawX, drawY, null);
	}

	/**
	 * Mirror image of getRotatedSouthBacking() - same cache-by-sprite-id
	 * shape, a pure horizontal flip instead of a 90° rotation. The source
	 * sprite's tab naturally points LEFT (unrotated) - exactly right for
	 * WEST already, which is why that direction never calls this at all
	 * and just leaves the real backing alone. EAST needs the tab pointing
	 * the other way, hence the flip.
	 */
	private BufferedImage getFlippedEastBacking(int spriteId)
	{
		if (flippedEastBacking != null && flippedEastBackingSpriteId == spriteId)
		{
			return flippedEastBacking;
		}

		BufferedImage source = spriteManager.getSprite(spriteId, 0);
		if (source == null)
		{
			return null;
		}

		flippedEastBacking = flipHorizontal(source);
		flippedEastBackingBodyRadius = source.getHeight() / 2;
		flippedEastBackingSpriteId = spriteId;
		return flippedEastBacking;
	}

	/** Plain horizontal mirror - dimensions unchanged, just x -> w-1-x. */
	private static BufferedImage flipHorizontal(BufferedImage source)
	{
		int w = source.getWidth();
		int h = source.getHeight();

		BufferedImage flipped = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = flipped.createGraphics();
		g.drawImage(source, new AffineTransform(-1, 0, 0, 1, w - 1, 0), null);
		g.dispose();

		return flipped;
	}

	/**
	 * Draws the flipped collar for EAST - counterpart to
	 * drawRotatedSouthBacking(), but simpler: a pure flip doesn't swap
	 * width/height the way a 90° rotation does, so the body (diameter =
	 * source height, same 57x34 sprite as SOUTH) still fills the full
	 * height edge-to-edge and now sits in the LEFT half of the image
	 * (post-flip) rather than centered in the whole bounding box. drawX is
	 * therefore anchored so the body's own center - not the image's -
	 * lands on iconCenterX, leaving the tab to protrude rightward from
	 * there. The caller has already hidden the real backing widget before
	 * calling this (see renderEast()).
	 */
	private void drawFlippedEastBacking(Graphics2D graphics, Widget icon, Widget backing, int iconCenterX, int iconCenterY)
	{
		if (icon == null || backing == null)
		{
			return;
		}

		BufferedImage flipped = getFlippedEastBacking(backing.getSpriteId());
		if (flipped == null)
		{
			return;
		}

		int drawX = iconCenterX - flippedEastBackingBodyRadius + EAST_COLLAR_X_NUDGE;
		int drawY = iconCenterY - flipped.getHeight() / 2;
		graphics.drawImage(flipped, drawX, drawY, null);
	}

	/**
	 * Background + colored current/max fill, confined to the inner
	 * capsule, filling along Y. anchorBottom controls which edge is the
	 * FIXED one (the end farthest from the icon): true for SOUTH (icon is
	 * above, far end is at the bottom), false for NORTH (icon is below,
	 * far end is at the top). Either way the bar always empties starting
	 * from the near-icon end first - the remaining "liquid" recedes toward
	 * the far end as the value drops, never the reverse.
	 */
	private void drawVial(Graphics2D graphics, int frameWidth, int innerWidth, int innerHeight, int innerArc, int current, int max, boolean anchorBottom)
	{
		Graphics2D vialGraphics = (Graphics2D) graphics.create();
		vialGraphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, barOpacityFraction()));
		vialGraphics.translate(frameWidth, frameWidth);
		vialGraphics.setClip(new RoundRectangle2D.Float(0, 0, innerWidth, innerHeight, innerArc, innerArc));

		vialGraphics.setColor(new Color(0, 0, 0, BACKGROUND_ALPHA));
		vialGraphics.fillRect(0, 0, innerWidth, innerHeight);

		double ratio = (double) current / max;
		int fillHeight = (int) Math.round(ratio * innerHeight);
		vialGraphics.setColor(fillColor());
		int fillY = (anchorBottom ^ reverseBarFill()) ? innerHeight - fillHeight : 0;
		vialGraphics.fillRect(0, fillY, innerWidth, fillHeight);

		vialGraphics.dispose();
	}

	/**
	 * Draws both the regen and drain outlines in whichever order makes the
	 * dominant one land on top - called from all four orientations instead
	 * of each one invoking the two outlines separately.
	 */
	private void drawVialOutlines(Graphics2D graphics, int frameWidth, int innerWidth, int innerHeight, int innerArc, boolean anchorBottom)
	{
		if (regenDrainPriority() == CompactOrbsConfig.RegenDrainPriority.DRAIN)
		{
			drawVialRegenOutline(graphics, frameWidth, innerWidth, innerHeight, innerArc, anchorBottom);
			drawVialDrainOutline(graphics, frameWidth, innerWidth, innerHeight, innerArc, anchorBottom);
		}
		else
		{
			drawVialDrainOutline(graphics, frameWidth, innerWidth, innerHeight, innerArc, anchorBottom);
			drawVialRegenOutline(graphics, frameWidth, innerWidth, innerHeight, innerArc, anchorBottom);
		}
	}

	/** Horizontal counterpart to drawVialOutlines(). */
	private void drawVialOutlinesHorizontal(Graphics2D graphics, int frameWidth, int innerLength, int innerThickness, int innerArc, boolean anchorRight)
	{
		if (regenDrainPriority() == CompactOrbsConfig.RegenDrainPriority.DRAIN)
		{
			drawVialRegenOutlineHorizontal(graphics, frameWidth, innerLength, innerThickness, innerArc, anchorRight);
			drawVialDrainOutlineHorizontal(graphics, frameWidth, innerLength, innerThickness, innerArc, anchorRight);
		}
		else
		{
			drawVialDrainOutlineHorizontal(graphics, frameWidth, innerLength, innerThickness, innerArc, anchorRight);
			drawVialRegenOutlineHorizontal(graphics, frameWidth, innerLength, innerThickness, innerArc, anchorRight);
		}
	}

	/**
	 * Regen-progress outline traced along the INSIDE of the vial cavity
	 * (not the frame ring - that was tried and dropped, read as a
	 * progression meter worse than this) - a thin stroke hugging the inner
	 * rounded-rect's own boundary, revealed only up to the current
	 * percentage, growing from the near-icon end first exactly like
	 * drawVial()'s own anchorBottom rule, so it tracks the same direction
	 * the vial itself fills. No-op whenever this bar has regen disabled or
	 * nothing to report (regenPercentage() == null).
	 */
	private void drawVialRegenOutline(Graphics2D graphics, int frameWidth, int innerWidth, int innerHeight, int innerArc, boolean anchorBottom)
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

		drawOutline(graphics, frameWidth, innerWidth, innerHeight, innerArc, anchorBottom ^ reverseRegenRing(), percentage, regenHighlightColor(), regenOpacityFraction());
	}

	/** Horizontal counterpart to drawVialRegenOutline() - grows along X instead of Y, mirroring drawVialHorizontal()'s anchorRight rule. */
	private void drawVialRegenOutlineHorizontal(Graphics2D graphics, int frameWidth, int innerLength, int innerThickness, int innerArc, boolean anchorRight)
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

		drawOutlineHorizontal(graphics, frameWidth, innerLength, innerThickness, innerArc, anchorRight ^ reverseRegenRing(), percentage, regenHighlightColor(), regenOpacityFraction());
	}

	/**
	 * Drain-progress outline - the mirror image of the regen outline: starts
	 * at full coverage and shrinks back toward the near-icon end as
	 * drainPercentage() counts down, instead of growing from it. Drawn
	 * identically to the regen outline otherwise (see drawOutline()); which
	 * one lands on top when both are visible is decided by
	 * drawVialOutlines(), not here. No-op whenever this bar has drain
	 * disabled or nothing to report (drainPercentage() == null).
	 */
	private void drawVialDrainOutline(Graphics2D graphics, int frameWidth, int innerWidth, int innerHeight, int innerArc, boolean anchorBottom)
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

		drawOutline(graphics, frameWidth, innerWidth, innerHeight, innerArc, anchorBottom ^ reverseDrainRing(), percentage, drainHighlightColor(), drainOpacityFraction());
	}

	/** Horizontal counterpart to drawVialDrainOutline() - grows along X instead of Y, mirroring drawVialHorizontal()'s anchorRight rule. */
	private void drawVialDrainOutlineHorizontal(Graphics2D graphics, int frameWidth, int innerLength, int innerThickness, int innerArc, boolean anchorRight)
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

		drawOutlineHorizontal(graphics, frameWidth, innerLength, innerThickness, innerArc, anchorRight ^ reverseDrainRing(), percentage, drainHighlightColor(), drainOpacityFraction());
	}

	/** Shared clip-and-stroke primitive behind both drawVialRegenOutline() and drawVialDrainOutline() - only what's visually distinct (percentage, color, opacity) differs between the two. */
	private void drawOutline(Graphics2D graphics, int frameWidth, int innerWidth, int innerHeight, int innerArc, boolean anchorBottom, double percentage, Color color, float opacityFraction)
	{
		int growthHeight = (int) Math.round(percentage * innerHeight);
		int growthY = anchorBottom ? innerHeight - growthHeight : 0;

		Graphics2D outlineGraphics = (Graphics2D) graphics.create();
		outlineGraphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacityFraction));
		outlineGraphics.translate(frameWidth, frameWidth);
		outlineGraphics.clip(new Rectangle(0, growthY, innerWidth, growthHeight));
		outlineGraphics.setColor(color);
		outlineGraphics.setStroke(new BasicStroke(Math.max(1, regenDrainRingSize())));
		outlineGraphics.draw(new RoundRectangle2D.Float(0, 0, innerWidth - 1, innerHeight - 1, innerArc, innerArc));
		outlineGraphics.dispose();
	}

	/** Horizontal counterpart to drawOutline() - grows along X instead of Y. */
	private void drawOutlineHorizontal(Graphics2D graphics, int frameWidth, int innerLength, int innerThickness, int innerArc, boolean anchorRight, double percentage, Color color, float opacityFraction)
	{
		int growthLength = (int) Math.round(percentage * innerLength);
		int growthX = anchorRight ? innerLength - growthLength : 0;

		Graphics2D outlineGraphics = (Graphics2D) graphics.create();
		outlineGraphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacityFraction));
		outlineGraphics.translate(frameWidth, frameWidth);
		outlineGraphics.clip(new Rectangle(growthX, 0, growthLength, innerThickness));
		outlineGraphics.setColor(color);
		outlineGraphics.setStroke(new BasicStroke(Math.max(1, regenDrainRingSize())));
		outlineGraphics.draw(new RoundRectangle2D.Float(0, 0, innerLength - 1, innerThickness - 1, innerArc, innerArc));
		outlineGraphics.dispose();
	}

	/**
	 * Horizontal counterpart to drawVial() - fills along X instead of Y.
	 * anchorRight controls which edge is the FIXED one (the end farthest
	 * from the icon): true for EAST (far end is on the right), false for
	 * WEST (far end is on the left). Matches drawVial()'s own rule
	 * exactly, just rotated 90°.
	 */
	private void drawVialHorizontal(Graphics2D graphics, int frameWidth, int innerLength, int innerThickness, int innerArc, int current, int max, boolean anchorRight)
	{
		Graphics2D vialGraphics = (Graphics2D) graphics.create();
		vialGraphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, barOpacityFraction()));
		vialGraphics.translate(frameWidth, frameWidth);
		vialGraphics.setClip(new RoundRectangle2D.Float(0, 0, innerLength, innerThickness, innerArc, innerArc));

		vialGraphics.setColor(new Color(0, 0, 0, BACKGROUND_ALPHA));
		vialGraphics.fillRect(0, 0, innerLength, innerThickness);

		double ratio = (double) current / max;
		int fillLength = (int) Math.round(ratio * innerLength);
		vialGraphics.setColor(fillColor());
		int fillX = (anchorRight ^ reverseBarFill()) ? innerLength - fillLength : 0;
		vialGraphics.fillRect(fillX, 0, fillLength, innerThickness);

		vialGraphics.dispose();
	}
}
