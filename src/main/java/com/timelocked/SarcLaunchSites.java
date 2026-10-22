package com.timelocked;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.storage.loot.LootTable;

public final class SarcLaunchSites extends Feature<NoneFeatureConfiguration> {
  public static final int CELL = 1536;
  private static final Map<String, Optional<BlockPos>> PLANS = new ConcurrentHashMap<>();
  static final String[] BRIEF =
      new String[] {
        "LAUNCH BRIEF\n\nVehicle: TIMESKIPPER. Drive: chronotite skip drive. Crew: whoever is left.",
        "Boarding: starboard hatch, via the gantry arm. Touch the beacon in the hatch.",
        "Nav: bridge console, far forward. Drive: aft, feed it chronotite. Each skip costs charges and a little of Earth's time.",
        "Out there: wear the full suit. Stay near life support. Do not fall off Halcyon."
      };

  public SarcLaunchSites() {
    super(NoneFeatureConfiguration.CODEC);
  }

  private static long mix(long n) {
    n = (n ^ n >>> 30) * -4658895280553007687L;
    n = (n ^ n >>> 27) * -7723592293110705685L;
    return n ^ n >>> 31;
  }

  public static BlockPos horizontal(long seed, int rx, int rz) {
    long n = mix(seed * 17L + rx * 918271L + rz * 571129L + 6668L);
    return new BlockPos(
        rx * 1536 + 256 + Math.floorMod(n, 1024), 0, rz * 1536 + 256 + Math.floorMod(mix(n), 1024));
  }

  public static Optional<BlockPos> plan(ServerLevel level, int rx, int rz) {
    long seed = level.getSeed();
    return PLANS.computeIfAbsent(
        seed + ":" + rx + ":" + rz,
        k -> {
          BlockPos h = horizontal(seed, rx, rz);
          if (SarcOutposts.nearFacility(seed, h.getX(), h.getZ())) {
            return Optional.empty();
          }

          Optional<SarcOutposts.Outpost> near = SarcOutposts.at(level, h);
          if (near.isPresent()
              && near.get()
                      .origin()
                      .distManhattan(new Vec3i(h.getX(), near.get().origin().getY(), h.getZ()))
                  < 260) {
            return Optional.empty();
          }

          ChunkGenerator gen = level.getChunkSource().getGenerator();
          RandomState rs = level.getChunkSource().randomState();
          int min = Integer.MAX_VALUE;
          int max = Integer.MIN_VALUE;
          int sum = 0;
          int n = 0;

          for (int dx = -48; dx <= 48; dx += 48) {
            for (int dz = -56; dz <= 56; dz += 56) {
              int x = h.getX() + dx;
              int z = h.getZ() + dz;
              int top = gen.getBaseHeight(x, z, Types.WORLD_SURFACE_WG, level, rs);
              int floor = gen.getBaseHeight(x, z, Types.OCEAN_FLOOR_WG, level, rs);
              if (floor < top - 1 || top <= gen.getSeaLevel() + 1) {
                return Optional.empty();
              }

              min = Math.min(min, top);
              max = Math.max(max, top);
              sum += top;
              n++;
            }
          }

          return max - min > 16
              ? Optional.empty()
              : Optional.of(new BlockPos(h.getX(), Math.round((float) sum / n) - 1, h.getZ()));
        });
  }

  public static Optional<BlockPos> nearest(ServerLevel level, BlockPos at) {
    int rx = Math.floorDiv(at.getX(), 1536);
    int rz = Math.floorDiv(at.getZ(), 1536);
    BlockPos best = null;
    double bd = Double.MAX_VALUE;

    for (int r = 0; r <= 4 && best == null; r++) {
      for (int a = rx - r; a <= rx + r; a++) {
        for (int b = rz - r; b <= rz + r; b++) {
          if (Math.max(Math.abs(a - rx), Math.abs(b - rz)) == r) {
            Optional<BlockPos> p = plan(level, a, b);
            if (p.isPresent()) {
              double d = p.get().distSqr(at);
              if (d < bd) {
                bd = d;
                best = p.get();
              }
            }
          }
        }
      }
    }

    return Optional.ofNullable(best);
  }

  public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> c) {
    WorldGenLevel r = c.level();
    if (!r.getLevel().dimension().equals(Level.OVERWORLD)) {
      return false;
    }

    int cx = c.origin().getX() & -16;
    int cz = c.origin().getZ() & -16;
    int rx = Math.floorDiv(cx, 1536);
    int rz = Math.floorDiv(cz, 1536);
    BlockPos h = horizontal(r.getSeed(), rx, rz);
    if (cx + 15 >= h.getX() - 48
        && cx <= h.getX() + 48
        && cz + 15 >= h.getZ() - 56
        && cz <= h.getZ() + 56) {
      Optional<BlockPos> plan = plan(r.getLevel(), rx, rz);
      if (plan.isEmpty()) {
        return false;
      }

      if (!LaunchSiteState.get(r.getLevel()).admit(r.getLevel(), plan.get())) return false;
      SarcOutposts.Out o = new SarcOutposts.Out(r, cx, cz, r.getSeed() ^ h.asLong());
      build(o, plan.get().getX(), plan.get().getY(), plan.get().getZ());
      return o.changed;
    } else {
      return false;
    }
  }

  static BlockState st(Block b) {
    return b.defaultBlockState();
  }

  static TimeskipperShip.Sink sink(final SarcOutposts.Out o) {
    return new TimeskipperShip.Sink() {
      @Override
      public void set(int x, int y, int z, BlockState s) {
        o.set(x, y, z, s);
      }

      @Override
      public void barrel(int x, int y, int z, ResourceKey<LootTable> loot, ItemStack extra) {
        o.barrel(x, y, z, loot, extra);
      }

      @Override
      public void screen(int x, int y, int z, Direction face, int theme, String... lines) {
        o.standMonitor(x, y, z, face, theme, lines);
      }
    };
  }

  static void build(SarcOutposts.Out o, int X, int Y, int Z) {
    LaunchSite082.build(o, X, Y, Z);
  }

  static void legacyBuild(SarcOutposts.Out o, int X, int Y, int Z) {
    TimeskipperShip.Sink s = sink(o);
    BlockState gc = st(Blocks.GRAY_CONCRETE);
    BlockState lgc = st(Blocks.LIGHT_GRAY_CONCRETE);
    BlockState bars = st(Blocks.IRON_BARS);
    BlockState iron = st(Blocks.IRON_BLOCK);
    BlockState steel = st((Block) MetalContent.STEEL_BRICKS.get());
    BlockState grate = st((Block) MetalContent.GRATING.get());
    SarcOutposts.prep(o, X, Y, Z, 30, 42, 36, st(Blocks.STONE));

    for (int dx = -29; dx <= 29; dx++) {
      for (int dz = -41; dz <= 41; dz++) {
        o.set(
            X + dx, Y, Z + dz, Math.floorMod(dx, 10) != 0 && Math.floorMod(dz, 10) != 0 ? gc : lgc);
        if ((Math.abs(dx) == 29 || Math.abs(dz) == 41) && (dz != 41 || Math.abs(dx) > 3)) {
          o.set(X + dx, Y + 1, Z + dz, bars);
          o.set(X + dx, Y + 2, Z + dz, bars);
        }
      }
    }

    for (int dz = -28; dz <= 24; dz++) {
      for (int dx = -9; dx <= 9; dx++) {
        if (Math.abs(dx) >= 8) {
          o.set(X + dx, Y, Z + dz, st((Block) Content.HAZARD.get()));
        }
      }
    }

    for (int dz = -20; dz <= 16; dz += 6) {
      for (int dx = -3; dx <= 3; dx += 6) {
        o.set(X + dx, Y + 1, Z + dz, iron);
      }
    }

    TimeskipperShip.hull(s, X, Y + 4, Z, false);
    o.set(X + 7, Y + 5, Z - 2, st((Block) SpaceContent.BEACON.get()));
    o.set(X + 7, Y + 6, Z - 2, st((Block) Content.HAZARD.get()));

    for (int dx = 8; dx <= 12; dx++) {
      for (int dz = -4; dz <= 0; dz++) {
        o.set(X + dx, Y + 4, Z + dz, grate);
      }
    }

    for (int dx = 8; dx <= 12; dx++) {
      for (int dz = -3; dz <= 0; dz++) {
        for (int y = 5; y <= 7; y++) {
          o.set(X + dx, Y + y, Z + dz, st(Blocks.AIR));
        }
      }
    }

    for (int dx = 8; dx <= 12; dx++) {
      o.set(X + dx, Y + 5, Z - 4, bars);
      if (dx > 8 && dx != 10) {
        o.set(X + dx, Y + 5, Z, bars);
      }
    }

    for (int i = 0; i < 4; i++) {
      o.set(
          X + 10,
          Y + 1 + i,
          Z + 4 - i,
          (BlockState) st(Blocks.STONE_BRICK_STAIRS).setValue(StairBlock.FACING, Direction.NORTH));

      for (int y = 1; y < 1 + i; y++) {
        o.set(X + 10, Y + y, Z + 4 - i, steel);
      }
    }

    for (int dx = 8; dx <= 12; dx += 4) {
      for (int y = 1; y <= 3; y++) {
        o.set(X + dx, Y + y, Z - 4, steel);
      }
    }

    for (int y = 1; y <= 30; y++) {
      for (int dx = 13; dx <= 16; dx++) {
        for (int dz = -6; dz <= -3; dz++) {
          boolean corner = (dx == 13 || dx == 16) && (dz == -6 || dz == -3);
          boolean side = dx == 13 || dx == 16 || dz == -6 || dz == -3;
          if (corner) {
            o.set(X + dx, Y + y, Z + dz, steel);
          } else if (side && y % 5 == 0) {
            o.set(X + dx, Y + y, Z + dz, iron);
          } else if (side && (dx + dz + y) % 2 == 0) {
            o.set(X + dx, Y + y, Z + dz, bars);
          }
        }
      }
    }

    o.set(X + 14, Y + 31, Z - 5, st(Blocks.LIGHTNING_ROD));
    o.set(X + 15, Y + 31, Z - 4, st((Block) Content.LIGHT.get()));

    for (int dx = 4; dx <= 12; dx++) {
      o.set(X + dx, Y + 16, Z - 5, steel);
    }

    for (int y = 12; y <= 15; y++) {
      o.set(X + 5, Y + y, Z - 5, st(Blocks.CHAIN));
    }

    for (int[] t : new int[][] {{-21, -30}, {-21, -20}}) {
      for (int y = 1; y <= 11; y++) {
        for (int a = -3; a <= 3; a++) {
          for (int b = -3; b <= 3; b++) {
            double d = Math.hypot(a, b);
            if (!(d > 3.2)) {
              o.set(
                  X + t[0] + a,
                  Y + y,
                  Z + t[1] + b,
                  !(d > 2.3) && y != 11
                      ? st(Blocks.AIR)
                      : (y % 4 == 0 ? st(Blocks.ORANGE_CONCRETE) : st(Blocks.WHITE_CONCRETE)));
            }
          }
        }
      }
    }

    for (int dx = -24; dx <= -14; dx++) {
      for (int dz = 22; dz <= 32; dz++) {
        boolean edge = dx == -24 || dx == -14 || dz == 22 || dz == 32;
        o.set(X + dx, Y, Z + dz, st((Block) Content.FLOOR.get()));
        o.set(X + dx, Y + 5, Z + dz, st((Block) Content.SHELL.get()));

        for (int y = 1; y <= 4; y++) {
          o.set(
              X + dx,
              Y + y,
              Z + dz,
              edge
                  ? (dz == 22 && y == 3 && dx % 2 == 0
                      ? st(Blocks.GLASS)
                      : (dx == -14 && (dz == 27 || dz == 28) && y <= 2
                          ? st(Blocks.AIR)
                          : st((Block) Content.WALL.get())))
                  : st(Blocks.AIR));
        }
      }
    }

    for (int dx = -22; dx <= -16; dx += 3) {
      o.set(
          X + dx,
          Y + 1,
          Z + 23,
          TimeskipperShip.face((Block) FacilityDetails.WORKSTATION.get(), Direction.SOUTH));
    }

    o.set(X - 19, Y + 4, Z + 27, st((Block) Content.LIGHT.get()));
    s.screen(
        X - 21,
        Y + 1,
        Z + 25,
        Direction.NORTH,
        0,
        "TIMESKIPPER",
        "STATUS: DOCKED",
        "DRIVE: COLD",
        "AWAITING CREW");
    s.screen(
        X - 17,
        Y + 1,
        Z + 25,
        Direction.NORTH,
        1,
        "BOARDING",
        "STARBOARD HATCH",
        "VIA GANTRY ARM",
        "");
    o.barrel(
        X - 23,
        Y + 1,
        Z + 31,
        TimeskipperShip.SUPPLY,
        SarcOutposts.book("TIMESKIPPER Launch Brief", "SARC Off-World Division", BRIEF));
    o.barrel(X - 22, Y + 1, Z + 31, TimeskipperShip.SUPPLY, null);
    o.mob((EntityType) SarcContent.SCIENTIST.get(), X - 19, Y + 1, Z + 29, 1, 8);

    for (int[] p : new int[][] {{-27, -38}, {27, -38}, {-27, 38}, {27, 38}, {20, 10}, {-20, 10}}) {
      for (int y = 1; y <= 3; y++) {
        o.set(X + p[0], Y + y, Z + p[1], st(Blocks.ANDESITE_WALL));
      }

      o.set(X + p[0], Y + 4, Z + p[1], st(Blocks.LANTERN));
    }
  }
}
