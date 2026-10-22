package com.timelocked;

import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Places the existing drivable SARC truck once per coastal site. No prop or fast travel. */
@EventBusSubscriber(modid = "timelocked")
public final class CoastalTruck {
  static BlockPos anchor(BlockPos site) {
    return IntroCoast.roadJunction(site).offset(254, 0, -39);
  }

  static Vec3 parkingPosition(BlockPos site) {
    return Vec3.atBottomCenterOf(anchor(site));
  }

  static String tag(BlockPos site) {
    return "timelocked.coastal_truck." + site.asLong();
  }

  /** 1 = safe new placement, 0 = already issued (even unloaded/moved), -1 = blocked. */
  static int install(ServerLevel level, BlockPos site, boolean confirm) {
    var state = FacilityState.get(level.getServer());
    var progress = state.progress.get(site.asLong());
    if (progress != null && progress.coastalTruck != null) return 0;
    SarcGroundVehicle truck = Content.TRUCK.get().create(level);
    if (truck == null) return -1;
    Vec3 feet = parkingPosition(site);
    truck.moveTo(feet.x, feet.y, feet.z, -90, 0);
    if (!VehicleSafety.loaded(level, truck.getBoundingBox().inflate(.1))) return -1;
    // Recover a loaded tagged truck if its placement record was not saved with the entity.
    var known =
        level.getEntitiesOfClass(
            SarcGroundVehicle.class,
            truck.getBoundingBox().inflate(8),
            e -> e.truck() && e.getTags().contains(tag(site)));
    if (!known.isEmpty()) {
      if (confirm) {
        SitePlanner.progress(level, site).coastalTruck = known.getFirst().getUUID();
        state.setDirty();
      }
      return 0;
    }
    var box = truck.getBoundingBox();
    for (int x = (int) Math.floor(box.minX); x <= (int) Math.floor(box.maxX); x++)
      for (int z = (int) Math.floor(box.minZ); z <= (int) Math.floor(box.maxZ); z++) {
        var floor = new BlockPos(x, anchor(site).getY() - 1, z);
        var paving = level.getBlockState(floor);
        if (!(paving.is(Blocks.BLACK_CONCRETE)
            || paving.is(Blocks.WHITE_CONCRETE)
            || paving.is(Blocks.GRAY_CONCRETE))) return -1;
        for (int y = anchor(site).getY(); y < Math.ceil(box.maxY); y++) {
          var at = new BlockPos(x, y, z);
          if (!level.getBlockState(at).isAir() || level.getBlockEntity(at) != null) return -1;
        }
      }
    if (!level.getEntities(null, box).isEmpty() || !VehicleSafety.deployment(truck)) return -1;
    if (!confirm) return 1;
    truck.addTag(tag(site));
    if (!level.addFreshEntity(truck)) return -1;
    SitePlanner.progress(level, site).coastalTruck = truck.getUUID();
    state.setDirty();
    return 1;
  }

  @SubscribeEvent
  public static void tick(ServerTickEvent.Post event) {
    var server = event.getServer();
    if (server.overworld().getGameTime() % 20 != 0) return;
    for (var player : server.getPlayerList().getPlayers()) {
      var intro = IntroSequence.state(player);
      if (!CoastChapter.eligible(player, intro)) continue;
      var site = BlockPos.of(intro.getLong("Site"));
      if (player.distanceToSqr(parkingPosition(site)) <= 48 * 48)
        install(player.serverLevel(), site, true);
    }
  }

  static int repair(ServerPlayer player, boolean confirm) {
    var intro = IntroSequence.state(player);
    if (!CoastChapter.eligible(player, intro)) {
      message(player, "unavailable");
      return 0;
    }
    var site = BlockPos.of(intro.getLong("Site"));
    if (player.distanceToSqr(parkingPosition(site)) > 48 * 48) {
      message(player, "nearby");
      return 0;
    }
    int result = install(player.serverLevel(), site, confirm);
    message(
        player,
        result < 0 ? "bay_blocked" : result == 0 ? "present" : confirm ? "installed" : "preview");
    return result < 0 ? 0 : 1;
  }

  static void message(ServerPlayer player, String key) {
    player.sendSystemMessage(Component.translatable("message.timelocked.truck." + key));
  }

  @SubscribeEvent
  public static void commands(RegisterCommandsEvent event) {
    event
        .getDispatcher()
        .register(
            Commands.literal("coastaltruck")
                .requires(s -> s.hasPermission(2))
                .then(
                    Commands.literal("repair")
                        .executes(c -> repair(c.getSource().getPlayerOrException(), false))
                        .then(
                            Commands.literal("confirm")
                                .executes(
                                    c -> repair(c.getSource().getPlayerOrException(), true)))));
  }

  private CoastalTruck() {}
}
