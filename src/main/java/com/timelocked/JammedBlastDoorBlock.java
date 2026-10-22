/* Reconstructed from the user-provided 0.8.9.8 release with permission. */
package com.timelocked;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class JammedBlastDoorBlock extends HorizontalDirectionalBlock {
  public static final IntegerProperty PART =
      IntegerProperty.create((String) "part", (int) 0, (int) 17);
  public static final MapCodec<JammedBlastDoorBlock> CODEC =
      JammedBlastDoorBlock.simpleCodec(JammedBlastDoorBlock::new);

  public JammedBlastDoorBlock(BlockBehaviour.Properties p) {
    super(p);
    this.registerDefaultState(
        (BlockState)
            ((BlockState)
                    ((BlockState) this.stateDefinition.any())
                        .setValue((Property) FACING, (Comparable) Direction.NORTH))
                .setValue((Property) PART, (Comparable) Integer.valueOf(0)));
  }

  protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
    return CODEC;
  }

  protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
    b.add(new Property[] {FACING, PART});
  }

  protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
    int part = (Integer) s.getValue((Property) PART);
    int bottom = part < 6 ? 5 + part % 3 : 0;
    return ((Direction) s.getValue((Property) FACING)).getAxis() == Direction.Axis.X
        ? Block.box(
            (double) 5.0,
            (double) bottom,
            (double) 0.0,
            (double) 11.0,
            (double) 16.0,
            (double) 16.0)
        : Block.box(
            (double) 0.0,
            (double) bottom,
            (double) 5.0,
            (double) 16.0,
            (double) 16.0,
            (double) 11.0);
  }
}
