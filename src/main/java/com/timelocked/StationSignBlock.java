package com.timelocked;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class StationSignBlock extends HorizontalDirectionalBlock {
  public static final int LEGENDS = 8;
  public static final IntegerProperty LEGEND = IntegerProperty.create("legend", 0, 7);
  public static final MapCodec<StationSignBlock> CODEC = simpleCodec(StationSignBlock::new);
  private static final VoxelShape SHAPE = Block.box(0.0, 1.0, 6.5, 16.0, 16.0, 9.5);

  public StationSignBlock(Properties p) {
    super(p);
    this.registerDefaultState(
        (BlockState)
            ((BlockState)
                    ((BlockState) this.stateDefinition.any()).setValue(FACING, Direction.NORTH))
                .setValue(LEGEND, 0));
  }

  protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
    return CODEC;
  }

  protected void createBlockStateDefinition(Builder<Block, BlockState> b) {
    b.add(new Property[] {FACING, LEGEND});
  }

  public BlockState getStateForPlacement(BlockPlaceContext c) {
    return (BlockState)
        this.defaultBlockState().setValue(FACING, c.getHorizontalDirection().getOpposite());
  }

  protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
    return FacingDecorBlock.rotate(SHAPE, (Direction) s.getValue(FACING));
  }

  protected VoxelShape getCollisionShape(
      BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
    return Shapes.empty();
  }

  protected InteractionResult useWithoutItem(
      BlockState s, Level l, BlockPos p, Player pl, BlockHitResult hit) {
    if (!l.isClientSide) {
      l.setBlock(p, (BlockState) s.cycle(LEGEND), 3);
    }

    return InteractionResult.sidedSuccess(l.isClientSide);
  }
}
