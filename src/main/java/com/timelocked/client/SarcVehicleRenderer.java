package com.timelocked.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.timelocked.SarcGroundVehicle;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

public final class SarcVehicleRenderer<T extends Entity> extends EntityRenderer<T> {
  public static final ModelLayerLocation ROVER =
      new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("timelocked", "rover"), "main");
  public static final ModelLayerLocation TRUCK =
      new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("timelocked", "truck"), "main");
  public static final ModelLayerLocation TRAM =
      new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("timelocked", "tram"), "main");
  private final ModelPart model;
  private final String kind;
  private static final Map<Entity, float[]> DOORS = new WeakHashMap<>();

  public SarcVehicleRenderer(Context ctx, ModelLayerLocation layer, String name) {
    super(ctx);
    this.model = ctx.bakeLayer(layer);
    this.kind = name;
    this.shadowRadius = name.equals("tram") ? 0.5F : 1.2F;
  }

  public ResourceLocation getTextureLocation(T v) {
    return ResourceLocation.fromNamespaceAndPath(
        "timelocked", "textures/entity/vehicles/" + this.kind + ".png");
  }

  private void tramDoors(T v, float partial) {
    float[] d = DOORS.computeIfAbsent(v, k -> new float[] {0.0F, 0.0F, 0.0F, -1.0F});
    if (d[3] != v.tickCount) {
      d[3] = v.tickCount;
      d[1] = d[0];
      double moved = Math.hypot(v.getX() - v.xo, v.getZ() - v.zo);
      d[2] = moved < 0.004 ? d[2] + 1.0F : 0.0F;
      float target = d[2] > 10.0F ? 1.0F : 0.0F;
      float old = d[0];
      d[0] = Mth.clamp(d[0] + (target > d[0] ? 0.07F : -0.12F), 0.0F, 1.0F);
      if (old == 0.0F && d[0] > 0.0F || old == 1.0F && d[0] < 1.0F) {
        v.level()
            .playLocalSound(
                v.getX(),
                v.getY(),
                v.getZ(),
                old == 0.0F ? SoundEvents.PISTON_EXTEND : SoundEvents.PISTON_CONTRACT,
                SoundSource.NEUTRAL,
                0.35F,
                1.5F,
                false);
      }
    }

    float f = Mth.lerp(partial, d[1], d[0]);
    float e = f * f * (3.0F - 2.0F * f);
    float out = 0.8F * Math.min(1.0F, e * 4.0F);
    float slide = 12.5F * Math.max(0.0F, (e - 0.15F) / 0.85F);

    for (int i : new int[] {22, 23, 24}) {
      ModelPart part = this.model.getChild("part_" + i);
      part.x = -out;
      part.z = slide;
    }

    for (int i : new int[] {41, 42, 43}) {
      ModelPart part = this.model.getChild("part_" + i);
      part.x = out;
      part.z = slide;
    }
  }

  public void render(
      T v, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
    float rotation =
        v instanceof SarcGroundVehicle ground
            ? ground.wheelAngle + ground.driveSpeed() * partial / (ground.truck() ? 0.375F : 0.5F)
            : (float)
                ((v.tickCount + partial) * v.getDeltaMovement().horizontalDistance() / 0.1875);

    for (int index : VehicleModels.wheels(this.kind)) {
      this.model.getChild("part_" + index).xRot = rotation;
    }

    if (this.kind.equals("tram")) {
      this.tramDoors(v, partial);
    }

    pose.pushPose();
    pose.mulPose(Axis.YP.rotationDegrees(180.0F - (this.kind.equals("tram") ? yaw - 90.0F : yaw)));
    pose.scale(-1.0F, -1.0F, 1.0F);
    this.model.render(
        pose,
        buffers.getBuffer(RenderType.entityTranslucent(this.getTextureLocation(v))),
        light,
        OverlayTexture.NO_OVERLAY);
    pose.popPose();
    super.render(v, yaw, partial, pose, buffers, light);
  }
}
