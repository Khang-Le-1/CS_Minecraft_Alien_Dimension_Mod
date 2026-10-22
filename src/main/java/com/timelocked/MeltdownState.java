package com.timelocked;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** Per-dimension recovery of staged excavation, avoiding a second ignition explosion on restart. */
public final class MeltdownState extends SavedData {
  private static final Factory<MeltdownState> FACTORY =
      new Factory<>(MeltdownState::new, MeltdownState::load);
  final List<PlantMeltdown.Blast> jobs = new ArrayList<>();

  public static MeltdownState get(ServerLevel l) {
    return l.getDataStorage().computeIfAbsent(FACTORY, "timelocked_meltdowns_085");
  }

  public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
    ListTag list = new ListTag();
    for (var b : jobs) {
      CompoundTag n = new CompoundTag();
      n.putString("Dimension", b.dim.location().toString());
      n.putLong("Core", b.c.asLong());
      n.putInt("Kind", b.kind);
      n.putInt("Radius", b.r);
      n.putLong("Start", b.start);
      n.putBoolean("Industrial", b.industrial);
      n.putInt("Surface", b.surface);
      n.putInt("Cursor", b.cursor.index());
      n.putBoolean("Fallout", b.falloutDone);
      n.putBoolean("Ignited", b.ignited);
      ListTag heard = new ListTag();
      for (var id : b.heard) heard.add(StringTag.valueOf(id.toString()));
      n.put("Heard", heard);
      list.add(n);
    }
    tag.put("Jobs", list);
    return tag;
  }

  private static MeltdownState load(CompoundTag tag, HolderLookup.Provider provider) {
    MeltdownState out = new MeltdownState();
    for (Tag v : tag.getList("Jobs", 10)) {
      CompoundTag n = (CompoundTag) v;
      try {
        int kind = n.getInt("Kind"), radius = n.getInt("Radius");
        if (kind < 0 || kind > 3 || radius < 1 || radius > 64) continue;
        var dim =
            ResourceKey.create(
                Registries.DIMENSION, ResourceLocation.parse(n.getString("Dimension")));
        var b =
            new PlantMeltdown.Blast(
                dim,
                BlockPos.of(n.getLong("Core")),
                kind,
                radius,
                n.getLong("Start"),
                n.getBoolean("Industrial"),
                n.getInt("Surface"));
        b.cursor.restore(n.getInt("Cursor"));
        b.falloutDone = n.getBoolean("Fallout");
        b.ignited = n.getBoolean("Ignited");
        for (Tag h : n.getList("Heard", 8))
          try {
            b.heard.add(UUID.fromString(h.getAsString()));
          } catch (IllegalArgumentException ignored) {
          }
        out.jobs.add(b);
      } catch (IllegalArgumentException ignored) {
      }
    }
    return out;
  }
}
