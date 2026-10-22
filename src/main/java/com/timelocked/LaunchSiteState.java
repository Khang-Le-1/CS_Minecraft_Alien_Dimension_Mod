package com.timelocked;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.saveddata.SavedData;

/** Persistent, fail-closed admission. Never schedules regeneration of an existing chunk. */
public final class LaunchSiteState extends SavedData {
  private static final Factory<LaunchSiteState> FACTORY =
      new Factory<>(LaunchSiteState::new, LaunchSiteState::load);
  private final Map<Long, Boolean> decisions = new HashMap<>();

  public static LaunchSiteState get(ServerLevel level) {
    synchronized (level.getDataStorage()) {
      return level.getDataStorage().computeIfAbsent(FACTORY, "timelocked_launch_sites_082");
    }
  }

  public synchronized boolean admit(ServerLevel level, BlockPos origin) {
    long key = origin.asLong();
    if (decisions.containsKey(key)) return decisions.get(key);
    boolean fresh = true;
    try {
      var b = new PowerStationGeometry.Bounds(-48, 48, -56, 56);
      outer:
      for (int x = Math.floorDiv(origin.getX() + b.x0(), 16);
          x <= Math.floorDiv(origin.getX() + b.x1(), 16);
          x++) {
        for (int z = Math.floorDiv(origin.getZ() + b.z0(), 16);
            z <= Math.floorDiv(origin.getZ() + b.z1(), 16);
            z++) {
          if (level.getChunkSource().getChunkNow(x, z) != null) {
            fresh = false;
            break outer;
          }
          var tag = level.getChunkSource().chunkMap.read(new ChunkPos(x, z)).join();
          if (tag.isPresent() && !unfinished(tag.get())) {
            fresh = false;
            break outer;
          }
        }
      }
    } catch (Exception e) {
      fresh = false;
    }
    decisions.put(key, fresh);
    setDirty();
    return fresh;
  }

  static boolean unfinished(CompoundTag root) {
    CompoundTag t = root.contains("Level", 10) ? root.getCompound("Level") : root;
    ChunkStatus status = ChunkStatus.byName(t.getString("Status"));
    return status != null && !status.isOrAfter(ChunkStatus.FEATURES);
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

  private static LaunchSiteState load(CompoundTag t, Provider p) {
    LaunchSiteState s = new LaunchSiteState();
    for (Tag entry : t.getList("Decisions", 10)) {
      CompoundTag n = (CompoundTag) entry;
      s.decisions.put(n.getLong("Origin"), n.getBoolean("Allow"));
    }
    return s;
  }
}
