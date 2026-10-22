package com.timelocked;

import java.util.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** Server-owned per-player story state. Offline players do not advance. */
public final class IntroState extends SavedData {
  static final Factory<IntroState> FACTORY = new Factory<>(IntroState::new, IntroState::load);
  public boolean enabled = true;
  int nextSlot;
  final Map<UUID, CompoundTag> players = new HashMap<>();

  static IntroState get(MinecraftServer s) {
    return s.overworld().getDataStorage().computeIfAbsent(FACTORY, "timelocked_intro");
  }

  static IntroState load(CompoundTag tag, HolderLookup.Provider registries) {
    IntroState state = new IntroState();
    state.enabled = !tag.contains("Enabled") || tag.getBoolean("Enabled");
    state.nextSlot = Math.max(0, tag.getInt("NextSlot"));
    for (Tag item : tag.getList("Players", Tag.TAG_COMPOUND)) {
      CompoundTag p = (CompoundTag) item;
      if (p.hasUUID("Player")) state.players.put(p.getUUID("Player"), p.copy());
    }
    return state;
  }

  @Override
  public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
    tag.putBoolean("Enabled", enabled);
    tag.putInt("NextSlot", nextSlot);
    ListTag list = new ListTag();
    players.values().forEach(p -> list.add(p.copy()));
    tag.put("Players", list);
    return tag;
  }
}
