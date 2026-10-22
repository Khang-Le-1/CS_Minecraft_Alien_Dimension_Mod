package com.timelocked;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class TramClearanceBlock extends Block {
  public static final MapCodec<TramClearanceBlock> CODEC = simpleCodec(TramClearanceBlock::new);
  public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
  public static final int[] GATES = new int[] {69, 154};

  public TramClearanceBlock(Properties p) {
    super(p);
    this.registerDefaultState(
        (BlockState) ((BlockState) this.stateDefinition.any()).setValue(OPEN, false));
  }

  protected MapCodec<? extends TramClearanceBlock> codec() {
    return CODEC;
  }

  protected void createBlockStateDefinition(Builder<Block, BlockState> b) {
    b.add(new Property[] {OPEN});
  }

  protected VoxelShape getCollisionShape(
      BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
    if (c instanceof EntityCollisionContext ec) {
      Entity e = ec.getEntity();
      if (e != null
          && (e instanceof SarcTramEntity || e.getRootVehicle() instanceof SarcTramEntity)) {
        return Shapes.empty();
      }
    }

    return Shapes.block();
  }

  protected VoxelShape getOcclusionShape(BlockState s, BlockGetter l, BlockPos p) {
    return Shapes.empty();
  }

  public static void updateGates(Level l, BlockPos o, double tramZ) {
    for (int gz : GATES) {
      boolean open = Math.abs(tramZ - (o.getZ() + gz + 0.5)) < 7.0;
      boolean changed = false;

      for (int x = 90; x <= 95; x++) {
        for (int y = 2; y <= 6; y++) {
          BlockPos p = o.offset(x, y, gz);
          if (l.hasChunkAt(p)) {
            BlockState s = l.getBlockState(p);
            if (s.getBlock() instanceof TramClearanceBlock && (Boolean) s.getValue(OPEN) != open) {
              l.setBlock(p, (BlockState) s.setValue(OPEN, open), 2);
              changed = true;
            }
          }
        }
      }

      if (changed) {
        l.playSound(
            null,
            o.offset(92, 4, gz),
            open ? SoundEvents.IRON_DOOR_OPEN : SoundEvents.IRON_DOOR_CLOSE,
            SoundSource.BLOCKS,
            1.5F,
            0.55F);
      }
    }
  }
}
