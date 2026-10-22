package com.timelocked.client;

import com.timelocked.UndergroundContent;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = "timelocked", bus = Bus.MOD, value = Dist.CLIENT)
public final class UndergroundClientEvents {
  @SubscribeEvent
  public static void screens(RegisterMenuScreensEvent e) {
    e.register((MenuType) UndergroundContent.FUEL_MENU.get(), ReactorFuelScreen::new);
  }
}
