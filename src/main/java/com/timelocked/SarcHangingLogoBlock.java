/* Reconstructed from the user-provided 0.8.9.8 release with permission. */
package com.timelocked;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class SarcHangingLogoBlock extends Block {
  public static final MapCodec<SarcHangingLogoBlock> CODEC =
      SarcHangingLogoBlock.simpleCodec(SarcHangingLogoBlock::new);
  public static final BooleanProperty LIT = BlockStateProperties.LIT;
  // Grid order is also a persistent block-state ID: row * columns + column.
  private static final int LOGO_COLUMNS = 26;
  private static final int LOGO_ROWS = 11;
  private static final int PART_COUNT = LOGO_COLUMNS * LOGO_ROWS;
  private static final double PIXELS_PER_BLOCK = 16.0;
  public static final IntegerProperty PART = IntegerProperty.create("part", 0, PART_COUNT - 1);
  private static final VoxelShape[] SHAPES = new VoxelShape[PART_COUNT];

  public SarcHangingLogoBlock(BlockBehaviour.Properties p) {
    super(p);
    registerDefaultState(stateDefinition.any().setValue(PART, 0).setValue(LIT, true));
  }

  protected MapCodec<? extends SarcHangingLogoBlock> codec() {
    return CODEC;
  }

  protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
    b.add(PART, LIT);
  }

  protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
    return SHAPES[s.getValue(PART)];
  }

  static {
    var drawing = AuthoredGeometry.logo();
    if (drawing.columns() != LOGO_COLUMNS
        || drawing.rows() != LOGO_ROWS
        || drawing.pixelsPerBlock() != PIXELS_PER_BLOCK) {
      throw new IllegalStateException("Logo dimensions must preserve existing block-state IDs");
    }
    for (int part = 0; part < PART_COUNT; part++) {
      VoxelShape shape = Shapes.empty();
      for (var rectangle : drawing.parts().get(part)) {
        shape =
            Shapes.or(
                shape,
                Block.box(
                    rectangle.minX(),
                    rectangle.minY(),
                    0,
                    rectangle.maxX(),
                    rectangle.maxY(),
                    PIXELS_PER_BLOCK));
      }
      SHAPES[part] = shape.optimize();
    }
  }
}
