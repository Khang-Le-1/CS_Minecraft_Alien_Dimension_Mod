package com.timelocked;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.*;

/** v28 only: composed home, teaching rooms, populated streets and SCC's front parking lot. */
final class CampusRebuild {
  static final String REVISION = "CampusSceneV28";

  static boolean active(CompoundTag t) {
    return t != null && t.getBoolean(REVISION);
  }

  static BlockPos home(int slot) {
    return new BlockPos((slot % 4096) * 512, 80, -3000000 - (slot / 4096) * 512);
  }

  static void build(ServerLevel l, BlockPos o) {
    CampusNeighborhood.build(l, o);
    house(l, o, true);
    classroom(l, CampusNeighborhood.school(o));
    parking(l, o);
    neighbourhood(l, o);
  }

  static void box(ServerLevel l, BlockPos o, int x, int y, int z, int xx, int yy, int zz, Block b) {
    SccCampus.box(l, o, x, y, z, xx, yy, zz, b);
  }

  static void block(ServerLevel l, BlockPos o, int x, int y, int z, Block b) {
    SccCampus.block(l, o, x, y, z, b);
  }

  static void house(ServerLevel l, BlockPos o, boolean morning) {
    LivedInHome.build(l, o, morning);
    // Lower living-room ceiling and a pitched main roof with timber gables, deep eaves and chimney.
    box(l, o, -3, 6, -3, 25, 6, 21, Blocks.SPRUCE_PLANKS);
    roof(l, o, -4, 26, -4, 22, 7, Blocks.DEEPSLATE_TILE_STAIRS);
    for (int x = -2; x <= 24; x++) {
      int h = 6 + Math.min(x + 4, 26 - x) / 2;
      box(l, o, x, 7, -2, x, h, -2, Blocks.WHITE_TERRACOTTA);
      box(l, o, x, 7, 20, x, h, 20, Blocks.WHITE_TERRACOTTA);
    }
    box(l, o, 2, 5, 15, 3, 14, 16, Blocks.BRICKS);
    box(l, o, 1, 15, 14, 4, 15, 17, Blocks.BRICK_SLAB);
    // Recessed front porch with a separate lower roof and brick plinth: a recognisable house
    // facade.
    box(l, o, 4, 0, -8, 17, 0, -3, Blocks.STONE_BRICKS);
    for (int x : new int[] {4, 17})
      for (int z : new int[] {-7, -3}) box(l, o, x, 1, z, x, 4, z, Blocks.STRIPPED_OAK_LOG);
    box(l, o, 3, 5, -9, 18, 5, -3, Blocks.DARK_OAK_SLAB);
    for (int x = 5; x <= 16; x++) if (x < 10 || x > 12) block(l, o, x, 1, -7, Blocks.OAK_FENCE);
    block(l, o, 7, 4, -4, Blocks.LANTERN);
    block(l, o, 15, 4, -4, Blocks.LANTERN);
    // Asymmetric front windows, brick sill and planting. Bedroom/TV anchors are unchanged.
    for (int[] w : new int[][] {{1, 5}, {17, 21}}) {
      box(l, o, w[0], 2, -2, w[1], 3, -2, Blocks.GLASS_PANE);
      box(l, o, w[0] - 1, 1, -3, w[1] + 1, 1, -3, Blocks.BRICKS);
    }
    box(l, o, 19, 1, -4, 23, 1, -4, Blocks.FLOWERING_AZALEA_LEAVES);
    // Side bay opens the living room into a reading nook, breaking the rectangular footprint.
    box(l, o, -6, 0, 4, -3, 0, 10, Blocks.SPRUCE_PLANKS);
    box(l, o, -6, 1, 4, -3, 4, 10, Blocks.WHITE_TERRACOTTA);
    box(l, o, -5, 1, 5, -2, 4, 9, Blocks.AIR);
    box(l, o, -6, 2, 5, -6, 3, 9, Blocks.GLASS_PANE);
    box(l, o, -5, 2, 4, -3, 3, 4, Blocks.GLASS_PANE);
    box(l, o, -5, 2, 10, -3, 3, 10, Blocks.GLASS_PANE);
    box(l, o, -7, 5, 3, -2, 5, 11, Blocks.DEEPSLATE_TILE_SLAB);
    CampusFurnishings.put(l, o.offset(-4, 1, 6), CampusFurnishings.HOME_CHAIR, Direction.EAST);
    block(l, o, -4, 1, 8, Blocks.BOOKSHELF);
    block(l, o, -4, 2, 8, Blocks.LANTERN);
    // Replace the oversized stair sofa and fence-top dining arrangement with scaled models.
    box(l, o, 6, 1, 10, 10, 2, 12, Blocks.AIR);
    for (int x = 6; x <= 9; x++)
      CampusFurnishings.put(l, o.offset(x, 1, 11), CampusFurnishings.HOME_CHAIR, Direction.NORTH);
    box(l, o, 9, 1, 14, 11, 2, 17, Blocks.AIR);
    for (int x = 9; x <= 11; x++) {
      CampusFurnishings.put(l, o.offset(x, 1, 16), CampusFurnishings.DESK, Direction.NORTH);
      CampusFurnishings.put(l, o.offset(x, 1, 17), CampusFurnishings.HOME_CHAIR, Direction.NORTH);
    }
    // These lights emit at the lowered ceiling, not several blocks above the occupied room.
    for (int[] a : new int[][] {{5, 6}, {10, 15}, {18, 5}, {20, 17}}) {
      block(l, o, a[0], 6, a[1], Blocks.SPRUCE_PLANKS);
      CampusFurnishings.put(l, o.offset(a[0], 5, a[1]), CampusFurnishings.PANEL, Direction.NORTH);
    }
    block(l, o, 10, 4, 15, Blocks.AIR);
  }

  static void roof(
      ServerLevel l, BlockPos o, int x0, int x1, int z0, int z1, int base, Block stairs) {
    Block slab =
        stairs == Blocks.DARK_OAK_STAIRS ? Blocks.DARK_OAK_SLAB : Blocks.DEEPSLATE_TILE_SLAB;
    for (int x = x0; x <= x1; x++) {
      int distance = Math.min(x - x0, x1 - x);
      int y = base + distance / 2;
      for (int z = z0; z <= z1; z++)
        l.setBlock(
            o.offset(x, y, z),
            slab.defaultBlockState()
                .setValue(SlabBlock.TYPE, distance % 2 == 0 ? SlabType.BOTTOM : SlabType.TOP),
            2);
    }
  }

  static void classroom(ServerLevel l, BlockPos o) {
    // Clear old furniture/board without deleting the mapped perimeter, windows or entrance.
    box(l, o, 76, 1, -49, 90, 4, -30, Blocks.AIR);
    box(l, o, 76, 5, -54, 91, 5, -29, Blocks.SMOOTH_QUARTZ);
    box(l, o, 76, 1, -50, 87, 4, -50, Blocks.WHITE_CONCRETE);
    box(l, o, 78, 2, -49, 85, 3, -49, Blocks.GREEN_CONCRETE);
    block(l, o, 79, 1, -48, Blocks.LECTERN);
    IntroScenes.sign(
        l, o.offset(88, 2, -50), "SCC 123", "Class exercise", "Club afterwards", "SCC 121");
    for (int z : new int[] {-45, -41, -37, -33})
      for (int x : new int[] {77, 78, 81, 82, 85, 86}) {
        CampusFurnishings.put(l, o.offset(x, 1, z), CampusFurnishings.DESK, Direction.NORTH);
        CampusFurnishings.put(l, o.offset(x, 1, z + 1), CampusFurnishings.CHAIR, Direction.NORTH);
      }
    for (int z : new int[] {-47, -42, -37, -32})
      for (int x : new int[] {78, 82, 86, 90})
        CampusFurnishings.put(l, o.offset(x, 4, z), CampusFurnishings.PANEL, Direction.NORTH);
    // Remove old high fluorescent look-alikes, leaving a quiet closed plenum above the new ceiling.
    for (int x = 76; x <= 91; x++)
      for (int z = -54; z <= -29; z++) {
        block(l, o, x, 7, z, Blocks.AIR);
        block(l, o, x, 8, z, Blocks.SMOOTH_QUARTZ);
      }
    // Club room: paired workstation rows, clear gathering space and a teaching/display wall.
    box(l, o, 94, 1, -54, 112, 4, -30, Blocks.AIR);
    box(l, o, 94, 5, -54, 112, 5, -29, Blocks.SMOOTH_QUARTZ);
    for (int z : new int[] {-48, -43, -38})
      for (int x : new int[] {96, 97, 101, 102}) {
        CampusFurnishings.put(l, o.offset(x, 1, z), CampusFurnishings.DESK, Direction.NORTH);
        CampusFurnishings.put(l, o.offset(x, 1, z + 1), CampusFurnishings.CHAIR, Direction.NORTH);
        l.setBlock(o.offset(x, 2, z), MonitorContent.stand(Direction.SOUTH), 2);
      }
    block(l, o, 105, 1, -39, Blocks.LECTERN);
    block(l, o, 117, 1, -43, Blocks.LECTERN);
    for (int z : new int[] {-51, -46, -41, -36, -31})
      for (int x : new int[] {96, 101, 106, 111})
        CampusFurnishings.put(l, o.offset(x, 4, z), CampusFurnishings.PANEL, Direction.NORTH);
    for (int x = 94; x <= 112; x++)
      for (int z = -54; z <= -29; z++) {
        block(l, o, x, 7, z, Blocks.AIR);
        block(l, o, x, 8, z, Blocks.SMOOTH_QUARTZ);
      }
  }

  static void parking(ServerLevel l, BlockPos o) {
    // South/front of the SCC; the existing satellite site remains east of the parking area.
    box(l, o, 239, 1, -86, 302, 4, -28, Blocks.STONE);
    box(l, o, 249, 4, -77, 301, 4, -40, Blocks.GRAY_CONCRETE);
    box(l, o, 240, 4, -87, 247, 4, -28, Blocks.SMOOTH_STONE);
    for (int x : new int[] {249, 301}) box(l, o, x, 5, -77, x, 5, -40, Blocks.STONE_SLAB);
    for (int x = 250; x <= 298; x += 6) {
      box(l, o, x, 4, -76, x, 4, -64, Blocks.WHITE_CONCRETE);
      box(l, o, x, 4, -53, x, 4, -41, Blocks.WHITE_CONCRETE);
      block(l, o, x + 2, 5, -75, Blocks.STONE_SLAB);
      block(l, o, x + 2, 5, -42, Blocks.STONE_SLAB);
    }
    // Accessible bays adjacent to the pedestrian sidewalk, not stranded across the lot.
    for (int x : new int[] {251, 257}) {
      box(l, o, x, 4, -73, x + 3, 4, -65, Blocks.BLUE_CONCRETE);
      IntroScenes.sign(
          l,
          o.offset(x + 1, 5, -76),
          "Accessible parking",
          "Keep access aisle clear",
          "SCC entrance ↑",
          "");
    }
    for (int z = -63; z <= -54; z += 2) box(l, o, 284, 4, z, 287, 4, z, Blocks.WHITE_CONCRETE);
    for (int[] c :
        new int[][] {{265, -73}, {277, -73}, {289, -73}, {259, -50}, {271, -50}, {289, -50}})
      car(l, o, c[0], 5, c[1], c[0] % 3 == 0 ? Blocks.RED_TERRACOTTA : Blocks.BLUE_TERRACOTTA);
    for (int x : new int[] {252, 298}) {
      box(l, o, x, 5, -56, x, 9, -56, Blocks.IRON_BARS);
      CampusFurnishings.put(l, o.offset(x, 10, -56), CampusFurnishings.PANEL, Direction.NORTH);
    }
    // A separate driveway climbs from College Rd; it does not block the northern pedestrian ramp.
    box(l, o, 217, 1, -61, 249, 6, -55, Blocks.AIR);
    for (int x = 217; x <= 249; x++) {
      int half = Math.max(0, Math.min(8, (x - 225) / 2));
      box(l, o, x, 0, -61, x, half / 2, -55, Blocks.SMOOTH_STONE);
      if (half % 2 == 1)
        box(l, o, x, half / 2 + 1, -61, x, half / 2 + 1, -55, Blocks.SMOOTH_STONE_SLAB);
    }
    for (int z = -61; z <= -55; z += 2) box(l, o, 241, 4, z, 247, 4, z, Blocks.WHITE_CONCRETE);
    IntroScenes.sign(
        l,
        o.offset(245, 5, -79),
        "SCC PARKING",
        "Entrance / Exit 1 ↑",
        "College of DuPage",
        "Pedestrians: sidewalk");
    // Bike rack and benches are off the entrance approach, not inside the doors.
    for (int x = 250; x <= 255; x++) block(l, o, x, 5, -84, Blocks.IRON_BARS);
    for (int x = 258; x <= 261; x++)
      CampusFurnishings.put(l, o.offset(x, 5, -84), CampusFurnishings.CHAIR, Direction.SOUTH);
  }

  static void car(ServerLevel l, BlockPos o, int x, int y, int z, Block color) {
    CampusFurnishings.put(l, o.offset(x, y, z), CampusFurnishings.CAR_FRONT, Direction.NORTH);
    CampusFurnishings.put(l, o.offset(x, y, z + 2), CampusFurnishings.CAR_REAR, Direction.NORTH);
  }

  static void neighbourhood(ServerLevel l, BlockPos o) {
    for (int i = 0; i < 6; i++) {
      int x = 73 + i * 21, z = i % 2 == 0 ? 14 : -32;
      box(l, o, x, 1, z, x + 14, 4, z + 12, i % 2 == 0 ? Blocks.WHITE_TERRACOTTA : Blocks.BRICKS);
      box(l, o, x + 1, 1, z + 1, x + 13, 3, z + 11, Blocks.AIR);
      roof(l, o, x - 1, x + 15, z - 1, z + 13, 5, Blocks.DARK_OAK_STAIRS);
      int front = z > 0 ? z : z + 12;
      box(l, o, x + 2, 2, front, x + 5, 3, front, Blocks.GLASS_PANE);
      box(l, o, x + 9, 2, front, x + 12, 3, front, Blocks.GLASS_PANE);
      CampusNeighborhood.door(
          l,
          o.offset(x + 7, 1, front),
          z > 0 ? Direction.NORTH : Direction.SOUTH,
          DoorHingeSide.LEFT,
          Blocks.OAK_DOOR);
      // Northern driveways stop at College Road's north curb, not across its lanes.
      int curb = z > 0 ? -3 : -19;
      box(l, o, x + 6, 0, Math.min(front, curb), x + 8, 0, Math.max(front, curb), Blocks.GRAVEL);
      box(l, o, x - 1, 1, z + 13, x + 15, 1, z + 13, Blocks.OAK_LEAVES);
      block(l, o, x + 1, 1, z - 2, Blocks.BARREL);
    }
    // College Rd edge gains planting, a bus shelter and a small forecourt instead of empty grass.
    box(l, o, 181, 0, -116, 199, 0, -100, Blocks.SMOOTH_STONE);
    box(l, o, 183, 1, -114, 183, 3, -103, Blocks.GLASS_PANE);
    box(l, o, 183, 4, -115, 198, 4, -102, Blocks.SMOOTH_STONE_SLAB);
    for (int x : new int[] {184, 197}) box(l, o, x, 1, -114, x, 3, -114, Blocks.IRON_BARS);
    for (int x = 185; x <= 190; x++)
      CampusFurnishings.put(l, o.offset(x, 1, -112), CampusFurnishings.CHAIR, Direction.SOUTH);
    IntroScenes.sign(
        l,
        o.offset(199, 1, -104),
        "CAMPUS SHUTTLE",
        "College of DuPage",
        "SCC / Campus walk",
        "Fictional service");
    for (int z : new int[] {-45, -82, -152, -185}) {
      box(l, o, 191, 1, z, 191, 4, z, Blocks.OAK_LOG);
      box(l, o, 188, 4, z - 2, 194, 6, z + 2, Blocks.OAK_LEAVES);
      box(l, o, 225, 1, z - 2, 225, 1, z + 2, Blocks.AZALEA_LEAVES);
    }
  }

  static boolean nightContains(BlockPos o, BlockPos p) {
    int x = p.getX() - o.getX(), y = p.getY() - o.getY(), z = p.getZ() - o.getZ();
    return x >= -7 && x <= 25 && z >= -9 && z <= 21 && y >= 1 && y <= 16;
  }

  static void populate(ServerPlayer p, CompoundTag t, BlockPos o) {
    int[][] spots = {
      {35, 1, -4},
      {65, 1, -7},
      {104, 1, 8},
      {150, 1, -12},
      {222, 1, -58},
      {222, 1, -103},
      {188, 1, -108},
      {244, 5, -80},
      {245, 5, -66},
      {297, 5, -59}
    };
    for (int i = 0; i < spots.length; i++) {
      var npc = net.minecraft.world.entity.EntityType.VILLAGER.create(p.serverLevel());
      if (npc == null) continue;
      int[] at = spots[i];
      npc.moveTo(o.getX() + at[0] + .5, o.getY() + at[1], o.getZ() + at[2] + .5, i * 37, 0);
      npc.setNoAi(true);
      npc.setInvulnerable(true);
      npc.setSilent(true);
      npc.setPersistenceRequired();
      npc.setCustomName(
          net.minecraft.network.chat.Component.literal(
              i < 4 ? "Neighbour" : i == 6 ? "Waiting for the shuttle" : "Student"));
      npc.addTag("timelocked_campus_actor");
      npc.addTag("CampusAmbient" + i);
      npc.getPersistentData().putUUID("CampusOwner", p.getUUID());
      t.putUUID("CampusAmbient" + i, npc.getUUID());
      if (!p.serverLevel().addFreshEntity(npc)) t.remove("CampusAmbient" + i);
    }
  }

  private CampusRebuild() {}
}
