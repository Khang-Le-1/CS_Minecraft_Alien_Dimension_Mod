package com.timelocked;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class Makglo extends Monster {
  public BlockPos siteOrigin;

  public Makglo(EntityType<? extends Makglo> type, Level level) {
    super(type, level);
    this.xpReward = 12;
  }

  public static Builder attributes() {
    return Monster.createMonsterAttributes()
        .add(Attributes.MAX_HEALTH, 30.0)
        .add(Attributes.MOVEMENT_SPEED, 0.24)
        .add(Attributes.ATTACK_DAMAGE, 4.0)
        .add(Attributes.ARMOR, 2.0)
        .add(Attributes.FOLLOW_RANGE, 40.0);
  }

  protected void registerGoals() {
    this.goalSelector.addGoal(0, new FloatGoal(this));
    this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
    this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6));
    this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0F));
    this.targetSelector.addGoal(1, new HurtByTargetGoal(this, new Class[0]));
    this.targetSelector.addGoal(2, new NearestAttackableTargetGoal(this, Player.class, true));
  }

  public void addAdditionalSaveData(CompoundTag t) {
    super.addAdditionalSaveData(t);
    if (this.siteOrigin != null) {
      t.putLong("TimelockedSite", this.siteOrigin.asLong());
    }
  }

  public void readAdditionalSaveData(CompoundTag t) {
    super.readAdditionalSaveData(t);
    this.siteOrigin =
        t.contains("TimelockedSite") ? BlockPos.of(t.getLong("TimelockedSite")) : null;
  }

  protected boolean shouldDespawnInPeaceful() {
    return false;
  }

  public void tick() {
    if (!this.level().isClientSide) {
      this.setNoAi(this.level().getDifficulty() == Difficulty.PEACEFUL);
    }

    super.tick();
  }

  public void die(DamageSource source) {
    super.die(source);
    if (!this.level().isClientSide && this.siteOrigin != null) {
      EncounterManager.defeated((ServerLevel) this.level(), this.siteOrigin, this.getUUID());
    }
  }
}
