package com.timelocked;

import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem.Type;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorMaterial.Layer;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

public final class AtlasContent {
  public static final DeferredHolder<ArmorMaterial, ArmorMaterial> EVERLEIGH_MATERIAL =
      MetalContent.ARMOR_MATERIALS.register(
          "atlas_hes_everleigh",
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
                  () -> Ingredient.of(new ItemLike[] {MetalContent.ingot("titanium")}),
                  List.of(
                      new Layer(
                          ResourceLocation.fromNamespaceAndPath(
                              "timelocked", "atlas_hes_everleigh"))),
                  0.0F,
                  0.0F));
  public static final DeferredItem<SpaceArmorItem> HELMET =
      Content.ITEMS.register("everleigh_atlas_helmet", () -> armor(Type.HELMET));
  public static final DeferredItem<SpaceArmorItem> CHEST =
      Content.ITEMS.register("everleigh_atlas_chestplate", () -> armor(Type.CHESTPLATE));
  public static final DeferredItem<SpaceArmorItem> LEGS =
      Content.ITEMS.register("everleigh_atlas_leggings", () -> armor(Type.LEGGINGS));
  public static final DeferredItem<SpaceArmorItem> BOOTS =
      Content.ITEMS.register("everleigh_atlas_boots", () -> armor(Type.BOOTS));

  private static SpaceArmorItem armor(Type type) {
    return new SpaceArmorItem(
        EVERLEIGH_MATERIAL, type, new Properties().durability(type.getDurability(20)), true);
  }

  public static void initialize() {}

  private AtlasContent() {}
}
