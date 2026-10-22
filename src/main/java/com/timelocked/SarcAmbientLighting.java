package com.timelocked;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.neoforged.neoforge.registries.DeferredBlock;

/**
 * Recessed service-light tiles. Real level-15 light close to the occupied floor, not glowing high
 * ceilings.
 */
final class SarcAmbientLighting {
  static final DeferredBlock<Block> LIGHT =
      Content.BLOCKS.registerSimpleBlock(
          "sarc_recessed_floor_light",
          BlockBehaviour.Properties.of().strength(3).lightLevel(s -> 15));

  static Map<BlockPos, BlockState> plan() {
    var result = new LinkedHashMap<BlockPos, BlockState>();
    for (int x = 6; x <= 150; x += 6)
      for (int z = 188; z <= 332; z += 6)
        result.put(new BlockPos(x, 1, z), LIGHT.get().defaultBlockState());
    for (int x = 58; x <= 76; x += 3)
      result.put(new BlockPos(x, 1, 329), LIGHT.get().defaultBlockState());
    return result;
  }

  static boolean eligible(LevelReader l, BlockPos at) {
    var s = l.getBlockState(at);
    if (l.getBlockEntity(at) != null
        || !(s.is(Content.FLOOR.get())
            || s.is(FacilityArchitecture.ROUTE.get())
            || s.is(Blocks.POLISHED_ANDESITE)
            || s.is(Blocks.LIGHT_GRAY_CONCRETE))) return false;
    return l.getBlockState(at.above()).isAir() && l.getBlockState(at.above(2)).isAir();
  }

  static void placeChunk(WorldGenLevel region, BlockPos o, int cx, int cz) {
    for (var e : plan().entrySet()) {
      BlockPos at = o.offset(e.getKey());
      if ((at.getX() & -16) == cx && (at.getZ() & -16) == cz && eligible(region, at))
        region.setBlock(at, e.getValue(), 2);
    }
  }

  static void installNew(ServerLevel l, BlockPos o) {
    for (var e : plan().entrySet()) {
      BlockPos at = o.offset(e.getKey());
      l.getChunkAt(at);
      if (eligible(l, at)) l.setBlock(at, e.getValue(), 3);
    }
  }

  static int retrofit(ServerLevel l, BlockPos o, boolean confirm) {
    var edits = plan();
    for (BlockPos p : edits.keySet()) if (!l.hasChunkAt(o.offset(p))) return -1;
    int changed = 0;
    for (var e : edits.entrySet()) {
      BlockPos at = o.offset(e.getKey());
      if (eligible(l, at)) {
        if (confirm) l.setBlock(at, e.getValue(), 3);
        changed++;
      }
    }
    return changed;
  }

  static void initialize() {}

  private SarcAmbientLighting() {}
}
