package com.timelocked;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class ContainmentTankBlock extends HorizontalDirectionalBlock {
  public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
  public static final MapCodec<ContainmentTankBlock> CODEC =
      simpleCodec(p -> new ContainmentTankBlock(p, false));
  private final boolean broken;

  public ContainmentTankBlock(Properties p, boolean broken) {
    super(p);
    this.broken = broken;
    this.registerDefaultState(
        (BlockState)
            ((BlockState)
                    ((BlockState) this.stateDefinition.any()).setValue(FACING, Direction.NORTH))
                .setValue(HALF, DoubleBlockHalf.LOWER));
  }

  protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
    return CODEC;
  }

  public boolean broken() {
    return this.broken;
  }

  protected void createBlockStateDefinition(Builder<Block, BlockState> b) {
    b.add(new Property[] {FACING, HALF});
  }

  public BlockState getStateForPlacement(BlockPlaceContext c) {
    BlockPos p = c.getClickedPos();
    return p.getY() < c.getLevel().getMaxBuildHeight() - 1
            && c.getLevel().getBlockState(p.above()).canBeReplaced(c)
        ? (BlockState)
            this.defaultBlockState().setValue(FACING, c.getHorizontalDirection().getOpposite())
        : null;
  }

  public void setPlacedBy(Level l, BlockPos p, BlockState s, LivingEntity e, ItemStack st) {
    l.setBlock(p.above(), (BlockState) s.setValue(HALF, DoubleBlockHalf.UPPER), 3);
  }

  protected BlockState updateShape(
      BlockState s, Direction d, BlockState n, LevelAccessor l, BlockPos p, BlockPos np) {
    DoubleBlockHalf h = (DoubleBlockHalf) s.getValue(HALF);
    return d.getAxis() != Axis.Y
            || h == DoubleBlockHalf.LOWER != (d == Direction.UP)
            || n.is(this) && n.getValue(HALF) != h
        ? s
        : Blocks.AIR.defaultBlockState();
  }

  protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
    boolean lower = s.getValue(HALF) == DoubleBlockHalf.LOWER;
    if (this.broken) {
      return lower
          ? Block.box(1.0, 0.0, 1.0, 15.0, 5.0, 15.0)
          : Block.box(1.0, 12.0, 1.0, 15.0, 16.0, 15.0);
    } else {
      return Block.box(1.0, 0.0, 1.0, 15.0, 16.0, 15.0);
    }
  }

  protected boolean propagatesSkylightDown(BlockState s, BlockGetter l, BlockPos p) {
    return true;
  }

  protected float getShadeBrightness(BlockState s, BlockGetter l, BlockPos p) {
    return 1.0F;
  }
}
