package com.timelocked;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A visibly sealed two-high transfer door. Clicking either half uses the established ship transfer.
 */
public final class BoardingAirlockBlock extends HorizontalDirectionalBlock {
  public static final MapCodec<BoardingAirlockBlock> CODEC = simpleCodec(BoardingAirlockBlock::new);
  public static final BooleanProperty UPPER = BooleanProperty.create("upper");

  public BoardingAirlockBlock(Properties p) {
    super(p);
    registerDefaultState(
        stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(UPPER, false));
  }

  protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
    return CODEC;
  }

  protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
    b.add(FACING, UPPER);
  }

  public BlockState getStateForPlacement(BlockPlaceContext c) {
    return c.getLevel().getBlockState(c.getClickedPos().above()).canBeReplaced()
        ? defaultBlockState().setValue(FACING, c.getHorizontalDirection().getOpposite())
        : null;
  }

  public void setPlacedBy(Level l, BlockPos p, BlockState s, LivingEntity e, ItemStack item) {
    l.setBlock(p.above(), s.setValue(UPPER, true), 3);
  }

  protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
    return FacingDecorBlock.rotate(Block.box(-7, 0, 6, 23, 16, 10), s.getValue(FACING));
  }

  protected BlockState updateShape(
      BlockState s, Direction d, BlockState other, LevelAccessor l, BlockPos at, BlockPos next) {
    if (d == (s.getValue(UPPER) ? Direction.DOWN : Direction.UP)
        && (!other.is(this) || other.getValue(UPPER) == s.getValue(UPPER)))
      return Blocks.AIR.defaultBlockState();
    return super.updateShape(s, d, other, l, at, next);
  }

  protected InteractionResult useWithoutItem(
      BlockState s, Level l, BlockPos p, Player user, BlockHitResult hit) {
    if (user instanceof ServerPlayer sp) {
      Timeskipper.Door door = Timeskipper.Door.of(s.getValue(FACING));
      if (l.dimension().equals(SpacePlanets.SHIP)) Timeskipper.disembark(sp, door);
      else Timeskipper.board(sp, door);
    }
    return InteractionResult.sidedSuccess(l.isClientSide);
  }
}
