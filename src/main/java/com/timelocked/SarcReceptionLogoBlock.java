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
import net.minecraft.world.phys.shapes.VoxelShape;

public final class SarcReceptionLogoBlock extends HorizontalDirectionalBlock {
  public static final MapCodec<SarcReceptionLogoBlock> CODEC =
      simpleCodec(SarcReceptionLogoBlock::new);
  public static final IntegerProperty PART = IntegerProperty.create("part", 0, 31);

  public SarcReceptionLogoBlock(Properties p) {
    super(p);
    this.registerDefaultState(
        (BlockState)
            ((BlockState)
                    ((BlockState) this.stateDefinition.any()).setValue(FACING, Direction.NORTH))
                .setValue(PART, 0));
  }

  protected InteractionResult useWithoutItem(
      BlockState s, Level l, BlockPos p, Player player, BlockHitResult hit) {
    if (!player.isShiftKeyDown()) {
      return InteractionResult.PASS;
    }

    if (!l.isClientSide) {
      l.setBlock(p, (BlockState) s.cycle(PART), 3);
    }

    return InteractionResult.sidedSuccess(l.isClientSide);
  }

  protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
    return CODEC;
  }

  protected void createBlockStateDefinition(Builder<Block, BlockState> b) {
    b.add(new Property[] {FACING, PART});
  }

  public BlockState getStateForPlacement(BlockPlaceContext c) {
    return (BlockState)
        this.defaultBlockState().setValue(FACING, c.getHorizontalDirection().getOpposite());
  }

  protected VoxelShape getShape(BlockState s, BlockGetter w, BlockPos p, CollisionContext c) {
    return switch ((Direction) s.getValue(FACING)) {
      case SOUTH -> Block.box(0.0, 0.0, 15.0, 16.0, 16.0, 16.0);
      case EAST -> Block.box(15.0, 0.0, 0.0, 16.0, 16.0, 16.0);
      case WEST -> Block.box(0.0, 0.0, 0.0, 1.0, 16.0, 16.0);
      default -> Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 1.0);
    };
  }
}
