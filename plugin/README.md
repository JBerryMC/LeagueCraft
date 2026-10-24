# LeagueCraft plugin (Paper 26.3)

This plugin is a server-side helper for the LeagueCraft datapack. The datapack still owns all the champion logic. The plugin takes over the jobs a datapack can only fake: **input** (abilities on keys 1–4), the **over-the-shoulder third-person view**, and a **hitscan basic attack** aimed with its crosshair. It's the first step towards a third-person hero fighter in the style of Marvel Rivals or Plants vs. Zombies: Garden Warfare.

## Install

1. Put `LeagueCraft-0.6.3.jar` in your Paper 26.3 server's `plugins/` folder. It replaces any older version. An existing `plugins/LeagueCraft/config.yml` is kept; settings older versions used (the right click/Shift keys and their M2/SH labels) are updated automatically, and the server log says what changed. New settings use their defaults until you add them, or delete the file to get a fresh one with every option.
2. Use the LeagueCraft 26.3 datapack and resource pack from the same commit. The HUD has the summoner-spell label and the crosshair slot the plugin writes to, and the resource pack carries the ShoulderCam shaders. The datapack now needs the plugin: champions' basic attack and summoner spell 1 come from it.
3. Start the server. Run `/lc` to check that it shows **Datapack detected: yes**.

The ShoulderCam resource pack is now merged into the LeagueCraft resource pack; don't load it separately as well.

## Controls

| Action | Key |
|---|---|
| Move | WASD |
| Basic attack | **Left click** (hitscan from the crosshair) |
| Q / W / E / R | **1 / 2 / 3 / 4** |
| Summoner spell 1 | **Q** (drop) |
| Summoner spell 2 | **F** (swap hands) |
| Zoom | **Right click** (hold) |

Champions can't sprint or crouch, so Sprint and Shift are free for abilities later.

Abilities follow each player's **Hotbar Slot 1–4** binds (Options → Controls → Key Binds), so players can move them to other keys, and the HUD label under each ability shows their key. Players get the controls in chat the first time they play a champion, and any time with `/lc keys`.

**How it works**
- While you play a champion, your hand rests on **hotbar key 8**, holding a copy of your weapon. The copy has your weapon's model and none of the ability data.
- Every hotbar key is a button. The plugin cancels the slot change, the client snaps back to 8, and pressing the same key again casts again.
- The scroll wheel can only move one slot from 8, to keys 7 or 9, and those are never mapped, so scrolling can't cast.
- The drop key and F never move or throw items. F also works on the first press after picking a champion; the datapack alone ignores that one.
- When the match ends or you stop being a champion, the weapon copy is removed.
- The plugin never decides what an ability does. It runs the datapack's own functions (`_champion:abilities/use_q` … `use_r`, `_champion:spell/check`, `_champion:spell/check_2`) as that player. Cooldowns, mana, crowd control and the HUD work exactly as before.

## Third person

With the LeagueCraft resource pack loaded, F5 becomes an over-the-shoulder camera (the ShoulderCam shaders shift the view 0.8 blocks right and 0.25 up). While a player plays a champion the plugin:

- sets the camera 3 blocks back (the `minecraft:camera_distance` attribute; vanilla 4) and puts it back to vanilla afterwards
- draws a crosshair in the middle of the screen, which vanilla hides in F5

A server can't switch a player's perspective, so players press **F5** themselves. The key tip says so. The shaders shift the first-person view too, so play in F5 with this pack.

**How the crosshair gets drawn.** ShoulderCam's text shader moves any GUI glyph in colour `#4E5C24` to the centre of the screen. The action bar would be the obvious place for it, but the champion HUD redraws the action bar every tick. So every HUD now ends with `lol:main crosshair.<uuid>` (like the key labels), the plugin writes the glyph there, and 14 px of negative space after it keeps the HUD from shifting.

## Zoom (right click)

Holding right click pulls the camera in, like aiming down sights in a third-person shooter. A server can't change the field of view, so zoom moves the camera instead (the `camera_distance` attribute). The hitscan aims the same zoomed or not: the crosshair ray doesn't depend on camera distance.

How far each champion zooms is a stat in `zoom:` in the config: `melee` (2.2 blocks) for champions the datapack tags `melee`, `ranged` (1.2) for the rest, and `zoom.champions.<name>` for any one champion. Holding right click makes the client send a click every 4 ticks: the first one sets the zoomed distance, and about 0.3 s after the last one the normal distance is set back. That's one change each way, nothing while the zoom is held. `/lc dev` shows zoom in/out with the distance and where it came from.

## No sprinting or crouching

The client controls movement, so neither can be blocked outright; the plugin takes away what they do:

- **Sprint:** Minecraft only lets players sprint with more than 6 hunger, so champions' hunger stays at 6 (the resource pack hides the hunger bar). The datapack no longer gives champions Saturation, which would refill it.
- **Shift:** `sneaking_speed` is set to 1, so crouching doesn't slow you, and the server never records the crouch, so other players don't see it and your hitbox stays full height. Your own view still crouches while Shift is held, and your client still won't let you walk off an edge while holding it.

Both are on by default (`movement.disable-sprint`, `movement.disable-sneak`), and both keys still reach the plugin, ready to be used for abilities.

## Basic attack (hitscan)

Left click fires instantly along the crosshair. Champions the datapack tags `melee` punch (3.5 blocks); everyone else shoots (48 blocks). For now the result is a chat message; the Animated Java attack animations and damage come later:

> **Shot** hit **Zombie** (enemy) 12.3m · **Punch** missed (3.5m range)

**Aiming.** The crosshair ray runs parallel to where the player looks, offset 0.8 right and 0.25 up from their eye. The shader only shifts the picture, so camera distance and wall clipping never change what the crosshair points at. Each attack does two traces:
1. Along the crosshair ray, starting beside the eye so nothing between the camera and the player counts, to find what's under the crosshair.
2. From the eye to that point. This is what actually hits, so a player can't shoot round a corner only the camera can see past.

**Targets** follow the datapack's basic attack rules: anything except the `#minecraft:unvalid` entity types and entities tagged `unvalid`, plus structure interactions (tag `lol.structure`: turrets and Nexus). Allies (same scoreboard team) are hit too and reported as "(ally)".

**Inputs.** Left click on air, on a block and on an entity all fire; one click fires once even though Minecraft can report it twice. Hitting an entity in vanilla reach is cancelled, so vanilla melee damage never applies. The arm swing the client plays when you press drop isn't counted as a click.

This replaced the datapack's old basic attack, which re-summoned an invisible interaction entity in front of every champion every tick and attacked when you hit it (and cast summoner 1 when you right-clicked it). That entity and its advancements are gone from the datapack.

`/lc dev` shows each attack's aim start and aim point next to what it hit.

## Rig test (`/lc rigtest`)

A first step towards Animated Java models for champions, played by the plugin. It mounts the Aatrox ultimate rig (34 bones) on you as passengers, to check that a model riding the player follows smoothly. The vanilla `/ride` command refuses to put anything on a player, but the plugin API allows it, and each client moves passengers along with its own view of the player.

- Only the rest pose shows; nothing animates yet. Your own body is hidden (armor and held items still show). Press F5 to see the model.
- Each tick the plugin works out which animation *would* play: `idle`, `walk`, `sprint`, `sneak`, `sneak_walk`, `jump` or `fall`. With `/lc dev` on, every change shows in chat with the keys you're holding, e.g. `animation idle → walk (keys: W A)`.
- The bones are moved down by your hitbox height (passengers sit on top of it) and turn with your yaw. If a teleport, death or world change drops them, they're put back the next tick.
- The rig comes from `src/main/resources/rigs/aatrox.json`, extracted from the Animated Java datapack with `python3 plugin/tools/extract_aj_rig.py aatrox`.

## Config (`plugins/LeagueCraft/config.yml`)

Every mapping can be changed. Run `/lc reload` afterwards; no restart needed.

```yaml
keys:                      # actions: q, w, e, r, summoner-1, summoner-2, none
  drop: summoner-1
  swap-hands: summoner-2
  hotbar-1: q
  hotbar-2: w
  hotbar-3: e
  hotbar-4: r
  hotbar-5: none           # free for later (items, recall ...)
  hotbar-6: none
hud-labels:                # "key.<control>" shows the player's own key; text is shown as written
  q: "key.hotbar.1"
  w: "key.hotbar.2"
  e: "key.hotbar.3"
  r: "key.hotbar.4"
  summoner-1: "key.drop"
first-time-tip: true
require-match: true        # only while score _ingame (objective "game") is 1
ability-items: [minecraft:phantom_membrane]
cast-function: "_champion:abilities/use_%s"
summoner-1-function: "_champion:spell/check"
summoner-2-function: "_champion:spell/check_2"
third-person:
  enabled: true
  camera-distance: 3.0     # blocks behind the player (vanilla 4)
  shoulder: 0.8            # must match TPS_SHOULDER in the resource pack's camera.glsl
  height: 0.25             # must match TPS_HEIGHT
  crosshair: true
zoom:
  enabled: true
  melee: 2.2               # camera distance while zoomed, champions tagged "melee"
  ranged: 1.2              # everyone else
  champions: {}            # per champion by name, e.g. {jhin: 0.8}
movement:
  disable-sprint: true
  disable-sneak: true
basic-attack:
  melee-range: 3.5         # champions tagged "melee"
  ranged-range: 48.0
  cooldown-ticks: 10
debug: false
```

Hotbar keys 7, 8 and 9 are reserved: the hand rests on 8 and the scroll wheel reaches 7 and 9. The plugin warns if the config maps them. To test champions outside a match, set `require-match: false`.

## Commands

| Command | Who | What |
|---|---|---|
| `/lc keys` | everyone | The controls |
| `/lc` | everyone | Status: datapack detected, match running, current key mapping |
| `/lc dev` | `leaguecraft.dev` (op) | Toggle dev mode: every input you make is echoed to your chat with what it did (cast, ignored and why, or what a basic attack hit). Per player, off after a restart. From the console: `execute as <player> run lc dev` |
| `/lc rigtest` | `leaguecraft.dev` (op) | Mount the Aatrox ultimate rig on yourself (see *Rig test*). Run again to take it off |
| `/lc reload` | `leaguecraft.admin` (op) | Re-read config.yml |
| `/lc debug on\|off` | `leaguecraft.admin` (op) | Log each cast to the console |

## Tested

The hitscan basic attack (0.4.0) was tested in game and works. Keys 1–4 and the rig test (0.5–0.6.1) were played and work. 0.6.2's zoom and the sprint/crouch lock are compiled and unit-tested by CI but not yet played.

Controls (0.3.x) were tested on Paper 26.3 build 214 with the LeagueCraft 26.3 datapack, playing Ezreal, with real key presses and mouse clicks from a 26.3 client.

- **Hand position:** the hand moved to key 8 with the weapon copy. The weapon showed in first person, and no ability was highlighted on the HUD.
- **Repeat presses:** pressing 1 twice cast Q twice. Pressing 3 twice reached the plugin both times, and the second E was refused by its cooldown.
- **Other inputs:** right click, Shift, 4, Q and F cast Q, W, R, Flash and Barrier.
- **Scrolling:** scrolling three times cast nothing, and the hand stayed on 8.
- **Match end:** after the match ended, the weapon copy was gone from slot 8.
- **HUD and tip:** the HUD showed M2, SH, 3, 4, Q and F. The setup tip arrived once.

## Building from source

```
mvn package          # needs JDK 25; runs the tests; output: target/LeagueCraft-0.6.3.jar
```

## Where this goes next

- **Attack animations and damage.** Play an Animated Java animation per champion on left click, and deal the datapack's basic attack damage to what the hitscan hit.
- **Aim for abilities.** Point skill shots along the same crosshair ray.
- **Minions.** Move the per-tick minion work, including the rig updates behind the lag you saw, into the plugin.
