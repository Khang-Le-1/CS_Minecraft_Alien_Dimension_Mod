package com.timelocked;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class SupportStationBlock extends Block {
  public static final MapCodec<SupportStationBlock> CODEC = simpleCodec(SupportStationBlock::new);

  public SupportStationBlock(Properties p) {
    super(p);
  }

  protected MapCodec<? extends SupportStationBlock> codec() {
    return CODEC;
  }

  protected InteractionResult useWithoutItem(
      BlockState s, Level level, BlockPos pos, Player p, BlockHitResult hit) {
    if (level.isClientSide) {
      return InteractionResult.SUCCESS;
    }

    if (!SitePlanner.powered((ServerLevel) level, pos)) {
      p.sendSystemMessage(Component.literal("Station offline: restore Reactor Ops."));
      return InteractionResult.CONSUME;
    }

    boolean medical = s.is((Block) Content.MEDICAL.get());
    String key = medical ? "timelocked_medical_ready" : "timelocked_decon_ready";
    long now = level.getServer().overworld().getGameTime();
    if (p.getPersistentData().getLong(key) > now) {
      p.displayClientMessage(Component.literal("Station recovering; try again shortly."), true);
      return InteractionResult.CONSUME;
    }

    if (medical) {
      if (!p.getMainHandItem().is((Item) Content.BANDAGE.get())) {
        p.sendSystemMessage(
            Component.literal(
                "Medical station: hold a bandage in your main hand, then use the station."));
        return InteractionResult.CONSUME;
      }

      if (p.getHealth() >= p.getMaxHealth()) {
        return InteractionResult.CONSUME;
      }

      if (!p.getAbilities().instabuild) {
        p.getMainHandItem().shrink(1);
      }

      p.heal(6.0F);
      p.getPersistentData().putLong(key, now + 200L);
      p.sendSystemMessage(
          Component.literal("Medical treatment complete: restored up to 3 hearts."));
    } else {
      p.removeEffect(MobEffects.POISON);
      p.removeEffect(MobEffects.WITHER);
      p.removeEffect(MobEffects.WEAKNESS);
      p.getPersistentData().putLong(key, now + 600L);
      p.sendSystemMessage(
          Component.literal("Decontamination complete: poison, wither and weakness cleared."));
    }

    return InteractionResult.CONSUME;
  }
}
