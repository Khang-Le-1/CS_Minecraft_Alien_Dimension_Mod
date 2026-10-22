package com.timelocked;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;

public class AnimatedSarcDoorBlock extends DoorBlock implements EntityBlock {
  public static final MapCodec<AnimatedSarcDoorBlock> CODEC =
      simpleCodec(AnimatedSarcDoorBlock::new);

  public AnimatedSarcDoorBlock(Properties p) {
    this(BlockSetType.COPPER, p);
  }

  protected AnimatedSarcDoorBlock(BlockSetType type, Properties p) {
    super(type, p);
  }

  public MapCodec<? extends DoorBlock> codec() {
    return CODEC;
  }

  protected RenderShape getRenderShape(BlockState s) {
    return RenderShape.ENTITYBLOCK_ANIMATED;
  }

  public BlockEntity newBlockEntity(BlockPos p, BlockState s) {
    return new SarcDoorEntity(p, s);
  }

  public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
      Level l, BlockState s, BlockEntityType<T> type) {
    return l.isClientSide && type == FacilityDetails.DOOR_ENTITY.get()
        ? (level, pos, state, be) -> ((SarcDoorEntity) be).animate(state)
        : null;
  }
}
