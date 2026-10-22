package com.timelocked;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Compact, fictionalized CoD Glen Ellyn teaching set, not a survey-accurate campus map. */
public final class IntroCampus {
  static final int SLOT_WIDTH = 192;
  static final int SLOTS_PER_ROW = 4096;
  static final int BASE_HEIGHT = 80;
  static final int MIN_X = -8, MAX_X = 144, MIN_Z = -12, MAX_Z = 92;
  static final BlockPos SCHEDULE = new BlockPos(22, 2, 14);
  static final BlockPos CLASS_DESK = new BlockPos(64, 1, 19);
  static final BlockPos CLUB_DESK = new BlockPos(64, 1, 55);
  static final BlockPos WRECK = new BlockPos(124, 2, 30);

  static BlockPos home(int slot) {
    return new BlockPos(
        (slot % SLOTS_PER_ROW) * SLOT_WIDTH, BASE_HEIGHT, (slot / SLOTS_PER_ROW) * SLOT_WIDTH);
  }

  static void block(ServerLevel level, BlockPos o, int x, int y, int z, Block block) {
    level.setBlock(o.offset(x, y, z), block.defaultBlockState(), 2);
  }

  static void box(
      ServerLevel l, BlockPos o, int x0, int y0, int z0, int x1, int y1, int z1, Block b) {
    for (int x = x0; x <= x1; x++)
      for (int y = y0; y <= y1; y++) for (int z = z0; z <= z1; z++) block(l, o, x, y, z, b);
  }

  static void room(ServerLevel l, BlockPos o, int x0, int z0, int x1, int z1) {
    box(l, o, x0, 0, z0, x1, 0, z1, Blocks.SMOOTH_STONE);
    box(l, o, x0, 1, z0, x1, 6, z1, Blocks.AIR);
    box(l, o, x0, 1, z0, x1, 5, z0, Blocks.BRICKS);
    box(l, o, x0, 1, z1, x1, 5, z1, Blocks.BRICKS);
    box(l, o, x0, 1, z0, x0, 5, z1, Blocks.BRICKS);
    box(l, o, x1, 1, z0, x1, 5, z1, Blocks.BRICKS);
    box(l, o, x0, 6, z0, x1, 6, z1, Blocks.SMOOTH_QUARTZ);
    // Broad east-facing windows show the satellite trail from both teaching rooms.
    box(l, o, x1, 2, z0 + 2, x1, 4, z1 - 2, Blocks.GLASS);
    box(l, o, x0, 1, z0 + 3, x0, 3, z0 + 6, Blocks.AIR);
    for (int x = x0 + 4; x < x1; x += 7)
      for (int z = z0 + 4; z < z1; z += 7) block(l, o, x, 5, z, Blocks.SEA_LANTERN);
  }

  static void build(ServerLevel l, BlockPos o) {
    // The platform is confined to this player's empty private dimension slot.
    box(l, o, MIN_X, -1, MIN_Z, MAX_X, -1, MAX_Z, Blocks.STONE);
    box(l, o, MIN_X, 0, MIN_Z, MAX_X, 0, MAX_Z, Blocks.GRASS_BLOCK);
    // Walkable neighbourhood, campus approach and off-campus service path.
    box(l, o, 8, 0, -7, 106, 0, -3, Blocks.SMOOTH_STONE);
    box(l, o, 34, 0, -7, 40, 0, 74, Blocks.SMOOTH_STONE);
    box(l, o, 36, 0, 10, 56, 0, 14, Blocks.SMOOTH_STONE);
    box(l, o, 36, 0, 46, 56, 0, 50, Blocks.SMOOTH_STONE);
    box(l, o, 94, 0, -7, 99, 0, 30, Blocks.SMOOTH_STONE);
    box(l, o, 96, 0, 27, 124, 0, 33, Blocks.GRAVEL);
    IntroScenes.house(l, o);
    for (int x = 3; x <= 8; x++)
      for (int y = 2; y <= 4; y++)
        if (l.getBlockEntity(o.offset(x, y, 3)) instanceof MonitorContent.MonitorEntity monitor)
          monitor.setLines("LOCAL BROADCAST", "MORNING", "GLEN ELLYN", "COLLEGE OF DUPAGE");
    // Open the home entrance for the playable daytime walk; night retains the original door.
    box(l, o, 10, 1, -2, 12, 3, -2, Blocks.AIR);
    block(l, o, SCHEDULE.getX(), SCHEDULE.getY() - 1, SCHEDULE.getZ(), Blocks.OAK_PLANKS);
    block(l, o, SCHEDULE.getX(), SCHEDULE.getY(), SCHEDULE.getZ(), Blocks.LECTERN);
    IntroScenes.sign(
        l,
        o.offset(21, 1, 15),
        "Morning schedule",
        "Read the lectern",
        "CoD - Glen Ellyn",
        "Class, then club");
    IntroScenes.sign(
        l, o.offset(10, 1, 4), "Local broadcast", "Morning bulletin", "Right-click TV", "Optional");
    // Two small neighbouring houses make the walk an inhabited street, not a teleport menu.
    room(l, o, 3, 31, 24, 45);
    room(l, o, 3, 54, 24, 68);
    IntroScenes.sign(
        l,
        o.offset(29, 1, -5),
        "Neighbourhood",
        "Campus ahead",
        "Follow the path",
        "CoD / Glen Ellyn");
    room(l, o, 56, 6, 88, 29);
    room(l, o, 56, 42, 88, 65);
    room(l, o, 51, 75, 84, 87);
    IntroScenes.sign(
        l, o.offset(42, 1, 9), "College of DuPage", "Glen Ellyn", "BIC: class", "SCC: club");
    IntroScenes.sign(
        l,
        o.offset(57, 1, 8),
        "Berg Instructional",
        "Center / BIC",
        "Fictional class",
        "Read front desk");
    IntroScenes.sign(
        l,
        o.offset(57, 1, 44),
        "Seaton Computing",
        "Center / SCC",
        "Club meeting",
        "Read club table");
    IntroScenes.sign(
        l,
        o.offset(52, 1, 78),
        "Student Resource",
        "Center / SRC",
        "Campus landmark",
        "Game adaptation");
    block(l, o, 64, 1, 19, Blocks.LECTERN);
    block(l, o, 64, 1, 55, Blocks.LECTERN);
    box(l, o, 76, 2, 8, 84, 3, 8, Blocks.GREEN_CONCRETE);
    for (int x = 62; x <= 80; x += 6)
      for (int z = 12; z <= 24; z += 6) {
        block(l, o, x, 1, z, Blocks.OAK_SLAB);
        block(l, o, x, 1, z + 1, Blocks.OAK_STAIRS);
      }
    box(l, o, 70, 1, 52, 78, 1, 56, Blocks.OAK_PLANKS);
    for (int x = 70; x <= 78; x += 4) {
      block(l, o, x, 2, 53, Blocks.BLACK_CONCRETE);
      block(l, o, x, 2, 55, Blocks.STONE_PRESSURE_PLATE);
    }
    IntroScenes.sign(
        l,
        o.offset(65, 1, 57),
        "Club project",
        "TIMELOCKED",
        "Read club lectern",
        "Meet, build, play");
    // Optional conversation stations are named fictional roles, never real students/staff.
    block(l, o, 61, 1, 22, Blocks.LECTERN);
    IntroScenes.sign(
        l,
        o.offset(60, 1, 23),
        "Classmate",
        "Optional chat",
        "Read lectern",
        "Fictional character");
    block(l, o, 80, 1, 58, Blocks.LECTERN);
    IntroScenes.sign(
        l,
        o.offset(79, 1, 59),
        "Club member",
        "Optional chat",
        "Read lectern",
        "Fictional character");
    IntroScenes.sign(
        l,
        o.offset(99, 1, 25),
        "Campus boundary",
        "Service path",
        "Fictional crash site",
        "Not a real route");
    // A supported clear approach to the later wreck; no dropped quest item or explosion damage.
    box(l, o, 119, 0, 25, 130, 0, 36, Blocks.COARSE_DIRT);
    for (int x = 29; x <= 137; x += 18) {
      block(l, o, x, 1, -8, Blocks.COBBLESTONE_WALL);
      block(l, o, x, 2, -8, Blocks.LANTERN);
    }
  }

  static void impact(ServerLevel l, BlockPos o) {
    box(l, o, 122, 1, 28, 126, 1, 32, Blocks.IRON_BLOCK);
    box(l, o, 117, 1, 29, 121, 1, 31, Blocks.BLUE_CONCRETE);
    box(l, o, 127, 1, 29, 131, 1, 31, Blocks.BLUE_CONCRETE);
    block(l, o, 124, 1, 30, Blocks.REDSTONE_LAMP);
    block(l, o, 124, 2, 30, Blocks.LODESTONE);
    IntroScenes.sign(
        l,
        o.offset(121, 1, 34),
        "Satellite wreckage",
        "Inspect the core",
        "Right-click metal",
        "Keep your distance");
  }

  static boolean contains(BlockPos o, BlockPos p) {
    int x = p.getX() - o.getX(), z = p.getZ() - o.getZ();
    return x >= MIN_X
        && x <= MAX_X
        && z >= MIN_Z
        && z <= MAX_Z
        && p.getY() >= o.getY() + 1
        && p.getY() <= o.getY() + 9;
  }

  static boolean inClassroom(BlockPos o, BlockPos p) {
    int x = p.getX() - o.getX(), z = p.getZ() - o.getZ();
    return x > 56 && x < 88 && z > 6 && z < 29;
  }

  private IntroCampus() {}
}
