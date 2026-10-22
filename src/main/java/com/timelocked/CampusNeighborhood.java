package com.timelocked;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.*;

/** New scenes only: raised SCC, street approach and a home with the same night-time furnishings. */
final class CampusNeighborhood {
  static final String REVISION = "CampusSceneV27";

  static BlockPos home(int slot) {
    return new BlockPos((slot % 4096) * 512, 80, -2000000 - (slot / 4096) * 512);
  }

  static BlockPos school(BlockPos o) {
    return o.offset(180, 4, -64);
  }

  static void build(ServerLevel l, BlockPos o) {
    SccCampus.box(l, o, -12, -1, -220, 355, -1, 85, Blocks.STONE);
    SccCampus.box(l, o, -12, 0, -220, 355, 0, 85, Blocks.GRASS_BLOCK);
    LivedInHome.build(l, o, true);
    // SCC-only approach: east along College Road, then north at Lambert Road into the SCC.
    // The rest of the campus remains intentionally unmodelled.
    SccCampus.box(l, o, -12, 0, -18, 224, 0, -9, Blocks.GRAY_CONCRETE);
    SccCampus.box(l, o, 204, 0, -215, 216, 0, 80, Blocks.GRAY_CONCRETE);
    SccCampus.box(l, o, 10, 0, -7, 224, 0, -3, Blocks.SMOOTH_STONE);
    SccCampus.box(l, o, 220, 0, -145, 224, 0, 12, Blocks.SMOOTH_STONE);
    for (int x = -6; x < 218; x += 9)
      SccCampus.box(l, o, x, 0, -14, x + 3, 0, -14, Blocks.YELLOW_CONCRETE);
    for (int z = -204; z < 75; z += 9) {
      SccCampus.box(l, o, 210, 0, z, 210, 0, z + 3, Blocks.YELLOW_CONCRETE);
    }
    // Mark both legs of the College/Lambert pedestrian crossing.
    for (int x = 204; x <= 216; x += 2)
      SccCampus.box(l, o, x, 0, -7, x, 0, -3, Blocks.WHITE_CONCRETE);
    for (int z = -18; z <= -9; z += 2)
      SccCampus.box(l, o, 220, 0, z, 224, 0, z, Blocks.WHITE_CONCRETE);
    IntroScenes.sign(
        l,
        o.offset(20, 1, -7),
        "College Rd",
        "SCC: continue east",
        "Lambert Rd: ahead",
        "Use marked crossing");
    IntroScenes.sign(
        l, o.offset(198, 1, -7), "Lambert Rd", "Cross east", "Then turn north", "SCC / Exit 1 ↑");
    IntroScenes.sign(
        l,
        o.offset(222, 1, -20),
        "College Rd / Lambert",
        "SCC entrance: north",
        "Follow east sidewalk",
        "↑");
    // A yard, neighbouring homes, planting and individual streetlights rather than luminous paving.
    for (int z : new int[] {32, 58}) {
      IntroCampus.room(l, o, 5, z, 25, z + 15);
      SccCampus.box(l, o, 5, 5, z, 25, 5, z + 15, Blocks.DARK_OAK_SLAB);
      SccCampus.box(l, o, 26, 1, z, 28, 1, z + 14, Blocks.OAK_LEAVES);
    }
    for (int x : new int[] {80, 125, 170}) {
      SccCampus.box(l, o, x, 1, 4, x, 4, 4, Blocks.OAK_LOG);
      SccCampus.box(l, o, x - 2, 4, 2, x + 2, 6, 6, Blocks.OAK_LEAVES);
    }
    for (int z : new int[] {-25, -60, -95, -130}) {
      SccCampus.box(l, o, 218, 1, z, 218, 4, z, Blocks.ANDESITE_WALL);
      SccCampus.block(l, o, 218, 5, z, Blocks.LANTERN);
    }
    // SCC sits four full blocks above the residential pavement.
    SccCampus.box(l, o, 234, 1, -214, 341, 4, -51, Blocks.STONE);
    SccCampus.box(l, o, 234, 4, -214, 341, 4, -51, Blocks.SMOOTH_STONE);
    SccCampus.school(l, school(o), true);
    fitOut(l, school(o));
    // The west approach rises SOUTH toward Exit 1, with eight half-block grade changes.
    SccCampus.box(l, o, 225, 0, -145, 233, 0, -139, Blocks.SMOOTH_STONE);
    for (int z = -140; z <= -96; z++) {
      int half = Math.min(8, (z + 140) / 5);
      for (int x = 228; x <= 232; x++) {
        SccCampus.box(l, o, x, 0, z, x, half / 2, z, Blocks.SMOOTH_STONE);
        if (half % 2 == 1)
          l.setBlock(o.offset(x, half / 2 + 1, z), Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 2);
      }
      for (int x : new int[] {227, 233}) {
        SccCampus.box(l, o, x, 0, z, x, half / 2, z, Blocks.SMOOTH_STONE);
        SccCampus.block(l, o, x, 1 + (half + 1) / 2, z, Blocks.IRON_BARS);
      }
    }
    SccCampus.box(l, o, 228, 0, -95, 246, 4, -88, Blocks.SMOOTH_STONE);
    SccCampus.box(l, o, 234, 5, -91, 246, 7, -88, Blocks.AIR);
    IntroScenes.sign(
        l,
        o.offset(225, 1, -143),
        "SCC side approach",
        "Uphill to Exit 1",
        "Club: SCC 121",
        "College of DuPage");
    // Crater's full depth is solid before the real satellite impact builder is invoked.
    SccCampus.box(l, o, 305, -2, -87, 335, 3, -57, Blocks.STONE);
    SccCampus.box(l, o, 313, 4, -77, 327, 4, -66, Blocks.COARSE_DIRT);
    SccCampus.box(l, o, 247, 4, -86, 322, 4, -82, Blocks.SMOOTH_STONE);
    SccCampus.box(l, o, 318, 4, -82, 322, 4, -73, Blocks.GRAVEL);
    IntroScenes.sign(
        l,
        o.offset(304, 5, -82),
        "Campus boundary",
        "Fictional crash site",
        "Follow the smoke",
        "Return home afterwards");
  }

  static void fitOut(ServerLevel l, BlockPos o) {
    // Recessed warm slots in classrooms. One or two authored rows per room, not a ceiling grid.
    int[][] rooms = {
      {80, -115},
      {111, -116},
      {145, -124},
      {145, -98},
      {145, -75},
      {145, -49},
      {145, -34},
      {110, -80},
      {110, -59},
      {109, -112},
      {83, -40},
      {106, -40},
      {82, -80}
    };
    for (int[] room : rooms)
      for (int dx : new int[] {-3, 3}) {
        for (int dz = -2; dz <= 2; dz++) {
          SccCampus.block(l, o, room[0] + dx, 8, room[1] + dz, Blocks.OCHRE_FROGLIGHT);
          l.setBlock(
              o.offset(room[0] + dx, 7, room[1] + dz),
              Blocks.IRON_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.HALF, Half.TOP),
              2);
        }
      }
    for (int[] at :
        new int[][] {{88, -139}, {78, -120}, {82, -90}, {114, -92}, {121, -67}, {121, -45}}) {
      SccCampus.block(l, o, at[0], 8, at[1], Blocks.OCHRE_FROGLIGHT);
      l.setBlock(
          o.offset(at[0], 7, at[1]),
          Blocks.IRON_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.HALF, Half.TOP),
          2);
    }
    // White acoustic baffles and suspended slender light rods in the tall glazed lobby.
    for (int z = -78; z <= -33; z += 9) {
      SccCampus.box(l, o, 58, 11, z, 68, 12, z, Blocks.WHITE_CONCRETE);
      for (int x : new int[] {60, 66}) {
        SccCampus.block(l, o, x, 12, z + 3, Blocks.CHAIN);
        SccCampus.block(l, o, x, 11, z + 3, Blocks.CHAIN);
        SccCampus.block(l, o, x, 10, z + 3, Blocks.END_ROD);
      }
    }
    // Orange wall and red fascia echo the supplied lobby image without changing PDF room
    // footprints.
    for (int z = -55; z <= -33; z++)
      for (int x = 70; x <= 78; x++)
        for (int y = 2; y <= 5; y++) {
          BlockPos at = o.offset(x, y, z);
          if (l.getBlockState(at).is(Blocks.WHITE_CONCRETE))
            l.setBlock(
                at,
                (y == 5 ? Blocks.RED_TERRACOTTA : Blocks.ORANGE_TERRACOTTA).defaultBlockState(),
                2);
        }
    // Seating nooks are along the glazing, keeping the central x64 circulation strip clear.
    for (int z : new int[] {-74, -56, -38}) {
      SccCampus.box(l, o, 58, 0, z - 2, 62, 0, z + 2, Blocks.GRAY_CONCRETE);
      SccCampus.block(l, o, 60, 1, z, Blocks.DARK_OAK_FENCE);
      SccCampus.block(l, o, 60, 2, z, Blocks.OAK_PRESSURE_PLATE);
      LivedInHome.seat(l, o, 58, 1, z, Direction.EAST, Blocks.CRIMSON_STAIRS);
      LivedInHome.seat(l, o, 62, 1, z, Direction.WEST, Blocks.CRIMSON_STAIRS);
      SccCampus.block(l, o, 58, 1, z + 2, Blocks.POTTED_FERN);
    }
    // Six entrance leaves and glass transom. Functional paired leaves, not an air hole.
    SccCampus.box(l, o, 61, 3, -28, 66, 3, -28, Blocks.GLASS);
    for (int x = 61; x <= 66; x++)
      door(
          l,
          o.offset(x, 1, -28),
          Direction.SOUTH,
          (x % 2 == 1 ? DoorHingeSide.LEFT : DoorHingeSide.RIGHT),
          CampusDoors.GLASS.get());
    // Actual gaps on the PDF: SCC123 north edge and SCC121 north edge.
    for (int x : new int[] {89, 92}) SccCampus.box(l, o, x, 1, -56, x, 3, -56, Blocks.GLASS);
    SccCampus.box(l, o, 90, 3, -56, 91, 3, -56, Blocks.GLASS);
    pair(l, o, 90, 1, -56, Direction.NORTH);
    SccCampus.box(l, o, 107, 1, -56, 112, 3, -56, Blocks.GLASS);
    pair(l, o, 109, 1, -56, Direction.NORTH);
  }

  static void pair(ServerLevel l, BlockPos o, int x, int y, int z, Direction facing) {
    door(
        l,
        o.offset(x, y, z),
        facing,
        facing == Direction.NORTH ? DoorHingeSide.RIGHT : DoorHingeSide.LEFT,
        CampusDoors.GLASS.get());
    door(
        l,
        o.offset(x + 1, y, z),
        facing,
        facing == Direction.NORTH ? DoorHingeSide.LEFT : DoorHingeSide.RIGHT,
        CampusDoors.GLASS.get());
  }

  static void door(ServerLevel l, BlockPos at, Direction facing, DoorHingeSide hinge, Block b) {
    for (DoubleBlockHalf half : DoubleBlockHalf.values())
      l.setBlock(
          half == DoubleBlockHalf.LOWER ? at : at.above(),
          b.defaultBlockState()
              .setValue(DoorBlock.FACING, facing)
              .setValue(DoorBlock.HINGE, hinge)
              .setValue(DoorBlock.HALF, half),
          2);
  }

  static boolean useDoor(ServerPlayer p, CompoundTag t, BlockPos at) {
    boolean day = CampusOpening.active(t) && p.level().dimension().equals(IntroSequence.CAMPUS_DIM);
    boolean night =
        t.getInt("Stage") == IntroSequence.BEDTIME
            && p.level().dimension().equals(IntroSequence.HOME_DIM);
    if (!day && !night) return false;
    BlockPos o = IntroSequence.home(t);
    if (day && !contains(o, at)) return false;
    // Night only permits bedroom/bathroom doors: the front entrance remains part of the break-in.
    int x = at.getX() - o.getX(), z = at.getZ() - o.getZ();
    if (night && !((x == 14 && z == 8) || (x == 18 && z == 15))) return false;
    var state = p.serverLevel().getBlockState(at);
    if (!(state.getBlock() instanceof DoorBlock block)
        || p.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(at)) > 36) return false;
    BlockPos lower = state.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER ? at.below() : at;
    state = p.serverLevel().getBlockState(lower);
    boolean open = !state.getValue(DoorBlock.OPEN);
    block.setOpen(p, p.serverLevel(), state, lower, open);
    Direction d =
        state.getValue(DoorBlock.HINGE) == DoorHingeSide.LEFT
            ? state.getValue(DoorBlock.FACING).getCounterClockWise()
            : state.getValue(DoorBlock.FACING).getClockWise();
    // Keep legacy pair adjacency first; a correctly outward-hinged bank uses the other side.
    // Never toggle a lone leaf twice or chain through multiple adjacent pairs.
    for (Direction side : new Direction[] {d, d.getOpposite()}) {
      BlockPos other = lower.relative(side);
      var b = p.serverLevel().getBlockState(other);
      if (b.is(block)
          && b.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER
          && b.getValue(DoorBlock.FACING) == state.getValue(DoorBlock.FACING)
          && b.getValue(DoorBlock.HINGE) != state.getValue(DoorBlock.HINGE)) {
        block.setOpen(p, p.serverLevel(), b, other, open);
        break;
      }
    }
    return true;
  }

  static boolean contains(BlockPos o, BlockPos p) {
    int x = p.getX() - o.getX(), z = p.getZ() - o.getZ(), y = p.getY() - o.getY();
    return x >= -12 && x <= 355 && z >= -220 && z <= 85 && y >= -3 && y <= 23;
  }

  static void route(ServerPlayer p, CompoundTag t, BlockPos o) {
    int x = p.blockPosition().getX() - o.getX(), z = p.blockPosition().getZ() - o.getZ();
    int phase = t.getInt("CampusStreetRoute"), next = phase;
    if (phase == 0 && x >= 10 && x <= 60 && z >= -8 && z <= -2) next = 1;
    if (phase == 1 && x >= 217 && x <= 225 && z >= -8 && z <= -2) next = 2;
    if (phase == 2 && x >= 220 && x <= 225 && z >= -135 && z <= -20) next = 3;
    if (next != phase) {
      t.putInt("CampusStreetRoute", next);
      IntroState.get(p.server).setDirty();
    }
  }

  static boolean arrived(BlockPos o, BlockPos p) {
    int x = p.getX() - o.getX(), z = p.getZ() - o.getZ();
    return x >= 237 && x <= 249 && z <= -93 && z >= -110 && p.getY() >= o.getY() + 4;
  }

  private CampusNeighborhood() {}
}
