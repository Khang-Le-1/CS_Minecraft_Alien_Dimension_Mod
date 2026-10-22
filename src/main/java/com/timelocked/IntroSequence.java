package com.timelocked;

import net.minecraft.commands.Commands;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.*;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * A per-player, saved, skippable opening. Never clears inventory or changes global time/weather.
 */
@EventBusSubscriber(modid = "timelocked")
public final class IntroSequence {
  // Stages below are persisted in saves/network packets. Never renumber them.
  // Durations use server ticks (20 ticks = one second at the normal tick rate).
  private static final int CUE_INTERVAL_TICKS = 20;
  private static final int LANDING_TIMEOUT_TICKS = 30 * 20;
  private static final int BLACKOUT_DURATION_TICKS = 50;
  private static final int WAKE_DURATION_TICKS = WakePresentation.DURATION_TICKS;
  private static final int INITIAL_JOIN_DELAY_TICKS = 30;
  private static final int FRESH_JOIN_MAX_PLAY_TICKS = 20;
  private static final int NEW_CHECKPOINT_MAX_PLAY_TICKS = 100;

  public static final int HOME = 1,
      BEDTIME = 2,
      BREAKIN = 3,
      BLACKOUT = 4,
      WAKE = 5,
      DONE = 6,
      LANDING = 7;
  public static final ResourceKey<Level> HOME_DIM =
      ResourceKey.create(
          Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath("timelocked", "intro_home"));

  public static final ResourceKey<Level> CAMPUS_DIM =
      ResourceKey.create(
          Registries.DIMENSION,
          ResourceLocation.fromNamespaceAndPath("timelocked", "intro_campus"));

  static ResourceKey<Level> sceneDimension(CompoundTag t) {
    return CampusOpening.active(t) ? CAMPUS_DIM : HOME_DIM;
  }

  static boolean active(CompoundTag t) {
    return t != null
        && t.getInt("Stage") >= HOME
        && (t.getInt("Stage") <= WAKE || t.getInt("Stage") == LANDING);
  }

  static CompoundTag state(ServerPlayer p) {
    return IntroState.get(p.server).players.get(p.getUUID());
  }

  static void line(ServerPlayer p, String key) {
    p.sendSystemMessage(Component.translatable("message.timelocked.intro." + key));
  }

  static void sound(ServerPlayer p, SoundEvent sound, float volume, float pitch) {
    p.playNotifySound(sound, SoundSource.AMBIENT, volume, pitch);
  }

  static void custom(ServerPlayer p, String name, float volume) {
    sound(
        p,
        switch (name) {
          case "tv" -> AlarmSounds.INTRO_TV.get();
          case "rain" -> AlarmSounds.INTRO_RAIN.get();
          case "transition" -> AlarmSounds.INTRO_TRANSITION.get();
          case "surf" -> AlarmSounds.INTRO_SURF.get();
          default -> throw new IllegalArgumentException(name);
        },
        volume,
        1);
  }

  static void stopSounds(ServerPlayer p) {
    for (String n : new String[] {"tv", "rain", "transition", "surf"})
      p.connection.send(
          new ClientboundStopSoundPacket(
              ResourceLocation.fromNamespaceAndPath("timelocked", "intro_" + n),
              SoundSource.AMBIENT));
  }

  static void cue(ServerPlayer p, CompoundTag t) {
    PacketDistributor.sendToPlayer(
        p,
        new IntroNetworking.Cue(
            t.getInt("Stage"), t.getInt("Age"), t.getLong("Anchor"), t.getInt("LandingToken")));
  }

  static void stage(ServerPlayer p, CompoundTag t, int stage) {
    t.putInt("Stage", stage);
    t.putInt("Age", 0);
    cue(p, t);
    IntroState.get(p.server).setDirty();
  }

  @SubscribeEvent
  public static void login(PlayerEvent.PlayerLoggedInEvent e) {
    if (!(e.getEntity() instanceof ServerPlayer p)) return;
    IntroState data = IntroState.get(p.server);
    CompoundTag t = data.players.get(p.getUUID());
    if (t != null) {
      if (active(t)) {
        stopSounds(p);
        if (t.getInt("Stage") == WAKE || t.getInt("Stage") == LANDING) {
          IntroDeparture.cleanup(p, t);
          beach(p, t);
        } else cue(p, t);
      }
      return;
    }
    t = new CompoundTag();
    t.putUUID("Player", p.getUUID());
    boolean fresh =
        p.getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME)) <= FRESH_JOIN_MAX_PLAY_TICKS
            && p.gameMode.getGameModeForPlayer() == GameType.SURVIVAL
            && p.level().dimension().equals(Level.OVERWORLD);
    t.putInt("Stage", data.enabled && StoryMode.enabled(p.server) && fresh ? 0 : DONE);
    t.putInt("Delay", INITIAL_JOIN_DELAY_TICKS);
    data.players.put(p.getUUID(), t);
    data.setDirty();
  }

  /** Commands are self-only. Replays require permission 2; skipping never does. */
  @SubscribeEvent
  public static void commands(RegisterCommandsEvent e) {
    e.getDispatcher()
        .register(
            Commands.literal("timelockedintro")
                .executes(
                    c -> {
                      c.getSource()
                          .sendSuccess(
                              () ->
                                  Component.literal(
                                      "/timelockedintro skip — leave the intro; rescue — recover a failed beach landing; operators: start, enable, disable. Replays preserve inventory."),
                              false);
                      return 1;
                    })
                .then(
                    Commands.literal("skip")
                        .executes(
                            c -> {
                              ServerPlayer p = c.getSource().getPlayerOrException();
                              CompoundTag t = state(p);
                              if (!active(t)) {
                                c.getSource().sendFailure(Component.literal("No active intro."));
                                return 0;
                              }
                              finish(p, t, true);
                              return 1;
                            }))
                .then(
                    Commands.literal("rescue")
                        .executes(
                            c -> {
                              ServerPlayer p = c.getSource().getPlayerOrException();
                              CompoundTag t = state(p);
                              if (t == null
                                  || active(t)
                                  || !t.getBoolean("AtBeach")
                                  || !t.contains("Site")
                                  || !p.level().dimension().equals(Level.OVERWORLD)) {
                                c.getSource()
                                    .sendFailure(
                                        Component.literal("No failed beach landing to recover."));
                                return 0;
                              }
                              BlockPos q = IntroCoast.wake(BlockPos.of(t.getLong("Site")));
                              double dx = p.getX() - q.getX(), dz = p.getZ() - q.getZ();
                              if (dx * dx + dz * dz > 64 * 64 || p.getY() >= q.getY() - .5) {
                                c.getSource()
                                    .sendFailure(
                                        Component.literal(
                                            "Rescue is only for falling beneath your intro beach."));
                                return 0;
                              }
                              t.putInt("Mode", p.gameMode.getGameModeForPlayer().getId());
                              t.putBoolean("OriginalNoGravity", p.isNoGravity());
                              t.putBoolean("SkipRequested", true);
                              t.putBoolean("RescueRequested", true);
                              return beach(p, t) ? 1 : 0;
                            }))
                .then(
                    Commands.literal("start")
                        .requires(s -> s.hasPermission(2))
                        .executes(
                            c -> {
                              ServerPlayer p = c.getSource().getPlayerOrException();
                              if (active(state(p))) {
                                c.getSource()
                                    .sendFailure(
                                        Component.literal(
                                            "Intro already running. Use /timelockedintro skip."));
                                return 0;
                              }
                              return begin(p) ? 1 : 0;
                            }))
                .then(
                    Commands.literal("enable")
                        .requires(s -> s.hasPermission(2))
                        .executes(
                            c -> {
                              IntroState s = IntroState.get(c.getSource().getServer());
                              s.enabled = true;
                              s.setDirty();
                              c.getSource()
                                  .sendSuccess(
                                      () ->
                                          Component.literal(
                                              "First-join intro enabled for new Survival players."),
                                      true);
                              return 1;
                            }))
                .then(
                    Commands.literal("disable")
                        .requires(s -> s.hasPermission(2))
                        .executes(
                            c -> {
                              IntroState s = IntroState.get(c.getSource().getServer());
                              s.enabled = false;
                              s.setDirty();
                              c.getSource()
                                  .sendSuccess(
                                      () ->
                                          Component.literal(
                                              "Automatic intro disabled. Active players can use /timelockedintro skip."),
                                      true);
                              return 1;
                            })));
  }

  static boolean begin(ServerPlayer p) {
    if (!p.level().dimension().equals(Level.OVERWORLD)) {
      line(p, "start_overworld");
      return false;
    }
    IntroState data = IntroState.get(p.server);
    ServerLevel home = p.server.getLevel(CAMPUS_DIM), earth = p.server.overworld();
    if (home == null || p.server.getLevel(HOME_DIM) == null) {
      line(p, "unavailable");
      return false;
    }
    // Deterministic nearest regional site. Search up to three regions for untouched coast modules.
    BlockPos site = null;
    int rx = Math.floorDiv(earth.getSharedSpawnPos().getX(), SitePlanner.SPACING),
        rz = Math.floorDiv(earth.getSharedSpawnPos().getZ(), SitePlanner.SPACING);
    for (int n = 0; n < 3; n++) {
      BlockPos candidate =
          SitePlanner.origin(earth, SitePlanner.horizontal(earth.getSeed(), rx + n, rz));
      BlockPos beach = IntroCoast.wake(candidate);
      earth.getChunk(beach.getX() >> 4, beach.getZ() >> 4);
      if (earth.getWorldBorder().isWithinBounds(beach)
          && earth.getBlockState(beach.below(2)).is(Blocks.SEA_LANTERN)
          && earth.getBlockState(beach.below()).is(Blocks.SAND)) {
        site = candidate;
        break;
      }
    }
    if (site == null) {
      line(p, "unavailable");
      return false;
    }
    CompoundTag t = new CompoundTag();
    t.putUUID("Player", p.getUUID());
    // Replaying the opening must not erase a completed or partially explored chapter.
    CompoundTag previous = state(p);
    if (previous != null && previous.contains(CoastChapter.SAVE_KEY))
      t.put(CoastChapter.SAVE_KEY, previous.getCompound(CoastChapter.SAVE_KEY).copy());
    if (previous != null && previous.contains(ReceptionChapter.SAVE_KEY))
      t.put(ReceptionChapter.SAVE_KEY, previous.getCompound(ReceptionChapter.SAVE_KEY).copy());
    if (previous != null && previous.contains(ResearchStory.SAVE_KEY))
      t.put(ResearchStory.SAVE_KEY, previous.getCompound(ResearchStory.SAVE_KEY).copy());
    if (previous != null && previous.contains(StaffChapter.SAVE_KEY))
      t.put(StaffChapter.SAVE_KEY, previous.getCompound(StaffChapter.SAVE_KEY).copy());
    t.putInt("Slot", data.nextSlot++);
    t.putLong("Site", site.asLong());
    t.putString("ReturnDimension", p.level().dimension().location().toString());
    t.putDouble("ReturnX", p.getX());
    t.putDouble("ReturnY", p.getY());
    t.putDouble("ReturnZ", p.getZ());
    t.putFloat("ReturnYaw", p.getYRot());
    t.putFloat("ReturnPitch", p.getXRot());
    t.putInt("Mode", p.gameMode.getGameModeForPlayer().getId());
    t.putBoolean("OriginalNoGravity", p.isNoGravity());
    // Only brand-new players receive the beach checkpoint; manual replays preserve existing beds.
    t.putBoolean(
        "NewPlayer",
        p.getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME)) < NEW_CHECKPOINT_MAX_PLAY_TICKS);
    t.putInt("Stage", HOME);
    t.putBoolean(CampusOpening.VERSION, true);
    t.putBoolean(SccCampus.REVISION, true);
    t.putBoolean(CampusNeighborhood.REVISION, true);
    t.putBoolean(CampusRebuild.REVISION, true);
    t.putBoolean(SccPhotoRevision.REVISION, true);
    t.putInt(CampusOpening.STEP, CampusOpening.MORNING);
    data.players.put(p.getUUID(), t);
    data.setDirty();
    BlockPos o = home(t);
    SccPhotoRevision.build(home, o);
    p.closeContainer();
    p.stopRiding();
    p.setGameMode(GameType.ADVENTURE);
    p.fallDistance = 0;
    p.teleportTo(home, o.getX() + 19.5, o.getY() + 1.6, o.getZ() + 12.5, 180, 15);
    stage(p, t, HOME);
    CampusOpening.populate(p, t);
    CampusOpening.line(p, "morning");
    line(p, "skip_hint");
    CampusOpening.objective(p, t);
    return true;
  }

  static BlockPos home(CompoundTag t) {
    return CampusOpening.active(t)
        ? (SccPhotoRevision.active(t)
            ? SccPhotoRevision.home(t.getInt("Slot"))
            : CampusRebuild.active(t)
                ? CampusRebuild.home(t.getInt("Slot"))
                : t.getBoolean(CampusNeighborhood.REVISION)
                    ? CampusNeighborhood.home(t.getInt("Slot"))
                    : t.getBoolean(SccCampus.REVISION)
                        ? SccCampus.home(t.getInt("Slot"))
                        : IntroCampus.home(t.getInt("Slot")))
        : IntroScenes.home(t.getInt("Slot"));
  }

  static void cleanupActor(ServerPlayer p, CompoundTag t) {
    CampusOpening.cleanup(p, t);
    if (t.hasUUID("Actor")) {
      ServerLevel l = p.server.getLevel(HOME_DIM);
      if (l != null) {
        Entity a = l.getEntity(t.getUUID("Actor"));
        if (a != null) a.discard();
      }
      t.remove("Actor");
    }
  }

  static boolean beach(ServerPlayer p, CompoundTag t) {
    cleanupActor(p, t);
    for (String n : new String[] {"tv", "rain"})
      p.connection.send(
          new ClientboundStopSoundPacket(
              ResourceLocation.fromNamespaceAndPath("timelocked", "intro_" + n),
              SoundSource.AMBIENT));
    ServerLevel earth = p.server.overworld();
    BlockPos q = IntroCoast.wake(BlockPos.of(t.getLong("Site")));
    IntroLanding.preload(earth, q);
    BlockPos safe = null;
    for (int radius = 0; radius <= 8 && safe == null; radius++)
      for (int x = -radius; x <= radius && safe == null; x++)
        for (int z = -radius; z <= radius; z++) {
          BlockPos at = q.offset(x, 0, z);
          if (IntroLanding.supported(earth, at)) {
            safe = at;
            break;
          }
        }
    if (safe == null) {
      recover(p, t);
      return false;
    }
    q = safe;
    t.putLong("Anchor", q.asLong());
    t.putInt(
        "LandingToken",
        java.util.concurrent.ThreadLocalRandom.current().nextInt(1, Integer.MAX_VALUE));
    t.putBoolean("ClientReady", false);
    p.setNoGravity(true);
    p.setDeltaMovement(Vec3.ZERO);
    p.fallDistance = 0;
    p.teleportTo(earth, q.getX() + .5, q.getY(), q.getZ() + .5, -90, 12);
    if (t.getBoolean("NewPlayer")) p.setRespawnPosition(Level.OVERWORLD, q, 90, true, false);
    t.putBoolean("AtBeach", true);
    stage(p, t, LANDING);
    return true;
  }

  static void finish(ServerPlayer p, CompoundTag t, boolean skipped) {
    if (skipped) t.putBoolean("SkipRequested", true);
    if (!t.getBoolean("AtBeach")) {
      beach(p, t);
      return;
    }
    if (!t.getBoolean("ClientReady")) return;
    if (!IntroLanding.supported(p.serverLevel(), BlockPos.of(t.getLong("Anchor")))) {
      recover(p, t);
      return;
    }
    cleanupActor(p, t);
    IntroDeparture.cleanup(p, t);
    stopSounds(p);
    p.fallDistance = 0;
    p.setDeltaMovement(Vec3.ZERO);
    p.setNoGravity(t.getBoolean("OriginalNoGravity"));
    p.setGameMode(GameType.byId(t.getInt("Mode")));
    if (!skipped && !t.getBoolean("RescueRequested"))
      p.connection.teleport(
          p.getX(),
          p.getY(),
          p.getZ(),
          WakePresentation.FINAL_YAW_DEGREES,
          WakePresentation.FINAL_PITCH_DEGREES);
    stage(p, t, DONE);
    CoastChapter.begin(p, t);
    line(p, t.getBoolean("RescueRequested") ? "rescued" : skipped ? "skipped" : "find_shelter");
  }

  static void recover(ServerPlayer p, CompoundTag t) {
    cleanupActor(p, t);
    IntroDeparture.cleanup(p, t);
    stopSounds(p);
    p.setNoGravity(t.getBoolean("OriginalNoGravity"));
    p.setDeltaMovement(Vec3.ZERO);
    p.fallDistance = 0;
    ResourceLocation id = ResourceLocation.tryParse(t.getString("ReturnDimension"));
    ServerLevel back =
        id == null ? null : p.server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
    if (back == null) back = p.server.overworld();
    p.teleportTo(
        back,
        t.getDouble("ReturnX"),
        t.getDouble("ReturnY"),
        t.getDouble("ReturnZ"),
        t.getFloat("ReturnYaw"),
        t.getFloat("ReturnPitch"));
    p.setGameMode(GameType.byId(t.getInt("Mode")));
    stage(p, t, DONE);
    line(p, "recovered");
  }

  @SubscribeEvent
  public static void tick(ServerTickEvent.Post e) {
    IntroState data = IntroState.get(e.getServer());
    for (ServerPlayer p : e.getServer().getPlayerList().getPlayers()) {
      CompoundTag t = data.players.get(p.getUUID());
      if (t == null) continue;
      int s = t.getInt("Stage");
      if (s == 0) {
        int delay = t.getInt("Delay") - 1;
        t.putInt("Delay", delay);
        if (delay <= 0) {
          t.putInt("Stage", DONE);
          data.setDirty();
          if (data.enabled && StoryMode.enabled(e.getServer())) begin(p);
        }
        continue;
      }
      if (!active(t)) continue;
      if (!p.isAlive()) {
        recover(p, t);
        continue;
      }
      if ((s <= BLACKOUT && !p.level().dimension().equals(sceneDimension(t)))
          || ((s == WAKE || s == LANDING) && !p.level().dimension().equals(Level.OVERWORLD))) {
        recover(p, t);
        continue;
      }
      int age = t.getInt("Age") + 1;
      t.putInt("Age", age);
      BlockPos o = home(t);
      if (age % 10 == 0) cue(p, t);
      if (CampusOpening.active(t)) {
        CampusOpening.tick(p, t);
        continue;
      }
      if (age % CUE_INTERVAL_TICKS == 0) {
        data.setDirty();
        p.displayClientMessage(
            Component.translatable("message.timelocked.intro.objective." + s), true);
      }
      if (s <= BREAKIN) {
        // Keep the private stage safe; movement and looking around otherwise remain free.
        if (CampusRebuild.active(t)
            ? !CampusRebuild.nightContains(o, p.blockPosition())
            : (p.getY() < o.getY() + .5
                || p.getY() > o.getY() + 6
                || p.getX() < o.getX() - 1
                || p.getX() > o.getX() + 23
                || p.getZ() < o.getZ() - 1
                || p.getZ() > o.getZ() + 19))
          p.teleportTo(
              p.serverLevel(),
              o.getX() + 9.5,
              o.getY() + 1,
              o.getZ() + 8.5,
              p.getYRot(),
              p.getXRot());
        if (age % 200 == 0) {
          custom(p, "rain", .3f);
          if (s == HOME) custom(p, "tv", .18f);
        }
      }
      if (s == BEDTIME && p.blockPosition().distSqr(o.offset(19, 1, 12)) < 16) {
        stage(p, t, BREAKIN);
        sound(p, SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, .55f, .8f);
        line(p, "noise");
      }
      if (s == BREAKIN) {
        if (age == 55) {
          IntroScenes.breach(p.serverLevel(), o);
          if (CampusRebuild.active(t))
            for (int y = 2; y <= 3; y++)
              p.serverLevel().setBlock(o.offset(-6, y, 5), Blocks.AIR.defaultBlockState(), 3);
          sound(p, SoundEvents.GLASS_BREAK, .6f, .8f);
        }
        if (age == 90) line(p, "investigate");
        if (age >= 130 && age < 230 && !t.hasUUID("Actor")) {
          var actor = IntroScenes.silhouette(p.serverLevel(), o);
          if (actor != null) t.putUUID("Actor", actor.getUUID());
        }
        if (age == 170 || age == 190) sound(p, SoundEvents.STONE_STEP, .55f, .75f);
        if (age >= 230) {
          stage(p, t, BLACKOUT);
          sound(p, SoundEvents.PLAYER_ATTACK_STRONG, .35f, .65f);
          stopSounds(p);
          custom(p, "transition", .45f);
        }
      }
      if (s == BLACKOUT && age >= BLACKOUT_DURATION_TICKS) {
        beach(p, t);
      }
      if (s == BLACKOUT) {
        p.setDeltaMovement(Vec3.ZERO);
        p.fallDistance = 0;
      }
      if (s == LANDING || s == WAKE) {
        if (!IntroLanding.hold(p, t)) {
          recover(p, t);
          continue;
        }
        if (s == LANDING && age >= LANDING_TIMEOUT_TICKS) {
          recover(p, t);
          continue;
        }
        if (s == WAKE) IntroDeparture.tick(p, t, age);
      }
      if (s == WAKE && age == WakePresentation.BREATH_LINE_TICK) line(p, "breath");
      if (s == WAKE && age == WakePresentation.WHERE_LINE_TICK) line(p, "where");
      if (s == WAKE && age >= WAKE_DURATION_TICKS) finish(p, t, false);
    }
  }

  /** Only the actual sender can acknowledge their own current landing nonce. */
  public static void ready(ServerPlayer p, int token) {
    CompoundTag t = state(p);
    if (t == null
        || t.getInt("Stage") != LANDING
        || token == 0
        || token != t.getInt("LandingToken")
        || !p.level().dimension().equals(Level.OVERWORLD)) return;
    if (!IntroLanding.hold(p, t)) {
      recover(p, t);
      return;
    }
    t.putBoolean("ClientReady", true);
    if (t.getBoolean("SkipRequested")) {
      finish(p, t, true);
      return;
    }
    stage(p, t, WAKE);
    custom(p, "surf", .42f);
    IntroDeparture.spawn(p, t);
  }

  @SubscribeEvent
  public static void logout(PlayerEvent.PlayerLoggedOutEvent e) {
    if (e.getEntity() instanceof ServerPlayer p) {
      CompoundTag t = state(p);
      if (t != null) IntroDeparture.cleanup(p, t);
    }
  }

  @SubscribeEvent
  public static void interact(PlayerInteractEvent.RightClickBlock e) {
    if (!(e.getEntity() instanceof ServerPlayer p)) return;
    CompoundTag t = state(p);
    if (!active(t)) return;
    int stage = t.getInt("Stage");
    e.setCanceled(true);
    e.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
    if (t.getBoolean(CampusNeighborhood.REVISION)
        && e.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND
        && CampusNeighborhood.useDoor(p, t, e.getPos())) return;
    if (CampusOpening.active(t)) {
      if (e.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND
          && p.level().dimension().equals(CAMPUS_DIM)) CampusOpening.interact(p, t, e.getPos());
      return;
    }
    if (stage == HOME
        && p.level().dimension().equals(HOME_DIM)
        && IntroScenes.isTv(home(t), e.getPos())) {
      IntroScenes.tvOff(p.serverLevel(), home(t));
      stopSounds(p);
      custom(p, "rain", .3f);
      stage(p, t, BEDTIME);
      line(p, "bed");
    }
  }

  @SubscribeEvent
  public static void attack(AttackEntityEvent e) {
    if (e.getTarget().getTags().contains("timelocked_campus_actor")
        || e.getTarget().getTags().contains("timelocked_intro_boat")
        || e.getTarget().getTags().contains("timelocked_intro_pantheris")
        || e.getEntity() instanceof ServerPlayer p && active(state(p))) e.setCanceled(true);
  }

  @SubscribeEvent
  public static void entityUse(PlayerInteractEvent.EntityInteract e) {
    if (e.getEntity() instanceof ServerPlayer campusPlayer
        && CampusOpening.active(state(campusPlayer))
        && e.getTarget().getTags().contains("timelocked_campus_actor")) {
      if (e.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND)
        CampusOpening.entityInteract(campusPlayer, state(campusPlayer), e.getTarget());
      e.setCanceled(true);
      e.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
      return;
    }

    if (e.getTarget().getTags().contains("timelocked_campus_actor")
        || e.getTarget().getTags().contains("timelocked_intro_boat")
        || e.getTarget().getTags().contains("timelocked_intro_pantheris")
        || e.getEntity() instanceof ServerPlayer p && active(state(p))) e.setCanceled(true);
  }

  @SubscribeEvent
  public static void entityUseSpecific(PlayerInteractEvent.EntityInteractSpecific e) {
    if (e.getEntity() instanceof ServerPlayer campusPlayer
        && CampusOpening.active(state(campusPlayer))
        && e.getTarget().getTags().contains("timelocked_campus_actor")) {
      if (e.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND)
        CampusOpening.entityInteract(campusPlayer, state(campusPlayer), e.getTarget());
      e.setCanceled(true);
      e.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
      return;
    }

    if (e.getTarget().getTags().contains("timelocked_campus_actor")
        || e.getTarget().getTags().contains("timelocked_intro_boat")
        || e.getTarget().getTags().contains("timelocked_intro_pantheris")
        || e.getEntity() instanceof ServerPlayer p && active(state(p))) e.setCanceled(true);
  }

  @SubscribeEvent
  public static void itemUse(PlayerInteractEvent.RightClickItem e) {
    if (e.getEntity() instanceof ServerPlayer p && active(state(p))) e.setCanceled(true);
  }

  @SubscribeEvent
  public static void damage(LivingIncomingDamageEvent e) {
    if (e.getEntity() instanceof ServerPlayer p && active(state(p))) e.setCanceled(true);
  }

  private IntroSequence() {}
}
