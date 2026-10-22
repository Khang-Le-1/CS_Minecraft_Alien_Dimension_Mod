package com.timelocked;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Stops unplanned mob spawning in registered SARC volumes, independently of light or Story mode.
 * Existing mobs and entities walking in are deliberately not deleted. This is a spawn policy, not
 * an entity-join filter: saved scientists, encounter waves, vehicles and pets must keep loading.
 */
@EventBusSubscriber(modid = "timelocked")
public final class FacilitySpawnProtection {
  // Only our explicit meltdown spawn path sets this before finalization. Do not allow every EVENT:
  // raids and other vanilla events must not become a loophole in the facility's protection.
  static final String SCRIPTED_SPAWN_KEY = "timelocked_scripted_facility_spawn";

  // World-generation workers must not load SavedData or iterate its mutable site maps.
  // The server publishes immutable volumes once per tick; weak keys do not retain closed worlds.
  private static final Map<MinecraftServer, List<Volume>> SNAPSHOTS =
      Collections.synchronizedMap(new WeakHashMap<>());

  private record Volume(BlockPos origin, int width, int depth) {
    boolean contains(BlockPos position) {
      BlockPos local = position.subtract(origin);
      return local.getX() >= 0
          && local.getX() < width
          && local.getY() >= 0
          && local.getY() < BlueprintRooms.HEIGHT
          && local.getZ() >= 0
          && local.getZ() < depth;
    }
  }

  private static List<Volume> snapshot(ServerLevel level) {
    List<Volume> volumes = new ArrayList<>();
    for (long packed : FacilityState.get(level.getServer()).sites.keySet()) {
      BlockPos origin = BlockPos.of(packed);
      volumes.add(
          new Volume(
              origin, BlueprintRooms.width(level, origin), BlueprintRooms.depth(level, origin)));
    }
    List<Volume> result = List.copyOf(volumes);
    SNAPSHOTS.put(level.getServer(), result);
    return result;
  }

  /** Never discover/generate terrain or dirty saved data during a spawn. */
  static boolean protectedPosition(ServerLevel level, BlockPos position) {
    if (!level.dimension().equals(Level.OVERWORLD)) return false;
    var server = level.getServer();
    List<Volume> volumes =
        server.isSameThread() ? snapshot(level) : SNAPSHOTS.getOrDefault(server, List.of());
    return volumes.stream().anyMatch(volume -> volume.contains(position));
  }

  /** Half-open authored bounds cover every floor, not just the arrival lobby. */
  static boolean contains(ServerLevel level, BlockPos origin, BlockPos position) {
    return new Volume(
            origin, BlueprintRooms.width(level, origin), BlueprintRooms.depth(level, origin))
        .contains(position);
  }

  @SubscribeEvent
  public static void tick(ServerTickEvent.Pre event) {
    snapshot(event.getServer().overworld());
  }

  @SubscribeEvent
  public static void stopped(ServerStoppedEvent event) {
    SNAPSHOTS.remove(event.getServer());
  }

  static boolean authoredSpawn(Mob mob, MobSpawnType reason) {
    return reason == MobSpawnType.STRUCTURE && mob instanceof SarcScientist
        || reason == MobSpawnType.EVENT && mob.getPersistentData().getBoolean(SCRIPTED_SPAWN_KEY);
  }

  static boolean blocked(ServerLevel level, Mob mob, MobSpawnType reason) {
    return !authoredSpawn(mob, reason) && protectedPosition(level, mob.blockPosition());
  }

  @SubscribeEvent(priority = EventPriority.LOWEST)
  public static void checkPosition(MobSpawnEvent.PositionCheck event) {
    // WorldGenRegion is handled at finalization too; use its owning dimension's saved site anchors.
    ServerLevel level = event.getLevel().getLevel();
    if (blocked(level, event.getEntity(), event.getSpawnType()))
      event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
  }

  // Another listener may cancel initialization without canceling the actual spawn.
  @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
  public static void finalizeSpawn(FinalizeSpawnEvent event) {
    if (blocked(event.getLevel().getLevel(), event.getEntity(), event.getSpawnType())) {
      // Canceling only the event skips initialization but STILL spawns the mob in NeoForge.
      event.setSpawnCancelled(true);
      event.setCanceled(true);
    }
  }

  private FacilitySpawnProtection() {}
}
