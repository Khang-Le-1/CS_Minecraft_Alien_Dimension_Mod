package com.timelocked;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;

/** Ramp reaches a modeled sealed airlock immediately inside the original belly aperture. */
public final class TimeskipperBoarding {
  public static void door(TimeskipperShip.Sink s, int X, int Y, int Z, Direction face) {
    s.set(
        X,
        Y,
        Z,
        ShipInteriorContent.BOARDING_AIRLOCK
            .get()
            .defaultBlockState()
            .setValue(BoardingAirlockBlock.FACING, face));
    s.set(
        X,
        Y + 1,
        Z,
        ShipInteriorContent.BOARDING_AIRLOCK
            .get()
            .defaultBlockState()
            .setValue(BoardingAirlockBlock.FACING, face)
            .setValue(BoardingAirlockBlock.UPPER, true));
    for (int side = -1; side <= 1; side++) {
      Direction d = side < 0 ? face.getCounterClockWise() : face.getClockWise();
      int dx = side == 0 ? 0 : d.getStepX(), dz = side == 0 ? 0 : d.getStepZ();
      s.set(X + dx, Y + 2, Z + dz, ShipInteriorContent.CEILING.get().defaultBlockState());
      if (side != 0)
        for (int h = 0; h < 2; h++)
          s.set(
              X + dx,
              Y + h,
              Z + dz,
              ShipInteriorContent.AIRLOCK_JAMB
                  .get()
                  .defaultBlockState()
                  .setValue(AirlockJambBlock.FACING, face)
                  .setValue(AirlockJambBlock.RIGHT, side > 0));
    }
  }

  public static void groundRamp(TimeskipperShip.Sink s, int X, int Y, int Z) {
    var ramp =
        ShipInteriorContent.BOARDING_RAMP
            .get()
            .defaultBlockState()
            .setValue(BoardingRampBlock.FACING, Direction.NORTH);
    var air = Blocks.AIR.defaultBlockState();
    var wall = ShipInteriorContent.BULKHEAD.get().defaultBlockState();
    for (int z = 6; z <= 9; z++) {
      int y = 3 - z;
      // Clear headroom only up to the lower-deck ceiling (Y-1). The old 3-high column cut a hole in
      // the upper deck floor at the top of the ramp.
      for (int x = -1; x <= 1; x++) {
        for (int h = 1; h <= 3; h++) if (y + h <= -1) s.set(X + x, Y + y + h, Z + z, air);
        s.set(X + x, Y + y, Z + z, ramp);
      }
      // Solid bay walls instead of single iron bars: the ramp bay no longer opens into the cargo
      // hold.
      for (int x : new int[] {-2, 2}) {
        s.set(X + x, Y + y, Z + z, ShipInteriorContent.THRESHOLD.get().defaultBlockState());
        for (int h = 1; y + h <= -1; h++) s.set(X + x, Y + y + h, Z + z, wall);
      }
    }
    // Close the aft end of the bay (above the hull floor) so the ramp trench is its own sealed
    // chamber.
    for (int x = -2; x <= 2; x++) for (int h = -2; h <= -1; h++) s.set(X + x, Y + h, Z + 10, wall);
    for (int x = -1; x <= 1; x++)
      for (int z = 10; z <= 12; z++)
        s.set(X + x, Y - 6, Z + z, ShipInteriorContent.DECK.get().defaultBlockState());
    for (int x = -1; x <= 1; x++)
      s.set(
          X + x,
          Y - 3,
          Z + 5,
          ShipInteriorContent.SERVICE_SLAB
              .get()
              .defaultBlockState()
              .setValue(
                  net.minecraft.world.level.block.state.properties.BlockStateProperties.SLAB_TYPE,
                  net.minecraft.world.level.block.state.properties.SlabType.TOP));
    door(s, X, Y - 2, Z + 5, Direction.SOUTH);
  }

  public static void sidePort(TimeskipperShip.Sink s, int X, int Y, int Z) {
    for (int x = 6; x <= 8; x++) {
      s.set(X + x, Y, Z - 2, ShipInteriorContent.DECK.get().defaultBlockState());
      for (int y = 1; y <= 3; y++) s.set(X + x, Y + y, Z - 2, Blocks.AIR.defaultBlockState());
    }
    s.set(
        X + 8,
        Y + 1,
        Z - 2,
        ShipInteriorContent.DOCK_PORT
            .get()
            .defaultBlockState()
            .setValue(DockingPortBlock.FACING, Direction.EAST));
    s.set(
        X + 8,
        Y + 2,
        Z - 2,
        ShipInteriorContent.DOCK_PORT
            .get()
            .defaultBlockState()
            .setValue(DockingPortBlock.FACING, Direction.EAST)
            .setValue(DockingPortBlock.UPPER, true));
    door(s, X + 6, Y + 1, Z - 2, Direction.WEST);
    // Roof the carved tunnel (it left a hole in the hull above the port) and wall the outboard
    // collar.
    var ceiling = ShipInteriorContent.CEILING.get().defaultBlockState();
    var wall = ShipInteriorContent.BULKHEAD.get().defaultBlockState();
    for (int x = 7; x <= 8; x++) s.set(X + x, Y + 3, Z - 2, ceiling);
    for (int z : new int[] {-3, -1}) {
      s.set(X + 8, Y, Z + z, ShipInteriorContent.DECK.get().defaultBlockState());
      for (int y = 1; y <= 3; y++) s.set(X + 8, Y + y, Z + z, y == 3 ? ceiling : wall);
    }
  }

  private TimeskipperBoarding() {}
}
