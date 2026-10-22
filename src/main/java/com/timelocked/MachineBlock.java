package com.timelocked;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public abstract class MachineBlock extends BaseEntityBlock {
  protected MachineBlock(Properties p) {
    super(p);
  }

  protected RenderShape getRenderShape(BlockState s) {
    return RenderShape.MODEL;
  }

  protected InteractionResult useWithoutItem(
      BlockState s, Level level, BlockPos p, Player player, BlockHitResult hit) {
    if (!level.isClientSide && level.getBlockEntity(p) instanceof MachineEntity be) {
      player.openMenu(be);
      player.displayClientMessage(Component.literal(be.status()), true);
    }

    return InteractionResult.sidedSuccess(level.isClientSide);
  }

  protected void onRemove(
      BlockState s, Level level, BlockPos pos, BlockState next, boolean moving) {
    if (!s.is(next.getBlock()) && level.getBlockEntity(pos) instanceof MachineEntity be) {
      Containers.dropContents(level, pos, be);
      level.updateNeighbourForOutputSignal(pos, this);
    }

    super.onRemove(s, level, pos, next, moving);
  }
}
