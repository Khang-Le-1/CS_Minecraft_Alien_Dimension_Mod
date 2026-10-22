package com.timelocked;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Five authored thresholds, separate from locked lab/security doors and the tram guideway. */
@EventBusSubscriber(modid = "timelocked")
public final class TransitionDoorways {
  static final int CHUNK_COORDINATE_MASK = -16;
  static final int DOOR_Y = 2;
  static final int FRAME_TOP_Y_EXCLUSIVE = 6;
  static final int OPERATOR_PERMISSION_LEVEL = 2;
  static final int STAIR_EXIT_X = 87;
  static final int STAIR_EXIT_FIRST_Z = 355;
  static final int STAIR_EXIT_LAST_Z = 357;
  static final int STAIR_DOOR_Z = 356;
  static final int LOBBY_FIRST_X = 78;
  static final int LOBBY_LAST_X = 81;
  static final int LOBBY_LEFT_DOOR_X = 79;
  static final int LOBBY_RIGHT_DOOR_X = 80;
  static final int LOBBY_NORTH_Z = 185;
  static final int LOBBY_SOUTH_Z = 336;
  static final int LAB_WING_Z = 170;
  static final int TRAM_ENTRY_X = 153;
  static final int TRAM_FIRST_Z = 269;
  static final int TRAM_LAST_Z = 276;
  static final int TRAM_FIRST_DOOR_Z = 272;
  static final int TRAM_SECOND_DOOR_Z = 273;

  private record Threshold(
      String name,
      BlockPos start,
      Direction along,
      int width,
      int firstDoor,
      int leaves,
      Direction facing) {}

  private static final List<Threshold> THRESHOLDS =
      List.of(
          new Threshold(
              "stair exit",
              new BlockPos(STAIR_EXIT_X, DOOR_Y, STAIR_EXIT_FIRST_Z),
              Direction.SOUTH,
              STAIR_EXIT_LAST_Z - STAIR_EXIT_FIRST_Z + 1,
              STAIR_DOOR_Z - STAIR_EXIT_FIRST_Z,
              1,
              Direction.WEST),
          lobby("south lobby", LOBBY_SOUTH_Z, Direction.NORTH),
          lobby("north lobby", LOBBY_NORTH_Z, Direction.SOUTH),
          lobby("lab wing", LAB_WING_Z, Direction.NORTH),
          new Threshold(
              "tram platform",
              new BlockPos(TRAM_ENTRY_X, DOOR_Y, TRAM_FIRST_Z),
              Direction.SOUTH,
              TRAM_LAST_Z - TRAM_FIRST_Z + 1,
              TRAM_FIRST_DOOR_Z - TRAM_FIRST_Z,
              2,
              Direction.EAST));

  private static Threshold lobby(String name, int z, Direction facing) {
    return new Threshold(
        name,
        new BlockPos(LOBBY_FIRST_X, DOOR_Y, z),
        Direction.EAST,
        LOBBY_LAST_X - LOBBY_FIRST_X + 1,
        LOBBY_LEFT_DOOR_X - LOBBY_FIRST_X,
        2,
        facing);
  }

  static boolean atLobbyThreshold(BlockPos local) {
    if (local.getY() < DOOR_Y || local.getY() >= FRAME_TOP_Y_EXCLUSIVE) return false;
    return (local.getZ() == LOBBY_SOUTH_Z || local.getZ() == LOBBY_NORTH_Z)
            && local.getX() >= LOBBY_FIRST_X
            && local.getX() <= LOBBY_LAST_X
        || local.getX() == TRAM_ENTRY_X
            && local.getZ() >= TRAM_FIRST_Z
            && local.getZ() <= TRAM_LAST_Z;
  }

  static Map<BlockPos, BlockState> plan() {
    Map<BlockPos, BlockState> out = new LinkedHashMap<>();
    BlockState frame = FacilityArchitecture.CLEAN.get().defaultBlockState();
    for (Threshold threshold : THRESHOLDS)
      for (int column = 0; column < threshold.width(); column++)
        for (int y = DOOR_Y; y < FRAME_TOP_Y_EXCLUSIVE; y++) {
          BlockPos position = threshold.start().relative(threshold.along(), column).atY(y);
          boolean door =
              y < DOOR_Y + 2
                  && column >= threshold.firstDoor()
                  && column < threshold.firstDoor() + threshold.leaves();
          BlockState state = frame;
          if (door) {
            boolean first = column == threshold.firstDoor();
            // DoorBlock's LEFT/RIGHT are defined looking along its facing, not map X/Z.
            boolean ascendingIsRight = threshold.along() == threshold.facing().getClockWise();
            DoorHingeSide hinge =
                first == ascendingIsRight ? DoorHingeSide.LEFT : DoorHingeSide.RIGHT;
            state =
                FacilityDetails.PUSH_BAR_DOOR
                    .get()
                    .defaultBlockState()
                    .setValue(DoorBlock.FACING, threshold.facing())
                    .setValue(DoorBlock.HINGE, hinge)
                    .setValue(
                        DoorBlock.HALF,
                        y == DOOR_Y ? DoubleBlockHalf.LOWER : DoubleBlockHalf.UPPER);
          }
          out.put(position, state);
        }
    return out;
  }

  private static boolean installedDoor(BlockState current, BlockState desired) {
    return desired.is(FacilityDetails.PUSH_BAR_DOOR.get())
        && current.is(desired.getBlock())
        && current.getValue(DoorBlock.FACING) == desired.getValue(DoorBlock.FACING)
        && current.getValue(DoorBlock.HINGE) == desired.getValue(DoorBlock.HINGE)
        && current.getValue(DoorBlock.HALF) == desired.getValue(DoorBlock.HALF);
  }

  /** No chunk generation, inventory replacement, or partial repair on preflight rejection. */
  static String obstruction(ServerLevel level, BlockPos origin, Map<BlockPos, BlockState> edits) {
    for (BlockPos local : edits.keySet())
      if (!level.hasChunkAt(origin.offset(local))) return "A doorway chunk is unloaded";
    for (var entry : edits.entrySet()) {
      BlockPos position = origin.offset(entry.getKey());
      BlockState current = level.getBlockState(position), desired = entry.getValue();
      boolean installed = installedDoor(current, desired);
      if (level.getBlockEntity(position) != null && !installed)
        return "Block entity at " + position.toShortString();
      if (!current.isAir() && !current.equals(desired) && !installed)
        return "Unexpected block at " + position.toShortString();
      if (desired.is(FacilityDetails.PUSH_BAR_DOOR.get())
          && desired.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER
          && (!desired.canSurvive(level, position)
              || level.getBlockEntity(position.below()) != null))
        return "Door floor is missing or contains a block entity at " + position.toShortString();
      if (!current.equals(desired)
          && !installed
          && !level.getEntities(null, new AABB(position)).isEmpty())
        return "Move entities out of the doorway at " + position.toShortString();
    }
    return null;
  }

  static int apply(ServerLevel level, BlockPos origin, Map<BlockPos, BlockState> edits) {
    int count = 0;
    // Build without neighbour cascades, then notify once BOTH halves and both leaves exist.
    for (var entry : edits.entrySet()) {
      BlockPos position = origin.offset(entry.getKey());
      BlockState current = level.getBlockState(position);
      if (!current.equals(entry.getValue())
          && !installedDoor(current, entry.getValue())
          && level.setBlock(position, entry.getValue(), Block.UPDATE_CLIENTS)) count++;
    }
    for (BlockPos local : edits.keySet()) {
      BlockPos position = origin.offset(local);
      level.updateNeighborsAt(position, level.getBlockState(position).getBlock());
    }
    return count;
  }

  /** Natural generation is chunk-clipped: never loads neighbouring chunks or touches SavedData. */
  static void placeChunk(
      net.minecraft.world.level.WorldGenLevel region, BlockPos origin, int chunkX, int chunkZ) {
    for (var e : plan().entrySet()) {
      BlockPos at = origin.offset(e.getKey());
      if ((at.getX() & CHUNK_COORDINATE_MASK) == chunkX
          && (at.getZ() & CHUNK_COORDINATE_MASK) == chunkZ)
        region.setBlock(at, e.getValue(), Block.UPDATE_CLIENTS);
    }
  }

  /** Only called at completion of a NEW drawing placement, which already excavates this volume. */
  static void installNew(ServerLevel level, BlockPos origin) {
    Map<BlockPos, BlockState> edits = plan();
    for (BlockPos local : edits.keySet()) level.getChunkAt(origin.offset(local));
    String issue = obstruction(level, origin, edits);
    if (issue != null) throw new IllegalStateException("Doorway installation refused: " + issue);
    apply(level, origin, edits);
  }

  @SubscribeEvent
  public static void commands(RegisterCommandsEvent event) {
    event
        .getDispatcher()
        .register(
            Commands.literal("sarcdoors")
                .requires(source -> source.hasPermission(OPERATOR_PERMISSION_LEVEL))
                .executes(
                    context -> {
                      context
                          .getSource()
                          .sendSuccess(
                              () ->
                                  Component.literal(
                                      "BACK UP YOUR WORLD. /sarcdoors repair confirm adds the stair-exit door, both lobby door pairs, lab-wing pair and tram-platform pair. Clear the doorway areas and keep their chunks loaded."),
                              false);
                      return 1;
                    })
                .then(
                    Commands.literal("repair")
                        .then(
                            Commands.literal("confirm")
                                .executes(
                                    context -> {
                                      var player = context.getSource().getPlayerOrException();
                                      var level = player.serverLevel();
                                      if (level.dimension().equals(Level.OVERWORLD))
                                        for (long key :
                                            FacilityState.get(level.getServer()).sites.keySet()) {
                                          BlockPos origin = BlockPos.of(key);
                                          if (!BlueprintRooms.drawing(level, origin)
                                              || !FacilitySpawnProtection.contains(
                                                  level, origin, player.blockPosition())) continue;
                                          Map<BlockPos, BlockState> edits = plan();
                                          String issue = obstruction(level, origin, edits);
                                          if (issue != null) {
                                            context
                                                .getSource()
                                                .sendFailure(
                                                    Component.literal(
                                                        issue
                                                            + ". No blocks changed. Do not force the repair."));
                                            return 0;
                                          }
                                          int count = apply(level, origin, edits);
                                          context
                                              .getSource()
                                              .sendSuccess(
                                                  () ->
                                                      Component.literal(
                                                          "Five transition doorways installed: "
                                                              + count
                                                              + " blocks changed. Push-bar doors open by hand; no key or power required."),
                                                  true);
                                          return 1;
                                        }
                                      context
                                          .getSource()
                                          .sendFailure(
                                              Component.literal(
                                                  "Stand inside a registered drawing-layout facility."));
                                      return 0;
                                    }))));
  }

  private TransitionDoorways() {}
}
