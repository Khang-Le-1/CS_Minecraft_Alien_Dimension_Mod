package com.timelocked;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapelessRecipe;

public final class VehicleAssemblyRecipe extends ShapelessRecipe {
  public VehicleAssemblyRecipe(ShapelessRecipe r) {
    super(r.getGroup(), r.category(), r.getResultItem(null), r.getIngredients());
    if (r.getIngredients().size() > 8) {
      throw new IllegalArgumentException("Assembly bench has eight input slots");
    }
  }

  public RecipeType<?> getType() {
    return (RecipeType<?>) WorkshopContent.ASSEMBLY_TYPE.get();
  }

  public RecipeSerializer<?> getSerializer() {
    return (RecipeSerializer<?>) WorkshopContent.ASSEMBLY_SERIALIZER.get();
  }

  public static final class Serializer implements RecipeSerializer<VehicleAssemblyRecipe> {
    private final net.minecraft.world.item.crafting.ShapelessRecipe.Serializer nativeSerializer =
        new net.minecraft.world.item.crafting.ShapelessRecipe.Serializer();

    public MapCodec<VehicleAssemblyRecipe> codec() {
      return this.nativeSerializer.codec().xmap(VehicleAssemblyRecipe::new, r -> r);
    }

    public StreamCodec<RegistryFriendlyByteBuf, VehicleAssemblyRecipe> streamCodec() {
      return this.nativeSerializer.streamCodec().map(VehicleAssemblyRecipe::new, r -> r);
    }
  }
}
