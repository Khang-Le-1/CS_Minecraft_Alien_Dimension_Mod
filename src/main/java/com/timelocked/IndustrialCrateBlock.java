package com.timelocked;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;

public final class IndustrialCrateBlock extends MachineBlock {
  public static final MapCodec<IndustrialCrateBlock> CODEC = simpleCodec(IndustrialCrateBlock::new);

  public IndustrialCrateBlock(Properties p) {
    super(p);
  }

  protected MapCodec<? extends IndustrialCrateBlock> codec() {
    return CODEC;
  }

  public BlockEntity newBlockEntity(BlockPos p, BlockState s) {
    return new IndustrialCrateEntity(p, s);
  }
}
