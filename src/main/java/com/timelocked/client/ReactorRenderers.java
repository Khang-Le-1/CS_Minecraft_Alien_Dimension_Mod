package com.timelocked.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.timelocked.PlantCoreBlock;
import com.timelocked.ReactorCoreBlock;
import com.timelocked.ReactorFrame;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;

public final class ReactorRenderers {
  static final int RED = 16726832;
  static final int GREEN = 5570410;
  static final int AMBER = 16756768;

  static float now(BlockEntity be, float pt) {
    return be.getLevel() == null ? 0.0F : (float) (be.getLevel().getGameTime() % 2400000L) + pt;
  }

  static int ambient(BlockEntity be, ReactorFrame.Shape sh, int self) {
    Level l = be.getLevel();
    if (l == null) {
      return 15728880;
    }

    BlockPos c = be.getBlockPos();
    int y = sh.bottom(c);
    int mid = y + sh.h() / 2;
    int R = sh.r() + 1;
    int b = self;
    int s = 0;

    for (BlockPos q :
        new BlockPos[] {
          c.atY(y + sh.h()),
          c.offset(R, 0, 0).atY(mid),
          c.offset(-R, 0, 0).atY(mid),
          c.offset(0, 0, R).atY(mid),
          c.offset(0, 0, -R).atY(mid),
          c.offset(R, 0, 0).atY(y),
          c.offset(-R, 0, 0).atY(y),
          c.offset(0, 0, R).atY(y),
          c.offset(0, 0, -R).atY(y)
        }) {
      int p = LevelRenderer.getLightColor(l, q);
      b = Math.max(b, LightTexture.block(p));
      s = Math.max(s, LightTexture.sky(p));
    }

    return LightTexture.pack(b, s);
  }

  static float wave(float t, float speed, float phase) {
    return 0.5F + 0.5F * (float) Math.sin(t * speed + phase);
  }

  static long hash(long v) {
    v ^= v >>> 33;
    v *= -49064778989728563L;
    v ^= v >>> 33;
    v *= -4265267296055464877L;
    return v ^ v >>> 33;
  }

  private ReactorRenderers() {}

  public static final class Facility implements BlockEntityRenderer<ReactorCoreBlock.Vessel> {
    public Facility(Context c) {}

    public AABB getRenderBoundingBox(ReactorCoreBlock.Vessel be) {
      return ReactorFrame.FACILITY.box(be.getBlockPos()).inflate(0.5);
    }

    public boolean shouldRenderOffScreen(ReactorCoreBlock.Vessel be) {
      return true;
    }

    public int getViewDistance() {
      return 128;
    }

    public void render(
        ReactorCoreBlock.Vessel be,
        float pt,
        PoseStack ps,
        MultiBufferSource buf,
        int light,
        int overlay) {
      boolean on = (Boolean) be.getBlockState().getValue(ReactorCoreBlock.ACTIVE);
      float t = ReactorRenderers.now(be, pt);
      float dt = Math.clamp(t - be.lastAnim, 0.0F, 5.0F);
      be.lastAnim = t;
      be.spin += dt * (on ? 16 : 0);
      be.rodLift = be.rodLift + ((on ? 1 : 0) - be.rodLift) * Math.min(1.0F, dt * 0.03F);
      ReactorRenderers.Sprites S = ReactorRenderers.Sprites.get();
      ReactorMesh m =
          new ReactorMesh(ps, buf, ReactorRenderers.ambient(be, ReactorFrame.FACILITY, on ? 9 : 0));
      ps.pushPose();
      ps.translate(0.5, 0.0, 0.5);
      m.solid(S.hazard).cylinder(3.3F, 0.0F, 0.45F, 24);
      m.solid(S.grate).ring(0.0F, 3.3F, 0.45F, 24, 1.0F);

      for (int i = 0; i < 12; i++) {
        double a = (Math.PI * 2) * i / 12.0;
        float x = (float) Math.cos(a) * 3.02F;
        float z = (float) Math.sin(a) * 3.02F;
        float b =
            on
                ? 0.3F
                    + 0.7F
                        * (float)
                            Math.pow(
                                0.5 + 0.5 * Math.cos((i - t * 0.25) * Math.PI * 2.0 / 12.0), 4.0)
                : 0.2F + 0.4F * ReactorRenderers.wave(t, 0.06F, 0.0F);
        m.glow(S.white, ReactorMesh.scale(on ? 6094730 : 16726832, b), 1.0F)
            .box(x - 0.09F, 0.45F, z - 0.09F, x + 0.09F, 0.58F, z + 0.09F);
      }

      m.solid(S.casing).frustum(2.95F, 0.45F, 2.4F, 1.45F, 16);
      int core =
          on ? ReactorMesh.mix(3116287, 11071743, ReactorRenderers.wave(t, 0.07F, 0.0F)) : 926262;
      m.glow(S.white, core, 1.0F).cylinder(2.2F, 1.5F, 8.5F, 16);
      if (on) {
        m.add(S.white, 3116287, 0.3F + 0.12F * ReactorRenderers.wave(t, 0.13F, 1.0F))
            .cylinder(2.3F, 2.3F, 7.7F, 16);
      }

      m.solid(S.panel);

      for (int i = 0; i < 16; i++) {
        if (i % 4 != 3 && i % 4 != 0) {
          m.frustum(2.4F, 1.45F, 2.4F, 8.5F, 16, i, i + 1);
        } else {
          m.frustum(2.4F, 1.45F, 2.4F, 2.4F, 16, i, i + 1);
          m.frustum(2.4F, 7.6F, 2.4F, 8.5F, 16, i, i + 1);
        }
      }

      m.solid(S.steel);

      for (int w = 0; w < 4; w++) {
        ps.pushPose();
        ps.mulPose(Axis.YP.rotation((float) ((-Math.PI / 2) * w)));

        for (float y : new float[] {3.9F, 5.95F}) {
          m.box(2.3F, y, -0.92F, 2.46F, y + 0.14F, 0.92F);
        }

        m.box(2.3F, 2.4F, -0.07F, 2.46F, 7.6F, 0.07F);
        ps.popPose();
      }

      m.solid(S.tung);

      for (float y : new float[] {1.45F, 4.85F, 8.25F}) {
        m.cylinder(2.5F, y, y + 0.22F, 16);
        m.ring(2.38F, 2.5F, y + 0.22F, 16, 1.0F);
        m.ring(2.38F, 2.5F, y, 16, -1.0F);
      }

      m.solid(S.casing).dome(2.4F, 8.5F, 1.2F, 16, 5, 0.95F);
      m.solid(S.steel).drum(1.05F, 9.55F, 9.9F, 12);

      for (int ix = -1; ix <= 1; ix++) {
        for (int iz = -1; iz <= 1; iz++) {
          float x = ix * 0.45F;
          float z = iz * 0.45F;
          float top = 10.45F + be.rodLift * 0.35F;
          m.solid(S.tung).box(x - 0.09F, 9.9F, z - 0.09F, x + 0.09F, top, z + 0.09F);
          int c =
              on
                  ? (ReactorRenderers.wave(t, 0.15F, ix * 2.1F + iz * 1.3F) > 0.2F
                      ? 5570410
                      : 2059052)
                  : 16726832;
          m.glow(S.white, c, 1.0F).box(x - 0.07F, top, z - 0.07F, x + 0.07F, top + 0.1F, z + 0.07F);
        }
      }

      for (int k = 0; k < 4; k++) {
        ps.pushPose();
        ps.mulPose(Axis.YP.rotation((float) (-((Math.PI / 4) + k * Math.PI / 2.0))));
        float P = 2.83F;
        ps.pushPose();
        ps.translate(P, 0.0F, 0.0F);
        m.solid(S.oxidized).drum(0.4F, 0.45F, 1.45F, 12);
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(be.spin * (k % 2 == 0 ? 1 : -1)));
        m.solid(S.steel);

        for (int b = 0; b < 3; b++) {
          ps.pushPose();
          ps.mulPose(Axis.YP.rotationDegrees(b * 60));
          m.box(-0.36F, 1.47F, -0.05F, 0.36F, 1.6F, 0.05F);
          ps.popPose();
        }

        ps.popPose();
        m.solid(S.steel).ring(0.3F, 0.4F, 1.62F, 12, 1.0F);
        m.drum(0.3F, 1.62F, 2.2F, 10);
        m.glow(S.white, on ? 5570410 : 16726832, 1.0F)
            .box(0.29F, 1.95F, -0.05F, 0.33F, 2.05F, 0.05F);
        m.solid(S.oxidized).cylinder(0.16F, 2.2F, 6.6F, 8);
        ps.popPose();
        m.solid(S.oxidized).box(2.3F, 6.38F, -0.16F, P + 0.16F, 6.7F, 0.16F);
        m.solid(S.steel).box(2.3F, 6.3F, -0.22F, 2.46F, 6.78F, 0.22F);
        m.box(P - 0.22F, 6.25F, -0.22F, P + 0.22F, 6.4F, 0.22F);
        ps.popPose();
      }

      ps.popPose();
    }
  }

  public static final class Plant implements BlockEntityRenderer<PlantCoreBlock.Core> {
    public Plant(Context c) {}

    public AABB getRenderBoundingBox(PlantCoreBlock.Core be) {
      return be.getLevel() == null
          ? new AABB(be.getBlockPos()).inflate(3.0)
          : ReactorFrame.shape(be.getLevel(), be.getBlockPos(), be.getBlockState())
              .box(be.getBlockPos())
              .inflate(0.5);
    }

    public boolean shouldRenderOffScreen(PlantCoreBlock.Core be) {
      return true;
    }

    public int getViewDistance() {
      return 128;
    }

    static int heatGlow(int base, float heat, float t, boolean crit) {
      int c = ReactorMesh.mix(base, 16777215, Math.clamp(heat / 1000.0F, 0.0F, 1.0F) * 0.45F);
      if (heat > 750.0F) {
        c =
            ReactorMesh.mix(
                c,
                16724000,
                (heat - 750.0F)
                    / 450.0F
                    * (0.6F + 0.4F * (float) Math.sin(t * (crit ? 0.6F : 0.2F))));
      }

      return c;
    }

    public void render(
        PlantCoreBlock.Core c,
        float pt,
        PoseStack ps,
        MultiBufferSource buf,
        int light,
        int overlay) {
      if (c.getLevel() != null && c.getBlockState().hasProperty(PlantCoreBlock.KIND)) {
        int k = c.kind();
        boolean lit = (Boolean) c.getBlockState().getValue(PlantCoreBlock.LIT);
        boolean crit = c.critical();
        ReactorFrame.Shape sh =
            ReactorFrame.shape(c.getLevel(), c.getBlockPos(), c.getBlockState());
        float t = ReactorRenderers.now(c, pt);
        float dt = Math.clamp(t - c.lastAnim, 0.0F, 5.0F);
        c.lastAnim = t;
        float out = lit ? Math.min(1.4F, c.output() / 400.0F) : 0.0F;
        int glow =
            lit
                ? heatGlow(PlantCoreBlock.COLOR[k], c.heat, t, crit)
                : ReactorMesh.scale(
                    PlantCoreBlock.COLOR[k], Math.clamp(0.1F + c.heat / 900.0F, 0.1F, 0.5F));

        for (int i = 0; i < 10; i++) {
          c.rodAnim[i] =
              c.rodAnim[i] + ((i >= c.rods ? 1 : 0) - c.rodAnim[i]) * Math.min(1.0F, dt * 0.08F);
        }

        ReactorRenderers.Sprites S = ReactorRenderers.Sprites.get();
        ReactorMesh m = new ReactorMesh(ps, buf, ReactorRenderers.ambient(c, sh, lit ? 10 : 2));
        ps.pushPose();
        ps.translate(0.5, -sh.base(), 0.5);
        if (c.industrial) {
          float width =
              com.timelocked.PowerStationGeometry.PLAN.industrial_reactor().mesh_width_scale();
          ps.scale(width, (float) ReactorFrame.INDUSTRIAL_H[k] / ReactorFrame.PLANT_H[k], width);
        }
        m.solid(S.hazard).cylinder(2.15F, 0.0F, 0.3F, 20);
        m.solid(S.grate).ring(0.0F, 2.15F, 0.3F, 20, 1.0F);
        switch (k) {
          case 1:
            this.fusion(c, m, ps, S, t, dt, lit, crit, glow, out);
            break;
          case 2:
            this.chrono(c, m, ps, S, t, dt, lit, crit, glow, out);
            break;
          case 3:
            this.voidCore(c, m, ps, S, t, dt, lit, crit, glow, out);
            break;
          default:
            this.fission(c, m, ps, S, t, dt, lit, crit, glow, out);
        }

        ps.popPose();
      }
    }

    static int alarm(PlantCoreBlock.Core c, float t, boolean lit, boolean crit) {
      if (crit) {
        return t % 8.0F < 4.0F ? 16726832 : 4198416;
      } else if (c.coolantFailing && lit) {
        return t % 20.0F < 10.0F ? 16756768 : 4206608;
      } else {
        return lit ? 5570410 : 2105376;
      }
    }

    void fission(
        PlantCoreBlock.Core c,
        ReactorMesh m,
        PoseStack ps,
        ReactorRenderers.Sprites S,
        float t,
        float dt,
        boolean lit,
        boolean crit,
        int glow,
        float out) {
      float pump = lit ? (c.coolantFailing ? (Math.sin(t * 0.3) > 0.0 ? 22 : 2) : 26) : 0.0F;
      c.spin[0] = c.spin[0] + dt * pump;
      m.glow(S.white, glow, 1.0F).cylinder(1.4F, 0.8F, 5.0F, 12);
      if (lit) {
        m.add(S.white, glow, 0.25F + 0.1F * ReactorRenderers.wave(t, 0.2F, 0.0F))
            .cylinder(1.47F, 0.9F, 4.9F, 12);
      }

      m.solid(S.steel);

      for (int i = 0; i < 12; i++) {
        if (i % 3 == 0) {
          m.frustum(1.55F, 0.3F, 1.55F, 0.9F, 12, i, i + 1);
          m.frustum(1.55F, 4.9F, 1.55F, 5.4F, 12, i, i + 1);
        } else {
          m.frustum(1.55F, 0.3F, 1.55F, 5.4F, 12, i, i + 1);
        }
      }

      m.solid(S.tung);

      for (float y : new float[] {0.3F, 2.8F, 5.2F}) {
        m.cylinder(1.62F, y, y + 0.2F, 12);
        m.ring(1.5F, 1.62F, y + 0.2F, 12, 1.0F);
        m.ring(1.5F, 1.62F, y, 12, -1.0F);
      }

      m.solid(S.casing).frustum(1.55F, 5.4F, 1.1F, 5.9F, 12);
      m.frustum(1.1F, 5.9F, 0.75F, 6.1F, 12);
      m.ring(0.0F, 0.75F, 6.1F, 12, 1.0F);
      m.solid(S.steel);

      for (int[] q : new int[][] {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}}) {
        m.box(
            q[0] * 1.05F - 0.09F,
            5.55F,
            q[1] * 1.05F - 0.09F,
            q[0] * 1.05F + 0.09F,
            7.6F,
            q[1] * 1.05F + 0.09F);
      }

      m.solid(S.panel).box(-1.2F, 7.6F, -1.2F, 1.2F, 7.85F, 1.2F);

      for (int i = 0; i < 10; i++) {
        double a = (Math.PI * 2) * i / 10.0;
        float x = (float) Math.cos(a) * 0.48F;
        float z = (float) Math.sin(a) * 0.48F;
        float top = 6.35F + c.rodAnim[i] * 1.05F;
        m.solid(S.tung).box(x - 0.06F, 6.1F, z - 0.06F, x + 0.06F, top, z + 0.06F);
        m.glow(S.white, c.rodAnim[i] > 0.5F ? 5570410 : 16726832, 1.0F)
            .box(x - 0.08F, top, z - 0.08F, x + 0.08F, top + 0.1F, z + 0.08F);
      }

      for (int side = 0; side < 2; side++) {
        ps.pushPose();
        ps.mulPose(Axis.YP.rotation((float) Math.PI * side));
        m.solid(S.oxidized).box(1.5F, 4.4F, -0.12F, 2.07F, 4.66F, 0.12F);
        m.solid(S.steel).box(1.55F, 4.33F, -0.17F, 1.68F, 4.73F, 0.17F);
        ps.pushPose();
        ps.translate(1.95F, 0.0F, 0.0F);
        m.solid(S.oxidized).drum(0.3F, 0.3F, 1.25F, 10);
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(c.spin[0] * (side == 0 ? 1 : -1)));
        m.solid(S.steel);

        for (int b = 0; b < 3; b++) {
          ps.pushPose();
          ps.mulPose(Axis.YP.rotationDegrees(b * 60));
          m.box(-0.27F, 1.27F, -0.04F, 0.27F, 1.37F, 0.04F);
          ps.popPose();
        }

        ps.popPose();
        m.solid(S.steel).ring(0.22F, 0.3F, 1.4F, 10, 1.0F);
        m.drum(0.22F, 1.4F, 1.7F, 10);
        m.solid(S.oxidized).cylinder(0.11F, 1.7F, 4.5F, 8);
        m.glow(
                S.white,
                c.coolantFailing && lit
                    ? (t % 16.0F < 8.0F ? 16756768 : 3153928)
                    : (lit ? 5570410 : 2105376),
                1.0F)
            .box(0.21F, 1.5F, -0.04F, 0.24F, 1.6F, 0.04F);
        ps.popPose();
        ps.popPose();
      }

      int a = alarm(c, t, lit, crit);

      for (int[] q : new int[][] {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}}) {
        float x = q[0] * 1.08F;
        float z = q[1] * 1.08F;
        m.glow(S.white, a, 1.0F).box(x - 0.08F, 7.85F, z - 0.08F, x + 0.08F, 8.0F, z + 0.08F);
        if (crit || c.coolantFailing && lit) {
          m.add(S.white, a, 0.4F).cube(x, 7.93F, z, 0.16F);
        }
      }
    }

    void fusion(
        PlantCoreBlock.Core c,
        ReactorMesh m,
        PoseStack ps,
        ReactorRenderers.Sprites S,
        float t,
        float dt,
        boolean lit,
        boolean crit,
        int glow,
        float out) {
      c.spin[0] = c.spin[0] + dt * (lit ? 4.0F + out * 16.0F : 0.0F);
      float yc = 2.5F;
      float R = 1.4F;
      float r = 0.5F;
      m.solid(S.tung);

      for (int q = 0; q < 4; q++) {
        ps.pushPose();
        ps.mulPose(Axis.YP.rotation((float) (-((Math.PI / 4) + q * Math.PI / 2.0))));
        m.box(1.22F, 0.3F, -0.12F, 1.5F, 2.05F, 0.12F);
        ps.popPose();
      }

      ps.pushPose();
      ps.translate(0.0F, yc, 0.0F);
      m.glow(S.white, glow, 1.0F).torus(R, 0.3F, 32, 8, null);
      if (lit) {
        for (int s = 0; s < 3; s++) {
          int from = (int) (c.spin[0] / 360.0F * 48.0F) + s * 16;
          m.glow(S.white, ReactorMesh.mix(glow, 16777215, 0.65F), 1.0F)
              .torus(R, 0.33F, 48, 8, from, from + 5, null);
        }

        m.add(S.white, glow, 0.3F + 0.1F * ReactorRenderers.wave(t, 0.25F, 0.0F))
            .torus(R, 0.4F, 32, 8, null);
      }

      m.solid(S.steel).torus(R, r, 24, 12, (iu, iv) -> iu % 6 < 2 && (iv <= 1 || iv >= 10));
      m.solid(S.copper);

      for (int q = 0; q < 12; q++) {
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(-(q * 30 + 15)));
        float s = 0.6F;
        float w = 0.1F;
        float d = 0.09F;
        m.box(R - s - w, -s - w, -d, R - s, s + w, d);
        m.box(R + s, -s - w, -d, R + s + w, s + w, d);
        m.box(R - s, s, -d, R + s, s + w, d);
        m.box(R - s, -s - w, -d, R + s, -s, d);
        ps.popPose();
      }

      m.solid(S.tung);

      for (float y : new float[] {-0.84F, 0.84F}) {
        ps.pushPose();
        ps.translate(0.0F, y, 0.0F);
        m.torus(R, 0.05F, 32, 6, null);
        ps.popPose();
      }

      ps.popPose();
      m.solid(S.tung).drum(0.42F, 0.3F, 4.7F, 12);

      for (float y : new float[] {1.3F, 2.1F, 2.9F, 3.7F}) {
        m.glow(S.white, glow, lit ? 0.55F + 0.45F * ReactorRenderers.wave(t, 0.2F, y) : 1.0F)
            .cylinder(0.44F, y, y + 0.09F, 12);
      }

      m.solid(S.casing).frustum(0.9F, 4.7F, 0.5F, 5.1F, 12);
      m.ring(0.0F, 0.5F, 5.1F, 12, 1.0F);
      m.solid(S.steel).box(-0.05F, 5.1F, -0.05F, 0.05F, 5.62F, 0.05F);
      m.glow(S.white, alarm(c, t, lit, crit), 1.0F).cube(0.0F, 5.7F, 0.0F, 0.07F);
    }

    void chrono(
        PlantCoreBlock.Core c,
        ReactorMesh m,
        PoseStack ps,
        ReactorRenderers.Sprites S,
        float t,
        float dt,
        boolean lit,
        boolean crit,
        int glow,
        float out) {
      float[] sp =
          new float[] {1.6F + out * 6.0F, 2.2F + out * 8.0F, 1.2F + out * 5.0F, 0.9F + out * 3.0F};

      for (int i = 0; i < 4; i++) {
        c.spin[i] = c.spin[i] + dt * (lit ? sp[i] : (i == 3 ? 0.15F : 0.0F));
      }

      m.solid(S.casing).drum(1.2F, 0.3F, 0.8F, 16);
      m.solid(S.steel).frustum(1.2F, 0.8F, 0.6F, 1.2F, 16);
      m.glow(S.white, glow, 1.0F).ring(0.0F, 0.6F, 1.21F, 16, 1.0F);

      for (int q = 0; q < 4; q++) {
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(-(45 + 90 * q)));
        m.solid(S.tung).box(1.6F, 0.3F, -0.15F, 1.9F, 5.9F, 0.15F);
        m.glow(S.white, glow, lit ? 0.55F + 0.45F * ReactorRenderers.wave(t, 0.15F, q) : 1.0F)
            .box(1.56F, 1.0F, -0.06F, 1.6F, 5.5F, 0.06F);
        m.solid(S.steel).box(0.0F, 5.75F, -0.08F, 1.6F, 5.9F, 0.08F);
        ps.popPose();
      }

      m.solid(S.steel).drum(0.3F, 5.55F, 5.95F, 12);
      m.glow(S.white, glow, 1.0F).frustum(0.25F, 5.55F, 0.04F, 5.15F, 12);
      float cy = 3.3F + 0.08F * (float) Math.sin(t * 0.05F);
      ps.pushPose();
      ps.translate(0.0F, cy, 0.0F);
      ps.mulPose(Axis.YP.rotationDegrees(c.spin[3] * 8.0F));
      m.glow(S.white, glow, lit ? 0.85F : 1.0F).octahedron(0.42F, 0.8F);
      m.glow(S.white, ReactorMesh.mix(glow, 16777215, 0.7F), 1.0F).octahedron(0.2F, 0.42F);
      if (lit) {
        m.add(S.white, glow, 0.3F).octahedron(0.56F, 1.0F);
      }

      ps.popPose();
      float[] rr = new float[] {1.0F, 1.17F, 1.34F};

      for (int g = 0; g < 3; g++) {
        ps.pushPose();
        ps.translate(0.0F, cy, 0.0F);
        if (g == 0) {
          ps.mulPose(Axis.XP.rotationDegrees(c.spin[0] * 10.0F));
        } else if (g == 1) {
          ps.mulPose(Axis.ZP.rotationDegrees(c.spin[1] * 10.0F));
          ps.mulPose(Axis.XP.rotationDegrees(90.0F));
        } else {
          ps.mulPose(Axis.YP.rotationDegrees(c.spin[2] * 10.0F));
          ps.mulPose(Axis.XP.rotationDegrees(60.0F));
        }

        m.solid(S.tung).torus(rr[g], 0.045F, 40, 4, null);

        for (int i = 0; i < 8; i++) {
          double a = (Math.PI * 2) * i / 8.0;
          m.glow(S.white, glow, 1.0F)
              .cube((float) Math.cos(a) * rr[g], 0.0F, (float) Math.sin(a) * rr[g], 0.055F);
        }

        ps.popPose();
      }

      if (crit) {
        m.add(S.white, 16726832, 0.35F * ReactorRenderers.wave(t, 0.6F, 0.0F)).sphere(1.6F, 16, 10);
      }
    }

    void voidCore(
        PlantCoreBlock.Core c,
        ReactorMesh m,
        PoseStack ps,
        ReactorRenderers.Sprites S,
        float t,
        float dt,
        boolean lit,
        boolean crit,
        int glow,
        float out) {
      c.spin[0] = c.spin[0] + dt * (lit ? 4.0F + out * 14.0F : 0.3F);
      c.spin[1] = c.spin[1] + dt * (lit ? 7.0F + out * 22.0F : 0.5F);
      m.solid(S.voidst).drum(1.5F, 0.3F, 0.6F, 16);
      m.frustum(0.9F, 0.6F, 0.35F, 1.0F, 16);
      m.glow(S.white, glow, 1.0F).ring(0.0F, 0.35F, 1.01F, 12, 1.0F);
      m.solid(S.voidst).drum(1.5F, 6.2F, 6.5F, 16);
      m.frustum(0.9F, 6.2F, 0.35F, 5.8F, 16);
      m.glow(S.white, glow, 1.0F).ring(0.0F, 0.35F, 5.79F, 12, -1.0F);

      for (int q = 0; q < 6; q++) {
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(-q * 60));
        m.solid(S.voidst).box(1.5F, 0.6F, -0.1F, 1.7F, 6.2F, 0.1F);
        float fl =
            lit
                ? ((ReactorRenderers.hash((long) (t / 2.0F) * 7L + q) & 3L) == 0L ? 0.35F : 0.9F)
                : 1.0F;
        m.glow(S.white, lit ? glow : ReactorMesh.scale(glow, 0.6F), fl)
            .box(1.46F, 1.2F, -0.04F, 1.5F, 5.6F, 0.04F);
        ps.popPose();
      }

      float sy = 3.4F + 0.06F * (float) Math.sin(t * 0.07F);
      if (lit) {
        float b = 0.45F + 0.35F * ReactorRenderers.wave(t, 0.9F, 0.0F);
        m.glow(S.white, ReactorMesh.mix(glow, 16777215, 0.3F), b)
            .beam(0.0F, 1.0F, 0.0F, 0.0F, sy - 0.5F, 0.0F, 0.06F);
        m.glow(S.white, ReactorMesh.mix(glow, 16777215, 0.3F), b)
            .beam(0.0F, sy + 0.5F, 0.0F, 0.0F, 5.8F, 0.0F, 0.06F);
      }

      ps.pushPose();
      ps.translate(0.0F, sy, 0.0F);
      m.lit(S.black, 1579032).sphere(0.5F, 16, 10);
      m.glow(S.white, glow, lit ? 0.35F : 0.15F).sphere(0.6F, 16, 10);
      if (lit) {
        m.add(S.white, glow, 0.22F + 0.1F * ReactorRenderers.wave(t, 0.3F, 0.0F))
            .sphere(0.8F, 16, 10);
      }

      ps.pushPose();
      ps.mulPose(Axis.XP.rotationDegrees(18.0F));
      ps.mulPose(Axis.YP.rotationDegrees(c.spin[0]));

      for (int i = 0; i < 36; i++) {
        float a = (float) (0.5 + 0.5 * Math.sin(i * Math.PI * 2.0 / 36.0 * 3.0));
        m.glow(
                S.white,
                ReactorMesh.mix(glow, 16777215, 0.15F),
                lit ? 0.25F + 0.55F * a : 0.08F + 0.1F * a)
            .ring(0.82F, 1.4F, 0.0F, 36, 1.0F, i, i + 1);
      }

      ps.popPose();
      ps.pushPose();
      ps.mulPose(Axis.XP.rotationDegrees(18.0F));
      ps.mulPose(Axis.YP.rotationDegrees(c.spin[1]));

      for (int i = 0; i < 24; i++) {
        float a = (float) (0.5 + 0.5 * Math.sin(i * Math.PI * 2.0 / 24.0 * 2.0));
        m.glow(S.white, ReactorMesh.mix(glow, 16777215, 0.6F), lit ? 0.45F + 0.45F * a : 0.12F)
            .ring(0.6F, 0.84F, 0.0F, 24, 1.0F, i, i + 1);
      }

      ps.popPose();
      ps.popPose();
      if (lit) {
        long seed = (long) (t / 3.0F);

        for (int a = 0; a < 3; a++) {
          long h = ReactorRenderers.hash(seed * 31L + a + c.getBlockPos().asLong());
          if ((h & 3L) != 0L) {
            int q = (int) Math.floorMod(h >> 4, 6L);
            double ang = q * Math.PI / 3.0;
            float cx = (float) Math.cos(ang);
            float cz = (float) Math.sin(ang);
            float ty = 1.4F + (float) (h >> 8 & 255L) / 255.0F * 3.8F;
            float px = cx * 0.55F;
            float py = sy;
            float pz = cz * 0.55F;
            m.glow(S.white, ReactorMesh.mix(glow, 16777215, 0.55F), 1.0F);

            for (int s = 1; s <= 5; s++) {
              float f = s / 5.0F;
              long j = ReactorRenderers.hash(h + s);
              float jx = s == 5 ? 0.0F : ((float) (j & 255L) / 255.0F - 0.5F) * 0.3F;
              float jy = s == 5 ? 0.0F : ((float) (j >> 8 & 255L) / 255.0F - 0.5F) * 0.3F;
              float jz = s == 5 ? 0.0F : ((float) (j >> 16 & 255L) / 255.0F - 0.5F) * 0.3F;
              float nx = cx * (0.55F + 0.91F * f) + jx;
              float ny = sy + (ty - sy) * f + jy;
              float nz = cz * (0.55F + 0.91F * f) + jz;
              m.beam(px, py, pz, nx, ny, nz, 0.022F);
              px = nx;
              py = ny;
              pz = nz;
            }
          }
        }
      }

      if (crit) {
        m.add(S.white, 16726832, 0.3F * ReactorRenderers.wave(t, 0.6F, 0.0F)).sphere(1.4F, 16, 10);
      }
    }
  }

  record Sprites(
      TextureAtlasSprite steel,
      TextureAtlasSprite panel,
      TextureAtlasSprite casing,
      TextureAtlasSprite hazard,
      TextureAtlasSprite grate,
      TextureAtlasSprite tung,
      TextureAtlasSprite voidst,
      TextureAtlasSprite copper,
      TextureAtlasSprite oxidized,
      TextureAtlasSprite white,
      TextureAtlasSprite black) {
    static ReactorRenderers.Sprites get() {
      return new ReactorRenderers.Sprites(
          ReactorMesh.sprite("timelocked:block/steel_block"),
          ReactorMesh.sprite("timelocked:block/titanium_panel"),
          ReactorMesh.sprite("timelocked:block/sarc_containment_casing"),
          ReactorMesh.sprite("timelocked:block/sarc_hazard_trim"),
          ReactorMesh.sprite("timelocked:block/steel_grating"),
          ReactorMesh.sprite("timelocked:block/tungsten_steel_block"),
          ReactorMesh.sprite("timelocked:block/voidsteel_block"),
          ReactorMesh.sprite("minecraft:block/copper_block"),
          ReactorMesh.sprite("minecraft:block/oxidized_copper"),
          ReactorMesh.sprite("minecraft:block/white_concrete"),
          ReactorMesh.sprite("minecraft:block/black_concrete"));
    }
  }
}
