package com.timelocked;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** User-supplied truck recordings. Door one-shots are server-authoritative. */
public final class TruckSounds {
  public static final DeferredRegister<SoundEvent> SOUNDS =
      DeferredRegister.create(Registries.SOUND_EVENT, "timelocked");
  public static final DeferredHolder<SoundEvent, SoundEvent> IN_LEFT = register("in_left");
  public static final DeferredHolder<SoundEvent, SoundEvent> IN_RIGHT = register("in_right");
  public static final DeferredHolder<SoundEvent, SoundEvent> OUT_LEFT = register("out_left");
  public static final DeferredHolder<SoundEvent, SoundEvent> OUT_RIGHT = register("out_right");
  public static final DeferredHolder<SoundEvent, SoundEvent> IDLE = register("idle");
  public static final DeferredHolder<SoundEvent, SoundEvent> DRIVE = register("drive");

  private static DeferredHolder<SoundEvent, SoundEvent> register(String suffix) {
    String name = "truck_" + suffix;
    return SOUNDS.register(
        name,
        () ->
            SoundEvent.createVariableRangeEvent(
                ResourceLocation.fromNamespaceAndPath("timelocked", name)));
  }

  /** Left/right is relative to truck heading, never a random sound choice. */
  public static boolean leftSide(float yaw, double dx, double dz) {
    double angle = Math.toRadians(yaw);
    return Math.cos(angle) * dx + Math.sin(angle) * dz >= 0;
  }

  /** Idle at rest, driving at speed, with the same mix when reversing. */
  public static float driveMix(float speed) {
    return Float.isFinite(speed) ? Mth.clamp((Math.abs(speed) - .015F) / .13F, 0, 1) : 0;
  }

  static void door(SarcGroundVehicle truck, boolean entering, boolean left) {
    if (!truck.truck() || truck.level().isClientSide) return;
    SoundEvent clip =
        entering
            ? (left ? IN_LEFT.get() : IN_RIGHT.get())
            : (left ? OUT_LEFT.get() : OUT_RIGHT.get());
    truck
        .level()
        .playSound(
            null, truck.getX(), truck.getY() + .8, truck.getZ(), clip, SoundSource.NEUTRAL, .8F, 1);
  }

  private TruckSounds() {}
}
