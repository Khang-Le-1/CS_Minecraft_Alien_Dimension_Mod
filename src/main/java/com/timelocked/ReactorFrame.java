package com.timelocked;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public final class ReactorFrame {
  public static final ReactorFrame.Shape FACILITY = new ReactorFrame.Shape(3, 11, 0);
  public static final int[] PLANT_H = new int[] {8, 6, 7, 7};
  public static final int[] INDUSTRIAL_H = PowerStationGeometry.PLAN.industrial_reactor().heights();
  public static final int INDUSTRIAL_R = PowerStationGeometry.PLAN.industrial_reactor().radius();
  private static final Set<String> LEGACY =
      Set.of(
          "sarc_containment_casing",
          "titanium_panel",
          "reactor_vessel_glow",
          "space_viewport",
          "iron_block",
          "chain");

  public static boolean isCore(BlockState s) {
    return s.getBlock() instanceof ReactorCoreBlock || s.getBlock() instanceof PlantCoreBlock;
  }

  public static ReactorFrame.Shape shape(BlockGetter l, BlockPos c, BlockState s) {
    if (s.getBlock() instanceof PlantCoreBlock) {
      int base = l.getBlockEntity(c) instanceof PlantCoreBlock.Core core ? core.base : 0;
      boolean industrial =
          l.getBlockEntity(c) instanceof PlantCoreBlock.Core core && core.industrial;
      return new ReactorFrame.Shape(
          industrial ? INDUSTRIAL_R : 2,
          (industrial ? INDUSTRIAL_H : PLANT_H)[s.getValue(PlantCoreBlock.KIND)],
          base);
    } else {
      return FACILITY;
    }
  }

  static boolean legacy(BlockState s) {
    String path = BuiltInRegistries.BLOCK.getKey(s.getBlock()).getPath();
    return LEGACY.contains(path) || path.endsWith("_stained_glass");
  }

  public static void build(Level l, BlockPos c, ReactorFrame.Shape sh) {
    BlockState cas = ((ReactorCasingBlock) SarcContent.CASING.get()).defaultBlockState();

    for (BlockPos q : sh.cells(c)) {
      if (!l.hasChunkAt(q)) continue;
      BlockState o = l.getBlockState(q);
      if (!o.is(cas.getBlock()) && (o.canBeReplaced() || legacy(o))) {
        l.setBlock(q, cas, 3);
      }
    }
  }

  public static void clear(Level l, BlockPos c, ReactorFrame.Shape sh) {
    for (BlockPos q : sh.cells(c)) {
      if (l.hasChunkAt(q) && l.getBlockState(q).is((Block) SarcContent.CASING.get())) {
        l.setBlock(q, Blocks.AIR.defaultBlockState(), 3);
      }
    }
  }

  public static boolean intact(Level l, BlockPos c) {
    return l.getBlockState(c.above()).is((Block) SarcContent.CASING.get());
  }

  public static int legacyPlantBase(Level l, BlockPos c) {
    if (l.getBlockState(c.below()).is(Blocks.IRON_BLOCK)
        && l.getBlockState(c.below(2)).is(Blocks.IRON_BLOCK)) {
      for (BlockPos q : BlockPos.betweenClosed(c.offset(-2, -2, -2), c.offset(2, 8, 2))) {
        if (!q.equals(c) && legacy(l.getBlockState(q))) {
          l.setBlock(q, Blocks.AIR.defaultBlockState(), 3);
        }
      }

      return 2;
    } else {
      return 0;
    }
  }

  public static BlockPos findCore(BlockGetter l, BlockPos q) {
    MutableBlockPos m = new MutableBlockPos();

    for (int dy = -18; dy <= 3; dy++) {
      for (int dx = -5; dx <= 5; dx++) {
        for (int dz = -5; dz <= 5; dz++) {
          m.set(q.getX() + dx, q.getY() + dy, q.getZ() + dz);
          if (l instanceof Level level && !level.hasChunkAt(m)) continue;
          BlockState s = l.getBlockState(m);
          if (isCore(s)) {
            BlockPos c = m.immutable();
            if (shape(l, c, s).contains(c, q)) {
              return c;
            }
          }
        }
      }
    }

    return null;
  }

  private ReactorFrame() {}

  public record Shape(int r, int h, int base) {
    public int bottom(BlockPos c) {
      return c.getY() - this.base;
    }

    public boolean contains(BlockPos c, BlockPos q) {
      int dx = q.getX() - c.getX();
      int dz = q.getZ() - c.getZ();
      int dy = q.getY() - this.bottom(c);
      return dy >= 0 && dy < this.h && dx * dx + dz * dz <= (this.r + 0.5) * (this.r + 0.5);
    }

    public AABB box(BlockPos c) {
      return new AABB(
          c.getX() - this.r,
          this.bottom(c),
          c.getZ() - this.r,
          c.getX() + this.r + 1,
          this.bottom(c) + this.h,
          c.getZ() + this.r + 1);
    }

    public List<BlockPos> cells(BlockPos c) {
      List<BlockPos> out = new ArrayList<>();

      for (int y = 0; y < this.h; y++) {
        for (int dx = -this.r; dx <= this.r; dx++) {
          for (int dz = -this.r; dz <= this.r; dz++) {
            BlockPos q = new BlockPos(c.getX() + dx, this.bottom(c) + y, c.getZ() + dz);
            if (!q.equals(c) && this.contains(c, q)) {
              out.add(q);
            }
          }
        }
      }

      return out;
    }

    public List<BlockPos> ports(BlockPos c) {
      List<BlockPos> out = new ArrayList<>();
      int y = this.bottom(c);
      int R = this.r + 1;

      for (int dx = -R; dx <= R; dx++) {
        for (int dz = -R; dz <= R; dz++) {
          BlockPos q = new BlockPos(c.getX() + dx, y, c.getZ() + dz);
          if (Math.max(Math.abs(dx), Math.abs(dz)) == R
              || !this.contains(c, q) && Math.max(Math.abs(dx), Math.abs(dz)) < R) {
            out.add(q);
          }
        }
      }

      for (Direction d : Direction.values()) {
        BlockPos q = c.relative(d);
        if (!this.contains(c, q)) {
          out.add(q);
        }
      }

      return out;
    }
  }
}
