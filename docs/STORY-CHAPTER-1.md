# Chapter 1: A Road Inland

The Earth opening continues after the supported beach landing. The player remembers the break-in, catches the existing glimpse of the departing boat, and follows the coast's only lead inland. Pantheris's identity and motive are not explained by this chapter.

## Playing

Use a **Story** world. After the opening, walk freely from the shore past the parking lot, along the boardwalk and access road, through the facility entrance and into the arrival hub. `/timelockedstory` repeats your current objective with X/Z coordinates. `/timelockedstory skip` finishes only your coastal chapter; it does not skip, power or restart the facility welcome.

Right-click either original modeled beach sign with your main hand to read its text in chat. Both panel halves work. Sneaking keeps normal building behavior. These readings are optional: missing signs or shortcuts cannot block progression.

## Dialogue and pacing

Each landmark sends at most one chat line, with at least seven seconds of active route time between chapter messages. Walking directly to a later landmark skips earlier lines instead of dumping a backlog.

| Landmark          | Player thought                                                                                               |
| ----------------- | ------------------------------------------------------------------------------------------------------------ |
| Shore             | The broken window. Footsteps behind me. Then nothing. I need to get off this beach.                          |
| Parking           | Marked parking bays and a staff walkway. People are supposed to arrive here by road—not wake up on the sand. |
| Boardwalk         | I keep looking back at the water. The boat is gone. The boardwalk is the only lead I have.                   |
| Access road       | A proper access road. It leads inland. If there is anyone here, they will be at the other end.               |
| Wayfinding        | SARC again. The same name as the coast signs. This road is not leading to a public beach.                    |
| Entrance          | A research facility. I do not have identification, or an explanation. But I cannot stay out here.            |
| After the welcome | A welcome recording. Not an answer. I need to find someone who can tell me what happened.                    |

The arrival hub is silent until the existing logo/welcome sequence has delivered all seven lines. A further quiet interval precedes chapter completion. This chapter only observes the saved arrival state: Dr. Everleigh's script, anthem, lighting and timing stay owned by `HubArrival`.

## Code map for contributors

- `CoastChapter.java`: eligibility, named route bounds, saved milestone progression, panel interaction, objectives and commands. Offsets are blocks in the coordinate frames documented by `IntroCoast`; milestones are persisted IDs and must not be renumbered.
- `IntroCoast.surfaceOrigin` / `roadJunction`: coordinate helpers; generated geometry and the existing wake position remain unchanged.
- `IntroSequence`: begins progress after the landing and copies the chapter compound during intro replay.
- `HubArrival.inside`: shared room boundary so chapter completion uses the same hub as the PA.
- `assets/timelocked/lang/en_us.json`: all new player-facing text under `message.timelocked.coast.*`.
- `validation/CoastChapterAudit.java`: test-only native server checks; the existing `tools/run_story_audit.py` copies the chapter, intro and facility-spawn Java audit files into its temporary test build. No new Python helper is needed.

## Save and multiplayer rules

Progress lives in each player's existing intro save compound as `CoastChapter`: `Step`, `Cooldown`, optional sign flags and `HubQuietTicks`. Steps are shore (1), parking (2), boardwalk (3), road (4), wayfinding (5), entrance (6), hub (7), complete (8); zero means not yet narrated. Invalid stages are treated as complete to prevent replay spam. Cooldowns are clamped on load/use.

An existing Story save gains progress only while physically on its own saved route. Rejoining does not repeat finished beats; intro replay preserves progress. Other players have separate progress. Non-Story worlds, other dimensions, dead/spectator players, airborne movement and unloaded ground do not advance it. Leaving the route pauses active progression; there is no offline catch-up.

This chapter does not change terrain, inventory, clearance, camera or game mode. It adds no NPCs, rescue explanation or new mandatory quest item. After completion, the story objective now hands off to the separate receptionist in [Chapter 2: First Contact](STORY-CHAPTER-2.md).

## Checks

Java 21 is required. Run the normal build and then the existing optional native audit:

```sh
./gradlew build
python tools/run_story_audit.py --accept-eula
```

On Windows use `gradlew.bat build` and `py tools/run_story_audit.py --accept-eula`. The flag is explicit acceptance of Minecraft's EULA for the temporary local test server. See `docs/CONTRIBUTING.md` for lint/tool requirements. Native checks are not a graphical in-game playtest; camera, presentation and walk-through QA still require a client.
