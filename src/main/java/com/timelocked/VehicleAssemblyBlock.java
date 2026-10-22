package com.timelocked;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;

public final class VehicleAssemblyBlock extends MachineBlock {
  public static final MapCodec<VehicleAssemblyBlock> CODEC = simpleCodec(VehicleAssemblyBlock::new);

  public VehicleAssemblyBlock(Properties p) {
    super(p);
  }

  protected MapCodec<? extends VehicleAssemblyBlock> codec() {
    return CODEC;
  }

  public BlockEntity newBlockEntity(BlockPos p, BlockState s) {
    return new VehicleAssemblyEntity(p, s);
  }

  public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
      Level l, BlockState s, BlockEntityType<T> type) {
    return l.isClientSide
        ? null
        : createTickerHelper(
            type, WorkshopContent.ASSEMBLY_ENTITY.get(), (level, pos, state, be) -> be.tick());
  }
}
