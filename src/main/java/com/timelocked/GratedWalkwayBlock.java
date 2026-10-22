package com.timelocked;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class GratedWalkwayBlock extends Block {
  public static final MapCodec<GratedWalkwayBlock> CODEC = simpleCodec(GratedWalkwayBlock::new);
  private static final VoxelShape SHAPE = Block.box(0.0, 12.0, 0.0, 16.0, 16.0, 16.0);

  public GratedWalkwayBlock(Properties p) {
    super(p);
  }

  protected MapCodec<? extends Block> codec() {
    return CODEC;
  }

  protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
    return SHAPE;
  }
}
