/* Reconstructed from the user-provided 0.8.9.8 release with permission. */
package com.timelocked;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class StationLightBlock extends DirectionalBlock {
  public static final BooleanProperty LIT = BlockStateProperties.LIT;
  public static final MapCodec<StationLightBlock> CODEC =
      StationLightBlock.simpleCodec(StationLightBlock::new);

  public StationLightBlock(BlockBehaviour.Properties p) {
    super(p);
    this.registerDefaultState(
        (BlockState)
            ((BlockState)
                    ((BlockState) this.stateDefinition.any())
                        .setValue((Property) FACING, (Comparable) Direction.DOWN))
                .setValue((Property) LIT, (Comparable) Boolean.valueOf(true)));
  }

  protected MapCodec<? extends DirectionalBlock> codec() {
    return CODEC;
  }

  protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
    b.add(new Property[] {FACING, LIT});
  }

  public BlockState getStateForPlacement(BlockPlaceContext c) {
    BlockState s =
        (BlockState)
            this.defaultBlockState().setValue((Property) FACING, (Comparable) c.getClickedFace());
    return s.canSurvive((LevelReader) c.getLevel(), c.getClickedPos()) ? s : null;
  }

  protected boolean canSurvive(BlockState s, LevelReader l, BlockPos p) {
    Direction f = (Direction) s.getValue((Property) FACING);
    BlockPos back = p.relative(f.getOpposite());
    if (f == Direction.DOWN
        && l.getBlockState(back).is(Blocks.CHAIN)
        && l.getBlockState(back).getValue((Property) ChainBlock.AXIS) == Direction.Axis.Y) {
      return true;
    }
    return l.getBlockState(back).isFaceSturdy((BlockGetter) l, back, f);
  }

  protected BlockState updateShape(
      BlockState s, Direction d, BlockState n, LevelAccessor l, BlockPos p, BlockPos other) {
    return d == ((Direction) s.getValue((Property) FACING)).getOpposite()
            && !s.canSurvive((LevelReader) l, p)
        ? Blocks.AIR.defaultBlockState()
        : super.updateShape(s, d, n, l, p, other);
  }

  protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
    return switch ((Direction) s.getValue((Property) FACING)) {
      default -> throw new MatchException(null, null);
      case Direction.DOWN ->
          Block.box(
              (double) 1.0,
              (double) 10.0,
              (double) 3.0,
              (double) 15.0,
              (double) 16.0,
              (double) 13.0);
      case Direction.UP ->
          Block.box(
              (double) 1.0, (double) 0.0, (double) 3.0, (double) 15.0, (double) 6.0, (double) 13.0);
      case Direction.NORTH ->
          Block.box(
              (double) 1.0,
              (double) 3.0,
              (double) 10.0,
              (double) 15.0,
              (double) 13.0,
              (double) 16.0);
      case Direction.SOUTH ->
          Block.box(
              (double) 1.0, (double) 3.0, (double) 0.0, (double) 15.0, (double) 13.0, (double) 6.0);
      case Direction.EAST ->
          Block.box(
              (double) 0.0, (double) 3.0, (double) 1.0, (double) 6.0, (double) 13.0, (double) 15.0);
      case Direction.WEST ->
          Block.box(
              (double) 10.0,
              (double) 3.0,
              (double) 1.0,
              (double) 16.0,
              (double) 13.0,
              (double) 15.0);
    };
  }
}
