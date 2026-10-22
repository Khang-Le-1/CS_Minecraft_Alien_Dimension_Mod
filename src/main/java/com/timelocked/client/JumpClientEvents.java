package com.timelocked.client;

import com.timelocked.JumpClientBridge;
import com.timelocked.JumpTimeline;
import com.timelocked.Timelocked;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

@EventBusSubscriber(modid = Timelocked.ID, value = Dist.CLIENT)
public final class JumpClientEvents {
  @SubscribeEvent
  public static void tick(ClientTickEvent.Post e) {
    JumpCutsceneClient.tick();
  }

  @SubscribeEvent
  public static void leave(ClientPlayerNetworkEvent.LoggingOut e) {
    JumpCutsceneClient.end();
  }

  @SubscribeEvent
  public static void fov(ViewportEvent.ComputeFov e) {
    if (JumpCutsceneClient.active()) {
      e.setFOV(JumpTimeline.camera(JumpCutsceneClient.age(0.0F)).fov());
    }
  }

  @SubscribeEvent
  public static void interact(InputEvent.InteractionKeyMappingTriggered e) {
    if (JumpCutsceneClient.active()) {
      e.setCanceled(true);
      e.setSwingHand(false);
    }
  }

  @EventBusSubscriber(modid = Timelocked.ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
  public static final class Setup {
    @SubscribeEvent
    public static void setup(FMLClientSetupEvent e) {
      e.enqueueWork(
          () -> {
            JumpClientBridge.start = JumpCutsceneClient::begin;
            JumpClientBridge.stop = JumpCutsceneClient::stop;
            JumpClientBridge.navigation =
                d -> {
                  Minecraft mc = Minecraft.getInstance();
                  if (mc.player != null && !JumpCutsceneClient.active()) {
                    mc.setScreen(new JumpNavigationScreen(d));
                  }
                };
            JumpClientBridge.notice =
                m -> {
                  Minecraft mc = Minecraft.getInstance();
                  if (mc.screen instanceof JumpNavigationScreen s) s.notice(m);
                };
          });
    }

    private Setup() {}
  }

  private JumpClientEvents() {}
}
