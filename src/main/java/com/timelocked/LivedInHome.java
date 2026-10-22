package com.timelocked;

import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.*;

/** Same furnished home in morning and evening; original bed, TV and break-in anchors retained. */
final class LivedInHome {
  static void build(ServerLevel l, BlockPos o, boolean morning) {
    IntroScenes.house(l, o);
    for (int x : new int[] {2, 10, 18, 22})
      for (int z : new int[] {3, 11, 17}) SccCampus.block(l, o, x, 5, z, Blocks.AIR);
    // Five shaded ceiling fittings rather than twelve exposed luminous cubes.
    for (int[] f : new int[][] {{5, 6}, {10, 15}, {18, 5}, {20, 17}}) {
      SccCampus.block(l, o, f[0], 6, f[1], Blocks.OCHRE_FROGLIGHT);
      l.setBlock(
          o.offset(f[0], 5, f[1]),
          Blocks.BIRCH_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.HALF, Half.TOP),
          2);
    }
    // Fitted kitchen: worktop, oven, sink, refrigerator and upper cupboards.
    SccCampus.box(l, o, 0, 1, 15, 6, 1, 18, Blocks.STRIPPED_BIRCH_WOOD);
    SccCampus.box(l, o, 0, 2, 15, 6, 2, 18, Blocks.AIR);
    for (int x = 0; x <= 6; x++) SccCampus.block(l, o, x, 2, 18, Blocks.SMOOTH_QUARTZ_SLAB);
    SccCampus.block(l, o, 2, 1, 18, Blocks.FURNACE);
    SccCampus.block(l, o, 2, 2, 18, Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE);
    SccCampus.block(l, o, 4, 2, 18, Blocks.WATER_CAULDRON);
    SccCampus.box(l, o, 0, 1, 14, 0, 3, 14, Blocks.QUARTZ_BLOCK);
    SccCampus.box(l, o, 0, 4, 17, 6, 4, 18, Blocks.BIRCH_SLAB);
    SccCampus.block(l, o, 6, 3, 18, Blocks.POTTED_FERN);
    SccCampus.block(l, o, 1, 3, 18, Blocks.FLOWER_POT);
    // Dining furniture, books and a warm suspended fixture.
    SccCampus.box(l, o, 9, 1, 15, 11, 1, 16, Blocks.SPRUCE_FENCE);
    SccCampus.box(l, o, 9, 2, 15, 11, 2, 16, Blocks.SPRUCE_PRESSURE_PLATE);
    for (int x : new int[] {9, 11}) {
      seat(l, o, x, 1, 14, Direction.SOUTH, Blocks.SPRUCE_STAIRS);
      seat(l, o, x, 1, 17, Direction.NORTH, Blocks.SPRUCE_STAIRS);
    }
    l.setBlock(
        o.offset(10, 4, 15),
        Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true),
        2);
    SccCampus.block(l, o, 10, 5, 15, Blocks.CHAIN);
    // Keep the walk from front door to bedroom and kitchen unobstructed.
    SccCampus.box(l, o, 6, 1, 10, 10, 2, 11, Blocks.AIR);
    for (int x = 6; x <= 9; x++) seat(l, o, x, 1, 11, Direction.NORTH, Blocks.DARK_OAK_STAIRS);
    SccCampus.box(l, o, 6, 1, 12, 9, 1, 12, Blocks.GRAY_WOOL);
    SccCampus.box(l, o, 1, 1, 8, 1, 3, 11, Blocks.BOOKSHELF);
    SccCampus.block(l, o, 2, 1, 12, Blocks.BARREL);
    SccCampus.block(l, o, 2, 2, 12, Blocks.LANTERN);
    SccCampus.block(l, o, 3, 1, 12, Blocks.POTTED_FERN);
    SccCampus.box(l, o, 4, 1, 6, 6, 1, 6, Blocks.BROWN_CARPET);
    // Entry shoe bench and coat rack sit outside the main centre aisle.
    SccCampus.box(l, o, 2, 1, 0, 5, 1, 0, Blocks.SPRUCE_SLAB);
    SccCampus.block(l, o, 7, 1, 0, Blocks.SPRUCE_FENCE);
    SccCampus.block(l, o, 7, 2, 0, Blocks.SPRUCE_FENCE);
    SccCampus.block(l, o, 7, 3, 0, Blocks.SPRUCE_FENCE);
    IntroScenes.sign(l, o.offset(2, 2, 0), "Keys / shoes", "See you tonight", "Club: SCC 121", "");
    // Bedroom study nook, storage and personal projects, not just a bed in an empty box.
    SccCampus.box(l, o, 16, 1, 3, 18, 1, 3, Blocks.BIRCH_SLAB);
    SccCampus.block(l, o, 16, 2, 3, Blocks.BOOKSHELF);
    l.setBlock(o.offset(17, 2, 3), MonitorContent.stand(Direction.SOUTH), 2);
    if (l.getBlockEntity(o.offset(17, 2, 3)) instanceof MonitorContent.MonitorEntity m)
      m.setLines("TIMELOCKED", "CLUB PROJECT", "SCC 121", "SAVE / BUILD / PLAY");
    seat(l, o, 17, 1, 5, Direction.NORTH, Blocks.SPRUCE_STAIRS);
    SccCampus.box(l, o, 22, 1, 3, 23, 3, 6, Blocks.BARREL);
    SccCampus.block(l, o, 23, 4, 5, Blocks.POTTED_CACTUS);
    // Bathroom partition, shower, basin and toilet. Retains the morning schedule at 22,2,14.
    SccCampus.box(l, o, 15, 1, 15, 23, 5, 15, Blocks.WHITE_TERRACOTTA);
    SccCampus.box(l, o, 18, 1, 15, 18, 2, 15, Blocks.AIR);
    CampusNeighborhood.door(
        l, o.offset(18, 1, 15), Direction.NORTH, DoorHingeSide.LEFT, Blocks.BIRCH_DOOR);
    SccCampus.box(l, o, 14, 1, 9, 14, 5, 9, Blocks.WHITE_TERRACOTTA);
    CampusNeighborhood.door(
        l, o.offset(14, 1, 8), Direction.EAST, DoorHingeSide.LEFT, Blocks.BIRCH_DOOR);
    SccCampus.box(l, o, 15, 0, 16, 23, 0, 19, Blocks.SMOOTH_QUARTZ);
    SccCampus.box(l, o, 22, 1, 17, 22, 3, 19, Blocks.GLASS_PANE);
    SccCampus.block(l, o, 23, 1, 18, Blocks.STONE_PRESSURE_PLATE);
    SccCampus.block(l, o, 15, 1, 18, Blocks.QUARTZ_STAIRS);
    SccCampus.block(l, o, 15, 2, 19, Blocks.QUARTZ_BLOCK);
    SccCampus.block(l, o, 19, 1, 19, Blocks.WATER_CAULDRON);
    SccCampus.block(l, o, 19, 3, 19, Blocks.LIGHT_BLUE_STAINED_GLASS);
    SccCampus.block(l, o, 22, 1, 14, Blocks.OAK_PLANKS);
    SccCampus.block(l, o, 22, 2, 14, Blocks.LECTERN);
    if (morning) {
      IntroScenes.sign(
          l,
          o.offset(21, 1, 13),
          "Morning schedule",
          "Cross Park St",
          "Walk along College Rd",
          "SCC 123 then SCC 121");
      for (int x = 3; x <= 8; x++)
        for (int y = 2; y <= 4; y++)
          if (l.getBlockEntity(o.offset(x, y, 3)) instanceof MonitorContent.MonitorEntity m)
            m.setLines("LOCAL BROADCAST", "MORNING", "GLEN ELLYN", "COLLEGE OF DUPAGE");
    }
  }

  static void seat(ServerLevel l, BlockPos o, int x, int y, int z, Direction facing, Block stairs) {
    l.setBlock(
        o.offset(x, y, z), stairs.defaultBlockState().setValue(StairBlock.FACING, facing), 2);
  }

  private LivedInHome() {}
}
