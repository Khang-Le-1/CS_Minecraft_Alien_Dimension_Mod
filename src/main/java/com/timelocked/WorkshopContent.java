package com.timelocked;

import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityType.Builder;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class WorkshopContent {
  public static final DeferredRegister<RecipeType<?>> TYPES =
      DeferredRegister.create(Registries.RECIPE_TYPE, "timelocked");
  public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
      DeferredRegister.create(Registries.RECIPE_SERIALIZER, "timelocked");
  public static final DeferredHolder<RecipeType<?>, RecipeType<VehicleAssemblyRecipe>>
      ASSEMBLY_TYPE =
          TYPES.register(
              "vehicle_assembly",
              () ->
                  new RecipeType<VehicleAssemblyRecipe>() {
                    @Override
                    public String toString() {
                      return "timelocked:vehicle_assembly";
                    }
                  });
  public static final DeferredHolder<RecipeSerializer<?>, VehicleAssemblyRecipe.Serializer>
      ASSEMBLY_SERIALIZER =
          SERIALIZERS.register("vehicle_assembly", VehicleAssemblyRecipe.Serializer::new);
  public static final DeferredBlock<VehicleAssemblyBlock> BENCH =
      Content.BLOCKS.register(
          "vehicle_assembly_bench",
          () ->
              new VehicleAssemblyBlock(
                  Properties.of().strength(3.5F).sound(SoundType.METAL).noOcclusion()));
  public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VehicleAssemblyEntity>>
      ASSEMBLY_ENTITY =
          Content.BLOCK_ENTITIES.register(
              "vehicle_assembly_bench",
              () ->
                  Builder.of(VehicleAssemblyEntity::new, new Block[] {(Block) BENCH.get()})
                      .build(null));
  public static final DeferredItem<Item> RUBBER = Content.ITEMS.registerSimpleItem("rubber");
  public static final DeferredItem<Item> WHEEL = Content.ITEMS.registerSimpleItem("vehicle_wheel");
  public static final DeferredItem<Item> MOTOR = Content.ITEMS.registerSimpleItem("vehicle_motor");
  public static final DeferredItem<Item> CHASSIS =
      Content.ITEMS.registerSimpleItem("vehicle_chassis");
  public static final DeferredItem<Item> TRUCK_CAB = Content.ITEMS.registerSimpleItem("truck_cab");
  public static final DeferredItem<Item> ROVER_CAB = Content.ITEMS.registerSimpleItem("rover_cab");
  public static final DeferredItem<Item> HOT_CHOCOLATE_MIX =
      Content.ITEMS.registerSimpleItem("hot_chocolate_mix");
  public static final DeferredItem<Item> CHOCOLATE =
      Content.ITEMS.register(
          "chocolate",
          () ->
              new Item(
                  new net.minecraft.world.item.Item.Properties()
                      .food(
                          new net.minecraft.world.food.FoodProperties.Builder()
                              .nutrition(4)
                              .saturationModifier(0.3F)
                              .build())));
  public static final DeferredItem<Item> HOT_CHOCOLATE =
      Content.ITEMS.register(
          "hot_chocolate",
          () ->
              new HotChocolateItem(
                  new net.minecraft.world.item.Item.Properties()
                      .stacksTo(1)
                      .food(
                          new net.minecraft.world.food.FoodProperties.Builder()
                              .nutrition(6)
                              .saturationModifier(0.5F)
                              .alwaysEdible()
                              .usingConvertsTo(Items.GLASS_BOTTLE)
                              .build())));
  public static final DeferredItem<Item> ESTROGEN =
      Content.ITEMS.register(
          "estrogen",
          () ->
              new Item(new net.minecraft.world.item.Item.Properties()) {
                public void appendHoverText(
                    ItemStack stack,
                    TooltipContext context,
                    List<Component> lines,
                    TooltipFlag flag) {
                  lines.add(Component.translatable("tooltip.timelocked.estrogen"));
                }
              });

  public static void initialize(IEventBus bus) {
    Content.ITEMS.register(
        "vehicle_assembly_bench",
        () ->
            new BlockItem((Block) BENCH.get(), new net.minecraft.world.item.Item.Properties()) {
              public void appendHoverText(
                  ItemStack stack,
                  TooltipContext context,
                  List<Component> lines,
                  TooltipFlag flag) {
                lines.add(Component.translatable("tooltip.timelocked.vehicle_assembly"));
                lines.add(Component.translatable("tooltip.timelocked.vehicle_assembly_recipe"));
              }
            });
    TYPES.register(bus);
    SERIALIZERS.register(bus);
    bus.addListener(
        (RegisterCapabilitiesEvent e) ->
            e.registerBlockEntity(
                ItemHandler.BLOCK, ASSEMBLY_ENTITY.get(), (be, side) -> be.itemsCapability));
  }

  private WorkshopContent() {}
}
