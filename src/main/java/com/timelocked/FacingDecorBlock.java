package com.timelocked;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class FacingDecorBlock extends HorizontalDirectionalBlock {
  public static final int DESK = 0;
  public static final int SHELF = 1;
  public static final int PYLON = 2;
  public static final MapCodec<FacingDecorBlock> CODEC =
      simpleCodec(p -> new FacingDecorBlock(p, 0));
  private static final VoxelShape DESK_SHAPE =
      Shapes.or(
          Block.box(0.0, 11.0, 0.0, 16.0, 13.0, 16.0),
          new VoxelShape[] {
            Block.box(0.0, 0.0, 1.0, 5.0, 11.0, 15.0),
            Block.box(14.0, 0.0, 1.0, 16.0, 11.0, 15.0),
            Block.box(2.0, 13.0, 10.0, 14.0, 21.0, 13.0)
          });
  private static final VoxelShape PYLON_SHAPE = Block.box(3.0, 0.0, 3.0, 13.0, 16.0, 13.0);
  private static final VoxelShape SHELF_SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0);
  private final int kind;

  public FacingDecorBlock(Properties p, int kind) {
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

  static VoxelShape rotate(VoxelShape shape, Direction d) {
    if (d == Direction.NORTH) {
      return shape;
    }

    VoxelShape[] out = new VoxelShape[] {Shapes.empty()};
    int turns = (d.get2DDataValue() - Direction.NORTH.get2DDataValue() + 4) % 4;
    shape.forAllBoxes(
        (x0, y0, z0, x1, y1, z1) -> {
          double ax0 = x0;
          double az0 = z0;
          double ax1 = x1;
          double az1 = z1;

          for (int i = 0; i < turns; i++) {
            double nx0 = 1.0 - az1;
            double nz0 = ax0;
            double nx1 = 1.0 - az0;
            double nz1 = ax1;
            ax0 = nx0;
            az0 = nz0;
            ax1 = nx1;
            az1 = nz1;
          }

          out[0] = Shapes.or(out[0], Shapes.box(ax0, y0, az0, ax1, y1, az1));
        });
    return out[0];
  }

  protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
    return this.kind == 3
        ? rotate(Block.box(2, 2, 5.8, 14, 14, 16), s.getValue(FACING))
        : this.kind == 0
            ? rotate(DESK_SHAPE, (Direction) s.getValue(FACING))
            : (this.kind == 2 ? PYLON_SHAPE : SHELF_SHAPE);
  }

  public void animateTick(BlockState s, Level l, BlockPos p, RandomSource r) {
    if (this.kind == 2 && r.nextInt(4) == 0) {
      l.addParticle(
          ParticleTypes.ELECTRIC_SPARK,
          p.getX() + 0.5 + (r.nextDouble() - 0.5) * 0.7,
          p.getY() + r.nextDouble(),
          p.getZ() + 0.5 + (r.nextDouble() - 0.5) * 0.7,
          0.0,
          0.02,
          0.0);
    }
  }
}
