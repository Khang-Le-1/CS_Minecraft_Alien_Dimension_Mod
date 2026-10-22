package com.timelocked;

import java.util.ArrayDeque;
import java.util.HashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/** Bounded, loaded-only discovery shared by model validation and the client renderer. */
public final class MonitorWallLayout {
  public record Wall(BlockPos anchor, int width, int height) {}

  public static Wall find(Level level, BlockPos start, Direction facing) {
    Wall single = new Wall(start, 1, 1);
    if (level == null) return single;
    Direction right = facing.getCounterClockWise();
    var seen = new HashSet<BlockPos>();
    var queue = new ArrayDeque<BlockPos>();
    seen.add(start);
    queue.add(start);
    while (!queue.isEmpty()) {
      BlockPos p = queue.remove();
      for (BlockPos n :
          new BlockPos[] {
            p.relative(right), p.relative(right.getOpposite()), p.above(), p.below()
          }) {
        if (seen.contains(n) || !level.hasChunkAt(n)) continue;
        var s = level.getBlockState(n);
        if (s.is(MonitorContent.MONITOR.get())
            && !s.getValue(MonitorContent.MonitorBlock.STAND)
            && s.getValue(MonitorContent.MonitorBlock.FACING) == facing) {
          seen.add(n);
          if (seen.size() > 40) return single;
          queue.add(n);
        }
      }
    }
    long lo = Long.MAX_VALUE, hi = Long.MIN_VALUE;
    int y0 = Integer.MAX_VALUE, y1 = Integer.MIN_VALUE;
    BlockPos anchor = start;
    for (BlockPos p : seen) {
      long u = (long) p.getX() * right.getStepX() + (long) p.getZ() * right.getStepZ();
      if (u < lo || (u == lo && p.getY() < anchor.getY())) {
        lo = u;
        anchor = p;
      }
      hi = Math.max(hi, u);
      y0 = Math.min(y0, p.getY());
      y1 = Math.max(y1, p.getY());
    }
    int w = (int) (hi - lo + 1), h = y1 - y0 + 1;
    return w <= 8 && h <= 5 && seen.size() == w * h ? new Wall(anchor, w, h) : single;
  }

  private MonitorWallLayout() {}
}
