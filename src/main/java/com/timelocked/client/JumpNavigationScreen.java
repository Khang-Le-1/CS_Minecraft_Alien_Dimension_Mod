package com.timelocked.client;

import com.timelocked.JumpNetworking;
import com.timelocked.SpacePlanets;
import com.timelocked.Timeskipper;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * A real flight panel: choose a destination, then deliberately press the guarded JUMP button.
 * Clicking a destination never launches by itself.
 */
public final class JumpNavigationScreen extends Screen {
  private final JumpNetworking.Navigation info;
  private int selected = -1;
  private Button jump;
  private String status = "Select a destination.";
  private int left, top, wide, tall;
  public static boolean reducedFlashes = true;

  public JumpNavigationScreen(JumpNetworking.Navigation info) {
    super(Component.literal("TIMESKIPPER Navigation"));
    this.info = info;
  }

  @Override
  protected void init() {
    wide = Math.min(500, width - 24);
    tall = Math.min(230, height - 20);
    left = (width - wide) / 2;
    top = (height - tall) / 2;

    SpacePlanets.Planet[] planets = SpacePlanets.Planet.values();
    int cell = (wide - 32) / 2;
    for (int i = 0; i < planets.length; i++) {
      final int n = i;
      SpacePlanets.Planet p = planets[i];
      String text =
          p.title
              + (i == info.location()
                  ? "  [HERE]"
                  : "  [" + p.cost + " charge" + (p.cost == 1 ? "" : "s") + "]");
      addRenderableWidget(
          Button.builder(
                  Component.literal(text),
                  b -> {
                    selected = n;
                    status = p.site + " | " + (p.atmosphere ? "breathable" : "vacuum");
                    refresh();
                  })
              .bounds(left + 12 + (i % 2) * (cell + 8), top + 48 + (i / 2) * 23, cell, 20)
              .build());
    }

    jump =
        addRenderableWidget(
            Button.builder(
                    Component.literal("SELECT DESTINATION"),
                    b -> {
                      if (selected < 0) return;
                      PacketDistributor.sendToServer(new JumpNetworking.Request(selected));
                      status = "Arming drive...";
                      jump.active = false;
                    })
                .bounds(left + 16, top + tall - 36, wide - 160, 24)
                .build());
    jump.active = false;

    addRenderableWidget(
        Button.builder(
                Component.literal(reducedFlashes ? "Soft flash: ON" : "Soft flash: OFF"),
                b -> {
                  reducedFlashes = !reducedFlashes;
                  b.setMessage(
                      Component.literal(reducedFlashes ? "Soft flash: ON" : "Soft flash: OFF"));
                })
            .bounds(left + wide - 132, top + tall - 36, 116, 24)
            .build());

    refresh();
  }

  private void refresh() {
    if (jump == null) return;
    SpacePlanets.Planet p = selected < 0 ? null : SpacePlanets.Planet.values()[selected];
    jump.active =
        p != null && selected != info.location() && (info.creative() || info.charges() >= p.cost);
    jump.setMessage(
        Component.literal(
            p == null
                ? "SELECT DESTINATION"
                : selected == info.location()
                    ? "ALREADY HERE"
                    : !jump.active
                        ? "INSUFFICIENT CHARGE"
                        : "JUMP TO " + p.title.toUpperCase(Locale.ROOT)));
  }

  public void notice(String message) {
    status = message;
    refresh();
  }

  @Override
  public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
    // Solid flight-deck backdrop; skip the default blur/panorama over the world.
    g.fill(0, 0, width, height, 0xA0080B14);
  }

  @Override
  public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
    super.render(g, mouseX, mouseY, partial);

    g.fill(left, top, left + wide, top + tall, 0xF0101928);
    g.fill(left, top, left + wide, top + 2, 0xFF6EDBEE);
    g.drawString(font, "SARC  /  TIMESKIPPER FLIGHT CONTROL", left + 12, top + 12, 0xC3F5FF, false);
    SpacePlanets.Planet here = SpacePlanets.Planet.byIndex(info.location());
    g.drawString(
        font,
        "AT: " + here.title + "    CHARGES: " + info.charges() + " / " + Timeskipper.MAX_CHARGES,
        left + 12,
        top + 29,
        0xA6B8D0,
        false);
    if (tall >= 215) {
      g.drawString(
          font, font.plainSubstrByWidth(status, wide - 24), left + 12, top + 147, 0xE1C9FF, false);
    }

    // A physical-looking guard around the explicit launch control.
    g.fill(left + 12, top + tall - 39, left + wide - 140, top + tall - 37, 0xFFD69E4C);
    g.fill(left + 12, top + tall - 12, left + wide - 140, top + tall - 10, 0xFFD69E4C);
  }

  @Override
  public boolean isPauseScreen() {
    return false;
  }
}
