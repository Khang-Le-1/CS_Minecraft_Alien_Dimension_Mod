package com.timelocked.client;

import com.timelocked.AlarmSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.sounds.Music;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.SelectMusicEvent;

/**
 * Menu and loading music. The Depths replaces the vanilla menu track on every menu screen (no world
 * loaded). Polaris plays on the world-load and dimension-transit loading screens.
 */
@EventBusSubscriber(modid = "timelocked", value = Dist.CLIENT)
public final class MusicSelector {
  private MusicSelector() {}

  @SubscribeEvent
  public static void select(SelectMusicEvent event) {
    Minecraft mc = Minecraft.getInstance();
    if (mc.screen instanceof LevelLoadingScreen
        || mc.screen instanceof ReceivingLevelScreen
        || mc.screen instanceof ProgressScreen) {
      event.setMusic(new Music(AlarmSounds.POLARIS, 0, 0, true));
    } else if (BionexianBattleMusic.active()) {
      event.setMusic(BionexianBattleMusic.MUSIC);
    } else if (mc.level == null) {
      event.setMusic(new Music(AlarmSounds.MENU_DEPTHS, 20, 600, true));
    }
  }
}
