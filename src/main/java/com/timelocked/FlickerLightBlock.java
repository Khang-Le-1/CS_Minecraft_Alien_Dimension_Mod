package com.timelocked;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;

public final class FlickerLightBlock extends Block {
  public static final BooleanProperty LIT = BlockStateProperties.LIT;

  public FlickerLightBlock(Properties p) {
    super(p);
    this.registerDefaultState(
        (BlockState) ((BlockState) this.stateDefinition.any()).setValue(LIT, true));
  }

  protected void createBlockStateDefinition(Builder<Block, BlockState> b) {
    b.add(new Property[] {LIT});
  }

  protected void onPlace(BlockState s, Level l, BlockPos p, BlockState old, boolean moving) {
    if (!l.isClientSide && !old.is(this)) {
      l.scheduleTick(p, this, 20 + l.random.nextInt(40));
    }
  }

  protected boolean isRandomlyTicking(BlockState s) {
    return true;
  }

  protected void randomTick(BlockState s, ServerLevel l, BlockPos p, RandomSource r) {
    if (!l.getBlockTicks().hasScheduledTick(p, this)) {
      l.scheduleTick(p, this, 1 + r.nextInt(10));
    }
  }

  protected void tick(BlockState s, ServerLevel l, BlockPos p, RandomSource r) {
    if ((Boolean) s.getValue(LIT)) {
      if (r.nextFloat() < 0.4F) {
        l.setBlock(p, (BlockState) s.setValue(LIT, false), 3);
        if (r.nextInt(3) == 0) {
          l.playSound(
              null,
              p,
              SoundEvents.COPPER_BULB_TURN_OFF,
              SoundSource.BLOCKS,
              0.25F,
              1.4F + r.nextFloat() * 0.4F);
        }

        l.scheduleTick(p, this, r.nextInt(8) == 0 ? 20 + r.nextInt(40) : 1 + r.nextInt(4));
      } else {
        l.scheduleTick(p, this, 30 + r.nextInt(120));
      }
    } else {
      l.setBlock(p, (BlockState) s.setValue(LIT, true), 3);
      if (r.nextInt(3) == 0) {
        l.playSound(null, p, SoundEvents.COPPER_BULB_TURN_ON, SoundSource.BLOCKS, 0.15F, 1.6F);
      }

      l.scheduleTick(p, this, r.nextBoolean() ? 2 + r.nextInt(4) : 40 + r.nextInt(100));
    }
  }
}
