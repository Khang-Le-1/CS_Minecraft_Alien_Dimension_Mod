package com.timelocked;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.shapes.*;
import net.neoforged.neoforge.registries.DeferredBlock;

/** Small furniture and light models with real dimensions, not full-cube/slab stand-ins. */
final class CampusFurnishings {
  static final DeferredBlock<Piece> DESK = piece("campus_student_desk", 0),
      CHAIR = piece("campus_student_chair", 1),
      PANEL = piece("campus_ceiling_panel", 2),
      HOME_CHAIR = piece("home_armchair", 1),
      CAR_FRONT = piece("campus_parked_car_front", 3),
      CAR_REAR = piece("campus_parked_car_rear", 3);

  static DeferredBlock<Piece> piece(String id, int kind) {
    return Content.BLOCKS.register(id, () -> new Piece(kind));
  }

  static final class Piece extends HorizontalDirectionalBlock {
    static final MapCodec<Piece> CODEC = simpleCodec(p -> new Piece(0));
    final int kind;

    Piece(int kind) {
      super(
          BlockBehaviour.Properties.of()
              .strength(2)
              .noOcclusion()
              .lightLevel(s -> kind == 2 ? 15 : 0));
      this.kind = kind;
      registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public MapCodec<Piece> codec() {
      return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
      b.add(FACING);
    }

    @Override
    protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
      return kind == 2
          ? Block.box(1, 13, 1, 15, 16, 15)
          : kind == 3
              ? Block.box(0, 0, 0, 32, 24, 32)
              : kind == 0 ? Block.box(0, 0, 0, 16, 12, 16) : Block.box(2, 0, 2, 14, 14, 14);
    }

    @Override
    protected BlockState rotate(BlockState s, Rotation r) {
      return s.setValue(FACING, r.rotate(s.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState s, Mirror m) {
      return s.rotate(m.getRotation(s.getValue(FACING)));
    }
  }

  static void put(
      net.minecraft.server.level.ServerLevel l, BlockPos p, DeferredBlock<Piece> b, Direction d) {
    l.setBlock(p, b.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, d), 2);
  }

  static void initialize() {}

  private CampusFurnishings() {}
}
