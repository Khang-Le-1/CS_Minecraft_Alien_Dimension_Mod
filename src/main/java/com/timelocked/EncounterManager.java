package com.timelocked;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent.BossBarColor;
import net.minecraft.world.BossEvent.BossBarOverlay;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent.Post;

public final class EncounterManager {
  private static final Map<MinecraftServer, Map<Long, ServerBossEvent>> BARS = new WeakHashMap<>();

  public static void register() {
    ReactorEffects.register();
    NeoForge.EVENT_BUS.addListener(EncounterManager::tick);
    NeoForge.EVENT_BUS.addListener(
        (ServerStoppingEvent e) -> {
          Map<Long, ServerBossEvent> bars = BARS.remove(e.getServer());
          if (bars != null) {
            bars.values().forEach(ServerBossEvent::removeAllPlayers);
          }
        });
  }

  public static String start(ServerLevel l, BlockPos o) {
    SiteProgress p = SitePlanner.progress(l, o);
    if (p.cleared) {
      return "Transit unlocked; encounter already cleared.";
    }

    if (p.fighting) {
      return "Bionexian encounter already active.";
    }

    if (l.getDifficulty() == Difficulty.PEACEFUL) {
      return "Switch out of Peaceful to activate the three-wave encounter.";
    }

    p.fighting = true;
    p.wave = 0;
    p.nextWaveTick = l.getGameTime();
    FacilityState.get(l.getServer()).setDirty();
    spawn(l, o, p);
    return "BIOLOGICAL BREACH: wave 1 of 3. Defeat all Makglo to unlock transit.";
  }

  private static void spawn(ServerLevel l, BlockPos o, SiteProgress p) {
    if (p.wave >= 3) {
      p.fighting = false;
      p.cleared = true;
      FacilityState.get(l.getServer()).setDirty();
      Achievements.grantNear(l, BlueprintRooms.at(l, o, 64, 4, 30), 128.0, "three_waves");
      notify(l, o, "Three waves cleared. Voidspace transit is now unlocked.");
    } else {
      int next = p.wave + 1;
      int count = next * 2 + 1;
      List<Makglo> spawned = new ArrayList<>();

      for (int i = 0; i < count; i++) {
        BlockPos desired = BlueprintRooms.at(l, o, 62 + i % 3 * 2, 8, 36 + i / 3 * 8);
        Optional<Vec3> position = catwalkSpot(l, desired);
        if (position.isEmpty()) {
          spawned.forEach(Entity::discard);
          notify(l, o, "Encounter paused: clear the chamber bridge for enemy spawning.");
          p.nextWaveTick = l.getGameTime() + 100L;
          return;
        }

        // Wave 1: Makglo only. Wave 2 adds Unnamed. Wave 3 adds Unnamed and Greed.
        EntityType kind =
            next >= 3 && i % 3 == 2
                ? Content.GREED.get()
                : (next >= 2 && i % 3 == 1 ? Content.UNNAMED.get() : Content.MAKGLO.get());
        Makglo mob = (Makglo) kind.create(l);
        if (mob == null) {
          spawned.forEach(Entity::discard);
          return;
        }

        mob.siteOrigin = o;
        mob.setPersistenceRequired();
        double healthScale = mob instanceof Greed ? 1.5 : (mob instanceof Unnamed ? 0.9 : 1.0);
        double damageBonus = mob instanceof Greed ? 2.0 : (mob instanceof Unnamed ? 1.0 : 0.0);
        mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue((24 + next * 6) * healthScale);
        mob.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(3 + next + damageBonus);
        mob.setHealth(mob.getMaxHealth());
        Vec3 pos = position.get();
        mob.moveTo(pos.x, pos.y, pos.z, 180.0F, 0.0F);
        if (!l.addFreshEntity(mob)) {
          spawned.forEach(Entity::discard);
          return;
        }

        spawned.add(mob);
      }

      p.wave = next;

      for (Makglo mob : spawned) {
        p.living.add(mob.getUUID());
      }

      FacilityState.get(l.getServer()).setDirty();
      notify(l, o, "Bionexian wave " + next + " / 3: " + count + " hostiles.");
    }
  }

  private static Optional<Vec3> catwalkSpot(ServerLevel l, BlockPos desired) {
    for (int r = 0; r <= 3; r++) {
      for (int dx = -r; dx <= r; dx++) {
        for (int dz = -r; dz <= r; dz++) {
          if (Math.max(Math.abs(dx), Math.abs(dz)) == r) {
            BlockPos pos = desired.offset(dx, 0, dz);
            l.getChunkAt(pos);
            if (l.getBlockState(pos.below()).is((Block) FacilityDetails.WALKWAY.get())) {
              Vec3 feet = new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
              if (l.noCollision(
                  new AABB(
                      feet.x - 0.3,
                      feet.y,
                      feet.z - 0.3,
                      feet.x + 0.3,
                      feet.y + 1.8,
                      feet.z + 0.3))) {
                return Optional.of(feet);
              }
            }
          }
        }
      }
    }

    return VoidspaceTravel.safeSpot(l, desired);
  }

  public static void advance(ServerLevel l, BlockPos origin) {
    SiteProgress p = SitePlanner.progress(l, origin);
    if (p.fighting && p.living.isEmpty() && l.getGameTime() >= p.nextWaveTick) {
      spawn(l, origin, p);
    }
  }

  public static void defeated(ServerLevel l, BlockPos origin, UUID id) {
    SiteProgress p = SitePlanner.progress(l, origin);
    if (p.living.remove(id)) {
      p.nextWaveTick = l.getGameTime() + 100L;
      FacilityState.get(l.getServer()).setDirty();
    }
  }

  private static void notify(ServerLevel l, BlockPos o, String text) {
    for (ServerPlayer player : l.players()) {
      if (player.blockPosition().distSqr(BlueprintRooms.at(l, o, 64, 8, 48)) < 36864.0) {
        player.sendSystemMessage(Component.literal(text));
      }
    }
  }

  private static void tick(Post e) {
    if (e.getServer().getTickCount() % 20 == 0) {
      ServerLevel l = e.getServer().overworld();
      if (l != null) {
        FacilityState state = FacilityState.get(e.getServer());
        Map<Long, ServerBossEvent> bars = BARS.computeIfAbsent(e.getServer(), s -> new HashMap<>());

        for (Entry<Long, SiteProgress> entry : state.progress.entrySet()) {
          BlockPos o = BlockPos.of(entry.getKey());
          SiteProgress p = entry.getValue();
          if (!p.fighting) {
            ServerBossEvent old = bars.remove(entry.getKey());
            if (old != null) {
              old.removeAllPlayers();
            }
          } else {
            ServerBossEvent bar =
                bars.computeIfAbsent(
                    entry.getKey(),
                    n ->
                        new ServerBossEvent(
                            Component.literal("Bionexian breach"),
                            BossBarColor.YELLOW,
                            BossBarOverlay.NOTCHED_6));
            bar.removeAllPlayers();
            boolean present = false;

            for (ServerPlayer player : l.players()) {
              if (player.blockPosition().distSqr(BlueprintRooms.at(l, o, 64, 8, 48)) < 36864.0) {
                bar.addPlayer(player);
                present = true;
              }
            }

            bar.setName(Component.literal("Bionexian breach — wave " + p.wave + " / 3"));
            bar.setProgress((float) p.living.size() / Math.max(1, p.wave * 2 + 1));
            if (l.getDifficulty() != Difficulty.PEACEFUL
                && present
                && p.living.isEmpty()
                && l.getGameTime() >= p.nextWaveTick) {
              spawn(l, o, p);
            }
          }
        }
      }
    }
  }

  private EncounterManager() {}
}
