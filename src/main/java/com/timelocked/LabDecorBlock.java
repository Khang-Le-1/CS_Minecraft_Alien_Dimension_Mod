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
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class LabDecorBlock extends HorizontalDirectionalBlock {
  public static final int PAPERS = 0;
  public static final int PUDDLE = 1;
  public static final int CLAW = 2;
  public static final MapCodec<LabDecorBlock> CODEC = simpleCodec(p -> new LabDecorBlock(p, 0));
  private static final VoxelShape FLOOR = Block.box(0.0, 0.0, 0.0, 16.0, 1.0, 16.0);
  private final int kind;

  public LabDecorBlock(Properties p, int kind) {
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
    if (this.kind == 2) {
      Direction face = c.getClickedFace();
      if (face.getAxis().isHorizontal()) {
        return (BlockState) this.defaultBlockState().setValue(FACING, face);
      }

      for (Direction d : c.getNearestLookingDirections()) {
        if (d.getAxis().isHorizontal()) {
          return (BlockState) this.defaultBlockState().setValue(FACING, d.getOpposite());
        }
      }
    }

    return (BlockState)
        this.defaultBlockState().setValue(FACING, c.getHorizontalDirection().getOpposite());
  }

  protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
    if (this.kind != 2) {
      return FLOOR;
    }

    return switch ((Direction) s.getValue(FACING)) {
      case SOUTH -> Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 1.0);
      case EAST -> Block.box(0.0, 0.0, 0.0, 1.0, 16.0, 16.0);
      case WEST -> Block.box(15.0, 0.0, 0.0, 16.0, 16.0, 16.0);
      default -> Block.box(0.0, 0.0, 15.0, 16.0, 16.0, 16.0);
    };
  }

  protected VoxelShape getCollisionShape(
      BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
    return Shapes.empty();
  }

  protected boolean canSurvive(BlockState s, LevelReader l, BlockPos p) {
    if (this.kind == 2) {
      Direction back = ((Direction) s.getValue(FACING)).getOpposite();
      return l.getBlockState(p.relative(back))
          .isFaceSturdy(l, p.relative(back), (Direction) s.getValue(FACING));
    } else {
      return l.getBlockState(p.below()).isFaceSturdy(l, p.below(), Direction.UP);
    }
  }

  protected BlockState updateShape(
      BlockState s, Direction d, BlockState n, LevelAccessor l, BlockPos p, BlockPos np) {
    return this.canSurvive(s, l, p) ? s : Blocks.AIR.defaultBlockState();
  }
}
