package com.timelocked;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;

/** Test-only native spawn checks. Does not change the world's global mob-spawning game rule. */
final class FacilitySpawnAudit {
  static void check(boolean value, String label) {
    IntroAudit.check(value, "facility spawn protection: " + label);
  }

  static void run(MinecraftServer server) throws Exception {
    var level = server.overworld();
    var state = FacilityState.get(server);
    BlockPos origin = new BlockPos(-24000, 80, -24000);
    long key = origin.asLong();
    var oldSite = state.sites.get(key);
    var oldProgress = state.progress.get(key);
    var progress = new SiteProgress();
    progress.geometryVersion = BlueprintRooms.VERSION;
    state.sites.put(key, 42);
    state.progress.put(key, progress);
    BlockPos inside = origin.offset(79, 2, 273);
    try {
      check(
          FacilitySpawnProtection.protectedPosition(level, inside), "saved drawing hub protected");
      check(FacilitySpawnProtection.contains(level, origin, origin), "inclusive minimum boundary");
      check(
          FacilitySpawnProtection.contains(
              level,
              origin,
              origin.offset(
                  BlueprintRooms.WIDTH - 1, BlueprintRooms.HEIGHT - 1, BlueprintRooms.DEPTH - 1)),
          "all floors through last interior block protected");
      for (BlockPos outside :
          new BlockPos[] {
            origin.west(),
            origin.below(),
            origin.north(),
            origin.offset(BlueprintRooms.WIDTH, 0, 0),
            origin.offset(0, BlueprintRooms.HEIGHT, 0),
            origin.offset(0, 0, BlueprintRooms.DEPTH)
          })
        check(
            !FacilitySpawnProtection.protectedPosition(level, outside),
            "outside face unchanged: " + outside);
      check(
          !FacilitySpawnProtection.protectedPosition(
              server.getLevel(IntroSequence.HOME_DIM), inside),
          "same coordinates in another dimension unaffected");
      check(
          CompletableFuture.supplyAsync(
                  () -> FacilitySpawnProtection.protectedPosition(level, inside))
              .get(5, TimeUnit.SECONDS),
          "worker reads immutable server-published bounds");
      int sitesBefore = state.sites.size(), progressBefore = state.progress.size();
      check(
          !FacilitySpawnProtection.protectedPosition(level, origin.offset(2000, 0, 0))
              && state.sites.size() == sitesBefore
              && state.progress.size() == progressBefore,
          "spawn lookup does not discover or register distant sites");

      Mob zombie = EntityType.ZOMBIE.create(level);
      Mob bat = EntityType.BAT.create(level);
      Mob cow = EntityType.COW.create(level);
      for (Mob mob : new Mob[] {zombie, bat, cow}) {
        mob.moveTo(inside.getX() + .5, inside.getY(), inside.getZ() + .5, 0, 0);
        for (MobSpawnType reason : MobSpawnType.values()) {
          var position = new MobSpawnEvent.PositionCheck(mob, level, reason, null);
          FacilitySpawnProtection.checkPosition(position);
          check(
              position.getResult() == MobSpawnEvent.PositionCheck.Result.FAIL,
              mob.getType() + " position denied for " + reason);
          var finalize =
              new FinalizeSpawnEvent(
                  mob,
                  level,
                  mob.getX(),
                  mob.getY(),
                  mob.getZ(),
                  level.getCurrentDifficultyAt(inside),
                  reason,
                  null,
                  null);
          FacilitySpawnProtection.finalizeSpawn(finalize);
          check(
              finalize.isSpawnCancelled() && finalize.isCanceled(),
              "actual spawn cancelled for " + reason);
          mob.setSpawnCancelled(false);
        }
      }
      // Exercise the real bus and NeoForge's entity-join blocker, not only our predicate.
      var realEvent =
          new FinalizeSpawnEvent(
              zombie,
              level,
              zombie.getX(),
              zombie.getY(),
              zombie.getZ(),
              level.getCurrentDifficultyAt(inside),
              MobSpawnType.SPAWNER,
              null,
              null);
      net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(realEvent);
      check(realEvent.isSpawnCancelled(), "event subscriber registered on native bus");
      check(!level.addFreshEntity(zombie), "NeoForge actually refuses cancelled entity insertion");
      var alreadyCancelled =
          new FinalizeSpawnEvent(
              cow,
              level,
              cow.getX(),
              cow.getY(),
              cow.getZ(),
              level.getCurrentDifficultyAt(inside),
              MobSpawnType.NATURAL,
              null,
              null);
      alreadyCancelled.setCanceled(true);
      net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(alreadyCancelled);
      check(
          alreadyCancelled.isSpawnCancelled(),
          "already-cancelled initialization still denies actual spawn");
      bat.moveTo(origin.getX() - 10, origin.getY(), origin.getZ(), 0, 0);
      var outsideEvent = new MobSpawnEvent.PositionCheck(bat, level, MobSpawnType.NATURAL, null);
      FacilitySpawnProtection.checkPosition(outsideEvent);
      check(
          outsideEvent.getResult() == MobSpawnEvent.PositionCheck.Result.DEFAULT,
          "outside spawn leaves vanilla decision untouched");

      var scientist = SarcContent.SCIENTIST.get().create(level);
      scientist.moveTo(inside.getX(), inside.getY(), inside.getZ(), 0, 0);
      check(
          !FacilitySpawnProtection.blocked(level, scientist, MobSpawnType.STRUCTURE),
          "authored scientist placement preserved");
      check(
          FacilitySpawnProtection.blocked(level, scientist, MobSpawnType.NATURAL),
          "scientist exception is not a blanket natural spawn bypass");
      cow.getPersistentData().putBoolean(FacilitySpawnProtection.SCRIPTED_SPAWN_KEY, true);
      check(
          !FacilitySpawnProtection.blocked(level, cow, MobSpawnType.EVENT),
          "marked scripted hazard preserved");
      check(
          FacilitySpawnProtection.blocked(level, cow, MobSpawnType.NATURAL),
          "script marker cannot bypass natural checks");
      check(
          FacilitySpawnProtection.blocked(level, zombie, MobSpawnType.STRUCTURE),
          "random structure mobs are not exempt");

      for (int version : new int[] {0, 4, BlueprintRooms.VERSION}) {
        progress.geometryVersion = version;
        int width = BlueprintRooms.width(level, origin),
            depth = BlueprintRooms.depth(level, origin);
        check(
            FacilitySpawnProtection.protectedPosition(
                level, origin.offset(width - 1, 2, depth - 1)),
            "saved legacy/current footprint protected: " + version);
        check(
            !FacilitySpawnProtection.protectedPosition(level, origin.offset(width, 2, depth - 1)),
            "legacy/current footprint does not grow into outside world: " + version);
      }
    } finally {
      if (oldSite == null) state.sites.remove(key);
      else state.sites.put(key, oldSite);
      if (oldProgress == null) state.progress.remove(key);
      else state.progress.put(key, oldProgress);
      FacilitySpawnProtection.protectedPosition(
          level, inside); // Refresh worker snapshot after cleanup.
    }
  }

  private FacilitySpawnAudit() {}
}
