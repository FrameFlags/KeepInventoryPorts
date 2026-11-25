# Keep Inventory — version/loader ports

Independent reimplementation of the original `keepinventory` mod behavior. It sets the vanilla `keepInventory` gamerule to `true` on every level at server start and re-asserts it every 20 ticks — **silently**, without a chat/log broadcast.

| Variant | MC | Loader | Java |
|---|---|---|---|
| `26.3-fabric` | 26.3 | Fabric | 25 |
| `26.3-neoforge` | 26.3 | NeoForge | 25 |
| `1.21.11-fabric` | 1.21.11 | Fabric | 21 |
| `1.21.8-fabric` | 1.21.8 | Fabric | 21 |
| `1.21.8-neoforge` | 1.21.8 | NeoForge | 21 |
| `1.21.4-fabric` | 1.21.4 | Fabric | 21 |
| `1.21.4-neoforge` | 1.21.4 | NeoForge | 21 |
| `1.21.1-fabric` | 1.21.1 | Fabric | 21 |
| `1.21.1-forge` | 1.21.1 | Forge | 21 |
| `1.20.1-fabric` | 1.20.1 | Fabric | 21 (compiled for Java 17) |
| `1.20.1-forge` | 1.20.1 | Forge | 17 |
| `1.12.2-forge` | 1.12.2 | Forge | 8 |

Behavior contract: [BEHAVIOR_CONTRACT.md](BEHAVIOR_CONTRACT.md).

Each variant builds two jars:
- product jar: `keepinventory-<variant>-1.0.0.jar`
- test-only probe jar: `keepinventory-testmod-<variant>-1.0.0.jar`

The probe is never shipped to CurseForge. CI stages the probe jar in `mods/` on every leg and the product jar only on product-present legs. The probe:

1. observes `keepInventory=true` at join (product present),
2. sets the rule to `false`, waits 21 ticks, and asserts the product re-asserted it to `true` (the exact 20-tick loop from the original),
3. gives the player a uniquely named paper, teleports far from spawn, kills them with a real death, confirms the death via the vanilla death stat, drives respawn server-side, and checks the marker survived.

Every leg also fails if the log contains a gamerule-change broadcast, so the silent setter is verified in-game rather than assumed.

For all cells except `1.12.2-forge`, the client is launched with `headlesshq/mc-runtime-test@5.0.0` under Xvfb. For `1.12.2-forge`, CI boots a real Forge 14.23.5.2847 dedicated server and connects a Mineflayer bot so the same server-side probe and negative control still run.

Fabric probe jars on 1.20.1/1.21.x are remapped to intermediary with Loom `remapTestmodJar`; 1.20.1-forge and 1.12.2-forge probe jars are reobfuscated with ForgeGradle, while `1.21.1-forge` ships official/Mojang names; `26.3` uses Mojang names directly; NeoForge variants use ModDevGradle/NeoGradle.

Attribution: original mod by Itamio, MIT. Original source (now unavailable): https://github.com/Sekai0NI0itamio/keep-inventory · CurseForge: https://www.curseforge.com/minecraft/mc-mods/keep-inventory

## Verification

Green CI run IDs are recorded after each full pass in this file and in the commit history. `build.yml` verifies all 12 variants produce both jars; `behavior.yml` verifies 24 legs (12 product-present + 12 negative control) with no gamerule broadcast.
