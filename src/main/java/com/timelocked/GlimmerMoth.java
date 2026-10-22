package com.timelocked;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class GlimmerMoth extends PathfinderMob implements FlyingAnimal {
  public GlimmerMoth(EntityType<? extends GlimmerMoth> t, Level l) {
    super(t, l);
    this.moveControl = new FlyingMoveControl(this, 20, true);
  }

  public static Builder attributes() {
    return Mob.createMobAttributes()
        .add(Attributes.MAX_HEALTH, 4.0)
        .add(Attributes.FLYING_SPEED, 0.5)
        .add(Attributes.MOVEMENT_SPEED, 0.25);
  }

  protected PathNavigation createNavigation(Level l) {
    FlyingPathNavigation n = new FlyingPathNavigation(this, l);
    n.setCanOpenDoors(false);
    n.setCanFloat(true);
    n.setCanPassDoors(true);
    return n;
  }

  protected void registerGoals() {
    this.goalSelector.addGoal(0, new FloatGoal(this));
    this.goalSelector.addGoal(1, new PanicGoal(this, 1.6));
    this.goalSelector.addGoal(3, new WaterAvoidingRandomFlyingGoal(this, 1.0));
    this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 6.0F));
  }

  public boolean isFlying() {
    return !this.onGround();
  }

  public boolean causeFallDamage(float d, float m, DamageSource s) {
    return false;
  }

  protected void checkFallDamage(double y, boolean g, BlockState s, BlockPos p) {}

  protected SoundEvent getAmbientSound() {
    return this.random.nextInt(3) == 0 ? SoundEvents.AMETHYST_BLOCK_CHIME : null;
  }

  protected SoundEvent getHurtSound(DamageSource d) {
    return SoundEvents.BAT_HURT;
  }

  protected SoundEvent getDeathSound() {
    return SoundEvents.AMETHYST_BLOCK_BREAK;
  }

  protected float getSoundVolume() {
    return 0.4F;
  }

  public void aiStep() {
    super.aiStep();
    if (!this.level().isClientSide && this.tickCount % 40 == 0) {
      VoidspaceEvents.survey(this, "glimmer_moth");
    }
  }
}
