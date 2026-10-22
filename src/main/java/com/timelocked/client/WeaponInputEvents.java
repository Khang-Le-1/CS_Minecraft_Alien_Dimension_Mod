package com.timelocked.client;

import com.timelocked.GunItem;
import com.timelocked.WeaponNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = "timelocked", value = Dist.CLIENT)
public final class WeaponInputEvents {
  @SubscribeEvent
  public static void tick(Post e) {
    Minecraft mc = Minecraft.getInstance();

    while (WeaponKeys.RELOAD.consumeClick()) {
      if (mc.player != null && mc.screen == null) {
        if (mc.player.getMainHandItem().getItem() instanceof GunItem) {
          PacketDistributor.sendToServer(
              new WeaponNetworking.Reload(false), new CustomPacketPayload[0]);
        } else if (mc.player.getOffhandItem().getItem() instanceof GunItem) {
          PacketDistributor.sendToServer(
              new WeaponNetworking.Reload(true), new CustomPacketPayload[0]);
        }
      }
    }
  }
}
