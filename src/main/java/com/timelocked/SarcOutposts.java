package com.timelocked;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LightningRodBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.storage.loot.LootTable;

public final class SarcOutposts extends Feature<NoneFeatureConfiguration> {
  public static final int CELL = 1024;
  public static final int POWER = 0;
  public static final int LAB = 1;
  public static final int LAB_DEPTH = 20;
  public static final ResourceKey<LootTable> POWER_LOOT = key("chests/sarc_power_plant");
  public static final ResourceKey<LootTable> LAB_LOOT = key("chests/sarc_research_lab");
  private static final Map<String, Optional<SarcOutposts.Outpost>> PLANS =
      new ConcurrentHashMap<>();
  static final String[] POWER_LOG =
      new String[] {
        "PLANT LOG\n\nDay 1. Grid stable. Output nominal. The Zero Point site wants more than we can give.",
        "Day 9. Corporate rerouted all output underground. Our turbines now feed something we are not cleared to see.",
        "Day 14. Load spiked to triple capacity for eleven seconds. The lights in town went out. Nobody called.",
        "Day 20. Output is zero. Turbine 2 is spinning anyway. There is no steam.",
        "Day 21. I am leaving the barrel unlocked for whoever comes next. Do not restore the grid connection."
      };
  static final String[] LAB_LOG =
      new String[] {
        "SUBJECT INTAKE\n\nSubjects 001 to 012: volunteers, paid, informed. Exposure: 30 seconds at the observation window.",
        "Subjects 013 to 040: reassigned staff. Consent forms on file. Some of them, anyway.",
        "Observed: perfect recall, no sleep, light sensitivity. Subjects describe a bright city beyond the field.",
        "Observed: subjects stop answering to their names. They answer to something else.",
        "Recommendation: halt intake. Recommendation denied."
      };
  static final String[] LAB_FINAL =
      new String[] {
        "FINAL ENTRY\n\nThe wing was sealed from the outside at 0300. Our badges no longer open anything.",
        "The subjects are calm in daylight and terrible in the dark. Keep the lights on.",
        "If you are reading this, the door upstairs opened for you. I hope that means the lockdown ended.",
        "And not that they wanted a visitor."
      };

  public SarcOutposts() {
    super(NoneFeatureConfiguration.CODEC);
  }

  private static ResourceKey<LootTable> key(String p) {
    return ResourceKey.create(
        Registries.LOOT_TABLE, ResourceLocation.fromNamespaceAndPath("timelocked", p));
  }

  private static long mix(long n) {
    n = (n ^ n >>> 30) * -4658895280553007687L;
    n = (n ^ n >>> 27) * -7723592293110705685L;
    return n ^ n >>> 31;
  }

  public static BlockPos horizontal(long seed, int rx, int rz) {
    long n = mix(seed * 31L + rx * 734287L + rz * 912931L + 23116L);
    return new BlockPos(
        rx * 1024 + 176 + Math.floorMod(n, 672), 0, rz * 1024 + 176 + Math.floorMod(mix(n), 640));
  }

  public static int kind(long seed, int rx, int rz) {
    return Math.floorMod(mix(seed ^ rx * 48271L + rz * 16807L ^ 119L), 2);
  }

  static boolean nearFacility(long seed, int x, int z) {
    int fx = Math.floorDiv(x, 4096);
    int fz = Math.floorDiv(z, 4096);

    for (int a = fx - 1; a <= fx + 1; a++) {
      for (int b = fz - 1; b <= fz + 1; b++) {
        BlockPos h = SitePlanner.horizontal(seed, a, b);
        if (x > h.getX() - 760 && x < h.getX() + 940 && z > h.getZ() - 760 && z < h.getZ() + 2960) {
          return true;
        }
      }
    }

    return false;
  }

  public static Optional<SarcOutposts.Outpost> plan(ServerLevel level, int rx, int rz) {
    long seed = level.getSeed();
    return PLANS.computeIfAbsent(
        seed + ":" + rx + ":" + rz,
        k -> {
          BlockPos h = horizontal(seed, rx, rz);
          int kind = kind(seed, rx, rz);
          if (nearFacility(seed, h.getX(), h.getZ())) {
            return Optional.empty();
          }

          ChunkGenerator gen = level.getChunkSource().getGenerator();
          RandomState rs = level.getChunkSource().randomState();
          int span = kind == 0 ? 32 : 13;
          int min = Integer.MAX_VALUE;
          int max = Integer.MIN_VALUE;
          int sum = 0;
          int n = 0;

          for (int dx = -span; dx <= span; dx += span) {
            for (int dz = -span; dz <= span; dz += span) {
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

          if (max - min > (kind == 0 ? 14 : 10)) {
            return Optional.empty();
          }

          int y = Math.round((float) sum / n) - 1;
          BlockPos origin = new BlockPos(h.getX(), y, h.getZ());
          // Locating and placing must agree on the underground station terrain checks.
          if (kind == POWER && !PowerStationLayout.suitable(level, origin)) return Optional.empty();
          return kind == 1 && y - 20 - 2 <= level.getMinBuildHeight() + 4
              ? Optional.empty()
              : Optional.of(new SarcOutposts.Outpost(kind, new BlockPos(h.getX(), y, h.getZ())));
        });
  }

  public static Optional<SarcOutposts.Outpost> at(ServerLevel level, BlockPos p) {
    return plan(level, Math.floorDiv(p.getX(), 1024), Math.floorDiv(p.getZ(), 1024));
  }

  public static boolean inside(SarcOutposts.Outpost o, BlockPos p) {
    int dx = p.getX() - o.origin().getX();
    int dz = p.getZ() - o.origin().getZ();
    int y = p.getY();
    return o.kind() == 0
        ? ((Math.abs(dx) <= 36 && Math.abs(dz) <= 36 && Math.abs(y - o.origin().getY()) < 24)
            || (Math.abs(dx) <= 64
                && dz >= -80
                && dz <= 96
                && y >= o.origin().getY() - 64
                && y <= o.origin().getY() + 8))
        : Math.abs(dx) <= 17 && dz <= -19 && dz >= -64 && y >= o.floor() && y <= o.floor() + 6;
  }

  public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> c) {
    WorldGenLevel r = c.level();
    if (!r.getLevel().dimension().equals(Level.OVERWORLD)) {
      return false;
    }

    int cx = c.origin().getX() & -16;
    int cz = c.origin().getZ() & -16;
    int rx = Math.floorDiv(cx, 1024);
    int rz = Math.floorDiv(cz, 1024);
    BlockPos h = horizontal(r.getSeed(), rx, rz);
    if (cx + 15 >= h.getX() - 64
        && cx <= h.getX() + 64
        && cz + 15 >= h.getZ() - 80
        && cz <= h.getZ() + 150) {
      Optional<SarcOutposts.Outpost> plan = plan(r.getLevel(), rx, rz);
      if (plan.isEmpty()) {
        return false;
      }

      SarcOutposts.Out o = new SarcOutposts.Out(r, cx, cz, r.getSeed() ^ h.asLong());
      BlockPos at = plan.get().origin();
      if (plan.get().kind() == 0) {
        // The normal feature pipeline writes only this newly generating chunk.
        // Do not veto a whole site because a neighbouring feature pass completed first.
        // Retain pre-upgrade denials: never partially backfill a known rejected site.
        if (PowerStationState.get(r.getLevel()).wasDenied(at)) return false;
        PowerStationLayout.place(o, at);
      } else {
        lab(o, at.getX(), at.getY(), at.getZ());
      }

      return o.changed;
    } else {
      return false;
    }
  }

  private static BlockState st(Block b) {
    return b.defaultBlockState();
  }

  private static BlockState facing(Block b, Direction d) {
    BlockState s = b.defaultBlockState();
    if (s.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
      s = (BlockState) s.setValue(BlockStateProperties.HORIZONTAL_FACING, d);
    }

    return s;
  }

  static void prep(
      SarcOutposts.Out o, int X, int Y, int Z, int rx, int rz, int clear, BlockState fill) {
    for (int x = X - rx; x <= X + rx; x++) {
      for (int z = Z - rz; z <= Z + rz; z++) {
        if (o.in(x, z)) {
          int top = o.top(x, z);

          for (int y = Math.min(top, Y - 1) - 3; y < Y; y++) {
            if (y > top - 1 || o.r.getBlockState(new BlockPos(x, y, z)).canBeReplaced()) {
              o.set(x, y, z, y >= Y - 1 ? Blocks.DIRT.defaultBlockState() : fill);
            }
          }

          for (int y = Y + 1; y <= Math.max(top, Y) + clear; y++) {
            o.air(x, y, z);
          }
        }
      }
    }
  }

  public static int coreKind(SarcOutposts.Out o, int X, int Y, int Z) {
    return Math.floorMod(o.rnd(X, Y + 1, Z), 4);
  }

  public static boolean unstable(SarcOutposts.Out o, int X, int Y, int Z) {
    return Math.floorMod(o.rnd(X, Y + 2, Z), 3) == 0;
  }

  static void power(SarcOutposts.Out o, int X, int Y, int Z) {
    BlockState gc = st(Blocks.GRAY_CONCRETE);
    BlockState lgc = st(Blocks.LIGHT_GRAY_CONCRETE);
    BlockState wc = st(Blocks.WHITE_CONCRETE);
    BlockState bars = st(Blocks.IRON_BARS);
    BlockState wall = st((Block) Content.WALL.get());
    BlockState floor = st((Block) Content.FLOOR.get());
    BlockState shell = st((Block) Content.SHELL.get());
    BlockState glass = st(Blocks.GLASS);
    BlockState iron = st(Blocks.IRON_BLOCK);
    BlockState light = st((Block) Content.LIGHT.get());
    prep(o, X, Y, Z, 37, 37, 40, st(Blocks.STONE));
    int core = coreKind(o, X, Y, Z);
    boolean unstable = unstable(o, X, Y, Z);

    for (int dx = -36; dx <= 36; dx++) {
      for (int dz = -36; dz <= 36; dz++) {
        o.set(X + dx, Y, Z + dz, Math.floorMod(dx, 8) != 0 && Math.floorMod(dz, 8) != 0 ? gc : lgc);
        if ((Math.abs(dx) == 36 || Math.abs(dz) == 36) && (dz != 36 || Math.abs(dx) > 3)) {
          o.set(X + dx, Y + 1, Z + dz, bars);
          o.set(X + dx, Y + 2, Z + dz, bars);
        }
      }
    }

    for (int tx : new int[] {-17, 17}) {
      int tz = -17;

      for (int hgt = 0; hgt < 32; hgt++) {
        double t = hgt / 31.0;
        double rad =
            t < 0.7
                ? 6.5 + 2.5 * Math.pow((0.7 - t) / 0.7, 2.0)
                : 6.5 + 0.6 * Math.pow((t - 0.7) / 0.3, 2.0);

        for (int dx = -10; dx <= 10; dx++) {
          for (int dz = -10; dz <= 10; dz++) {
            double d = Math.sqrt(dx * dx + dz * dz);
            if (!(Math.abs(d - rad) >= 0.6)) {
              int sector = (int) Math.floor((Math.atan2(dz, dx) + Math.PI) / (Math.PI / 8));
              if (hgt >= 3 || sector % 2 != 0) {
                o.set(X + tx + dx, Y + 1 + hgt, Z + tz + dz, hgt != 24 && hgt != 25 ? wc : lgc);
              }
            }
          }
        }
      }

      for (int[] s : new int[][] {{-3, -3}, {3, -3}, {-3, 3}, {3, 3}, {0, 0}}) {
        o.set(X + tx + s[0], Y, Z + tz + s[1], st(Blocks.HAY_BLOCK));
        o.set(
            X + tx + s[0],
            Y + 1,
            Z + tz + s[1],
            (BlockState) st(Blocks.CAMPFIRE).setValue(CampfireBlock.SIGNAL_FIRE, true));
      }
    }

    for (int dx = -20; dx <= 20; dx++) {
      for (int dz = 4; dz <= 22; dz++) {
        boolean edge = Math.abs(dx) == 20 || dz == 4 || dz == 22;
        o.set(X + dx, Y, Z + dz, floor);
        o.set(X + dx, Y + 13, Z + dz, shell);
        if (!edge) {
          if (Math.floorMod(dx, 6) == 0 && Math.floorMod(dz - 4, 6) == 0) {
            o.set(X + dx, Y + 12, Z + dz, light);
          }
        } else {
          for (int y = 1; y <= 12; y++) {
            boolean window =
                (dz == 4 || dz == 22) && (y == 8 || y == 9) && Math.floorMod(dx, 4) != 0;
            boolean doorway = Math.abs(dx) <= 1 && y <= 4;
            o.set(
                X + dx,
                Y + y,
                Z + dz,
                doorway ? Blocks.AIR.defaultBlockState() : (window ? glass : wall));
          }
        }
      }
    }

    for (int tz : new int[] {9, 17}) {
      for (int x = -15; x <= 14; x++) {
        for (int a = -1; a <= 1; a++) {
          for (int b = -1; b <= 1; b++) {
            if (Math.abs(a) != 1 || Math.abs(b) != 1) {
              o.set(
                  X + x,
                  Y + 3 + a,
                  Z + tz + b,
                  x >= 10
                      ? st(Blocks.WAXED_COPPER_BLOCK)
                      : (Math.floorMod(x, 6) == 0 ? st(Blocks.LIGHT_BLUE_CONCRETE) : iron));
            }
          }
        }

        if (Math.floorMod(x, 5) == 0) {
          o.set(X + x, Y + 1, Z + tz, st(Blocks.SMOOTH_STONE));
        }
      }

      for (int x = 15; x <= 19; x++) {
        o.set(X + x, Y + 1, Z + tz, st((Block) Content.CONDUIT.get()));
      }
    }

    for (int dz = 5; dz <= 21; dz++) {
      o.set(X - 8, Y + 11, Z + dz, iron);
      o.set(X + 8, Y + 11, Z + dz, iron);
    }

    for (int dx = -7; dx <= 7; dx++) {
      o.set(X + dx, Y + 11, Z + 13, st(Blocks.SMOOTH_STONE_SLAB));
    }

    for (int y = 8; y <= 10; y++) {
      o.set(X, Y + y, Z + 13, st(Blocks.CHAIN));
    }

    o.set(X, Y + 10, Z + 23, facing((Block) Content.LOGO.get(), Direction.SOUTH));
    o.set(X, Y + 10, Z + 3, facing((Block) Content.LOGO.get(), Direction.NORTH));
    BlockState vglass =
        st(
            new Block[] {
                  Blocks.LIME_STAINED_GLASS,
                  Blocks.ORANGE_STAINED_GLASS,
                  Blocks.LIGHT_BLUE_STAINED_GLASS,
                  Blocks.PURPLE_STAINED_GLASS
                }
                [core]);
    BlockState hazard = st((Block) Content.HAZARD.get());

    for (int dx = -6; dx <= 6; dx++) {
      for (int dz = -30; dz <= -6; dz++) {
        boolean edge = Math.abs(dx) == 6 || dz == -30 || dz == -6;
        o.set(X + dx, Y, Z + dz, floor);
        o.set(X + dx, Y + 12, Z + dz, shell);

        for (int y = 1; y <= 11; y++) {
          if (edge) {
            o.set(
                X + dx,
                Y + y,
                Z + dz,
                dz == -6 && Math.abs(dx) <= 1 && y <= 3
                    ? Blocks.AIR.defaultBlockState()
                    : (y == 6 ? hazard : wall));
          } else {
            o.air(X + dx, Y + y, Z + dz);
          }
        }

        if (!edge && Math.floorMod(dx, 4) == 0 && Math.floorMod(dz, 5) == 0) {
          o.set(X + dx, Y + 11, Z + dz, light);
        }
      }
    }

    int vz = Z - 20;

    for (int dx = -3; dx <= 3; dx++) {
      for (int dz = -3; dz <= 3; dz++) {
        if (Math.max(Math.abs(dx), Math.abs(dz)) == 3) {
          o.set(X + dx, Y, vz + dz, hazard);
        }
      }
    }

    o.core(X, Y + 1, vz, core, unstable);
    o.set(X + 3, Y + 1, Z - 17, st((Block) Content.CONDUIT.get()));

    for (int dz = -17; dz <= -7; dz++) {
      o.set(X + 4, Y + 1, Z + dz, st((Block) Content.CONDUIT.get()));
    }

    o.set(X, Y + 1, Z - 14, facing((Block) SarcContent.CONSOLE.get(), Direction.SOUTH));
    o.standMonitor(
        X - 2,
        Y + 1,
        Z - 14,
        Direction.SOUTH,
        unstable ? 2 : 0,
        PlantCoreBlock.NAMES[core] + " CORE",
        unstable ? "COOLANT PUMPS FAILING" : "OFFLINE - NEEDS FUEL");
    o.standMonitor(
        X + 2,
        Y + 1,
        Z - 14,
        Direction.SOUTH,
        1,
        "CONSOLE",
        "CLICK: WITHDRAW ROD",
        "SNEAK+CLICK: SCRAM",
        "FUEL: " + PlantCoreBlock.FUELS[core].replace('_', ' ').toUpperCase(Locale.ROOT));
    o.mob((EntityType) SarcContent.SCIENTIST.get(), X + 3, Y + 1, Z - 10, 1, 5);

    for (int dx = 24; dx <= 34; dx++) {
      for (int dz = 4; dz <= 16; dz++) {
        boolean edge = dx == 24 || dx == 34 || dz == 4 || dz == 16;
        o.set(X + dx, Y, Z + dz, floor);
        o.set(X + dx, Y + 6, Z + dz, shell);
        if (edge) {
          for (int y = 1; y <= 5; y++) {
            boolean window = dz == 4 && (y == 3 || y == 4) && dx >= 26 && dx <= 32;
            boolean doorway = dx == 24 && (dz == 9 || dz == 10) && y <= 2;
            o.set(
                X + dx,
                Y + y,
                Z + dz,
                doorway ? Blocks.AIR.defaultBlockState() : (window ? glass : wall));
          }
        }
      }
    }

    for (int dx = 26; dx <= 32; dx += 2) {
      o.set(
          X + dx, Y + 1, Z + 5, facing((Block) FacilityDetails.WORKSTATION.get(), Direction.SOUTH));
    }

    o.set(X + 29, Y + 5, Z + 10, light);
    if (unstable) {
      o.monitor(
          X + 33,
          Y + 3,
          Z + 7,
          Direction.WEST,
          2,
          "COOLANT PUMPS FAILING",
          "CORE TEMP RISING",
          "SCRAM AT CORE CONSOLE");
    } else {
      o.monitor(
          X + 33, Y + 3, Z + 7, Direction.WEST, 1, "GRID OUTPUT", "0.0 MW", "TURBINES OFFLINE");
    }

    o.monitor(
        X + 33,
        Y + 3,
        Z + 10,
        Direction.WEST,
        0,
        "LOAD ROUTED TO",
        "ZERO POINT SITE",
        "DO NOT RESTORE");
    o.monitor(X + 33, Y + 3, Z + 13, Direction.WEST, 2, "TURBINE 2", "SPINNING", "NO STEAM INPUT");
    o.set(X + 28, Y + 1, Z + 13, facing(Blocks.LECTERN, Direction.SOUTH));
    o.barrel(
        X + 33,
        Y + 1,
        Z + 15,
        POWER_LOOT,
        book("Plant Supervisor's Log", "Supervisor R. Okafor", POWER_LOG));
    o.barrel(X + 32, Y + 1, Z + 15, POWER_LOOT, null);
    o.mob((EntityType) SarcContent.SCIENTIST.get(), X + 29, Y + 1, Z + 9, 1, 12);
    o.mob((EntityType) SarcContent.SCIENTIST.get(), X + 31, Y + 1, Z + 12, 0, 12);

    for (int dx = -34; dx <= -24; dx++) {
      for (int dz = 6; dz <= 30; dz++) {
        o.set(X + dx, Y, Z + dz, st(Blocks.GRAVEL));
        if (dx == -23) {}
      }
    }

    for (int dz = 6; dz <= 30; dz++) {
      if (dz < 17 || dz > 18) {
        o.set(X - 23, Y + 1, Z + dz, bars);
        o.set(X - 23, Y + 2, Z + dz, bars);
      }
    }

    for (int tx : new int[] {-32, -28}) {
      for (int tz : new int[] {9, 15, 21, 27}) {
        for (int a = 0; a <= 1; a++) {
          for (int b = 0; b <= 1; b++) {
            o.set(X + tx + a, Y + 1, Z + tz + b, iron);
            o.set(X + tx + a, Y + 2, Z + tz + b, a == b ? iron : st(Blocks.LIGHT_GRAY_CONCRETE));
          }
        }

        o.set(X + tx, Y + 3, Z + tz, st(Blocks.LIGHTNING_ROD));
        o.set(X + tx + 1, Y + 3, Z + tz + 1, st(Blocks.LIGHTNING_ROD));
      }
    }

    for (int dx = 26; dx <= 28; dx++) {
      o.barrel(X + dx, Y + 1, Z + 22, POWER_LOOT, null);
    }

    for (int dx = 30; dx <= 33; dx++) {
      for (int y = 1; y <= (dx % 2 == 0 ? 2 : 1); y++) {
        o.set(X + dx, Y + y, Z + 28, st((Block) MetalContent.STEEL_BRICKS.get()));
      }
    }

    o.set(X + 25, Y + 1, Z + 30, st((Block) Content.CABINET.get()));
    o.set(X + 26, Y + 1, Z + 30, st((Block) Content.CABINET.get()));

    for (int[] p :
        new int[][] {{-30, -30}, {30, -30}, {-30, 34}, {30, 34}, {-6, 30}, {6, 30}, {0, -34}}) {
      for (int y = 1; y <= 3; y++) {
        o.set(X + p[0], Y + y, Z + p[1], st(Blocks.ANDESITE_WALL));
      }

      o.set(X + p[0], Y + 4, Z + p[1], st(Blocks.LANTERN));
    }

    for (int dz = 37; dz <= 150; dz++) {
      for (int dx = -2; dx <= 2; dx++) {
        int x = X + dx;
        int z = Z + dz;
        if (o.in(x, z)) {
          int t = o.top(x, z);
          o.set(x, t, z, Math.abs(dx) == 2 ? lgc : gc);

          for (int y = t + 1; y <= t + 4; y++) {
            o.air(x, y, z);
          }
        }
      }
    }

    int px = (X + 16 >> 4 << 4) + 8;

    for (int k = 0; k < 4; k++) {
      int pz = (Z + 52 + 28 * k >> 4 << 4) + 8;
      if (o.in(px, pz)) {
        int g = o.top(px, pz);

        for (int a = -1; a <= 1; a += 2) {
          for (int b = -1; b <= 1; b += 2) {
            for (int y = 1; y <= 11; y++) {
              o.set(px + a, g + y, pz + b, bars);
            }
          }
        }

        for (int a = -1; a <= 1; a++) {
          for (int b = -1; b <= 1; b++) {
            o.set(px + a, g + 12, pz + b, st(Blocks.SMOOTH_STONE_SLAB));
          }
        }

        for (int y = 13; y <= 16; y++) {
          o.set(px, g + y, pz, bars);
        }

        for (int a = -4; a <= 4; a++) {
          if (a != 0) {
            o.set(
                px + a,
                g + 16,
                pz,
                (BlockState) st(Blocks.CHAIN).setValue(RotatedPillarBlock.AXIS, Axis.X));
          }
        }

        o.set(
            px - 4,
            g + 15,
            pz,
            (BlockState)
                st(Blocks.LIGHTNING_ROD).setValue(LightningRodBlock.FACING, Direction.DOWN));
        o.set(
            px + 4,
            g + 15,
            pz,
            (BlockState)
                st(Blocks.LIGHTNING_ROD).setValue(LightningRodBlock.FACING, Direction.DOWN));
      }
    }
  }

  static void lab(SarcOutposts.Out o, int X, int Y, int Z) {
    BlockState wall = st(FacilityArchitecture.CLEAN.get());
    BlockState floor = st((Block) Content.FLOOR.get());
    BlockState shell = st((Block) Content.SHELL.get());
    BlockState bars = st(Blocks.IRON_BARS);
    BlockState glass = st(Blocks.GLASS);
    BlockState light = st((Block) Content.LIGHT.get());
    BlockState flicker = st((Block) LabContent.FLICKER.get());
    BlockState air = st(Blocks.AIR);
    int F = Y - 20;
    prep(o, X, Y, Z, 14, 14, 14, st(Blocks.STONE));

    for (int dx = -14; dx <= 14; dx++) {
      for (int dz = -14; dz <= 14; dz++) {
        o.set(
            X + dx,
            Y,
            Z + dz,
            dz > 4 && Math.abs(dx) <= 1 ? st(Blocks.GRAY_CONCRETE) : st(Blocks.GRAVEL));
        if ((Math.abs(dx) == 14 || Math.abs(dz) == 14) && (dz != 14 || Math.abs(dx) > 1)) {
          o.set(X + dx, Y + 1, Z + dz, bars);
          o.set(X + dx, Y + 2, Z + dz, bars);
        }
      }
    }

    for (int dx = -6; dx <= 6; dx++) {
      for (int dz = -6; dz <= 6; dz++) {
        boolean edge = Math.abs(dx) == 6 || Math.abs(dz) == 6;
        o.set(X + dx, Y, Z + dz, floor);
        o.set(X + dx, Y + 6, Z + dz, shell);
        if (edge) {
          for (int y = 1; y <= 5; y++) {
            o.set(X + dx, Y + y, Z + dz, dz == 6 && Math.abs(dx) <= 1 && y <= 3 ? air : wall);
          }
        }
      }
    }

    o.set(X, Y + 4, Z + 7, facing((Block) Content.LOGO.get(), Direction.SOUTH));
    o.set(X + 3, Y + 1, Z + 3, facing((Block) FacilityDetails.WORKSTATION.get(), Direction.NORTH));
    for (int x : new int[] {-3, 3})
      o.set(X + x, Y + 5, Z + 2, SarcContent.STATION_LIGHT.get().defaultBlockState());
    for (int x : new int[] {-4, 4})
      for (int y = 2; y <= 3; y++) o.set(X + x, Y + y, Z + 6, st(Blocks.CYAN_STAINED_GLASS));
    o.monitor(
        X + 3,
        Y + 3,
        Z - 5,
        Direction.SOUTH,
        2,
        "SITE-B RESEARCH WING",
        "LOCKDOWN ACTIVE",
        "DO NOT DESCEND");

    for (int i = 0; i <= 19; i++) {
      int y = Y - i;
      int z = Z + 1 - i;

      for (int x = -1; x <= 1; x++) {
        o.set(X + x, y, z, facing((Block) Content.STAIRS.get(), Direction.SOUTH));
        o.set(X + x, y - 1, z, floor);

        for (int a = 1; a <= 4; a++) {
          o.air(X + x, y + a, z);
        }

        if (y + 5 <= Y) {
          o.set(X + x, y + 5, z, shell);
        }
      }

      for (int a = -1; a <= 5; a++) {
        if (y + a <= Y) {
          o.set(X - 2, y + a, z, wall);
          o.set(X + 2, y + a, z, wall);
        }
      }

      if (i % 4 == 2 && y + 4 < Y) {
        o.set(X, y + 4, z, light);
      }
    }

    for (int dx = -17; dx <= 17; dx++) {
      for (int dz = -64; dz <= -19; dz++) {
        boolean landing = dz == -19;
        if (!landing || Math.abs(dx) <= 2) {
          boolean edge =
              Math.abs(dx) == 17
                  || dz == -64
                  || landing && Math.abs(dx) == 2
                  || dz == -20 && Math.abs(dx) > 1;
          o.set(X + dx, F, Z + dz, floor);
          o.set(X + dx, F + 5, Z + dz, shell);

          for (int y = 1; y <= 4; y++) {
            if (!edge || dz == -20 && Math.abs(dx) <= 1) {
              o.set(X + dx, F + y, Z + dz, air);
            } else {
              o.set(X + dx, F + y, Z + dz, wall);
            }
          }
        }
      }
    }

    int[] rooms = new int[] {-26, -36, -46, -58};
    int[] splits = new int[] {-31, -41, -51};

    for (int dz = -63; dz <= -21; dz++) {
      for (int side : new int[] {-3, 3}) {
        boolean door = false;

        for (int m : rooms) {
          if (dz == m || dz == m + 1) {
            door = true;
          }
        }

        for (int y = 1; y <= 4; y++) {
          o.set(
              X + side,
              F + y,
              Z + dz,
              door && y <= 2 ? air : (y == 3 ? st(FacilityArchitecture.CONTAINMENT.get()) : wall));
        }
      }
    }

    for (int s : splits) {
      for (int dx = 4; dx <= 16; dx++) {
        for (int y = 1; y <= 4; y++) {
          o.set(X + dx, F + y, Z + s, wall);
          o.set(X - dx, F + y, Z + s, wall);
        }
      }
    }

    for (int dz = -62; dz <= -21; dz += 5) {
      o.set(X, F + 4, Z + dz, flicker);
    }

    for (int dz = -63; dz <= -21; dz += 3) {
      o.set(X - 2, F, Z + dz, st((Block) FacilityDetails.EMERGENCY.get()));
      o.set(X + 2, F, Z + dz, st((Block) FacilityDetails.EMERGENCY.get()));
    }

    for (int m : rooms) {
      o.set(X - 10, F + 4, Z + m, light);
      o.set(X + 10, F + 4, Z + m, light);
    }

    o.set(X - 3, F + 1, Z - 25, wall);
    o.set(X - 3, F + 2, Z - 25, wall);
    o.door(X - 3, F + 1, Z - 26, Direction.EAST);

    for (int dz = -30; dz <= -21; dz++) {
      for (int y = 1; y <= 3; y++) {
        o.set(X - 16, F + y, Z + dz, st(Blocks.BOOKSHELF));
      }
    }

    o.set(X - 10, F + 1, Z - 24, facing(Blocks.LECTERN, Direction.EAST));
    o.set(X - 6, F + 1, Z - 29, facing((Block) FacilityDetails.WORKSTATION.get(), Direction.EAST));
    o.set(X - 7, F + 1, Z - 29, facing((Block) FacilityDetails.WORKSTATION.get(), Direction.EAST));
    o.barrel(X - 15, F + 1, Z - 22, LAB_LOOT, book("Subject Intake Log", "Dr. M. Varga", LAB_LOG));
    o.barrel(X - 14, F + 1, Z - 22, LAB_LOOT, null);
    o.barrel(X - 15, F + 1, Z - 30, LAB_LOOT, book("Final Entry", "Dr. E. Lindqvist", LAB_FINAL));

    for (int[] p : new int[][] {{-12, -27}, {-8, -23}, {-13, -25}}) {
      o.set(X + p[0], F + 1, Z + p[1], facing((Block) LabContent.PAPERS.get(), Direction.NORTH));
    }

    o.mob((EntityType) SarcContent.SCIENTIST.get(), X - 10, F + 1, Z - 27, 2, 6);

    for (int room = 0; room < 2; room++) {
      int z0 = room == 0 ? -40 : -50;

      for (int cell = 0; cell < 2; cell++) {
        int a = z0 + cell * 5;
        int b = a + 3;

        for (int dz = a; dz <= b; dz++) {
          boolean broken = room == 0 && cell == 1 && dz >= a + 1 && dz <= a + 2;

          for (int y = 1; y <= 3; y++) {
            o.set(X - 11, F + y, Z + dz, broken ? air : bars);
          }
        }

        if (cell == 0) {
          for (int dx = -16; dx <= -11; dx++) {
            for (int y = 1; y <= 4; y++) {
              o.set(X + dx, F + y, Z + b + 1, wall);
            }
          }
        }

        o.bed(X - 15, F + 1, Z + a + 1, Direction.WEST);
        o.set(X - 13, F + 1, Z + b, facing((Block) LabContent.CLAW.get(), Direction.EAST));
      }

      o.mob((EntityType) SarcContent.SUBJECT.get(), X - 14, F + 1, Z + z0 + 2, -1, 4);
    }

    o.set(X - 9, F + 1, Z - 34, facing((Block) LabContent.PUDDLE.get(), Direction.NORTH));
    o.set(X - 6, F + 1, Z - 35, facing((Block) LabContent.PUDDLE.get(), Direction.NORTH));

    for (int dx = -16; dx <= -4; dx++) {
      for (int dz = -63; dz <= -52; dz++) {
        o.set(
            X + dx,
            F,
            Z + dz,
            Math.abs(dx + 10) != 6 && dz != -63 && dz != -52
                ? st((Block) Content.CASING.get())
                : st((Block) Content.HAZARD.get()));
      }
    }

    for (int dz = -62; dz <= -53; dz++) {
      for (int y = 2; y <= 3; y++) {
        if (dz != -58 && dz != -57) {
          o.set(X - 3, F + y, Z + dz, glass);
        }
      }
    }

    for (int y = 1; y <= 3; y++) {
      o.air(X - 3, F + y, Z - 58);
      o.air(X - 3, F + y, Z - 57);
    }

    o.tank(X - 14, F + 1, Z - 61, true);
    o.tank(X - 14, F + 1, Z - 55, true);
    o.mob((EntityType) SarcContent.SUBJECT.get(), X - 10, F + 1, Z - 58, -1, 8);
    o.mob((EntityType) SarcContent.SUBJECT.get(), X - 12, F + 1, Z - 55, -1, 8);

    for (int dz = -60; dz <= -54; dz += 2) {
      o.set(X - 1, F + 1, Z + dz, facing((Block) LabContent.PUDDLE.get(), Direction.NORTH));
    }

    o.set(X + 6, F + 1, Z - 22, facing((Block) Content.MEDICAL.get(), Direction.SOUTH));
    o.set(X + 10, F + 1, Z - 22, facing((Block) Content.MEDICAL.get(), Direction.SOUTH));
    o.set(X + 5, F + 1, Z - 29, facing((Block) Content.DECON.get(), Direction.EAST));

    for (int dz = -28; dz <= -22; dz += 3) {
      o.bed(X + 14, F + 1, Z + dz, Direction.EAST);
    }

    o.barrel(X + 15, F + 1, Z - 30, LAB_LOOT, null);

    for (int dz = -37; dz <= -35; dz++) {
      o.set(X + 10, F + 1, Z + dz, st(Blocks.IRON_BLOCK));
      o.set(X + 10, F + 2, Z + dz, st(Blocks.WHITE_CARPET));
    }

    o.set(X + 10, F + 4, Z - 36, light);
    o.set(X + 13, F + 1, Z - 39, facing((Block) FacilityDetails.WORKSTATION.get(), Direction.WEST));
    o.tank(X + 15, F + 1, Z - 33, true);
    o.set(X + 8, F + 1, Z - 38, facing((Block) LabContent.PUDDLE.get(), Direction.NORTH));
    o.set(X + 12, F + 1, Z - 33, facing((Block) LabContent.CLAW.get(), Direction.NORTH));
    o.mob((EntityType) SarcContent.SUBJECT.get(), X + 7, F + 1, Z - 34, -1, 6);

    for (int tx : new int[] {6, 9, 12, 15}) {
      for (int tz : new int[] {-44, -48}) {
        o.tank(X + tx, F + 1, Z + tz, (tx + tz) % 3 == 0);
      }
    }

    o.mob((EntityType) SarcContent.SUBJECT.get(), X + 10, F + 1, Z - 46, -1, 6);

    for (int dz = -62; dz <= -53; dz++) {
      o.set(X + 16, F + 1, Z + dz, st((Block) Content.CABINET.get()));
      o.set(
          X + 16,
          F + 2,
          Z + dz,
          dz % 2 == 0 ? st((Block) Content.DISPLAY.get()) : st((Block) Content.CABINET.get()));
    }

    for (int dx = 7; dx <= 10; dx++) {
      for (int dz = -60; dz <= -58; dz++) {
        for (int y = 1; y <= 2; y++) {
          o.set(
              X + dx,
              F + y,
              Z + dz,
              y == 2 && dx % 2 == 0 ? st(Blocks.LIGHT_GRAY_CONCRETE) : st(Blocks.IRON_BLOCK));
        }
      }
    }

    for (int dx = 11; dx <= 15; dx++) {
      o.set(X + dx, F + 1, Z - 59, st((Block) Content.CONDUIT.get()));
    }

    o.barrel(X + 5, F + 1, Z - 62, LAB_LOOT, null);
    o.mob((EntityType) SarcContent.SUBJECT.get(), X, F + 1, Z - 45, -1, 16);
    o.mob((EntityType) SarcContent.SUBJECT.get(), X + 1, F + 1, Z - 55, -1, 16);
    // Decontamination checkpoint, with a clear three-wide central passage.
    for (int x : new int[] {-2, 2}) {
      o.set(X + x, F + 1, Z - 23, st(FacilityArchitecture.SERVICE.get()));
      o.set(X + x, F + 2, Z - 23, st(FacilityArchitecture.VENT.get()));
    }
    o.set(X, F + 4, Z - 23, SarcContent.STATION_LIGHT.get().defaultBlockState());
    for (int z = -62; z <= -21; z++) o.set(X, F, Z + z, st(FacilityArchitecture.ROUTE.get()));
    // A supported warning pair at the checkpoint, on the existing corridor bulkhead.
    o.set(X - 2, F + 3, Z - 24, facing(FacilityDetails.ALARM_SIREN.get(), Direction.EAST));
    o.set(X - 2, F + 4, Z - 24, facing(FacilityDetails.ALARM_BEACON.get(), Direction.EAST));
  }

  public static ItemStack book(String title, String author, String[] text) {
    ItemStack s = new ItemStack(Items.WRITTEN_BOOK);
    List<Filterable<Component>> pages = new ArrayList<>();

    for (String p : text) {
      pages.add(Filterable.passThrough(Component.literal(p)));
    }

    s.set(
        DataComponents.WRITTEN_BOOK_CONTENT,
        new WrittenBookContent(Filterable.passThrough(title), author, 0, pages, true));
    return s;
  }

  public static final class Out {
    final WorldGenLevel r;
    final int minX;
    final int minZ;
    final long seed;
    boolean changed;

    Out(WorldGenLevel r, int minX, int minZ, long seed) {
      this.r = r;
      this.minX = minX;
      this.minZ = minZ;
      this.seed = seed;
    }

    boolean in(int x, int z) {
      return x >= this.minX && x < this.minX + 16 && z >= this.minZ && z < this.minZ + 16;
    }

    void set(int x, int y, int z, BlockState s) {
      if (this.in(x, z) && y >= this.r.getMinBuildHeight() && y < this.r.getMaxBuildHeight()) {
        BlockPos p = new BlockPos(x, y, z);
        if (!this.r.getBlockState(p).is(Blocks.BEDROCK)) {
          this.r.setBlock(p, s, 2);
          this.changed = true;
        }
      }
    }

    void air(int x, int y, int z) {
      if (this.in(x, z) && !this.r.getBlockState(new BlockPos(x, y, z)).isAir()) {
        this.set(x, y, z, Blocks.AIR.defaultBlockState());
      }
    }

    int top(int x, int z) {
      return this.r.getHeight(Types.WORLD_SURFACE_WG, x, z) - 1;
    }

    long rnd(int x, int y, int z) {
      return SarcOutposts.mix(this.seed ^ x * 73856093L ^ y * 19349663L ^ z * 83492791L);
    }

    void barrel(int x, int y, int z, ResourceKey<LootTable> loot, ItemStack extra) {
      if (this.in(x, z)) {
        BlockPos p = new BlockPos(x, y, z);
        this.set(
            x,
            y,
            z,
            (BlockState)
                Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP));
        if (this.r.getBlockEntity(p) instanceof BarrelBlockEntity b) {
          b.setLootTable(loot, this.rnd(x, y, z));
          if (extra != null) {
            b.setItem(13, extra);
          }
        }
      }
    }

    void monitor(int x, int y, int z, Direction face, int theme, String... lines) {
      if (this.in(x, z)) {
        this.set(x, y, z, MonitorContent.wall(face));
        if (this.r.getBlockEntity(new BlockPos(x, y, z))
            instanceof MonitorContent.MonitorEntity m) {
          m.setTheme(theme);
          m.setLines(lines);
        }
      }
    }

    void core(int x, int y, int z, int kind, boolean unstable) {
      core(x, y, z, kind, unstable, false);
    }

    void industrialCore(int x, int y, int z, int kind, boolean unstable) {
      core(x, y, z, kind, unstable, true);
    }

    private void core(int x, int y, int z, int kind, boolean unstable, boolean industrial) {
      if (this.in(x, z)) {
        this.set(
            x,
            y,
            z,
            (BlockState)
                ((BlockState)
                        ((PlantCoreBlock) SarcContent.CORE.get())
                            .defaultBlockState()
                            .setValue(PlantCoreBlock.KIND, kind))
                    .setValue(PlantCoreBlock.LIT, unstable));
        if (this.r.getBlockEntity(new BlockPos(x, y, z)) instanceof PlantCoreBlock.Core c) {
          c.industrial = industrial;
          c.stationLayoutVersion = industrial ? PowerStationGeometry.PLAN.version() : 0;
          c.init(unstable);
        }
      }
    }

    void standMonitor(int x, int y, int z, Direction face, int theme, String... lines) {
      if (this.in(x, z)) {
        this.set(
            x,
            y,
            z,
            (BlockState)
                MonitorContent.wall(face).setValue(MonitorContent.MonitorBlock.STAND, true));
        if (this.r.getBlockEntity(new BlockPos(x, y, z))
            instanceof MonitorContent.MonitorEntity m) {
          m.setTheme(theme);
          m.setLines(lines);
        }
      }
    }

    void tank(int x, int y, int z, boolean broken) {
      BlockState s =
          ((ContainmentTankBlock) (broken ? LabContent.BROKEN_TANK : LabContent.TANK).get())
              .defaultBlockState();
      if (s.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
        this.set(
            x,
            y,
            z,
            (BlockState) s.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER));
        this.set(
            x,
            y + 1,
            z,
            (BlockState) s.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER));
      } else {
        this.set(x, y, z, s);
        this.set(x, y + 1, z, s);
      }
    }

    void door(int x, int y, int z, Direction facing) {
      BlockState d =
          (BlockState) Blocks.SPRUCE_DOOR.defaultBlockState().setValue(DoorBlock.FACING, facing);
      this.set(x, y, z, (BlockState) d.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
      this.set(x, y + 1, z, (BlockState) d.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
    }

    void bed(int x, int y, int z, Direction toward) {
      BlockState b =
          (BlockState) Blocks.WHITE_BED.defaultBlockState().setValue(BedBlock.FACING, toward);
      this.set(x, y, z, (BlockState) b.setValue(BedBlock.PART, BedPart.FOOT));
      this.set(
          x + toward.getStepX(),
          y,
          z + toward.getStepZ(),
          (BlockState) b.setValue(BedBlock.PART, BedPart.HEAD));
    }

    <T extends Mob> void mob(EntityType<T> type, int x, int y, int z, int kind, int leash) {
      if (this.in(x, z)) {
        T e = (T) type.create(this.r.getLevel());
        if (e != null) {
          BlockPos p = new BlockPos(x, y, z);
          e.moveTo(x + 0.5, y, z + 0.5, Math.floorMod(this.rnd(x, y, z), 360), 0.0F);
          e.finalizeSpawn(this.r, this.r.getCurrentDifficultyAt(p), MobSpawnType.STRUCTURE, null);
          if (e instanceof SarcScientist s && kind >= 0) {
            s.setKind(kind);
          }

          e.setPersistenceRequired();
          if (e instanceof PathfinderMob pm) {
            pm.restrictTo(p, leash);
          }

          this.r.addFreshEntityWithPassengers(e);
          this.changed = true;
        }
      }
    }
  }

  public record Outpost(int kind, BlockPos origin) {
    public int floor() {
      return this.origin.getY() - 20;
    }
  }
}
