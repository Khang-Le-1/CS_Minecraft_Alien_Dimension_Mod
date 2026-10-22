/* Reconstructed from the user-provided 0.8.9.8 release with permission. */
package com.timelocked.client;

import com.timelocked.Content;
import com.timelocked.FacilityDetails;
import com.timelocked.LabFluids;
import com.timelocked.MonitorContent;
import com.timelocked.SarcContent;
import com.timelocked.SpaceContent;
import com.timelocked.UndergroundContent;
import com.timelocked.VoidspaceContent;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.fluids.FluidType;
import org.joml.Vector3f;

@EventBusSubscriber(
    modid = "timelocked",
    bus = EventBusSubscriber.Bus.MOD,
    value = {Dist.CLIENT})
public final class ClientEvents {
  @SubscribeEvent
  public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
    e.registerEntityRenderer((EntityType) UndergroundContent.LIFT.get(), SarcLiftRenderer::new);
    e.registerBlockEntityRenderer(
        (BlockEntityType) FacilityDetails.DOOR_ENTITY.get(), SarcDoorRenderer::new);
    e.registerBlockEntityRenderer(
        (BlockEntityType) MonitorContent.MONITOR_ENTITY.get(), MonitorRenderer::new);
    e.registerBlockEntityRenderer(
        (BlockEntityType) SpaceContent.MODEL_ENTITY.get(), TimeskipperModelRenderer::new);
    e.registerBlockEntityRenderer(
        (BlockEntityType) UndergroundContent.CORE_ENTITY.get(), ReactorRenderers.Facility::new);
    e.registerBlockEntityRenderer(
        (BlockEntityType) SarcContent.CORE_ENTITY.get(), ReactorRenderers.Plant::new);
    e.registerBlockEntityRenderer(
        (BlockEntityType) FacilityDetails.PORTAL_ENTITY.get(), PortalFrameRenderer::new);
    e.registerBlockEntityRenderer(
        (BlockEntityType) FacilityDetails.ALARM_ROTOR_ENTITY.get(), AlarmRotorRenderer::new);
    e.registerEntityRenderer(
        Content.INTRO_BOAT.get(),
        c -> new net.minecraft.client.renderer.entity.BoatRenderer(c, false));
    e.registerEntityRenderer(Content.PANTHERIS.get(), PantherisRenderer::new);
    e.registerEntityRenderer((EntityType) Content.MAKGLO.get(), MakgloRenderer::new);
    e.registerEntityRenderer((EntityType) Content.UNNAMED.get(), BionexianRenderer::unnamed);
    e.registerEntityRenderer((EntityType) Content.GREED.get(), BionexianRenderer::greed);
    e.registerEntityRenderer(
        (EntityType) VoidspaceContent.ASHBACK.get(),
        c ->
            new AlienMobRenderer(
                c, AlienMobRenderer.ASHBACK, AlienMobModel.Kind.ASHBACK, "ashback", 0.9f, 1.0f));
    e.registerEntityRenderer(
        (EntityType) VoidspaceContent.SKITTER.get(),
        c ->
            new AlienMobRenderer(
                c,
                AlienMobRenderer.SKITTER,
                AlienMobModel.Kind.SKITTER,
                "voidskitter",
                0.5f,
                1.0f));
    e.registerEntityRenderer(
        (EntityType) VoidspaceContent.MOTH.get(),
        c ->
            new AlienMobRenderer(
                c, AlienMobRenderer.MOTH, AlienMobModel.Kind.MOTH, "glimmer_moth", 0.2f, 1.0f));
    e.registerEntityRenderer((EntityType) VoidspaceContent.CELESTIAL.get(), CelestialRenderer::new);
    e.registerEntityRenderer(
        (EntityType) VoidspaceContent.DAWN_LEGIONNAIRE.get(), DawnbringerRenderer::new);
    e.registerEntityRenderer(
        (EntityType) VoidspaceContent.DAWN_SUNSPEAKER.get(), DawnbringerRenderer::new);
    e.registerEntityRenderer(
        (EntityType) VoidspaceContent.DAWN_WARLORD.get(), DawnbringerRenderer::new);
    e.registerEntityRenderer(
        (EntityType) VoidspaceContent.GLOWKIT.get(),
        c ->
            new AlienMobRenderer(
                c, AlienMobRenderer.GLOWKIT, AlienMobModel.Kind.GLOWKIT, "glowkit", 0.4f, 1.0f));
    e.registerEntityRenderer(
        (EntityType) Content.ROVER.get(),
        c -> new SarcVehicleRenderer(c, SarcVehicleRenderer.ROVER, "rover"));
    e.registerEntityRenderer(
        (EntityType) Content.TRUCK.get(),
        c -> new SarcVehicleRenderer(c, SarcVehicleRenderer.TRUCK, "truck"));
    e.registerEntityRenderer(
        (EntityType) Content.TRAM_VEHICLE.get(),
        c -> new SarcVehicleRenderer(c, SarcVehicleRenderer.TRAM, "tram"));
  }

  @SubscribeEvent
  public static void layers(EntityRenderersEvent.RegisterLayerDefinitions e) {
    AtlasArmorModels.registerLayers((EntityRenderersEvent.RegisterLayerDefinitions) e);
    e.registerLayerDefinition(MakgloRenderer.LAYER, MakgloModel::layer);
    e.registerLayerDefinition(BionexianRenderer.UNNAMED_LAYER, BionexianModel::unnamed);
    e.registerLayerDefinition(BionexianRenderer.GREED_LAYER, BionexianModel::greed);
    e.registerLayerDefinition(AlienMobRenderer.ASHBACK, AlienMobLayers::ashback);
    e.registerLayerDefinition(AlienMobRenderer.SKITTER, AlienMobLayers::voidskitter);
    e.registerLayerDefinition(AlienMobRenderer.MOTH, AlienMobLayers::glimmerMoth);
    e.registerLayerDefinition(AlienMobRenderer.GLOWKIT, AlienMobLayers::glowkit);
    e.registerLayerDefinition(CelestialRenderer.LAYER, CelestialLayers::celestial);
    e.registerLayerDefinition(DawnbringerRenderer.LAYER, DawnbringerLayers::dawnbringer);
    e.registerLayerDefinition(SarcVehicleRenderer.ROVER, VehicleModels::roverLayer);
    e.registerLayerDefinition(SarcVehicleRenderer.TRUCK, VehicleModels::truckLayer);
    e.registerLayerDefinition(SarcVehicleRenderer.TRAM, VehicleModels::tramLayer);
  }

  @SubscribeEvent
  public static void atlasExtensions(RegisterClientExtensionsEvent e) {
    AtlasArmorModels.registerItems((RegisterClientExtensionsEvent) e);
    for (LabFluids.Spec spec : new LabFluids.Spec[] {LabFluids.PORTAL, LabFluids.COOLANT}) {
      final String id = spec.type.getId().getPath();
      e.registerFluidType(
          new IClientFluidTypeExtensions() {

            public ResourceLocation getStillTexture() {
              return ResourceLocation.fromNamespaceAndPath(
                  (String) "timelocked", (String) ("block/" + id + "_still"));
            }

            public ResourceLocation getFlowingTexture() {
              return ResourceLocation.fromNamespaceAndPath(
                  (String) "timelocked", (String) ("block/" + id + "_flow"));
            }

            public Vector3f modifyFogColor(
                Camera camera,
                float partial,
                ClientLevel level,
                int distance,
                float darkness,
                Vector3f color) {
              return id.equals("portal_condensate")
                  ? new Vector3f(0.25f, 0.06f, 0.38f)
                  : new Vector3f(0.02f, 0.23f, 0.26f);
            }
          },
          new FluidType[] {(FluidType) spec.type.get()});
    }
  }

  @SubscribeEvent
  public static void reloadAtlas(EntityRenderersEvent.AddLayers e) {
    AtlasArmorModels.clear();
  }
}
