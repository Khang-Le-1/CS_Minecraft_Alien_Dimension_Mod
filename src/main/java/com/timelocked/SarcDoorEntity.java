package com.timelocked;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class SarcDoorEntity extends BlockEntity {
  public static final float STEP = 0.125F;
  private float previous;
  private float current;
  private boolean initialized;
  // Server-only proximity state. Never changes saved inventories or door NBT.
  long entranceHoldUntil;
  boolean entranceAlignmentChecked;

  public SarcDoorEntity(BlockPos p, BlockState s) {
    super(FacilityDetails.DOOR_ENTITY.get(), p, s);
  }

  public static float advance(float from, boolean open) {
    return open ? Math.min(1.0F, from + 0.125F) : Math.max(0.0F, from - 0.125F);
  }

  public void animate(BlockState s) {
    if (!this.initialized) {
      this.current = this.previous = s.getValue(DoorBlock.OPEN) ? 1.0F : 0.0F;
      this.initialized = true;
    }

    this.previous = this.current;
    this.current = advance(this.current, (Boolean) s.getValue(DoorBlock.OPEN));
  }

  public float progress(float partial) {
    if (!this.initialized) {
      return this.getBlockState().getValue(DoorBlock.OPEN) ? 1.0F : 0.0F;
    } else {
      return this.previous + (this.current - this.previous) * partial;
    }
  }
}
