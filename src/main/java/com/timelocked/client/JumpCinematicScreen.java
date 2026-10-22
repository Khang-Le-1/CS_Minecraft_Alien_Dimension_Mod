package com.timelocked.client;

import com.timelocked.JumpTimeline;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Letterboxed real-time world rendering, not a prerecorded movie or fake loading screen. The shared
 * sequence plays out in the actual world.
 */
final class JumpCinematicScreen extends Screen {
  private boolean requested;

  JumpCinematicScreen() {
    super(Component.literal("TIMESKIP"));
  }

  @Override
  public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
    float age = JumpCutsceneClient.age(partial);
    JumpTimeline.Shot shot = JumpTimeline.shot(age);
    int bar = Math.max(20, height / 9);

    // Single gentle departure flash, never repeated strobing.
    float flash = JumpTimeline.flash(age) * (JumpNavigationScreen.reducedFlashes ? 0.36F : 0.85F);
    if (flash > 0.0F) {
      g.fill(0, 0, width, height, ((int) (flash * 255.0F) << 24) | 0xE6DBFF);
    }
    if (shot == JumpTimeline.Shot.SILENCE) g.fill(0, 0, width, height, 0xFF070911);
    if (shot == JumpTimeline.Shot.PANEL) g.fill(0, 0, width, height, 0x30060B1A);

    g.fill(0, 0, width, bar, 0xFF070911);
    g.fill(0, height - bar, width, height, 0xFF070911);

    String title =
        switch (shot) {
          case PANEL -> "JUMP AUTHORIZED";
          case DRIVE -> "CHRONOTITE DRIVE / SPIN-UP";
          case ENGINES -> "PROPULSION / IGNITION";
          case FLASH, SILENCE -> "";
          case ARRIVAL, DONE ->
              "ARRIVAL / " + JumpCutsceneClient.destination().toUpperCase(java.util.Locale.ROOT);
        };
    if (!title.isEmpty()) {
      g.drawCenteredString(font, title, width / 2, Math.max(5, (bar - 9) / 2), 0xD6EEFF);
    }
    String hint = requested ? "Skipping..." : "Space or Esc: skip shared sequence";
    g.drawCenteredString(font, hint, width / 2, height - bar + (bar - 9) / 2, 0x9EAAC2);
  }

  @Override
  public boolean keyPressed(int key, int scan, int mods) {
    if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_SPACE) {
      if (!requested) {
        requested = true;
        JumpCutsceneClient.skip();
      }
      return true;
    }
    return true;
  }

  @Override
  public boolean mouseClicked(double x, double y, int button) {
    return true;
  }

  @Override
  public boolean shouldCloseOnEsc() {
    return false;
  }

  @Override
  public void onClose() {
    if (!requested) {
      requested = true;
      JumpCutsceneClient.skip();
    }
  }

  @Override
  public boolean isPauseScreen() {
    return false;
  }
}
