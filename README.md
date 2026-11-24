# Keep Inventory — modern version/loader ports

Independent reimplementation of the original `keepinventory` mod behavior, targeting four uncovered version+loader cells:

| Variant | MC | Loader | Java |
|---|---|---|---|
| `26.3-fabric` | 26.3 | Fabric | 25 |
| `26.3-neoforge` | 26.3 | NeoForge | 25 |
| `1.21.11-fabric` | 1.21.11 | Fabric | 21 |
| `1.21.1-fabric` | 1.21.1 | Fabric | 21 |

Behavior contract: [BEHAVIOR_CONTRACT.md](BEHAVIOR_CONTRACT.md).

Each variant builds two jars:
- product jar: `keepinventory-<variant>-1.0.0.jar`
- test-only probe jar: `keepinventory-testmod-<variant>-1.0.0.jar`

The probe is never shipped to CurseForge. CI runs a real client, gives the player a unique item, teleports them high, kills them, waits for respawn, and asserts the item survived — plus a negative control with the product jar absent.

Attribution: original mod by Itamio, MIT. Original source (now unavailable): https://github.com/Sekai0NI0itamio/keep-inventory · CurseForge: https://www.curseforge.com/minecraft/mc-mods/keep-inventory

## Verification (green)

- Build workflow: https://github.com/FrameFlags/KeepInventoryPorts/actions/runs/45375610722 — all four variants `success`.
- In-game behavior workflow: https://github.com/FrameFlags/KeepInventoryPorts/actions/runs/45375610114 — all eight legs `success` (4 product-present + 4 negative control).

Every green leg emitted `KEEPINVENTORY_TEST_RESULT: PASS product=true` with the marker present after a real death (`death=` tick) and respawn (`respawn=` tick). Every negative leg emitted `FAIL product=false` with `rule_false_at_join,rule_not_restored,item_lost_on_death`.

CI uses `mc-runtime-test` to launch a real Minecraft **client** per cell (Xvfb), with the probe jar staged in `mods/` and the product jar staged only on product-present legs (negative-control legs omit it). The probe:
1. observes `KEEP_INVENTORY=true` at join (product present),
2. sets the rule to `false`, waits 21 ticks, and asserts the mod re-asserted it to `true` (the exact 20-tick loop from the original),
3. gives the player a uniquely named paper, teleports far from spawn, kills the player (`Player.die`), confirms a real death via the vanilla death stat, drives respawn server-side, and checks the marker survived.

`1.21.x` probe jars are passed through Loom's `remapJar` (named -> intermediary) before CI stages them; `26.3` uses Mojang names directly; `26.3-neoforge` uses ModDevGradle.
