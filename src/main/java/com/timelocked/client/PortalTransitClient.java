package com.timelocked.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.timelocked.PortalCinematicTimeline;
import net.minecraft.Util;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;

/**
 * External camera, animated local avatar and an actual world-space portal shaft. No authoritative
 * movement/game mode/flight flags are changed by this client.
 */
@EventBusSubscriber(modid = "timelocked", value = Dist.CLIENT)
public final class PortalTransitClient {
  private static boolean active, previousHud;
  private static CameraType previousType;
  private static net.minecraft.client.CloudStatus previousClouds;
  private static Entity previousCamera, rig;
  private static int age, exitAt = -1, landedAt = -1, missing;
  private static long began;

  public static boolean active() {
    return active;
  }

  public static boolean actor(Entity e) {
    return active && e == Minecraft.getInstance().player;
  }

  private static float partial() {
    return Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
  }

  public static PortalCinematicTimeline.Pose pose(float partial) {
    return PortalCinematicTimeline.sample(
        age + partial,
        exitAt < 0 ? -1 : age - exitAt + partial,
        landedAt < 0 ? -1 : age - landedAt + partial);
  }

  public static void phase(int phase) {
    Minecraft mc = Minecraft.getInstance();
    if (phase == 0) {
      if (active) reset();
      // Do not steal the camera from the separate Timeskipper jump cinematic.
      if (JumpCutsceneClient.active()) return;
      active = true;
      age = 0;
      exitAt = -1;
      landedAt = -1;
      missing = 0;
      began = Util.getMillis();
      previousClouds = mc.options.cloudStatus().get();
      mc.options.cloudStatus().set(net.minecraft.client.CloudStatus.OFF);
      previousHud = mc.options.hideGui;
      previousType = mc.options.getCameraType();
      previousCamera = mc.getCameraEntity();
      mc.options.hideGui = true;
      mc.options.setCameraType(CameraType.FIRST_PERSON);
      if (mc.player != null) mc.player.playSound(SoundEvents.PORTAL_TRAVEL, .35F, .85F);
      positionRig(true);
    } else if (phase == 1 && active && exitAt < 0) {
      exitAt = age;
      if (mc.player != null) mc.player.playSound(SoundEvents.ENDERMAN_TELEPORT, .4F, .75F);
      positionRig(true);
    }
  }

  public static void reset() {
    if (!active) return;
    active = false;
    Minecraft mc = Minecraft.getInstance();
    Entity restore = previousCamera;
    if (restore == null || restore.isRemoved() || restore.level() != mc.level) restore = mc.player;
    mc.setCameraEntity(restore);
    mc.options.hideGui = previousHud;
    if (previousType != null) mc.options.setCameraType(previousType);
    if (previousClouds != null) mc.options.cloudStatus().set(previousClouds);
    previousClouds = null;
    if (rig != null) rig.discard();
    rig = null;
    previousCamera = null;
    previousType = null;
    age = 0;
    exitAt = -1;
    landedAt = -1;
  }

  @SubscribeEvent
  public static void leave(ClientPlayerNetworkEvent.LoggingOut e) {
    reset();
  }

  @SubscribeEvent
  public static void tick(ClientTickEvent.Post e) {
    if (!active) return;
    Minecraft mc = Minecraft.getInstance();
    if (Util.getMillis() - began > 15000 || JumpCutsceneClient.active()) {
      reset();
      return;
    }
    // A dimension packet can precede installation of its new client level/player.
    if (mc.level == null || mc.player == null) {
      if (++missing > 40) reset();
      return;
    }
    missing = 0;
    if (!mc.player.isAlive()
        || (landedAt >= 0 && age - landedAt >= PortalCinematicTimeline.EXIT_TICKS)
        || (exitAt >= 0 && landedAt < 0 && age - exitAt > 100)) {
      reset();
      return;
    }
    age++;
    // END means ejected from the gate, not grounded. Start the faceplant only on contact.
    if (exitAt >= 0 && landedAt < 0 && mc.player.onGround()) {
      landedAt = age;
      mc.player.playSound(SoundEvents.PLAYER_SMALL_FALL, .65F, .8F);
    }
    if (mc.options.keyInventory.isDown()
        || (mc.screen != null
            && !(mc.screen instanceof net.minecraft.client.gui.screens.ReceivingLevelScreen))) {
      reset();
      return;
    }
    positionRig(false);
  }

  private static void positionRig(boolean snap) {
    Minecraft mc = Minecraft.getInstance();
    if (mc.level == null || mc.player == null) return;
    if (rig == null || rig.level() != mc.level) {
      if (rig != null) rig.discard();
      rig = EntityType.MARKER.create(mc.level);
      snap = true;
    }
    if (rig == null) {
      reset();
      return;
    }
    var p = pose(0);
    double a = Math.toRadians(p.orbit() + mc.player.getYRot());
    Vec3 focus = mc.player.position().add(0, .95 - .55 * p.faceplant(), 0);
    Vec3 desired = focus.add(Math.cos(a) * p.radius(), p.height(), Math.sin(a) * p.radius());
    var hit =
        mc.level.clip(
            new ClipContext(
                focus, desired, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, mc.player));
    if (hit.getType() != HitResult.Type.MISS) {
      Vec3 delta = hit.getLocation().subtract(focus);
      desired =
          focus.add(
              delta.scale(Math.max(.15, (delta.length() - .25) / Math.max(.001, delta.length()))));
    }
    Vec3 look = focus.subtract(desired);
    float yaw = (float) (Math.toDegrees(Math.atan2(look.z, look.x)) - 90);
    float pitch =
        (float) -Math.toDegrees(Math.atan2(look.y, Math.sqrt(look.x * look.x + look.z * look.z)));
    double ox = rig.getX(), oy = rig.getY(), oz = rig.getZ();
    float yr = rig.getYRot(), xr = rig.getXRot();
    yaw = yr + net.minecraft.util.Mth.wrapDegrees(yaw - yr);
    rig.moveTo(desired.x, desired.y, desired.z, yaw, pitch);
    if (!snap) {
      rig.xo = rig.xOld = ox;
      rig.yo = rig.yOld = oy;
      rig.zo = rig.zOld = oz;
      rig.yRotO = yr;
      rig.xRotO = xr;
    }
    mc.setCameraEntity(rig);
  }

  @SubscribeEvent
  public static void fov(ViewportEvent.ComputeFov e) {
    if (active) e.setFOV(pose(partial()).fov());
  }

  @SubscribeEvent
  public static void angles(ViewportEvent.ComputeCameraAngles e) {
    if (active) e.setRoll(e.getRoll() + pose(partial()).cameraRoll());
  }

  @SubscribeEvent
  public static void input(MovementInputUpdateEvent e) {
    if (!actor(e.getEntity())) return;
    var i = e.getInput();
    i.forwardImpulse = 0;
    i.leftImpulse = 0;
    i.jumping = false;
    i.up = i.down = i.left = i.right = false;
  }

  @SubscribeEvent
  public static void interact(InputEvent.InteractionKeyMappingTriggered e) {
    if (active) {
      e.setCanceled(true);
      e.setSwingHand(false);
    }
  }

  @SubscribeEvent
  public static void tunnel(RenderLevelStageEvent e) {
    if (!active || e.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
    Minecraft mc = Minecraft.getInstance();
    if (mc.player == null) return;
    float pt = e.getPartialTick().getGameTimeDeltaPartialTick(false);
    var p = pose(pt);
    if (p.tunnel() < .005) return;
    PoseStack s = e.getPoseStack();
    Vec3 center = mc.player.getPosition(pt), cam = e.getCamera().getPosition();
    s.pushPose();
    s.translate(center.x - cam.x, center.y - cam.y, center.z - cam.z);
    var buffers = mc.renderBuffers().bufferSource();
    var sprite = ReactorMesh.sprite("minecraft:block/white_concrete");
    if (sprite != null) {
      ReactorMesh mesh = new ReactorMesh(s, buffers, 15728880);
      // A dark, translucent twelve-sided shaft masks ordinary terrain without
      // covering the avatar or replacing the scene with a GUI animation.
      mesh.glow(sprite, 0x090D1E, p.tunnel() * .97F);
      for (int i = 0; i < 12; i++) {
        double a = i * Math.PI / 6, b = (i + 1) * Math.PI / 6;
        float x0 = (float) Math.cos(a) * 7.4F,
            z0 = (float) Math.sin(a) * 7.4F,
            x1 = (float) Math.cos(b) * 7.4F,
            z1 = (float) Math.sin(b) * 7.4F;
        mesh.v(x0, -32, z0, 0, 1, 0, 0, 1);
        mesh.v(x1, -32, z1, 1, 1, 0, 0, 1);
        mesh.v(x1, 34, z1, 1, 0, 0, 0, 1);
        mesh.v(x0, 34, z0, 0, 0, 0, 0, 1);
        for (float yy : new float[] {-32, 34}) {
          mesh.v(0, yy, 0, 0, 0, 0, 1, 0);
          mesh.v(x0, yy, z0, 1, 0, 0, 1, 0);
          mesh.v(x1, yy, z1, 1, 1, 0, 1, 0);
          mesh.v(0, yy, 0, 0, 1, 0, 1, 0);
        }
      }
      // Twelve world-space ribs; bounded geometry, no particle storm or 2-D rectangle tunnel.
      for (int k = -5; k <= 5; k++) {
        float y = k * 6 + ((age + pt) * 1.4F % 6);
        int color = (k & 1) == 0 ? 0x80D8ED : 0xA495DD;
        for (int i = 0; i < 12; i++) {
          s.pushPose();
          s.mulPose(Axis.YP.rotationDegrees(i * 30 + (float) Math.sin(k) * 3));
          mesh.glow(sprite, color, p.tunnel() * .8F)
              .box(6.65F, y - .055F, -1.82F, 6.76F, y + .055F, 1.82F);
          s.popPose();
        }
      }
      // Four twisting light filaments make the direction of travel readable beside the actor.
      for (int i = 0; i < 4; i++)
        for (int j = 0; j < 12; j++) {
          double a = i * Math.PI * .5 + j * .17 + (age + pt) * .025;
          float x = (float) Math.cos(a) * 5.8F,
              z = (float) Math.sin(a) * 5.8F,
              y = j * 4 - 24 + ((age + pt) * 1.4F % 4);
          mesh.glow(sprite, 0x697FC4, p.tunnel() * .45F)
              .box(x - .025F, y, z - .025F, x + .025F, y + 1.2F, z + .025F);
        }
    }
    buffers.endBatch();
    s.popPose();
  }

  @SubscribeEvent
  public static void overlay(RenderGuiEvent.Post e) {
    if (!active) return;
    var g = e.getGuiGraphics();
    int w = g.guiWidth(), h = g.guiHeight();
    float f = pose(partial()).tunnel();
    if (exitAt >= 0)
      f =
          Math.max(
              f,
              landedAt < 0
                  ? 1
                  : 1 - PortalCinematicTimeline.smooth((age - landedAt - 48 + partial()) / 12F));
    int bar = Math.round(h * .065F * f);
    g.fill(0, 0, w, bar, 0xFF090A10);
    g.fill(0, h - bar, w, h, 0xFF090A10);
    // Gentle edge tint, not the former full-screen white flash.
    int a = (int) (70 * f);
    g.fillGradient(0, bar, w, bar + h / 6, (a << 24) | 0x1A123B, 0x001A123B);
    g.fillGradient(0, h - bar - h / 6, w, h - bar, 0x001A123B, (a << 24) | 0x1A123B);
  }

  private PortalTransitClient() {}
}
