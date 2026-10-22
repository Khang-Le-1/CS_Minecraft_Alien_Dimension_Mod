package com.timelocked;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;

/** Saved HOME substeps: existing stage IDs, old saves and beach protocol remain unchanged. */
@net.neoforged.fml.common.EventBusSubscriber(modid = "timelocked")
public final class CampusOpening {
  static final String VERSION = "CampusOpening";
  static final String STEP = "CampusStep";
  static final String AGE = "CampusAge";
  static final int MORNING = 1,
      WALK = 2,
      CLASS = 3,
      LESSON = 4,
      CLUB = 5,
      FALL = 6,
      INVESTIGATE = 7,
      RETURN = 8;
  static final int LESSON_TICKS = 160;
  static final int FALL_TICKS = 80;
  static final int IMPACT_TICKS = 60;
  static final int CUE_TICKS = 20;
  static final int CLASS_GATE_X = 42;
  static final double USE_RANGE_SQUARED = 36;

  private static final String[] ACTORS = {"CampusTeacher", "CampusClassmate", "CampusClubmate"};

  static void populate(ServerPlayer p, CompoundTag t) {
    BlockPos o = IntroSequence.home(t);
    int[][] spots =
        scc(t)
            ? new int[][] {{84, 1, -39}, {80, 1, -43}, {106, 1, -39}}
            : new int[][] {{65, 1, 19}, {61, 1, 23}, {65, 1, 55}};
    if (CampusRebuild.active(t)) {
      spots[0] = new int[] {80, 1, -48};
      spots[1] = new int[] {80, 1, -43};
    }
    if (modern(t))
      for (int[] spot : spots) {
        spot[0] += 180;
        spot[1] += 4;
        spot[2] -= 64;
      }
    String[] names = {"Instructor", "Classmate", "Club member"};
    for (int i = 0; i < ACTORS.length; i++) {
      var actor = net.minecraft.world.entity.EntityType.VILLAGER.create(p.serverLevel());
      if (actor == null) continue;
      actor.moveTo(
          o.getX() + spots[i][0] + .5, o.getY() + spots[i][1], o.getZ() + spots[i][2] + .5, 90, 0);
      actor.setNoAi(true);
      actor.setInvulnerable(true);
      actor.setSilent(true);
      actor.setPersistenceRequired();
      actor.setCustomName(Component.literal(names[i]));
      actor.setCustomNameVisible(true);
      actor.addTag("timelocked_campus_actor");
      actor.addTag(ACTORS[i]);
      actor.getPersistentData().putUUID("CampusOwner", p.getUUID());
      t.putUUID(ACTORS[i], actor.getUUID());
      if (!p.serverLevel().addFreshEntity(actor)) t.remove(ACTORS[i]);
    }
    if (CampusRebuild.active(t)) CampusRebuild.populate(p, t, o);
  }

  static boolean owns(CompoundTag t, net.minecraft.world.entity.Entity actor) {
    if (!active(t)) return false;
    for (String key : actorKeys())
      if (t.hasUUID(key) && t.getUUID(key).equals(actor.getUUID())) return true;
    return false;
  }

  @net.neoforged.bus.api.SubscribeEvent
  public static void actorLoad(net.neoforged.neoforge.event.entity.EntityJoinLevelEvent e) {
    if (!(e.getLevel() instanceof net.minecraft.server.level.ServerLevel level)
        || !(e.getEntity().getTags().contains("timelocked_campus_actor")
            || e.getEntity().getTags().contains("timelocked_campus_satellite"))) return;
    var data = e.getEntity().getPersistentData();
    CompoundTag owner =
        data.hasUUID("CampusOwner")
            ? IntroState.get(level.getServer()).players.get(data.getUUID("CampusOwner"))
            : null;
    if (!(e.getEntity().getTags().contains("timelocked_campus_satellite")
        ? SatelliteCrash.ownsIntro(owner, e.getEntity())
        : owns(owner, e.getEntity()))) e.setCanceled(true);
  }

  static void cleanup(ServerPlayer p, CompoundTag t) {
    SatelliteCrash.clearIntro(p, t);
    var l = p.server.getLevel(IntroSequence.CAMPUS_DIM);
    for (String key : actorKeys()) {
      if (l != null && t.hasUUID(key)) {
        var actor = l.getEntity(t.getUUID(key));
        if (actor != null) actor.discard();
      }
      t.remove(key);
    }
  }

  static void entityInteract(
      ServerPlayer p, CompoundTag t, net.minecraft.world.entity.Entity actor) {
    if (!owns(t, actor)
        || !p.level().dimension().equals(IntroSequence.CAMPUS_DIM)
        || p.distanceToSqr(actor) > USE_RANGE_SQUARED) return;
    if (actor.getTags().contains("CampusTeacher"))
      interact(p, t, IntroSequence.home(t).offset(class_desk(t)));
    else if (actor.getTags().contains("CampusClubmate"))
      interact(p, t, IntroSequence.home(t).offset(club_desk(t)));
    else if (actor.getTags().contains("CampusClassmate")) line(p, "classmate");
  }

  static boolean active(CompoundTag t) {
    return t != null && t.getBoolean(VERSION) && t.getInt("Stage") == IntroSequence.HOME;
  }

  static int step(CompoundTag t) {
    return Math.clamp(t.getInt(STEP), MORNING, RETURN);
  }

  static void line(ServerPlayer p, String key) {
    p.sendSystemMessage(
        Component.translatable(
            "message.timelocked." + (scc(IntroSequence.state(p)) ? "scc." : "campus.") + key));
  }

  static void advance(ServerPlayer p, CompoundTag t, int next, String key) {
    t.putInt(STEP, next);
    t.putInt(AGE, 0);
    IntroState.get(p.server).setDirty();
    line(p, key);
  }

  static void objective(ServerPlayer p, CompoundTag t) {
    if (modern(t) && step(t) == WALK) {
      p.displayClientMessage(
          Component.literal(
              switch (t.getInt("CampusStreetRoute")) {
                case 0 -> "Follow College Rd east toward Lambert Rd.";
                case 1 -> "Use the marked College Rd / Lambert Rd crossing.";
                case 2 -> "Turn north along Lambert Rd. Follow the east sidewalk to SCC.";
                default -> "Follow the rising SCC side path to the glass entrance doors.";
              }),
          true);
      return;
    }
    p.displayClientMessage(
        Component.translatable(
            "message.timelocked." + (scc(t) ? "scc." : "campus.") + "objective." + step(t)),
        true);
  }

  static void tick(ServerPlayer p, CompoundTag t) {
    BlockPos o = IntroSequence.home(t);
    if (!(modern(t)
        ? CampusNeighborhood.contains(o, p.blockPosition())
        : scc(t)
            ? SccCampus.contains(o, p.blockPosition())
            : IntroCampus.contains(o, p.blockPosition())))
      p.teleportTo(
          p.serverLevel(),
          o.getX() + 19.5,
          o.getY() + 1,
          o.getZ() + 13.5,
          p.getYRot(),
          p.getXRot());
    int s = step(t);
    if (modern(t) && s == WALK) CampusNeighborhood.route(p, t, o);
    if (s == LESSON
        && !(scc(t)
            ? SccCampus.inClassroom(schoolOrigin(o, t), p.blockPosition())
            : IntroCampus.inClassroom(o, p.blockPosition()))) {
      if (t.getInt("Age") % CUE_TICKS == 0) {
        p.displayClientMessage(
            Component.translatable(
                scc(t)
                    ? "message.timelocked.scc.lesson_return"
                    : "message.timelocked.campus.lesson_return"),
            true);
      }
      return;
    }
    int age = Math.clamp(t.getInt(AGE), 0, LESSON_TICKS) + 1;
    t.putInt(AGE, age);
    if (age % CUE_TICKS == 0) {
      objective(p, t);
      IntroState.get(p.server).setDirty();
    }
    if (s == WALK
        && (scc(t)
            ? (modern(t)
                ? (t.getInt("CampusStreetRoute") == 3
                    && CampusNeighborhood.arrived(o, p.blockPosition()))
                : (p.getX() >= o.getX() + 56
                    && p.getX() <= o.getX() + 70
                    && p.getZ() <= o.getZ() - 28))
            : p.getX() >= o.getX() + CLASS_GATE_X)) advance(p, t, CLASS, "arrival");
    if (s == LESSON && age >= LESSON_TICKS) advance(p, t, CLUB, "class_done");
    if (s == FALL) {
      if (scc(t) && age <= impactTicks(t)) {
        SatelliteCrash.introFrame(
            p,
            t,
            net.minecraft.world.phys.Vec3.atBottomCenterOf(schoolOrigin(o, t).offset(100, 55, 24)),
            net.minecraft.world.phys.Vec3.atBottomCenterOf(
                schoolOrigin(o, t).offset(SccCampus.CRASH_SURFACE)),
            age);
      }
      if (!scc(t) && age <= IMPACT_TICKS) {
        double part = (double) age / IMPACT_TICKS;
        p.serverLevel()
            .sendParticles(
                p,
                ParticleTypes.END_ROD,
                true,
                o.getX() + (scc(t) ? 100 + 40 * part : 82 + 42 * part),
                o.getY() + 55 - 52 * part,
                o.getZ() + (scc(t) ? 24 - 32 * part : 60 - 30 * part),
                8,
                .25,
                .25,
                .25,
                .01);
      }
      if (age == impactTicks(t)) {
        SatelliteCrash.clearIntro(p, t);
        if (scc(t)) SccCampus.impact(p.serverLevel(), schoolOrigin(o, t));
        else IntroCampus.impact(p.serverLevel(), o);
        p.serverLevel()
            .sendParticles(
                p,
                ParticleTypes.EXPLOSION_EMITTER,
                true,
                o.getX() + wreck(t).getX() + .5,
                o.getY() + (modern(t) ? 7 : 3),
                o.getZ() + wreck(t).getZ() + .5,
                1,
                0,
                0,
                0,
                0);
        line(p, "flash");
      }
      if (age >= fallTicks(t)) {
        IntroSequence.sound(p, SoundEvents.GENERIC_EXPLODE.value(), .7f, .7f);
        advance(p, t, INVESTIGATE, "impact");
      }
    }
    if (s == INVESTIGATE && age % CUE_TICKS == 0)
      p.serverLevel()
          .sendParticles(
              p,
              ParticleTypes.CAMPFIRE_COSY_SMOKE,
              true,
              o.getX() + wreck(t).getX() + .5,
              o.getY() + (modern(t) ? 7 : 3),
              o.getZ() + wreck(t).getZ() + .5,
              4,
              .4,
              .3,
              .4,
              .01);
  }

  static boolean near(ServerPlayer p, BlockPos clicked, BlockPos target) {
    return clicked.distSqr(target) <= 2
        && p.distanceToSqr(target.getX() + .5, target.getY() + .5, target.getZ() + .5)
            <= USE_RANGE_SQUARED;
  }

  static void interact(ServerPlayer p, CompoundTag t, BlockPos at) {
    BlockPos o = IntroSequence.home(t);
    int s = step(t);
    if (near(p, at, o.offset(IntroCampus.SCHEDULE))) {
      if (s == MORNING) advance(p, t, WALK, "schedule");
      else line(p, "schedule_repeat");
    } else if (near(p, at, o.offset(class_desk(t)))) {
      if (s == CLASS) advance(p, t, LESSON, "lesson");
      else line(p, "class_hint");
    } else if (near(p, at, o.offset(club_desk(t)))) {
      if (s == CLUB) advance(p, t, FALL, "club");
      else line(p, "club_hint");
    } else if (near(p, at, o.offset(wreck(t)))
        && p.serverLevel()
            .getBlockState(o.offset(wreck(t)))
            .is(
                scc(t)
                    ? net.minecraft.world.level.block.Blocks.BARREL
                    : net.minecraft.world.level.block.Blocks.LODESTONE)) {
      if (s == INVESTIGATE) {
        // The player sees only an anomalous response; Pantheris's interest stays hidden.
        IntroSequence.sound(p, SoundEvents.BEACON_ACTIVATE, .35f, 1.4f);
        p.serverLevel()
            .sendParticles(
                p,
                ParticleTypes.END_ROD,
                true,
                o.getX() + wreck(t).getX() + .5,
                o.getY() + (modern(t) ? 6 : 2),
                o.getZ() + wreck(t).getZ() + .5,
                16,
                .4,
                .5,
                .4,
                .04);
        t.putBoolean("SatelliteInvestigated", true);
        advance(p, t, RETURN, "core");
      } else if (s == RETURN) line(p, "core_repeat");
    } else if ((near(p, at, o.offset(19, 1, 12)) || near(p, at, o.offset(19, 1, 11)))) {
      if (s == RETURN && t.getBoolean("SatelliteInvestigated")) night(p, t);
      else line(p, "bed_early");
    } else if (near(
        p, at, o.offset(scc(t) ? shifted(t, new BlockPos(80, 1, -42)) : new BlockPos(61, 1, 22))))
      line(p, "classmate");
    else if (near(
        p, at, o.offset(scc(t) ? shifted(t, new BlockPos(117, 1, -43)) : new BlockPos(80, 1, 58))))
      line(p, s >= INVESTIGATE ? "club_after" : "clubmate");
    else if (IntroScenes.isTv(o, at)) line(p, "tv");
  }

  static void night(ServerPlayer p, CompoundTag t) {
    var night = p.server.getLevel(IntroSequence.HOME_DIM);
    if (night == null) {
      line(p, "night_unavailable");
      return;
    }
    BlockPos o = IntroScenes.home(t.getInt("Slot"));
    if (SccPhotoRevision.active(t)) SccPhotoRevision.house(night, o, false);
    else if (CampusRebuild.active(t)) CampusRebuild.house(night, o, false);
    else if (modern(t)) LivedInHome.build(night, o, false);
    else IntroScenes.house(night, o);
    IntroScenes.tvOff(night, o);
    cleanup(p, t);
    IntroSequence.stage(p, t, IntroSequence.BEDTIME);
    p.teleportTo(night, o.getX() + 9.5, o.getY() + 1, o.getZ() + 8.5, 145, 0);
    line(p, "evening");
    IntroSequence.custom(p, "rain", .3f);
  }

  static int impactTicks(CompoundTag t) {
    return scc(t) ? SatelliteCrash.FALL_TICKS : IMPACT_TICKS;
  }

  static int fallTicks(CompoundTag t) {
    return impactTicks(t) + CUE_TICKS;
  }

  static boolean scc(CompoundTag t) {
    return t != null && t.getBoolean(SccCampus.REVISION);
  }

  static BlockPos class_desk(CompoundTag t) {
    return CampusRebuild.active(t)
        ? shifted(t, new BlockPos(79, 1, -48))
        : scc(t) ? shifted(t, SccCampus.CLASS_DESK) : IntroCampus.CLASS_DESK;
  }

  static BlockPos club_desk(CompoundTag t) {
    return scc(t) ? shifted(t, SccCampus.CLUB_DESK) : IntroCampus.CLUB_DESK;
  }

  static BlockPos wreck(CompoundTag t) {
    return scc(t) ? shifted(t, SccCampus.WRECK) : IntroCampus.WRECK;
  }

  static boolean modern(CompoundTag t) {
    return t != null && t.getBoolean(CampusNeighborhood.REVISION);
  }

  static BlockPos shifted(CompoundTag t, BlockPos pos) {
    return modern(t) ? pos.offset(180, 4, -64) : pos;
  }

  static BlockPos schoolOrigin(BlockPos o, CompoundTag t) {
    return modern(t) ? o.offset(180, 4, -64) : o;
  }

  static java.util.List<String> actorKeys() {
    var keys = new java.util.ArrayList<String>(java.util.List.of(ACTORS));
    for (int i = 0; i < 10; i++) keys.add("CampusAmbient" + i);
    return keys;
  }

  private CampusOpening() {}
}
