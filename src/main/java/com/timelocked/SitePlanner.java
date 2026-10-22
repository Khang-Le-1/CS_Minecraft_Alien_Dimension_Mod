package com.timelocked;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public final class SitePlanner {
  public static final int SPACING = 4096;

  private static long mix(long n) {
    n = (n ^ n >>> 30) * -4658895280553007687L;
    n = (n ^ n >>> 27) * -7723592293110705685L;
    return n ^ n >>> 31;
  }

  public static BlockPos horizontal(long seed, int rx, int rz) {
    long n = mix(seed + rx * 341873128712L + rz * 132897987541L);
    int x = rx * 4096 + 512 + Math.floorMod(n, 1536);
    int z = rz * 4096 + 512 + Math.floorMod(mix(n), 1536);
    return new BlockPos(x & -16, 0, z & -16);
  }

  public static int designation(long seed, BlockPos o) {
    return Math.floorMod(mix(seed ^ (long) o.getX() << 32 ^ o.getZ()), 1000);
  }

  public static BlockPos origin(ServerLevel level, BlockPos horizontal) {
    return FacilityTerrain.plan(level, horizontal).origin();
  }

  public static List<BlockPos> nearby(ServerLevel level, BlockPos pos) {
    int rx = Math.floorDiv(pos.getX(), 4096);
    int rz = Math.floorDiv(pos.getZ(), 4096);
    List<BlockPos> result = new ArrayList<>();

    for (int x = rx - 1; x <= rx + 1; x++) {
      for (int z = rz - 1; z <= rz + 1; z++) {
        result.add(origin(level, horizontal(level.getSeed(), x, z)));
      }
    }

    return result;
  }

  public static Optional<BlockPos> containing(ServerLevel level, BlockPos pos) {
    FacilityState state = FacilityState.get(level.getServer());

    for (long packed : state.sites.keySet()) {
      BlockPos o = BlockPos.of(packed);
      if (contains(level, o, pos)) {
        return Optional.of(o);
      }
    }

    for (BlockPos o : nearby(level, pos)) {
      if (contains(level, o, pos)) {
        state.sites.putIfAbsent(o.asLong(), designation(level.getSeed(), o));
        state.setDirty();
        return Optional.of(o);
      }
    }

    return Optional.empty();
  }

  private static boolean contains(ServerLevel level, BlockPos o, BlockPos p) {
    return p.getX() >= o.getX()
        && p.getX() < o.getX() + BlueprintRooms.width(level, o)
        && p.getY() >= o.getY()
        && p.getY() < o.getY() + 43
        && p.getZ() >= o.getZ()
        && p.getZ() < o.getZ() + BlueprintRooms.depth(level, o);
  }

  public static SiteProgress progress(ServerLevel level, BlockPos origin) {
    FacilityState s = FacilityState.get(level.getServer());
    return s.progress.computeIfAbsent(
        origin.asLong(),
        k -> {
          s.setDirty();
          return new SiteProgress();
        });
  }

  public static boolean powered(ServerLevel l, BlockPos pos) {
    return containing(l, pos).map(o -> progress(l, o).powered).orElse(true);
  }

  private SitePlanner() {}
}
