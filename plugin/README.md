# LeagueCraft plugin (Paper 26.3)

This plugin is a server-side helper for the LeagueCraft datapack. The datapack still owns all the champion logic. The plugin takes over the jobs a datapack can only fake, starting with **input**: it gives LeagueCraft League of Legends' WASD controls.

## Install

1. Put `LeagueCraft-0.3.1.jar` in your Paper 26.3 server's `plugins/` folder. It replaces any older version; delete the old `plugins/LeagueCraft/config.yml` so the new one is written.
2. Use the LeagueCraft 26.3 datapack from the same release. Its HUD has the summoner-spell label the plugin writes to.
3. Start the server. Run `/lc` to check that it shows **Datapack detected: yes**.

It runs alongside ShoulderCam; they don't depend on each other.

## Controls

| Action | Key |
|---|---|
| Move | WASD |
| Basic attack | Left click |
| Q | **Right click** (or 1) |
| W | **Shift** (or 2) |
| E | **E** once rebound (or 3) |
| R | **R** once rebound (or 4) |
| Summoner spell 1 | **Q** (drop) |
| Summoner spell 2 | **F** (swap hands) |

That's League's WASD default layout. Minecraft only sends the server *actions*, not raw keys. Opening the inventory (E) is never sent, and R has no action at all. So E and R go through the "Hotbar Slot 3/4" actions, and each player sets them up once in **Options → Controls → Key Binds**:

- **Inventory** → I (frees up E)
- **Hotbar Slot 3** → E
- **Hotbar Slot 4** → R

Until they do, the number keys 1–4 cast Q/W/E/R. Players get this tip in chat the first time they play a champion, and any time with `/lc keys`. The HUD labels follow their binds: once Hotbar Slot 3 is on E, the label under E shows "E".

**How it works**
- While you play a champion, your hand rests on **hotbar key 8**, holding a copy of your weapon. The copy has your weapon's model and none of the ability data.
- Every hotbar key is a button. The plugin cancels the slot change, the client snaps back to 8, and pressing the same key again casts again.
- The scroll wheel can only move one slot from 8, to keys 7 or 9, and those are never mapped, so scrolling can't cast.
- The drop key and F never move or throw items. F also works on the first press after picking a champion; the datapack alone ignores that one.
- When the match ends or you stop being a champion, the weapon copy is removed.
- The plugin never decides what an ability does. It runs the datapack's own functions (`_champion:abilities/use_q` … `use_r`, `_champion:spell/check`, `_champion:spell/check_2`) as that player. Cooldowns, mana, crowd control and the HUD work exactly as before.
- Shift still crouches. In LeagueCraft that only slows you while the key is held, so tap it.

## Config (`plugins/LeagueCraft/config.yml`)

Every mapping can be changed. Run `/lc reload` afterwards; no restart needed.

```yaml
keys:                      # actions: q, w, e, r, summoner-1, summoner-2, none
  right-click: q
  sneak: w
  drop: summoner-1
  swap-hands: summoner-2
  hotbar-1: q
  hotbar-2: w
  hotbar-3: e
  hotbar-4: r
  hotbar-5: none           # free for later (items, recall ...)
  hotbar-6: none
hud-labels:                # "key.<control>" shows the player's own key; text is shown as written
  q: "M2"
  w: "SH"
  e: "key.hotbar.3"
  r: "key.hotbar.4"
  summoner-1: "key.drop"
first-time-tip: true
require-match: true        # only while score _ingame (objective "game") is 1
ability-items: [minecraft:phantom_membrane]
cast-function: "_champion:abilities/use_%s"
summoner-1-function: "_champion:spell/check"
summoner-2-function: "_champion:spell/check_2"
debug: false
```

Hotbar keys 7, 8 and 9 are reserved: the hand rests on 8 and the scroll wheel reaches 7 and 9. The plugin warns if the config maps them. Left click stays the datapack's basic attack. To test champions outside a match, set `require-match: false`.

## Commands

| Command | Who | What |
|---|---|---|
| `/lc keys` | everyone | The controls and the one-time key bind setup |
| `/lc` | everyone | Status: datapack detected, match running, current key mapping |
| `/lc dev` | `leaguecraft.dev` (op) | Toggle dev mode: every input you make is echoed to your chat with what it did (cast, ignored and why, or handled by the datapack). Per player, off after a restart. From the console: `execute as <player> run lc dev` |
| `/lc reload` | `leaguecraft.admin` (op) | Re-read config.yml |
| `/lc debug on\|off` | `leaguecraft.admin` (op) | Log each cast to the console |

## Tested

Tested on Paper 26.3 build 214 with the LeagueCraft 26.3 datapack, playing Ezreal, with real key presses and mouse clicks from a 26.3 client.

- **Hand position:** the hand moved to key 8 with the weapon copy. The weapon showed in first person, and no ability was highlighted on the HUD.
- **Repeat presses:** pressing 1 twice cast Q twice. Pressing 3 twice reached the plugin both times, and the second E was refused by its cooldown.
- **Other inputs:** right click, Shift, 4, Q and F cast Q, W, R, Flash and Barrier.
- **Scrolling:** scrolling three times cast nothing, and the hand stayed on 8.
- **Match end:** after the match ended, the weapon copy was gone from slot 8.
- **HUD and tip:** the HUD showed M2, SH, 3, 4, Q and F. The setup tip arrived once.

## Building from source

```
mvn package          # needs JDK 25; output: target/LeagueCraft-0.3.1.jar
```

## Where this goes next

- **Basic attack without the interaction entity.** The datapack re-summons an invisible interaction entity in front of every champion, every tick, just to catch clicks. Paper click events can replace it, which removes an entity per player per tick.
- **Aim.** Use ShoulderCam's aim API so skill shots go where the crosshair points in the over-the-shoulder view.
- **Minions.** Move the per-tick minion work, including the rig updates behind the lag you saw, into the plugin.
