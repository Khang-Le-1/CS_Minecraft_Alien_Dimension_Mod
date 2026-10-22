package com.timelocked;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/** Exact authored offsets link loaded fixtures to their station; never loads or tickets chunks. */
public final class StationAlarms {
  private static final Map<ServerLevel, Map<BlockPos, BlockPos>> CACHE = new WeakHashMap<>();

  private static boolean blast(ServerLevel l, BlockPos core) {
    return PlantMeltdown.ACTIVE.stream()
        .anyMatch(b -> b.industrial && b.dim.equals(l.dimension()) && b.c.equals(core));
  }

  private static boolean valid(ServerLevel l, BlockPos core) {
    return l.hasChunkAt(core)
        && ((l.getBlockEntity(core) instanceof PlantCoreBlock.Core c && c.industrial)
            || blast(l, core));
  }

  public static BlockPos linkedCore(ServerLevel l, BlockPos p) {
    var cache =
        CACHE.computeIfAbsent(
            l,
            k ->
                new LinkedHashMap<>() {
                  protected boolean removeEldestEntry(Map.Entry<BlockPos, BlockPos> e) {
                    return size() > 1024;
                  }
                });
    BlockPos cached = cache.get(p);
    if (cached != null && valid(l, cached)) return cached;
    cache.remove(p);
    BlockState state = l.getBlockState(p);
    int[] c = PowerStationGeometry.PLAN.core();
    for (var a : PowerStationGeometry.PLAN.station_alarms()) {
      if (!state.is(
          a.beacon() ? FacilityDetails.ALARM_BEACON.get() : FacilityDetails.ALARM_SIREN.get()))
        continue;
      BlockPos core = p.offset(c[0] - a.x(), c[1] - a.y(), c[2] - a.z());
      if (valid(l, core)) {
        cache.put(p.immutable(), core);
        return core;
      }
    }
    return null;
  }

  public static boolean fault(ServerLevel l, BlockPos p) {
    BlockPos pos = linkedCore(l, p);
    if (pos == null) return false;
    if (blast(l, pos)) return true;
    return l.getBlockEntity(pos) instanceof PlantCoreBlock.Core c
        && !c.spent
        && (c.critical() || c.heat >= 800 || c.coolantFailing && c.fuel > 0);
  }

  public static List<BlockPos> group(ServerLevel l, BlockPos p) {
    BlockPos core = linkedCore(l, p);
    if (core == null) return List.of();
    int[] c = PowerStationGeometry.PLAN.core();
    BlockPos origin = core.offset(-c[0], -c[1], -c[2]);
    var out = new ArrayList<BlockPos>();
    for (var a : PowerStationGeometry.PLAN.station_alarms()) {
      BlockPos q = origin.offset(a.x(), a.y(), a.z());
      if (l.hasChunkAt(q)
          && l.getBlockState(q)
              .is(
                  a.beacon()
                      ? FacilityDetails.ALARM_BEACON.get()
                      : FacilityDetails.ALARM_SIREN.get())) out.add(q);
    }
    return out;
  }

  private StationAlarms() {}
}
