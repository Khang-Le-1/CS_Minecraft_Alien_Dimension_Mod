package com.timelocked;

public final class VoidspaceLand {
  public static final VoidspaceLand.Village[] VILLAGES =
      new VoidspaceLand.Village[] {
        new VoidspaceLand.Village(-240, 180, true),
        new VoidspaceLand.Village(310, -240, true),
        new VoidspaceLand.Village(-450, -430, true),
        new VoidspaceLand.Village(600, 460, true),
        new VoidspaceLand.Village(170, 270, false),
        new VoidspaceLand.Village(-150, -250, false),
        new VoidspaceLand.Village(-690, 260, false),
        new VoidspaceLand.Village(300, -670, false),
        new VoidspaceLand.Village(620, -170, false)
      };

  public static boolean land(int x, int z) {
    double a = Math.atan2(z, x);
    double r = 950.0 + 65.0 * Math.sin(a * 5.0) + 35.0 * Math.cos(a * 9.0);
    return (double) x * x + (double) z * z < r * r;
  }

  public static boolean sanctuary(Village v) {
    return v.intact() && v.x() == -240 && v.z() == 180;
  }

  public static int padRadius(Village v) {
    return sanctuary(v) ? 94 : 46;
  }

  public static double refugeRadius(Village v) {
    return sanctuary(v) ? 300.0 : (v.x() == 600 ? 185.0 : 205.0);
  }

  public static boolean preserved(int x, int z) {
    for (VoidspaceLand.Village v : VILLAGES) {
      if (v.intact && Math.hypot(x - v.x, z - v.z) < refugeRadius(v)) {
        return true;
      }
    }

    return false;
  }

  public static boolean plateau(int x, int z) {
    if (Math.abs(x) < 28 && Math.abs(z) < 28) {
      return true;
    }

    for (VoidspaceLand.Village v : VILLAGES) {
      if (Math.abs(x - v.x) <= padRadius(v) && Math.abs(z - v.z) <= padRadius(v)) {
        return true;
      }
    }

    return false;
  }

  public static boolean approach(int x, int z) {
    // Follow the original diagonal until the sanctuary apron, then enter
    // through the east gate rather than cutting diagonally through homes.
    int[][] points = {{0, 0}, {-150, 112}, {-150, 180}, {-240, 180}};
    for (int i = 1; i < points.length; i++) {
      double ax = points[i - 1][0], az = points[i - 1][1];
      double dx = points[i][0] - ax, dz = points[i][1] - az;
      double t = Math.clamp(((x - ax) * dx + (z - az) * dz) / (dx * dx + dz * dz), 0.0, 1.0);
      if (Math.hypot(x - ax - t * dx, z - az - t * dz) < 3.0) return true;
    }
    return false;
  }

  private VoidspaceLand() {}

  public record Village(int x, int z, boolean intact) {}
}
