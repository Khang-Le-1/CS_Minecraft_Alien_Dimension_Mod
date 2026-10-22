package com.timelocked;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;

/**
 * Resizes the Timeskipper's EXISTING wall screens in place. No new screens are added: each fitted
 * ship screen (see TimeskipperBridgeScreens) grows into a rectangle of wall monitors that the
 * client renderer draws as one large panel, and its text and live data move to the rectangle's
 * anchor block. The two bridge screens, which used to hang in mid air, are moved onto the bridge
 * side walls beside the helm. Nothing is overwritten: a screen whose space is blocked stays as it
 * was.
 */
public final class TimeskipperBigScreens {
  private static final String MARK = "timelocked:big_screen";
  private static final String FITTED = "timelocked:ceiling_bridge_monitor";

  /**
   * Ship-relative inclusive rectangle, facing, and where the fitted screen currently is (its data
   * moves to the new anchor).
   */
  private record Spec(
      String key,
      int x0,
      int y0,
      int z0,
      int x1,
      int y1,
      int z1,
      Direction face,
      int fx,
      int fy,
      int fz) {}

  private static final Spec[] SPECS = {
    new Spec("bridge_status", -5, 2, -20, -5, 3, -18, Direction.EAST, -2, 4, -18),
    new Spec("bridge_nav", 5, 2, -20, 5, 3, -18, Direction.WEST, 2, 4, -18),
    new Spec("crew", -6, 2, -8, -6, 3, -6, Direction.EAST, -6, 3, -6),
    new Spec("medbay", 6, 2, -8, 6, 3, -6, Direction.WEST, 6, 3, -6),
    new Spec("cargo", -6, 2, 8, -6, 3, 12, Direction.EAST, -6, 3, 9),
    new Spec("engine", 6, 3, 15, 6, 4, 19, Direction.WEST, 6, 3, 15),
    new Spec("engine_aft", -6, 3, 15, -6, 4, 19, Direction.EAST, -6, 3, 15)
  };

  public record Result(int changed, int protectedCells) {}

  private TimeskipperBigScreens() {}

  /** The cell that anchors a rectangle: lowest, and furthest left as the viewer sees it. */
  private static BlockPos anchor(BlockPos origin, Spec s) {
    Direction right = s.face().getCounterClockWise();
    BlockPos best = null;
    long bestKey = Long.MAX_VALUE;
    for (int x = s.x0(); x <= s.x1(); x++) {
      for (int y = s.y0(); y <= s.y1(); y++) {
        for (int z = s.z0(); z <= s.z1(); z++) {
          long u = (long) x * right.getStepX() + (long) z * right.getStepZ();
          long key = u * 1000L + y;
          if (key < bestKey) {
            bestKey = key;
            best = origin.offset(x, y, z);
          }
        }
      }
    }

    return best;
  }

  /** The anchor monitor of an enlarged ship screen, or null if that screen was not enlarged. */
  public static BlockEntity anchorEntity(
      net.minecraft.world.level.Level level, BlockPos origin, String key) {
    for (Spec s : SPECS) {
      if (!s.key().equals(key)) continue;
      BlockEntity be = level.getBlockEntity(anchor(origin, s));
      return be instanceof MonitorContent.MonitorEntity && be.getPersistentData().getBoolean(MARK)
          ? be
          : null;
    }

    return null;
  }

  public static Result fit(ServerLevel level, BlockPos origin) {
    int changed = 0;
    int protectedCells = 0;
    for (Spec s : SPECS) {
      BlockPos anchor = anchor(origin, s);
      if (level.getBlockEntity(anchor) instanceof MonitorContent.MonitorEntity done
          && done.getPersistentData().getBoolean(MARK)) continue;
      BlockPos from = origin.offset(s.fx(), s.fy(), s.fz());
      // Only upgrade the ship's own fitted screens. A custom or removed screen is never touched.
      if (!(level.getBlockEntity(from) instanceof MonitorContent.MonitorEntity source)
          || !source.getPersistentData().getBoolean(FITTED)) continue;
      List<BlockPos> cells = new ArrayList<>();
      boolean clear = true;
      Direction back = s.face().getOpposite();
      for (int x = s.x0(); x <= s.x1() && clear; x++) {
        for (int y = s.y0(); y <= s.y1() && clear; y++) {
          for (int z = s.z0(); z <= s.z1() && clear; z++) {
            BlockPos p = origin.offset(x, y, z);
            cells.add(p);
            if (level.getBlockState(p.relative(back)).isAir()) {
              clear = false;
            } else if (!p.equals(from)
                && (!level.getBlockState(p).isAir()
                    || !level.getFluidState(p).isEmpty()
                    || !level
                        .getEntities((Entity) null, new AABB(p), e -> e.isAlive())
                        .isEmpty())) {
              clear = false;
            }
          }
        }
      }

      if (!clear) {
        protectedCells++;
        continue;
      }

      var registries = level.registryAccess();
      var data = source.saveWithoutMetadata(registries);
      if (!cells.contains(from)) {
        level.setBlock(from, Blocks.AIR.defaultBlockState(), 3);
        changed++;
      }

      for (BlockPos p : cells) {
        if (!p.equals(from) && level.setBlock(p, MonitorContent.wall(s.face()), 3)) changed++;
      }

      if (level.getBlockEntity(anchor) instanceof MonitorContent.MonitorEntity target) {
        try {
          target.loadWithComponents(data, registries);
        } catch (RuntimeException error) {
          // Keep the screen working even if the old data could not be copied.
        }

        target.getPersistentData().putBoolean(FITTED, true);
        target.getPersistentData().putBoolean(MARK, true);
        target.setChanged();
        level.sendBlockUpdated(anchor, target.getBlockState(), target.getBlockState(), 3);
      }

      if (!anchor.equals(from)
          && cells.contains(from)
          && level.getBlockEntity(from) instanceof MonitorContent.MonitorEntity member) {
        member.setLines("", "", "", "");
        member.getPersistentData().remove(FITTED);
        member.setChanged();
      }
    }

    return new Result(changed, protectedCells);
  }
}
