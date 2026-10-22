package com.timelocked;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class ReactorFuelBlock extends BaseEntityBlock {
  public static final MapCodec<ReactorFuelBlock> CODEC = simpleCodec(ReactorFuelBlock::new);

  public ReactorFuelBlock(Properties p) {
    super(p);
  }

  protected MapCodec<? extends ReactorFuelBlock> codec() {
    return CODEC;
  }

  protected RenderShape getRenderShape(BlockState s) {
    return RenderShape.MODEL;
  }

  public BlockEntity newBlockEntity(BlockPos p, BlockState s) {
    return new ReactorFuelEntity(p, s);
  }

  protected InteractionResult useWithoutItem(
      BlockState s, Level l, BlockPos p, Player user, BlockHitResult hit) {
    if (!l.isClientSide && l.getBlockEntity(p) instanceof ReactorFuelEntity be) {
      user.openMenu(be);
    }

    return InteractionResult.sidedSuccess(l.isClientSide);
  }

  protected void onRemove(BlockState s, Level l, BlockPos p, BlockState next, boolean moving) {
    if (!s.is(next.getBlock()) && l.getBlockEntity(p) instanceof ReactorFuelEntity be) {
      Containers.dropContents(l, p, be);
      l.updateNeighbourForOutputSignal(p, this);
    }

    super.onRemove(s, l, p, next, moving);
  }
}
