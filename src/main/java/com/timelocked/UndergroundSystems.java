package com.timelocked;

import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.ChunkEvent;

public final class UndergroundSystems {
  public static void restore(ServerLevel l, BlockPos o) {
    for (BlockPos p : SiteLayout.LAMPS) {
      BlockPos at = BlueprintRooms.at(l, o, p);
      if (l.hasChunkAt(at) && l.getBlockState(at).is((Block) Content.LIGHT.get())) {
        l.setBlock(at, (BlockState) l.getBlockState(at).setValue(FacilityLampBlock.LIT, true), 3);
      }
    }

    for (BlockPos p :
        (BlueprintRooms.active(l, o)
            ? java.util.List.<BlockPos>of()
            : TramTerminalBlock.UNDERGROUND_ROUTE)) {
      BlockPos at = BlueprintRooms.at(l, o, p);
      if (l.hasChunkAt(at) && l.getBlockState(at).is((Block) UndergroundContent.TRACK.get())) {
        l.setBlock(at.below(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
        l.setBlock(
            at, (BlockState) l.getBlockState(at).setValue(PoweredRailBlock.POWERED, true), 3);
      }
    }

    if (l.hasChunkAt(BlueprintRooms.at(l, o, 88, 8, 36))
        && l.getBlockEntity(BlueprintRooms.at(l, o, 88, 8, 36))
            instanceof PortalControllerEntity be) {
      be.bind(o);
      be.provisionInitialGrid();
    }

    if (l.hasChunkAt(BlueprintRooms.at(l, o, 108, 2, 72))
        && l.getBlockEntity(BlueprintRooms.at(l, o, 108, 2, 72)) instanceof ArcFurnaceEntity be) {
      be.receiver.receiveEnergy(64000, false);
    }

    if (l.hasChunkAt(BlueprintRooms.at(l, o, 38, 2, 26))
        && l.getBlockEntity(BlueprintRooms.at(l, o, 38, 2, 26)) instanceof PowerSupplyEntity be) {
      be.output.generate(128000);
      be.setItem(0, new ItemStack(Items.REDSTONE, 16));
    }
  }

  public static void register() {
    NeoForge.EVENT_BUS.addListener(
        (ChunkEvent.Load e) -> {
          if (e.getLevel() instanceof ServerLevel l && l.dimension().equals(Level.OVERWORLD)) {
            ChunkPos cp = e.getChunk().getPos();

            for (Entry<Long, SiteProgress> entry :
                FacilityState.get(l.getServer()).progress.entrySet()) {
              if (entry.getValue().powered && entry.getValue().layoutVersion >= 2) {
                BlockPos o = BlockPos.of(entry.getKey());
                BlockPos controller = BlueprintRooms.at(l, o, 88, 8, 36);
                if (controller.getX() >> 4 == cp.x
                    && controller.getZ() >> 4 == cp.z
                    && l.getBlockEntity(controller) instanceof PortalControllerEntity be) {
                  be.bind(o);
                  be.provisionInitialGrid();
                }

                for (BlockPos p : SiteLayout.LAMPS) {
                  BlockPos at = BlueprintRooms.at(l, o, p);
                  if (at.getX() >> 4 == cp.x
                      && at.getZ() >> 4 == cp.z
                      && l.getBlockState(at).is((Block) Content.LIGHT.get())) {
                    l.setBlock(
                        at,
                        (BlockState) l.getBlockState(at).setValue(FacilityLampBlock.LIT, true),
                        3);
                  }
                }

                for (BlockPos p :
                    (BlueprintRooms.active(l, o)
                        ? java.util.List.<BlockPos>of()
                        : TramTerminalBlock.UNDERGROUND_ROUTE)) {
                  BlockPos at = BlueprintRooms.at(l, o, p);
                  if (at.getX() >> 4 == cp.x
                      && at.getZ() >> 4 == cp.z
                      && l.getBlockState(at).is((Block) UndergroundContent.TRACK.get())) {
                    l.setBlock(at.below(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
                    l.setBlock(
                        at,
                        (BlockState) l.getBlockState(at).setValue(PoweredRailBlock.POWERED, true),
                        3);
                  }
                }
              }
            }
          }
        });
  }

  private UndergroundSystems() {}
}
