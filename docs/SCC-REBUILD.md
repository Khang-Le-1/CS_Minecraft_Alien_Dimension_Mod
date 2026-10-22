# Seaton Computing Center rebuild — 0.8.9.26

## Reference and scope

The user supplied College of DuPage's **Seaton Computing Center (SCC), Level 1** PDF and an exterior entrance photograph. The club room is **SCC 121**, as confirmed by the user. The playable approach uses **Exit 1 → lobby 100 → south cross-corridor → SCC 121**. SCC 123 is used for the fictional class exercise; that is a game timetable, not a claim about the user's real classes.

`SccCampus.PLAN` traces the PDF's wall-fill paths at 4.5 PDF points per Minecraft block. It retains the angled west-side circulation, central room islands, east corridor, south cross-corridor, and mapped room door gaps. The map includes rooms 101–105, 107, 109–121, 123–124 and the two restrooms, rather than separate generic classroom boxes. Lobby 100 occupies the open southwest circulation area. The BIC link is labelled at the north end; the BIC interior is outside this scene.

The exterior adapts the supplied photo's blue-tinted glass corner and long glazed elevation, silver mullions, bronze band, white projecting roof/soffit, pink masonry, metal approach railings, blue posts, and planting. The large, static white **Seaton Computing Center / SCC** plaque is one merged 8×2 display, not tiny repeated sign text. Existing monitor theme IDs remain unchanged.

Room proportions are block-quantized; roof elevations, finishes, furnishings, landscaping, neighborhood and approach are interpretations, not a surveyed building model. No interior photo was supplied. The sign renderer and client appearance still need an in-game visual review; a dedicated-server audit cannot establish photographic likeness.

## Real satellite crash

The new intro uses `SatelliteCrash` itself, not the old iron/lodestone placeholder:

- Shared display creation and animation: iron body, daylight-detector solar array and lightning-rod antenna, rotation, flame/smoke trail and flyby sound.
- The real 80-tick fall, followed by the intro's delayed investigation cue.
- The existing impact and `SatelliteCrash.build`: excavated crater, scorched material, broken panels, chain, SARC logo, signal campfire and recovery barrel containing the actual recorder/tracker setup.
- Investigation targets the **actual recovery barrel**, not a newly added fake core. Loot collection is not required, and intro interaction protection still prevents inventory changes.

The intro does not run the overworld event's proximity-site selection, automatic timer, player-persisted completion/achievement flags or orbital-network announcement. Its fictional crash is in the player's private scene and does not consume the overworld satellite event. The original overworld event keeps its normal behavior. Pantheris's involvement remains hidden until the existing nighttime sequence.

## Saved-game compatibility

New openings save `SccLayoutV26=true` and use a separate negative-Z scene bank with 320-block slots. Existing active 0.8.9.25 openings lack that flag and retain their 192-block positive-Z coordinates, old buildings, old lecterns, old wreck position, old timing and old dialogue. No persisted intro stage or campus-step IDs change. Existing nighttime, beach and later chapter state is preserved.

Falling display UUIDs belong to the player's saved intro state. They can resume on rejoin; impact, skip and departure remove them, and stale entities are rejected on chunk reload. New scene geometry appears on a new opening/replay, not by rebuilding an occupied old save in place.

## Verification

Run with JDK 21:

```bash
./gradlew spotlessApply build
python3 tools/run_story_audit.py --accept-eula
```

The native audit covers all mapped room routes from Exit 1, supported floors, entrance headroom, roof/glass cues, one continuous plaque, half-block ramp increments, new-slot separation, legacy v25 quest progression, real satellite display ownership/cleanup, real wreck components and walkable crater access, then the existing kidnapping/beach/chapter regressions. Test sources remain outside the production JAR.
