package com.timelocked;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class MachineMenu extends AbstractContainerMenu {
  public final MachineEntity machine;

  public MachineMenu(int id, Inventory inventory, final MachineEntity machine) {
    super(MenuType.GENERIC_3x3, id);
    this.machine = machine;

    for (int row = 0; row < 3; row++) {
      for (int col = 0; col < 3; col++) {
        final int s = col + row * 3;
        this.addSlot(
            new Slot(machine, s, 62 + col * 18, 17 + row * 18) {
              public boolean mayPlace(ItemStack item) {
                return machine.accepts(s, item);
              }
            });
      }
    }

    for (int row = 0; row < 3; row++) {
      for (int col = 0; col < 9; col++) {
        this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
      }
    }

    for (int col = 0; col < 9; col++) {
      this.addSlot(new Slot(inventory, col, 8 + col * 18, 142));
    }
  }

  public boolean stillValid(Player p) {
    return this.machine.stillValid(p);
  }

  public ItemStack quickMoveStack(Player p, int i) {
    Slot slot = (Slot) this.slots.get(i);
    if (!slot.hasItem()) {
      return ItemStack.EMPTY;
    }

    ItemStack original = slot.getItem();
    ItemStack copy = original.copy();
    if (i < 9) {
      if (!this.moveItemStackTo(original, 9, 45, true)) {
        return ItemStack.EMPTY;
      }
    } else if (!this.moveItemStackTo(original, 0, 9, false)) {
      return ItemStack.EMPTY;
    }

    if (original.isEmpty()) {
      slot.set(ItemStack.EMPTY);
    } else {
      slot.setChanged();
    }

    slot.onTake(p, original);
    return copy;
  }
}
