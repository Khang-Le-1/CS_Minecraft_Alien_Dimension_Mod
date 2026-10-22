package com.timelocked;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * One shared station connection plan, used by fresh station generation and explicitly confirmed
 * repair.
 */
public final class OrbitalDock083 {
  public static final BlockPos PORT = new BlockPos(-18, 121, 38);

  public static void build(TimeskipperShip.Sink s) {
    // Enclosed wing outside the west solar array; floor shares the station's y=120 datum.
    for (int z = 3; z <= 40; z++)
      for (int x = -17; x <= -13; x++)
        for (int y = 120; y <= 124; y++) {
          boolean shell = x == -17 || x == -13 || z == 40 || y == 120 || y == 124;
          s.set(
              x,
              y,
              z,
              (y == 120
                      ? Content.FLOOR.get()
                      : y == 124
                          ? ShipInteriorContent.CEILING.get()
                          : shell ? ShipInteriorContent.BULKHEAD.get() : Blocks.AIR)
                  .defaultBlockState());
        }
    for (int z = 36; z <= 40; z++)
      for (int x = -18; x <= -12; x++)
        for (int y = 120; y <= 124; y++) {
          boolean shell = x == -18 || x == -12 || z == 36 || z == 40 || y == 120 || y == 124;
          s.set(
              x,
              y,
              z,
              (y == 120
                      ? Content.FLOOR.get()
                      : y == 124
                          ? ShipInteriorContent.CEILING.get()
                          : shell ? ShipInteriorContent.BULKHEAD.get() : Blocks.AIR)
                  .defaultBlockState());
        }
    for (int z = 35; z <= 37; z++)
      for (int y = 121; y <= 123; y++) s.set(-15, y, z, Blocks.AIR.defaultBlockState());
    for (int y = 121; y <= 123; y++) s.set(-15, y, 2, Blocks.AIR.defaultBlockState());
    s.set(
        PORT.getX(),
        PORT.getY(),
        PORT.getZ(),
        ShipInteriorContent.DOCK_PORT
            .get()
            .defaultBlockState()
            .setValue(DockingPortBlock.FACING, Direction.WEST));
    s.set(
        PORT.getX(),
        PORT.getY() + 1,
        PORT.getZ(),
        ShipInteriorContent.DOCK_PORT
            .get()
            .defaultBlockState()
            .setValue(DockingPortBlock.FACING, Direction.WEST)
            .setValue(DockingPortBlock.UPPER, true));
    s.set(-13, 123, 38, SpaceContent.LIFE_SUPPORT.get().defaultBlockState());
  }

  public static boolean ready(ServerLevel l) {
    return l.getBlockState(PORT).is(ShipInteriorContent.DOCK_PORT.get())
        && l.getBlockState(PORT.above()).is(ShipInteriorContent.DOCK_PORT.get());
  }

  public static boolean refit(ServerLevel l) {
    if (ready(l)) return true;
    Map<BlockPos, BlockState> plan = new LinkedHashMap<>();
    build((x, y, z, state) -> plan.put(new BlockPos(x, y, z), state));
    for (var e : plan.entrySet()) {
      var at = e.getKey();
      var actual = l.getBlockState(at);
      if (actual.equals(e.getValue())) continue;
      boolean stockOpening =
          at.getX() == -15
              && at.getZ() == 2
              && at.getY() >= 121
              && at.getY() <= 123
              && (actual.is(MetalContent.HULL.get()) || actual.is(MetalContent.VIEWPORT.get()));
      if ((!actual.isAir() && !stockOpening)
          || !l.getFluidState(at).isEmpty()
          || l.getBlockEntity(at) != null
          || !l.getEntities(
                  (net.minecraft.world.entity.Entity) null,
                  new net.minecraft.world.phys.AABB(at),
                  e2 -> e2.isAlive())
              .isEmpty()) return false;
    }
    ShipRefitBackup.save(l, new BlockPos(-15, 120, 20));
    plan.forEach((at, state) -> l.setBlock(at, state, 3));
    return true;
  }

  private OrbitalDock083() {}
}
