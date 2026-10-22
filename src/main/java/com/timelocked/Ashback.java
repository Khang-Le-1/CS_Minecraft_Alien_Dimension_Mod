package com.timelocked;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class Ashback extends Animal {
  public Ashback(EntityType<? extends Ashback> t, Level l) {
    super(t, l);
  }

  public static Builder attributes() {
    return Animal.createMobAttributes()
        .add(Attributes.MAX_HEALTH, 24.0)
        .add(Attributes.MOVEMENT_SPEED, 0.2)
        .add(Attributes.ARMOR, 4.0)
        .add(Attributes.KNOCKBACK_RESISTANCE, 0.3);
  }

  protected void registerGoals() {
    this.goalSelector.addGoal(0, new FloatGoal(this));
    this.goalSelector.addGoal(1, new PanicGoal(this, 1.6));
    this.goalSelector.addGoal(2, new BreedGoal(this, 1.0));
    this.goalSelector.addGoal(
        3,
        new TemptGoal(
            this,
            1.15,
            Ingredient.of(new ItemLike[] {(ItemLike) VoidspaceContent.STAR_BLOOM.get()}),
            false));
    this.goalSelector.addGoal(4, new FollowParentGoal(this, 1.1));
    this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
    this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
    this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
  }

  public boolean isFood(ItemStack s) {
    return s.is(((AlienPlantBlock) VoidspaceContent.STAR_BLOOM.get()).asItem());
  }

  public AgeableMob getBreedOffspring(ServerLevel l, AgeableMob other) {
    return (AgeableMob) ((EntityType) VoidspaceContent.ASHBACK.get()).create(l);
  }

  protected SoundEvent getAmbientSound() {
    return SoundEvents.CAMEL_AMBIENT;
  }

  protected SoundEvent getHurtSound(DamageSource d) {
    return SoundEvents.CAMEL_HURT;
  }

  protected SoundEvent getDeathSound() {
    return SoundEvents.CAMEL_DEATH;
  }

  protected void playStepSound(BlockPos p, BlockState s) {
    this.playSound(SoundEvents.CAMEL_STEP, 0.25F, 0.8F);
  }

  public float getVoicePitch() {
    return 0.65F + this.random.nextFloat() * 0.1F;
  }

  public void aiStep() {
    super.aiStep();
    if (!this.level().isClientSide && this.tickCount % 40 == 0) {
      VoidspaceEvents.survey(this, "ashback");
    }
  }
}
