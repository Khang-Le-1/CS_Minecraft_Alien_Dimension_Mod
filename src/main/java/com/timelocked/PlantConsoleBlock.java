package com.timelocked;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;

public final class PlantConsoleBlock extends HorizontalDirectionalBlock {
  public static final MapCodec<PlantConsoleBlock> CODEC = simpleCodec(PlantConsoleBlock::new);

  public PlantConsoleBlock(Properties p) {
    super(p);
    this.registerDefaultState(
        (BlockState) ((BlockState) this.stateDefinition.any()).setValue(FACING, Direction.NORTH));
  }

  protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
    return CODEC;
  }

  protected void createBlockStateDefinition(Builder<Block, BlockState> b) {
    b.add(new Property[] {FACING});
  }

  public BlockState getStateForPlacement(BlockPlaceContext c) {
    return (BlockState)
        this.defaultBlockState().setValue(FACING, c.getHorizontalDirection().getOpposite());
  }

  public static PlantCoreBlock.Core nearestCore(Level l, BlockPos p) {
    // Generated station consoles have an exact plan-derived target beyond the legacy range.
    if (l.getBlockState(p).is(SarcContent.STATION_CONSOLE.get())) {
      int[] core = PowerStationGeometry.PLAN.core(),
          control = PowerStationGeometry.PLAN.gallery_console();
      for (int dx = -2; dx <= 2; dx++) {
        BlockPos q =
            p.offset(core[0] - control[0] + dx, core[1] - control[1], core[2] - control[2]);
        if (l.hasChunkAt(q) && l.getBlockEntity(q) instanceof PlantCoreBlock.Core c && c.industrial)
          return c;
      }
    }
    PlantCoreBlock.Core best = null;
    double bd = Double.MAX_VALUE;

    for (BlockPos q : BlockPos.betweenClosed(p.offset(-12, -6, -12), p.offset(12, 6, 12))) {
      if (l.hasChunkAt(q) && l.getBlockEntity(q) instanceof PlantCoreBlock.Core c) {
        double d = q.distSqr(p);
        if (d < bd) {
          bd = d;
          best = c;
        }
      }
    }

    return best;
  }

  protected InteractionResult useWithoutItem(
      BlockState s, Level l, BlockPos p, Player user, BlockHitResult hit) {
    if (l.isClientSide) {
      return InteractionResult.SUCCESS;
    }

    PlantCoreBlock.Core core = nearestCore(l, p);
    if (core == null) {
      user.displayClientMessage(
          Component.literal("[SARC] No loaded core linked to this console."), true);
      return InteractionResult.CONSUME;
    }

    if (user.isSecondaryUseActive()) {
      core.scram();
      l.playSound(null, p, SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 1.2F, 0.6F);
      user.displayClientMessage(
          Component.literal("[SARC] SCRAM! All control rods inserted. " + core.status()), true);
      if (core.critical() && user instanceof ServerPlayer sp) {
        Achievements.grant(sp, "not_today");
      }
    } else {
      core.withdraw();
      l.playSound(null, p, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 1.0F, 1.2F);
      user.displayClientMessage(Component.literal("[SARC] Rod withdrawn. " + core.status()), true);
    }

    return InteractionResult.CONSUME;
  }
}
