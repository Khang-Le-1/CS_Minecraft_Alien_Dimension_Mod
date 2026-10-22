package com.timelocked;

import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityType.Builder;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour.OffsetType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

public final class VoidspaceContent {
  public static final DeferredBlock<Block> EMBER_CRUST =
      Content.BLOCKS.registerSimpleBlock(
          "ember_crust",
          Properties.of()
              .strength(1.2F, 3.0F)
              .sound(SoundType.BASALT)
              .lightLevel(s -> 6)
              .emissiveRendering((s, l, p) -> true));
  public static final DeferredBlock<Block> GLIMMER_MOSS =
      Content.BLOCKS.registerSimpleBlock(
          "glimmer_moss", Properties.of().strength(0.7F).sound(SoundType.MOSS).lightLevel(s -> 3));
  public static final DeferredBlock<Block> CRYSTAL_BLOCK =
      Content.BLOCKS.registerSimpleBlock(
          "void_crystal_block",
          Properties.of()
              .strength(1.5F, 4.0F)
              .sound(SoundType.AMETHYST)
              .lightLevel(s -> 7)
              .noOcclusion()
              .requiresCorrectToolForDrops());
  public static final DeferredBlock<Block> SPORE_CAP =
      Content.BLOCKS.registerSimpleBlock(
          "spore_cap",
          Properties.of().strength(0.4F).sound(SoundType.WART_BLOCK).lightLevel(s -> 8));
  public static final DeferredBlock<RotatedPillarBlock> SPORE_STEM =
      Content.BLOCKS.register(
          "spore_stem",
          () -> new RotatedPillarBlock(Properties.of().strength(1.0F).sound(SoundType.STEM)));
  public static final DeferredBlock<AlienPlantBlock> CRYSTAL_CLUSTER =
      Content.BLOCKS.register(
          "void_crystal_cluster",
          () -> new AlienPlantBlock(plant(SoundType.AMETHYST_CLUSTER, 9), 12));
  public static final DeferredBlock<AlienPlantBlock> ASHEN_TUFT =
      Content.BLOCKS.register(
          "ashen_tuft", () -> new AlienPlantBlock(plant(SoundType.GRASS, 0), 9));
  public static final DeferredBlock<AlienPlantBlock> STAR_BLOOM =
      Content.BLOCKS.register(
          "star_bloom", () -> new AlienPlantBlock(plant(SoundType.AZALEA, 6), 11));
  public static final DeferredBlock<AlienPlantBlock> SPORE_SPROUT =
      Content.BLOCKS.register(
          "spore_sprout", () -> new AlienPlantBlock(plant(SoundType.FUNGUS, 5), 8));
  public static final DeferredBlock<RotatedPillarBlock> CELESTIAL_BARK =
      Content.BLOCKS.register(
          "celestial_bark",
          () -> new RotatedPillarBlock(Properties.of().strength(1.4F).sound(SoundType.WOOD)));
  public static final DeferredBlock<Block> SILVERLEAF =
      Content.BLOCKS.registerSimpleBlock(
          "silverleaf_canopy",
          Properties.of()
              .strength(0.25F)
              .noOcclusion()
              .noCollission()
              .sound(SoundType.AZALEA_LEAVES)
              .lightLevel(s -> 3));
  public static final DeferredBlock<Block> MOONVEIL_LEAVES =
      Content.BLOCKS.registerSimpleBlock(
          "moonveil_canopy",
          Properties.of()
              .strength(0.25F)
              .noOcclusion()
              .noCollission()
              .sound(SoundType.AZALEA_LEAVES)
              .lightLevel(s -> 4));
  public static final DeferredBlock<Block> STARLEAF =
      Content.BLOCKS.registerSimpleBlock(
          "hanging_starleaf",
          Properties.of()
              .strength(0.25F)
              .noOcclusion()
              .noCollission()
              .sound(SoundType.AZALEA_LEAVES)
              .lightLevel(s -> 7));
  public static final DeferredBlock<AlienPlantBlock> MOON_FERN =
      Content.BLOCKS.register(
          "moon_fern", () -> new AlienPlantBlock(plant(SoundType.AZALEA, 3), 14));
  public static final DeferredBlock<AlienPlantBlock> LANTERN_BLOOM =
      Content.BLOCKS.register(
          "lantern_bloom", () -> new AlienPlantBlock(plant(SoundType.AZALEA, 9), 14));
  public static final DeferredBlock<AlienPlantBlock> PRISM_REED =
      Content.BLOCKS.register(
          "prism_reed", () -> new AlienPlantBlock(plant(SoundType.AZALEA, 5), 14));
  public static final DeferredBlock<AlienPlantBlock> DUSK_LOTUS =
      Content.BLOCKS.register(
          "dusk_lotus", () -> new AlienPlantBlock(plant(SoundType.AZALEA, 6), 14));
  public static final DeferredHolder<EntityType<?>, EntityType<Ashback>> ASHBACK =
      Content.ENTITIES.register(
          "ashback",
          () ->
              Builder.of(Ashback::new, MobCategory.CREATURE)
                  .sized(1.3F, 1.4F)
                  .clientTrackingRange(10)
                  .build("timelocked:ashback"));
  public static final DeferredHolder<EntityType<?>, EntityType<Voidskitter>> SKITTER =
      Content.ENTITIES.register(
          "voidskitter",
          () ->
              Builder.of(Voidskitter::new, MobCategory.MONSTER)
                  .sized(0.9F, 0.45F)
                  .clientTrackingRange(8)
                  .build("timelocked:voidskitter"));
  public static final DeferredHolder<EntityType<?>, EntityType<GlimmerMoth>> MOTH =
      Content.ENTITIES.register(
          "glimmer_moth",
          () ->
              Builder.of(GlimmerMoth::new, MobCategory.AMBIENT)
                  .sized(0.5F, 0.4F)
                  .clientTrackingRange(8)
                  .build("timelocked:glimmer_moth"));
  public static final DeferredHolder<EntityType<?>, EntityType<Glowkit>> GLOWKIT =
      Content.ENTITIES.register(
          "glowkit",
          () ->
              Builder.of(Glowkit::new, MobCategory.CREATURE)
                  .sized(0.6F, 0.75F)
                  .clientTrackingRange(10)
                  .build("timelocked:glowkit"));
  public static final DeferredHolder<EntityType<?>, EntityType<CelestialVillager>> CELESTIAL =
      Content.ENTITIES.register(
          "celestial",
          () ->
              Builder.of(CelestialVillager::new, MobCategory.MISC)
                  .sized(0.6F, 1.75F)
                  .eyeHeight(1.5F)
                  .clientTrackingRange(10)
                  .build("timelocked:celestial"));
  public static final DeferredHolder<EntityType<?>, EntityType<Dawnbringer>> DAWN_LEGIONNAIRE =
      dawn("dawnbringer_legionnaire", 0.6F, 1.95F);
  public static final DeferredHolder<EntityType<?>, EntityType<Dawnbringer>> DAWN_SUNSPEAKER =
      dawn("dawnbringer_sunspeaker", 0.6F, 1.95F);
  public static final DeferredHolder<EntityType<?>, EntityType<Dawnbringer>> DAWN_WARLORD =
      dawn("dawnbringer_warlord", 0.85F, 2.6F);
  public static final DeferredItem<Item> DAWN_SIGIL =
      Content.ITEMS.register(
          "dawnbringer_sigil",
          () ->
              new Item(
                  new net.minecraft.world.item.Item.Properties()
                      .rarity(Rarity.EPIC)
                      .fireResistant()));

  private static Properties plant(SoundType s, int light) {
    return Properties.of()
        .noCollission()
        .noOcclusion()
        .instabreak()
        .sound(s)
        .offsetType(OffsetType.XZ)
        .lightLevel(st -> light)
        .pushReaction(PushReaction.DESTROY);
  }

  private static DeferredHolder<EntityType<?>, EntityType<Dawnbringer>> dawn(
      String id, float w, float h) {
    return Content.ENTITIES.register(
        id,
        () ->
            Builder.of(Dawnbringer::new, MobCategory.MONSTER)
                .sized(w, h)
                .eyeHeight(h * 0.88F)
                .fireImmune()
                .clientTrackingRange(10)
                .build("timelocked:" + id));
  }

  public static void initialize(IEventBus bus) {
    CelestialJobs.initialize();
    for (DeferredBlock<?> b :
        new DeferredBlock[] {
          EMBER_CRUST,
          GLIMMER_MOSS,
          CRYSTAL_BLOCK,
          SPORE_CAP,
          SPORE_STEM,
          CRYSTAL_CLUSTER,
          ASHEN_TUFT,
          STAR_BLOOM,
          SPORE_SPROUT,
          CELESTIAL_BARK,
          SILVERLEAF,
          MOONVEIL_LEAVES,
          STARLEAF,
          MOON_FERN,
          LANTERN_BLOOM,
          PRISM_REED,
          DUSK_LOTUS
        }) {
      Content.ITEMS.registerSimpleBlockItem(b);
    }

    Content.ITEMS.register(
        "ashback_spawn_egg",
        () ->
            new DeferredSpawnEggItem(
                ASHBACK, 4867394, 14711354, new net.minecraft.world.item.Item.Properties()));
    Content.ITEMS.register(
        "voidskitter_spawn_egg",
        () ->
            new DeferredSpawnEggItem(
                SKITTER, 1841200, 9332158, new net.minecraft.world.item.Item.Properties()));
    Content.ITEMS.register(
        "glowkit_spawn_egg",
        () ->
            new DeferredSpawnEggItem(
                GLOWKIT, 12175846, 15976778, new net.minecraft.world.item.Item.Properties()));
    Content.ITEMS.register(
        "celestial_spawn_egg",
        () ->
            new DeferredSpawnEggItem(
                CELESTIAL, 14272506, 16771466, new net.minecraft.world.item.Item.Properties()));
    Content.ITEMS.register(
        "dawnbringer_legionnaire_spawn_egg",
        () ->
            new DeferredSpawnEggItem(
                DAWN_LEGIONNAIRE,
                2760218,
                15905338,
                new net.minecraft.world.item.Item.Properties()));
    Content.ITEMS.register(
        "dawnbringer_sunspeaker_spawn_egg",
        () ->
            new DeferredSpawnEggItem(
                DAWN_SUNSPEAKER,
                9313308,
                16769136,
                new net.minecraft.world.item.Item.Properties()));
    Content.ITEMS.register(
        "dawnbringer_warlord_spawn_egg",
        () ->
            new DeferredSpawnEggItem(
                DAWN_WARLORD, 1314830, 16742938, new net.minecraft.world.item.Item.Properties()));
    Content.ITEMS.register(
        "glimmer_moth_spawn_egg",
        () ->
            new DeferredSpawnEggItem(
                MOTH, 3037802, 10482687, new net.minecraft.world.item.Item.Properties()));
    bus.addListener(
        (EntityAttributeCreationEvent e) -> {
          e.put((EntityType) ASHBACK.get(), Ashback.attributes().build());
          e.put((EntityType) SKITTER.get(), Voidskitter.attributes().build());
          e.put((EntityType) MOTH.get(), GlimmerMoth.attributes().build());
          e.put((EntityType) GLOWKIT.get(), Glowkit.attributes().build());
          e.put((EntityType) CELESTIAL.get(), CelestialVillager.attributes().build());
          e.put((EntityType) DAWN_LEGIONNAIRE.get(), Dawnbringer.attributes(0).build());
          e.put((EntityType) DAWN_SUNSPEAKER.get(), Dawnbringer.attributes(1).build());
          e.put((EntityType) DAWN_WARLORD.get(), Dawnbringer.attributes(2).build());
        });
    bus.addListener(
        (RegisterSpawnPlacementsEvent e) -> {
          e.register(
              (EntityType) ASHBACK.get(),
              SpawnPlacementTypes.ON_GROUND,
              Types.MOTION_BLOCKING_NO_LEAVES,
              (t, l, r, p, rand) ->
                  l.getBlockState(p.below()).isFaceSturdy(l, p.below(), Direction.UP),
              Operation.REPLACE);
          e.register(
              (EntityType) GLOWKIT.get(),
              SpawnPlacementTypes.ON_GROUND,
              Types.MOTION_BLOCKING_NO_LEAVES,
              (t, l, r, p, rand) ->
                  l.getBlockState(p.below()).isFaceSturdy(l, p.below(), Direction.UP),
              Operation.REPLACE);
          e.register(
              (EntityType) SKITTER.get(),
              SpawnPlacementTypes.ON_GROUND,
              Types.MOTION_BLOCKING_NO_LEAVES,
              Monster::checkAnyLightMonsterSpawnRules,
              Operation.REPLACE);
          e.register(
              (EntityType) MOTH.get(),
              SpawnPlacementTypes.NO_RESTRICTIONS,
              Types.MOTION_BLOCKING_NO_LEAVES,
              (t, l, r, p, rand) -> l.getBlockState(p).isAir(),
              Operation.REPLACE);
        });
    VoidspaceEvents.register();
  }

  public static Block block(String id) {
    return (Block) BuiltInRegistries.BLOCK.get(ResourceLocation.parse(id));
  }

  private VoidspaceContent() {}
}
