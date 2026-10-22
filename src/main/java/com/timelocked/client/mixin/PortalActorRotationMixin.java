package com.timelocked.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.timelocked.client.PortalTransitClient;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Runs inside vanilla's balanced per-entity pose push/pop; cannot leak into other entities. */
@Mixin(PlayerRenderer.class)
public abstract class PortalActorRotationMixin {
  @Inject(
      method =
          "setupRotations(Lnet/minecraft/client/player/AbstractClientPlayer;Lcom/mojang/blaze3d/vertex/PoseStack;FFFF)V",
      at = @At("TAIL"))
  private void timelocked$tumble(
      AbstractClientPlayer actor,
      PoseStack s,
      float bob,
      float yaw,
      float partial,
      float scale,
      CallbackInfo ci) {
    if (!PortalTransitClient.actor(actor)) return;
    var p = PortalTransitClient.pose(partial);
    s.translate(0, -p.bodyDrop(), 0);
    s.translate(0, .95, 0);
    s.mulPose(Axis.XP.rotationDegrees(p.bodyPitch()));
    s.mulPose(Axis.ZP.rotationDegrees(p.bodyRoll()));
    s.translate(0, -.95, 0);
  }
}
