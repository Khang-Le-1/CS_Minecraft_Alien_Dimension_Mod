package com.timelocked;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Chapters 4–6. Deliberate read-only evidence work, founder interview and a simulated field trial.
 */
@EventBusSubscriber(modid = "timelocked")
public final class ResearchStory {
  static final String SAVE_KEY = "ResearchStory";
  static final String NPC_ROLE = "timelocked_founder";
  static final String NPC_SITE = "timelocked_founder_site";
  static final int POLL_TICKS = 20, CHAT_GAP_TICKS = 60;
  private static final double REACH_SQUARED = 36, CHECKPOINT_REACH_SQUARED = 4;
  static final BlockPos DESK = StaffChapter.DESK;
  static final BlockPos FOUNDER_DESK = new BlockPos(109, 2, 240);
  static final BlockPos INTAKE = StaffChapter.TERMINAL;
  static final BlockPos SIGNAL = new BlockPos(74, 2, 148);
  static final BlockPos ARCHIVE = new BlockPos(74, 2, 136);
  static final BlockPos RECEPTION = ReceptionChapter.TERMINAL;
  static final BlockPos ATRIUM = new BlockPos(80, 2, 155);
  static final BlockPos CONNECTOR = new BlockPos(80, 2, 180);
  static final int FOUNDER_START = 12, TRIAL_START = 24;

  enum Kind {
    OFFICER,
    FOUNDER,
    READ,
    WALK
  }

  record Beat(int chapter, Kind kind, BlockPos target, int id) {}

  static final List<Beat> BEATS =
      List.of(
          new Beat(4, Kind.OFFICER, DESK, 0),
          new Beat(4, Kind.READ, INTAKE, 1),
          new Beat(4, Kind.READ, INTAKE, 2),
          new Beat(4, Kind.READ, SIGNAL, 3),
          new Beat(4, Kind.READ, SIGNAL, 4),
          new Beat(4, Kind.READ, ARCHIVE, 5),
          new Beat(4, Kind.READ, ARCHIVE, 6),
          new Beat(4, Kind.OFFICER, DESK, 7),
          new Beat(4, Kind.READ, SIGNAL, 8),
          new Beat(4, Kind.READ, RECEPTION, 9),
          new Beat(4, Kind.OFFICER, DESK, 10),
          new Beat(4, Kind.OFFICER, DESK, 11),
          new Beat(5, Kind.FOUNDER, FOUNDER_DESK, 12),
          new Beat(5, Kind.FOUNDER, FOUNDER_DESK, 13),
          new Beat(5, Kind.FOUNDER, FOUNDER_DESK, 14),
          new Beat(5, Kind.FOUNDER, FOUNDER_DESK, 15),
          new Beat(5, Kind.READ, ARCHIVE, 16),
          new Beat(5, Kind.READ, ARCHIVE, 17),
          new Beat(5, Kind.READ, SIGNAL, 18),
          new Beat(5, Kind.FOUNDER, FOUNDER_DESK, 19),
          new Beat(5, Kind.FOUNDER, FOUNDER_DESK, 20),
          new Beat(5, Kind.FOUNDER, FOUNDER_DESK, 21),
          new Beat(5, Kind.FOUNDER, FOUNDER_DESK, 22),
          new Beat(5, Kind.FOUNDER, FOUNDER_DESK, 23),
          new Beat(6, Kind.OFFICER, DESK, 24),
          new Beat(6, Kind.READ, INTAKE, 25),
          new Beat(6, Kind.READ, INTAKE, 26),
          new Beat(6, Kind.READ, SIGNAL, 27),
          new Beat(6, Kind.READ, SIGNAL, 28),
          new Beat(6, Kind.WALK, ATRIUM, 29),
          new Beat(6, Kind.READ, ARCHIVE, 30),
          new Beat(6, Kind.WALK, CONNECTOR, 31),
          new Beat(6, Kind.READ, RECEPTION, 32),
          new Beat(6, Kind.OFFICER, DESK, 33),
          new Beat(6, Kind.READ, SIGNAL, 34),
          new Beat(6, Kind.FOUNDER, FOUNDER_DESK, 35),
          new Beat(6, Kind.OFFICER, DESK, 36),
          new Beat(6, Kind.OFFICER, DESK, 37));
  static final int COMPLETE = BEATS.size();

  static boolean founder(SarcScientist npc) {
    return npc.getPersistentData().getBoolean(NPC_ROLE);
  }

  /** Invalid stages halt instead of wrapping, replaying rewards or inventing completion. */
  static int stage(CompoundTag intro) {
    var chapter = intro.getCompound(SAVE_KEY);
    if (intro.contains(SAVE_KEY)
        && (!intro.contains(SAVE_KEY, Tag.TAG_COMPOUND) || !chapter.contains("Stage", Tag.TAG_INT)))
      return -1;
    int value = chapter.getInt("Stage");
    return value >= 0 && value <= COMPLETE ? value : -1;
  }

  static Beat current(CompoundTag intro) {
    int value = stage(intro);
    return value >= 0 && value < COMPLETE ? BEATS.get(value) : null;
  }

  static boolean ready(ServerPlayer player, CompoundTag intro) {
    return StaffChapter.cleared(player, intro)
        && intro.getCompound(StaffChapter.SAVE_KEY).getInt("Step") == StaffChapter.COMPLETE;
  }

  static Component objective(CompoundTag intro) {
    var beat = current(intro);
    if (beat == null)
      return Component.translatable(
          "message.timelocked.research." + (stage(intro) == COMPLETE ? "complete" : "invalid"));
    BlockPos at = BlockPos.of(intro.getLong("Site")).offset(beat.target());
    return Component.translatable(
        "message.timelocked.research.objective." + beat.id(), at.getX(), at.getY(), at.getZ());
  }

  static StoryObjective.View view(CompoundTag intro) {
    // Read-only and fail-closed: corrupted Chapter 3 data must not invent a new assignment.
    var staff = intro.getCompound(StaffChapter.SAVE_KEY);
    if (staff.getInt("Step") != StaffChapter.COMPLETE || !staff.getBoolean(StaffChapter.CLEARANCE))
      return null;
    var beat = current(intro);
    return beat == null
        ? null
        : new StoryObjective.View(
            "hud.timelocked.objective.research." + beat.id(),
            Level.OVERWORLD.location(),
            BlockPos.of(intro.getLong("Site")).offset(beat.target()),
            true);
  }

  private static void advance(ServerPlayer player, CompoundTag intro, Beat beat) {
    CompoundTag chapter = intro.getCompound(SAVE_KEY);
    if (chapter.getInt("Cooldown") > 0 || current(intro) != beat) return;
    if (!intro.contains(SAVE_KEY)) intro.put(SAVE_KEY, chapter);
    player.sendSystemMessage(
        Component.translatable("message.timelocked.research.line." + beat.id()));
    chapter.putInt("Stage", beat.id() + 1);
    chapter.putInt("Cooldown", CHAT_GAP_TICKS);
    CoastChapter.dirty(player);
    player.displayClientMessage(objective(intro), true);
  }

  /** Existing authored positions only. Never edits blocks, loads chunks or replaces a saved NPC. */
  static SarcScientist ensure(ServerLevel level, BlockPos site) {
    if (!StoryMode.enabled(level.getServer())
        || level.dimension() != Level.OVERWORLD
        || !BlueprintRooms.drawing(level, site)) return null;
    var progress = FacilityState.get(level.getServer()).progress.get(site.asLong());
    if (progress == null) return null;
    if (progress.founder != null) {
      var saved = level.getEntity(progress.founder);
      return saved instanceof SarcScientist npc
              && founder(npc)
              && npc.getPersistentData().getLong(NPC_SITE) == site.asLong()
          ? npc
          : null;
    }
    BlockPos at = site.offset(FOUNDER_DESK);
    if (!level.hasChunkAt(at)
        || !level.getBlockState(at).isAir()
        || !level.getBlockState(at.above()).isAir()
        || !level.getBlockState(at.below()).isFaceSturdy(level, at.below(), Direction.UP))
      return null;
    SarcScientist npc = SarcContent.SCIENTIST.get().create(level);
    if (npc == null) return null;
    UUID id =
        UUID.nameUUIDFromBytes(
            ("timelocked:founder:" + level.getSeed() + ":" + site.asLong())
                .getBytes(StandardCharsets.UTF_8));
    if (level.getEntity(id) != null) return null;
    npc.setUUID(id);
    npc.setKind(SarcScientist.RESEARCHER);
    npc.setCustomName(Component.translatable("entity.timelocked.founder"));
    npc.setCustomNameVisible(true);
    npc.setPersistenceRequired();
    npc.setNoAi(true);
    npc.setInvulnerable(true);
    npc.getPersistentData().putBoolean(NPC_ROLE, true);
    npc.getPersistentData().putLong(NPC_SITE, site.asLong());
    npc.moveTo(at.getX() + .5, at.getY(), at.getZ() + .5, 0, 0);
    if (!level.noCollision(npc)
        || !level.getEntities(npc, npc.getBoundingBox()).isEmpty()
        || FacilitySpawnProtection.blocked(level, npc, MobSpawnType.STRUCTURE)
        || !level.addFreshEntity(npc)) return null;
    progress.founder = id;
    FacilityState.get(level.getServer()).setDirty();
    return npc;
  }

  static InteractionResult interact(SarcScientist npc, ServerPlayer player, InteractionHand hand) {
    if (hand != InteractionHand.MAIN_HAND) return InteractionResult.CONSUME;
    var intro = IntroSequence.state(player);
    if (!ready(player, intro) || npc.distanceToSqr(player) > REACH_SQUARED)
      return InteractionResult.CONSUME;
    var progress = FacilityState.get(player.server).progress.get(intro.getLong("Site"));
    boolean isFounder = founder(npc);
    String siteKey = isFounder ? NPC_SITE : StaffChapter.NPC_SITE;
    UUID expected =
        progress == null ? null : isFounder ? progress.founder : progress.briefingOfficer;
    if (!npc.getUUID().equals(expected)
        || npc.getPersistentData().getLong(siteKey) != intro.getLong("Site"))
      return InteractionResult.CONSUME;
    var beat = current(intro);
    if (beat != null && beat.kind() == (isFounder ? Kind.FOUNDER : Kind.OFFICER))
      advance(player, intro, beat);
    else player.displayClientMessage(objective(intro), true);
    return InteractionResult.SUCCESS;
  }

  static boolean terminal(ServerPlayer player, BlockPos pos) {
    var intro = IntroSequence.state(player);
    if (!ready(player, intro) || player.isSecondaryUseActive()) return false;
    var beat = current(intro);
    if (beat == null
        || beat.kind() != Kind.READ
        || !pos.equals(BlockPos.of(intro.getLong("Site")).offset(beat.target()))
        || player.distanceToSqr(pos.getCenter()) > REACH_SQUARED
        || !player.level().getBlockState(pos).is(FacilityDetails.WORKSTATION.get())) return false;
    advance(player, intro, beat);
    return true;
  }

  @SubscribeEvent
  public static void terminalClick(PlayerInteractEvent.RightClickBlock event) {
    if (event.getHand() == InteractionHand.MAIN_HAND
        && event.getEntity() instanceof ServerPlayer p
        && terminal(p, event.getPos())) {
      event.setCanceled(true);
      event.setCancellationResult(InteractionResult.SUCCESS);
    }
  }

  static void tickPlayer(ServerPlayer player) {
    var intro = IntroSequence.state(player);
    if (!ready(player, intro)) return;
    BlockPos site = BlockPos.of(intro.getLong("Site"));
    if (!FacilitySpawnProtection.contains(player.serverLevel(), site, player.blockPosition()))
      return;
    if (stage(intro) >= FOUNDER_START && stage(intro) <= COMPLETE)
      ensure(player.serverLevel(), site);
    if (intro.contains(SAVE_KEY)) {
      var chapter = intro.getCompound(SAVE_KEY);
      int before = chapter.getInt("Cooldown");
      int after = Math.max(0, Math.clamp(before, 0, CHAT_GAP_TICKS) - POLL_TICKS);
      if (before != after) {
        chapter.putInt("Cooldown", after);
        CoastChapter.dirty(player);
      }
    }
    var beat = current(intro);
    if (beat != null
        && beat.kind() == Kind.WALK
        && player.onGround()
        && player.distanceToSqr(site.offset(beat.target()).getBottomCenter())
            <= CHECKPOINT_REACH_SQUARED) advance(player, intro, beat);
  }

  @SubscribeEvent
  public static void tick(ServerTickEvent.Post event) {
    if (event.getServer().overworld().getGameTime() % POLL_TICKS != 0) return;
    for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) tickPlayer(player);
  }

  @SubscribeEvent
  public static void commands(RegisterCommandsEvent event) {
    event
        .getDispatcher()
        .register(
            Commands.literal("sarcresearch")
                .executes(
                    context -> {
                      var p = context.getSource().getPlayerOrException();
                      var intro = IntroSequence.state(p);
                      if (!ready(p, intro)) {
                        p.sendSystemMessage(
                            Component.translatable("message.timelocked.research.wait"));
                        return 0;
                      }
                      p.sendSystemMessage(objective(intro));
                      if (stage(intro) >= FOUNDER_START && stage(intro) <= COMPLETE) {
                        var npc = ensure(p.serverLevel(), BlockPos.of(intro.getLong("Site")));
                        if (npc == null)
                          p.sendSystemMessage(
                              Component.translatable("message.timelocked.research.placement_wait"));
                      }
                      return 1;
                    }));
  }

  private ResearchStory() {}
}
