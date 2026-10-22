package com.timelocked.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.progress.StartupNotificationManager;

/** Self-contained startup assets: no dependency on the resource reload being finished. */
public final class StartupBranding {
  private static final org.slf4j.Logger LOGGER = LogUtils.getLogger();
  private static final ResourceLocation LOGO =
      ResourceLocation.fromNamespaceAndPath("timelocked", "dynamic/cks_boot_logo");
  private static final ResourceLocation FONT =
      ResourceLocation.fromNamespaceAndPath("timelocked", "dynamic/cks_boot_font");
  private static final BootTerminal TERMINAL = new BootTerminal();
  private static boolean attempted, ready, reported, overlayFrame;

  public static void markOverlayFrame() {
    overlayFrame = true;
  }

  public static boolean consumeOverlayFrame() {
    boolean marked = overlayFrame;
    overlayFrame = false;
    return marked;
  }

  private static void upload(Minecraft mc, ResourceLocation id, String path) throws IOException {
    try (var in =
        StartupBranding.class.getResourceAsStream(
            "/assets/timelocked/textures/gui/loading/" + path)) {
      if (in == null) throw new IOException("Missing packaged startup asset " + path);
      NativeImage image = NativeImage.read(in);
      DynamicTexture texture = null;
      try {
        texture = new DynamicTexture(image);
        mc.getTextureManager().register(id, texture);
      } catch (RuntimeException ex) {
        if (texture != null) texture.close();
        else image.close();
        throw ex;
      }
    }
  }

  private static boolean prepare(Minecraft mc) {
    if (!attempted) {
      attempted = true;
      try {
        upload(mc, LOGO, "cks_white.png");
        upload(mc, FONT, "terminal_font.png");
        ready = true;
      } catch (IOException | RuntimeException ex) {
        LOGGER.error("TIMELOCKED boot branding unavailable; retaining NeoForge overlay", ex);
      }
    }
    return ready;
  }

  public static void draw(float progress, float opacity) {
    Minecraft mc = Minecraft.getInstance();
    int w = mc.getWindow().getWidth(), h = mc.getWindow().getHeight();
    if (w < 1 || h < 1 || opacity <= 0 || !prepare(mc)) return;
    var savedProjection = new org.joml.Matrix4f(RenderSystem.getProjectionMatrix());
    var savedSorting = RenderSystem.getVertexSorting();
    var savedModel = new org.joml.Matrix4f(RenderSystem.getModelViewMatrix());
    try {
      RenderSystem.getModelViewMatrix().identity();
      RenderSystem.setProjectionMatrix(
          new org.joml.Matrix4f().setOrtho(0, w, 0, h, .1F, -.1F), VertexSorting.ORTHOGRAPHIC_Z);
      drawLayout(mc, w, h, progress, opacity);
    } finally {
      RenderSystem.getModelViewMatrix().set(savedModel);
      RenderSystem.setProjectionMatrix(savedProjection, savedSorting);
    }
  }

  private static void drawLayout(Minecraft mc, int w, int h, float progress, float opacity) {
    if (!reported) {
      reported = true;
      LOGGER.info("TIMELOCKED CKS boot terminal rendered (packaged textures, real FML progress)");
    }
    long now = Util.getMillis();
    var messages = new ArrayList<>(StartupNotificationManager.getMessages());
    messages.sort(
        Comparator.comparingInt((StartupNotificationManager.AgeMessage m) -> m.age()).reversed());
    for (var message : messages) TERMINAL.offer(message.message().getText(), now);
    float scale = Math.max(.25F, Math.min(2F, h / 900F));
    float tw = Math.min(w * .70F, h * 1.12F), left = (w - tw) / 2, top = h * .44F;
    float lineHeight = 26 * scale, charWidth = 12 * scale, glyphHeight = 24 * scale;
    int columns = Math.max(16, (int) ((tw - 40 * scale) / charWidth) - 2);
    RenderSystem.disableDepthTest();
    RenderSystem.disableCull();
    RenderSystem.enableBlend();
    RenderSystem.defaultBlendFunc();
    RenderSystem.setShaderColor(1, 1, 1, opacity);
    RenderSystem.setShader(GameRenderer::getPositionColorShader);
    BufferBuilder b =
        Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
    rect(b, 0, 0, w, h, 0xFF10171D);
    rect(b, left - 1, h - top - 11.5F * lineHeight - 1, left + tw + 1, h - top + 1, 0xFF304550);
    rect(b, left, h - top - 11.5F * lineHeight, left + tw, h - top, 0xFF0A1116);
    float barWidth = tw, x = left, y = h * .13F, barHeight = Math.max(3, h / 180F);
    rect(b, x - 1, y - 1, x + barWidth + 1, y + barHeight + 1, 0xFF4B5B67);
    rect(b, x, y, x + barWidth, y + barHeight, 0xFF192B35);
    boolean known = Float.isFinite(progress) && progress >= 0;
    float p = known ? Math.max(0, Math.min(1, progress)) : 0;
    if (known) rect(b, x, y, x + barWidth * p, y + barHeight, 0xFF94D8E2);
    else {
      float seg = barWidth / 4, off = (now / 8L % (long) Math.max(1, barWidth + seg)) - seg;
      float a = Math.max(x, x + off), z = Math.min(x + barWidth, x + off + seg);
      if (z > a) rect(b, a, y, z, y + barHeight, 0xFF94D8E2);
    }
    BufferUploader.drawWithShader(b.buildOrThrow());
    float panelTop = h - top, margin = h * .03F, logoTop = h * .96F, logoBottom = panelTop + margin;
    float lw = Math.min(Math.min(w * .52F, h * 1.5F), (logoTop - logoBottom) * 5000F / 1500F),
        lh = lw * 1500F / 5000F,
        lx = (w - lw) / 2,
        ly = logoBottom + ((logoTop - logoBottom) - lh) / 2;
    RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
    RenderSystem.setShaderTexture(0, LOGO);
    b =
        Tesselator.getInstance()
            .begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
    quad(b, lx, ly, lw, lh, 0, 0, 1, 1, 0xFFFFFFFF);
    BufferUploader.drawWithShader(b.buildOrThrow());
    RenderSystem.setShaderTexture(0, FONT);
    b =
        Tesselator.getInstance()
            .begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
    float tx = left + 20 * scale, ty = h - top - 17 * scale - glyphHeight;
    text(b, "CKS / RESOURCE BOOT CONSOLE", tx, ty, charWidth, glyphHeight, 0xFF94D8E2, columns);
    ty -= lineHeight;
    var history = TERMINAL.visible(now);
    if (history.isEmpty())
      text(
          b,
          "> Waiting for loader notifications",
          tx,
          ty,
          charWidth,
          glyphHeight,
          0xFF8AA4B2,
          columns);
    else
      for (String line : history) {
        text(b, "> " + line, tx, ty, charWidth, glyphHeight, 0xFFD7E1E7, columns);
        ty -= lineHeight;
      }
    float statusY = h - top - 9.4F * lineHeight;
    var meters = StartupNotificationManager.getCurrentProgress();
    int meterLines = 0;
    for (var meter : meters) {
      if (meter.name().equals("Minecraft Progress")) continue;
      String label = meter.label() == null ? "" : meter.label().getText();
      String status =
          meter.name()
              + " "
              + meter.current()
              + "/"
              + meter.steps()
              + (label.isBlank() ? "" : " | " + label);
      text(b, BootTerminal.clean(status), tx, statusY, charWidth, glyphHeight, 0xFF91A9B7, columns);
      statusY -= lineHeight;
      if (++meterLines == 1) break;
    }
    text(
        b,
        (known ? "> reload " + Math.round(p * 100) + "%" : "> waiting for reload progress")
            + (now / 500 % 2 == 0 ? " _" : ""),
        tx,
        h - top - 10.4F * lineHeight,
        charWidth,
        glyphHeight,
        0xFF94D8E2,
        columns);
    BufferUploader.drawWithShader(b.buildOrThrow());
    RenderSystem.setShaderColor(1, 1, 1, 1);
    RenderSystem.disableBlend();
    RenderSystem.enableCull();
  }

  private static void text(
      BufferBuilder b, String raw, float x, float y, float cw, float ch, int color, int max) {
    String s = BootTerminal.clean(raw);
    if (s.length() > max) s = s.substring(0, Math.max(0, max - 3)) + "...";
    for (int i = 0; i < s.length(); i++) {
      int n = s.charAt(i);
      float u = (n % 16) / 16F, v = (n / 16) / 8F;
      quad(b, x + i * cw, y, cw * 4 / 3F, ch, u, v, u + 1 / 16F, v + 1 / 8F, color);
    }
  }

  private static void quad(
      BufferBuilder b,
      float x,
      float y,
      float w,
      float h,
      float u0,
      float v0,
      float u1,
      float v1,
      int color) {
    b.addVertex(x, y, 0).setUv(u0, v1).setColor(color);
    b.addVertex(x + w, y, 0).setUv(u1, v1).setColor(color);
    b.addVertex(x + w, y + h, 0).setUv(u1, v0).setColor(color);
    b.addVertex(x, y + h, 0).setUv(u0, v0).setColor(color);
  }

  private static void rect(BufferBuilder b, float x0, float y0, float x1, float y1, int color) {
    b.addVertex(x0, y0, 0).setColor(color);
    b.addVertex(x1, y0, 0).setColor(color);
    b.addVertex(x1, y1, 0).setColor(color);
    b.addVertex(x0, y1, 0).setColor(color);
  }

  private StartupBranding() {}
}
