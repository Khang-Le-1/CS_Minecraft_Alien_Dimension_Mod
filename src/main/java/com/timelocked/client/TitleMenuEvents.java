package com.timelocked.client;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

/** Title-only branding. Gameplay, other screens, and panorama assets are unchanged. */
@EventBusSubscriber(modid = "timelocked", value = Dist.CLIENT)
public final class TitleMenuEvents {
  private TitleMenuEvents() {}

  @SubscribeEvent(priority = EventPriority.LOWEST)
  public static void opening(ScreenEvent.Opening event) {
    // Do not replace title-screen subclasses supplied by other mods.
    if (event.getNewScreen() != null && event.getNewScreen().getClass() == TitleScreen.class) {
      event.setNewScreen(new MenuScreen());
    }
  }

  private static final class MenuScreen extends TitleScreen {
    private static final ResourceLocation LOGO =
        ResourceLocation.fromNamespaceAndPath("timelocked", "textures/gui/title/logo.png");
    private static final int TEXTURE_WIDTH = 2048;
    private static final int TEXTURE_HEIGHT = 544;
    private int logoWidth;
    private int logoHeight;
    private int logoY;
    private int subtitleY;
    private String versionLabel;
    private float uiScale;

    private MenuScreen() {
      super(false);
    }

    @Override
    protected void init() {
      super.init();
      List<AbstractWidget> widgets = new ArrayList<>();
      for (var child : children()) {
        if (child instanceof AbstractWidget widget) widgets.add(widget);
      }
      int nativeTop =
          widgets.stream()
              .filter(this::menuControl)
              .mapToInt(AbstractWidget::getY)
              .min()
              .orElse(height / 4 + 32);
      widgets.add(
          0,
          Button.builder(
                  net.minecraft.network.chat.Component.literal("Story"),
                  b -> StoryWorldCreation.open(this))
              .bounds(width / 2 - 100, nativeTop - 24, 200, 20)
              .build());
      int firstRow =
          widgets.stream()
              .filter(this::menuControl)
              .mapToInt(AbstractWidget::getY)
              .min()
              .orElse(height / 4 + 32);
      int lastRow =
          widgets.stream()
              .filter(this::menuControl)
              .mapToInt(AbstractWidget::getY)
              .max()
              .orElse(firstRow);
      // Match the approved composition independently of Minecraft's GUI scale.
      uiScale = Math.min(width / (1600.0F / 3.0F), height / 285.0F);
      int controlsHeight = Math.round((lastRow - firstRow + 20) * uiScale);
      logoWidth = Math.min(width - 32, Math.round(300 * uiScale));
      logoHeight = Math.round(logoWidth * (float) TEXTURE_HEIGHT / TEXTURE_WIDTH);
      int subtitleHeight = Math.round(font.lineHeight * uiScale);
      int afterSubtitle = Math.round(14 * uiScale);
      int stackHeight = logoHeight + 8 + subtitleHeight + afterSubtitle + controlsHeight;
      logoY = Math.max(8, (height - 20 - stackHeight) / 2);
      subtitleY = logoY + logoHeight + 8;
      int controlsY = subtitleY + subtitleHeight + afterSubtitle;
      versionLabel =
          "TL: Zero Point / "
              + ModList.get()
                  .getModContainerById("timelocked")
                  .map(container -> container.getModInfo().getVersion().toString())
                  .orElse("0.8.0");

      // Re-add in native order, retaining keyboard traversal and narration order.
      for (AbstractWidget widget : widgets) removeWidget(widget);
      for (AbstractWidget widget : widgets) {
        boolean control = menuControl(widget);
        boolean footer =
            widget instanceof Button && widget.getY() >= height - 24 && widget.getHeight() < 20;
        if (control) {
          widget.setPosition(
              width / 2 + Math.round((widget.getX() - width / 2) * uiScale),
              controlsY + Math.round((widget.getY() - firstRow) * uiScale));
        } else if (footer) {
          widget.setPosition(
              width - Math.round(widget.getWidth() * uiScale) - 2,
              height - Math.round(widget.getHeight() * uiScale) - 2);
        }
        if ((control || footer) && widget instanceof Button button) {
          String utility = nativeUtilityLabel(button);
          if (!(button instanceof SpriteIconButton) || utility != null) {
            addRenderableWidget(new MetalButton(button, utility, uiScale, footer));
            continue;
          }
        }
        addRenderableWidget(widget);
      }
    }

    private boolean menuControl(AbstractWidget widget) {
      return widget instanceof Button && widget.getHeight() == 20 && widget.getY() < height - 24;
    }

    private static String nativeUtilityLabel(Button button) {
      if (button.getMessage().getContents() instanceof TranslatableContents text) {
        return switch (text.getKey()) {
          case "options.language" -> "L";
          case "options.accessibility" -> "A";
          default -> null;
        };
      }
      return null;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      // The native panorama renderer keeps the supplied six faces and user settings.
      PANORAMA.render(graphics, width, height, 1.0F, partialTick);
      graphics.fillGradient(0, 0, width, Math.max(1, subtitleY + 20), 0x60080D17, 0x08080D17);
      graphics.fillGradient(0, height * 3 / 4, width, height, 0x00080D17, 0xA0080D17);
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      graphics.setColor(1, 1, 1, 1);
      graphics.blit(
          LOGO,
          (width - logoWidth) / 2,
          logoY,
          logoWidth,
          logoHeight,
          0,
          0,
          TEXTURE_WIDTH,
          TEXTURE_HEIGHT,
          TEXTURE_WIDTH,
          TEXTURE_HEIGHT);
      RenderSystem.disableBlend();
      drawSubtitle(graphics);
      for (Renderable widget : renderables) {
        widget.render(graphics, mouseX, mouseY, partialTick);
      }
      graphics.pose().pushPose();
      graphics.pose().translate(6 * uiScale, height - font.lineHeight * uiScale - 2, 0);
      graphics.pose().scale(uiScale, uiScale, 1);
      graphics.drawString(font, versionLabel, 0, 0, 0xFFB5C3CF, false);
      graphics.pose().popPose();
    }

    private void drawSubtitle(GuiGraphics graphics) {
      String text = "ZERO POINT";
      int spacing = 3;
      int textWidth = font.width(text) + spacing * (text.length() - 1);
      graphics.pose().pushPose();
      graphics.pose().translate(width / 2.0F, subtitleY, 0);
      graphics.pose().scale(uiScale, uiScale, 1);
      int x = -textWidth / 2;
      int midY = 4;
      graphics.fill(x - 28, midY, x - 10, midY + 1, 0xFFC6AD6E);
      for (int i = 0; i < text.length(); i++) {
        String character = text.substring(i, i + 1);
        graphics.drawString(font, character, x, 0, 0xFFA6DCE8, false);
        x += font.width(character) + spacing;
      }
      x -= spacing;
      graphics.fill(x + 10, midY, x + 28, midY + 1, 0xFFC6AD6E);
      graphics.pose().popPose();
    }
  }

  /** Keep each native callback, disabled state, translation, and tooltip. */
  private static final class MetalButton extends Button {
    private final Button original;
    private final String utility;
    private final float scale;
    private final boolean footer;

    private MetalButton(Button original, String utility, float scale, boolean footer) {
      super(
          original.getX(),
          original.getY(),
          Math.round(original.getWidth() * scale),
          Math.round(original.getHeight() * scale),
          original.getMessage(),
          ignored -> {
            if (original.active && original.visible) original.onPress();
          },
          DEFAULT_NARRATION);
      this.original = original;
      this.utility = utility;
      this.scale = scale;
      this.footer = footer;
      active = original.active;
      visible = original.visible;
      setTooltip(original.getTooltip());
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      active = original.active;
      setMessage(original.getMessage());
      setTooltip(original.getTooltip());
      boolean emphasis = active && isHoveredOrFocused();
      int w = original.getWidth();
      int h = original.getHeight();
      var font = Minecraft.getInstance().font;
      graphics.pose().pushPose();
      graphics.pose().translate(getX(), getY(), 0);
      graphics.pose().scale(scale, scale, 1);
      if (footer) {
        graphics.drawString(
            font,
            getMessage().copy().withStyle(style -> style.withUnderlined(emphasis)),
            0,
            0,
            0xFFFFFFFF,
            true);
        graphics.pose().popPose();
        return;
      }
      int border = !active ? 0xFF3D4955 : emphasis ? 0xFFE7C77A : 0xFF80A4B0;
      int top = !active ? 0xF51C232C : emphasis ? 0xF5343A3C : 0xF5203342;
      int bottom = !active ? 0xF5141A22 : emphasis ? 0xF520272C : 0xF5192936;
      graphics.fill(0, 0, w, h, border);
      graphics.fillGradient(1, 1, w - 1, h - 1, top, bottom);
      graphics.fill(2, 2, w - 2, 3, 0x306C8394);
      graphics.fill(2, 3, 3, h - 3, !active ? 0xFF586875 : emphasis ? 0xFFE7C77A : 0xFF5DA5B8);
      int color = !active ? 0xFF7F8D99 : emphasis ? 0xFFF2DC9D : 0xFFE2EEF4;
      if (utility != null) {
        graphics.drawString(
            font, utility, (w - font.width(utility)) / 2, (h - font.lineHeight) / 2, color, false);
      } else {
        AbstractWidget.renderScrollingString(graphics, font, getMessage(), 6, 0, w - 6, h, color);
      }
      graphics.pose().popPose();
    }
  }
}
