package com.timelocked;

import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Chapter one: a free-roaming journey from the Earth beach to SARC.
 *
 * <p>Only per-player progress/chat is changed here. No terrain, inventory, camera, clearance or
 * facility power is changed. Missing signs and bypassed landmarks must never strand a player.
 */
@EventBusSubscriber(modid = "timelocked")
public final class CoastChapter {
  static final String SAVE_KEY = "CoastChapter";
  // These numbers are persisted milestones, not timers. Do not renumber them in a later release.
  static final int SHORE = 1,
      PARKING = 2,
      BOARDWALK = 3,
      ROAD = 4,
      WAYFINDING = 5,
      ENTRANCE = 6,
      HUB = 7,
      COMPLETE = 8;
  static final int TICK_INTERVAL = 20;
  private static final int TICKS_PER_SECOND = 20;
  private static final int CHAT_GAP_SECONDS = 7;
  static final int MESSAGE_GAP_TICKS = CHAT_GAP_SECONDS * TICKS_PER_SECOND;
  // Reserve a full extra sample: a sign click just before a tick must not shorten the gap.
  static final int COOLDOWN_BUDGET_TICKS = MESSAGE_GAP_TICKS + TICK_INTERVAL;
  private static final int ROAD_HALF_WIDTH_BLOCKS = 12;
  private static final int ROAD_SIGN_Z_BLOCKS = 688;
  private static final int ROAD_START_Z_BLOCKS = 256;
  private static final int ROAD_END_Z_BLOCKS = 2188;
  private static final int SURFACE_FLOOR_OFFSET_BLOCKS = 28;
  private static final int ENTRANCE_HEIGHT_TOLERANCE_BLOCKS = 6;
  private static final double PANEL_REACH_BLOCKS_SQUARED = 6 * 6;

  // Authored layout offsets (blocks), shared coordinate frame with IntroCoast and ApproachRoad.
  // Bounds include a small walking margin; they never edit or regenerate the route.
  private static final int FORECOURT_WIDTH_BLOCKS = 130;
  private static final int FORECOURT_NEAR_Z_BLOCKS = 188;
  private static final int ROAD_CENTER_X_BLOCKS = 64;
  private static final int WAYFINDING_MARGIN_BLOCKS = 24;
  private static final int PARKING_MIN_X_BLOCKS = 250, PARKING_MAX_X_BLOCKS = 276;
  private static final int PARKING_MIN_Z_BLOCKS = -42, PARKING_WALKWAY_Z_BLOCKS = -8;
  private static final int BOARDWALK_MIN_X_BLOCKS = 3, BOARDWALK_HALF_WIDTH_BLOCKS = 6;
  private static final int BOARDWALK_MAX_X_BLOCKS = 278, SHORE_MIN_X_BLOCKS = 248;
  private static final int SHORE_MAX_X_BLOCKS = 340, SHORE_HALF_LENGTH_BLOCKS = 72;
  private static final int SHORE_HEIGHT_TOLERANCE_BLOCKS = 8;
  private static final int RESTRICTION_SIGN_X_BLOCKS = -3, RESTRICTION_SIGN_Z_BLOCKS = -5;
  private static final int DIRECTION_SIGN_X_BLOCKS = -6, DIRECTION_SIGN_Z_BLOCKS = 3;
  private static final int WALKWAY_TARGET_X_BLOCKS = 273, WALKWAY_TARGET_Z_BLOCKS = -6;
  private static final int BOARDWALK_TARGET_X_BLOCKS = 200, FORECOURT_TARGET_Z_BLOCKS = 225;

  static boolean eligible(ServerPlayer player, CompoundTag intro) {
    return StoryMode.enabled(player.server)
        && intro != null
        && intro.getInt("Stage") == IntroSequence.DONE
        && intro.getBoolean("AtBeach")
        && intro.contains("Site")
        && player.level().dimension().equals(Level.OVERWORLD)
        && player.isAlive()
        && !player.isSpectator();
  }

  static CompoundTag chapter(CompoundTag intro) {
    return intro.getCompound(SAVE_KEY);
  }

  static int step(CompoundTag chapter) {
    // A corrupt/out-of-range stage is treated as finished, never as a reason to replay chat.
    int value = chapter.getInt("Step");
    return value < 0 || value > COMPLETE ? COMPLETE : value;
  }

  static void dirty(ServerPlayer player) {
    IntroState.get(player.server).setDirty();
  }

  /** Called after a supported landing, never during the locked camera. */
  static void begin(ServerPlayer player, CompoundTag intro) {
    if (!eligible(player, intro) || intro.contains(SAVE_KEY)) return;
    CompoundTag chapter = new CompoundTag();
    chapter.putInt("Step", 0);
    // Leave space after the intro's final chat line rather than sending a second message at once.
    chapter.putInt("Cooldown", COOLDOWN_BUDGET_TICKS);
    intro.put(SAVE_KEY, chapter);
    dirty(player);
  }

  /** Integer route landmarks are in blocks relative to the generated surface facility origin. */
  static int reached(ServerPlayer player, BlockPos site) {
    if (HubArrival.inside(player, site)) return HUB;
    BlockPos surface = IntroCoast.surfaceOrigin(site);
    double x = player.getX() - surface.getX(), z = player.getZ() - surface.getZ();
    double height = player.getY() - (surface.getY() + SURFACE_FLOOR_OFFSET_BLOCKS + 1);
    if (x >= 0
        && x <= FORECOURT_WIDTH_BLOCKS
        && z >= FORECOURT_NEAR_Z_BLOCKS
        && z <= ROAD_START_Z_BLOCKS
        && Math.abs(height) <= ENTRANCE_HEIGHT_TOLERANCE_BLOCKS) return ENTRANCE;
    double roadX = x - ROAD_CENTER_X_BLOCKS;
    if (Math.abs(roadX) <= ROAD_HALF_WIDTH_BLOCKS
        && z >= ROAD_START_Z_BLOCKS
        && z <= ROAD_END_Z_BLOCKS)
      return z <= ROAD_SIGN_Z_BLOCKS + WAYFINDING_MARGIN_BLOCKS ? WAYFINDING : ROAD;
    BlockPos junction = IntroCoast.roadJunction(site);
    double dx = player.getX() - junction.getX(), dz = player.getZ() - junction.getZ();
    if (dx >= PARKING_MIN_X_BLOCKS
        && dx <= PARKING_MAX_X_BLOCKS
        && dz >= PARKING_MIN_Z_BLOCKS
        && dz <= PARKING_WALKWAY_Z_BLOCKS) return PARKING;
    if (dx >= BOARDWALK_MIN_X_BLOCKS
        && dx <= BOARDWALK_MAX_X_BLOCKS
        && Math.abs(dz) <= BOARDWALK_HALF_WIDTH_BLOCKS) return BOARDWALK;
    if (dx >= SHORE_MIN_X_BLOCKS
        && dx <= SHORE_MAX_X_BLOCKS
        && Math.abs(dz) <= SHORE_HALF_LENGTH_BLOCKS
        && Math.abs(player.getY() - junction.getY()) <= SHORE_HEIGHT_TOLERANCE_BLOCKS) return SHORE;
    return 0;
  }

  private static boolean supported(ServerPlayer player) {
    BlockPos foot = player.blockPosition();
    return player.serverLevel().hasChunkAt(foot)
        // Free walking can be on slabs/stairs. Unlike the locked landing, do not
        // require an empty whole foot-block here: vanilla's grounded flag handles partial tops.
        && player.onGround()
        && !player.isInWaterOrBubble();
  }

  static void say(ServerPlayer player, CompoundTag chapter, String key) {
    player.sendSystemMessage(Component.translatable("message.timelocked.coast." + key));
    chapter.putInt("Cooldown", COOLDOWN_BUDGET_TICKS);
    dirty(player);
  }

  static boolean welcomeFinished(SiteProgress progress) {
    // Old already-completed arrivals have no queued welcome; do not restart it just for this
    // chapter.
    return progress != null
        && progress.arrivalLogoAnnounced
        && (progress.arrivalWelcomeNext >= SiteProgress.WELCOME_LINE_COUNT
            || progress.arrivalWelcomeNext == 0
                && progress.arrivalStep >= SiteProgress.FINAL_ARRIVAL_STAGE);
  }

  static void tickPlayer(ServerPlayer player) {
    CompoundTag intro = IntroSequence.state(player);
    if (!eligible(player, intro) || !supported(player)) return;
    BlockPos site = BlockPos.of(intro.getLong("Site"));
    int location = reached(player, site);
    if (location == 0) return; // Away from the route: no unsolicited chat or countdown catch-up.
    if (!intro.contains(SAVE_KEY)) {
      // Existing .18 Story saves opt in only when physically on their own route. No
      // teleport/rebuild.
      begin(player, intro);
    }
    CompoundTag chapter = chapter(intro);
    if (step(chapter) == COMPLETE) return;
    int cooldown = Math.clamp(chapter.getInt("Cooldown"), 0, COOLDOWN_BUDGET_TICKS);
    chapter.putInt("Cooldown", Math.max(0, cooldown - TICK_INTERVAL));
    dirty(player);
    if (location == HUB) {
      chapter.putInt("Step", HUB);
      var progress = FacilityState.get(player.server).progress.get(site.asLong());
      if (!welcomeFinished(progress)) {
        chapter.putInt("HubQuietTicks", 0);
        return; // The existing logo/anthem/welcome owns the room. Never talk over it.
      }
      int quiet =
          Math.clamp(chapter.getInt("HubQuietTicks"), 0, COOLDOWN_BUDGET_TICKS) + TICK_INTERVAL;
      chapter.putInt("HubQuietTicks", Math.min(quiet, COOLDOWN_BUDGET_TICKS));
      if (quiet >= COOLDOWN_BUDGET_TICKS && cooldown <= TICK_INTERVAL) {
        chapter.putInt("Step", COMPLETE);
        say(player, chapter, "complete");
      }
      return;
    }
    chapter.putInt("HubQuietTicks", 0);
    if (cooldown > TICK_INTERVAL) {
      player.displayClientMessage(objective(player, intro), true);
      return;
    }
    int previous = step(chapter);
    if (location > previous) {
      // Going directly to a later landmark is allowed. Skip missed lines, never dump a backlog.
      chapter.putInt("Step", location);
      say(player, chapter, "beat." + location);
    }
    player.displayClientMessage(objective(player, intro), true);
  }

  @SubscribeEvent
  public static void tick(ServerTickEvent.Post event) {
    if (event.getServer().overworld().getGameTime() % TICK_INTERVAL != 0) return;
    for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) tickPlayer(player);
  }

  /** Inspect the actual authored panels; both halves work, and sign theft never gates progress. */
  static boolean inspect(ServerPlayer player, BlockPos clicked) {
    CompoundTag intro = IntroSequence.state(player);
    if (!eligible(player, intro)
        || !intro.contains(SAVE_KEY)
        || player.isSecondaryUseActive()
        || player.distanceToSqr(clicked.getCenter()) > PANEL_REACH_BLOCKS_SQUARED) return false;
    CompoundTag chapter = chapter(intro);
    if (step(chapter) == COMPLETE || chapter.getInt("Cooldown") > 0) return false;
    var block = player.level().getBlockState(clicked);
    if (!(block.getBlock() instanceof CoastSigns.Panel)) return false;
    BlockPos base =
        block.getValue(CoastSigns.Panel.HALF) == DoubleBlockHalf.UPPER ? clicked.below() : clicked;
    BlockPos wake = IntroCoast.wake(BlockPos.of(intro.getLong("Site")));
    boolean restriction =
        base.equals(wake.offset(RESTRICTION_SIGN_X_BLOCKS, 0, RESTRICTION_SIGN_Z_BLOCKS))
            && block.is(CoastSigns.RESTRICTION.get());
    boolean direction =
        base.equals(wake.offset(DIRECTION_SIGN_X_BLOCKS, 0, DIRECTION_SIGN_Z_BLOCKS))
            && block.is(CoastSigns.DIRECTION.get());
    if (!restriction && !direction) return false;
    String flag = restriction ? "ReadRestriction" : "ReadDirection";
    if (chapter.getBoolean(flag)) return false;
    chapter.putBoolean(flag, true);
    say(player, chapter, restriction ? "panel.restriction" : "panel.direction");
    return true;
  }

  @SubscribeEvent
  public static void interact(PlayerInteractEvent.RightClickBlock event) {
    if (event.getHand() == InteractionHand.MAIN_HAND
        && event.getEntity() instanceof ServerPlayer player
        && inspect(player, event.getPos())) {
      event.setCanceled(true);
      event.setCancellationResult(InteractionResult.SUCCESS);
    }
  }

  static Component objective(ServerPlayer player, CompoundTag intro) {
    int step = intro.contains(SAVE_KEY) ? step(chapter(intro)) : 0;
    if (step == COMPLETE
        && BlueprintRooms.drawing(player.serverLevel(), BlockPos.of(intro.getLong("Site"))))
      return ReceptionChapter.objective(intro);
    BlockPos site = BlockPos.of(intro.getLong("Site"));
    BlockPos junction = IntroCoast.roadJunction(site), surface = IntroCoast.surfaceOrigin(site);
    BlockPos target =
        switch (step) {
          case 0, SHORE -> junction.offset(WALKWAY_TARGET_X_BLOCKS, 0, WALKWAY_TARGET_Z_BLOCKS);
          case PARKING -> junction.offset(BOARDWALK_TARGET_X_BLOCKS, 0, 0);
          case BOARDWALK -> junction;
          case ROAD -> new BlockPos(junction.getX(), 0, surface.getZ() + ROAD_SIGN_Z_BLOCKS);
          default -> surface.offset(ROAD_CENTER_X_BLOCKS, 0, FORECOURT_TARGET_Z_BLOCKS);
        };
    return Component.translatable(
        "message.timelocked.coast.objective." + step, target.getX(), target.getZ());
  }

  @SubscribeEvent
  public static void commands(RegisterCommandsEvent event) {
    event
        .getDispatcher()
        .register(
            Commands.literal("timelockedstory")
                .executes(
                    context -> {
                      ServerPlayer player = context.getSource().getPlayerOrException();
                      CompoundTag intro = IntroSequence.state(player);
                      if (!eligible(player, intro)) {
                        player.sendSystemMessage(
                            Component.translatable("message.timelocked.coast.unavailable"));
                        return 0;
                      }
                      player.sendSystemMessage(objective(player, intro));
                      return 1;
                    })
                .then(
                    Commands.literal("skip")
                        .executes(
                            context -> {
                              ServerPlayer player = context.getSource().getPlayerOrException();
                              CompoundTag intro = IntroSequence.state(player);
                              if (!eligible(player, intro)) return 0;
                              begin(player, intro);
                              chapter(intro).putInt("Step", COMPLETE);
                              dirty(player);
                              player.sendSystemMessage(
                                  Component.translatable("message.timelocked.coast.skipped"));
                              return 1;
                            })));
  }

  private CoastChapter() {}
}
