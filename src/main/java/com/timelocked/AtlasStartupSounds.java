package com.timelocked;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class AtlasStartupSounds {
  public static final DeferredRegister<SoundEvent> SOUNDS =
      DeferredRegister.create(Registries.SOUND_EVENT, "timelocked");
  public static final DeferredHolder<SoundEvent, SoundEvent> BOOT =
      SOUNDS.register(
          "atlas_startup",
          () ->
              SoundEvent.createVariableRangeEvent(
                  ResourceLocation.fromNamespaceAndPath("timelocked", "atlas_startup")));

  private AtlasStartupSounds() {}
}
