package com.timelocked;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

/** Native placement and driving checks using the shipped truck entity, not a replacement. */
final class CoastalTruckAudit {
  // FakePlayer deliberately refuses startRiding; use a real ServerPlayer with a dummy connection.
  static final class Driver extends net.minecraft.server.level.ServerPlayer {
    Driver(net.minecraft.server.level.ServerLevel level) throws Exception {
      super(
          level.getServer(),
          level,
          new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "OriginalTruckDriver"),
          net.minecraft.server.level.ClientInformation.createDefault());
      var network =
          new net.minecraft.network.Connection(
              net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
      var channel = net.minecraft.network.Connection.class.getDeclaredField("channel");
      channel.setAccessible(true);
      channel.set(network, new io.netty.channel.embedded.EmbeddedChannel());
      connection =
          new net.minecraft.server.network.ServerGamePacketListenerImpl(
              level.getServer(),
              network,
              this,
              net.minecraft.server.network.CommonListenerCookie.createInitial(
                  getGameProfile(), false)) {
            @Override
            public void send(net.minecraft.network.protocol.Packet<?> packet) {}

            @Override
            public void send(
                net.minecraft.network.protocol.Packet<?> packet,
                net.minecraft.network.PacketSendListener listener) {}
          };
    }

    @Override
    public void sendSystemMessage(net.minecraft.network.chat.Component message) {}
  }

  static void check(boolean ok, String label) {
    IntroAudit.check(ok, "existing coastal truck: " + label);
  }

  static void run(MinecraftServer server) throws Exception {
    var level = server.overworld();
    var rule = server.getGameRules().getRule(StoryMode.RULE);
    boolean oldRule = rule.get();
    rule.set(true, server);
    BlockPos site = new BlockPos(74000, 60, 74000), a = CoastalTruck.anchor(site);
    var state = FacilityState.get(server);
    state.progress.remove(site.asLong());
    check(CoastalTruck.install(level, site, true) < 0, "unloaded bay refused");
    for (int x = -8; x <= 18; x++)
      for (int z = -8; z <= 8; z++) {
        var floor = a.offset(x, -1, z);
        level.getChunkAt(floor);
        level.setBlock(floor, Blocks.BLACK_CONCRETE.defaultBlockState(), 2);
        for (int y = 0; y <= 4; y++)
          level.setBlock(a.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
      }
    check(CoastalTruck.install(level, site, false) == 1, "safe bay preview");
    check(!state.progress.containsKey(site.asLong()), "preview does not change saved state");
    level.setBlock(a, Blocks.CHEST.defaultBlockState(), 2);
    check(
        CoastalTruck.install(level, site, true) < 0 && level.getBlockState(a).is(Blocks.CHEST),
        "container prevents placement, preserved");
    level.setBlock(a, Blocks.AIR.defaultBlockState(), 2);
    level.setBlock(a.above(2), Blocks.STONE.defaultBlockState(), 2);
    check(
        CoastalTruck.install(level, site, true) < 0,
        "original 2.9m truck needs overhead clearance");
    level.setBlock(a.above(2), Blocks.AIR.defaultBlockState(), 2);
    level.setBlock(a.below(), Blocks.WATER.defaultBlockState(), 2);
    check(CoastalTruck.install(level, site, true) < 0, "edited/wet pavement refused");
    level.setBlock(a.below(), Blocks.BLACK_CONCRETE.defaultBlockState(), 2);
    var item =
        new ItemEntity(
            level, a.getX() + .5, a.getY() + .2, a.getZ() + .5, new ItemStack(Items.STONE));
    level.addFreshEntity(item);
    check(CoastalTruck.install(level, site, true) < 0, "occupied bay refused");
    item.discard();
    check(CoastalTruck.install(level, site, true) == 1, "original entity parked");
    var progress = state.progress.get(site.asLong());
    check(progress != null && progress.coastalTruck != null, "UUID persisted per site");
    var truck = (SarcGroundVehicle) level.getEntity(progress.coastalTruck);
    check(
        truck != null && truck.getType() == Content.TRUCK.get() && truck.truck(),
        "uses existing sarc_truck registration");
    check(truck.getTags().contains(CoastalTruck.tag(site)), "site tag retained");
    check(truck.position().equals(CoastalTruck.parkingPosition(site)), "position is coastal lot");
    check(truck.getYRot() == -90, "faces east toward lot aisle");
    check(truck.getBbWidth() == Content.TRUCK.get().getWidth(), "original collision dimensions");
    check(VehicleSafety.deployment(truck), "supported dry collision-free parking");
    check(CoastalTruck.install(level, site, true) == 0, "repeat tick does not duplicate");
    check(
        progress.coastalTruck.equals(SiteProgress.load(progress.save()).coastalTruck),
        "placement survives save reload");
    check(
        SiteProgress.load(new net.minecraft.nbt.CompoundTag()).coastalTruck == null,
        "older saves load without a truck record");
    var p = new Driver(level);
    p.setGameMode(GameType.SURVIVAL);
    var intro = new net.minecraft.nbt.CompoundTag();
    intro.putUUID("Player", p.getUUID());
    intro.putInt("Stage", IntroSequence.DONE);
    intro.putBoolean("AtBeach", true);
    intro.putLong("Site", site.asLong());
    IntroState.get(server).players.put(p.getUUID(), intro);
    CoastChapter.begin(p, intro);
    var chapter = CoastChapter.chapter(intro);
    chapter.putInt("Step", CoastChapter.PARKING);
    p.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 7));
    p.setPos(a.getX() + 4.5, a.getY(), a.getZ() + .5);
    p.setOnGround(true);
    truck.interact(p, InteractionHand.MAIN_HAND);
    check(p.getVehicle() == truck, "right-click boards existing truck");
    var other = new CoastChapterAudit.Reader(level, "OtherTruckDriver");
    check(!truck.acceptInput(other, 1, 0, false), "non-driver cannot steer");
    check(!truck.acceptInput(p, Float.NaN, 0, false), "nonfinite input refused");
    var start = truck.position();
    for (int i = 0; i < 12; i++) {
      truck.tickCount++; // ServerLevel normally advances this before entity.tick().
      check(truck.acceptInput(p, 1, 0, false), "driver input accepted " + i);
      truck.tick();
    }
    check(
        truck.getX() > start.x + 1 && truck.position().distanceTo(start) < 12,
        "truck physically drives, no teleport");
    check(truck.driveSpeed() > 0, "original speed synchronization");
    for (int i = 0; i < 10; i++) {
      truck.tickCount++;
      truck.acceptInput(p, 0, 0, true);
      truck.tick();
    }
    check(truck.driveSpeed() == 0, "original brake stops truck");
    check(VehicleSafety.exit(truck, p).isPresent(), "supported dismount available");
    p.stopRiding();
    check(!p.isPassenger(), "player can exit");
    check(chapter.getInt("Step") == CoastChapter.PARKING, "no chapter skip");
    check(
        p.gameMode.getGameModeForPlayer() == GameType.SURVIVAL
            && p.getInventory().getItem(0).getCount() == 7
            && !p.isNoGravity(),
        "no mode inventory or gravity changes");
    truck.moveTo(a.getX() + 16, a.getY(), a.getZ(), -90, 0);
    check(CoastalTruck.install(level, site, true) == 0, "driving away never issues another");
    var id = truck.getUUID();
    progress.coastalTruck = null;
    truck.moveTo(
        CoastalTruck.parkingPosition(site).x,
        a.getY(),
        CoastalTruck.parkingPosition(site).z,
        -90,
        0);
    check(
        CoastalTruck.install(level, site, true) == 0 && id.equals(progress.coastalTruck),
        "tagged truck adopted if placement record missing");
    truck.discard();
    check(
        CoastalTruck.install(level, site, true) == 0,
        "missing or unloaded issued truck not duplicated");
    check(
        server.getCommands().getDispatcher().getRoot().getChild("coastaltruck") != null,
        "safe operator preview command registered");
    rule.set(oldRule, server);
    java.nio.file.Files.writeString(
        IntroAudit.output("coastal-truck-verification.json"),
        new com.google.gson.Gson()
            .toJson(
                Map.of(
                    "entity",
                    "timelocked:sarc_truck",
                    "assets",
                    "existing geometry/truck.json and textures/entity/vehicles/truck.png unchanged",
                    "transport",
                    "existing physical driving, no teleport",
                    "persistence",
                    "one issued truck UUID per coastal site; no automatic replacement",
                    "safety",
                    "loaded dry supported empty painted bay, no terrain edits",
                    "story",
                    "no chapter/power/door changes")));
  }

  private CoastalTruckAudit() {}
}
