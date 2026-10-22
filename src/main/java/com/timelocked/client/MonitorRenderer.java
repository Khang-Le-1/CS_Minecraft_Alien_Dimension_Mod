package com.timelocked.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.timelocked.MonitorContent;
import com.timelocked.SpacePlanets;
import com.timelocked.Timeskipper;
import com.timelocked.TimeskipperBridgeScreens;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Font.DisplayMode;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public final class MonitorRenderer implements BlockEntityRenderer<MonitorContent.MonitorEntity> {
  private static final int[] COLORS = new int[] {8382719, 16757575, 16730698, 16769162, 13999359};
  private static final String NOISE = "#%&@$?!/\\<>*";
  private final Font font;

  public MonitorRenderer(Context c) {
    this.font = c.getFont();
  }

  private static final float PLATE_Z = 0.004F;
  private static final float TEXT_Z = 0.015F;

  public void render(
      MonitorContent.MonitorEntity m,
      float partial,
      PoseStack pose,
      MultiBufferSource buffers,
      int light,
      int overlay) {
    BlockState state = m.getBlockState();
    if (!state.hasProperty(MonitorContent.MonitorBlock.FACING)) return;
    Direction facing = (Direction) state.getValue(MonitorContent.MonitorBlock.FACING);
    boolean stand = (Boolean) state.getValue(MonitorContent.MonitorBlock.STAND);
    boolean ship = m.getLevel() != null && m.getLevel().dimension().equals(SpacePlanets.SHIP);
    long time = m.getLevel() == null ? 0L : m.getLevel().getGameTime();
    float t = time + partial;
    int theme = m.theme;
    int count = 0;
    for (int i = 0; i < 4; i++) {
      if (!m.line(i).isEmpty()) count = i + 1;
    }

    int gw = 1;
    int gh = 1;
    if (!stand) {
      int[] g = this.group(m, facing, time);
      if (g[3] == 0) return; // another monitor in this display wall draws the whole screen
      gw = g[1];
      gh = g[2];
    }

    // A blank anchor can still own a wall whose data is on another member.
    if (count == 0 && gw * gh > 1 && m.getLevel() != null) {
      Direction right = facing.getCounterClockWise();
      outer:
      for (int u = 0; u < gw; u++)
        for (int v = 0; v < gh; v++) {
          if (m.getLevel().getBlockEntity(m.getBlockPos().relative(right, u).above(v))
              instanceof MonitorContent.MonitorEntity other)
            for (int j = 0; j < 4; j++)
              if (!other.line(j).isEmpty()) {
                m = other;
                count = 4;
                theme = other.theme;
                break outer;
              }
        }
    }
    if (gw * gh > 1) {
      pose.pushPose();
      pose.translate(0.5, 0.5, 0.5);
      pose.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
      pose.translate(0.0, 0.0, -0.375);
      pose.translate((gw - 1) * 0.5, (gh - 1) * 0.5, 0.0);
      this.housing(pose, buffers, gw, gh, light);
      this.renderWall(m, pose, buffers, ship, theme, gw, gh, t, count);
      pose.popPose();
      return;
    }

    float hw = stand ? .35F : .435F;
    float hh = stand ? .23F : .425F;
    pose.pushPose();
    pose.translate(0.5, 0.5, 0.5);
    pose.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
    if (ship) {
      pose.translate(0.0, stand ? 0.125 : 0.0, stand ? 0.071 : -0.375);
    } else {
      pose.translate(0.0, stand ? 0.125 : 0.0, stand ? 0.071 : -0.375);
    }

    if (!stand) this.housing(pose, buffers, 1, 1, light);
    this.skin(
        pose,
        buffers,
        ship ? (theme == 2 ? 0xFF726A : 0xE7C77A) : COLORS[Math.floorMod(theme, COLORS.length)],
        theme == 2,
        hw,
        hh,
        t,
        0.092F);
    long seed = m.getBlockPos().asLong() * 31L + time / 3L;
    float maxW = 2 * hw - 0.028F - 0.07F;
    float bodyTop = hh - 0.115F;
    float bodyBottom = -hh + 0.022F;
    float slot = (bodyTop - bodyBottom) / 3.0F;

    for (int i = 0; i < count; i++) {
      String text = ship ? shortLine(m.line(i)) : m.line(i);
      if (text.isEmpty()) continue;
      int color;
      float dx = 0.0F;
      if (ship) {
        color = i == 0 ? (theme == 2 ? 0xFF726A : 0xE7C77A) : 0xBEEFF5;
      } else {
        color = COLORS[Math.floorMod(theme, COLORS.length)];
        if (theme == 2) {
          long h = mix(seed + i * 977);
          if ((h & 15L) == 0L) dx = ((h >> 4 & 7L) - 3.5F) * 0.004F;
          if ((h >> 8 & 31L) == 0L) continue;
          if ((h >> 13 & 7L) == 0L) {
            char[] c = text.toCharArray();
            c[Math.floorMod(h >> 16, c.length)] =
                NOISE.charAt(Math.floorMod(h >> 24, NOISE.length()));
            text = new String(c);
          }
        }

        if (theme == 4) color = scaleColor(color, 0.75F + 0.25F * (float) Math.sin(t * 0.05));
        if (i > 0 && theme != 4) color = scaleColor(color, 0.86F);
      }

      if (i == 0) {
        this.textLeft(text, -hw + 0.03F + dx, hh - 0.0495F, 0.062F, maxW, color, pose, buffers);
      } else {
        // Body lines sit in three even slots under the header. A blinking cursor trails the last
        // line.
        float y = bodyTop - slot * (i - 0.5F);
        String shown = i == count - 1 && ((int) (t / 10.0F) & 1) == 0 && !ship ? text + " _" : text;
        this.textLeft(
            shown,
            -hw + 0.034F + dx,
            y,
            Math.min(slot * 0.78F, 0.075F),
            maxW,
            color,
            pose,
            buffers);
      }
    }

    pose.popPose();
  }

  @Override
  public boolean shouldRenderOffScreen(MonitorContent.MonitorEntity m) {
    // A merged display can remain on screen while its bottom-left owner is outside the frustum.
    return !m.getBlockState().getValue(MonitorContent.MonitorBlock.STAND);
  }

  private final java.util.HashMap<Long, CachedWall> wallCache = new java.util.HashMap<>();
  private net.minecraft.world.level.Level cachedLevel;

  private record CachedWall(long tick, com.timelocked.MonitorWallLayout.Wall wall) {}

  private int[] group(MonitorContent.MonitorEntity m, Direction facing, long time) {
    if (cachedLevel != m.getLevel()) {
      wallCache.clear();
      cachedLevel = m.getLevel();
    }
    long key = m.getBlockPos().asLong();
    var c = wallCache.get(key);
    if (c == null || time < c.tick() || time - c.tick() >= 5) {
      if (wallCache.size() > 512) wallCache.clear();
      c =
          new CachedWall(
              time, com.timelocked.MonitorWallLayout.find(m.getLevel(), m.getBlockPos(), facing));
      wallCache.put(key, c);
    }
    var g = c.wall();
    return new int[] {0, g.width(), g.height(), g.anchor().equals(m.getBlockPos()) ? 1 : 0};
  }

  private void housing(
      PoseStack pose, MultiBufferSource buffers, int width, int height, int light) {
    var sprite = ReactorMesh.sprite("timelocked:block/timeskipper_brushed_metal");
    if (sprite == null) return;
    ReactorMesh mesh = new ReactorMesh(pose, buffers, light);
    float x = width * .5F, y = height * .5F, b = .045F;
    mesh.solid(sprite, 0x87949A).box(-x, -y, -.015F, x, -y + b, .003F);
    mesh.box(-x, y - b, -.015F, x, y, .003F);
    mesh.box(-x, -y + b, -.015F, -x + b, y - b, .003F);
    mesh.box(x - b, -y + b, -.015F, x, y - b, .003F);
    for (float px : new float[] {-x + .023F, x - .023F})
      for (float py : new float[] {-y + .023F, y - .023F})
        mesh.solid(sprite, 0x293038)
            .box(px - .009F, py - .009F, .0031F, px + .009F, py + .009F, .004F);
  }

  /**
   * Display wall: a large panel with a title bar, readout text on the left and a live bar graph on
   * the right.
   */
  private void renderWall(
      MonitorContent.MonitorEntity m,
      PoseStack pose,
      MultiBufferSource buffers,
      boolean ship,
      int theme,
      int gw,
      int gh,
      float t,
      int count) {
    float hw = gw * 0.5F - 0.06F;
    float hh = gh * 0.5F - 0.07F;
    if (theme == MonitorContent.HOME_TV || theme == MonitorContent.HOME_TV_OFF) {
      television(pose, buffers, hw, hh, t, theme == MonitorContent.HOME_TV_OFF);
      return;
    }
    if (theme == MonitorContent.SCC_PLAQUE) {
      var sprite = ReactorMesh.sprite("minecraft:block/white_concrete");
      if (sprite == null) return;
      var mesh = new ReactorMesh(pose, buffers, 15728880);
      mesh.lit(sprite, 0xA5A7AA).box(-hw, -hh, PLATE_Z, hw, hh, PLATE_Z + .002F);
      mesh.lit(sprite, 0xF2F0E8)
          .box(-hw + .08F, -hh + .08F, PLATE_Z + .003F, hw - .08F, hh - .08F, PLATE_Z + .004F);
      textLeft(
          "Seaton Computing Center", -hw + .25F, .20F, .40F, 2 * hw - .5F, 0x343C43, pose, buffers);
      textLeft("SCC", -hw + .25F, -.38F, .29F, 2 * hw - .5F, 0x555B62, pose, buffers);
      return;
    }
    boolean alarm = theme == 2;
    int accent =
        ship
            ? (alarm ? 0xFF726A : (theme == 1 ? 0x7FD6FF : 0xE7C77A))
            : COLORS[Math.floorMod(theme, COLORS.length)];
    float hdr = Math.min(0.16F, 0.092F * (0.6F + hh * 0.9F));
    this.skin(pose, buffers, accent, alarm, hw, hh, t, hdr);

    float splitX = hw * 0.30F;
    float px0 = splitX + 0.05F;
    float px1 = hw - 0.07F;
    float py0 = -hh + 0.08F;
    float py1 = hh - hdr - 0.08F;
    var sprite = ReactorMesh.sprite("minecraft:block/white_concrete");
    if (sprite != null && px1 - px0 > 0.25F && py1 - py0 > 0.15F) {
      ReactorMesh mesh = new ReactorMesh(pose, buffers, 15728880);
      mesh.lit(sprite, 0x0E1A22).box(px0, py0, PLATE_Z + 0.0016F, px1, py1, PLATE_Z + 0.0022F);
      int bars = Math.max(6, Math.min(18, gw * 3 + 2));
      float bw = (px1 - px0 - 0.04F) / bars;
      for (int i = 0; i < bars; i++) {
        float f =
            0.22F + 0.62F * (0.5F + 0.5F * (float) Math.sin(t * 0.06F + i * 1.7F + (i % 3) * 0.9F));
        float x0 = px0 + 0.02F + i * bw + bw * 0.12F;
        float x1 = x0 + bw * 0.76F;
        float top = py0 + 0.02F + (py1 - py0 - 0.04F) * f;
        mesh.lit(sprite, scaleColor(accent, alarm ? 0.8F : 0.5F))
            .box(x0, py0 + 0.02F, PLATE_Z + 0.0024F, x1, top, PLATE_Z + 0.0032F);
        mesh.lit(sprite, accent)
            .box(x0, top - 0.012F, PLATE_Z + 0.0033F, x1, top, PLATE_Z + 0.0038F);
      }

      mesh.lit(sprite, scaleColor(accent, 0.7F))
          .box(px0, py0, PLATE_Z + 0.0034F, px1, py0 + 0.008F, PLATE_Z + 0.0040F);
    }

    float left = -hw + 0.06F;
    float textW = (splitX - 0.04F) - left;
    int body = ship ? (alarm ? 0xFF9A93 : 0xBEEFF5) : scaleColor(accent, 0.9F);
    this.textLeft(
        m.line(0), left, hh - hdr * 0.5F, hdr * 0.62F, 2 * hw - 0.2F, accent, pose, buffers);
    float bodyTop = hh - hdr - 0.07F;
    float bodyBottom = -hh + 0.08F;
    float slot = (bodyTop - bodyBottom) / 3.0F;
    for (int i = 1; i < count; i++) {
      String text = m.line(i);
      if (text.isEmpty()) continue;
      if (i == count - 1 && ((int) (t / 10.0F) & 1) == 0) text = text + " _";
      this.textLeft(
          text,
          left,
          bodyTop - slot * (i - 0.5F),
          Math.min(slot * 0.62F, 0.16F),
          textW,
          body,
          pose,
          buffers);
    }
  }

  /** One continuous broadcast panel, no cell bezels or duplicated text. */
  private void television(
      PoseStack pose, MultiBufferSource buffers, float hw, float hh, float t, boolean off) {
    var sprite = ReactorMesh.sprite("minecraft:block/white_concrete");
    if (sprite == null) return;
    var mesh = new ReactorMesh(pose, buffers, 15728880);
    mesh.lit(sprite, 0x06090D).box(-hw, -hh, PLATE_Z, hw, hh, PLATE_Z + .002F);
    mesh.lit(sprite, off ? 0xB83E39 : 0x78C9B2)
        .box(hw - .12F, -hh + .04F, .008F, hw - .08F, -hh + .06F, .009F);
    if (off) return;
    int[] bars = {0xC6D4CD, 0xB8AA58, 0x55AAA9, 0x619D68, 0xA076A6, 0xAC6464, 0x4F668E};
    float barW = (2 * hw - .16F) / bars.length;
    for (int i = 0; i < bars.length; i++)
      mesh.lit(sprite, bars[i])
          .box(
              -hw + .08F + i * barW,
              -hh * .5F,
              .007F,
              -hw + .08F + (i + 1) * barW,
              hh * .45F,
              .008F);
    textLeft("LOCAL BROADCAST", -hw + .15F, hh - .25F, .18F, 2 * hw - .3F, 0xE6F2F2, pose, buffers);
    textLeft(
        "LATE NIGHT  /  RAIN", -hw + .15F, -hh + .25F, .12F, 2 * hw - .3F, 0xAEC4D2, pose, buffers);
    float sweep = -hh * .5F + (t % 100) / 100F * (hh * .95F);
    mesh.lit(sprite, 0xC6D8D6).box(-hw + .08F, sweep, .0085F, hw - .08F, sweep + .006F, .009F);
  }

  /**
   * Dark glass plate, steel bezel, accent header, status LED, scanlines and a slow sweep. Drawn on
   * top of the block's screen face.
   */
  private void skin(
      PoseStack pose,
      MultiBufferSource buffers,
      int accent,
      boolean flashing,
      float hw,
      float hh,
      float t,
      float hdr) {
    var s = ReactorMesh.sprite("minecraft:block/white_concrete");
    if (s == null) return;
    ReactorMesh m = new ReactorMesh(pose, buffers, 15728880);
    float z = PLATE_Z;
    float b = 0.007F;
    m.lit(s, 0x0A1116).box(-hw, -hh, z, hw, hh, z + 0.0015F);
    int steel = 0x304550;
    m.lit(s, steel).box(-hw, hh - b, z, hw, hh, z + 0.0025F);
    m.lit(s, steel).box(-hw, -hh, z, hw, -hh + b, z + 0.0025F);
    m.lit(s, steel).box(-hw, -hh, z, -hw + b, hh, z + 0.0025F);
    m.lit(s, steel).box(hw - b, -hh, z, hw, hh, z + 0.0025F);
    m.lit(s, scaleColor(accent, 0.22F))
        .box(-hw + b, hh - hdr, z + 0.0016F, hw - b, hh - b, z + 0.0026F);
    m.lit(s, accent).box(-hw + b, hh - hdr - 0.005F, z + 0.0016F, hw - b, hh - hdr, z + 0.0028F);
    m.lit(s, scaleColor(accent, 0.8F))
        .box(
            -hw + b,
            -hh + b + 0.006F,
            z + 0.0016F,
            -hw + b + 0.006F,
            hh - hdr - 0.012F,
            z + 0.0028F);
    float blink = flashing ? ((int) (t / 6.0F) & 1) == 0 ? 1.0F : 0.25F : 0.85F;
    m.lit(s, scaleColor(flashing ? 0xFF3030 : accent, blink))
        .box(
            hw - hdr * 0.45F,
            hh - hdr * 0.74F,
            z + 0.0016F,
            hw - hdr * 0.24F,
            hh - hdr * 0.54F,
            z + 0.0032F);
    m.glow(s, 0x000000, 0.2F);
    for (float y = -hh + b; y < hh - hdr - 0.012F; y += 0.02F) {
      m.box(-hw + b + 0.008F, y, z + 0.003F, hw - b, y + 0.0045F, z + 0.0033F);
    }

    float span = 2 * hh - hdr - 0.03F;
    float sy = -hh + b + (t * 0.0035F % 1.0F) * span;
    m.add(s, 0x1E4650, 0.6F)
        .box(-hw + b + 0.008F, sy, z + 0.0034F, hw - b, sy + 0.018F, z + 0.0038F);
  }

  private void textLeft(
      String text,
      float x,
      float yCenter,
      float height,
      float maxW,
      int rgb,
      PoseStack pose,
      MultiBufferSource buffers) {
    if (text == null || text.isEmpty()) return;
    float scale = Math.min(height / 9.0F, maxW / Math.max(1, this.font.width(text)));
    pose.pushPose();
    pose.translate(x, yCenter, TEXT_Z);
    pose.scale(scale, -scale, scale);
    this.font.drawInBatch(
        text,
        0.0F,
        -4.0F,
        0xFF000000 | rgb,
        false,
        pose.last().pose(),
        buffers,
        DisplayMode.NORMAL,
        0,
        15728880);
    pose.popPose();
  }

  private static boolean bridgeMonitor(MonitorContent.MonitorEntity m) {
    if (m.getLevel() == null || !m.getLevel().dimension().equals(SpacePlanets.SHIP)) return false;
    var p = m.getBlockPos().subtract(Timeskipper.ORIGIN);
    return (p.getY() == 1 || p.getY() == 4) && p.getZ() == -18 && Math.abs(p.getX()) == 2;
  }

  private static String shortLine(String text) {
    if (text.startsWith("AT: ")) return "At: " + text.substring(4);
    if (text.startsWith("CHARGES: ")) return "Charge: " + text.substring(9);
    if (text.equals("ATMOSPHERE: NONE")) return "Air: None";
    if (text.equals("ATMOSPHERE: YES")) return "Air: Present";
    if (text.equals("RIGHT-CLICK FOR")) return "Right-click helm";
    if (text.equals("STAR MAP")) return "Open star map";
    return text;
  }

  private void renderBridge(
      MonitorContent.MonitorEntity m,
      Direction facing,
      PoseStack pose,
      MultiBufferSource buffers,
      int light) {
    String title = m.line(0),
        one = shortLine(m.line(1)),
        two = shortLine(m.line(2)),
        three = shortLine(m.line(3));
    if (title.equals("NAV CONSOLE")) title = "NAVIGATION";
    // The legacy prompt's charge count was fixed text. Use the existing live
    // status monitor's synchronized charge line, without modifying save data.
    if (m.line(3).startsWith("JUMP CHARGES:")
        && m.getLevel() != null
        && liveStatus(m) instanceof MonitorContent.MonitorEntity status
        && status.line(2).startsWith("CHARGES: ")) three = shortLine(status.line(2));
    boolean overhead = m.getBlockPos().getY() - Timeskipper.ORIGIN.getY() == 4;
    pose.pushPose();
    pose.translate(.5, overhead ? .48 : .625, .5);
    pose.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
    if (overhead) {
      ReactorMesh mount = new ReactorMesh(pose, buffers, light);
      var s = ReactorMesh.sprite("minecraft:block/white_concrete");
      mount.lit(s, 0x617785).box(-.53F, .35F, -.24F, -.48F, .52F, .31F);
      mount.lit(s, 0x617785).box(.48F, .35F, -.24F, .53F, .52F, .31F);
      pose.translate(0, 0, .24);
      pose.mulPose(Axis.XP.rotationDegrees(18F));
      // Upscale the compact display around its center, below the ceiling.
      pose.scale(1.35F, 1.22F, 1F);
    }
    ReactorMesh mesh = new ReactorMesh(pose, buffers, light);
    var sprite = ReactorMesh.sprite("minecraft:block/white_concrete");
    // Compact frame overlays the original stand screen, staying at its block.
    mesh.lit(sprite, 0x5C7383).box(-.525F, -.345F, -.014F, .525F, .345F, .016F);
    mesh.lit(sprite, 0x081722).box(-.485F, -.305F, .016F, .485F, .305F, .020F);
    mesh.lit(sprite, 0xCFAD65).box(-.47F, .093F, .021F, .47F, .105F, .024F);
    mesh.lit(sprite, 0x77CEDE).box(-.47F, -.292F, .021F, -.42F, -.277F, .024F);
    drawLine(title, .94F, .018F, .210F, 0xE7C77A, pose, buffers);
    drawLine(one, .94F, .0125F, .042F, 0xBEEFF5, pose, buffers);
    drawLine(two, .94F, .0125F, -.075F, 0xBEEFF5, pose, buffers);
    drawLine(three, .94F, .0125F, -.192F, 0xBEEFF5, pose, buffers);
    pose.popPose();
  }

  private static net.minecraft.world.level.block.entity.BlockEntity liveStatus(
      MonitorContent.MonitorEntity m) {
    return TimeskipperBridgeScreens.status(m.getLevel(), Timeskipper.ORIGIN);
  }

  private void drawLine(
      String text,
      float width,
      float maximum,
      float y,
      int rgb,
      PoseStack pose,
      MultiBufferSource buffers) {
    if (text.isEmpty()) return;
    float scale = Math.min(maximum, width / Math.max(1, font.width(text)));
    pose.pushPose();
    pose.translate(0, y, .025);
    pose.scale(scale, -scale, scale);
    font.drawInBatch(
        text,
        -font.width(text) / 2F,
        -4F,
        0xFF000000 | rgb,
        false,
        pose.last().pose(),
        buffers,
        DisplayMode.POLYGON_OFFSET,
        0,
        15728880);
    pose.popPose();
  }

  private static long mix(long v) {
    v ^= v >>> 33;
    v *= -49064778989728563L;
    v ^= v >>> 33;
    v *= -4265267296055464877L;
    return v ^ v >>> 33;
  }

  private static int scaleColor(int c, float f) {
    int r = (int) ((c >> 16 & 0xFF) * f);
    int g = (int) ((c >> 8 & 0xFF) * f);
    int b = (int) ((c & 0xFF) * f);
    return r << 16 | g << 8 | b;
  }
}
