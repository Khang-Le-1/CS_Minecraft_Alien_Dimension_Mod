package com.timelocked.client;

import com.mojang.blaze3d.platform.InputConstants.Type;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;

@EventBusSubscriber(modid = "timelocked", value = Dist.CLIENT, bus = Bus.MOD)
public final class WeaponKeys {
  public static final KeyMapping RELOAD =
      new KeyMapping(
          "key.timelocked.reload",
          KeyConflictContext.IN_GAME,
          Type.KEYSYM,
          82,
          "key.categories.timelocked");

  @SubscribeEvent
  public static void register(RegisterKeyMappingsEvent e) {
    e.register(RELOAD);
  }
}
