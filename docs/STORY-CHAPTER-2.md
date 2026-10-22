# Chapter 2: First Contact

A separate **SARC Receptionist** now stands at the reception counter in Story
worlds. This uses the existing scientist model with a dedicated saved receptionist
role and visible name; it is not Dr. Everleigh or another ambient trading NPC.
The recorded welcome, anthem and arrival lighting remain owned by `HubArrival`.

## Playing

Finish Chapter 1 and the seven-line arrival welcome, then:

1. Right-click the SARC Receptionist with your main hand to begin **First Contact**.
2. Right-click one of the four workstations on that reception counter to register
   your arrival. The terminal records the check-in and tells you to return.
3. Speak to the receptionist again to confirm registration and finish the chapter.

`/timelockedstory` shows the active objective and target coordinates. There is a
short three-second dialogue cooldown after each completed interaction; repeated
clicks do not replay lines or advance twice. Sneaking at a workstation preserves
normal building interaction. No trading UI, bandage, power-restoration task,
mandatory quest item or creative access is required. This first-contact chapter
ends after visitor check-in; [Chapter 3: On the Record](STORY-CHAPTER-3.md) continues with
a separate briefing officer, witness statement and read-only research orientation.
Pantheris's identity and motive are not explained.

## Existing saves and placement

The receptionist is added automatically when an eligible Story player enters their
own registered drawing-layout facility's lobby. No new world, intro replay, stair
repair or facility rebuild is needed. In that facility, `/sarcreception` checks
placement and repeats the receptionist's location and current quest objective.
It cannot skip the welcome, erase quest progress or overwrite a player build.

Drawing-local positions are receptionist feet `(64, 2, 329)` and the main quest
terminal `(67, 3, 327)`. Terminals at X=61, 64, 67 or 70 on that same counter all
work. The receptionist stands on the approach side of the three-deep counter,
where survival interaction reach can reach them. Placement verifies loaded chunks,
floor support, headroom, collision and entity occupancy. It never generates a
chunk or removes a player's blocks to make room. If blocked, clear the placement
area yourself only after checking your build; the command does not force repairs.

One receptionist UUID is saved in the facility's `SiteProgress`. The entity keeps
its role and site in its persistent entity data, does not wander or get pushed,
and is invulnerable to normal damage. Its saved UUID prevents a missing/unloaded
entity from being mistaken for permission to spawn another. Wait for the reception
chunk/entity to load if the command reports a saved NPC is unavailable. Deliberate
operator removal of that entity is not automatically undone.

## Progress and multiplayer

Player progress lives in the existing intro save under `ReceptionChapter`: saved
stages are not-started (0), register (1), return (2), complete (3), plus a bounded
chat cooldown. Saving, relogging and intro replay retain it. Players share one
receptionist but have independent assignments and completion. Clicking another
facility's receptionist or a moved/unrelated workstation cannot advance it.
Non-Story worlds, unfinished intros, spectators and other dimensions cannot
advance the chapter. The existing `/timelockedstory skip` still skips **only
Chapter 1**; it does not complete Chapter 2 or power/restart the PA.

## Contributor map and verification

- `ReceptionChapter`: placement, interaction, counter validation, objectives and
  saved per-player assignment progression. Named timings, reach limits and authored
  coordinates are covered by the existing named-number gate.
- `SarcScientist`: routes the dedicated receptionist role to the chapter instead
  of ordinary trades/ambient lore. Other staff behavior is unchanged.
- `SiteProgress`: saves the shared receptionist UUID without changing save IDs.
- `IntroSequence`: retains Chapter 2 data on intro replay.
- `CoastChapter`: hands the existing objective command to reception after Chapter 1.
- `ReceptionChapterAudit`: native entity creation, real NPC right-clicks, terminal
  interactions, independent multiplayer progress, entity/save reload and duplicate
  prevention. The existing audit runner includes it; no new Python helper is added.

Run `./gradlew build` and `python tools/run_story_audit.py --accept-eula` with JDK 21.
These are dedicated-server tests, not a graphical client playthrough.
