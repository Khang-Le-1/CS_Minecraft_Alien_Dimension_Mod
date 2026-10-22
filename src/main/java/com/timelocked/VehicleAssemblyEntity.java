package com.timelocked;

import java.util.ArrayList;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public final class VehicleAssemblyEntity extends MachineEntity {
  public int progress;
  private String active = "";

  public VehicleAssemblyEntity(BlockPos p, BlockState s) {
    super((BlockEntityType<?>) WorkshopContent.ASSEMBLY_ENTITY.get(), p, s);
  }

  @Override
  public boolean accepts(int slot, ItemStack s) {
    return slot >= 0 && slot < 8;
  }

  protected Component getDefaultName() {
    return Component.translatable("container.timelocked.vehicle_assembly");
  }

  @Override
  public String status() {
    return "Vehicle assembly | eight inputs | bottom-right output | "
        + this.progress
        + " / 60 ticks | no power required";
  }

  public CraftingInput input() {
    ArrayList<ItemStack> stacks = new ArrayList<>();

    for (int i = 0; i < 8; i++) {
      stacks.add(this.getItem(i));
    }

    stacks.add(ItemStack.EMPTY);
    return CraftingInput.of(3, 3, stacks);
  }

  public void tick() {
    if (this.level != null && !this.level.isClientSide) {
      CraftingInput input = this.input();
      Optional<RecipeHolder<VehicleAssemblyRecipe>> found =
          this.level
              .getRecipeManager()
              .getRecipeFor((RecipeType) WorkshopContent.ASSEMBLY_TYPE.get(), input, this.level);
      if (found.isEmpty()) {
        this.reset();
        this.report();
      } else {
        RecipeHolder<VehicleAssemblyRecipe> recipe = found.get();
        ItemStack result =
            ((VehicleAssemblyRecipe) recipe.value()).assemble(input, this.level.registryAccess());
        ItemStack out = this.getItem(8);
        if (!result.isEmpty()
            && (out.isEmpty()
                || ItemStack.isSameItemSameComponents(out, result)
                    && out.getCount() + result.getCount() <= out.getMaxStackSize())) {
          String key = recipe.id().toString();
          if (!this.active.equals(key)) {
            this.progress = 0;
            this.active = key;
          }

          if (++this.progress >= 60) {
            for (int i = 0; i < 8; i++) {
              if (!this.getItem(i).isEmpty()) {
                this.getItem(i).shrink(1);
              }
            }

            if (out.isEmpty()) {
              this.setItem(8, result.copy());
            } else {
              out.grow(result.getCount());
            }

            this.progress = 0;
            this.active = "";
          }

          this.setChanged();
          this.report();
        } else {
          this.reset();
          this.report();
        }
      }
    }
  }

  private void reset() {
    if (this.progress != 0 || !this.active.isEmpty()) {
      this.progress = 0;
      this.active = "";
      this.setChanged();
    }
  }

  @Override
  protected void saveAdditional(CompoundTag t, Provider p) {
    super.saveAdditional(t, p);
    t.putInt("AssemblyProgress", this.progress);
    t.putString("AssemblyRecipe", this.active);
  }

  @Override
  protected void loadAdditional(CompoundTag t, Provider p) {
    super.loadAdditional(t, p);
    this.progress = Math.clamp(t.getInt("AssemblyProgress"), 0, 59);
    this.active = t.getString("AssemblyRecipe");
  }
}
