package com.timelocked.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexBuffer.Usage;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.mojang.math.Axis;
import com.timelocked.SpacePlanets;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.DimensionSpecialEffects.SkyType;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import org.joml.Matrix4f;

@EventBusSubscriber(modid = "timelocked", bus = Bus.MOD, value = Dist.CLIENT)
public final class SpaceSky {
  static final ResourceLocation SUN =
      ResourceLocation.withDefaultNamespace("textures/environment/sun.png");
  static final int GlStateManager_SRC_ALPHA = 770;
  static final int GlStateManager_ONE = 1;
  static final int GlStateManager_ZERO = 0;
  private static VertexBuffer STARS;

  static ResourceLocation tex(String n) {
    return ResourceLocation.fromNamespaceAndPath(
        "timelocked", "textures/environment/space/" + n + ".png");
  }

  static SpaceSky.Body b(String t, float size, float yaw, float el) {
    return new SpaceSky.Body(tex(t), size, yaw, el, false);
  }

  static List<SpaceSky.Body> bodies(SpacePlanets.Planet p) {
    return switch (p) {
      case EARTH -> List.of(b("earth", 150.0F, 0.0F, -62.0F), b("moon", 14.0F, 140.0F, 25.0F));
      case ORBIT -> List.of(b("earth", 260.0F, 0.0F, -75.0F), b("moon", 8.0F, 140.0F, 25.0F));
      case LUNA -> List.of(b("earth", 30.0F, 30.0F, 38.0F));
      case MARS -> List.of(b("phobos", 6.0F, 200.0F, 48.0F), b("deimos", 3.0F, 60.0F, 30.0F));
      case BELT ->
          List.of(
              b("jupiter", 10.0F, 120.0F, 12.0F),
              b("asteroid", 7.0F, 300.0F, 40.0F),
              b("asteroid", 4.0F, 250.0F, -10.0F));
      case EUROPA -> List.of(b("jupiter", 95.0F, 40.0F, 30.0F), b("io", 8.0F, 170.0F, 55.0F));
      case AURELIA ->
          List.of(
              b("aurelia_giant", 70.0F, 20.0F, 28.0F),
              b("aurelia_moon", 12.0F, 170.0F, 50.0F),
              b("aurelia_moon_b", 6.0F, 250.0F, 22.0F));
      default -> List.of();
    };
  }

  static void quad(PoseStack ps, SpaceSky.Body body) {
    ps.pushPose();
    ps.mulPose(Axis.YP.rotationDegrees(-body.yaw()));
    ps.mulPose(Axis.XP.rotationDegrees(90.0F - body.elevation()));
    Matrix4f m = ps.last().pose();
    float s = body.size();
    if (body.glow()) {
      RenderSystem.blendFuncSeparate(770, 1, 1, 0);
    } else {
      RenderSystem.defaultBlendFunc();
    }

    RenderSystem.setShader(GameRenderer::getPositionTexShader);
    RenderSystem.setShaderTexture(0, body.tex());
    BufferBuilder buf =
        Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
    buf.addVertex(m, -s, 100.0F, -s).setUv(0.0F, 0.0F);
    buf.addVertex(m, s, 100.0F, -s).setUv(1.0F, 0.0F);
    buf.addVertex(m, s, 100.0F, s).setUv(1.0F, 1.0F);
    buf.addVertex(m, -s, 100.0F, s).setUv(0.0F, 1.0F);
    BufferUploader.drawWithShader(buf.buildOrThrow());
    ps.popPose();
  }

  static VertexBuffer stars() {
    if (STARS != null) {
      return STARS;
    }

    RandomSource r = RandomSource.create(10842L);
    BufferBuilder buf =
        Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

    for (int i = 0; i < 4200; i++) {
      double x;
      double y;
      double z;
      if (i < 2600) {
        x = r.nextFloat() * 2.0F - 1.0F;
        y = r.nextFloat() * 2.0F - 1.0F;
        z = r.nextFloat() * 2.0F - 1.0F;
      } else {
        double a = r.nextDouble() * Math.PI * 2.0;
        x = Math.cos(a);
        z = Math.sin(a);
        y = r.nextGaussian() * 0.09;
      }

      double len = x * x + y * y + z * z;
      if (!(len < 0.01) && (!(len > 1.0) || i >= 2600)) {
        len = Math.sqrt(len);
        x /= len;
        y /= len;
        z /= len;
        float size = (float) (i < 2600 ? 0.15 + r.nextFloat() * 0.35 : 0.08 + r.nextFloat() * 0.14);
        if (r.nextInt(40) == 0) {
          size *= 2.2F;
        }

        float br = 0.45F + r.nextFloat() * 0.55F;
        int tint = r.nextInt(10);
        float cr = tint < 2 ? 0.75F : (tint < 3 ? 1.0F : 0.95F);
        float cg = tint < 2 ? 0.85F : (tint < 3 ? 0.8F : 0.95F);
        float cb = tint < 2 ? 1.0F : (tint < 3 ? 0.6F : 1.0F);
        int alpha = (int) (255.0F * br);
        double px = x * 100.0;
        double py = y * 100.0;
        double pz = z * 100.0;
        double ang = r.nextDouble() * Math.PI * 2.0;
        double atanXZ = Math.atan2(x, z);
        double sinXZ = Math.sin(atanXZ);
        double cosXZ = Math.cos(atanXZ);
        double atanY = Math.atan2(Math.sqrt(x * x + z * z), y);
        double sinY = Math.sin(atanY);
        double cosY = Math.cos(atanY);
        double sinA = Math.sin(ang);
        double cosA = Math.cos(ang);

        for (int v = 0; v < 4; v++) {
          double u = ((v & 2) - 1) * size;
          double w = ((v + 1 & 2) - 1) * size;
          double rx = u * cosA - w * sinA;
          double rw = w * cosA + u * sinA;
          double ry = rx * sinY;
          double yy = -rx * cosY;
          double vx = yy * sinXZ - rw * cosXZ;
          double vz = rw * sinXZ + yy * cosXZ;
          buf.addVertex((float) (px + vx), (float) (py + ry), (float) (pz + vz))
              .setColor((int) (cr * 255.0F), (int) (cg * 255.0F), (int) (cb * 255.0F), alpha);
        }
      }
    }

    STARS = new VertexBuffer(Usage.STATIC);
    STARS.bind();
    STARS.upload(buf.buildOrThrow());
    VertexBuffer.unbind();
    return STARS;
  }

  @SubscribeEvent
  public static void register(RegisterDimensionSpecialEffectsEvent e) {
    e.register(
        ResourceLocation.fromNamespaceAndPath("timelocked", "timeskipper"),
        new SpaceSky.Sky(SpacePlanets.Planet.EARTH, true, 1.0F, 22.0F));
    e.register(
        ResourceLocation.fromNamespaceAndPath("timelocked", "orbit"),
        new SpaceSky.Sky(SpacePlanets.Planet.ORBIT, false, 1.0F, 22.0F));
    e.register(
        ResourceLocation.fromNamespaceAndPath("timelocked", "luna"),
        new SpaceSky.Sky(SpacePlanets.Planet.LUNA, false, 1.0F, 22.0F));
    e.register(
        ResourceLocation.fromNamespaceAndPath("timelocked", "mars"),
        new SpaceSky.Sky(SpacePlanets.Planet.MARS, false, 0.12F, 16.0F));
    e.register(
        ResourceLocation.fromNamespaceAndPath("timelocked", "kessler_belt"),
        new SpaceSky.Sky(SpacePlanets.Planet.BELT, false, 1.0F, 9.0F));
    e.register(
        ResourceLocation.fromNamespaceAndPath("timelocked", "europa"),
        new SpaceSky.Sky(SpacePlanets.Planet.EUROPA, false, 1.0F, 7.0F));
    e.register(
        ResourceLocation.fromNamespaceAndPath("timelocked", "aurelia"),
        new SpaceSky.Sky(SpacePlanets.Planet.AURELIA, false, 0.35F, 26.0F));
  }

  private SpaceSky() {}

  record Body(ResourceLocation tex, float size, float yaw, float elevation, boolean glow) {}

  static final class Sky extends DimensionSpecialEffects {
    final SpacePlanets.Planet planet;
    final boolean ship;
    final float starAlpha;
    final float sunSize;

    Sky(SpacePlanets.Planet planet, boolean ship, float starAlpha, float sunSize) {
      super(Float.NaN, false, SkyType.NONE, false, false);
      this.planet = planet;
      this.ship = ship;
      this.starAlpha = starAlpha;
      this.sunSize = sunSize;
    }

    public Vec3 getBrightnessDependentFogColor(Vec3 c, float brightness) {
      return c;
    }

    public boolean isFoggyAt(int x, int y) {
      return false;
    }

    public boolean renderSky(
        ClientLevel level,
        int ticks,
        float partialTick,
        Matrix4f modelView,
        Camera camera,
        Matrix4f projection,
        boolean foggy,
        Runnable setupFog) {
      SpacePlanets.Planet at =
          this.ship ? SpacePlanets.Planet.byIndex(SpacePlanets.clientShipLocation) : this.planet;
      PoseStack ps = new PoseStack();
      ps.mulPose(modelView);
      FogRenderer.setupNoFog();
      RenderSystem.depthMask(false);
      RenderSystem.enableBlend();
      RenderSystem.blendFuncSeparate(770, 1, 1, 0);
      float t = (ticks + partialTick) * 0.0015F;
      ps.pushPose();
      ps.mulPose(Axis.YP.rotationDegrees(t));
      ps.mulPose(Axis.XP.rotationDegrees(23.0F));
      VertexBuffer stars = SpaceSky.stars();
      RenderSystem.setShaderColor(
          1.0F, 1.0F, 1.0F, this.ship && at.atmosphere ? 0.6F : this.starAlpha);
      stars.bind();
      stars.drawWithShader(ps.last().pose(), projection, GameRenderer.getPositionColorShader());
      VertexBuffer.unbind();
      ps.popPose();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      float sun =
          this.ship
              ? (at == SpacePlanets.Planet.AURELIA
                  ? 24
                  : (at.ordinal() >= SpacePlanets.Planet.BELT.ordinal() ? 9 : 22))
              : this.sunSize;
      SpaceSky.quad(ps, new SpaceSky.Body(SpaceSky.SUN, sun, -60.0F, 58.0F, true));
      SpaceSky.quad(ps, new SpaceSky.Body(SpaceSky.SUN, sun * 2.2F, -60.0F, 58.0F, true));
      RenderSystem.defaultBlendFunc();

      for (SpaceSky.Body body : SpaceSky.bodies(at)) {
        SpaceSky.quad(ps, body);
      }

      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.disableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.depthMask(true);
      setupFog.run();
      return true;
    }

    public boolean renderClouds(
        ClientLevel level,
        int ticks,
        float partialTick,
        PoseStack ps,
        double x,
        double y,
        double z,
        Matrix4f mv,
        Matrix4f proj) {
      return true;
    }

    public boolean tickRain(ClientLevel level, int ticks, Camera camera) {
      return true;
    }

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
}
