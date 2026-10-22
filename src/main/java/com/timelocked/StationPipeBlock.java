package com.timelocked;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class StationPipeBlock extends RotatedPillarBlock {
  public static final MapCodec<StationPipeBlock> CODEC = simpleCodec(StationPipeBlock::new);

  public StationPipeBlock(Properties p) {
    super(p);
  }

  public MapCodec<? extends RotatedPillarBlock> codec() {
    return CODEC;
  }

  protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
    return switch (s.getValue(AXIS)) {
      case Y ->
          Shapes.or(
              Block.box(4, 0, 4, 12, 16, 12),
              Block.box(2, 0, 2, 14, 2, 14),
              Block.box(2, 14, 2, 14, 16, 14));
      case X ->
          Shapes.or(
              Block.box(0, 4, 4, 16, 12, 12),
              Block.box(0, 2, 2, 2, 14, 14),
              Block.box(14, 2, 2, 16, 14, 14));
      case Z ->
          Shapes.or(
              Block.box(4, 4, 0, 12, 12, 16),
              Block.box(2, 2, 0, 14, 14, 2),
              Block.box(2, 2, 14, 14, 14, 16));
    };
  }
}
