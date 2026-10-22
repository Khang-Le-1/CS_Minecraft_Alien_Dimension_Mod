package com.timelocked;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.*;

/**
 * Test-only v27 walkthrough, physical grade, door event, furnishing and save compatibility checks.
 */
final class NeighborhoodAudit {
  static void run(IntroAudit.IntroPlayer p, CompoundTag t, ServerLevel l) {
    BlockPos o = IntroSequence.home(t), s = CampusNeighborhood.school(o);
    CampusRebuildAudit.run(p, t, l);
    IntroAudit.check(CampusOpening.modern(t), "fresh scene selects v27 revision");
    IntroAudit.check(
        o.equals(
            CampusRebuild.active(t)
                ? CampusRebuild.home(t.getInt("Slot"))
                : CampusNeighborhood.home(t.getInt("Slot"))),
        "v27 uses independent slot bank");
    for (int slot : new int[] {0, 1, 4095, 4096, 4097})
      if (slot != t.getInt("Slot"))
        IntroAudit.check(
            !CampusNeighborhood.contains(o, CampusNeighborhood.home(slot)),
            "v27 slots separated " + slot);
    for (int z = -140; z <= -96; z++) {
      int half = Math.min(8, (z + 140) / 5);
      BlockPos floor = o.offset(230, half / 2, z);
      IntroAudit.check(
          l.getBlockState(floor).is(Blocks.SMOOTH_STONE), "supported uphill path " + z);
      BlockPos above = floor.above();
      if (half % 2 == 1)
        IntroAudit.check(
            l.getBlockState(above).is(Blocks.SMOOTH_STONE_SLAB), "real half-block grade " + z);
      else IntroAudit.check(l.getBlockState(above).isAir(), "grade headroom " + z);
      IntroAudit.check(l.getBlockState(above.above()).isAir(), "ramp standing height " + z);
    }
    IntroAudit.check(
        l.getBlockState(o.offset(230, 4, -96)).is(Blocks.SMOOTH_STONE), "ramp gains four blocks");
    for (int x = 11; x <= 224; x++) clear(l, o.offset(x, 1, -5), "College Rd sidewalk " + x);
    for (int z = -140; z <= -11; z++) clear(l, o.offset(223, 1, z), "Lambert Rd sidewalk " + z);
    for (int x = 223; x <= 230; x++) clear(l, o.offset(x, 1, -142), "side path connection " + x);
    for (int x = 230; x <= 244; x++) clear(l, o.offset(x, 5, -90), "raised landing " + x);
    IntroAudit.check(
        l.getBlockState(o.offset(100, 0, -12)).is(Blocks.GRAY_CONCRETE),
        "College Rd east-west pavement");
    for (int x = 94; x <= 94 + 4 * 21; x += 42)
      for (int laneX = x + 6; laneX <= x + 8; laneX++)
        for (int z = -18; z <= -9; z++)
          IntroAudit.check(
              l.getBlockState(o.offset(laneX, 0, z))
                  .is(
                      z == -14 && (laneX + 6) % 9 <= 3
                          ? Blocks.YELLOW_CONCRETE
                          : Blocks.GRAY_CONCRETE),
              "College Road not overwritten by northern driveway " + laneX + ":" + z);
    IntroAudit.check(
        l.getBlockState(o.offset(208, 0, -50)).is(Blocks.GRAY_CONCRETE),
        "Lambert Rd north-south pavement");
    IntroAudit.check(
        !l.getBlockState(o.offset(52, 0, 20)).is(Blocks.GRAY_CONCRETE),
        "fictional Park St road removed");
    for (int x = 61; x <= 66; x++) {
      var a = l.getBlockState(s.offset(x, 1, -28));
      IntroAudit.check(
          a.is(CampusDoors.GLASS.get()) && !a.getValue(DoorBlock.OPEN), "glass entrance leaf " + x);
      IntroAudit.check(
          l.getBlockState(s.offset(x, 2, -28)).getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER,
          "glass entrance upper half " + x);
    }
    // Real canceled intro event manually opens both leaves once; off-hand cannot toggle it back.
    BlockPos front = s.offset(63, 1, -28);
    p.setPos(front.getX() + .5, front.getY(), front.getZ() + 2.5);
    click(p, front, net.minecraft.world.InteractionHand.MAIN_HAND);
    IntroAudit.check(
        l.getBlockState(front).getValue(DoorBlock.OPEN)
            && l.getBlockState(front.east()).getValue(DoorBlock.OPEN),
        "paired entrance toggles together");
    IntroAudit.check(
        !l.getBlockState(front.west()).getValue(DoorBlock.OPEN),
        "adjacent entrance pair stays closed");
    click(p, front, net.minecraft.world.InteractionHand.OFF_HAND);
    IntroAudit.check(
        l.getBlockState(front).getValue(DoorBlock.OPEN), "offhand cannot double-toggle entrance");
    p.setPos(o.getX() + 19.5, o.getY() + 1, o.getZ() + 13.5);
    click(p, front, net.minecraft.world.InteractionHand.MAIN_HAND);
    IntroAudit.check(
        l.getBlockState(front).getValue(DoorBlock.OPEN), "distant forged door click rejected");
    for (int[] d : new int[][] {{90, 1, -56}, {109, 1, -56}}) {
      BlockPos door = s.offset(d[0], d[1], d[2]);
      p.setPos(door.getX() + .5, door.getY(), door.getZ() - 2.5);
      click(p, door, net.minecraft.world.InteractionHand.MAIN_HAND);
      IntroAudit.check(
          l.getBlockState(door).getValue(DoorBlock.OPEN)
              && l.getBlockState(door.east()).getValue(DoorBlock.OPEN),
          "classroom glass pair opens " + d[0]);
    }
    var reachable = reachable(l, s, s.offset(63, 1, -29));
    SccCampusAudit.checkRoom(l, s, 27, 102, "v27 SCC123", reachable);
    SccCampusAudit.checkRoom(l, s, 49, 102, "v27 SCC121", reachable);
    // Two real home doors lie on the player's morning exit route.
    for (BlockPos door : java.util.List.of(o.offset(14, 1, 8), o.offset(11, 1, -2))) {
      p.setPos(door.getX() - 2, door.getY(), door.getZ() + .5);
      click(p, door, net.minecraft.world.InteractionHand.MAIN_HAND);
      IntroAudit.check(
          l.getBlockState(door).getValue(DoorBlock.OPEN), "home route door works " + door);
    }
    int sea = 0, orange = 0;
    for (int x = -2; x <= 341; x++)
      for (int z = -211; z <= 20; z++)
        for (int y = 0; y <= 18; y++) {
          var bs = l.getBlockState(o.offset(x, y, z));
          if (bs.is(Blocks.SEA_LANTERN)) sea++;
          if (bs.is(Blocks.ORANGE_TERRACOTTA)) orange++;
        }
    IntroAudit.check(sea == 0, "no sea lanterns in new home, streets or SCC");
    IntroAudit.check(orange > 12, "orange lobby feature wall exists");
    for (BlockPos at :
        java.util.List.of(
            new BlockPos(2, 1, 18),
            new BlockPos(4, 2, 18),
            new BlockPos(22, 2, 4),
            new BlockPos(19, 1, 19),
            new BlockPos(17, 2, 3)))
      IntroAudit.check(!l.getBlockState(o.offset(at)).isAir(), "lived-in home furniture " + at);
    IntroAudit.check(
        l.getBlockState(o.offset(19, 1, 12)).is(Blocks.RED_BED), "original bed anchor preserved");
    var actor = l.getEntity(t.getUUID("CampusClubmate"));
    IntroAudit.check(
        actor != null && Math.abs(actor.getY() - (o.getY() + 5)) < .01,
        "club actor stands on raised floor");
    CampusOpeningAudit.use(p, t, IntroCampus.SCHEDULE);
    IntroAudit.check(
        CampusOpening.step(t) == CampusOpening.WALK, "v27 schedule begins morning walk");
    // Arrival must follow College Road to Lambert Road, not just jump to campus.
    p.setPos(o.getX() + 244.5, o.getY() + 5, o.getZ() - 94.5);
    IntroAudit.ticks(p.server, 1);
    IntroAudit.check(
        CampusOpening.step(t) == CampusOpening.WALK, "campus shortcut cannot skip street route");
    for (int[] pt : new int[][] {{40, 1, -5}, {222, 1, -5}, {223, 1, -75}, {244, 5, -94}}) {
      p.setPos(o.getX() + pt[0] + .5, o.getY() + pt[1], o.getZ() + pt[2] + .5);
      IntroAudit.ticks(p.server, 1);
    }
    IntroAudit.check(
        CampusOpening.step(t) == CampusOpening.CLASS,
        "follow College Rd then Lambert Rd before arrival");
    CampusOpeningAudit.use(p, t, CampusOpening.class_desk(t));
    IntroAudit.check(
        CampusOpening.step(t) == CampusOpening.LESSON, "raised classroom starts lesson");
    p.setPos(s.getX() + 83.5, s.getY() + 1, s.getZ() - 41.5);
    IntroAudit.ticks(p.server, CampusOpening.LESSON_TICKS);
    IntroAudit.check(
        CampusOpening.step(t) == CampusOpening.CLUB, "raised classroom completes lesson");
    CampusOpeningAudit.use(p, t, CampusOpening.club_desk(t));
    IntroAudit.check(
        CampusOpening.step(t) == CampusOpening.FALL, "raised club starts actual satellite");
    IntroAudit.ticks(p.server, CampusOpening.fallTicks(t));
    IntroAudit.check(
        CampusOpening.step(t) == CampusOpening.INVESTIGATE,
        "raised impact keeps investigation beat");
    IntroAudit.check(
        l.getBlockState(o.offset(CampusOpening.wreck(t))).is(Blocks.BARREL),
        "real satellite crate translated with campus");
    CampusOpeningAudit.use(p, t, CampusOpening.wreck(t));
    IntroAudit.check(
        CampusOpening.step(t) == CampusOpening.RETURN, "raised crater investigation unlocks home");
    CampusOpeningAudit.use(p, t, new BlockPos(19, 1, 12));
    IntroAudit.check(t.getInt("Stage") == IntroSequence.BEDTIME, "v27 returns to night home");
    for (int i = 0; i < 10; i++) {
      IntroAudit.check(!t.hasUUID("CampusAmbient" + i), "ambient actor key cleaned at night " + i);
    }
    BlockPos n = IntroSequence.home(t);
    IntroAudit.check(
        p.serverLevel().getBlockState(n.offset(4, 2, 18)).is(Blocks.WATER_CAULDRON),
        "kitchen persists into evening");
    IntroAudit.check(
        p.serverLevel().getBlockState(n.offset(14, 1, 8)).is(Blocks.BIRCH_DOOR),
        "bedroom door persists into evening");
    BlockPos bedroom = n.offset(14, 1, 8);
    p.setPos(bedroom.getX() - 2, bedroom.getY(), bedroom.getZ() + .5);
    click(p, bedroom, net.minecraft.world.InteractionHand.MAIN_HAND);
    IntroAudit.check(
        p.serverLevel().getBlockState(bedroom).getValue(DoorBlock.OPEN),
        "night bedroom door opens before break-in");
    // Continue the entire existing v26 suite against an explicit old-save fixture.
    CampusOpening.cleanup(p, t);
    t.remove(CampusRebuild.REVISION);
    t.remove(CampusNeighborhood.REVISION);
    t.remove("SatelliteInvestigated");
    IntroSequence.stage(p, t, IntroSequence.HOME);
    t.putInt(CampusOpening.STEP, CampusOpening.MORNING);
    t.putInt(CampusOpening.AGE, 0);
    BlockPos old = IntroSequence.home(t);
    SccCampus.build(l, old);
    p.teleportTo(l, old.getX() + 19.5, old.getY() + 1, old.getZ() + 13.5, 0, 0);
    CampusOpening.populate(p, t);
    IntroAudit.check(old.equals(SccCampus.home(t.getInt("Slot"))), "v26 save retains old origin");
    CompoundTag v25 = t.copy();
    v25.remove(SccCampus.REVISION);
    IntroAudit.check(
        IntroSequence.home(v25).equals(IntroCampus.home(t.getInt("Slot"))),
        "v25 save retains positive-Z origin");
  }

  static void clear(ServerLevel l, BlockPos at, String label) {
    IntroAudit.check(
        l.getBlockState(at).getCollisionShape(l, at).isEmpty()
            && l.getBlockState(at.above()).getCollisionShape(l, at.above()).isEmpty()
            && l.getBlockState(at.below()).isSolidRender(l, at.below()),
        label);
  }

  static void click(
      IntroAudit.IntroPlayer p, BlockPos at, net.minecraft.world.InteractionHand hand) {
    var e =
        new net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock(
            p,
            hand,
            at,
            new net.minecraft.world.phys.BlockHitResult(
                net.minecraft.world.phys.Vec3.atCenterOf(at), Direction.SOUTH, at, false));
    IntroSequence.interact(e);
    IntroAudit.check(e.isCanceled(), "door events stay protected");
  }

  // Swept player-width collision, not "whole voxel empty": an open door retains a thin side leaf.
  static java.util.Set<BlockPos> reachable(ServerLevel l, BlockPos o, BlockPos first) {
    var seen = new java.util.HashSet<BlockPos>();
    var queue = new java.util.ArrayDeque<BlockPos>();
    seen.add(first);
    queue.add(first);
    while (!queue.isEmpty()) {
      BlockPos current = queue.removeFirst();
      for (Direction d : Direction.Plane.HORIZONTAL) {
        BlockPos n = current.relative(d);
        if (seen.contains(n)
            || !SccCampus.contains(o, n)
            || !l.getBlockState(n.below()).isSolidRender(l, n.below())) continue;
        var swept =
            new net.minecraft.world.phys.AABB(
                Math.min(current.getX(), n.getX()) + .2,
                n.getY(),
                Math.min(current.getZ(), n.getZ()) + .2,
                Math.max(current.getX(), n.getX()) + .8,
                n.getY() + 1.8,
                Math.max(current.getZ(), n.getZ()) + .8);
        if (l.noCollision(null, swept)) {
          seen.add(n);
          queue.addLast(n);
        }
      }
    }
    return seen;
  }

  private NeighborhoodAudit() {}
}
