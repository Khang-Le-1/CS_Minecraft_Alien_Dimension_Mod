package com.timelocked.client;

import com.timelocked.AlarmSounds;
import com.timelocked.BattleMusicState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.Music;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Streamed, non-positional MUSIC-category track. One instance for the whole encounter. */
@EventBusSubscriber(modid = "timelocked", value = Dist.CLIENT)
public final class BionexianBattleMusic {
  public static final Music MUSIC = new Music(AlarmSounds.BIONEXIAN_BATTLE, 0, 0, true);
  private static final BattleMusicState STATE = new BattleMusicState();
  private static ClientLevel boundLevel;

  private BionexianBattleMusic() {}

  public static boolean active() {
    return STATE.active();
  }

  public static void receive(boolean active, ResourceLocation dimension) {
    Minecraft mc = Minecraft.getInstance();
    if (mc.level == null
        || mc.player == null
        || !mc.player.isAlive()
        || mc.player.isSpectator()
        || !mc.level.dimension().location().equals(dimension)) {
      reset();
      return;
    }
    boolean wasActive = STATE.active();
    boundLevel = mc.level;
    STATE.receive(active);
    if (active && !wasActive) {
      mc.getMusicManager().stopPlaying();
      mc.getMusicManager().startPlaying(MUSIC);
    } else if (!active && wasActive) {
      stopTrack();
    }
  }

  @SubscribeEvent
  public static void tick(ClientTickEvent.Post event) {
    Minecraft mc = Minecraft.getInstance();
    boolean valid =
        mc.level != null
            && mc.level == boundLevel
            && mc.player != null
            && mc.player.isAlive()
            && !mc.player.isSpectator();
    if (!valid) {
      reset();
      return;
    }
    // Integrated-server heartbeats pause along with the game. Do not expire in a pause menu.
    if (mc.isPaused()) return;
    boolean wasActive = STATE.active();
    STATE.tick(true);
    if (wasActive && !STATE.active()) stopTrack();
  }

  @SubscribeEvent
  public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
    reset();
  }

  private static void reset() {
    boolean wasActive = STATE.active();
    STATE.receive(false);
    boundLevel = null;
    if (wasActive) stopTrack();
  }

  private static void stopTrack() {
    var manager = Minecraft.getInstance().getMusicManager();
    if (manager.isPlayingMusic(MUSIC)) manager.stopPlaying();
  }
}
