package com.viciousliqx.viciousbars;

import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.events.GameTick;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.eventbus.Subscribe;
import lombok.extern.slf4j.Slf4j;

/**
 * TEMPORARY - pure read-only diagnostic, no behavior change. Logs the real
 * canvas position/size of every widget in the minimap/orbs cluster once per
 * tick (to compare a manual Alt+drag's before/after), plus each one's real
 * drag-mechanics fields (getDragParent()/getDragDeadZone()/getDragDeadTime())
 * - VarbitChanged and VarClientInt/StrChanged sweeps both came back negative
 * (nothing changed in a way that correlated with the drag, only incidental
 * UI-rebuild side effects), so this checks the other real, confirmed-to-exist
 * drag-specific API surface instead: which widget (if any) is actually
 * configured with a non-null drag parent reveals which one the client
 * considers "the draggable one." Delete this class and its wiring lines in
 * ViciousBarsPlugin once done with it.
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
		{InterfaceID.ToplevelPreEoc.GAMEFRAME, 5},
		{InterfaceID.ToplevelPreEoc.TLI_LISTENER, 6},
		{InterfaceID.ToplevelPreEoc.CONTROL, 7},
	};

	private static final String[] NAMES = {
		"MAP_CONTAINER", "ORBS", "MAP_MINIMAP", "MINIMAP", "COMPASSCLICK",
		"GAMEFRAME", "TLI_LISTENER", "CONTROL",
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
			Widget dragParent = w.getDragParent();
			log.debug("minimap drag: {} canvas={} size={}x{} hidden={} dragParent={} deadZone={} deadTime={}",
				NAMES[nameIndex], loc, w.getWidth(), w.getHeight(), w.isHidden(),
				dragParent == null ? "null" : dragParent.getId(),
				w.getDragDeadZone(), w.getDragDeadTime());
		}
	}
}
