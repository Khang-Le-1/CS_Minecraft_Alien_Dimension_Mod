package com.timelocked;

import com.mojang.serialization.MapCodec;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Power-independent, two-bank interlock. Each bank is two leaves; the other bank is five blocks
 * away.
 */
public final class ReactorAirlockBlock extends AnimatedSarcDoorBlock {
  public static final IntegerProperty PURGE = IntegerProperty.create("purge", 0, 4);
  public static final MapCodec<ReactorAirlockBlock> CODEC = simpleCodec(ReactorAirlockBlock::new);

  public ReactorAirlockBlock(BlockBehaviour.Properties p) {
    super(p);
    registerDefaultState(defaultBlockState().setValue(PURGE, 0));
  }

  public MapCodec<? extends AnimatedSarcDoorBlock> codec() {
    return CODEC;
  }

  protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
    super.createBlockStateDefinition(b);
    b.add(PURGE);
  }

  private static BlockPos lower(BlockPos p, BlockState s) {
    return s.getValue(HALF) == DoubleBlockHalf.UPPER ? p.below() : p;
  }

  private List<BlockPos> bank(Level l, BlockPos p) {
    BlockState s = l.getBlockState(p);
    if (!s.is(this)) return List.of();
    Direction along = s.getValue(FACING).getClockWise();
    var out = new ArrayList<BlockPos>();
    for (int d = -1; d <= 1; d++) {
      BlockPos q = p.relative(along, d);
      if (l.hasChunkAt(q)
          && l.getBlockState(q).is(this)
          && l.getBlockState(q).getValue(HALF) == DoubleBlockHalf.LOWER
          && l.getBlockState(q).getValue(FACING) == s.getValue(FACING)) out.add(q);
    }
    return out;
  }

  private List<BlockPos> partner(Level l, BlockPos p) {
    Direction axis = l.getBlockState(p).getValue(FACING);
    for (int d : new int[] {5, -5}) {
      BlockPos q = p.relative(axis, d);
      if (l.hasChunkAt(q)
          && l.getBlockState(q).is(this)
          && l.getBlockState(q).getValue(HALF) == DoubleBlockHalf.LOWER) return bank(l, q);
    }
    return List.of();
  }

  private void state(Level l, List<BlockPos> bank, boolean open, int purge) {
    for (BlockPos q : bank) {
      if (l instanceof ServerLevel server)
        server
            .getBlockTicks()
            .clearArea(
                new net.minecraft.world.level.levelgen.structure.BoundingBox(
                    q.getX(), q.getY(), q.getZ(), q.getX(), q.getY(), q.getZ()));
      BlockState s = l.getBlockState(q);
      if (!s.is(this)) continue;
      setOpen(null, l, s, q, open);
      for (BlockPos h : new BlockPos[] {q, q.above()})
        if (l.getBlockState(h).is(this))
          l.setBlock(h, l.getBlockState(h).setValue(PURGE, purge), 3);
    }
  }

  protected InteractionResult useWithoutItem(
      BlockState s, Level l, BlockPos p, Player user, BlockHitResult hit) {
    if (l.isClientSide) return InteractionResult.SUCCESS;
    p = lower(p, s);
    s = l.getBlockState(p);
    var own = bank(l, p);
    if (s.getValue(OPEN) || user.isSecondaryUseActive()) {
      state(l, own, false, 0);
      return InteractionResult.CONSUME;
    }
    if (s.getValue(PURGE) > 0) {
      user.displayClientMessage(
          Component.literal("[AIRLOCK] Purge cycle in progress. Sneak + use to cancel."), true);
      return InteractionResult.CONSUME;
    }
    var other = partner(l, p);
    if (other.size() != 2 || own.size() != 2) {
      user.displayClientMessage(
          Component.literal(
              "[AIRLOCK] Interlock incomplete or partner bank unloaded. Both two-leaf banks must be five blocks apart."),
          true);
      return InteractionResult.CONSUME;
    }
    state(l, other, false, 0);
    state(l, own, false, 4);
    l.scheduleTick(p, this, 5);
    l.playSound(null, p, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 0.7F, 0.7F);
    user.displayClientMessage(
        Component.literal("[AIRLOCK] Closing opposite bank. Purging vestibule before opening."),
        true);
    return InteractionResult.CONSUME;
  }

  protected void neighborChanged(
      BlockState s,
      Level l,
      BlockPos p,
      Block b,
      BlockPos from,
      boolean moving) {} // redstone cannot bypass the interlock

  protected void onPlace(BlockState s, Level l, BlockPos p, BlockState old, boolean moving) {
    super.onPlace(s, l, p, old, moving);
    if (!l.isClientSide
        && s.getValue(HALF) == DoubleBlockHalf.LOWER
        && s.getValue(PURGE) > 0
        && !old.is(this)) l.scheduleTick(p, this, 5);
  }

  protected void tick(BlockState s, ServerLevel l, BlockPos p, RandomSource random) {
    if (s.getValue(HALF) != DoubleBlockHalf.LOWER) return;
    var own = bank(l, p);
    int purge = s.getValue(PURGE);
    if (purge > 0) {
      var other = partner(l, p);
      if (other.size() != 2 || own.size() != 2) {
        state(l, own, false, 0);
        return;
      }
      if (other.stream()
          .anyMatch(
              q -> l.getBlockState(q).getValue(OPEN) || l.getBlockState(q).getValue(PURGE) > 0)) {
        state(l, other, false, 0);
        state(l, own, false, 4);
        l.scheduleTick(p, this, 5);
        return;
      }
      if (purge > 1) {
        state(l, own, false, purge - 1);
        l.scheduleTick(p, this, 5);
      } else {
        state(l, own, true, 0);
        l.scheduleTick(p, this, 80);
      }
      return;
    }
    if (s.getValue(OPEN)) state(l, own, false, 0);
  }
}
