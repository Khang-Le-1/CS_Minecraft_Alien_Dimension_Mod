package com.timelocked;

import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.lighting.*;

/**
 * Uses the vanilla block-light engine against real block states, synchronously for deterministic
 * tests.
 */
final class LightEngineAudit {
  static LevelLightEngine light(ServerLevel l, int x0, int x1, int z0, int z1) {
    var engine =
        new LevelLightEngine(
            new LightChunkGetter() {
              public LightChunk getChunkForLighting(int x, int z) {
                return l.getChunkSource().getChunkForLighting(x, z);
              }

              public BlockGetter getLevel() {
                return l;
              }
            },
            true,
            false);
    for (int x = x0 >> 4; x <= x1 >> 4; x++)
      for (int z = z0 >> 4; z <= z1 >> 4; z++) {
        var chunk = l.getChunk(x, z);
        for (int sec = l.getMinSection(); sec < l.getMaxSection(); sec++)
          engine.updateSectionStatus(
              SectionPos.of(x, sec, z), chunk.getSection(sec - l.getMinSection()).hasOnlyAir());
        engine.setLightEnabled(new ChunkPos(x, z), true);
        engine.propagateLightSources(new ChunkPos(x, z));
      }
    while (engine.hasLightWork()) engine.runLightUpdates();
    return engine;
  }

  static void run(
      net.minecraft.server.MinecraftServer server, ServerLevel l, BlockPos o, BlockPos s) {
    var engine = light(l, s.getX() + 72, s.getX() + 115, s.getZ() - 60, s.getZ() - 25);
    int minimum = 15, samples = 0;
    for (int x = 77; x <= 90; x++)
      for (int z = -48; z <= -31; z++) {
        var at = s.offset(x, 1, z);
        if (!l.getBlockState(at).getCollisionShape(l, at).isEmpty()) continue;
        int value = engine.getLayerListener(LightLayer.BLOCK).getLightValue(at);
        minimum = Math.min(minimum, value);
        samples++;
      }
    IntroAudit.check(
        samples > 100 && minimum >= 7,
        "classroom walking-height vanilla block light >=7; measured " + minimum);
    // A small real-floor fixture test isolates the lamp's useful radius from arbitrary terrain.
    BlockPos f = o.offset(110, 0, 55);
    CampusRebuild.box(l, f, -2, 0, -2, 26, 0, 26, Blocks.STONE);
    CampusRebuild.box(l, f, -2, 1, -2, 26, 1, 26, Content.FLOOR.get());
    CampusRebuild.box(l, f, -2, 2, -2, 26, 7, 26, Blocks.AIR);
    CampusRebuild.box(l, f, -2, 8, -2, 26, 8, 26, Blocks.SMOOTH_QUARTZ);
    for (int x = 0; x <= 24; x += 6)
      for (int z = 0; z <= 24; z += 6)
        l.setBlock(f.offset(x, 1, z), SarcAmbientLighting.LIGHT.get().defaultBlockState(), 2);
    var floorEngine = light(l, f.getX() - 2, f.getX() + 26, f.getZ() - 2, f.getZ() + 26);
    int floorMin = 15;
    for (int x = 1; x <= 23; x++)
      for (int z = 1; z <= 23; z++)
        floorMin =
            Math.min(
                floorMin,
                floorEngine.getLayerListener(LightLayer.BLOCK).getLightValue(f.offset(x, 2, z)));
    IntroAudit.check(
        floorMin >= 8,
        "SARC recessed fixtures produce walking-height block light >=8 on open floor; measured "
            + floorMin);
    // Exactly the same chunk-clipped door pass is called by natural generation after templates.
    BlockPos d = o.offset(0, 0, -650);
    for (var e : TransitionDoorways.plan().entrySet()) {
      var at = d.offset(e.getKey());
      l.getChunkAt(at);
      l.setBlock(at.below(2), Blocks.STONE.defaultBlockState(), 2);
      l.setBlock(at, Blocks.AIR.defaultBlockState(), 2);
    }
    var chunks = new java.util.HashSet<ChunkPos>();
    for (var local : TransitionDoorways.plan().keySet()) chunks.add(new ChunkPos(d.offset(local)));
    for (var chunk : chunks)
      TransitionDoorways.placeChunk(l, d, chunk.getMinBlockX(), chunk.getMinBlockZ());
    for (var e : TransitionDoorways.plan().entrySet())
      IntroAudit.check(
          l.getBlockState(d.offset(e.getKey())).equals(e.getValue()),
          "natural-generation framed door state " + e.getKey());
    try {
      java.nio.file.Files.writeString(
          IntroAudit.output("v28-light-levels.json"),
          new com.google.gson.Gson()
              .toJson(
                  java.util.Map.of(
                      "classroom_minimum_walking_block_light",
                      minimum,
                      "classroom_samples",
                      samples,
                      "sarc_open_floor_minimum_walking_block_light",
                      floorMin,
                      "method",
                      "vanilla LevelLightEngine on real generated block states; no sky light; not a shader render")));
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  private LightEngineAudit() {}
}
