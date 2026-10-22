# SCC circulation corrections — v34

- Widened the west resource/network/data-room circulation strip to four clear blocks, x77–80, by moving the east room boundaries to x81. The east-facing SCC101 front stays at x76.
- Rejoined resource-room wall/glass corners and enclosed the network/data-room ends instead of leaving unframed gaps. The widened corridor has a continuous acoustic ceiling at y5.
- Moved the near-side server racks inward with the room boundary. Glass room doors and signs remain functional; SCC124 still faces the south hallway.
- Replaced the two adjacent entrance pairs with one centered double door per bank (two leaves, fixed glass sidelights). The open leaves rest at the outside jambs, not between adjacent pairs.
- Corrected a retained neighborhood driveway overlay that crossed College Road; north-side driveways now end at the road edge.
- Retained v33 Chapter 3, First Contact, the road approach, story interaction anchors and scene-bank selection.

## Updating an existing save

Back up the save and replace the TIMELOCKED JAR on both client and server while the game is closed. Restart fully, with only one TIMELOCKED JAR installed.

Campus geometry is authored when the intro scene is built. This update does not automatically overwrite an already-built campus. To see the corrected campus, an operator can use `/timelockedintro start` to replay the intro, or test with a new player/world. No chunk deletion is necessary. Do not replay solely to install the mod if continuing Chapter 3 is your priority.

## Verification

Native dedicated-server tests cover door pair open/close behavior, hinge collision shapes, four-wide swept clearance and connectivity, continuous corridor ceilings, enclosed room-end seams, usable room doors, retained story anchors and existing chapter regressions. A graphical-client/shader playthrough is not claimed; verify the visual appearance in game.
