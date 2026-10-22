package com.timelocked;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class AlienPlantBlock extends Block {
  public static final MapCodec<AlienPlantBlock> CODEC =
      simpleCodec(p -> new AlienPlantBlock(p, 10));
  private final VoxelShape shape;

  public AlienPlantBlock(Properties p, int height) {
    super(p);
    this.shape = Block.box(3.0, 0.0, 3.0, 13.0, height, 13.0);
  }

  protected MapCodec<? extends Block> codec() {
    return CODEC;
  }

  protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
    Vec3 o = s.getOffset(l, p);
    return this.shape.move(o.x, o.y, o.z);
  }

  protected VoxelShape getCollisionShape(
      BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
    return Shapes.empty();
  }

  protected boolean canSurvive(BlockState s, LevelReader l, BlockPos p) {
    return l.getBlockState(p.below()).isFaceSturdy(l, p.below(), Direction.UP);
  }

  protected BlockState updateShape(
      BlockState s, Direction d, BlockState n, LevelAccessor l, BlockPos p, BlockPos np) {
    return d == Direction.DOWN && !this.canSurvive(s, l, p) ? Blocks.AIR.defaultBlockState() : s;
  }

  protected boolean propagatesSkylightDown(BlockState s, BlockGetter l, BlockPos p) {
    return true;
  }
}
