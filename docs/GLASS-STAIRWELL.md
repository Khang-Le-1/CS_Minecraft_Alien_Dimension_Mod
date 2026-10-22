# Glass-slot stairs and the photographed recessed exit — 0.8.9.23

The five-wide **glass-surrounded surface slot** stays where it is. The stairs now exit **west through the recessed opening in the old stairwell room**, not into the north/south entrance hallway used by 0.8.9.20. The old spiral staircase is removed and its upper hole is capped.

## Route and identification

1. Enter the existing glass slot from its south-side approach.
2. Descend the upper flight north to a three-deep turning landing.
3. Turn onto the parallel return flight, now on the east side.
4. At the bottom, a short level landing turns west through the photographed recess. It does not require a trip down the entrance hallway and a loop back.

The photographed F3 position was approximately **898.231, 41, 1749.380**, looking east at block **909, 43, 1747**. Those features match the drawing's recessed opening at local **X=87, Z=355–357** and the rear wall at **X=93** (anchor **816, 39, 1392**). The rear wall is opened into the new landing. Production placement remains relative to each facility's anchor; it is not hardcoded to this one world.

Walk heights relative to the anchor remain surface **29**, midway **16**, and bottom **2**. Thirteen upper treads and fourteen lower treads provide half-block transitions, including both landings. The upper flight is five blocks wide and the return flight is three blocks wide. Stair sections have four blocks of clearance above each tread block.

Existing emergency-strip blocks light both flights and the landing without turning on the lobby logo, PA or arrival ceiling lights.

## What is removed or retained

- Every stone-brick tread in the old spiral shaft at local X=88–92, Z=353–359 is removed.
- Its vertical shaft above the low exit vestibule is filled, and its upper floor is restored with solid planks. It is no longer a second staircase or a fall hole.
- The superseded 0.8.9.20 hallway return flight, landing and bypass edits are restored before the new route is applied. Updating from that release does not leave an extra route behind.
- The original surface glass, exterior entrances, lift, retained alarm fixtures and unrelated facility staircases remain intact.
- Four drawing modules are updated: X=2–3, Y=0, Z=10–11. Other authored cells are unchanged, except the explicit restoration of sparse rock cells excavated by the previous repair.

## Fresh facilities

The route and old-shaft closure are baked into the shipped drawing modules. New facilities use the revised geometry automatically. Updating the JAR **does not silently excavate an existing world**.

## Existing facilities: back up first

1. Close Minecraft and copy the world save somewhere safe.
2. Install only `timelocked-0.8.9.23-first-contact-dev.jar` with the normal NeoForge dependencies. Remove the older TIMELOCKED JAR.
3. Enter the intended registered drawing-layout SARC facility in the Overworld. Keep the entrance chunks loaded.
4. Move players, mobs and dropped items out of both the old and new stair areas. Stand back in the main lobby, away from the entrance stairwell.
5. As an operator, run:

   ```text
   /sarcstairs
   /sarcstairs repair confirm
   ```

The same command accepts either the original facility or the authored 0.8.9.20 repair. It performs one complete preflight before changing blocks: unloaded chunks, occupied space, block entities and unexpected player edits cause refusal. No force option is provided. It does not reset progression, inventory, quests or arrival effects.

Sparse original cells may contain ordinary excavation rock or air. Previously excavated sparse cells belonging to the retired 0.8.9.20 route are filled with stone. Original-looking player replacement blocks cannot be distinguished from authored blocks, so a backup remains essential.

Successful repairs are idempotent: running the command again changes zero blocks. If it refuses, inspect the reported block rather than deleting builds to satisfy the command.

## Source and tests

`GlassStairwell.java` contains named drawing-local dimensions and the authoritative repair plan. Original modules under `structure/sarc/stairwell_baseline/` and the previous modified module under `structure/sarc/stairwell_v20/` supply repair preconditions. Neither is placed wholesale over a world.

The existing native runner includes `validation/GlassStairwellAudit.java`. It checks:

- equivalence between the Java repair plan and all four baked modules;
- unchanged surface glass and authored cells outside the repair;
- both travel directions through actual stair voxel shapes with a standing 0.6-wide / 1.8-high body;
- the west-facing recessed exit, removal of every old spiral tread, and the capped upper hole;
- upgrades from both the original facility and 0.8.9.20, with repeat repair changing zero blocks;
- operator permissions, occupied-space refusal, custom blocks, containers, unloaded chunks and actual emergency-light emission.

Run with JDK 21 and the existing helper:

```sh
python3 tools/run_story_audit.py --accept-eula
```

This is dedicated-server geometry verification, not a graphical client playthrough. Check the updated route in a backed-up test world before changing a shared server.

## Stair hallway lighting and arrival-trigger correction

The upper turning landing reaches local Z=333–335, inside the original lobby
arrival cuboid (Z<337 and Y<19). The arrival test now excludes the stair enclosure,
including both flights and the turning landing. The lobby boundary outside that
enclosure is unchanged. This fixes early light/PA startup without moving the
surface glass, recessed exit, landing, or lobby lights. The visible protrusion
has not been redesigned in this release.

Six permanent emergency strips light the bottom stair hallway: four in the
westbound exit ceiling and two along the return passage. They emit block light level
12 independently of arrival progression. The front doorway beacon is moved to the wall beside the lift; the front doorway siren joins it in this release. The other alarm
fixtures and walking width/headroom are retained. Shader appearance still needs a client check.

Existing 0.8.9.21 worlds use the same backed-up `/sarcstairs repair confirm`
command. The preflight recognises the exact preserved .21 templates; upgrading
an unchanged .21 stairwell changes only ten blocks (six light strips and the four alarm endpoints), and a second run changes zero.
This command does not reset an arrival sequence that has already fired.

### Beacon beside the lift

The targeted wall is the south wall beside the lift, local `(83, 5, 360)`.
The beacon occupies the adjacent air cell `(83, 5, 359)` facing north, replacing
the front doorway beacon at `(86, 6, 356)`. The supplied F3 image targets world
`(-1677, 74, 1512)` facing south; placement in other facilities remains relative
to their own anchor, never hardcoded to that world coordinate.

The repair preserves the beacon's active/manual/lit state and complete rotor
block-entity payload. It refuses containers and other block entities; only the
expected source/target alarm rotor is permitted. If both mounts already contain
beacons, the preflight refuses rather than discarding either one. The alarm index
keeps both mount positions for compatibility with worlds not yet repaired.

### Siren on the same wall

The front doorway siren at `(86, 6, 358)` now occupies `(85, 5, 359)` facing north,
next to the beacon at `(83, 5, 359)`. The backing clean-panel wall is retained.
Both fixtures preserve active/manual/lit state and complete rotor payloads during
repair, and both new mounts are linked to the existing facility alarm group.
Old mount coordinates stay indexed for worlds not yet repaired. Duplicate source
and destination alarms cause a preflight refusal rather than an overwrite.

An unchanged .22 facility needs only the same backed-up `/sarcstairs repair confirm`
command: it changes exactly the two siren endpoints. Receptionist placement and
Chapter 2 progress are separate and do not require this stair repair.
