package com.timelocked;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Two flights beneath the original surface glass slot. Coordinates are drawing-local block units,
 * not legacy BlueprintRooms.at coordinates. The upper slot and entrance doors are not relocated.
 */
@EventBusSubscriber(modid = "timelocked")
public final class GlassStairwell {
  // Walk heights are surface 29, turning landing 16, hallway 2. Each tread drops one block;
  // the native stair shape supplies the intermediate half-step, so no jumping is required.
  static final int SURFACE_FLOOR = 28, TURN_FLOOR = 15, HALL_FLOOR = 1;
  static final int FLIGHT_FIRST_Z = 336, FLIGHT_LAST_Z = 348;
  static final int UPPER_LEFT = 88, UPPER_RIGHT = 92;
  static final int LOWER_LEFT = 98, LOWER_RIGHT = 100;
  static final int TURN_BACK_Z = 333, TURN_FRONT_Z = 335;
  static final int UPPER_HEIGHT_OFFSET = 320, LOWER_HEIGHT_OFFSET = 351;
  static final int CLEARANCE = 4, CEILING_OFFSET = CLEARANCE + 1;
  static final int SLOT_BACK_Z = 344, UPPER_CLEAR_TOP = 32;
  static final int GLAZING_HEIGHT = 29, APPROACH_Z = 349;
  static final int EXIT_FIRST_Z = 350, EXIT_BACK_Z = 355, EXIT_FRONT_Z = 357;
  static final int RECESS_X = 87, EXIT_CEILING = 6, OLD_SHAFT_BACK_Z = 353, OLD_SHAFT_FRONT_Z = 359;
  static final int FILL_FIRST_Y = 7, OLD_SHAFT_TOP = 27;
  static final int MODULE_SIZE = 32, FIRST_MODULE_X = 2, LAST_MODULE_X = 3;
  static final int FIRST_MODULE_Z = 10, LAST_MODULE_Z = 11;
  // Bounds include the superseded 0.8.9.20 hallway route that must be restored during upgrades.
  static final int REPAIR_MIN_X = 76, REPAIR_MIN_Z = 332, REPAIR_MAX_Z = 360;
  static final int APPROACH_MIN_X = UPPER_LEFT - 1, APPROACH_MAX_X = LOWER_RIGHT + 1;
  static final int APPROACH_MIN_Z = TURN_BACK_Z - 1, APPROACH_MAX_Z = EXIT_FRONT_Z + 1;
  static final int RETURN_LIGHT_Y = 4;
  // Drawing-local mount: north face of the blank wall beside the lift, not world coordinates.
  static final BlockPos OLD_BEACON = new BlockPos(86, 6, 356);
  static final BlockPos WALL_BEACON = new BlockPos(83, 5, 359);
  static final BlockPos BEACON_SUPPORT = WALL_BEACON.south();
  static final BlockPos OLD_SIREN = new BlockPos(86, 6, 358);
  static final BlockPos WALL_SIREN = new BlockPos(85, 5, 359);

  private record AlarmMove(BlockPos from, BlockPos to, Block block) {}

  private static List<AlarmMove> alarmMoves() {
    return List.of(
        new AlarmMove(OLD_BEACON, WALL_BEACON, FacilityDetails.ALARM_BEACON.get()),
        new AlarmMove(OLD_SIREN, WALL_SIREN, FacilityDetails.ALARM_SIREN.get()));
  }

  private static final int[] HALL_LIGHT_X = {88, 92, 96, 100};
  private static final int[] RETURN_LIGHT_Z = {350, 353};

  // The enclosed turning landing protrudes into the old hub cuboid. Being inside that
  // enclosure is not entering the lobby. The lobby floor beneath it still counts.
  static boolean containsApproach(BlockPos local) {
    return local.getX() >= APPROACH_MIN_X
        && local.getX() <= APPROACH_MAX_X
        && local.getZ() >= APPROACH_MIN_Z
        && local.getZ() <= APPROACH_MAX_Z
        && local.getY() >= TURN_FLOOR
        && local.getY() < UPPER_CLEAR_TOP;
  }

  private static final int[] LIGHT_TREADS = {337, 341, 345, 348};
  private static final int[] LANDING_LIGHT_X = {89, 94, 99};
  private static final BlockState AIR = Blocks.AIR.defaultBlockState();

  static Map<BlockPos, BlockState> plan(ServerLevel level) {
    Map<BlockPos, BlockState> out = new LinkedHashMap<>();
    var baseline = templateStates(level, "stairwell_baseline");
    var previous = templateStates(level, "stairwell_v20");
    // Undo the previous return flight/bypass instead of leaving a second route behind.
    // Original sparse rock cells are restored to stone, never silently left as another tunnel.
    for (var e : previous.entrySet()) {
      BlockState old = baseline.get(e.getKey());
      if (!e.getValue().equals(old))
        out.put(e.getKey(), old == null ? Blocks.STONE.defaultBlockState() : old);
    }
    BlockState floor = Content.FLOOR.get().defaultBlockState();
    BlockState wall = FacilityArchitecture.CLEAN.get().defaultBlockState();
    BlockState glow = FacilityDetails.EMERGENCY.get().defaultBlockState();
    for (int z = FLIGHT_FIRST_Z; z <= FLIGHT_LAST_Z; z++) {
      int y = z - UPPER_HEIGHT_OFFSET;
      for (int x = UPPER_LEFT; x <= UPPER_RIGHT; x++) {
        put(out, x, y - 1, z, floor);
        put(out, x, y, z, tread(Direction.SOUTH));
        int top = z >= SLOT_BACK_Z ? UPPER_CLEAR_TOP : y + CEILING_OFFSET;
        for (int h = y + 1; h < top; h++) put(out, x, h, z, AIR);
        if (z < SLOT_BACK_Z) put(out, x, y + CEILING_OFFSET, z, wall);
      }
      for (int x : new int[] {UPPER_LEFT - 1, UPPER_RIGHT + 1})
        for (int h = y - 1; h < Math.min(y + CEILING_OFFSET, GLAZING_HEIGHT); h++)
          put(out, x, h, z, h == y + 1 && lit(z) ? glow : wall);
    }
    for (int x = UPPER_LEFT; x <= LOWER_RIGHT; x++)
      for (int z = TURN_BACK_Z; z <= TURN_FRONT_Z; z++) {
        put(out, x, TURN_FLOOR, z, floor);
        for (int y = TURN_FLOOR + 1; y < TURN_FLOOR + CEILING_OFFSET; y++) put(out, x, y, z, AIR);
        put(out, x, TURN_FLOOR + CEILING_OFFSET, z, wall);
      }
    for (int x = UPPER_LEFT - 1; x <= LOWER_RIGHT + 1; x++)
      for (int y = TURN_FLOOR; y < TURN_FLOOR + CEILING_OFFSET; y++)
        put(out, x, y, TURN_BACK_Z - 1, y == TURN_FLOOR + 1 && landingLight(x) ? glow : wall);
    for (int x : new int[] {UPPER_LEFT - 1, LOWER_RIGHT + 1})
      for (int z = TURN_BACK_Z; z <= TURN_FRONT_Z; z++)
        for (int y = TURN_FLOOR; y < TURN_FLOOR + CEILING_OFFSET; y++) put(out, x, y, z, wall);
    for (int x = UPPER_RIGHT + 1; x < LOWER_LEFT; x++)
      for (int y = TURN_FLOOR; y < TURN_FLOOR + CEILING_OFFSET; y++)
        put(out, x, y, FLIGHT_FIRST_Z, wall);
    for (int z = FLIGHT_FIRST_Z; z <= FLIGHT_LAST_Z + 1; z++) {
      int y = LOWER_HEIGHT_OFFSET - z;
      for (int x = LOWER_LEFT; x <= LOWER_RIGHT; x++) {
        put(out, x, y - 1, z, floor);
        put(out, x, y, z, tread(Direction.NORTH));
        for (int h = y + 1; h < y + CEILING_OFFSET; h++) put(out, x, h, z, AIR);
        put(out, x, y + CEILING_OFFSET, z, wall);
      }
      for (int x : new int[] {LOWER_LEFT - 1, LOWER_RIGHT + 1})
        for (int h = y; h < Math.min(y + CEILING_OFFSET, SURFACE_FLOOR); h++)
          put(out, x, h, z, h == y + 1 && lit(z) ? glow : wall);
    }
    // Six-block bottom landing turns WEST directly through the photographed recessed opening.
    // It does not discharge into the distant north/south entrance hallway as 0.8.9.20 did.
    for (int z = EXIT_FIRST_Z; z <= EXIT_FRONT_Z; z++)
      for (int x = LOWER_LEFT; x <= LOWER_RIGHT; x++) passage(out, baseline, x, z, floor, wall);
    for (int z = EXIT_BACK_Z; z <= EXIT_FRONT_Z; z++)
      for (int x = RECESS_X; x <= LOWER_RIGHT; x++) passage(out, baseline, x, z, floor, wall);
    for (int z = EXIT_FIRST_Z; z < EXIT_BACK_Z; z++)
      for (int x : new int[] {LOWER_LEFT - 1, LOWER_RIGHT + 1})
        for (int y = HALL_FLOOR; y <= EXIT_CEILING; y++) put(out, x, y, z, wall);
    for (int x = UPPER_RIGHT + 1; x <= LOWER_RIGHT + 1; x++)
      for (int y = HALL_FLOOR; y <= EXIT_CEILING; y++) {
        if (x < LOWER_LEFT) put(out, x, y, EXIT_BACK_Z - 1, wall);
        put(out, x, y, EXIT_FRONT_Z + 1, wall);
      }
    for (int z = EXIT_BACK_Z; z <= EXIT_FRONT_Z; z++)
      for (int y = HALL_FLOOR; y <= EXIT_CEILING; y++) put(out, LOWER_RIGHT + 1, y, z, wall);
    // Retire the OLD spiral completely. Keep the low recessed vestibule, seal the vertical shaft,
    // and restore its upper floor so it is not another fall hole. Existing alarm fixtures at y=6
    // remain untouched; no containers, lifts or unrelated staircases are removed.
    for (int x = UPPER_LEFT; x <= UPPER_RIGHT; x++)
      for (int z = OLD_SHAFT_BACK_Z; z <= OLD_SHAFT_FRONT_Z; z++) {
        for (int y = FILL_FIRST_Y; y <= OLD_SHAFT_TOP; y++) put(out, x, y, z, wall);
        put(out, x, SURFACE_FLOOR, z, Blocks.OAK_PLANKS.defaultBlockState());
        for (int y = HALL_FLOOR + 1; y < FILL_FIRST_Y; y++) {
          BlockState old = baseline.get(new BlockPos(x, y, z));
          if (old != null && old.is(Blocks.STONE_BRICK_STAIRS)) put(out, x, y, z, AIR);
        }
      }
    for (int x = UPPER_LEFT; x <= UPPER_RIGHT; x++) {
      put(out, x, SURFACE_FLOOR, APPROACH_Z, Blocks.OAK_PLANKS.defaultBlockState());
      for (int y = GLAZING_HEIGHT; y < UPPER_CLEAR_TOP; y++) put(out, x, y, APPROACH_Z, AIR);
    }
    // Permanent emergency strips, independent of the hub's staged arrival lights.
    // Embed them in the ceiling and return wall, leaving alarms, headroom and width intact.
    for (int x : HALL_LIGHT_X) put(out, x, EXIT_CEILING, EXIT_FRONT_Z, glow);
    for (int z : RETURN_LIGHT_Z) put(out, LOWER_RIGHT + 1, RETURN_LIGHT_Y, z, glow);
    for (AlarmMove move : alarmMoves()) {
      out.put(move.from(), AIR);
      out.put(
          move.to(), move.block().defaultBlockState().setValue(AlarmBlock.FACING, Direction.NORTH));
      out.put(move.to().south(), baseline.get(move.to().south()));
    }
    return out;
  }

  private static void passage(
      Map<BlockPos, BlockState> out,
      Map<BlockPos, BlockState> baseline,
      int x,
      int z,
      BlockState floor,
      BlockState wall) {
    put(out, x, HALL_FLOOR, z, floor);
    for (int y = HALL_FLOOR + 1; y < EXIT_CEILING; y++) put(out, x, y, z, AIR);
    BlockState ceiling = baseline.get(new BlockPos(x, EXIT_CEILING, z));
    // Existing alarms are fixtures with block entities, not disposable ceiling blocks.
    if (ceiling == null || ceiling.isAir()) put(out, x, EXIT_CEILING, z, wall);
  }

  private static boolean lit(int z) {
    for (int tread : LIGHT_TREADS) if (z == tread) return true;
    return false;
  }

  private static boolean landingLight(int x) {
    for (int light : LANDING_LIGHT_X) if (x == light) return true;
    return false;
  }

  private static BlockState tread(Direction facing) {
    return Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, facing);
  }

  private static void put(Map<BlockPos, BlockState> out, int x, int y, int z, BlockState state) {
    out.put(new BlockPos(x, y, z), state);
  }

  // Read exact original states from the preserved source template. They are repair preconditions,
  // not
  // a second generated facility: no wholesale template placement or room/inventory replacement.
  static Map<BlockPos, BlockState> templateStates(ServerLevel level, String directory) {
    boolean previous = directory.equals("stairwell_v20");
    Map<BlockPos, BlockState> out =
        previous
            ? new LinkedHashMap<>(templateStates(level, "stairwell_baseline"))
            : new LinkedHashMap<>();
    for (int x = FIRST_MODULE_X; x <= LAST_MODULE_X; x++)
      for (int z = FIRST_MODULE_Z; z <= LAST_MODULE_Z; z++) {
        ResourceLocation id =
            ResourceLocation.fromNamespaceAndPath(
                "timelocked",
                String.format(java.util.Locale.ROOT, "sarc/%s/x%02d_y00_z%02d", directory, x, z));
        var found = level.getStructureManager().get(id);
        if (previous && found.isEmpty()) continue;
        var template = found.orElseThrow();
        CompoundTag nbt = template.save(new CompoundTag());
        var palette = nbt.getList("palette", Tag.TAG_COMPOUND);
        var blocks = nbt.getList("blocks", Tag.TAG_COMPOUND);
        for (int i = 0; i < blocks.size(); i++) {
          var row = blocks.getCompound(i);
          var pos = row.getList("pos", Tag.TAG_INT);
          BlockState state =
              NbtUtils.readBlockState(
                  level.holderLookup(Registries.BLOCK), palette.getCompound(row.getInt("state")));
          if (!state.is(Blocks.STRUCTURE_VOID))
            out.put(
                new BlockPos(
                    pos.getInt(0) + x * MODULE_SIZE,
                    pos.getInt(1),
                    pos.getInt(2) + z * MODULE_SIZE),
                state);
        }
      }
    return out;
  }

  /** All-or-nothing preflight: loaded chunks, no inventories, no unrecognised player edits. */
  static String obstruction(
      ServerLevel level,
      BlockPos origin,
      Map<BlockPos, BlockState> edits,
      Map<BlockPos, BlockState> baseline) {
    var previous = templateStates(level, "stairwell_v20");
    var recessed = templateStates(level, "stairwell_v21");
    for (var entry : edits.entrySet()) {
      BlockPos p = origin.offset(entry.getKey());
      if (!level.hasChunkAt(p)) return "Unloaded chunk at " + p.toShortString();
      BlockState actual = level.getBlockState(p), old = baseline.get(entry.getKey());
      boolean authoredAlarm = false;
      for (AlarmMove move : alarmMoves())
        authoredAlarm |=
            actual.is(move.block())
                && level.getBlockEntity(p) instanceof AlarmRotorEntity
                && ((entry.getKey().equals(move.from())
                        && actual.getValue(AlarmBlock.FACING) == Direction.WEST)
                    || (entry.getKey().equals(move.to())
                        && actual.getValue(AlarmBlock.FACING) == Direction.NORTH));
      // Only the two explicitly requested alarm rotors may move. Payload/state are preserved.
      if (level.getBlockEntity(p) != null && !authoredAlarm)
        return "Block entity at " + p.toShortString();
      if (authoredAlarm) continue;
      if (actual.equals(entry.getValue())
          || actual.equals(old)
          || actual.equals(previous.get(entry.getKey()))
          || actual.equals(recessed.get(entry.getKey()))) continue;
      // Sparse structure-void cells contain natural terrain, not authored air. Only ordinary
      // excavation rock is accepted here, never arbitrary blocks, water, containers or machines.
      if (old == null
          && (actual.is(Blocks.AIR)
              || actual.is(Blocks.STONE)
              || actual.is(Blocks.DEEPSLATE)
              || actual.is(Blocks.GRANITE)
              || actual.is(Blocks.DIORITE)
              || actual.is(Blocks.ANDESITE))) continue;
      return "Changed/unsupported block at " + p.toShortString();
    }
    for (AlarmMove move : alarmMoves())
      if (level.getBlockState(origin.offset(move.from())).is(move.block())
          && level.getBlockState(origin.offset(move.to())).is(move.block()))
        return "Both alarm mounts are occupied; no fixture will be overwritten";
    return null;
  }

  static int apply(ServerLevel level, BlockPos origin, Map<BlockPos, BlockState> edits) {
    int changed = 0;
    Map<BlockPos, BlockState> movedStates = new LinkedHashMap<>();
    Map<BlockPos, CompoundTag> movedData = new LinkedHashMap<>();
    for (AlarmMove move : alarmMoves()) {
      BlockPos source = origin.offset(move.from());
      BlockState state = level.getBlockState(source);
      if (state.is(move.block())) {
        movedStates.put(move.to(), state.setValue(AlarmBlock.FACING, Direction.NORTH));
        var rotor = level.getBlockEntity(source);
        if (rotor != null)
          movedData.put(move.to(), rotor.saveWithoutMetadata(level.registryAccess()));
      }
    }
    // Explicit authored shapes; suppress neighbour stair-shape rewrites during the batch.
    for (var entry : edits.entrySet()) {
      BlockPos p = origin.offset(entry.getKey());
      BlockState desired = entry.getValue();
      boolean alreadyMounted = false;
      for (AlarmMove move : alarmMoves())
        if (entry.getKey().equals(move.to()) && level.getBlockState(p).is(move.block()))
          alreadyMounted = true;
      // A repeated repair must not reset a running or manually enabled alarm.
      if (alreadyMounted) continue;
      desired = movedStates.getOrDefault(entry.getKey(), desired);
      if (!level.getBlockState(p).equals(desired)) {
        level.setBlock(p, desired, Block.UPDATE_CLIENTS);
        CompoundTag data = movedData.get(entry.getKey());
        if (data != null && level.getBlockEntity(p) != null) {
          level.getBlockEntity(p).loadWithComponents(data, level.registryAccess());
          level.getBlockEntity(p).setChanged();
        }
        changed++;
      }
    }
    return changed;
  }

  @SubscribeEvent
  public static void commands(RegisterCommandsEvent event) {
    event
        .getDispatcher()
        .register(
            Commands.literal("sarcstairs")
                .requires(s -> s.hasPermission(2))
                .executes(
                    c -> {
                      c.getSource()
                          .sendSuccess(
                              () ->
                                  Component.literal(
                                      "BACK UP YOUR WORLD FIRST. Stand inside the facility but outside the stairwell. "
                                          + "/sarcstairs repair confirm rebuilds only the glass-slot stairs for drawing geometry 5; "
                                          + "also lights its bottom hallway and moves the doorway beacon and siren beside the lift; refuses edited blocks, inventories, occupied space and unloaded chunks."),
                              false);
                      return 1;
                    })
                .then(
                    Commands.literal("repair")
                        .then(
                            Commands.literal("confirm")
                                .executes(
                                    c -> {
                                      var player = c.getSource().getPlayerOrException();
                                      ServerLevel level = player.serverLevel();
                                      if (level.dimension() != Level.OVERWORLD) return 0;
                                      for (long key :
                                          FacilityState.get(level.getServer()).sites.keySet()) {
                                        BlockPos origin = BlockPos.of(key);
                                        if (!BlueprintRooms.drawing(level, origin)
                                            || !FacilitySpawnProtection.contains(
                                                level, origin, player.blockPosition())) continue;
                                        var edits = plan(level);
                                        var bounds =
                                            new net.minecraft.world.phys.AABB(
                                                net.minecraft.world.phys.Vec3.atLowerCornerOf(
                                                    origin.offset(
                                                        REPAIR_MIN_X,
                                                        HALL_FLOOR - 1,
                                                        REPAIR_MIN_Z)),
                                                net.minecraft.world.phys.Vec3.atLowerCornerOf(
                                                    origin.offset(
                                                        LOWER_RIGHT + 2,
                                                        UPPER_CLEAR_TOP,
                                                        REPAIR_MAX_Z + 1)));
                                        if (!level.getEntities(null, bounds).isEmpty()) {
                                          c.getSource()
                                              .sendFailure(
                                                  Component.literal(
                                                      "Move players, mobs and dropped items outside the stairwell first. No blocks changed."));
                                          return 0;
                                        }
                                        String issue =
                                            obstruction(
                                                level,
                                                origin,
                                                edits,
                                                templateStates(level, "stairwell_baseline"));
                                        if (issue != null) {
                                          c.getSource()
                                              .sendFailure(
                                                  Component.literal(
                                                      issue
                                                          + ". No blocks changed. Restore/customise manually rather than forcing the repair."));
                                          return 0;
                                        }
                                        int count = apply(level, origin, edits);
                                        c.getSource()
                                            .sendSuccess(
                                                () ->
                                                    Component.literal(
                                                        "Glass-slot stairwell and beacon repaired: "
                                                            + count
                                                            + " blocks changed. West-facing recessed exit connected; old spiral removed and capped; original entrance glass retained; hallway strips installed; beacon and siren moved beside lift."),
                                                true);
                                        return 1;
                                      }
                                      c.getSource()
                                          .sendFailure(
                                              Component.literal(
                                                  "Stand inside a registered drawing-layout SARC facility. No blocks changed."));
                                      return 0;
                                    }))));
  }

  private GlassStairwell() {}
}
