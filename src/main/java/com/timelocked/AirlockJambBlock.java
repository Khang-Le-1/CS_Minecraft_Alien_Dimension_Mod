package com.timelocked;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Full-depth fitted surround, flush with the wide door's outer frame. */
public final class AirlockJambBlock extends HorizontalDirectionalBlock {
  public static final MapCodec<AirlockJambBlock> CODEC = simpleCodec(AirlockJambBlock::new);
  public static final BooleanProperty RIGHT = BooleanProperty.create("right");

  public AirlockJambBlock(Properties p) {
    super(p);
    registerDefaultState(
        stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(RIGHT, false));
  }

  protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
    return CODEC;
  }

  protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
    b.add(FACING, RIGHT);
  }

  protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
    return FacingDecorBlock.rotate(
        s.getValue(RIGHT) ? Block.box(7, 0, 0, 16, 16, 16) : Block.box(0, 0, 0, 9, 16, 16),
        s.getValue(FACING));
  }
}
