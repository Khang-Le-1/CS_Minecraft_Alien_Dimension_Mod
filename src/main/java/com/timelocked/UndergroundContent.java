package com.timelocked;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityType.Builder;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class UndergroundContent {
  public static final DeferredRegister<MenuType<?>> MENUS =
      DeferredRegister.create(Registries.MENU, "timelocked");
  public static final DeferredHolder<MenuType<?>, MenuType<ReactorFuelMenu>> FUEL_MENU =
      MENUS.register(
          "reactor_fuel",
          () ->
              new MenuType(
                  (id, inv) -> new ReactorFuelMenu(id, inv, new SimpleContainer(3)),
                  FeatureFlags.VANILLA_SET));
  public static final DeferredBlock<TramClearanceBlock> CLEARANCE =
      Content.BLOCKS.register(
          "tram_clearance_interlock",
          () -> new TramClearanceBlock(metal().noOcclusion().dynamicShape()));
  public static final DeferredBlock<Block> VESSEL_GLOW =
      Content.BLOCKS.registerSimpleBlock("reactor_vessel_glow", metal().lightLevel(b -> 4));
  public static final DeferredBlock<ReactorCoreBlock> CORE =
      Content.BLOCKS.register(
          "sarc_reactor",
          () ->
              new ReactorCoreBlock(
                  metal()
                      .noOcclusion()
                      .lightLevel(s -> s.getValue(ReactorCoreBlock.ACTIVE) ? 8 : 0)));
  public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ReactorCoreBlock.Vessel>>
      CORE_ENTITY =
          Content.BLOCK_ENTITIES.register(
              "sarc_reactor",
              () ->
                  Builder.of(ReactorCoreBlock.Vessel::new, new Block[] {(Block) CORE.get()})
                      .build(null));
  public static final DeferredBlock<PoweredRailBlock> TRACK =
      Content.BLOCKS.register(
          "sarc_tram_track", () -> new PoweredRailBlock(metal().noCollission().noOcclusion()));
  public static final DeferredHolder<EntityType<?>, EntityType<SarcLiftEntity>> LIFT =
      Content.ENTITIES.register(
          "sarc_lift",
          () ->
              net.minecraft.world.entity.EntityType.Builder.of(
                      SarcLiftEntity::new, MobCategory.MISC)
                  .sized(3.0F, 0.25F)
                  .clientTrackingRange(12)
                  .updateInterval(1)
                  .build("timelocked:sarc_lift"));
  public static final DeferredBlock<LiftCallBlock> LIFT_CALL =
      Content.BLOCKS.register("lift_call_panel", () -> new LiftCallBlock(metal().noOcclusion()));
  public static final DeferredBlock<LiftDoorBlock> LIFT_DOOR =
      Content.BLOCKS.register("lift_landing_door", () -> new LiftDoorBlock(metal().noOcclusion()));
  public static final DeferredBlock<ReactorStartupBlock> STARTUP =
      Content.BLOCKS.register(
          "reactor_startup_panel", () -> new ReactorStartupBlock(metal().noOcclusion()));
  public static final DeferredBlock<Block> CEILING_EMERGENCY =
      Content.BLOCKS.registerSimpleBlock("emergency_ceiling_light", metal().lightLevel(s -> 15));
  public static final DeferredBlock<ReactorFuelBlock> FUEL =
      Content.BLOCKS.register("reactor_fuel_system", () -> new ReactorFuelBlock(metal()));
  public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ReactorFuelEntity>>
      FUEL_ENTITY =
          Content.BLOCK_ENTITIES.register(
              "reactor_fuel_system",
              () ->
                  Builder.of(ReactorFuelEntity::new, new Block[] {(Block) FUEL.get()}).build(null));

  private static Properties metal() {
    return Properties.of()
        .strength(5.0F, 8.0F)
        .sound(SoundType.METAL)
        .isValidSpawn((s, l, p, t) -> false);
  }

  public static void initialize(IEventBus bus) {
    Content.ITEMS.registerSimpleBlockItem(CLEARANCE);
    Content.ITEMS.registerSimpleBlockItem(VESSEL_GLOW);
    Content.ITEMS.registerSimpleBlockItem(LIFT_CALL);
    Content.ITEMS.registerSimpleBlockItem(LIFT_DOOR);
    Content.ITEMS.registerSimpleBlockItem(STARTUP);
    Content.ITEMS.registerSimpleBlockItem(CEILING_EMERGENCY);
    Content.ITEMS.registerSimpleBlockItem(CORE);
    Content.ITEMS.registerSimpleBlockItem(TRACK);
    Content.ITEMS.registerSimpleBlockItem(FUEL);
    MENUS.register(bus);
  }

  private UndergroundContent() {}
}
