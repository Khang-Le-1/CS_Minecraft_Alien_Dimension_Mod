package com.timelocked;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.WallSide;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public final class ApproachRoad {
  public static final int SAMPLE = 16;
  public static final int CLEARANCE = 5;
  private static final Map<ApproachRoad.Key, ApproachRoad.Profile> CACHE =
      new LinkedHashMap<ApproachRoad.Key, ApproachRoad.Profile>(16, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Entry<ApproachRoad.Key, ApproachRoad.Profile> e) {
          return this.size() > 16;
        }
      };

  private static ApproachRoad.Terrain sample(
      FeaturePlaceContext<NoneFeatureConfiguration> c, int x, int z) {
    WorldGenLevel r = c.level();
    RandomState random = r.getLevel().getChunkSource().randomState();
    int surface = c.chunkGenerator().getBaseHeight(x, z, Types.WORLD_SURFACE_WG, r, random) - 1;
    int floor = c.chunkGenerator().getBaseHeight(x, z, Types.OCEAN_FLOOR_WG, r, random) - 1;
    return new ApproachRoad.Terrain(surface, floor);
  }

  public static int[] plan(int anchor, int[] surfaces, int[] floors, int min, int max) {
    int n = surfaces.length;
    if (n == floors.length && n >= 2) {
      int[] desired = new int[n];
      int[] required = new int[n];
      Arrays.fill(required, min);

      for (int i = 0; i < n; i++) {
        int a = Math.max(0, i - 8);
        int b = Math.min(n, i + 9);
        int[] window = Arrays.copyOfRange(surfaces, a, b);
        Arrays.sort(window);
        desired[i] = Math.clamp(window[window.length / 4], min, max);
        if (surfaces[i] > floors[i] && floors[i] <= desired[i] + 5) {
          required[i] = Math.clamp(surfaces[i] + 1, min, max);
        }
      }

      for (int i = n - 2; i >= 1; i--) {
        required[i] = Math.max(required[i], required[i + 1] - 2);
      }

      int[] height = new int[n];
      height[0] = anchor;

      for (int i = 1; i < n; i++) {
        int target = Math.max(desired[i], required[i]);
        height[i] =
            Math.clamp(target, Math.max(min, height[i - 1] - 2), Math.min(max, height[i - 1] + 2));
      }

      return height;
    } else {
      throw new IllegalArgumentException("terrain samples");
    }
  }

  public static ApproachRoad.Kind kind(int deck, ApproachRoad.Terrain terrain) {
    if (terrain.floor() > deck + 5 + 1) {
      return ApproachRoad.Kind.TUNNEL;
    } else {
      return !terrain.water() && terrain.surface() >= deck - 3
          ? ApproachRoad.Kind.GROUND
          : ApproachRoad.Kind.BRIDGE;
    }
  }

  public static ApproachRoad.Profile profile(
      FeaturePlaceContext<NoneFeatureConfiguration> c, BlockPos o) {
    ApproachRoad.Key key =
        new ApproachRoad.Key(c.level().getSeed(), o.immutable(), c.chunkGenerator());
    synchronized (CACHE) {
      ApproachRoad.Profile old = CACHE.get(key);
      if (old != null) {
        return old;
      }

      int start = o.getZ() + 255;
      int end = o.getZ() + 2188;
      int n = (end - start + 16 - 1) / 16 + 1;
      int[] surface = new int[n];
      int[] floor = new int[n];

      for (int i = 0; i < n; i++) {
        ApproachRoad.Terrain t = sample(c, o.getX() + 64, start + i * 16);
        surface[i] = t.surface();
        floor[i] = t.floor();
      }

      ApproachRoad.Profile p =
          new ApproachRoad.Profile(
              start,
              end,
              o.getX() + 64,
              plan(
                  o.getY() + 28,
                  surface,
                  floor,
                  c.level().getMinBuildHeight() + 3,
                  c.level().getMaxBuildHeight() - 12));
      CACHE.put(key, p);
      return p;
    }
  }

  public static boolean generate(FeaturePlaceContext<NoneFeatureConfiguration> c, BlockPos o) {
    int cx = c.origin().getX() & -16;
    int cz = c.origin().getZ() & -16;
    int ex = o.getX() + 64;
    int ez = o.getZ() + 188;
    boolean main =
        cx <= ex + 4 && cx + 15 >= ex - 4 && cz <= ez + 2000 && cz + 15 >= o.getZ() + 256;
    boolean town = cx <= ex + 72 && cx + 15 >= ex && cz <= ez + 1491 && cz + 15 >= ez + 1483;
    if (!main && !town) {
      return false;
    }

    ApproachRoad.Profile p = profile(c, o);
    if (main) {
      for (int z = Math.max(cz, o.getZ() + 256); z <= Math.min(cz + 15, ez + 2000); z++) {
        int y = p.height(z);
        boolean ramp = p.height(z - 1) < y || p.height(z + 1) < y;
        ApproachRoad.Terrain t = p.terrain(c, z);
        ApproachRoad.Kind k = kind(y, t);
        buildSlice(
            c.level(),
            ex,
            z,
            y,
            k,
            t.floor(),
            ramp,
            cx,
            cz,
            false,
            z >= ez + 1483 && z <= ez + 1491);
      }
    }

    if (town) {
      int z = ez + 1487;
      int y = p.height(z);
      int[] grades = new int[74];
      grades[0] = y;

      for (int dx = 1; dx < grades.length; dx++) {
        ApproachRoad.Terrain terrain = sample(c, ex + dx, z);
        int target = terrain.surface() + (terrain.water() ? 1 : 0);
        grades[dx] =
            dx % 8 == 0
                ? Math.clamp(target, grades[dx - 1] - 1, grades[dx - 1] + 1)
                : grades[dx - 1];
      }

      for (int x = Math.max(cx, ex + 4); x <= Math.min(cx + 15, ex + 72); x++) {
        int dx = x - ex;
        int deck = grades[dx];
        ApproachRoad.Terrain t = sample(c, x, z);
        boolean ramp = grades[dx - 1] < deck || grades[Math.min(dx + 1, 73)] < deck;
        buildSlice(c.level(), x, z, deck, kind(deck, t), t.floor(), ramp, cx, cz, true, false);
      }
    }

    return true;
  }

  public static void buildSlice(
      WorldGenLevel r,
      int x,
      int z,
      int deck,
      ApproachRoad.Kind kind,
      int ground,
      boolean ramp,
      int clipX,
      int clipZ,
      boolean cross,
      boolean junction) {
    for (int offset = -4; offset <= 4; offset++) {
      int wx = x + (cross ? 0 : offset);
      int wz = z + (cross ? offset : 0);
      if (wx >= clipX && wx <= clipX + 15 && wz >= clipZ && wz <= clipZ + 15) {
        boolean edge = Math.abs(offset) == 4;
        BlockPos at = new BlockPos(wx, deck, wz);
        long hash = r.getSeed() ^ wx * 73428767L ^ wz * 912931L;
        if (!edge) {
          for (int dy = 1; dy <= 5; dy++) {
            set(r, at.above(dy), Blocks.AIR.defaultBlockState());
          }

          BlockState pavement =
              ramp
                  ? (BlockState)
                      Blocks.SMOOTH_STONE_SLAB
                          .defaultBlockState()
                          .setValue(SlabBlock.TYPE, SlabType.BOTTOM)
                  : (offset == 0
                          && Math.floorMod(cross ? wx : wz, 12) < 5
                          && Math.floorMod(hash, 3) != 0
                      ? Blocks.YELLOW_CONCRETE.defaultBlockState()
                      : (Math.floorMod(hash, 13) == 0
                          ? Blocks.ANDESITE.defaultBlockState()
                          : Blocks.GRAY_CONCRETE.defaultBlockState()));
          set(r, at, pavement);
          set(
              r,
              at.below(),
              kind == ApproachRoad.Kind.GROUND
                  ? Blocks.COARSE_DIRT.defaultBlockState()
                  : Blocks.STONE_BRICKS.defaultBlockState());
          if (kind == ApproachRoad.Kind.GROUND) {
            for (int dy = 2; dy <= Math.min(32, deck - ground); dy++) {
              set(r, at.below(dy), Blocks.COARSE_DIRT.defaultBlockState());
            }
          }
        }

        if (kind == ApproachRoad.Kind.TUNNEL) {
          if (edge) {
            for (int dy = 0; dy <= 6; dy++) {
              set(
                  r,
                  at.above(dy),
                  Math.floorMod(wx + wz + dy, 9) == 0
                      ? Blocks.CRACKED_STONE_BRICKS.defaultBlockState()
                      : Blocks.STONE_BRICKS.defaultBlockState());
            }
          } else {
            set(r, at.above(6), Blocks.STONE_BRICKS.defaultBlockState());
          }
        } else if (kind == ApproachRoad.Kind.BRIDGE && edge) {
          set(r, at, Blocks.STONE_BRICKS.defaultBlockState());

          for (int dy = 1; dy <= 5; dy++) {
            set(r, at.above(dy), Blocks.AIR.defaultBlockState());
          }

          if (!junction || cross || offset != 4) {
            set(
                r,
                at.above(),
                cross
                    ? (BlockState)
                        ((BlockState)
                                Blocks.STONE_BRICK_WALL
                                    .defaultBlockState()
                                    .setValue(WallBlock.EAST_WALL, WallSide.LOW))
                            .setValue(WallBlock.WEST_WALL, WallSide.LOW)
                    : (BlockState)
                        ((BlockState)
                                Blocks.STONE_BRICK_WALL
                                    .defaultBlockState()
                                    .setValue(WallBlock.NORTH_WALL, WallSide.LOW))
                            .setValue(WallBlock.SOUTH_WALL, WallSide.LOW));
          }

          if (Math.floorMod(cross ? wx : wz, 12) == 0) {
            for (int y = deck - 1; y >= r.getMinBuildHeight() + 1; y--) {
              BlockPos foot = new BlockPos(wx, y, wz);
              if (r.getFluidState(foot).isEmpty()
                  && r.getBlockState(foot).isFaceSturdy(r, foot, Direction.UP)) {
                break;
              }

              set(r, foot, Blocks.STONE_BRICKS.defaultBlockState());
            }
          }
        } else if (edge && junction && !cross && offset == 4) {
          for (int dy = 1; dy <= 5; dy++) {
            set(r, at.above(dy), Blocks.AIR.defaultBlockState());
          }

          set(r, at, Blocks.GRAY_CONCRETE.defaultBlockState());
          set(r, at.below(), Blocks.STONE_BRICKS.defaultBlockState());
        }

        if (edge && junction && !cross && offset == 4) {
          for (int dy = 1; dy <= 5; dy++) {
            set(r, at.above(dy), Blocks.AIR.defaultBlockState());
          }

          set(r, at, Blocks.GRAY_CONCRETE.defaultBlockState());
          set(r, at.below(), Blocks.STONE_BRICKS.defaultBlockState());
        }
        // Real block-light sources; keep the entire seven-block roadway and tunnel headroom clear.
        int along = cross ? wx : wz;
        if (kind == ApproachRoad.Kind.TUNNEL && offset == 0 && Math.floorMod(along, 6) == 0) {
          // Recessed into the roof, rather than hanging in the traffic envelope.
          set(r, at.above(6), Blocks.SEA_LANTERN.defaultBlockState());
        } else if (kind != ApproachRoad.Kind.TUNNEL
            && edge
            && !(junction && !cross && offset == 4)
            && Math.floorMod(along, 12) == (offset < 0 ? 0 : 6)) {
          set(r, at, Blocks.STONE_BRICKS.defaultBlockState());
          for (int h = 1; h <= 3; h++) set(r, at.above(h), Blocks.IRON_BARS.defaultBlockState());
          set(r, at.above(4), Blocks.SEA_LANTERN.defaultBlockState());
          set(r, at.above(5), Blocks.STONE_BRICK_SLAB.defaultBlockState());
        }
      }
    }
  }

  private static void set(WorldGenLevel r, BlockPos p, BlockState s) {
    if (p.getY() >= r.getMinBuildHeight() && p.getY() < r.getMaxBuildHeight()) {
      r.setBlock(p, s, 50);
    }
  }

  public static void signPocket(
      FeaturePlaceContext<NoneFeatureConfiguration> c, int ex, int ez, int y) {
    WorldGenLevel r = c.level();
    int cx = c.origin().getX() & -16;
    int cz = c.origin().getZ() & -16;

    for (int x = Math.max(cx, ex - 14); x <= Math.min(cx + 15, ex - 4); x++) {
      for (int z = Math.max(cz, ez + 497); z <= Math.min(cz + 15, ez + 503); z++) {
        set(r, new BlockPos(x, y, z), Blocks.STONE_BRICKS.defaultBlockState());

        for (int dy = 1; dy <= 8; dy++) {
          set(r, new BlockPos(x, y + dy, z), Blocks.AIR.defaultBlockState());
        }
      }
    }
  }

  private ApproachRoad() {}

  private record Key(long seed, BlockPos origin, ChunkGenerator generator) {}

  public enum Kind {
    GROUND,
    BRIDGE,
    TUNNEL;
  }

  public static final class Profile {
    public final int start;
    public final int end;
    public final int centerX;
    private final int[] heights;
    private final Map<Integer, ApproachRoad.Terrain> terrain = new HashMap<>();

    Profile(int start, int end, int centerX, int[] heights) {
      this.start = start;
      this.end = end;
      this.centerX = centerX;
      this.heights = heights;
    }

    public int height(int z) {
      double node = Math.clamp((z - this.start) / 16.0, 0.0, this.heights.length - 1);
      int i = (int) node;
      return i == this.heights.length - 1
          ? this.heights[i]
          : (int)
              Math.round(this.heights[i] + (this.heights[i + 1] - this.heights[i]) * (node - i));
    }

    synchronized ApproachRoad.Terrain terrain(
        FeaturePlaceContext<NoneFeatureConfiguration> c, int z) {
      return this.terrain.computeIfAbsent(z, k -> ApproachRoad.sample(c, this.centerX, k));
    }
  }

  public record Terrain(int surface, int floor) {
    public boolean water() {
      return this.surface > this.floor;
    }
  }
}
