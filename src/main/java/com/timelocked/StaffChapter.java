package com.timelocked;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
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

/** Chapter three: witness interview, limited lab access and a read-only orientation assignment. */
@EventBusSubscriber(modid = "timelocked")
public final class StaffChapter {
  static final String SAVE_KEY = "StaffChapter";
  static final String NPC_ROLE = "timelocked_briefing_officer";
  static final String NPC_SITE = "timelocked_briefing_site";
  // Persisted quest milestones. Do not renumber existing saves.
  static final int NOT_STARTED = 0, REPORT = 1, ASSIGNED = 2, RETURN = 3, COMPLETE = 4;
  static final int POLL_TICKS = 20;
  private static final int CHAT_GAP_TICKS = 3 * POLL_TICKS;
  private static final double BLOCK_CENTER = .5;
  private static final double REACH_SQUARED_BLOCKS = 6 * 6;
  // Authored drawing-local positions: existing operations/briefing zone and lab workstation.
  static final BlockPos DESK = new BlockPos(105, 2, 240);
  static final BlockPos TERMINAL = new BlockPos(74, 2, 160);
  static final int LAB_DOOR_Z = 170, LAB_DOOR_LEFT_X = 79, LAB_DOOR_RIGHT_X = 80;
  static final int LAB_DOOR_Y = 2;
  static final String CLEARANCE = "ResearchOrientationClearance";

  static boolean officer(SarcScientist npc) {
    return npc.getPersistentData().getBoolean(NPC_ROLE);
  }

  static int step(CompoundTag intro) {
    int value = intro.getCompound(SAVE_KEY).getInt("Step");
    return value < NOT_STARTED || value > COMPLETE ? COMPLETE : value;
  }

  static boolean ready(ServerPlayer player, CompoundTag intro) {
    return ReceptionChapter.ready(player, intro)
        && intro.contains(ReceptionChapter.SAVE_KEY)
        && ReceptionChapter.step(intro) == ReceptionChapter.COMPLETE;
  }

  static boolean cleared(ServerPlayer player, CompoundTag intro) {
    return ready(player, intro)
        && intro.getCompound(SAVE_KEY).getBoolean(CLEARANCE)
        && step(intro) >= ASSIGNED;
  }

  /** Limited manual-entry authorization only. Never blocks egress or changes other locks. */
  static boolean deniedEntry(ServerPlayer player, BlockPos lower) {
    CompoundTag intro = IntroSequence.state(player);
    if (!CoastChapter.eligible(player, intro)) return false;
    BlockPos site = BlockPos.of(intro.getLong("Site"));
    if (!BlueprintRooms.drawing(player.serverLevel(), site)) return false;
    BlockPos local = lower.subtract(site);
    if (local.getY() != LAB_DOOR_Y
        || local.getZ() != LAB_DOOR_Z
        || (local.getX() != LAB_DOOR_LEFT_X && local.getX() != LAB_DOOR_RIGHT_X)
        || player.getZ() < site.getZ() + LAB_DOOR_Z) return false;
    return !cleared(player, intro);
  }

  static Component objective(CompoundTag intro) {
    if (step(intro) == COMPLETE) return ResearchStory.objective(intro);
    BlockPos site = BlockPos.of(intro.getLong("Site"));
    int stage = step(intro);
    BlockPos target = site.offset(stage == ASSIGNED ? TERMINAL : DESK);
    return Component.translatable(
        "message.timelocked.staff.objective." + stage, target.getX(), target.getY(), target.getZ());
  }

  static void say(ServerPlayer player, CompoundTag chapter, String key) {
    player.sendSystemMessage(Component.translatable("message.timelocked.staff." + key));
    chapter.putInt("Cooldown", CHAT_GAP_TICKS);
    CoastChapter.dirty(player);
  }

  /** Never loads a chunk or rebuilds a player's desk. A saved UUID prevents duplicate respawns. */
  static SarcScientist ensure(ServerLevel level, BlockPos site) {
    if (!StoryMode.enabled(level.getServer())
        || level.dimension() != Level.OVERWORLD
        || !BlueprintRooms.drawing(level, site)) return null;
    SiteProgress progress = FacilityState.get(level.getServer()).progress.get(site.asLong());
    if (progress.briefingOfficer != null) {
      var saved = level.getEntity(progress.briefingOfficer);
      return saved instanceof SarcScientist npc
              && officer(npc)
              && npc.getPersistentData().getLong(NPC_SITE) == site.asLong()
          ? npc
          : null;
    }
    BlockPos home = site.offset(DESK);
    if (!level.hasChunkAt(home)
        || !level.getBlockState(home).isAir()
        || !level.getBlockState(home.above()).isAir()
        || !level
            .getBlockState(home.below())
            .isFaceSturdy(level, home.below(), net.minecraft.core.Direction.UP)) return null;
    SarcScientist npc = SarcContent.SCIENTIST.get().create(level);
    if (npc == null) return null;
    UUID id =
        UUID.nameUUIDFromBytes(
            ("timelocked:briefing_officer:" + level.getSeed() + ":" + site.asLong())
                .getBytes(StandardCharsets.UTF_8));
    if (level.getEntity(id) != null) return null;
    npc.setUUID(id);
    npc.setKind(SarcScientist.RESEARCHER);
    npc.setCustomName(Component.translatable("entity.timelocked.briefing_officer"));
    npc.setCustomNameVisible(true);
    npc.setPersistenceRequired();
    npc.setNoAi(true);
    npc.setInvulnerable(true);
    npc.getPersistentData().putBoolean(NPC_ROLE, true);
    npc.getPersistentData().putLong(NPC_SITE, site.asLong());
    npc.moveTo(home.getX() + BLOCK_CENTER, home.getY(), home.getZ() + BLOCK_CENTER, 0, 0);
    if (!level.noCollision(npc) || !level.getEntities(npc, npc.getBoundingBox()).isEmpty())
      return null;
    // Deliberately authored staff are allowed by the facility spawn guard. No random spawn route.
    if (FacilitySpawnProtection.blocked(level, npc, MobSpawnType.STRUCTURE)
        || !level.addFreshEntity(npc)) return null;
    progress.briefingOfficer = id;
    FacilityState.get(level.getServer()).setDirty();
    return npc;
  }

  static InteractionResult interact(SarcScientist npc, ServerPlayer player, InteractionHand hand) {
    if (hand != InteractionHand.MAIN_HAND) return InteractionResult.CONSUME;
    CompoundTag intro = IntroSequence.state(player);
    if (!CoastChapter.eligible(player, intro)
        || npc.distanceToSqr(player) > REACH_SQUARED_BLOCKS
        || npc.getPersistentData().getLong(NPC_SITE) != intro.getLong("Site"))
      return InteractionResult.CONSUME;
    var progress = FacilityState.get(player.server).progress.get(intro.getLong("Site"));
    if (progress == null || !npc.getUUID().equals(progress.briefingOfficer))
      return InteractionResult.CONSUME;
    if (!ready(player, intro)) {
      player.displayClientMessage(Component.translatable("message.timelocked.staff.wait"), true);
      return InteractionResult.CONSUME;
    }
    if (step(intro) == COMPLETE) return ResearchStory.interact(npc, player, hand);
    CompoundTag chapter = intro.getCompound(SAVE_KEY);
    if (!intro.contains(SAVE_KEY)) intro.put(SAVE_KEY, chapter);
    if (chapter.getInt("Cooldown") > 0) {
      player.displayClientMessage(objective(intro), true);
      return InteractionResult.CONSUME;
    }
    if (step(intro) == NOT_STARTED) {
      chapter.putInt("Step", REPORT);
      say(player, chapter, "start");
    } else if (step(intro) == REPORT) {
      player.sendSystemMessage(Component.translatable("message.timelocked.staff.witness"));
      chapter.putInt("Step", ASSIGNED);
      chapter.putBoolean(CLEARANCE, true);
      say(player, chapter, "assignment");
    } else if (step(intro) == RETURN) {
      chapter.putInt("Step", COMPLETE);
      say(player, chapter, "complete");
    } else player.displayClientMessage(objective(intro), true);
    return InteractionResult.SUCCESS;
  }

  static boolean terminal(ServerPlayer player, BlockPos position) {
    CompoundTag intro = IntroSequence.state(player);
    if (!ready(player, intro)
        || player.isSecondaryUseActive()
        || step(intro) != ASSIGNED
        || player.distanceToSqr(position.getCenter()) > REACH_SQUARED_BLOCKS
        || !player.level().getBlockState(position).is(FacilityDetails.WORKSTATION.get()))
      return false;
    BlockPos local = position.subtract(BlockPos.of(intro.getLong("Site")));
    if (!local.equals(TERMINAL) || !cleared(player, intro)) return false;
    CompoundTag chapter = intro.getCompound(SAVE_KEY);
    if (chapter.getInt("Cooldown") > 0) return true;
    chapter.putInt("Step", RETURN);
    say(player, chapter, "orientation");
    return true;
  }

  @SubscribeEvent
  public static void terminalClick(PlayerInteractEvent.RightClickBlock event) {
    if (event.getHand() == InteractionHand.MAIN_HAND
        && event.getEntity() instanceof ServerPlayer player
        && terminal(player, event.getPos())) {
      event.setCanceled(true);
      event.setCancellationResult(InteractionResult.SUCCESS);
    }
  }

  static void tickPlayer(ServerPlayer player) {
    CompoundTag intro = IntroSequence.state(player);
    if (!CoastChapter.eligible(player, intro)) return;
    BlockPos site = BlockPos.of(intro.getLong("Site"));
    if (!FacilitySpawnProtection.contains(player.serverLevel(), site, player.blockPosition()))
      return;
    if (ready(player, intro) && HubArrival.inside(player, site)) ensure(player.serverLevel(), site);
    if (!intro.contains(SAVE_KEY)) return;
    CompoundTag chapter = intro.getCompound(SAVE_KEY);
    int before = chapter.getInt("Cooldown");
    int after = Math.max(0, Math.clamp(before, 0, CHAT_GAP_TICKS) - POLL_TICKS);
    if (before != after) {
      chapter.putInt("Cooldown", after);
      CoastChapter.dirty(player);
    }
    if (ready(player, intro) && step(intro) > NOT_STARTED && step(intro) < COMPLETE)
      player.displayClientMessage(objective(intro), true);
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
            Commands.literal("sarcbriefing")
                .executes(
                    context -> {
                      var player = context.getSource().getPlayerOrException();
                      var intro = IntroSequence.state(player);
                      if (!ready(player, intro)) {
                        player.sendSystemMessage(
                            Component.translatable("message.timelocked.staff.wait"));
                        return 0;
                      }
                      BlockPos site = BlockPos.of(intro.getLong("Site"));
                      if (!FacilitySpawnProtection.contains(
                          player.serverLevel(), site, player.blockPosition())) return 0;
                      SarcScientist npc = ensure(player.serverLevel(), site);
                      player.sendSystemMessage(
                          Component.translatable(
                              npc == null
                                  ? "message.timelocked.staff.placement_wait"
                                  : "message.timelocked.staff.available"));
                      player.sendSystemMessage(objective(intro));
                      return npc == null ? 0 : 1;
                    }));
  }

  private StaffChapter() {}
}
