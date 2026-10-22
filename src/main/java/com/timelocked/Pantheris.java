package com.timelocked;

import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.level.Level;

/** Scripted, non-hostile boat cameo. No natural spawns, dialogue or trading. */
public final class Pantheris extends PathfinderMob {
  public Pantheris(EntityType<? extends Pantheris> t, Level l) {
    super(t, l);
    setNoAi(true);
    setPersistenceRequired();
    setInvulnerable(true);
    setSilent(true);
  }

  public static AttributeSupplier.Builder attributes() {
    return Mob.createMobAttributes()
        .add(Attributes.MAX_HEALTH, 20)
        .add(Attributes.MOVEMENT_SPEED, 0);
  }

  @Override
  public boolean shouldBeSaved() {
    return false;
  }

  @Override
  public boolean canBeLeashed() {
    return false;
  }
}
