package com.timelocked;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep.Carving;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.blending.Blender;

public final class PlanetChunkGenerator extends ChunkGenerator {
  public static final MapCodec<PlanetChunkGenerator> CODEC =
      RecordCodecBuilder.mapCodec(
          i ->
              i.group(
                      BiomeSource.CODEC
                          .fieldOf("biome_source")
                          .forGetter(ChunkGenerator::getBiomeSource),
                      Codec.STRING.fieldOf("planet").forGetter(g -> g.planet.id))
                  .apply(i, (b, p) -> new PlanetChunkGenerator(b, SpacePlanets.Planet.byId(p))));
  public static final int SEA = 62;
  public static final int ICE = 14;
  public final SpacePlanets.Planet planet;
  private static final Map<RandomState, Long> SEEDS =
      Collections.synchronizedMap(new WeakHashMap<>());
  private static Block[] oreCache;
  private static final String[][] ORES =
      new String[][] {
        new String[0],
        {"titanium", "aluminum", "silver"},
        {"titanium", "luminite", "iridite", "tin"},
        {"chromite", "ardanium", "nickel", "titanium"},
        {"platinum", "osmium", "iridite", "gravitite", "neutronium", "nickel"},
        {"cobaltite", "vibranite", "silver"},
        {"celestrium", "aetherium", "orithichalite", "luminite"}
      };
  private static final double[] ORE_CHANCE = new double[] {0.0, 0.04, 0.05, 0.05, 0.12, 0.05, 0.06};

  public PlanetChunkGenerator(BiomeSource source, SpacePlanets.Planet planet) {
    super(source);
    this.planet = planet;
  }

  protected MapCodec<? extends ChunkGenerator> codec() {
    return CODEC;
  }

  static long hash(long s, int x, int y, int z) {
    long h = s ^ x * -7046029254386353131L ^ y * -4417276706812531889L ^ z * 1609587929392839161L;
    h = (h ^ h >>> 31) * -4658895280553007687L;
    h = (h ^ h >>> 29) * -7723592293110705685L;
    return h ^ h >>> 32;
  }

  static double rnd(long s, int x, int y, int z) {
    return (hash(s, x, y, z) >>> 11) * 1.110223E-16F;
  }

  static double smooth(double t) {
    return t * t * (3.0 - 2.0 * t);
  }

  static double value2(long s, double x, double z) {
    int x0 = (int) Math.floor(x);
    int z0 = (int) Math.floor(z);
    double fx = smooth(x - x0);
    double fz = smooth(z - z0);
    double a = rnd(s, x0, 0, z0);
    double b = rnd(s, x0 + 1, 0, z0);
    double c = rnd(s, x0, 0, z0 + 1);
    double d = rnd(s, x0 + 1, 0, z0 + 1);
    return (a + (b - a) * fx + (c + (d - c) * fx - (a + (b - a) * fx)) * fz) * 2.0 - 1.0;
  }

  static double value3(long s, double x, double y, double z) {
    int x0 = (int) Math.floor(x);
    int y0 = (int) Math.floor(y);
    int z0 = (int) Math.floor(z);
    double fx = smooth(x - x0);
    double fy = smooth(y - y0);
    double fz = smooth(z - z0);
    double[] v = new double[2];

    for (int k = 0; k < 2; k++) {
      double a = rnd(s, x0, y0 + k, z0);
      double b = rnd(s, x0 + 1, y0 + k, z0);
      double c = rnd(s, x0, y0 + k, z0 + 1);
      double d = rnd(s, x0 + 1, y0 + k, z0 + 1);
      double ab = a + (b - a) * fx;
      double cd = c + (d - c) * fx;
      v[k] = ab + (cd - ab) * fz;
    }

    return (v[0] + (v[1] - v[0]) * fy) * 2.0 - 1.0;
  }

  static double fbm(long s, double x, double z, int oct) {
    double sum = 0.0;
    double amp = 1.0;
    double norm = 0.0;
    double f = 1.0;

    for (int i = 0; i < oct; i++) {
      sum += amp * value2(s + i * 1013L, x * f, z * f);
      norm += amp;
      amp *= 0.5;
      f *= 2.03;
    }

    return sum / norm;
  }

  long seed(RandomState r) {
    return SEEDS.computeIfAbsent(
        r,
        k ->
            k.getOrCreateRandomFactory(
                    ResourceLocation.fromNamespaceAndPath("timelocked", "planet_" + this.planet.id))
                .at(0, 0, 0)
                .nextLong());
  }

  static double craters(long s, int x, int z, int cell, double chance, double rmin, double rmax) {
    int cx = Math.floorDiv(x, cell);
    int cz = Math.floorDiv(z, cell);
    double h = 0.0;

    for (int a = cx - 1; a <= cx + 1; a++) {
      for (int b = cz - 1; b <= cz + 1; b++) {
        if (!(rnd(s, a, 1, b) > chance)) {
          double px = (a + 0.15 + 0.7 * rnd(s, a, 2, b)) * cell;
          double pz = (b + 0.15 + 0.7 * rnd(s, a, 3, b)) * cell;
          double r = rmin + Math.pow(rnd(s, a, 4, b), 2.0) * (rmax - rmin);
          double d = Math.hypot(x - px, z - pz);
          if (!(d > r * 1.8)) {
            double depth = r * 0.38;
            if (d < r) {
              h -= depth * (1.0 - d / r * (d / r));
            }

            double t = (d - r) / (r * 0.28);
            h += r * 0.13 * Math.exp(-t * t);
          }
        }
      }
    }

    return h;
  }

  public int height(long s, int x, int z) {
    switch (this.planet) {
      case LUNA:
        {
          double h =
              66.0
                  + 8.0 * fbm(s, x / 180.0, z / 180.0, 4)
                  + 3.0 * fbm(s + 7L, x / 40.0, z / 40.0, 3);
          double maria = fbm(s + 11L, x / 600.0, z / 600.0, 2);
          if (maria < -0.15) {
            h -= Math.min(6.0, (-0.15 - maria) * 30.0);
          }

          h += craters(s, x, z, 56, 0.7, 4.0, 22.0) + craters(s + 5L, x, z, 15, 0.5, 1.5, 5.0);
          return clamp(h);
        }
      case MARS:
        {
          double h = 70.0 + 14.0 * fbm(s, x / 260.0, z / 260.0, 5);
          double mesa = fbm(s + 3L, x / 150.0, z / 150.0, 3);
          if (mesa > 0.25) {
            double up = Math.min(14.0, (mesa - 0.25) * 60.0);
            h += Math.floor(up / 4.0) * 4.0 + up % 4.0 * 0.2;
          }

          double canyon = Math.abs(fbm(s + 5L, x / 420.0, z / 420.0, 3));
          if (canyon < 0.05) {
            h -= (1.0 - canyon / 0.05) * 26.0;
          }

          h +=
              craters(s + 9L, x, z, 90, 0.4, 6.0, 28.0) + 2.0 * fbm(s + 13L, x / 24.0, z / 24.0, 2);
          return clamp(h);
        }
      case EUROPA:
        {
          double h = 72.0 + 3.0 * fbm(s, x / 120.0, z / 120.0, 4);
          double rr = 1.0 - Math.abs(fbm(s + 9L, x / 90.0, z / 90.0, 3));
          if (rr > 0.93) {
            h += (rr - 0.93) * 60.0;
          }

          return clamp(h);
        }
      case AURELIA:
        {
          double h =
              70.0
                  + 18.0 * fbm(s, x / 220.0, z / 220.0, 5)
                  + 6.0 * fbm(s + 2L, x / 50.0, z / 50.0, 3);
          return clamp(h);
        }
      default:
        return -1;
    }
  }

  static int clamp(double h) {
    return (int) Math.max(12L, Math.min(220L, Math.round(h)));
  }

  boolean crack(long s, int x, int z) {
    return this.planet == SpacePlanets.Planet.EUROPA
        && Math.abs(fbm(s + 13L, x / 70.0, z / 70.0, 2)) < 0.018;
  }

  private static BlockState b(Block b) {
    return b.defaultBlockState();
  }

  BlockState ore(long s, int x, int y, int z) {
    String[] list = ORES[this.planet.ordinal()];
    if (list.length == 0) {
      return null;
    }

    int cx = x >> 2;
    int cy = y >> 2;
    int cz = z >> 2;
    if (!(rnd(s + 77L, cx, cy, cz) > ORE_CHANCE[this.planet.ordinal()])
        && !(rnd(s + 79L, x, y, z) > 0.45)) {
      String id = list[(int) (rnd(s + 78L, cx, cy, cz) * list.length)];
      if (id.equals("neutronium") && rnd(s + 80L, cx, cy, cz) > 0.2) {
        id = "nickel";
      }

      Block o =
          (Block)
              BuiltInRegistries.BLOCK.get(
                  ResourceLocation.fromNamespaceAndPath("timelocked", id + "_ore"));
      return o == Blocks.AIR ? null : b(o);
    } else {
      return null;
    }
  }

  BlockState state(long s, int x, int y, int z, int h) {
    if (y == 0) {
      return b(Blocks.BEDROCK);
    }

    switch (this.planet) {
      case LUNA:
        if (y > h) {
          return null;
        } else {
          int d = h - y;
          if (d < 3) {
            return b((Block) SpaceContent.REGOLITH.get());
          } else {
            BlockState o = this.ore(s, x, y, z);
            return o != null
                ? o
                : (d > 40 && rnd(s + 3L, x, y, z) < 0.3
                    ? b(Blocks.DEEPSLATE)
                    : b((Block) SpaceContent.LUNAR_ROCK.get()));
          }
        }
      case MARS:
        if (y > h) {
          return null;
        } else {
          int d = h - y;
          if (d < 2) {
            return b((Block) SpaceContent.MARS_SAND.get());
          } else if (d < 5) {
            return b((Block) SpaceContent.MARS_ROCK.get());
          } else {
            BlockState o = this.ore(s, x, y, z);
            if (o != null) {
              return o;
            }

            return (y + (int) (rnd(s + 4L, x >> 3, 0, z >> 3) * 3.0)) % 7 == 0
                ? b(Blocks.RED_TERRACOTTA)
                : b((Block) SpaceContent.MARS_ROCK.get());
          }
        }
      case EUROPA:
        int floor = 12 + (int) (4.0 * fbm(s + 21L, x / 40.0, z / 40.0, 2));
        int bottom = h - 14;
        if (y > h) {
          return null;
        } else if (y <= floor) {
          return y >= floor - 1 && rnd(s + 22L, x >> 2, 0, z >> 2) < 0.08
              ? b(Blocks.MAGMA_BLOCK)
              : b(Blocks.DEEPSLATE);
        } else if (y < bottom) {
          return b(Blocks.WATER);
        } else if (this.crack(s, x, z)) {
          return y < bottom + 3 ? b(Blocks.WATER) : null;
        } else if (y == h) {
          return b(Blocks.SNOW_BLOCK);
        } else {
          BlockState o = y < bottom + 4 ? null : this.ore(s, x, y, z);
          if (o != null) {
            return o;
          }

          return !(rnd(s + 23L, x, y, z) < 0.12) && y >= bottom + 2
              ? b(Blocks.PACKED_ICE)
              : b(Blocks.BLUE_ICE);
        }
      case AURELIA:
        if (y > h) {
          return y <= 62 ? b(Blocks.WATER) : null;
        } else {
          int d = h - y;
          if (d == 0) {
            return h <= 63
                ? b((Block) Content.CELESTIAL_SILT.get())
                : (rnd(s + 5L, x >> 1, 0, z >> 1) < 0.65
                    ? b((Block) VoidspaceContent.GLIMMER_MOSS.get())
                    : b((Block) Content.ALIEN_LOAM.get()));
          } else if (d < 4) {
            return b((Block) Content.ALIEN_LOAM.get());
          } else {
            BlockState o = this.ore(s, x, y, z);
            if (o != null) {
              return o;
            }

            return d < 12 ? b(Blocks.CALCITE) : b(Blocks.DEEPSLATE);
          }
        }
      default:
        return null;
    }
  }

  List<PlanetChunkGenerator.Body> bodies(long s, int minX, int minZ) {
    List<PlanetChunkGenerator.Body> out = new ArrayList<>();
    int cell = this.planet == SpacePlanets.Planet.BELT ? 40 : 56;
    int max = this.planet == SpacePlanets.Planet.BELT ? 15 : 6;

    for (int cx = Math.floorDiv(minX - max, cell);
        cx <= Math.floorDiv(minX + 15 + max, cell);
        cx++) {
      for (int cz = Math.floorDiv(minZ - max, cell);
          cz <= Math.floorDiv(minZ + 15 + max, cell);
          cz++) {
        for (int cy = 0; cy < 6; cy++) {
          if (Math.abs(cx) > 1 || Math.abs(cz) > 1 || cy < 2 || cy > 3) {
            double chance = this.planet == SpacePlanets.Planet.BELT ? 0.55 : 0.22;
            if (!(rnd(s + 31L, cx, cy, cz) > chance)) {
              int r =
                  this.planet == SpacePlanets.Planet.BELT
                      ? 4 + (int) (Math.pow(rnd(s + 32L, cx, cy, cz), 1.5) * 11.0)
                      : 1 + (int) (rnd(s + 32L, cx, cy, cz) * 5.0);
              out.add(
                  new PlanetChunkGenerator.Body(
                      cx * cell + (int) (rnd(s + 33L, cx, cy, cz) * cell),
                      24 + cy * 36 + (int) (rnd(s + 34L, cx, cy, cz) * 24.0),
                      cz * cell + (int) (rnd(s + 35L, cx, cy, cz) * cell),
                      r,
                      (int) (rnd(s + 36L, cx, cy, cz) * 10.0)));
            }
          }
        }
      }
    }

    return out;
  }

  BlockState bodyState(long s, PlanetChunkGenerator.Body a, int x, int y, int z) {
    int dx = x - a.x;
    int dy = y - a.y;
    int dz = z - a.z;
    if (this.planet == SpacePlanets.Planet.ORBIT) {
      boolean panel = a.kind < 3;
      if (!panel) {
        double n = value3(s + 41L, x / 3.0, y / 3.0, z / 3.0);
        if (Math.abs(dx) <= a.r
            && Math.abs(dy) <= a.r / 2 + 1
            && Math.abs(dz) <= a.r
            && !(n < -0.2)) {
          return a.kind < 6
              ? b(Blocks.WHITE_CONCRETE)
              : (a.kind < 8
                  ? b((Block) Content.SHELL.get())
                  : (rnd(s + 42L, x, y, z) < 0.5
                      ? b(Blocks.IRON_BARS)
                      : b(Blocks.LIGHT_GRAY_CONCRETE)));
        } else {
          return null;
        }
      } else if (Math.abs(dy) <= 0 && Math.abs(dx) <= a.r + 3 && Math.abs(dz) <= a.r) {
        return Math.abs(dx) != a.r + 3 && Math.abs(dz) != a.r && dx != 0
            ? b(Blocks.BLUE_STAINED_GLASS)
            : b(Blocks.IRON_BLOCK);
      } else {
        return null;
      }
    } else {
      double d =
          Math.sqrt(dx * dx + dy * dy * 1.3 + dz * dz) / a.r
              + 0.28 * value3(s + 43L, x / 6.0, y / 6.0, z / 6.0);
      if (d > 1.0) {
        return null;
      } else if (a.kind < 2) {
        return d > 0.8 ? b(Blocks.PACKED_ICE) : b(Blocks.BLUE_ICE);
      } else {
        BlockState o = d < 0.85 ? this.ore(s, x, y, z) : null;
        if (o != null) {
          return o;
        } else if (a.kind == 9 && d < 0.5) {
          return rnd(s + 44L, x, y, z) < 0.4 ? b(Blocks.RAW_IRON_BLOCK) : b(Blocks.TUFF);
        } else {
          return d > 0.92
              ? b((Block) SpaceContent.REGOLITH.get())
              : b((Block) SpaceContent.ASTEROID_ROCK.get());
        }
      }
    }
  }

  boolean floating() {
    return this.planet == SpacePlanets.Planet.BELT || this.planet == SpacePlanets.Planet.ORBIT;
  }

  public CompletableFuture<ChunkAccess> fillFromNoise(
      Blender blender, RandomState random, StructureManager structures, ChunkAccess chunk) {
    long s = this.seed(random);
    MutableBlockPos pos = new MutableBlockPos();
    int sx = chunk.getPos().getMinBlockX();
    int sz = chunk.getPos().getMinBlockZ();
    if (this.floating()) {
      for (PlanetChunkGenerator.Body a : this.bodies(s, sx, sz)) {
        int r = a.r + 4;

        for (int x = Math.max(sx, a.x - r); x <= Math.min(sx + 15, a.x + r); x++) {
          for (int z = Math.max(sz, a.z - r); z <= Math.min(sz + 15, a.z + r); z++) {
            for (int y = Math.max(1, a.y - r); y <= Math.min(250, a.y + r); y++) {
              BlockState st = this.bodyState(s, a, x, y, z);
              if (st != null) {
                chunk.setBlockState(pos.set(x, y, z), st, false);
              }
            }
          }
        }
      }
    } else {
      for (int x = sx; x < sx + 16; x++) {
        for (int z = sz; z < sz + 16; z++) {
          int h = this.height(s, x, z);
          int top = Math.max(h, this.planet == SpacePlanets.Planet.AURELIA ? 62 : h);

          for (int y = 0; y <= top; y++) {
            BlockState st = this.state(s, x, y, z, h);
            if (st != null) {
              chunk.setBlockState(pos.set(x, y, z), st, false);
            }
          }
        }
      }
    }

    Heightmap.primeHeightmaps(chunk, EnumSet.of(Types.WORLD_SURFACE_WG, Types.OCEAN_FLOOR_WG));
    return CompletableFuture.completedFuture(chunk);
  }

  public void buildSurface(
      WorldGenRegion region, StructureManager structures, RandomState random, ChunkAccess chunk) {
    long s = this.seed(random);
    int sx = chunk.getPos().getMinBlockX();
    int sz = chunk.getPos().getMinBlockZ();
    BiConsumer<BlockPos, BlockState> sink =
        (p, st) -> {
          if (p.getX() >> 4 == chunk.getPos().x
              && p.getZ() >> 4 == chunk.getPos().z
              && p.getY() > 0
              && p.getY() < 255) {
            chunk.setBlockState(p, st, false);
          }
        };

    for (int x = sx; x < sx + 16; x++) {
      for (int z = sz; z < sz + 16; z++) {
        if (Math.abs(x) >= 24 || Math.abs(z) >= 24) {
          int lx = x - sx;
          int lz = z - sz;
          boolean m2 = lx >= 2 && lx <= 13 && lz >= 2 && lz <= 13;
          boolean m3 = lx >= 3 && lx <= 12 && lz >= 3 && lz <= 12;
          double roll = rnd(s + 51L, x, 0, z);
          switch (this.planet) {
            case LUNA:
              int h = this.height(s, x, z);
              if (roll < 0.002 && m2) {
                VoidspaceFlora.boulder(
                    x,
                    h,
                    z,
                    1 + (int) (rnd(s + 52L, x, 0, z) * 2.0),
                    b((Block) SpaceContent.LUNAR_ROCK.get()),
                    sink);
              }
              break;
            case MARS:
              int hxx = this.height(s, x, z);
              if (roll < 0.003 && m2) {
                VoidspaceFlora.boulder(
                    x,
                    hxx,
                    z,
                    1 + (int) (rnd(s + 52L, x, 0, z) * 2.0),
                    b((Block) SpaceContent.MARS_ROCK.get()),
                    sink);
              } else if (roll < 0.0035) {
                VoidspaceFlora.column(x, hxx, z, 2 + (int) (rnd(s + 53L, x, 0, z) * 5.0), sink);
              }
              break;
            case EUROPA:
              int hxxx = this.height(s, x, z);
              if (this.crack(s, x, z)) {
                break;
              }

              if (roll < 0.0015 && m2) {
                int tall = 4 + (int) (rnd(s + 54L, x, 0, z) * 9.0);

                for (int y = 1; y <= tall; y++) {
                  int rad = Math.max(0, (tall - y) / 4);

                  for (int a = -rad; a <= rad; a++) {
                    for (int c = -rad; c <= rad; c++) {
                      sink.accept(new BlockPos(x + a, hxxx + y, z + c), b(Blocks.PACKED_ICE));
                    }
                  }
                }
              }

              int floor = 12 + (int) (4.0 * fbm(s + 21L, x / 40.0, z / 40.0, 2));
              if (roll > 0.985) {
                sink.accept(new BlockPos(x, floor + 1, z), b(Blocks.SEA_PICKLE));
              } else if (roll > 0.97) {
                sink.accept(new BlockPos(x, floor + 1, z), b(Blocks.TUBE_CORAL_BLOCK));
              }
              break;
            case AURELIA:
              int hx = this.height(s, x, z);
              if (hx > 63) {
                BlockPos up = new BlockPos(x, hx + 1, z);
                if (roll < 0.004 && m3) {
                  VoidspaceFlora.fungus(
                      x,
                      hx,
                      z,
                      5 + (int) (rnd(s + 55L, x, 0, z) * 6.0),
                      2 + (int) (rnd(s + 56L, x, 0, z) * 2.0),
                      sink);
                } else if (roll < 0.007 && m2) {
                  VoidspaceFlora.spire(x, hx, z, 4 + (int) (rnd(s + 57L, x, 0, z) * 8.0), sink);
                } else if (roll < 0.012 && m2) {
                  VoidspaceFlora.emit(
                      x, hx, z, 3 + (int) (rnd(s + 58L, x, 0, z) * 3.0), true, sink);
                } else if (roll < 0.06) {
                  plant(chunk, up, (Block) VoidspaceContent.STAR_BLOOM.get());
                } else if (roll < 0.1) {
                  plant(chunk, up, (Block) Content.CELESTIAL_FROND.get());
                } else if (roll < 0.13) {
                  plant(chunk, up, (Block) VoidspaceContent.SPORE_SPROUT.get());
                } else if (roll < 0.145) {
                  plant(chunk, up, (Block) VoidspaceContent.CRYSTAL_CLUSTER.get());
                }
              }
          }
        }
      }
    }

    Heightmap.primeHeightmaps(chunk, EnumSet.of(Types.WORLD_SURFACE_WG, Types.OCEAN_FLOOR_WG));
  }

  private static void plant(ChunkAccess chunk, BlockPos p, Block plant) {
    if (chunk.getBlockState(p).isAir() && chunk.getBlockState(p.below()).isSolid()) {
      chunk.setBlockState(p, plant.defaultBlockState(), false);
    }
  }

  public int getBaseHeight(
      int x, int z, Types type, LevelHeightAccessor level, RandomState random) {
    if (this.floating()) {
      return 0;
    }

    int h = this.height(this.seed(random), x, z);
    return (this.planet == SpacePlanets.Planet.AURELIA ? Math.max(h, 62) : h) + 1;
  }

  public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState random) {
    BlockState[] states = new BlockState[256];
    long s = this.seed(random);
    int h = this.floating() ? -1 : this.height(s, x, z);

    for (int y = 0; y < 256; y++) {
      BlockState st = h < 0 ? null : this.state(s, x, y, z, h);
      states[y] = st == null ? b(Blocks.AIR) : st;
    }

    return new NoiseColumn(0, states);
  }

  public void applyCarvers(
      WorldGenRegion r,
      long seed,
      RandomState s,
      BiomeManager b,
      StructureManager m,
      ChunkAccess c,
      Carving step) {}

  public void spawnOriginalMobs(WorldGenRegion region) {
    if (this.planet == SpacePlanets.Planet.AURELIA) {
      ChunkPos cp = region.getCenter();
      Holder<Biome> biome =
          region.getBiome(cp.getWorldPosition().atY(region.getMaxBuildHeight() - 1));
      WorldgenRandom r =
          new WorldgenRandom(new LegacyRandomSource(RandomSupport.generateUniqueSeed()));
      r.setDecorationSeed(region.getSeed(), cp.getMinBlockX(), cp.getMinBlockZ());
      NaturalSpawner.spawnMobsForChunkGeneration(region, biome, cp, r);
    }
  }

  public int getGenDepth() {
    return 256;
  }

  public int getMinY() {
    return 0;
  }

  public int getSeaLevel() {
    return this.planet == SpacePlanets.Planet.AURELIA ? 62 : 0;
  }

  public void addDebugScreenInfo(List<String> info, RandomState random, BlockPos p) {
    info.add("TIMESKIPPER nav: " + this.planet.title);
  }

  record Body(int x, int y, int z, int r, int kind) {}
}
