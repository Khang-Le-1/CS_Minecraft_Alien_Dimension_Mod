# Contributing without getting lost

## Make one small change

1. Start in the original `com.cs_club.minecraft_alien_mod` examples if you are new.
2. Create a branch. Keep gameplay/story additions separate from cleanup.
3. Change one feature, inspect `git diff`, then run the checks below.
4. Commit only source, intentional game assets and documentation. Do not commit
   `.gradle/`, `build/`, `.audit/`, worlds, logs, ZIPs or Python caches.
5. Push your branch to your fork and update/open a PR. Do not replace the whole
   repository from a downloaded source archive. Never force-push shared branches.

Existing teaching Java stays unchanged. The cleanup restores it from upstream and
adds a second metadata entry so its items remain available. A single trailing comma
was removed from its translation JSON because Minecraft cannot parse that file.
Changes to shared build files must keep both namespaces working. The preserved
`src/main/templates` file explains the original metadata setup but is not expanded
or copied into the combined JAR; do not enable a second writer for the same file.

## Prerequisites and commands

Normal development needs Git and **JDK 21**. Use the checked-in Gradle wrapper,
not a system Gradle installation. First builds download dependencies.

| Task                                  | Linux/macOS                                            | Windows PowerShell                                   |
| ------------------------------------- | ------------------------------------------------------ | ---------------------------------------------------- |
| Build, Java format and static lint    | `./gradlew build`                                      | `.\gradlew.bat build`                                |
| Fix TIMELOCKED Java formatting        | `./gradlew spotlessApply`                              | `.\gradlew.bat spotlessApply`                        |
| Launch development client             | `./gradlew runClient`                                  | `.\gradlew.bat runClient`                            |
| Install optional checks               | `python3 -m pip install -r tools/requirements-dev.txt` | `py -3 -m pip install -r tools/requirements-dev.txt` |
| Check helper code and structured data | `python3 tools/check_project.py`                       | `py -3 tools/check_project.py`                       |
| Fix Python formatting                 | `python3 -m ruff format tools`                         | `py -3 -m ruff format tools`                         |
| Native Story regression server        | `python3 tools/run_story_audit.py --accept-eula`       | `py -3 tools/run_story_audit.py --accept-eula`       |

For documentation formatting checks, install Node.js 22+ and run `npm ci`, then
`npm run lint:docs` (or `npm run format:docs` to fix). Node is optional for building
the mod; it is only used for Markdown/YAML formatting checks in CI.

The optional Python checks require **Python 3.11+** (`tomllib` is built in).
Use a virtual environment if your operating system manages Python packages.
If Unix reports permission denied, `chmod +x gradlew` once. Windows does not need
Bash for any required command. Review the Minecraft EULA before opting into the
server audit. That test uses a disposable local-only server, not your real saves.

`build` checks TIMELOCKED Java formatting with Spotless/google-java-format,
runs Checkstyle static rules on both mod namespaces, and compiles both mods.
Run `./gradlew checkstyleMain` (Windows: `.\gradlew.bat checkstyleMain`) for lint alone.
The enforced rules currently cover empty statements, equals/hashCode pairing,
switch fall-through, array declaration style and public type/file naming. This is
an intentionally limited starting ruleset, not comprehensive bug detection.

Run `./gradlew auditMagicNumbers` (Windows: `.\gradlew.bat auditMagicNumbers`)
for the separate, **non-gating** numeric-literal inventory. Read
`build/reports/checkstyle/magic-numbers.html` or its XML counterpart. A successful
task here means the report was produced, NOT that numeric debt is fixed. It covers
all main Java, including drawing data; it ignores only -1, 0, 1, 2, constant
initializers and hashCode methods according to Checkstyle's configured semantics.
No file-wide magic-number suppressions or hidden baseline are used.
`build` also enforces numeric checks in the 18 selected gameplay/geometry classes
via `checkArrivalNumbers`; the full-codebase inventory remains non-gating. See
[geometry and numeric rules](GEOMETRY-AND-NUMERIC-RULES.md) for the exact scope.

`check_project.py` checks Python syntax, Ruff lint/format, and
parses all tracked/unignored JSON (including `.mcmeta`), TOML and YAML. TOML
placeholders in the preserved metadata template are substituted for parsing only.
Binary art/audio and Gradle's generated code are not text-linted. Gradle compiles
the Groovy build scripts; CI also checks documentation with Prettier and shell
syntax with Bash. These checks are **not** a full static correctness proof.
Original club Java is intentionally excluded from automatic formatting so its
teaching examples stay byte-for-byte intact; it is still compiled.

## What each helper does

All paths below are relative to the repository. Generators deliberately update
**tracked game assets**: run them only when authoring that asset, review the diff,
and run `spotlessApply` after generating Java. They never run during normal builds.
Install `tools/requirements-assets.txt` only if you need image/audio authoring.

| Helper in `tools/`               | Inputs / purpose                                             | Writes                                                                                   |
| -------------------------------- | ------------------------------------------------------------ | ---------------------------------------------------------------------------------------- |
| `generate_foliage_assets.py`     | Authored palettes and block geometry                         | Foliage models/textures in `src/main/resources/assets/timelocked/`                       |
| `generate_workstation_assets.py` | Authored station geometry and palettes                       | Workstation models/textures in that asset namespace                                      |
| `generate_coast_signs.py`        | Sign text/layout and a TrueType font                         | Sign models, blockstates, textures and loot/item definitions                             |
| `generate_pantheris_model.py`    | `tools/pantheris-model.json`                                 | Native `client/PantherisModel.java` and palette texture                                  |
| `generate_intro_audio.py`        | Ambience by default, or `--anthem INPUT`; FFmpeg on PATH     | Ambience updates intro assets; `--anthem` writes only `build/audio/` (or `--output DIR`) |
| `process_anthem.sh`              | Optional Unix shortcut to the Python command                 | Same as above; no longer the only way to process audio                                   |
| `render_coast_signs.py`          | Shipped sign models/textures                                 | `build/previews/`                                                                        |
| `render_pantheris_preview.py`    | Editable character cuboids                                   | `build/previews/`                                                                        |
| `render_scene_layouts.py`        | Historical `validation/coast-layout.json` plus source layout | `build/previews/`; this is not an in-game capture                                        |
| `project_paths.py`               | Shared font/output-path helpers                              | Creates `build/previews/` only when requested                                            |
| `run_story_audit.py`             | `validation/IntroAudit.java`, JDK and Gradle                 | Temporary `.audit/` server; `build/reports/story/` report/log                            |
| `check_project.py`               | Repository text plus Ruff                                    | Console diagnostics only; no asset generation                                            |

Portable audio examples:

```sh
python3 tools/generate_intro_audio.py --anthem "/path/to/NAU_Anthem.mp3"
```

```powershell
py -3 tools/generate_intro_audio.py --anthem "C:\Music\NAU_Anthem.mp3"
```

For fonts, set `TIMELOCKED_FONT` to a local `.ttf` file. Default font discovery tries
common Windows/macOS/Linux fonts without fontconfig. Different fonts can change
artwork: keep existing shipped textures unless the change is intentional.

`build/`, `.audit/`, `__pycache__/`, `.ruff_cache/`, `.venv/` and data-generator
`.cache/` are ignored. Actual `src/generated/resources` game assets are not hidden
wholesale: generated assets intended to ship still need review. Historical reports
and preview images already in `validation/` remain evidence, not current-run output.
`IntroAudit.java`, `CoastChapterAudit.java`, `FacilitySpawnAudit.java`, `GlassStairwellAudit.java` and `ReceptionChapterAudit.java` are wired into the current native runner;
the other audit Java files document earlier releases, not additional automatically executed tests.

## Finding the Story code

- `StoryMode`: saved game rule and Story-world gating.
- `IntroSequence`: server-owned per-player stages, timing and transitions.
- `CoastChapter`: saved free-roaming beach-to-SARC chapter; see [the script and code map](STORY-CHAPTER-1.md).
- `IntroState`: saved progress; stage numbers are save/network IDs, not tunables.
- `IntroLanding`: server-side chunk/support barrier and position hold.
- `client/IntroClient`: local camera, blackout, input lock and readiness acknowledgment.
- `IntroScenes`: private home set; `IntroCoast`: new-chunk beach and access layout.
- `IntroDeparture`, `IntroBoat`: scene-only offshore actor/boat cleanup.
- `IntroNetworking`: client/server cues; never trust a client to select a safe landing.

## Why Python is here

Python is optional **asset authoring and test tooling**, not a Minecraft runtime
dependency. Existing generators turn editable palettes/cuboids into native game
assets; the audit runner launches a disposable regression server. Do not run the
generators just to build the mod.

The prior cleanup added three Python files. This correction removes the separate
`process_anthem.py` and folds its unchanged FFmpeg graph into the existing audio
generator. Two additions remain deliberately: `project_paths.py` centralizes portable
font/preview paths (rather than repeating OS-specific code in six tools), and
`check_project.py` is a read-only structured-text/Ruff checker. Neither generates
Java, and neither runs during a normal Gradle build. No new Python file is added
by this correction. The Bash audio shim is optional; Windows uses the Python command.
With the shim, choose a custom folder using `--output DIR` after the input path.

## Numbers and comments

Prefer constants with units for rules: `_TICKS`, `_MILLIS`, `_BLOCKS`, `_DEGREES`.
Comments should explain **why**, especially saved IDs, coordinate systems, client
vs server ownership and safety barriers. The earlier cleanup named some landing/network/camera timing values.
This correction refactors `HubArrival`: typed progress/state access, small named
methods, documented hub bounds, seconds-to-ticks timing, legacy logo offsets and
Minecraft's named block-update flag. Existing seven-line welcome delays and five
light stages are unchanged. `DrawingArrivalIndex` documents its five-column schema
and names the stage/kind indices; its coordinate data is unchanged.
`DrawingAlarmIndex` documents its three-column local-coordinate schema. The coast
class now documents its coordinate frame. Cuboid vertices, texture UVs, palettes
and authored map coordinates remain data, not hundreds of one-use constants.

See `docs/REVIEW-CORRECTIONS.md` for the verification scope. There is still legacy debt across the larger codebase: decompiled/reconstructed
classes, short local names, older APIs and additional literal timings/layout bounds.
This pass does **not** claim that every magic number has been eliminated. Refactor
one system at a time with behavior tests; do not bulk-invent names or comments for
values whose meaning has not been checked. Generated Pantheris Java should be
edited through its cuboid data/generator, not the generated mesh.

Native tests cannot validate what the camera actually draws. Keep the README's
manual client checklist, especially with shaders or first-person body mods.

See [geometry and numeric rules](GEOMETRY-AND-NUMERIC-RULES.md) for the next cleanup pass,
editable model schemas, unchanged save IDs and expanded numeric gate.

## IntelliJ review versus automated checks

This project uses Gradle Spotless/google-java-format as its canonical TIMELOCKED
Java formatter. Run `spotlessApply` from IntelliJ's Gradle tool window (or use the
wrapper command above) before `build`; an IDE's default Reformat Code settings
may differ. Do not bulk-reformat the preserved club teaching examples.

A passing build is not a clean IntelliJ inspection report. Checkstyle covers only
the configured static rules, and the named-number gate covers the documented
classes, not the whole mod. Deprecation and unchecked compiler warnings remain.
Reviewers should include the exact class, inspection name and line so fixes can
be verified without suppressing warnings or renaming arbitrary coordinates.

Campus opening source and test map: [CAMPUS-OPENING.md](CAMPUS-OPENING.md).
