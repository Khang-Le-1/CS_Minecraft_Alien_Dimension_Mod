package com.timelocked;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class LiftDoorBlock extends AnimatedSarcDoorBlock {
  public LiftDoorBlock(Properties p) {
    super(p);
  }

  protected InteractionResult useWithoutItem(
      BlockState s, Level l, BlockPos p, Player user, BlockHitResult hit) {
    if (!l.isClientSide) {
      user.displayClientMessage(
          Component.literal("Use the lift call panel. Doors open only when the platform docks."),
          true);
    }

    return InteractionResult.sidedSuccess(l.isClientSide);
  }

  protected ItemInteractionResult useItemOn(
      ItemStack stack,
      BlockState s,
      Level l,
      BlockPos p,
      Player user,
      InteractionHand hand,
      BlockHitResult hit) {
    return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
  }

  protected void neighborChanged(
      BlockState s, Level l, BlockPos p, Block b, BlockPos from, boolean moving) {}
}
