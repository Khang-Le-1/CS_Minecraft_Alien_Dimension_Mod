package com.timelocked;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** The rotor angle is client-rendered from world time, not serialized or server-ticked. */
public final class AlarmRotorEntity extends BlockEntity {
  public AlarmRotorEntity(BlockPos p, BlockState s) {
    super(FacilityDetails.ALARM_ROTOR_ENTITY.get(), p, s);
  }
}
