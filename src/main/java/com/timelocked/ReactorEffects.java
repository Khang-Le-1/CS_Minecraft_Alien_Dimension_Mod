package com.timelocked;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent.Post;
import org.joml.Vector3f;

public final class ReactorEffects {
  private static final Map<Long, Long> IGNITION = new ConcurrentHashMap<>();
  private static final int SPIN_UP = 120;
  private static final DustParticleOptions CYAN =
      new DustParticleOptions(new Vector3f(0.35F, 0.9F, 1.0F), 1.6F);
  private static final DustParticleOptions WHITE =
      new DustParticleOptions(new Vector3f(0.9F, 1.0F, 1.0F), 1.0F);
  private static final DustParticleOptions BLUE =
      new DustParticleOptions(new Vector3f(0.25F, 0.45F, 1.0F), 1.2F);

  public static void register() {
    NeoForge.EVENT_BUS.addListener(ReactorEffects::tick);
  }

  public static Vec3 coreCenter(ServerLevel l, BlockPos o) {
    BlockPos c = BlueprintRooms.at(l, o, 108, 2, 160);
    return new Vec3(c.getX() + 1.5, c.getY() + 6, c.getZ() + .5);
  }

  public static void ignite(ServerLevel l, BlockPos o) {
    Achievements.grantNear(l, BlueprintRooms.at(l, o, 108, 2, 170), 96.0, "lights_on");
    IGNITION.put(o.asLong(), l.getGameTime());
    Vec3 c = coreCenter(l, o);
    BlockPos at = BlockPos.containing(c);
    l.sendParticles(ParticleTypes.FLASH, c.x, c.y, c.z, 1, 0.0, 0.0, 0.0, 0.0);
    l.sendParticles(ParticleTypes.ELECTRIC_SPARK, c.x, c.y, c.z, 60, 0.8, 1.0, 0.8, 0.4);
    l.playSound(null, at, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 3.0F, 0.5F);
    l.playSound(
        null,
        BlueprintRooms.at(l, o, 110, 3, 184),
        SoundEvents.BEACON_POWER_SELECT,
        SoundSource.BLOCKS,
        1.0F,
        0.8F);
    lights(l, o);
  }

  static void lights(ServerLevel l, BlockPos o) {
    for (int x = 107; x <= 109; x++) {
      for (int y = 6; y <= 9; y++) {
        for (int z = 159; z <= 161; z++) {
          BlockPos p = BlueprintRooms.at(l, o, x, y, z);
          if (l.hasChunkAt(p) && l.getBlockState(p).isAir()) {
            l.setBlock(
                p, (BlockState) Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 15), 3);
          }
        }
      }
    }
  }

  private static void tick(Post e) {
    ServerLevel l = e.getServer().overworld();
    if (l != null && !l.players().isEmpty()) {
      long t = l.getGameTime();

      for (Entry<Long, SiteProgress> entry : FacilityState.get(e.getServer()).progress.entrySet()) {
        if (entry.getValue().powered) {
          BlockPos o = BlockPos.of(entry.getKey());
          Vec3 c = coreCenter(l, o);
          BlockPos at = BlockPos.containing(c);
          if (l.hasChunkAt(at)) {
            List<ServerPlayer> near = new ArrayList<>();

            for (ServerPlayer p : l.players()) {
              if (p.distanceToSqr(c) < 3136.0) {
                near.add(p);
              }
            }

            if (!near.isEmpty()) {
              Long ig = IGNITION.get(entry.getKey());
              long age = ig == null ? 120L : t - ig;
              float ramp = Math.min(1.0F, (float) age / 120.0F);
              if (age == 120L) {
                l.sendParticles(ParticleTypes.SONIC_BOOM, c.x, c.y, c.z, 1, 0.0, 0.0, 0.0, 0.0);
                l.sendParticles(ParticleTypes.FLASH, c.x, c.y, c.z, 2, 0.0, 0.0, 0.0, 0.0);
                l.sendParticles(ParticleTypes.END_ROD, c.x, c.y, c.z, 140, 0.2, 0.2, 0.2, 0.35);
                l.playSound(
                    null, at, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.BLOCKS, 1.6F, 1.4F);
                l.playSound(null, at, SoundEvents.CONDUIT_ACTIVATE, SoundSource.BLOCKS, 3.0F, 0.8F);
              }

              if (t % 100L == 0L) {
                lights(l, o);
              }

              for (ServerPlayer p : near) {
                emit(l, p, c, t, ramp, age < 120L);
              }

              if (t % 80L == 0L) {
                l.playSound(
                    null,
                    at,
                    SoundEvents.BEACON_AMBIENT,
                    SoundSource.BLOCKS,
                    1.4F,
                    0.5F + 0.1F * ramp);
              }

              if (t % 240L == 0L) {
                l.playSound(null, at, SoundEvents.CONDUIT_AMBIENT, SoundSource.BLOCKS, 1.2F, 0.7F);
              }
            }
          }
        }
      }
    }
  }

  private static void emit(
      ServerLevel l, ServerPlayer p, Vec3 c, long t, float ramp, boolean spinning) {
    double spin = spinning ? 0.1 + 0.5 * ramp : 0.22;
    double r = 0.25 + 0.85 * ramp;

    for (int k = 0; k < 3; k++) {
      double a = t * spin + k * Math.PI * 2.0 / 3.0;
      double b = Math.sin(t * 0.07 + k * 2.1) * 0.75 * ramp;
      l.sendParticles(
          p,
          ParticleTypes.END_ROD,
          false,
          c.x + Math.cos(a) * r,
          c.y + b,
          c.z + Math.sin(a) * r,
          0,
          -Math.sin(a),
          0.0,
          Math.cos(a),
          0.025);
    }

    double ring = 1.25 * ramp;

    for (int k = 0; k < 4; k++) {
      double a = t * 0.15 + k * Math.PI / 2.0;
      l.sendParticles(
          p,
          CYAN,
          false,
          c.x + Math.cos(a) * ring,
          c.y + Math.sin(a) * ring,
          c.z,
          1,
          0.0,
          0.0,
          0.0,
          0.0);
      double b = -t * 0.13 + k * Math.PI / 2.0;
      l.sendParticles(
          p,
          BLUE,
          false,
          c.x,
          c.y + Math.sin(b) * ring,
          c.z + Math.cos(b) * ring,
          1,
          0.0,
          0.0,
          0.0,
          0.0);
    }

    float beat = (float) (0.5 + 0.5 * Math.sin(t * 0.3));
    l.sendParticles(
        p,
        new DustParticleOptions(
            new Vector3f(0.5F + 0.5F * beat, 0.95F, 1.0F), 1.0F + 1.4F * beat * ramp),
        false,
        c.x,
        c.y,
        c.z,
        3,
        0.25 * ramp,
        0.35 * ramp,
        0.25 * ramp,
        0.0);
    if (t % 2L == 0L) {
      l.sendParticles(p, WHITE, false, c.x, c.y, c.z, 1, 0.1, 0.15, 0.1, 0.0);
    }

    l.sendParticles(
        p, ParticleTypes.END_ROD, false, c.x, c.y - 1.8, c.z, 0, 0.0, 1.0, 0.0, 0.12 * ramp);
    if (t % 3L == 0L) {
      l.sendParticles(
          p, ParticleTypes.ELECTRIC_SPARK, false, c.x, c.y + 5.5, c.z, 1, 0.15, 0.8, 0.15, 0.02);
    }

    if (t % 40L == 0L && ramp > 0.5) {
      for (int k = 0; k < 20; k++) {
        double a = k * Math.PI / 10.0;
        l.sendParticles(
            p,
            ParticleTypes.END_ROD,
            false,
            c.x + Math.cos(a) * 0.4,
            c.y,
            c.z + Math.sin(a) * 0.4,
            0,
            Math.cos(a),
            0.0,
            Math.sin(a),
            0.09);
      }
    }

    if (t % 9L == 0L && ramp > 0.3) {
      double a = t * 2.399 % (Math.PI * 2);
      double h = t * 7L % 9L / 9.0 * 3.0 - 1.5;
      Vec3 end = c.add(Math.cos(a) * 1.45, h, Math.sin(a) * 1.45);

      for (int s = 0; s <= 6; s++) {
        double f = s / 6.0;
        Vec3 q =
            c.lerp(end, f)
                .add(
                    (s % 2 == 0 ? 1 : -1) * 0.08, (s % 3 - 1) * 0.08, (s % 2 == 0 ? -1 : 1) * 0.08);
        l.sendParticles(
            p, ParticleTypes.ELECTRIC_SPARK, false, q.x, q.y, q.z, 1, 0.0, 0.0, 0.0, 0.0);
      }
    }

    if (t % 3L == 0L) {
      l.sendParticles(
          p,
          ParticleTypes.ELECTRIC_SPARK,
          false,
          c.x,
          c.y,
          c.z,
          1,
          0.9 * ramp,
          1.1 * ramp,
          0.9 * ramp,
          0.05);
    }
  }

  private ReactorEffects() {}
}
