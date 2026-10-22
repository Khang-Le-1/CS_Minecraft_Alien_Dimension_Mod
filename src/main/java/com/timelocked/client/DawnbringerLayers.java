package com.timelocked.client;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

public final class DawnbringerLayers {
  public static LayerDefinition dawnbringer() {
    MeshDefinition mesh = new MeshDefinition();
    PartDefinition root = mesh.getRoot();
    PartDefinition p_body =
        root.addOrReplaceChild(
            "body",
            CubeListBuilder.create().texOffs(16, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
    PartDefinition p_head =
        root.addOrReplaceChild(
            "head",
            CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
    PartDefinition p_helm =
        p_head.addOrReplaceChild(
            "helm",
            CubeListBuilder.create()
                .texOffs(32, 0)
                .addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.6F)),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
    PartDefinition p_crest =
        p_head.addOrReplaceChild(
            "crest",
            CubeListBuilder.create().texOffs(64, 10).addBox(-6.0F, -6.0F, 0.0F, 12.0F, 6.0F, 0.0F),
            PartPose.offsetAndRotation(0.0F, -8.4F, 0.5F, 0.0F, 0.0F, 0.0F));
    PartDefinition p_right_arm =
        root.addOrReplaceChild(
            "right_arm",
            CubeListBuilder.create().texOffs(40, 16).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F),
            PartPose.offsetAndRotation(-5.0F, 2.0F, 0.0F, 0.0F, 0.0F, 0.0F));
    PartDefinition p_left_arm =
        root.addOrReplaceChild(
            "left_arm",
            CubeListBuilder.create().texOffs(40, 32).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F),
            PartPose.offsetAndRotation(5.0F, 2.0F, 0.0F, 0.0F, 0.0F, 0.0F));
    PartDefinition p_pauldron_r =
        p_right_arm.addOrReplaceChild(
            "pauldron_r",
            CubeListBuilder.create().texOffs(64, 0).addBox(-4.0F, -3.0F, -3.0F, 6.0F, 3.0F, 6.0F),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
    PartDefinition p_pauldron_l =
        p_left_arm.addOrReplaceChild(
            "pauldron_l",
            CubeListBuilder.create().texOffs(88, 0).addBox(-2.0F, -3.0F, -3.0F, 6.0F, 3.0F, 6.0F),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
    PartDefinition p_right_leg =
        root.addOrReplaceChild(
            "right_leg",
            CubeListBuilder.create().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F),
            PartPose.offsetAndRotation(-1.9F, 12.0F, 0.0F, 0.0F, 0.0F, 0.0F));
    PartDefinition p_left_leg =
        root.addOrReplaceChild(
            "left_leg",
            CubeListBuilder.create().texOffs(0, 32).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F),
            PartPose.offsetAndRotation(1.9F, 12.0F, 0.0F, 0.0F, 0.0F, 0.0F));
    PartDefinition p_cape =
        p_body.addOrReplaceChild(
            "cape",
            CubeListBuilder.create().texOffs(16, 32).addBox(-5.0F, 0.0F, 0.0F, 10.0F, 16.0F, 1.0F),
            PartPose.offsetAndRotation(0.0F, 0.5F, 2.1F, 0.1F, 0.0F, 0.0F));
    PartDefinition p_tabard =
        p_body.addOrReplaceChild(
            "tabard",
            CubeListBuilder.create().texOffs(40, 48).addBox(-3.0F, 0.0F, -0.5F, 6.0F, 10.0F, 1.0F),
            PartPose.offsetAndRotation(0.0F, 6.0F, -2.5F, 0.0F, 0.0F, 0.0F));
    PartDefinition p_faulds =
        p_body.addOrReplaceChild(
            "faulds",
            CubeListBuilder.create().texOffs(64, 48).addBox(-4.5F, 0.0F, -2.5F, 9.0F, 4.0F, 5.0F),
            PartPose.offsetAndRotation(0.0F, 10.0F, 0.0F, 0.0F, 0.0F, 0.0F));
    PartDefinition p_robe =
        p_body.addOrReplaceChild(
            "robe",
            CubeListBuilder.create().texOffs(64, 20).addBox(-4.5F, 0.0F, -2.5F, 9.0F, 10.0F, 5.0F),
            PartPose.offsetAndRotation(0.0F, 10.0F, 0.0F, 0.0F, 0.0F, 0.0F));
    PartDefinition p_shaft =
        p_right_arm.addOrReplaceChild(
            "shaft",
            CubeListBuilder.create()
                .texOffs(56, 32)
                .addBox(-0.5F, -20.0F, -0.5F, 1.0F, 28.0F, 1.0F),
            PartPose.offsetAndRotation(-1.0F, 10.0F, 0.0F, 0.25F, 0.0F, 0.0F));
    PartDefinition p_blade =
        p_shaft.addOrReplaceChild(
            "blade",
            CubeListBuilder.create().texOffs(112, 0).addBox(0.0F, 0.0F, -6.0F, 0.0F, 7.0F, 6.0F),
            PartPose.offsetAndRotation(0.0F, -19.0F, -0.5F, 0.0F, 0.0F, 0.0F));
    PartDefinition p_disc =
        p_shaft.addOrReplaceChild(
            "disc",
            CubeListBuilder.create().texOffs(96, 40).addBox(-3.5F, -3.5F, -0.5F, 7.0F, 7.0F, 1.0F),
            PartPose.offsetAndRotation(0.0F, -21.0F, 0.0F, 0.0F, 0.0F, 0.0F));
    PartDefinition p_sword =
        p_right_arm.addOrReplaceChild(
            "sword",
            CubeListBuilder.create()
                .texOffs(96, 16)
                .addBox(-1.0F, -20.0F, -0.5F, 2.0F, 18.0F, 1.0F),
            PartPose.offsetAndRotation(-1.0F, 10.0F, 0.0F, 0.25F, 0.0F, 0.0F));
    PartDefinition p_guard =
        p_sword.addOrReplaceChild(
            "guard",
            CubeListBuilder.create().texOffs(102, 16).addBox(-3.5F, -1.0F, -1.0F, 7.0F, 1.0F, 2.0F),
            PartPose.offsetAndRotation(0.0F, -2.0F, 0.0F, 0.0F, 0.0F, 0.0F));
    PartDefinition p_grip =
        p_sword.addOrReplaceChild(
            "grip",
            CubeListBuilder.create().texOffs(102, 20).addBox(-0.5F, -2.0F, -0.5F, 1.0F, 4.0F, 1.0F),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
    return LayerDefinition.create(mesh, 128, 64);
  }

  private DawnbringerLayers() {}
}
