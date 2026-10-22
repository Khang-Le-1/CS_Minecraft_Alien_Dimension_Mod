package com.timelocked.client;

import com.timelocked.StoryMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

/** One-shot creation intent is consumed by a fresh screen, never by a world join. */
@EventBusSubscriber(modid = "timelocked", value = Dist.CLIENT)
public final class StoryWorldCreation {
  private static boolean requested;

  public static void open(Screen parent) {
    requested = true;
    CreateWorldScreen.openFresh(Minecraft.getInstance(), parent);
  }

  @SubscribeEvent
  public static void opening(ScreenEvent.Opening event) {
    if (!requested) return;
    if (event.getNewScreen() instanceof CreateWorldScreen screen) {
      requested = false;
      var state = screen.getUiState();
      state.setName("TIMELOCKED - Story");
      apply(state);
      state.addListener(StoryWorldCreation::apply);
    } else if (event.getNewScreen() instanceof TitleScreen) {
      requested = false;
    }
  }

  private static void apply(WorldCreationUiState state) {
    state.getGameRules().getRule(StoryMode.RULE).set(true, null);
    if (state.getGameMode() != WorldCreationUiState.SelectedGameMode.SURVIVAL)
      state.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL);
    if (state.getWorldType().preset() != null
        && !state.getWorldType().preset().is(WorldPresets.NORMAL)) {
      state.getNormalPresetList().stream()
          .filter(t -> t.preset() != null && t.preset().is(WorldPresets.NORMAL))
          .findFirst()
          .ifPresent(state::setWorldType);
    }
  }

  private StoryWorldCreation() {}
}
