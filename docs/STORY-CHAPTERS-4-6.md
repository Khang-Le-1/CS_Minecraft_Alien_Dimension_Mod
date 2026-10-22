# Chapters 4–6: Evidence, the founder, and a controlled trial

A playable continuation after Chapter 3, **On the Record**. The three new chapters
contain **38 sequential beats**: 12 in Chapter 4, 12 in Chapter 5, and 14 in Chapter 6.
Each previous chapter had fewer milestones. Length here comes from multiple visits,
record comparisons, conversations, and a walking exercise—not extended countdowns.
No completion-time estimate has been validated in a graphical playthrough.

## Start or resume

Finish the coastal arrival, reception registration, and the briefing officer's
research orientation. Then speak to the **SARC Briefing Officer** again in operations.
The existing objective HUD and `/timelockedstory` follow the continuation.
`/sarcresearch` reports the current assignment. There is no new skip command;
`/timelockedstory skip` still skips only the coastal Chapter 1 sequence.

Right-click the indicated NPC or workstation with the main hand. Consecutive
interactions have a three-second anti-repeat cooldown; the same click cannot
complete several panels. Sneak-use of workstations keeps the ordinary block action.
You can walk away at any point and return later. No dialogue locks movement.

Targets are drawing-local coordinates, added to your own registered facility origin:

| Target | Local coordinates | Use |
| --- | --- | --- |
| Briefing Officer | 105, 2, 240 | Assignments, findings, reports |
| Dr. Everleigh | 109, 2, 240 | Founder meeting and trial escalation |
| Intake workstation | 74, 2, 160 | Chronology and training preflight |
| Signal workstation | 74, 2, 148 | Clock quality, comparisons, training copy |
| Archive workstation | 74, 2, 136 | Provenance, charter, sample observation |
| Reception workstation | 67, 3, 327 | Arrival entry and report handoff |
| Atrium checkpoint | 80, 2, 155 | Chapter 6 walking milestone |
| Connector checkpoint | 80, 2, 180 | Chapter 6 exit-route milestone |

The workstation headings describe quest panels, not replacements for their physical
models or animated screens. Use the central research-atrium lane. North lobby and
research doors remain manually operated. The compass arrow is a bearing, not a
wall-avoiding route planner. Checkpoints require being on the ground within two
blocks of the marked position.

## Chapter 4 — The Missing Interval

1. Accept the evidence review from the officer.
2. Read intake chronology, then distinguish recorded arrival from witness memory.
3. At the signal desk, examine the copied telemetry and its clock-quality warning.
4. At the archive, trace provenance and read the evidence-handling note.
5. Return to the officer with the limits of all three records.
6. Compare the two kinds of missing time at the signal desk.
7. Verify the unedited arrival entry at reception.
8. Discuss competing explanations, then submit the review to the officer.

The chapter does not treat a telemetry gap as proof of displacement or identify the
intruder. The source recorder has not been recovered. Its records are fictional
quest text, not a new generated satellite-loot or telemetry subsystem.

## Chapter 5 — Meet the Founder

**Dr. Everleigh Alexi Darkfang** appears in operations after the review. This is a
separate persistent NPC with the existing scientist model as a stand-in, not the
receptionist or briefing officer renamed in place. No bespoke founder artwork,
voice acting, facial animation, or cinematic is included.

The meeting covers your account, SARC's purpose, and the limits of a Zero Point
explanation. Everleigh sends you to read the charter, visitor safeguards and
case-access history, then takes questions about accountability, missing source
records, the intruder, and the limits of further participation. Closing the meeting
unlocks the officer's next assignment. Pantheris's identity and motive remain
unrevealed; the welcome recording and existing audio are unchanged.

## Chapter 6 — A Controlled Trial

A **read-only ATLAS HES familiarization dry run**, not a live deployment or suit award.
Everleigh's personalized ATLAS remains Creative/operator-only; Survival players
need no suit, quest item, currency, ammunition or consumables to finish.

Review stop conditions and the HES preflight checklist. Inspect a simulated report
with an unverified seal and record HOLD. Walk the atrium checkpoint, collect a sample
at the archive, then follow the connector exit route to reception. Return to operations,
compare an unsupported correction in a later sample copy, escalate it to Everleigh,
and submit the trial report to the officer.

The data discrepancy is expressly a training example, not a new claim about the
player's real case. All readings and responses are authored quest text. No live
sensor, armor boot, seal validation, power, portal, combat or exposure is simulated
by changing game systems. Checkpoint visits are the two physical route checks.
Finishing records training completion only; existing access remains unchanged.
The missing-interval case is left open at the end of the available story.

## Existing worlds, multiplayer, and saves

No new world, facility regeneration, opening replay, or quest reset is needed.
Existing Chapter 3 completion and its explicit orientation-clearance flag are required.
New-player progress is unchanged until that point. Existing Chapter 3-complete players
start at the Chapter 4 assignment; merely loading a save does not start dialogue.

`ResearchStory` saves `Stage` (0–38) and a bounded `Cooldown` in each player's intro
record. Stages 0–11 are Chapter 4, 12–23 Chapter 5, 24–37 Chapter 6, and 38 means
complete. Treat these indices as stable saved IDs; do not reorder the beat list.
Save/reload and intentional intro replay preserve this progress. No stage is inferred
from shared NPCs, another player's progress, power state, or inventory. Invalid
out-of-range progression halts rather than wrapping or replaying completed work.

`SiteProgress.Founder` stores one founder UUID per facility, independent of reception
and the briefing officer. Placement requires an already loaded chunk, two clear air
blocks, supported floor and no occupying entity. Player blocks and containers are
never cleared. A saved-but-missing or unloaded NPC does not trigger a replacement.
If the founder is unavailable, visit operations and use `/sarcresearch`; check that
the position is clear and that the saved entity has not been deliberately removed.
The command does not delete or regenerate staff.

Shared physical doors still permit another player to walk through an open door.
These chapters do not add an invisible access barrier or secure multiplayer region.
NPC and workstation quest interactions still require the individual player's own
prerequisites, current stage, facility, proximity and correct target. Offhand,
spectator, dead, wrong-site and non-Story interactions cannot advance the chapters.

## Implementation and verification

- `ResearchStory`: authored beats, actor identity, terminal/checkpoint hooks, bounded
  cooldown, founder placement and status command.
- `StaffChapter`: completed interview hands NPC interaction/objectives to Chapter 4.
- `StoryObjective`: per-stage HUD labels and targets, with no quest mutation.
- `SarcScientist`: dedicated founder interaction and suppression of ambient chatter,
  trading and pushing for this NPC.
- `SiteProgress`: backward-compatible optional founder UUID.
- `IntroSequence`: preserves new progress during intentional opening replay.
- `ResearchStoryAudit`: every new beat through actual server interaction hooks;
  authored workstation/route checks; serialization, multiplayer and rejection cases.

Build and native tests require Java 21:

```sh
./gradlew clean build --no-daemon --console=plain
python3 tools/run_story_audit.py --accept-eula
```

A native dedicated-server audit is not a graphical-client playthrough. It cannot
sign off pacing, client interaction feel, textures, shaders, or first-person framing.
No existing campus/facility geometry or audio assets are rewritten. The previously
reported SCC ceiling/circulation and bypassable door-partition issues remain open.
