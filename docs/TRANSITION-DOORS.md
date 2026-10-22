# Transition doors and brighter reception — 0.8.9.24

## Five doorway locations

All coordinates below are drawing-local block positions, relative to the saved facility origin.

| Transition                 | Opening plane    | Door leaves      | Facing |
| -------------------------- | ---------------- | ---------------- | ------ |
| Recessed stairway exit     | X 87, Z 355–357  | One at Z 356     | West   |
| South end of lobby         | Z 336, X 78–81   | Two at X 79–80   | North  |
| North end of lobby         | Z 185, X 78–81   | Two at X 79–80   | South  |
| Lab-wing corridor entrance | Z 170, X 78–81   | Two at X 79–80   | North  |
| Lobby to tram platform     | X 153, Z 269–276 | Two at Z 272–273 | East   |

Doors occupy Y 2–3. Solid jambs and headers fill the remaining opening through Y 5.
The two-block-high door opening is intentional. Doors have steel panels, a narrow
wired-glass window and a horizontal push bar. They use the existing smooth animated
door renderer, but have **no proximity sensor, lock, card or power requirement**.
Right-click either half from either side. Paired leaves open and close together.
They remain in the position chosen by the player; there is no timed auto-close.
This is not automatic collision-to-open behaviour.

The lab-wing entrance is a transition into the corridor serving the labs; individual
lab/security doors and their existing locks are retained. Tram doors are on the
pedestrian threshold, not the guideway or boarding area. Surface entrance doors,
the lift, alarm mountings and Chapter 2 receptionist remain unchanged.

## Existing saves

1. Close Minecraft and back up the world. Replace the old TIMELOCKED mod with the new JAR.
2. If the stairs are not yet at the .23 layout, run `/sarcstairs repair confirm` FIRST.
3. Keep all five doorway chunks loaded. Move players, mobs and dropped items clear of the openings.
4. As an operator, run `/sarcdoors` for instructions, then `/sarcdoors repair confirm`.
5. Preview brighter floor lighting with `/sarclights`, then apply `/sarclights brighten confirm`.

The door command preflights all locations before mutation. It refuses unloaded
chunks, unexpected blocks, block entities other than its already-installed doors,
missing supports and entities occupying newly filled cells. It never generates
chunks. Do not force a refused repair. Existing correctly installed doors retain
their open/powered state and block entities on repeat; an unchanged repeat edits
zero blocks. It does not teleport entities, replace inventory, reset story progress
or replay an arrival sequence.

The stair repair has deliberately not been taught to overwrite these new doorways.
Running it AFTER installing doors can refuse the edited exit: this is a safeguard,
not a reason to force it. Backups remain required. Authored-looking replacement
blocks cannot always be distinguished from original blocks.

New drawing-layout facility placements install the five doorways at completion.
Only that new-placement path can load its own doorway chunks. Existing facilities
are never silently rewritten. The arrival predicate excludes the lobby door planes,
so crossing into the room, rather than standing against a closed door, starts the
existing light/PA sequence. An arrival already started is not reset.

## Brighter floor lighting

`HubLighting` adds real level-15 sea-lantern floor lights every six blocks throughout
the main lobby, plus extra reception-frontage lights. This visibly changes the floor.
Up to 632 positions are candidates; obstructed/nonstandard tiles are skipped.
Only standard SARC floor/route tiles, polished andesite and light-gray concrete at
Y 1 with two air blocks above can be replaced. Containers, hazard trim, carpets,
holes and furniture are left alone. A player tile made from the same eligible
material is indistinguishable, so back up first. The command refuses before editing
if any candidate chunk is unloaded. It does not generate terrain.

Lighting installation is opt-in for both existing and new facilities. It stays on
independently of the original ceiling-light startup. It covers the main lobby, not
every enclosed lab or every facility floor. Check F3 block light at several walking
positions after installation; shadows/exposure still depend on client shaders.
No minimum light level at every point or graphical client playtest is claimed.

## Build from source

Requires Minecraft 1.21.1, NeoForge 21.1.252 and **JDK 21**.
No old TIMELOCKED JAR, IDE plugin, Python, Node or asset generator is needed for a normal build.
The Gradle wrapper and all Java/assets/data are included. First build needs internet
access for Gradle and NeoForge dependencies.

```bash
java -version
chmod +x gradlew
./gradlew build
```

Install `build/libs/timelocked-0.8.9.24-reveal-doors-dev.jar`, not the source archive
or a `-sources.jar`. Use `./gradlew runClient` for the configured development client.
Use a fresh folder for the complete source snapshot; never extract it over your
existing Git checkout. Apply the matching incremental patch to update a checkout.
The incremental patch does not touch `FacilityAlarms.java`, preserving the three
named constants you just added. Git branch names are not the mod's version.

## Code and checks

- `PushBarDoorBlock`: unlocked interaction and paired-leaf operation.
- `TransitionDoorways`: authored thresholds, safe repair, new-placement hook.
- `HubArrival`: excludes the actual lobby doorway planes.
- `HubLighting`: opt-in, floor-level lighting, preview and protected-block checks.
- `validation/TransitionDoorwaysAudit.java`: actual template openings, container/entity
  refusal, native collision, paired and reverse-side interaction, idempotence,
  guideway separation and arrival-trigger checks.
- `validation/HubLightingAudit.java`: preview, eligibility, placement, emission and repeat checks.

New production classes are included in the named-number gate. The pre-existing
whole-project numeric warnings and compiler deprecation/unchecked warnings remain;
no blanket suppression or claim of zero IntelliJ warnings is made.
