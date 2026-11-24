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
