# LeagueCraft
A League of Legends fan game in Minecraft, updated from 1.20.2 to Minecraft 26.3, and on its way to becoming a third-person hero fighter in the style of Marvel Rivals or Plants vs. Zombies: Garden Warfare.

## What's in this repo

| Folder | What | Details |
|---|---|---|
| [`datapack/`](datapack/) | The game: queue, teams, Nexus, turrets, minions and 15 champions. An [MC-Build](https://github.com/mc-build/mc-build) 4.2 project (`.mcb` source in `src/`). Also contains the Animated Java rigs in `generated/leaguecraft-animated-java/`, which load as a second datapack. | [datapack/README.md](datapack/README.md), [conversion report](datapack/CONVERSION_REPORT.md) |
| [`resourcepack/`](resourcepack/) | Models, textures, sounds, fonts and HUD (resource pack format 97), plus the ShoulderCam shaders for the over-the-shoulder third-person camera and crosshair (`assets/tpscam`, `assets/minecraft/shaders`). | Covered in the datapack README; camera settings in `assets/tpscam/shaders/include/camera.glsl` |
| [`plugin/`](plugin/) | Paper 26.3 plugin (0.7.0) that takes over what a datapack can only fake: abilities on keys 1–4, the third-person view, the basic attack (melee cone, ranged hitscan with ammo), the velocity behind dashes and knockups, and a placeholder model on every champion. The datapack needs it. | [plugin/README.md](plugin/README.md) |

## Quick start

1. **Build the datapack.** The built `data/` folder isn't committed; it's generated from `src/`:
   ```
   npm i -g mc-build@4.2.0
   cd datapack && mcb build
   ```
2. **Install on a 26.3 world/server.** In `<world>/datapacks/`, add `datapack/` and `datapack/generated/leaguecraft-animated-java/` as two separate packs.
3. **Resource pack.** Zip the *contents* of `resourcepack/` (so `pack.mcmeta` is at the zip root) and use it as the server resource pack, or put it in `resourcepacks/`.
4. **Plugin (Paper server).** Champions' basic attack, ability keys and summoner spell 1 come from it. Build with `cd plugin && mvn package` (JDK 25), then copy `target/LeagueCraft-0.7.0.jar` into `plugins/`. CI builds it on every push too (Actions → Build → `LeagueCraft-plugin` artifact).
5. **Paper settings.** In `spigot.yml`, set every `entity-activation-range` to `0`, or far-away timers and minions stop (see the datapack README, *Server settings*).
6. **Play in F5.** The over-the-shoulder camera needs third-person view.

Champion stats (HP, damage, speed, attack speed, range, ammo, reload…) are one chart in `datapack/src/settings.mcb`: change it, `mcb build`, then `/reload`.
