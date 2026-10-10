package com.viciousliqx.viciousbars;

import com.compactorbs.CompactOrbsConfig;
import com.compactorbs.CompactOrbsConstants.ConfigGroup;
import static com.compactorbs.CompactOrbsConstants.ConfigGroup.GROUP_NAME;
import com.compactorbs.CompactOrbsConstants.ConfigKeys;
import static com.compactorbs.CompactOrbsConstants.Layout.EDIT_MODE_HIDDEN_OPACITY;
import com.compactorbs.CompactOrbsConstants.Script;
import com.compactorbs.CompactOrbsConstants.VarClient;
import com.compactorbs.CompactOrbsConstants.Varbit;
import com.compactorbs.CompactOrbsConstants.Widgets.Orb;
import com.compactorbs.CompactOrbsManager;
import com.compactorbs.widget.WidgetManager;
import com.compactorbs.widget.elements.Compass;
import com.compactorbs.widget.elements.Minimap;
import com.compactorbs.widget.elements.Orbs;
import com.compactorbs.widget.layout.HideOrbRegistry;
import com.compactorbs.widget.layout.edit.DragListener;
import com.compactorbs.widget.layout.edit.DragState;
import com.compactorbs.widget.layout.edit.EditManager;
import com.compactorbs.widget.layout.slot.SlotManager;
import com.google.inject.Provides;
import java.awt.event.KeyEvent;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ItemContainer;
import net.runelite.api.NPC;
import net.runelite.api.Prayer;
import net.runelite.api.Skill;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.InteractingChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.events.VarClientIntChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.PluginChanged;
import net.runelite.client.events.ProfileChanged;
import net.runelite.client.input.KeyListener;
import net.runelite.client.input.KeyManager;
import net.runelite.client.input.MouseManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * FULL RESET IN PROGRESS - see vicious_bars_dev_talk.txt / git log for the
 * original bars feature (fully recoverable from history, nothing lost).
 * <p>
 * This plugin is currently the verbatim-copied Compact Orbs engine
 * (its-cue/compact-orbs, BSD-2-Clause - see com.compactorbs's own source
 * comments and compact_orbs_walkthrough.md at the hangar root), wired
 * directly into OUR plugin identity rather than running as a separate
 * parallel plugin: CompactOrbsConstants.ConfigGroup.GROUP_NAME was
 * retargeted to "vicious-bars" (this plugin's own existing config group),
 * so CompactOrbsConfig IS this plugin's real, live config now - not a
 * second entry in the plugin panel.
 * <p>
 * Being gutted down feature-by-feature from here with context applied to
 * each removal, not blanket deletion - e.g. the layout-preset SYSTEM
 * (Minimap/Orbs/Offset/Slot machinery) stays, since Vicious Bars still
 * needs it to place things; only the multi-layout CONFIG CHOICE goes,
 * since Vicious Bars will only ever have one single layout, not four.
 * Already removed before reaching this integrated state: the detached
 * minimap overlay feature, the wiki-plugin-conflict chat warning, the dead
 * "Detached Minimap" hotkey option, and orb-swapping in full (Compact Orbs'
 * own feature - letting you reassign which named orb sits in which slot
 * instead of freely dragging it. Vicious Bars is about bars, not
 * rearranging orbs, so keeping this would make us a redundant subset of
 * the real Compact Orbs rather than something distinct - see git history
 * for exact diffs). Store/activity orb hide toggles turned out to be
 * ordinary vanilla icons riding the same generic per-orb hide/reorder
 * system HP/Prayer/Run/Spec already need, so nothing to cut there. Still
 * to be picked off: the multi-layout config surface mentioned above.
 */
@Slf4j
@PluginDescriptor(
	name = "Vicious Bars"
)
public class ViciousBarsPlugin extends Plugin implements KeyListener
{
	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private CompactOrbsConfig config;

	@Inject
	private CompactOrbsManager manager;

	@Inject
	private ConfigManager configManager;

	@Inject
	private KeyManager keyManager;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private WidgetManager widgetManager;

	@Inject
	private SlotManager slotManager;

	@Inject
	private HideOrbRegistry orbRegistry;

	@Inject
	private EditManager editManager;

	@Inject
	private DragListener dragListener;

	@Inject
	private DragState dragState;

	@Inject
	private MouseManager mouseManager;

	// HP/Spec regen tracking - exact port of RuneLite's own RegenMeterPlugin
	// (net.runelite.client.plugins.regenmeter), just re-anchored to our own
	// bar/frame geometry instead of drawing a ring off the real orb's
	// (drifted) widget bounds. See onGameTick() below.
	private static final int SPEC_REGEN_TICKS = 50;
	private static final int NORMAL_HP_REGEN_TICKS = 100;
	private int ticksSinceHPRegen;
	private int ticksSinceSpecRegen;
	private boolean wearingLightbearer;

	// Prayer regen tracking - not a RegenMeterPlugin port (it doesn't cover
	// Prayer), derived from the OSRS wiki and cross-checked against
	// Supalosa's "Prayer Regeneration Helper" plugin: a Prayer regeneration
	// potion's effect restores 1 prayer point every 12 ticks, signalled by
	// VarbitID.PRAYER_REGENERATION_POTION_TIMER (legacy name
	// Varbits.BUFF_PRAYER_REGENERATION) going nonzero/zero. See
	// onVarbitChanged() and onGameTick() below.
	private static final int PRAYER_REGEN_TICKS = 12;
	private int ticksSincePrayerRegen;
	private boolean prayerRegenActive;

	// Poison/envenom damage-tick countdown - ported from RuneLite's own
	// PoisonPlugin (net.runelite.client.plugins.poison), which resyncs a
	// wall-clock timer to "now" on every VarPlayerID.POISON change (that
	// varp decrements by 1 on each damage tick, so the event marks the
	// exact moment one just landed) and waits 18.2 real seconds for the
	// next. We track ticks instead, same as every other counter here, and
	// use the wiki's 30-tick figure that 18.2s is rounded up from for
	// display safety - onGameTick() below writes the inverted progress
	// (1 - fraction) into manager.hpDrainPercentage while poisoned (null
	// otherwise), a separate field from hpRegenPercentage since the two
	// ring off completely independent schedules - see HpVialOverlay's
	// drain trio and VialOverlay's drawVialOutlines() for how the two
	// rings combine on screen.
	// Deliberately clamped rather than wrapped at POISON_TICK_TICKS (unlike
	// HP/Spec's modulo counters above) - confirmed live that the real
	// interval isn't a clean 30 ticks, so wrapping on our own count
	// refilled the ring a tick or so before the real splat/VarbitChanged
	// event; only that real event (onVarbitChanged below) should refill it.
	private static final int POISON_TICK_TICKS = 30;
	private int ticksSincePoisonTick;

	// Prayer point drain countdown. Originally tried porting the real OSRS
	// drain-rate formula (https://oldschool.runescape.wiki/w/Prayer#Prayer_drain_mechanics,
	// cross-checked against RuneLite's own core Prayer plugin's
	// getEstimatedTimeRemaining()) to predict ticks-per-point, but
	// confirmed live that formula runs noticeably faster than the real
	// drain (~1.5x on Piety) - not just an off-by-one like poison, a real
	// pacing error somewhere between the wiki's numbers and the live
	// client, not worth chasing blind. So instead of trusting any formula,
	// this measures the real tick gap between the player's last two actual
	// point losses (previousBoostedPrayer dropping) and paces the ring off
	// THAT - self-calibrating to whatever the real current rate is (gear,
	// active prayers, whatever) rather than predicting it. No ring shows
	// until a second real point loss gives an actual interval to calibrate
	// to; clamped rather than wrapped once paced (same reasoning as poison
	// - only a real loss should refill it). PrayerVialOverlay inverts this
	// the same way HpVialOverlay does for poison: full right after a point
	// drains, empty right before the next one.
	private int ticksSincePrayerDrainTick;
	private double measuredPrayerDrainTicksPerPoint = -1;
	// -1 sentinel so the very first tick (including right after a relog)
	// never falsely reads as a drain just because there's no prior value
	// to compare against yet.
	private int previousBoostedPrayer = -1;

	// Combat tracking for the threshold-glow feature's COMBAT_ONLY mode -
	// restored from v1's OpponentTracker (see git log before the
	// full-reset "gut pass"), trimmed down to just the in-combat flag
	// (v1 also tracked the opponent NPC specifically, for an Enemy
	// Hitpoints bar this plugin doesn't have). Same lingering-after-combat
	// pattern as RuneLite's own built-in OpponentInfo plugin: keep
	// "in combat" true for a few seconds after interaction stops, so this
	// doesn't flicker off mid-fight between hits.
	private static final Duration COMBAT_LINGER = Duration.ofSeconds(5);
	private Actor lastOpponentActor;
	private Instant lastInteractionTime;

	@Inject
	private HpVialOverlay hpVialOverlay;

	@Inject
	private PrayerVialOverlay prayerVialOverlay;

	@Inject
	private RunVialOverlay runVialOverlay;

	@Inject
	private SpecVialOverlay specVialOverlay;

	@Inject
	private OrbCollarOverlay orbCollarOverlay;

	@Inject
	private EditGridOverlay editGridOverlay;

	@Inject
	private net.runelite.client.game.SpriteManager spriteManager;

	@Inject
	private HpAuxBarOverlay hpAuxBarOverlay;

	@Inject
	private PrayerAuxBarOverlay prayerAuxBarOverlay;

	@Inject
	private SpecAuxBarOverlay specAuxBarOverlay;

	@Inject
	private RunAuxBarOverlay runAuxBarOverlay;

	@Inject
	private WarmthAuxBarOverlay warmthAuxBarOverlay;

	@Inject
	private EnemyHpAuxBarOverlay enemyHpAuxBarOverlay;

	@Override
	protected void startUp() throws Exception
	{
		manager.migrateConfigs();
		overlayManager.add(hpVialOverlay);
		overlayManager.add(prayerVialOverlay);
		overlayManager.add(runVialOverlay);
		overlayManager.add(specVialOverlay);
		overlayManager.add(orbCollarOverlay);
		overlayManager.add(editGridOverlay);
		overlayManager.add(hpAuxBarOverlay);
		overlayManager.add(prayerAuxBarOverlay);
		overlayManager.add(specAuxBarOverlay);
		overlayManager.add(runAuxBarOverlay);
		overlayManager.add(warmthAuxBarOverlay);
		overlayManager.add(enemyHpAuxBarOverlay);
		keyManager.registerKeyListener(this);
		mouseManager.registerMouseListener(dragListener);
		orbRegistry.registerAll();

		if (!manager.isLoggedIn())
		{
			manager.isLoggingIn = true;
		}

		manager.isUpdatingProfile = false;
		manager.isEditingLayout = false;
		manager.hideWorldMap = config.hideWorld();
		manager.hideLogoutX = config.hideLogout();
		manager.enableNoClickThrough = true;
		manager.snapCornerRepositioned = manager.isSnapCornerRepositioned();

		clientThread.invoke(() ->
		{
			slotManager.init();

			if (manager.isLoggedIn())
			{
				manager.update(Script.FORCE_UPDATE);
				manager.hideAllOrbsByConfig();
				manager.hideMinimapOnTabClose(config.hideMinimapWithSidePanel());
			}
		});

		log.debug("Vicious Bars started (Compact Orbs engine, retargeted config group)");
	}

	@Override
	protected void shutDown() throws Exception
	{
		overlayManager.remove(hpVialOverlay);
		hpVialOverlay.shutDown();
		overlayManager.remove(prayerVialOverlay);
		prayerVialOverlay.shutDown();
		overlayManager.remove(runVialOverlay);
		runVialOverlay.shutDown();
		overlayManager.remove(specVialOverlay);
		specVialOverlay.shutDown();
		overlayManager.remove(orbCollarOverlay);
		overlayManager.remove(editGridOverlay);
		overlayManager.remove(hpAuxBarOverlay);
		overlayManager.remove(prayerAuxBarOverlay);
		overlayManager.remove(specAuxBarOverlay);
		overlayManager.remove(runAuxBarOverlay);
		overlayManager.remove(warmthAuxBarOverlay);
		overlayManager.remove(enemyHpAuxBarOverlay);
		restoreBarZoneSize();
		keyManager.unregisterKeyListener(this);
		mouseManager.unregisterMouseListener(dragListener);
		clientThread.invoke(manager::reset);
		log.debug("Vicious Bars stopped");
	}

	@Provides
	CompactOrbsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CompactOrbsConfig.class);
	}

	// Dev-only, temporary - see CompactOrbsConfig#barZoneWidthOffset and
	// #barZoneHeightOffset. Two widgets, one offset per axis: F7's
	// diagnostic dump confirmed getOrbsParent() and getMapParent() are
	// separate, nested widgets at the exact same native size (both plain
	// absolute sizing - original==current, safe to edit), so both get
	// resized together here. The orb's own direct parent (ORBS_UNIVERSE) is
	// a THIRD, different widget - original width 0, sized some other way at
	// render time - and is deliberately NOT touched any more: editing it
	// directly is what caused the screen-edge blowup from +1 and the
	// hide/show-to-settle glitch.
	// Saved against the widget INSTANCE, same reasoning as every other
	// save/restore-on-toggle-off pair in this plugin - minimizing the
	// minimap swaps in a brand new widget, so a stale saved size must
	// never get applied to the wrong instance.
	private Widget orbsZoneWidget;
	private int orbsZoneOriginalWidth;
	private int orbsZoneOriginalHeight;
	private Widget orbMapParentWidget;
	private int orbMapParentOriginalWidth;
	private int orbMapParentOriginalHeight;

	@Subscribe
	public void onClientTick(ClientTick event)
	{
		applyBarZoneSize();

		if (!manager.isEditingLayout)
		{
			return;
		}

		dragListener.updateDrag();

		if (client.getMouseCurrentButton() == 0 && dragState.wasDragging)
		{
			dragListener.finalizeDrag();
		}
	}

	/** Resizes getOrbsParent() and getMapParent() together, on both axes - see the field comment above for why these two and not ORBS_UNIVERSE. */
	private void applyBarZoneSize()
	{
		int widthOffset = config.barZoneWidthOffset();
		int heightOffset = config.barZoneHeightOffset();

		if (!manager.isCompactLayout() || (widthOffset == 0 && heightOffset == 0))
		{
			restoreBarZoneSize();
			return;
		}

		Widget orbsParent = widgetManager.getOrbsParent();
		if (orbsParent != null)
		{
			if (orbsZoneWidget != orbsParent)
			{
				orbsZoneOriginalWidth = orbsParent.getOriginalWidth();
				orbsZoneOriginalHeight = orbsParent.getOriginalHeight();
				orbsZoneWidget = orbsParent;
			}

			int targetWidth = orbsZoneOriginalWidth + widthOffset;
			int targetHeight = orbsZoneOriginalHeight + heightOffset;
			if (orbsParent.getOriginalWidth() != targetWidth || orbsParent.getOriginalHeight() != targetHeight)
			{
				orbsParent.setOriginalWidth(targetWidth);
				orbsParent.setOriginalHeight(targetHeight);
				orbsParent.revalidate();
			}
		}

		Widget mapParent = widgetManager.getMapParent();
		if (mapParent != null)
		{
			if (orbMapParentWidget != mapParent)
			{
				orbMapParentOriginalWidth = mapParent.getOriginalWidth();
				orbMapParentOriginalHeight = mapParent.getOriginalHeight();
				orbMapParentWidget = mapParent;
			}

			int targetWidth = orbMapParentOriginalWidth + widthOffset;
			int targetHeight = orbMapParentOriginalHeight + heightOffset;
			if (mapParent.getOriginalWidth() != targetWidth || mapParent.getOriginalHeight() != targetHeight)
			{
				mapParent.setOriginalWidth(targetWidth);
				mapParent.setOriginalHeight(targetHeight);
				mapParent.revalidate();
			}
		}

		// ORBS_UNIVERSE's own width/height MODE resolves against GAMEFRAME (the
		// full client canvas) whenever it naturally revalidates - confirmed by
		// CompactOrbsManager#setupMinimapContainer()'s own pre-existing TODO
		// comment, and exactly the screen-edge blowup we hit trying to edit it
		// directly earlier. That method already works around it by forcing
		// ORBS_UNIVERSE's LIVE width/height (not original/mode) to match
		// getOrbsParent()'s current size - but only runs that patch on
		// startup/profile-change/real-resize, which is why toggling the
		// RuneLite side panel incidentally "fixed" the scrambled layout (it's
		// one of those triggers, and by then getOrbsParent() was already
		// widened). Doing the same sync here, every tick, removes the
		// dependency on that incidental trigger entirely.
		if (orbsParent != null)
		{
			Widget orbsUniverse = client.getWidget(Minimap.ORBS_UNIVERSE.getComponentId());
			if (orbsUniverse != null)
			{
				widgetManager.updateValue(orbsUniverse::getWidth, orbsUniverse::setWidth, orbsParent.getWidth());
				widgetManager.updateValue(orbsUniverse::getHeight, orbsUniverse::setHeight, orbsParent.getHeight());
			}

			// The edit-mode red overlay box has the exact same disease: its
			// widthMode/heightMode were copied from ORBS_UNIVERSE ONCE, at the
			// moment EditManager first created it (EditManager#updateBackground()),
			// and never touched again in compact layout - so it never re-resolves
			// against our widened zone on its own, same as ORBS_UNIVERSE itself
			// didn't. Forcing its live size here too, every tick, is the same fix.
			Widget editBackground = editManager.getEditBackground();
			if (editBackground != null)
			{
				widgetManager.updateValue(editBackground::getWidth, editBackground::setWidth, orbsParent.getWidth());
				widgetManager.updateValue(editBackground::getHeight, editBackground::setHeight, orbsParent.getHeight());
			}
		}
	}

	/** Safe to call unconditionally any time we're not actively resizing the zone - a no-op once nothing's saved. */
	private void restoreBarZoneSize()
	{
		if (orbsZoneWidget != null)
		{
			orbsZoneWidget.setOriginalWidth(orbsZoneOriginalWidth);
			orbsZoneWidget.setOriginalHeight(orbsZoneOriginalHeight);
			orbsZoneWidget.revalidate();
			orbsZoneWidget = null;
		}

		if (orbMapParentWidget != null)
		{
			orbMapParentWidget.setOriginalWidth(orbMapParentOriginalWidth);
			orbMapParentWidget.setOriginalHeight(orbMapParentOriginalHeight);
			orbMapParentWidget.revalidate();
			orbMapParentWidget = null;
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.HOPPING ||
			event.getGameState() == GameState.LOGIN_SCREEN ||
			event.getGameState() == GameState.LOGGING_IN)
		{
			manager.isLoggingIn = true;
			manager.wikiPluginBannerExists = false;
			manager.clearCustomChildren();

			// Same reset RegenMeterPlugin does on this transition - avoids
			// carrying a stale tick count across a world hop/relog.
			ticksSinceHPRegen = -2;
			ticksSinceSpecRegen = 0;
			ticksSincePrayerRegen = 0;
			prayerRegenActive = false;
			ticksSincePoisonTick = 0;
			ticksSincePrayerDrainTick = 0;
			measuredPrayerDrainTicksPerPoint = -1;
			previousBoostedPrayer = -1;
			lastOpponentActor = null;
			lastInteractionTime = null;
		}
		else if (manager.isLoggedIn() && manager.isLoggingIn)
		{
			manager.isLoggingIn = false;
			manager.createCustomChildren();
		}
	}

	@Subscribe(priority = -1.0f)
	public void onScriptPostFired(ScriptPostFired event)
	{
		int scriptId = event.getScriptId();

		if (manager.isEditingLayout &&
			(scriptId == Script.TOPLEVEL_REDRAW ||
				scriptId == Script.PROC_TOPLEVEL_SUBCHANGE ||
				scriptId == Script.TOPLEVEL_SIDE_CUSTOMIZE ||
				scriptId == Script.WIKI_ICON_INIT ||
				scriptId == Script.WORLD_MAP_UPDATE ||
				scriptId == Script.WIKI_ICON_UPDATE ||
				scriptId == Script.ORBS_UPDATE_STORE ||
				scriptId == Script.ORBS_UPDATE_ACTIVITY))
		{
			widgetManager.remapTargets(
				Orbs.WIKI_ICON_CONTAINER,
				Orbs.STORE_ORB_CONTAINER,
				Orbs.ACTIVITY_ORB_CONTAINER);

			widgetManager.setTargetsHidden(false, Compass.values());
			if (manager.isCompactLayout())
			{
				if (manager.compassFrame != null)
				{
					manager.compassFrame.setHidden(false);
				}
			}
			return;
		}

		switch (scriptId)
		{
			case Script.TOPLEVEL_SUBCHANGE:
				if (manager.isEditingLayout)
				{
					editManager.toggleEditMode(false);
				}
				break;

			case Script.TOPLEVEL_REDRAW:
			case Script.TOPLEVEL_RESIZE_CUSTOMIZE:
			case Script.PROC_TOPLEVEL_SUBCHANGE:
			case Script.TOPLEVEL_SIDE_CUSTOMIZE:
				manager.hideLogout();
				manager.hideMinimapOnTabClose(config.hideMinimapWithSidePanel());
				break;

			case Script.ORBS_UPDATE_SPECENERGY:
				if (manager.isEditingLayout && config.hideSpec())
				{
					widgetManager.setTargetOpacity(Orbs.SPEC_ORB_CONTAINER, EDIT_MODE_HIDDEN_OPACITY);
				}
			case Script.ORBS_UPDATE_HEALTH:
				if (!manager.isCompactLayout())
				{
					manager.updateNoClickThrough();
				}
				break;

			case Script.WORLD_MAP_UPDATE:
				if (manager.hideWorldMap)
				{
					widgetManager.remapTargets(Orbs.WORLD_MAP_CONTAINER);
					return;
				}
			case Script.ORBS_UPDATE_STORE:
			case Script.ORBS_UPDATE_ACTIVITY:
			case Script.WIKI_ICON_INIT:
			case Script.WIKI_ICON_UPDATE:
				if (!manager.isMinimapMinimized())
				{
					manager.update(scriptId);

					if (scriptId == Script.WIKI_ICON_INIT || scriptId == Script.WIKI_ICON_UPDATE)
					{
						manager.updateWikiBannerVisibility(config.hideWiki());
					}
				}
				break;
		}
	}

	@Subscribe
	public void onScriptPreFired(ScriptPreFired event)
	{
		int scriptId = event.getScriptId();

		if (scriptId == Orbs.WORLD_MAP_TOOLTIP.getScriptId())
		{
			int id = client.getIntStack()[2];
			int tooltipId = Orbs.WORLD_MAP_TOOLTIP.getComponentId();
			if (id == tooltipId)
			{
				boolean hidden = manager.isCompactLayout() || manager.hideWorldMap;
				widgetManager.setHidden(tooltipId, hidden);
			}
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		if (event.getVarbitId() == VarbitID.PRAYER_RAPIDHEAL)
		{
			// Rapid Heal toggling changes the HP regen period (100 <-> 50
			// ticks) - resync the counter right at the switch instead of
			// letting it carry stale progress from the old period, exact
			// same reset RegenMeterPlugin does on this varbit.
			ticksSinceHPRegen = 0;
		}

		if (event.getVarbitId() == VarbitID.PRAYER_REGENERATION_POTION_TIMER)
		{
			// Resync to 0 on every nonzero report, not just the activating
			// edge - keeps us locked to the server's real cadence instead
			// of drifting if a tick gets missed client-side.
			prayerRegenActive = event.getValue() > 0;
			if (prayerRegenActive)
			{
				ticksSincePrayerRegen = 0;
			}
		}

		if (event.getVarpId() == VarPlayerID.POISON)
		{
			// Fires once per damage tick (the varp decrements by 1 each
			// time) as well as on first getting poisoned/reapplied - either
			// way, a fresh countdown just started, same resync-on-event
			// approach as the Prayer regen potion timer above.
			ticksSincePoisonTick = 0;
		}

		switch (event.getVarbitId())
		{
			case Varbit.CUTSCENE_STATUS:
				manager.isCutsceneActive = manager.isCutsceneActive();
				break;

			case Varbit.MINIMAP_TOGGLE:
				widgetManager.remapTargets(Orbs.LOGOUT_X_ICON, Orbs.LOGOUT_X_STONE);
				manager.updateCustomChildren();
				manager.setupMinimapContainer(false);
				break;

			case Varbit.STORE_ORB_TOGGLE:
			case Varbit.ACTIVITY_ORB_TOGGLE:
				if (manager.isEditingLayout)
				{
					editManager.toggleEditMode(false);
				}

				if (manager.allowReordering())
				{
					widgetManager.remapTargets(Orbs.values());
					manager.updateMinimapToggleButton();
				}
				break;
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		// Exact port of RegenMeterPlugin's own Lightbearer check - halves
		// the spec regen period (50 -> 25 ticks) while worn.
		if (event.getContainerId() != InventoryID.WORN)
		{
			return;
		}

		ItemContainer equipment = event.getItemContainer();
		boolean wasWearingLightbearer = wearingLightbearer;
		wearingLightbearer = equipment.contains(ItemID.LIGHTBEARER);

		if (wearingLightbearer == wasWearingLightbearer)
		{
			return;
		}

		ticksSinceSpecRegen = Math.max(0, ticksSinceSpecRegen - 25);
	}

	@Subscribe
	public void onInteractingChanged(InteractingChanged event)
	{
		if (event.getSource() != client.getLocalPlayer())
		{
			return;
		}

		lastOpponentActor = event.getTarget();
		lastInteractionTime = Instant.now();
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (lastOpponentActor != null
			&& client.getLocalPlayer().getInteracting() == null
			&& Duration.between(lastInteractionTime, Instant.now()).compareTo(COMBAT_LINGER) > 0)
		{
			lastOpponentActor = null;
		}
		manager.inCombat = lastOpponentActor != null;
		// Restored for the Enemy HP aux bar - v1's OpponentTracker#getOpponent()
		// (see git log before the full-reset "gut pass"), null unless the
		// current (or recently-lingering) target is specifically an NPC.
		manager.combatTargetNpc = lastOpponentActor instanceof NPC ? (NPC) lastOpponentActor : null;

		int specRegenTicks = wearingLightbearer ? SPEC_REGEN_TICKS / 2 : SPEC_REGEN_TICKS;

		if (client.getVarpValue(VarPlayerID.SA_ENERGY) == 1000)
		{
			ticksSinceSpecRegen = 0;
		}
		else
		{
			ticksSinceSpecRegen = (ticksSinceSpecRegen + 1) % specRegenTicks;
		}

		manager.specRegenPercentage = ticksSinceSpecRegen / (double) specRegenTicks;

		int hpRegenTicks = client.isPrayerActive(Prayer.RAPID_HEAL) ? NORMAL_HP_REGEN_TICKS / 2 : NORMAL_HP_REGEN_TICKS;
		ticksSinceHPRegen = (ticksSinceHPRegen + 1) % hpRegenTicks;

		double hitpointsPercentage = ticksSinceHPRegen / (double) hpRegenTicks;

		int boostedHp = client.getBoostedSkillLevel(Skill.HITPOINTS);
		int realHp = client.getRealSkillLevel(Skill.HITPOINTS);

		if (boostedHp == realHp)
		{
			hitpointsPercentage = 0;
		}
		else if (boostedHp > realHp)
		{
			// Overheal (above max, e.g. a few absorption-style effects) -
			// counting back DOWN to max instead of up, so invert direction.
			hitpointsPercentage = 1 - hitpointsPercentage;
		}

		manager.hpRegenPercentage = hitpointsPercentage;

		if (client.getVarpValue(VarPlayerID.POISON) > 0)
		{
			// Clamp, don't wrap - the real interval isn't a clean 30 ticks
			// (RuneLite's own PoisonPlugin waits 18.2 real seconds, ~30.33
			// ticks, not 30 flat), so a modulo wrap here would refill the
			// ring on our own guessed cadence a tick or so before the real
			// splat/VarbitChanged event lands. Holding at empty instead
			// means the ring only ever refills off the real event above.
			ticksSincePoisonTick = Math.min(ticksSincePoisonTick + 1, POISON_TICK_TICKS);
			manager.hpDrainPercentage = 1 - (ticksSincePoisonTick / (double) POISON_TICK_TICKS);
		}
		else
		{
			manager.hpDrainPercentage = null;
		}

		boolean anyPrayerActive = false;
		for (Prayer prayer : Prayer.values())
		{
			if (client.isPrayerActive(prayer))
			{
				anyPrayerActive = true;
				break;
			}
		}

		int boostedPrayer = client.getBoostedSkillLevel(Skill.PRAYER);
		int realPrayer = client.getRealSkillLevel(Skill.PRAYER);

		boolean justLostPrayerPoint = previousBoostedPrayer >= 0 && boostedPrayer < previousBoostedPrayer;
		if (justLostPrayerPoint)
		{
			if (ticksSincePrayerDrainTick > 0)
			{
				measuredPrayerDrainTicksPerPoint = ticksSincePrayerDrainTick;
			}
			ticksSincePrayerDrainTick = 0;
		}
		previousBoostedPrayer = boostedPrayer;

		if (anyPrayerActive)
		{
			if (!justLostPrayerPoint)
			{
				// Don't also advance on the same tick as a reset - keeps
				// the reset tick reading as a clean, literal full ring
				// rather than one tick's worth into the next cycle already.
				ticksSincePrayerDrainTick++;
			}

			manager.prayerDrainPercentage = measuredPrayerDrainTicksPerPoint > 0
				? 1 - Math.min(1.0, ticksSincePrayerDrainTick / measuredPrayerDrainTicksPerPoint)
				: null;
		}
		else
		{
			ticksSincePrayerDrainTick = 0;
			manager.prayerDrainPercentage = null;
		}

		if (!prayerRegenActive || boostedPrayer >= realPrayer)
		{
			manager.prayerRegenPercentage = 0;
		}
		else
		{
			ticksSincePrayerRegen = (ticksSincePrayerRegen + 1) % PRAYER_REGEN_TICKS;
			manager.prayerRegenPercentage = ticksSincePrayerRegen / (double) PRAYER_REGEN_TICKS;
		}
	}

	@Subscribe
	public void onVarClientIntChanged(VarClientIntChanged event)
	{
		if (event.getIndex() == VarClient.SIDE_PANEL_ID)
		{
			if (manager.isEditingLayout &&
				config.hideMinimapWithSidePanel() && manager.isSidePanelHidden())
			{
				editManager.toggleEditMode(false);
			}
		}
	}

	@Subscribe(priority = -1.0f)
	public void onWidgetLoaded(WidgetLoaded event)
	{
		int id = event.getGroupId();
		if (id == Orb.UNIVERSE >> 16)
		{
			manager.update(Script.FORCE_UPDATE);
			manager.hideAllOrbsByConfig();
		}
	}

	@Subscribe(priority = -1.0f)
	public void onConfigChanged(ConfigChanged event)
	{
		String group = event.getGroup();
		String key = event.getKey();

		if (group.equals(ConfigGroup.Core.RUNELITE))
		{
			if (key.equals(ConfigKeys.Core.SNAPCORNER_PREFIX + OverlayPosition.CANVAS_TOP_RIGHT + ConfigKeys.Core.SNAPCORNER_CONFIG_LOCATION))
			{
				manager.snapCornerRepositioned = event.getNewValue() != null;
			}
		}

		if (group.equals(ConfigGroup.Core.MINIMAP))
		{
			if (key.equals(ConfigKeys.Core.HIDE_MINIMAP))
			{
				clientThread.invokeLater(() -> manager.hideMinimapOnTabClose(config.hideMinimapWithSidePanel()));
			}
		}

		if (group.equals(ConfigGroup.Core.WIKI))
		{
			if (key.equals(ConfigKeys.Core.SHOW_WIKI_MINIMAP_BUTTON))
			{
				clientThread.invokeLater(() ->
				{
					if (manager.isEditingLayout)
					{
						editManager.toggleEditMode(false);
					}
					widgetManager.remapTargets(Orbs.WIKI_ICON_CONTAINER);
					manager.updateWikiBannerVisibility(config.hideWiki());
					manager.updateCustomChildren();
				});
			}
		}

		if (!group.equals(GROUP_NAME))
		{
			return;
		}

		if (orbRegistry.isHideConfig(key))
		{
			if (!manager.isEditingLayout)
			{
				clientThread.invokeLater(() ->
				{
					manager.hideOrbByConfig(key);

					if (!manager.isFixedMode())
					{
						manager.rebuildLayout();
					}
				});
			}
			return;
		}

		if (manager.isEditingLayout && !manager.isUpdatingProfile)
		{
			clientThread.invokeLater(() ->
			{
				if (!key.contains(manager.getCurrentPrefix()))
				{
					editManager.toggleEditMode(false);
				}
			});
		}

		switch (key)
		{
			case ConfigKeys.COMPASS:
				clientThread.invokeLater(() ->
				{
					manager.setupMinimapContainer(false);
					widgetManager.setTargetsHidden(manager.isCompassHidden(), Compass.values());
					widgetManager.remapTargets(Compass.values());
					widgetManager.remapTargets(Orbs.values());
				});
				break;

			case ConfigKeys.ENABLE_NO_CLICKTHROUGH:
				clientThread.invokeLater(() ->
				{
					manager.enableNoClickThrough = true;
					manager.updateNoClickThrough();

					if (!manager.isCompactLayout())
					{
						widgetManager.remapTargets(Orbs.XP_DROPS_CONTAINER);
					}
				});
				break;

			case ConfigKeys.HIDE_MINIMAP_WITH_SIDE_PANEL:
				clientThread.invokeLater(() -> manager.hideMinimapOnTabClose(config.hideMinimapWithSidePanel()));
				break;

			case ConfigKeys.ORB_LAYOUT:
				clientThread.invokeLater(() ->
				{
					if (!manager.isFixedMode())
					{
						manager.rebuildLayout();
					}
				});
				break;
		}
	}

	@Override
	public void resetConfiguration()
	{
		manager.isUpdatingProfile = true;
		clientThread.invoke(() -> manager.rebuild(true));
	}

	@Subscribe
	public void onProfileChanged(ProfileChanged event)
	{
		manager.migrateConfigs();

		manager.isUpdatingProfile = true;
		clientThread.invoke(() -> manager.rebuild(false));
	}

	@Subscribe
	public void onPluginChanged(PluginChanged event)
	{
		String name = event.getPlugin().getName().toLowerCase(Locale.ROOT);
		switch (name)
		{
			case ConfigGroup.Core.WIKI:
				clientThread.invokeLater(() ->
				{
					if (manager.isEditingLayout)
					{
						editManager.toggleEditMode(false);
					}
					else
					{
						manager.rebuildLayout();
					}
					manager.updateWikiBannerVisibility(config.hideWiki());
				});
				break;

			case ConfigGroup.Core.MINIMAP:
				clientThread.invokeLater(() -> manager.hideMinimapOnTabClose(config.hideMinimapWithSidePanel()));
				break;
		}
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		manager.addCustomMenuEntries(event.getMenuEntry());
	}

	@Override
	public void keyTyped(KeyEvent e)
	{
	}

	// Throwaway diagnostic, not permanent: F9 dumps every sprite used
	// anywhere in the side/inventory panel's widget tree, so the panel's
	// REAL border/corner sprite(s) can be identified directly instead of
	// guessed at - SpriteID.Steelborder was tried once already and
	// confirmed wrong (see HpVialOverlay's frame comment / git history).
	private static final int SIDE_PANEL_SPRITE_DUMP_KEY = KeyEvent.VK_F9;

	// Also throwaway: F8 dumps EVERY child of the real HP orb container
	// (not just the named backing/indicator/icon/button/empty sub-ids
	// Orbs.HP_ORB_CONTAINER already tracks), looking for the "96" level
	// number's own widget - it's not any of those 5, so it must be an
	// untracked child, likely TEXT type.
	private static final int ORB_CHILDREN_DUMP_KEY = KeyEvent.VK_F8;

	// Also throwaway: F7 dumps id + original/current size for every zone
	// widget candidate the 3 zone-width sliders have guessed at so far
	// (getOrbsParent(), the orb's own direct parent, getMapParent(),
	// ORBS_UNIVERSE), plus a full ancestor-chain walk from the orb, so the
	// ones that are literally the same widget (same id) or the same native
	// size can be read off directly instead of guessed at one at a time.
	// Set all 3 orb*ZoneWidthOffset sliders to 0 first so this reports true
	// native sizes, not ones we're already mid-modifying.
	private static final int ZONE_WIDGET_DUMP_KEY = KeyEvent.VK_F7;

	// Also throwaway: F6 walks every widget root and logs every widget whose
	// canvas bounds contain the CURRENT mouse position, regardless of
	// click/hover routing - for the logout-X-on-top "dead zone" when it's
	// dragged over the toolbar's blank slot: no click effect and no hover
	// text there, meaning whatever's intercepting the input doesn't even
	// have its own listener - so the game's own click/hover dispatch can't
	// be used to find it. This finds it by raw bounds-containment instead,
	// walking the tree ourselves rather than relying on that dispatch.
	private static final int MOUSE_WIDGET_DUMP_KEY = KeyEvent.VK_F6;

	@Override
	public void keyPressed(KeyEvent e)
	{
		if (e.getKeyCode() == SIDE_PANEL_SPRITE_DUMP_KEY)
		{
			clientThread.invokeLater(this::dumpSidePanelSprites);
			return;
		}

		if (e.getKeyCode() == ORB_CHILDREN_DUMP_KEY)
		{
			clientThread.invokeLater(this::dumpOrbChildren);
			return;
		}

		if (e.getKeyCode() == ZONE_WIDGET_DUMP_KEY)
		{
			clientThread.invokeLater(this::dumpZoneWidgets);
			return;
		}

		if (e.getKeyCode() == MOUSE_WIDGET_DUMP_KEY)
		{
			clientThread.invokeLater(() -> dumpWidgetsAtMouse());
			return;
		}

		if (manager.isEditingLayout)
		{
			if (e.getKeyCode() == KeyEvent.VK_ESCAPE || config.editMainBarsKeybind().matches(e))
			{
				if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
				{
					e.consume();
				}
				clientThread.invokeLater(() -> editManager.toggleEditMode(false));
			}
			return;
		}

		if (manager.isCutsceneActive)
		{
			return;
		}

		// Three independent keys now instead of one shared key + a dropdown
		// picking which action it performs - each only fires for its own
		// dedicated binding, and an unbound (NOT_SET) key never matches.
		if (!manager.isFixedMode() && config.mainBarsKeybind().matches(e))
		{
			clientThread.invokeLater(manager::onMinimapToggle);
			return;
		}

		if (config.editMainBarsKeybind().matches(e))
		{
			clientThread.invokeLater(() -> editManager.toggleEditMode(true));
			return;
		}

		if (config.hideAuxBarsKeybind().matches(e))
		{
			manager.auxBarsHidden = !manager.auxBarsHidden;
		}
	}

	@Override
	public void keyReleased(KeyEvent e)
	{
	}

	private void dumpSidePanelSprites()
	{
		// SIDE_CONTAINER is a separate, sibling widget - not a descendant
		// of SIDE_BACKGROUND - which is why the first sweep (walking only
		// SIDE_BACKGROUND's own subtree) never found it. Checking both
		// explicitly by id this time instead of just recursing from one root.
		dumpNamedWidget("SIDE_BACKGROUND", InterfaceID.ToplevelPreEoc.SIDE_BACKGROUND);
		dumpNamedWidget("SIDE_CONTAINER", InterfaceID.ToplevelPreEoc.SIDE_CONTAINER);

		// The real 9-slice border kit, confirmed via the widget inspector
		// (821 = left edge) and the dump above: 4 corners (824-827, 32x32
		// each) + 4 edges (820 top, 822 bottom, 821 left, 823 right -
		// edges are 140x32/32x211, meant to tile/stretch along their run).
		for (int spriteId : new int[]{820, 821, 822, 823, 824, 825, 826, 827})
		{
			saveSprite(spriteId);
		}
	}

	private void saveSprite(int spriteId)
	{
		java.awt.image.BufferedImage sprite = spriteManager.getSprite(spriteId, 0);
		if (sprite == null)
		{
			log.info("[sprite dump] sprite {} not loaded/cached yet", spriteId);
			return;
		}

		String fileName = "debug_sprite_" + spriteId + ".png";
		try
		{
			javax.imageio.ImageIO.write(sprite, "png",
				new java.io.File("C:\\Users\\Harri\\RL_MOD_DEV\\plugins\\vicious-bars\\" + fileName));
			log.info("[sprite dump] Saved sprite {} to {} ({}x{})", spriteId, fileName, sprite.getWidth(), sprite.getHeight());
		}
		catch (java.io.IOException e)
		{
			log.info("[sprite dump] Failed to save sprite {}: {}", spriteId, e.getMessage());
		}
	}

	private void dumpNamedWidget(String label, int componentId)
	{
		Widget widget = client.getWidget(componentId);
		if (widget == null)
		{
			log.info("[sprite dump] {} not found (not logged in, or wrong layout)", label);
			return;
		}

		log.info("[sprite dump] --- {} (id={}) ---", label, widget.getId());
		dumpWidgetSprites(widget, 0);
		log.info("[sprite dump] --- end {} ---", label);

		if (widget.getSpriteId() == -1)
		{
			return;
		}

		java.awt.image.BufferedImage sprite = spriteManager.getSprite(widget.getSpriteId(), 0);
		if (sprite == null)
		{
			return;
		}

		String fileName = "debug_sprite_" + label.toLowerCase(java.util.Locale.ROOT) + ".png";
		try
		{
			javax.imageio.ImageIO.write(sprite, "png",
				new java.io.File("C:\\Users\\Harri\\RL_MOD_DEV\\plugins\\vicious-bars\\" + fileName));
			log.info("[sprite dump] Saved {}'s sprite {} to {} ({}x{})",
				label, widget.getSpriteId(), fileName, sprite.getWidth(), sprite.getHeight());
		}
		catch (java.io.IOException e)
		{
			log.info("[sprite dump] Failed to save {}'s sprite: {}", label, e.getMessage());
		}
	}

	private void dumpWidgetSprites(Widget widget, int depth)
	{
		if (widget == null || depth > 12)
		{
			return;
		}

		if (widget.getSpriteId() != -1)
		{
			log.info("[sprite dump] {}spriteId={} id={} pos=({},{}) size={}x{} hidden={}",
				"  ".repeat(depth), widget.getSpriteId(), widget.getId(),
				widget.getRelativeX(), widget.getRelativeY(),
				widget.getWidth(), widget.getHeight(), widget.isHidden());
		}

		for (Widget[] children : new Widget[][]{
			widget.getChildren(), widget.getStaticChildren(), widget.getDynamicChildren(), widget.getNestedChildren()})
		{
			if (children == null)
			{
				continue;
			}
			for (Widget child : children)
			{
				dumpWidgetSprites(child, depth + 1);
			}
		}
	}

	private void dumpWidgetsAtMouse()
	{
		net.runelite.api.Point mouse = client.getMouseCanvasPosition();
		if (mouse == null)
		{
			log.info("[mouse dump] no mouse position");
			return;
		}

		log.info("[mouse dump] mouse at ({},{})", mouse.getX(), mouse.getY());

		Widget[] roots = client.getWidgetRoots();
		if (roots == null)
		{
			log.info("[mouse dump] no widget roots");
			return;
		}

		for (Widget root : roots)
		{
			dumpWidgetsAtMouse(root, mouse, 0);
		}
	}

	private void dumpWidgetsAtMouse(Widget widget, net.runelite.api.Point mouse, int depth)
	{
		if (widget == null || depth > 20)
		{
			return;
		}

		if (!widget.isHidden())
		{
			net.runelite.api.Point canvas = widget.getCanvasLocation();
			if (canvas != null
				&& mouse.getX() >= canvas.getX() && mouse.getX() < canvas.getX() + widget.getWidth()
				&& mouse.getY() >= canvas.getY() && mouse.getY() < canvas.getY() + widget.getHeight())
			{
				log.info("[mouse dump] {}id={} name={} actions={} noClickThrough={} hasListener={} pos=({},{}) size={}x{}",
					"  ".repeat(depth), widget.getId(), widget.getName(),
					java.util.Arrays.toString(widget.getActions()),
					widget.getNoClickThrough(), widget.hasListener(),
					canvas.getX(), canvas.getY(), widget.getWidth(), widget.getHeight());
			}
		}

		for (Widget[] children : new Widget[][]{
			widget.getChildren(), widget.getStaticChildren(), widget.getDynamicChildren(), widget.getNestedChildren()})
		{
			if (children == null)
			{
				continue;
			}
			for (Widget child : children)
			{
				dumpWidgetsAtMouse(child, mouse, depth + 1);
			}
		}
	}

	private void dumpOrbChildren()
	{
		dumpOneOrbChildren("HP", Orbs.HP_ORB_CONTAINER);
		dumpOneOrbChildren("Prayer", Orbs.PRAYER_ORB_CONTAINER);
		dumpOneOrbChildren("Run", Orbs.RUN_ORB_CONTAINER);
		dumpOneOrbChildren("Spec", Orbs.SPEC_ORB_CONTAINER);
	}

	private void dumpOneOrbChildren(String label, com.compactorbs.widget.TargetWidget target)
	{
		Widget container = widgetManager.getTargetWidget(target);
		if (container == null)
		{
			log.info("[orb dump] {} orb container not found", label);
			return;
		}

		log.info("[orb dump] --- {} orb container (id={}, {}x{}) ---", label, container.getId(), container.getWidth(), container.getHeight());
		dumpAllChildren(container, 0);
		log.info("[orb dump] --- end {} ---", label);

		// Temporary, alongside the above - saves the RAW backing sprite
		// (pre-rotation/flip) so it can be inspected directly, to check
		// whether the SOUTH/EAST 1-2px collar clip is baked into the
		// source asset itself (e.g. asymmetric padding) versus only
		// appearing in our own rotated/flipped copy or draw position.
		Widget backing = client.getWidget(target.getBackingId());
		if (backing != null && backing.getSpriteId() != -1)
		{
			saveSprite(backing.getSpriteId());
		}
	}

	private void dumpZoneWidgets()
	{
		Widget orb = widgetManager.getTargetWidget(Orbs.HP_ORB_CONTAINER);

		dumpOneZoneWidget("getOrbsParent()", widgetManager.getOrbsParent());
		dumpOneZoneWidget("orb.getParent() [inner]", orb != null ? orb.getParent() : null);
		dumpOneZoneWidget("getMapParent()", widgetManager.getMapParent());
		dumpOneZoneWidget("ORBS_UNIVERSE", client.getWidget(Minimap.ORBS_UNIVERSE.getComponentId()));

		log.info("[zone dump] --- full ancestor chain from HP orb ---");
		Widget w = orb != null ? orb.getParent() : null;
		int depth = 0;
		while (w != null && depth < 8)
		{
			log.info("[zone dump] chain depth={} id={} original={}x{} current={}x{}",
				depth, w.getId(), w.getOriginalWidth(), w.getOriginalHeight(), w.getWidth(), w.getHeight());
			w = w.getParent();
			depth++;
		}
		log.info("[zone dump] --- end chain ---");
	}

	private void dumpOneZoneWidget(String label, Widget widget)
	{
		if (widget == null)
		{
			log.info("[zone dump] {} -> null", label);
			return;
		}
		log.info("[zone dump] {} -> id={} original={}x{} current={}x{}",
			label, widget.getId(), widget.getOriginalWidth(), widget.getOriginalHeight(), widget.getWidth(), widget.getHeight());
	}

	private void dumpAllChildren(Widget widget, int depth)
	{
		if (widget == null || depth > 12)
		{
			return;
		}

		log.info("[orb dump] {}type={} id={} spriteId={} text='{}' pos=({},{}) size={}x{} hidden={}",
			"  ".repeat(depth), widget.getType(), widget.getId(), widget.getSpriteId(), widget.getText(),
			widget.getRelativeX(), widget.getRelativeY(),
			widget.getWidth(), widget.getHeight(), widget.isHidden());

		for (Widget[] children : new Widget[][]{
			widget.getChildren(), widget.getStaticChildren(), widget.getDynamicChildren(), widget.getNestedChildren()})
		{
			if (children == null)
			{
				continue;
			}
			for (Widget child : children)
			{
				dumpAllChildren(child, depth + 1);
			}
		}
	}
}
