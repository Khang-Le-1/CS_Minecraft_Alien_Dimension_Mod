package com.timelocked;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public final class IndustrialCrateEntity extends MachineEntity {
  public IndustrialCrateEntity(BlockPos p, BlockState s) {
    super((BlockEntityType<?>) MetalContent.CRATE_ENTITY.get(), p, s);
  }

  @Override
  public boolean accepts(int slot, ItemStack s) {
    return slot >= 0 && slot < 9;
  }

  protected Component getDefaultName() {
    return Component.literal("Industrial supply crate — 9 slots");
  }

  @Override
  public String status() {
    return "Industrial supply crate | 9 inventory slots";
  }
}
