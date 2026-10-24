# LeagueCraft plugin (Paper 26.3)

This plugin is a server-side helper for the LeagueCraft datapack. The datapack still owns all the champion logic. The plugin takes over the jobs a datapack can only fake: **input** (abilities on keys 1–4), the **over-the-shoulder third-person view**, and a **hitscan basic attack** aimed with its crosshair. It's the first step towards a third-person hero fighter in the style of Marvel Rivals or Plants vs. Zombies: Garden Warfare.

## Install

1. Put `LeagueCraft-0.7.4.jar` in your Paper 26.3 server's `plugins/` folder. It replaces any older version. An existing `plugins/LeagueCraft/config.yml` is kept; settings older versions used (the right click/Shift keys and their M2/SH labels, zoom camera distances) are updated automatically, and the server log says what changed. New settings use their defaults until you add them, or delete the file to get a fresh one with every option.
2. Use the LeagueCraft 26.3 datapack and resource pack from the same commit. The HUD has the summoner-spell label and the crosshair slot the plugin writes to, and the resource pack carries the ShoulderCam shaders. The datapack now needs the plugin: champions' basic attack and summoner spell 1 come from it.
3. Start the server. Run `/lc` to check that it shows **Datapack detected: yes**.

The ShoulderCam resource pack is now merged into the LeagueCraft resource pack; don't load it separately as well.

## Controls

| Action | Key |
|---|---|
| Move | WASD |
| Basic attack | **Left click** (aimed with the crosshair) |
| Reload | **Sprint** (Ctrl), or by itself when the ammo runs out |
| Q / W / E / R | **1 / 2 / 3 / 4** |
| Summoner spell 1 | **Q** (drop) |
| Summoner spell 2 | **F** (swap hands) |
| Zoom | **Right click** (hold) |

Champions can't sprint or crouch, so Sprint reloads and Shift is free for abilities later.

Abilities follow each player's **Hotbar Slot 1–4** binds (Options → Controls → Key Binds), so players can move them to other keys, and the HUD label under each ability shows their key. Players get the controls in chat the first time they play a champion, and any time with `/lc keys`.

**How it works**
- While you play a champion, your hand rests on **hotbar key 8**, holding a copy of your weapon. The copy has your weapon's model and none of the ability data.
- Every hotbar key is a button. The plugin cancels the slot change, the client snaps back to 8, and pressing the same key again casts again.
- The scroll wheel can only move one slot from 8, to keys 7 or 9, and those are never mapped, so scrolling can't cast.
- The drop key and F never move or throw items. F also works on the first press after picking a champion; the datapack alone ignores that one.
- When the match ends or you stop being a champion, the weapon copy is removed.
- The plugin never decides what an ability does. It runs the datapack's own functions (`lc:champion/abilities/use_q` … `use_r`, `lc:champion/spell/check`, `lc:champion/spell/check_2`) as that player. Cooldowns, mana, crowd control and the HUD work exactly as before.

## Third person

With the LeagueCraft resource pack loaded, F5 becomes an over-the-shoulder camera (the ShoulderCam shaders shift the view 0.8 blocks right and 0.25 up). While a player plays a champion the plugin:

- sets the camera 3 blocks back (the `minecraft:camera_distance` attribute; vanilla 4) and puts it back to vanilla afterwards
- draws a crosshair in the middle of the screen, which vanilla hides in F5

A server can't switch a player's perspective, so players press **F5** themselves. The key tip says so. The shaders shift the first-person view too, so play in F5 with this pack.

**How the crosshair gets drawn.** ShoulderCam's text shader moves any GUI glyph in colour `#4E5C24` to the centre of the screen. The action bar would be the obvious place for it, but the champion HUD redraws the action bar every tick. So every HUD now ends with `lol:main crosshair.<uuid>` (like the key labels), the plugin writes the glyph there, and 14 px of negative space after it keeps the HUD from shifting.

## Zoom (right click)

Holding right click narrows your view, like aiming down sights, **without slowing you**. A server can't set the field of view, but the client works it out from your speed divided by your *walking speed*, a value nothing else uses (the server only reads it when you log in). So the plugin raises your walking speed while you zoom: the view narrows smoothly, at full frame rate, and you keep moving at full speed. The camera distance doesn't change, and the hitscan aims the same zoomed or not.

How much each champion zooms is `zoom.view` in the config: how much narrower the view gets: `melee` (0.15) for champions the datapack tags `melee`, `ranged` (0.3) for the rest, and `zoom.view.champions.<name>` for any one champion. The most is **0.49**: the client works the view out as (speed ÷ walking speed + 1) ÷ 2, which can't go below half, however high the walking speed. Bukkit caps walk speeds too low for that on fast champions, so above its cap the plugin sets Minecraft's own value directly. `zoom.slowdown` slows zoomed players too: `melee` 0.2 (20% slower), `ranged` 0.5 (50% slower), `zoom.slowdown.champions.<name>` for any one champion. Holding right click makes the client send a click every 4 ticks: the first one starts the zoom, and about 0.25 s after the last one it ends. `/lc dev` shows zoom in/out with how much and where it came from.

Players who turned **FOV Effects** down (Options → Accessibility) see less zoom, or none at 0%.

**Why not a "used" item?** Giving the weapon copy a food, bow, spyglass or shield behaviour would tell the server the moment right click is let go, instead of about 0.25 s later. But the client throws away every left click while an item is in use, so you couldn't basic-attack while zoomed. The bow's own zoom is also capped at 15% and only works on a real bow, and the spyglass zoom only works in first person. A carrot on a stick only counts clicks, the same 4-tick repeat the plugin already reads.

## No sprinting or crouching

The client controls movement, so neither can be blocked outright; the plugin takes away what they do:

- **Sprint:** Minecraft only lets players sprint with more than 6 hunger, so champions' hunger stays at 6 (the resource pack hides the hunger bar). The datapack no longer gives champions Saturation, which would refill it.
- **Shift:** `sneaking_speed` is set to 1, so crouching doesn't slow you, and the server never records the crouch, so other players don't see it and your hitbox stays full height. Your own view still crouches while Shift is held, and your client still won't let you walk off an edge while holding it.

Both are on by default (`movement.disable-sprint`, `movement.disable-sneak`), and both keys still reach the plugin, ready to be used for abilities.

## Basic attack

Left click is the champion's basic attack, aimed with the crosshair. It deals real damage through the datapack's own damage system (`lc:champion/aa/plugin.hit`), with armor, magic resist and every on-hit effect: Ashe's slow, Yone's mark, Zed's R, the passives and so on.

- **Melee** champions (datapack tag `melee`) hit **every enemy in a cone** in front of them: `basic-attack.melee-angle` wide (100°), as far as their range, and only what they can see (no hitting through walls).
- **Ranged** champions hit **the first enemy** on the crosshair and use **ammo**.
- **Allies** are never hit, and don't block shots.

Range, attack speed, ammo and reload time are **champion stats** in the datapack's stats chart (`datapack/src/settings.mcb`), which sets the scores the plugin reads (`aa.range`, `aa.spd`, `ammo.max`, `ammo.reload`). Abilities that change them, like Ezreal's passive or Aatrox's R, work straight away. The time between attacks is the datapack's own formula, 2 × (100 / `aa.spd`) ticks, doubled while crippled. Champions can't attack while crowd controlled or channeling an ability.

**Ammo.** A reload starts by itself when the last shot is fired, or early with **Sprint** (`basic-attack.reload-key`: sprint, sneak or none), and refills the magazine. Champions with 0 ammo in the chart (all melee champions) never run out. There's no ammo display yet; for the HUD the plugin keeps the scores `ammo.left` (shots left) and `ammo.reloading` (ticks until the reload ends) up to date.

**Effects.** A shot leaves a trail from the weapon to where it landed, in the particle each champion's old projectile left (Ashe light blue, Ezreal gold, Lux sparks, Annie flames…), with a burst where it hit. A melee attack shows a sweep in front, and a burst on each target hit. The attack sound is the champion's own (`lc:champion/aa/plugin.attack`).

**Aiming.** The crosshair ray runs parallel to where the player looks, offset 0.8 right and 0.25 up from their eye. The shader only shifts the picture, so camera distance and wall clipping never change what the crosshair points at. Each attack does two traces:
1. Along the crosshair ray, starting beside the eye so nothing between the camera and the player counts, to find what's under the crosshair.
2. From the eye to that point. This is what actually hits, so a player can't shoot round a corner only the camera can see past. Melee cones point the same way.

**Targets** follow the datapack's basic attack rules: enemies, except the `#minecraft:unvalid` entity types and entities tagged `unvalid`, plus enemy structure interactions (tag `lol.structure`: turrets and Nexus).

**Inputs.** Left click on air, on a block and on an entity all attack; one click attacks once even though Minecraft can report it twice. Hitting an entity in vanilla reach is cancelled, so vanilla melee damage never applies. The arm swing the client plays when you press drop isn't counted as a click.

`/lc dev` shows each attack (what it hit, aim start and point, the melee cone's targets) and each reload.

## Steady view

Champions walk about 30% faster than before (the `speed` column of the stats chart; vanilla is 0.1), and jump higher (`jump`, 0.55 instead of 0.42: about 1.6 blocks). Minecraft widens the view whenever you're faster than your walking speed and narrows it when you're slower, so at the end of every tick the plugin sets each champion's walking speed to their current speed. Minecraft sends a player's new speed only every other tick while a walking speed goes out at once, and a client holding the old speed against the new walking speed zooms in for a moment (the end of a cast's root did this). So when a champion's speed changes, the plugin sends it right away, just before the walking speed (if that ever fails, the walking speed is 0, which the client ignores, for 3 ticks). Channeling, root and stun take 99% of the speed rather than 100%: still frozen in practice, but a speed of exactly 0 would leave the client no way to show a zoom, so zooming survives casting an ability. Nothing that changes a champion's speed moves the camera: not their faster base speed, not Slowness, and not the "can't move" modifiers abilities put on while channeling or crowd controlled. Only the zoom does.

## Ground-targeted abilities

Abilities cast on the ground (Leona R, Soraka Q and E, Lux E, Annie R, Malphite R; the `aim` table of the stats chart) land where the **crosshair meets the ground**, the way third-person hero shooters aim them:

- The crosshair ray stops at the first block, so a target is **never behind a wall**.
- Aiming at a wall or a **ceiling** drops the target straight down from just in front of it, to the floor.
- Aiming past the ability's range (or at the sky) drops it from the end of the range.
- With no floor below (within 32 blocks), the ability **isn't cast**, and `/lc dev` says why.

The plugin puts a marker (`lc.ground_target`) there while the ability's datapack function runs; the ability then finds the ground from it as before.

**Aim, then cast.** Holding a key can't be detected (a hotbar key sends one press), so ground-targeted abilities work like a confirm cast (`abilities.ground-cast: confirm`): the **first press** shows where the ability will land, **pressing it again or left clicking** casts it there, and **Shift** cancels. Pressing another ability's key switches to it. An ability you can't cast yet (on cooldown, or not enough mana or energy) shows no indicator and casts straight away, so the datapack can say why. `quick` casts on the first press.

**Indicators.** The ability you're aiming shows a double circle where it will land, in its key's colour (Q yellow, W green, E blue, R red), sized by the `aim` table. With `abilities.indicators: always`, every other ready ground-targeted ability shows a circle too, all the time (`aimed`, the default, shows only the one you're aiming). Only you see them.

## Skill shots

Skill shots fire at what the third-person crosshair points at. When a champion casts, the plugin puts a marker (`lc.aim_target`) on that point for the datapack to aim at while the ability runs. The point comes from **ShoulderCam's aim API** (`ShoulderCam.aim().target(...)`) when ShoulderCam is installed, so it matches each player's camera mode (shader camera, Shoulder Surfing, or none); it's called by reflection, so LeagueCraft works without it and then uses its own crosshair ray. **Ezreal Q** is the first ability that uses it; the others still fire the way the champion faces, and switch over one by one (`facing entity @e[type=marker,tag=lc.aim_target,limit=1] feet`, see `lc:champions/ezreal/use_q`).

## Dashes and knockups

Dashes and knockups used to mount the player on another entity. Now the plugin sets velocities instead, because the datapack can't set a player's velocity and the API can. The datapack asks for one by leaving scores and a tag on the entity (`lc:core/motion`; see the comments there):

- **Knockups** (Yasuo and Yone tornadoes, Malphite R, Annie's Tibbers, Aatrox Q, Lee Sin R's collisions, Yone R) push the target straight up, as high as before. **Aatrox W** throws it at the chain, **Lee Sin R** kicks it away in a straight line, and **Yasuo R** holds his targets in the air.
- **Dashes** keep their dash entity and everything that runs on it (contact damage, arriving at a target, Yasuo's E→Q). The player **follows** it instead of riding it: every tick the velocity is the dash entity's own step, and the first tick without one the player stops dead, as dismounting used to do. (Steering by "distance to the dash entity" overshoots once the velocity reaches the client a tick or two late; following the step doesn't.)
- **Dash Looking** goes where the camera looks: Yasuo E (no longer aimed at the target), Lee Sin W, Yone Q3 and E. **Dash Moving** goes where your WASD keys point: Aatrox E. Dashes that home in on something still do: Lee Sin Q2, Leona E2, Malphite R, Yone's return and Zed R.

Flash and Ezreal's E during a dash still carry it on from where you blinked to (or end it, for Lee Sin's).

## Champion model (placeholder)

Every champion now wears a model, the way `/lc rigtest` mounts the Aatrox rig. Until each champion has an Animated Java rig it's a **mannequin wearing the player's skin**, and the player's own body is hidden. A rider sits on top of its vehicle, so the mannequin rides an **interaction entity** that rides the player: an interaction may have a negative height, and one `0.6 × scale − player height` tall (−1.2 at normal size) puts the mannequin's feet at the player's feet. The mannequin turns with the player every tick, copies their scale, and disappears while the player has Invisibility. Both entities are tagged `unvalid`; a shot at the model hits the player's hidden hitbox in the same place. Players wearing the test rig (`/lc rigtest`) don't get the mannequin.

**Ability rigs.** An ability can swap in its own Animated Java rig: the datapack summons it and runs `lc:core/model/wear`, and the plugin mounts it on the champion the same way (on an interaction as tall as the player, negative). Aatrox's R does this now, instead of riding an invisible horse.

Known limit: Minecraft draws any humanoid that rides something **sitting** (legs forward) and without its walk animation, so the mannequin looks seated. Animated Java rigs are item displays and don't have this problem.

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
cast-function: "lc:champion/abilities/use_%s"
summoner-1-function: "lc:champion/spell/check"
summoner-2-function: "lc:champion/spell/check_2"
third-person:
  enabled: true
  camera-distance: 3.0     # blocks behind the player (vanilla 4)
  shoulder: 0.8            # must match TPS_SHOULDER in the resource pack's camera.glsl
  height: 0.25             # must match TPS_HEIGHT
  crosshair: true
zoom:
  enabled: true
  view:
    melee: 0.15            # how much narrower the view gets (at most 0.49), champions tagged "melee"
    ranged: 0.3            # everyone else
    champions: {}          # per champion by name, e.g. {jhin: 0.4}
  slowdown:                # how much slower you walk while zoomed
    melee: 0.2
    ranged: 0.5
    champions: {}
movement:
  disable-sprint: true
  disable-sneak: true
basic-attack:
  melee-angle: 100         # how wide the melee cone is, in degrees
  reload-key: sprint       # sprint, sneak or none
abilities:
  ground-cast: confirm     # confirm (press, then press again / left click; Shift cancels) or quick
  indicators: aimed        # aimed (only the ability you're aiming) or always (every ready one)
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
| `/lc queue join` / `leave` | everyone | Join or leave the queue (same as the lobby item) |
| `/lc queue start` | `leaguecraft.admin` (op) | Start the queue now: champion select opens for everyone queued |
| `/lc queue skip` | `leaguecraft.admin` (op) | Skip the champion select countdown; the match starts |
| `/lc queue end` | `leaguecraft.admin` (op) | End the running match, or empty the queue if there's none |
| `/lc pick <champion>` | everyone during champion select; `leaguecraft.dev` any time | Pick a champion (tab completes the 15 names); refused if a teammate has it |
| `/lc team red\|blue` | everyone, outside a match | Join a team |
| `/lc surrender` | champions in a match | Surrender (your team's Nexus is destroyed) |
| `/lc dummy red\|blue\|clear` | `leaguecraft.dev` (op) | Spawn a practice dummy of that team where you stand, or remove them all |

## Menus

The datapack's menus (queue, champion select, settings) are items in your inventory, and an entry runs when its slot is empty. That relied on the player picking the item up, which stopped working. Now the plugin handles the click: it's cancelled (nothing ends up on your cursor or on the ground), the item is removed, and the datapack's menu runs at once, sees the empty slot and does what that entry always did.

## Tested

The hitscan basic attack (0.4.0) was tested in game and works. Keys 1–4 and the rig test (0.5–0.6.1) were played and work. The sprint/crouch lock (0.6.2) is compiled and unit-tested but not yet played.

0.7.0 and 0.7.1 haven’t been played with a client yet. On a headless copy of the test server (Paper 26.3 build 214) it loads with no errors, and these were checked with commands:
- the stats chart applies to an entity (HP, attack speed, range, reload ticks, weapon, speed 0.1344, the melee/ranged tag);
- the basic attack’s datapack hit takes 200 HP off a 1000 HP target for Ashe’s 200 attack damage, through the normal damage pipeline;
- velocity requests: a push up, a 20-tick straight throw (14.25 blocks in 19 ticks at 0.75/tick), a hold in place, and a 20-tick dash follow that ends exactly where its dash entity ended and stops dead;
- a mannequin on a negative-height interaction lands at its vehicle’s feet.
0.7.1 adds: every area effect cloud lives exactly its Duration (a Duration:20 cloud without WaitTime was measured still alive after 25 ticks, and gone on time with WaitTime:0). Melee cones, ranged hitscan with ammo and the reload key, the WASD dash direction, the steady view and zoom, ground targets and their indicators, Aatrox’s new R and the mannequin’s look need a real client.

Controls (0.3.x) were tested on Paper 26.3 build 214 with the LeagueCraft 26.3 datapack, playing Ezreal, with real key presses and mouse clicks from a 26.3 client.

- **Hand position:** the hand moved to key 8 with the weapon copy. The weapon showed in first person, and no ability was highlighted on the HUD.
- **Repeat presses:** pressing 1 twice cast Q twice. Pressing 3 twice reached the plugin both times, and the second E was refused by its cooldown.
- **Other inputs:** right click, Shift, 4, Q and F cast Q, W, R, Flash and Barrier.
- **Scrolling:** scrolling three times cast nothing, and the hand stayed on 8.
- **Match end:** after the match ended, the weapon copy was gone from slot 8.
- **HUD and tip:** the HUD showed M2, SH, 3, 4, Q and F. The setup tip arrived once.

## Building from source

```
mvn package          # needs JDK 25; runs the tests; output: target/LeagueCraft-0.7.4.jar
```

## Where this goes next

- **Attack animations and damage.** Play an Animated Java animation per champion on left click, and deal the datapack's basic attack damage to what the hitscan hit.
- **Aim for abilities.** Point skill shots along the same crosshair ray.
- **Minions.** Move the per-tick minion work, including the rig updates behind the lag you saw, into the plugin.
