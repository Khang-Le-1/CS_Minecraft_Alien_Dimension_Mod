package com.timelocked;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Manual, unlocked egress door. Paired leaves move together; no proximity sensor spoils the reveal.
 */
public final class PushBarDoorBlock extends AnimatedSarcDoorBlock {
  private static final int PAIRED_UPDATE_FLAGS = 10;
  public static final MapCodec<PushBarDoorBlock> CODEC = simpleCodec(PushBarDoorBlock::new);

  public PushBarDoorBlock(Properties properties) {
    super(PushBarDoorSounds.TYPE, properties);
  }

  @Override
  public MapCodec<? extends AnimatedSarcDoorBlock> codec() {
    return CODEC;
  }

  static BlockPos mate(Level level, BlockPos lower, BlockState state) {
    Direction facing = state.getValue(FACING);
    for (Direction side : new Direction[] {facing.getClockWise(), facing.getCounterClockWise()}) {
      BlockPos position = lower.relative(side);
      if (!level.hasChunkAt(position)) continue;
      BlockState other = level.getBlockState(position);
      if (other.is(state.getBlock())
          && other.getValue(HALF) == DoubleBlockHalf.LOWER
          && other.getValue(FACING) == facing
          && other.getValue(HINGE) != state.getValue(HINGE)) return position;
    }
    return null;
  }

  @Override
  protected InteractionResult useWithoutItem(
      BlockState state, Level level, BlockPos position, Player player, BlockHitResult hit) {
    if (level.isClientSide) return InteractionResult.SUCCESS;
    BlockPos lower = state.getValue(HALF) == DoubleBlockHalf.UPPER ? position.below() : position;
    BlockState bottom = level.getBlockState(lower);
    if (!bottom.is(this) || bottom.getValue(HALF) != DoubleBlockHalf.LOWER)
      return InteractionResult.PASS;
    boolean open = !bottom.getValue(OPEN);
    if (open
        && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer
        && StaffChapter.deniedEntry(serverPlayer, lower)) {
      player.displayClientMessage(
          net.minecraft.network.chat.Component.translatable(
              "message.timelocked.staff.access_denied"),
          true);
      return InteractionResult.CONSUME;
    }
    BlockPos other = mate(level, lower, bottom);
    // Server-side manual interaction broadcasts to the clicking player too.
    setOpen(null, level, bottom, lower, open);
    if (other != null) {
      BlockState paired = level.getBlockState(other);
      if (paired.getValue(OPEN) != open) {
        // Move the mate with identical vanilla update flags, without layering a second clip.
        level.setBlock(other, paired.setValue(OPEN, open), PAIRED_UPDATE_FLAGS);
        level.gameEvent(
            player,
            open
                ? net.minecraft.world.level.gameevent.GameEvent.BLOCK_OPEN
                : net.minecraft.world.level.gameevent.GameEvent.BLOCK_CLOSE,
            other);
      }
    }
    return InteractionResult.CONSUME;
  }
}
