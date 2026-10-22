package com.timelocked;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class Glowkit extends Animal {
  public Glowkit(EntityType<? extends Glowkit> t, Level l) {
    super(t, l);
  }

  public static Builder attributes() {
    return Animal.createMobAttributes()
        .add(Attributes.MAX_HEALTH, 10.0)
        .add(Attributes.MOVEMENT_SPEED, 0.27);
  }

  private static Ingredient food() {
    return Ingredient.of(
        new ItemLike[] {(ItemLike) VoidspaceContent.STAR_BLOOM.get(), Items.GLOW_BERRIES});
  }

  protected void registerGoals() {
    this.goalSelector.addGoal(0, new FloatGoal(this));
    this.goalSelector.addGoal(1, new PanicGoal(this, 1.5));
    this.goalSelector.addGoal(2, new AvoidEntityGoal(this, Voidskitter.class, 8.0F, 1.2, 1.5));
    this.goalSelector.addGoal(3, new BreedGoal(this, 1.0));
    this.goalSelector.addGoal(4, new TemptGoal(this, 1.1, food(), false));
    this.goalSelector.addGoal(5, new FollowParentGoal(this, 1.1));
    this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.9));
    this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
    this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
  }

  public boolean isFood(ItemStack s) {
    return food().test(s);
  }

  public AgeableMob getBreedOffspring(ServerLevel l, AgeableMob other) {
    return (AgeableMob) ((EntityType) VoidspaceContent.GLOWKIT.get()).create(l);
  }

  protected SoundEvent getAmbientSound() {
    return this.random.nextInt(3) == 0 ? SoundEvents.CAT_PURR : SoundEvents.CAT_AMBIENT;
  }

  protected SoundEvent getHurtSound(DamageSource d) {
    return SoundEvents.SLIME_HURT_SMALL;
  }

  protected SoundEvent getDeathSound() {
    return SoundEvents.SLIME_DEATH_SMALL;
  }

  protected void playStepSound(BlockPos p, BlockState s) {
    this.playSound(SoundEvents.SLIME_SQUISH_SMALL, 0.12F, 1.4F);
  }

  public float getVoicePitch() {
    return 1.35F + this.random.nextFloat() * 0.15F;
  }

  public void aiStep() {
    super.aiStep();
    if (this.level().isClientSide) {
      if (this.random.nextInt(14) == 0) {
        this.level()
            .addParticle(
                this.random.nextBoolean() ? ParticleTypes.GLOW : ParticleTypes.DRIPPING_WATER,
                this.getRandomX(0.5),
                this.getY() + this.getBbHeight() * (0.3 + this.random.nextDouble() * 0.6),
                this.getRandomZ(0.5),
                0.0,
                0.0,
                0.0);
      }
    } else if (this.tickCount % 40 == 0) {
      VoidspaceEvents.survey(this, "glowkit");
    }
  }
}
