# Keep Inventory — behavior contract (test-first baseline)

Recovered from the published 2.6 KB NeoForge 1.21.11 jar (`asd.itamio.keepinventory.KeepInventoryMod`), whose original GitHub repo is no longer reachable.

## Product contract

- `modId`: `keepinventory`; package `asd.itamio.keepinventory`; license MIT; author Itamio.
- On server start, iterate `server.getAllLevels()` and set `GameRules.KEEP_INVENTORY` to `true` for every level.
- On `ServerTickEvent.Post` / `ServerTickEvents.END_SERVER_TICK`, increment a counter; **every 20 ticks** re-check each level and re-set the rule to `true` if it was changed to `false`.
- The rule change must be silent: no chat message and no gamerule-broadcast log line. Modern variants pass `null` as the server argument to the gamerule setter; 1.12.2 uses the legacy string setter, which has no broadcast overload.
- No other behavior: no client code, no config, no item logic. The rule itself is what preserves items on death.
- Negative control is created by **omitting the product jar**; a defensive `-Dkip.probe.only=true` gate also disables event registration but is not used by CI.

## In-game scenario (all twelve cells)

1. A real player body is provided: a Minecraft client via mc-runtime-test under Xvfb for every cell except `1.12.2-forge`, which boots a real Forge dedicated server and connects a Mineflayer bot.
2. Probe observes player join; asserts `KEEP_INVENTORY == true` at join.
3. Probe gives the player a unique named paper stack (`KIP_PROBE_MARKER_7F3A`) in slot 0 and records an inventory signature.
4. Probe deliberately sets the rule to `false`, then waits 21 ticks and asserts the product restored it to `true`.
5. Probe teleports the player ~300 blocks up and applies lethal damage. mc-runtime-test auto-respawns on the death screen.
6. On respawn, probe checks the unique marker stack is still present and emits exactly one terminal result line.
7. CI fails the leg if any gamerule-change broadcast appears in the collected logs.

## PASS / FAIL marker

```
KEEPINVENTORY_TEST_RESULT: PASS product=true reasons= before=... after=... death=<tick> respawn=<tick>
KEEPINVENTORY_TEST_RESULT: FAIL product=false reasons=rule_false_at_join,item_lost_on_death,... before=... after=... death=<tick> respawn=<tick>
```

The probe writes `kip-result.txt` (root and `run/`) before any process exit, and logs the same line. CI greps both. Progress lines use `[KIP_PROBE]` and are never counted as results.

## Negative control

With the product jar absent, a fresh world defaults to `KEEP_INVENTORY=false`. The probe must report `FAIL product=false` with `rule_false_at_join`, `rule_not_restored`, and `item_lost_on_death`. If the negative control passes, the test is invalid and the whole plan stops.

## What this test proves / does not prove

Proves: real player joined, rule set at startup, 20-tick re-assertion works, player died (confirmed via vanilla death-stat delta) and respawned, the unique item survived. Does not prove: fall-damage physics (lethal damage is applied directly), rule persistence across server restarts, modpack compatibility, or client rendering.

## Target cells

| Cell | MC | Loader | Java | Runtime API pin | Gradle |
|---|---|---|---|---|---|
| A | 26.3 | Fabric | 25 | Fabric API 0.165.0 | 9.8.2 |
| B | 26.3 | NeoForge | 25 | NeoForge 26.3.0.101 | 9.2.1 |
| C | 1.21.11 | Fabric | 21 | Fabric API 0.141.4 | 9.5.0 |
| D | 1.21.1 | Fabric | 21 | Fabric API 0.107.0 | 9.5.0 |
| E | 1.21.8 | Fabric | 21 | Fabric API 0.134.0 | 9.5.0 |
| F | 1.21.8 | NeoForge | 21 | NeoForge 21.8.57 | 9.2.1 |
| G | 1.21.4 | Fabric | 21 | Fabric API 0.119.4 | 9.5.0 |
| H | 1.21.4 | NeoForge | 21 | NeoForge 21.4.161 | 9.2.1 |
| I | 1.21.1 | Forge | 21 | Forge 52.1.21 | 8.14.5 |
| J | 1.20.1 | Fabric | 21 (compile 17) | Fabric API 0.92.11 | 9.5.1 |
| K | 1.20.1 | Forge | 17 | Forge 47.4.20 | 8.14.5 |
| L | 1.12.2 | Forge | 8 | Forge 14.23.5.2847 | 4.10.3 |
