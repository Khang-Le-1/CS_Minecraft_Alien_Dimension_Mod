package com.timelocked;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class EngineeringBenchBlock extends Block {
  public static final MapCodec<EngineeringBenchBlock> CODEC =
      simpleCodec(EngineeringBenchBlock::new);

  public EngineeringBenchBlock(Properties p) {
    super(p);
  }

  protected MapCodec<? extends Block> codec() {
    return CODEC;
  }

  protected InteractionResult useWithoutItem(
      BlockState s, Level l, BlockPos p, Player player, BlockHitResult h) {
    if (!l.isClientSide) {
      ContainerLevelAccess access = ContainerLevelAccess.create(l, p);
      player.openMenu(
          new SimpleMenuProvider(
              (id, inv, user) ->
                  new CraftingMenu(id, inv, access) {
                    public boolean stillValid(Player who) {
                      return AbstractContainerMenu.stillValid(
                          access, who, (Block) MetalContent.BENCH.get());
                    }
                  },
              Component.translatable("block.timelocked.engineering_bench")));
    }

    return InteractionResult.sidedSuccess(l.isClientSide);
  }
}
