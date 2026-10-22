package com.timelocked;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Chunk-ready barrier: the server owns gravity, the saved anchor and release. */
public final class IntroLanding {
  // Minecraft chunks are 16 blocks wide; load one neighbor on each side (3 x 3).
  public static final int CHUNK_COORDINATE_SHIFT = 4;
  public static final int READY_CHUNK_RADIUS = 1;
  // Squared block distance: tolerate rounding, but correct any meaningful landing drift.
  private static final double HOLD_POSITION_EPSILON_SQUARED = 0.0001;

  static void preload(ServerLevel l, BlockPos p) {
    for (int x = -READY_CHUNK_RADIUS; x <= READY_CHUNK_RADIUS; x++)
      for (int z = -READY_CHUNK_RADIUS; z <= READY_CHUNK_RADIUS; z++)
        l.getChunk(
            (p.getX() >> CHUNK_COORDINATE_SHIFT) + x, (p.getZ() >> CHUNK_COORDINATE_SHIFT) + z);
  }

  public static boolean supported(Level l, BlockPos p) {
    return l.getWorldBorder().isWithinBounds(p)
        && l.getBlockState(p).getCollisionShape(l, p).isEmpty()
        && l.getFluidState(p).isEmpty()
        && l.getBlockState(p.above()).getCollisionShape(l, p.above()).isEmpty()
        && l.getFluidState(p.above()).isEmpty()
        && l.getBlockState(p.below()).isFaceSturdy(l, p.below(), Direction.UP);
  }

  static boolean hold(ServerPlayer p, CompoundTag t) {
    BlockPos q = BlockPos.of(t.getLong("Anchor"));
    if (!supported(p.serverLevel(), q)) return false;
    p.setNoGravity(true);
    p.setDeltaMovement(Vec3.ZERO);
    p.fallDistance = 0;
    double x = q.getX() + .5, y = q.getY(), z = q.getZ() + .5;
    if (p.position().distanceToSqr(new Vec3(x, y, z)) > HOLD_POSITION_EPSILON_SQUARED)
      p.teleportTo(p.serverLevel(), x, y, z, p.getYRot(), p.getXRot());
    p.setOnGround(true);
    return true;
  }

  private IntroLanding() {}
}
