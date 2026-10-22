package com.timelocked;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;

public final class FacilityLampBlock extends Block {
  public static final BooleanProperty LIT = BooleanProperty.create("lit");
  public static final MapCodec<FacilityLampBlock> CODEC = simpleCodec(FacilityLampBlock::new);

  public FacilityLampBlock(Properties p) {
    super(p);
    this.registerDefaultState(
        (BlockState) ((BlockState) this.stateDefinition.any()).setValue(LIT, true));
  }

  protected MapCodec<? extends FacilityLampBlock> codec() {
    return CODEC;
  }

  protected void createBlockStateDefinition(Builder<Block, BlockState> b) {
    b.add(new Property[] {LIT});
  }
}
