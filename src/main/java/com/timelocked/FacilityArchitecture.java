package com.timelocked;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;

/** Shared architectural kit. Full cubes replace only full structural cubes. */
public final class FacilityArchitecture {
  public static final DeferredBlock<Block> CLEAN = panel("sarc_clean_panel"),
      SERVICE = panel("sarc_service_panel"),
      CONTAINMENT = panel("sarc_containment_panel"),
      ROUTE = panel("sarc_route_tile"),
      VENT = panel("sarc_vent_panel"),
      ACOUSTIC =
          Content.BLOCKS.registerSimpleBlock(
              "sarc_acoustic_ceiling",
              BlockBehaviour.Properties.of().strength(1.5F).sound(SoundType.STONE));

  private static DeferredBlock<Block> panel(String id) {
    return Content.BLOCKS.registerSimpleBlock(
        id,
        BlockBehaviour.Properties.of()
            .strength(4, 8)
            .sound(SoundType.METAL)
            .requiresCorrectToolForDrops());
  }

  public static void initialize() {}

  static {
    for (var b : new DeferredBlock[] {CLEAN, SERVICE, CONTAINMENT, ROUTE, VENT, ACOUSTIC})
      Content.ITEMS.registerSimpleBlockItem(b);
  }

  private FacilityArchitecture() {}
}
