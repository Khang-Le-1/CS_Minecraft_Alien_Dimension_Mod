package com.timelocked;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;

/**
 * Records only placements actually made by the new landing builder. Legacy replicas are never
 * guessed.
 */
@EventBusSubscriber(modid = "timelocked")
public final class ShipLandingOwnership extends SavedData {
  private static final Factory<ShipLandingOwnership> FACTORY =
      new Factory<>(ShipLandingOwnership::new, ShipLandingOwnership::load);
  private final Map<String, Map<Long, BlockState>> cells = new HashMap<>();
  private final Map<String, Map<Long, CompoundTag>> tags = new HashMap<>();

  private Map<Long, CompoundTag> tags(ServerLevel l, String owner) {
    return tags.computeIfAbsent(l.dimension().location() + "|" + owner, k -> new HashMap<>());
  }

  public static ShipLandingOwnership get(ServerLevel l) {
    return l.getServer()
        .overworld()
        .getDataStorage()
        .computeIfAbsent(FACTORY, "timelocked_landing_ownership_082");
  }

  private Map<Long, BlockState> group(ServerLevel l, String owner) {
    return cells.computeIfAbsent(l.dimension().location() + "|" + owner, k -> new HashMap<>());
  }

  public boolean place(ServerLevel l, Map<BlockPos, BlockState> desired) {
    return place(l, desired, "landing");
  }

  public boolean place(ServerLevel l, Map<BlockPos, BlockState> desired, String owner) {
    // Validate the entire placement before the first write. No terrain clearing or non-air
    // replacement.
    for (var e : desired.entrySet()) {
      var actual = l.getBlockState(e.getKey());
      if (actual.equals(e.getValue())) continue;
      if (!actual.isAir()
          || !l.getFluidState(e.getKey()).isEmpty()
          || l.getBlockEntity(e.getKey()) != null) return false;
      if (!l.getEntities(
                  (net.minecraft.world.entity.Entity) null,
                  new net.minecraft.world.phys.AABB(e.getKey()),
                  x -> x.isAlive())
              .isEmpty()
          && !e.getValue().isAir()) return false;
    }
    var owned = group(l, owner);
    for (var e : desired.entrySet())
      if (!l.getBlockState(e.getKey()).equals(e.getValue())) {
        if (!l.setBlock(e.getKey(), e.getValue(), 3))
          throw new IllegalStateException("Landing placement failed at " + e.getKey());
        owned.put(e.getKey().asLong(), e.getValue());
        var be = l.getBlockEntity(e.getKey());
        if (be != null)
          tags(l, owner).put(e.getKey().asLong(), be.saveWithoutMetadata(l.registryAccess()));
      }
    setDirty();
    return true;
  }

  public int cleanup(ServerLevel l) {
    return cleanup(l, "landing");
  }

  public int cleanup(ServerLevel l, String owner) {
    var owned = group(l, owner);
    int changed = 0;
    for (var e : owned.entrySet()) {
      var p = BlockPos.of(e.getKey());
      var expectedTag = tags(l, owner).get(e.getKey());
      boolean nbtUnchanged =
          expectedTag == null
              || l.getBlockEntity(p) != null
                  && l.getBlockEntity(p)
                      .saveWithoutMetadata(l.registryAccess())
                      .equals(expectedTag);
      if (l.getBlockState(p).equals(e.getValue())
          && !(l.getBlockEntity(p) instanceof Container)
          && nbtUnchanged) {
        l.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
        changed++;
      }
    }
    owned.clear();
    tags(l, owner).clear();
    setDirty();
    return changed;
  }

  public void invalidate(ServerLevel l, BlockPos pos) {
    String prefix = l.dimension().location() + "|";
    cells.forEach(
        (k, map) -> {
          if (k.startsWith(prefix) && map.remove(pos.asLong()) != null) setDirty();
        });
  }

  @SubscribeEvent
  public static void placed(BlockEvent.EntityPlaceEvent e) {
    if (e.getLevel() instanceof ServerLevel l) get(l).invalidate(l, e.getPos());
  }

  @SubscribeEvent
  public static void broken(BlockEvent.BreakEvent e) {
    if (e.getLevel() instanceof ServerLevel l) get(l).invalidate(l, e.getPos());
  }

  @SubscribeEvent
  public static void exploded(ExplosionEvent.Detonate e) {
    if (e.getLevel() instanceof ServerLevel l)
      for (BlockPos p : e.getAffectedBlocks()) get(l).invalidate(l, p);
  }

  @SubscribeEvent
  public static void piston(net.neoforged.neoforge.event.level.PistonEvent.Pre e) {
    if (e.getLevel() instanceof ServerLevel l) {
      var helper = e.getStructureHelper();
      if (helper != null) {
        helper.resolve();
        for (BlockPos p : helper.getToPush()) {
          get(l).invalidate(l, p);
          get(l).invalidate(l, p.relative(e.getDirection()));
          get(l).invalidate(l, p.relative(e.getDirection().getOpposite()));
        }
        for (BlockPos p : helper.getToDestroy()) get(l).invalidate(l, p);
      }
    }
  }

  public CompoundTag save(CompoundTag t, Provider p) {
    ListTag list = new ListTag();
    cells.forEach(
        (dim, map) ->
            map.forEach(
                (pos, state) -> {
                  CompoundTag n = new CompoundTag();
                  n.putString("Group", dim);
                  n.putLong("Pos", pos);
                  n.put("After", NbtUtils.writeBlockState(state));
                  if (tags.getOrDefault(dim, Map.of()).containsKey(pos))
                    n.put("BlockEntityAfter", tags.get(dim).get(pos));
                  list.add(n);
                }));
    t.put("Cells", list);
    return t;
  }

  private static ShipLandingOwnership load(CompoundTag t, Provider p) {
    var data = new ShipLandingOwnership();
    for (Tag tag : t.getList("Cells", 10)) {
      var n = (CompoundTag) tag;
      data.cells
          .computeIfAbsent(n.getString("Group"), k -> new HashMap<>())
          .put(
              n.getLong("Pos"),
              NbtUtils.readBlockState(p.lookupOrThrow(Registries.BLOCK), n.getCompound("After")));
      if (n.contains("BlockEntityAfter", 10))
        data.tags
            .computeIfAbsent(n.getString("Group"), k -> new HashMap<>())
            .put(n.getLong("Pos"), n.getCompound("BlockEntityAfter"));
    }
    return data;
  }
}
