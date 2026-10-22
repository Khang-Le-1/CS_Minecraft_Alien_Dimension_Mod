package com.timelocked.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.function.BiPredicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

final class ReactorMesh {
  static final RenderType SOLID = RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS);
  static final RenderType GLOW = RenderType.entityTranslucentEmissive(InventoryMenu.BLOCK_ATLAS);
  static final RenderType ADD = RenderType.eyes(InventoryMenu.BLOCK_ATLAS);
  final PoseStack ps;
  final MultiBufferSource buf;
  final int ambient;
  VertexConsumer vc;
  TextureAtlasSprite sp;
  int col;
  int light;

  ReactorMesh(PoseStack ps, MultiBufferSource buf, int ambient) {
    this.ps = ps;
    this.buf = buf;
    this.ambient = ambient;
  }

  static TextureAtlasSprite sprite(String id) {
    return Minecraft.getInstance() == null
        ? null
        : (TextureAtlasSprite)
            Minecraft.getInstance()
                .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(ResourceLocation.parse(id));
  }

  ReactorMesh solid(TextureAtlasSprite s) {
    return this.solid(s, 16777215);
  }

  ReactorMesh solid(TextureAtlasSprite s, int rgb) {
    this.vc = this.buf.getBuffer(SOLID);
    this.sp = s;
    this.col = 0xFF000000 | rgb;
    this.light = this.ambient;
    return this;
  }

  ReactorMesh lit(TextureAtlasSprite s, int rgb) {
    this.vc = this.buf.getBuffer(SOLID);
    this.sp = s;
    this.col = 0xFF000000 | rgb;
    this.light = 15728880;
    return this;
  }

  ReactorMesh glow(TextureAtlasSprite s, int rgb, float a) {
    this.vc = this.buf.getBuffer(GLOW);
    this.sp = s;
    this.col = Math.clamp((int) (a * 255.0F), 0, 255) << 24 | rgb & 16777215;
    this.light = 15728880;
    return this;
  }

  ReactorMesh add(TextureAtlasSprite s, int rgb, float a) {
    this.vc = this.buf.getBuffer(ADD);
    this.sp = s;
    this.col = 0xFF000000 | scale(rgb, Math.clamp(a, 0.0F, 1.0F));
    this.light = 15728880;
    return this;
  }

  void v(float x, float y, float z, float u, float w, float nx, float ny, float nz) {
    Pose p = this.ps.last();
    this.vc
        .addVertex(p, x, y, z)
        .setColor(this.col)
        .setUv(this.sp == null ? u : this.sp.getU(u), this.sp == null ? w : this.sp.getV(w))
        .setOverlay(OverlayTexture.NO_OVERLAY)
        .setLight(this.light)
        .setNormal(p, nx, ny, nz);
  }

  void face(
      float ox,
      float oy,
      float oz,
      float ax,
      float ay,
      float az,
      float bx,
      float by,
      float bz,
      float nx,
      float ny,
      float nz) {
    float la = (float) Math.sqrt(ax * ax + ay * ay + az * az);
    float lb = (float) Math.sqrt(bx * bx + by * by + bz * bz);
    int na = Math.max(1, (int) Math.ceil(la - 0.01));
    int nb = Math.max(1, (int) Math.ceil(lb - 0.01));
    float ua = Math.min(1.0F, la / na);
    float ub = Math.min(1.0F, lb / nb);

    for (int i = 0; i < na; i++) {
      for (int j = 0; j < nb; j++) {
        float s0 = (float) i / na;
        float s1 = (float) (i + 1) / na;
        float t0 = (float) j / nb;
        float t1 = (float) (j + 1) / nb;
        this.v(
            ox + ax * s0 + bx * t0,
            oy + ay * s0 + by * t0,
            oz + az * s0 + bz * t0,
            0.0F,
            ub,
            nx,
            ny,
            nz);
        this.v(
            ox + ax * s1 + bx * t0,
            oy + ay * s1 + by * t0,
            oz + az * s1 + bz * t0,
            ua,
            ub,
            nx,
            ny,
            nz);
        this.v(
            ox + ax * s1 + bx * t1,
            oy + ay * s1 + by * t1,
            oz + az * s1 + bz * t1,
            ua,
            0.0F,
            nx,
            ny,
            nz);
        this.v(
            ox + ax * s0 + bx * t1,
            oy + ay * s0 + by * t1,
            oz + az * s0 + bz * t1,
            0.0F,
            0.0F,
            nx,
            ny,
            nz);
      }
    }
  }

  void box(float x0, float y0, float z0, float x1, float y1, float z1) {
    float w = x1 - x0;
    float h = y1 - y0;
    float d = z1 - z0;
    this.face(x0, y0, z1, w, 0.0F, 0.0F, 0.0F, h, 0.0F, 0.0F, 0.0F, 1.0F);
    this.face(x1, y0, z0, -w, 0.0F, 0.0F, 0.0F, h, 0.0F, 0.0F, 0.0F, -1.0F);
    this.face(x1, y0, z1, 0.0F, 0.0F, -d, 0.0F, h, 0.0F, 1.0F, 0.0F, 0.0F);
    this.face(x0, y0, z0, 0.0F, 0.0F, d, 0.0F, h, 0.0F, -1.0F, 0.0F, 0.0F);
    this.face(x0, y1, z1, w, 0.0F, 0.0F, 0.0F, 0.0F, -d, 0.0F, 1.0F, 0.0F);
    this.face(x0, y0, z0, w, 0.0F, 0.0F, 0.0F, 0.0F, d, 0.0F, -1.0F, 0.0F);
  }

  void cube(float x, float y, float z, float s) {
    this.box(x - s, y - s, z - s, x + s, y + s, z + s);
  }

  void frustum(float r0, float y0, float r1, float y1, int sides) {
    this.frustum(r0, y0, r1, y1, sides, 0, sides);
  }

  void frustum(float r0, float y0, float r1, float y1, int sides, int from, int to) {
    float slant = (float) Math.hypot(r1 - r0, y1 - y0);
    int rings = Math.max(1, (int) Math.ceil(slant - 0.01));
    float ny0 = r0 - r1;
    float nh = y1 - y0;
    float nl = (float) Math.hypot(ny0, nh);
    if (nl < 1.0E-5) {
      nl = 1.0F;
      nh = 0.0F;
      ny0 = y1 >= y0 ? 1.0F : -1.0F;
    }

    for (int i = from; i < to; i++) {
      double a0 = (Math.PI * 2) * i / sides;
      double a1 = (Math.PI * 2) * (i + 1) / sides;
      double am = (a0 + a1) / 2.0;
      float c0 = (float) Math.cos(a0);
      float s0 = (float) Math.sin(a0);
      float c1 = (float) Math.cos(a1);
      float s1 = (float) Math.sin(a1);
      float cm = (float) Math.cos(am);
      float sm = (float) Math.sin(am);
      float nx = cm * nh / nl;
      float nz = sm * nh / nl;
      float ny = ny0 / nl;
      float chord = (float) (2.0F * Math.max(r0, r1) * Math.sin(Math.PI / sides));
      float uu = Math.min(1.0F, chord);

      for (int k = 0; k < rings; k++) {
        float ta = (float) k / rings;
        float tb = (float) (k + 1) / rings;
        float ra = r0 + (r1 - r0) * ta;
        float rb = r0 + (r1 - r0) * tb;
        float ya = y0 + (y1 - y0) * ta;
        float yb = y0 + (y1 - y0) * tb;
        float vv = Math.min(1.0F, slant / rings);
        this.v(ra * c0, ya, ra * s0, 0.0F, vv, nx, ny, nz);
        this.v(ra * c1, ya, ra * s1, uu, vv, nx, ny, nz);
        this.v(rb * c1, yb, rb * s1, uu, 0.0F, nx, ny, nz);
        this.v(rb * c0, yb, rb * s0, 0.0F, 0.0F, nx, ny, nz);
      }
    }
  }

  void cylinder(float r, float y0, float y1, int sides) {
    this.frustum(r, y0, r, y1, sides);
  }

  void drum(float r, float y0, float y1, int sides) {
    this.cylinder(r, y0, y1, sides);
    this.ring(0.0F, r, y1, sides, 1.0F);
    this.ring(0.0F, r, y0, sides, -1.0F);
  }

  void ring(float rin, float rout, float y, int sides, float ny) {
    this.ring(rin, rout, y, sides, ny, 0, sides);
  }

  void ring(float rin, float rout, float y, int sides, float ny, int from, int to) {
    for (int i = from; i < to; i++) {
      double a0 = (Math.PI * 2) * i / sides;
      double a1 = (Math.PI * 2) * (i + 1) / sides;
      float c0 = (float) Math.cos(a0);
      float s0 = (float) Math.sin(a0);
      float c1 = (float) Math.cos(a1);
      float s1 = (float) Math.sin(a1);
      float uu = Math.min(1.0F, rout - rin);
      float vv = Math.min(1.0F, (float) (2.0F * rout * Math.sin(Math.PI / sides)));
      this.v(rin * c0, y, rin * s0, 0.0F, 0.0F, 0.0F, ny, 0.0F);
      this.v(rout * c0, y, rout * s0, uu, 0.0F, 0.0F, ny, 0.0F);
      this.v(rout * c1, y, rout * s1, uu, vv, 0.0F, ny, 0.0F);
      this.v(rin * c1, y, rin * s1, 0.0F, vv, 0.0F, ny, 0.0F);
    }
  }

  void dome(float r, float y0, float height, int sides, int stacks, float topR) {
    for (int k = 0; k < stacks; k++) {
      double a0 = (Math.PI / 2) * k / stacks;
      double a1 = (Math.PI / 2) * (k + 1) / stacks;
      float ra = (float) (r * Math.cos(a0));
      float rb = Math.max(topR, (float) (r * Math.cos(a1)));
      this.frustum(
          ra,
          y0 + (float) (height * Math.sin(a0)),
          rb,
          y0 + (float) (height * Math.sin(a1)),
          sides);
      if (rb <= topR) {
        break;
      }
    }
  }

  void torus(float R, float r, int su, int sv, BiPredicate<Integer, Integer> skip) {
    this.torus(R, r, su, sv, 0, su, skip);
  }

  void torus(
      float R, float r, int su, int sv, int from, int to, BiPredicate<Integer, Integer> skip) {
    float uu = Math.min(1.0F, (float) ((Math.PI * 2) * R / su));
    float vv = Math.min(1.0F, (float) ((Math.PI * 2) * r / sv));

    for (int iu = from; iu < to; iu++) {
      for (int iv = 0; iv < sv; iv++) {
        if (skip == null || !skip.test(Math.floorMod(iu, su), iv)) {
          this.tv(R, r, iu, iv, su, sv, 0.0F, vv);
          this.tv(R, r, iu + 1, iv, su, sv, uu, vv);
          this.tv(R, r, iu + 1, iv + 1, su, sv, uu, 0.0F);
          this.tv(R, r, iu, iv + 1, su, sv, 0.0F, 0.0F);
        }
      }
    }
  }

  private void tv(float R, float r, int iu, int iv, int su, int sv, float u, float w) {
    double a = (Math.PI * 2) * iu / su;
    double b = (Math.PI * 2) * iv / sv;
    float ca = (float) Math.cos(a);
    float sa = (float) Math.sin(a);
    float cb = (float) Math.cos(b);
    float sb = (float) Math.sin(b);
    this.v((R + r * cb) * ca, r * sb, (R + r * cb) * sa, u, w, cb * ca, sb, cb * sa);
  }

  void sphere(float r, int su, int sv) {
    for (int iu = 0; iu < su; iu++) {
      for (int iv = 0; iv < sv; iv++) {
        this.sv(r, iu, iv, su, sv, 0.0F, 1.0F);
        this.sv(r, iu + 1, iv, su, sv, 1.0F, 1.0F);
        this.sv(r, iu + 1, iv + 1, su, sv, 1.0F, 0.0F);
        this.sv(r, iu, iv + 1, su, sv, 0.0F, 0.0F);
      }
    }
  }

  private void sv(float r, int iu, int iv, int su, int sv, float u, float w) {
    double a = (Math.PI * 2) * iu / su;
    double b = Math.PI * iv / sv - (Math.PI / 2);
    float x = (float) (Math.cos(b) * Math.cos(a));
    float y = (float) Math.sin(b);
    float z = (float) (Math.cos(b) * Math.sin(a));
    this.v(x * r, y * r, z * r, u * 0.5F, w * 0.5F, x, y, z);
  }

  void octahedron(float w, float h) {
    float[][] eq =
        new float[][] {{w, 0.0F, 0.0F}, {0.0F, 0.0F, w}, {-w, 0.0F, 0.0F}, {0.0F, 0.0F, -w}};

    for (int i = 0; i < 4; i++) {
      float[] a = eq[i];
      float[] b = eq[(i + 1) % 4];

      for (int s = -1; s <= 1; s += 2) {
        float nx = (a[0] + b[0]) / 2.0F;
        float nz = (a[2] + b[2]) / 2.0F;
        float ny = s * w * 0.7F;
        float l = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        nx /= l;
        ny /= l;
        nz /= l;
        this.v(a[0], 0.0F, a[2], 0.0F, 1.0F, nx, ny, nz);
        this.v(b[0], 0.0F, b[2], 1.0F, 1.0F, nx, ny, nz);
        this.v(0.0F, s * h, 0.0F, 0.5F, 0.0F, nx, ny, nz);
        this.v(0.0F, s * h, 0.0F, 0.5F, 0.0F, nx, ny, nz);
      }
    }
  }

  void beam(float ax, float ay, float az, float bx, float by, float bz, float w) {
    float dx = bx - ax;
    float dy = by - ay;
    float dz = bz - az;
    float[] up =
        Math.abs(dy) > 0.9F * Math.sqrt(dx * dx + dy * dy + dz * dz)
            ? new float[] {1.0F, 0.0F, 0.0F}
            : new float[] {0.0F, 1.0F, 0.0F};
    float sx = dy * up[2] - dz * up[1];
    float sy = dz * up[0] - dx * up[2];
    float sz = dx * up[1] - dy * up[0];
    float l = (float) Math.sqrt(sx * sx + sy * sy + sz * sz);
    sx *= w / l;
    sy *= w / l;
    sz *= w / l;
    float tx = dy * sz - dz * sy;
    float ty = dz * sx - dx * sz;
    float tz = dx * sy - dy * sx;
    l = (float) Math.sqrt(tx * tx + ty * ty + tz * tz);
    tx *= w / l;
    ty *= w / l;
    tz *= w / l;

    for (float[] s : new float[][] {{sx, sy, sz}, {tx, ty, tz}}) {
      this.v(ax - s[0], ay - s[1], az - s[2], 0.0F, 1.0F, 0.0F, 1.0F, 0.0F);
      this.v(ax + s[0], ay + s[1], az + s[2], 1.0F, 1.0F, 0.0F, 1.0F, 0.0F);
      this.v(bx + s[0], by + s[1], bz + s[2], 1.0F, 0.0F, 0.0F, 1.0F, 0.0F);
      this.v(bx - s[0], by - s[1], bz - s[2], 0.0F, 0.0F, 0.0F, 1.0F, 0.0F);
    }
  }

  static int mix(int a, int b, float t) {
    t = Math.clamp(t, 0.0F, 1.0F);
    int r = (int) ((a >> 16 & 0xFF) * (1.0F - t) + (b >> 16 & 0xFF) * t);
    int g = (int) ((a >> 8 & 0xFF) * (1.0F - t) + (b >> 8 & 0xFF) * t);
    int bl = (int) ((a & 0xFF) * (1.0F - t) + (b & 0xFF) * t);
    return r << 16 | g << 8 | bl;
  }

  static int scale(int c, float f) {
    return mix(0, c, f);
  }
}
