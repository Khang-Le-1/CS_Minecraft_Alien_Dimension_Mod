# Facility spawn protection

SARC's registered facility volume is protected from ordinary mob spawning, even while its lights are off. This applies in both Story and normal worlds, to hostile mobs, ambient mobs such as bats, and passive animals. Natural spawning and ordinary/trial spawners use the same policy. Spawn-egg/command finalization also respects the protected volume; there is no blanket `COMMAND` or `EVENT` bypass.

## Scope and exceptions

- All floors in the saved facility footprint are protected, not only the lobby. Current drawing and legacy footprints use the existing `BlueprintRooms` dimensions and saved site origin.
- The protected bounds are half-open: the minimum corner is included; the outer maximum faces and blocks above/below the facility remain outside. The beach, boardwalk, access road and other dimensions are not globally disabled.
- Authored structure placement of SARC scientists remains allowed. Encounter waves already created directly by `EncounterManager` are unchanged. Meltdown hazard mobs carry an explicit marker before event finalization so the authored hazard still works; arbitrary vanilla events are not exempt.
- Existing mobs are **not removed**. This does not prevent a mob from walking in, teleporting in or loading from a save. There is no destructive entity-join sweep. Direct entity insertion by another mod that bypasses spawn-finalization events is outside this spawn-hook policy.
- Existing registered facilities gain protection without regeneration, a new world, lighting changes or a game-rule change.

## Contributor code map

`FacilitySpawnProtection` denies `MobSpawnEvent.PositionCheck` and uses `FinalizeSpawnEvent.setSpawnCancelled(true)` at finalization. Canceling only the finalization event would skip initialization but would still allow the mob to appear, so both spawn and event cancellation are set.

Only registered site anchors are examined. No terrain planning, chunk loading, new site allocation or saved-data writes occur in the lookup. Server ticks publish immutable volume snapshots for world-generation workers, which must not access mutable saved-data maps. Server-thread lookups refresh immediately; snapshots are cleared when the server stops.

The existing numeric gate includes the new class. `FacilitySpawnAudit.java` adds native boundary, dimension, spawn-reason, passive/ambient/hostile, real-event-bus, actual insertion-denial, NPC exception, legacy-layout and worker-snapshot checks. The existing audit runner includes it; no Python helper was added.

## Verify

```sh
./gradlew build
python3 tools/check_project.py
npm run lint:docs
python3 tools/run_story_audit.py --accept-eula
```

Java 21 is required. The native audit starts a temporary local server; `--accept-eula` is explicit Minecraft EULA acceptance. On Windows use `gradlew.bat` and `py` instead of the Unix launchers. Python/Node are optional checks, not runtime dependencies.

In a client, confirm dark rooms and an ordinary spawner cannot create mobs inside the facility, while a comparable outside location behaves normally. Also confirm existing scientists, saved pets and authored encounters still work. Headless checks do not substitute for this graphical playtest.
