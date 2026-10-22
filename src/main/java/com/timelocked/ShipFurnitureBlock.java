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
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class ShipFurnitureBlock extends HorizontalDirectionalBlock {
  public static final MapCodec<ShipFurnitureBlock> CODEC =
      simpleCodec(p -> new ShipFurnitureBlock(p, 0));
  private final int kind;
  private static final VoxelShape[] SHAPES =
      new VoxelShape[] {
        Shapes.or(
            Block.box(5.0, 0.0, 5.0, 11.0, 2.0, 11.0),
            new VoxelShape[] {
              Block.box(7.0, 2.0, 7.0, 9.0, 6.0, 9.0),
              Block.box(2.0, 6.0, 3.0, 14.0, 8.0, 13.0),
              Block.box(3.0, 8.0, 3.0, 13.0, 9.0, 12.0),
              Block.box(3.0, 8.0, 12.0, 13.0, 20.0, 14.0),
              Block.box(4.0, 9.0, 11.5, 12.0, 18.0, 12.5),
              Block.box(4.0, 18.0, 11.0, 12.0, 21.0, 14.0),
              Block.box(1.0, 7.0, 5.0, 3.0, 11.0, 12.0),
              Block.box(1.0, 11.0, 4.0, 3.0, 12.0, 12.0),
              Block.box(13.0, 7.0, 5.0, 15.0, 11.0, 12.0),
              Block.box(13.0, 11.0, 4.0, 15.0, 12.0, 12.0),
              Block.box(4.0, 10.0, 11.2, 5.0, 17.0, 11.7),
              Block.box(10.0, 10.0, 11.2, 11.0, 17.0, 11.7),
              Block.box(7.0, 9.0, 11.0, 9.0, 11.0, 11.6)
            }),
        Shapes.or(
            Block.box(1.0, 0.0, 2.0, 15.0, 1.0, 15.0),
            new VoxelShape[] {
              Block.box(1.0, 1.0, 3.0, 15.0, 15.0, 15.0),
              Block.box(2.0, 1.0, 2.0, 8.0, 14.0, 3.0),
              Block.box(8.5, 1.0, 2.0, 14.0, 14.0, 3.0),
              Block.box(1.0, 15.0, 2.0, 15.0, 16.0, 15.0),
              Block.box(6.0, 6.0, 1.5, 6.6, 10.0, 2.0),
              Block.box(9.0, 6.0, 1.5, 9.6, 10.0, 2.0),
              Block.box(3.0, 3.0, 1.8, 6.0, 3.4, 2.0),
              Block.box(10.0, 3.0, 1.8, 13.0, 3.4, 2.0),
              Block.box(3.0, 4.0, 1.8, 6.0, 4.4, 2.0),
              Block.box(10.0, 4.0, 1.8, 13.0, 4.4, 2.0),
              Block.box(3.0, 5.0, 1.8, 6.0, 5.4, 2.0),
              Block.box(10.0, 5.0, 1.8, 13.0, 5.4, 2.0),
              Block.box(2.0, 12.0, 1.8, 5.0, 13.0, 2.0),
              Block.box(10.0, 12.0, 1.8, 13.0, 13.0, 2.0)
            }),
        Shapes.or(
            Block.box(2.0, 0.0, 3.0, 14.0, 1.0, 14.0),
            new VoxelShape[] {
              Block.box(2.0, 1.0, 3.0, 14.0, 9.0, 14.0),
              Block.box(3.0, 2.0, 2.7, 13.0, 5.0, 3.0),
              Block.box(3.0, 5.5, 2.7, 13.0, 8.5, 3.0),
              Block.box(6.0, 4.0, 2.2, 10.0, 4.5, 2.8),
              Block.box(2.0, 9.0, 2.0, 14.0, 10.0, 14.0),
              Block.box(10.0, 10.0, 9.0, 12.0, 12.0, 11.0),
              Block.box(9.0, 12.0, 8.0, 13.0, 14.0, 12.0),
              Block.box(3.0, 10.0, 4.0, 7.0, 10.5, 8.0)
            }),
        Shapes.or(
            Block.box(2.0, 0.0, 3.0, 4.0, 2.0, 5.0),
            new VoxelShape[] {
              Block.box(2.0, 0.0, 12.0, 4.0, 2.0, 14.0),
              Block.box(12.0, 0.0, 3.0, 14.0, 2.0, 5.0),
              Block.box(12.0, 0.0, 12.0, 14.0, 2.0, 14.0),
              Block.box(2.0, 2.0, 3.0, 14.0, 10.0, 14.0),
              Block.box(3.0, 3.0, 2.7, 13.0, 6.0, 3.0),
              Block.box(3.0, 6.5, 2.7, 13.0, 9.0, 3.0),
              Block.box(6.0, 4.0, 2.2, 10.0, 4.5, 2.8),
              Block.box(1.0, 10.0, 2.0, 15.0, 11.0, 15.0),
              Block.box(11.0, 11.0, 11.0, 12.0, 14.0, 12.0),
              Block.box(7.0, 13.0, 10.0, 15.0, 17.0, 12.0),
              Block.box(8.0, 14.0, 9.8, 14.0, 16.0, 10.0),
              Block.box(3.0, 11.0, 4.0, 7.0, 11.5, 8.0),
              Block.box(9.0, 4.0, 2.4, 10.0, 8.0, 2.7),
              Block.box(7.5, 5.5, 2.4, 11.5, 6.5, 2.7)
            }),
        Shapes.or(
            Block.box(3.0, 0.0, 3.0, 13.0, 1.0, 13.0),
            new VoxelShape[] {
              Block.box(7.0, 1.0, 7.0, 9.0, 10.0, 9.0),
              Block.box(0.0, 10.0, 0.0, 16.0, 12.0, 16.0),
              Block.box(1.0, 12.0, 1.0, 15.0, 12.3, 15.0),
              Block.box(2.0, 12.3, 3.0, 7.0, 12.5, 8.0),
              Block.box(11.0, 12.3, 10.0, 13.0, 14.3, 12.0),
              Block.box(13.0, 13.0, 10.5, 14.0, 13.7, 11.5),
              Block.box(8.0, 12.3, 3.0, 12.0, 12.5, 7.0)
            }),
        Shapes.or(
            Block.box(2.0, 0.0, 3.0, 14.0, 1.0, 15.0),
            new VoxelShape[] {
              Block.box(2.0, 1.0, 5.0, 14.0, 15.0, 15.0),
              Block.box(3.0, 5.0, 4.0, 13.0, 14.0, 5.0),
              Block.box(4.0, 9.0, 3.8, 10.0, 13.0, 4.0),
              Block.box(11.0, 10.0, 3.6, 12.0, 11.0, 4.0),
              Block.box(5.0, 7.0, 3.0, 7.0, 8.0, 5.0),
              Block.box(2.0, 1.0, 1.0, 14.0, 2.0, 6.0),
              Block.box(2.0, 2.0, 1.0, 3.0, 3.0, 6.0),
              Block.box(13.0, 2.0, 1.0, 14.0, 3.0, 6.0),
              Block.box(5.0, 2.0, 2.0, 7.0, 5.0, 4.0),
              Block.box(3.0, 15.0, 5.0, 13.0, 16.0, 15.0)
            }),
        Shapes.or(
            Block.box(0.0, 0.0, 1.0, 2.0, 16.0, 3.0),
            new VoxelShape[] {
              Block.box(0.0, 0.0, 13.0, 2.0, 16.0, 15.0),
              Block.box(14.0, 0.0, 1.0, 16.0, 16.0, 3.0),
              Block.box(14.0, 0.0, 13.0, 16.0, 16.0, 15.0),
              Block.box(0.0, 0.0, 1.0, 16.0, 1.0, 15.0),
              Block.box(0.0, 7.0, 1.0, 16.0, 8.0, 15.0),
              Block.box(0.0, 15.0, 1.0, 16.0, 16.0, 15.0),
              Block.box(2.0, 1.0, 3.0, 8.0, 7.0, 13.0),
              Block.box(2.0, 2.0, 2.8, 8.0, 3.0, 3.0),
              Block.box(9.0, 1.0, 3.0, 14.0, 6.0, 13.0),
              Block.box(9.0, 2.0, 2.8, 14.0, 3.0, 3.0),
              Block.box(2.0, 8.0, 4.0, 7.0, 14.0, 13.0),
              Block.box(2.0, 9.0, 3.8, 7.0, 10.0, 4.0),
              Block.box(8.0, 8.0, 3.0, 14.0, 15.0, 13.0),
              Block.box(8.0, 9.0, 2.8, 14.0, 10.0, 3.0),
              Block.box(7.0, 1.0, 0.8, 8.0, 15.0, 1.1),
              Block.box(1.0, 6.0, 0.8, 15.0, 7.0, 1.1),
              Block.box(11.0, 10.0, 2.7, 13.0, 12.0, 3.0)
            }),
        Shapes.or(
            Block.box(0.0, 0.0, 1.0, 16.0, 2.0, 15.0),
            new VoxelShape[] {
              Block.box(3.0, 2.0, 3.0, 13.0, 12.0, 13.0),
              Block.box(4.0, 3.0, 2.0, 12.0, 11.0, 3.0),
              Block.box(5.0, 4.0, 1.8, 11.0, 10.0, 2.0),
              Block.box(6.0, 12.0, 6.0, 10.0, 15.0, 10.0),
              Block.box(5.0, 15.0, 5.0, 11.0, 16.0, 11.0),
              Block.box(1.0, 2.0, 5.0, 3.0, 13.0, 11.0),
              Block.box(1.0, 6.0, 4.5, 3.0, 8.0, 11.5),
              Block.box(13.0, 2.0, 5.0, 15.0, 13.0, 11.0),
              Block.box(13.0, 6.0, 4.5, 15.0, 8.0, 11.5),
              Block.box(5.0, 3.0, 13.0, 5.6, 11.0, 14.0),
              Block.box(8.0, 3.0, 13.0, 8.6, 11.0, 14.0),
              Block.box(11.0, 3.0, 13.0, 11.6, 11.0, 14.0)
            })
      };

  public ShipFurnitureBlock(Properties p, int kind) {
    super(p);
    this.kind = kind;
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

  protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
    return FacingDecorBlock.rotate(SHAPES[this.kind], (Direction) s.getValue(FACING));
  }
}
