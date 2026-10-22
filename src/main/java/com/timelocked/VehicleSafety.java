package com.timelocked;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class VehicleSafety {
  public static boolean loaded(Level l, AABB box) {
    for (int x = (int) Math.floor(box.minX) >> 4; x <= (int) Math.floor(box.maxX) >> 4; x++) {
      for (int z = (int) Math.floor(box.minZ) >> 4; z <= (int) Math.floor(box.maxZ) >> 4; z++) {
        if (!l.hasChunk(x, z)) {
          return false;
        }
      }
    }

    return l.getWorldBorder().isWithinBounds(box);
  }

  public static boolean deployment(Entity e) {
    Level l = e.level();
    AABB b = e.getBoundingBox();
    if (loaded(l, b.inflate(0.1)) && l.noCollision(e, b) && !l.containsAnyLiquid(b)) {
      for (double x : new double[] {b.minX + 0.1, b.maxX - 0.1}) {
        for (double z : new double[] {b.minZ + 0.1, b.maxZ - 0.1}) {
          BlockPos p = BlockPos.containing(x, b.minY - 0.05, z);
          if (!l.getBlockState(p).isFaceSturdy(l, p, Direction.UP)
              || !l.getFluidState(p).isEmpty()) {
            return false;
          }
        }
      }

      return true;
    } else {
      return false;
    }
  }

  public static Optional<Vec3> exit(Entity v, LivingEntity rider) {
    Level l = v.level();
    double radius = v.getBbWidth() / 2.0F + 0.85;

    for (int ring = 0; ring < 3; ring++) {
      for (int i = 0; i < 8; i++) {
        for (int dy : new int[] {0, 1, -1}) {
          double angle = i * Math.PI / 4.0;
          BlockPos p =
              BlockPos.containing(
                  v.getX() + Math.cos(angle) * (radius + ring),
                  v.getY() + dy,
                  v.getZ() + Math.sin(angle) * (radius + ring));
          Vec3 feet = new Vec3(p.getX() + 0.5, p.getY(), p.getZ() + 0.5);
          AABB b = rider.getDimensions(Pose.STANDING).makeBoundingBox(feet);
          if (!b.intersects(v.getBoundingBox())
              && loaded(l, b.inflate(0.1))
              && l.getBlockState(p.below()).isFaceSturdy(l, p.below(), Direction.UP)
              && !l.containsAnyLiquid(b)
              && l.noCollision(rider, b)) {
            return Optional.of(feet);
          }
        }
      }
    }

    return Optional.empty();
  }

  private VehicleSafety() {}
}
