package com.timelocked;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;

public final class ElectricArcFurnaceBlock extends MachineBlock {
  public static final MapCodec<ElectricArcFurnaceBlock> CODEC =
      simpleCodec(ElectricArcFurnaceBlock::new);

  public ElectricArcFurnaceBlock(Properties p) {
    super(p);
  }

  protected MapCodec<? extends ElectricArcFurnaceBlock> codec() {
    return CODEC;
  }

  public BlockEntity newBlockEntity(BlockPos p, BlockState s) {
    return new ArcFurnaceEntity(p, s);
  }

  public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
      Level l, BlockState s, BlockEntityType<T> t) {
    return l.isClientSide
        ? null
        : createTickerHelper(t, Content.EAF_ENTITY.get(), (w, p, state, be) -> be.tick());
  }
}
