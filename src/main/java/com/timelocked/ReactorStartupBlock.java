package com.timelocked;

import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class ReactorStartupBlock extends Block {
  public static final MapCodec<ReactorStartupBlock> CODEC = simpleCodec(ReactorStartupBlock::new);

  public ReactorStartupBlock(Properties p) {
    super(p);
  }

  protected MapCodec<? extends ReactorStartupBlock> codec() {
    return CODEC;
  }

  public static String startup(ServerLevel l, BlockPos o) {
    SiteProgress progress = SitePlanner.progress(l, o);
    if (progress.powered) {
      return "Reactor grid already online. No further fuel consumed.";
    }

    BlockPos core = BlueprintRooms.at(l, o, 108, 2, 160);
    BlockPos fuelPos = BlueprintRooms.at(l, o, 108, 2, 199);
    BlockPos controller = BlueprintRooms.at(l, o, 88, 8, 36);

    for (BlockPos p : new BlockPos[] {core, fuelPos}) {
      if (!l.hasChunkAt(p)) {
        return "Startup denied: required machinery chunks unavailable.";
      }
    }

    if (!l.getBlockState(core).is((Block) UndergroundContent.CORE.get())) {
      return "Startup denied: reactor core missing.";
    }

    if (l.getBlockEntity(fuelPos) instanceof ReactorFuelEntity fuel) {
      if (l.hasChunkAt(controller)
          && !(l.getBlockEntity(controller) instanceof PortalControllerEntity)) {
        return "Startup denied: observation controller missing.";
      }

      if (!fuel.allChannelsLoaded()) {
        return "Startup denied: put one sealed fuel cell in each of the three fuel channels.";
      }

      if (!fuel.consumeStartupBatch()) {
        return "Fuel inventory changed; startup cancelled.";
      }

      l.setBlock(
          core, (BlockState) l.getBlockState(core).setValue(ReactorCoreBlock.ACTIVE, true), 3);
      progress.layoutVersion = 3;
      progress.powered = true;
      FacilityState.get(l.getServer()).setDirty();
      UndergroundSystems.restore(l, o);
      ReactorEffects.ignite(l, o);
      return "REACTOR ONLINE. Three fuel channels committed. Main lights, lift and airlocks available. Tram remains on independent emergency power.";
    } else {
      return "Startup denied: fuel system missing.";
    }
  }

  protected InteractionResult useWithoutItem(
      BlockState s, Level level, BlockPos pos, Player p, BlockHitResult hit) {
    if (!level.isClientSide) {
      ServerLevel l = (ServerLevel) level;
      Optional<BlockPos> o = SitePlanner.containing(l, pos);
      p.sendSystemMessage(
          Component.literal(
              o.isEmpty()
                  ? "Startup panel is outside a registered SARC site."
                  : startup(l, o.get())));
    }

    return InteractionResult.sidedSuccess(level.isClientSide);
  }
}
