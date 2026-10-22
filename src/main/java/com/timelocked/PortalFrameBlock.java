package com.timelocked;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;

public final class PortalFrameBlock extends Block {
  public static final MapCodec<PortalFrameBlock> CODEC = simpleCodec(PortalFrameBlock::new);

  public PortalFrameBlock(Properties p) {
    super(p);
  }

  protected MapCodec<? extends Block> codec() {
    return CODEC;
  }

  protected RenderShape getRenderShape(BlockState s) {
    return RenderShape.INVISIBLE;
  }
}
