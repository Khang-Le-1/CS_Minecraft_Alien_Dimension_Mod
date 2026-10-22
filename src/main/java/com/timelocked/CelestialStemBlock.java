package com.timelocked;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class CelestialStemBlock extends Block {
  public static final MapCodec<CelestialStemBlock> CODEC = simpleCodec(CelestialStemBlock::new);
  private static final VoxelShape SHAPE =
      Shapes.or(
          box(6.0, 0.0, 6.0, 10.0, 16.0, 10.0),
          new VoxelShape[] {
            box(0.0, 7.0, 7.0, 16.0, 9.0, 9.0), box(7.0, 7.0, 0.0, 9.0, 9.0, 16.0)
          });

  public CelestialStemBlock(Properties p) {
    super(p);
  }

  protected MapCodec<? extends Block> codec() {
    return CODEC;
  }

  protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
    return SHAPE;
  }
}
