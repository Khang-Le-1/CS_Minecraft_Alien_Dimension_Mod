package com.timelocked;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;

public final class FactoryBlock extends MachineBlock {
  public static final MapCodec<FactoryBlock> CODEC = simpleCodec(FactoryBlock::new);

  public FactoryBlock(Properties p) {
    super(p);
  }

  protected MapCodec<? extends FactoryBlock> codec() {
    return CODEC;
  }

  public BlockEntity newBlockEntity(BlockPos p, BlockState s) {
    return new FactoryEntity(p, s);
  }

  public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
      Level l, BlockState s, BlockEntityType<T> t) {
    return l.isClientSide
        ? null
        : createTickerHelper(t, MetalContent.FACTORY_ENTITY.get(), (w, p, state, b) -> b.tick());
  }
}
