/* Reconstructed from the user-provided 0.8.9.8 release with permission. */
package com.timelocked;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class AlarmSounds {
  public static final DeferredRegister<SoundEvent> SOUNDS =
      DeferredRegister.create((ResourceKey) Registries.SOUND_EVENT, (String) "timelocked");
  public static final DeferredHolder<SoundEvent, SoundEvent> SIREN =
      SOUNDS.register(
          "facility_alarm",
          () ->
              SoundEvent.createVariableRangeEvent(
                  (ResourceLocation)
                      ResourceLocation.fromNamespaceAndPath(
                          (String) "timelocked", (String) "facility_alarm")));
  public static final DeferredHolder<SoundEvent, SoundEvent> FACILITY_DANGER =
      SOUNDS.register(
          "facility_danger_alarm",
          () ->
              SoundEvent.createVariableRangeEvent(
                  (ResourceLocation)
                      ResourceLocation.fromNamespaceAndPath(
                          (String) "timelocked", (String) "facility_danger_alarm")));
  public static final DeferredHolder<SoundEvent, SoundEvent> CORE_MELT =
      SOUNDS.register(
          "core_melt_alarm",
          () ->
              SoundEvent.createVariableRangeEvent(
                  (ResourceLocation)
                      ResourceLocation.fromNamespaceAndPath(
                          (String) "timelocked", (String) "core_melt_alarm")));
  public static final DeferredHolder<SoundEvent, SoundEvent> MENU_DEPTHS =
      SOUNDS.register(
          "menu_depths",
          () ->
              SoundEvent.createVariableRangeEvent(
                  (ResourceLocation)
                      ResourceLocation.fromNamespaceAndPath(
                          (String) "timelocked", (String) "menu_depths")));
  public static final DeferredHolder<SoundEvent, SoundEvent> POLARIS =
      SOUNDS.register(
          "polaris",
          () ->
              SoundEvent.createVariableRangeEvent(
                  (ResourceLocation)
                      ResourceLocation.fromNamespaceAndPath(
                          (String) "timelocked", (String) "polaris")));
  public static final DeferredHolder<SoundEvent, SoundEvent> BIONEXIAN_BATTLE =
      SOUNDS.register(
          "bionexian_battle",
          () ->
              SoundEvent.createVariableRangeEvent(
                  (ResourceLocation)
                      ResourceLocation.fromNamespaceAndPath(
                          (String) "timelocked", (String) "bionexian_battle")));
  public static final DeferredHolder<SoundEvent, SoundEvent> NAU_ANTHEM_PA =
      SOUNDS.register(
          "nau_anthem_pa",
          () ->
              SoundEvent.createVariableRangeEvent(
                  ResourceLocation.fromNamespaceAndPath("timelocked", "nau_anthem_pa")));
  public static final DeferredHolder<SoundEvent, SoundEvent> ARRIVAL_SWITCH =
      AlarmSounds.arrival("switch");
  public static final DeferredHolder<SoundEvent, SoundEvent> ARRIVAL_01 = AlarmSounds.arrival("01");
  public static final DeferredHolder<SoundEvent, SoundEvent> ARRIVAL_02 = AlarmSounds.arrival("02");
  public static final DeferredHolder<SoundEvent, SoundEvent> ARRIVAL_03 = AlarmSounds.arrival("03");
  public static final DeferredHolder<SoundEvent, SoundEvent> ARRIVAL_04 = AlarmSounds.arrival("04");

  public static final DeferredHolder<SoundEvent, SoundEvent> INTRO_TV =
      SOUNDS.register(
          "intro_tv",
          () ->
              SoundEvent.createVariableRangeEvent(
                  ResourceLocation.fromNamespaceAndPath("timelocked", "intro_tv")));
  public static final DeferredHolder<SoundEvent, SoundEvent> INTRO_RAIN =
      SOUNDS.register(
          "intro_rain",
          () ->
              SoundEvent.createVariableRangeEvent(
                  ResourceLocation.fromNamespaceAndPath("timelocked", "intro_rain")));
  public static final DeferredHolder<SoundEvent, SoundEvent> INTRO_TRANSITION =
      SOUNDS.register(
          "intro_transition",
          () ->
              SoundEvent.createVariableRangeEvent(
                  ResourceLocation.fromNamespaceAndPath("timelocked", "intro_transition")));
  public static final DeferredHolder<SoundEvent, SoundEvent> INTRO_SURF =
      SOUNDS.register(
          "intro_surf",
          () ->
              SoundEvent.createVariableRangeEvent(
                  ResourceLocation.fromNamespaceAndPath("timelocked", "intro_surf")));

  private static DeferredHolder<SoundEvent, SoundEvent> arrival(String id) {
    String name = "arrival_power_" + id;
    return SOUNDS.register(
        name,
        () ->
            SoundEvent.createVariableRangeEvent(
                (ResourceLocation)
                    ResourceLocation.fromNamespaceAndPath((String) "timelocked", (String) name)));
  }

  private AlarmSounds() {}
}
