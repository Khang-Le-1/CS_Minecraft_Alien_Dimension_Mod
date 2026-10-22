package com.timelocked;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class TimeskipperRefit {
  static BlockState facing(Block b, Direction d) {
    return (BlockState) b.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, d);
  }

  private static void add(
      List<TimeskipperRefit.Part> list, int x, int z, Block before, Block after, Direction face) {
    list.add(
        new TimeskipperRefit.Part(
            new BlockPos(x, 1, z), before.defaultBlockState(), facing(after, face)));
  }

  public static List<TimeskipperRefit.Part> parts() {
    List<TimeskipperRefit.Part> p = new ArrayList<>();

    for (int x : new int[] {-3, 0, 3}) {
      add(
          p,
          x,
          -15,
          Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS,
          (Block) ShipInteriorContent.FLIGHT_SEAT.get(),
          Direction.NORTH);
      p.add(
          new TimeskipperRefit.Part(
              new BlockPos(x, 1, -14),
              Blocks.POLISHED_BLACKSTONE_BRICK_WALL.defaultBlockState(),
              Blocks.AIR.defaultBlockState()));
    }

    add(
        p,
        -4,
        1,
        Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS,
        (Block) ShipInteriorContent.FLIGHT_SEAT.get(),
        Direction.WEST);
    p.add(
        new TimeskipperRefit.Part(
            new BlockPos(-3, 1, 1),
            Blocks.POLISHED_BLACKSTONE_BRICK_WALL.defaultBlockState(),
            Blocks.AIR.defaultBlockState()));
    add(
        p,
        -6,
        -6,
        (Block) Content.LIGHT.get(),
        (Block) ShipInteriorContent.CREW_LOCKER.get(),
        Direction.EAST);
    add(p, -6, -11, Blocks.AIR, (Block) ShipInteriorContent.CREW_LOCKER.get(), Direction.EAST);
    add(p, -3, -10, Blocks.AIR, (Block) ShipInteriorContent.BEDSIDE_UNIT.get(), Direction.EAST);
    add(p, -3, -7, Blocks.AIR, (Block) ShipInteriorContent.BEDSIDE_UNIT.get(), Direction.EAST);
    add(
        p,
        4,
        -10,
        Blocks.BREWING_STAND,
        (Block) ShipInteriorContent.MEDICAL_CART.get(),
        Direction.SOUTH);
    add(p, 5, -10, Blocks.CAULDRON, (Block) ShipInteriorContent.CREW_LOCKER.get(), Direction.SOUTH);
    add(
        p,
        -5,
        2,
        Blocks.SMOOTH_STONE_SLAB,
        (Block) ShipInteriorContent.GALLEY_TABLE.get(),
        Direction.EAST);
    add(p, -6, -3, Blocks.AIR, (Block) ShipInteriorContent.DRINKS_STATION.get(), Direction.EAST);

    for (int x : new int[] {-4, 4}) {
      add(
          p,
          x,
          6,
          Blocks.AIR,
          (Block) ShipInteriorContent.CARGO_RACK.get(),
          x < 0 ? Direction.EAST : Direction.WEST);
    }

    for (int x : new int[] {-4, 4}) {
      add(p, x, 19, Blocks.AIR, (Block) ShipInteriorContent.COOLANT_PUMP.get(), Direction.NORTH);
    }

    return List.copyOf(p);
  }

  public static void furnish(TimeskipperShip.Sink out, int X, int Y, int Z) {
    for (TimeskipperRefit.Part p : parts()) {
      out.set(X + p.pos.getX(), Y + p.pos.getY(), Z + p.pos.getZ(), p.after);
    }
  }

  public static TimeskipperRefit.Result apply(ServerLevel ship, BlockPos o) {
    ShipRefitBackup.save(ship, o);
    // Station changes occur only within this explicitly confirmed, backup-first refit.
    var orbit = ship.getServer().getLevel(SpacePlanets.Planet.ORBIT.dimension());
    if (orbit != null
        && Timeskipper.data(ship.getServer()).pads.containsKey(SpacePlanets.Planet.ORBIT.id))
      OrbitalDock083.refit(orbit);
    int changed = 0;
    int protectedCells = 0;

    for (int x = o.getX() - 8 >> 4; x <= o.getX() + 8 >> 4; x++) {
      for (int z = o.getZ() - 24 >> 4; z <= o.getZ() + 22 >> 4; z++) {
        ship.getChunk(x, z);
      }
    }

    for (TimeskipperRefit.Part part : parts()) {
      BlockPos at = o.offset(part.pos);
      BlockState state = ship.getBlockState(at);
      if (!state.equals(part.after)) {
        if (state.getBlock() != part.before.getBlock()) {
          protectedCells++;
        } else {
          BlockEntity be = ship.getBlockEntity(at);
          if (be == null || be instanceof Container c && c.isEmpty()) {
            if (!part.after.isAir()) {
              VoxelShape shape = part.after.getShape(ship, at);
              if (shape.max(Axis.Y) > 1.0 && !ship.getBlockState(at.above()).isAir()) {
                protectedCells++;
                continue;
              }

              if (!ship.getEntities(
                      (Entity) null, new AABB(at).inflate(0.0, 0.4, 0.0), e -> e.isAlive())
                  .isEmpty()) {
                protectedCells++;
                continue;
              }
            }

            ship.setBlock(at, part.after, 3);
            changed++;
          } else {
            protectedCells++;
          }
        }
      }
    }

    TimeskipperBridgeScreens.Result screens = TimeskipperBridgeScreens.fit(ship, o);
    TimeskipperBigScreens.Result bigScreens = TimeskipperBigScreens.fit(ship, o);
    TimeskipperInteriorStyle.Result interior = TimeskipperInteriorStyle.apply(ship, o);
    TimeskipperDecks.initialize(ship, o);
    if (TimeskipperDecks.ready(ship, o)) {
      BlockPos anchor = o.offset(TimeskipperShip.MODEL_ANCHOR);
      if (ship.getBlockState(anchor).is(SpaceContent.MODEL_ANCHOR.get()))
        ship.setBlock(
            anchor, ship.getBlockState(anchor).setValue(ShipModelAnchorBlock.NEW_LAYOUT, true), 3);
    }
    return new TimeskipperRefit.Result(
        changed + interior.changed() + screens.changed() + bigScreens.changed(),
        protectedCells
            + interior.protectedCells()
            + screens.protectedCells()
            + bigScreens.protectedCells());
  }

  private TimeskipperRefit() {}

  public record Part(BlockPos pos, BlockState before, BlockState after) {}

  public record Result(int changed, int protectedCells) {}
}
