package com.timelocked;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Native collision-shape and saved-world repair checks. Never shipped as mod entry points. */
final class GlassStairwellAudit {
  static void check(boolean value, String label) {
    IntroAudit.check(value, "glass-slot stairwell: " + label);
  }

  static void run(MinecraftServer server) throws Exception {
    ServerLevel level = server.overworld();
    BlockPos origin = new BlockPos(28000, 80, 28000);
    var baseline = GlassStairwell.templateStates(level, "stairwell_baseline");
    var fresh = GlassStairwell.templateStates(level, "drawing");
    var edits = GlassStairwell.plan(level);
    var export = new com.google.gson.JsonArray();
    for (var e : edits.entrySet()) {
      var row = new com.google.gson.JsonObject();
      var xyz = new com.google.gson.JsonArray();
      xyz.add(e.getKey().getX());
      xyz.add(e.getKey().getY());
      xyz.add(e.getKey().getZ());
      row.add("p", xyz);
      row.add(
          "s",
          BlockState.CODEC
              .encodeStart(com.mojang.serialization.JsonOps.INSTANCE, e.getValue())
              .getOrThrow());
      export.add(row);
    }
    java.nio.file.Files.writeString(IntroAudit.output("glass-stair-plan.json"), export.toString());
    for (var e : edits.entrySet())
      check(
          e.getValue().equals(fresh.get(e.getKey())),
          "baked template matches runtime repair " + e.getKey());
    int glass = 0, unaffected = 0;
    for (var e : baseline.entrySet()) {
      if (e.getValue().is(Blocks.GLASS) && e.getKey().getY() >= 29) {
        glass++;
        check(
            e.getValue().equals(fresh.get(e.getKey())),
            "original upper glazing preserved " + e.getKey());
      }
      if (!edits.containsKey(e.getKey())) {
        unaffected++;
        check(e.getValue().equals(fresh.get(e.getKey())), "outside repair unchanged " + e.getKey());
      }
    }
    check(glass >= 15 && unaffected > 1000, "real surface glass and surrounding room tested");
    // Explicitly emulate sparse old templates in underground rock, not an all-air toy staircase.
    for (var e : baseline.entrySet()) {
      var p = origin.offset(e.getKey());
      level.getChunkAt(p);
      level.setBlock(p, e.getValue(), 2);
    }
    for (var p : edits.keySet())
      if (!baseline.containsKey(p))
        level.setBlock(origin.offset(p), Blocks.STONE.defaultBlockState(), 2);
    check(
        GlassStairwell.obstruction(level, origin, edits, baseline) == null,
        "original site passes repair preflight");
    BlockPos probe = origin.offset(90, 27, 348);
    BlockState saved = level.getBlockState(probe);
    level.setBlock(probe, Blocks.DIAMOND_BLOCK.defaultBlockState(), 2);
    check(
        GlassStairwell.obstruction(level, origin, edits, baseline) != null,
        "custom player edit refuses repair before mutation");
    check(
        level.getBlockState(probe).is(Blocks.DIAMOND_BLOCK),
        "rejected preflight leaves custom edit intact");
    level.setBlock(probe, Blocks.CHEST.defaultBlockState(), 2);
    check(
        level.getBlockEntity(probe) != null
            && GlassStairwell.obstruction(level, origin, edits, baseline) != null,
        "container refuses repair");
    level.setBlock(probe, saved, 2);
    int changed = GlassStairwell.apply(level, origin, edits);
    check(changed > 1000, "old facility actually rebuilt");
    check(
        GlassStairwell.obstruction(level, origin, edits, baseline) == null,
        "already-repaired world passes preflight");
    check(GlassStairwell.apply(level, origin, edits) == 0, "repeat repair is idempotent");
    BlockPos unloaded = new BlockPos(300000, 80, 300000);
    check(
        !level.hasChunkAt(unloaded.offset(edits.keySet().iterator().next())),
        "unloaded test chunk starts absent");
    check(
        GlassStairwell.obstruction(level, unloaded, edits, baseline).startsWith("Unloaded chunk"),
        "repair refuses unloaded chunks rather than generating them");
    check(
        !level.hasChunkAt(unloaded.offset(edits.keySet().iterator().next())),
        "preflight did not load distant chunk");
    var node = server.getCommands().getDispatcher().getRoot().getChild("sarcstairs");
    check(
        !node.canUse(server.createCommandSourceStack().withPermission(0)),
        "non-operator cannot use repair command");
    check(
        node.canUse(server.createCommandSourceStack().withPermission(2)),
        "operator can use repair command");
    var facilities = FacilityState.get(server);
    var previousSite = facilities.sites.put(origin.asLong(), 42);
    var progress = new SiteProgress();
    progress.geometryVersion = BlueprintRooms.VERSION;
    var previousProgress = facilities.progress.put(origin.asLong(), progress);
    try {
      var player = new IntroAudit.Listener(level);
      player.setPos(origin.getX() + 79.5, origin.getY() + 2, origin.getZ() + 330.5);
      var source = player.createCommandSourceStack().withPermission(2);
      var item =
          new net.minecraft.world.entity.item.ItemEntity(
              level,
              origin.getX() + 90.5,
              origin.getY() + 29,
              origin.getZ() + 348.5,
              new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STONE));
      level.addFreshEntity(item);
      try {
        check(
            server.getCommands().getDispatcher().execute("sarcstairs repair confirm", source) == 0,
            "real operator command refuses occupied stairwell");
      } finally {
        item.discard();
      }
      check(
          server.getCommands().getDispatcher().execute("sarcstairs repair confirm", source) == 1,
          "real operator command repairs an empty registered facility");
      check(
          player.messages.stream().anyMatch(m -> m.contains("0 blocks changed")),
          "real repeated command reports zero changed blocks");
    } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
      throw new IllegalStateException(e);
    } finally {
      if (previousSite == null) facilities.sites.remove(origin.asLong());
      else facilities.sites.put(origin.asLong(), previousSite);
      if (previousProgress == null) facilities.progress.remove(origin.asLong());
      else facilities.progress.put(origin.asLong(), previousProgress);
    }
    var triggerPlayer = new IntroAudit.Listener(level);
    for (int x = 88; x <= 100; x++)
      for (int z = 333; z <= 336; z++) {
        triggerPlayer.setPos(origin.getX() + x + .5, origin.getY() + 16, origin.getZ() + z + .5);
        check(
            !HubArrival.inside(triggerPlayer, origin),
            "turning landing never triggers lobby " + x + "," + z);
      }
    triggerPlayer.setPos(origin.getX() + 86.5, origin.getY() + 2, origin.getZ() + 330.5);
    check(HubArrival.inside(triggerPlayer, origin), "real lobby still triggers arrival");
    triggerPlayer.setPos(origin.getX() + 90.5, origin.getY() + 2, origin.getZ() + 331.5);
    check(
        HubArrival.inside(triggerPlayer, origin),
        "lobby immediately outside stair enclosure still triggers");
    triggerPlayer.setPos(origin.getX() + 94.5, origin.getY() + 2, origin.getZ() + 334.5);
    check(HubArrival.inside(triggerPlayer, origin), "lobby floor beneath overhang is not excluded");
    for (int x : new int[] {88, 92, 96, 100}) {
      var p = origin.offset(x, 6, 357);
      check(
          level.getBlockState(p).getLightEmission(level, p) == 12,
          "bottom hallway permanent ceiling strip " + x);
    }
    for (int z : new int[] {350, 353}) {
      var p = origin.offset(101, 4, z);
      check(
          level.getBlockState(p).getLightEmission(level, p) == 12,
          "return passage permanent wall strip " + z);
    }
    check(
        level.getBlockState(origin.offset(GlassStairwell.OLD_BEACON)).isAir(),
        "doorway beacon removed");
    var mount = origin.offset(GlassStairwell.WALL_BEACON);
    check(
        level.getBlockEntity(mount) instanceof AlarmRotorEntity,
        "relocated beacon has its rotor entity");
    check(
        level.getBlockState(mount).getValue(AlarmBlock.FACING)
            == net.minecraft.core.Direction.NORTH,
        "beacon faces out from the south wall beside lift");
    check(
        level
            .getBlockState(origin.offset(GlassStairwell.BEACON_SUPPORT))
            .is(FacilityArchitecture.CLEAN.get()),
        "beacon mounted against photographed clean-panel wall");
    check(
        java.util.Arrays.stream(DrawingAlarmIndex.CELLS)
            .anyMatch(v -> new BlockPos(v[0], v[1], v[2]).equals(GlassStairwell.WALL_BEACON)),
        "new beacon mount linked to facility alarm group");
    check(
        level.getBlockState(origin.offset(GlassStairwell.OLD_SIREN)).isAir(),
        "old doorway siren removed");
    var siren = origin.offset(GlassStairwell.WALL_SIREN);
    check(
        level.getBlockState(siren).is(FacilityDetails.ALARM_SIREN.get())
            && level.getBlockEntity(siren) instanceof AlarmRotorEntity
            && level.getBlockState(siren).getValue(AlarmBlock.FACING)
                == net.minecraft.core.Direction.NORTH,
        "siren beside beacon faces out from photographed wall");
    int lamps = 0;
    for (var e : edits.entrySet())
      if (e.getValue().is(FacilityDetails.EMERGENCY.get())) {
        lamps++;
        check(
            level
                    .getBlockState(origin.offset(e.getKey()))
                    .getLightEmission(level, origin.offset(e.getKey()))
                > 0,
            "emergency stair marker emits light before logo startup");
      }
    check(lamps >= 15, "both flights and landing have real emergency lighting");
    // Sweep a 0.6-wide, 1.8-high standing body in 1/8-block increments. Native voxel shapes
    // determine floor height (including half treads); every increment must allow normal stepping.
    segment(level, origin, 90.5, 350.5, 90.5, 334.5, 0);
    segment(level, origin, 90.5, 334.5, 99.5, 334.5, 1);
    segment(level, origin, 99.5, 334.5, 99.5, 350.5, 2);
    segment(level, origin, 99.5, 350.5, 99.5, 356.5, 3);
    segment(level, origin, 99.5, 356.5, 82.5, 356.5, 3);
    for (int x = 88; x <= 92; x++)
      for (int z = 353; z <= 359; z++) {
        check(
            level.getBlockState(origin.offset(x, 28, z)).is(Blocks.OAK_PLANKS),
            "old spiral upper hole capped " + x + "," + z);
        for (int y = 2; y <= 27; y++)
          check(
              !level.getBlockState(origin.offset(x, y, z)).is(Blocks.STONE_BRICK_STAIRS),
              "old spiral tread removed " + x + "," + y + "," + z);
      }
    check(
        level.getBlockState(origin.offset(93, 4, 355)).isAir(),
        "photographed F3 target wall is open (909,43,1747 at anchor 816,39,1392)");
    check(
        !level.getBlockState(origin.offset(80, 10, 341)).is(Blocks.STONE_BRICK_STAIRS),
        "superseded 0.8.9.20 hallway stair is gone");
    var recessed = GlassStairwell.templateStates(level, "stairwell_v21");
    for (var e : recessed.entrySet()) level.setBlock(origin.offset(e.getKey()), e.getValue(), 2);
    check(
        GlassStairwell.obstruction(level, origin, edits, baseline) == null,
        "0.8.9.21 world accepts new hallway lights safely");
    check(
        GlassStairwell.apply(level, origin, edits) == 10,
        "0.8.9.21 repair adds six strips and relocates both alarms");
    check(GlassStairwell.apply(level, origin, edits) == 0, "0.8.9.21 upgrade idempotent");
    // Emulate a .22 world: ceiling lights and beacon already updated, siren still at old mount.
    level.setBlock(
        origin.offset(GlassStairwell.OLD_SIREN),
        FacilityDetails.ALARM_SIREN
            .get()
            .defaultBlockState()
            .setValue(AlarmBlock.FACING, net.minecraft.core.Direction.WEST)
            .setValue(AlarmBlock.MANUAL, true),
        2);
    level
        .getBlockEntity(origin.offset(GlassStairwell.OLD_SIREN))
        .getPersistentData()
        .putString("audit", "siren payload");
    level.setBlock(origin.offset(GlassStairwell.WALL_SIREN), Blocks.AIR.defaultBlockState(), 2);
    check(
        GlassStairwell.obstruction(level, origin, edits, baseline) == null,
        ".22 siren upgrade passes preflight");
    check(GlassStairwell.apply(level, origin, edits) == 2, ".22 upgrade only moves siren");
    check(
        level.getBlockState(origin.offset(GlassStairwell.WALL_SIREN)).getValue(AlarmBlock.MANUAL),
        "siren manual state retained");
    check(
        level
            .getBlockEntity(origin.offset(GlassStairwell.WALL_SIREN))
            .getPersistentData()
            .getString("audit")
            .equals("siren payload"),
        "siren payload transferred");
    check(GlassStairwell.apply(level, origin, edits) == 0, ".22 siren upgrade idempotent");
    var activeBeacon =
        FacilityDetails.ALARM_BEACON
            .get()
            .defaultBlockState()
            .setValue(AlarmBlock.FACING, net.minecraft.core.Direction.WEST)
            .setValue(AlarmBlock.ACTIVE, true)
            .setValue(AlarmBlock.MANUAL, true)
            .setValue(AlarmBlock.LIT, true);
    level.setBlock(origin.offset(GlassStairwell.WALL_BEACON), Blocks.AIR.defaultBlockState(), 2);
    level.setBlock(origin.offset(GlassStairwell.OLD_BEACON), activeBeacon, 2);
    level
        .getBlockEntity(origin.offset(GlassStairwell.OLD_BEACON))
        .getPersistentData()
        .putString("audit", "rotor payload");
    check(
        GlassStairwell.obstruction(level, origin, edits, baseline) == null,
        "active original beacon accepted");
    check(GlassStairwell.apply(level, origin, edits) == 2, "active beacon moved exactly once");
    check(
        level
            .getBlockState(origin.offset(GlassStairwell.WALL_BEACON))
            .equals(activeBeacon.setValue(AlarmBlock.FACING, net.minecraft.core.Direction.NORTH)),
        "active/manual/lit beacon state retained");
    check(
        GlassStairwell.obstruction(level, origin, edits, baseline) == null,
        "active moved beacon accepted on repeat");
    check(
        GlassStairwell.apply(level, origin, edits) == 0,
        "repeat repair preserves running beacon state");
    check(
        level
            .getBlockEntity(origin.offset(GlassStairwell.WALL_BEACON))
            .getPersistentData()
            .getString("audit")
            .equals("rotor payload"),
        "complete beacon block-entity payload transferred");
    level.setBlock(origin.offset(GlassStairwell.OLD_BEACON), activeBeacon, 2);
    check(
        GlassStairwell.obstruction(level, origin, edits, baseline) != null,
        "duplicate mounts refuse rather than overwrite a beacon");
    // Test an actual 0.8.9.20 world, not just the original and already-repaired cases.
    var previous = GlassStairwell.templateStates(level, "stairwell_v20");
    for (var e : previous.entrySet()) level.setBlock(origin.offset(e.getKey()), e.getValue(), 2);
    for (var p : edits.keySet())
      if (!previous.containsKey(p))
        level.setBlock(origin.offset(p), Blocks.STONE.defaultBlockState(), 2);
    check(
        GlassStairwell.obstruction(level, origin, edits, baseline) == null,
        "previous 0.8.9.20 repair also passes safe upgrade preflight");
    check(GlassStairwell.apply(level, origin, edits) > 0, "previous release actually upgraded");
    check(GlassStairwell.apply(level, origin, edits) == 0, "upgraded release is idempotent");
    for (var e : edits.entrySet())
      check(
          level.getBlockState(origin.offset(e.getKey())).equals(e.getValue()),
          "upgraded world equals fresh authored route " + e.getKey());
    check(
        server.getCommands().getDispatcher().getRoot().getChild("sarcstairs") != null,
        "operator repair command registered on native server");
  }

  static void segment(
      ServerLevel level, BlockPos origin, double ax, double az, double bx, double bz, int section) {
    int samples = (int) Math.ceil(Math.hypot(bx - ax, bz - az) * 8);
    double previous = Double.NaN;
    var walker = new IntroAudit.Listener(level);
    for (int i = 0; i <= samples; i++) {
      double x = ax + (bx - ax) * i / samples, z = az + (bz - az) * i / samples;
      double rough =
          section == 0
              ? (z >= 349 ? 29 : z < 336 ? 16 : Math.floor(z) - 319)
              : section == 1
                  ? 16
                  : section == 2 ? (z < 336 ? 16 : z >= 350 ? 2 : 352 - Math.floor(z)) : 2;
      double top = floor(level, origin, x, z, rough);
      check(Double.isFinite(top), "supported walking surface at " + x + "," + z);
      if (!Double.isFinite(top)) continue;
      walker.setPos(origin.getX() + x, origin.getY() + top, origin.getZ() + z);
      check(
          !HubArrival.inside(walker, origin),
          "walking staircase does not start arrival at " + x + "," + top + "," + z);
      var body =
          new AABB(
              origin.getX() + x - .3,
              origin.getY() + top + .00001,
              origin.getZ() + z - .3,
              origin.getX() + x + .3,
              origin.getY() + top + 1.8,
              origin.getZ() + z + .3);
      check(
          level.noCollision(body),
          "standing body clears walls/ceiling at " + x + "," + top + "," + z);
      if (Double.isFinite(previous))
        check(
            Math.abs(previous - top) <= .5,
            "continuous half-step supports descending AND ascending at " + x + "," + z);
      previous = top;
    }
  }

  static double floor(ServerLevel level, BlockPos origin, double x, double z, double rough) {
    double best = Double.NEGATIVE_INFINITY;
    for (int bx = (int) Math.floor(x - .3); bx <= (int) Math.floor(x + .3); bx++)
      for (int bz = (int) Math.floor(z - .3); bz <= (int) Math.floor(z + .3); bz++)
        for (int y = (int) rough - 3; y <= (int) rough + 1; y++) {
          BlockPos p = origin.offset(bx, y, bz);
          for (AABB box : level.getBlockState(p).getCollisionShape(level, p).toAabbs()) {
            double top = y + box.maxY;
            if (bx + box.maxX > x - .3
                && bx + box.minX < x + .3
                && bz + box.maxZ > z - .3
                && bz + box.minZ < z + .3
                && top <= rough + 1
                && top >= rough - 2) best = Math.max(best, top);
          }
        }
    return best;
  }

  private GlassStairwellAudit() {}
}
