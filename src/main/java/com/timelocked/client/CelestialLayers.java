package com.timelocked.client;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

public final class CelestialLayers {
  public static LayerDefinition celestial() {
    MeshDefinition mesh = new MeshDefinition();
    PartDefinition root = mesh.getRoot();
    PartDefinition p_body =
        root.addOrReplaceChild(
            "body",
            CubeListBuilder.create().texOffs(16, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 10.0F, 4.0F),
            PartPose.offsetAndRotation(0.0F, 4.0F, 0.0F, 0.0F, 0.0F, 0.0F));
    PartDefinition p_head =
        root.addOrReplaceChild(
            "head",
            CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F),
            PartPose.offsetAndRotation(0.0F, 4.0F, 0.0F, 0.0F, 0.0F, 0.0F));
    PartDefinition p_right_arm =
        root.addOrReplaceChild(
            "right_arm",
            CubeListBuilder.create().texOffs(40, 16).addBox(-2.0F, -1.0F, -1.5F, 3.0F, 10.0F, 3.0F),
            PartPose.offsetAndRotation(-5.0F, 5.0F, 0.0F, 0.0F, 0.0F, 0.0F));
    PartDefinition p_left_arm =
        root.addOrReplaceChild(
            "left_arm",
            CubeListBuilder.create().texOffs(52, 16).addBox(-1.0F, -1.0F, -1.5F, 3.0F, 10.0F, 3.0F),
            PartPose.offsetAndRotation(5.0F, 5.0F, 0.0F, 0.0F, 0.0F, 0.0F));
    PartDefinition p_right_leg =
        root.addOrReplaceChild(
            "right_leg",
            CubeListBuilder.create().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 10.0F, 4.0F),
            PartPose.offsetAndRotation(-2.0F, 14.0F, 0.0F, 0.0F, 0.0F, 0.0F));
    PartDefinition p_left_leg =
        root.addOrReplaceChild(
            "left_leg",
            CubeListBuilder.create().texOffs(0, 32).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 10.0F, 4.0F),
            PartPose.offsetAndRotation(2.0F, 14.0F, 0.0F, 0.0F, 0.0F, 0.0F));
    PartDefinition p_hat =
        p_head.addOrReplaceChild(
            "hat",
            CubeListBuilder.create()
                .texOffs(32, 0)
                .addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.5F)),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
    PartDefinition p_ear_l =
        p_head.addOrReplaceChild(
            "ear_l",
            CubeListBuilder.create().texOffs(48, 32).addBox(-1.5F, -4.0F, -0.5F, 3.0F, 4.0F, 1.0F),
            PartPose.offsetAndRotation(2.6F, -8.0F, 0.5F, 0.0F, 0.0F, 0.3F));
    PartDefinition p_ear_r =
        p_head.addOrReplaceChild(
            "ear_r",
            CubeListBuilder.create().texOffs(56, 32).addBox(-1.5F, -4.0F, -0.5F, 3.0F, 4.0F, 1.0F),
            PartPose.offsetAndRotation(-2.6F, -8.0F, 0.5F, 0.0F, 0.0F, -0.3F));
    PartDefinition p_hair_long =
        p_head.addOrReplaceChild(
            "hair_long",
            CubeListBuilder.create().texOffs(64, 28).addBox(-4.5F, -8.0F, 3.6F, 9.0F, 15.0F, 2.0F),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.08F, 0.0F, 0.0F));
    PartDefinition p_tail =
        p_body.addOrReplaceChild(
            "tail",
            CubeListBuilder.create().texOffs(16, 32).addBox(-2.0F, -2.0F, 0.0F, 4.0F, 4.0F, 11.0F),
            PartPose.offsetAndRotation(0.0F, 8.0F, 2.0F, -0.35F, 0.0F, 0.0F));
    PartDefinition p_skirt =
        p_body.addOrReplaceChild(
            "skirt",
            CubeListBuilder.create().texOffs(0, 48).addBox(-4.5F, 0.0F, -2.5F, 9.0F, 4.0F, 5.0F),
            PartPose.offsetAndRotation(0.0F, 8.0F, 0.0F, 0.0F, 0.0F, 0.0F));
    PartDefinition p_wing_l =
        p_body.addOrReplaceChild(
            "wing_l",
            CubeListBuilder.create().texOffs(64, 0).addBox(0.0F, -8.0F, 0.0F, 14.0F, 14.0F, 0.0F),
            PartPose.offsetAndRotation(1.0F, 3.0F, 2.0F, 0.0F, -0.55F, 0.0F));
    PartDefinition p_wing_r =
        p_body.addOrReplaceChild(
            "wing_r",
            CubeListBuilder.create()
                .texOffs(64, 14)
                .addBox(-14.0F, -8.0F, 0.0F, 14.0F, 14.0F, 0.0F),
            PartPose.offsetAndRotation(-1.0F, 3.0F, 2.0F, 0.0F, 0.55F, 0.0F));
    PartDefinition p_mantle =
        p_body.addOrReplaceChild(
            "mantle",
            CubeListBuilder.create()
                .texOffs(88, 45)
                .addBox(-7.0F, -0.5F, -3.0F, 14.0F, 5.0F, 6.0F, new CubeDeformation(0.25F)),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
    return LayerDefinition.create(mesh, 128, 64);
  }

  private CelestialLayers() {}
}
