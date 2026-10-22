# Beach wake-up presentation — 0.8.9.24

The existing beach wake-up now lasts nine seconds after the server has accepted
the client's stable, loaded, dry landing. It does not extend the loading barrier
or begin the timer before the client is ready.

- Brief initial darkness, then an opening and two separated, gentler blinks.
- A low head lift, pause, then a slower rise to the actual player eye height.
- A small four-degree yaw settle; no roll, head shake, FOV pulse or nausea effect.
- The existing offshore Pantheris/boat cameo and surf remain. No motive is revealed.
- "Easy... just breathe." at 2.4 seconds; the existing "Where... am I?" at 5.6 seconds.
- Overlays clear before release. Server look direction is aligned to the final
  cinematic view before normal control returns, avoiding the old yaw/pitch snap.
- Client camera positions interpolate between ticks. Repeated server cues within
  the same stage/nonce cannot rewind the presentation age.

This is a presentation update, not a new story chapter. The saved stage IDs,
landing nonce, supported-ground checks, client readiness barrier, held position,
skip/rescue paths, restoration of gravity/game mode/inventory, Chapter 1 and
Chapter 2 persistence all remain. A normal completion aligns look direction;
a skip or rescue does not force the cinematic's final look direction.

Nothing is replayed automatically in a completed save. To review it, make a backup
and use the existing operator command `/timelockedintro start`. The existing
`/timelockedintro skip` remains available; no world regeneration is required.
Replay preserves the previously implemented chapter progress rules.

`WakePresentation` contains common, renderer-independent named timing/pose curves.
`client/IntroClient` applies the camera and overlay locally; `IntroSequence` owns
server timing, dialogue and handoff. No audio files, shader settings, global time,
weather or persistent player effects are changed.

Native tests cover curve bounds/continuity, blink separation, exact end pose,
shorter eye heights, readiness/skip/rescue regressions, one-shot dialogue and the
normal release look direction. They do not replace a graphical client playtest.
All changed client/common presentation code compiles in the normal source build.
