/* Reconstructed from the user-provided 0.8.9.8 release with permission. */
package com.timelocked.client;

import com.timelocked.LabFluids;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(
    modid = "timelocked",
    bus = EventBusSubscriber.Bus.MOD,
    value = {Dist.CLIENT})
public final class LabFluidRendering {
  @SubscribeEvent
  public static void setup(FMLClientSetupEvent e) {
    e.enqueueWork(
        () -> {
          for (LabFluids.Spec s : new LabFluids.Spec[] {LabFluids.PORTAL, LabFluids.COOLANT}) {
            ItemBlockRenderTypes.setRenderLayer(
                (Fluid) ((Fluid) s.source.get()), (RenderType) RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(
                (Fluid) ((Fluid) s.flowing.get()), (RenderType) RenderType.translucent());
          }
        });
  }
}
