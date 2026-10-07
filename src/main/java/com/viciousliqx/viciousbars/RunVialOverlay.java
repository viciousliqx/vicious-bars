package com.viciousliqx.viciousbars;

import com.compactorbs.CompactOrbsConfig;
import com.compactorbs.CompactOrbsManager;
import com.compactorbs.widget.TargetWidget;
import com.compactorbs.widget.WidgetManager;
import com.compactorbs.widget.elements.Orbs;
import java.awt.Color;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.client.game.SpriteManager;

/**
 * Run energy's vial bar - see VialOverlay for all the shared logic.
 * Confirmed via F8's widget dump that Run's native geometry is
 * pixel-identical to HP's (same backing sprite 1071, icon/number at the
 * same relative positions). client.getEnergy() returns 0-10000 (not
 * 0-100 as first assumed) - confirmed live: without the /100, the bar
 * read as permanently full, since any real value over 100 just clamped
 * to max every frame.
 */
@Singleton
class RunVialOverlay extends VialOverlay
{
	private static final Color FILL_COLOR = new Color(220, 160, 40);

	@Inject
	private RunVialOverlay(Client client, CompactOrbsManager manager, CompactOrbsConfig config, WidgetManager widgetManager, SpriteManager spriteManager)
	{
		super(client, manager, config, widgetManager, spriteManager);
	}

	@Override
	protected TargetWidget orbContainer()
	{
		return Orbs.RUN_ORB_CONTAINER;
	}

	@Override
	protected int currentValue()
	{
		return client.getEnergy() / 100;
	}

	@Override
	protected int maxValue()
	{
		return 100;
	}

	@Override
	protected Color fillColor()
	{
		return FILL_COLOR;
	}
}
