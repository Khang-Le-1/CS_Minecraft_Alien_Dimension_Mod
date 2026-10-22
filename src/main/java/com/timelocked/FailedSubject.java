package com.timelocked;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public final class FailedSubject extends Monster {
  public FailedSubject(EntityType<? extends FailedSubject> t, Level l) {
    super(t, l);
    this.xpReward = 8;
  }

  public static Builder attributes() {
    return Monster.createMonsterAttributes()
        .add(Attributes.MAX_HEALTH, 26.0)
        .add(Attributes.MOVEMENT_SPEED, 0.27)
        .add(Attributes.ATTACK_DAMAGE, 5.0)
        .add(Attributes.FOLLOW_RANGE, 28.0)
        .add(Attributes.ARMOR, 2.0);
  }

  protected void registerGoals() {
    this.goalSelector.addGoal(0, new FloatGoal(this));
    this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, false));
    this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
    this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 10.0F));
    this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    this.targetSelector.addGoal(1, new HurtByTargetGoal(this, new Class[0]));
    this.targetSelector.addGoal(2, new NearestAttackableTargetGoal(this, Player.class, true));
    this.targetSelector.addGoal(
        3, new NearestAttackableTargetGoal(this, SarcScientist.class, true));
  }

  public boolean doHurtTarget(Entity e) {
    boolean hit = super.doHurtTarget(e);
    if (hit
        && e instanceof LivingEntity le
        && this.level().getMaxLocalRawBrightness(this.blockPosition()) < 6) {
      le.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0));
    }

    return hit;
  }

  protected SoundEvent getAmbientSound() {
    return SoundEvents.ZOMBIE_VILLAGER_AMBIENT;
  }

  protected SoundEvent getHurtSound(DamageSource d) {
    return SoundEvents.ZOMBIE_VILLAGER_HURT;
  }

  protected SoundEvent getDeathSound() {
    return SoundEvents.ZOMBIE_VILLAGER_DEATH;
  }

  public float getVoicePitch() {
    return 0.65F + this.random.nextFloat() * 0.15F;
  }
}
