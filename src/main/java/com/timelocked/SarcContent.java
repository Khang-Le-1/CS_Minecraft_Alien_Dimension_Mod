package com.timelocked;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityType.Builder;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class SarcContent {
  public static final DeferredHolder<EntityType<?>, EntityType<SarcScientist>> SCIENTIST =
      Content.ENTITIES.register(
          "sarc_scientist",
          () ->
              Builder.of(SarcScientist::new, MobCategory.MISC)
                  .sized(0.6F, 1.95F)
                  .eyeHeight(1.62F)
                  .clientTrackingRange(10)
                  .build("timelocked:sarc_scientist"));
  public static final DeferredHolder<EntityType<?>, EntityType<FailedSubject>> SUBJECT =
      Content.ENTITIES.register(
          "failed_subject",
          () ->
              Builder.of(FailedSubject::new, MobCategory.MONSTER)
                  .sized(0.6F, 1.95F)
                  .eyeHeight(1.62F)
                  .clientTrackingRange(8)
                  .build("timelocked:failed_subject"));
  public static final DeferredHolder<Feature<?>, SarcOutposts> OUTPOSTS =
      Content.FEATURES.register("sarc_outposts", SarcOutposts::new);
  public static final DeferredBlock<PlantCoreBlock> CORE =
      Content.BLOCKS.register(
          "sarc_plant_core",
          () ->
              new PlantCoreBlock(steel().lightLevel(s -> s.getValue(PlantCoreBlock.LIT) ? 14 : 5)));
  public static final DeferredBlock<PlantConsoleBlock> CONSOLE =
      Content.BLOCKS.register(
          "sarc_plant_console", () -> new PlantConsoleBlock(steel().lightLevel(s -> 6)));
  public static final DeferredBlock<ReactorCasingBlock> CASING =
      Content.BLOCKS.register(
          "reactor_casing",
          () ->
              new ReactorCasingBlock(
                  Properties.of()
                      .strength(5.0F, 1200.0F)
                      .sound(SoundType.NETHERITE_BLOCK)
                      .noOcclusion()
                      .noLootTable()
                      .pushReaction(PushReaction.BLOCK)
                      .isValidSpawn((st, l, p, t) -> false)
                      .isRedstoneConductor((st, l, p) -> false)
                      .isSuffocating((st, l, p) -> false)
                      .isViewBlocking((st, l, p) -> false)
                      .randomTicks()));
  public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PlantCoreBlock.Core>>
      CORE_ENTITY =
          Content.BLOCK_ENTITIES.register(
              "sarc_plant_core",
              () ->
                  net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(
                          PlantCoreBlock.Core::new, new Block[] {(Block) CORE.get()})
                      .build(null));

  public static final DeferredBlock<StationLightBlock> STATION_LIGHT =
      Content.BLOCKS.register(
          "station_utility_light",
          () -> new StationLightBlock(steel().noOcclusion().lightLevel(s -> 15)));
  public static final DeferredBlock<StationEquipmentBlock> STATION_CONSOLE =
      Content.BLOCKS.register(
          "station_control_console",
          () -> new StationEquipmentBlock(steel().noOcclusion().lightLevel(s -> 5), 0));
  public static final DeferredBlock<StationEquipmentBlock> STATION_PUMP =
      Content.BLOCKS.register(
          "station_coolant_pump", () -> new StationEquipmentBlock(steel().noOcclusion(), 1));
  public static final DeferredBlock<StationEquipmentBlock> STATION_TURBINE =
      Content.BLOCKS.register(
          "station_turbine_module", () -> new StationEquipmentBlock(steel().noOcclusion(), 2));
  public static final DeferredBlock<StationEquipmentBlock> MONITOR_PEDESTAL =
      Content.BLOCKS.register(
          "station_monitor_pedestal", () -> new StationEquipmentBlock(steel().noOcclusion(), 3));
  public static final DeferredBlock<StationPipeBlock> STATION_PIPE =
      Content.BLOCKS.register(
          "station_coolant_pipe", () -> new StationPipeBlock(steel().noOcclusion()));

  private static Properties steel() {
    return Properties.of()
        .strength(6.0F, 1200.0F)
        .requiresCorrectToolForDrops()
        .sound(SoundType.NETHERITE_BLOCK);
  }

  public static void initialize(IEventBus bus) {
    Content.ITEMS.registerSimpleBlockItem(CORE);
    Content.ITEMS.registerSimpleBlockItem(CONSOLE);
    Content.ITEMS.registerSimpleBlockItem(STATION_LIGHT);
    Content.ITEMS.registerSimpleBlockItem(STATION_CONSOLE);
    Content.ITEMS.registerSimpleBlockItem(STATION_PUMP);
    Content.ITEMS.registerSimpleBlockItem(STATION_TURBINE);
    Content.ITEMS.registerSimpleBlockItem(STATION_PIPE);
    Content.ITEMS.registerSimpleBlockItem(MONITOR_PEDESTAL);
    PlantMeltdown.register();
    bus.addListener(
        (RegisterCapabilitiesEvent e) ->
            e.registerBlockEntity(EnergyStorage.BLOCK, CORE_ENTITY.get(), (be, side) -> be.energy));
    Content.ITEMS.register(
        "sarc_scientist_spawn_egg",
        () ->
            new DeferredSpawnEggItem(
                SCIENTIST, 15921906, 8016584, new net.minecraft.world.item.Item.Properties()));
    Content.ITEMS.register(
        "failed_subject_spawn_egg",
        () ->
            new DeferredSpawnEggItem(
                SUBJECT, 10336432, 11550975, new net.minecraft.world.item.Item.Properties()));
    bus.addListener(
        (EntityAttributeCreationEvent e) -> {
          e.put((EntityType) SCIENTIST.get(), SarcScientist.attributes().build());
          e.put((EntityType) SUBJECT.get(), FailedSubject.attributes().build());
        });
    bus.addListener(
        (RegisterSpawnPlacementsEvent e) ->
            e.register(
                (EntityType) SUBJECT.get(),
                SpawnPlacementTypes.ON_GROUND,
                Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules,
                Operation.REPLACE));
  }

  private SarcContent() {}
}
