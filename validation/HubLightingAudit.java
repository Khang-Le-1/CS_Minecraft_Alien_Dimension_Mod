package com.timelocked;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.Blocks;

/** Dedicated-server checks for the opt-in lobby lighting retrofit. */
final class HubLightingAudit {
  private static void check(boolean condition, String label) {
    IntroAudit.check(condition, "lobby lighting: " + label);
  }

  static void run(MinecraftServer server) {
    var level = server.overworld();
    BlockPos origin = new BlockPos(48000, 80, 48000);
    var plan = HubLighting.plan();
    check(plan.size() == 632, "625 regular floor lamps plus seven reception lights");
    check(HubLighting.apply(level, origin, true) == -1, "unloaded room refused without generation");
    for (var local : plan.keySet()) {
      BlockPos position = origin.offset(local);
      level.getChunkAt(position);
      level.setBlock(position, Content.FLOOR.get().defaultBlockState(), 3);
      level.setBlock(position.above(), Blocks.AIR.defaultBlockState(), 3);
      level.setBlock(position.above(2), Blocks.AIR.defaultBlockState(), 3);
      check(local.getY() == 1 && local.getZ() < 333, "lights stay flush and outside stairs");
    }
    BlockPos probe = origin.offset(plan.keySet().iterator().next());
    check(HubLighting.apply(level, origin, false) == plan.size(), "preview counts all clear tiles");
    check(level.getBlockState(probe).is(Content.FLOOR.get()), "preview makes no edits");
    level.setBlock(probe, Blocks.CHEST.defaultBlockState(), 3);
    check(!HubLighting.eligible(level, probe), "container protected");
    level.setBlock(probe, Content.HAZARD.get().defaultBlockState(), 3);
    check(!HubLighting.eligible(level, probe), "hazard markings protected");
    level.setBlock(probe, Blocks.DIAMOND_BLOCK.defaultBlockState(), 3);
    check(!HubLighting.eligible(level, probe), "nonstandard player block protected");
    level.setBlock(probe, Blocks.AIR.defaultBlockState(), 3);
    check(!HubLighting.eligible(level, probe), "missing floor never filled");
    level.setBlock(probe, Blocks.POLISHED_ANDESITE.defaultBlockState(), 3);
    check(HubLighting.eligible(level, probe), "authored andesite floor supported");
    level.setBlock(probe, Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(), 3);
    check(HubLighting.eligible(level, probe), "authored concrete floor supported");
    level.setBlock(probe.above(), Blocks.WHITE_CARPET.defaultBlockState(), 3);
    check(!HubLighting.eligible(level, probe), "carpet and furnishings protected");
    level.setBlock(probe.above(), Blocks.AIR.defaultBlockState(), 3);
    level.setBlock(probe.above(2), Blocks.STONE.defaultBlockState(), 3);
    check(!HubLighting.eligible(level, probe), "obstructed headroom skipped");
    level.setBlock(probe.above(2), Blocks.AIR.defaultBlockState(), 3);
    check(HubLighting.apply(level, origin, true) == plan.size(), "all clear cells installed");
    for (var local : plan.keySet()) {
      BlockPos position = origin.offset(local);
      check(level.getBlockState(position).is(Blocks.SEA_LANTERN), "full-cube floor lamp installed");
      check(
          level.getBlockState(position).getLightEmission() == 15, "lamp emits maximum block light");
      check(level.getBlockState(position.above()).isAir(), "walking space unchanged");
    }
    check(HubLighting.apply(level, origin, true) == 0, "repeat changes zero blocks");
    for (var local : plan.keySet()) {
      BlockPos position = origin.offset(local);
      level.setBlock(position, Content.FLOOR.get().defaultBlockState(), 2);
    }
    check(
        SarcAmbientLighting.retrofit(level, origin, false) == 632,
        "architectural preview counts eligible floor");
    check(level.getBlockState(probe).is(Content.FLOOR.get()), "architectural preview is read only");
    level.setBlock(probe, Blocks.CHEST.defaultBlockState(), 2);
    check(
        SarcAmbientLighting.retrofit(level, origin, true) == 631,
        "architectural install protects containers");
    check(level.getBlockState(probe).is(Blocks.CHEST), "architectural install retains container");
    check(
        SarcAmbientLighting.retrofit(level, origin, true) == 0,
        "architectural repeat is idempotent");
    check(
        server.getCommands().getDispatcher().getRoot().getChild("sarclights") != null,
        "operator command registered");
  }

  private HubLightingAudit() {}
}
