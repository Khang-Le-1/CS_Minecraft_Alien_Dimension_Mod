package com.timelocked;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.RandomState;

public final class FacilityTerrain {
  public static final int RIM = 2;
  public static final int SEARCH_OFFSET = 512;
  public static final int FLAT_RELIEF = 4;
  private static final Map<FacilityTerrain.Key, FacilityTerrain.Plan> CACHE =
      new LinkedHashMap<FacilityTerrain.Key, FacilityTerrain.Plan>(16, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Entry<FacilityTerrain.Key, FacilityTerrain.Plan> e) {
          return this.size() > 16;
        }
      };
  private static final BitSet EXTERIOR = new BitSet(27040);

  public static boolean exterior(int x, int z) {
    return x >= 0 && x < 130 && z >= 0 && z < 208 && EXTERIOR.get(x * 208 + z);
  }

  private static FacilityTerrain.Survey survey(
      BlockPos p, FacilityTerrain.Sampler source, boolean fine) {
    List<Integer> tops = new ArrayList<>();
    int wet = 0;
    int min = Integer.MAX_VALUE;
    int max = Integer.MIN_VALUE;
    int fluid = Integer.MIN_VALUE;
    List<int[]> points = new ArrayList<>();
    if (fine) {
      for (int x = 0; x < 130; x += 16) {
        for (int z = 0; z < 208; z += 16) {
          points.add(new int[] {x, z});
        }
      }

      for (int z = 0; z < 208; z += 16) {
        points.add(new int[] {129, z});
      }

      for (int x = 0; x < 130; x += 16) {
        points.add(new int[] {x, 207});
      }

      points.add(new int[] {129, 207});

      for (int x : new int[] {36, 52, 68, 84, 95}) {
        for (int z : new int[] {208, 224, 240, 255}) {
          points.add(new int[] {x, z});
        }
      }
    } else {
      for (int x : new int[] {0, 64, 129}) {
        for (int z : new int[] {0, 104, 207}) {
          points.add(new int[] {x, z});
        }
      }

      for (int x : new int[] {36, 95}) {
        for (int z : new int[] {208, 255}) {
          points.add(new int[] {x, z});
        }
      }
    }

    for (int[] at : points) {
      FacilityTerrain.Surface s = source.at(p.getX() + at[0], p.getZ() + at[1]);
      tops.add(s.top());
      min = Math.min(min, s.top());
      max = Math.max(max, s.top());
      if (s.wet()) {
        wet++;
        fluid = Math.max(fluid, s.top());
      }
    }

    Collections.sort(tops);
    return new FacilityTerrain.Survey(
        tops.get(tops.size() / 2), wet, max - min, fluid, tops.size());
  }

  public static FacilityTerrain.Plan select(
      BlockPos anchor, FacilityTerrain.Sampler sampler, int minY, int maxY) {
    List<BlockPos> candidates = new ArrayList<>();
    candidates.add(new BlockPos(anchor.getX(), 0, anchor.getZ()));

    for (int distance : new int[] {256, 512}) {
      for (int[] d :
          new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}}) {
        candidates.add(
            new BlockPos(anchor.getX() + distance * d[0], 0, anchor.getZ() + distance * d[1]));
      }
    }

    BlockPos best = candidates.get(0);
    long score = Long.MAX_VALUE;
    Map<Long, FacilityTerrain.Surface> memo = new HashMap<>();
    FacilityTerrain.Sampler source =
        (x, z) -> memo.computeIfAbsent((long) x << 32 ^ z & 4294967295L, k -> sampler.at(x, z));

    for (BlockPos p : candidates) {
      FacilityTerrain.Survey coarse = survey(p, source, false);
      if (coarse.flat()) {
        FacilityTerrain.Survey fine = survey(p, source, true);
        if (fine.flat()) {
          return result(p, fine, true, minY, maxY);
        }

        coarse = fine;
      }

      long value = coarse.score();
      if (value < score) {
        score = value;
        best = p;
      }
    }

    FacilityTerrain.Survey finalSurvey = survey(best, source, true);
    return result(best, finalSurvey, false, minY, maxY);
  }

  private static FacilityTerrain.Plan result(
      BlockPos p, FacilityTerrain.Survey s, boolean flat, int minY, int maxY) {
    int grade =
        Math.max(s.median(), s.maxFluid() == Integer.MIN_VALUE ? s.median() : s.maxFluid() + 2);
    grade = Math.clamp(grade, minY + 29, maxY - 15);
    return new FacilityTerrain.Plan(
        new BlockPos(p.getX(), grade - 28, p.getZ()), flat, s.wet(), s.relief(), s.count());
  }

  public static FacilityTerrain.Plan plan(ServerLevel level, BlockPos anchor) {
    FacilityTerrain.Key key =
        new FacilityTerrain.Key(
            level.getSeed(),
            new BlockPos(anchor.getX(), 0, anchor.getZ()),
            level.getChunkSource().getGenerator());
    synchronized (CACHE) {
      FacilityTerrain.Plan old = CACHE.get(key);
      if (old != null) {
        return old;
      }

      ChunkGenerator generator = level.getChunkSource().getGenerator();
      RandomState random = level.getChunkSource().randomState();
      FacilityTerrain.Plan p =
          select(
              anchor,
              (x, z) ->
                  new FacilityTerrain.Surface(
                      generator.getBaseHeight(x, z, Types.WORLD_SURFACE_WG, level, random) - 1,
                      generator.getBaseHeight(x, z, Types.OCEAN_FLOOR_WG, level, random) - 1),
              level.getMinBuildHeight(),
              level.getMaxBuildHeight());
      int grade =
          Math.max(
              p.origin().getY() + 28,
              Math.min(level.getMaxBuildHeight() - 15, generator.getSeaLevel() + 2));
      if (grade != p.origin().getY() + 28) {
        p =
            new FacilityTerrain.Plan(
                new BlockPos(p.origin().getX(), grade - 28, p.origin().getZ()),
                p.naturalFlat(),
                p.wetSamples(),
                p.relief(),
                p.samples());
      }

      CACHE.put(key, p);
      return p;
    }
  }

  public static boolean inPad(int x, int z) {
    return x >= 42 && x < 89 && z >= 140 && z < 201
        || x >= 51 && x < 81 && z >= 177 && z < 210
        || x >= 34 && x < 98 && z >= 206 && z < 258;
  }

  private static boolean core(int x, int z) {
    return x >= 0 && x < 130 && z >= 0 && z < 208;
  }

  private static boolean parking(int x, int z) {
    return x >= 36 && x < 96 && z >= 208 && z < 256;
  }

  public static boolean prepareChunk(WorldGenLevel r, BlockPos o, int cx, int cz) {
    boolean changed = false;
    int grade = o.getY() + 28;

    for (int x = cx; x < cx + 16; x++) {
      for (int z = cz; z < cz + 16; z++) {
        int dx = x - o.getX();
        int dz = z - o.getZ();
        if (inPad(dx, dz)) {
          int surface = r.getHeight(Types.WORLD_SURFACE_WG, x, z) - 1;
          int floor = r.getHeight(Types.OCEAN_FLOOR_WG, x, z) - 1;

          int low;
          for (low = Math.max(r.getMinBuildHeight(), Math.min(o.getY() - 2, floor + 1));
              low > r.getMinBuildHeight();
              low--) {
            BlockPos below = new BlockPos(x, low - 1, z);
            if (r.getFluidState(below).isEmpty()
                && r.getBlockState(below).isFaceSturdy(r, below, Direction.UP)) {
              break;
            }
          }

          boolean rim = !core(dx, dz) && !parking(dx, dz);

          for (int y = low; y <= grade; y++) {
            set(
                r,
                new BlockPos(x, y, z),
                y == grade
                    ? Blocks.GRAY_CONCRETE.defaultBlockState()
                    : (rim
                        ? Blocks.STONE_BRICKS.defaultBlockState()
                        : Blocks.STONE.defaultBlockState()));
          }

          for (int y = grade + 1;
              y <= Math.min(r.getMaxBuildHeight() - 1, Math.max(surface, grade + 14));
              y++) {
            set(r, new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
          }

          changed = true;
        }
      }
    }

    return changed;
  }

  public static void finishChunk(WorldGenLevel r, BlockPos o, int cx, int cz) {}

  private static void set(WorldGenLevel r, BlockPos at, BlockState state) {
    if (!r.getBlockState(at).is(Blocks.BEDROCK)) {
      r.setBlock(at, state, 50);
    }
  }

  private FacilityTerrain() {}

  static {
    try (InputStream in =
        FacilityTerrain.class.getResourceAsStream(
            "/assets/timelocked/facility-exterior-pad.json")) {
      if (in == null) {
        throw new IllegalStateException("Missing exterior terrain mask");
      }

      JsonObject json =
          JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8))
              .getAsJsonObject();
      if (json.get("width").getAsInt() != 130 || json.get("depth").getAsInt() != 208) {
        throw new IllegalStateException("Terrain mask size");
      }

      for (JsonElement cell : json.getAsJsonArray("cells")) {
        EXTERIOR.set(cell.getAsInt());
      }
    } catch (IOException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  private record Key(long seed, BlockPos anchor, ChunkGenerator generator) {}

  public record Plan(
      BlockPos origin, boolean naturalFlat, int wetSamples, int relief, int samples) {}

  @FunctionalInterface
  public interface Sampler {
    FacilityTerrain.Surface at(int var1, int var2);
  }

  public record Surface(int top, int floor) {
    public boolean wet() {
      return this.top > this.floor;
    }
  }

  private record Survey(int median, int wet, int relief, int maxFluid, int count) {
    boolean flat() {
      return this.wet == 0 && this.relief <= 4;
    }

    long score() {
      return (this.wet > 0 ? 1000000000L : 0L)
          + this.wet * 100000L / this.count
          + this.relief * 100L;
    }
  }
}
