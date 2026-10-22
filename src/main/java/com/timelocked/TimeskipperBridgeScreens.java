package com.timelocked;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/** Opt-in, idempotent relocation of the two live bridge monitors, preserving NBT. */
public final class TimeskipperBridgeScreens {
  private static final String FITTED = "timelocked:ceiling_bridge_monitor";
  public static final BlockPos STATUS = new BlockPos(-2, 4, -18);
  public static final BlockPos NAV_HINT = new BlockPos(2, 4, -18);

  public static Result fit(ServerLevel level, BlockPos origin) {
    int changed = 0, protectedCells = 0;
    int[][] placements = {
      {-2, 1, -18, -2, 4, -18, 0},
      {2, 1, -18, 2, 4, -18, 0},
      {-5, 1, -6, -6, 3, -6, 1},
      {5, 1, -6, 6, 3, -6, 2},
      {5, 1, -3, 6, 3, -3, 2},
      {-3, 1, 9, -6, 3, 9, 1},
      {3, 1, 15, 6, 3, 15, 2},
      {-3, 1, 15, -6, 3, 15, 1}
    };
    for (int[] row : placements) {
      BlockPos old = origin.offset(row[0], row[1], row[2]),
          at = origin.offset(row[3], row[4], row[5]);
      Direction face =
          row[6] == 0 ? Direction.SOUTH : (row[6] == 1 ? Direction.EAST : Direction.WEST);
      if (!(level.getBlockEntity(old) instanceof MonitorContent.MonitorEntity source)) continue;
      // Never overwrite anything, including a customized ceiling or another monitor.
      if (!level.getBlockState(at).isAir()
          || !level.getFluidState(at).isEmpty()
          || !level
              .getEntities((net.minecraft.world.entity.Entity) null, new AABB(at), e -> e.isAlive())
              .isEmpty()) {
        protectedCells++;
        continue;
      }
      var data = source.saveWithoutMetadata(level.registryAccess());
      if (!level.setBlock(at, MonitorContent.wall(face), 3)) {
        protectedCells++;
        continue;
      }
      if (level.getBlockEntity(at) instanceof MonitorContent.MonitorEntity target) {
        try {
          target.loadWithComponents(data, level.registryAccess());
          target.getPersistentData().putBoolean(FITTED, true);
          target.setChanged();
          level.sendBlockUpdated(at, target.getBlockState(), target.getBlockState(), 3);
        } catch (RuntimeException error) {
          // Roll back only the newly created target. The old source is untouched.
          level.setBlock(at, Blocks.AIR.defaultBlockState(), 3);
          protectedCells++;
          continue;
        }
        level.setBlock(old, Blocks.AIR.defaultBlockState(), 3);
        changed += 2;
      } else {
        level.setBlock(at, Blocks.AIR.defaultBlockState(), 3);
        protectedCells++;
      }
    }
    return new Result(changed, protectedCells);
  }

  public static net.minecraft.world.level.block.entity.BlockEntity status(
      net.minecraft.world.level.Level level, BlockPos origin) {
    var big = TimeskipperBigScreens.anchorEntity(level, origin, "bridge_status");
    if (big != null) return big;
    var ceiling = level.getBlockEntity(origin.offset(STATUS));
    if (ceiling instanceof MonitorContent.MonitorEntity
        && ceiling.getPersistentData().getBoolean(FITTED)) return ceiling;
    // An unrelated/custom monitor at the destination is never updated by the ship.
    return level.getBlockEntity(origin.offset(TimeskipperShip.BRIDGE_SCREEN));
  }

  public static net.minecraft.world.level.block.entity.BlockEntity engine(
      net.minecraft.world.level.Level level, BlockPos o) {
    var bigEngine = TimeskipperBigScreens.anchorEntity(level, o, "engine");
    if (bigEngine != null) return bigEngine;
    var wall = level.getBlockEntity(o.offset(6, 3, 15));
    if (wall instanceof MonitorContent.MonitorEntity && wall.getPersistentData().getBoolean(FITTED))
      return wall;
    return level.getBlockEntity(o.offset(TimeskipperShip.ENGINE_SCREEN));
  }

  public record Result(int changed, int protectedCells) {}

  private TimeskipperBridgeScreens() {}
}
