/* Reconstructed from the user-provided 0.8.9.8 release with permission. */
package com.timelocked;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class LabFluids {
  public static final DeferredRegister<FluidType> TYPES =
      DeferredRegister.create(
          (ResourceKey) NeoForgeRegistries.Keys.FLUID_TYPES, (String) "timelocked");
  public static final DeferredRegister<Fluid> FLUIDS =
      DeferredRegister.create((ResourceKey) Registries.FLUID, (String) "timelocked");
  public static final Spec PORTAL = new Spec("portal_condensate", 9, 1800, 3200, 300);
  public static final Spec COOLANT = new Spec("laboratory_coolant", 4, 1200, 1800, 270);

  public static void initialize(IEventBus bus) {
    TYPES.register(bus);
    FLUIDS.register(bus);
  }

  private LabFluids() {}

  public static final class Spec {
    public final DeferredHolder<FluidType, FluidType> type;
    public final DeferredHolder<Fluid, BaseFlowingFluid.Source> source;
    public final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> flowing;
    public final DeferredBlock<LiquidBlock> block;
    public final DeferredItem<BucketItem> bucket;

    Spec(String id, int light, int density, int viscosity, int temperature) {
      this.type =
          TYPES.register(
              id,
              () ->
                  new FluidType(
                      FluidType.Properties.create()
                          .descriptionId("fluid_type.timelocked." + id)
                          .density(density)
                          .viscosity(viscosity)
                          .temperature(temperature)
                          .lightLevel(light)
                          .canConvertToSource(false)
                          .canHydrate(false)
                          .canDrown(true)
                          .canSwim(true)) {});
      this.source = FLUIDS.register(id, () -> new BaseFlowingFluid.Source(this.properties()));
      this.flowing =
          FLUIDS.register("flowing_" + id, () -> new BaseFlowingFluid.Flowing(this.properties()));
      this.block =
          Content.BLOCKS.register(
              id,
              () ->
                  new LiquidBlock(
                      (FlowingFluid) this.source.get(),
                      BlockBehaviour.Properties.ofFullCopy((BlockBehaviour) Blocks.WATER)
                          .lightLevel(s -> light)));
      this.bucket =
          Content.ITEMS.register(
              id + "_bucket",
              () ->
                  new BucketItem(
                      (Fluid) this.source.get(),
                      new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));
    }

    private BaseFlowingFluid.Properties properties() {
      return new BaseFlowingFluid.Properties(this.type, this.source, this.flowing)
          .block(this.block)
          .bucket(this.bucket)
          .tickRate(12)
          .slopeFindDistance(3)
          .levelDecreasePerBlock(2)
          .explosionResistance(100.0f);
    }
  }
}
