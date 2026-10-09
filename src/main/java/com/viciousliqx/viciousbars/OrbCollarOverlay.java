package com.viciousliqx.viciousbars;

import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

/**
 * Draws every bar's collar (the rotated real-backing replacement for
 * SOUTH/EAST/NORTH - see VialOverlay#drawCollar()) in one pass, separate
 * from and after all four bars' own frame/vial. Collar-drawing used to
 * happen inline inside each bar's own render() - confirmed live
 * (screenshot) that left one bar's Z-order relative to a DIFFERENT bar's
 * collar depending on arbitrary overlay registration order rather than
 * what's actually on screen, so e.g. one bar's frame could cover another
 * bar's collar depending purely on injection order. Since OverlayPriority
 * applies to a whole Overlay instance and all four bars share the same
 * VialOverlay class (so they can't each carry a different priority for
 * just their collar), this is a second overlay instead, given a higher
 * priority than the default the four bars use - guaranteeing every
 * collar draws after every bar's frame/vial, regardless of which orb
 * either belongs to.
 */
@Singleton
class OrbCollarOverlay extends Overlay
{
	private final HpVialOverlay hpVialOverlay;
	private final PrayerVialOverlay prayerVialOverlay;
	private final SpecVialOverlay specVialOverlay;
	private final RunVialOverlay runVialOverlay;

	@Inject
	private OrbCollarOverlay(HpVialOverlay hpVialOverlay, PrayerVialOverlay prayerVialOverlay, SpecVialOverlay specVialOverlay, RunVialOverlay runVialOverlay)
	{
		this.hpVialOverlay = hpVialOverlay;
		this.prayerVialOverlay = prayerVialOverlay;
		this.specVialOverlay = specVialOverlay;
		this.runVialOverlay = runVialOverlay;
		setPosition(OverlayPosition.DYNAMIC);
		setMovable(false);
		setPriority(OverlayPriority.HIGH);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		hpVialOverlay.drawCollar(graphics);
		prayerVialOverlay.drawCollar(graphics);
		specVialOverlay.drawCollar(graphics);
		runVialOverlay.drawCollar(graphics);
		return null;
	}
}
