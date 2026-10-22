package com.timelocked;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityType.Builder;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

public final class SpaceContent {
  public static final DeferredBlock<Block> REGOLITH =
      Content.BLOCKS.registerSimpleBlock(
          "lunar_regolith",
          Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY).strength(0.6F).sound(SoundType.SAND));
  public static final DeferredBlock<Block> LUNAR_ROCK =
      Content.BLOCKS.registerSimpleBlock("lunar_rock", rock(MapColor.COLOR_GRAY, 1.6F));
  public static final DeferredBlock<Block> MARS_SAND =
      Content.BLOCKS.registerSimpleBlock(
          "martian_regolith",
          Properties.of()
              .mapColor(MapColor.TERRACOTTA_ORANGE)
              .strength(0.6F)
              .sound(SoundType.SAND));
  public static final DeferredBlock<Block> MARS_ROCK =
      Content.BLOCKS.registerSimpleBlock("martian_rock", rock(MapColor.TERRACOTTA_RED, 1.6F));
  public static final DeferredBlock<Block> ASTEROID_ROCK =
      Content.BLOCKS.registerSimpleBlock("asteroid_rock", rock(MapColor.TERRACOTTA_BROWN, 2.0F));
  public static final DeferredBlock<SpaceBlocks.Nav> NAV =
      Content.BLOCKS.register(
          "timeskipper_nav_console",
          () -> new SpaceBlocks.Nav(metal().lightLevel(s -> 9).noOcclusion()));
  public static final DeferredBlock<SpaceBlocks.Drive> DRIVE =
      Content.BLOCKS.register(
          "chronotite_drive", () -> new SpaceBlocks.Drive(metal().lightLevel(s -> 15)));
  public static final DeferredBlock<SpaceBlocks.Airlock> AIRLOCK =
      Content.BLOCKS.register(
          "timeskipper_airlock", () -> new SpaceBlocks.Airlock(metal().lightLevel(s -> 5)));
  public static final DeferredBlock<SpaceBlocks.Beacon> BEACON =
      Content.BLOCKS.register(
          "timeskipper_beacon", () -> new SpaceBlocks.Beacon(metal().lightLevel(s -> 12)));
  public static final DeferredBlock<SpaceBlocks.LifeSupport> LIFE_SUPPORT =
      Content.BLOCKS.register(
          "life_support_unit", () -> new SpaceBlocks.LifeSupport(metal().lightLevel(s -> 8)));
  public static final DeferredBlock<SpaceBlocks.LifeSupport> MOONBREATH =
      Content.BLOCKS.register(
          "moonbreath_lantern",
          () ->
              new SpaceBlocks.LifeSupport(
                  Properties.of()
                      .mapColor(MapColor.COLOR_PINK)
                      .strength(1.5F)
                      .sound(SoundType.AMETHYST)
                      .lightLevel(st -> 15)
                      .noOcclusion()));
  public static final DeferredBlock<ShipModelAnchorBlock> MODEL_ANCHOR =
      Content.BLOCKS.register(
          "timeskipper_model_anchor",
          () ->
              new ShipModelAnchorBlock(
                  Properties.of()
                      .strength(-1.0F, 3600000.0F)
                      .noOcclusion()
                      .noCollission()
                      .noLootTable()));
  public static final DeferredHolder<
          BlockEntityType<?>, BlockEntityType<ShipModelAnchorBlock.ModelAnchor>>
      MODEL_ENTITY =
          Content.BLOCK_ENTITIES.register(
              "timeskipper_model_anchor",
              () ->
                  Builder.of(
                          ShipModelAnchorBlock.ModelAnchor::new,
                          new Block[] {(Block) MODEL_ANCHOR.get()})
                      .build(null));
  public static final DeferredItem<SpaceBlocks.Link> LINK =
      Content.ITEMS.register(
          "timeskipper_link",
          () ->
              new SpaceBlocks.Link(
                  new net.minecraft.world.item.Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
  public static final DeferredHolder<Feature<?>, SarcLaunchSites> LAUNCH_SITES =
      Content.FEATURES.register("sarc_launch_sites", SarcLaunchSites::new);
  public static final DeferredHolder<
          MapCodec<? extends ChunkGenerator>, MapCodec<PlanetChunkGenerator>>
      PLANET_GENERATOR =
          Content.CHUNK_GENERATORS.register("planet", () -> PlanetChunkGenerator.CODEC);

  private static Properties rock(MapColor c, float s) {
    return Properties.of()
        .mapColor(c)
        .strength(s, 6.0F)
        .requiresCorrectToolForDrops()
        .sound(SoundType.STONE);
  }

  private static Properties metal() {
    return Properties.of()
        .mapColor(MapColor.METAL)
        .strength(5.0F, 1200.0F)
        .requiresCorrectToolForDrops()
        .sound(SoundType.NETHERITE_BLOCK);
  }

  public static void initialize(IEventBus bus) {
    for (DeferredBlock<?> b :
        new DeferredBlock[] {
          REGOLITH,
          LUNAR_ROCK,
          MARS_SAND,
          MARS_ROCK,
          ASTEROID_ROCK,
          NAV,
          DRIVE,
          AIRLOCK,
          BEACON,
          LIFE_SUPPORT,
          MOONBREATH
        }) {
      Content.ITEMS.registerSimpleBlockItem(b);
    }

    bus.addListener(SpaceNetworking::register);
    bus.addListener(JumpNetworking::register);
    SpaceEvents.register();
  }

  private SpaceContent() {}
}
