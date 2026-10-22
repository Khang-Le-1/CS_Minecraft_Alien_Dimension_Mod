package com.timelocked;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class ReactorFuelMenu extends AbstractContainerMenu {
  private final Container fuel;

  public ReactorFuelMenu(int id, Inventory inv, Container fuel) {
    super((MenuType) UndergroundContent.FUEL_MENU.get(), id);
    checkContainerSize(fuel, 3);
    this.fuel = fuel;
    fuel.startOpen(inv.player);

    for (int i = 0; i < 3; i++) {
      this.addSlot(
          new Slot(fuel, i, 62 + i * 18, 35) {
            public boolean mayPlace(ItemStack s) {
              return s.is((Item) Content.FUEL_CELL.get());
            }
          });
    }

    for (int r = 0; r < 3; r++) {
      for (int c = 0; c < 9; c++) {
        this.addSlot(new Slot(inv, c + r * 9 + 9, 8 + c * 18, 84 + r * 18));
      }
    }

    for (int c = 0; c < 9; c++) {
      this.addSlot(new Slot(inv, c, 8 + c * 18, 142));
    }
  }

  public boolean stillValid(Player p) {
    return this.fuel.stillValid(p);
  }

  public void removed(Player p) {
    super.removed(p);
    this.fuel.stopOpen(p);
  }

  public ItemStack quickMoveStack(Player p, int index) {
    Slot s = (Slot) this.slots.get(index);
    if (!s.hasItem()) {
      return ItemStack.EMPTY;
    }

    ItemStack original = s.getItem();
    ItemStack copy = original.copy();
    if (index < 3) {
      if (!this.moveItemStackTo(original, 3, 39, true)) {
        return ItemStack.EMPTY;
      }
    } else if (!original.is((Item) Content.FUEL_CELL.get())
        || !this.moveItemStackTo(original, 0, 3, false)) {
      return ItemStack.EMPTY;
    }

    if (original.isEmpty()) {
      s.set(ItemStack.EMPTY);
    } else {
      s.setChanged();
    }

    s.onTake(p, original);
    return copy;
  }
}
