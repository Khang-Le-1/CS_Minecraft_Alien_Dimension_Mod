package com.timelocked;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;

/** Shared declarative interior plan and a non-destructive, opt-in save upgrade. */
public final class TimeskipperInteriorStyle {
  private static Plan cached;

  private static Plan plan() {
    if (cached != null) return cached;
    try (var stream =
        TimeskipperInteriorStyle.class.getResourceAsStream(
            "/assets/timelocked/models/ship/timeskipper-interior-plan.json")) {
      if (stream == null) throw new IllegalStateException("Missing TIMESKIPPER interior plan");
      JsonObject json =
          JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
              .getAsJsonObject();
      if (json.get("version").getAsInt() != 1)
        throw new IllegalStateException("Unsupported interior plan");
      cached = new Plan(read(json, "hull"), read(json, "interior"));
      return cached;
    } catch (Exception e) {
      throw new IllegalStateException("Unable to load TIMESKIPPER interior", e);
    }
  }

  private static Map<BlockPos, BlockState> read(JsonObject json, String phase) {
    Map<BlockPos, BlockState> result = new LinkedHashMap<>();
    for (var entry : json.getAsJsonArray(phase)) {
      JsonObject j = entry.getAsJsonObject();
      ResourceLocation id = ResourceLocation.parse(j.get("block").getAsString());
      if (!BuiltInRegistries.BLOCK.containsKey(id))
        throw new IllegalStateException("Unknown interior block " + id);
      BlockState state = BuiltInRegistries.BLOCK.get(id).defaultBlockState();
      for (var prop : j.getAsJsonObject("properties").entrySet()) {
        Property<?> property = state.getBlock().getStateDefinition().getProperty(prop.getKey());
        if (property == null) throw new IllegalStateException("Unknown property " + prop.getKey());
        state = property(state, property, prop.getValue().getAsString());
      }
      result.put(
          new BlockPos(j.get("x").getAsInt(), j.get("y").getAsInt(), j.get("z").getAsInt()), state);
    }
    return result;
  }

  private static <T extends Comparable<T>> BlockState property(
      BlockState state, Property<T> property, String value) {
    return state.setValue(property, property.getValue(value).orElseThrow());
  }

  public static void hull(TimeskipperShip.Sink out, int X, int Y, int Z) {
    place(plan().hull(), out, X, Y, Z);
  }

  public static void interior(TimeskipperShip.Sink out, int X, int Y, int Z) {
    place(plan().interior(), out, X, Y, Z);
  }

  private static void place(
      Map<BlockPos, BlockState> cells, TimeskipperShip.Sink out, int X, int Y, int Z) {
    cells.forEach((p, s) -> out.set(X + p.getX(), Y + p.getY(), Z + p.getZ(), s));
  }

  private static Map<BlockPos, BlockState> legacy(boolean furniture, boolean migrating082) {
    Map<BlockPos, BlockState> m = new LinkedHashMap<>();
    TimeskipperShip.Sink sink = (x, y, z, state) -> m.put(new BlockPos(x, y, z), state);
    TimeskipperShip.legacyHull(sink, 0, 0, 0, true);
    TimeskipperShip.legacyInterior(sink, 0, 0, 0);
    if (furniture) TimeskipperRefit.furnish(sink, 0, 0, 0);
    try (var in =
        TimeskipperInteriorStyle.class.getResourceAsStream(
            "/assets/timelocked/models/ship/timeskipper-interior-plan-081.json")) {
      JsonObject old =
          JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8))
              .getAsJsonObject();
      m.putAll(read(old, "hull"));
      m.putAll(read(old, "interior"));
      // A prior 0.8.2 refit has a distinct stock baseline; only exact matching cells may be
      // removed.
      if (migrating082)
        try (var newer =
            TimeskipperInteriorStyle.class.getResourceAsStream(
                "/assets/timelocked/models/ship/timeskipper-interior-plan-082.json")) {
          var before =
              JsonParser.parseReader(new InputStreamReader(newer, StandardCharsets.UTF_8))
                  .getAsJsonObject();
          m.putAll(read(before, "hull"));
          m.putAll(read(before, "interior"));
          for (int z = 6; z <= 12; z++)
            for (int x = -5; x <= -3; x++) {
              int y = z - 12;
              for (int h = 1; h <= 4; h++)
                m.put(new BlockPos(x, y + h, z), Blocks.AIR.defaultBlockState());
              m.put(
                  new BlockPos(x, y, z),
                  Content.STAIRS
                      .get()
                      .defaultBlockState()
                      .setValue(
                          net.minecraft.world.level.block.StairBlock.FACING,
                          net.minecraft.core.Direction.SOUTH));
            }
        }
    } catch (Exception e) {
      throw new IllegalStateException("Missing guarded 0.8.1 baseline", e);
    }
    return m;
  }

  public static Result apply(ServerLevel level, BlockPos origin) {
    // Compare stock cells against a generated baseline. Never call build()
    // on an existing save and never reseed inventories or SavedData.
    boolean migrating082 =
        level.getBlockState(origin.offset(0, -6, -2)).is(ShipInteriorContent.PATH.get());
    Map<BlockPos, BlockState> expected = legacy(true, migrating082),
        preFurniture = legacy(false, migrating082);
    if (!migrating082
        && level.getBlockState(origin.offset(0, -2, -4)).is(ShipInteriorContent.PATH.get()))
      try (var old =
          TimeskipperInteriorStyle.class.getResourceAsStream(
              "/assets/timelocked/models/ship/timeskipper-interior-plan-083.json")) {
        var before =
            JsonParser.parseReader(new InputStreamReader(old, StandardCharsets.UTF_8))
                .getAsJsonObject();
        expected.putAll(read(before, "hull"));
        expected.putAll(read(before, "interior"));
        preFurniture.putAll(read(before, "hull"));
        preFurniture.putAll(read(before, "interior"));
      } catch (Exception e) {
        throw new IllegalStateException("Missing 0.8.3 comparison plan", e);
      }
    Map<BlockPos, BlockState> previous0843 = new LinkedHashMap<>();
    try (var old =
        TimeskipperInteriorStyle.class.getResourceAsStream(
            "/assets/timelocked/models/ship/timeskipper-interior-plan-0843.json")) {
      var before =
          JsonParser.parseReader(new InputStreamReader(old, StandardCharsets.UTF_8))
              .getAsJsonObject();
      previous0843.putAll(read(before, "hull"));
      previous0843.putAll(read(before, "interior"));
    } catch (Exception e) {
      throw new IllegalStateException("Missing 0.8.4.3 stock comparison plan", e);
    }
    Map<BlockPos, BlockState> target = new LinkedHashMap<>(expected);
    if (migrating082)
      for (int z = -30; z <= 27; z++)
        for (int x = -10; x <= 10; x++)
          for (int y = -10; y <= -4; y++)
            target.put(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
    TimeskipperShip.Sink sink = (x, y, z, state) -> target.put(new BlockPos(x, y, z), state);
    TimeskipperShip.hull(sink, 0, 0, 0, true);
    TimeskipperShip.interior(sink, 0, 0, 0);
    TimeskipperRefit.furnish(sink, 0, 0, 0);
    TimeskipperDecks.finish(sink, 0, 0, 0);
    int changed = 0, protectedCells = 0;
    for (var cell : target.entrySet()) {
      BlockPos relative = cell.getKey(), at = origin.offset(relative);
      BlockState before = expected.getOrDefault(relative, Blocks.AIR.defaultBlockState());
      BlockState after = cell.getValue();
      if (after.equals(before)) continue;
      BlockState actual = level.getBlockState(at);
      if (actual.equals(after)) continue;
      if (!actual.equals(before)
          && !actual.equals(preFurniture.getOrDefault(relative, Blocks.AIR.defaultBlockState()))
          && !actual.equals(previous0843.getOrDefault(relative, Blocks.AIR.defaultBlockState()))) {
        protectedCells++;
        continue;
      }
      // Preserve every block entity, not merely nonempty inventories.
      if (level.getBlockEntity(at) != null || !level.getFluidState(at).isEmpty()) {
        protectedCells++;
        continue;
      }
      if (!after.isAir()
          && !level
              .getEntities(
                  (net.minecraft.world.entity.Entity) null,
                  new AABB(at).inflate(0.01),
                  e -> e.isAlive())
              .isEmpty()) {
        protectedCells++;
        continue;
      }
      level.setBlock(at, after, 3);
      changed++;
    }
    return new Result(changed, protectedCells);
  }

  private record Plan(Map<BlockPos, BlockState> hull, Map<BlockPos, BlockState> interior) {}

  public record Result(int changed, int protectedCells) {}

  private TimeskipperInteriorStyle() {}
}
