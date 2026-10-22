package com.timelocked;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;

/** New pad sized to the full 43.42 x 55.33 mesh, with separate ground and side boarding routes. */
public final class LaunchSite082 {
  static void build(SarcOutposts.Out o, int X, int Y, int Z) {
    SarcOutposts.prep(o, X, Y, Z, 48, 56, 40, Blocks.STONE.defaultBlockState());
    var s = SarcLaunchSites.sink(o);
    for (int x = -48; x <= 48; x++)
      for (int z = -56; z <= 56; z++) {
        o.set(
            X + x,
            Y,
            Z + z,
            (x % 12 == 0 || z % 12 == 0 ? Blocks.LIGHT_GRAY_CONCRETE : Blocks.GRAY_CONCRETE)
                .defaultBlockState());
        if (Math.abs(x) == 48 || Math.abs(z) == 56)
          o.set(X + x, Y + 1, Z + z, Blocks.IRON_BARS.defaultBlockState());
        if ((Math.abs(x) == 24 && Math.abs(z) <= 34) || (Math.abs(z) == 34 && Math.abs(x) <= 24))
          o.set(X + x, Y, Z + z, Content.HAZARD.get().defaultBlockState());
      }
    TimeskipperShip.hull(s, X, Y + 6, Z, false);
    TimeskipperBoarding.groundRamp(s, X, Y + 6, Z);
    TimeskipperBoarding.sidePort(s, X, Y + 6, Z);
    // Dedicated docking/service tower is well outside the ship's +/-21.71 engine envelope.
    for (int x = 38; x <= 42; x++)
      for (int z = -5; z <= 1; z++)
        for (int y = 1; y <= 18; y++) {
          if ((x == 38 || x == 42) && (z == -5 || z == 1))
            o.set(X + x, Y + y, Z + z, ShipInteriorContent.DOCK_FRAME.get().defaultBlockState());
          if (y % 5 == 0
              && (x == 38 || x == 42 || z == -5 || z == 1)
              && !(z == 1 && x >= 39 && x <= 41 && y >= 7 && y <= 9))
            o.set(X + x, Y + y, Z + z, MetalContent.GRATING.get().defaultBlockState());
        }
    // Station-side transfer deck leads to an opposing port. The arm is deployed by the docking
    // controller.
    for (int x = 32; x <= 42; x++)
      for (int z = -3; z <= -1; z++) {
        o.set(X + x, Y + 6, Z + z, ShipInteriorContent.DECK.get().defaultBlockState());
        if (z != -2) o.set(X + x, Y + 7, Z + z, Blocks.IRON_BARS.defaultBlockState());
      }
    o.set(
        X + 32,
        Y + 7,
        Z - 2,
        ShipInteriorContent.DOCK_PORT
            .get()
            .defaultBlockState()
            .setValue(DockingPortBlock.FACING, Direction.WEST));
    o.set(
        X + 32,
        Y + 8,
        Z - 2,
        ShipInteriorContent.DOCK_PORT
            .get()
            .defaultBlockState()
            .setValue(DockingPortBlock.FACING, Direction.WEST)
            .setValue(DockingPortBlock.UPPER, true));
    for (int z = 2; z <= 7; z++) {
      int h = 8 - z;
      for (int x = 39; x <= 41; x++)
        o.set(
            X + x,
            Y + h,
            Z + z,
            Content.STAIRS.get().defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH));
    }
    for (int x = 39; x <= 41; x++)
      for (int z = 0; z <= 1; z++)
        o.set(X + x, Y + 6, Z + z, ShipInteriorContent.DECK.get().defaultBlockState());
    // Tanks relocated outside both the nose and engines.
    for (int z : new int[] {-40, -30})
      for (int x = -3; x <= 3; x++)
        for (int q = -3; q <= 3; q++)
          if (x * x + q * q <= 10)
            for (int y = 1; y <= 10; y++) {
              o.set(
                  X - 36 + x,
                  Y + y,
                  Z + z + q,
                  (y % 4 == 0 ? Blocks.ORANGE_CONCRETE : Blocks.WHITE_CONCRETE)
                      .defaultBlockState());
            }
    s.screen(
        X + 39,
        Y + 5,
        Z - 4,
        Direction.SOUTH,
        0,
        "DOCKING CONTROL",
        "OPPOSED PORTS: 24m",
        "USE PORT TO ALIGN",
        "GROUND BOARDING: RAMP");
    o.barrel(X + 42, Y + 1, Z + 8, TimeskipperShip.SUPPLY, null);
  }

  private LaunchSite082() {}
}
