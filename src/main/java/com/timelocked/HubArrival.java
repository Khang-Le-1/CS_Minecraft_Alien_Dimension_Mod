/* Reconstructed from the user-provided 0.8.9.8 release with permission. */
package com.timelocked;

import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Facility-wide light startup, then one welcome line at a time. Progress belongs to the site, not a
 * player; leaving the hub pauses the welcome instead of queuing a burst on return.
 */
@EventBusSubscriber(modid = "timelocked")
public final class HubArrival {
  private static final int TICKS_PER_SECOND = 20;
  private static final int POLL_INTERVAL_TICKS = TICKS_PER_SECOND;
  private static final int LIGHT_STAGE_INTERVAL_TICKS = 2 * TICKS_PER_SECOND;
  private static final int FINAL_LIGHT_STAGE = SiteProgress.FINAL_ARRIVAL_STAGE;
  // Only the current drawing layout supports this startup sequence; this is a saved version ID.
  private static final int ARRIVAL_GEOMETRY_VERSION = 5;
  // Hub bounds in blocks relative to the facility origin. Preserve strict X/Z edges.
  private static final int HUB_MIN_X_EXCLUSIVE = 3;
  private static final int HUB_MAX_X_EXCLUSIVE = 154;
  private static final int HUB_MIN_Y_INCLUSIVE = 2;
  private static final int HUB_MAX_Y_EXCLUSIVE = 19;
  private static final int HUB_MIN_Z_EXCLUSIVE = 185;
  private static final int HUB_MAX_Z_EXCLUSIVE = 337;
  // Old releases placed the logo this many blocks back from the corrected drawing position.
  // Power those blocks in place; never move or rebuild a player's existing facility.
  private static final int[] LEGACY_LOGO_Z_OFFSETS_BLOCKS = {-12, -17};
  // Delay before each of the seven recorded lines. Line one is emitted at logo activation.
  private static final int[] WELCOME_DELAYS_TICKS = {
    0,
    7 * TICKS_PER_SECOND,
    9 * TICKS_PER_SECOND,
    8 * TICKS_PER_SECOND,
    10 * TICKS_PER_SECOND,
    9 * TICKS_PER_SECOND,
    7 * TICKS_PER_SECOND
  };
  private static final float ANTHEM_VOLUME = 0.85F;

  // Shared with the Story chapter so it uses the exact same hub boundary as the PA.
  static boolean inside(ServerPlayer player, BlockPos origin) {
    BlockPos local = player.blockPosition().subtract(origin);
    return !player.isSpectator()
        && local.getX() > HUB_MIN_X_EXCLUSIVE
        && local.getX() < HUB_MAX_X_EXCLUSIVE
        && local.getY() >= HUB_MIN_Y_INCLUSIVE
        && local.getY() < HUB_MAX_Y_EXCLUSIVE
        && local.getZ() > HUB_MIN_Z_EXCLUSIVE
        && local.getZ() < HUB_MAX_Z_EXCLUSIVE
        && !GlassStairwell.containsApproach(local)
        && !TransitionDoorways.atLobbyThreshold(local);
  }

  @SubscribeEvent
  public static void tick(ServerTickEvent.Post event) {
    ServerLevel level = event.getServer().overworld();
    if (level.getGameTime() % POLL_INTERVAL_TICKS != 0) return;
    FacilityState data = FacilityState.get(event.getServer());
    for (Map.Entry<Long, SiteProgress> entry : data.progress.entrySet()) {
      SiteProgress progress = entry.getValue();
      if (progress.geometryVersion != ARRIVAL_GEOMETRY_VERSION) continue;
      BlockPos origin = BlockPos.of(entry.getKey());
      List<ServerPlayer> players = level.players().stream().filter(p -> inside(p, origin)).toList();
      if (players.isEmpty()) continue;
      if (tickWelcome(progress, players)) data.setDirty();
      if (!progress.arrivalStarted) {
        if (!hasUnlitFirstStage(level, origin)) continue;
        progress.arrivalStarted = true;
        data.setDirty();
        play(players, AlarmSounds.ARRIVAL_SWITCH.get());
      }
      if (progress.arrivalStep < FINAL_LIGHT_STAGE) {
        progress.arrivalTicks += POLL_INTERVAL_TICKS;
        if (progress.arrivalTicks >= LIGHT_STAGE_INTERVAL_TICKS) {
          progress.arrivalTicks = 0;
          ++progress.arrivalStep;
          play(players, soundForStage(progress.arrivalStep));
        }
        data.setDirty();
      }
      boolean logoActivated = false;
      for (int[] cell : DrawingArrivalIndex.CELLS) {
        BlockPos position = DrawingArrivalIndex.position(origin, cell);
        if (cell[DrawingArrivalIndex.STAGE] > progress.arrivalStep || !level.hasChunkAt(position))
          continue;
        BlockState state = level.getBlockState(position);
        if (cell[DrawingArrivalIndex.KIND] == DrawingArrivalIndex.LOGO
            && !state.is(Content.HANGING_LOGO.get())) {
          for (int offset : LEGACY_LOGO_Z_OFFSETS_BLOCKS) {
            BlockPos legacy = position.offset(0, 0, offset);
            if (level.hasChunkAt(legacy)
                && level.getBlockState(legacy).is(Content.HANGING_LOGO.get())) {
              position = legacy;
              state = level.getBlockState(position);
              break;
            }
          }
        }
        if (cell[DrawingArrivalIndex.KIND] == DrawingArrivalIndex.LIGHT
            && state.getBlock() instanceof StationLightBlock
            && !state.getValue(StationLightBlock.LIT)) {
          level.setBlock(position, state.setValue(StationLightBlock.LIT, true), Block.UPDATE_ALL);
        } else if (cell[DrawingArrivalIndex.KIND] == DrawingArrivalIndex.LOGO
            && state.is(Content.HANGING_LOGO.get())
            && !state.getValue(SarcHangingLogoBlock.LIT)) {
          logoActivated |=
              level.setBlock(
                  position, state.setValue(SarcHangingLogoBlock.LIT, true), Block.UPDATE_ALL);
        }
      }
      if (logoActivated && announceLogoPower(progress, players)) data.setDirty();
    }
  }

  private static boolean hasUnlitFirstStage(ServerLevel level, BlockPos origin) {
    for (int[] cell : DrawingArrivalIndex.CELLS) {
      BlockPos position = DrawingArrivalIndex.position(origin, cell);
      if (cell[DrawingArrivalIndex.STAGE] != 1 || !level.hasChunkAt(position)) continue;
      BlockState state = level.getBlockState(position);
      if (state.getBlock() instanceof StationLightBlock && !state.getValue(StationLightBlock.LIT))
        return true;
    }
    return false;
  }

  private static SoundEvent soundForStage(int stage) {
    // Historical recording numbering differs from the chronological light-stage order.
    return switch (stage) {
      case FINAL_LIGHT_STAGE -> AlarmSounds.ARRIVAL_01.get();
      case 1 -> AlarmSounds.ARRIVAL_02.get();
      case 2 -> AlarmSounds.ARRIVAL_03.get();
      default -> AlarmSounds.ARRIVAL_04.get();
    };
  }

  private static void play(List<ServerPlayer> players, SoundEvent sound) {
    for (ServerPlayer player : players)
      player.playNotifySound(sound, SoundSource.BLOCKS, 1.0F, 1.0F);
  }

  /** Start one timed welcome and anthem per facility on the logo transition. */
  static boolean announceLogoPower(SiteProgress progress, List<ServerPlayer> players) {
    if (progress.arrivalLogoAnnounced || players.isEmpty()) return false;
    progress.arrivalLogoAnnounced = true;
    progress.arrivalWelcomeNext = 1;
    progress.arrivalWelcomeTicks = 0;
    for (ServerPlayer player : players) {
      player.sendSystemMessage(
          net.minecraft.network.chat.Component.translatable(
              "message.timelocked.arrival_welcome.1"));
      player.playNotifySound(
          AlarmSounds.NAU_ANTHEM_PA.get(), SoundSource.BLOCKS, ANTHEM_VOLUME, 1.0F);
    }
    return true;
  }

  /** Called once per second; no catch-up burst or progress without a hub audience. */
  static boolean tickWelcome(SiteProgress progress, List<ServerPlayer> players) {
    int next = progress.arrivalWelcomeNext;
    if (!progress.arrivalLogoAnnounced
        || next < 1
        || next >= WELCOME_DELAYS_TICKS.length
        || players.isEmpty()) return false;
    progress.arrivalWelcomeTicks += POLL_INTERVAL_TICKS;
    if (progress.arrivalWelcomeTicks >= WELCOME_DELAYS_TICKS[next]) {
      for (ServerPlayer player : players)
        player.sendSystemMessage(
            net.minecraft.network.chat.Component.translatable(
                "message.timelocked.arrival_welcome." + (next + 1)));
      progress.arrivalWelcomeNext = next + 1;
      progress.arrivalWelcomeTicks = 0;
    }
    return true;
  }

  private HubArrival() {}
}
