package com.timelocked;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;

public final class PortalGeometry {
  public static final BlockPos ANCHOR = new BlockPos(64, 24, 30);
  public static final BlockPos CENTER = new BlockPos(64, 14, 30);
  public static final List<BlockPos> APERTURE;
  public static final List<BlockPos> FRAME;

  public static boolean aperture(int u, int v) {
    return u * u + v * v < 49;
  }

  public static boolean frame(int u, int v) {
    int r = u * u + v * v;
    return r >= 49 && r <= 110;
  }

  private PortalGeometry() {}

  static {
    ArrayList<BlockPos> inside = new ArrayList<>();
    ArrayList<BlockPos> rim = new ArrayList<>();

    for (int u = -10; u <= 10; u++) {
      for (int v = -10; v <= 10; v++) {
        for (int z = 29; z <= 30; z++) {
          if (aperture(u, v)) {
            inside.add(new BlockPos(64 + u, 14 + v, z));
          }

          if (frame(u, v)) {
            rim.add(new BlockPos(64 + u, 14 + v, z));
          }
        }
      }
    }

    APERTURE = List.copyOf(inside);
    FRAME = List.copyOf(rim);
  }
}
