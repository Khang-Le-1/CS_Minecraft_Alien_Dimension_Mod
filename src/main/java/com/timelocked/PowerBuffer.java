package com.timelocked;

import net.neoforged.neoforge.energy.IEnergyStorage;

public final class PowerBuffer implements IEnergyStorage {
  private int energy;
  private final int capacity;
  private final boolean receive;
  private final boolean extract;
  private final Runnable changed;

  public PowerBuffer(int capacity, boolean receive, boolean extract, Runnable changed) {
    this.capacity = capacity;
    this.receive = receive;
    this.extract = extract;
    this.changed = changed;
  }

  public int receiveEnergy(int n, boolean simulate) {
    int v = this.receive ? Math.min(Math.max(0, n), this.capacity - this.energy) : 0;
    if (!simulate && v > 0) {
      this.energy += v;
      this.changed.run();
    }

    return v;
  }

  public int extractEnergy(int n, boolean simulate) {
    int v = this.extract ? Math.min(Math.max(0, n), this.energy) : 0;
    if (!simulate && v > 0) {
      this.energy -= v;
      this.changed.run();
    }

    return v;
  }

  public int getEnergyStored() {
    return this.energy;
  }

  public int getMaxEnergyStored() {
    return this.capacity;
  }

  public boolean canExtract() {
    return this.extract;
  }

  public boolean canReceive() {
    return this.receive;
  }

  public boolean consume(int n) {
    if (n >= 0 && this.energy >= n) {
      this.energy -= n;
      this.changed.run();
      return true;
    } else {
      return false;
    }
  }

  public void generate(int n) {
    int v = Math.min(this.capacity, this.energy + Math.max(0, n));
    if (v != this.energy) {
      this.energy = v;
      this.changed.run();
    }
  }

  public void restore(int n) {
    this.energy = Math.clamp(n, 0, this.capacity);
  }
}
