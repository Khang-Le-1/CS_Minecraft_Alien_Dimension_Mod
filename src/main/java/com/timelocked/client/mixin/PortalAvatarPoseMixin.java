package com.timelocked.client.mixin;

import com.timelocked.client.PortalTransitClient;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applied before PlayerModel copies its base limbs to sleeves/trousers. */
@Mixin(HumanoidModel.class)
public abstract class PortalAvatarPoseMixin {
  @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
  private void timelocked$fall(
      LivingEntity actor,
      float walk,
      float amount,
      float age,
      float yaw,
      float pitch,
      CallbackInfo ci) {
    if (!PortalTransitClient.actor(actor)) return;
    var pose =
        PortalTransitClient.pose(
            net.minecraft.client.Minecraft.getInstance()
                .getTimer()
                .getGameTimeDeltaPartialTick(false));
    HumanoidModel<?> model = (HumanoidModel<?>) (Object) this;
    model.rightArm.xRot = pose.arm();
    model.leftArm.xRot = pose.arm() + .12F * (1 - pose.faceplant());
    model.rightArm.zRot = (.28F * pose.tunnel() + .32F * pose.faceplant());
    model.leftArm.zRot = -(.28F * pose.tunnel() + .32F * pose.faceplant());
    model.rightLeg.xRot = pose.leg();
    model.leftLeg.xRot = -pose.leg();
    model.head.xRot = .12F * pose.tunnel();
  }
}
