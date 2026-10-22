package com.timelocked.client;

import com.timelocked.ReactorFuelMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class ReactorFuelScreen extends AbstractContainerScreen<ReactorFuelMenu> {
  public ReactorFuelScreen(ReactorFuelMenu m, Inventory inv, Component title) {
    super(m, inv, title);
    this.imageWidth = 176;
    this.imageHeight = 166;
  }

  protected void renderBg(GuiGraphics g, float partial, int x, int y) {
    g.fill(this.leftPos, this.topPos, this.leftPos + 176, this.topPos + 166, 0xFF80A4B0);
    g.fillGradient(
        this.leftPos + 1,
        this.topPos + 1,
        this.leftPos + 175,
        this.topPos + 165,
        0xF5203342,
        0xF5192936);
    g.fill(this.leftPos + 2, this.topPos + 2, this.leftPos + 174, this.topPos + 3, 0x306C8394);
    g.fill(this.leftPos + 2, this.topPos + 3, this.leftPos + 3, this.topPos + 163, 0xFF5DA5B8);

    for (int i = 0; i < 3; i++) {
      int sx = this.leftPos + 61 + i * 18;
      int sy = this.topPos + 34;
      g.fill(sx, sy, sx + 18, sy + 18, 0xFF4B5B67);
      g.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xFF0A1116);
      g.drawString(
          this.font, Character.toString((char) ('A' + i)), sx + 6, sy - 11, 0xFFC6AD6E, false);
    }

    for (int r = 0; r < 4; r++) {
      for (int c = 0; c < 9; c++) {
        int sx = this.leftPos + 7 + c * 18;
        int sy = this.topPos + (r == 3 ? 141 : 83 + r * 18);
        g.fill(sx, sy, sx + 18, sy + 18, 0xFF4B5B67);
        g.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xFF0A1116);
      }
    }
  }

  protected void renderLabels(GuiGraphics g, int x, int y) {
    g.drawString(this.font, "Integral reactor fuel slots", 8, 6, 0xFFE2EEF4, false);
    g.drawString(this.font, "One cell per channel to start", 8, 60, 0xFF91A9B7, false);
    g.drawString(this.font, this.playerInventoryTitle, 8, 73, 0xFFE2EEF4, false);
  }

  public void render(GuiGraphics g, int x, int y, float partial) {
    super.render(g, x, y, partial);
    this.renderTooltip(g, x, y);
  }
}
