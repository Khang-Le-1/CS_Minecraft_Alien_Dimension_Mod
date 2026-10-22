package com.timelocked;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.phys.BlockHitResult;

public final class AccessDoorBlock extends AnimatedSarcDoorBlock {
  public static final MapCodec<AccessDoorBlock> CODEC = simpleCodec(AccessDoorBlock::new);

  public AccessDoorBlock(Properties p) {
    super(BlockSetType.IRON, p);
  }

  @Override
  public MapCodec<? extends AnimatedSarcDoorBlock> codec() {
    return CODEC;
  }

  protected InteractionResult useWithoutItem(
      BlockState s, Level l, BlockPos p, Player player, BlockHitResult hit) {
    if (l.isClientSide) {
      return InteractionResult.SUCCESS;
    } else {
      boolean airlock = s.is((Block) Content.AIRLOCK.get());
      boolean allowed =
          airlock
              ? SitePlanner.powered((ServerLevel) l, p)
              : player.getMainHandItem().is((Item) Content.KEYCARD.get());
      if (!allowed) {
        player.sendSystemMessage(
            Component.literal(
                airlock
                    ? "Airlock offline. Restore power in Wing 4 Reactor Ops."
                    : "Security checkpoint: hold the SARC access card found in Wing 3."));
        return InteractionResult.CONSUME;
      } else {
        this.setOpen(player, l, s, p, !(Boolean) s.getValue(OPEN));
        return InteractionResult.CONSUME;
      }
    }
  }

  protected void neighborChanged(
      BlockState s, Level l, BlockPos p, Block b, BlockPos from, boolean moving) {}
}
