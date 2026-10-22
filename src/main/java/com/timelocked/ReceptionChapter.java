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

/** Chapter two: a real receptionist, visitor registration, and a return conversation. */
@EventBusSubscriber(modid = "timelocked")
public final class ReceptionChapter {
  static final String SAVE_KEY = "ReceptionChapter";
  static final String NPC_ROLE = "timelocked_receptionist";
  static final String NPC_SITE = "timelocked_reception_site";
  // Persisted quest milestones. Do not renumber existing saves.
  static final int NOT_STARTED = 0, REGISTER = 1, RETURN = 2, COMPLETE = 3;
  static final int POLL_TICKS = 20;
  private static final int CHAT_GAP_TICKS = 3 * POLL_TICKS;
  private static final double BLOCK_CENTER = .5;
  private static final double REACH_SQUARED_BLOCKS = 6 * 6;
  // Authored drawing-local positions. The NPC stands in front of the three-deep counter,
  // not behind it where a survival player's normal entity reach could not reach them.
  static final BlockPos DESK = new BlockPos(64, 2, 329);
  static final BlockPos TERMINAL = new BlockPos(67, 3, 327);
  private static final int[] TERMINAL_X = {61, 64, 67, 70};

  static boolean receptionist(SarcScientist npc) {
    return npc.getPersistentData().getBoolean(NPC_ROLE);
  }

  static int step(CompoundTag intro) {
    int value = intro.getCompound(SAVE_KEY).getInt("Step");
    return value < NOT_STARTED || value > COMPLETE ? COMPLETE : value;
  }

  static boolean ready(ServerPlayer player, CompoundTag intro) {
    return CoastChapter.eligible(player, intro)
        && intro.contains(CoastChapter.SAVE_KEY)
        && CoastChapter.step(CoastChapter.chapter(intro)) == CoastChapter.COMPLETE
        && BlueprintRooms.drawing(player.serverLevel(), BlockPos.of(intro.getLong("Site")))
        && CoastChapter.welcomeFinished(
            FacilityState.get(player.server).progress.get(intro.getLong("Site")));
  }

  static Component objective(CompoundTag intro) {
    BlockPos site = BlockPos.of(intro.getLong("Site"));
    int stage = step(intro);
    if (stage == COMPLETE) return StaffChapter.objective(intro);
    BlockPos target = site.offset(stage == REGISTER ? TERMINAL : DESK);
    return Component.translatable(
        "message.timelocked.reception.objective." + stage,
        target.getX(),
        target.getY(),
        target.getZ());
  }

  static void say(ServerPlayer player, CompoundTag chapter, String key) {
    player.sendSystemMessage(Component.translatable("message.timelocked.reception." + key));
    chapter.putInt("Cooldown", CHAT_GAP_TICKS);
    CoastChapter.dirty(player);
  }

  /** Never loads a chunk or rebuilds a player's desk. A saved UUID prevents duplicate respawns. */
  static SarcScientist ensure(ServerLevel level, BlockPos site) {
    if (!StoryMode.enabled(level.getServer())
        || level.dimension() != Level.OVERWORLD
        || !BlueprintRooms.drawing(level, site)) return null;
    SiteProgress progress = FacilityState.get(level.getServer()).progress.get(site.asLong());
    if (progress.receptionist != null) {
      var saved = level.getEntity(progress.receptionist);
      return saved instanceof SarcScientist npc
              && receptionist(npc)
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
            ("timelocked:receptionist:" + level.getSeed() + ":" + site.asLong())
                .getBytes(StandardCharsets.UTF_8));
    if (level.getEntity(id) != null) return null;
    npc.setUUID(id);
    npc.setKind(SarcScientist.RESEARCHER);
    npc.setCustomName(Component.translatable("entity.timelocked.receptionist"));
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
    progress.receptionist = id;
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
    if (progress == null || !npc.getUUID().equals(progress.receptionist))
      return InteractionResult.CONSUME;
    if (!ready(player, intro)) {
      player.displayClientMessage(
          Component.translatable("message.timelocked.reception.wait"), true);
      return InteractionResult.CONSUME;
    }
    CompoundTag chapter = intro.getCompound(SAVE_KEY);
    if (!intro.contains(SAVE_KEY)) intro.put(SAVE_KEY, chapter);
    if (chapter.getInt("Cooldown") > 0) {
      player.displayClientMessage(objective(intro), true);
      return InteractionResult.CONSUME;
    }
    if (step(intro) == NOT_STARTED) {
      chapter.putInt("Step", REGISTER);
      say(player, chapter, "start");
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
        || step(intro) != REGISTER
        || player.distanceToSqr(position.getCenter()) > REACH_SQUARED_BLOCKS
        || !player.level().getBlockState(position).is(FacilityDetails.WORKSTATION.get()))
      return false;
    BlockPos local = position.subtract(BlockPos.of(intro.getLong("Site")));
    if (local.getY() != TERMINAL.getY() || local.getZ() != TERMINAL.getZ()) return false;
    boolean authored = false;
    for (int x : TERMINAL_X) authored |= local.getX() == x;
    if (!authored) return false;
    CompoundTag chapter = intro.getCompound(SAVE_KEY);
    if (chapter.getInt("Cooldown") > 0) return true;
    chapter.putInt("Step", RETURN);
    say(player, chapter, "registered");
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
    if (HubArrival.inside(player, site)) ensure(player.serverLevel(), site);
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
            Commands.literal("sarcreception")
                .executes(
                    context -> {
                      var player = context.getSource().getPlayerOrException();
                      var intro = IntroSequence.state(player);
                      if (!CoastChapter.eligible(player, intro)) return 0;
                      BlockPos site = BlockPos.of(intro.getLong("Site"));
                      if (!FacilitySpawnProtection.contains(
                          player.serverLevel(), site, player.blockPosition())) return 0;
                      SarcScientist npc = ensure(player.serverLevel(), site);
                      player.sendSystemMessage(
                          Component.translatable(
                              npc == null
                                  ? "message.timelocked.reception.placement_wait"
                                  : "message.timelocked.reception.available"));
                      player.sendSystemMessage(objective(intro));
                      return npc == null ? 0 : 1;
                    }));
  }

  private ReceptionChapter() {}
}
