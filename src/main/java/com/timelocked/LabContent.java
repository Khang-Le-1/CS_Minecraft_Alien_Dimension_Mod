/* Reconstructed from the user-provided 0.8.9.8 release with permission. */
package com.timelocked;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;

public final class LabContent {
  public static final DeferredBlock<HallwayDecorBlock> WAITING_BENCH =
      Content.BLOCKS.register(
          "sarc_waiting_bench",
          () ->
              new HallwayDecorBlock(
                  BlockBehaviour.Properties.of()
                      .strength(2.5f)
                      .sound(SoundType.METAL)
                      .noOcclusion(),
                  0));
  public static final DeferredBlock<HallwayDecorBlock> ENERGY_AD =
      Content.BLOCKS.register(
          "brain_implosion_ad",
          () ->
              new HallwayDecorBlock(
                  BlockBehaviour.Properties.of()
                      .strength(1.5f)
                      .sound(SoundType.METAL)
                      .noOcclusion()
                      .lightLevel(s -> 5),
                  1));
  public static final DeferredBlock<HallwayDecorBlock> PLANTER =
      Content.BLOCKS.register(
          "sarc_hall_planter",
          () ->
              new HallwayDecorBlock(
                  BlockBehaviour.Properties.of()
                      .strength(2.0f)
                      .sound(SoundType.STONE)
                      .noOcclusion(),
                  2));
  public static final DeferredBlock<Block> DERELICT_PANEL =
      Content.BLOCKS.registerSimpleBlock(
          "sarc_derelict_panel",
          BlockBehaviour.Properties.of().strength(3.0f, 6.0f).sound(SoundType.METAL));
  public static final DeferredBlock<JammedBlastDoorBlock> JAMMED_DOOR =
      Content.BLOCKS.register(
          "sarc_jammed_blast_door",
          () ->
              new JammedBlastDoorBlock(
                  BlockBehaviour.Properties.of()
                      .strength(8.0f, 30.0f)
                      .sound(SoundType.METAL)
                      .noOcclusion()));
  public static final DeferredBlock<LabDecorBlock> PAPERS =
      Content.BLOCKS.register(
          "sarc_scattered_papers", () -> new LabDecorBlock(LabContent.thin(SoundType.WOOL), 0));
  public static final DeferredBlock<LabDecorBlock> PUDDLE =
      Content.BLOCKS.register(
          "xenogel_puddle",
          () ->
              new LabDecorBlock(
                  LabContent.thin(SoundType.HONEY_BLOCK).speedFactor(0.5f).jumpFactor(0.6f), 1));
  public static final DeferredBlock<LabDecorBlock> CLAW =
      Content.BLOCKS.register(
          "sarc_claw_marks", () -> new LabDecorBlock(LabContent.thin(SoundType.STONE), 2));
  public static final DeferredBlock<ContainmentTankBlock> TANK =
      Content.BLOCKS.register(
          "sarc_containment_tank",
          () -> new ContainmentTankBlock(LabContent.tank().lightLevel(s -> 4), false));
  public static final DeferredBlock<ContainmentTankBlock> BROKEN_TANK =
      Content.BLOCKS.register(
          "sarc_containment_tank_broken", () -> new ContainmentTankBlock(LabContent.tank(), true));
  public static final DeferredBlock<FlickerLightBlock> FLICKER =
      Content.BLOCKS.register(
          "sarc_flicker_light",
          () ->
              new FlickerLightBlock(
                  BlockBehaviour.Properties.of()
                      .strength(4.0f, 6.0f)
                      .sound(SoundType.COPPER_BULB)
                      .isValidSpawn((st, l, p, t) -> false)
                      .lightLevel(
                          s ->
                              (Boolean) s.getValue((Property) FlickerLightBlock.LIT) != false
                                  ? 14
                                  : 1)));
  public static final DeferredBlock<StationSignBlock> STATION_SIGN =
      Content.BLOCKS.register(
          "sarc_station_sign",
          () ->
              new StationSignBlock(
                  BlockBehaviour.Properties.of()
                      .strength(2.0f, 6.0f)
                      .sound(SoundType.METAL)
                      .noCollission()
                      .noOcclusion()
                      .lightLevel(s -> 6)
                      .isValidSpawn((st, l, p, t) -> false)));
  public static final DeferredBlock<FacingDecorBlock> FUEL_RACK =
      Content.BLOCKS.register(
          "sarc_fuel_cell_rack",
          () ->
              new FacingDecorBlock(
                  BlockBehaviour.Properties.of()
                      .strength(4.0f, 6.0f)
                      .sound(SoundType.METAL)
                      .noOcclusion()
                      .lightLevel(s -> 9)
                      .isValidSpawn((st, l, p, t) -> false),
                  1));
  public static final DeferredBlock<FacingDecorBlock> STABILIZER =
      Content.BLOCKS.register(
          "sarc_field_stabilizer",
          () ->
              new FacingDecorBlock(
                  BlockBehaviour.Properties.of()
                      .strength(4.0f, 6.0f)
                      .sound(SoundType.METAL)
                      .noOcclusion()
                      .lightLevel(s -> 11)
                      .isValidSpawn((st, l, p, t) -> false),
                  2));

  private static BlockBehaviour.Properties thin(SoundType s) {
    return BlockBehaviour.Properties.of()
        .noCollission()
        .noOcclusion()
        .instabreak()
        .sound(s)
        .pushReaction(PushReaction.DESTROY)
        .isValidSpawn((st, l, p, t) -> false);
  }

  private static BlockBehaviour.Properties tank() {
    return BlockBehaviour.Properties.of()
        .strength(2.0f, 4.0f)
        .sound(SoundType.GLASS)
        .noOcclusion()
        .pushReaction(PushReaction.BLOCK)
        .isValidSpawn((st, l, p, t) -> false)
        .isRedstoneConductor((s, l, p) -> false)
        .isSuffocating((s, l, p) -> false)
        .isViewBlocking((s, l, p) -> false);
  }

  public static void initialize() {
    Content.ITEMS.register(
        "xenogel_puddle",
        () ->
            new BlockItem((Block) PUDDLE.get(), new Item.Properties()) {

              public void appendHoverText(
                  ItemStack st, Item.TooltipContext c, List<Component> tips, TooltipFlag fl) {
                tips.add(
                    (Component)
                        Component.translatable((String) "tooltip.timelocked.xenogel")
                            .withStyle(
                                new ChatFormatting[] {
                                  ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC
                                }));
              }
            });
    for (DeferredBlock b :
        new DeferredBlock[] {
          WAITING_BENCH,
          ENERGY_AD,
          PLANTER,
          DERELICT_PANEL,
          JAMMED_DOOR,
          PAPERS,
          CLAW,
          TANK,
          BROKEN_TANK,
          FLICKER,
          STATION_SIGN,
          FUEL_RACK,
          STABILIZER
        }) {
      Content.ITEMS.registerSimpleBlockItem((Holder) b);
    }
  }

  private LabContent() {}
}
