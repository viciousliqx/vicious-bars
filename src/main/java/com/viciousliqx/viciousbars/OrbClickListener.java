package com.viciousliqx.viciousbars;

import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.input.MouseListener;

/**
 * Hit-tests real mouse clicks against each orb-bearing bar's current icon
 * rectangle when Orb mode is Merged or Mirrored. Left click invokes the
 * bar's primary action ({@link VitalBarOverlay#onOrbActivate()}, same as the
 * real orb's left click); right click invokes its secondary action
 * ({@link VitalBarOverlay#onOrbSecondaryActivate()}, e.g. Prayer's "Set
 * Quick-prayers" - a no-op for bars without one). Vanilla bars never
 * register a hit (their orbMode check fails) so clicks pass through
 * untouched.
 *
 * Consumes on BOTH mousePressed and mouseClicked - OSRS's click-to-walk
 * appears to react to the press, not the synthesized "clicked" event, so
 * consuming only mouseClicked was too late to stop movement underneath the
 * icon (confirmed: clicking the icon still walked the player there).
 *
 * The actual activation runs via ClientThread.invoke() - mouseClicked/
 * mousePressed run on the AWT event thread, not RuneLite's client thread,
 * and client.* calls assert they're only ever called from the client thread.
 */
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

	private VitalBarOverlay hitOrbBar(MouseEvent event)
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
				return bar;
			}
		}

		return null;
	}

	@Override
	public MouseEvent mousePressed(MouseEvent event)
	{
		if (hitOrbBar(event) != null)
		{
			// Verified against MouseManager's real source: it never null-checks
			// a listener's return value, it checks isConsumed() - returning null
			// here throws a NullPointerException on the very next listener in
			// the chain, every time. Consume + return the event, don't null it.
			event.consume();
		}
		return event;
	}

	@Override
	public MouseEvent mouseClicked(MouseEvent event)
	{
		VitalBarOverlay bar = hitOrbBar(event);
		if (bar != null)
		{
			if (event.getButton() == MouseEvent.BUTTON1)
			{
				clientThread.invoke(bar::onOrbActivate);
			}
			else if (event.getButton() == MouseEvent.BUTTON3)
			{
				clientThread.invoke(bar::onOrbSecondaryActivate);
			}
			event.consume();
		}
		return event;
	}

	@Override
	public MouseEvent mouseReleased(MouseEvent event)
	{
		if (hitOrbBar(event) != null)
		{
			event.consume();
		}
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
