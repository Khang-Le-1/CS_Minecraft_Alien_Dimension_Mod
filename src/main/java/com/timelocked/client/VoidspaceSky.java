package com.timelocked.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import org.joml.Matrix4f;

/** A fixed celestial night: violet horizon, stable constellations, auroras and twin moons. */
@EventBusSubscriber(modid = "timelocked", bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class VoidspaceSky extends DimensionSpecialEffects {
  public static final ResourceLocation EFFECTS =
      ResourceLocation.fromNamespaceAndPath("timelocked", "voidspace");

  public VoidspaceSky() {
    super(Float.NaN, false, SkyType.NONE, false, false);
  }

  @SubscribeEvent
  public static void register(RegisterDimensionSpecialEffectsEvent event) {
    event.register(EFFECTS, new VoidspaceSky());
  }

  @Override
  public Vec3 getBrightnessDependentFogColor(Vec3 color, float brightness) {
    return color;
  }

  @Override
  public boolean isFoggyAt(int x, int y) {
    return false;
  }

  private static void vertex(
      BufferBuilder buffer,
      Matrix4f matrix,
      double azimuth,
      double elevation,
      int red,
      int green,
      int blue,
      int alpha) {
    double a = Math.toRadians(azimuth),
        e = Math.toRadians(elevation),
        horizontal = Math.cos(e) * 100;
    buffer
        .addVertex(
            matrix,
            (float) (Math.cos(a) * horizontal),
            (float) (Math.sin(e) * 100),
            (float) (Math.sin(a) * horizontal))
        .setColor(red, green, blue, alpha);
  }

  private static void domeVertex(
      BufferBuilder buffer, Matrix4f matrix, double azimuth, double elevation) {
    double horizon = Math.exp(-Math.pow(elevation / 22, 2));
    double lower = elevation < -10 ? .55 : 1;
    vertex(
        buffer,
        matrix,
        azimuth,
        elevation,
        (int) ((7 + 30 * horizon) * lower),
        (int) ((9 + 14 * horizon) * lower),
        (int) ((24 + 36 * horizon) * lower),
        255);
  }

  private static void dome(Matrix4f matrix) {
    BufferBuilder buffer =
        Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
    for (int elevation = -90; elevation < 90; elevation += 10) {
      for (int azimuth = 0; azimuth < 360; azimuth += 6) {
        domeVertex(buffer, matrix, azimuth, elevation);
        domeVertex(buffer, matrix, azimuth + 6, elevation);
        domeVertex(buffer, matrix, azimuth + 6, elevation + 10);
        domeVertex(buffer, matrix, azimuth, elevation + 10);
      }
    }
    BufferUploader.drawWithShader(buffer.buildOrThrow());
  }

  private static void aurora(Matrix4f matrix, float time, int ribbon) {
    BufferBuilder buffer =
        Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
    for (int segment = 0; segment < 128; segment++) {
      double a = -35 + segment * 2.2 + ribbon * 125, b = a + 2.2;
      for (int band = 0; band < 8; band++) {
        for (int corner = 0; corner < 4; corner++) {
          double azimuth = corner == 0 || corner == 3 ? a : b;
          double fraction = (band + (corner >= 2 ? 1 : 0)) / 8.0;
          double wave =
              Math.sin(Math.toRadians(azimuth * 2.5) + time * .13 + ribbon) * 4
                  + Math.sin(Math.toRadians(azimuth * 5) - time * .08) * 1.7;
          double elevation = 23 + ribbon * 11 + wave + fraction * 19;
          double edge = Math.sin(segment / 127.0 * Math.PI);
          double curtain = .65 + .35 * Math.sin(Math.toRadians(azimuth * 15) + time * .3);
          int alpha = (int) (Math.sin(fraction * Math.PI) * edge * curtain * 38);
          vertex(
              buffer,
              matrix,
              azimuth,
              elevation,
              ribbon == 0 ? 65 : 155,
              ribbon == 0 ? 205 : 105,
              220,
              Math.max(0, alpha));
        }
      }
    }
    BufferUploader.drawWithShader(buffer.buildOrThrow());
  }

  @Override
  public boolean renderSky(
      ClientLevel level,
      int ticks,
      float partialTick,
      Matrix4f modelView,
      Camera camera,
      Matrix4f projection,
      boolean foggy,
      Runnable setupFog) {
    // Do not paint a sky over underwater/lava fog or a visually obscured camera.
    if (foggy || camera.getFluidInCamera() != FogType.NONE) return true;
    PoseStack pose = new PoseStack();
    pose.mulPose(modelView);
    FogRenderer.setupNoFog();
    RenderSystem.depthMask(false);
    RenderSystem.disableCull();
    RenderSystem.enableBlend();
    RenderSystem.defaultBlendFunc();
    try {
      RenderSystem.setShaderColor(1, 1, 1, 1);
      RenderSystem.setShader(GameRenderer::getPositionColorShader);
      dome(pose.last().pose());
      pose.pushPose();
      pose.mulPose(Axis.XP.rotationDegrees(27));
      pose.mulPose(Axis.YP.rotationDegrees(38));
      RenderSystem.blendFuncSeparate(770, 1, 1, 0);
      RenderSystem.setShaderColor(.85F, .88F, 1, .86F);
      VertexBuffer stars = SpaceSky.stars();
      stars.bind();
      stars.drawWithShader(pose.last().pose(), projection, GameRenderer.getPositionColorShader());
      VertexBuffer.unbind();
      pose.popPose();
      RenderSystem.setShaderColor(1, 1, 1, 1);
      RenderSystem.setShader(GameRenderer::getPositionColorShader);
      float time = (ticks + partialTick) / 20F;
      aurora(pose.last().pose(), time, 0);
      aurora(pose.last().pose(), time, 1);
      RenderSystem.defaultBlendFunc();
      SpaceSky.quad(pose, new SpaceSky.Body(SpaceSky.tex("aurelia_moon"), 16, 35, 42, false));
      SpaceSky.quad(pose, new SpaceSky.Body(SpaceSky.tex("aurelia_moon_b"), 7, 152, 27, false));
    } finally {
      RenderSystem.setShaderColor(1, 1, 1, 1);
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableBlend();
      RenderSystem.enableCull();
      RenderSystem.depthMask(true);
      setupFog.run();
    }
    return true;
  }

  @Override
  public boolean renderClouds(
      ClientLevel level,
      int ticks,
      float partialTick,
      PoseStack pose,
      double x,
      double y,
      double z,
      Matrix4f view,
      Matrix4f projection) {
    return true;
  }

  @Override
  public boolean tickRain(ClientLevel level, int ticks, Camera camera) {
    return true;
  }

  @Override
  public boolean renderSnowAndRain(
      ClientLevel level,
      int ticks,
      float partialTick,
      LightTexture light,
      double x,
      double y,
      double z) {
    return true;
  }
}
