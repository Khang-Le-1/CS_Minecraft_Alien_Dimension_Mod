package com.timelocked;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

/** Test-only map topology, sign grouping, private-slot and legacy-coordinate regression. */
final class SccCampusAudit {
  static void run(IntroAudit.IntroPlayer p, CompoundTag t, ServerLevel l) {
    BlockPos o = IntroSequence.home(t);
    IntroAudit.check(t.getBoolean(SccCampus.REVISION), "fresh starts select SCC revision");
    IntroAudit.check(o.getZ() < 0, "new campus bank cannot overlap positive-Z v25 slots");
    for (int a : new int[] {0, 1, 4095, 4096, 4097})
      for (int b : new int[] {0, 1, 4095, 4096, 4097})
        if (a != b)
          IntroAudit.check(
              !SccCampus.contains(SccCampus.home(a), SccCampus.home(b)),
              "new slot separation " + a + ":" + b);
    var seen = reachable(l, o, o.offset(63, 1, -29));
    checkRoom(l, o, 8, 4, "F", seen);
    checkRoom(l, o, 8, 13, "M", seen);
    checkRoom(l, o, 9, 20, "105", seen);
    checkRoom(l, o, 9, 43, "101", seen);
    checkRoom(l, o, 31, 37, "103", seen);
    checkRoom(l, o, 17, 8, "107", seen);
    checkRoom(l, o, 32, 10, "109", seen);
    checkRoom(l, o, 50, 11, "110", seen);
    checkRoom(l, o, 67, 5, "111", seen);
    checkRoom(l, o, 87, 9, "112", seen);
    checkRoom(l, o, 87, 30, "113", seen);
    checkRoom(l, o, 87, 55, "115", seen);
    checkRoom(l, o, 87, 81, "117", seen);
    checkRoom(l, o, 87, 104, "119", seen);
    checkRoom(l, o, 68, 106, "120", seen);
    checkRoom(l, o, 49, 102, "121", seen);
    checkRoom(l, o, 27, 102, "123", seen);
    checkRoom(l, o, 52, 71, "118", seen);
    checkRoom(l, o, 52, 56, "116", seen);
    checkRoom(l, o, 52, 41, "114", seen);
    checkRoom(l, o, 30, 61, "102", seen);
    checkRoom(l, o, 29, 79, "124", seen);
    checkRoom(l, o, 31, 21, "104", seen);
    checkRoom(l, o, 8, 71, "100", seen);
    for (int x = 61; x <= 66; x++)
      for (int y = 1; y <= 3; y++)
        IntroAudit.check(
            l.getBlockState(o.offset(x, y, -28))
                .getCollisionShape(l, o.offset(x, y, -28))
                .isEmpty(),
            "Exit 1 clear " + x + ":" + y);
    for (int x = SccCampus.X; x <= SccCampus.X + 101; x++)
      for (int z = SccCampus.Z; z <= SccCampus.Z + 116; z++)
        IntroAudit.check(
            l.getBlockState(o.offset(x, 0, z)).isSolidRender(l, o.offset(x, 0, z)),
            "SCC supported floor " + x + ":" + z);
    IntroAudit.check(
        l.getBlockState(o.offset(56, 10, -100)).is(Blocks.LIGHT_BLUE_STAINED_GLASS),
        "tall west curtain wall");
    IntroAudit.check(
        l.getBlockState(o.offset(54, 14, -26)).is(Blocks.SMOOTH_QUARTZ), "white roof overhang");
    for (int x = 73; x <= 80; x++)
      for (int y = 9; y <= 10; y++) {
        var at = o.offset(x, y, -27);
        var e = l.getBlockEntity(at);
        IntroAudit.check(e instanceof MonitorContent.MonitorEntity, "wall mounted plaque cell");
        if (e instanceof MonitorContent.MonitorEntity m) {
          IntroAudit.check(
              m.theme == MonitorContent.SCC_PLAQUE
                  && m.line(0).equals("Seaton Computing Center")
                  && m.line(1).equals("SCC"),
              "plaque readable name stored");
          var w = MonitorWallLayout.find(l, at, Direction.SOUTH);
          IntroAudit.check(
              w.width() == 8 && w.height() == 2, "all sign cells form one continuous 8x2 plaque");
        }
      }
    // Check ramp surface rises in HALF-block increments, with two-block clear headroom.
    double last = -100;
    for (int z = -60; z <= -31; z++) {
      double surface = -100;
      for (int y = -3; y <= 1; y++) {
        var at = o.offset(50, y, z);
        var shape = l.getBlockState(at).getCollisionShape(l, at);
        if (!shape.isEmpty()) surface = Math.max(surface, y + shape.max(Direction.Axis.Y));
      }
      IntroAudit.check(
          surface >= last && (last == -100 || surface - last <= .501),
          "graded ramp half step " + z);
      last = surface;
    }
    // v25 did NOT save a revision flag: preserve its stride, anchors and stage progression.
    CompoundTag old = t.copy();
    old.remove(SccCampus.REVISION);
    old.putInt("Slot", 7);
    old.putInt(CampusOpening.STEP, CampusOpening.MORNING);
    var legacy = IntroSequence.home(old);
    IntroAudit.check(
        legacy.equals(IntroCampus.home(7)), "v25 active saves retain original home coordinate");
    IntroAudit.check(
        CampusOpening.class_desk(old).equals(IntroCampus.CLASS_DESK)
            && CampusOpening.club_desk(old).equals(IntroCampus.CLUB_DESK)
            && CampusOpening.wreck(old).equals(IntroCampus.WRECK),
        "v25 active saves retain every quest anchor");
    IntroCampus.build(l, legacy);
    useLegacy(p, old, IntroCampus.SCHEDULE);
    IntroAudit.check(CampusOpening.step(old) == CampusOpening.WALK, "legacy schedule");
    p.setPos(legacy.getX() + 45.5, legacy.getY() + 1, legacy.getZ() + 12.5);
    CampusOpening.tick(p, old);
    IntroAudit.check(CampusOpening.step(old) == CampusOpening.CLASS, "legacy arrival");
    useLegacy(p, old, IntroCampus.CLASS_DESK);
    for (int i = 0; i < CampusOpening.LESSON_TICKS; i++) CampusOpening.tick(p, old);
    IntroAudit.check(CampusOpening.step(old) == CampusOpening.CLUB, "legacy class");
    useLegacy(p, old, IntroCampus.CLUB_DESK);
    for (int i = 0; i < CampusOpening.FALL_TICKS; i++) CampusOpening.tick(p, old);
    IntroAudit.check(
        l.getBlockState(legacy.offset(IntroCampus.WRECK)).is(Blocks.LODESTONE),
        "legacy crash remains at v25 coordinate");
    useLegacy(p, old, IntroCampus.WRECK);
    IntroAudit.check(
        CampusOpening.step(old) == CampusOpening.RETURN && old.getBoolean("SatelliteInvestigated"),
        "legacy investigation");
    p.setPos(o.getX() + 19.5, o.getY() + 1, o.getZ() + 13.5);
  }

  static void craterRoute(ServerLevel l, BlockPos o) {
    var seen = new java.util.HashSet<BlockPos>();
    var q = new java.util.ArrayDeque<BlockPos>();
    var first = o.offset(140, 1, -22);
    q.add(first);
    seen.add(first);
    while (!q.isEmpty()) {
      var c = q.removeFirst();
      for (var d : Direction.Plane.HORIZONTAL)
        for (int dy = -1; dy <= 1; dy++) {
          var n = c.relative(d).above(dy);
          int x = n.getX() - o.getX(), y = n.getY() - o.getY(), z = n.getZ() - o.getZ();
          if (x < 125 || x > 155 || z < -23 || z > 7 || y < -2 || y > 2 || seen.contains(n))
            continue;
          if (!l.getBlockState(n).getCollisionShape(l, n).isEmpty()
              || !l.getBlockState(n.above()).getCollisionShape(l, n.above()).isEmpty()
              || !l.getBlockState(n.below()).isSolidRender(l, n.below())) continue;
          seen.add(n);
          q.addLast(n);
        }
    }
    var target = o.offset(SccCampus.WRECK);
    IntroAudit.check(
        seen.stream().anyMatch(n -> n.distSqr(target) <= 4),
        "walkable descent from approach to actual recovery crate");
  }

  static void useLegacy(IntroAudit.IntroPlayer p, CompoundTag t, BlockPos a) {
    var at = IntroSequence.home(t).offset(a);
    p.setPos(at.getX() + .5, at.getY(), at.getZ() + 2.5);
    CampusOpening.interact(p, t, at);
  }

  static void checkRoom(
      ServerLevel l, BlockPos o, int x, int z, String name, java.util.Set<BlockPos> seen) {
    var at = o.offset(SccCampus.X + x, 1, SccCampus.Z + z);
    IntroAudit.check(
        seen.stream().anyMatch(p -> p.distSqr(at) <= 4), "Exit 1 route to mapped room " + name);
  }

  static java.util.Set<BlockPos> reachable(ServerLevel l, BlockPos o, BlockPos first) {
    var seen = new java.util.HashSet<BlockPos>();
    var q = new java.util.ArrayDeque<BlockPos>();
    q.add(first);
    seen.add(first);
    while (!q.isEmpty()) {
      var current = q.removeFirst();
      for (var d : Direction.Plane.HORIZONTAL) {
        var n = current.relative(d);
        if (SccCampus.contains(o, n)
            && !seen.contains(n)
            && l.getBlockState(n).getCollisionShape(l, n).isEmpty()
            && l.getBlockState(n.above()).getCollisionShape(l, n.above()).isEmpty()
            && l.getBlockState(n.below()).isSolidRender(l, n.below())) {
          seen.add(n);
          q.addLast(n);
        }
      }
    }
    return seen;
  }
}
