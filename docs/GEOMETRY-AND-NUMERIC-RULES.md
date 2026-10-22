# Geometry data and numeric gameplay rules

## Why this changed

The previous numeric audit mixed two different concerns: gameplay rules with no
explanation, and large native mesh drawings embedded in Java methods. Moving a
mesh into a table is not a substitute for documenting gameplay limits. This pass
handles both, without changing the Checkstyle rule or adding suppressions.

The largest recovered drawing, `SarcHangingLogoBlock`, no longer contains 11
opaque `rowN()` methods. `VehicleModels` no longer repeats hundreds of native
cuboid builder calls. They are small adapters over named geometry fields.

## Editable drawings

Shipped classpath files live in `src/main/resources/data/timelocked/geometry/`:

| File             | Meaning                                                 |
| ---------------- | ------------------------------------------------------- |
| `sarc-logo.json` | Logo outline rectangles, indexed by block-state part ID |
| `rover.json`     | Native rover cuboids, UV origins, pivots and wheel IDs  |
| `truck.json`     | Same schema for the truck                               |
| `tram.json`      | Same schema for the tram, including suffixed part names |

These are built-in model definitions, **not reloadable gameplay datapack settings**.
`AuthoredGeometry` reads them from the JAR as UTF-8, validates them and caches
immutable records. Missing or malformed data fails explicitly; it does not silently
produce invisible models. No Python or asset-generation step is required to build.
All drawing values were extracted from the prior Java, not redesigned.

### Logo schema, version 1

- `columns = 26`, `rows = 11`, `pixelsPerBlock = 16` describe the existing mosaic.
- `parts[row][column]` is a list of rectangles. Empty lists are transparent parts.
- Each rectangle has `minX`, `minY`, `maxX`, `maxY` in **local block model pixels**.
  Minima are inclusive bounds; maxima are the opposite face, not pixel counts.
- The collision/outline extrusion spans Z=0 through Z=16 model pixels.
- Saved `part` ID = `row * columns + column`, ranging from 0 through 285.
  **Do not reorder rows/columns or resize the grid**: existing worlds store these IDs.
- Rectangle order is retained. Rectangles must have positive area within the block.

### Vehicle schema, version 1

- `textureWidth`, `textureHeight`, `textureU`, `textureV` use atlas pixels.
- Each named cuboid has `x`, `y`, `z` for its local minimum, then positive `width`,
  `height`, `depth`, all in native Minecraft **model pixels**, not world blocks.
- `pivotX`, `pivotY`, `pivotZ` reproduce the original `PartPose.offset` exactly.
- `wheelParts` holds numeric suffixes of named `part_N` children. It is **not** an
  index into the cuboid array: the tram also has names such as `part_2b`.
- Names, insertion order and wheel order are preserved for animation compatibility.
- Fractional dimensions are intentional. JSON values are parsed as Java floats,
  preserving the bit patterns of the original Java float literals.

Native regression tests fingerprint every rectangle, cuboid, UV, pivot, texture
size and wheel ID against values extracted from the pre-refactor Java. They also
compare all 286 native logo shapes and reject malformed dimensions/wheel IDs.
Do not update those expected fingerprints merely to make a failed refactor pass.
Intentional artwork changes need review and a deliberate test rebaseline.

## Gameplay constants cleaned in this pass

- `FacilityState`: three-digit saved site-ID capacity, wraparound and NBT tag type.
- `SiteProgress` / `HubArrival`: shared saved arrival-stage limits, message counts,
  welcome timer clamp and encounter wave limit; save IDs remain unchanged.
- `AlarmRules`: normal sound cadence, distinct from the longer danger recording.
- `IntroDeparture`: anchor-relative boat offsets, water/headroom clearance,
  cinematic movement speed, yaw, rowing start and look-back duration.
- `CelestialWorkstationBlock`: batch sizes, attunement item cost/effect durations,
  treatment HP, cooldowns and absent-worker claim grace. Collision geometry is a
  named immutable shape rather than an unexplained inline return expression.
- `GunItem` / `BatonItem`: durability, trigger timing, hit padding, tracer spacing,
  sound/color settings and attack-speed modifier, with units/context.
- `DockingRules`: names for the existing aligned, sealed and walk-through stage IDs.
- `VehicleDeploymentItem`: block-center offset, rail feet height and rail yaw.
- `StartupOverlayMixin`: the existing real-time hold/fade durations, in milliseconds.

Values and behavior were retained. Public/save/network identifiers were not renumbered.
The five original club Java files and existing `.gitignore` are not modified.

## Enforcement and remaining work

`./gradlew build` (Windows: `.\gradlew.bat build`) now applies the existing
`checkArrivalNumbers` task to **14 cleaned classes**, not just the arrival controller.
The task name is retained for compatibility. A literal reintroduced in these classes
must pass the same numeric rule; this patch adds no ignored numeric values or file
suppressions. Static Java rules and Spotless remain separate checks.

`./gradlew auditMagicNumbers` still scans **all main Java** and publishes the
remaining debt. It is non-gating and a successful task does not mean a clean report.
This is a substantial targeted pass, **not a claim that every legacy class is fixed**.
The handoff's `verification.json` records before/after counts and test results.
Continue reviewing the remaining world-generation and renderer code by subsystem;
do not bulk-rename coordinates to `NUMBER_123` or hide entire files from the audit.

The native server audit verifies data and behavior, not graphical client output.
Vehicle rendering still needs a visual check in a real client (especially with shaders).
