package com.timelocked;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public final class ArcFurnaceEntity extends MachineEntity {
  public static final int DURATION = 200;
  public static final int COST_PER_TICK = 40;
  public int progress;
  public final PowerBuffer receiver = new PowerBuffer(64000, true, false, this::setChanged);

  public ArcFurnaceEntity(BlockPos p, BlockState s) {
    super((BlockEntityType<?>) Content.EAF_ENTITY.get(), p, s);
  }

  @Override
  public boolean accepts(int slot, ItemStack stack) {
    return slot == 0 && (stack.is(Items.IRON_INGOT) || stack.is((Item) Content.CAST_IRON.get()));
  }

  protected Component getDefaultName() {
    return Component.literal("EAF: iron left / steel right");
  }

  @Override
  public String status() {
    return "EAF "
        + this.receiver.getEnergyStored()
        + " / 64000 FE | "
        + this.progress
        + " / 200 ticks | upper-left input, upper-right output";
  }

  public void tick() {
    ItemStack in = this.getItem(0);
    ItemStack out = this.getItem(2);
    boolean valid =
        this.accepts(0, in)
            && (out.isEmpty()
                || out.is((Item) Content.STEEL.get()) && out.getCount() < out.getMaxStackSize());
    if (!valid) {
      if (this.progress != 0) {
        this.progress = 0;
        this.setChanged();
      }
    } else if (this.receiver.consume(40)) {
      if (++this.progress >= 200) {
        in.shrink(1);
        if (out.isEmpty()) {
          this.setItem(2, new ItemStack((ItemLike) Content.STEEL.get()));
        } else {
          out.grow(1);
        }

        this.progress = 0;
      }

      this.setChanged();
    }

    this.report();
  }

  @Override
  protected void saveAdditional(CompoundTag t, Provider p) {
    super.saveAdditional(t, p);
    t.putInt("Energy", this.receiver.getEnergyStored());
    t.putInt("Progress", this.progress);
  }

  @Override
  protected void loadAdditional(CompoundTag t, Provider p) {
    super.loadAdditional(t, p);
    this.receiver.restore(t.getInt("Energy"));
    this.progress = Math.clamp(t.getInt("Progress"), 0, 199);
  }
}
