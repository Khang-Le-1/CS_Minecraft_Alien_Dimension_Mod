package com.timelocked;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Voidspace beds are safe respawn checkpoints, without advancing its fixed night. */
@EventBusSubscriber(modid = "timelocked")
public final class VoidspaceBeds {
  @SubscribeEvent(priority = EventPriority.LOWEST)
  public static void useBed(PlayerInteractEvent.RightClickBlock event) {
    Level level = event.getLevel();
    if (!level.dimension().equals(VoidspaceTravel.DIMENSION)) return;
    BlockState state = level.getBlockState(event.getPos());
    if (!(state.getBlock() instanceof BedBlock)) return;
    // Keep normal sneaking-with-an-item placement behavior.
    if (event.getEntity().isSecondaryUseActive()
        && (!event.getEntity().getMainHandItem().isEmpty()
            || !event.getEntity().getOffhandItem().isEmpty())) return;
    if (level.isClientSide) {
      event.setCanceled(true);
      event.setCancellationResult(InteractionResult.SUCCESS);
      return;
    }
    if (!(event.getEntity() instanceof ServerPlayer player) || player.isSpectator()) return;
    // Respect a protection/interaction handler that denied using this block.
    if (event.getUseBlock() == net.neoforged.neoforge.common.util.TriState.FALSE) return;
    BlockPos head = headPosition(event.getPos(), state);
    BlockState headState = level.getBlockState(head);
    BlockPos foot = head.relative(state.getValue(BedBlock.FACING).getOpposite());
    BlockState footState = level.getBlockState(foot);
    event.setCanceled(true);
    event.setCancellationResult(InteractionResult.SUCCESS);
    if (!headState.is(state.getBlock())
        || headState.getValue(BedBlock.PART) != BedPart.HEAD
        || !footState.is(state.getBlock())
        || footState.getValue(BedBlock.PART) != BedPart.FOOT
        || footState.getValue(BedBlock.FACING) != headState.getValue(BedBlock.FACING)) return;
    if (headState.getValue(BedBlock.OCCUPIED)) {
      player.displayClientMessage(Component.translatable("block.minecraft.bed.occupied"), true);
      return;
    }
    setCheckpoint(player, head);
  }

  static BlockPos headPosition(BlockPos clicked, BlockState state) {
    return state.getValue(BedBlock.PART) == BedPart.HEAD
        ? clicked
        : clicked.relative(state.getValue(BedBlock.FACING));
  }

  static void setCheckpoint(ServerPlayer player, BlockPos head) {
    // Not a forced spawn: vanilla still requires an intact bed and a safe exit.
    player.setRespawnPosition(VoidspaceTravel.DIMENSION, head, player.getYRot(), false, true);
    player.displayClientMessage(
        Component.translatable("message.timelocked.voidspace_bed_rest"), true);
  }

  private VoidspaceBeds() {}
}
