package com.viciousliqx.viciousbars;

import java.awt.Color;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.Skill;
import net.runelite.api.VarPlayer;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.SpriteID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.plugins.itemstats.Effect;
import net.runelite.client.plugins.itemstats.ItemStatChangesService;
import net.runelite.client.plugins.itemstats.StatChange;

@Singleton
class HpBarOverlay extends VitalBarOverlay
{
	private static final Color NORMAL = new Color(220, 40, 40);
	private static final Color POISONED = new Color(90, 170, 60);
	private static final Color ENVENOMED = new Color(60, 120, 40);
	private static final Color DISEASED = new Color(150, 110, 40);

	// InterfaceID.Orbs.HEALTHBUTTON - confirmed by decompiling the real
	// runelite-api jar to match exactly the component captured from a real
	// MenuOptionClicked event while poisoned: action=CC_OP id=1 param0=-1
	// option='Cure' target=''. The orb has no click action at all while not
	// poisoned - that's real vanilla behavior (not something we gate
	// ourselves), so firing this when not poisoned relies on the game's own
	// script behind this component being a no-op, same as every other orb.
	private static final int HP_ORB_COMPONENT = InterfaceID.Orbs.HEALTHBUTTON;

	private final Client client;
	private final ViciousBarsConfig config;
	private final SpriteManager spriteManager;
	private final ItemStatChangesService itemStatService;

	@Inject
	private HpBarOverlay(Client client, ViciousBarsConfig config, SpriteManager spriteManager, ItemStatChangesService itemStatService)
	{
		super(client);
		this.client = client;
		this.config = config;
		this.spriteManager = spriteManager;
		this.itemStatService = itemStatService;
	}

	@Override
	protected boolean shouldRender()
	{
		return config.hpEnabled();
	}

	@Override
	protected PlacementMode getPlacementMode()
	{
		return config.hpPlacementMode();
	}

	@Override
	protected DockSide getDockSide()
	{
		return config.hpDockSide();
	}

	@Override
	protected int getDockSlot()
	{
		return config.hpDockSlot();
	}

	@Override
	protected int getDockPadding()
	{
		return config.dockPadding();
	}

	@Override
	protected int getDockOffset()
	{
		return config.dockOffset();
	}

	@Override
	protected Justification getDockSlotOrientation()
	{
		return config.hpDockJustification();
	}

	@Override
	protected boolean isClampSlot()
	{
		return config.hpClampSlot();
	}

	@Override
	protected int getCurrent()
	{
		return client.getBoostedSkillLevel(Skill.HITPOINTS);
	}

	@Override
	protected int getMax()
	{
		return client.getRealSkillLevel(Skill.HITPOINTS);
	}

	@Override
	protected Color getColor()
	{
		int poison = client.getVarpValue(VarPlayer.POISON);
		if (poison >= 1_000_000)
		{
			return ENVENOMED;
		}
		if (poison > 0)
		{
			return POISONED;
		}
		if (client.getVarpValue(VarPlayer.DISEASE_VALUE) > 0)
		{
			return DISEASED;
		}
		return NORMAL;
	}

	@Override
	protected boolean isShowIcon()
	{
		return config.hpShowIcon();
	}

	@Override
	protected BufferedImage getIcon()
	{
		return spriteManager.getSprite(SpriteID.OrbIcon.HITPOINTS, 0);
	}

	@Override
	protected int getIconSize()
	{
		return config.hpIconSize();
	}

	@Override
	protected boolean isShowCounter()
	{
		return config.hpShowCounter();
	}

	@Override
	protected Justification getJustification()
	{
		return config.hpJustification();
	}

	@Override
	protected int getContentOffset()
	{
		return config.hpContentOffset();
	}

	@Override
	protected boolean isReverseContent()
	{
		return config.hpReverseContent();
	}

	@Override
	protected Orientation getOrientation()
	{
		return config.hpFloatingOrientation();
	}

	@Override
	protected int getThickness()
	{
		return config.hpThickness();
	}

	@Override
	protected int getLength()
	{
		return config.hpLength();
	}

	@Override
	protected int getBorderWidth()
	{
		return config.borderWidth();
	}

	@Override
	protected int getOpacity()
	{
		return config.hpOpacity();
	}

	@Override
	protected OrbMode getOrbMode()
	{
		return config.hpOrbMode();
	}

	@Override
	protected void onOrbActivate()
	{
		client.menuAction(-1, HP_ORB_COMPONENT, MenuAction.CC_OP, 1, -1, "Cure", "");
	}

	@Override
	protected int getOrbWidgetComponent()
	{
		return InterfaceID.Orbs.ORB_HEALTH;
	}

	@Override
	protected int getPreviewAmount()
	{
		if (!config.hpShowHealPreview())
		{
			return 0;
		}
		return getRestoreValue(Skill.HITPOINTS.getName());
	}

	@Override
	protected boolean isWarningEnabled()
	{
		return config.hpWarningEnabled();
	}

	@Override
	protected int getWarningThreshold()
	{
		return config.hpWarningThreshold();
	}

	@Override
	protected Color getWarningColor()
	{
		return config.hpWarningColor();
	}

	@Override
	protected int getWarningOpacity()
	{
		return config.hpWarningOpacity();
	}

	// Verbatim logic from RuneLite core's own StatusBarsOverlay.getRestoreValue()
	// - same menu-entry/widget sniffing approach used to detect which
	// inventory item is currently hovered, same ItemStatChangesService call
	// to get its real computed stat change. Not guessed or approximated -
	// this is the exact mechanism the feature we're replicating already uses.
	private int getRestoreValue(String skill)
	{
		MenuEntry[] menu = client.getMenuEntries();
		int menuSize = menu.length;
		if (menuSize == 0)
		{
			return 0;
		}

		MenuEntry entry = menu[menuSize - 1];
		Widget widget = entry.getWidget();
		int restoreValue = 0;

		if (widget != null && widget.getId() == InterfaceID.Inventory.ITEMS)
		{
			Effect change = itemStatService.getItemStatChanges(widget.getItemId());

			if (change != null)
			{
				for (StatChange c : change.calculate(client).getStatChanges())
				{
					int value = c.getTheoretical();

					if (value != 0 && c.getStat().getName().equals(skill))
					{
						restoreValue = value;
					}
				}
			}
		}

		return restoreValue;
	}
}
