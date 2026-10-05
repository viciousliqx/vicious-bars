package com.viciousliqx.viciousbars;

import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.eventbus.Subscribe;
import lombok.extern.slf4j.Slf4j;

/**
 * TEMPORARY - pure read-only diagnostic, no behavior change. Logs the real
 * canvas position/size of every widget in the minimap/orbs cluster once per
 * tick (to compare a manual Alt+drag's before/after), and separately logs
 * every VarPlayer/Varbit change (same sniffing technique that found Special
 * Attack's queued-state VarPlayer earlier) - since the dragged position
 * survives a full client restart, it must be stored somewhere durable, and
 * a VarPlayer is the obvious real candidate. Delete this class and its two
 * wiring lines in ViciousBarsPlugin once done with it.
 */
@Slf4j
@Singleton
class MinimapDragDiagnostic
{
	private static final int[][] WIDGETS = {
		{InterfaceID.ToplevelPreEoc.MAP_CONTAINER, 0},
		{InterfaceID.ToplevelPreEoc.ORBS, 1},
		{InterfaceID.ToplevelPreEoc.MAP_MINIMAP, 2},
		{InterfaceID.ToplevelPreEoc.MINIMAP, 3},
		{InterfaceID.ToplevelPreEoc.COMPASSCLICK, 4},
		{InterfaceID.ToplevelPreEoc.MAP_MINIMAP_GRAPHIC6, 5},
		{InterfaceID.ToplevelPreEoc.MAP_MINIMAP_GRAPHIC9, 6},
		{InterfaceID.Orbs.ORB_HEALTH, 8},
		{InterfaceID.Orbs.ORB_PRAYER, 9},
	};

	private static final String[] NAMES = {
		"MAP_CONTAINER", "ORBS", "MAP_MINIMAP", "MINIMAP", "COMPASSCLICK",
		"MAP_MINIMAP_GRAPHIC6", "MAP_MINIMAP_GRAPHIC9", "XP_DROPS",
		"ORB_HEALTH", "ORB_PRAYER",
	};

	@Inject
	private Client client;

	@Subscribe
	public void onGameTick(GameTick event)
	{
		for (int[] entry : WIDGETS)
		{
			int componentId = entry[0];
			int nameIndex = entry[1];
			Widget w = client.getWidget(componentId);
			if (w == null)
			{
				log.debug("minimap drag: {} = null", NAMES[nameIndex]);
				continue;
			}
			Point loc = w.getCanvasLocation();
			log.debug("minimap drag: {} canvas={} size={}x{} hidden={}",
				NAMES[nameIndex], loc, w.getWidth(), w.getHeight(), w.isHidden());
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		log.debug("minimap drag varp: varpId={} varbitId={} value={}",
			event.getVarpId(), event.getVarbitId(), event.getValue());
	}
}
