> **0.8.9.26:** New openings use the PDF-based SCC scene and actual satellite event assets described in [SCC rebuild](SCC-REBUILD.md). The original 0.8.9.25 description below remains applicable to active legacy openings, which are deliberately preserved in place. The confirmed club room for the new scene is SCC 121.

# Opening: an ordinary day at College of DuPage

## Playable sequence

1. Wake on the bed in a private daytime home. Read the bedside lectern schedule.
2. Leave through the front doorway; follow the neighbourhood sidewalk to College of DuPage, Glen Ellyn.
3. Enter the BIC classroom. Right-click the instructor or front lectern for a short design exercise (eight seconds of classroom time, with free movement; leaving the room pauses it).
4. Walk to the SCC club room. Right-click the club member or club lectern. Optional classmate conversations are available.
5. Look east through the windows: a bright particle trail descends beyond campus. Wreckage appears, with a delayed impact sound. This is not a damaging world explosion.
6. Follow the northern sidewalk east to the service path and smoke. Right-click the central satellite core. Its pulse marks the investigation; it gives no inventory item.
7. Walk home and right-click the bed to advance to that evening. The scene switches to the original private nighttime house; walk to the bedroom to trigger the existing break-in and abduction.
8. Continue through the existing blackout, safe beach landing, nine-second wake presentation, offshore cameo, and coastal chapters.

The investigation is the narrative reason Pantheris targets the player. The player-facing script never names Pantheris or explains the satellite's owner, purpose or the abduction motive. The real college, its students and staff are not depicted as responsible for the fictional crash or abduction.

## Campus adaptation and references

This is a **compact fictionalized game adaptation**, not a scale-accurate or navigational recreation of the Glen Ellyn campus. Recognisable cues are the College of DuPage/Glen Ellyn signage and BIC, SCC and SRC names, plus classroom and computing-club spaces. Building positions, interiors, the neighbourhood, lesson, club dialogue, service path and crash location are fictional. No claim is made about the real club's room, schedule or membership; all NPCs are fictional roles. The falling object is outside the fictional campus boundary.

Official reference: College of DuPage, Maps and Directions, https://www.cod.edu/about/visitor-information/maps-directions and the linked campus map https://www.cod.edu/about/visitor-information/maps-directions/pdf/full-campus.pdf. These sources establish the Glen Ellyn setting and building names, not the invented plot or room layouts.

## Saves, isolation and replay

- New automatic intros and explicit operator replays use the new day. Completed saves are not reset. Existing active .24 HOME saves without the `CampusOpening` flag continue the old TV-to-bed opening.
- HOME stage ID remains 1; no persisted/network stage IDs are renumbered. Campus substeps are saved as `CampusStep` and `CampusAge`, with `SatelliteInvestigated` set only by inspecting wreckage.
- Daytime scenes use the private `timelocked:intro_campus` dimension at fixed morning; nighttime keeps `timelocked:intro_home`. Earth time/weather and other players are not changed.
- Each player has a separate 192-block slot. The generated set is bounded by X -8..144, Z -12..92 relative to the slot origin. Paths have solid support and free movement. Leaving its bounds returns the player to their own home, not another player's set.
- Inventory is never cleared. Existing skip/rescue/landing nonce, mode/gravity restoration and saved coastal chapter progress remain owned by `IntroSequence`.
- Named village NPCs have no AI/trades and cannot be damaged. They persist with the active daytime scene and are removed on evening, skip, recovery or beach transition. Departed NPCs in unloaded chunks are rejected when those chunks next load; NPC interactions are restricted to their owning player.
- No satellite fragments must be collected, so a dropped/missing item cannot block progression. Main-hand range-checked interactions prevent distant or offhand duplicate progression.

## Testing

Use a backed-up Story world or a fresh Story world. Existing completed players can use the operator command `/timelockedintro start` from the Overworld. This does not automatically update an intro already in progress.

Build with Java 21:

```sh
./gradlew spotlessApply
./gradlew build
python tools/run_story_audit.py --accept-eula
```

The audit flag explicitly accepts Minecraft's EULA for a temporary dedicated test server. `CampusOpeningAudit` checks the playable progression, no early bed/club bypass, path support, range checks, saved investigation and fall progress, named NPC cleanup, old-save compatibility, inventory and handoff to the existing night sequence. It is not a graphical client playthrough. Visual campus fidelity, NPC placement, the falling trail's visibility, shader exposure and pacing still require a real client check.

This update does not repair the separately reported vestibule holes; the exact affected openings have not been identified.
