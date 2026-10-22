package com.timelocked;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent.Post;

public final class PlantMeltdown {
  public static final int HEAR = 900;
  public static final int CLOUD = 320;
  static final List<PlantMeltdown.Blast> ACTIVE = new CopyOnWriteArrayList<>();

  public static void register() {
    NeoForge.EVENT_BUS.addListener(PlantMeltdown::tick);
    NeoForge.EVENT_BUS.addListener(
        (net.neoforged.neoforge.event.server.ServerStartedEvent e) -> {
          ACTIVE.clear();
          for (ServerLevel l : e.getServer().getAllLevels())
            ACTIVE.addAll(MeltdownState.get(l).jobs);
        });
    NeoForge.EVENT_BUS.addListener(
        (net.neoforged.neoforge.event.server.ServerStoppedEvent e) -> ACTIVE.clear());
  }

  public static boolean active() {
    return !ACTIVE.isEmpty();
  }

  public static void start(ServerLevel l, BlockPos c, int kind) {
    if (ACTIVE.stream().anyMatch(b -> b.dim.equals(l.dimension()) && b.c.equals(c))) return;
    boolean industrial = l.getBlockEntity(c) instanceof PlantCoreBlock.Core core && core.industrial;
    Blast b =
        new Blast(
            l.dimension(),
            c.immutable(),
            kind,
            industrial ? MeltdownExcavation.INDUSTRIAL_RADIUS[kind] : PlantCoreBlock.RADIUS[kind],
            l.getGameTime(),
            industrial,
            l.getHeight(Types.WORLD_SURFACE, c.getX(), c.getZ()));
    ACTIVE.add(b);
    MeltdownState.get(l).jobs.add(b);
    MeltdownState.get(l).setDirty();
  }

  private static void tick(Post e) {
    MinecraftServer s = e.getServer();

    int allowance = Math.max(1, MeltdownExcavation.GLOBAL_BUDGET / Math.max(1, ACTIVE.size()));
    int remaining = MeltdownExcavation.GLOBAL_BUDGET;
    for (PlantMeltdown.Blast b : ACTIVE) {
      ServerLevel l = s.getLevel(b.dim);
      if (l == null) {
        ACTIVE.remove(b);
      } else {
        int age = (int) (l.getGameTime() - b.start);
        if (age >= 0) {
          step(l, b, age);
          if (age >= 2 && !b.cursor.done() && remaining > 0) {
            int used = excavate(l, b, Math.min(allowance, remaining));
            remaining -= used;
            MeltdownState.get(l).setDirty();
          }
          if (b.cursor.done() && !b.falloutDone) {
            fallout(l, b);
            b.falloutDone = true;
            MeltdownState.get(l).setDirty();
          }
          if (age > 320 && b.cursor.done() && b.falloutDone) {
            ACTIVE.remove(b);
            MeltdownState.get(l).jobs.remove(b);
            MeltdownState.get(l).setDirty();
          }
        }
      }
    }
  }

  static double radius(PlantMeltdown.Blast b, int x, int y, int z) {
    double a = Math.atan2(z, x);
    return b.r * (1.0 + 0.1 * Math.sin(3.0 * a + b.c.getX()) + 0.07 * Math.cos(5.0 * a + y * 0.3));
  }

  private static void step(ServerLevel l, PlantMeltdown.Blast b, int age) {
    Vec3 c = Vec3.atCenterOf(b.c);
    int k = b.kind;
    if (!b.ignited) {
      b.ignited = true;
      MeltdownState.get(l).setDirty();
      l.sendParticles(ParticleTypes.FLASH, c.x, c.y, c.z, 6, 2.0, 2.0, 2.0, 0.0);
      l.sendParticles(ParticleTypes.EXPLOSION_EMITTER, c.x, c.y, c.z, 8, 4.0, 3.0, 4.0, 0.0);
      if (k == 3) {
        for (Entity en :
            l.getEntities(
                (Entity) null,
                new AABB(b.c).inflate(b.r * 3),
                x -> !(x instanceof Player px && px.isSpectator()))) {
          en.setDeltaMovement(
              en.getDeltaMovement().add(c.subtract(en.position()).normalize().scale(1.4)));
        }
      }

      l.explode(
          null, c.x, c.y, c.z, b.industrial ? 16.0F : 10.0F, k == 1, ExplosionInteraction.BLOCK);
      hurt(l, b, c);
    }

    if (age >= 3 && age <= 36 && age % 3 == 0) {
      double a = age * 2.399;
      Vec3 q = c.add(Math.cos(a) * b.r * 0.75, -2.0, Math.sin(a) * b.r * 0.75);
      l.explode(
          null, q.x, q.y, q.z, b.industrial ? 9.0F : 6.0F, k == 1, ExplosionInteraction.BLOCK);
    }

    if (k == 2 && (age == 60 || age == 120)) {
      l.sendParticles(ParticleTypes.SONIC_BOOM, c.x, c.y, c.z, 1, 0.0, 0.0, 0.0, 0.0);
      l.sendParticles(
          ParticleTypes.REVERSE_PORTAL, c.x, c.y, c.z, 400, b.r * 0.5, b.r * 0.3, b.r * 0.5, 0.5);
      l.playSound(null, b.c, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.BLOCKS, 6.0F, 0.5F);
      hurt(l, b, c);
    }

    for (ServerPlayer p : l.players()) {
      double d = Math.sqrt(p.distanceToSqr(c));
      if (!(d > 900.0) && !b.heard.contains(p.getUUID()) && !(age < d / 17.0)) {
        b.heard.add(p.getUUID());
        Vec3 dir = c.subtract(p.position()).normalize();
        Vec3 at = d < 16.0 ? c : p.position().add(dir.scale(16.0));
        float vol = (float) Mth.clamp(6.0 - d / 200.0, 1.5, 6.0);
        p.connection.send(
            new ClientboundSoundPacket(
                PlantMeltdown.BuiltInRegistries_SOUND.EXPLODE,
                SoundSource.BLOCKS,
                at.x,
                at.y,
                at.z,
                vol,
                0.45F,
                l.random.nextLong()));
        p.connection.send(
            new ClientboundSoundPacket(
                PlantMeltdown.BuiltInRegistries_SOUND.THUNDER,
                SoundSource.WEATHER,
                at.x,
                at.y,
                at.z,
                vol,
                0.6F,
                l.random.nextLong()));
        Achievements.grant(p, "big_bang");
      }
    }

    if (age % 2 == 0 && age <= 320) {
      cloud(l, b, c, age);
    }
  }

  private static void hurt(ServerLevel l, PlantMeltdown.Blast b, Vec3 c) {
    double reach = b.r * 1.6;

    for (LivingEntity en : l.getEntitiesOfClass(LivingEntity.class, new AABB(b.c).inflate(reach))) {
      double d = en.position().distanceTo(c);
      if (!(d > reach)) {
        float f = (float) (1.0 - d / reach);
        en.hurt(l.damageSources().explosion(null, null), 6.0F + 40.0F * f * f);
        Vec3 push = en.position().subtract(c).normalize().scale(b.kind == 3 ? -1.2 * f : 2.2 * f);
        en.setDeltaMovement(en.getDeltaMovement().add(push.x, 0.6 * f, push.z));
        en.hurtMarked = true;
      }
    }
  }

  static int excavate(ServerLevel l, Blast b, int budget) {
    int used = 0;
    while (used < budget && !b.cursor.done()) {
      var q = b.cursor.next();
      used++;
      BlockPos p = b.c.offset(q.x(), q.y(), q.z());
      if (p.getY() < l.getMinBuildHeight() || p.getY() >= l.getMaxBuildHeight() || !l.hasChunkAt(p))
        continue;
      double rr = radius(b, q.x(), p.getY(), q.z());
      double dy = q.y();
      double d = Math.sqrt(q.x() * q.x() + q.z() * q.z() + dy * dy * (dy < 0 ? 1.25 : .6));
      if (d >= rr + 2.2) continue;
      BlockState state = l.getBlockState(p);
      if (state.isAir() || state.getDestroySpeed(l, p) < 0) continue;
      if (d < rr) l.setBlock(p, Blocks.AIR.defaultBlockState(), 18);
      else if (d < rr + 2.2 && dy <= 2 && state.isSolidRender(l, p))
        l.setBlock(p, lining(b.kind, l.random.nextInt(100)), 18);
    }
    return used;
  }

  static BlockState lining(int k, int n) {
    Block b =
        switch (k) {
          case 0 ->
              n < 55
                  ? (Block) Content.SCORCHED_STONE.get()
                  : (n < 80 ? (Block) Content.ASH.get() : Blocks.GREEN_STAINED_GLASS);
          case 1 ->
              n < 40
                  ? Blocks.MAGMA_BLOCK
                  : (n < 70
                      ? Blocks.BASALT
                      : (n < 90 ? Blocks.BLACKSTONE : (Block) Content.ASH.get()));
          case 2 ->
              n < 40
                  ? Blocks.CALCITE
                  : (n < 60
                      ? Blocks.LIGHT_BLUE_STAINED_GLASS
                      : (n < 78
                          ? Blocks.AMETHYST_BLOCK
                          : (n < 88 ? chrono() : (Block) Content.SCORCHED_STONE.get())));
          default ->
              n < 50
                  ? Blocks.OBSIDIAN
                  : (n < 80 ? Blocks.CRYING_OBSIDIAN : (Block) Content.ASH.get());
        };
    return b.defaultBlockState();
  }

  private static Block chrono() {
    return (Block)
        BuiltInRegistries.BLOCK.get(
            ResourceLocation.fromNamespaceAndPath("timelocked", "chronotite_block"));
  }

  static void fallout(ServerLevel l, PlantMeltdown.Blast b) {
    int cx = b.c.getX();
    int cz = b.c.getZ();
    int floor = l.hasChunkAt(b.c) ? l.getHeight(Types.MOTION_BLOCKING, cx, cz) : b.surface;
    Vec3 f = new Vec3(cx + 0.5, floor, cz + 0.5);
    RandomSource rnd = l.random;
    switch (b.kind) {
      case 0:
        for (int i = 0; i < 3; i++) {
          AreaEffectCloud a =
              new AreaEffectCloud(
                  l, f.x + (i - 1) * b.r * 0.4, floor + 0.2, f.z + (i == 1 ? 0.0 : b.r * 0.2));
          a.setRadius(b.r * 0.45F);
          a.setDuration(6000);
          a.setRadiusPerTick(0.0F);
          a.setWaitTime(0);
          a.setPotionContents(
              new PotionContents(
                  Optional.empty(),
                  Optional.of(8191823),
                  List.of(
                      new MobEffectInstance(MobEffects.POISON, 120, 0),
                      new MobEffectInstance(MobEffects.HUNGER, 200, 1))));
          l.addFreshEntity(a);
        }
        break;
      case 1:
        for (int i = 0; i < 260; i++) {
          int x = cx + rnd.nextInt(b.r * 3) - b.r * 3 / 2;
          int z = cz + rnd.nextInt(b.r * 3) - b.r * 3 / 2;
          if (!l.hasChunkAt(new BlockPos(x, b.c.getY(), z))) continue;
          BlockPos top = new BlockPos(x, l.getHeight(Types.MOTION_BLOCKING, x, z), z);
          if (l.hasChunkAt(top)
              && l.getBlockState(top).isAir()
              && l.getBlockState(top.below()).isSolidRender(l, top.below())) {
            l.setBlock(top, BaseFireBlock.getState(l, top), 3);
          }
        }
        break;
      case 2:
        for (LivingEntity en :
            l.getEntitiesOfClass(LivingEntity.class, new AABB(b.c).inflate(b.r * 3))) {
          en.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 240, 4));
          en.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 600, 2));
        }
        break;
      default:
        for (int i = 0; i < 4; i++) {
          Mob m =
              (Mob)
                  (i == 0
                          ? (EntityType) VoidspaceContent.ASHBACK.get()
                          : (EntityType) VoidspaceContent.SKITTER.get())
                      .create(l);
          if (m != null) {
            m.moveTo(
                f.x + rnd.nextInt(7) - 3.0,
                floor + 1,
                f.z + rnd.nextInt(7) - 3.0,
                rnd.nextFloat() * 360.0F,
                0.0F);
            // Authored hazard, not ambient wildlife or an ordinary spawner.
            m.getPersistentData().putBoolean(FacilitySpawnProtection.SCRIPTED_SPAWN_KEY, true);
            m.finalizeSpawn(
                l, l.getCurrentDifficultyAt(m.blockPosition()), MobSpawnType.EVENT, null);
            l.addFreshEntity(m);
          }
        }
    }
  }

  private static void cloud(ServerLevel l, PlantMeltdown.Blast b, Vec3 c, int age) {
    float t = age / 320.0F;
    double plumeBase = b.industrial ? Math.max(c.y, b.surface - 4) : c.y;
    double scale = b.industrial ? 1.6 : 1;
    double top = plumeBase + scale * (20.0 + 70.0 * Math.min(1.0, t * 2.2));
    double cap = scale * (8.0 + 26.0 * Math.min(1.0, t * 1.6));
    DustParticleOptions col = PlantCoreBlock.dust(b.kind, 4.0F);

    for (ServerPlayer p : l.players()) {
      if (!(p.distanceToSqr(c) > 360000.0)) {
        for (int i = 0; i < 6; i++) {
          double h = plumeBase + l.random.nextDouble() * (top - plumeBase);
          l.sendParticles(
              p,
              ParticleTypes.CAMPFIRE_SIGNAL_SMOKE,
              true,
              c.x + l.random.nextGaussian() * 3.0 * scale,
              h,
              c.z + l.random.nextGaussian() * 3.0 * scale,
              0,
              0.0,
              0.05,
              0.0,
              1.0);
        }

        for (int i = 0; i < 10; i++) {
          double a = l.random.nextDouble() * Math.PI * 2.0;
          double rr = cap * Math.sqrt(l.random.nextDouble());
          l.sendParticles(
              p,
              i % 3 == 0 ? ParticleTypes.LARGE_SMOKE : ParticleTypes.CAMPFIRE_COSY_SMOKE,
              true,
              c.x + Math.cos(a) * rr,
              top + l.random.nextGaussian() * 4.0,
              c.z + Math.sin(a) * rr,
              0,
              Math.cos(a) * 0.1,
              0.02,
              Math.sin(a) * 0.1,
              1.0);
        }

        if (age < 160) {
          l.sendParticles(p, col, true, c.x, top - 4.0, c.z, 8, cap * 0.4, 3.0, cap * 0.4, 0.0);
          l.sendParticles(
              p, ParticleTypes.LAVA, true, c.x, c.y + 4.0, c.z, 3, b.r * 0.3, 2.0, b.r * 0.3, 0.0);
        }
      }
    }
  }

  private PlantMeltdown() {}

  static final class Blast {
    final ResourceKey<Level> dim;
    final BlockPos c;
    final int kind;
    final int r;
    final long start;
    final Set<UUID> heard = new HashSet<>();
    final boolean industrial;
    final int surface;
    final MeltdownExcavation.Cursor cursor;
    boolean falloutDone, ignited;

    Blast(ResourceKey<Level> d, BlockPos c, int k, int r, long s, boolean industrial, int surface) {
      this.industrial = industrial;
      this.surface = surface;
      this.cursor = new MeltdownExcavation.Cursor(r, industrial);
      this.dim = d;
      this.c = c;
      this.kind = k;
      this.r = r;
      this.start = s;
    }
  }

  private static final class BuiltInRegistries_SOUND {
    static final Holder<SoundEvent> EXPLODE = SoundEvents.GENERIC_EXPLODE;
    static final Holder<SoundEvent> THUNDER =
        BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.LIGHTNING_BOLT_THUNDER);
  }
}
