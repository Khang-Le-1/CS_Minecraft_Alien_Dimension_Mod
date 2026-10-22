package com.timelocked;

import java.util.HashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Tests actual authored openings, paired interaction and collision, never shipped as an entry
 * point.
 */
final class TransitionDoorwaysAudit {
  private static void check(boolean condition, String label) {
    IntroAudit.check(condition, "transition doorways: " + label);
  }

  static void run(MinecraftServer server) throws Exception {
    var level = server.overworld();
    BlockPos origin = new BlockPos(68000, 80, 68000);
    var plan = TransitionDoorways.plan();
    check(plan.size() == 92, "five framed thresholds contain 92 cells");
    check(TransitionDoorways.obstruction(level, origin, plan) != null, "unloaded world refused");
    var modules = new HashMap<String, HashMap<BlockPos, BlockState>>();
    for (var local : plan.keySet()) {
      String id =
          String.format(
              java.util.Locale.ROOT,
              "sarc/drawing/x%02d_y00_z%02d",
              local.getX() / 32,
              local.getZ() / 32);
      if (!modules.containsKey(id)) {
        var tag =
            level
                .getStructureManager()
                .get(ResourceLocation.fromNamespaceAndPath("timelocked", id))
                .orElseThrow()
                .save(new CompoundTag());
        var states = new HashMap<BlockPos, BlockState>();
        var palette = tag.getList("palette", Tag.TAG_COMPOUND);
        var blocks = tag.getList("blocks", Tag.TAG_COMPOUND);
        for (int i = 0; i < blocks.size(); i++) {
          var row = blocks.getCompound(i);
          var pos = row.getList("pos", Tag.TAG_INT);
          states.put(
              new BlockPos(pos.getInt(0), pos.getInt(1), pos.getInt(2)),
              NbtUtils.readBlockState(
                  level.holderLookup(Registries.BLOCK), palette.getCompound(row.getInt("state"))));
        }
        modules.put(id, states);
      }
      var inModule = new BlockPos(local.getX() % 32, local.getY(), local.getZ() % 32);
      check(
          modules.get(id).get(inModule).isAir(),
          "doorway occupies an actual authored opening " + local);
      var position = origin.offset(local);
      level.getChunkAt(position);
      level.setBlock(position, Blocks.AIR.defaultBlockState(), 3);
      level.setBlock(position.atY(origin.getY() + 1), Content.FLOOR.get().defaultBlockState(), 3);
    }
    BlockPos probe = origin.offset(plan.keySet().iterator().next());
    check(TransitionDoorways.obstruction(level, origin, plan) == null, "clear opening passes");
    level.setBlock(probe, Blocks.DIAMOND_BLOCK.defaultBlockState(), 3);
    check(
        TransitionDoorways.obstruction(level, origin, plan) != null,
        "custom edit refuses entire repair");
    level.setBlock(probe, Blocks.CHEST.defaultBlockState(), 3);
    check(
        level.getBlockEntity(probe) != null
            && TransitionDoorways.obstruction(level, origin, plan) != null,
        "container refuses repair");
    level.setBlock(probe, Blocks.AIR.defaultBlockState(), 3);
    var item =
        new net.minecraft.world.entity.item.ItemEntity(
            level,
            probe.getX() + .5,
            probe.getY() + .1,
            probe.getZ() + .5,
            new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STONE));
    level.addFreshEntity(item);
    check(
        TransitionDoorways.obstruction(level, origin, plan) != null,
        "occupied doorway refuses repair");
    item.discard();
    check(TransitionDoorways.apply(level, origin, plan) == 92, "all five thresholds installed");
    check(
        TransitionDoorways.obstruction(level, origin, plan) == null,
        "own door entities accepted on repeat");
    check(TransitionDoorways.apply(level, origin, plan) == 0, "repeat changes zero blocks");
    var player = new IntroAudit.Listener(level);
    int leaves = 0, pairs = 0;
    for (var entry : plan.entrySet()) {
      BlockState desired = entry.getValue();
      BlockPos lower = origin.offset(entry.getKey());
      check(
          level.getBlockState(lower).equals(desired),
          "installed state matches plan " + entry.getKey());
      if (!desired.is(FacilityDetails.PUSH_BAR_DOOR.get())
          || desired.getValue(DoorBlock.HALF) != DoubleBlockHalf.LOWER) continue;
      leaves++;
      check(level.getBlockEntity(lower) instanceof SarcDoorEntity, "animated lower entity exists");
      check(
          FacilityDetails.DOOR_ENTITY.get().isValid(desired),
          "door entity type supports new block");
      BlockPos mate = PushBarDoorBlock.mate(level, lower, desired);
      if (mate != null) pairs++;
      var center =
          mate == null
              ? Vec3.atLowerCornerOf(lower).add(.5, 0, .5)
              : Vec3.atLowerCornerOf(lower)
                  .add(Vec3.atLowerCornerOf(mate))
                  .scale(.5)
                  .add(.5, 0, .5);
      boolean eastWest = desired.getValue(DoorBlock.FACING).getAxis() == Direction.Axis.X;
      var box =
          new AABB(
              center.x - (eastWest ? .5 : .3),
              center.y,
              center.z - (eastWest ? .3 : .5),
              center.x + (eastWest ? .5 : .3),
              center.y + 1.8,
              center.z + (eastWest ? .3 : .5));
      check(!level.noCollision(player, box), "closed door conceals and blocks passage");
      var block = FacilityDetails.PUSH_BAR_DOOR.get();
      block.useWithoutItem(
          desired,
          level,
          lower,
          player,
          new BlockHitResult(
              Vec3.atCenterOf(lower), desired.getValue(DoorBlock.FACING), lower, false));
      check(
          level.getBlockState(lower).getValue(DoorBlock.OPEN)
              && level.getBlockState(lower.above()).getValue(DoorBlock.OPEN),
          "hand opens both halves without key or power");
      if (mate != null)
        check(
            level.getBlockState(mate).getValue(DoorBlock.OPEN)
                && level.getBlockState(mate.above()).getValue(DoorBlock.OPEN),
            "paired leaf also opens");
      check(level.noCollision(player, box), "open doorway has player-width collision clearance");
      check(
          TransitionDoorways.apply(level, origin, plan) == 0
              && level.getBlockState(lower).getValue(DoorBlock.OPEN),
          "repeat repair preserves open state");
      var upper = lower.above();
      block.useWithoutItem(
          level.getBlockState(upper),
          level,
          upper,
          player,
          new BlockHitResult(
              Vec3.atCenterOf(upper),
              desired.getValue(DoorBlock.FACING).getOpposite(),
              upper,
              false));
      check(
          !level.getBlockState(lower).getValue(DoorBlock.OPEN),
          "upper-half interaction from reverse side closes");
      if (mate != null)
        check(!level.getBlockState(mate).getValue(DoorBlock.OPEN), "paired leaf closes together");
    }
    check(leaves == 9 && pairs == 8, "one single door and four paired entrances");
    for (var local : plan.keySet())
      for (BlockPos route : DrawingTram.ROUTE)
        check(local.distManhattan(route) > 2, "all door geometry stays clear of tram guideway");
    var southDoor = origin.offset(79, 2, 336);
    player.setPos(southDoor.getX() + .5, southDoor.getY(), southDoor.getZ() + .5);
    check(!HubArrival.inside(player, origin), "south door plane does not start arrival");
    player.setPos(southDoor.getX() + .5, southDoor.getY(), southDoor.getZ() - .5);
    check(HubArrival.inside(player, origin), "crossing into lobby starts arrival");
    player.setPos(origin.getX() + 153.5, origin.getY() + 2, origin.getZ() + 272.5);
    check(!HubArrival.inside(player, origin), "tram door plane excluded from arrival");
    player.setPos(origin.getX() + 152.5, origin.getY() + 2, origin.getZ() + 272.5);
    check(HubArrival.inside(player, origin), "lobby side of tram door still counts");
    check(
        server.getCommands().getDispatcher().getRoot().getChild("sarcdoors") != null,
        "operator repair command registered");
  }

  private TransitionDoorwaysAudit() {}
}
