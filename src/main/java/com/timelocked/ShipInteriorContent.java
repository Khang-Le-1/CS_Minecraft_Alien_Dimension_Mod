package com.timelocked;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.neoforged.neoforge.registries.DeferredBlock;

public final class ShipInteriorContent {
  public static final DeferredBlock<ShipFurnitureBlock> FLIGHT_SEAT =
      Content.BLOCKS.register(
          "timeskipper_flight_seat",
          () ->
              new ShipFurnitureBlock(
                  Properties.of().strength(3.0F, 6.0F).sound(SoundType.METAL).noOcclusion(), 0));
  public static final DeferredBlock<ShipFurnitureBlock> CREW_LOCKER =
      Content.BLOCKS.register(
          "timeskipper_crew_locker",
          () ->
              new ShipFurnitureBlock(
                  Properties.of().strength(3.0F, 6.0F).sound(SoundType.METAL).noOcclusion(), 1));
  public static final DeferredBlock<ShipFurnitureBlock> BEDSIDE_UNIT =
      Content.BLOCKS.register(
          "timeskipper_bedside_unit",
          () ->
              new ShipFurnitureBlock(
                  Properties.of().strength(3.0F, 6.0F).sound(SoundType.METAL).noOcclusion(), 2));
  public static final DeferredBlock<ShipFurnitureBlock> MEDICAL_CART =
      Content.BLOCKS.register(
          "timeskipper_medical_cart",
          () ->
              new ShipFurnitureBlock(
                  Properties.of().strength(3.0F, 6.0F).sound(SoundType.METAL).noOcclusion(), 3));
  public static final DeferredBlock<ShipFurnitureBlock> GALLEY_TABLE =
      Content.BLOCKS.register(
          "timeskipper_galley_table",
          () ->
              new ShipFurnitureBlock(
                  Properties.of().strength(3.0F, 6.0F).sound(SoundType.METAL).noOcclusion(), 4));
  public static final DeferredBlock<ShipFurnitureBlock> DRINKS_STATION =
      Content.BLOCKS.register(
          "timeskipper_drinks_station",
          () ->
              new ShipFurnitureBlock(
                  Properties.of().strength(3.0F, 6.0F).sound(SoundType.METAL).noOcclusion(), 5));
  public static final DeferredBlock<ShipFurnitureBlock> CARGO_RACK =
      Content.BLOCKS.register(
          "timeskipper_cargo_rack",
          () ->
              new ShipFurnitureBlock(
                  Properties.of().strength(3.0F, 6.0F).sound(SoundType.METAL).noOcclusion(), 6));
  public static final DeferredBlock<ShipFurnitureBlock> COOLANT_PUMP =
      Content.BLOCKS.register(
          "timeskipper_coolant_pump",
          () ->
              new ShipFurnitureBlock(
                  Properties.of().strength(3.0F, 6.0F).sound(SoundType.METAL).noOcclusion(), 7));

  public static final DeferredBlock<Block> DECK =
      Content.BLOCKS.registerSimpleBlock(
          "timeskipper_deck_panel", Properties.of().strength(3.0F, 6.0F).sound(SoundType.METAL));
  public static final DeferredBlock<Block> PATH =
      Content.BLOCKS.registerSimpleBlock(
          "timeskipper_path_panel", Properties.of().strength(3.0F, 6.0F).sound(SoundType.METAL));
  public static final DeferredBlock<Block> BULKHEAD =
      Content.BLOCKS.registerSimpleBlock(
          "timeskipper_bulkhead", Properties.of().strength(3.0F, 6.0F).sound(SoundType.METAL));
  public static final DeferredBlock<Block> CEILING =
      Content.BLOCKS.registerSimpleBlock(
          "timeskipper_ceiling_panel", Properties.of().strength(3.0F, 6.0F).sound(SoundType.METAL));
  public static final DeferredBlock<Block> THRESHOLD =
      Content.BLOCKS.registerSimpleBlock(
          "timeskipper_threshold", Properties.of().strength(3.0F, 6.0F).sound(SoundType.METAL));
  public static final DeferredBlock<Block> LIGHT_PANEL =
      Content.BLOCKS.registerSimpleBlock(
          "timeskipper_light_panel",
          Properties.of().strength(3.0F, 6.0F).sound(SoundType.METAL).lightLevel(s -> 13));
  public static final DeferredBlock<TransparentBlock> COCKPIT_GLASS =
      Content.BLOCKS.register(
          "timeskipper_cockpit_glass",
          () ->
              new TransparentBlock(
                  Properties.of().strength(2.0F).sound(SoundType.GLASS).noOcclusion()));

  public static final DeferredBlock<net.minecraft.world.level.block.SlabBlock> SERVICE_SLAB =
      Content.BLOCKS.register(
          "timeskipper_service_slab",
          () ->
              new net.minecraft.world.level.block.SlabBlock(
                  Properties.of().strength(3F).sound(SoundType.METAL)));
  public static final DeferredBlock<AirlockJambBlock> AIRLOCK_JAMB =
      Content.BLOCKS.register(
          "timeskipper_airlock_jamb",
          () ->
              new AirlockJambBlock(
                  Properties.of().strength(5F).sound(SoundType.METAL).noOcclusion()));
  public static final DeferredBlock<BoardingAirlockBlock> BOARDING_AIRLOCK =
      Content.BLOCKS.register(
          "timeskipper_boarding_airlock",
          () ->
              new BoardingAirlockBlock(
                  Properties.of().strength(5F).sound(SoundType.METAL).noOcclusion()));
  public static final DeferredBlock<BoardingRampBlock> BOARDING_RAMP =
      Content.BLOCKS.register(
          "timeskipper_boarding_ramp",
          () ->
              new BoardingRampBlock(
                  Properties.of().strength(5F).sound(SoundType.METAL).noOcclusion()));
  public static final DeferredBlock<DockingPortBlock> DOCK_PORT =
      Content.BLOCKS.register(
          "timeskipper_docking_port",
          () ->
              new DockingPortBlock(
                  Properties.of().strength(5F).sound(SoundType.METAL).noOcclusion()));
  public static final DeferredBlock<Block> DOCK_FRAME =
      Content.BLOCKS.registerSimpleBlock(
          "timeskipper_docking_arm_frame",
          Properties.of().strength(5F).sound(SoundType.METAL).noOcclusion());
  public static final net.neoforged.neoforge.registries.DeferredHolder<
          net.minecraft.world.level.block.entity.BlockEntityType<?>,
          net.minecraft.world.level.block.entity.BlockEntityType<DockingPortBlock.Port>>
      DOCK_ENTITY =
          Content.BLOCK_ENTITIES.register(
              "timeskipper_docking_port",
              () ->
                  net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(
                          DockingPortBlock.Port::new, DOCK_PORT.get())
                      .build(null));

  public static void initialize() {
    for (DeferredBlock<?> b :
        new DeferredBlock[] {
          SERVICE_SLAB,
          AIRLOCK_JAMB,
          BOARDING_AIRLOCK,
          BOARDING_RAMP,
          DOCK_PORT,
          DOCK_FRAME,
          FLIGHT_SEAT,
          CREW_LOCKER,
          BEDSIDE_UNIT,
          MEDICAL_CART,
          GALLEY_TABLE,
          DRINKS_STATION,
          CARGO_RACK,
          COOLANT_PUMP,
          DECK,
          PATH,
          BULKHEAD,
          CEILING,
          THRESHOLD,
          LIGHT_PANEL,
          COCKPIT_GLASS
        }) {
      Content.ITEMS.registerSimpleBlockItem(b);
    }
  }

  private ShipInteriorContent() {}
}
