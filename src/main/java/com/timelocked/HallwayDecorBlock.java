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
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class HallwayDecorBlock extends HorizontalDirectionalBlock {
  public static final IntegerProperty PART =
      IntegerProperty.create((String) "part", (int) 0, (int) 11);
  public static final MapCodec<HallwayDecorBlock> CODEC =
      HallwayDecorBlock.simpleCodec(p -> new HallwayDecorBlock((BlockBehaviour.Properties) p, 0));
  private final int kind;

  public HallwayDecorBlock(BlockBehaviour.Properties p, int kind) {
    super(p);
    this.kind = kind;
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
    if (this.kind == 2) {
      return Block.box(
          (double) 1.0, (double) 0.0, (double) 1.0, (double) 15.0, (double) 16.0, (double) 15.0);
    }
    Direction f = (Direction) s.getValue((Property) FACING);
    if (this.kind == 1) {
      return switch (f) {
        case Direction.EAST ->
            Block.box(
                (double) 0.0,
                (double) 0.0,
                (double) 1.0,
                (double) 3.0,
                (double) 16.0,
                (double) 15.0);
        case Direction.WEST ->
            Block.box(
                (double) 13.0,
                (double) 0.0,
                (double) 1.0,
                (double) 16.0,
                (double) 16.0,
                (double) 15.0);
        case Direction.SOUTH ->
            Block.box(
                (double) 1.0,
                (double) 0.0,
                (double) 0.0,
                (double) 15.0,
                (double) 16.0,
                (double) 3.0);
        default ->
            Block.box(
                (double) 1.0,
                (double) 0.0,
                (double) 13.0,
                (double) 15.0,
                (double) 16.0,
                (double) 16.0);
      };
    }
    VoxelShape voxelShape =
        Block.box(
            (double) 1.0, (double) 0.0, (double) 2.0, (double) 15.0, (double) 8.0, (double) 14.0);
    return Shapes.or(
        (VoxelShape) voxelShape,
        (VoxelShape)
            (switch (f) {
              case Direction.EAST ->
                  Block.box(
                      (double) 1.0,
                      (double) 8.0,
                      (double) 1.0,
                      (double) 4.0,
                      (double) 15.0,
                      (double) 15.0);
              case Direction.WEST ->
                  Block.box(
                      (double) 12.0,
                      (double) 8.0,
                      (double) 1.0,
                      (double) 15.0,
                      (double) 15.0,
                      (double) 15.0);
              case Direction.SOUTH ->
                  Block.box(
                      (double) 1.0,
                      (double) 8.0,
                      (double) 1.0,
                      (double) 15.0,
                      (double) 15.0,
                      (double) 4.0);
              default ->
                  Block.box(
                      (double) 1.0,
                      (double) 8.0,
                      (double) 12.0,
                      (double) 15.0,
                      (double) 15.0,
                      (double) 15.0);
            }));
  }
}
