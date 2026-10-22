package com.timelocked;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem.Type;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorMaterial.Layer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityType.Builder;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class MetalContent {
  public static final String[] ORDINARY =
      new String[] {
        "copper",
        "tin",
        "zinc",
        "lead",
        "nickel",
        "aluminum",
        "silver",
        "gold",
        "platinum",
        "tungsten",
        "titanium",
        "osmium",
        "uranium",
        "cobaltite",
        "iridite",
        "chromite"
      };
  public static final String[] EXOTIC =
      new String[] {
        "ardanium",
        "orithichalite",
        "arthilite",
        "adamantite",
        "vibranite",
        "aetherite",
        "celestrium",
        "luminite",
        "necronium",
        "solarium",
        "ecliptite",
        "neutronium",
        "gravitite",
        "chronotite",
        "aetherium"
      };
  public static final String[] ALLOYS =
      new String[] {"steel", "bronze", "brass", "voidsteel", "tungsten_steel"};
  public static final Map<String, Supplier<Item>> INGOTS = new LinkedHashMap<>();
  public static final Map<String, Supplier<Item>> RAW = new LinkedHashMap<>();
  public static final Map<String, Supplier<Item>> PLATES = new LinkedHashMap<>();
  public static final Map<String, Supplier<Block>> ORES = new LinkedHashMap<>();
  public static final Map<String, Supplier<Block>> STORAGE = new LinkedHashMap<>();
  public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
      DeferredRegister.create(Registries.ARMOR_MATERIAL, "timelocked");
  public static final DeferredBlock<FactoryBlock> ALLOY_SMELTER;
  public static final DeferredBlock<FactoryBlock> PLATE_PRESS;
  public static final DeferredBlock<IndustrialCrateBlock> CRATE;
  public static final DeferredBlock<EngineeringBenchBlock> BENCH;
  public static final DeferredBlock<Block> STEEL_BRICKS;
  public static final DeferredBlock<Block> TITANIUM_PANEL;
  public static final DeferredBlock<Block> GRATING;
  public static final DeferredBlock<Block> HULL;
  public static final DeferredBlock<Block> FACTORY_LAMP;
  public static final DeferredBlock<TransparentBlock> VIEWPORT;
  public static final DeferredItem<Item> FABRIC;
  public static final DeferredHolder<ArmorMaterial, ArmorMaterial> SUIT_MATERIAL;
  public static final DeferredItem<SpaceArmorItem> HELMET;
  public static final DeferredItem<SpaceArmorItem> CHEST;
  public static final DeferredItem<SpaceArmorItem> LEGS;
  public static final DeferredItem<SpaceArmorItem> BOOTS;
  public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FactoryEntity>>
      FACTORY_ENTITY;
  public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<IndustrialCrateEntity>>
      CRATE_ENTITY;

  private static Properties metal() {
    return Properties.of()
        .strength(4.0F, 6.0F)
        .sound(SoundType.METAL)
        .requiresCorrectToolForDrops();
  }

  private static SpaceArmorItem armor(Type type) {
    return new SpaceArmorItem(
        SUIT_MATERIAL,
        type,
        new net.minecraft.world.item.Item.Properties().durability(type.getDurability(20)));
  }

  public static Item ingot(String n) {
    return INGOTS.get(n).get();
  }

  public static Item plate(String n) {
    return PLATES.get(n).get();
  }

  public static void capabilities(RegisterCapabilitiesEvent e) {
    e.registerBlockEntity(EnergyStorage.BLOCK, FACTORY_ENTITY.get(), (b, s) -> b.receiver);
    e.registerBlockEntity(ItemHandler.BLOCK, FACTORY_ENTITY.get(), (b, s) -> b.itemsCapability);
    e.registerBlockEntity(ItemHandler.BLOCK, CRATE_ENTITY.get(), (b, s) -> b.itemsCapability);
  }

  public static void initialize() {}

  private MetalContent() {}

  static {
    INGOTS.put("copper", () -> Items.COPPER_INGOT);
    INGOTS.put("gold", () -> Items.GOLD_INGOT);
    INGOTS.put("steel", Content.STEEL::get);
    INGOTS.put("tungsten", Content.TUNGSTEN::get);
    RAW.put("copper", () -> Items.RAW_COPPER);
    RAW.put("gold", () -> Items.RAW_GOLD);
    RAW.put("tungsten", Content.RAW_TUNGSTEN::get);
    ORES.put("copper", () -> Blocks.COPPER_ORE);
    ORES.put("gold", () -> Blocks.GOLD_ORE);
    ORES.put("tungsten", Content.TUNGSTEN_ORE::get);
    STORAGE.put("copper", () -> Blocks.COPPER_BLOCK);
    STORAGE.put("gold", () -> Blocks.GOLD_BLOCK);
    List<String> all = new ArrayList<>();
    all.addAll(List.of(ORDINARY));
    all.addAll(List.of(EXOTIC));
    all.addAll(List.of(ALLOYS));

    for (String name : all) {
      if (!INGOTS.containsKey(name)) {
        INGOTS.put(name, Content.ITEMS.registerSimpleItem(name + "_ingot")::get);
      }

      if (!List.of(ALLOYS).contains(name)) {
        if (!RAW.containsKey(name)) {
          RAW.put(name, Content.ITEMS.registerSimpleItem("raw_" + name)::get);
        }

        if (!ORES.containsKey(name)) {
          DeferredBlock<Block> ore =
              Content.BLOCKS.registerSimpleBlock(
                  name + "_ore",
                  Properties.of()
                      .strength(3.5F)
                      .requiresCorrectToolForDrops()
                      .lightLevel(s -> name.equals("ardanium") ? 6 : 0));
          ORES.put(name, ore::get);
          Content.ITEMS.registerSimpleBlockItem(ore);
        }
      }

      if (!STORAGE.containsKey(name)) {
        DeferredBlock<Block> block = Content.BLOCKS.registerSimpleBlock(name + "_block", metal());
        STORAGE.put(name, block::get);
        Content.ITEMS.registerSimpleBlockItem(block);
      }

      PLATES.put(name, Content.ITEMS.registerSimpleItem(name + "_plate")::get);
    }

    ALLOY_SMELTER = Content.BLOCKS.register("alloy_smelter", () -> new FactoryBlock(metal()));
    PLATE_PRESS =
        Content.BLOCKS.register("plate_press", () -> new FactoryBlock(metal().noOcclusion()));
    CRATE = Content.BLOCKS.register("industrial_crate", () -> new IndustrialCrateBlock(metal()));
    BENCH =
        Content.BLOCKS.register(
            "engineering_bench",
            () ->
                new EngineeringBenchBlock(
                    Properties.of().strength(2.5F).sound(SoundType.METAL).noOcclusion()));
    STEEL_BRICKS = Content.BLOCKS.registerSimpleBlock("steel_bricks", metal());
    TITANIUM_PANEL = Content.BLOCKS.registerSimpleBlock("titanium_panel", metal());
    GRATING = Content.BLOCKS.registerSimpleBlock("steel_grating", metal());
    HULL = Content.BLOCKS.registerSimpleBlock("space_hull_panel", metal());
    FACTORY_LAMP = Content.BLOCKS.registerSimpleBlock("factory_lamp", metal().lightLevel(s -> 15));
    VIEWPORT =
        Content.BLOCKS.register(
            "space_viewport",
            () ->
                new TransparentBlock(
                    Properties.of().strength(1.5F).noOcclusion().sound(SoundType.GLASS)));
    FABRIC = Content.ITEMS.registerSimpleItem("insulation_fabric");
    SUIT_MATERIAL =
        ARMOR_MATERIALS.register(
            "sarc_space",
            () ->
                new ArmorMaterial(
                    Map.of(
                        Type.HELMET,
                        2,
                        Type.CHESTPLATE,
                        6,
                        Type.LEGGINGS,
                        5,
                        Type.BOOTS,
                        2,
                        Type.BODY,
                        6),
                    10,
                    SoundEvents.ARMOR_EQUIP_IRON,
                    () -> Ingredient.of(new ItemLike[] {ingot("titanium")}),
                    List.of(
                        new Layer(
                            ResourceLocation.fromNamespaceAndPath("timelocked", "sarc_space"))),
                    0.0F,
                    0.0F));
    HELMET = Content.ITEMS.register("sarc_space_helmet", () -> armor(Type.HELMET));
    CHEST = Content.ITEMS.register("sarc_space_chestplate", () -> armor(Type.CHESTPLATE));
    LEGS = Content.ITEMS.register("sarc_space_leggings", () -> armor(Type.LEGGINGS));
    BOOTS = Content.ITEMS.register("sarc_space_boots", () -> armor(Type.BOOTS));
    FACTORY_ENTITY =
        Content.BLOCK_ENTITIES.register(
            "industrial_processor",
            () ->
                Builder.of(
                        FactoryEntity::new,
                        new Block[] {(Block) ALLOY_SMELTER.get(), (Block) PLATE_PRESS.get()})
                    .build(null));
    CRATE_ENTITY =
        Content.BLOCK_ENTITIES.register(
            "industrial_crate",
            () ->
                Builder.of(IndustrialCrateEntity::new, new Block[] {(Block) CRATE.get()})
                    .build(null));

    for (DeferredBlock<?> b :
        new DeferredBlock[] {
          ALLOY_SMELTER,
          PLATE_PRESS,
          CRATE,
          BENCH,
          STEEL_BRICKS,
          TITANIUM_PANEL,
          GRATING,
          HULL,
          FACTORY_LAMP,
          VIEWPORT
        }) {
      Content.ITEMS.registerSimpleBlockItem(b);
    }
  }
}
