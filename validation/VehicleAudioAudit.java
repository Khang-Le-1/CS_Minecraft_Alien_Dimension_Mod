package com.timelocked;

import java.util.ArrayList;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
import net.neoforged.neoforge.event.entity.EntityMountEvent;

/** Real server event checks; client mixing is not represented as an audible playthrough. */
final class VehicleAudioAudit {
  static void check(boolean condition, String label) {
    IntroAudit.check(condition, "user audio: " + label);
  }

  static void run(MinecraftServer server) throws Exception {
    var level = server.overworld();
    var heard = new ArrayList<String>();
    Consumer<PlayLevelSoundEvent.AtPosition> sounds =
        e -> {
          if (e.getLevel() == level && e.getSound() != null)
            heard.add(e.getSound().value().getLocation().getPath());
        };
    NeoForge.EVENT_BUS.addListener(PlayLevelSoundEvent.AtPosition.class, sounds);
    try {
      for (String name :
          new String[] {
            "truck_in_left",
            "truck_in_right",
            "truck_out_left",
            "truck_out_right",
            "truck_idle",
            "truck_drive",
            "pushbar_open",
            "pushbar_close"
          })
        check(
            BuiltInRegistries.SOUND_EVENT.containsKey(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("timelocked", name)),
            "registered sound " + name);
      for (float yaw : new float[] {0, 90, 180, -90}) {
        double angle = Math.toRadians(yaw);
        check(
            TruckSounds.leftSide(yaw, Math.cos(angle) * 4, Math.sin(angle) * 4),
            "left side follows heading " + yaw);
        check(
            !TruckSounds.leftSide(yaw, -Math.cos(angle) * 4, -Math.sin(angle) * 4),
            "right side follows heading " + yaw);
      }
      check(TruckSounds.driveMix(0) == 0, "idle mix at rest");
      check(
          TruckSounds.driveMix(.075F) > 0 && TruckSounds.driveMix(.075F) < 1,
          "intermediate driving crossfade");
      check(
          TruckSounds.driveMix(.6F) == 1 && TruckSounds.driveMix(-.2F) == 1,
          "forward and reverse driving mix");
      check(TruckSounds.driveMix(Float.NaN) == 0, "invalid mix input safe");
      BlockPos o = new BlockPos(96000, 80, 96000);
      for (int x = -12; x <= 12; x++)
        for (int z = -12; z <= 12; z++) {
          level.getChunkAt(o.offset(x, 0, z));
          level.setBlock(o.offset(x, -1, z), Blocks.STONE.defaultBlockState(), 2);
          for (int y = 0; y < 5; y++)
            level.setBlock(o.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
        }
      var truck = Content.TRUCK.get().create(level);
      truck.moveTo(o.getX() + .5, o.getY(), o.getZ() + .5, 0, 0);
      level.addFreshEntity(truck);
      var driver = new CoastalTruckAudit.Driver(level);
      check(!truck.engineRunning(), "parked empty truck is silent");
      for (boolean left : new boolean[] {true, false}) {
        heard.clear();
        driver.setPos(truck.getX() + (left ? 4 : -4), truck.getY(), truck.getZ());
        truck.interact(driver, InteractionHand.MAIN_HAND);
        check(
            driver.getVehicle() == truck && truck.engineRunning(),
            "successful mount starts engine");
        check(
            heard.equals(java.util.List.of(left ? "truck_in_left" : "truck_in_right")),
            "one side-specific entry clip " + left);
        truck.interact(driver, InteractionHand.OFF_HAND);
        check(heard.size() == 1, "occupied/offhand mount cannot replay entry");
        Consumer<EntityMountEvent> denyExit =
            e -> {
              if (e.getEntityMounting() == driver && e.isDismounting()) e.setCanceled(true);
            };
        NeoForge.EVENT_BUS.addListener(EntityMountEvent.class, denyExit);
        try {
          driver.stopRiding();
          check(
              driver.getVehicle() == truck && truck.engineRunning() && heard.size() == 1,
              "canceled dismount makes no exit sound or engine stop");
        } finally {
          NeoForge.EVENT_BUS.unregister(denyExit);
        }
        truck.tickCount = 60;
        truck.tick();
        check(
            truck.engineRunning() && heard.size() == 1,
            "occupied idle stays running without server loop or minecart spam");
        Vec3 exit = VehicleSafety.exit(truck, driver).orElseThrow();
        boolean outLeft =
            TruckSounds.leftSide(truck.getYRot(), exit.x - truck.getX(), exit.z - truck.getZ());
        driver.stopRiding();
        check(!driver.isPassenger() && !truck.engineRunning(), "successful dismount stops engine");
        check(
            heard.size() == 2
                && heard.get(1).equals(outLeft ? "truck_out_left" : "truck_out_right"),
            "one exit clip follows safe dismount side");
      }
      var rover = Content.ROVER.get().create(level);
      rover.moveTo(o.getX() + 7.5, o.getY(), o.getZ() + .5, 0, 0);
      level.addFreshEntity(rover);
      heard.clear();
      driver.setPos(rover.getX() + 4, rover.getY(), rover.getZ());
      rover.interact(driver, InteractionHand.MAIN_HAND);
      check(
          driver.getVehicle() == rover && !rover.engineRunning() && heard.isEmpty(),
          "rover does not receive truck sounds");
      driver.stopRiding();
      check(heard.isEmpty(), "rover exit audio unchanged");
      rover.discard();
      truck.discard();
      check(!truck.engineRunning(), "removed truck cannot keep engine active");
      var block = FacilityDetails.PUSH_BAR_DOOR.get();
      check(
          block.type().doorOpen() == PushBarDoorSounds.OPEN
              && block.type().doorClose() == PushBarDoorSounds.CLOSE,
          "only push-bar type uses custom open/close events");
      BlockPos door = o.offset(-6, 0, 0);
      CampusNeighborhood.door(level, door, Direction.SOUTH, DoorHingeSide.RIGHT, block);
      CampusNeighborhood.door(level, door.east(), Direction.SOUTH, DoorHingeSide.LEFT, block);
      var player = new IntroAudit.Listener(level);
      heard.clear();
      block.useWithoutItem(
          level.getBlockState(door),
          level,
          door,
          player,
          new BlockHitResult(Vec3.atCenterOf(door), Direction.SOUTH, door, false));
      check(
          heard.equals(java.util.List.of("pushbar_open")),
          "one open clip for paired manual push-bar door");
      check(
          level.getBlockState(door.east().above()).getValue(DoorBlock.OPEN),
          "quiet mate still updates upper half");
      heard.clear();
      block.useWithoutItem(
          level.getBlockState(door.above()),
          level,
          door.above(),
          player,
          new BlockHitResult(Vec3.atCenterOf(door.above()), Direction.NORTH, door.above(), false));
      check(
          heard.equals(java.util.List.of("pushbar_close")),
          "one close clip from upper-half interaction");
      heard.clear();
      block.setOpen(null, level, level.getBlockState(door), door, false);
      check(heard.isEmpty(), "no-op setOpen does not replay audio");
      block.setOpen(null, level, level.getBlockState(door), door, true);
      check(
          heard.equals(java.util.List.of("pushbar_open")),
          "scripted opening uses supplied recording");
      java.nio.file.Files.writeString(
          IntroAudit.output("vehicle-audio-verification.json"),
          "{\"status\":\"PASS\",\"method\":\"native server sound events and state transitions; no client listening test\"}\n");
    } finally {
      NeoForge.EVENT_BUS.unregister(sounds);
    }
  }

  private VehicleAudioAudit() {}
}
