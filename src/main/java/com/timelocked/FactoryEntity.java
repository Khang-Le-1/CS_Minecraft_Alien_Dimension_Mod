package com.timelocked;

import java.util.Map.Entry;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public final class FactoryEntity extends MachineEntity {
  public int progress;
  private String active = "";
  public final PowerBuffer receiver = new PowerBuffer(64000, true, false, this::setChanged);

  public FactoryEntity(BlockPos p, BlockState s) {
    super((BlockEntityType<?>) MetalContent.FACTORY_ENTITY.get(), p, s);
  }

  public boolean press() {
    return this.getBlockState().is((Block) MetalContent.PLATE_PRESS.get());
  }

  public int duration() {
    return this.press() ? 100 : 160;
  }

  public int cost() {
    return this.press() ? 20 : 40;
  }

  @Override
  public boolean accepts(int slot, ItemStack s) {
    return slot == 0
        ? MetalContent.INGOTS.values().stream().anyMatch(i -> s.is(i.get()))
        : !this.press()
            && slot == 1
            && MetalContent.INGOTS.values().stream().anyMatch(i -> s.is(i.get()));
  }

  protected Component getDefaultName() {
    return Component.literal(
        this.press()
            ? "Plate press: input top-left / output top-right"
            : "Alloy smelter: inputs top-left + top-middle / output top-right");
  }

  @Override
  public String status() {
    return (this.press() ? "Press" : "Alloy smelter")
        + " | "
        + this.receiver.getEnergyStored()
        + " FE | "
        + this.progress
        + " / "
        + this.duration()
        + " ticks | inputs 0"
        + (this.press() ? "" : " + 1")
        + ", output 2";
  }

  private FactoryEntity.Batch pair(
      String left, int a, String right, int b, String output, int count) {
    ItemStack x = this.getItem(0);
    ItemStack y = this.getItem(1);
    if (x.is(MetalContent.ingot(left))
        && x.getCount() >= a
        && y.is(MetalContent.ingot(right))
        && y.getCount() >= b) {
      return new FactoryEntity.Batch(output, MetalContent.ingot(output), count, a, b);
    } else {
      return x.is(MetalContent.ingot(right))
              && x.getCount() >= b
              && y.is(MetalContent.ingot(left))
              && y.getCount() >= a
          ? new FactoryEntity.Batch(output, MetalContent.ingot(output), count, b, a)
          : null;
    }
  }

  public FactoryEntity.Batch batch() {
    if (this.press()) {
      for (Entry<String, Supplier<Item>> e : MetalContent.INGOTS.entrySet()) {
        if (this.getItem(0).is(e.getValue().get())) {
          return new FactoryEntity.Batch(e.getKey(), MetalContent.plate(e.getKey()), 1, 1, 0);
        }
      }

      return null;
    } else {
      FactoryEntity.Batch b = this.pair("copper", 3, "tin", 1, "bronze", 4);
      if (b == null) {
        b = this.pair("copper", 3, "zinc", 1, "brass", 4);
      }

      if (b == null) {
        b = this.pair("steel", 1, "tungsten", 1, "tungsten_steel", 2);
      }

      if (b == null) {
        b = this.pair("steel", 2, "necronium", 1, "voidsteel", 3);
      }

      return b;
    }
  }

  public void tick() {
    FactoryEntity.Batch b = this.batch();
    ItemStack out = this.getItem(2);
    boolean valid =
        b != null
            && (out.isEmpty()
                || ItemStack.isSameItemSameComponents(out, new ItemStack(b.out()))
                    && out.getCount() + b.count() <= out.getMaxStackSize());
    if (valid) {
      if (!this.active.equals(b.key())) {
        this.active = b.key();
        this.progress = 0;
        this.setChanged();
      }

      if (this.receiver.consume(this.cost())) {
        if (++this.progress >= this.duration()) {
          this.getItem(0).shrink(b.a());
          if (b.b() > 0) {
            this.getItem(1).shrink(b.b());
          }

          if (out.isEmpty()) {
            this.setItem(2, new ItemStack(b.out(), b.count()));
          } else {
            out.grow(b.count());
          }

          this.progress = 0;
          this.active = "";
        }

        this.setChanged();
      }

      this.report();
    } else {
      if (this.progress != 0 || !this.active.isEmpty()) {
        this.progress = 0;
        this.active = "";
        this.setChanged();
      }

      this.report();
    }
  }

  @Override
  protected void saveAdditional(CompoundTag t, Provider p) {
    super.saveAdditional(t, p);
    t.putInt("Energy", this.receiver.getEnergyStored());
    t.putInt("Progress", this.progress);
    t.putString("ActiveRecipe", this.active);
  }

  @Override
  protected void loadAdditional(CompoundTag t, Provider p) {
    super.loadAdditional(t, p);
    this.receiver.restore(t.getInt("Energy"));
    this.progress = Math.clamp(t.getInt("Progress"), 0, this.duration() - 1);
    this.active = t.getString("ActiveRecipe");
  }

  public record Batch(String key, Item out, int count, int a, int b) {}
}
