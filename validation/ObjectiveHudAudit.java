package com.timelocked;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;

/** Read-only objectives, private coordinate frames, handoffs, navigation math and wire protocol. */
final class ObjectiveHudAudit {
  static void check(boolean value, String label) {
    IntroAudit.check(value, "objective HUD: " + label);
  }

  static void step(CompoundTag t, String key, int value) {
    CompoundTag chapter = new CompoundTag();
    chapter.putInt("Step", value);
    t.put(key, chapter);
  }

  static StoryObjective.View campus(CompoundTag t, BlockPos player) {
    return StoryObjective.describe(t, IntroSequence.CAMPUS_DIM.location(), player, false);
  }

  static StoryObjective.View coast(CompoundTag t, boolean ready) {
    return StoryObjective.describe(t, Level.OVERWORLD.location(), BlockPos.ZERO, ready);
  }

  static void run(MinecraftServer server) throws Exception {
    var overworld = Level.OVERWORLD.location();
    check(
        StoryObjective.describe(null, overworld, BlockPos.ZERO, false) == null,
        "missing save hidden");
    check(
        StoryObjective.describe(new CompoundTag(), overworld, BlockPos.ZERO, false) == null,
        "unstarted save hidden");
    CompoundTag t = new CompoundTag();
    for (int stage = 1; stage <= 7; stage++) {
      t.putInt("Stage", stage);
      var view =
          StoryObjective.describe(t, IntroSequence.HOME_DIM.location(), BlockPos.ZERO, false);
      check((view != null) == (stage <= 3), "legacy stages and cinematic suppression " + stage);
      if (stage <= 3) {
        check(view.label().endsWith("home." + stage), "legacy label " + stage);
        check(
            StoryObjective.describe(t, overworld, BlockPos.ZERO, false) == null,
            "home wrong dimension " + stage);
      }
    }
    t.putInt("Stage", IntroSequence.HOME);
    t.putBoolean(CampusOpening.VERSION, true);
    t.putBoolean(CampusNeighborhood.REVISION, true);
    t.putBoolean(CampusRebuild.REVISION, true);
    t.putBoolean(SccCampus.REVISION, true);
    t.putBoolean(SccPhotoRevision.REVISION, true);
    t.putInt("Slot", 2);
    BlockPos origin = IntroSequence.home(t);
    for (int stage = 1; stage <= 8; stage++) {
      t.putInt(CampusOpening.STEP, stage);
      CompoundTag before = t.copy();
      var view = campus(t, origin);
      check(
          view != null && view.dimension().equals(IntroSequence.CAMPUS_DIM.location()),
          "campus objective " + stage);
      check(
          (view.target() == null) == (stage == CampusOpening.LESSON || stage == CampusOpening.FALL),
          "timed stage has no false destination " + stage);
      if (stage != CampusOpening.WALK)
        check(view.label().endsWith("campus." + stage), "campus label " + stage);
      check(t.equals(before), "read leaves campus save untouched " + stage);
      check(
          StoryObjective.describe(t, overworld, origin, false) == null,
          "campus wrong dimension " + stage);
    }
    t.putInt(CampusOpening.STEP, CampusOpening.MORNING);
    check(
        campus(t, origin).target().equals(origin.offset(IntroCampus.SCHEDULE)),
        "bedside schedule exact target");
    t.putInt(CampusOpening.STEP, CampusOpening.CLASS);
    check(
        campus(t, origin).target().equals(origin.offset(CampusOpening.class_desk(t))),
        "class interaction anchor");
    t.putInt(CampusOpening.STEP, CampusOpening.CLUB);
    check(
        campus(t, origin).target().equals(origin.offset(CampusOpening.club_desk(t))),
        "club interaction anchor");
    t.putInt(CampusOpening.STEP, CampusOpening.INVESTIGATE);
    check(
        campus(t, origin).target().equals(origin.offset(CampusOpening.wreck(t))),
        "satellite interaction anchor");
    t.putInt(CampusOpening.STEP, CampusOpening.WALK);
    int[][] spots = {{19, 1, 13}, {40, 1, -5}, {222, 1, -5}, {223, 1, -75}};
    int[][] targets = {{40, 1, -5}, {222, 1, -5}, {223, 1, -75}, {223, 1, -143}};
    for (int route = 0; route < 4; route++) {
      t.putInt("CampusStreetRoute", route);
      var p = origin.offset(spots[route][0], spots[route][1], spots[route][2]);
      var v = campus(t, p);
      check(
          v.target().equals(origin.offset(targets[route][0], targets[route][1], targets[route][2])),
          "street subtarget " + route);
      check(v.label().endsWith("route." + route), "street instruction " + route);
    }
    check(
        campus(t, origin.offset(223, 1, -143)).target().equals(origin.offset(230, 1, -143)),
        "enter ramp from its north foot, not through retaining wall");
    check(
        campus(t, origin.offset(230, 1, -140)).target().equals(origin.offset(230, 5, -94)),
        "uphill ramp faces south");
    check(
        campus(t, origin.offset(230, 5, -94)).target().equals(origin.offset(243, 5, -94)),
        "upper landing turns toward entrance");
    check(
        campus(t, origin.offset(243, 5, -94)).target().equals(origin.offset(243, 5, -99)),
        "landing to glass doors");
    t.putInt(CampusOpening.STEP, CampusOpening.RETURN);
    var own = campus(t, origin);
    t.putInt("Slot", 3);
    check(
        !campus(t, IntroSequence.home(t)).target().equals(own.target()),
        "private scene slots never share objective coordinates");
    for (String flag :
        new String[] {
          SccPhotoRevision.REVISION,
          CampusRebuild.REVISION,
          CampusNeighborhood.REVISION,
          SccCampus.REVISION
        }) {
      t.remove(flag);
      check(
          campus(t, IntroSequence.home(t)).target().equals(IntroSequence.home(t).offset(19, 1, 12)),
          "old campus revision supported without migration " + flag);
    }
    t = new CompoundTag();
    t.putInt("Stage", IntroSequence.DONE);
    t.putBoolean("AtBeach", true);
    check(coast(t, false) == null, "no fabricated site at world origin");
    BlockPos site = new BlockPos(-12000, 40, -16000);
    t.putLong("Site", site.asLong());
    for (int stage = 0; stage <= 8; stage++) {
      step(t, CoastChapter.SAVE_KEY, stage);
      CompoundTag before = t.copy();
      var view = coast(t, false);
      check(view != null, "coast objective " + stage);
      check(
          (view.target() == null) == (stage >= CoastChapter.HUB),
          "welcome has no misleading marker " + stage);
      check(t.equals(before), "read leaves coast save untouched " + stage);
      check(
          StoryObjective.describe(t, IntroSequence.HOME_DIM.location(), site, false) == null,
          "coast wrong dimension " + stage);
      if (stage == CoastChapter.ROAD)
        check(!view.height(), "terrain-graded road never reports bogus Y=0 descent");
      if (stage <= CoastChapter.BOARDWALK)
        check(view.target().getY() == 65, "shore landmark fixed elevation " + stage);
      if (stage == CoastChapter.WAYFINDING)
        check(view.target().getY() == site.getY() + 29, "surface walk height includes deck offset");
    }
    check(coast(t, false).label().endsWith("welcome"), "reception not revealed during welcome");
    for (int stage = 0; stage < ReceptionChapter.COMPLETE; stage++) {
      step(t, ReceptionChapter.SAVE_KEY, stage);
      var view = coast(t, true);
      check(view.label().endsWith("reception." + stage), "reception milestone " + stage);
      check(
          view.target()
              .equals(
                  site.offset(
                      stage == ReceptionChapter.REGISTER
                          ? ReceptionChapter.TERMINAL
                          : ReceptionChapter.DESK)),
          "reception target " + stage);
    }
    step(t, ReceptionChapter.SAVE_KEY, ReceptionChapter.COMPLETE);
    for (int stage = 0; stage < StaffChapter.COMPLETE; stage++) {
      step(t, StaffChapter.SAVE_KEY, stage);
      CompoundTag before = t.copy();
      var view = coast(t, true);
      check(view.label().endsWith("staff." + stage), "staff milestone " + stage);
      check(
          view.target()
              .equals(
                  site.offset(
                      stage == StaffChapter.ASSIGNED ? StaffChapter.TERMINAL : StaffChapter.DESK)),
          "staff target " + stage);
      check(t.equals(before), "read never grants clearance or changes assignment " + stage);
    }
    step(t, StaffChapter.SAVE_KEY, StaffChapter.COMPLETE);
    check(
        coast(t, true) == null,
        "finished released chapter hides panel instead of inventing next quest");
    for (int corrupt : new int[] {-99, 99, Integer.MAX_VALUE}) {
      step(t, StaffChapter.SAVE_KEY, corrupt);
      check(coast(t, true) == null, "invalid staff milestone treated as finished " + corrupt);
    }
    // All payload shapes, including negative world positions and explicit clear packets.
    var registry = server.registryAccess();
    var samples =
        java.util.List.of(
            ObjectiveNetworking.Update.of(null),
            new ObjectiveNetworking.Update(
                "hud.timelocked.objective.welcome", overworld, false, 0, false),
            new ObjectiveNetworking.Update(
                "hud.timelocked.objective.staff.2",
                overworld,
                true,
                site.offset(StaffChapter.TERMINAL).asLong(),
                true));
    for (var sample : samples) {
      var buffer =
          new net.minecraft.network.RegistryFriendlyByteBuf(
              io.netty.buffer.Unpooled.buffer(), registry);
      try {
        ObjectiveNetworking.Update.CODEC.encode(buffer, sample);
        check(
            ObjectiveNetworking.Update.CODEC.decode(buffer).equals(sample),
            "native payload codec round trip " + sample.label());
        check(buffer.readableBytes() == 0, "payload consumed completely");
      } finally {
        buffer.release();
      }
    }
    check(
        ObjectiveNetworking.Update.of(null).label().isEmpty(),
        "hidden objective explicitly clears previous client state");
    // Arrow rotation follows Minecraft's yaw, including wrapped and negative yaw.
    check(StoryObjective.bearing(0, 10, 0) == 0, "forward south");
    check(StoryObjective.bearing(-10, 0, 0) == 90, "west is right facing south");
    check(StoryObjective.bearing(10, 0, 0) == -90, "east is left facing south");
    check(StoryObjective.bearing(0, -10, 0) == -180, "north behind");
    check(StoryObjective.bearing(0, -10, 90) == 90, "north right facing west");
    check(StoryObjective.bearing(0, 10, 720) == 0, "wrapped yaw");
    check(StoryObjective.distance(3, 4) == 5, "horizontal distance exact");
    check(StoryObjective.distance(.5, .5) == 1, "fractional distance rounded up");
    check(
        StoryObjective.distance(60000000, 60000000) > 80000000,
        "world-scale distance does not overflow");
    for (int yaw = -1080; yaw <= 1080; yaw += 15) {
      double angle = StoryObjective.bearing(-31, 47, yaw);
      check(angle >= -180 && angle < 180, "bearing stays normalized " + yaw);
    }
    var rule = server.getGameRules().getRule(StoryMode.RULE);
    boolean old = rule.get();
    var player = new CoastChapterAudit.Reader(server.overworld(), "ObjectiveReader");
    try {
      rule.set(false, server);
      check(StoryObjective.resolve(player, t) == null, "disabled Story mode hidden");
      rule.set(true, server);
      player.setGameMode(GameType.SPECTATOR);
      check(StoryObjective.resolve(player, t) == null, "spectator hidden");
      player.setGameMode(GameType.SURVIVAL);
      player.setHealth(0);
      check(StoryObjective.resolve(player, t) == null, "dead player hidden");
      player.setHealth(20);
      check(StoryObjective.resolve(player, null) == null, "no intro state hidden");
    } finally {
      rule.set(old, server);
      player.discard();
    }
  }
}
