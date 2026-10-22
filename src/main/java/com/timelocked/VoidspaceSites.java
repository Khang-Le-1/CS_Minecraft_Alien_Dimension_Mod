package com.timelocked;

import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Plane;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class VoidspaceSites {
  public static final int[][] GARRISON =
      new int[][] {
        {-3, 1, 20, 0},
        {3, 1, 20, 0},
        {-6, 1, 6, 0},
        {6, 1, 6, 0},
        {-17, 1, 0, 0},
        {-17, 1, 8, 0},
        {17, 1, 0, 0},
        {10, 1, 10, 0},
        {0, 1, 4, 1},
        {-8, 1, 12, 1},
        {12, 1, -12, 1},
        {-10, 9, -24, 1},
        {10, 9, -24, 1},
        {0, 1, -11, 2}
      };

  public static void emit(VoidspaceGen.Site site, VoidspaceSites.Out out) {
    VoidspaceSites.P p = new VoidspaceSites.P(site, out);
    switch (site.type()) {
      case OUTPOST:
        outpost(p);
        break;
      case SHRINE:
        shrine(p);
        break;
      case DROPSHIP:
        dropship(p);
        break;
      case WATCHTOWER:
        tower(p);
        break;
      case GROTTO:
        grotto(p);
        break;
      case WARBASE:
        warbase(p);
    }
  }

  private static Block b(String id) {
    return VoidspaceContent.block("timelocked:" + id);
  }

  private static BlockState lantern(boolean hang) {
    return (BlockState) Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, hang);
  }

  static void outpost(VoidspaceSites.P p) {
    Block floor = (Block) Content.FLOOR.get();
    Block grate = b("steel_grating");
    boolean live = !p.s().ruined();

    for (int x = -10; x <= 10; x++) {
      for (int z = -8; z <= 9; z++) {
        p.put(x, 0, z, Math.abs(x) != 10 && z != -8 && z != 9 ? floor : grate);
      }
    }

    for (FieldShelter.Cell c : FieldShelter.shell()) {
      if (c.y() == 0) {
        p.put(c.x(), 0, c.z(), c.state());
      } else {
        boolean core = c.state().is((Block) FieldShelter.SHELTER_CORE.get());
        if ((!core || live)
            && (core
                || c.state().getBlock() instanceof DoorBlock
                || !p.gone(c.x(), c.y(), c.z(), 41))) {
          p.put(c.x(), c.y(), c.z(), c.state());
        }
      }
    }

    p.put(-4, 1, -2, (Block) Content.MEDICAL.get());
    p.loot(4, 1, -2, Blocks.BARREL, "sarc_field_aid");
    p.put(4, 1, -3, Blocks.BARREL);
    p.put(-4, 1, 2, Blocks.WHITE_WOOL);
    p.put(-5, 1, 2, Blocks.WHITE_WOOL);
    p.put(5, 1, 2, Blocks.CRAFTING_TABLE);

    for (int y = 1; y <= 8; y++) {
      p.put(
          8,
          y,
          -5,
          y == 8 ? Blocks.LIGHTNING_ROD.defaultBlockState() : Blocks.IRON_BARS.defaultBlockState());
    }

    p.put(8, 1, -4, Blocks.IRON_BARS);
    p.put(8, 9, -5, Blocks.END_ROD);
    if (live) {
      p.sign(3, 1, 9, 0, "SARC FIELD OUTPOST", "Sealed shelter", "Sneak-use core", "to pack up");
    } else {
      p.sign(3, 1, 9, 0, "SARC FIELD OUTPOST", "Hull breached", "Report Dawnbringer", "sightings");
    }

    for (int i = 0; i < 5; i++) {
      int x = -9 + p.roll(i, 0, 0, 47, 3);
      int z = 6 - p.roll(i, 1, 0, 49, 3);
      p.put(x, 1, z, Blocks.BARREL);
    }
  }

  static void shrine(VoidspaceSites.P p) {
    boolean live = !p.s().ruined();
    Block stone =
        live ? (Block) Content.CELESTIAL_MASONRY.get() : (Block) Content.SCORCHED_STONE.get();
    Block cap = live ? (Block) Content.CELESTIAL_ROOF.get() : (Block) Content.CHARRED_TIMBER.get();

    for (int x = -7; x <= 7; x++) {
      for (int z = -7; z <= 7; z++) {
        double d = Math.hypot(x, z);
        if (!(d > 7.4)) {
          p.put(
              x,
              0,
              z,
              d > 4.6 && d < 6.4
                  ? (live ? (Block) Content.CELESTIAL_PATH.get() : (Block) Content.ASH.get())
                  : stone);
          if (d >= 5.4 && d <= 6.6 && !p.gone(x, 6, z, 51)) {
            p.put(x, 6, z, cap);
          }
        }
      }
    }

    for (int k = 0; k < 6; k++) {
      int x = (int) Math.round(Math.cos(k * Math.PI / 3.0) * 6.0);
      int z = (int) Math.round(Math.sin(k * Math.PI / 3.0) * 6.0);
      int top = live ? 5 : 1 + p.roll(k, 0, 0, 53, 4);

      for (int y = 1; y <= top; y++) {
        p.put(x, y, z, stone);
      }

      if (live) {
        p.put(x, 7, z, (Block) VoidspaceContent.CRYSTAL_CLUSTER.get());
      }

      int bx = (int) Math.round(Math.cos(k * Math.PI / 3.0 + (Math.PI / 6)) * 5.3);
      int bz = (int) Math.round(Math.sin(k * Math.PI / 3.0 + (Math.PI / 6)) * 5.3);
      p.put(
          bx,
          1,
          bz,
          live
              ? (Block) VoidspaceContent.STAR_BLOOM.get()
              : (Block) VoidspaceContent.ASHEN_TUFT.get());
    }

    for (int x = -1; x <= 1; x++) {
      for (int z = -1; z <= 1; z++) {
        p.put(x, 1, z, cap);
      }
    }

    p.loot(0, 2, 0, Blocks.CHEST, "chests/celestial_shrine");
    if (live) {
      p.put(1, 2, 1, lantern(false));
      p.put(-1, 2, -1, lantern(false));
      p.sign(0, 1, 3, 3, "CELESTIAL SHRINE", "Dawn is not", "the end", "Rest here");
    }
  }

  static void dropship(VoidspaceSites.P p) {
    Block hull = b("space_hull_panel");
    Block panel = b("titanium_panel");
    Block grate = b("steel_grating");

    for (int x = -12; x <= 12; x++) {
      double cy = 1.6 - x * 0.13;
      int floor = (int) Math.round(cy - 2.2);
      double rz = x > 9 ? 3.5 - (x - 9) * 0.8 : 3.5;
      double ry = x > 9 ? 3.0 - (x - 9) * 0.7 : 3.0;

      for (int z = -4; z <= 4; z++) {
        for (int y = (int) Math.floor(cy - ry) - 1; y <= (int) Math.ceil(cy + ry) + 1; y++) {
          double q = z / rz * (z / rz) + (y - cy) / ry * ((y - cy) / ry);
          if (q <= 1.15 && q > 0.45) {
            boolean breach =
                z >= 3 && x >= -3 && x <= 0 && y > floor && y <= floor + 2
                    || p.roll(x, y, z, 61, 9) == 0;
            p.put(x, y, z, breach ? Blocks.AIR.defaultBlockState() : hull.defaultBlockState());
          } else if (q <= 0.45) {
            p.put(
                x,
                y,
                z,
                y == floor
                    ? grate.defaultBlockState()
                    : (y < floor ? hull.defaultBlockState() : Blocks.AIR.defaultBlockState()));
          }
        }
      }

      if (x >= -3 && x <= 3) {
        for (int z = 4; z <= 9; z++) {
          int side = z;
          p.put(x, (int) Math.round(cy - 1.0), side, panel);
          if (z <= 6) {
            p.put(x, (int) Math.round(cy - 1.0), -side, panel);
          }
        }
      }
    }

    for (int y = 4; y <= 7; y++) {
      for (int x = -12; x <= -10; x++) {
        p.put(x, (int) Math.round(3.16) + y - 3, 0, panel);
      }
    }

    int fl = (int) Math.round(0.17999999999999972);
    p.loot(-6, fl + 1, 0, Blocks.CHEST, "chests/dropship_salvage");
    p.loot(2, (int) Math.round(-0.8600000000000001) + 1, -1, Blocks.BARREL, "sarc_field_aid");
    p.put(-2, fl + 3, 0, lantern(true));

    for (int i = 0; i < 16; i++) {
      int x = -15 + p.roll(i, 0, 1, 63, 31);
      int z = -15 + p.roll(i, 0, 2, 65, 31);
      if ((Math.abs(z) > 10 || Math.abs(x) > 13) && !(Math.hypot(x, z) > 16.0)) {
        p.put(x, 1, z, p.roll(i, 0, 3, 67, 3) == 0 ? grate : panel);
      }
    }

    for (int x = 10; x <= 16; x++) {
      for (int z = -5; z <= 5; z++) {
        if (Math.hypot(x - 13, z) < 4.5) {
          p.put(
              x,
              0,
              z,
              p.roll(x, 0, z, 69, 4) == 0
                  ? Blocks.MAGMA_BLOCK
                  : (Block) Content.SCORCHED_STONE.get());
        }
      }
    }

    p.sign(0, 1, 12, 1, "SARC DROPSHIP", "LV-07 \"Kestrel\"", "Crew evacuated", "Salvage freely");
  }

  static void tower(VoidspaceSites.P p) {
    boolean live = !p.s().ruined();
    Block wall =
        live ? (Block) Content.CELESTIAL_MASONRY.get() : (Block) Content.SCORCHED_STONE.get();
    Block roof = live ? (Block) Content.CELESTIAL_ROOF.get() : (Block) Content.CHARRED_TIMBER.get();
    int top = 13;

    for (int x = -3; x <= 3; x++) {
      for (int z = -3; z <= 3; z++) {
        p.put(
            x,
            0,
            z,
            Math.abs(x) <= 2 && Math.abs(z) <= 2
                ? wall
                : (live ? (Block) Content.CELESTIAL_PATH.get() : (Block) Content.ASH.get()));
      }
    }

    for (int y = 1; y < top; y++) {
      for (int x = -2; x <= 2; x++) {
        for (int z = -2; z <= 2; z++) {
          boolean edge = Math.abs(x) == 2 || Math.abs(z) == 2;
          if (edge) {
            boolean door = z == 2 && x == 0 && y <= 2;
            boolean window = (y == 6 || y == 9) && (x == 0 || z == 0) && z != -2;
            if (!door && !window && (y <= 7 || !p.gone(x, y, z, 71))) {
              p.put(x, y, z, wall);
            }
          }
        }
      }
    }

    for (int y = 1; y <= top; y++) {
      if (live || y <= 7) {
        p.put(
            0,
            y,
            -1,
            (BlockState)
                Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH));
      }
    }

    if (live) {
      for (int x = -3; x <= 3; x++) {
        for (int z = -3; z <= 3; z++) {
          if (x != 0 || z != -1) {
            p.put(x, top, z, roof);
            if ((Math.abs(x) == 3 || Math.abs(z) == 3) && (x + z) % 2 == 0) {
              p.put(x, top + 1, z, wall);
            }
          }
        }
      }

      p.put(0, top + 1, 1, lantern(false));
      p.loot(2, top + 1, 2, Blocks.CHEST, "chests/celestial_shrine");
      p.sign(1, 1, 4, 3, "CELESTIAL WATCH", "Ring the gate", "if fire comes", "");
    } else {
      p.loot(1, 1, 1, Blocks.BARREL, "sarc_field_aid");

      for (int i = 0; i < 10; i++) {
        int x = -5 + p.roll(i, 0, 0, 73, 11);
        int z = -5 + p.roll(i, 0, 1, 75, 11);
        if (Math.abs(x) > 2 || Math.abs(z) > 2) {
          p.put(x, 1, z, wall);
        }
      }
    }
  }

  static void grotto(VoidspaceSites.P p) {
    boolean spore = p.s().biome() == VoidspaceGen.Biome.SPORE;
    BlockState shell =
        (spore
                ? (Block) VoidspaceContent.SPORE_CAP.get()
                : (Block) VoidspaceContent.CRYSTAL_BLOCK.get())
            .defaultBlockState();
    BlockState rib =
        spore
            ? ((RotatedPillarBlock) VoidspaceContent.SPORE_STEM.get()).defaultBlockState()
            : Blocks.CALCITE.defaultBlockState();

    for (int x = -8; x <= 8; x++) {
      for (int z = -8; z <= 8; z++) {
        double f = Math.hypot(x, z);
        if (f <= 7.6) {
          p.put(
              x,
              0,
              z,
              f <= 6.2 ? ((Block) VoidspaceContent.GLIMMER_MOSS.get()).defaultBlockState() : rib);
        }

        for (int y = 1; y <= 8; y++) {
          double d = Math.sqrt(x * x + y * y * 1.15 + z * z);
          boolean door = z >= 5 && Math.abs(x) <= 1 && y <= 3;
          if (d > 6.3 && d <= 7.4 && !door && p.roll(x, y, z, 81, 11) != 0) {
            p.put(x, y, z, (x == 0 || z == 0) && y < 6 ? rib : shell);
          } else if (d <= 6.3) {
            p.put(x, y, z, Blocks.AIR.defaultBlockState());
          }
        }
      }
    }

    Block plant =
        spore
            ? (Block) VoidspaceContent.SPORE_SPROUT.get()
            : (Block) VoidspaceContent.CRYSTAL_CLUSTER.get();

    for (int i = 0; i < 14; i++) {
      int x = -5 + p.roll(i, 0, 0, 83, 11);
      int z = -5 + p.roll(i, 0, 1, 85, 11);
      if (!(Math.hypot(x, z) > 5.4) && Math.max(Math.abs(x), Math.abs(z)) > 1) {
        p.put(x, 1, z, i % 4 == 0 ? (Block) VoidspaceContent.STAR_BLOOM.get() : plant);
      }
    }

    p.put(0, 1, 0, rib);
    p.loot(0, 2, 0, Blocks.CHEST, "chests/voidspace_grotto");
    p.put(
        0,
        6,
        0,
        spore
            ? ((Block) VoidspaceContent.SPORE_CAP.get()).defaultBlockState()
            : ((Block) VoidspaceContent.CRYSTAL_BLOCK.get()).defaultBlockState());
  }

  public static int worldX(VoidspaceGen.Site s, int lx, int lz) {
    return s.x()
        + switch (s.rot()) {
          case 1 -> -lz;
          case 2 -> -lx;
          case 3 -> lz;
          default -> lx;
        };
  }

  public static int worldZ(VoidspaceGen.Site s, int lx, int lz) {
    return s.z()
        + switch (s.rot()) {
          case 1 -> lx;
          case 2 -> -lz;
          case 3 -> -lx;
          default -> lz;
        };
  }

  private static BlockState stair(Block b, Direction facing) {
    return (BlockState) b.defaultBlockState().setValue(StairBlock.FACING, facing);
  }

  static void warbase(VoidspaceSites.P p) {
    Block brick = Blocks.POLISHED_BLACKSTONE_BRICKS;
    Block stone = Blocks.BLACKSTONE;
    Block floor = Blocks.POLISHED_BLACKSTONE;
    Block gild = Blocks.GILDED_BLACKSTONE;
    Block red = Blocks.RED_NETHER_BRICKS;
    BlockState air = Blocks.AIR.defaultBlockState();

    for (int x = -33; x <= 33; x++) {
      for (int z = -33; z <= 33; z++) {
        double d = Math.hypot(x, z);
        if (!(d > 33.5)) {
          boolean in = Math.abs(x) <= 24 && Math.abs(z) <= 24;
          p.put(
              x,
              0,
              z,
              in
                  ? (p.roll(x, 0, z, 301, 9) == 0 ? red : floor)
                  : (p.roll(x, 0, z, 303, 7) == 0 ? Blocks.MAGMA_BLOCK : stone));
        }
      }
    }

    for (int z = -2; z <= 33; z++) {
      for (int x = -2; x <= 2; x++) {
        if (Math.hypot(x, z) <= 33.5) {
          p.put(x, 0, z, Math.abs(x) == 2 ? gild : brick);
        }
      }
    }

    for (int x = -25; x <= 25; x++) {
      for (int z = -25; z <= 25; z++) {
        int m = Math.max(Math.abs(x), Math.abs(z));
        if (m >= 24) {
          boolean gate = z >= 24 && Math.abs(x) <= 2;

          for (int y = 1; y <= 8; y++) {
            if (!gate || y > 5) {
              p.put(
                  x,
                  y,
                  z,
                  y == 4
                      ? red
                      : (m == 24 && y == 6 && Math.floorMod(x + z, 8) == 0
                          ? Blocks.SHROOMLIGHT
                          : brick));
            }
          }

          if (m == 25 && Math.floorMod(x + z, 2) == 0 && !gate) {
            p.put(x, 9, z, brick);
          }
        }
      }
    }

    for (int x = -2; x <= 2; x++) {
      p.put(x, 5, 25, Blocks.IRON_BARS);
      p.put(x, 5, 24, Blocks.CHAIN);
    }

    for (int sx : new int[] {-5, 5}) {
      tower(p, sx, 25, 2.6, 12, false);
      p.put(sx, 13, 25, gild);
      p.put(sx, 14, 25, Blocks.CAMPFIRE);
    }

    for (int sx : new int[] {-25, 25}) {
      for (int sz : new int[] {-25, 25}) {
        tower(p, sx, sz, 4.2, 15, true);
      }
    }

    for (int x = -8; x <= 8; x++) {
      for (int z = -16; z <= -2; z++) {
        boolean edge = Math.abs(x) == 8 || z == -16 || z == -2;
        boolean corner = Math.abs(x) == 8 && (z == -16 || z == -2);
        p.put(x, 0, z, edge ? brick : red);

        for (int y = 1; y <= 15; y++) {
          if (!edge) {
            p.put(x, y, z, air);
          } else {
            boolean door = z == -2 && Math.abs(x) <= 1 && y <= 4;
            boolean slit =
                !corner
                    && (y == 8 || y == 9)
                    && (Math.abs(x) == 8 ? Math.floorMod(z, 4) == 0 : Math.floorMod(x, 4) == 0)
                    && z != -2;
            if (!door && !slit) {
              p.put(x, y, z, corner ? gild : (y != 5 && y != 11 ? brick : red));
            } else {
              p.put(x, y, z, air);
            }
          }
        }

        p.put(x, 16, z, brick);
        if (edge && Math.floorMod(x + z, 2) == 0) {
          p.put(x, 17, z, corner ? gild : brick);
        }
      }
    }

    for (int dx = -4; dx <= 4; dx++) {
      for (int dy = -4; dy <= 4; dy++) {
        double dd = Math.hypot(dx, dy);
        double a = Math.atan2(dy, dx);
        Block b =
            dd <= 1.5
                ? Blocks.SHROOMLIGHT
                : (dd <= 2.6
                    ? Blocks.GOLD_BLOCK
                    : (dd <= 4.4 && Math.abs(Math.sin(a * 4.0)) < 0.35
                        ? (dd > 3.5 ? Blocks.ORANGE_CONCRETE : Blocks.GOLD_BLOCK)
                        : null));
        if (b != null) {
          p.put(dx, 10 + dy, -2, b);
        }
      }
    }

    for (int x = -2; x <= 2; x++) {
      for (int z = -15; z <= -13; z++) {
        p.put(x, 1, z, gild);
      }
    }

    p.put(0, 2, -14, stair(Blocks.POLISHED_BLACKSTONE_STAIRS, Direction.NORTH));

    for (int y = 2; y <= 5; y++) {
      p.put(0, y, -15, y == 5 ? Blocks.SHROOMLIGHT : gild);
    }

    p.put(-1, 2, -14, gild);
    p.put(1, 2, -14, gild);

    for (int x = -2; x <= 2; x++) {
      p.put(x, 1, -8, x == 0 ? Blocks.CARTOGRAPHY_TABLE : Blocks.POLISHED_BLACKSTONE);
    }

    p.put(3, 1, -8, Blocks.LECTERN);

    for (int px : new int[] {-5, 5}) {
      for (int pz : new int[] {-12, -5}) {
        for (int y = 1; y <= 15; y++) {
          p.put(px, y, pz, y != 1 && y != 15 ? brick : gild);
        }
      }
    }

    for (int lx : new int[] {-3, 3}) {
      for (int lz : new int[] {-12, -5}) {
        p.put(lx, 15, lz, Blocks.CHAIN);
        p.put(lx, 14, lz, Blocks.CHAIN);
        p.put(
            lx,
            13,
            lz,
            (BlockState) Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
      }
    }

    for (int side : new int[] {-7, 7}) {
      for (int hz : new int[] {-13, -9, -5}) {
        for (int y = 6; y <= 11; y++) {
          p.put(side, y, hz, y == 6 ? Blocks.GOLD_BLOCK : Blocks.RED_WOOL);
        }
      }
    }

    p.loot(-6, 1, -15, Blocks.CHEST, "chests/dawnbringer_war_chest");
    p.loot(6, 1, -15, Blocks.CHEST, "chests/dawnbringer_war_chest");

    for (int y = 17; y <= 21; y++) {
      p.put(0, y, -9, Blocks.GOLD_BLOCK);
    }

    p.put(0, 22, -9, Blocks.SHROOMLIGHT);
    p.put(0, 23, -9, Blocks.END_ROD);

    for (Direction d : Plane.HORIZONTAL) {
      p.put(d.getStepX(), 21, -9 + d.getStepZ(), Blocks.GOLD_BLOCK);
      p.put(
          d.getStepX() * 2,
          21,
          -9 + d.getStepZ() * 2,
          (BlockState) Blocks.END_ROD.defaultBlockState().setValue(DirectionalBlock.FACING, d));
    }

    for (int x = -22; x <= -12; x++) {
      for (int z = -10; z <= 14; z++) {
        boolean edge = x == -22 || x == -12 || z == -10 || z == 14;
        p.put(x, 0, z, floor);

        for (int y = 1; y <= 5; y++) {
          boolean door = x == -12 && z >= 1 && z <= 3 && y <= 3;
          boolean win =
              edge && y == 3 && (x == -22 || z == -10 || z == 14) && Math.floorMod(x + z, 4) == 0;
          p.put(
              x,
              y,
              z,
              edge && !door
                  ? (win
                      ? Blocks.IRON_BARS.defaultBlockState()
                      : (y == 5 ? red : brick).defaultBlockState())
                  : air);
        }

        p.put(x, 6, z, red);
      }
    }

    for (int z = -8; z <= 12; z += 3) {
      for (int x : new int[] {-21, -13}) {
        p.put(x, 1, z, Blocks.RED_WOOL);
        p.put(x + (x < -17 ? 1 : -1), 1, z, Blocks.RED_WOOL);
      }
    }

    p.put(
        -17,
        4,
        -5,
        (BlockState) Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
    p.put(
        -17,
        4,
        7,
        (BlockState) Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
    p.loot(-21, 1, 13, Blocks.CHEST, "chests/dawnbringer_supplies");
    p.loot(-13, 1, -9, Blocks.BARREL, "chests/dawnbringer_supplies");

    for (int x = 12; x <= 22; x++) {
      for (int z = -8; z <= 8; z++) {
        p.put(x, 0, z, p.roll(x, 0, z, 307, 5) == 0 ? Blocks.MAGMA_BLOCK : floor);
        boolean post = (x == 12 || x == 22) && Math.floorMod(z, 4) == 0;

        for (int y = 1; y <= 5; y++) {
          if (post) {
            p.put(x, y, z, y == 1 ? gild : brick);
          }
        }

        p.put(x, 6, z, red);
      }
    }

    p.put(21, 1, -6, Blocks.BLAST_FURNACE);
    p.put(21, 1, -4, Blocks.SMITHING_TABLE);
    p.put(21, 1, -2, Blocks.ANVIL);
    p.put(21, 1, 2, Blocks.GRINDSTONE);
    p.put(20, 1, 6, Blocks.LAVA_CAULDRON);
    p.put(21, 1, 6, Blocks.LAVA_CAULDRON);
    p.put(15, 1, -7, Blocks.ANVIL);
    p.loot(21, 1, 4, Blocks.CHEST, "chests/dawnbringer_supplies");
    p.put(
        17,
        5,
        0,
        (BlockState) Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));

    for (int tx : new int[] {8, 12, 16}) {
      p.put(tx, 1, 19, Blocks.HAY_BLOCK);
      p.put(tx, 2, 19, Blocks.TARGET);
    }

    for (int x = 6; x <= 18; x++) {
      p.put(x, 1, 13, Blocks.POLISHED_BLACKSTONE_BRICK_WALL);
    }

    for (int bx : new int[] {-4, 4}) {
      for (int bz : new int[] {6, 12, 18}) {
        p.put(bx, 1, bz, gild);

        for (int y = 2; y <= 7; y++) {
          p.put(bx, y, bz, Blocks.IRON_BARS);
        }

        for (int y = 4; y <= 7; y++) {
          p.put(bx, y, bz + 1, y >= 5 ? Blocks.ORANGE_WOOL : Blocks.RED_WOOL);
          p.put(bx, y, bz + 2, Blocks.RED_WOOL);
        }
      }
    }

    for (int bx : new int[] {-4, 4}) {
      p.put(bx, 1, 22, gild);
      p.put(bx, 2, 22, Blocks.CAMPFIRE);
    }

    p.sign(4, 1, 30, 1, "SARC ALERT", "House Dawnbringer", "war base", "Hostile ground");
  }

  private static void tower(VoidspaceSites.P p, int cx, int cz, double r, int h, boolean hollow) {
    double toward = Math.atan2(-cz, -cx);
    int R = (int) Math.ceil(r) + 1;

    for (int x = -R; x <= R; x++) {
      for (int z = -R; z <= R; z++) {
        double d = Math.hypot(x, z);
        if (!(d > r + 1.0)) {
          if (d <= r) {
            p.put(cx + x, 0, cz + z, Blocks.POLISHED_BLACKSTONE_BRICKS);
            boolean ring = d > r - 1.2 || !hollow;
            double ang =
                Math.abs(
                    Math.atan2(
                        Math.sin(Math.atan2(z, x) - toward), Math.cos(Math.atan2(z, x) - toward)));

            for (int y = 1; y < h; y++) {
              if (!ring) {
                p.put(cx + x, y, cz + z, Blocks.AIR);
              } else {
                boolean door = hollow && ang < 0.32 && y <= 3;
                boolean slit = hollow && (y == 5 || y == 10) && (x == 0 || z == 0);
                if (!door && !slit) {
                  p.put(
                      cx + x,
                      y,
                      cz + z,
                      y % 5 == 0 ? Blocks.RED_NETHER_BRICKS : Blocks.POLISHED_BLACKSTONE_BRICKS);
                } else {
                  p.put(cx + x, y, cz + z, Blocks.AIR);
                }
              }
            }
          }

          p.put(cx + x, h, cz + z, Blocks.POLISHED_BLACKSTONE_BRICKS);
          if (d > r && Math.floorMod(x + z, 2) == 0) {
            p.put(cx + x, h + 1, cz + z, Blocks.POLISHED_BLACKSTONE_BRICKS);
          }
        }
      }
    }

    if (hollow) {
      int lz = cz < 0 ? -1 : 1;

      for (int y = 1; y <= h; y++) {
        p.put(
            cx,
            y,
            cz + lz * (int) Math.floor(r - 1.2),
            (BlockState)
                Blocks.LADDER
                    .defaultBlockState()
                    .setValue(LadderBlock.FACING, lz < 0 ? Direction.SOUTH : Direction.NORTH));
      }

      p.put(cx + 1, h + 1, cz + 1, Blocks.GILDED_BLACKSTONE);
      p.put(cx + 1, h + 2, cz + 1, Blocks.CAMPFIRE);

      for (int y = h + 1; y <= h + 5; y++) {
        p.put(cx - 1, y, cz - 1, Blocks.IRON_BARS);
      }

      for (int y = h + 3; y <= h + 5; y++) {
        p.put(cx - 2, y, cz - 1, Blocks.ORANGE_WOOL);
        p.put(cx - 3, y, cz - 1, y == h + 3 ? Blocks.RED_WOOL : Blocks.ORANGE_WOOL);
      }
    }
  }

  private VoidspaceSites() {}

  public interface Out {
    void block(int var1, int var2, int var3, BlockState var4);

    void loot(int var1, int var2, int var3, String var4);

    void screen(int var1, int var2, int var3, int var4, String... var5);
  }

  private record P(VoidspaceGen.Site s, VoidspaceSites.Out o) {
    int wx(int lx, int lz) {
      return this.s.x()
          + switch (this.s.rot()) {
            case 1 -> -lz;
            case 2 -> -lx;
            case 3 -> lz;
            default -> lx;
          };
    }

    int wz(int lx, int lz) {
      return this.s.z()
          + switch (this.s.rot()) {
            case 1 -> lx;
            case 2 -> -lz;
            case 3 -> -lx;
            default -> lz;
          };
    }

    BlockState r(BlockState b) {
      return b.rotate(Rotation.values()[this.s.rot()]);
    }

    void put(int x, int y, int z, BlockState b) {
      this.o.block(this.wx(x, z), this.s.y() + y, this.wz(x, z), this.r(b));
    }

    void put(int x, int y, int z, Block b) {
      this.put(x, y, z, b.defaultBlockState());
    }

    void loot(int x, int y, int z, Block box, String table) {
      this.put(x, y, z, box.defaultBlockState());
      this.o.loot(this.wx(x, z), this.s.y() + y, this.wz(x, z), table);
    }

    void sign(int x, int y, int z, int theme, String... lines) {
      this.put(x, y, z, MonitorContent.stand(Direction.SOUTH));
      this.o.screen(this.wx(x, z), this.s.y() + y, this.wz(x, z), theme, lines);
    }

    boolean gone(int x, int y, int z, int salt) {
      return this.s.ruined() && VoidspaceVillage.destroyed(this.wx(x, z), y, this.wz(x, z), salt);
    }

    long h(int x, int y, int z, int salt) {
      long v =
          this.wx(x, z) * 341873128712L
              ^ this.wz(x, z) * 132897987541L
              ^ y * 1664525L
              ^ salt * 22695477L
              ^ (long) this.s.x() << 20;
      v ^= v >>> 23;
      v *= 2388976653695081527L;
      return v ^ v >>> 47;
    }

    int roll(int x, int y, int z, int salt, int n) {
      return (int) Math.floorMod((long) this.h(x, y, z, salt), (long) n);
    }
  }
}
