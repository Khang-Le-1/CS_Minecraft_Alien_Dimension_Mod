package com.timelocked;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Read-only legacy admission history. Rejected old sites stay rejected, with no automatic backfill.
 */
public final class PowerStationState extends SavedData {
  private static final Factory<PowerStationState> FACTORY =
      new Factory<>(PowerStationState::new, PowerStationState::load);
  private final Map<Long, Boolean> decisions = new HashMap<>();

  public static PowerStationState get(ServerLevel level) {
    synchronized (level.getDataStorage()) {
      return level.getDataStorage().computeIfAbsent(FACTORY, "timelocked_power_stations_081");
    }
  }

  /**
   * Read-only legacy decision; new chunk-local features no longer make whole-footprint admissions.
   */
  public synchronized boolean wasDenied(BlockPos origin) {
    return Boolean.FALSE.equals(decisions.get(origin.asLong()));
  }

  public synchronized CompoundTag save(CompoundTag t, Provider p) {
    ListTag list = new ListTag();
    decisions.forEach(
        (pos, allow) -> {
          CompoundTag n = new CompoundTag();
          n.putLong("Origin", pos);
          n.putBoolean("Allow", allow);
          list.add(n);
        });
    t.put("Decisions", list);
    return t;
  }

  private static PowerStationState load(CompoundTag t, Provider p) {
    PowerStationState s = new PowerStationState();
    for (Tag entry : t.getList("Decisions", 10)) {
      CompoundTag n = (CompoundTag) entry;
      s.decisions.put(n.getLong("Origin"), n.getBoolean("Allow"));
    }
    return s;
  }
}
