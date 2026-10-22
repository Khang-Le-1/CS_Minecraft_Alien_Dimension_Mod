package com.timelocked.client;

import com.timelocked.IntroLanding;
import com.timelocked.IntroNetworking;
import com.timelocked.IntroSequence;
import com.timelocked.WakePresentation;
import net.minecraft.Util;
import net.minecraft.client.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.network.PacketDistributor;

/** Local blackout and low-to-standing wake camera; no forced camera during playable home. */
@EventBusSubscriber(modid = "timelocked", value = Dist.CLIENT)
public final class IntroClient {
  // Require consecutive FULL chunks and dry support, not just receipt of a teleport.
  private static final int READY_STABLE_TICKS = 8;
  private static final int READY_RETRY_TICKS = 20;
  // Server cues arrive every second. A six-second silence releases a stale camera.
  private static final long CUE_TIMEOUT_MILLIS = 6000;
  private static final int BLACKOUT_FADE_TICKS = 10;
  private static final int FULL_ALPHA = 255;
  private static final int VIGNETTE_ALPHA = 48;
  private static final int VIGNETTE_COLOR = 0x080b13;
  private static final int ALPHA_SHIFT = 24;
  private static final int VIGNETTE_HEIGHT_DIVISOR = 4;
  private static final double BLOCK_CENTER = 0.5;
  private static final double MAX_INTERPOLATION_STEP_BLOCKS = 2;

  private static int stage, age, token, stable, ackTicks;
  private static BlockPos anchor = BlockPos.ZERO;
  private static long lastCue;
  private static Entity rig;
  private static CameraType previous;

  public static void cue(int s, int t, long a, int nonce) {
    boolean changed = token != nonce || stage != s;
    if (changed) {
      stable = 0;
      ackTicks = 0;
    }
    anchor = BlockPos.of(a);
    token = nonce;
    if (s == IntroSequence.DONE) {
      reset();
      return;
    }
    stage = s;
    age = changed ? t : Math.max(age, t);
    lastCue = Util.getMillis();
    Minecraft mc = Minecraft.getInstance();
    if ((s == IntroSequence.BLACKOUT || s == IntroSequence.WAKE || s == IntroSequence.LANDING)
        && previous == null) {
      previous = mc.options.getCameraType();
      mc.options.setCameraType(CameraType.FIRST_PERSON);
    }
  }

  static void reset() {
    Minecraft mc = Minecraft.getInstance();
    if (rig != null) {
      if (mc.getCameraEntity() == rig) mc.setCameraEntity(mc.player);
      rig.discard();
      rig = null;
    }
    if (previous != null) {
      mc.options.setCameraType(previous);
      previous = null;
    }
    stage = 0;
    age = 0;
    stable = 0;
    ackTicks = 0;
    token = 0;
  }

  static boolean locked() {
    return stage == IntroSequence.BLACKOUT
        || stage == IntroSequence.WAKE
        || stage == IntroSequence.LANDING;
  }

  @SubscribeEvent
  public static void logout(ClientPlayerNetworkEvent.LoggingOut e) {
    reset();
  }

  @SubscribeEvent
  public static void tick(ClientTickEvent.Post e) {
    if (stage == 0) return;
    Minecraft mc = Minecraft.getInstance();
    if (mc.player == null
        || mc.level == null
        || !mc.player.isAlive()
        || Util.getMillis() - lastCue > CUE_TIMEOUT_MILLIS) {
      reset();
      return;
    }
    age++;
    if (!locked()) return;
    mc.options.setCameraType(CameraType.FIRST_PERSON);
    if ((stage == IntroSequence.LANDING || stage == IntroSequence.WAKE)
        && mc.level.dimension().equals(Level.OVERWORLD)) {
      mc.player.setPos(anchor.getX() + BLOCK_CENTER, anchor.getY(), anchor.getZ() + BLOCK_CENTER);
      mc.player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
      mc.player.fallDistance = 0;
      if (stage == IntroSequence.LANDING) {
        boolean loaded = true;
        for (int x = -IntroLanding.READY_CHUNK_RADIUS; x <= IntroLanding.READY_CHUNK_RADIUS; x++)
          for (int z = -IntroLanding.READY_CHUNK_RADIUS;
              z <= IntroLanding.READY_CHUNK_RADIUS;
              z++) {
            var chunk =
                mc.level
                    .getChunkSource()
                    .getChunk(
                        (anchor.getX() >> IntroLanding.CHUNK_COORDINATE_SHIFT) + x,
                        (anchor.getZ() >> IntroLanding.CHUNK_COORDINATE_SHIFT) + z,
                        ChunkStatus.FULL,
                        false);
            if (chunk == null || chunk instanceof net.minecraft.world.level.chunk.EmptyLevelChunk)
              loaded = false;
          }
        stable = loaded && IntroLanding.supported(mc.level, anchor) ? stable + 1 : 0;
        if (stable >= READY_STABLE_TICKS && ackTicks++ % READY_RETRY_TICKS == 0)
          PacketDistributor.sendToServer(new IntroNetworking.Ready(token));
      }
    }
    boolean created = rig == null || rig.level() != mc.level;
    if (created) {
      if (rig != null) rig.discard();
      rig = EntityType.MARKER.create(mc.level);
    }
    if (rig == null) return;
    double oldX = rig.getX(), oldY = rig.getY(), oldZ = rig.getZ();
    float oldYaw = rig.getYRot(), oldPitch = rig.getXRot();
    boolean waking = stage == IntroSequence.WAKE;
    double eye =
        waking
            ? WakePresentation.eyeHeight(age, mc.player.getEyeHeight())
            : mc.player.getEyeHeight();
    float pitch = waking ? WakePresentation.pitch(age) : mc.player.getXRot();
    boolean beach = waking || stage == IntroSequence.LANDING;
    rig.moveTo(
        beach ? anchor.getX() + BLOCK_CENTER : mc.player.getX(),
        (beach ? anchor.getY() : mc.player.getY()) + eye,
        beach ? anchor.getZ() + BLOCK_CENTER : mc.player.getZ(),
        waking
            ? WakePresentation.yaw(age)
            : beach ? WakePresentation.FINAL_YAW_DEGREES : mc.player.getYRot(),
        pitch);
    // Preserve previous pose for render interpolation, but never interpolate in from a new rig's
    // origin.
    boolean interpolate = !created && Math.abs(oldY - rig.getY()) < MAX_INTERPOLATION_STEP_BLOCKS;
    rig.xo = rig.xOld = interpolate ? oldX : rig.getX();
    rig.yo = rig.yOld = interpolate ? oldY : rig.getY();
    rig.zo = rig.zOld = interpolate ? oldZ : rig.getZ();
    rig.yRotO = interpolate ? oldYaw : rig.getYRot();
    rig.xRotO = interpolate ? oldPitch : rig.getXRot();
    mc.setCameraEntity(rig);
  }

  // A detached camera makes vanilla treat the local player as another entity.
  // At the low wake eye height that renders the torso around the lens. Cull only
  // this client's body/hands during locked shots; never modify player invisibility.
  @SubscribeEvent
  public static void frame(RenderFrameEvent.Pre e) {
    if (!locked()) return;
    Minecraft mc = Minecraft.getInstance();
    mc.options.setCameraType(CameraType.FIRST_PERSON);
    if (rig != null && mc.player != null && rig.level() == mc.level && mc.getCameraEntity() != rig)
      mc.setCameraEntity(rig);
  }

  @SubscribeEvent
  public static void body(RenderPlayerEvent.Pre e) {
    if (locked() && e.getEntity() == Minecraft.getInstance().player) e.setCanceled(true);
  }

  @SubscribeEvent
  public static void hands(RenderHandEvent e) {
    if (locked()) e.setCanceled(true);
  }

  @SubscribeEvent
  public static void input(MovementInputUpdateEvent e) {
    if (!locked()) return;
    var i = e.getInput();
    i.forwardImpulse = 0;
    i.leftImpulse = 0;
    i.jumping = false;
    i.shiftKeyDown = false;
    i.up = i.down = i.left = i.right = false;
  }

  @SubscribeEvent
  public static void interaction(InputEvent.InteractionKeyMappingTriggered e) {
    if (locked()) {
      e.setCanceled(true);
      e.setSwingHand(false);
    }
  }

  @SubscribeEvent
  public static void overlay(RenderGuiEvent.Post e) {
    if (!locked()) return;
    var g = e.getGuiGraphics();
    int w = g.guiWidth(), h = g.guiHeight();
    float presentationAge =
        Math.max(0, age - 1 + e.getPartialTick().getGameTimeDeltaPartialTick(false));
    float f =
        stage == IntroSequence.LANDING
            ? 1
            : stage == IntroSequence.BLACKOUT
                ? Math.clamp(age / (float) BLACKOUT_FADE_TICKS, 0, 1)
                : WakePresentation.blackout(presentationAge);
    int alpha = Math.round(FULL_ALPHA * f);
    g.fill(0, 0, w, h, alpha << ALPHA_SHIFT);
    if (stage == IntroSequence.WAKE) {
      int edge = Math.round(VIGNETTE_ALPHA * WakePresentation.vignette(presentationAge));
      int color = (edge << ALPHA_SHIFT) | VIGNETTE_COLOR;
      g.fillGradient(0, 0, w, h / VIGNETTE_HEIGHT_DIVISOR, color, 0);
      g.fillGradient(0, h - h / VIGNETTE_HEIGHT_DIVISOR, w, h, 0, color);
    }
  }

  private IntroClient() {}
}
