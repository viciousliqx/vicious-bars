package com.viciousliqx.viciousbars;

import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.input.MouseListener;

/**
 * Hit-tests real mouse clicks against each orb-bearing bar's current icon
 * rectangle, and invokes the bar's action when Orb mode is Merged or
 * Mirrored. Vanilla bars never register a hit (their orbMode check fails) so
 * clicks pass through untouched.
 *
 * mouseClicked runs on the AWT event thread, not RuneLite's client thread -
 * calling client.* from here directly throws (confirmed: "AssertionError:
 * must be called on client thread"), so the actual activation is deferred via
 * ClientThread, same as Movable Orbs does for its own widget mutations.
 */
@Slf4j
@Singleton
class OrbClickListener implements MouseListener
{
	private final ClientThread clientThread;
	private final VitalBarOverlay[] orbBars;

	@Inject
	private OrbClickListener(ClientThread clientThread, HpBarOverlay hp, PrayerBarOverlay prayer, SpecBarOverlay spec, RunBarOverlay run)
	{
		this.clientThread = clientThread;
		this.orbBars = new VitalBarOverlay[] { hp, prayer, spec, run };
	}

	@Override
	public MouseEvent mouseClicked(MouseEvent event)
	{
		for (VitalBarOverlay bar : orbBars)
		{
			OrbMode mode = bar.getOrbMode();
			if (mode != OrbMode.MERGED && mode != OrbMode.MIRRORED)
			{
				continue;
			}

			Rectangle bounds = bar.getIconBounds();
			if (bounds != null && bounds.contains(event.getPoint()))
			{
				log.debug("hit {} at {}, bounds={}", bar.getClass().getSimpleName(), event.getPoint(), bounds);
					clientThread.invoke(bar::onOrbActivate);
				// Verified against MouseManager's real source: it never null-checks
				// a listener's return value, it checks isConsumed() - returning null
				// here throws a NullPointerException on the very next listener in
				// the chain, every time. Consume + return the event, don't null it.
				event.consume();
				return event;
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
