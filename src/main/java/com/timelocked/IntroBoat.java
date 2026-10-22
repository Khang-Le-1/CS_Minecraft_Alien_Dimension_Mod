package com.timelocked;

import net.minecraft.world.entity.*;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.Level;

/** Cinematic boat only: cannot be saved or boarded by players. */
public final class IntroBoat extends Boat {
  public IntroBoat(EntityType<? extends IntroBoat> t, Level l) {
    super(t, l);
    setVariant(Type.DARK_OAK);
    setNoGravity(true);
    setInvulnerable(true);
    setSilent(true);
  }

  @Override
  public boolean shouldBeSaved() {
    return false;
  }

  @Override
  protected boolean canAddPassenger(Entity e) {
    return e instanceof Pantheris && getPassengers().isEmpty();
  }
}
