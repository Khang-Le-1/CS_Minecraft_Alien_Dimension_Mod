package com.timelocked;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Backup-first, explicitly confirmed visual patches. No facility/road regeneration or progression
 * reset.
 */
@EventBusSubscriber(modid = "timelocked")
public final class VisualRefit083 {
  private record Change(BlockPos pos, BlockState before, BlockState after) {}

  @SubscribeEvent
  public static void commands(RegisterCommandsEvent e) {
    e.getDispatcher()
        .register(
            Commands.literal("sarcvisuals")
                .requires(s -> s.hasPermission(2))
                .executes(
                    c -> {
                      c.getSource()
                          .sendFailure(
                              Component.literal(
                                  "BACK UP YOUR WORLD FIRST. Stand inside a registered underground site and use /sarcvisuals confirm for reactor/tram fixture patches, or on its main approach road and use /sarcvisuals road-lights confirm. Changed blocks, block entities and occupied cells are protected. No facility rebuild."));
                      return 0;
                    })
                .then(
                    Commands.literal("confirm")
                        .executes(c -> facility(c.getSource().getPlayerOrException())))
                .then(
                    Commands.literal("road-lights")
                        .then(
                            Commands.literal("confirm")
                                .executes(c -> road(c.getSource().getPlayerOrException())))));
  }

  private static int facility(net.minecraft.server.level.ServerPlayer p) {
    ServerLevel l = p.serverLevel();
    if (!l.dimension().equals(Level.OVERWORLD))
      return fail(p, "Use this inside a registered Overworld underground facility.");
    var d = FacilityState.get(p.server);
    if (d.pending) return fail(p, "Wait for the active facility placement to finish.");
    List<BlockPos> origins = new ArrayList<>();
    for (long key : d.sites.keySet()) {
      BlockPos o = BlockPos.of(key), q = p.blockPosition().subtract(o);
      var progress = d.progress.get(key);
      if (progress != null
          && progress.layoutVersion >= 2
          && q.getX() >= 0
          && q.getX() < SiteLayout.WIDTH
          && q.getY() >= 0
          && q.getY() < SiteLayout.HEIGHT
          && q.getZ() >= 0
          && q.getZ() < SiteLayout.DEPTH) origins.add(o);
    }
    if (origins.size() != 1)
      return fail(p, "Stand inside one uniquely identified, completed underground facility.");
    if (BlueprintRooms.active(l, origins.getFirst()))
      return fail(p, "Legacy visual patches do not apply to blueprint facilities.");
    List<Change> safe = new ArrayList<>();
    int protectedCells = 0;
    try (var in =
        VisualRefit083.class.getResourceAsStream(
            "/data/timelocked/facility/visual-patches-083.json")) {
      var json =
          JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8))
              .getAsJsonObject();
      for (var value : json.getAsJsonArray("patches")) {
        var v = value.getAsJsonObject();
        var a = v.getAsJsonArray("pos");
        BlockPos at =
            origins
                .getFirst()
                .offset(a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsInt());
        var before = state(v.getAsJsonObject("before"));
        var after = state(v.getAsJsonObject("after"));
        if (!l.hasChunkAt(at)) {
          protectedCells++;
          continue;
        }
        var actual = l.getBlockState(at);
        if (actual.equals(after)) continue;
        boolean tall = after.is(FacilityDetails.WORKSTATION.get());
        if (!actual.equals(before)
            || l.getBlockEntity(at) != null
            || !l.getFluidState(at).isEmpty()
            || tall
                && (!l.getBlockState(at.above()).isAir() || !l.getFluidState(at.above()).isEmpty())
            || !l.getEntities(
                    (net.minecraft.world.entity.Entity) null,
                    new AABB(at).inflate(0, .4, 0),
                    x -> x.isAlive())
                .isEmpty()) {
          protectedCells++;
          continue;
        }
        safe.add(new Change(at, before, after));
      }
    } catch (Exception error) {
      return fail(p, "Visual patch could not be read. No blocks changed.");
    }
    return apply(p, l, safe, protectedCells, "facility visual refit");
  }

  private static int road(net.minecraft.server.level.ServerPlayer p) {
    ServerLevel l = p.serverLevel();
    if (!l.dimension().equals(Level.OVERWORLD))
      return fail(p, "Use this on a registered facility's main Overworld approach road.");
    List<BlockPos> origins = new ArrayList<>();
    for (long key : FacilityState.get(p.server).sites.keySet()) {
      BlockPos o = BlockPos.of(key);
      int z = p.getBlockZ() - o.getZ();
      if (Math.abs(p.getBlockX() - (o.getX() + 64)) <= 5 && z >= 256 && z <= 2188) origins.add(o);
    }
    if (origins.size() != 1)
      return fail(p, "Stand on one uniquely identified registered main approach road.");
    BlockPos o = origins.getFirst();
    int x = o.getX() + 64;
    List<Change> safe = new ArrayList<>();
    int skipped = 0;
    for (int z = Math.max(o.getZ() + 256, p.getBlockZ() - 64);
        z <= Math.min(o.getZ() + 2188, p.getBlockZ() + 64);
        z++) {
      BlockPos center = null;
      for (int distance = 0; distance <= 24 && center == null; distance++)
        for (int sign : new int[] {-1, 1}) {
          BlockPos at = new BlockPos(x, p.getBlockY() - 1 + distance * sign, z);
          if (!l.hasChunkAt(at)) continue;
          var b = l.getBlockState(at);
          var under = l.getBlockState(at.below());
          if ((b.is(Blocks.GRAY_CONCRETE)
                  || b.is(Blocks.YELLOW_CONCRETE)
                  || b.is(Blocks.ANDESITE)
                  || b.is(Blocks.SMOOTH_STONE_SLAB))
              && (under.is(Blocks.COARSE_DIRT) || under.is(Blocks.STONE_BRICKS))
              && l.getBlockState(at.above()).isAir()
              && l.getBlockState(at.above(2)).isAir()) center = at;
        }
      if (center == null) {
        skipped++;
        continue;
      }
      var roof = l.getBlockState(center.above(6));
      boolean tunnel =
          (roof.is(Blocks.STONE_BRICKS) || roof.is(Blocks.CRACKED_STONE_BRICKS))
              && l.getBlockState(center.offset(4, 2, 0)).is(Blocks.STONE_BRICKS)
              && l.getBlockState(center.offset(-4, 2, 0)).is(Blocks.STONE_BRICKS);
      if (tunnel) {
        if (Math.floorMod(z, 6) == 0)
          safe.add(new Change(center.above(6), roof, Blocks.SEA_LANTERN.defaultBlockState()));
        continue;
      }
      for (int side : new int[] {-1, 1})
        if (Math.floorMod(z, 12) == (side < 0 ? 0 : 6)) {
          BlockPos at = center.offset(side * 4, 0, 0);
          List<Change> fixture = new ArrayList<>();
          boolean clear = true;
          if (!l.hasChunkAt(at) || l.getBlockEntity(at) != null) {
            skipped++;
            continue;
          }
          if (l.getBlockState(at).isAir())
            fixture.add(
                new Change(at, l.getBlockState(at), Blocks.STONE_BRICKS.defaultBlockState()));
          for (int h = 1; h <= 5; h++) {
            var q = at.above(h);
            var actual = l.getBlockState(q);
            if (h == 1 && actual.is(Blocks.STONE_BRICK_WALL)) continue;
            if (!actual.isAir()
                || !l.getFluidState(q).isEmpty()
                || l.getBlockEntity(q) != null
                || !l.getEntities(
                        (net.minecraft.world.entity.Entity) null, new AABB(q), ent -> ent.isAlive())
                    .isEmpty()) {
              clear = false;
              break;
            }
            fixture.add(
                new Change(
                    q,
                    actual,
                    (h <= 3
                            ? Blocks.IRON_BARS
                            : h == 4 ? Blocks.SEA_LANTERN : Blocks.STONE_BRICK_SLAB)
                        .defaultBlockState()));
          }
          if (clear) safe.addAll(fixture);
          else skipped++;
        }
    }
    return apply(p, l, safe, skipped, "road lighting patch (128-block local span)");
  }

  private static int apply(
      net.minecraft.server.level.ServerPlayer p,
      ServerLevel l,
      List<Change> changes,
      int skipped,
      String label) {
    if (changes.isEmpty()) {
      p.sendSystemMessage(
          Component.literal(
              "[VISUALS] No eligible changes. Customized, occupied or unloaded cells: " + skipped));
      return 0;
    }
    try {
      var dir = p.server.getWorldPath(LevelResource.ROOT).resolve("timelocked-backups");
      Files.createDirectories(dir);
      CompoundTag root = new CompoundTag();
      root.putString("Dimension", l.dimension().location().toString());
      root.putString("Purpose", label);
      ListTag cells = new ListTag();
      for (var c : changes) {
        CompoundTag n = new CompoundTag();
        n.putLong("Pos", c.pos.asLong());
        n.put("Before", NbtUtils.writeBlockState(l.getBlockState(c.pos)));
        cells.add(n);
      }
      root.put("Cells", cells);
      root.put(
          "FacilityData", FacilityState.get(p.server).save(new CompoundTag(), l.registryAccess()));
      NbtIo.writeCompressed(root, dir.resolve("visuals-083-" + System.nanoTime() + ".nbt"));
    } catch (Exception e) {
      return fail(p, "Snapshot failed. No blocks changed; check disk permissions.");
    }
    int changed = 0;
    for (var c : changes)
      if (l.getBlockState(c.pos).equals(c.before) && l.getBlockEntity(c.pos) == null) {
        if (l.setBlock(c.pos, c.after, 3)) changed++;
      }
    p.sendSystemMessage(
        Component.literal(
            "[VISUALS] "
                + label
                + ": "
                + changed
                + " cells updated; "
                + skipped
                + " customized, occupied or unloaded cells skipped. Local snapshot saved under timelocked-backups; this is not a full-world backup."));
    return changed;
  }

  private static int fail(net.minecraft.server.level.ServerPlayer p, String s) {
    p.sendSystemMessage(Component.literal("[VISUALS] " + s));
    return 0;
  }

  private static BlockState state(JsonObject json) {
    var id = ResourceLocation.parse(json.get("Name").getAsString());
    if (!BuiltInRegistries.BLOCK.containsKey(id))
      throw new IllegalArgumentException("Unknown patch block");
    var state = BuiltInRegistries.BLOCK.get(id).defaultBlockState();
    for (var e : json.getAsJsonObject("Properties").entrySet()) {
      var property = state.getBlock().getStateDefinition().getProperty(e.getKey());
      if (property == null) throw new IllegalArgumentException("Unknown patch property");
      state = value(state, property, e.getValue().getAsString());
    }
    return state;
  }

  private static <T extends Comparable<T>> BlockState value(BlockState s, Property<T> p, String v) {
    return s.setValue(p, p.getValue(v).orElseThrow());
  }

  private VisualRefit083() {}
}
