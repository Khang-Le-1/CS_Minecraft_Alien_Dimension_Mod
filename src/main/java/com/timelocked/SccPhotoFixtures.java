package com.timelocked;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.shapes.*;
import net.neoforged.neoforge.registries.DeferredBlock;

/** Photo-inspired fixtures. Models stay within their block; no invisible light blocks. */
final class SccPhotoFixtures {
  static final DeferredBlock<Fixture> STRIP = piece("scc_linear_light", 1),
      PENDANT = piece("scc_pendant_light", 2),
      RACK_LOWER = piece("scc_network_rack_lower", 3),
      RACK_UPPER = piece("scc_network_rack_upper", 3),
      TRAY = piece("scc_blue_cable_tray", 4);
  static final DeferredBlock<Block> WOOD = material("scc_wood_panels"),
      CEILING = material("scc_acoustic_ceiling"),
      CARPET = material("scc_streaked_carpet");

  static DeferredBlock<Block> material(String id) {
    return Content.BLOCKS.register(id, () -> new Block(BlockBehaviour.Properties.of().strength(2)));
  }

  static DeferredBlock<Fixture> piece(String id, int kind) {
    return Content.BLOCKS.register(id, () -> new Fixture(kind));
  }

  static final class Fixture extends CampusFurnishingsBase {
    static final MapCodec<Fixture> CODEC = simpleCodec(p -> new Fixture(0));
    final int kind;

    Fixture(int kind) {
      super(
          BlockBehaviour.Properties.of()
              .strength(2)
              .noOcclusion()
              .lightLevel(s -> kind == 1 || kind == 2 ? 15 : 0));
      this.kind = kind;
    }

    @Override
    public MapCodec<Fixture> codec() {
      return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
      return switch (kind) {
        case 1 -> Block.box(6, 14, 0, 10, 16, 16);
        case 2 -> Block.box(5, 2, 5, 11, 16, 11);
        case 3 -> Block.box(1, 0, 2, 15, 16, 15);
        default -> Block.box(1, 12, 0, 15, 16, 16);
      };
    }
  }

  // Shared direction support without inheriting v28's shape or altering existing block IDs.
  abstract static class CampusFurnishingsBase extends HorizontalDirectionalBlock {
    CampusFurnishingsBase(BlockBehaviour.Properties p) {
      super(p);
      registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
      b.add(FACING);
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
      net.minecraft.server.level.ServerLevel l, BlockPos p, DeferredBlock<Fixture> b, Direction d) {
    l.setBlock(p, b.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, d), 2);
  }

  static void initialize() {}

  private SccPhotoFixtures() {}
}
