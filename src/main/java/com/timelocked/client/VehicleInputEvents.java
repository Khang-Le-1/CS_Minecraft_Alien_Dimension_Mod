package com.timelocked.client;

import com.timelocked.SarcGroundVehicle;
import com.timelocked.SarcTramEntity;
import com.timelocked.VehicleNetworking;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;
import net.neoforged.neoforge.client.event.InputEvent.InteractionKeyMappingTriggered;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = "timelocked", value = Dist.CLIENT)
public final class VehicleInputEvents {
  @SubscribeEvent
  public static void board(InteractionKeyMappingTriggered e) {
    Minecraft mc = Minecraft.getInstance();
    LocalPlayer p = mc.player;
    if (e.isUseItem()
        && p != null
        && mc.level != null
        && !p.isPassenger()
        && !p.isSecondaryUseActive()) {
      Vec3 eye = p.getEyePosition();
      Vec3 end = eye.add(p.getViewVector(1.0F).scale(5.5));
      Entity best = null;
      double bestD = Double.MAX_VALUE;

      for (Entity v :
          mc.level.getEntities(
              p,
              p.getBoundingBox().inflate(9.0),
              x -> x instanceof SarcTramEntity || x instanceof SarcGroundVehicle)) {
        AABB box = v.getBoundingBox();
        Optional<Vec3> clip = box.contains(eye) ? Optional.of(eye) : box.clip(eye, end);
        if (clip.isPresent()) {
          double d = eye.distanceToSqr(clip.get());
          if (d < bestD) {
            bestD = d;
            best = v;
          }
        }
      }

      if (best == null) {
        Vec3 look = p.getViewVector(1.0F);

        for (Entity v :
            mc.level.getEntities(
                p,
                p.getBoundingBox().inflate(4.0),
                x -> x instanceof SarcTramEntity || x instanceof SarcGroundVehicle)) {
          AABB box = v.getBoundingBox();
          Vec3 near =
              new Vec3(
                  Mth.clamp(eye.x, box.minX, box.maxX),
                  Mth.clamp(eye.y, box.minY, box.maxY),
                  Mth.clamp(eye.z, box.minZ, box.maxZ));
          double d = eye.distanceToSqr(near);
          Vec3 to = v.getBoundingBox().getCenter().subtract(eye).normalize();
          if (d <= 9.0 && look.dot(to) > 0.35 && d < bestD) {
            bestD = d;
            best = v;
          }
        }
      }

      if (best != null) {
        if (mc.hitResult == null
            || mc.hitResult.getType() == Type.MISS
            || !(eye.distanceToSqr(mc.hitResult.getLocation()) < bestD - 0.01)) {
          PacketDistributor.sendToServer(
              new VehicleNetworking.Board(best.getId()), new CustomPacketPayload[0]);
          e.setCanceled(true);
          e.setSwingHand(false);
        }
      }
    }
  }

  @SubscribeEvent
  public static void input(Post e) {
    Minecraft mc = Minecraft.getInstance();
    if (mc.player != null && mc.player.getVehicle() instanceof SarcGroundVehicle v) {
      boolean paused = mc.screen != null;
      PacketDistributor.sendToServer(
          new VehicleNetworking.Input(
              v.getId(),
              paused ? 0.0F : mc.player.input.forwardImpulse,
              paused ? 0.0F : mc.player.input.leftImpulse,
              paused || mc.player.input.jumping || mc.player.input.shiftKeyDown),
          new CustomPacketPayload[0]);
    }
  }
}
