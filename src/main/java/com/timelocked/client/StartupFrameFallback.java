package com.timelocked.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.progress.StartupNotificationManager;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

/**
 * Frame-level safety net, including configurations using vanilla's loading overlay. Never cancels a
 * frame, reload, or overlay transition. No branding on title/world screens.
 */
@EventBusSubscriber(modid = "timelocked", value = Dist.CLIENT)
public final class StartupFrameFallback {
  private static Overlay tracked;
  private static long completeAt = -1;

  @SubscribeEvent
  public static void render(RenderFrameEvent.Post event) {
    boolean alreadyDrawn = StartupBranding.consumeOverlayFrame();
    Minecraft mc = Minecraft.getInstance();
    Overlay overlay = mc.getOverlay();
    if (!(overlay instanceof LoadingOverlay)) {
      tracked = null;
      completeAt = -1;
      return;
    }
    if (alreadyDrawn) return;
    if (tracked != overlay) {
      tracked = overlay;
      completeAt = -1;
    }
    float progress = -1;
    for (var meter : StartupNotificationManager.getCurrentProgress())
      if (meter.name().equals("Minecraft Progress") && meter.steps() > 0) {
        progress = meter.progress();
        break;
      }
    long now = Util.getMillis();
    if (progress >= 1 && completeAt < 0) completeAt = now;
    float opacity = completeAt < 0 ? 1 : 1 - Mth.clamp((now - completeAt - 1000) / 1000F, 0, 1);
    int w = mc.getWindow().getWidth(), h = mc.getWindow().getHeight();
    if (w < 1 || h < 1 || opacity <= 0) return;
    var projection = new Matrix4f(RenderSystem.getProjectionMatrix());
    var sorting = RenderSystem.getVertexSorting();
    var model = new Matrix4f(RenderSystem.getModelViewMatrix());
    var shader = RenderSystem.getShader();
    float[] colour = RenderSystem.getShaderColor().clone();
    boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST),
        cull = GL11.glIsEnabled(GL11.GL_CULL_FACE),
        blend = GL11.glIsEnabled(GL11.GL_BLEND);
    int sr = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB),
        dr = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB),
        sa = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA),
        da = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
    try {
      StartupBranding.draw(progress, opacity);
    } finally {
      RenderSystem.getModelViewMatrix().set(model);
      RenderSystem.setProjectionMatrix(projection, sorting);
      if (shader != null) RenderSystem.setShader(() -> shader);
      RenderSystem.setShaderColor(colour[0], colour[1], colour[2], colour[3]);
      RenderSystem.blendFuncSeparate(sr, dr, sa, da);
      if (depth) RenderSystem.enableDepthTest();
      else RenderSystem.disableDepthTest();
      if (cull) RenderSystem.enableCull();
      else RenderSystem.disableCull();
      if (blend) RenderSystem.enableBlend();
      else RenderSystem.disableBlend();
    }
  }

  private StartupFrameFallback() {}
}
