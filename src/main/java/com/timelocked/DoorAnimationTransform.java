package com.timelocked;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.phys.Vec3;

public final class DoorAnimationTransform {
  public static float base(Direction face) {
    return switch (face) {
      case EAST -> 0.0F;
      case SOUTH -> 90.0F;
      case WEST -> 180.0F;
      default -> 270.0F;
    };
  }

  public static Vec3 point(Vec3 v, Direction facing, DoorHingeSide hinge, float progress) {
    double t = Math.clamp(progress, 0.0F, 1.0F);
    double angle = (hinge == DoorHingeSide.LEFT ? 1 : -1) * t * Math.PI / 2.0;
    double pivot = hinge == DoorHingeSide.LEFT ? 0.0 : 1.0;
    double x = v.x * Math.cos(angle) + (v.z - pivot) * Math.sin(angle);
    double z =
        -v.x * Math.sin(angle)
            + (v.z - pivot) * Math.cos(angle)
            + pivot
            + (hinge == DoorHingeSide.LEFT ? 1 : -1) * 0.1875 * t;
    double yaw = -base(facing) * Math.PI / 180.0;
    return new Vec3(
        0.5 + (x - 0.5) * Math.cos(yaw) + (z - 0.5) * Math.sin(yaw),
        v.y,
        0.5 - (x - 0.5) * Math.sin(yaw) + (z - 0.5) * Math.cos(yaw));
  }

  private DoorAnimationTransform() {}
}
