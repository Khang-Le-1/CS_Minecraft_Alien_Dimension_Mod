package com.timelocked;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;

public final class PowerSupplyBlock extends MachineBlock {
  public static final MapCodec<PowerSupplyBlock> CODEC = simpleCodec(PowerSupplyBlock::new);

  public PowerSupplyBlock(Properties p) {
    super(p);
  }

  protected MapCodec<? extends PowerSupplyBlock> codec() {
    return CODEC;
  }

  public BlockEntity newBlockEntity(BlockPos p, BlockState s) {
    return new PowerSupplyEntity(p, s);
  }

  public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
      Level l, BlockState s, BlockEntityType<T> t) {
    return l.isClientSide
        ? null
        : createTickerHelper(t, Content.POWER_ENTITY.get(), (w, p, state, be) -> be.tick());
  }
}
