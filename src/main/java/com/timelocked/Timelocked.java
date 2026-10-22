/* Reconstructed from the user-provided 0.8.9.8 release with permission. */
package com.timelocked;

import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(value = "timelocked")
public final class Timelocked {
  public static final String ID = "timelocked";

  public Timelocked(IEventBus bus) {
    StoryMode.initialize();
    Content.initialize();
    CoastSigns.initialize();
    FacilityArchitecture.initialize();
    AlarmSounds.SOUNDS.register(bus);
    TruckSounds.SOUNDS.register(bus);
    AtlasStartupSounds.SOUNDS.register(bus);
    PushBarDoorSounds.SOUNDS.register(bus);
    UndergroundContent.initialize((IEventBus) bus);
    FacilityDetails.initialize();
    LabContent.initialize();
    LabFluids.initialize(bus);
    MetalContent.initialize();
    FacilityDetails.initialize();
    AtlasContent.initialize();
    FacilityDetails.initialize();
    WorkshopContent.initialize((IEventBus) bus);
    VoidspaceContent.initialize((IEventBus) bus);
    SarcContent.initialize((IEventBus) bus);
    SpaceContent.initialize((IEventBus) bus);
    ShipInteriorContent.initialize();
    MonitorContent.initialize();
    CampusDoors.initialize();
    CampusFurnishings.initialize();
    SccPhotoFixtures.initialize();
    SarcAmbientLighting.initialize();
    FieldShelter.initialize();
    MetalContent.ARMOR_MATERIALS.register(bus);
    bus.addListener(MetalContent::capabilities);
    Content.CHUNK_GENERATORS.register(bus);
    Content.BIOME_SOURCES.register(bus);
    Content.BLOCKS.register(bus);
    Content.ITEMS.register(bus);
    Content.BLOCK_ENTITIES.register(bus);
    Content.TABS.register(bus);
    Content.ENTITIES.register(bus);
    Content.FEATURES.register(bus);
    EncounterManager.register();
    JumpSequence.register();
    bus.addListener(PortalTransitNetworking::register);
    bus.addListener(IntroNetworking::register);
    bus.addListener(ObjectiveNetworking::register);
    bus.addListener(BattleMusicNetworking::register);
    bus.addListener(
        (net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent e) -> {
          e.put(Content.PANTHERIS.get(), Pantheris.attributes().build());
          e.put((EntityType) Content.MAKGLO.get(), Makglo.attributes().build());
          e.put((EntityType) Content.UNNAMED.get(), Unnamed.attributes().build());
          e.put((EntityType) Content.GREED.get(), Greed.attributes().build());
        });
    bus.addListener(Content::capabilities);
    SarcSitePlacement.register();
    SatelliteCrash.register();
    UndergroundSystems.register();
    VehicleEvents.register();
    bus.addListener(VehicleNetworking::register);
    WeaponEvents.register();
    bus.addListener(WeaponNetworking::register);
  }
}
