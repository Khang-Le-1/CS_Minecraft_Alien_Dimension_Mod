# Chapter 3: On the Record

A short playable continuation of **First Contact**. A separate SARC Briefing Officer
in the existing operations/briefing area takes your satellite-incident account,
assigns a read-only research orientation, and confirms your return. The officer is
not Dr. Everleigh. Pantheris's identity and motive remain unrevealed.

## Playing

Finish Chapter 1, the arrival welcome and Chapter 2 visitor registration. Then:

1. Follow `/timelockedstory` to the **SARC Briefing Officer** in the operations/briefing
   area (drawing-local feet **105, 2, 240**). Right-click to begin the interview.
2. After the three-second dialogue cooldown, speak again to give the satellite
   statement. The officer explicitly treats it as an unverified witness account,
   not proof of the cause of your displacement.
3. Receive limited research-orientation clearance. Follow the central spine north,
   manually open the north lobby doors and the research-entrance doors, then use
   the existing workstation just left of the research entrance (local **74, 2, 160**).
4. Read the safety protocol, then return to the officer and speak again to complete
   the chapter. Limited orientation clearance remains valid.

The terminal is read-only quest interaction: no trade UI, required quest item,
inventory cost, automatic repair, reactor activation, camera lock, teleport or
forced game-mode change. Sneak-use keeps normal block interaction. Repeated clicks
do not skip the cooldown, duplicate the report or replay the completed chapter.

`/timelockedstory` now hands off from completed reception to Chapter 3 objectives.
`/sarcbriefing` checks staff availability and reports the current objective within
your own registered drawing-layout facility. It does not skip chapters or alter blocks.
The existing `/timelockedstory skip` still skips **only Chapter 1**.

## Limited research access — not a global lock overhaul

Authorization affects manual **opening from the hub/connector side** of the one
existing paired research entrance at local **X 79–80, Y 2–3, Z 170** for eligible
Story players assigned to that facility. Giving the witness report grants the
permission. North-side egress and closing an open door remain available.

This is progression-aware interaction, not a secure multiplayer region: doors are
shared physical blocks. Another player's open door can be walked through. This
release does not add an invisible barrier, force doors closed, stop block breaking
or teleport players back. Independent quests still require each player's own
interview, assignment, exact terminal interaction and return conversation.

Other lobby, stair and tram doors remain manual. Security-card checkpoints,
reactor-power airlocks, containment, portal controls and encounter progression
keep their existing rules. Non-Story worlds and unrelated facilities are unchanged.

## Existing saves and multiplayer

No new world, intro replay, terrain regeneration or facility rebuild is needed.
After First Contact, enter the operations/briefing area to load its chunk; the
briefing officer is added only to a clear, supported, unoccupied authored position.
If player blocks or a container occupy it, no blocks are removed. The status
command explains that placement is blocked or the saved NPC is unavailable.

One staff UUID is saved per facility as `BriefingOfficer`; the receptionist UUID
is independent. A missing/unloaded previously saved officer does not authorize a
replacement spawn. Deliberate operator removal is not automatically undone.
Existing entities keep loading normally. The new officer uses the existing
scientist model with a dedicated visible name and role, no wandering or pushing,
and protection against ordinary damage.

Per-player `StaffChapter` stores `Step`, a bounded dialogue cooldown, and explicit
`ResearchOrientationClearance`. Saved steps are 0=not started, 1=report requested,
2=orientation assigned, 3=return, 4=complete. Save/reload and intro replay preserve
this data. A corrupt stage does not invent a missing clearance flag. Earlier
chapter progress, inventory, personal respawn checkpoint and site power remain
independent. Two players share one officer but have separate statements and quests.

## Contributor map

- `StaffChapter`: NPC placement/identity, interview, assignment, terminal hook,
  bounded cooldowns, objectives, status command and the scoped entry policy.
- `SarcScientist`: dedicated staff interaction instead of normal ambient trades.
- `SiteProgress`: separately saved staff UUID; older saves load without migration.
- `ReceptionChapter`: completed check-in hands the objective to the staff interview.
- `PushBarDoorBlock`: consults staff authorization only when opening the authored
  research entrance from the entry side; egress and other doors are untouched.
- `IntroSequence`: preserves Chapter 3 state during an intentional intro replay.
- `StaffChapterAudit`: actual NPC/block interactions, authored-template routes,
  save/reload, repeated-click protection and independent multiplayer state.

Build with Java 21:

```sh
./gradlew clean build --no-daemon --console=plain
python3 tools/run_story_audit.py --accept-eula
```

Native dedicated-server tests are not a graphical-client playthrough or shader
visual sign-off. No new NPC artwork, voice acting or cinematic cutscene is claimed.
The separate reported SCC ceiling hole remains unresolved from v32.
