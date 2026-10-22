# Objective HUD — v0.8.9.36

A compact top-center panel shows the current Story-mode task. It follows the existing saved milestones; it does not restart the intro, grant clearance, complete quests, teleport anyone, or rebuild a world.

- Localized task text, a rotating direction arrow, horizontal distance in blocks, and an above/below hint when a known target differs by at least four blocks.
- Read-only targets for the campus opening, legacy home introduction, coastal approach, visitor registration, and briefing/orientation assignment.
- Street and ramp subtargets help with the College Road / Lambert Road approach. The facility entrance uses stairwell landmarks. Arrows show a bearing to a landmark, not an obstacle-avoiding route or a world-space waypoint beacon. Follow actual doors, corridors, and stairs.
- Terrain-graded inland road targets do not invent an elevation or report a descent to Y=0.
- The panel updates twice per second from the server; direction and distance follow the local player every rendered frame.
- No objective is shared between players. Private intro scenes use each player's saved scene slot and facility objectives use that player's saved site.
- Timed class, falling-object, and welcome moments show text without an invented destination. Later assignments are not revealed before their handoff. The panel hides after the final released assignment.
- F1 hides it. It also hides for death, spectators, wrong dimensions, intro locked-camera scenes, portal/jump cinematics, and a stale/missing server heartbeat. Session state clears on login/logout.
- When wearing an ATLAS helmet, it sits below the existing top-center health panel. Task and navigation text wrap within the available panel width.

Requires matching v36 on client and server. Story mode must already be active; ordinary non-Story worlds do not acquire objectives.

## Preservation

Truck entry/exit recordings, idle/driving loops, push-bar door audio, Chapter 3, and SCC circulation fixes are retained. No new audio was synthesized. Existing audio asset bytes are unchanged from v35.

## Validation and limits

Production compilation and dedicated-server regression tests cover saved milestone resolution, handoffs, private scene offsets, dimension gates, route landmark selection, direction math, corrupt/old state handling, packet codec round trips, and the existing story/geometry/audio regression suite. The native server loads the real mod and its registrations.

A dedicated-server pass is not an in-game visual or listening test. The release has not been visually tested in a Minecraft client; GUI scale, resource packs, shaders, and other mods' overlays still warrant a quick in-game check. The HUD does not add pathfinding, map markers, a minimap, or new quest content.
