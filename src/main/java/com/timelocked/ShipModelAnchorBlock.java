package com.timelocked;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;

public final class ShipModelAnchorBlock extends BaseEntityBlock {
  public static final net.minecraft.world.level.block.state.properties.BooleanProperty NEW_LAYOUT =
      net.minecraft.world.level.block.state.properties.BooleanProperty.create("new_layout");
  public static final MapCodec<ShipModelAnchorBlock> CODEC = simpleCodec(ShipModelAnchorBlock::new);

  public ShipModelAnchorBlock(Properties properties) {
    super(properties);
    registerDefaultState(stateDefinition.any().setValue(NEW_LAYOUT, false));
  }

  protected void createBlockStateDefinition(
      net.minecraft.world.level.block.state.StateDefinition.Builder<
              net.minecraft.world.level.block.Block, BlockState>
          b) {
    b.add(NEW_LAYOUT);
  }

  protected MapCodec<? extends BaseEntityBlock> codec() {
    return CODEC;
  }

  protected RenderShape getRenderShape(BlockState state) {
    return RenderShape.INVISIBLE;
  }

  public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
    return new ShipModelAnchorBlock.ModelAnchor(pos, state);
  }

  public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
      Level level, BlockState state, BlockEntityType<T> type) {
    return null;
  }

  public static final class ModelAnchor extends BlockEntity {
    public ModelAnchor(BlockPos pos, BlockState state) {
      super((BlockEntityType) SpaceContent.MODEL_ENTITY.get(), pos, state);
    }
  }
}
