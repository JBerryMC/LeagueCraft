# LeagueCraft — MC-Build project for Minecraft 26.3

This is LeagueCraft BETA v2.0 updated from Minecraft 1.20.2 to **26.3** (data pack format 121, resource pack format 97). It's an MC-Build 4.2 project. Since 0.7.0 all of it lives in one namespace, `lc` (see *Layout*), with the champion stats in one chart (`src/settings.mcb`).

The game logic is unchanged. Commands and JSON were rewritten only where 26.3 reads them differently (see *What changed*). The few places where behaviour differs on purpose are listed under *Behaviour differences*.

## Install

1. Run `mcb build` in this folder (see *Building*); the built `data/` folder isn't committed.
2. In `<world>/datapacks/`, add two packs:
   - this folder (Minecraft only reads `pack.mcmeta` and `data/`)
   - `generated/leaguecraft-animated-java/`, copied as its own pack
3. Use `LeagueCraft-26.3-ResourcePack` as the resource pack. Load it as a server resource pack, or put it in `resourcepacks/`.
4. **Paper servers:** set every `entity-activation-range` in `spigot.yml` to `0` (see *Server settings*).

Don't load the old 1.20.2 packs alongside these.

Run it on a Paper server with the **LeagueCraft plugin** (`plugin/`). Champions' basic attack, ability keys 1–4, summoner spell 1 (drop key), and the velocity behind every dash and knockup come from the plugin.

## Layout

```
datapack/
├─ src/
│  ├─ settings.mcb      ← the champion stats chart: change it, mcb build, /reload
│  └─ lc/               ← everything else, namespace lc
│     ├─ core.mcb, core/            lc:core/...       loops, setup, champion setup, minions, towers, effects, motion
│     ├─ champion.mcb, champion/    lc:champion/...   HUD, basic attack, abilities input, damage, HP/mana
│     ├─ champions/<name>.mcb, champions/<name>/   lc:champions/<name>/...   the 15 champions
│     ├─ game.mcb, game/            lc:game/...       queue, match, lobby
│     ├─ menu.mcb, menu/            lc:menu/...       settings menu and champion pick
│     ├─ animation.mcb, animation/  lc:animation/...
│     └─ mobs.mcb                   lc:mobs/...       mob conversion, death and hurt
├─ static/              ← JSON copied into data/ on every build: damage types, item modifiers, tags
├─ generated/
│  └─ leaguecraft-animated-java/   ← Animated Java rigs (separate datapack, see below)
├─ extras/              ← _lol/functions/hud.json (not a real resource; kept for reference)
├─ mcb.config.js        FORMAT = 121, plus the static/ copy step
├─ pack.mcmeta          min_format/max_format 121
└─ data/                ← build output (don't edit)
```

MC-Build names every function after its file: `src/lc/champions/ashe/skills.mcb` holds `lc:champions/ashe/skills/...`. Before 0.7.0 each of these was its own namespace; the old ids map like this:

| Before | Now |
|---|---|
| `_lol:x` | `lc:core/x` |
| `_champion:x` | `lc:champion/x` |
| `_game:x` | `lc:game/x` |
| `_settings:x` | `lc:menu/x` |
| `_animation:x` | `lc:animation/x` |
| `_minecraft:x` | `lc:mobs/x` |
| `aatrox:x` (and every champion) | `lc:champions/aatrox/x` |

The 15 champion pick functions and the 15 champion setup functions are now one macro each: `lc:menu/champion/pick {name:"ashe"}` and `lc:core/champion/setup {name:"ashe"}`. What's special about a champion (lore, resource, starting mana, select sounds, counters) is in its own `lc:champions/<name>/pick`.

`static/` holds the damage types, the damage-type tags, the `minecraft:` tags and the item modifiers. MC-Build writes damage types to the wrong folder. The item modifiers were JSON arrays in 1.20.2 and are now `minecraft:sequence` objects, so they could move into `.mcb` later; they were left in `static/` to keep this update small.

## Building

```
npm i -g mc-build@4.2.0
mcb build        # or: mcb watch
```

## Champion stats (`src/settings.mcb`)

Every champion's stats are in one chart: four tables (health and resource, offense and movement, abilities, basic attack) with one row per champion. Change a number, run `mcb build`, then `/reload` in game. The chart is applied again to everyone already playing that champion (current HP and mana are kept, capped at the new maximum), and to everyone who picks one. Each column is explained above its table.

The plugin reads the basic-attack columns (`attack_speed`, `range`, `ammo`, `reload`) from scores the chart sets. Melee champions have 0 ammo, so they never reload.

## Moving players (`lc:core/motion`)

Dashes and knockups used to mount the player on an entity. A datapack can't set a player's velocity, so now it asks the plugin to: `lc:core/motion/push {x, y, z, ticks}` (thousandths of a block per tick; `ticks` above 1 holds it), `throw {speed, ticks}` (the way the execution faces) and `halt`. Dashes keep their dash entity, and the player follows it with `lc:core/motion/follow` every tick. *Dash Looking* faces the camera; *Dash Moving* (Aatrox E) faces the WASD keys (`lc:core/motion/face_moving`, through player input predicates). The comments in `src/lc/core/motion.mcb` explain the rest.

## Server settings

Paper (Spigot) stops ticking entities that are far from every player (`entity-activation-range` in `spigot.yml`; 16 blocks for "misc" entities, 32 for monsters). Area effect clouds count as misc, and the datapack uses them as timers and cooldowns, so far from players they stopped counting down; minions far away froze too. Set every range to 0, which keeps everything ticking:

```yaml
world-settings:
  default:
    entity-activation-range:
      animals: 0
      monsters: 0
      raiders: 0
      misc: 0
      water: 0
      villagers: 0
      flying-monsters: 0
```

## What changed for 26.3

**Commands**
- Item NBT became data components:
  - `CustomModelData` → `custom_model_data={floats:[N]}`
  - `tag` → `custom_data`
  - display name and lore → `custom_name`/`lore` as text components
  - `HideFlags` → `tooltip_display`
  - `Unbreakable` → `unbreakable`
  - book pages → `written_book_content`
- Item arguments in commands:
  - `give`/`item replace` use `item[component=...]`
  - `clear` and item tests use `item[custom_data~{...}]`
- Entity data changes:
  - `HandItems`/`ArmorItems`/`SaddleItem` → `equipment`
  - `Attributes` → `attributes` (ids no longer have the `generic.` prefix)
  - `AngryAt`/`AngerTime` → `angry_at`/`anger_end_time`
  - armor stand `Pose` rotations padded to 3 numbers
- Data paths into items (`Inventory[...]`, `SelectedItem`, `Items[...]`, `item.tag...`) now point at `components."minecraft:custom_data"` and `components."minecraft:custom_model_data".floats[0]`.
- Text is now SNBT instead of JSON-in-a-string. This covers `tellraw`, `title`, `bossbar`, team and scoreboard names, `CustomName`, and text display `text`. Storage values used with `interpret` were converted too, because 26.3 no longer parses JSON strings.
- `click_event`/`hover_event` use the new field names.
- Particle options use the new syntax (`dust{color,scale}`, `block{block_state}`, and so on). Dust scale is capped at 4.
- Other command changes:
  - Effect amplifiers are capped at 127.
  - Gamerules are snake_case.
  - `spawnpoint`/`setworldspawn` take a pitch.
  - `/replaceitem` became `item replace`.
  - `data remove storage` needs a path.
  - Renamed ids: `grass` → `short_grass`, `scute` → `turtle_scute`, boats, potions and flower tags.

**JSON**
- **Predicates:**
  - `condition` → `type`
  - entity predicate fields use the new component-style keys (`minecraft:entity_type`, `minecraft:nbt`, …)
  - lists are wrapped in `all_of`
  - item predicates check `minecraft:custom_data`
  - damage-type tags need the `#` prefix
- **Item modifiers:**
  - `function` → `type`
  - top-level lists → `minecraft:sequence`
  - `set_nbt` → `set_custom_data` + `set_components`
  - `copy_nbt` → `copy_custom_data`, except the weapon-model copy, which became `set_custom_model_data` reading from storage
- **Loot tables:** `conditions`/`functions` → `condition`/`modifier`.
- **Advancements:** entity fields wrapped in `entity_properties`.

**Drop key casting.** In 1.20.2, pressing drop while holding an ability item re-cast it. That only worked because of an engine quirk: the dropped stack stayed in your hand with 0 items and still carried its NBT, so `lc:champion/hold_<x>` kept matching. From 1.20.5, empty stacks carry no data, so the drop key silently stopped casting. `lc:core/loop` now starts with one extra line that puts the ability items back before the champion checks run. The drop key casts exactly as it did in 1.20.2; this was tested on both versions with real key presses.

## Changes for the plugin

Apart from the version update, these changes were made so the datapack works with the LeagueCraft plugin (`plugin/`), which it now needs:

- **Key labels.** The summoner spell 1 label comes from storage (`lol:main keybind.d.<uuid>`) like the Q/W/E/R labels, instead of a fixed right-click icon. `lc:champion/abilities/keybind/default` sets all five to the plugin's controls (hotbar 1–4 and the drop key, as keybind components that show each player's binds). The old drop-to-cast labels, rewritten on every cast by `keybind/use_q`…`use_r`, are gone.
- **Crosshair slot.** Every champion HUD ends with `lol:main crosshair.<uuid>`, which the plugin fills with ShoulderCam's crosshair glyph (zero width, so the HUD doesn't move).
- **Basic attack.** The invisible "attack" interaction entity re-summoned in front of every champion each tick is gone, with the `lc:champion/aa` and `lc:champion/spell` advancements and the `lc:champion/aa` predicate. The plugin's hitscan is the basic attack, and the drop key casts summoner 1. `lc:champion/aa/check` stays for minions.
- **No Saturation for champions.** The plugin keeps champions' hunger at 6 so they can't sprint.
- **No suffocation check.** `lc:core/main_10t` no longer pushes champions whose eyes or feet are inside a block upwards (`lc:core/effect/stuck` is gone).

**Area-effect-cloud timers.** 157 summon commands create area-effect clouds as timers and cooldowns. 1.20.2 drew no visible particles for them, but 26.3 gives potion-less clouds blue swirls. They now get `custom_particle:{type:"minecraft:block",block_state:"minecraft:air"}`, which spawns nothing. `Radius` is left at its default because some `dx/dy/dz` selectors test against the cloud's hitbox.

## Behaviour differences from 1.20.2

| What | Why |
|---|---|
| `lc:champions/jhin/skills/r.end` now loads and runs | In 1.20.2 it failed to load because it used `/replaceitem`, removed in 1.17. Jhin's R now cleans up properly when it ends. |
| Jhin's R hover uses a `gravity` attribute modifier (`lol:jhin_hover`) plus slow falling instead of Levitation 255 | Effect amplifiers are capped at 127 now, and Levitation 255 relied on the old byte overflow. |
| 4 incomplete commands removed | `lc:game/tick` (dangling `positioned`), `lc:core/uninstall` (`run` with nothing after it), Yasuo's champion setup (`item modify` with no modifier), one Animated Java leaf. Each is left as a `# [26.3] ...` comment. 1.20.2 either ignored them or failed the whole function. |

## Resource pack (`LeagueCraft-26.3-ResourcePack`)

- `pack.mcmeta` now uses format 97.
- The CustomModelData overrides on 16 items became `assets/minecraft/items/*.json` `range_dispatch` definitions. Each threshold picks the same model the old override list picked.
- 26.3 rejects some models that 1.20.2 accepted, which had made whole items fail to render. Fixes:
  - Removed 903 Blockbench faces textured `#missing`, and 1 element that was left with no faces.
  - Moved 72 out-of-range face UVs back into 0–16.
  - Models that mixed block and item textures now use copies under `block/lc_item/`, since 26.3 requires one atlas per item model.
- Vanilla textures that moved were moved too:
  - arrow
  - horse saddle and armor slot sprites
  - `widgets.png` and the effect backgrounds were removed, because 26.3 no longer has them.

The full list is in `CONVERSION_REPORT.md`.

## Validation

All of this was checked on the official 26.3 server and client jars:

| Check | Result |
|---|---|
| Server load (functions, predicates, item modifiers, loot tables, advancements, tags) | 0 errors |
| All 265 macro (`$`) lines | all parse |
| All 50 distinct `/summon` commands, read back with `/data get` | fields applied. 3 are damage-number and knock-up items that despawn by design within a few ticks; their data was checked separately. |
| Entity serialization warnings | 0 |
| Ability keys, pressed in a real 26.3 client | Number key, then drop, casts like 1.20.2 (vanilla server) |
| 26.3 client resource-pack load | Only the same missing-file warnings as 1.20.2 (below), plus two intentionally empty models |
| In game (headless client screenshots, compared with 1.20.2) | Lobby menu hotbar, Ezreal HUD (portrait, ability icons, HP and mana bars), Ezreal's gauntlet, Ezreal Q firing and hitting a minion, minion models, Aatrox, tower and Tibbers rigs |

## Known gaps carried over from the original

These were already missing or unfinished in the 1.20.2 pack and are unchanged:

- **Missing functions:**
  - `lc:champions/ezreal/skills/e.missile`
  - `lc:champions/vayne/skills/aa`, `lc:champions/vayne/skills/aa.animation`, `lc:champions/vayne/skills/r.silverfish`
  - `lc:champions/ashe/skills/r.particle.center`
  - `lc:champions/yasuo/set_item`
  - `lc:core/lang/champion/vie.annie`, `vie.leona`, `vie.lux`
- **Missing assets:**
  - Vayne and Jhin `icon_x4` textures
  - siege and super minion models
  - `ashe/aa_q_active`
  - one Vayne sound
- **Unfinished champions:** Jhin and Vayne.
- **Minion lag and invisible minions.** Not changed here; see the separate notes on the minion rigs.

## Animated Java rigs

`generated/leaguecraft-animated-java/` was converted with the same rules as the main pack. It loads and renders on 26.3, but it's still generated output. Once you have the `.ajblueprint` files again, re-exporting from Animated Java for 26.3 is cleaner than editing it.
