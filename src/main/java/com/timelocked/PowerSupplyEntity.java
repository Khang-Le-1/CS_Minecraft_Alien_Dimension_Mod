package com.timelocked;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;

public final class PowerSupplyEntity extends MachineEntity {
  public int burnTicks;
  public final PowerBuffer output = new PowerBuffer(128000, false, true, this::setChanged);

  public PowerSupplyEntity(BlockPos p, BlockState s) {
    super((BlockEntityType<?>) Content.POWER_ENTITY.get(), p, s);
  }

  @Override
  public boolean accepts(int slot, ItemStack stack) {
    return slot == 0 && stack.is(Items.REDSTONE);
  }

  protected Component getDefaultName() {
    return Component.literal("Power: redstone upper-left");
  }

  @Override
  public String status() {
    return "Power supply "
        + this.output.getEnergyStored()
        + " / 128000 FE | fuel ticks "
        + this.burnTicks
        + " | 1 redstone = 32000 FE";
  }

  public void tick() {
    if (this.burnTicks == 0
        && this.getItem(0).is(Items.REDSTONE)
        && this.output.getEnergyStored() <= this.output.getMaxEnergyStored() - 32000) {
      this.getItem(0).shrink(1);
      this.burnTicks = 160;
      this.setChanged();
    }

    if (this.burnTicks > 0
        && this.output.getEnergyStored() <= this.output.getMaxEnergyStored() - 200) {
      this.output.generate(200);
      this.burnTicks--;
      this.setChanged();
    }

    this.distribute();
    this.report();
  }

  private void distribute() {
    if (this.level != null && this.output.getEnergyStored() != 0) {
      ArrayDeque<BlockPos> queue = new ArrayDeque<>();
      Set<BlockPos> visited = new HashSet<>();
      queue.add(this.worldPosition);
      visited.add(this.worldPosition);
      int budget = 200;

      while (!queue.isEmpty() && visited.size() <= 512 && budget > 0) {
        BlockPos at = queue.remove();

        for (Direction dir : Direction.values()) {
          BlockPos n = at.relative(dir);
          if (visited.add(n) && this.level.hasChunkAt(n)) {
            if (this.level.getBlockState(n).is((Block) Content.CONDUIT.get())) {
              queue.add(n);
            } else {
              IEnergyStorage receiver =
                  (IEnergyStorage)
                      this.level.getCapability(EnergyStorage.BLOCK, n, dir.getOpposite());
              if (receiver != null && receiver.canReceive()) {
                int sent =
                    receiver.receiveEnergy(Math.min(budget, this.output.getEnergyStored()), false);
                this.output.consume(sent);
                budget -= sent;
              }
            }
          }
        }
      }
    }
  }

  @Override
  protected void saveAdditional(CompoundTag t, Provider p) {
    super.saveAdditional(t, p);
    t.putInt("Energy", this.output.getEnergyStored());
    t.putInt("BurnTicks", this.burnTicks);
  }

  @Override
  protected void loadAdditional(CompoundTag t, Provider p) {
    super.loadAdditional(t, p);
    this.output.restore(t.getInt("Energy"));
    this.burnTicks = Math.clamp(t.getInt("BurnTicks"), 0, 160);
  }
}
