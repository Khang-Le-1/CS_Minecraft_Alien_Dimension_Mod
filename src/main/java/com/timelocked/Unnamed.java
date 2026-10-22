package com.timelocked;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * Unnamed: the split black and white stitched Bionexian. Fast and fragile; its hits can leave you
 * disoriented.
 */
public final class Unnamed extends Makglo {
  public Unnamed(EntityType<? extends Makglo> type, Level level) {
    super(type, level);
    this.xpReward = 14;
  }

  public static Builder attributes() {
    return Monster.createMonsterAttributes()
        .add(Attributes.MAX_HEALTH, 26.0)
        .add(Attributes.MOVEMENT_SPEED, 0.30)
        .add(Attributes.ATTACK_DAMAGE, 5.0)
        .add(Attributes.ARMOR, 0.0)
        .add(Attributes.FOLLOW_RANGE, 40.0);
  }

  @Override
  public boolean doHurtTarget(Entity target) {
    boolean hit = super.doHurtTarget(target);
    if (hit
        && !this.level().isClientSide
        && target instanceof LivingEntity living
        && this.random.nextFloat() < 0.3F) {
      living.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100));
    }

    return hit;
  }
}
