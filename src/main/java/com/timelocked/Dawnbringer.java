package com.timelocked;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent.BossBarColor;
import net.minecraft.world.BossEvent.BossBarOverlay;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.Goal.Flag;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;

public final class Dawnbringer extends Monster {
  public static final int LEGIONNAIRE = 0;
  public static final int SUNSPEAKER = 1;
  public static final int WARLORD = 2;
  public static final String[] IDS =
      new String[] {"dawnbringer_legionnaire", "dawnbringer_sunspeaker", "dawnbringer_warlord"};
  private static final EntityDataAccessor<Boolean> CASTING =
      SynchedEntityData.defineId(Dawnbringer.class, EntityDataSerializers.BOOLEAN);
  private final ServerBossEvent boss;
  int lanceCooldown = 40;
  int flareCooldown = 80;
  int hymnCooldown = 160;

  public Dawnbringer(EntityType<? extends Dawnbringer> t, Level l) {
    super(t, l);
    int k = this.kind();
    this.xpReward = k == 2 ? 80 : (k == 1 ? 12 : 10);
    this.boss =
        k == 2
            ? new ServerBossEvent(
                Component.translatable("entity.timelocked.dawnbringer_warlord"),
                BossBarColor.YELLOW,
                BossBarOverlay.NOTCHED_10)
            : null;
  }

  public int kind() {
    EntityType<?> t = this.getType();
    return t == VoidspaceContent.DAWN_WARLORD.get()
        ? 2
        : (t == VoidspaceContent.DAWN_SUNSPEAKER.get() ? 1 : 0);
  }

  public boolean casting() {
    return (Boolean) this.entityData.get(CASTING);
  }

  public static Builder attributes(int kind) {
    Builder b = Monster.createMonsterAttributes();

    return switch (kind) {
      case 1 ->
          b.add(Attributes.MAX_HEALTH, 24.0)
              .add(Attributes.ARMOR, 3.0)
              .add(Attributes.ATTACK_DAMAGE, 3.0)
              .add(Attributes.MOVEMENT_SPEED, 0.26)
              .add(Attributes.FOLLOW_RANGE, 32.0);
      case 2 ->
          b.add(Attributes.MAX_HEALTH, 160.0)
              .add(Attributes.ARMOR, 14.0)
              .add(Attributes.ARMOR_TOUGHNESS, 6.0)
              .add(Attributes.ATTACK_DAMAGE, 13.0)
              .add(Attributes.MOVEMENT_SPEED, 0.27)
              .add(Attributes.FOLLOW_RANGE, 40.0)
              .add(Attributes.KNOCKBACK_RESISTANCE, 0.85)
              .add(Attributes.ATTACK_KNOCKBACK, 1.2);
      default ->
          b.add(Attributes.MAX_HEALTH, 32.0)
              .add(Attributes.ARMOR, 8.0)
              .add(Attributes.ATTACK_DAMAGE, 6.0)
              .add(Attributes.MOVEMENT_SPEED, 0.27)
              .add(Attributes.FOLLOW_RANGE, 32.0)
              .add(Attributes.KNOCKBACK_RESISTANCE, 0.3);
    };
  }

  protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder b) {
    super.defineSynchedData(b);
    b.define(CASTING, false);
  }

  protected void registerGoals() {
    this.goalSelector.addGoal(0, new FloatGoal(this));
    if (this.kind() == 1) {
      this.goalSelector.addGoal(2, new Dawnbringer.LanceGoal(this));
    }

    this.goalSelector.addGoal(3, new MeleeAttackGoal(this, this.kind() == 2 ? 1.05 : 1.15, true));
    this.goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 1.0));
    this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.7));
    this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12.0F));
    this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    this.targetSelector.addGoal(
        1,
        new HurtByTargetGoal(this, new Class[] {Dawnbringer.class}).setAlertOthers(new Class[0]));
    this.targetSelector.addGoal(2, new NearestAttackableTargetGoal(this, Player.class, true));
    this.targetSelector.addGoal(
        3, new NearestAttackableTargetGoal(this, CelestialVillager.class, true));
  }

  public boolean isAlliedTo(Entity e) {
    return e instanceof Dawnbringer || super.isAlliedTo(e);
  }

  public boolean doHurtTarget(Entity target) {
    boolean hit = super.doHurtTarget(target);
    if (hit) {
      target.igniteForSeconds(this.kind() == 2 ? 5.0F : 3.0F);
    }

    return hit;
  }

  protected void customServerAiStep() {
    super.customServerAiStep();
    LivingEntity t = this.getTarget();
    if (this.kind() == 1 && t != null && --this.hymnCooldown <= 0) {
      this.hymnCooldown = 240 + this.random.nextInt(80);
      boolean any = false;

      for (Dawnbringer ally :
          this.level()
              .getEntitiesOfClass(
                  Dawnbringer.class, this.getBoundingBox().inflate(10.0), a -> a != this)) {
        ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 0));
        any = true;
      }

      if (any) {
        this.playSound(SoundEvents.EVOKER_PREPARE_ATTACK, 1.2F, 1.3F);
      }
    }

    if (this.kind() == 2) {
      if (this.boss != null) {
        this.boss.setProgress(this.getHealth() / this.getMaxHealth());
      }

      if (t != null && --this.flareCooldown <= 0 && this.distanceToSqr(t) < 25.0) {
        this.flareCooldown = 140 + this.random.nextInt(40);
        this.solarFlare();
      }
    }
  }

  int solarFlare() {
    if (!(this.level() instanceof ServerLevel l)) {
      return -1;
    } else {
      int var6 = 0;
      l.sendParticles(
          ParticleTypes.FLAME,
          this.getX(),
          this.getY() + 1.0,
          this.getZ(),
          120,
          2.5,
          0.6,
          2.5,
          0.08);
      l.sendParticles(
          ParticleTypes.END_ROD,
          this.getX(),
          this.getY() + 1.5,
          this.getZ(),
          40,
          1.5,
          1.0,
          1.5,
          0.05);
      this.playSound(SoundEvents.BLAZE_SHOOT, 2.0F, 0.6F);
      this.playSound((SoundEvent) SoundEvents.GENERIC_EXPLODE.value(), 0.8F, 1.4F);

      for (LivingEntity e :
          l.getEntitiesOfClass(
              LivingEntity.class,
              this.getBoundingBox().inflate(5.0),
              ex -> ex != this && !(ex instanceof Dawnbringer) && ex.isAlive())) {
        e.hurt(this.damageSources().mobAttack(this), 8.0F);
        e.igniteForSeconds(4.0F);
        Vec3 push =
            e.position().subtract(this.position()).multiply(1.0, 0.0, 1.0).normalize().scale(1.4);
        e.push(push.x, 0.5, push.z);
        e.hurtMarked = true;
        var6++;
      }

      return var6;
    }
  }

  public void startSeenByPlayer(ServerPlayer p) {
    super.startSeenByPlayer(p);
    if (this.boss != null) {
      this.boss.addPlayer(p);
    }
  }

  public void stopSeenByPlayer(ServerPlayer p) {
    super.stopSeenByPlayer(p);
    if (this.boss != null) {
      this.boss.removePlayer(p);
    }
  }

  public void setCustomName(Component c) {
    super.setCustomName(c);
    if (this.boss != null && c != null) {
      this.boss.setName(c);
    }
  }

  public void readAdditionalSaveData(CompoundTag t) {
    super.readAdditionalSaveData(t);
    if (this.boss != null && this.hasCustomName()) {
      this.boss.setName(this.getDisplayName());
    }
  }

  protected SoundEvent getAmbientSound() {
    return this.kind() == 1 ? SoundEvents.EVOKER_AMBIENT : SoundEvents.PILLAGER_AMBIENT;
  }

  protected SoundEvent getHurtSound(DamageSource d) {
    return this.kind() == 1 ? SoundEvents.EVOKER_HURT : SoundEvents.VINDICATOR_HURT;
  }

  protected SoundEvent getDeathSound() {
    return this.kind() == 2
        ? SoundEvents.RAVAGER_DEATH
        : (this.kind() == 1 ? SoundEvents.EVOKER_DEATH : SoundEvents.VINDICATOR_DEATH);
  }

  protected void playStepSound(BlockPos p, BlockState s) {
    this.playSound(
        (SoundEvent) SoundEvents.ARMOR_EQUIP_IRON.value(), 0.25F, this.kind() == 2 ? 0.6F : 1.0F);
  }

  public float getVoicePitch() {
    return this.kind() == 2 ? 0.7F : super.getVoicePitch();
  }

  public boolean removeWhenFarAway(double d) {
    return false;
  }

  static final class LanceGoal extends Goal {
    final Dawnbringer m;
    int windup;

    LanceGoal(Dawnbringer m) {
      this.m = m;
      this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    public boolean canUse() {
      LivingEntity t = this.m.getTarget();
      return t != null
          && t.isAlive()
          && this.m.distanceToSqr(t) < 484.0
          && this.m.hasLineOfSight(t);
    }

    public boolean canContinueToUse() {
      return this.canUse();
    }

    public void stop() {
      this.m.entityData.set(Dawnbringer.CASTING, false);
      this.windup = 0;
    }

    public boolean requiresUpdateEveryTick() {
      return true;
    }

    public void tick() {
      LivingEntity t = this.m.getTarget();
      if (t != null) {
        this.m.getLookControl().setLookAt(t, 30.0F, 30.0F);
        double d = this.m.distanceToSqr(t);
        if (d > 196.0) {
          this.m.getNavigation().moveTo(t, 1.0);
        } else if (d < 36.0) {
          Vec3 away = this.m.position().subtract(t.position()).normalize().scale(4.0);
          this.m
              .getNavigation()
              .moveTo(this.m.getX() + away.x, this.m.getY(), this.m.getZ() + away.z, 1.1);
        } else {
          this.m.getNavigation().stop();
        }

        if (--this.m.lanceCooldown <= 0) {
          if (this.windup == 0) {
            this.windup = 14;
            this.m.entityData.set(Dawnbringer.CASTING, true);
            this.m.playSound(SoundEvents.EVOKER_CAST_SPELL, 1.0F, 1.4F);
          }

          if (--this.windup <= 0) {
            this.m.entityData.set(Dawnbringer.CASTING, false);
            this.m.lanceCooldown = 45 + this.m.random.nextInt(25);
            fire(this.m, t);
          }
        }
      }
    }

    static void fire(Dawnbringer m, LivingEntity t) {
      if (m.level() instanceof ServerLevel l) {
        Vec3 var12 = m.getEyePosition();
        Vec3 to = t.getEyePosition().add(0.0, -0.3, 0.0);
        BlockHitResult hit = l.clip(new ClipContext(var12, to, Block.COLLIDER, Fluid.NONE, m));
        boolean clear = hit.getType() == Type.MISS;
        Vec3 end = clear ? to : hit.getLocation();
        Vec3 step = end.subtract(var12);
        int n = (int) Math.max(4.0, step.length() * 3.0);

        for (int i = 0; i <= n; i++) {
          Vec3 p = var12.add(step.scale((double) i / n));
          l.sendParticles(
              i % 3 == 0 ? ParticleTypes.END_ROD : ParticleTypes.FLAME,
              p.x,
              p.y,
              p.z,
              1,
              0.0,
              0.0,
              0.0,
              0.0);
        }

        m.playSound(SoundEvents.BLAZE_SHOOT, 1.2F, 1.5F);
        if (clear) {
          t.hurt(m.damageSources().indirectMagic(m, m), 5.0F);
          t.igniteForSeconds(3.0F);
        }
      }
    }
  }
}
