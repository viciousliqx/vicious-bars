# Vicious Bars

Configurable vital bars for Old School RuneScape, built on RuneLite. Blends [Status
Bars](https://github.com/runelite/runelite/wiki/Status-Bars)' docked layout with
[Custom Vital Bars](https://runelite.net/plugin-hub/show/custom-vital-bars)'
customizability, plus orb click-through and a few extras of its own.

## Requirements

**Developed and tested exclusively with:**

- Display Mode: **Resizable**
- Interface Style: **Modern**

Docked (Fixed) placement locates the real inventory panel widget to anchor against, and
that widget has only ever been confirmed present under this exact combination - other
combinations (Fixed display, Classic interface) have not been tested, and docked bars
may not appear under them at all.

**Floating placement is unaffected by this** - it doesn't look up the panel widget at
all, so floating bars should work under any display/interface combination.

## Bars

Hitpoints, Prayer, Special Attack, Run Energy, Warmth (Wintertodt only, off by
default), and Enemy Hitpoints (current target only, off by default).

## Features

### Placement

- **Bar type**: Docked (pinned to a side of the inventory panel, stacked via Dock
  slot) or Floating (drag anywhere, alt-click like any other RuneLite overlay)
- **Dock settings**: side, slot (stacking order), slot orientation, and Clamp slot
  (stretches a bar's Length to match the panel's own dimension)
- Two bars can share a single dock slot: give them the same Dock side + Dock slot, set
  one bar's Dock slot orientation to Start and the other's to End, and size each
  Length to a bit over half the panel's own length

### Appearance

- Thickness, Length, and Opacity (0-100) per bar
- A rounded stone-toned frame drawn around every bar, independent of the bar's own
  Opacity
- Content justification plus Content offset (a per-pixel nudge) for the icon/counter

### Orb modes (Hitpoints, Prayer, Special Attack, Run Energy)

- **Vanilla** - the bar is a display-only readout; the real orb is untouched
- **Mirrored** - the bar's own icon becomes a second, independent click target for the
  same action the orb performs (quick-prayers, special attack, toggle run, cure
  poison); the real orb keeps working too
- **Merged** - same click-through as Mirrored, but the real orb is hidden while this
  bar is doing its job, so the bar is the only thing on screen

Every orb click is wired to the real, captured menu-action parameters for that exact
widget - none of it is simulated or guessed.

### Prayer

- **Metronome** - a marker sweeps the bar once per game tick, with the icon flashing
  exactly on each real tick, as a prayer-flicking aid. Direction and "only in combat"
  are both configurable. The flash itself is tied to the real tick event, not the
  (self-correcting) interpolated sweep, so the cue that actually matters is never
  subject to drift
- Dynamic bar color distinguishes active/inactive Quick Prayers

### Special Attack

- **Threshold** - the icon glows gold once Special Attack reaches a configurable
  percentage, but only while in combat
- Dynamic bar color distinguishes queued (armed) vs. normal

### Run Energy

- Dynamic bar color distinguishes toggled on/off

### Hitpoints

- Dynamic bar color for poisoned/envenomed/diseased
- **Show heal preview** - hovering a food or potion item previews how much it would
  heal, shown as a lighter shade beyond your current Hitpoints. Requires the **Item
  Stats** plugin to be enabled, since that's what actually computes the heal amount
- Clicking the icon in Mirrored/Merged mode cures poison, exactly like the real orb

## Known limitations

- Enemy Hitpoints' counter shows the game's own coarse health ratio (not a real HP
  number) for most monsters - a hard game-data limitation, not a bug
- Only verified under Resizable/Modern - see Requirements

## Credits

- Layout concept inspired by RuneLite's own **Status Bars** plugin and **Custom Vital
  Bars** for its customizability
- The Hitpoints heal preview is adapted directly from Status Bars' own
  `StatusBarsOverlay.getRestoreValue()` - same menu-entry detection, same
  `ItemStatChangesService` call

## License

BSD 2-Clause - see [LICENSE](LICENSE)
