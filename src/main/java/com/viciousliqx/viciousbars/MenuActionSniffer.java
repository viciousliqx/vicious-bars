package com.viciousliqx.viciousbars;

import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.eventbus.Subscribe;

/**
 * TEMPORARY diagnostic - logs every real menu action's exact parameters so we
 * can capture the REAL, verified values for clicking the vanilla Prayer orb
 * (manually, by the user) instead of continuing to guess at CC_OP parameters.
 * Remove once Prayer's onOrbActivate() is confirmed working.
 *
 * Also captures the special attack orb's component id (child 36) clicks and,
 * for a short window afterward, every varbit that changes - so we can spot
 * whichever one flips when special attack becomes "queued" (armed for the
 * next hit). Remove once that varbit is confirmed and wired into SpecBarOverlay.
 */
@Slf4j
@Singleton
class MenuActionSniffer
{
	private static final int SPECIAL_ATTACK_ORB_COMPONENT = (160 << 16) | 36;
	private static final int FOCUS_WINDOW_TICKS = 3;

	private int focusTicksRemaining;

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		log.debug("MenuOptionClicked: action={} id={} param0={} param1={} option='{}' target='{}' itemId={} widget={}",
			event.getMenuAction(), event.getId(), event.getParam0(), event.getParam1(),
			event.getMenuOption(), event.getMenuTarget(), event.getItemId(), event.getWidget());

		if (event.getParam1() == SPECIAL_ATTACK_ORB_COMPONENT)
		{
			log.debug("SpecialAttackSniffer: spec orb clicked, watching varbit changes for the next {} ticks", FOCUS_WINDOW_TICKS);
			focusTicksRemaining = FOCUS_WINDOW_TICKS;
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (focusTicksRemaining > 0)
		{
			focusTicksRemaining--;
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		if (focusTicksRemaining > 0)
		{
			log.debug("SpecialAttackSniffer: varbitId={} varpId={} value={}",
				event.getVarbitId(), event.getVarpId(), event.getValue());
		}
	}
}
