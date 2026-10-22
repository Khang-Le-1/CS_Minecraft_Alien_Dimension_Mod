/* Reconstructed from the user-provided 0.8.9.8 release with permission. */
package com.timelocked;

import net.minecraft.core.Holder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class FacilityDetails {
  public static final DeferredBlock<StationLightBlock> LINEAR_LIGHT =
      Content.BLOCKS.register(
          "sarc_linear_light",
          () ->
              new StationLightBlock(
                  FacilityDetails.metal()
                      .noOcclusion()
                      .lightLevel(
                          s ->
                              (Boolean) s.getValue((Property) StationLightBlock.LIT) != false
                                  ? 15
                                  : 0)));
  public static final DeferredBlock<StationLightBlock> WARM_LIGHT =
      Content.BLOCKS.register(
          "sarc_warm_light",
          () ->
              new StationLightBlock(
                  FacilityDetails.metal()
                      .noOcclusion()
                      .lightLevel(
                          s ->
                              (Boolean) s.getValue((Property) StationLightBlock.LIT) != false
                                  ? 14
                                  : 0)));
  public static final DeferredBlock<PushBarDoorBlock> PUSH_BAR_DOOR =
      Content.BLOCKS.register(
          "sarc_push_bar_door", () -> new PushBarDoorBlock(FacilityDetails.metal().noOcclusion()));
  public static final DeferredBlock<AnimatedSarcDoorBlock> SERVICE_DOOR =
      Content.BLOCKS.register(
          "sarc_service_door",
          () -> new AnimatedSarcDoorBlock(FacilityDetails.metal().noOcclusion()));
  public static final DeferredBlock<ReactorAirlockBlock> REACTOR_AIRLOCK =
      Content.BLOCKS.register(
          "sarc_reactor_airlock",
          () ->
              new ReactorAirlockBlock(
                  FacilityDetails.metal().strength(6.0f, 120.0f).noOcclusion()));
  public static final DeferredBlock<Block> EMERGENCY =
      Content.BLOCKS.registerSimpleBlock(
          "sarc_emergency_strip", FacilityDetails.metal().lightLevel(s -> 12));
  public static final DeferredBlock<IronBarsBlock> RAILING =
      Content.BLOCKS.register(
          "sarc_safety_railing", () -> new IronBarsBlock(FacilityDetails.metal().noOcclusion()));
  public static final DeferredBlock<GratedWalkwayBlock> WALKWAY =
      Content.BLOCKS.register(
          "sarc_grated_walkway",
          () -> new GratedWalkwayBlock(FacilityDetails.metal().noOcclusion()));
  public static final DeferredBlock<AlarmBlock> ALARM_SIREN =
      Content.BLOCKS.register(
          "sarc_alarm_siren", () -> new AlarmBlock(FacilityDetails.metal().noOcclusion(), false));
  public static final DeferredBlock<AlarmBlock> ALARM_BEACON =
      Content.BLOCKS.register(
          "sarc_alarm_beacon",
          () ->
              new AlarmBlock(
                  FacilityDetails.metal()
                      .noOcclusion()
                      .lightLevel(
                          s -> (Boolean) s.getValue((Property) AlarmBlock.LIT) != false ? 10 : 0),
                  true));
  public static final DeferredBlock<FacingDecorBlock> WORKSTATION =
      Content.BLOCKS.register(
          "sarc_workstation", () -> new FacingDecorBlock(FacilityDetails.metal().noOcclusion(), 0));
  public static final DeferredBlock<FacingDecorBlock> STORAGE_SHELF =
      Content.BLOCKS.register(
          "sarc_storage_shelf",
          () -> new FacingDecorBlock(FacilityDetails.metal().noOcclusion(), 1));
  public static final DeferredBlock<PortalFrameBlock> FRAME =
      Content.BLOCKS.register(
          "sarc_portal_frame", () -> new PortalFrameBlock(FacilityDetails.metal().noOcclusion()));
  public static final DeferredBlock<PortalFrameAnchorBlock> ANCHOR =
      Content.BLOCKS.register(
          "sarc_portal_anchor",
          () -> new PortalFrameAnchorBlock(FacilityDetails.metal().noOcclusion()));
  public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SarcDoorEntity>>
      DOOR_ENTITY =
          Content.BLOCK_ENTITIES.register(
              "sarc_door",
              () ->
                  BlockEntityType.Builder.of(
                          SarcDoorEntity::new,
                          (Block[])
                              new Block[] {
                                (Block) SERVICE_DOOR.get(),
                                (Block) PUSH_BAR_DOOR.get(),
                                (Block) REACTOR_AIRLOCK.get(),
                                (Block) UndergroundContent.LIFT_DOOR.get(),
                                (Block) Content.ENTRANCE_DOOR.get(),
                                (Block) Content.AIRLOCK.get(),
                                (Block) Content.SECURITY_GATE.get()
                              })
                      .build(null));
  public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PortalFrameEntity>>
      PORTAL_ENTITY =
          Content.BLOCK_ENTITIES.register(
              "sarc_portal_frame",
              () ->
                  BlockEntityType.Builder.of(
                          PortalFrameEntity::new, (Block[]) new Block[] {(Block) ANCHOR.get()})
                      .build(null));
  public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AlarmRotorEntity>>
      ALARM_ROTOR_ENTITY =
          Content.BLOCK_ENTITIES.register(
              "alarm_rotor",
              () ->
                  BlockEntityType.Builder.of(
                          AlarmRotorEntity::new,
                          (Block[])
                              new Block[] {(Block) ALARM_BEACON.get(), (Block) ALARM_SIREN.get()})
                      .build(null));

  private static BlockBehaviour.Properties metal() {
    return BlockBehaviour.Properties.of()
        .strength(4.0f, 6.0f)
        .sound(SoundType.METAL)
        .isValidSpawn((s, l, pos, type) -> false);
  }

  public static void initialize() {}

  private FacilityDetails() {}

  static {
    for (DeferredBlock block :
        new DeferredBlock[] {
          LINEAR_LIGHT,
          WARM_LIGHT,
          SERVICE_DOOR,
          PUSH_BAR_DOOR,
          REACTOR_AIRLOCK,
          EMERGENCY,
          RAILING,
          WALKWAY,
          ALARM_SIREN,
          ALARM_BEACON,
          WORKSTATION,
          STORAGE_SHELF,
          FRAME,
          ANCHOR
        }) {
      Content.ITEMS.registerSimpleBlockItem((Holder) block);
    }
  }
}
