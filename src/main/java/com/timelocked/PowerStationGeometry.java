package com.timelocked;

import com.google.gson.Gson;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Shared placement geometry used by generation and the offline voxel audit, in relative
 * coordinates.
 */
public final class PowerStationGeometry {
  public record Bounds(int x0, int x1, int z0, int z1) {}

  public record Room(String name, int x0, int x1, int z0, int z1, int floor, int height) {}

  public record Door(String a, String b, int x0, int x1, int z0, int z1, int floor) {}

  public record Industrial(int radius, int[] heights, float mesh_width_scale) {}

  public record Airlock(String name, int x, int z0, int z1, int floor) {}

  public record MonitorMount(
      String name, int x, int y, int z, String facing, boolean stand, int theme, String[] lines) {}

  public record LightMount(String name, int x, int y, int z, String facing, int ceiling_y) {}

  public record AlarmMount(String name, int x, int y, int z, String facing, boolean beacon) {}

  public record Plan(
      int version,
      String release,
      Bounds bounds,
      int depth,
      List<Room> rooms,
      List<Door> connections,
      int[] catwalk_levels,
      int[] core,
      boolean new_generation_only,
      Industrial industrial_reactor,
      List<Airlock> airlocks,
      List<MonitorMount> station_monitors,
      int[] gallery_console,
      List<AlarmMount> station_alarms,
      List<LightMount> station_lights) {}

  public static final Plan PLAN = load();

  private static Plan load() {
    var in =
        PowerStationGeometry.class.getResourceAsStream(
            "/data/timelocked/stations/power-station-plan.json");
    if (in == null) throw new IllegalStateException("Missing power station layout");
    try (var reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
      return new Gson().fromJson(reader, Plan.class);
    } catch (Exception e) {
      throw new IllegalStateException("Invalid power station layout", e);
    }
  }

  public interface Sink {
    int minX();

    int maxX();

    int minZ();

    int maxZ();

    void set(int x, int y, int z, String state);
  }

  static final String AIR = "minecraft:air",
      FLOOR = "timelocked:sarc_floor_tile",
      WALL = "timelocked:sarc_wall_panel",
      FRAME = "timelocked:sarc_reinforced_shell",
      LIGHT = "timelocked:station_utility_light[facing=down]",
      GLASS = "minecraft:cyan_stained_glass",
      HAZARD = "timelocked:sarc_hazard_trim",
      PIPE = "timelocked:station_coolant_pipe",
      RAIL = "minecraft:iron_bars[north=true,east=true,south=true,west=true]";

  static void box(Sink s, int x0, int x1, int y0, int y1, int z0, int z1, String block) {
    for (int x = Math.max(x0, s.minX()); x <= Math.min(x1, s.maxX()); x++)
      for (int z = Math.max(z0, s.minZ()); z <= Math.min(z1, s.maxZ()); z++)
        for (int y = y0; y <= y1; y++) s.set(x, y, z, block);
  }

  private static void room(Sink s, Room r) {
    for (int x = Math.max(r.x0, s.minX()); x <= Math.min(r.x1, s.maxX()); x++)
      for (int z = Math.max(r.z0, s.minZ()); z <= Math.min(r.z1, s.maxZ()); z++) {
        boolean edge = x == r.x0 || x == r.x1 || z == r.z0 || z == r.z1;
        boolean route = (x == (r.x0 + r.x1) / 2 || z == (r.z0 + r.z1) / 2);
        s.set(
            x,
            r.floor,
            z,
            route
                ? "timelocked:sarc_route_tile"
                : (Math.floorMod(x, 8) == 0 || Math.floorMod(z, 8) == 0 ? FRAME : FLOOR));
        s.set(x, r.floor + r.height, z, FRAME);
        for (int y = 1; y < r.height; y++) {
          boolean pier = Math.floorMod(x, 8) == 0 || Math.floorMod(z, 8) == 0;
          String zone =
              r.name.contains("coolant") || r.name.contains("water")
                  ? "timelocked:sarc_clean_panel"
                  : "timelocked:sarc_service_panel";
          if (r.name.contains("reactor") && y == 3) zone = "timelocked:sarc_containment_panel";
          if (y == r.height - 2 && !pier) zone = "timelocked:sarc_vent_panel";
          s.set(x, r.floor + y, z, edge ? (y <= 1 || pier ? FRAME : zone) : AIR);
        }
        if (!edge) {
          if (Math.floorMod(z, 8) == 0) s.set(x, r.floor + r.height - 1, z, FRAME);
          if (Math.floorMod(x, 8) == 4
              && Math.floorMod(z, 8) == 4
              && !(r.name.equals("lower_reactor_service") && x == 20 && z == -4))
            s.set(x, r.floor + r.height - 1, z, LIGHT);
        }
      }
  }

  private static void stairs(Sink s, int x0, int x1, int z, int y, boolean south) {
    box(s, x0, x1, y + 1, y + 4, z, z, AIR);
    box(
        s,
        x0,
        x1,
        y,
        y,
        z,
        z,
        "timelocked:sarc_floor_stairs[facing=" + (south ? "south" : "north") + "]");
  }

  private static void pipe(Sink s, int x0, int x1, int y0, int y1, int z0, int z1, String axis) {
    box(s, x0, x1, y0, y1, z0, z1, PIPE + "[axis=" + axis + "]");
  }

  public static void build(Sink s, int entranceHeight, int emergencyHeight, int coreKind) {
    for (Room r : PLAN.rooms) room(s, r);
    // Elevated glass gallery, then intentional openings rather than intersecting sealed shells.
    // Taller two-layer viewport keeps the enlarged upper assembly visible from the controls.
    box(s, -12, 12, 4, 14, 7, 8, GLASS);
    for (Door d : PLAN.connections) {
      box(s, d.x0, d.x1, d.floor, d.floor, d.z0, d.z1, FLOOR);
      box(s, d.x0, d.x1, d.floor + 1, d.floor + 4, d.z0, d.z1, AIR);
    }
    for (int z = 0; z <= 3; z++) stairs(s, -11, -9, z, z, true);
    for (int z = 26; z <= 29; z++) stairs(s, -2, 2, z, 29 - z, false);
    for (int x = -15; x <= -12; x++) {
      box(s, x, x, x + 16, x + 19, 13, 17, AIR);
      box(s, x, x, x + 15, x + 15, 13, 17, "timelocked:sarc_floor_stairs[facing=east]");
    }
    for (int x = 12; x <= 15; x++) {
      box(s, x, x, 16 - x, 19 - x, 13, 17, AIR);
      box(s, x, x, 15 - x, 15 - x, 13, 17, "timelocked:sarc_floor_stairs[facing=west]");
    }
    // Main entrance stair tunnel: about fifty metres of descent, no giant surface yard.
    int start = Math.max(30, 84 - entranceHeight);
    int end = start + entranceHeight;
    for (int z = start; z <= end; z++) {
      int y = z - start;
      box(s, -4, 4, y - 1, y + 5, z, z, FRAME);
      box(s, -3, 3, y, y + 4, z, z, AIR);
      stairs(s, -3, 3, z, y, true);
      if (z % 8 == 0) {
        s.set(-3, y + 3, z, "timelocked:station_utility_light[facing=east]");
        s.set(3, y + 3, z, "timelocked:station_utility_light[facing=west]");
      }
    }
    room(s, new Room("surface_entry", -9, 9, 80, 96, entranceHeight, 6));
    // Reopen the stair end through the surface entry floor, after its shell was built.
    for (int z = 80; z <= end; z++) stairs(s, -3, 3, z, z - start, true);
    box(s, -1, 1, entranceHeight + 1, entranceHeight + 3, 96, 96, AIR);
    box(s, -7, -3, entranceHeight + 2, entranceHeight + 3, 96, 96, GLASS);
    box(s, 3, 7, entranceHeight + 2, entranceHeight + 3, 96, 96, GLASS);
    box(s, -9, 9, entranceHeight - 4, entranceHeight - 1, 80, 96, FRAME);
    // Restore all stair steps penetrating the entrance foundation.
    for (int z = 80; z <= end; z++) stairs(s, -3, 3, z, z - start, true);
    // Independent emergency ladder shaft, casing also seals aquifers.
    box(s, -51, -45, -1, emergencyHeight + 5, 83, 89, FRAME);
    box(s, -50, -46, 1, emergencyHeight + 4, 84, 88, AIR);
    room(s, new Room("exit_hatch", -52, -44, 82, 90, emergencyHeight, 5));
    box(s, -50, -46, 1, emergencyHeight + 3, 84, 88, AIR);
    box(s, -49, -47, 1, 4, 83, 83, AIR);
    box(s, -49, -47, emergencyHeight + 1, emergencyHeight + 3, 90, 90, AIR);
    // A real exit landing: the old shaft left a multi-block gap from the ladder to the hatch.
    box(s, -50, -46, emergencyHeight, emergencyHeight, 84, 88, FLOOR);
    box(s, -50, -50, emergencyHeight, emergencyHeight, 86, 86, AIR);
    box(s, -50, -50, 1, emergencyHeight + 2, 86, 86, "minecraft:ladder[facing=east]");
    // Service level access, with an actual fourteen-block staircase.
    for (int z = -14; z <= 0; z++) stairs(s, 18, 20, z, z, true);
    // Two walkable maintenance rings, rails leave clear stairs and landings.
    for (int h : PLAN.catwalk_levels) {
      box(s, -20, -18, h, h, -26, 6, "timelocked:sarc_grated_walkway");
      box(s, 18, 20, h, h, -26, 6, "timelocked:sarc_grated_walkway");
      box(s, -20, 20, h, h, -26, -24, "timelocked:sarc_grated_walkway");
      box(s, -20, -7, h, h, 4, 6, "timelocked:sarc_grated_walkway");
      box(s, 7, 20, h, h, 4, 6, "timelocked:sarc_grated_walkway");
      box(s, -17, -17, h + 1, h + 1, -23, 3, RAIL);
      box(s, 17, 17, h + 1, h + 1, -23, 3, RAIL);
      box(s, -17, 17, h + 1, h + 1, -23, -23, RAIL);
      box(s, -17, -7, h + 1, h + 1, 3, 3, RAIL);
      box(s, 7, 17, h + 1, h + 1, 3, 3, RAIL);
      box(s, -7, -7, h + 1, h + 1, 4, 6, RAIL);
      box(s, 7, 7, h + 1, h + 1, 4, 6, RAIL);
    }
    for (int z = -24; z <= -16; z++) stairs(s, -20, -18, z, z + 24, true);
    for (int z = -24; z <= -16; z++) stairs(s, 18, 20, z, z + 32, true);
    // Upper stair needs a bridge from the first ring, not a jump over the rail.
    box(s, 18, 20, 8, 8, -26, -24, FLOOR);
    box(s, -20, -18, 8, 8, -15, -12, FLOOR);
    box(s, 18, 20, 16, 16, -15, -12, FLOOR);
    // Every part of the assembly uses the same physical core center as runtime generation.
    int CX = PLAN.core[0], CY = PLAN.core[1], CZ = PLAN.core[2], floor = CY - 1;
    Room vault =
        PLAN.rooms.stream().filter(r -> r.name.equals("reactor_vault")).findFirst().orElseThrow();
    int ceiling = vault.floor + vault.height;
    box(s, CX - 6, CX + 6, floor, floor, CZ - 6, CZ + 6, FLOOR);
    for (int x = -6; x <= 6; x++)
      for (int z = -6; z <= 6; z++)
        if (Math.max(Math.abs(x), Math.abs(z)) == 6)
          box(s, CX + x, CX + x, floor, floor, CZ + z, CZ + z, HAZARD);
    s.set(CX, CY, CZ, "timelocked:sarc_plant_core[kind=" + coreKind + ",lit=false]");
    for (int x : new int[] {CX - 9, CX + 9}) {
      box(s, x - 1, x + 1, floor, floor, CZ - 3, CZ + 3, HAZARD);
      pipe(s, x, x, CY, ceiling - 1, CZ - 2, CZ - 2, "y");
      pipe(s, x, x, CY, ceiling - 1, CZ + 2, CZ + 2, "y");
      pipe(s, x, x, ceiling - 1, ceiling - 1, CZ - 2, CZ + 2, "z");
      s.set(x, CY, CZ, "timelocked:station_coolant_pump[facing=south]");
    }
    // Move the rear service gantry with the assembly, clear of the containment envelope.
    box(s, CX - 16, CX + 16, 18, 18, CZ - 12, CZ - 12, "minecraft:iron_block");
    box(s, CX, CX, 10, 17, CZ - 12, CZ - 12, "minecraft:chain");
    box(s, CX - 1, CX + 1, 9, 9, CZ - 13, CZ - 11, HAZARD);
    // Four heavy corner supports and a ceiling gantry anchor the assembly, not a stretched vessel.
    for (int x : new int[] {CX - 6, CX + 6})
      for (int z : new int[] {CZ - 6, CZ + 6}) box(s, x, x, CY, ceiling - 1, z, z, FRAME);
    for (int z : new int[] {CZ - 6, CZ + 6})
      box(s, CX - 6, CX + 6, ceiling - 1, ceiling - 1, z, z, FRAME);
    for (int x : new int[] {CX - 6, CX + 6})
      box(s, x, x, ceiling - 1, ceiling - 1, CZ - 6, CZ + 6, FRAME);
    box(s, CX - 9, CX + 9, ceiling - 1, ceiling - 1, CZ, CZ, FRAME);
    s.set(CX, ceiling - 2, CZ, LIGHT);
    // Sloped modeled control desks face the operator, with room to walk behind them.
    int[] control = PLAN.gallery_console;
    for (int x = control[0] - 2; x <= control[0] + 2; x++)
      s.set(x, control[1], control[2], "timelocked:station_control_console[facing=south]");
    // Stand displays are mounted later from the shared monitor schedule, not on the floor.
    // Coolant pump rows and pipe headers, kept away from five-wide doors.
    for (int x : new int[] {-60, -40}) {
      pipe(s, x, x, 6, 6, -18, 6, "z");
      for (int z = -16; z <= 4; z += 5) {
        s.set(
            x,
            1,
            z,
            "timelocked:station_coolant_pump[facing=" + (x == -60 ? "east" : "west") + "]");
        pipe(s, x, x, 2, 6, z, z, "y");
      }
    }
    // Turbine trains: segmented ribs, explicit bases, aligned service aisle and overhead ducts.
    for (int x : new int[] {36, 54}) {
      for (int z = -42; z <= -6; z++) {
        box(s, x - 2, x + 2, 0, 0, z, z, HAZARD);
        for (int dx = -1; dx <= 1; dx++)
          for (int y = 1; y <= 3; y++)
            s.set(x + dx, y, z, "timelocked:station_turbine_module[facing=south]");
      }
      pipe(s, x, x, 8, 8, -44, 4, "z");
      for (int z = -38; z <= -6; z += 8) pipe(s, x, x, 2, 7, z, z, "y");
    }
    // Generator and switchgear banks have cable trays, aisle gaps and consoles.
    for (int x : new int[] {42, 58})
      for (int z = 28; z <= 40; z += 4) {
        for (int dx = -1; dx <= 1; dx++)
          s.set(x + dx, 1, z, "timelocked:station_turbine_module[facing=north]");
        s.set(x, 2, z, "minecraft:lightning_rod[facing=up]");
      }
    for (int x : new int[] {-60, -40})
      for (int z = 28; z <= 40; z += 4) {
        box(s, x, x, 1, 3, z, z, "minecraft:iron_block");
        // Cabinet-mounted screens are placed by the same shared monitor schedule.
        s.set(x, 4, z, "timelocked:electrical_conduit");
      }
    for (int x : new int[] {42, 58}) {
      box(s, x - 3, x + 3, 1, 1, 57, 72, FRAME);
      box(s, x - 3, x + 3, 2, 5, 57, 72, "minecraft:cyan_stained_glass");
      box(s, x - 2, x + 2, 2, 4, 58, 71, "minecraft:water");
      box(s, x - 3, x + 3, 6, 6, 57, 72, FRAME);
      pipe(s, x, x, 7, 7, 54, 75, "z");
    }
    for (int z = 52; z <= 68; z += 8)
      for (int x : new int[] {-60, -40})
        s.set(
            x,
            1,
            z,
            "timelocked:station_control_console[facing=" + (x == -60 ? "east" : "west") + "]");
    // Tool/preparation bays make the workshop a room rather than another empty hall.
    for (int z : new int[] {58, 68}) {
      box(s, -54, -50, 0, 0, z - 1, z + 1, "timelocked:sarc_containment_panel");
      box(s, -54, -54, 1, 2, z, z, "timelocked:sarc_service_panel");
      box(s, -50, -50, 1, 2, z, z, "timelocked:sarc_service_panel");
      for (int x = -53; x <= -51; x++) s.set(x, 1, z, "timelocked:sarc_workstation[facing=south]");
    }
    // Fuel storage and lower-level coolant manifold. Storage is loot-bearing at runtime.
    for (int z = -52; z <= -32; z += 5)
      for (int x : new int[] {-60, -40}) s.set(x, 1, z, "minecraft:barrel[facing=up]");
    for (int x = -12; x <= 12; x += 6) {
      pipe(s, x, x, -13, -7, -18, -18, "y");
      s.set(x, -13, -12, "timelocked:station_coolant_pump[facing=south]");
    }
    pipe(s, -12, 12, -7, -7, -18, -18, "x");
    for (int x : new int[] {-14, 14})
      for (int z = -60; z <= -36; z += 8) s.set(x, 1, z, "minecraft:barrel[facing=up]");
    box(s, -5, 5, -14, -14, 14, 28, HAZARD);
    s.set(0, -13, 22, "timelocked:station_control_console[facing=south]");
    // Chunk-local containment volume matches the larger renderer and interaction retargeting.
    int[] heights = PLAN.industrial_reactor.heights;
    for (int x = -5; x <= 5; x++)
      for (int z = -5; z <= 5; z++)
        if (x * x + z * z <= 30.25)
          for (int y = 1; y <= heights[Math.floorMod(coreKind, 4)]; y++)
            if (x != 0 || z != 0 || y != 1)
              box(
                  s,
                  CX + x,
                  CX + x,
                  CY + y - 1,
                  CY + y - 1,
                  CZ + z,
                  CZ + z,
                  "timelocked:reactor_casing");
    // Inner service decks connect the reactor assembly to the existing maintenance rings.
    for (int h : PLAN.catwalk_levels) {
      box(s, -18, 18, h, h, CZ - 10, CZ - 8, "timelocked:sarc_grated_walkway");
      box(s, -9, 9, h + 1, h + 1, CZ - 7, CZ - 7, RAIL);
      box(s, -9, 9, h + 1, h + 1, CZ - 11, CZ - 11, RAIL);
      box(s, -17, -17, h + 1, h + 2, CZ - 10, CZ - 8, AIR);
      box(s, 17, 17, h + 1, h + 2, CZ - 10, CZ - 8, AIR);
    }
    // Mounted task rail over the controls: drops reach the roof, never sit on the floor.
    box(s, -4, 4, 8, 8, 12, 12, FRAME);
    for (int x : new int[] {-4, 4}) box(s, x, x, 9, 15, 12, 12, "minecraft:chain");
    // Ladder fixtures face into the shaft, away from its climbing line.
    for (int y = 3; y <= emergencyHeight - 2; y += 8)
      s.set(-46, y, 85, "timelocked:station_utility_light[facing=west]");
    // All authored routes into the vault/service chamber pass through two shield-door banks.
    for (Airlock a : PLAN.airlocks) vestibule(s, a.x, a.z0, a.z1, a.floor);
    for (LightMount m : PLAN.station_lights) {
      if (m.ceiling_y != 0) {
        s.set(m.x, m.y + 1, m.z, FRAME);
        box(s, m.x, m.x, m.y + 2, m.ceiling_y - 1, m.z, m.z, "minecraft:chain");
      }
      s.set(m.x, m.y, m.z, "timelocked:station_utility_light[facing=" + m.facing + "]");
    }
    for (AlarmMount a : PLAN.station_alarms)
      s.set(
          a.x,
          a.y,
          a.z,
          "timelocked:sarc_alarm_"
              + (a.beacon ? "beacon" : "siren")
              + "[facing="
              + a.facing
              + ",active=false,manual=false,lit=false]");
    // Keep model placement, physical mounts and runtime display data in one schedule.
    for (MonitorMount m : PLAN.station_monitors) {
      if (m.stand)
        s.set(m.x, m.y - 1, m.z, "timelocked:station_monitor_pedestal[facing=" + m.facing + "]");
      s.set(
          m.x, m.y, m.z, "timelocked:sarc_monitor[facing=" + m.facing + ",stand=" + m.stand + "]");
    }
  }

  private static void vestibule(Sink s, int x, int z0, int z1, int floor) {
    box(s, x - 3, x + 3, floor - 1, floor + 5, z0, z1, FRAME);
    box(s, x - 1, x + 1, floor + 1, floor + 4, z0 + 1, z1 - 1, AIR);
    box(s, x - 1, x + 1, floor, floor, z0 + 1, z1 - 1, HAZARD);
    for (int z : new int[] {z0, z1})
      for (int leaf = 0; leaf < 2; leaf++) {
        String state =
            "timelocked:sarc_reactor_airlock[facing=north,hinge="
                + (leaf == 0 ? "left" : "right")
                + ",open=false,powered=false,purge=0,half=";
        box(s, x - 1 + leaf, x - 1 + leaf, floor + 1, floor + 1, z, z, state + "lower]");
        box(s, x - 1 + leaf, x - 1 + leaf, floor + 2, floor + 2, z, z, state + "upper]");
      }
    box(s, x, x, floor + 4, floor + 4, z0 + 2, z0 + 3, LIGHT);
  }

  private PowerStationGeometry() {}
}
