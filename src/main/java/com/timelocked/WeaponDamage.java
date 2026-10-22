package com.timelocked;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.player.Player;

public final class WeaponDamage {
  public static final ResourceKey<DamageType> BALLISTIC =
      ResourceKey.create(
          Registries.DAMAGE_TYPE,
          ResourceLocation.fromNamespaceAndPath("timelocked", "sarc_ballistic"));
  public static final ResourceKey<DamageType> PULSE =
      ResourceKey.create(
          Registries.DAMAGE_TYPE,
          ResourceLocation.fromNamespaceAndPath("timelocked", "sarc_pulse"));

  public static DamageSource source(Player p, boolean energy) {
    return new DamageSource(
        p.registryAccess()
            .registryOrThrow(Registries.DAMAGE_TYPE)
            .getHolderOrThrow(energy ? PULSE : BALLISTIC),
        p,
        p,
        p.getEyePosition());
  }
}
