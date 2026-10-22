package com.timelocked;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;

public abstract class MachineEntity extends BaseContainerBlockEntity {
  private NonNullList<ItemStack> inventory = NonNullList.withSize(9, ItemStack.EMPTY);
  public final IItemHandler itemsCapability =
      new IItemHandler() {
        public int getSlots() {
          return 9;
        }

        public ItemStack getStackInSlot(int s) {
          return MachineEntity.this.getItem(s);
        }

        public int getSlotLimit(int s) {
          return 64;
        }

        public boolean isItemValid(int s, ItemStack stack) {
          return MachineEntity.this.accepts(s, stack);
        }

        public ItemStack insertItem(int s, ItemStack stack, boolean simulate) {
          if (!stack.isEmpty() && MachineEntity.this.accepts(s, stack)) {
            ItemStack current = MachineEntity.this.getItem(s);
            if (!current.isEmpty() && !ItemStack.isSameItemSameComponents(current, stack)) {
              return stack;
            }

            int n =
                Math.min(
                    stack.getCount(), Math.min(64, stack.getMaxStackSize()) - current.getCount());
            if (n <= 0) {
              return stack;
            }

            if (!simulate) {
              if (current.isEmpty()) {
                MachineEntity.this.setItem(s, stack.copyWithCount(n));
              } else {
                current.grow(n);
                MachineEntity.this.setChanged();
              }
            }

            return n == stack.getCount()
                ? ItemStack.EMPTY
                : stack.copyWithCount(stack.getCount() - n);
          } else {
            return stack;
          }
        }

        public ItemStack extractItem(int s, int n, boolean simulate) {
          ItemStack stack = MachineEntity.this.getItem(s);
          if (!stack.isEmpty() && n > 0) {
            int count = Math.min(n, stack.getCount());
            return simulate ? stack.copyWithCount(count) : MachineEntity.this.removeItem(s, count);
          } else {
            return ItemStack.EMPTY;
          }
        }
      };

  protected MachineEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
    super(type, pos, state);
  }

  public int getContainerSize() {
    return 9;
  }

  protected NonNullList<ItemStack> getItems() {
    return this.inventory;
  }

  protected void setItems(NonNullList<ItemStack> items) {
    this.inventory = items;
  }

  protected AbstractContainerMenu createMenu(int id, Inventory inv) {
    return new MachineMenu(id, inv, this);
  }

  public abstract boolean accepts(int var1, ItemStack var2);

  public boolean canPlaceItem(int slot, ItemStack stack) {
    return this.accepts(slot, stack);
  }

  public abstract String status();

  protected void report() {
    if (this.level != null && this.level.getGameTime() % 20L == 0L) {
      for (Player p : this.level.players()) {
        if (p.containerMenu instanceof MachineMenu menu && menu.machine == this) {
          p.displayClientMessage(Component.literal(this.status()), true);
        }
      }
    }
  }

  protected void saveAdditional(CompoundTag tag, Provider provider) {
    super.saveAdditional(tag, provider);
    ContainerHelper.saveAllItems(tag, this.inventory, provider);
  }

  protected void loadAdditional(CompoundTag tag, Provider provider) {
    super.loadAdditional(tag, provider);
    this.inventory = NonNullList.withSize(9, ItemStack.EMPTY);
    ContainerHelper.loadAllItems(tag, this.inventory, provider);
  }
}
