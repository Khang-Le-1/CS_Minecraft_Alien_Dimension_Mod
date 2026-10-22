> v32 route correction: the current playable route is College Road east → cross Lambert Road → north on Lambert’s east sidewalk to SCC. The old Park Street leg described below is superseded; the rest of the campus network is not being reconstructed.

# Campus neighborhood — 0.8.9.27

## What changed

- Paired silver-frame glass entrance doors and doors in the SCC 123 / SCC 121 floor-plan openings.
  Right-click either leaf; the intro consumes the event and toggles that pair server-side.
  Off-hand duplicate events, remote clicks and other dimensions cannot toggle them.
- The west-side route rises four full blocks through eight half-block changes, then joins the
  south entrance landing. It is a physical grade with handrails, not a flat painted path.
- A morning walk crosses Park St before following College Rd north to SCC. Saved route checkpoints
  enforce that order before classroom arrival. Street lengths and building placement are fictional.
- Lobby ceiling baffles, slim pendant rods, red seating and orange/red wall treatment draw on the
  supplied lobby image. Classrooms use warm recessed strip fittings with metal diffusers.
  New SCC, home and street scenes contain no sea lanterns. No shaders are required.
- Home includes a kitchen, dining area, bookcases, living seating, entry storage, bedroom desk,
  wardrobe, bathroom and bedside lighting. The same builder supplies the evening home.
  Original bed, television and break-in anchors remain unchanged.
- The real satellite fall/crater implementation is retained and translated to the raised scene.
  It still leads to investigation, returning home, and the existing night-time kidnapping.

## Scope and reference fidelity

The supplied SCC Level 1 PDF still determines room adjacency and the confirmed club room 121.
The existing large Seaton Computing Center / SCC plaque is preserved. Furnishing, compressed
street distances, class schedule, crash site and home are fictional game set dressing. This is
not an official College of DuPage model, surveyed walking guide or claimed exact interior replica.
The supplied exterior photo, lobby photo and floor plan informed this scene; there is no claim
that another real college building has been modelled.

## Active-save compatibility

Fresh starts/replays save `CampusSceneV27` and use 512-block private slots in a separate negative-Z
bank. Existing v26 `SccLayoutV26` saves retain their 320-block bank and geometry. Existing v25
campus saves retain their 192-block positive-Z bank. Older non-campus saves keep the TV opening.
No persisted stage IDs change; no occupied scene is silently moved. Skip, recovery, inventory,
return location, beach transition and later chapter state retain the existing protocol.

## Checks

Run production formatting/build and the native gameplay suite with Java 21:

```sh
./gradlew spotlessApply build
python tools/run_story_audit.py --accept-eula
```

The temporary native server is allocated 2 GB of heap; the Gradle test launcher uses 768 MB.
The native suite checks v27 route order, raised anchors, half-block grade/headroom, paired doors,
off-hand/distance protection, furnishings, day-to-night continuity and the real satellite beat,
then runs the existing v26 and v25/legacy coverage. Test entry points are temporary and must not
be shipped in the production JAR. Generated reports are under `build/reports/story`.

Native server tests do not establish final in-game appearance. Check the lobby, glass doors,
ramp and furnished home in a graphical client with the intended resource pack/shaders before
calling the visuals final.
