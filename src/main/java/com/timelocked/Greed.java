package com.timelocked;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * Greed: the heavy red and black collar Bionexian. Slow and armored; it takes, and heals a little
 * from every hit it lands.
 */
public final class Greed extends Makglo {
  public Greed(EntityType<? extends Makglo> type, Level level) {
    super(type, level);
    this.xpReward = 20;
  }

  public static Builder attributes() {
    return Monster.createMonsterAttributes()
        .add(Attributes.MAX_HEALTH, 42.0)
        .add(Attributes.MOVEMENT_SPEED, 0.21)
        .add(Attributes.ATTACK_DAMAGE, 6.0)
        .add(Attributes.ARMOR, 4.0)
        .add(Attributes.KNOCKBACK_RESISTANCE, 0.3)
        .add(Attributes.FOLLOW_RANGE, 40.0);
  }

  @Override
  public boolean doHurtTarget(Entity target) {
    boolean hit = super.doHurtTarget(target);
    if (hit && !this.level().isClientSide) {
      this.heal(2.0F);
    }

    return hit;
  }
}
