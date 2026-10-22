package com.timelocked;

import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class ReactorConsoleBlock extends Block {
  public static final MapCodec<ReactorConsoleBlock> CODEC = simpleCodec(ReactorConsoleBlock::new);

  public ReactorConsoleBlock(Properties p) {
    super(p);
  }

  protected MapCodec<? extends ReactorConsoleBlock> codec() {
    return CODEC;
  }

  protected InteractionResult useWithoutItem(
      BlockState s, Level level, BlockPos pos, Player player, BlockHitResult hit) {
    if (level.isClientSide) {
      return InteractionResult.SUCCESS;
    }

    ServerLevel l = (ServerLevel) level;
    Optional<BlockPos> site = SitePlanner.containing(l, pos);
    if (site.isEmpty()) {
      player.sendSystemMessage(Component.literal("Console is outside a registered SARC facility."));
      return InteractionResult.CONSUME;
    }

    SiteProgress progress = SitePlanner.progress(l, site.get());
    if (progress.powered) {
      player.sendSystemMessage(Component.literal("Reactor grid online. Return to Wing 1 by tram."));
      return InteractionResult.CONSUME;
    }

    BlockPos core = BlueprintRooms.at(l, site.get(), 108, 2, 160);
    BlockPos fuel = BlueprintRooms.at(l, site.get(), 108, 2, 199);
    BlockPos startup = BlueprintRooms.at(l, site.get(), 110, 2, 184);
    if (l.hasChunkAt(core) && l.hasChunkAt(fuel) && l.hasChunkAt(startup)) {
      if (progress.layoutVersion >= 2
          || l.getBlockState(core).is((Block) UndergroundContent.CORE.get())
          || l.getBlockState(fuel).is((Block) UndergroundContent.FUEL.get())
          || l.getBlockState(startup).is((Block) UndergroundContent.STARTUP.get())) {
        player.sendSystemMessage(
            Component.literal(
                "This underground site uses three fuel channels. Use the dedicated startup panel in Reactor CR."));
        return InteractionResult.CONSUME;
      }

      if (!player.getMainHandItem().is((Item) Content.FUEL_CELL.get())) {
        player.sendSystemMessage(
            Component.literal(
                "Reactor offline: hold a sealed fuel cell from Wing 6 Storage, then use this console."));
        return InteractionResult.CONSUME;
      }

      if (!player.getAbilities().instabuild) {
        player.getMainHandItem().shrink(1);
      }

      restore(l, site.get());
      player.sendSystemMessage(
          Component.literal(
              "POWER RESTORED. Lights, airlocks and tram are online. Return to Wing 1 and activate the portal."));
      return InteractionResult.CONSUME;
    } else {
      player.sendSystemMessage(
          Component.literal("Startup unavailable: load the site machinery rooms first."));
      return InteractionResult.CONSUME;
    }
  }

  public static void restore(ServerLevel l, BlockPos origin) {
    SiteProgress p = SitePlanner.progress(l, origin);
    p.powered = true;
    FacilityState.get(l.getServer()).setDirty();

    for (BlockPos lamp : SiteLayout.LEGACY_LAMPS) {
      BlockPos at = origin.offset(lamp);
      if (l.hasChunkAt(at)) {
        BlockState s = l.getBlockState(at);
        if (s.is((Block) Content.LIGHT.get())) {
          l.setBlock(at, (BlockState) s.setValue(FacilityLampBlock.LIT, true), 3);
        }
      }
    }

    for (BlockPos local : TramTerminalBlock.POWERED_RAILS) {
      BlockPos at = origin.offset(local);
      l.setBlock(at.below(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
      l.setBlock(at, (BlockState) l.getBlockState(at).setValue(PoweredRailBlock.POWERED, true), 3);
    }

    for (int y = 3; y <= 4; y++) {
      for (int x = 98; x <= 101; x++) {
        BlockPos at = origin.offset(x, y, 36);
        if (l.getBlockState(at).is((Block) Content.SHELL.get())) {
          l.removeBlock(at, false);
        }
      }
    }

    if (l.getBlockEntity(origin.offset(104, 2, 24)) instanceof PowerSupplyEntity be) {
      be.output.generate(128000);
      be.setItem(0, new ItemStack(Items.REDSTONE, 16));
    }

    if (l.getBlockEntity(origin.offset(88, 8, 36)) instanceof PortalControllerEntity be) {
      be.bind(origin);
      be.receiver.receiveEnergy(64000, false);
    }

    if (l.getBlockEntity(origin.offset(108, 2, 72)) instanceof ArcFurnaceEntity be) {
      be.receiver.receiveEnergy(64000, false);
    }
  }
}
