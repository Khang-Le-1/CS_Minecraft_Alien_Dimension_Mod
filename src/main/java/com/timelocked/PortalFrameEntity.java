package com.timelocked;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public final class PortalFrameEntity extends BlockEntity {
  public PortalFrameEntity(BlockPos p, BlockState s) {
    super((BlockEntityType) FacilityDetails.PORTAL_ENTITY.get(), p, s);
  }
}
