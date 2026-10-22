package com.timelocked;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.shapes.*;
import net.neoforged.neoforge.registries.DeferredBlock;

/** Enamel wayfinding panels on two modeled steel posts, not vanilla sign text. */
public final class CoastSigns {
  public static final DeferredBlock<Panel> RESTRICTION =
      Content.BLOCKS.register(
          "coastal_restriction_sign",
          () ->
              new Panel(
                  BlockBehaviour.Properties.of().strength(3).sound(SoundType.METAL).noOcclusion()));
  public static final DeferredBlock<Panel> DIRECTION =
      Content.BLOCKS.register(
          "boardwalk_direction_sign",
          () ->
              new Panel(
                  BlockBehaviour.Properties.of().strength(3).sound(SoundType.METAL).noOcclusion()));

  public static void initialize() {
    Content.ITEMS.registerSimpleBlockItem(RESTRICTION);
    Content.ITEMS.registerSimpleBlockItem(DIRECTION);
  }

  public static final class Panel extends HorizontalDirectionalBlock {
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final MapCodec<Panel> CODEC = simpleCodec(Panel::new);

    public Panel(BlockBehaviour.Properties p) {
      super(p);
      registerDefaultState(
          stateDefinition
              .any()
              .setValue(FACING, Direction.NORTH)
              .setValue(HALF, DoubleBlockHalf.LOWER));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
      return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
      b.add(FACING, HALF);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext c) {
      return c.getClickedPos().getY() < c.getLevel().getMaxBuildHeight() - 1
              && c.getLevel().getBlockState(c.getClickedPos().above()).canBeReplaced(c)
          ? defaultBlockState().setValue(FACING, c.getHorizontalDirection().getOpposite())
          : null;
    }

    @Override
    public void setPlacedBy(
        Level l,
        BlockPos p,
        BlockState s,
        net.minecraft.world.entity.LivingEntity e,
        net.minecraft.world.item.ItemStack stack) {
      l.setBlock(p.above(), s.setValue(HALF, DoubleBlockHalf.UPPER), 3);
    }

    @Override
    protected BlockState updateShape(
        BlockState s, Direction d, BlockState other, LevelAccessor l, BlockPos p, BlockPos q) {
      Direction mate = s.getValue(HALF) == DoubleBlockHalf.LOWER ? Direction.UP : Direction.DOWN;
      if (d == mate && (!other.is(this) || other.getValue(HALF) == s.getValue(HALF)))
        return Blocks.AIR.defaultBlockState();
      return super.updateShape(s, d, other, l, p, q);
    }

    @Override
    protected BlockState rotate(BlockState s, Rotation r) {
      return s.setValue(FACING, r.rotate(s.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState s, Mirror m) {
      return s.rotate(m.getRotation(s.getValue(FACING)));
    }

    @Override
    protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
      boolean ns = s.getValue(FACING).getAxis() == Direction.Axis.Z;
      if (s.getValue(HALF) == DoubleBlockHalf.UPPER)
        return ns ? Block.box(0, 0, 6, 16, 16, 10) : Block.box(6, 0, 0, 10, 16, 16);
      return ns
          ? Shapes.or(Block.box(2, 0, 7, 4, 16, 9), Block.box(12, 0, 7, 14, 16, 9))
          : Shapes.or(Block.box(7, 0, 2, 9, 16, 4), Block.box(7, 0, 12, 9, 16, 14));
    }
  }

  private CoastSigns() {}
}
