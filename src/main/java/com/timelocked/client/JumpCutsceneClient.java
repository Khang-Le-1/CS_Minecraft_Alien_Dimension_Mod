package com.timelocked.client;

import com.timelocked.JumpNetworking;
import com.timelocked.JumpTimeline;
import com.timelocked.SpacePlanets;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Client-side state that drives the shared jump cinematic. The exterior renderer reads engine power
 * and wake from this while the sequence runs.
 */
public final class JumpCutsceneClient {
  private static boolean active;
  private static int ageTicks;
  private static long lastGameTime = -1L;
  private static String destination = "";
  private static int token;
  private static Entity rig;
  private static Entity previousCamera;

  private JumpCutsceneClient() {}

  public static boolean active() {
    return active;
  }

  public static float age(float partialTick) {
    return active ? Math.max(0.0F, ageTicks + partialTick) : 0.0F;
  }

  public static float enginePower(float partialTick) {
    return active ? JumpTimeline.engines(age(partialTick)) : 0.0F;
  }

  public static String destination() {
    return destination;
  }

  public static void begin(int token, int from, int to, long started) {
    if (active) return;
    active = true;
    JumpCutsceneClient.token = token;
    ageTicks = 0;
    lastGameTime = -1L;
    destination = SpacePlanets.Planet.byIndex(to).title;

    Minecraft mc = Minecraft.getInstance();
    previousCamera = mc.getCameraEntity();
    if (rig == null && mc.level != null) {
      rig = EntityType.MARKER.create(mc.level);
    }
    if (rig != null) {
      JumpTimeline.Rig r = JumpTimeline.camera(0.0F);
      rig.moveTo(r.x(), r.y(), r.z(), r.yaw(), r.pitch());
      mc.setCameraEntity(rig);
    }
    mc.options.hideGui = true;
    mc.setScreen(new JumpCinematicScreen());
  }

  public static void stop(boolean success) {
    end();
  }

  public static void end() {
    if (!active) return;
    active = false;
    Minecraft mc = Minecraft.getInstance();
    mc.setCameraEntity(previousCamera);
    previousCamera = null;
    mc.options.hideGui = false;
    if (mc.screen instanceof JumpCinematicScreen) {
      mc.setScreen(null);
    }
    ageTicks = 0;
    lastGameTime = -1L;
    destination = "";
  }

  public static void skip() {
    if (!active) return;
    PacketDistributor.sendToServer(new JumpNetworking.Skip(token), new CustomPacketPayload[0]);
  }

  public static void tick() {
    if (!active) return;
    Minecraft mc = Minecraft.getInstance();
    if (mc.level == null) {
      end();
      return;
    }
    long now = mc.level.getGameTime();
    if (lastGameTime < 0L) lastGameTime = now;
    ageTicks += (int) Math.max(0L, now - lastGameTime);
    lastGameTime = now;

    if (ageTicks >= JumpTimeline.END) {
      end();
      return;
    }
    positionRig();
    if (mc.screen == null) {
      mc.setScreen(new JumpCinematicScreen());
    }
  }

  private static void positionRig() {
    Minecraft mc = Minecraft.getInstance();
    if (rig == null || mc.level == null) return;
    JumpTimeline.Rig r = JumpTimeline.camera(ageTicks);
    rig.moveTo(r.x(), r.y(), r.z(), r.yaw(), r.pitch());
    if (mc.getCameraEntity() != rig) mc.setCameraEntity(rig);
  }
}
