package com.timelocked;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.properties.*;

/** SCC finishes over the PDF shell; v34 widens the photo-reported circulation pinch points. */
final class SccPhotoRevision {
  static final String REVISION = "SccApproachV32";
  static final String PREVIOUS_REVISION = "SccPhotosV31";
  static final String EARLIER_REVISION = "SccPhotosV30";

  static boolean active(CompoundTag t) {
    return t != null
        && (t.getBoolean(REVISION)
            || t.getBoolean(PREVIOUS_REVISION)
            || t.getBoolean(EARLIER_REVISION));
  }

  static BlockPos home(int slot) {
    return new BlockPos((slot % 4096) * 512, 80, -4000000 - (slot / 4096) * 512);
  }

  static void build(ServerLevel l, BlockPos o) {
    CampusRebuild.build(l, o);
    homeFinish(l, o);
    school(l, CampusNeighborhood.school(o));
  }

  static void house(ServerLevel l, BlockPos o, boolean morning) {
    CampusRebuild.house(l, o, morning);
    homeFinish(l, o);
  }

  static void box(ServerLevel l, BlockPos o, int x, int y, int z, int xx, int yy, int zz, Block b) {
    SccCampus.box(l, o, x, y, z, xx, yy, zz, b);
  }

  static void block(ServerLevel l, BlockPos o, int x, int y, int z, Block b) {
    SccCampus.block(l, o, x, y, z, b);
  }

  static void homeFinish(ServerLevel l, BlockPos o) {
    // A real lintel closes the full-height slit above the bedroom's two-block door.
    box(l, o, 14, 3, 8, 14, 5, 8, Blocks.WHITE_TERRACOTTA);
    // Bay ceiling aligns with the living room. Roof is outside, no floating interior dark beam.
    box(l, o, -7, 5, 3, -2, 5, 11, Blocks.AIR);
    box(l, o, -6, 4, 4, -2, 5, 4, Blocks.WHITE_TERRACOTTA);
    box(l, o, -6, 4, 10, -2, 5, 10, Blocks.WHITE_TERRACOTTA);
    box(l, o, -6, 4, 5, -6, 5, 9, Blocks.WHITE_TERRACOTTA);
    box(l, o, -5, 6, 4, -2, 6, 10, Blocks.SPRUCE_PLANKS);
    box(l, o, -7, 7, 3, -2, 7, 11, Blocks.DEEPSLATE_TILE_SLAB);
    for (int[] p : new int[][] {{11, 8}, {16, 9}, {3, 10}, {-4, 7}})
      CampusFurnishings.put(l, o.offset(p[0], 5, p[1]), CampusFurnishings.PANEL, Direction.NORTH);
  }

  static boolean wall(ServerLevel l, BlockPos p) {
    Block b = l.getBlockState(p).getBlock();
    return b == Blocks.WHITE_CONCRETE
        || b == Blocks.WHITE_TERRACOTTA
        || b == Blocks.ORANGE_TERRACOTTA
        || b == Blocks.RED_TERRACOTTA;
  }

  static void school(ServerLevel l, BlockPos o) {
    // Same PDF footprint, with quiet low classroom/corridor ceilings. Tall west lobby stays tall.
    for (int x = 57; x <= 155; x++)
      for (int z = -143; z <= -29; z++) {
        boolean lobby = x <= 69 && z >= -82;
        block(l, o, x, 0, z, SccPhotoFixtures.CARPET.get());
        if (!lobby) {
          block(l, o, x, 5, z, SccPhotoFixtures.CEILING.get());
          if (l.getBlockState(o.offset(x, 4, z)).is(CampusFurnishings.PANEL.get()))
            block(l, o, x, 4, z, Blocks.AIR);
        }
        for (int y = 1; y <= 4; y++) {
          BlockPos at = o.offset(x, y, z);
          if (wall(l, at)) {
            Block finish = y == 1 ? Blocks.GRAY_CONCRETE : Blocks.WHITE_TERRACOTTA;
            // Long mustard corridor walls and light wood / orange lobby elevations.
            if (x == 93 || x == 129)
              finish = y == 1 ? Blocks.GRAY_CONCRETE : Blocks.YELLOW_TERRACOTTA;
            if (x >= 70 && x <= 81 && z >= -114)
              finish = y == 4 ? Blocks.ORANGE_TERRACOTTA : SccPhotoFixtures.WOOD.get();
            l.setBlock(at, finish.defaultBlockState(), 2);
          }
        }
      }
    // Central linear ceiling lights follow circulation, not a random scatter of bright cubes.
    strip(l, o, 125, -139, -32);
    for (int z : new int[] {-119, -61})
      for (int x = 72; x <= 126; x += 4) lamp(l, o, x, 4, z, Direction.EAST);
    // Extend light coverage through each occupied room in two restrained rows.
    for (int[] bounds :
        new int[][] {
          {76, 90, -48, -31},
          {95, 112, -53, -30},
          {95, 120, -139, -123},
          {95, 120, -112, -96},
          {95, 120, -88, -71},
          {95, 120, -80, -63},
          {131, 154, -138, -126},
          {131, 154, -121, -103},
          {131, 154, -98, -78},
          {131, 154, -73, -57},
          {131, 154, -49, -30},
          {58, 76, -120, -87},
          {82, 91, -112, -102},
          {79, 91, -96, -72},
          {79, 91, -67, -59}
        }) {
      for (int x = bounds[0] + 2; x <= bounds[1]; x += 5)
        for (int z = bounds[2] + 2; z <= bounds[3]; z += 5)
          if (l.getBlockState(o.offset(x, 3, z)).isAir()) lamp(l, o, x, 4, z, Direction.NORTH);
    }
    // Wood lower panels, orange upper wall, silver glazing at the main lobby interface.
    for (int z = -54; z <= -32; z++) {
      int x = wallX(z);
      box(l, o, x, 1, z, x, 3, z, SccPhotoFixtures.WOOD.get());
      box(l, o, x, 4, z, x, 10, z, Blocks.ORANGE_TERRACOTTA);
    }
    for (int z = -77; z <= -34; z += 9)
      for (int x : new int[] {60, 66}) {
        box(l, o, x, 10, z + 2, x, 11, z + 2, Blocks.CHAIN);
        SccPhotoFixtures.put(l, o.offset(x, 9, z + 2), SccPhotoFixtures.PENDANT, Direction.NORTH);
        // Secondary suspended downlights illuminate occupied lobby height.
        box(l, o, x, 6, z + 5, x, 8, z + 5, Blocks.CHAIN);
        SccPhotoFixtures.put(l, o.offset(x, 5, z + 5), SccPhotoFixtures.PENDANT, Direction.NORTH);
      }
    entrance(l, o, -28);
    entrance(l, o, -33);
    box(l, o, 60, 0, -32, 67, 0, -29, Blocks.GRAY_CONCRETE);
    label(l, o.offset(61, 3, -27), Direction.SOUTH, "EXIT", "Entrance 1", "", "");
    label(l, o.offset(66, 3, -27), Direction.SOUTH, "EXIT", "Entrance 1", "", "");
    label(
        l,
        o.offset(69, 2, -58),
        Direction.WEST,
        "Seaton Computing",
        "Center · Level 1",
        "↑ SCC 101–103",
        "→ SCC 110–124");
    label(
        l,
        o.offset(123, 2, -61),
        Direction.WEST,
        "SCC 110–124",
        "↑ Restrooms / BIC",
        "Vending →",
        "");
    // SCC 101's glass front turns right onto its east side, facing the hallway.
    // Keep the same twenty-block frontage; close the obsolete south-facing door and sign.
    block(l, o, 62, 2, -80, Blocks.AIR);
    box(l, o, 57, 1, -81, 75, 3, -81, SccPhotoFixtures.WOOD.get());
    box(l, o, 57, 4, -81, 75, 4, -81, Blocks.ORANGE_TERRACOTTA);
    for (int z = -100; z <= -81; z++) {
      box(l, o, 76, 1, z, 76, 3, z, Blocks.GLASS);
      block(l, o, 76, 4, z, SccPhotoFixtures.WOOD.get());
    }
    door(l, o, 76, -91, Direction.EAST, CampusDoors.GLASS.get());
    label(
        l,
        o.offset(77, 2, -95),
        Direction.EAST,
        "SCC 101",
        "Game Design Lab",
        "Student Studio",
        "");
    // SCC 103's resource-room glass also faces the same corridor, rather than SCC 101.
    for (int x = 82; x <= 92; x++) {
      box(l, o, x, 1, -100, x, 3, -100, Blocks.GLASS);
      block(l, o, x, 4, -100, SccPhotoFixtures.WOOD.get());
    }
    door(l, o, 87, -100, Direction.SOUTH, CampusDoors.GLASS.get());
    label(l, o.offset(83, 2, -99), Direction.SOUTH, "SCC 103", "Resource Room", "", "");
    // Move the network/resource boundary east, rather than merely deleting its wall.
    // Four full blocks (x77..80) now separate it from SCC101's x76 glazing.
    // Keep room corners, headers and ceilings continuous after the move.
    box(l, o, 77, 1, -115, 80, 4, -57, Blocks.AIR);
    for (int x = 77; x <= 80; x++)
      for (int z = -115; z <= -57; z++) {
        block(l, o, x, 0, z, SccPhotoFixtures.CARPET.get());
        block(l, o, x, 5, z, SccPhotoFixtures.CEILING.get());
      }
    label(
        l,
        o.offset(77, 2, -95),
        Direction.EAST,
        "SCC 101",
        "Game Design Lab",
        "Student Studio",
        "");
    // Resource-room west wall and its junction with the existing south glass front.
    box(l, o, 81, 1, -115, 81, 3, -101, SccPhotoFixtures.WOOD.get());
    box(l, o, 81, 4, -115, 81, 4, -101, Blocks.ORANGE_TERRACOTTA);
    box(l, o, 81, 1, -115, 93, 3, -115, SccPhotoFixtures.WOOD.get());
    box(l, o, 81, 4, -115, 93, 4, -115, Blocks.ORANGE_TERRACOTTA);
    box(l, o, 81, 1, -100, 81, 3, -100, Blocks.GLASS);
    block(l, o, 81, 4, -100, SccPhotoFixtures.WOOD.get());
    for (int z = -99; z <= -74; z++) {
      box(l, o, 81, 1, z, 81, 3, z, Blocks.GLASS);
      block(l, o, 81, 4, z, SccPhotoFixtures.WOOD.get());
    }
    // Close the unframed end-of-room slit; circulation stays outside the room.
    box(l, o, 81, 1, -73, 93, 3, -73, SccPhotoFixtures.WOOD.get());
    box(l, o, 81, 4, -73, 93, 4, -73, Blocks.ORANGE_TERRACOTTA);
    door(l, o, 81, -91, Direction.WEST, CampusDoors.GLASS.get());
    label(
        l,
        o.offset(80, 2, -93),
        Direction.WEST,
        "SCC 102",
        "NetLabs / Server",
        "Authorized",
        "Personnel Only");
    // SCC124 keeps its south-facing front, but no longer pinches the west hall.
    box(l, o, 81, 1, -68, 93, 3, -68, SccPhotoFixtures.WOOD.get());
    box(l, o, 81, 4, -68, 93, 4, -68, Blocks.ORANGE_TERRACOTTA);
    box(l, o, 81, 1, -67, 81, 3, -59, SccPhotoFixtures.WOOD.get());
    box(l, o, 81, 4, -67, 81, 4, -59, Blocks.ORANGE_TERRACOTTA);
    for (int x = 81; x <= 93; x++) {
      box(l, o, x, 1, -58, x, 3, -58, Blocks.GLASS);
      block(l, o, x, 4, -58, Blocks.YELLOW_TERRACOTTA);
    }
    door(l, o, 85, -58, Direction.SOUTH, CampusDoors.GLASS.get());
    label(
        l,
        o.offset(82, 2, -57),
        Direction.SOUTH,
        "SCC 124",
        "Data Room",
        "Authorized",
        "Personnel Only");
    rackRoom(l, o, 84, 89, -95, -76);
    rackRoom(l, o, 84, 89, -67, -60);
    for (int z = -112; z <= -60; z += 6) lamp(l, o, 79, 4, z, Direction.NORTH);
    // Recessed brown glazed classroom doors retain the actual north-wall PDF openings.
    brownEntry(l, o, 90, -56, "SCC 123", "Artificial", "Intelligence Lab");
    brownEntry(l, o, 109, -56, "SCC 121", "Club room", "Interior adapted");
    door(l, o, 117, -46, Direction.WEST, CampusDoors.BROWN.get());
    label(
        l,
        o.offset(116, 2, -48),
        Direction.WEST,
        "SCC 120",
        "Faculty / Staff",
        "Workroom",
        "Authorized staff");
    door(l, o, 129, -45, Direction.WEST, CampusDoors.BROWN.get());
    label(l, o.offset(128, 2, -47), Direction.WEST, "SCC 119", "Data Analytics Lab", "", "");
    aiLab(l, o);
    // Interior window from the circulation hall into SCC 123. Follow the traced diagonal wall.
    for (int z = -46; z <= -39; z++) {
      int x = wallX(z);
      box(l, o, x, 2, z, x, 3, z, Blocks.GLASS);
      block(l, o, x, 1, z, Blocks.GRAY_CONCRETE);
      block(l, o, x, 4, z, SccPhotoFixtures.WOOD.get());
    }
    // A compact game studio, and a glazed resource room with reading seats.
    for (int z = -116; z <= -90; z += 6) for (int x : new int[] {60, 64, 68}) station(l, o, x, z);
    for (int z : new int[] {-112, -109, -106}) {
      CampusFurnishings.put(l, o.offset(85, 1, z), CampusFurnishings.HOME_CHAIR, Direction.EAST);
      CampusFurnishings.put(l, o.offset(90, 1, z), CampusFurnishings.DESK, Direction.WEST);
    }
    // Short lobby rail segments flank, rather than cross, the existing circulation strip.
    for (int z = -115; z <= -102; z++)
      for (int x : new int[] {73}) block(l, o, x, 1, z, Blocks.IRON_BARS);
    label(
        l,
        o.offset(74, 3, -115),
        Direction.SOUTH,
        "↑ SCC 101–102",
        "← SCC 110–124",
        "Seaton Computing",
        "Center");
    for (int z : new int[] {-76, -40}) {
      block(l, o, 58, 1, z, Blocks.POTTED_FERN);
      CampusFurnishings.put(l, o.offset(59, 1, z + 1), CampusFurnishings.CHAIR, Direction.EAST);
    }
    // Distinct AI-lab naming on the existing story exercise plaque, no fictional room rename.
    label(
        l,
        o.offset(88, 2, -50),
        Direction.SOUTH,
        "SCC 123 · AI Lab",
        "Class exercise",
        "Club afterwards",
        "SCC 121");
  }

  static int wallX(int z) {
    int row = z - SccCampus.Z;
    if (row < 0 || row >= SccCampus.PLAN.length) return 70;
    String line = SccCampus.PLAN[row];
    for (int i = 14; i <= 37; i++) if (line.charAt(i) == '#') return SccCampus.X + i;
    return 79;
  }

  static void strip(ServerLevel l, BlockPos o, int x, int z0, int z1) {
    for (int z = z0; z <= z1; z += 3) lamp(l, o, x, 4, z, Direction.NORTH);
  }

  static void lamp(ServerLevel l, BlockPos o, int x, int y, int z, Direction d) {
    if (l.getBlockState(o.offset(x, y - 1, z)).isAir())
      SccPhotoFixtures.put(l, o.offset(x, y, z), SccPhotoFixtures.STRIP, d);
  }

  static void entrance(ServerLevel l, BlockPos o, int z) {
    box(l, o, 60, 1, z, 67, 3, z, Blocks.GLASS);
    for (int x : new int[] {60, 67}) box(l, o, x, 1, z, x, 4, z, Blocks.SMOOTH_QUARTZ);
    box(l, o, 60, 4, z, 67, 4, z, Blocks.SMOOTH_QUARTZ);
    // One centered double door per bank, with fixed sidelights: no center hinges.
    for (int x = 63; x <= 64; x++)
      CampusNeighborhood.door(
          l,
          o.offset(x, 1, z),
          Direction.SOUTH,
          x == 63 ? DoorHingeSide.RIGHT : DoorHingeSide.LEFT,
          CampusDoors.GLASS.get());
  }

  static void door(ServerLevel l, BlockPos o, int x, int z, Direction d, Block b) {
    block(l, o, x, 0, z, SccPhotoFixtures.CARPET.get());
    box(l, o, x, 1, z, x, 3, z, Blocks.GLASS);
    CampusNeighborhood.door(l, o.offset(x, 1, z), d, DoorHingeSide.LEFT, b);
  }

  static void brownEntry(
      ServerLevel l, BlockPos o, int x, int z, String number, String name, String detail) {
    box(l, o, x - 1, 1, z, x + 2, 3, z, Blocks.WHITE_CONCRETE);
    box(l, o, x + 1, 1, z, x + 1, 3, z, Blocks.GLASS);
    door(l, o, x, z, Direction.NORTH, CampusDoors.BROWN.get());
    label(l, o.offset(x + 2, 2, z - 1), Direction.NORTH, number, name, detail, "");
  }

  static void rackRoom(ServerLevel l, BlockPos o, int x0, int x1, int z0, int z1) {
    for (int z = z0; z <= z1; z += 3)
      for (int x : new int[] {x0, x1}) {
        SccPhotoFixtures.put(l, o.offset(x, 1, z), SccPhotoFixtures.RACK_LOWER, Direction.WEST);
        SccPhotoFixtures.put(l, o.offset(x, 2, z), SccPhotoFixtures.RACK_UPPER, Direction.WEST);
      }
    for (int z = z0; z <= z1; z++)
      SccPhotoFixtures.put(l, o.offset(x0, 4, z), SccPhotoFixtures.TRAY, Direction.NORTH);
    station(l, o, x1 - 3, z1 - 1);
  }

  static void station(ServerLevel l, BlockPos o, int x, int z) {
    CampusFurnishings.put(l, o.offset(x, 1, z), CampusFurnishings.DESK, Direction.NORTH);
    CampusFurnishings.put(l, o.offset(x, 1, z + 1), CampusFurnishings.CHAIR, Direction.NORTH);
    l.setBlock(o.offset(x, 2, z), MonitorContent.stand(Direction.SOUTH), 2);
    if (l.getBlockEntity(o.offset(x, 2, z)) instanceof MonitorContent.MonitorEntity m)
      m.setLines("SCC WORKSTATION", "Student access", "College of DuPage", "");
  }

  static void aiLab(ServerLevel l, BlockPos o) {
    box(l, o, 76, 1, -49, 90, 3, -30, Blocks.AIR);
    box(l, o, 78, 2, -49, 85, 3, -49, Blocks.WHITE_CONCRETE);
    block(l, o, 79, 1, -48, Blocks.LECTERN);
    // Two computer benches and an open center aisle; photo shows a long whiteboard.
    for (int z : new int[] {-45, -41, -37, -33})
      for (int x : new int[] {77, 78, 86, 87}) station(l, o, x, z);
    block(l, o, 83, 1, -39, Blocks.LECTERN); // exact lesson interaction anchor
  }

  static void label(ServerLevel l, BlockPos p, Direction facing, String... lines) {
    l.setBlock(
        p, Blocks.BIRCH_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, facing), 2);
    if (l.getBlockEntity(p) instanceof SignBlockEntity s) {
      var text = s.getFrontText().setColor(DyeColor.BLACK);
      for (int i = 0; i < 4; i++)
        text = text.setMessage(i, Component.literal(i < lines.length ? lines[i] : ""));
      s.setText(text, true);
      s.setWaxed(true);
      s.setChanged();
      l.sendBlockUpdated(p, l.getBlockState(p), l.getBlockState(p), 3);
    }
  }

  private SccPhotoRevision() {}
}
