package com.timelocked;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public final class ReactorFuelEntity extends BaseContainerBlockEntity {
  private NonNullList<ItemStack> items = NonNullList.withSize(3, ItemStack.EMPTY);

  public ReactorFuelEntity(BlockPos p, BlockState s) {
    super((BlockEntityType) UndergroundContent.FUEL_ENTITY.get(), p, s);
  }

  public int getContainerSize() {
    return 3;
  }

  protected NonNullList<ItemStack> getItems() {
    return this.items;
  }

  protected void setItems(NonNullList<ItemStack> n) {
    this.items = n;
  }

  public boolean canPlaceItem(int slot, ItemStack stack) {
    return slot >= 0 && slot < 3 && stack.is((Item) Content.FUEL_CELL.get());
  }

  protected Component getDefaultName() {
    return Component.literal("Reactor integral fuel slots — A / B / C");
  }

  protected AbstractContainerMenu createMenu(int id, Inventory inv) {
    return new ReactorFuelMenu(id, inv, this);
  }

  private boolean channelLoaded(int i) {
    return this.getItem(i).is((Item) Content.FUEL_CELL.get()) && this.getItem(i).getCount() > 0;
  }

  public boolean allChannelsLoaded() {
    return ReactorFuelChannels.loaded(this::channelLoaded);
  }

  public boolean consumeStartupBatch() {
    if (!ReactorFuelChannels.consume(this::channelLoaded, i -> this.getItem(i).shrink(1)))
      return false;
    this.setChanged();
    return true;
  }

  protected void saveAdditional(CompoundTag t, Provider p) {
    super.saveAdditional(t, p);
    ContainerHelper.saveAllItems(t, this.items, p);
  }

  protected void loadAdditional(CompoundTag t, Provider p) {
    super.loadAdditional(t, p);
    this.items = NonNullList.withSize(3, ItemStack.EMPTY);
    ContainerHelper.loadAllItems(t, this.items, p);
  }
}
