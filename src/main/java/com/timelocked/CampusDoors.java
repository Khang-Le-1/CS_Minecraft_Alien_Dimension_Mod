package com.timelocked;

import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.neoforged.neoforge.registries.DeferredBlock;

/** Static cutout-glass doors: no SARC branding or animated-door entity dependency. */
final class CampusDoors {
  static final DeferredBlock<GlassDoor> GLASS =
      Content.BLOCKS.register("campus_glass_door", () -> new GlassDoor());

  static final DeferredBlock<GlassDoor> BROWN =
      Content.BLOCKS.register("scc_brown_glazed_door", () -> new GlassDoor());

  static final class GlassDoor extends DoorBlock {
    GlassDoor() {
      super(BlockSetType.OAK, BlockBehaviour.Properties.of().strength(3).noOcclusion());
    }
  }

  static void initialize() {}

  private CampusDoors() {}
}
