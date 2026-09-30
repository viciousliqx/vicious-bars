package com.viciousliqx.viciousbars;

import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.input.MouseListener;

/**
 * Hit-tests real mouse clicks against each orb-bearing bar's current icon
 * rectangle, and invokes the bar's action when Orb mode is Merged or
 * Mirrored. Vanilla bars never register a hit (their orbMode check fails) so
 * clicks pass through untouched.
 */
@Slf4j
@Singleton
class OrbClickListener implements MouseListener
{
	private final VitalBarOverlay[] orbBars;

	@Inject
	private OrbClickListener(HpBarOverlay hp, PrayerBarOverlay prayer, SpecBarOverlay spec, RunBarOverlay run)
	{
		this.orbBars = new VitalBarOverlay[] { hp, prayer, spec, run };
	}

	@Override
	public MouseEvent mouseClicked(MouseEvent event)
	{
		log.debug("OrbClickListener.mouseClicked at {}", event.getPoint());
		for (VitalBarOverlay bar : orbBars)
		{
			OrbMode mode = bar.getOrbMode();
			Rectangle bounds = bar.getIconBounds();
			log.debug("  {} mode={} iconBounds={}", bar.getClass().getSimpleName(), mode, bounds);

			if (mode != OrbMode.MERGED && mode != OrbMode.MIRRORED)
			{
				continue;
			}

			if (bounds != null && bounds.contains(event.getPoint()))
			{
				log.debug("  HIT - activating {}", bar.getClass().getSimpleName());
				bar.onOrbActivate();
				return null;
			}
		}

		return event;
	}

	@Override
	public MouseEvent mousePressed(MouseEvent event)
	{
		return event;
	}

	@Override
	public MouseEvent mouseReleased(MouseEvent event)
	{
		return event;
	}

	@Override
	public MouseEvent mouseEntered(MouseEvent event)
	{
		return event;
	}

	@Override
	public MouseEvent mouseExited(MouseEvent event)
	{
		return event;
	}

	@Override
	public MouseEvent mouseDragged(MouseEvent event)
	{
		return event;
	}

	@Override
	public MouseEvent mouseMoved(MouseEvent event)
	{
		return event;
	}
}
