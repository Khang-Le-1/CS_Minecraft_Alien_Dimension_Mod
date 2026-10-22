package com.timelocked;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;

public final class PortalFrameAnchorBlock extends BaseEntityBlock {
  public static final MapCodec<PortalFrameAnchorBlock> CODEC =
      simpleCodec(PortalFrameAnchorBlock::new);

  public static final net.minecraft.world.level.block.state.properties.DirectionProperty FACING =
      net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;

  public PortalFrameAnchorBlock(Properties p) {
    super(p);
    registerDefaultState(defaultBlockState().setValue(FACING, net.minecraft.core.Direction.NORTH));
  }

  protected void createBlockStateDefinition(
      net.minecraft.world.level.block.state.StateDefinition.Builder<
              net.minecraft.world.level.block.Block, BlockState>
          b) {
    b.add(FACING);
  }

  protected MapCodec<? extends BaseEntityBlock> codec() {
    return CODEC;
  }

  protected RenderShape getRenderShape(BlockState s) {
    return RenderShape.ENTITYBLOCK_ANIMATED;
  }

  public BlockEntity newBlockEntity(BlockPos p, BlockState s) {
    return new PortalFrameEntity(p, s);
  }
}
