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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Station-only equipment. Pump/turbine modules are architectural, not extra FE generators. */
public final class StationEquipmentBlock extends HorizontalDirectionalBlock {
  public static final MapCodec<StationEquipmentBlock> CODEC =
      simpleCodec(p -> new StationEquipmentBlock(p, 0));
  private final int kind;

  public StationEquipmentBlock(Properties p, int kind) {
    super(p);
    this.kind = kind;
    registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
  }

  protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
    return CODEC;
  }

  protected void createBlockStateDefinition(Builder<Block, BlockState> b) {
    b.add(FACING);
  }

  public BlockState getStateForPlacement(BlockPlaceContext c) {
    return defaultBlockState().setValue(FACING, c.getHorizontalDirection().getOpposite());
  }

  protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
    VoxelShape shape =
        kind == 3
            ? Shapes.or(
                Block.box(1, 0, 2, 15, 2, 14),
                Block.box(3, 2, 3, 13, 14, 13),
                Block.box(0, 14, 1, 16, 16, 15))
            : kind == 0
                ? Shapes.or(
                    Block.box(1, 0, 2, 4, 10, 14),
                    Block.box(12, 0, 2, 15, 10, 14),
                    Block.box(0, 9, 1, 16, 16, 15))
                : kind == 1
                    ? Shapes.or(Block.box(0, 0, 0, 16, 3, 16), Block.box(3, 3, 2, 13, 13, 14))
                    : Shapes.or(Block.box(1, 0, 0, 15, 3, 16), Block.box(2, 3, 0, 14, 15, 16));
    return FacingDecorBlock.rotate(shape, s.getValue(FACING));
  }

  protected InteractionResult useWithoutItem(
      BlockState s, Level l, BlockPos p, Player user, BlockHitResult hit) {
    return kind == 0
        ? SarcContent.CONSOLE.get().useWithoutItem(s, l, p, user, hit)
        : InteractionResult.PASS;
  }
}
