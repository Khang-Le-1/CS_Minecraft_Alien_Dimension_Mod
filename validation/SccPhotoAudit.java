package com.timelocked;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.*;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.properties.*;

/** Real generated-world checks for photo revision, followed by the unchanged older-scene suite. */
final class SccPhotoAudit {
  static void run(IntroAudit.IntroPlayer p, CompoundTag t, ServerLevel l) {
    BlockPos o = IntroSequence.home(t), s = CampusNeighborhood.school(o);
    IntroAudit.check(SccPhotoRevision.active(t), "fresh start selects v32 SCC approach revision");
    IntroAudit.check(
        o.equals(SccPhotoRevision.home(t.getInt("Slot"))), "v32 independent private scene bank");
    CompoundTag v31 = t.copy();
    v31.remove(SccPhotoRevision.REVISION);
    v31.putBoolean(SccPhotoRevision.PREVIOUS_REVISION, true);
    IntroAudit.check(
        SccPhotoRevision.active(v31) && IntroSequence.home(v31).equals(o),
        "v31 active save retains its private scene bank");
    CompoundTag v30 = t.copy();
    v30.remove(SccPhotoRevision.REVISION);
    v30.putBoolean(SccPhotoRevision.EARLIER_REVISION, true);
    IntroAudit.check(
        SccPhotoRevision.active(v30) && IntroSequence.home(v30).equals(o),
        "v30 active save retains its private scene bank");
    CompoundTag old = t.copy();
    old.remove(SccPhotoRevision.REVISION);
    old.remove(SccPhotoRevision.PREVIOUS_REVISION);
    old.remove(SccPhotoRevision.EARLIER_REVISION);
    IntroAudit.check(
        IntroSequence.home(old).equals(CampusRebuild.home(t.getInt("Slot"))),
        "v29 active save retains v28/v29 scene bank");
    for (int slot : new int[] {0, 1, 4095, 4096, 4097})
      if (slot != t.getInt("Slot"))
        IntroAudit.check(
            !CampusNeighborhood.contains(o, SccPhotoRevision.home(slot)),
            "photo slots separated " + slot);
    for (int z : new int[] {-28, -33}) {
      for (int x = 63; x <= 64; x++) {
        BlockPos at = s.offset(x, 1, z);
        IntroAudit.check(
            l.getBlockState(at).is(CampusDoors.GLASS.get()),
            "two entrance leaves per bank " + x + ":" + z);
        IntroAudit.check(
            l.getBlockState(at.above()).getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER,
            "complete entrance door upper half");
      }
      IntroAudit.check(
          l.getBlockState(s.offset(61, 1, z)).is(Blocks.GLASS),
          "entrance sidelight, not six doors");
      BlockPos at = s.offset(63, 1, z);
      p.setPos(at.getX() + .5, at.getY(), at.getZ() + 1.5);
      NeighborhoodAudit.click(p, at, net.minecraft.world.InteractionHand.MAIN_HAND);
      IntroAudit.check(
          l.getBlockState(at).getValue(DoorBlock.OPEN)
              && l.getBlockState(at.east()).getValue(DoorBlock.OPEN),
          "entrance pair opens together " + z);
      for (int x : new int[] {61, 62, 65, 66})
        IntroAudit.check(
            l.getBlockState(s.offset(x, 1, z)).is(Blocks.GLASS),
            "fixed entrance sidelight " + x + ":" + z);
      for (int x = 63; x <= 64; x++) {
        BlockPos leaf = s.offset(x, 1, z);
        var shape = l.getBlockState(leaf).getCollisionShape(l, leaf);
        var bounds = shape.bounds();
        IntroAudit.check(
            x == 63 ? bounds.maxX <= 0.2 : bounds.minX >= 0.8,
            "open leaf rests on outside jamb " + x + ":" + z);
      }
      NeighborhoodAudit.click(p, at, net.minecraft.world.InteractionHand.MAIN_HAND);
      IntroAudit.check(
          !l.getBlockState(at).getValue(DoorBlock.OPEN)
              && !l.getBlockState(at.east()).getValue(DoorBlock.OPEN),
          "entrance pair closes together " + z);
      NeighborhoodAudit.click(p, at.east(), net.minecraft.world.InteractionHand.MAIN_HAND);
      IntroAudit.check(
          l.getBlockState(at).getValue(DoorBlock.OPEN)
              && l.getBlockState(at.east()).getValue(DoorBlock.OPEN),
          "right entrance leaf also opens the pair " + z);
    }
    for (int x : new int[] {90, 109}) {
      BlockPos at = s.offset(x, 1, -56);
      p.setPos(at.getX() + .5, at.getY(), at.getZ() - 1.5);
      IntroAudit.check(
          l.getBlockState(at).is(CampusDoors.BROWN.get()), "brown glazed classroom door " + x);
      NeighborhoodAudit.click(p, at, net.minecraft.world.InteractionHand.MAIN_HAND);
      IntroAudit.check(
          l.getBlockState(at).getValue(DoorBlock.OPEN), "brown classroom door is usable " + x);
      IntroAudit.check(
          l.getBlockState(at.east()).is(Blocks.GLASS), "classroom sidelight preserved");
    }
    // Open remaining internal doors for swept player-width route checks.
    for (int[] a :
        new int[][] {{76, -91}, {87, -100}, {81, -91}, {85, -58}, {117, -46}, {129, -45}}) {
      BlockPos at = s.offset(a[0], 1, a[1]);
      var st = l.getBlockState(at);
      IntroAudit.check(
          st.getBlock() instanceof DoorBlock, "photo room doorway exists " + a[0] + ":" + a[1]);
      if (st.getBlock() instanceof DoorBlock d) d.setOpen(p, l, st, at, true);
    }
    var reachable = NeighborhoodAudit.reachable(l, s, s.offset(63, 1, -27));
    for (int[] a :
        new int[][] {
          {8, 43}, {31, 37}, {30, 61}, {29, 79}, {27, 102}, {49, 102}, {68, 106}, {87, 104}
        }) SccCampusAudit.checkRoom(l, s, a[0], a[1], "photo " + a[0] + ":" + a[1], reachable);
    IntroAudit.check(
        l.getBlockState(s.offset(80, 2, -49)).is(Blocks.WHITE_CONCRETE),
        "AI lab has long whiteboard, not green chalkboard");
    for (int z : new int[] {-45, -41, -37, -33})
      for (int x : new int[] {77, 78, 86, 87}) {
        IntroAudit.check(
            l.getBlockState(s.offset(x, 1, z)).is(CampusFurnishings.DESK.get()),
            "AI computer bench desk");
        IntroAudit.check(
            l.getBlockEntity(s.offset(x, 2, z)) instanceof MonitorContent.MonitorEntity,
            "AI station has real monitor entity");
      }
    IntroAudit.check(
        l.getBlockState(s.offset(83, 1, -39)).is(Blocks.LECTERN), "AI lesson anchor retained");
    IntroAudit.check(
        l.getBlockState(s.offset(105, 1, -39)).is(Blocks.LECTERN), "club anchor retained");
    for (int z = -100; z <= -81; z++) {
      BlockPos at = s.offset(76, 1, z);
      IntroAudit.check(
          l.getBlockState(at).is(z == -91 ? CampusDoors.GLASS.get() : Blocks.GLASS),
          "SCC 101 east hallway glass frontage " + z);
      IntroAudit.check(
          l.getBlockState(s.offset(76, 4, z)).is(SccPhotoFixtures.WOOD.get()),
          "SCC 101 rotated header " + z);
    }
    IntroAudit.check(
        l.getBlockState(s.offset(76, 1, -91)).getValue(DoorBlock.FACING) == Direction.EAST,
        "SCC 101 doorway faces east hallway");
    for (int x = 57; x <= 75; x++)
      IntroAudit.check(
          l.getBlockState(s.offset(x, 1, -81)).is(SccPhotoFixtures.WOOD.get()),
          "SCC 101 former south front closed " + x);
    IntroAudit.check(l.getBlockState(s.offset(62, 2, -80)).isAir(), "SCC 101 old sign removed");
    IntroAudit.check(
        reachable.contains(s.offset(75, 1, -91)), "SCC 101 accessible behind rotated doorway");
    IntroAudit.check(
        !(l.getBlockState(s.offset(76, 1, -120)).getBlock() instanceof DoorBlock),
        "SCC 101 no misplaced north-side door");
    for (int x = 82; x <= 92; x++) {
      BlockPos at = s.offset(x, 1, -100);
      IntroAudit.check(
          x == 87
              ? l.getBlockState(at).is(CampusDoors.GLASS.get())
              : l.getBlockState(at).is(Blocks.GLASS),
          "SCC 103 corridor-facing glass frontage " + x);
    }
    IntroAudit.check(
        !(l.getBlockState(s.offset(81, 1, -108)).getBlock() instanceof DoorBlock),
        "SCC 103 no west-facing doorway into SCC 101");
    for (int x = 81; x <= 93; x++) {
      BlockPos at = s.offset(x, 1, -58);
      IntroAudit.check(
          l.getBlockState(at).is(x == 85 ? CampusDoors.GLASS.get() : Blocks.GLASS),
          "SCC 124 south hallway glass frontage " + x);
      IntroAudit.check(
          l.getBlockState(s.offset(x, 4, -58)).is(Blocks.YELLOW_TERRACOTTA),
          "SCC 124 rotated mustard header " + x);
    }
    IntroAudit.check(
        l.getBlockState(s.offset(85, 1, -58)).getValue(DoorBlock.FACING) == Direction.SOUTH,
        "SCC 124 doorway faces south hallway");
    for (int z = -67; z <= -59; z++)
      for (int y = 1; y <= 3; y++)
        IntroAudit.check(
            l.getBlockState(s.offset(81, y, z)).is(SccPhotoFixtures.WOOD.get()),
            "SCC 124 former west front closed " + y + ":" + z);
    IntroAudit.check(l.getBlockState(s.offset(77, 2, -65)).isAir(), "SCC 124 old sign removed");
    IntroAudit.check(
        reachable.contains(s.offset(85, 1, -59)), "SCC 124 accessible behind rotated doorway");
    for (int x = 77; x <= 80; x++)
      for (int z = -114; z <= -57; z++) {
        BlockPos floor = s.offset(x, 1, z);
        var sweep =
            new net.minecraft.world.phys.AABB(
                floor.getX() + .05,
                floor.getY(),
                floor.getZ() + .05,
                floor.getX() + .95,
                floor.getY() + 2,
                floor.getZ() + .95);
        IntroAudit.check(l.noCollision(p, sweep), "four-wide circulation clearance " + x + ":" + z);
        IntroAudit.check(reachable.contains(floor), "continuous widened route " + x + ":" + z);
        IntroAudit.check(
            l.getBlockState(floor.above(4)).is(SccPhotoFixtures.CEILING.get()),
            "widened hallway ceiling sealed " + x + ":" + z);
      }
    for (int z : new int[] {-73, -68})
      for (int x = 81; x <= 93; x++)
        for (int y = 1; y <= 4; y++)
          IntroAudit.check(
              l.getBlockState(s.offset(x, y, z))
                  .is(y == 4 ? Blocks.ORANGE_TERRACOTTA : SccPhotoFixtures.WOOD.get()),
              "room end seam closed " + x + ":" + y + ":" + z);
    for (int z = -46; z <= -39; z++)
      IntroAudit.check(
          l.getBlockState(s.offset(SccPhotoRevision.wallX(z), 2, z)).is(Blocks.GLASS),
          "SCC 123 hallway window " + z);
    for (int[] b : new int[][] {{84, -95}, {89, -95}, {84, -67}, {89, -67}}) {
      IntroAudit.check(
          l.getBlockState(s.offset(b[0], 1, b[1])).is(SccPhotoFixtures.RACK_LOWER.get()),
          "network rack lower");
      IntroAudit.check(
          l.getBlockState(s.offset(b[0], 2, b[1])).is(SccPhotoFixtures.RACK_UPPER.get()),
          "network rack upper");
    }
    IntroAudit.check(
        l.getBlockState(s.offset(84, 4, -90)).is(SccPhotoFixtures.TRAY.get()),
        "blue overhead cable tray");
    IntroAudit.check(
        l.getBlockState(s.offset(60, 9, -75)).is(SccPhotoFixtures.PENDANT.get()),
        "cylindrical lobby pendant");
    for (int[] b :
        new int[][] {
          {92, -57},
          {111, -57},
          {83, -99},
          {80, -93},
          {82, -57},
          {77, -95},
          {116, -48},
          {128, -47}
        })
      IntroAudit.check(
          l.getBlockEntity(s.offset(b[0], 2, b[1])) instanceof SignBlockEntity,
          "room label " + b[0] + ":" + b[1]);
    for (int y = 3; y <= 5; y++)
      IntroAudit.check(
          l.getBlockState(o.offset(14, y, 8)).is(Blocks.WHITE_TERRACOTTA),
          "bedroom opening has full lintel " + y);
    for (int x = -5; x <= -2; x++)
      for (int z = 5; z <= 9; z++)
        IntroAudit.check(
            !l.getBlockState(o.offset(x, 5, z)).is(Blocks.DEEPSLATE_TILE_SLAB),
            "no dark bay beam inside " + x + ":" + z);
    IntroAudit.check(
        l.getBlockState(o.offset(-4, 6, 7)).is(Blocks.SPRUCE_PLANKS),
        "bay timber ceiling continuous at living height");
    for (int z : new int[] {4, 10})
      for (int y = 4; y <= 5; y++)
        IntroAudit.check(
            l.getBlockState(o.offset(-2, y, z)).is(Blocks.WHITE_TERRACOTTA),
            "reading-bay inner header sealed " + y + ":" + z);
    var engine =
        LightEngineAudit.light(l, s.getX() + 54, s.getX() + 158, s.getZ() - 145, s.getZ() - 25);
    int minimum = 15, samples = 0;
    for (int x = 77; x <= 90; x++)
      for (int z = -48; z <= -31; z++) {
        BlockPos at = s.offset(x, 1, z);
        if (!l.getBlockState(at).getCollisionShape(l, at).isEmpty()) continue;
        minimum = Math.min(minimum, engine.getLayerListener(LightLayer.BLOCK).getLightValue(at));
        samples++;
      }
    IntroAudit.check(
        samples > 100 && minimum >= 7, "photo AI lab block light >=7; measured " + minimum);
    var he = LightEngineAudit.light(l, o.getX() - 8, o.getX() + 27, o.getZ() - 10, o.getZ() + 23);
    int hm = 15;
    for (int[] q :
        new int[][] {{12, 8}, {13, 8}, {15, 8}, {16, 8}, {-3, 7}, {-4, 6}, {8, 8}, {10, 13}})
      hm =
          Math.min(
              hm, he.getLayerListener(LightLayer.BLOCK).getLightValue(o.offset(q[0], 2, q[1])));
    IntroAudit.check(hm >= 7, "home doorway and bay block light >=7; measured " + hm);
    export(l, o, s);
    try {
      java.nio.file.Files.writeString(
          IntroAudit.output("v31-light-levels.json"),
          new com.google.gson.Gson()
              .toJson(
                  java.util.Map.of(
                      "ai_lab_minimum",
                      minimum,
                      "ai_lab_samples",
                      samples,
                      "home_door_bay_minimum",
                      hm,
                      "method",
                      "Native vanilla block light only; not a shader capture")));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    // Exercise this actual fresh revision through the existing story events, not just geometry.
    CampusOpeningAudit.use(p, t, IntroCampus.SCHEDULE);
    for (int[] q : new int[][] {{40, 1, -5}, {222, 1, -5}, {223, 1, -75}, {244, 5, -94}}) {
      p.setPos(o.getX() + q[0] + .5, o.getY() + q[1], o.getZ() + q[2] + .5);
      IntroAudit.ticks(p.server, 1);
    }
    IntroAudit.check(
        CampusOpening.step(t) == CampusOpening.CLASS, "v32 SCC road approach reaches class");
    CampusOpeningAudit.use(p, t, CampusOpening.class_desk(t));
    IntroAudit.ticks(p.server, CampusOpening.LESSON_TICKS);
    IntroAudit.check(CampusOpening.step(t) == CampusOpening.CLUB, "v32 AI lab lesson completes");
    CampusOpeningAudit.use(p, t, CampusOpening.club_desk(t));
    IntroAudit.ticks(p.server, CampusOpening.fallTicks(t));
    IntroAudit.check(
        CampusOpening.step(t) == CampusOpening.INVESTIGATE, "v32 club triggers satellite sequence");
    CampusOpeningAudit.use(p, t, CampusOpening.wreck(t));
    IntroAudit.check(
        CampusOpening.step(t) == CampusOpening.RETURN, "v32 wreck remains interactable");
    CampusOpeningAudit.use(p, t, new BlockPos(19, 1, 12));
    IntroAudit.check(t.getInt("Stage") == IntroSequence.BEDTIME, "v32 returns to night home");
    BlockPos n = IntroSequence.home(t);
    var night = p.server.getLevel(IntroSequence.HOME_DIM);
    IntroAudit.check(
        night.getBlockState(n.offset(14, 4, 8)).is(Blocks.WHITE_TERRACOTTA),
        "night house has repaired door header");
    IntroAudit.check(
        night.getBlockState(n.offset(-4, 6, 7)).is(Blocks.SPRUCE_PLANKS),
        "night house has repaired bay ceiling");
    // Explicit older-save fixture: the full v25–v29 suites remain unchanged, not weakened.
    CampusOpening.cleanup(p, t);
    t.remove(SccPhotoRevision.REVISION);
    t.remove("SatelliteInvestigated");
    t.putInt("CampusStreetRoute", 0);
    IntroSequence.stage(p, t, IntroSequence.HOME);
    t.putInt(CampusOpening.STEP, CampusOpening.MORNING);
    t.putInt(CampusOpening.AGE, 0);
    BlockPos legacy = IntroSequence.home(t);
    CampusRebuild.build(l, legacy);
    p.teleportTo(l, legacy.getX() + 19.5, legacy.getY() + 1.6, legacy.getZ() + 12.5, 180, 15);
    CampusOpening.populate(p, t);
  }

  static void export(ServerLevel l, BlockPos o, BlockPos s) {
    try {
      var data = new java.util.LinkedHashMap<String, Object>();
      for (String name :
          java.util.List.of("home", "ai-lab", "lobby", "network", "game-lab", "resource")) {
        int[] b =
            switch (name) {
              case "home" -> new int[] {-7, 0, -3, 25, 7, 21};
              case "ai-lab" -> new int[] {70, 0, -58, 93, 5, -28};
              case "network" -> new int[] {74, 0, -99, 94, 5, -58};
              case "game-lab" -> new int[] {55, 0, -123, 80, 5, -79};
              case "resource" -> new int[] {78, 0, -116, 94, 5, -98};
              default -> new int[] {55, 0, -58, 79, 13, -26};
            };
        BlockPos base = name.equals("home") ? o : s;
        var cells = new java.util.ArrayList<Object>();
        for (int x = b[0]; x <= b[3]; x++)
          for (int y = b[1]; y <= b[4]; y++)
            for (int z = b[2]; z <= b[5]; z++) {
              var st = l.getBlockState(base.offset(x, y, z));
              if (st.isAir()) continue;
              cells.add(
                  java.util.Map.of(
                      "x",
                      x,
                      "y",
                      y,
                      "z",
                      z,
                      "id",
                      net.minecraft.core.registries.BuiltInRegistries.BLOCK
                          .getKey(st.getBlock())
                          .toString(),
                      "state",
                      st.toString()));
            }
        data.put(name, cells);
      }
      java.nio.file.Files.writeString(
          IntroAudit.output("scc-v31-geometry.json"), new com.google.gson.Gson().toJson(data));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private SccPhotoAudit() {}
}
