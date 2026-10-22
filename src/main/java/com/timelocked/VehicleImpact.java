package com.timelocked;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class VehicleImpact {
  public static final ResourceKey<DamageType> IMPACT =
      ResourceKey.create(
          Registries.DAMAGE_TYPE,
          ResourceLocation.fromNamespaceAndPath("timelocked", "vehicle_impact"));
  public static final double PUSH = 0.06;
  public static final double HURT = 0.18;
  public static final double LETHAL = 0.42;

  public static int hit(Entity vehicle, double speed, Vec3 heading) {
    if (vehicle.level() instanceof ServerLevel l && !(Math.abs(speed) < 0.06)) {
      Player driver = vehicle.getFirstPassenger() instanceof Player p ? p : null;
      int hits = 0;
      Vec3 dir =
          heading.lengthSqr() < 1.0E-6 ? Vec3.ZERO : heading.normalize().scale(Math.signum(speed));
      double s = Math.abs(speed);

      for (LivingEntity e :
          l.getEntitiesOfClass(
              LivingEntity.class,
              vehicle.getBoundingBox().inflate(0.35, 0.0, 0.35).expandTowards(dir.scale(s)),
              ex ->
                  ex.isAlive()
                      && !ex.isSpectator()
                      && ex.getVehicle() == null
                      && !vehicle.hasPassenger(ex)
                      && !(ex instanceof Player p && p.isCreative()))) {
        if (e.invulnerableTime <= 10) {
          Vec3 away = e.position().subtract(vehicle.position());
          Vec3 side = new Vec3(away.x, 0.0, away.z);
          side = side.lengthSqr() < 1.0E-4 ? new Vec3(-dir.z, 0.0, dir.x) : side.normalize();
          Vec3 push = dir.scale(s * 2.2).add(side.scale(0.35 + s * 1.2));
          e.push(push.x, 0.18 + s * 0.5, push.z);
          e.hurtMarked = true;
          hits++;
          if (s >= 0.18) {
            DamageSource src =
                new DamageSource(
                    l.registryAccess()
                        .registryOrThrow(Registries.DAMAGE_TYPE)
                        .getHolderOrThrow(IMPACT),
                    vehicle,
                    driver);
            float dmg = s >= 0.42 ? Math.max(e.getMaxHealth() * 4.0F, 1000.0F) : (float) (s * 20.0);
            if (e instanceof Player) {
              dmg = Math.min(dmg, 10.0F);
            }

            e.hurt(src, dmg);
            l.playSound(
                null,
                e.blockPosition(),
                s >= 0.42 ? SoundEvents.PLAYER_ATTACK_CRIT : SoundEvents.PLAYER_ATTACK_KNOCKBACK,
                SoundSource.NEUTRAL,
                1.0F,
                0.8F);
          } else {
            l.playSound(
                null,
                e.blockPosition(),
                SoundEvents.PLAYER_ATTACK_WEAK,
                SoundSource.NEUTRAL,
                0.6F,
                0.7F);
          }

          if (driver instanceof ServerPlayer sp && s >= 0.18) {
            Achievements.grant(sp, "speed_bump");
            if (!e.isAlive()) {
              Achievements.grant(sp, "roadkill");
            }
          }
        }
      }

      return hits;
    } else {
      return 0;
    }
  }

  private VehicleImpact() {}
}
