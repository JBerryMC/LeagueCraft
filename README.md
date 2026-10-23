# LeagueCraft
A League of Legends fan game in Minecraft, updated from 1.20.2 to Minecraft 26.3.

## What's in this repo

| Folder | What | Details |
|---|---|---|
| [`datapack/`](datapack/) | The game: queue, teams, Nexus, turrets, minions and 15 champions. An [MC-Build](https://github.com/mc-build/mc-build) 4.2 project (`.mcb` source in `src/`). Also contains the Animated Java rigs in `generated/leaguecraft-animated-java/`, which load as a second datapack. | [datapack/README.md](datapack/README.md), [conversion report](datapack/CONVERSION_REPORT.md) |
| [`resourcepack/`](resourcepack/) | Models, textures, sounds, fonts and HUD (resource pack format 97). | Covered in the datapack README |
| [`plugin/`](plugin/) | Paper 26.3 plugin (0.3.1) that takes over what a datapack can only fake, starting with League-style WASD controls. The datapack still runs on its own without it. | [plugin/README.md](plugin/README.md) |

## Quick start

1. **Build the datapack.** The built `data/` folder isn't committed; it's generated from `src/`:
   ```
   npm i -g mc-build@4.2.0
   cd datapack && mcb build
   ```
2. **Install on a 26.3 world/server.** In `<world>/datapacks/`, add `datapack/` and `datapack/generated/leaguecraft-animated-java/` as two separate packs.
3. **Resource pack.** Zip the *contents* of `resourcepack/` (so `pack.mcmeta` is at the zip root) and use it as the server resource pack, or put it in `resourcepacks/`.
4. **Plugin (optional, Paper only).** Build with `cd plugin && mvn package` (JDK 25), then copy `target/LeagueCraft-0.3.1.jar` into `plugins/`.
