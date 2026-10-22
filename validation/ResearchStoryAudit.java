package com.timelocked;

import java.util.HashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Test-only end-to-end interactions through each new beat on shipped structure geometry. */
final class ResearchStoryAudit {
  static void check(boolean value, String message) {
    IntroAudit.check(value, "research story: " + message);
  }

  static void quiet(CoastChapterAudit.Reader p) {
    for (int i = 0; i < 4; i++) ResearchStory.tickPlayer(p);
  }

  static CompoundTag ready(CoastChapterAudit.Reader p, BlockPos site) {
    var t = StaffChapterAudit.ready(p, site);
    var staff = new CompoundTag();
    staff.putInt("Step", StaffChapter.COMPLETE);
    staff.putBoolean(StaffChapter.CLEARANCE, true);
    t.put(StaffChapter.SAVE_KEY, staff);
    return t;
  }

  static void near(CoastChapterAudit.Reader p, BlockPos pos) {
    p.setPos(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
    p.setOnGround(true);
  }

  static BlockPos approach(BlockPos site, ResearchStory.Beat beat) {
    var at = site.offset(beat.target());
    if (beat.kind() == ResearchStory.Kind.OFFICER) return at.south(2);
    if (beat.target().equals(ResearchStory.RECEPTION)) return at.south(3).below();
    return at.east();
  }

  static void perform(
      CoastChapterAudit.Reader p,
      ResearchStory.Beat beat,
      SarcScientist officer,
      SarcScientist founder,
      BlockPos site) {
    switch (beat.kind()) {
      case OFFICER -> officer.mobInteract(p, InteractionHand.MAIN_HAND);
      case FOUNDER -> founder.mobInteract(p, InteractionHand.MAIN_HAND);
      case READ -> {
        BlockPos pos = site.offset(beat.target());
        var e =
            new PlayerInteractEvent.RightClickBlock(
                p,
                InteractionHand.MAIN_HAND,
                pos,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.EAST, pos, false));
        ResearchStory.terminalClick(e);
        check(e.isCanceled(), "real terminal event consumed for beat " + beat.id());
      }
      case WALK -> ResearchStory.tickPlayer(p);
    }
  }

  static void run(MinecraftServer server) throws Exception {
    var level = server.overworld();
    var rule = server.getGameRules().getRule(StoryMode.RULE);
    boolean previous = rule.get();
    var data = FacilityState.get(server);
    var site = new BlockPos(82000, 80, 82000);
    var oldSite = data.sites.put(site.asLong(), 45);
    var progress = new SiteProgress();
    progress.geometryVersion = BlueprintRooms.VERSION;
    progress.layoutVersion = 2;
    progress.arrivalLogoAnnounced = true;
    progress.arrivalStep = SiteProgress.FINAL_ARRIVAL_STAGE;
    progress.arrivalWelcomeNext = SiteProgress.WELCOME_LINE_COUNT;
    var oldProgress = data.progress.put(site.asLong(), progress);
    var p = new CoastChapterAudit.Reader(level, "ResearchReader");
    var second = new CoastChapterAudit.Reader(level, "ResearchSecond");
    SarcScientist officer = null, founder = null;
    try {
      rule.set(true, server);
      StaffChapterAudit.authored(level, site);
      var founderAt = site.offset(ResearchStory.FOUNDER_DESK);
      check(
          level.getBlockState(founderAt).isAir() && level.getBlockState(founderAt.above()).isAir(),
          "founder fits authored room");
      var floor = level.getBlockState(founderAt.below());
      level.setBlock(founderAt, Blocks.CHEST.defaultBlockState(), 2);
      check(
          ResearchStory.ensure(level, site) == null && level.getBlockEntity(founderAt) != null,
          "player chest not overwritten");
      level.setBlock(founderAt, Blocks.AIR.defaultBlockState(), 2);
      rule.set(false, server);
      check(ResearchStory.ensure(level, site) == null, "non-Story world gets no founder");
      rule.set(true, server);
      officer = StaffChapter.ensure(level, site);
      founder = ResearchStory.ensure(level, site);
      check(officer != null && founder != null, "separate real staff and founder spawn");
      if (officer == null || founder == null) return;
      check(
          ResearchStory.founder(founder)
              && !StaffChapter.officer(founder)
              && !ReceptionChapter.receptionist(founder)
              && !founder.isPushable()
              && founder.isNoAi()
              && founder.isInvulnerable()
              && founder.isPersistenceRequired(),
          "founder is a dedicated protected persistent role, not an ambient trader");
      var founderId = founder.getUUID();
      check(ResearchStory.ensure(level, site) == founder, "repeated ensure reuses founder");
      check(SiteProgress.load(progress.save()).founder.equals(founderId), "founder UUID saved");
      check(SiteProgress.load(new CompoundTag()).founder == null, "old sites load without founder");
      check(
          level.getBlockState(founderAt.below()).equals(floor), "founder placement edits no floor");
      p.setGameMode(GameType.SURVIVAL);
      second.setGameMode(GameType.SURVIVAL);
      var t = ready(p, site);
      var t2 = ready(second, site);
      near(p, site.offset(ResearchStory.DESK).south(2));
      var inv = new ItemStack(Items.DIAMOND, 7);
      p.getInventory().setItem(0, inv.copy());
      t.getCompound(StaffChapter.SAVE_KEY).putInt("Step", StaffChapter.RETURN);
      check(
          !ResearchStory.ready(p, t) && ResearchStory.view(t) == null,
          "Chapter 3 required before continuation");
      t.getCompound(StaffChapter.SAVE_KEY).putInt("Step", StaffChapter.COMPLETE);
      t.getCompound(StaffChapter.SAVE_KEY).remove(StaffChapter.CLEARANCE);
      officer.mobInteract(p, InteractionHand.MAIN_HAND);
      check(!t.contains(ResearchStory.SAVE_KEY), "missing clearance cannot begin story");
      t.getCompound(StaffChapter.SAVE_KEY).putBoolean(StaffChapter.CLEARANCE, true);
      officer.mobInteract(p, InteractionHand.OFF_HAND);
      check(!t.contains(ResearchStory.SAVE_KEY), "offhand cannot start story");
      near(second, site.offset(ResearchStory.DESK).south(2));
      // Existing manual door policy and every target must share a player-width route.
      StaffChapterAudit.door(p, site.offset(79, 2, 185));
      StaffChapterAudit.door(p, site.offset(79, 2, 170));
      var route = StaffChapterAudit.reachable(p, site, site.offset(64, 2, 331));
      for (var target :
          new BlockPos[] {
            ResearchStory.DESK,
            ResearchStory.FOUNDER_DESK,
            ResearchStory.ATRIUM,
            ResearchStory.CONNECTOR
          })
        check(
            route.contains(
                site.offset(target)
                    .offset(
                        target.equals(ResearchStory.DESK)
                            ? new BlockPos(0, 0, 2)
                            : new BlockPos(1, 0, 0))),
            "authored foot route to " + target);
      for (var target :
          new BlockPos[] {ResearchStory.INTAKE, ResearchStory.SIGNAL, ResearchStory.ARCHIVE}) {
        check(
            level.getBlockState(site.offset(target)).is(FacilityDetails.WORKSTATION.get()),
            "shipped workstation exists at " + target);
        check(
            route.contains(site.offset(target).east()), "normal reach route to terminal " + target);
      }
      check(
          route.contains(site.offset(67, 2, 330)), "reception terminal reachable from public lane");
      var transcript = new HashSet<Integer>();
      for (int i = 0; i < ResearchStory.COMPLETE; i++) {
        var beat = ResearchStory.BEATS.get(i);
        check(ResearchStory.stage(t) == i, "sequential stage before " + i);
        BlockPos at = site.offset(beat.target());
        near(p, site.offset(ResearchStory.DESK).south(2));
        quiet(p);
        // Round-trip the actual per-player SavedData at every milestone, then continue on reload.
        var saved =
            IntroState.load(
                    IntroState.get(server).save(new CompoundTag(), level.registryAccess()),
                    level.registryAccess())
                .players
                .get(p.getUUID());
        check(ResearchStory.stage(saved) == i, "full player save round trip at " + i);
        t = saved;
        IntroState.get(server).players.put(p.getUUID(), t);
        var beforeView = t.copy();
        var view =
            StoryObjective.describe(t, level.dimension().location(), p.blockPosition(), true);
        check(
            view != null && view.label().endsWith("research." + i) && view.target().equals(at),
            "HUD chapter/position for " + i);
        check(t.equals(beforeView), "HUD never mutates stage " + i);
        p.setPos(at.getX() + 40, at.getY(), at.getZ() + 40);
        if (beat.kind() == ResearchStory.Kind.READ)
          check(!ResearchStory.terminal(p, at), "remote terminal rejected " + i);
        else if (beat.kind() != ResearchStory.Kind.WALK) {
          (beat.kind() == ResearchStory.Kind.FOUNDER ? founder : officer)
              .mobInteract(p, InteractionHand.MAIN_HAND);
          check(ResearchStory.stage(t) == i, "remote NPC rejected " + i);
        }
        near(p, approach(site, beat));
        if (beat.kind() == ResearchStory.Kind.READ) {
          check(!ResearchStory.terminal(p, at.east(3)), "wrong workstation rejected " + i);
          p.setShiftKeyDown(true);
          check(!ResearchStory.terminal(p, at), "sneak preserves vanilla interaction " + i);
          p.setShiftKeyDown(false);
          var off =
              new PlayerInteractEvent.RightClickBlock(
                  p,
                  InteractionHand.OFF_HAND,
                  at,
                  new BlockHitResult(Vec3.atCenterOf(at), Direction.EAST, at, false));
          ResearchStory.terminalClick(off);
          check(!off.isCanceled() && ResearchStory.stage(t) == i, "offhand terminal rejected " + i);
        }
        var pos = p.position();
        rule.set(false, server);
        if (beat.kind() == ResearchStory.Kind.READ)
          check(!ResearchStory.terminal(p, at), "disabled terminal " + i);
        else perform(p, beat, officer, founder, site);
        check(ResearchStory.stage(t) == i, "Story-disabled cannot advance " + i);
        rule.set(true, server);
        p.setGameMode(GameType.SPECTATOR);
        if (beat.kind() == ResearchStory.Kind.READ)
          check(!ResearchStory.terminal(p, at), "spectator terminal " + i);
        else perform(p, beat, officer, founder, site);
        check(ResearchStory.stage(t) == i, "spectator cannot advance " + i);
        p.setGameMode(GameType.SURVIVAL);
        p.setHealth(0);
        if (beat.kind() == ResearchStory.Kind.READ)
          check(!ResearchStory.terminal(p, at), "dead terminal " + i);
        else perform(p, beat, officer, founder, site);
        check(ResearchStory.stage(t) == i, "dead player cannot advance " + i);
        p.setHealth(20);
        if (beat.kind() == ResearchStory.Kind.WALK) {
          p.setOnGround(false);
          ResearchStory.tickPlayer(p);
          check(ResearchStory.stage(t) == i, "flying past checkpoint does not advance " + i);
          p.setOnGround(true);
        } else if (beat.kind() == ResearchStory.Kind.OFFICER
            || beat.kind() == ResearchStory.Kind.FOUNDER) {
          var wrong = beat.kind() == ResearchStory.Kind.OFFICER ? founder : officer;
          near(p, wrong.blockPosition().south(2));
          wrong.mobInteract(p, InteractionHand.MAIN_HAND);
          check(ResearchStory.stage(t) == i, "wrong staff role cannot advance " + i);
          near(p, approach(site, beat));
          pos = p.position();
        }
        perform(p, beat, officer, founder, site);
        check(ResearchStory.stage(t) == i + 1, "actual interaction advances exactly one beat " + i);
        check(
            p.position().equals(pos)
                && p.gameMode.getGameModeForPlayer() == GameType.SURVIVAL
                && ItemStack.matches(inv, p.getMainHandItem()),
            "no teleport, mode change or inventory use " + i);
        check(
            !progress.powered && !progress.cleared && !progress.fighting,
            "no live equipment or encounter " + i);
        var next = ResearchStory.current(t);
        if (next != null) {
          near(p, approach(site, next));
          perform(p, next, officer, founder, site);
          check(ResearchStory.stage(t) == i + 1, "rapid input cannot skip next beat after " + i);
        }
        check(
            ResearchStory.stage(t2) == 0 && !t2.contains(ResearchStory.SAVE_KEY),
            "second player not advanced by shared NPC or terminal " + i);
        transcript.add(i);
      }
      check(transcript.size() == 38, "all 38 beats exercised");
      for (int i : transcript) {
        final String key = "message.timelocked.research.line." + i;
        check(p.keys.stream().filter(key::equals).count() == 1, "no duplicate dialogue " + i);
      }
      check(ResearchStory.view(t) == null, "completed story hides HUD");
      near(p, founderAt.east());
      quiet(p);
      founder.mobInteract(p, InteractionHand.MAIN_HAND);
      near(p, site.offset(ResearchStory.DESK).south(2));
      officer.mobInteract(p, InteractionHand.MAIN_HAND);
      check(ResearchStory.stage(t) == ResearchStory.COMPLETE, "end state does not replay");
      check(
          t.getCompound(StaffChapter.SAVE_KEY).getBoolean(StaffChapter.CLEARANCE)
              && StaffChapter.step(t) == StaffChapter.COMPLETE
              && ReceptionChapter.step(t) == ReceptionChapter.COMPLETE,
          "earlier story and clearance preserved");
      // Identity and facility binding: marking an arbitrary scientist is not authorization.
      var impostor = SarcContent.SCIENTIST.get().create(level);
      impostor.getPersistentData().putBoolean(StaffChapter.NPC_ROLE, true);
      impostor.getPersistentData().putLong(StaffChapter.NPC_SITE, site.asLong());
      impostor.moveTo(site.offset(ResearchStory.DESK).getBottomCenter());
      near(second, site.offset(ResearchStory.DESK).south(2));
      impostor.mobInteract(second, InteractionHand.MAIN_HAND);
      check(!t2.contains(ResearchStory.SAVE_KEY), "forged staff role rejected by saved UUID");
      impostor.discard();
      t2.putLong("Site", site.east(1000).asLong());
      officer.mobInteract(second, InteractionHand.MAIN_HAND);
      check(!t2.contains(ResearchStory.SAVE_KEY), "other-site NPC rejected");
      t2.putLong("Site", site.asLong());
      officer.mobInteract(second, InteractionHand.MAIN_HAND);
      check(
          ResearchStory.stage(t2) == 1 && ResearchStory.stage(t) == ResearchStory.COMPLETE,
          "second player starts independently after first finishes");
      for (int bad : new int[] {-1, 39, Integer.MAX_VALUE}) {
        t2.getCompound(ResearchStory.SAVE_KEY).putInt("Stage", bad);
        quiet(second);
        officer.mobInteract(second, InteractionHand.MAIN_HAND);
        check(
            ResearchStory.stage(t2) == -1 && ResearchStory.view(t2) == null,
            "corrupt progression halts without reset " + bad);
      }
      t2.put(ResearchStory.SAVE_KEY, new CompoundTag());
      check(ResearchStory.stage(t2) == -1, "missing saved stage halts instead of replaying");
      t2.putString(ResearchStory.SAVE_KEY, "corrupt");
      check(ResearchStory.stage(t2) == -1, "wrong chapter tag type halts");
      var wrongType = new CompoundTag();
      wrongType.putString("Stage", "12");
      t2.put(ResearchStory.SAVE_KEY, wrongType);
      check(ResearchStory.stage(t2) == -1, "wrong stage tag type halts");
      t2.getCompound(ResearchStory.SAVE_KEY).putInt("Stage", 0);
      t2.getCompound(ResearchStory.SAVE_KEY).putInt("Cooldown", Integer.MAX_VALUE);
      ResearchStory.tickPlayer(second);
      check(
          t2.getCompound(ResearchStory.SAVE_KEY).getInt("Cooldown") <= ResearchStory.CHAT_GAP_TICKS,
          "corrupt cooldown bounded");
      t2.getCompound(StaffChapter.SAVE_KEY).putInt("Step", 999);
      check(
          !ResearchStory.ready(second, t2) && ResearchStory.view(t2) == null,
          "corrupt staff completion grants no new chapter");
      var entity = new CompoundTag();
      founder.save(entity);
      founder.discard();
      check(
          ResearchStory.ensure(level, site) == null && progress.founder.equals(founderId),
          "missing saved founder does not cause duplicate respawn");
      var loaded = EntityType.loadEntityRecursive(entity, level, e -> e);
      check(
          loaded instanceof SarcScientist && level.addFreshEntity(loaded),
          "founder entity reloads");
      founder = (SarcScientist) loaded;
      check(
          ResearchStory.ensure(level, site) == founder,
          "saved founder identity reused after reload");
      check(
          server.getCommands().getDispatcher().getRoot().getChild("sarcresearch") != null,
          "safe story status command registered");
    } finally {
      if (officer != null) officer.discard();
      if (founder != null) founder.discard();
      IntroState.get(server).players.remove(p.getUUID());
      IntroState.get(server).players.remove(second.getUUID());
      p.discard();
      second.discard();
      rule.set(previous, server);
      if (oldSite == null) data.sites.remove(site.asLong());
      else data.sites.put(site.asLong(), oldSite);
      if (oldProgress == null) data.progress.remove(site.asLong());
      else data.progress.put(site.asLong(), oldProgress);
    }
  }
}
