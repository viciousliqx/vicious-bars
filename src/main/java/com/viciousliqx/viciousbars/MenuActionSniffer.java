package com.viciousliqx.viciousbars;

import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.eventbus.Subscribe;

/**
 * TEMPORARY diagnostic - logs every real menu action's exact parameters so we
 * can capture the REAL, verified values for clicking the vanilla Prayer orb
 * (manually, by the user) instead of continuing to guess at CC_OP parameters.
 * Remove once Prayer's onOrbActivate() is confirmed working.
 */
@Slf4j
@Singleton
class MenuActionSniffer
{
	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		log.debug("MenuOptionClicked: action={} id={} param0={} param1={} option='{}' target='{}' itemId={} widget={}",
			event.getMenuAction(), event.getId(), event.getParam0(), event.getParam1(),
			event.getMenuOption(), event.getMenuTarget(), event.getItemId(), event.getWidget());
	}
}
