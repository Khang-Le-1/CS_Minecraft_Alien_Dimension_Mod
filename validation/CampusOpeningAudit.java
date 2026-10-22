package com.timelocked;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

/** Native walkthrough and old-save compatibility checks; test source only. */
final class CampusOpeningAudit {
  static void use(IntroAudit.IntroPlayer p, CompoundTag t, BlockPos relative) {
    BlockPos at = IntroSequence.home(t).offset(relative);
    p.setPos(at.getX() + .5, at.getY(), at.getZ() + 2.5);
    var e =
        new net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock(
            p,
            net.minecraft.world.InteractionHand.MAIN_HAND,
            at,
            new net.minecraft.world.phys.BlockHitResult(
                net.minecraft.world.phys.Vec3.atCenterOf(at),
                net.minecraft.core.Direction.SOUTH,
                at,
                false));
    IntroSequence.interact(e);
    IntroAudit.check(e.isCanceled(), "campus interaction cannot open inventories or edit signs");
  }

  static void run(IntroAudit.IntroPlayer p, CompoundTag t, ServerLevel earth) {
    var server = p.server;
    var campus = server.getLevel(IntroSequence.CAMPUS_DIM);
    SccPhotoAudit.run(p, t, campus);
    NeighborhoodAudit.run(p, t, campus);
    BlockPos o = IntroSequence.home(t);
    IntroAudit.check(
        campus != null && p.level() == campus, "fresh opening uses private campus world");
    IntroAudit.check(
        campus.dimensionType().fixedTime().orElse(-1) == 1000,
        "campus morning does not alter global world time");
    IntroAudit.check(CampusOpening.step(t) == CampusOpening.MORNING, "new opening starts at bed");
    IntroAudit.check(
        campus.getBlockState(o.offset(19, 1, 12)).is(Blocks.RED_BED),
        "morning starts in an actual home with bed");
    IntroAudit.check(!campus.getBiome(o).value().hasPrecipitation(), "daytime set has no rain");
    var clubActor = campus.getEntity(t.getUUID("CampusClubmate"));
    IntroAudit.check(
        clubActor != null && clubActor.getTags().contains("timelocked_campus_actor"),
        "club member is a real named NPC");
    // Campuses are private, non-overlapping slots, not modifications to real-world terrain.
    for (int slot : new int[] {1, 4095, 4096}) {
      BlockPos other = SccCampus.home(slot);
      IntroAudit.check(!SccCampus.contains(o, other), "private slot separation " + slot);
    }
    SccCampusAudit.run(p, t, campus);
    routes(campus, o);
    // Continuous home approach; no invisible barriers, missing floors or two-block obstacles.
    for (int x = 10; x <= 40; x++) {
      BlockPos at = o.offset(x, 1, -5);
      IntroAudit.check(
          campus.getBlockState(at.below()).isSolidRender(campus, at.below()),
          "home-to-campus ground " + x);
      IntroAudit.check(
          campus.getBlockState(at).getCollisionShape(campus, at).isEmpty()
              && campus.getBlockState(at.above()).getCollisionShape(campus, at.above()).isEmpty(),
          "home-to-campus clearance " + x);
    }
    for (int z = -26; z <= -5; z++) {
      BlockPos at = o.offset(63, 1, z);
      IntroAudit.check(
          campus.getBlockState(at.below()).isSolidRender(campus, at.below()),
          "campus path floor " + z);
      IntroAudit.check(
          campus.getBlockState(at).getCollisionShape(campus, at).isEmpty()
              && campus.getBlockState(at.above()).getCollisionShape(campus, at.above()).isEmpty(),
          "campus path clearance " + z);
    }
    use(p, t, new BlockPos(19, 1, 12));
    IntroAudit.check(
        CampusOpening.step(t) == CampusOpening.MORNING, "cannot skip to kidnapping via bed");
    use(p, t, SccCampus.CLUB_DESK);
    IntroAudit.check(
        CampusOpening.step(t) == CampusOpening.MORNING, "club cannot bypass class and walk");
    use(p, t, IntroCampus.SCHEDULE);
    IntroAudit.check(
        CampusOpening.step(t) == CampusOpening.WALK, "schedule starts neighbourhood walk");
    p.setPos(o.getX() + 63.5, o.getY() + 1, o.getZ() - 30.5);
    IntroAudit.ticks(server, 1);
    IntroAudit.check(
        CampusOpening.step(t) == CampusOpening.CLASS, "campus arrival updates objective");
    // Forged distant block interactions cannot complete classroom objectives.
    CampusOpening.interact(p, t, o.offset(SccCampus.CLASS_DESK));
    IntroAudit.check(
        CampusOpening.step(t) == CampusOpening.CLASS, "distance checked for interactions");
    use(p, t, SccCampus.CLASS_DESK);
    IntroAudit.check(CampusOpening.step(t) == CampusOpening.LESSON, "classroom starts lesson");
    p.setPos(o.getX() + 38.5, o.getY() + 1, o.getZ() + 12.5);
    IntroAudit.ticks(server, 20);
    IntroAudit.check(
        t.getInt(CampusOpening.AGE) == 0, "class exercise pauses when player leaves room");
    p.setPos(o.getX() + 83.5, o.getY() + 1, o.getZ() - 41.5);
    IntroAudit.ticks(server, CampusOpening.LESSON_TICKS - 1);
    IntroAudit.check(
        CampusOpening.step(t) == CampusOpening.LESSON, "lesson retains a short playable beat");
    IntroAudit.ticks(server, 1);
    IntroAudit.check(CampusOpening.step(t) == CampusOpening.CLUB, "lesson hands off to club");
    p.setPos(o.getX() + 105.5, o.getY() + 1, o.getZ() - 36.5);
    var npcUse =
        new net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteractSpecific(
            p,
            net.minecraft.world.InteractionHand.MAIN_HAND,
            clubActor,
            net.minecraft.world.phys.Vec3.ZERO);
    IntroSequence.entityUseSpecific(npcUse);
    IntroAudit.check(
        npcUse.isCanceled()
            && npcUse.getCancellationResult() == net.minecraft.world.InteractionResult.SUCCESS,
        "NPC-specific right-click is consumed without trades or duplicate callbacks");
    IntroAudit.check(CampusOpening.step(t) == CampusOpening.FALL, "club starts falling satellite");
    // Rejoining does not replay the club or reset fall age.
    IntroAudit.ticks(server, 10);
    var fallingParts = new java.util.ArrayList<net.minecraft.world.entity.Entity>();
    for (int i = 0; i < 3; i++) {
      String key = SatelliteCrash.introKey(i);
      IntroAudit.check(t.hasUUID(key), "actual satellite part UUID persisted " + i);
      if (t.hasUUID(key)) {
        var part = campus.getEntity(t.getUUID(key));
        IntroAudit.check(
            part instanceof net.minecraft.world.entity.Display.BlockDisplay,
            "real satellite block display " + i);
        if (part != null) {
          fallingParts.add(part);
          IntroAudit.check(SatelliteCrash.ownsIntro(t, part), "fall entity tied to active intro");
          var loaded = new net.neoforged.neoforge.event.entity.EntityJoinLevelEvent(part, campus);
          CampusOpening.actorLoad(loaded);
          IntroAudit.check(!loaded.isCanceled(), "owned falling satellite survives reload");
        }
      }
    }
    IntroAudit.check(
        !SatelliteCrash.falling(), "intro does not enter or consume overworld fall queue");
    int age = t.getInt(CampusOpening.AGE);
    IntroSequence.login(
        new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent(p));
    IntroAudit.check(t.getInt(CampusOpening.AGE) == age, "rejoin preserves in-progress fall");
    IntroAudit.ticks(server, CampusOpening.fallTicks(t) - 10);
    IntroAudit.check(
        CampusOpening.step(t) == CampusOpening.INVESTIGATE,
        "impact leaves investigation as a playable objective");
    IntroAudit.check(
        campus.getBlockState(o.offset(SccCampus.WRECK)).is(Blocks.BARREL),
        "satellite creates actual SatelliteCrash recovery crate");
    IntroAudit.check(
        campus.getBlockState(o.offset(SccCampus.WRECK).above()).isAir(),
        "actual recovery crate has exposed upper face");
    for (var part : fallingParts) {
      IntroAudit.check(part.isRemoved(), "falling satellite display removed on impact");
      var stale = new net.neoforged.neoforge.event.entity.EntityJoinLevelEvent(part, campus);
      CampusOpening.actorLoad(stale);
      IntroAudit.check(stale.isCanceled(), "old fall display cannot reappear after impact");
    }
    for (int i = 0; i < 3; i++)
      IntroAudit.check(!t.hasUUID(SatelliteCrash.introKey(i)), "fall UUID cleaned after impact");
    IntroAudit.check(
        campus.getBlockState(o.offset(140, -3, -8)).is(Blocks.IRON_BLOCK),
        "real satellite body in crater");
    IntroAudit.check(
        campus.getBlockState(o.offset(140, -2, -9)).is(Blocks.DAYLIGHT_DETECTOR),
        "real broken solar panel");
    IntroAudit.check(
        campus.getBlockState(o.offset(142, -2, -6)).is(Content.LOGO.get()),
        "real SARC satellite logo");
    IntroAudit.check(
        campus.getBlockState(o.offset(138, -3, -7)).is(Blocks.CAMPFIRE),
        "real signal-smoke campfire");
    if (campus.getBlockEntity(o.offset(SccCampus.WRECK))
        instanceof net.minecraft.world.level.block.entity.BarrelBlockEntity barrel)
      IntroAudit.check(
          barrel.getItem(4).is(net.minecraft.world.item.Items.WRITTEN_BOOK),
          "actual flight recorder retained in recovery crate");
    SccCampusAudit.craterRoute(campus, o);
    use(p, t, new BlockPos(19, 1, 12));
    IntroAudit.check(t.getInt("Stage") == IntroSequence.HOME, "must investigate before night");
    use(p, t, SccCampus.WRECK);
    IntroAudit.check(
        t.getBoolean("SatelliteInvestigated") && CampusOpening.step(t) == CampusOpening.RETURN,
        "investigation records causal flag and unlocks return");
    long coreLines =
        p.messages.stream().filter(m -> m.equals("message.timelocked.scc.core")).count();
    use(p, t, SccCampus.WRECK);
    IntroAudit.check(
        p.messages.stream().filter(m -> m.equals("message.timelocked.scc.core")).count()
            == coreLines,
        "investigation progression never repeats");
    CompoundTag saved = IntroState.get(server).save(new CompoundTag(), server.registryAccess());
    CompoundTag restored = IntroState.load(saved, server.registryAccess()).players.get(p.getUUID());
    IntroAudit.check(
        restored.getBoolean("SatelliteInvestigated")
            && restored.getInt(CampusOpening.STEP) == CampusOpening.RETURN,
        "campus progress survives save/load");
    use(p, t, new BlockPos(19, 1, 12));
    IntroAudit.check(
        p.level() == server.getLevel(IntroSequence.HOME_DIM)
            && t.getInt("Stage") == IntroSequence.BEDTIME,
        "return advances to separate fixed-night home");
    IntroAudit.check(
        clubActor.isRemoved() && !t.hasUUID("CampusClubmate"), "campus NPCs cleaned on departure");
    IntroAudit.check(!CampusOpening.active(t), "night uses existing break-in, not campus logic");
    var staleLoad = new net.neoforged.neoforge.event.entity.EntityJoinLevelEvent(clubActor, campus);
    CampusOpening.actorLoad(staleLoad);
    IntroAudit.check(
        staleLoad.isCanceled(), "unloaded departed NPCs are rejected when their chunk reloads");

    IntroAudit.check(
        p.getInventory().getItem(0).getCount() == 7, "campus day never takes player inventory");
    IntroAudit.check(
        p.messages.stream().noneMatch(m -> m.toLowerCase().contains("pantheris")),
        "campus dialogue does not reveal antagonist identity");
    // Explicit legacy fixture: old HOME saves without the version flag retain their TV objective.
    t.remove(CampusOpening.VERSION);
    IntroSequence.stage(p, t, IntroSequence.HOME);
    IntroScenes.house(p.serverLevel(), IntroSequence.home(t));
    IntroAudit.check(
        !CampusOpening.active(t) && IntroSequence.sceneDimension(t).equals(IntroSequence.HOME_DIM),
        "old active HOME saves retain original dimension and flow");
  }

  static void routes(ServerLevel l, BlockPos o) {
    var queue = new java.util.ArrayDeque<BlockPos>();
    var seen = new java.util.HashSet<BlockPos>();
    BlockPos first = o.offset(19, 1, 13);
    queue.add(first);
    seen.add(first);
    while (!queue.isEmpty()) {
      BlockPos at = queue.removeFirst();
      for (var d : net.minecraft.core.Direction.Plane.HORIZONTAL) {
        BlockPos next = at.relative(d);
        if (!SccCampus.contains(o, next) || seen.contains(next)) continue;
        if (!l.getBlockState(next).getCollisionShape(l, next).isEmpty()
            || !l.getBlockState(next.above()).getCollisionShape(l, next.above()).isEmpty()
            || !l.getBlockState(next.below()).isSolidRender(l, next.below())) continue;
        seen.add(next);
        queue.addLast(next);
      }
    }
    for (BlockPos goal :
        java.util.List.of(
            IntroCampus.SCHEDULE, SccCampus.CLASS_DESK, SccCampus.CLUB_DESK, SccCampus.WRECK)) {
      BlockPos target = o.offset(goal);
      IntroAudit.check(
          seen.stream().anyMatch(at -> at.distSqr(target) <= 9),
          "walkable route from bed to objective " + goal);
    }
  }

  private CampusOpeningAudit() {}
}
