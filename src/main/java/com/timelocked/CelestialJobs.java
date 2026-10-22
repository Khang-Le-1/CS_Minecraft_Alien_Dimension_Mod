package com.timelocked;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.*;
import net.minecraft.world.item.trading.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.*;

/** Celestial occupations are independent of house/species and vanilla professions. */
public final class CelestialJobs {
  public enum Role {
    HERBALIST("herbalist_bench", "Herbalist", "Hold 2 star blooms to make 2 bandages."),
    CRYSTAL_SHAPER(
        "crystal_shaping_bench",
        "Crystal shaper",
        "Hold 9 void crystal clusters to form a crystal block; hold a block to split it."),
    GARDENER(
        "cultivation_table",
        "Gardener",
        "Hold bone meal with a fern, bloom, reed, lotus, or frond in your off hand to grow another."),
    COOK("celestial_hearth", "Cook", "Hold 4 glow berries to prepare 2 baked potatoes."),
    ATTUNER(
        "attunement_altar",
        "Attuner",
        "Offer 3 void crystal clusters for night vision and resistance. One-minute cooldown."),
    WARDEN("warden_desk", "Warden", "Use for sanctuary and return-gate directions."),
    MEDIC(
        "celestial_medical_station",
        "Medic",
        "Hold a bandage for treatment: up to 3 hearts. Ten-second cooldown; no reactor needed.");
    public final String id, title, hint;
    public final DeferredBlock<CelestialWorkstationBlock> block;

    Role(String id, String title, String hint) {
      this.id = id;
      this.title = title;
      this.hint = hint;
      block =
          Content.BLOCKS.register(
              id,
              () ->
                  new CelestialWorkstationBlock(
                      this,
                      BlockBehaviour.Properties.of()
                          .strength(2.0F)
                          .sound(SoundType.WOOD)
                          .noOcclusion()
                          .lightLevel(s -> id.equals("attunement_altar") ? 6 : 2)));
    }
  }

  public static final DeferredHolder<
          BlockEntityType<?>, BlockEntityType<CelestialWorkstationBlock.Station>>
      ENTITY =
          Content.BLOCK_ENTITIES.register(
              "celestial_workstation",
              () ->
                  BlockEntityType.Builder.of(
                          CelestialWorkstationBlock.Station::new,
                          Arrays.stream(Role.values())
                              .map(r -> r.block.get())
                              .toArray(Block[]::new))
                      .build(null));

  public static void initialize() {
    for (Role r : Role.values()) Content.ITEMS.registerSimpleBlockItem(r.block);
  }

  public static Role role(int id) {
    return id >= 0 && id < Role.values().length ? Role.values()[id] : null;
  }

  static MerchantOffer sell(int price, net.minecraft.world.level.ItemLike item, int count) {
    return new MerchantOffer(
        new ItemCost(VoidspaceContent.CRYSTAL_CLUSTER.get(), price),
        new ItemStack(item, count),
        12,
        3,
        0.05F);
  }

  static MerchantOffer buy(net.minecraft.world.level.ItemLike item, int count, int price) {
    return new MerchantOffer(
        new ItemCost(item, count),
        new ItemStack(VoidspaceContent.CRYSTAL_CLUSTER.get(), price),
        12,
        2,
        0.05F);
  }

  public static MerchantOffers trades(Role role) {
    MerchantOffers offers = new MerchantOffers();
    switch (role) {
      case HERBALIST -> {
        offers.add(buy(VoidspaceContent.STAR_BLOOM.get(), 6, 1));
        offers.add(sell(2, Content.BANDAGE.get(), 3));
        offers.add(sell(4, Items.HONEY_BOTTLE, 2));
      }
      case CRYSTAL_SHAPER -> {
        offers.add(buy(Items.AMETHYST_SHARD, 8, 1));
        offers.add(sell(2, Content.CELESTIAL_MASONRY.get(), 16));
        offers.add(sell(2, Content.CELESTIAL_PATH.get(), 16));
        offers.add(sell(3, Content.CELESTIAL_ROOF.get(), 16));
      }
      case GARDENER -> {
        offers.add(buy(VoidspaceContent.GLIMMER_MOSS.get(), 8, 1));
        offers.add(sell(1, Items.BONE_MEAL, 6));
        offers.add(sell(2, VoidspaceContent.MOON_FERN.get(), 3));
        offers.add(sell(2, VoidspaceContent.DUSK_LOTUS.get(), 3));
        offers.add(sell(2, VoidspaceContent.PRISM_REED.get(), 3));
      }
      case COOK -> {
        offers.add(buy(Items.GLOW_BERRIES, 16, 1));
        offers.add(sell(1, Items.BREAD, 4));
        offers.add(sell(2, Items.COOKED_BEEF, 3));
        offers.add(sell(3, Items.GOLDEN_CARROT, 2));
      }
      case ATTUNER -> {
        offers.add(buy(Items.ENDER_PEARL, 2, 2));
        offers.add(sell(3, Items.SPECTRAL_ARROW, 12));
        offers.add(sell(4, Items.ENDER_PEARL, 1));
      }
      case WARDEN -> {
        offers.add(buy(Items.IRON_INGOT, 8, 1));
        offers.add(sell(2, Items.TORCH, 24));
        offers.add(sell(2, Items.COMPASS, 1));
        offers.add(sell(3, Items.SHIELD, 1));
      }
      case MEDIC -> {
        offers.add(buy(VoidspaceContent.LANTERN_BLOOM.get(), 6, 1));
        offers.add(sell(2, Content.BANDAGE.get(), 3));
        offers.add(sell(2, Items.MILK_BUCKET, 1));
        offers.add(sell(4, Items.GOLDEN_APPLE, 1));
      }
    }
    return offers;
  }

  // Each entry: local station X/Z, occupational role. Four homes become services;
  // eight remain dwellings. Worktops sit to one side of the entrance aisle.
  public static final int[][] STATIONS = {
    {-26, -20, 0}, {25, -20, 1}, {-26, 21, 2}, {25, 21, 3}, {4, -4, 4}, {77, -6, 5}, {12, -39, 6}
  };

  public static void furnish(
      int ox,
      int oz,
      java.util.function.BiConsumer<BlockPos, net.minecraft.world.level.block.state.BlockState>
          sink) {
    for (int[] s : STATIONS)
      sink.accept(
          new BlockPos(ox + s[0], 63, oz + s[1]),
          Role.values()[s[2]].block.get().defaultBlockState());

    // Genuine beds rather than decorative wool in the remaining residences.
    int[][] homes = {
      {-58, -48}, {0, -65}, {58, -48}, {-68, 10}, {68, 10}, {-48, 58}, {0, 70}, {48, 58}
    };
    for (int[] h : homes) {
      for (int z = 0; z < 2; z++)
        sink.accept(
            new BlockPos(ox + h[0] + 3, 63, oz + h[1] + z),
            Blocks.BLUE_BED
                .defaultBlockState()
                .setValue(BedBlock.FACING, net.minecraft.core.Direction.SOUTH)
                .setValue(
                    BedBlock.PART,
                    z == 0
                        ? net.minecraft.world.level.block.state.properties.BedPart.FOOT
                        : net.minecraft.world.level.block.state.properties.BedPart.HEAD));
    }
  }

  private CelestialJobs() {}
}
