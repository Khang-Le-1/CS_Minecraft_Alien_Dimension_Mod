package com.timelocked;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Validated, immutable classpath drawings. These are shipped model data, not gameplay tuning. Never
 * silently substitute an empty mesh: a missing/corrupt drawing is a packaging error. No client
 * classes are referenced, so the same schemas can be tested on a dedicated server.
 */
public final class AuthoredGeometry {
  private static final int SCHEMA_VERSION = 1;
  private static final String ROOT = "/data/timelocked/geometry/";

  public record Rectangle(int minX, int minY, int maxX, int maxY) {}

  public record Logo(int columns, int rows, int pixelsPerBlock, List<List<Rectangle>> parts) {}

  public record Cuboid(
      String name,
      int textureU,
      int textureV,
      float x,
      float y,
      float z,
      float width,
      float height,
      float depth,
      float pivotX,
      float pivotY,
      float pivotZ) {}

  public record Vehicle(
      int textureWidth, int textureHeight, List<Integer> wheelIds, List<Cuboid> cuboids) {
    public int[] wheelParts() {
      return wheelIds.stream().mapToInt(Integer::intValue).toArray();
    }
  }

  // Lazy holders avoid parsing client-only drawings during an ordinary server startup.
  private static final class LogoHolder {
    static final Logo VALUE = parseLogo(read("sarc-logo"));
  }

  private static final class VehicleHolder {
    static final Map<String, Vehicle> VALUES =
        Map.of(
            "rover",
            parseVehicle(read("rover")),
            "truck",
            parseVehicle(read("truck")),
            "tram",
            parseVehicle(read("tram")));
  }

  public static Logo logo() {
    return LogoHolder.VALUE;
  }

  public static Vehicle vehicle(String kind) {
    Vehicle vehicle = VehicleHolder.VALUES.get(kind);
    if (vehicle == null) throw new IllegalArgumentException("Unknown vehicle drawing: " + kind);
    return vehicle;
  }

  private static JsonObject read(String name) {
    try (var stream = AuthoredGeometry.class.getResourceAsStream(ROOT + name + ".json")) {
      if (stream == null) throw new IllegalStateException("Missing drawing: " + ROOT + name);
      var root =
          JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
              .getAsJsonObject();
      if (integer(root, "schemaVersion") != SCHEMA_VERSION)
        throw new IllegalArgumentException("Unsupported drawing schema: " + name);
      return root;
    } catch (IOException | RuntimeException error) {
      throw new IllegalStateException("Cannot load drawing " + name, error);
    }
  }

  // Package-private parsers also test rejection of malformed authoring data.
  static Logo parseLogo(JsonObject data) {
    int columns = positive(data, "columns"), rows = positive(data, "rows");
    int pixels = positive(data, "pixelsPerBlock");
    var grid = data.getAsJsonArray("parts");
    require(grid.size() == rows, "Logo row count differs from declared rows");
    var parts = new ArrayList<List<Rectangle>>();
    for (var rowValue : grid) {
      var row = rowValue.getAsJsonArray();
      require(row.size() == columns, "Logo column count differs from declared columns");
      for (var partValue : row) {
        var rectangles = new ArrayList<Rectangle>();
        for (var value : partValue.getAsJsonArray()) {
          var rectangle = value.getAsJsonObject();
          int minX = integer(rectangle, "minX"), minY = integer(rectangle, "minY");
          int maxX = integer(rectangle, "maxX"), maxY = integer(rectangle, "maxY");
          require(
              minX >= 0
                  && minY >= 0
                  && maxX <= pixels
                  && maxY <= pixels
                  && minX < maxX
                  && minY < maxY,
              "Logo rectangle outside a block or empty");
          rectangles.add(new Rectangle(minX, minY, maxX, maxY));
        }
        parts.add(List.copyOf(rectangles));
      }
    }
    return new Logo(columns, rows, pixels, List.copyOf(parts));
  }

  static Vehicle parseVehicle(JsonObject data) {
    int width = positive(data, "textureWidth"), height = positive(data, "textureHeight");
    var cuboids = new ArrayList<Cuboid>();
    var names = new HashSet<String>();
    for (var value : data.getAsJsonArray("cuboids")) {
      var cube = value.getAsJsonObject();
      String name = cube.get("name").getAsString();
      require(!name.isBlank() && names.add(name), "Duplicate or empty model part name");
      int u = integer(cube, "textureU"), v = integer(cube, "textureV");
      require(u >= 0 && v >= 0 && u < width && v < height, "Texture origin outside atlas");
      float xSize = number(cube, "width"),
          ySize = number(cube, "height"),
          zSize = number(cube, "depth");
      require(xSize > 0 && ySize > 0 && zSize > 0, "Cuboid size must be positive");
      cuboids.add(
          new Cuboid(
              name,
              u,
              v,
              number(cube, "x"),
              number(cube, "y"),
              number(cube, "z"),
              xSize,
              ySize,
              zSize,
              number(cube, "pivotX"),
              number(cube, "pivotY"),
              number(cube, "pivotZ")));
    }
    var wheels = new ArrayList<Integer>();
    var distinctWheels = new HashSet<Integer>();
    for (var wheel : data.getAsJsonArray("wheelParts")) {
      int id = wheel.getAsBigDecimal().intValueExact();
      // IDs refer to named part_N children, NOT their index in the cuboid array.
      require(
          names.contains("part_" + id) && distinctWheels.add(id),
          "Missing or duplicate wheel part");
      wheels.add(id);
    }
    require(!cuboids.isEmpty(), "Vehicle drawing must not be empty");
    return new Vehicle(width, height, List.copyOf(wheels), List.copyOf(cuboids));
  }

  private static int integer(JsonObject data, String key) {
    return data.get(key).getAsBigDecimal().intValueExact();
  }

  private static int positive(JsonObject data, String key) {
    int value = integer(data, key);
    require(value > 0, key + " must be positive");
    return value;
  }

  private static float number(JsonObject data, String key) {
    float value = data.get(key).getAsFloat();
    require(Float.isFinite(value), key + " must be finite");
    return value;
  }

  private static void require(boolean condition, String message) {
    if (!condition) throw new IllegalArgumentException(message);
  }

  private AuthoredGeometry() {}
}
