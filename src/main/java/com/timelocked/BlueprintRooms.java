package com.timelocked;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/** Separate geometry schema. Progression version is deliberately not a geometry version. */
public final class BlueprintRooms {
  public static final int VERSION = 5, WIDTH = 480, HEIGHT = 43, DEPTH = 384;

  public static boolean active(Level l, BlockPos o) {
    if (!(l instanceof ServerLevel s) || o == null) return false;
    SiteProgress p = FacilityState.get(s.getServer()).progress.get(o.asLong());
    return p != null && (p.geometryVersion == 4 || p.geometryVersion == VERSION);
  }

  public static boolean drawing(Level l, BlockPos o) {
    if (!(l instanceof ServerLevel s) || o == null) return false;
    var p = FacilityState.get(s.getServer()).progress.get(o.asLong());
    return p != null && p.geometryVersion == VERSION;
  }

  public static BlockPos local(int x, int y, int z) {
    if (x == 88 && y == 8 && z == 36) return new BlockPos(250, 10, 15);
    if (x == 38 && y == 2 && z == 26) return new BlockPos(246, 10, 15);
    if (x == 108 && y == 2 && z == 199) return new BlockPos(386, 2, 97);
    if (x == 108 && y == 2 && z == 72) return new BlockPos(239, 2, 94);
    if (y >= 28 || (x >= 48 && x <= 69 && z >= 150 && z < 190))
      return new BlockPos(x + 26, y, z + 43);
    if (x >= 42 && z <= 62) {
      // Spawn rows fit the chamber, while the two-block portal plane remains exact.
      int nz = z > 35 ? 48 + (z - 35) / 2 : z + 13;
      return new BlockPos(x + 167, y, nz);
    }
    if (x >= 97 && z >= 143 && z < 175) return new BlockPos(x + 278, y, z - 67);
    if (x >= 97 && z >= 175) return new BlockPos(x + 276, y, z - 42);
    if (x >= 97 && z >= 64 && z < 93) return new BlockPos(x + 131, y, z + 22);
    return new BlockPos(x, y, z);
  }

  private static BlockPos local4(int x, int y, int z) {
    if (x == 108 && y == 2 && z == 199)
      return new BlockPos(214, 2, 89); // integral vessel fuel port
    if (x == 108 && y == 2 && z == 72) return new BlockPos(136, 2, 89);
    if (x == 38 && y == 2 && z == 26) return new BlockPos(134, 8, 12);
    if (y >= 28 || (x >= 48 && x <= 69 && z >= 150 && z < 190))
      return new BlockPos(x - 20, y, z + 70);
    if (x >= 42 && z <= 62) return new BlockPos(105 + z, y, 100 - x);
    if (x >= 97 && z >= 143 && z < 175) return new BlockPos(x + 106, y, z - 75);
    if (x >= 97 && z >= 175) return new BlockPos(x + 106, y, z - 56);
    if (x >= 97 && z >= 64 && z < 93) return new BlockPos(x + 28, y, z + 12);
    return new BlockPos(x, y, z);
  }

  public static BlockPos at(Level l, BlockPos o, int x, int y, int z) {
    return o.offset(
        drawing(l, o)
            ? local(x, y, z).offset(0, 0, 128)
            : active(l, o) ? local4(x, y, z) : new BlockPos(x, y, z));
  }

  public static BlockPos at(Level l, BlockPos o, BlockPos p) {
    return at(l, o, p.getX(), p.getY(), p.getZ());
  }

  public static int width(Level l, BlockPos o) {
    return drawing(l, o) ? WIDTH : active(l, o) ? 256 : 130;
  }

  public static int depth(Level l, BlockPos o) {
    return drawing(l, o) ? DEPTH : active(l, o) ? 288 : 208;
  }

  private BlueprintRooms() {}
}
