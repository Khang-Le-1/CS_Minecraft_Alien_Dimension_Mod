package com.timelocked;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;

/** Compact service deck inside the original hull; two clear metres under the upper floor. */
public final class TimeskipperDecks {
  public static final BlockPos SPAWN = new BlockPos(0, -2, 0);

  public static void finish(TimeskipperShip.Sink s, int X, int Y, int Z) {
    for (int z = 6; z <= 9; z++)
      for (int x = -4; x <= -3; x++) {
        int y = z - 9;
        for (int h = 1; h <= 3; h++) s.set(X + x, Y + y + h, Z + z, Blocks.AIR.defaultBlockState());
        s.set(
            X + x,
            Y + y,
            Z + z,
            Content.STAIRS.get().defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH));
      }
    s.screen(
        X - 4,
        Y - 2,
        Z + 18,
        Direction.EAST,
        0,
        "LOWER SERVICE",
        "DRIVE: UPPER DECK",
        "CARGO STAIRS: FORWARD",
        "SEALED TRANSFER DOOR");
    s.set(X - 4, Y - 2, Z + 18, MonitorContent.wall(Direction.EAST));
  }

  public static boolean twoDeck(net.minecraft.world.level.Level l) {
    return l.getBlockState(Timeskipper.ORIGIN.offset(0, -2, -4)).is(ShipInteriorContent.PATH.get())
        && l.getBlockState(Timeskipper.ORIGIN.offset(SPAWN)).isAir()
        && l.getBlockState(Timeskipper.ORIGIN.offset(SPAWN).above()).isAir()
        && !l.getBlockState(Timeskipper.ORIGIN.offset(SPAWN).below())
            .getCollisionShape(l, Timeskipper.ORIGIN.offset(SPAWN).below())
            .isEmpty();
  }

  public static boolean ready(net.minecraft.world.level.Level l, BlockPos o) {
    if (!twoDeck(l)) return false;
    for (int x = -4; x <= -3; x++) {
      boolean clear = true;
      for (int z = 6; z <= 9; z++) {
        var at = o.offset(x, z - 9, z);
        if (!l.getBlockState(at).is(Content.STAIRS.get())
            || !l.getBlockState(at.above()).isAir()
            || !l.getBlockState(at.above(2)).isAir()) clear = false;
      }
      if (clear) return true;
    }
    return false;
  }

  public static void initialize(net.minecraft.server.level.ServerLevel l, BlockPos o) {
    var p = o.offset(-4, -2, 18);
    if (l.getBlockEntity(p) instanceof MonitorContent.MonitorEntity m && m.line(0).isEmpty()) {
      m.setLines(
          new String[] {
            "LOWER SERVICE", "DRIVE: UPPER DECK", "CARGO STAIRS: FORWARD", "SEALED TRANSFER DOOR"
          });
      m.setTheme(0);
    }
  }

  private TimeskipperDecks() {}
}
