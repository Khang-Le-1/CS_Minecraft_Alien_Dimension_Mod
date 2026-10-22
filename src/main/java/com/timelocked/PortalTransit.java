package com.timelocked;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Animated dimensional portal travel. Stepping into a gate puts you on the far side high above the
 * arrival point with nothing underneath you. You fall through a tunnel of portal frames (drawn by
 * the client) and, at the bottom, you are thrown out of the destination gate. Fall damage is
 * cancelled for the whole trip. Everything is bounded: a timeout, a logout guard and a death guard
 * make sure no one can be left hanging in the sky.
 */
@EventBusSubscriber(modid = "timelocked")
public final class PortalTransit {
  /** Blocks above the arrival point. About 3.5 seconds of free fall. */
  private static final int FALL_HEIGHT = 130;

  private static final int MAX_TICKS = 100;
  private static final int GRACE_TICKS = 80;
  private static final Map<UUID, State> ACTIVE = new HashMap<>();

  private static final class State {
    final Vec3 exit;
    final float yaw;
    int ticks;
    int grace = -1;

    State(Vec3 exit, float yaw) {
      this.exit = exit;
      this.yaw = yaw;
    }
  }

  private PortalTransit() {}

  /**
   * Replaces the old instant dimension change. {@code ground} is the real arrival spot on the far
   * side.
   */
  public static void begin(
      ServerPlayer player, ServerLevel target, Vec3 ground, float yaw, float pitch) {
    int ceiling = target.getMaxBuildHeight() - 6;
    double drop = Math.max(0.0, Math.min(FALL_HEIGHT, ceiling - ground.y));
    Vec3 sky = new Vec3(ground.x, ground.y + drop, ground.z);
    ACTIVE.put(player.getUUID(), new State(ground, yaw));
    player.setPortalCooldown(MAX_TICKS + 40);
    player.changeDimension(
        new DimensionTransition(
            target,
            sky,
            Vec3.ZERO,
            yaw,
            45.0F,
            DimensionTransition.PLAY_PORTAL_SOUND.then(DimensionTransition.PLACE_PORTAL_TICKET)));
    player.fallDistance = 0.0F;
    PacketDistributor.sendToPlayer(
        player, new PortalTransitNetworking.Phase(PortalTransitNetworking.Phase.START));
  }

  public static boolean protects(UUID id) {
    return ACTIVE.containsKey(id);
  }

  @SubscribeEvent
  public static void tick(PlayerTickEvent.Post event) {
    if (!(event.getEntity() instanceof ServerPlayer p) || p.level().isClientSide) return;
    State s = ACTIVE.get(p.getUUID());
    if (s == null) return;
    p.fallDistance = 0.0F;
    if (s.grace >= 0) {
      // Landing grace after the exit: still no fall damage while the player drops out of the gate.
      if (--s.grace < 0 || p.onGround()) ACTIVE.remove(p.getUUID());
      return;
    }

    s.ticks++;
    if (!p.isAlive() || p.isRemoved()) {
      ACTIVE.remove(p.getUUID());
      return;
    }

    if (p.getY() <= s.exit.y + 8.0 || s.ticks >= MAX_TICKS) finish(p, s);
  }

  private static void finish(ServerPlayer p, State s) {
    ServerLevel level = p.serverLevel();
    p.teleportTo(level, s.exit.x, s.exit.y + 1.1, s.exit.z, s.yaw, 0.0F);
    Vec3 out = Vec3.directionFromRotation(0.0F, s.yaw + 180.0F).scale(0.28);
    p.setDeltaMovement(out.x, 0.12, out.z);
    p.hurtMarked = true;
    p.fallDistance = 0.0F;
    level.sendParticles(
        ParticleTypes.REVERSE_PORTAL, s.exit.x, s.exit.y + 1.5, s.exit.z, 60, 0.7, 0.9, 0.7, 0.25);
    level.sendParticles(
        ParticleTypes.END_ROD, s.exit.x, s.exit.y + 1.5, s.exit.z, 30, 0.6, 0.8, 0.6, 0.15);
    level.playSound(
        null,
        s.exit.x,
        s.exit.y,
        s.exit.z,
        SoundEvents.ENDERMAN_TELEPORT,
        SoundSource.PLAYERS,
        0.8F,
        0.65F);
    PacketDistributor.sendToPlayer(
        p, new PortalTransitNetworking.Phase(PortalTransitNetworking.Phase.END));
    s.grace = GRACE_TICKS;
  }

  @SubscribeEvent
  public static void fall(LivingFallEvent event) {
    if (event.getEntity() instanceof ServerPlayer p && protects(p.getUUID())) {
      event.setCanceled(true);
    }
  }

  /**
   * Someone who logs off mid-fall is put on the ground first, so they never log back in to a death
   * drop.
   */
  @SubscribeEvent
  public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
    if (event.getEntity() instanceof ServerPlayer p) {
      State s = ACTIVE.remove(p.getUUID());
      if (s != null && p.isAlive()) {
        p.teleportTo(p.serverLevel(), s.exit.x, s.exit.y + 0.1, s.exit.z, s.yaw, 0.0F);
        p.fallDistance = 0.0F;
      }
    }
  }
}
