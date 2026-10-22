package com.timelocked;

import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Plane;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

public final class VoidspaceVillage {
  private static void garden(int ox, int oz, BiConsumer<BlockPos, BlockState> out) {
    BlockState path = Content.CELESTIAL_PATH.get().defaultBlockState(),
        trim = Blocks.CALCITE.defaultBlockState();
    int[][] homes = {
      {-58, -48}, {0, -65}, {58, -48}, {-68, 10}, {68, 10}, {-48, 58}, {0, 70}, {48, 58}
    };
    for (int x = -90; x <= 90; x++)
      for (int z = -90; z <= 90; z++) {
        double d = Math.hypot(x, z);
        if (d > 90 || d < 35) continue;
        boolean road = Math.abs(d - 83) < 2 || Math.abs(x) <= 2 || Math.abs(z) <= 2;
        for (int[] home : homes) {
          double t =
              Math.clamp(
                  (double) (x * home[0] + z * home[1]) / (home[0] * home[0] + home[1] * home[1]),
                  0,
                  1);
          if (Math.hypot(x - home[0] * t, z - home[1] * t) < 2) road = true;
        }
        out.accept(
            new BlockPos(ox + x, 62, oz + z),
            road ? path : VoidspaceContent.GLIMMER_MOSS.get().defaultBlockState());
        if (!road && Math.floorMod(x * 31 + z * 17, 29) == 0)
          out.accept(
              new BlockPos(ox + x, 63, oz + z),
              (Math.floorMod(x + z, 3) == 0
                      ? VoidspaceContent.LANTERN_BLOOM.get()
                      : VoidspaceContent.MOON_FERN.get())
                  .defaultBlockState());
      }
    for (int[] center : new int[][] {{-41, 0}, {41, 0}, {0, -41}, {0, 43}}) {
      for (int dx = -6; dx <= 6; dx++)
        for (int dz = -6; dz <= 6; dz++) {
          double d = Math.hypot(dx, dz);
          if (d > 6) continue;
          out.accept(new BlockPos(ox + center[0] + dx, 61, oz + center[1] + dz), trim);
          out.accept(
              new BlockPos(ox + center[0] + dx, 62, oz + center[1] + dz),
              d > 4.5 ? trim : Blocks.WATER.defaultBlockState());
          out.accept(
              new BlockPos(ox + center[0] + dx, 63, oz + center[1] + dz),
              Blocks.AIR.defaultBlockState());
        }
      for (int q = -6; q <= 6; q++)
        for (int w = -1; w <= 1; w++) {
          int x = center[0] + (center[0] == 0 ? w : q), z = center[1] + (center[0] == 0 ? q : w);
          out.accept(new BlockPos(ox + x, 62, oz + z), path);
        }
    }
    for (int k = 0; k < 12; k++) {
      double a = (k + 0.5) * Math.PI / 6;
      int x = (int) Math.round(Math.cos(a) * 85), z = (int) Math.round(Math.sin(a) * 85);
      VoidspaceFlora.tree(ox + x, 62, oz + z, 9 + (k % 3), k % 3 == 0 ? 1 : 0, k % 4, out);
    }
    for (int k = 0; k < 16; k++) {
      double a = (k + 0.5) * Math.PI / 8;
      int x = (int) Math.round(Math.cos(a) * 79), z = (int) Math.round(Math.sin(a) * 79);
      for (int y = 63; y <= 65; y++)
        out.accept(
            new BlockPos(ox + x, y, oz + z), Content.CELESTIAL_MASONRY.get().defaultBlockState());
      out.accept(
          new BlockPos(ox + x, 66, oz + z),
          VoidspaceContent.CRYSTAL_BLOCK.get().defaultBlockState());
    }
  }

  public static void emit(VoidspaceLand.Village v, BiConsumer<BlockPos, BlockState> sink) {
    emit(v, 62, sink);
  }

  public static void emit(
      VoidspaceLand.Village v, int baseY, BiConsumer<BlockPos, BlockState> out) {
    int lift = baseY - 62;
    BiConsumer<BlockPos, BlockState> sink =
        lift == 0 ? out : (p, s) -> out.accept(p.above(lift), s);
    boolean live = v.intact();
    int ox = v.x();
    int oz = v.z();
    BlockState wall =
        (live ? (Block) Content.CELESTIAL_MASONRY.get() : (Block) Content.SCORCHED_STONE.get())
            .defaultBlockState();
    BlockState roof =
        (live ? (Block) Content.CELESTIAL_ROOF.get() : (Block) Content.CHARRED_TIMBER.get())
            .defaultBlockState();
    BlockState path =
        (live ? (Block) Content.CELESTIAL_PATH.get() : (Block) Content.ASH.get())
            .defaultBlockState();
    BlockState inlay = (live ? Blocks.CALCITE : Blocks.BLACKSTONE).defaultBlockState();
    BlockState glass = Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState();
    BlockState crystal = ((Block) VoidspaceContent.CRYSTAL_BLOCK.get()).defaultBlockState();
    BlockState air = Blocks.AIR.defaultBlockState();

    for (int x = -34; x <= 34; x++) {
      for (int z = -34; z <= 34; z++) {
        double d = Math.hypot(x, z);
        boolean road =
            Math.abs(x) <= 2
                || Math.abs(z) <= 2
                || lane(x, z)
                || d >= 15.5 && d <= 17.5
                || d <= 12.5;
        if (road) {
          put(sink, ox + x, 62, oz + z, path);
        }
      }
    }

    for (int x = -12; x <= 12; x++) {
      for (int z = -12; z <= 12; z++) {
        double d = Math.hypot(x, z);
        double a = Math.atan2(z, x);
        double star = 6.0 + 4.5 * Math.pow(Math.abs(Math.cos(a * 4.0)), 3.0);
        if (d <= 12.5 && (d <= star && d > star - 1.3 || Math.abs(d - 11.6) < 0.5)) {
          put(sink, ox + x, 62, oz + z, inlay);
        }
      }
    }

    if (VoidspaceLand.sanctuary(v)) garden(ox, oz, sink);
    int[][] homes =
        VoidspaceLand.sanctuary(v)
            ? new int[][] {
              {-23, -20},
              {22, -20},
              {-23, 21},
              {22, 21},
              {-58, -48},
              {0, -65},
              {58, -48},
              {-68, 10},
              {68, 10},
              {-48, 58},
              {0, 70},
              {48, 58}
            }
            : new int[][] {{-23, -20}, {22, -20}, {-23, 21}, {22, 21}};

    for (int index = 0; index < homes.length; index++) {
      int cx = ox + homes[index][0];
      int cz = oz + homes[index][1];
      double door = Math.atan2(-homes[index][1], -homes[index][0]);

      for (int x = -8; x <= 8; x++) {
        for (int z = -8; z <= 8; z++) {
          double d = Math.hypot(x, z);
          if (!(d > 7.5)) {
            put(sink, cx + x, 62, cz + z, d <= 2.2 && live ? inlay : wall);
            double ang =
                Math.abs(
                    Math.atan2(
                        Math.sin(Math.atan2(z, x) - door), Math.cos(Math.atan2(z, x) - door)));
            boolean ring = d > 6.4;
            boolean doorway = ring && ang < 0.24;

            for (int y = 63; y <= 67; y++) {
              if (ring) {
                if (doorway && y <= 65) {
                  put(sink, cx + x, y, cz + z, air);
                } else if (live || !destroyed(cx + x, y, cz + z, index)) {
                  boolean win =
                      (y == 65 || y == 66)
                          && Math.floorMod((int) Math.round(Math.atan2(z, x) * 8.0 / Math.PI), 2)
                              == 1
                          && ang > 0.5;
                  put(sink, cx + x, y, cz + z, win && live ? glass : wall);
                }
              } else {
                put(sink, cx + x, y, cz + z, air);
              }
            }

            int top = 68 + (int) Math.round(Math.sqrt(Math.max(0.0, 57.76 - d * d)) * 0.85);
            int low =
                68
                    + (int)
                        Math.round(Math.sqrt(Math.max(0.0, 57.76 - (d + 1.0) * (d + 1.0))) * 0.85);

            for (int y = Math.min(low, top); y <= top; y++) {
              if (live || !destroyed(cx + x, y, cz + z, 7 + index)) {
                put(sink, cx + x, y, cz + z, roof);
              }
            }

            if (live && ring) {
              put(sink, cx + x, 68, cz + z, Blocks.CUT_SANDSTONE.defaultBlockState());
            }
          }
        }
      }

      if (live) {
        put(sink, cx, 75, cz, crystal);
        put(
            sink,
            cx,
            76,
            cz,
            ((AlienPlantBlock) VoidspaceContent.CRYSTAL_CLUSTER.get()).defaultBlockState());
        int bx = (int) Math.round(-Math.cos(door) * 4.0);
        int bz = (int) Math.round(-Math.sin(door) * 4.0);
        put(sink, cx + bx, 63, cz + bz, Blocks.BLUE_WOOL.defaultBlockState());
        put(sink, cx + bx + (bx > 0 ? -1 : 1), 63, cz + bz, Blocks.BLUE_WOOL.defaultBlockState());
        put(sink, cx + bz, 63, cz - bx, Blocks.CRAFTING_TABLE.defaultBlockState());
        put(sink, cx - bz, 63, cz + bx, Blocks.BARREL.defaultBlockState());
        put(
            sink,
            cx,
            67,
            cz,
            (BlockState) Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
      }
    }

    for (int k = 0; k < 8; k++) {
      double a = (k + 0.5) * Math.PI / 4.0;
      VoidspaceFlora.emit(
          ox + (int) Math.round(Math.cos(a) * 21.0),
          62,
          oz + (int) Math.round(Math.sin(a) * 21.0),
          3,
          live,
          sink);
    }

    for (int x = -10; x <= 10; x++) {
      for (int z = -10; z <= 10; z++) {
        double d = Math.hypot(x, z);
        if (!(d > 10.5)) {
          if (d >= 2.5) {
            int y = 72 + (int) Math.round(Math.sqrt(Math.max(0.0, 110.25 - d * d)) * 0.4);
            if (live || !destroyed(ox + x, y, oz + z, 19)) {
              put(sink, ox + x, y, oz + z, roof);
            }
          }

          if (d > 9.3 && live) {
            put(sink, ox + x, 72, oz + z, Blocks.CUT_SANDSTONE.defaultBlockState());
          }
        }
      }
    }

    for (int k = 0; k < 8; k++) {
      double a = k * Math.PI / 4.0;
      int px = (int) Math.round(Math.cos(a) * 9.0);
      int pz = (int) Math.round(Math.sin(a) * 9.0);
      int ptop = 72 + (int) Math.round(Math.sqrt(Math.max(0.0, 29.25)) * 0.4);

      for (int y = 63; y < ptop; y++) {
        if (live || y < 66) {
          put(sink, ox + px, y, oz + pz, wall);
        }
      }

      if (live) {
        put(
            sink,
            ox + (int) Math.round(Math.cos(a) * 6.0),
            74,
            oz + (int) Math.round(Math.sin(a) * 6.0),
            (BlockState) Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
      }
    }

    int spire = live ? 86 : 70;

    for (int y = 63; y <= spire; y++) {
      int r = y < 70 ? 1 : 0;

      for (int x = -r; x <= r; x++) {
        for (int z = -r; z <= r; z++) {
          if (Math.abs(x) + Math.abs(z) <= r) {
            put(sink, ox + x, y, oz + z, y % 5 == 0 && live ? inlay : wall);
          }
        }
      }
    }

    if (live) {
      put(sink, ox, spire + 1, oz, crystal);
      put(sink, ox, spire + 2, oz, Blocks.END_ROD.defaultBlockState());

      for (Direction d : Plane.HORIZONTAL) {
        put(
            sink,
            ox + d.getStepX(),
            80,
            oz + d.getStepZ(),
            (BlockState) Blocks.END_ROD.defaultBlockState().setValue(DirectionalBlock.FACING, d));
      }
    }

    for (Direction d : Plane.HORIZONTAL) {
      int gx = ox + d.getStepX() * 33;
      int gz = oz + d.getStepZ() * 33;
      int sx = d.getStepZ();
      int sz = d.getStepX();

      for (int t = -3; t <= 3; t++) {
        int top = Math.abs(t) == 3 ? 69 : (Math.abs(t) == 2 ? 70 : 71);

        for (int y = 63; y <= top; y++) {
          boolean open = Math.abs(t) <= 2 && y < top;
          if (!open && (live || !destroyed(gx + sx * t, y, gz + sz * t, 23))) {
            put(sink, gx + sx * t, y, gz + sz * t, wall);
          }
        }
      }

      if (live) {
        put(sink, gx, 70, gz, crystal);
        put(
            sink,
            gx + sx * 2,
            68,
            gz + sz * 2,
            (BlockState) Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        put(
            sink,
            gx - sx * 2,
            68,
            gz - sz * 2,
            (BlockState) Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
      }
    }

    if (live) {
      for (Direction d : Plane.HORIZONTAL) {
        for (int r : new int[] {14, 26}) {
          int lx = ox + d.getStepX() * r + d.getStepZ() * 3;
          int lz = oz + d.getStepZ() * r + d.getStepX() * 3;

          for (int y = 63; y <= 65; y++) {
            put(sink, lx, y, lz, wall);
          }

          put(sink, lx, 66, lz, Blocks.LANTERN.defaultBlockState());
        }
      }
    }

    if (live) {
      clinic(sink, ox + 16, oz - 39);
      if (VoidspaceLand.sanctuary(v)) {
        entranceRoutes(ox, oz, sink);
        CelestialJobs.furnish(ox, oz, sink);
      }
    }
  }

  /** Last pass: reserved, level pedestrian routes cannot be overgrown. */
  private static void entranceRoutes(int ox, int oz, BiConsumer<BlockPos, BlockState> sink) {
    // Arrival apron -> east gate -> central plaza. Avoid every home footprint.
    route(sink, ox, oz, 90, -70, 90, 0, 2);
    route(sink, ox, oz, 90, 0, 12, 0, 2);
    // Clinic doors face +Z at local (16,-31). Join the north promenade
    // via an elbow so the connection does not cut through the NE home.
    route(sink, ox, oz, 16, -30, 16, -29, 1);
    route(sink, ox, oz, 16, -29, 0, -29, 1);
    BlockState wall = Content.CELESTIAL_MASONRY.get().defaultBlockState();
    BlockState trim = Blocks.CALCITE.defaultBlockState();
    // Deep, roofed gatehouse centred exactly on the east promenade crossing.
    for (int x = 80; x <= 86; x++)
      for (int z = -6; z <= 6; z++) {
        put(sink, ox + x, 62, oz + z, Content.CELESTIAL_PATH.get().defaultBlockState());
        for (int y = 63; y <= 72; y++) {
          boolean tower = Math.abs(z) >= 4;
          boolean lintel = y >= 69 && y <= 70;
          put(
              sink,
              ox + x,
              y,
              oz + z,
              tower ? (y == 71 ? trim : wall) : (lintel ? trim : Blocks.AIR.defaultBlockState()));
        }
      }
    for (int z : new int[] {-5, 5}) {
      put(sink, ox + 83, 73, oz + z, VoidspaceContent.CRYSTAL_BLOCK.get().defaultBlockState());
      put(sink, ox + 79, 65, oz + z, Blocks.LANTERN.defaultBlockState());
      put(sink, ox + 87, 65, oz + z, Blocks.LANTERN.defaultBlockState());
    }
  }

  private static void route(
      BiConsumer<BlockPos, BlockState> sink,
      int ox,
      int oz,
      int x0,
      int z0,
      int x1,
      int z1,
      int halfWidth) {
    for (int x = Math.min(x0, x1) - (x0 == x1 ? halfWidth : 0);
        x <= Math.max(x0, x1) + (x0 == x1 ? halfWidth : 0);
        x++)
      for (int z = Math.min(z0, z1) - (z0 == z1 ? halfWidth : 0);
          z <= Math.max(z0, z1) + (z0 == z1 ? halfWidth : 0);
          z++) {
        put(sink, ox + x, 62, oz + z, Content.CELESTIAL_PATH.get().defaultBlockState());
        for (int y = 63; y <= 65; y++) put(sink, ox + x, y, oz + z, Blocks.AIR.defaultBlockState());
      }
  }

  private static boolean lane(int x, int z) {
    for (int[] h : new int[][] {{-23, -20}, {22, -20}, {-23, 21}, {22, 21}}) {
      double t = Math.clamp((double) (x * h[0] + z * h[1]) / (h[0] * h[0] + h[1] * h[1]), 0.0, 1.0);
      if (Math.hypot(x - h[0] * t, z - h[1] * t) <= 1.5) {
        return true;
      }
    }

    return false;
  }

  private static boolean dome(int x, int z) {
    return Math.abs(x) <= 7 && Math.abs(z) <= 5 && Math.abs(x) + Math.abs(z) <= 10;
  }

  private static boolean domeEdge(int x, int z) {
    return dome(x, z) && (!dome(x + 1, z) || !dome(x - 1, z) || !dome(x, z + 1) || !dome(x, z - 1));
  }

  static void clinic(BiConsumer<BlockPos, BlockState> sink, int cx, int cz) {
    BlockState panel = VoidspaceContent.block("timelocked:sarc_wall_panel").defaultBlockState();
    BlockState shell =
        VoidspaceContent.block("timelocked:sarc_reinforced_shell").defaultBlockState();
    BlockState glass = VoidspaceContent.block("timelocked:space_viewport").defaultBlockState();
    BlockState grate = VoidspaceContent.block("timelocked:steel_grating").defaultBlockState();
    BlockState lamp = VoidspaceContent.block("timelocked:factory_lamp").defaultBlockState();
    BlockState floor = ((Block) Content.FLOOR.get()).defaultBlockState();
    BlockState green = Blocks.GREEN_CONCRETE.defaultBlockState();
    BlockState white = Blocks.WHITE_CONCRETE.defaultBlockState();
    BlockState air = Blocks.AIR.defaultBlockState();

    for (int x = -8; x <= 9; x++) {
      for (int z = -7; z <= 9; z++) {
        put(sink, cx + x, 62, cz + z, x != -8 && x != 9 && z != -7 && z != 9 ? floor : grate);

        for (int y = 63; y <= 72; y++) {
          put(sink, cx + x, y, cz + z, air);
        }
      }
    }

    for (int x = -7; x <= 7; x++) {
      for (int z = -5; z <= 5; z++) {
        if (dome(x, z)) {
          boolean edge = domeEdge(x, z);
          if (edge) {
            for (int y = 1; y <= 4; y++) {
              boolean window =
                  (y == 2 || y == 3)
                      && (Math.abs(z) == 5 && (Math.abs(x) == 2 || Math.abs(x) == 4)
                          || Math.abs(x) == 7 && Math.abs(z) <= 1);
              put(
                  sink,
                  cx + x,
                  62 + y,
                  cz + z,
                  y == 1 ? shell : (window ? glass : (y == 4 ? green : panel)));
            }
          }

          boolean lit = Math.abs(x) == 3 && z == 0 || x == 0 && Math.abs(z) == 3;
          put(sink, cx + x, 67, cz + z, edge ? shell : (lit ? lamp : panel));
          if (Math.abs(x) <= 5 && Math.abs(z) <= 3 && Math.abs(x) + Math.abs(z) <= 7) {
            boolean cross = x == 0 && Math.abs(z) <= 2 || z == 0 && Math.abs(x) <= 2;
            put(sink, cx + x, 68, cz + z, cross ? green : white);
          }
        }
      }
    }

    for (int x = -2; x <= 2; x++) {
      for (int z = 6; z <= 8; z++) {
        put(sink, cx + x, 66, cz + z, shell);

        for (int y = 1; y <= 3; y++) {
          if (Math.abs(x) == 2 || z == 8) {
            put(
                sink,
                cx + x,
                62 + y,
                cz + z,
                y != 3 && (Math.abs(x) != 2 || z != 8) ? glass : shell);
          }
        }
      }
    }

    put(
        sink,
        cx,
        65,
        cz + 8,
        VoidspaceContent.block("timelocked:containment_light").defaultBlockState());
    BlockState door =
        (BlockState)
            ((BlockState)
                    ((BlockState)
                            ((AnimatedSarcDoorBlock) FacilityDetails.SERVICE_DOOR.get())
                                .defaultBlockState()
                                .setValue(DoorBlock.FACING, Direction.NORTH))
                        .setValue(DoorBlock.HINGE, DoorHingeSide.LEFT))
                .setValue(DoorBlock.OPEN, false);

    for (int z : new int[] {5, 8}) {
      put(sink, cx, 63, cz + z, (BlockState) door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
      put(sink, cx, 64, cz + z, (BlockState) door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
    }

    BlockState bed =
        (BlockState)
            Blocks.WHITE_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.NORTH);

    for (int x : new int[] {-2, 0, 2}) {
      put(sink, cx + x, 63, cz - 4, (BlockState) bed.setValue(BedBlock.PART, BedPart.HEAD));
      put(sink, cx + x, 63, cz - 3, (BlockState) bed.setValue(BedBlock.PART, BedPart.FOOT));
    }

    put(sink, cx - 4, 63, cz - 3, CelestialJobs.Role.MEDIC.block.get().defaultBlockState());
    put(sink, cx - 5, 63, cz + 2, CelestialJobs.Role.MEDIC.block.get().defaultBlockState());
    put(sink, cx + 4, 63, cz - 3, Blocks.BARREL.defaultBlockState());
    put(sink, cx + 5, 63, cz + 2, Blocks.CRAFTING_TABLE.defaultBlockState());
    put(sink, cx + 5, 63, cz - 1, Blocks.BREWING_STAND.defaultBlockState());
    put(sink, cx - 5, 63, cz - 1, Blocks.FLOWER_POT.defaultBlockState());

    for (int y = 68; y <= 71; y++) {
      put(sink, cx - 5, y, cz - 3, Blocks.IRON_BARS.defaultBlockState());
    }

    put(sink, cx - 5, 72, cz - 3, Blocks.LIGHTNING_ROD.defaultBlockState());
  }

  public static boolean destroyed(int x, int y, int z, int salt) {
    long h = x * 73428767L ^ z * 912931L ^ y * 19349663L ^ salt * 83492791L;
    h ^= h >>> 17;
    return Math.floorMod(h, 10) < (y >= 69 ? 7 : 4);
  }

  private static void put(
      BiConsumer<BlockPos, BlockState> sink, int x, int y, int z, BlockState s) {
    sink.accept(new BlockPos(x, y, z), s);
  }

  private VoidspaceVillage() {}
}
