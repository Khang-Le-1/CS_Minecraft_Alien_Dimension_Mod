package com.timelocked;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/** Read-only presentation of existing story milestones. Never advances or repairs a save. */
public final class StoryObjective {
  public record View(String label, ResourceLocation dimension, BlockPos target, boolean height) {}

  static View resolve(ServerPlayer p, CompoundTag t) {
    if (!StoryMode.enabled(p.server) || !p.isAlive() || p.isSpectator() || t == null) return null;
    boolean reception = CoastChapter.eligible(p, t) && ReceptionChapter.ready(p, t);
    return describe(t, p.level().dimension().location(), p.blockPosition(), reception);
  }

  static View describe(
      CompoundTag t, ResourceLocation dimension, BlockPos player, boolean receptionReady) {
    if (t == null) return null;
    int stage = t.getInt("Stage");
    if (CampusOpening.active(t)) {
      if (!dimension.equals(IntroSequence.CAMPUS_DIM.location())) return null;
      BlockPos o = IntroSequence.home(t);
      int step = CampusOpening.step(t);
      BlockPos target =
          switch (step) {
            case CampusOpening.MORNING -> o.offset(IntroCampus.SCHEDULE);
            case CampusOpening.WALK ->
                CampusOpening.modern(t)
                    ? campusRoute(t, o, player)
                    : o.offset(CampusOpening.class_desk(t));
            case CampusOpening.CLASS -> o.offset(CampusOpening.class_desk(t));
            case CampusOpening.CLUB -> o.offset(CampusOpening.club_desk(t));
            case CampusOpening.INVESTIGATE -> o.offset(CampusOpening.wreck(t));
            case CampusOpening.RETURN -> o.offset(19, 1, 12);
            default -> null;
          };
      String label = "campus." + step;
      if (step == CampusOpening.WALK && CampusOpening.modern(t))
        label = "route." + Math.clamp(t.getInt("CampusStreetRoute"), 0, 3);
      return view(label, dimension, target, true);
    }
    if (stage == IntroSequence.HOME
        || stage == IntroSequence.BEDTIME
        || stage == IntroSequence.BREAKIN) {
      if (!dimension.equals(IntroSequence.HOME_DIM.location())) return null;
      BlockPos o = IntroSequence.home(t);
      BlockPos target =
          stage == IntroSequence.HOME
              ? o.offset(9, 2, 3)
              : stage == IntroSequence.BEDTIME ? o.offset(19, 1, 12) : null;
      return view("home." + stage, dimension, target, true);
    }
    if (stage != IntroSequence.DONE
        || !t.getBoolean("AtBeach")
        || !t.contains("Site")
        || !dimension.equals(Level.OVERWORLD.location())) return null;
    BlockPos site = BlockPos.of(t.getLong("Site"));
    int coast = CoastChapter.step(CoastChapter.chapter(t));
    if (coast == CoastChapter.COMPLETE) {
      // Do not expose the receptionist or briefing assignment before the welcome has finished.
      if (!receptionReady) return view("welcome", dimension, null, false);
      int reception = ReceptionChapter.step(t);
      if (reception != ReceptionChapter.COMPLETE)
        return view(
            "reception." + reception,
            dimension,
            site.offset(
                reception == ReceptionChapter.REGISTER
                    ? ReceptionChapter.TERMINAL
                    : ReceptionChapter.DESK),
            true);
      int staff = StaffChapter.step(t);
      if (staff == StaffChapter.COMPLETE) return ResearchStory.view(t);
      return view(
          "staff." + staff,
          dimension,
          site.offset(staff == StaffChapter.ASSIGNED ? StaffChapter.TERMINAL : StaffChapter.DESK),
          true);
    }
    if (coast == CoastChapter.HUB) return view("welcome", dimension, null, false);
    BlockPos junction = IntroCoast.roadJunction(site), surface = IntroCoast.surfaceOrigin(site);
    BlockPos target =
        switch (coast) {
          case 0, CoastChapter.SHORE -> junction.offset(273, 0, -6);
          case CoastChapter.PARKING -> junction.offset(200, 0, 0);
          case CoastChapter.BOARDWALK -> junction;
          // Inland road height is terrain-graded, not Y=0. Show horizontal distance only.
          case CoastChapter.ROAD -> new BlockPos(junction.getX(), 0, surface.getZ() + 688);
          case CoastChapter.WAYFINDING -> surface.offset(64, 29, 225);
          default -> entrance(site, player);
        };
    return view("coast." + coast, dimension, target, coast != CoastChapter.ROAD);
  }

  private static BlockPos campusRoute(CompoundTag t, BlockPos o, BlockPos p) {
    int route = Math.clamp(t.getInt("CampusStreetRoute"), 0, 3);
    BlockPos local = p.subtract(o);
    BlockPos target =
        switch (route) {
          case 0 -> new BlockPos(40, 1, -5);
          case 1 -> new BlockPos(222, 1, -5);
          case 2 -> new BlockPos(223, 1, -75);
          default ->
              local.getX() < 228
                  ? (local.getZ() > -139 ? new BlockPos(223, 1, -143) : new BlockPos(230, 1, -143))
                  : local.getX() < 237 && local.getZ() < -94
                      ? new BlockPos(230, 5, -94)
                      : local.getX() < 237 ? new BlockPos(243, 5, -94) : new BlockPos(243, 5, -99);
        };
    return o.offset(target);
  }

  private static BlockPos entrance(BlockPos site, BlockPos p) {
    BlockPos local = p.subtract(site);
    BlockPos target;
    if (local.getY() <= 5)
      target = local.getX() >= 87 ? new BlockPos(86, 2, 356) : new BlockPos(80, 2, 333);
    else if (local.getY() <= 18)
      target = local.getX() < 97 ? new BlockPos(99, 16, 334) : new BlockPos(99, 2, 355);
    else if (local.getX() >= 87 && local.getX() <= 96 && local.getZ() >= 332 && local.getZ() <= 350)
      target = new BlockPos(90, 16, 334);
    else target = new BlockPos(90, 29, 349);
    return site.offset(target);
  }

  private static View view(
      String label, ResourceLocation dimension, BlockPos target, boolean height) {
    return new View(
        "hud.timelocked.objective." + label, dimension, target, height && target != null);
  }

  /** Minecraft yaw: zero faces south; positive yaw turns west. Result rotates an up arrow. */
  public static double bearing(double dx, double dz, double yaw) {
    double angle = Math.toDegrees(Math.atan2(-dx, dz)) - yaw;
    return ((angle + 180) % 360 + 360) % 360 - 180;
  }

  public static int distance(double dx, double dz) {
    return (int) Math.ceil(Math.hypot(dx, dz));
  }

  private StoryObjective() {}
}
