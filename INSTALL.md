# TIMELOCKED 0.8.9.27 — Campus neighborhood

Close Minecraft and back up your world. Build with Java 21:

```sh
./gradlew clean build --console=plain
```

Replace the previous TIMELOCKED JAR with
`build/libs/timelocked-0.8.9.27-campus-neighborhood-dev.jar`.
Requires Minecraft 1.21.1 / NeoForge 21.1.252. Do not keep two TIMELOCKED JARs installed.

For an existing source checkout, apply the reviewed `from-0.8.9.26.patch` with
`git apply --check` before `git apply`. Never extract a full-source snapshot over a working
checkout. Preserve local changes and untracked files; do not force a failed patch.

The new layout is selected on fresh starts or explicit intro replay. A player already in an
older intro keeps the old coordinates and layout; installation does not rebuild occupied scenes.
The later SARC chapters and existing world structures are not rebuilt by this update.
See [Campus neighborhood](docs/CAMPUS-NEIGHBORHOOD.md).

---

## Historical 0.8.9.23 installation notes

# TIMELOCKED 0.8.9.23 — First Contact and siren move

## This release actually includes Chapter 2

A separate SARC Receptionist now appears at the reception counter in Story worlds.
This is a named, persistent scientist-model staff NPC, NOT Dr. Everleigh and NOT
just chat triggered by entering the room. Other scientists remain ordinary staff.

After Chapter 1 and all seven arrival-welcome lines are finished:

1. Right-click the receptionist to start Chapter 2: First Contact.
2. Use one of the four workstations on that reception counter to register arrival.
3. Return to the receptionist to confirm registration and complete the chapter.

`/timelockedstory` shows the active objective and XYZ target. `/sarcreception`
checks placement in your own saved facility. There is a short three-second dialogue
cooldown. No trade screen, bandage, power restoration, quest item or creative mode
is required. This is the playable visitor check-in chapter; a later staff mission
or Chapter 3 is not included. Pantheris's identity/motive stays unrevealed.

The NPC is added to EXISTING eligible Story saves when you enter the lobby. You do
not need a new world, intro replay or terrain rebuild. The role/site and shared NPC
UUID survive saves; each player has independent Chapter 2 progress, also preserved
on intro replay. Placement refuses blocked/occupied/unloaded space and never
replaces player blocks. A saved but unloaded NPC is not respawned as a duplicate.
If missing, load the reception area and use `/sarcreception` for status. Deliberate
operator deletion of a saved NPC is not automatically undone.

## Install

Close Minecraft and BACK UP YOUR WORLD. Replace the previous TIMELOCKED JAR with
`timelocked-0.8.9.23-first-contact-dev.jar`. Do not install two TIMELOCKED versions
together. Requires Minecraft 1.21.1, NeoForge 21.1.252 and Java 21.

The receptionist uses the authored reception counter, local feet `(64,2,329)`.
The story objective points to a real registration workstation near `(67,3,327)`.
It uses normal survival interaction reach; it is not stranded behind the counter.
The PA, anthem and welcome pacing are not restarted or replaced.

## Move the siren in an existing facility

The front doorway siren now joins the beacon on the photographed south wall beside
the lift: beacon `(83,5,359)`, siren `(85,5,359)`, both facing north. New facilities
include both mounts automatically. For an existing facility, move players/mobs/items
out of the stairs, keep entrance chunks loaded and stand back in the lobby. As OP:

```text
/sarcstairs
/sarcstairs repair confirm
```

An unchanged .22 facility changes only TWO blocks: the old/new siren endpoints.
An unchanged .21 stairwell changes ten blocks (six lights plus both alarm moves).
Repeating changes zero blocks. Both alarms retain active/manual/lit state and
complete rotor payloads. Duplicate source/destination fixtures, inventories,
unexpected edits, occupied space and unloaded chunks cause refusal before changes.
Do not force a refused repair. Authored-looking replacement blocks cannot be
identified as player edits, so the backup remains mandatory.

The stair trigger exclusion and six independent hallway lights are retained. The
visible stair protrusion is not redesigned, and arrival progress is not reset.
Receptionist placement does NOT depend on running the stair command.

## Source upgrade: choose ONE matching patch

Preserve local work first. Do not reset, overwrite your repository with a nested
source folder, or force a failed patch. Check SOURCE version, not only installed JAR:

```bash
cd ~/TIMELOCKED-Zero-Point
git status --short
grep '^mod_version=' gradle.properties
```

Choose `from-0.8.9.22.patch` for `0.8.9.22-stair-lighting-dev`,
`from-0.8.9.21.patch` for `0.8.9.21-recess-exit-dev`, `from-0.8.9.20.patch` for
`0.8.9.20-glass-stairs-dev`, or `from-0.8.9.19.patch` for the original coast-chapter
baseline 4f9eaee. Stop on a different source version. Apply only one patch.

Example from .22, with this extracted folder in Downloads:

```bash
git apply --check "$HOME/Downloads/TIMELOCKED-0.8.9.23-first-contact-update/from-0.8.9.22.patch"
git apply "$HOME/Downloads/TIMELOCKED-0.8.9.23-first-contact-update/from-0.8.9.22.patch"
./gradlew build
```

For .19/.20/.21 substitute the matching filename in BOTH commands. Apply only after
the check succeeds. No asset-generator helpers or force/reject flags are needed.
Review, commit and push your own branch manually; no remote push was performed.

## Verification and limits

- Full build, Spotless/google-java-format, Checkstyle and named-number gate passed.
  The new chapter is included in the named-number gate.
- Prettier documentation and project structured-text/Python checks passed.
- 51990 native dedicated-server checks passed with zero failures,
  including real receptionist creation and right-clicks, terminal registration,
  return/completion, multiplayer separation, entity/save reload, replay preservation,
  no duplicate NPC placement, siren/beacon state and payload preservation, old-world
  repair and idempotence.
- All four source patches reproduce the complete final Git source tree, including
  binary structures. All 18 affected fixture payloads are preserved. Production JAR
  metadata/resources match source; native test entry points are excluded.
- These are server checks, NOT a graphical Minecraft or IntelliJ playthrough.
  Existing compiler deprecation/unchecked warnings and broader legacy numeric debt
  remain. The reviewer's whole-codebase concerns are not claimed resolved.

See `docs/STORY-CHAPTER-2.md` after applying and `verification/` in this package.
No new repository Python helper or ignore rule was added; club teaching Java is
untouched.

JAR SHA256: `e9bafd9c60e2e2bd5922fb85c5ed8e74c69ddf0deba2c254777dd61cb20b08da`
