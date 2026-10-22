package com.timelocked;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Opt-in, floor-level illumination for the authored drawing lobby; never rebuilds a room. */
@EventBusSubscriber(modid = "timelocked")
public final class HubLighting {
  // Drawing-local block coordinates. These bounds deliberately stop before the stair enclosure.
  static final int FIRST_X = 6;
  static final int LAST_X = 150;
  static final int FIRST_Z = 188;
  static final int LAST_Z = 332;
  static final int FLOOR_Y = 1;
  static final int GRID_SPACING_BLOCKS = 6;
  static final int HEADROOM_BLOCKS = 2;
  static final int RECEPTION_FIRST_X = 58;
  static final int RECEPTION_LAST_X = 76;
  static final int RECEPTION_FRONT_Z = 329;
  static final int RECEPTION_SPACING_BLOCKS = 3;
  private static final int OPERATOR_PERMISSION_LEVEL = 2;

  static Map<BlockPos, BlockState> plan() {
    Map<BlockPos, BlockState> out = new LinkedHashMap<>();
    BlockState lamp = Blocks.SEA_LANTERN.defaultBlockState();
    for (int x = FIRST_X; x <= LAST_X; x += GRID_SPACING_BLOCKS)
      for (int z = FIRST_Z; z <= LAST_Z; z += GRID_SPACING_BLOCKS)
        out.put(new BlockPos(x, FLOOR_Y, z), lamp);
    for (int x = RECEPTION_FIRST_X; x <= RECEPTION_LAST_X; x += RECEPTION_SPACING_BLOCKS)
      out.put(new BlockPos(x, FLOOR_Y, RECEPTION_FRONT_Z), lamp);
    return out;
  }

  /** A complete preflight, before any mutation. Never requests or generates a chunk. */
  static boolean loaded(ServerLevel level, BlockPos origin) {
    for (BlockPos local : plan().keySet())
      if (!level.hasChunkAt(origin.offset(local))) return false;
    return true;
  }

  static boolean eligible(ServerLevel level, BlockPos position) {
    if (!level.hasChunkAt(position) || level.getBlockEntity(position) != null) return false;
    BlockState floor = level.getBlockState(position);
    if (!floor.is(Content.FLOOR.get())
        && !floor.is(FacilityArchitecture.ROUTE.get())
        && !floor.is(Blocks.POLISHED_ANDESITE)
        && !floor.is(Blocks.LIGHT_GRAY_CONCRETE)) return false;
    for (int height = 1; height <= HEADROOM_BLOCKS; height++)
      if (!level.getBlockState(position.above(height)).isAir()) return false;
    return true;
  }

  static int apply(ServerLevel level, BlockPos origin, boolean confirm) {
    if (!loaded(level, origin)) return -1;
    int changed = 0;
    for (var entry : plan().entrySet()) {
      BlockPos position = origin.offset(entry.getKey());
      if (eligible(level, position)
          && (!confirm || level.setBlock(position, entry.getValue(), Block.UPDATE_ALL))) changed++;
    }
    return changed;
  }

  private static int execute(CommandSourceStack source, boolean confirm, boolean architectural)
      throws com.mojang.brigadier.exceptions.CommandSyntaxException {
    var player = source.getPlayerOrException();
    ServerLevel level = player.serverLevel();
    if (!level.dimension().equals(Level.OVERWORLD)) {
      source.sendFailure(Component.literal("Use this inside the Overworld SARC lobby."));
      return 0;
    }
    for (long key : FacilityState.get(level.getServer()).sites.keySet()) {
      BlockPos origin = BlockPos.of(key);
      if (!BlueprintRooms.drawing(level, origin) || !HubArrival.inside(player, origin)) continue;
      int count =
          architectural
              ? SarcAmbientLighting.retrofit(level, origin, confirm)
              : apply(level, origin, confirm);
      if (count < 0) {
        source.sendFailure(
            Component.literal(
                "Lobby chunks are not all loaded. No blocks changed. Load the room and retry; this command never generates chunks."));
        return 0;
      }
      source.sendSuccess(
          () ->
              Component.literal(
                  confirm
                      ? "Lobby lighting installed: "
                          + count
                          + " flush floor lamps. Existing lamps, hazards, containers and obstructed cells left untouched."
                      : "BACK UP YOUR WORLD FIRST. Preview: "
                          + count
                          + (architectural
                              ? " floor cells would receive recessed service-light tiles. Run /sarclights architectural confirm to apply. No blocks changed."
                              : " floor cells would become sea-lantern floor lights. Run /sarclights brighten confirm to apply. No blocks changed.")),
          confirm);
      return 1;
    }
    source.sendFailure(Component.literal("Stand inside a registered drawing-layout SARC lobby."));
    return 0;
  }

  @SubscribeEvent
  public static void commands(RegisterCommandsEvent event) {
    event
        .getDispatcher()
        .register(
            Commands.literal("sarclights")
                .requires(source -> source.hasPermission(OPERATOR_PERMISSION_LEVEL))
                .executes(context -> execute(context.getSource(), false, true))
                .then(
                    Commands.literal("architectural")
                        .executes(context -> execute(context.getSource(), false, true))
                        .then(
                            Commands.literal("confirm")
                                .executes(context -> execute(context.getSource(), true, true))))
                .then(
                    Commands.literal("brighten")
                        .executes(context -> execute(context.getSource(), false, false))
                        .then(
                            Commands.literal("confirm")
                                .executes(context -> execute(context.getSource(), true, false)))));
  }

  private HubLighting() {}
}
