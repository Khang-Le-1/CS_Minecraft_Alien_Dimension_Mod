package com.timelocked;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** An eight-slice collision slope aligned with the modeled inclined deck. North is uphill. */
public final class BoardingRampBlock extends HorizontalDirectionalBlock {
  public static final MapCodec<BoardingRampBlock> CODEC = simpleCodec(BoardingRampBlock::new);
  private static final VoxelShape SLOPE = slope();

  private static VoxelShape slope() {
    VoxelShape s = Shapes.empty();
    for (int i = 0; i < 8; i++)
      s = Shapes.or(s, Block.box(0, 0, i * 2, 16, 16 - i * 2, (i + 1) * 2));
    return s;
  }

  public BoardingRampBlock(Properties p) {
    super(p);
    registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
  }

  protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
    return CODEC;
  }

  protected void createBlockStateDefinition(Builder<Block, BlockState> b) {
    b.add(FACING);
  }

  public BlockState getStateForPlacement(BlockPlaceContext c) {
    return defaultBlockState().setValue(FACING, c.getHorizontalDirection());
  }

  protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
    return FacingDecorBlock.rotate(SLOPE, s.getValue(FACING));
  }
}
