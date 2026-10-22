package com.timelocked.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * Gives every other menu the title screen's look. Buttons, sliders, text fields, list backdrops and
 * separators are re-skinned through resource overrides (assets/minecraft/textures/gui). This class
 * adds the same navy top and bottom gradients the title screen draws over the panorama. Gameplay,
 * layouts, widgets and callbacks are untouched, and mod-provided title screens are skipped.
 */
@EventBusSubscriber(modid = "timelocked", value = Dist.CLIENT)
public final class MenuSkin {
  private MenuSkin() {}

  @SubscribeEvent
  public static void background(ScreenEvent.BackgroundRendered event) {
    Screen screen = event.getScreen();
    if (screen instanceof TitleScreen || screen instanceof JumpNavigationScreen) return;
    // In a world the vanilla transparent backdrop already darkens the view; only the panorama menus
    // need it.
    if (Minecraft.getInstance().level != null) return;
    GuiGraphics g = event.getGuiGraphics();
    int w = screen.width, h = screen.height;
    g.fillGradient(0, 0, w, Math.max(1, h / 4), 0x60080D17, 0x08080D17);
    g.fillGradient(0, h * 3 / 4, w, h, 0x00080D17, 0xA0080D17);
  }
}
