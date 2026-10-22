# Existing coastal truck — 0.8.9.29

## What is parked

The coastal parking lot receives the project's existing `timelocked:sarc_truck` entity. It uses the unchanged `data/timelocked/geometry/truck.json`, original `textures/entity/vehicles/truck.png`, `SarcVehicleRenderer` and `SarcGroundVehicle` implementation. There is no new block-built model, shuttle prop, teleport or replacement driving system.

## Using it

After the beach arrival finishes in Story mode, approach the coastal parking lot. Right-click the truck to board. Use your normal movement controls to accelerate/reverse and steer, jump to brake, and sneak to dismount when a safe adjacent exit exists. Drive out toward the central aisle, follow the driveway to the approach road, then head inland toward SARC. Walking remains possible. The truck has the existing single driver seat and existing damage/collision behavior.

The truck does not unlock SARC, change facility power, complete a chapter or bypass reception. It is not parked at the SCC.

## Placement and saves

The truck is issued once per coastal site when an eligible player approaches within 48 blocks. This covers both new worlds and existing coastal lots, without attempting to spawn an entity during chunk world generation. Its original 3.8m-wide, 2.9m-high collision body must fit in a fully loaded, dry, empty painted bay. Blocks, containers, entities, unsupported ground or edited paving prevent placement. No terrain is rebuilt or overwritten.

The issued truck UUID is stored in the site's persistent progression. Driving it away, unloading it, reconnecting or revisiting the lot does not issue another. A destroyed truck is not automatically replaced. The truck persists as an ordinary vehicle entity. Nearby players share the same site truck rather than receiving personal duplicates.

Operator fallback near your coastal lot:

```
/coastaltruck repair
/coastaltruck repair confirm
```

The first previews only. Confirm places the original entity only if no truck has already been issued and the bay is safe. It does not summon a replacement for a truck driven elsewhere or destroyed.

## Verification scope

Native dedicated-server tests cover safe placement, blockers, original registration and dimensions, boarding, driver-only controls, physical movement, braking, safe dismount, unchanged player/story state, save compatibility and duplicate prevention. Original truck geometry, texture, renderer and controls are unchanged. No graphical Minecraft/shader client is available; native checks do not certify final shader appearance or a complete player-driven road journey.
