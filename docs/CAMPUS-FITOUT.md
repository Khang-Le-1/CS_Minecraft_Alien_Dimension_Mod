# Campus fit-out and fresh-world facility fixes — 0.8.9.28

## Changes

- Home: pitched deepslate roof, chimney, timber porch, side reading bay, custom upholstered seating and dining furniture. Existing bedroom, kitchen, bathroom and story interactions remain; morning and evening use the same home architecture.
- SCC 123: lower ceiling, 24 modeled desks and chairs, clear circulation aisles, teaching wall and board away from the windows. SCC 121 gets modeled workstations and lower emitting ceiling panels.
- Surroundings: six additional residential buildings, ten stationary authored neighbour/student NPCs, a shuttle shelter, planting and a marked parking lot in front of SCC. The lot includes six modeled cars, accessible bays, a pedestrian crossing, sidewalk and graded driveway. The existing uphill side approach remains.
- Natural SARC generation now installs the five framed transition-door thresholds after its structure templates. Previously only manual `/sarcsite` placement installed them: a fresh world could genuinely have missing doors.
- Fresh natural/manual drawing-layout SARC facilities also receive level-15 recessed floor-light tiles at eligible open-floor grid positions. These use the facility floor texture with narrow inset diffuser strips, not full sea-lantern cubes. Furniture, containers, hazard strips and nonstandard flooring are not replaced.

## Existing saves and commands

New/replayed intros select a separate v28 scene bank. Already occupied v25, v26 and v27 intros keep their own coordinates and quest anchors; they are not silently rebuilt under the player. Install the JAR before starting a new world to test automatic SARC placement. Installing it does not retrofit an already generated facility, even if that world was originally created recently.

For an existing affected facility, back up the world and stand inside its registered drawing-layout SARC lobby with operator permission:

```text
/sarcdoors
/sarcdoors repair confirm
/sarclights architectural
/sarclights architectural confirm
```

`/sarcdoors` displays repair instructions; the lighting command without `confirm` previews its count without changes. These repairs require all affected chunks to be loaded and do not generate chunks; obstruction checks protect custom doorway edits and occupied thresholds. Lighting skips ineligible floor cells. The older `/sarclights brighten` sea-lantern option remains for compatibility but is not the new automatic lighting path.

## Story and reference scope

The supplied SCC floor plan, Exit 1, building-name sign, confirmed SCC 121 club location, paired entrance doors, Park St → College Rd route and actual game satellite fall/crater are retained. Home-to-campus distances, neighbouring buildings, parking geometry, furnishings, NPCs and timetable are fictionalized, not a surveyed reconstruction. Ambient NPCs are stationary, not simulated traffic or daily schedules.

## Verification and limitations

The test-only dedicated-server suite checks the new home/room/parking geometry, clear routes and door interaction, actor ownership/cleanup, actual satellite sequence, day/night transition, and older saved-intro compatibility. It exercises the same chunk-clipped doorway helper now called by natural generation. Existing repair tests cover containers, obstructions, preview-only behavior and repeat safety.

A synchronous vanilla `LevelLightEngine` test uses real generated classroom blocks without sky light. The separate recessed-light test measures an open-floor fixture grid; it is not a minimum-light guarantee for every furnished SARC room. Measurements and test results are included with the release.

A graphical Minecraft client and the user's shader pack were unavailable. Geometry previews and server block-light tests do **not** certify final shader brightness or visual appearance. No gamma/fullbright setting is forced. Please check the new world in-game with the same shader settings used for the reported screenshots.
