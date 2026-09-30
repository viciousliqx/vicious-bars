package com.viciousliqx.viciousbars;

import java.awt.Color;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.Skill;
import net.runelite.api.Varbits;
import net.runelite.client.game.SkillIconManager;

@Slf4j
@Singleton
class PrayerBarOverlay extends VitalBarOverlay
{
	private static final Color COLOR = new Color(60, 170, 220);

	// Verified against the real "Movable Orbs" Hub plugin (anonyser/runelite-movable-orbs).
	private static final int PRAYER_ORB_COMPONENT = (160 << 16) | 18;

	private final Client client;
	private final ViciousBarsConfig config;
	private final SkillIconManager skillIconManager;

	@Inject
	private PrayerBarOverlay(Client client, ViciousBarsConfig config, SkillIconManager skillIconManager)
	{
		super(client);
		this.client = client;
		this.config = config;
		this.skillIconManager = skillIconManager;
	}

	@Override
	protected boolean shouldRender()
	{
		return config.prayerEnabled();
	}

	@Override
	protected PlacementMode getPlacementMode()
	{
		return config.prayerPlacementMode();
	}

	@Override
	protected DockSide getDockSide()
	{
		return config.prayerDockSide();
	}

	@Override
	protected int getDockSlot()
	{
		return config.prayerDockSlot();
	}

	@Override
	protected int getDockPadding()
	{
		return config.dockPadding();
	}

	@Override
	protected int getCurrent()
	{
		return client.getBoostedSkillLevel(Skill.PRAYER);
	}

	@Override
	protected int getMax()
	{
		return client.getRealSkillLevel(Skill.PRAYER);
	}

	@Override
	protected Color getColor()
	{
		return COLOR;
	}

	@Override
	protected boolean isShowIcon()
	{
		return config.prayerShowIcon();
	}

	@Override
	protected BufferedImage getIcon()
	{
		// No orb icon sprite exists for Prayer (confirmed against RuneLite's own
		// Status Bars source, which falls back to the skill icon for the same reason).
		return skillIconManager.getSkillImage(Skill.PRAYER, true);
	}

	@Override
	protected boolean isShowCounter()
	{
		return config.prayerShowCounter();
	}

	@Override
	protected Justification getJustification()
	{
		return config.prayerJustification();
	}

	@Override
	protected Orientation getOrientation()
	{
		return config.prayerFloatingOrientation();
	}

	@Override
	protected int getThickness()
	{
		return config.prayerThickness();
	}

	@Override
	protected int getLength()
	{
		return config.prayerLength();
	}

	@Override
	protected OrbMode getOrbMode()
	{
		return config.prayerOrbMode();
	}

	@Override
	protected void onOrbActivate()
	{
		// Verified against the real "prayer-manager" plugin (MGreenwood), whose
		// whole premise is an overlay button mirroring the prayer orb's click.
		boolean active = client.getVarbitValue(Varbits.QUICK_PRAYER) != 0;
		String option = active ? "Deactivate" : "Activate";
		net.runelite.api.widgets.Widget widget = client.getWidget(PRAYER_ORB_COMPONENT);
		log.debug("onOrbActivate: active={} option={} component={} widget={} actions={}",
			active, option, PRAYER_ORB_COMPONENT, widget,
			widget != null ? java.util.Arrays.toString(widget.getActions()) : "n/a");
		client.menuAction(0, PRAYER_ORB_COMPONENT, MenuAction.CC_OP, 0, -1, option, "Quick-prayers");
		log.debug("onOrbActivate: menuAction returned without throwing");
	}
}
