package com.timelocked.client;

import com.mojang.blaze3d.systems.RenderSystem;
import java.lang.reflect.Field;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent.Render.Pre;

@EventBusSubscriber(modid = "timelocked", value = Dist.CLIENT)
public final class LoadingScreens {
  public static final ResourceLocation BACKGROUND =
      ResourceLocation.fromNamespaceAndPath("timelocked", "textures/gui/loading/background.png");
  public static final ResourceLocation LOGO =
      ResourceLocation.fromNamespaceAndPath("timelocked", "textures/gui/title/logo.png");
  public static final String[] TIPS =
      new String[] {
        "SARC reminds all staff: the Zero Point is perfectly safe.",
        "Nexian breaches come in three waves. Hold the line.",
        "Celestials remember every kindness. And every grudge.",
        "House Dawnbringer burns what it cannot convert.",
        "Hot chocolate restores morale. This is documented science.",
        "Lost? Follow the approach road to the facility entrance.",
        "Sneak to step off a vehicle once it is safe.",
        "Glimmer moths are drawn to light. So are worse things.",
        "If a satellite falls from the sky, it is probably fine. Probably.",
        "Recovery trackers always point to the nearest SARC facility.",
        "The reactor needs three sealed fuel cells. Not two. Three.",
        "Field shelters keep the dark outside. Mostly.",
        "Time is a flat circle. SARC is working on a fix.",
        "Mind the gap between the tram and the platform."
      };

  @SubscribeEvent
  public static void render(Pre e) {
    Screen s = e.getScreen();
    String title;
    float progress;
    if (s instanceof LevelLoadingScreen) {
      title = "Preparing the timeline";
      progress = levelProgress(s);
    } else if (s instanceof ReceivingLevelScreen) {
      ClientLevel level = Minecraft.getInstance().level;
      String dim = level == null ? "" : level.dimension().location().toString();
      title =
          dim.contains("voidspace")
              ? "Crossing the Zero Point"
              : (dim.equals("minecraft:overworld") ? "Returning to Earth" : "Shifting dimensions");
      progress = -1.0F;
    } else {
      if (!(s instanceof GenericMessageScreen) && !(s instanceof ProgressScreen)) {
        return;
      }

      title = s.getTitle().getString();
      if (title.isBlank()) {
        title = "Working";
      }

      progress = s instanceof ProgressScreen ? intField(s, "progress") / 100.0F : -1.0F;
    }

    e.setCanceled(true);
    draw(e.getGuiGraphics(), s.width, s.height, title, progress);
  }

  private static float levelProgress(Screen s) {
    try {
      Field f = LevelLoadingScreen.class.getDeclaredField("progressListener");
      f.setAccessible(true);
      Object listener = f.get(s);
      return ((Number) listener.getClass().getMethod("getProgress").invoke(listener)).floatValue()
          / 100.0F;
    } catch (ReflectiveOperationException | RuntimeException ex) {
      return -1.0F;
    }
  }

  private static int intField(Object o, String name) {
    try {
      Field f = o.getClass().getDeclaredField(name);
      f.setAccessible(true);
      return f.getInt(o);
    } catch (ReflectiveOperationException | RuntimeException ex) {
      return -1;
    }
  }

  public static void draw(GuiGraphics g, int w, int h, String title, float progress) {
    float scale = Math.max(w / 1280.0F, h / 720.0F);
    int uw = Math.round(w / scale);
    int vh = Math.round(h / scale);
    g.blit(BACKGROUND, 0, 0, w, h, (1280 - uw) / 2.0F, (720 - vh) / 2.0F, uw, vh, 1280, 720);
    g.fillGradient(0, h * 2 / 3, w, h, 0, -1073413360);
    Font font = Minecraft.getInstance().font;
    int maxLogoHeight = Math.max(1, h / 3 - 20);
    int lw = Math.min(Math.round(w * 0.36F), maxLogoHeight * 2048 / 544);
    lw = Math.max(1, Math.min(w - 32, lw));
    int lh = Math.round(lw * 544.0F / 2048.0F);
    int logoY = h / 6;
    RenderSystem.enableBlend();
    RenderSystem.defaultBlendFunc();
    g.setColor(1, 1, 1, 1);
    g.blit(LOGO, (w - lw) / 2, logoY, lw, lh, 0.0F, 0.0F, 2048, 544, 2048, 544);
    RenderSystem.disableBlend();
    float brandScale = Math.max(0.65F, Math.min(1.25F, lw / 250.0F));
    int subtitleY = logoY + lh + Math.max(4, Math.round(8 * brandScale));
    drawSubtitle(g, font, w, subtitleY, brandScale);
    long now = Util.getMillis();
    int dots = (int) (now / 400L % 4L);
    g.drawCenteredString(
        font, Component.literal(title + ".".repeat(dots)), w / 2, h * 2 / 3, -1450497);
    int bw = Math.min(w - 40, 240);
    int bx = (w - bw) / 2;
    int by = h * 2 / 3 + 16;
    g.fill(bx - 1, by - 1, bx + bw + 1, by + 7, -8760632);
    g.fill(bx, by, bx + bw, by + 6, -15594972);
    if (progress >= 0.0F) {
      int fw = Math.round(bw * Math.min(1.0F, progress));
      g.fillGradient(bx, by, bx + fw, by + 6, -4158209, -8766240);
      g.drawCenteredString(
          font,
          Component.literal(Math.round(Math.min(1.0F, progress) * 100.0F) + "%"),
          w / 2,
          by + 11,
          -4610330);
    } else {
      int seg = bw / 4;
      int off = (int) (now / 8L % (bw + seg)) - seg;
      int x0 = Math.max(bx, bx + off);
      int x1 = Math.min(bx + bw, bx + off + seg);
      if (x1 > x0) {
        g.fillGradient(x0, by, x1, by + 6, -4158209, -8766240);
      }
    }

    String tip = TIPS[(int) (now / 5000L % TIPS.length)];
    g.drawCenteredString(font, Component.literal(tip), w / 2, h - 24, -6650168);
  }

  /** Shared visual language with the title menu, without changing loading state. */
  private static void drawSubtitle(GuiGraphics g, Font font, int w, int y, float scale) {
    String text = "ZERO POINT";
    int spacing = 3;
    int textWidth = font.width(text) + spacing * (text.length() - 1);
    g.pose().pushPose();
    g.pose().translate(w / 2.0F, y, 0);
    g.pose().scale(scale, scale, 1);
    int x = -textWidth / 2;
    g.fill(x - 28, 4, x - 10, 5, 0xFFC6AD6E);
    for (int i = 0; i < text.length(); i++) {
      String character = text.substring(i, i + 1);
      // A hard pixel outline keeps the subtitle legible over the eclipse flare.
      g.drawString(font, character, x - 1, 0, 0xFF1B122C, false);
      g.drawString(font, character, x + 1, 0, 0xFF1B122C, false);
      g.drawString(font, character, x, -1, 0xFF1B122C, false);
      g.drawString(font, character, x, 1, 0xFF1B122C, false);
      g.drawString(font, character, x, 0, 0xFFA6DCE8, false);
      x += font.width(character) + spacing;
    }
    x -= spacing;
    g.fill(x + 10, 4, x + 28, 5, 0xFFC6AD6E);
    g.pose().popPose();
  }

  private LoadingScreens() {}
}
