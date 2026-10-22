/* Reconstructed from the user-provided 0.8.9.8 release with permission. */
package com.timelocked;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class Content {
  public static final DeferredRegister.Blocks BLOCKS =
      DeferredRegister.createBlocks((String) "timelocked");
  public static final DeferredRegister.Items ITEMS =
      DeferredRegister.createItems((String) "timelocked");
  public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
      DeferredRegister.create((ResourceKey) Registries.BLOCK_ENTITY_TYPE, (String) "timelocked");
  public static final DeferredRegister<EntityType<?>> ENTITIES =
      DeferredRegister.create((ResourceKey) Registries.ENTITY_TYPE, (String) "timelocked");
  public static final DeferredHolder<EntityType<?>, EntityType<IntroBoat>> INTRO_BOAT =
      ENTITIES.register(
          "intro_boat",
          () ->
              EntityType.Builder.of(IntroBoat::new, MobCategory.MISC)
                  .sized(1.375f, .5625f)
                  .clientTrackingRange(12)
                  .updateInterval(1)
                  .build("timelocked:intro_boat"));
  public static final DeferredHolder<EntityType<?>, EntityType<Pantheris>> PANTHERIS =
      ENTITIES.register(
          "pantheris",
          () ->
              EntityType.Builder.of(Pantheris::new, MobCategory.MISC)
                  .sized(.65f, 1.95f)
                  .clientTrackingRange(12)
                  .updateInterval(1)
                  .build("timelocked:pantheris"));
  public static final DeferredHolder<EntityType<?>, EntityType<Makglo>> MAKGLO =
      ENTITIES.register(
          "makglo",
          () ->
              EntityType.Builder.of(Makglo::new, (MobCategory) MobCategory.MONSTER)
                  .sized(1.0f, 2.45f)
                  .clientTrackingRange(10)
                  .build("timelocked:makglo"));
  public static final DeferredHolder<EntityType<?>, EntityType<Unnamed>> UNNAMED =
      ENTITIES.register(
          "unnamed",
          () ->
              EntityType.Builder.of(Unnamed::new, (MobCategory) MobCategory.MONSTER)
                  .sized(0.9f, 2.2f)
                  .clientTrackingRange(10)
                  .build("timelocked:unnamed"));
  public static final DeferredHolder<EntityType<?>, EntityType<Greed>> GREED =
      ENTITIES.register(
          "greed",
          () ->
              EntityType.Builder.of(Greed::new, (MobCategory) MobCategory.MONSTER)
                  .sized(1.1f, 2.3f)
                  .clientTrackingRange(10)
                  .build("timelocked:greed"));
  public static final DeferredHolder<EntityType<?>, EntityType<SarcGroundVehicle>> ROVER =
      ENTITIES.register(
          "sarc_rover",
          () ->
              EntityType.Builder.of(SarcGroundVehicle::new, (MobCategory) MobCategory.MISC)
                  .sized(3.8f, 3.2f)
                  .clientTrackingRange(12)
                  .updateInterval(1)
                  .build("timelocked:sarc_rover"));
  public static final DeferredHolder<EntityType<?>, EntityType<SarcGroundVehicle>> TRUCK =
      ENTITIES.register(
          "sarc_truck",
          () ->
              EntityType.Builder.of(SarcGroundVehicle::new, (MobCategory) MobCategory.MISC)
                  .sized(3.8f, 2.9f)
                  .clientTrackingRange(12)
                  .updateInterval(1)
                  .build("timelocked:sarc_truck"));
  public static final DeferredHolder<EntityType<?>, EntityType<SarcTramEntity>> TRAM_VEHICLE =
      ENTITIES.register(
          "sarc_tram",
          () ->
              EntityType.Builder.of(SarcTramEntity::new, (MobCategory) MobCategory.MISC)
                  .sized(1.9f, 2.85f)
                  .clientTrackingRange(12)
                  .updateInterval(1)
                  .build("timelocked:sarc_tram"));
  public static final DeferredItem<VehicleDeploymentItem> ROVER_KIT =
      ITEMS.register(
          "rover_deployment",
          () -> new VehicleDeploymentItem(new Item.Properties().stacksTo(1), ROVER));
  public static final DeferredItem<VehicleDeploymentItem> TRUCK_KIT =
      ITEMS.register(
          "truck_deployment",
          () -> new VehicleDeploymentItem(new Item.Properties().stacksTo(1), TRUCK));
  public static final DeferredItem<VehicleDeploymentItem> TRAM_KIT =
      ITEMS.register(
          "tram_deployment",
          () -> new VehicleDeploymentItem(new Item.Properties().stacksTo(1), TRAM_VEHICLE));
  public static final DeferredRegister<MapCodec<? extends ChunkGenerator>> CHUNK_GENERATORS =
      DeferredRegister.create((ResourceKey) Registries.CHUNK_GENERATOR, (String) "timelocked");
  public static final DeferredRegister<MapCodec<? extends BiomeSource>> BIOME_SOURCES =
      DeferredRegister.create((ResourceKey) Registries.BIOME_SOURCE, (String) "timelocked");
  public static final DeferredHolder<
          MapCodec<? extends ChunkGenerator>, MapCodec<VoidspaceChunkGenerator>>
      VOIDSPACE_GENERATOR =
          CHUNK_GENERATORS.register("voidspace_fragment", () -> VoidspaceChunkGenerator.CODEC);
  public static final DeferredHolder<
          MapCodec<? extends BiomeSource>, MapCodec<VoidspaceBiomeSource>>
      VOIDSPACE_BIOMES =
          BIOME_SOURCES.register("voidspace_fragment", () -> VoidspaceBiomeSource.CODEC);
  public static final DeferredBlock<Block> CELESTIAL_SILT =
      BLOCKS.registerSimpleBlock(
          "celestial_silt", BlockBehaviour.Properties.of().strength(0.8f).sound(SoundType.SAND));
  public static final DeferredBlock<Block> ALIEN_LOAM =
      BLOCKS.registerSimpleBlock(
          "alien_loam", BlockBehaviour.Properties.of().strength(0.7f).sound(SoundType.GRAVEL));
  public static final DeferredBlock<CelestialStemBlock> CELESTIAL_STEM =
      BLOCKS.register(
          "celestial_stem",
          () ->
              new CelestialStemBlock(
                  BlockBehaviour.Properties.of()
                      .strength(0.8f)
                      .noOcclusion()
                      .sound(SoundType.WOOD)));
  public static final DeferredBlock<Block> CELESTIAL_FROND =
      BLOCKS.registerSimpleBlock(
          "celestial_frond",
          BlockBehaviour.Properties.of()
              .strength(0.3f)
              .noOcclusion()
              .noCollission()
              .lightLevel(s -> 4)
              .sound(SoundType.AMETHYST));
  public static final DeferredBlock<Block> ASH =
      BLOCKS.registerSimpleBlock(
          "voidspace_ash", BlockBehaviour.Properties.of().strength(0.6f).sound(SoundType.SAND));
  public static final DeferredBlock<Block> SCORCHED_STONE =
      BLOCKS.registerSimpleBlock(
          "scorched_stone", BlockBehaviour.Properties.of().strength(2.0f).sound(SoundType.STONE));
  public static final DeferredBlock<Block> CHARRED_TIMBER =
      BLOCKS.registerSimpleBlock(
          "charred_timber", BlockBehaviour.Properties.of().strength(1.0f).sound(SoundType.WOOD));
  public static final DeferredBlock<Block> CELESTIAL_MASONRY =
      BLOCKS.registerSimpleBlock(
          "celestial_masonry",
          BlockBehaviour.Properties.of().strength(2.0f).sound(SoundType.STONE));
  public static final DeferredBlock<Block> CELESTIAL_ROOF =
      BLOCKS.registerSimpleBlock(
          "celestial_roof", BlockBehaviour.Properties.of().strength(2.0f).sound(SoundType.STONE));
  public static final DeferredBlock<Block> CELESTIAL_PATH =
      BLOCKS.registerSimpleBlock(
          "celestial_path", BlockBehaviour.Properties.of().strength(1.5f).sound(SoundType.STONE));
  public static final DeferredRegister<CreativeModeTab> TABS =
      DeferredRegister.create((ResourceKey) Registries.CREATIVE_MODE_TAB, (String) "timelocked");
  public static final DeferredBlock<Block> SHELL =
      BLOCKS.registerSimpleBlock("sarc_reinforced_shell", Content.metal());
  public static final DeferredBlock<Block> WALL =
      BLOCKS.registerSimpleBlock("sarc_wall_panel", Content.metal());
  public static final DeferredBlock<Block> FLOOR =
      BLOCKS.registerSimpleBlock("sarc_floor_tile", Content.metal());
  public static final DeferredBlock<Block> DISPLAY =
      BLOCKS.registerSimpleBlock("sarc_status_display", Content.metal());
  public static final DeferredBlock<Block> CABINET =
      BLOCKS.registerSimpleBlock("sarc_service_cabinet", Content.metal());
  public static final DeferredBlock<Block> CASING =
      BLOCKS.registerSimpleBlock("sarc_containment_casing", Content.metal());
  public static final DeferredBlock<Block> HAZARD =
      BLOCKS.registerSimpleBlock("sarc_hazard_trim", Content.metal());
  public static final DeferredBlock<FacilityLampBlock> LIGHT =
      BLOCKS.register(
          "containment_light",
          () ->
              new FacilityLampBlock(
                  Content.metal()
                      .lightLevel(
                          s ->
                              (Boolean) s.getValue((Property) FacilityLampBlock.LIT) != false
                                  ? 15
                                  : 0)));
  public static final DeferredBlock<StairBlock> STAIRS =
      BLOCKS.register(
          "sarc_floor_stairs",
          () -> new StairBlock(((Block) FLOOR.get()).defaultBlockState(), Content.metal()));
  public static final DeferredBlock<SarcLogoPanelBlock> LOGO =
      BLOCKS.register(
          "sarc_logo_panel", () -> new SarcLogoPanelBlock(Content.metal().noOcclusion()));
  public static final DeferredBlock<SarcReceptionLogoBlock> RECEPTION_LOGO =
      BLOCKS.register(
          "sarc_reception_logo", () -> new SarcReceptionLogoBlock(Content.metal().noOcclusion()));
  public static final DeferredBlock<EntranceGlassDoorBlock> ENTRANCE_DOOR =
      BLOCKS.register(
          "entrance_glass_door", () -> new EntranceGlassDoorBlock(Content.metal().noOcclusion()));
  public static final DeferredBlock<SarcHangingLogoBlock> HANGING_LOGO =
      BLOCKS.register(
          "sarc_hanging_logo",
          () ->
              new SarcHangingLogoBlock(
                  Content.metal()
                      .noOcclusion()
                      .lightLevel(
                          st ->
                              (Boolean) st.getValue((Property) SarcHangingLogoBlock.LIT) != false
                                  ? 10
                                  : 0)));
  public static final DeferredBlock<SarcFullLogoBlock> FULL_LOGO =
      BLOCKS.register("sarc_full_logo", () -> new SarcFullLogoBlock(Content.metal().noOcclusion()));
  public static final DeferredBlock<AccessDoorBlock> AIRLOCK =
      BLOCKS.register("powered_airlock", () -> new AccessDoorBlock(Content.metal().noOcclusion()));
  public static final DeferredBlock<AccessDoorBlock> SECURITY_GATE =
      BLOCKS.register(
          "security_checkpoint", () -> new AccessDoorBlock(Content.metal().noOcclusion()));
  public static final DeferredBlock<ReactorConsoleBlock> REACTOR_CONSOLE =
      BLOCKS.register("reactor_console", () -> new ReactorConsoleBlock(Content.metal()));
  public static final DeferredBlock<TramTerminalBlock> TRAM =
      BLOCKS.register("tram_terminal", () -> new TramTerminalBlock(Content.metal()));
  public static final DeferredBlock<SarcHighwayPanelBlock> HIGHWAY =
      BLOCKS.register(
          "highway_sign_panel", () -> new SarcHighwayPanelBlock(Content.metal().noOcclusion()));
  public static final DeferredItem<Item> FUEL_CELL = ITEMS.registerSimpleItem("sealed_fuel_cell");
  public static final DeferredItem<Item> KEYCARD = ITEMS.registerSimpleItem("sarc_access_card");
  public static final DeferredRegister<Feature<?>> FEATURES =
      DeferredRegister.create((ResourceKey) Registries.FEATURE, (String) "timelocked");
  public static final DeferredHolder<Feature<?>, CampaignFeature> CAMPAIGN =
      FEATURES.register("sarc_campaign", CampaignFeature::new);
  public static final DeferredBlock<ElectricArcFurnaceBlock> EAF =
      BLOCKS.register("electric_arc_furnace", () -> new ElectricArcFurnaceBlock(Content.metal()));
  public static final DeferredBlock<PowerSupplyBlock> POWER =
      BLOCKS.register("redstone_power_supply", () -> new PowerSupplyBlock(Content.metal()));
  public static final DeferredBlock<Block> CONDUIT =
      BLOCKS.registerSimpleBlock("electrical_conduit", Content.metal());
  public static final DeferredBlock<PortalControllerBlock> CONTROLLER =
      BLOCKS.register(
          "portal_controller", () -> new PortalControllerBlock(Content.metal().noOcclusion()));
  public static final DeferredBlock<PortalFieldBlock> FIELD =
      BLOCKS.register(
          "portal_field",
          () ->
              new PortalFieldBlock(
                  BlockBehaviour.Properties.of()
                      .strength(-1.0f, 3600000.0f)
                      .noCollission()
                      .noOcclusion()
                      .noLootTable()
                      .lightLevel(s -> 11)));
  public static final DeferredBlock<SupportStationBlock> MEDICAL =
      BLOCKS.register("medical_station", () -> new SupportStationBlock(Content.metal()));
  public static final DeferredBlock<SupportStationBlock> DECON =
      BLOCKS.register("decontamination_station", () -> new SupportStationBlock(Content.metal()));
  public static final DeferredBlock<Block> TUNGSTEN_ORE =
      BLOCKS.registerSimpleBlock(
          "tungsten_ore",
          BlockBehaviour.Properties.of().strength(4.0f).requiresCorrectToolForDrops());
  public static final DeferredItem<Item> LIGHT_ROUND = ITEMS.registerSimpleItem("light_round");
  public static final DeferredItem<Item> RIFLE_ROUND = ITEMS.registerSimpleItem("rifle_round");
  public static final DeferredItem<Item> PULSE_CELL = ITEMS.registerSimpleItem("pulse_cell");
  public static final DeferredItem<GunItem> SERVICE_PISTOL =
      ITEMS.register(
          "service_pistol",
          () -> new GunItem(() -> LIGHT_ROUND.get(), 12, 6.0f, 8, 40, 48, false, false));
  public static final DeferredItem<GunItem> CONTAINMENT_CARBINE =
      ITEMS.register(
          "containment_carbine",
          () -> new GunItem(() -> RIFLE_ROUND.get(), 24, 5.0f, 5, 55, 64, true, false));
  public static final DeferredItem<GunItem> PULSE_RIFLE =
      ITEMS.register(
          "pulse_rifle",
          () -> new GunItem(() -> PULSE_CELL.get(), 6, 14.0f, 24, 65, 56, false, true));
  public static final DeferredItem<BatonItem> SECURITY_BATON =
      ITEMS.register("security_baton", BatonItem::new);
  public static final DeferredItem<Item> TUNGSTEN = ITEMS.registerSimpleItem("tungsten_ingot");
  public static final DeferredItem<Item> RAW_TUNGSTEN = ITEMS.registerSimpleItem("raw_tungsten");
  public static final DeferredItem<Item> CAST_IRON = ITEMS.registerSimpleItem("cast_iron_ingot");
  public static final DeferredItem<Item> STEEL = ITEMS.registerSimpleItem("steel_ingot");
  public static final DeferredItem<Item> CIRCUIT = ITEMS.registerSimpleItem("circuit_board");
  public static final DeferredItem<Item> BANDAGE = ITEMS.registerSimpleItem("bandage");
  public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ArcFurnaceEntity>>
      EAF_ENTITY =
          BLOCK_ENTITIES.register(
              "electric_arc_furnace",
              () ->
                  BlockEntityType.Builder.of(
                          ArcFurnaceEntity::new, (Block[]) new Block[] {(Block) EAF.get()})
                      .build(null));
  public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PowerSupplyEntity>>
      POWER_ENTITY =
          BLOCK_ENTITIES.register(
              "redstone_power_supply",
              () ->
                  BlockEntityType.Builder.of(
                          PowerSupplyEntity::new, (Block[]) new Block[] {(Block) POWER.get()})
                      .build(null));
  public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PortalControllerEntity>>
      CONTROLLER_ENTITY =
          BLOCK_ENTITIES.register(
              "portal_controller",
              () ->
                  BlockEntityType.Builder.of(
                          PortalControllerEntity::new,
                          (Block[]) new Block[] {(Block) CONTROLLER.get()})
                      .build(null));

  private static BlockBehaviour.Properties metal() {
    return BlockBehaviour.Properties.of()
        .strength(4.0f, 6.0f)
        .sound(SoundType.METAL)
        .isValidSpawn((s, l, pos, type) -> false);
  }

  public static void capabilities(RegisterCapabilitiesEvent e) {
    e.registerBlockEntity(
        Capabilities.EnergyStorage.BLOCK, EAF_ENTITY.get(), (be, side) -> be.receiver);
    e.registerBlockEntity(
        Capabilities.EnergyStorage.BLOCK, POWER_ENTITY.get(), (be, side) -> be.output);
    e.registerBlockEntity(
        Capabilities.EnergyStorage.BLOCK, CONTROLLER_ENTITY.get(), (be, side) -> be.receiver);
    e.registerBlockEntity(
        Capabilities.ItemHandler.BLOCK, EAF_ENTITY.get(), (be, side) -> be.itemsCapability);
    e.registerBlockEntity(
        Capabilities.ItemHandler.BLOCK, POWER_ENTITY.get(), (be, side) -> be.itemsCapability);
  }

  public static void initialize() {}

  private Content() {}

  static {
    for (DeferredBlock b :
        new DeferredBlock[] {
          CELESTIAL_SILT,
          ALIEN_LOAM,
          CELESTIAL_STEM,
          CELESTIAL_FROND,
          ASH,
          SCORCHED_STONE,
          CHARRED_TIMBER,
          CELESTIAL_MASONRY,
          CELESTIAL_ROOF,
          CELESTIAL_PATH,
          SHELL,
          WALL,
          FLOOR,
          DISPLAY,
          CABINET,
          CASING,
          HAZARD,
          LIGHT,
          STAIRS,
          LOGO,
          RECEPTION_LOGO,
          FULL_LOGO,
          HANGING_LOGO,
          ENTRANCE_DOOR,
          EAF,
          POWER,
          CONDUIT,
          CONTROLLER,
          MEDICAL,
          DECON,
          TUNGSTEN_ORE,
          AIRLOCK,
          SECURITY_GATE,
          REACTOR_CONSOLE,
          TRAM,
          HIGHWAY
        }) {
      ITEMS.registerSimpleBlockItem((Holder) b);
    }
    TABS.register(
        "zero_point",
        () ->
            CreativeModeTab.builder()
                .title((Component) Component.translatable((String) "itemGroup.timelocked"))
                .icon(() -> ((SarcLogoPanelBlock) LOGO.get()).asItem().getDefaultInstance())
                .displayItems(
                    (p, o) -> ITEMS.getEntries().forEach(e -> o.accept((ItemLike) e.get())))
                .build());
  }
}
