package com.timelocked;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.storage.loot.LootTable;

public final class EclipticCourt {
  public static final int CX = 112;
  public static final int CZ = -96;
  public static final int R = 26;
  public static final ResourceKey<LootTable> VAULT =
      ResourceKey.create(
          Registries.LOOT_TABLE,
          ResourceLocation.fromNamespaceAndPath("timelocked", "chests/ecliptic_vault"));
  static final String[] ACCORD =
      new String[] {
        "THE ECLIPTIC ACCORD\n\nSworn beneath the crescent, between House Ecliptic of the Moon and House Darkfang of the far night.",
        "When Darkfang's sky burned, the Moon's lanterns stayed lit. What survived came to us, and we gave them a pavilion of moonstone to remember us by.",
        "Darkfang coin is Ecliptic coin. A Darkfang guest is an Ecliptic guest. Their enemies are our enemies.",
        "To any Earthling who reads this: the lanterns hold our air. Walk in their light, speak softly before the throne, and you are welcome.\n\n  Luna"
      };

  static BlockState st(Block b) {
    return b.defaultBlockState();
  }

  static int ground(ServerLevel l, int x, int z) {
    return Math.max(24, Math.min(200, l.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1));
  }

  public static BlockPos build(ServerLevel l, int padY) {
    for (int cx = 5; cx <= 9; cx++) {
      for (int cz = -8; cz <= -4; cz++) {
        l.getChunk(cx, cz);
      }
    }

    TimeskipperShip.Sink o = TimeskipperShip.of(l);
    int Y = ground(l, 112, -96);
    BlockState rock = st((Block) SpaceContent.LUNAR_ROCK.get());
    BlockState tile = st(Blocks.DEEPSLATE_TILES);
    BlockState pol = st(Blocks.POLISHED_DEEPSLATE);
    BlockState brick = st(Blocks.DEEPSLATE_BRICKS);
    BlockState calc = st(Blocks.CALCITE);
    BlockState pink = st(Blocks.PINK_CONCRETE);
    BlockState blue = st(Blocks.BLUE_TERRACOTTA);
    BlockState glass = st(Blocks.LIGHT_BLUE_STAINED_GLASS);
    BlockState pglass = st(Blocks.PINK_STAINED_GLASS);
    BlockState quartz = st(Blocks.QUARTZ_BLOCK);
    BlockState air = st(Blocks.AIR);
    BlockState lantern = st((Block) SpaceContent.MOONBREATH.get());
    double gx = -112.0;
    double gz = 96.0;
    double gl = Math.sqrt(gx * gx + gz * gz);
    gx /= gl;
    gz /= gl;

    for (int dx = -29; dx <= 29; dx++) {
      for (int dz = -29; dz <= 29; dz++) {
        double d = Math.sqrt(dx * dx + dz * dz);
        if (!(d > 29.5)) {
          int x = 112 + dx;
          int z = -96 + dz;

          for (int y = Y - 6; y < Y; y++) {
            o.set(x, y, z, rock);
          }

          BlockState f =
              d < 3.5
                  ? quartz
                  : ((!(d >= 10.5) || !(d < 11.5)) && (!(d >= 20.0) || !(d < 21.0))
                      ? (d < 25.0 ? (Math.floorMod(dx + dz, 2) == 0 ? tile : pol) : brick)
                      : pink);
          o.set(x, Y, z, d <= 26.0 ? f : rock);

          for (int y = Y + 1; y <= Y + 26 + 2; y++) {
            double s = Math.sqrt(dx * dx + (y - Y) * (y - Y) * 1.0 + dz * dz);
            if (d > 26.5) {
              if (y <= Y + 3 && d <= 29.0) {
                o.set(x, y, z, air);
              }
            } else if (s >= 25.5 && s < 26.5) {
              double a = Math.toDegrees(Math.atan2(dz, dx));
              boolean rib = Math.abs((a % 45.0 + 45.0) % 45.0 - 22.5) > 20.5;
              boolean gate = (dx * gx + dz * gz) / Math.max(d, 1.0) > 0.96 && y <= Y + 5;
              o.set(
                  x,
                  y,
                  z,
                  gate
                      ? air
                      : (y <= Y + 3 ? brick : (rib ? pol : ((y - Y) % 7 == 0 ? pglass : glass))));
            } else if (s < 25.5) {
              o.set(x, y, z, air);
            }
          }
        }
      }
    }

    for (int i = 0; i < 6; i++) {
      double a = (Math.PI / 3) * i + 0.3;
      int x = 112 + (int) Math.round(Math.cos(a) * 14.0);
      int z = -96 + (int) Math.round(Math.sin(a) * 14.0);

      for (int h = 1; h <= 3; h++) {
        o.set(x, Y + h, z, pol);
      }

      o.set(x, Y + 4, z, lantern);
    }

    keep(o, l, Y, brick, blue, pink, quartz, calc, air, lantern, tile);
    BlockPos[] huts = new BlockPos[4];

    for (int i = 0; i < 4; i++) {
      double a = (Math.PI / 2) * i + (Math.PI / 4);
      int x = 112 + (int) Math.round(Math.cos(a) * 18.0);
      int z = -96 + (int) Math.round(Math.sin(a) * 18.0);
      huts[i] = new BlockPos(x, Y + 1, z);
      cottage(o, x, Y, z, calc, blue, air, lantern, i);
    }

    path(o, l, padY, gx, gz, lantern, pol);
    BlockPos throne = new BlockPos(112, Y + 1, -99);
    spawn(l, 5, throne, 3);

    for (int i = 0; i < 4; i++) {
      double back = i < 2 ? 4.0 : 9.0;
      double side = i % 2 == 0 ? -3.0 : 3.0;
      int x = 112 + (int) Math.round(gx * (26.0 - back) - gz * side);
      int z = -96 + (int) Math.round(gz * (26.0 - back) + gx * side);
      spawn(l, 3, new BlockPos(x, Y + 1, z), 8);
    }

    for (int i = 0; i < 3; i++) {
      spawn(l, 4, huts[i], 6);
    }

    return throne;
  }

  static void keep(
      TimeskipperShip.Sink o,
      ServerLevel l,
      int Y,
      BlockState brick,
      BlockState blue,
      BlockState pink,
      BlockState quartz,
      BlockState calc,
      BlockState air,
      BlockState lantern,
      BlockState tile) {
    int X = 112;
    int Z = -96;

    for (int dx = -6; dx <= 6; dx++) {
      for (int dz = -6; dz <= 6; dz++) {
        boolean edge = Math.abs(dx) == 6 || Math.abs(dz) == 6;
        boolean corner = Math.abs(dx) == 6 && Math.abs(dz) == 6;
        o.set(X + dx, Y, Z + dz, quartz);

        for (int y = 1; y <= 10; y++) {
          o.set(X + dx, Y + y, Z + dz, edge ? (corner ? tile : (y % 5 == 0 ? brick : blue)) : air);
        }

        int roof = 11 + Math.max(0, 5 - Math.max(Math.abs(dx), Math.abs(dz)));
        o.set(X + dx, Y + 11, Z + dz, tile);
        if (Math.max(Math.abs(dx), Math.abs(dz)) <= 4) {
          for (int y = 12; y <= roof; y++) {
            o.set(X + dx, Y + y, Z + dz, y == roof ? tile : brick);
          }
        }
      }
    }

    for (int y = 17; y <= 22; y++) {
      o.set(X, Y + y, Z, y == 22 ? lantern : st(Blocks.POLISHED_DEEPSLATE_WALL));
    }

    for (int y = 1; y <= 4; y++) {
      for (int dx = -1; dx <= 1; dx++) {
        o.set(X + dx, Y + y, Z + 6, air);
      }
    }

    for (int y = 6; y <= 8; y++) {
      for (int k : new int[] {-3, 3}) {
        o.set(X + k, Y + y, Z - 6, st(Blocks.PINK_STAINED_GLASS));
        o.set(X - 6, Y + y, Z + k, st(Blocks.LIGHT_BLUE_STAINED_GLASS));
        o.set(X + 6, Y + y, Z + k, st(Blocks.LIGHT_BLUE_STAINED_GLASS));
      }
    }

    for (int dx = -4; dx <= 4; dx++) {
      for (int y = 5; y <= 10; y++) {
        double a = Math.hypot(dx, y - 7.5);
        double b = Math.hypot(dx - 1.6, y - 8.1);
        if (a <= 3.2 && b > 2.6) {
          o.set(X + dx, Y + y, Z + 6, pink);
        }
      }
    }

    for (int dz = -3; dz <= 5; dz++) {
      o.set(X, Y + 1, Z + dz, st(Blocks.PINK_CARPET));
    }

    o.set(
        X,
        Y + 1,
        Z - 4,
        (BlockState) st(Blocks.QUARTZ_STAIRS).setValue(StairBlock.FACING, Direction.NORTH));
    o.set(X - 1, Y + 1, Z - 4, st(Blocks.QUARTZ_PILLAR));
    o.set(X + 1, Y + 1, Z - 4, st(Blocks.QUARTZ_PILLAR));

    for (int y = 2; y <= 4; y++) {
      o.set(X, Y + y, Z - 5, y == 4 ? pink : st(Blocks.QUARTZ_PILLAR));
    }

    o.set(X - 1, Y + 4, Z - 5, pink);
    o.set(X + 1, Y + 5, Z - 5, pink);

    for (int[] c : new int[][] {{-5, -5}, {5, -5}, {-5, 5}, {5, 5}}) {
      o.set(X + c[0], Y + 1, Z + c[1], st(Blocks.POLISHED_DEEPSLATE));
      o.set(X + c[0], Y + 2, Z + c[1], lantern);
    }

    o.barrel(
        X - 4,
        Y + 1,
        Z - 5,
        VAULT,
        SarcOutposts.book("The Ecliptic Accord", "Luna, Ruler of House Ecliptic", ACCORD));
    o.barrel(X + 4, Y + 1, Z - 5, VAULT, null);
    o.screen(
        X + 3,
        Y + 1,
        Z + 5,
        Direction.SOUTH,
        3,
        "HOUSE ECLIPTIC",
        "COURT OF LUNA",
        "FRIENDS OF",
        "HOUSE DARKFANG");
  }

  static void cottage(
      TimeskipperShip.Sink o,
      int X,
      int Y,
      int Z,
      BlockState calc,
      BlockState blue,
      BlockState air,
      BlockState lantern,
      int i) {
    for (int dx = -3; dx <= 3; dx++) {
      for (int dz = -3; dz <= 3; dz++) {
        double d = Math.hypot(dx, dz);
        if (!(d > 3.4)) {
          for (int y = 1; y <= 5; y++) {
            double cap = 3.4 - (y - 4) * 1.2;
            BlockState s =
                y <= 3 ? (d > 2.5 ? calc : air) : (d <= cap ? (d > cap - 1.2 ? blue : air) : null);
            if (s != null) {
              o.set(X + dx, Y + y, Z + dz, s);
            }
          }
        }
      }
    }

    int sx = Integer.signum(112 - X);
    int sz = Integer.signum(-96 - Z);

    for (int y = 1; y <= 2; y++) {
      o.set(X + sx * 3, Y + y, Z, air);
      o.set(X + sx * 2, Y + y, Z, air);
    }

    o.set(X, Y + 1, Z - sz, lantern);
    o.set(
        X - sx,
        Y + 1,
        Z + sz,
        (BlockState)
            ((BlockState) st(Blocks.PINK_BED).setValue(BedBlock.FACING, Direction.NORTH))
                .setValue(BedBlock.PART, BedPart.HEAD));
    if (i == 0) {
      o.barrel(X + sx, Y + 1, Z + sz * 2, VAULT, null);
    }
  }

  static void path(
      TimeskipperShip.Sink o,
      ServerLevel l,
      int padY,
      double gx,
      double gz,
      BlockState lantern,
      BlockState post) {
    double gl = Math.sqrt(21760.0) - 26.0 - 2.0;

    for (double t = 14.0; t < gl; t += 14.0) {
      int x = (int) Math.round(112.0 + gx * (28.0 + gl - t));
      int z = (int) Math.round(-96.0 + gz * (28.0 + gl - t));
      l.getChunk(x >> 4, z >> 4);
      int y = ground(l, x, z);
      o.set(x, y + 1, z, post);
      o.set(x, y + 2, z, post);
      o.set(x, y + 3, z, lantern);
    }
  }

  static void spawn(ServerLevel l, int kind, BlockPos at, int leash) {
    CelestialVillager e =
        (CelestialVillager) ((EntityType) VoidspaceContent.CELESTIAL.get()).create(l);
    if (e != null) {
      e.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 180.0F, 0.0F);
      e.finalizeSpawn(l, l.getCurrentDifficultyAt(at), MobSpawnType.STRUCTURE, null);
      e.setKind(kind);
      e.setPersistenceRequired();
      e.restrictTo(at, leash);
      l.addFreshEntity(e);
    }
  }

  private EclipticCourt() {}
}
