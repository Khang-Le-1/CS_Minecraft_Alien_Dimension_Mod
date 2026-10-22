package com.timelocked.client;

import com.timelocked.MetalContent;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(modid = "timelocked", value = Dist.CLIENT, bus = Bus.MOD)
public final class IndustryClientEvents {
  @SubscribeEvent
  public static void setup(FMLClientSetupEvent e) {
    e.enqueueWork(
        () ->
            ItemBlockRenderTypes.setRenderLayer(
                (Block) MetalContent.VIEWPORT.get(), RenderType.translucent()));
  }
}
