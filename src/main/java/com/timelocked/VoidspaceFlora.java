package com.timelocked;

import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class VoidspaceFlora {
  public static void emit(
      int x, int groundY, int z, int height, boolean live, BiConsumer<BlockPos, BlockState> sink) {
    tree(
        x,
        groundY,
        z,
        Math.max(6, height + 2),
        live ? 0 : 3,
        Math.floorMod(x * 31 + z * 17, 4),
        sink);
  }

  /** Silverleaf, moonveil, lantern-acacia and dead snag; all have radius <=6. */
  public static void tree(
      int x, int g, int z, int h, int species, int turn, BiConsumer<BlockPos, BlockState> out) {
    BlockState bark =
        (species == 3 ? Content.CHARRED_TIMBER.get() : VoidspaceContent.CELESTIAL_BARK.get())
            .defaultBlockState();
    BlockState leaf =
        (species == 1 ? VoidspaceContent.MOONVEIL_LEAVES.get() : VoidspaceContent.SILVERLEAF.get())
            .defaultBlockState();
    int lx = turn == 0 ? 1 : turn == 2 ? -1 : 0, lz = turn == 1 ? 1 : turn == 3 ? -1 : 0;
    for (int y = 1; y <= h; y++) {
      int bend = y > h / 2 ? 1 : 0;
      out.accept(new BlockPos(x + lx * bend, g + y, z + lz * bend), bark);
      if (bend == 1 && y == h / 2 + 1) out.accept(new BlockPos(x, g + y, z), bark);
    }
    for (int[] dir : new int[][] {{1, 0}, {0, 1}, {-1, 0}, {0, -1}}) {
      out.accept(new BlockPos(x + dir[0], g + 1, z + dir[1]), bark);
      int by = h - 2 + (dir[0] == 0 ? -1 : 0), reach = species == 1 ? 3 : 2;
      for (int j = 1; j <= reach; j++) {
        out.accept(new BlockPos(x + lx + dir[0] * j, g + by + j / 2, z + lz + dir[1] * j), bark);
        if (j > 1)
          out.accept(
              new BlockPos(x + lx + dir[0] * j, g + by + (j - 1) / 2, z + lz + dir[1] * j), bark);
      }
      if (species != 3) {
        int cx = x + lx + dir[0] * reach, cz = z + lz + dir[1] * reach, cy = g + by + reach / 2 + 1;
        crown(cx, cy, cz, 2, leaf, out);
        if (species == 1)
          for (int k = 1; k <= 3; k++)
            out.accept(
                new BlockPos(cx + dir[0], cy - k, cz + dir[1]),
                VoidspaceContent.STARLEAF.get().defaultBlockState());
        else if (species == 2)
          out.accept(
              new BlockPos(cx, cy - 2, cz), VoidspaceContent.STARLEAF.get().defaultBlockState());
      }
    }
    if (species != 3) crown(x + lx, g + h + 1, z + lz, 2, leaf, out);
  }

  private static void crown(
      int x, int y, int z, int r, BlockState leaf, BiConsumer<BlockPos, BlockState> out) {
    for (int dx = -r; dx <= r; dx++)
      for (int dz = -r; dz <= r; dz++)
        for (int dy = -1; dy <= 1; dy++)
          if (dx * dx + dz * dz + dy * dy * 3 <= r * r + 1
              && Math.floorMod(x + dx * 7 + z + dz * 11 + dy, 13) != 0)
            out.accept(new BlockPos(x + dx, y + dy, z + dz), leaf);
  }

  public static void fungus(
      int x, int g, int z, int height, int radius, BiConsumer<BlockPos, BlockState> sink) {
    BlockState stem = ((RotatedPillarBlock) VoidspaceContent.SPORE_STEM.get()).defaultBlockState();
    BlockState cap = ((Block) VoidspaceContent.SPORE_CAP.get()).defaultBlockState();

    for (int y = 1; y <= height; y++) {
      sink.accept(new BlockPos(x, g + y, z), stem);
    }

    for (int dx = -radius; dx <= radius; dx++) {
      for (int dz = -radius; dz <= radius; dz++) {
        double d = Math.hypot(dx, dz);
        if (!(d > radius + 0.4)) {
          int y = g + height + (d < radius - 0.6 ? 1 : 0);
          sink.accept(new BlockPos(x + dx, y, z + dz), cap);
          if (d > radius - 0.6 && (dx + dz) % 2 == 0) {
            sink.accept(new BlockPos(x + dx, y - 1, z + dz), cap);
          }
        }
      }
    }

    sink.accept(new BlockPos(x, g + height + 2, z), cap);
  }

  public static void spire(int x, int g, int z, int height, BiConsumer<BlockPos, BlockState> sink) {
    BlockState c = ((Block) VoidspaceContent.CRYSTAL_BLOCK.get()).defaultBlockState();

    for (int y = 1; y <= height; y++) {
      sink.accept(new BlockPos(x, g + y, z), c);
      if (y <= height / 3) {
        sink.accept(new BlockPos(x + 1, g + y, z), c);
        sink.accept(new BlockPos(x, g + y, z + 1), c);
      }

      if (y <= height / 5) {
        sink.accept(new BlockPos(x - 1, g + y, z), c);
        sink.accept(new BlockPos(x, g + y, z - 1), c);
      }
    }

    sink.accept(
        new BlockPos(x, g + height + 1, z),
        ((AlienPlantBlock) VoidspaceContent.CRYSTAL_CLUSTER.get()).defaultBlockState());
  }

  public static void boulder(
      int x, int g, int z, int r, BlockState stone, BiConsumer<BlockPos, BlockState> sink) {
    for (int dx = -r; dx <= r; dx++) {
      for (int dy = 0; dy <= r; dy++) {
        for (int dz = -r; dz <= r; dz++) {
          if (dx * dx + dy * dy * 1.4 + dz * dz <= r * r + 0.5) {
            sink.accept(new BlockPos(x + dx, g + dy + (dy == 0 ? 0 : 0), z + dz), stone);
          }
        }
      }
    }
  }

  public static void vent(int x, int g, int z, BiConsumer<BlockPos, BlockState> sink) {
    BlockState basalt = Blocks.BASALT.defaultBlockState();
    BlockState crust = ((Block) VoidspaceContent.EMBER_CRUST.get()).defaultBlockState();

    for (int dx = -2; dx <= 2; dx++) {
      for (int dz = -2; dz <= 2; dz++) {
        int d = Math.abs(dx) + Math.abs(dz);
        if (d <= 3) {
          int h = d <= 1 ? 2 : 1;

          for (int y = 1; y <= h; y++) {
            sink.accept(
                new BlockPos(x + dx, g + y, z + dz),
                d == 0
                    ? Blocks.MAGMA_BLOCK.defaultBlockState()
                    : (d == 1 && y == 2 ? crust : basalt));
          }
        }
      }
    }

    sink.accept(new BlockPos(x, g + 3, z), Blocks.AIR.defaultBlockState());
  }

  public static void column(
      int x, int g, int z, int height, BiConsumer<BlockPos, BlockState> sink) {
    for (int y = 1; y <= height; y++) {
      sink.accept(new BlockPos(x, g + y, z), Blocks.BASALT.defaultBlockState());
    }
  }

  private VoidspaceFlora() {}
}
