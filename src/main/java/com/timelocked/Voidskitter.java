package com.timelocked;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class Voidskitter extends Monster {
  public Voidskitter(EntityType<? extends Voidskitter> t, Level l) {
    super(t, l);
    this.xpReward = 5;
  }

  public static Builder attributes() {
    return Monster.createMonsterAttributes()
        .add(Attributes.MAX_HEALTH, 10.0)
        .add(Attributes.MOVEMENT_SPEED, 0.32)
        .add(Attributes.ATTACK_DAMAGE, 3.0)
        .add(Attributes.FOLLOW_RANGE, 24.0);
  }

  protected void registerGoals() {
    this.goalSelector.addGoal(0, new FloatGoal(this));
    this.goalSelector.addGoal(2, new LeapAtTargetGoal(this, 0.35F));
    this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.2, false));
    this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
    this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0F));
    this.targetSelector.addGoal(1, new HurtByTargetGoal(this, new Class[0]));
    this.targetSelector.addGoal(2, new NearestAttackableTargetGoal(this, Player.class, true));
  }

  protected SoundEvent getAmbientSound() {
    return SoundEvents.SILVERFISH_AMBIENT;
  }

  protected SoundEvent getHurtSound(DamageSource d) {
    return SoundEvents.AMETHYST_CLUSTER_HIT;
  }

  protected SoundEvent getDeathSound() {
    return SoundEvents.AMETHYST_CLUSTER_BREAK;
  }

  protected void playStepSound(BlockPos p, BlockState s) {
    this.playSound(SoundEvents.SPIDER_STEP, 0.15F, 1.6F);
  }

  public void aiStep() {
    super.aiStep();
    if (!this.level().isClientSide && this.tickCount % 40 == 0) {
      VoidspaceEvents.survey(this, "voidskitter");
    }
  }
}
