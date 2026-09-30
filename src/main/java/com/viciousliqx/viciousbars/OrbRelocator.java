package com.viciousliqx.viciousbars;

import java.util.HashMap;
import java.util.Map;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetPositionMode;

/**
 * Moves a vanilla orb widget to sit at our bar's icon position (Orb mode:
 * Relocated), and restores it to wherever it was before we touched it.
 *
 * Mechanism verified against the real, published "Movable Orbs" Hub plugin
 * (anonyser/runelite-movable-orbs) - setXPositionMode/setYPositionMode to
 * ABSOLUTE_LEFT/ABSOLUTE_TOP, then setOriginalX/setOriginalY + revalidate().
 * No custom click handling needed: the widget is still the real one, so its
 * native click behavior (quick-prayers, run toggle, etc.) keeps working
 * automatically at its new position.
 */
@Singleton
class OrbRelocator
{
	private static class Original
	{
		final int xPositionMode;
		final int yPositionMode;
		final int originalX;
		final int originalY;

		Original(Widget w)
		{
			this.xPositionMode = w.getXPositionMode();
			this.yPositionMode = w.getYPositionMode();
			this.originalX = w.getOriginalX();
			this.originalY = w.getOriginalY();
		}

		void applyTo(Widget w)
		{
			w.setXPositionMode(xPositionMode);
			w.setYPositionMode(yPositionMode);
			w.setOriginalX(originalX);
			w.setOriginalY(originalY);
			w.revalidate();
		}
	}

	private final Map<Integer, Original> originals = new HashMap<>();

	/** Moves the widget to (x, y), capturing its pre-move position the first time. */
	void relocate(Client client, int componentId, int x, int y)
	{
		Widget widget = client.getWidget(componentId);
		if (widget == null)
		{
			return;
		}

		originals.computeIfAbsent(componentId, id -> new Original(widget));

		widget.setXPositionMode(WidgetPositionMode.ABSOLUTE_LEFT);
		widget.setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP);
		widget.setOriginalX(x);
		widget.setOriginalY(y);
		widget.revalidate();
	}

	/** No-op if this widget was never relocated (or already released). */
	void release(Client client, int componentId)
	{
		Original original = originals.remove(componentId);
		if (original == null)
		{
			return;
		}

		Widget widget = client.getWidget(componentId);
		if (widget != null)
		{
			original.applyTo(widget);
		}
	}

	void releaseAll(Client client)
	{
		for (Integer componentId : originals.keySet().toArray(new Integer[0]))
		{
			release(client, componentId);
		}
	}
}
