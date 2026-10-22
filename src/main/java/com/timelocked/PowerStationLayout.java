package com.timelocked;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.Heightmap.Types;

/** Chunk-local adapter: all generation writes pass through Out's bounds and bedrock guard. */
public final class PowerStationLayout {
  private static final Map<String, BlockState> STATES = new ConcurrentHashMap<>();

  private record Terrain(boolean suitable, int entry, int exit) {}

  private static final Map<ServerLevel, Map<Long, Terrain>> TERRAIN =
      Collections.synchronizedMap(new WeakHashMap<>());

  private static Terrain terrain(ServerLevel l, BlockPos o) {
    Map<Long, Terrain> sites;
    synchronized (TERRAIN) {
      sites = TERRAIN.computeIfAbsent(l, k -> new ConcurrentHashMap<>());
    }
    return sites.computeIfAbsent(o.asLong(), k -> inspect(l, o));
  }

  private static int surface(ServerLevel l, BlockPos o, int x, int z) {
    return l.getChunkSource()
            .getGenerator()
            .getBaseHeight(
                o.getX() + x,
                o.getZ() + z,
                Types.WORLD_SURFACE_WG,
                l,
                l.getChunkSource().randomState())
        - 1;
  }

  public static boolean suitable(ServerLevel l, BlockPos o) {
    return terrain(l, o).suitable;
  }

  /** Locate the accessible surface entrance, not the deeply buried reactor origin. */
  public static BlockPos entrance(ServerLevel l, BlockPos o) {
    return o.offset(0, terrain(l, o).entry - PowerStationGeometry.PLAN.depth(), 96);
  }

  private static Terrain inspect(ServerLevel l, BlockPos o) {
    int f = o.getY() - PowerStationGeometry.PLAN.depth();
    int entry = surface(l, o, 0, 84) - f, exit = surface(l, o, -48, 86) - f;
    if (f - 14 < l.getMinBuildHeight() + 8
        || entry < 42
        || entry > 56
        || exit < 35
        || exit > 65
        || f + Math.max(entry, exit) + 6 >= l.getMaxBuildHeight())
      return new Terrain(false, entry, exit);
    for (var r : PowerStationGeometry.PLAN.rooms()) {
      for (int x = r.x0(); ; x = Math.min(x + 8, r.x1())) {
        for (int z = r.z0(); ; z = Math.min(z + 8, r.z1())) {
          if (surface(l, o, x, z) < f + r.floor() + r.height() + 6)
            return new Terrain(false, entry, exit);
          if (z == r.z1()) break;
        }
        if (x == r.x1()) break;
      }
    }
    return new Terrain(true, entry, exit);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static BlockState resolve(String spec) {
    return STATES.computeIfAbsent(
        spec,
        k -> {
          int i = k.indexOf('[');
          String id = i < 0 ? k : k.substring(0, i);
          var loc = ResourceLocation.parse(id);
          if (!BuiltInRegistries.BLOCK.containsKey(loc))
            throw new IllegalArgumentException("Unknown station block " + id);
          BlockState state = BuiltInRegistries.BLOCK.get(loc).defaultBlockState();
          if (i >= 0)
            for (String pair : k.substring(i + 1, k.length() - 1).split(",")) {
              String[] p = pair.split("=");
              Property prop = state.getBlock().getStateDefinition().getProperty(p[0]);
              if (prop == null)
                throw new IllegalArgumentException("Unknown station property " + pair);
              state = state.setValue(prop, (Comparable) prop.getValue(p[1]).orElseThrow());
            }
          return state;
        });
  }

  static void place(SarcOutposts.Out out, BlockPos o) {
    int X = o.getX(), Z = o.getZ(), F = o.getY() - PowerStationGeometry.PLAN.depth();
    int kind = Math.floorMod(out.rnd(X, F, Z), 4);
    boolean unstable = Math.floorMod(out.rnd(X, F + 1, Z), 4) == 0;
    PowerStationGeometry.build(
        new PowerStationGeometry.Sink() {
          public int minX() {
            return out.minX - X;
          }

          public int maxX() {
            return out.minX + 15 - X;
          }

          public int minZ() {
            return out.minZ - Z;
          }

          public int maxZ() {
            return out.minZ + 15 - Z;
          }

          public void set(int x, int y, int z, String s) {
            out.set(X + x, F + y, Z + z, resolve(s));
          }
        },
        terrain(out.r.getLevel(), o).entry,
        terrain(out.r.getLevel(), o).exit,
        kind);
    int[] core = PowerStationGeometry.PLAN.core();
    out.industrialCore(X + core[0], F + core[1], Z + core[2], kind, unstable);
    for (var m : PowerStationGeometry.PLAN.station_monitors()) {
      String[] lines = m.lines().clone();
      for (int i = 0; i < lines.length; i++)
        lines[i] = lines[i].replace("{fuel}", PlantCoreBlock.FUELS[kind].replace('_', ' '));
      int theme = m.name().equals("thermal") ? (unstable ? 2 : 0) : m.theme();
      Direction facing = Direction.valueOf(m.facing().toUpperCase(java.util.Locale.ROOT));
      if (m.stand()) out.standMonitor(X + m.x(), F + m.y(), Z + m.z(), facing, theme, lines);
      else out.monitor(X + m.x(), F + m.y(), Z + m.z(), facing, theme, lines);
    }
    for (int z = -52; z <= -32; z += 5)
      for (int x : new int[] {-60, -40})
        out.barrel(X + x, F + 1, Z + z, SarcOutposts.POWER_LOOT, null);
    for (int x : new int[] {-14, 14})
      for (int z = -60; z <= -36; z += 8)
        out.barrel(X + x, F + 1, Z + z, SarcOutposts.POWER_LOOT, null);
    out.barrel(
        X - 60,
        F + 1,
        Z + 60,
        SarcOutposts.POWER_LOOT,
        SarcOutposts.book("Underground Plant Log", "SARC Engineering", SarcOutposts.POWER_LOG));
    out.mob(SarcContent.SCIENTIST.get(), X + 6, F + 4, Z + 14, 1, 7);
  }

  private PowerStationLayout() {}
}
