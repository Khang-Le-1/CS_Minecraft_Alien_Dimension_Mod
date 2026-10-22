package com.timelocked.client;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

public final class AlienMobLayers {
  public static LayerDefinition ashback() {
    MeshDefinition mesh = new MeshDefinition();
    PartDefinition root = mesh.getRoot();
    PartDefinition p_body =
        root.addOrReplaceChild(
            "body",
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-7.0F, -5.0F, -10.0F, 14.0F, 10.0F, 20.0F),
            PartPose.offsetAndRotation(0.0F, 11.0F, 0.0F, 0.0F, 0.0F, 0.0F));
    PartDefinition p_head =
        root.addOrReplaceChild(
            "head",
            CubeListBuilder.create().texOffs(0, 30).addBox(-4.0F, -4.0F, -8.0F, 8.0F, 8.0F, 8.0F),
            PartPose.offsetAndRotation(0.0F, 8.0F, -10.0F, 0.0F, 0.0F, 0.0F));
    PartDefinition p_snout =
        p_head.addOrReplaceChild(
            "snout",
            CubeListBuilder.create().texOffs(32, 30).addBox(-3.0F, 0.0F, -12.0F, 6.0F, 4.0F, 4.0F),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
    PartDefinition p_horn_l =
        p_head.addOrReplaceChild(
            "horn_l",
            CubeListBuilder.create().texOffs(52, 30).addBox(2.5F, -7.0F, -4.0F, 2.0F, 4.0F, 2.0F),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.35F, 0.0F, 0.3F));
    PartDefinition p_horn_r =
        p_head.addOrReplaceChild(
            "horn_r",
            CubeListBuilder.create().texOffs(60, 30).addBox(-4.5F, -7.0F, -4.0F, 2.0F, 4.0F, 2.0F),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.35F, 0.0F, -0.3F));
    PartDefinition p_leg0 =
        root.addOrReplaceChild(
            "leg0",
            CubeListBuilder.create().texOffs(0, 46).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 8.0F, 4.0F),
            PartPose.offsetAndRotation(4.5F, 16.0F, -7.0F, 0.0F, 0.0F, 0.0F));
    PartDefinition p_leg1 =
        root.addOrReplaceChild(
            "leg1",
            CubeListBuilder.create().texOffs(16, 46).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 8.0F, 4.0F),
            PartPose.offsetAndRotation(-4.5F, 16.0F, -7.0F, 0.0F, 0.0F, 0.0F));
    PartDefinition p_leg2 =
        root.addOrReplaceChild(
            "leg2",
            CubeListBuilder.create().texOffs(32, 46).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 8.0F, 4.0F),
            PartPose.offsetAndRotation(4.5F, 16.0F, 7.0F, 0.0F, 0.0F, 0.0F));
    PartDefinition p_leg3 =
        root.addOrReplaceChild(
            "leg3",
            CubeListBuilder.create().texOffs(48, 46).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 8.0F, 4.0F),
            PartPose.offsetAndRotation(-4.5F, 16.0F, 7.0F, 0.0F, 0.0F, 0.0F));
    PartDefinition p_plate0 =
        p_body.addOrReplaceChild(
            "plate0",
            CubeListBuilder.create().texOffs(68, 0).addBox(-4.0F, -3.0F, -2.0F, 8.0F, 3.0F, 4.0F),
            PartPose.offsetAndRotation(0.0F, -5.0F, -6.0F, -0.25F, 0.0F, 0.0F));
    PartDefinition p_plate1 =
        p_body.addOrReplaceChild(
            "plate1",
            CubeListBuilder.create().texOffs(68, 8).addBox(-4.0F, -3.0F, -2.0F, 8.0F, 3.0F, 4.0F),
            PartPose.offsetAndRotation(0.0F, -5.0F, 0.0F, -0.25F, 0.0F, 0.0F));
    PartDefinition p_plate2 =
        p_body.addOrReplaceChild(
            "plate2",
            CubeListBuilder.create().texOffs(68, 16).addBox(-4.0F, -3.0F, -2.0F, 8.0F, 3.0F, 4.0F),
            PartPose.offsetAndRotation(0.0F, -5.0F, 6.0F, -0.25F, 0.0F, 0.0F));
    PartDefinition p_tail =
        p_body.addOrReplaceChild(
            "tail",
            CubeListBuilder.create().texOffs(92, 0).addBox(-1.5F, -1.5F, 0.0F, 3.0F, 3.0F, 8.0F),
            PartPose.offsetAndRotation(0.0F, -2.0F, 10.0F, -0.55F, 0.0F, 0.0F));
    return LayerDefinition.create(mesh, 128, 64);
  }

  public static LayerDefinition voidskitter() {
    MeshDefinition mesh = new MeshDefinition();
    PartDefinition root = mesh.getRoot();
    PartDefinition p_body =
        root.addOrReplaceChild(
            "body",
            CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, -2.5F, -6.0F, 10.0F, 5.0F, 12.0F),
            PartPose.offsetAndRotation(0.0F, 19.0F, 0.0F, 0.0F, 0.0F, 0.0F));
    PartDefinition p_head =
        root.addOrReplaceChild(
            "head",
            CubeListBuilder.create().texOffs(0, 17).addBox(-3.0F, -2.0F, -5.0F, 6.0F, 4.0F, 5.0F),
            PartPose.offsetAndRotation(0.0F, 19.0F, -6.0F, 0.0F, 0.0F, 0.0F));
    PartDefinition p_leg0 =
        root.addOrReplaceChild(
            "leg0",
            CubeListBuilder.create().texOffs(0, 26).addBox(0.0F, -1.0F, -1.0F, 9.0F, 2.0F, 2.0F),
            PartPose.offsetAndRotation(5.0F, 20.0F, -4.0F, 0.0F, 0.35F, 0.45F));
    PartDefinition p_leg1 =
        root.addOrReplaceChild(
            "leg1",
            CubeListBuilder.create().texOffs(0, 30).addBox(0.0F, -1.0F, -1.0F, 9.0F, 2.0F, 2.0F),
            PartPose.offsetAndRotation(5.0F, 20.0F, 0.0F, 0.0F, -0.0F, 0.45F));
    PartDefinition p_leg2 =
        root.addOrReplaceChild(
            "leg2",
            CubeListBuilder.create().texOffs(0, 34).addBox(0.0F, -1.0F, -1.0F, 9.0F, 2.0F, 2.0F),
            PartPose.offsetAndRotation(5.0F, 20.0F, 4.0F, 0.0F, -0.35F, 0.45F));
    PartDefinition p_leg3 =
        root.addOrReplaceChild(
            "leg3",
            CubeListBuilder.create().texOffs(22, 26).addBox(-9.0F, -1.0F, -1.0F, 9.0F, 2.0F, 2.0F),
            PartPose.offsetAndRotation(-5.0F, 20.0F, -4.0F, 0.0F, -0.35F, -0.45F));
    PartDefinition p_leg4 =
        root.addOrReplaceChild(
            "leg4",
            CubeListBuilder.create().texOffs(22, 30).addBox(-9.0F, -1.0F, -1.0F, 9.0F, 2.0F, 2.0F),
            PartPose.offsetAndRotation(-5.0F, 20.0F, 0.0F, 0.0F, 0.0F, -0.45F));
    PartDefinition p_leg5 =
        root.addOrReplaceChild(
            "leg5",
            CubeListBuilder.create().texOffs(22, 34).addBox(-9.0F, -1.0F, -1.0F, 9.0F, 2.0F, 2.0F),
            PartPose.offsetAndRotation(-5.0F, 20.0F, 4.0F, 0.0F, 0.35F, -0.45F));
    PartDefinition p_mandible_l =
        p_head.addOrReplaceChild(
            "mandible_l",
            CubeListBuilder.create().texOffs(44, 17).addBox(0.0F, 0.0F, -2.0F, 1.0F, 2.0F, 2.0F),
            PartPose.offsetAndRotation(1.5F, 1.0F, -5.0F, 0.0F, -0.3F, 0.0F));
    PartDefinition p_mandible_r =
        p_head.addOrReplaceChild(
            "mandible_r",
            CubeListBuilder.create().texOffs(50, 17).addBox(-1.0F, 0.0F, -2.0F, 1.0F, 2.0F, 2.0F),
            PartPose.offsetAndRotation(-1.5F, 1.0F, -5.0F, 0.0F, 0.3F, 0.0F));
    PartDefinition p_spike0 =
        p_body.addOrReplaceChild(
            "spike0",
            CubeListBuilder.create().texOffs(44, 0).addBox(-1.0F, -5.0F, -1.0F, 2.0F, 5.0F, 2.0F),
            PartPose.offsetAndRotation(-2.0F, -2.5F, -3.0F, -0.3F, 0.0F, -0.35F));
    PartDefinition p_spike1 =
        p_body.addOrReplaceChild(
            "spike1",
            CubeListBuilder.create().texOffs(52, 0).addBox(-1.0F, -5.0F, -1.0F, 2.0F, 5.0F, 2.0F),
            PartPose.offsetAndRotation(2.0F, -2.5F, -1.0F, -0.2F, 0.0F, 0.35F));
    PartDefinition p_spike2 =
        p_body.addOrReplaceChild(
            "spike2",
            CubeListBuilder.create().texOffs(44, 7).addBox(-1.0F, -6.0F, -1.0F, 2.0F, 6.0F, 2.0F),
            PartPose.offsetAndRotation(-1.5F, -2.5F, 2.0F, 0.15F, 0.0F, -0.25F));
    PartDefinition p_spike3 =
        p_body.addOrReplaceChild(
            "spike3",
            CubeListBuilder.create().texOffs(54, 7).addBox(-1.0F, -4.0F, -1.0F, 2.0F, 4.0F, 2.0F),
            PartPose.offsetAndRotation(2.0F, -2.5F, 4.0F, 0.35F, 0.0F, 0.3F));
    return LayerDefinition.create(mesh, 64, 64);
  }

  public static LayerDefinition glimmerMoth() {
    MeshDefinition mesh = new MeshDefinition();
    PartDefinition root = mesh.getRoot();
    PartDefinition p_body =
        root.addOrReplaceChild(
            "body",
            CubeListBuilder.create().texOffs(0, 0).addBox(-1.5F, -1.5F, -3.5F, 3.0F, 3.0F, 7.0F),
            PartPose.offsetAndRotation(0.0F, 20.0F, 0.0F, 0.0F, 0.0F, 0.0F));
    PartDefinition p_head =
        root.addOrReplaceChild(
            "head",
            CubeListBuilder.create().texOffs(20, 0).addBox(-1.5F, -1.5F, -2.0F, 3.0F, 3.0F, 2.0F),
            PartPose.offsetAndRotation(0.0F, 20.0F, -3.5F, 0.0F, 0.0F, 0.0F));
    PartDefinition p_feelers =
        p_head.addOrReplaceChild(
            "feelers",
            CubeListBuilder.create().texOffs(30, 0).addBox(-2.0F, -3.0F, 0.0F, 4.0F, 3.0F, 0.0F),
            PartPose.offsetAndRotation(0.0F, -1.5F, -1.5F, -0.5F, 0.0F, 0.0F));
    PartDefinition p_wing_l =
        root.addOrReplaceChild(
            "wing_l",
            CubeListBuilder.create().texOffs(0, 10).addBox(0.0F, 0.0F, -4.0F, 9.0F, 0.0F, 8.0F),
            PartPose.offsetAndRotation(1.5F, 18.6F, 0.5F, 0.0F, 0.0F, -0.25F));
    PartDefinition p_wing_r =
        root.addOrReplaceChild(
            "wing_r",
            CubeListBuilder.create().texOffs(0, 18).addBox(-9.0F, 0.0F, -4.0F, 9.0F, 0.0F, 8.0F),
            PartPose.offsetAndRotation(-1.5F, 18.6F, 0.5F, 0.0F, 0.0F, 0.25F));
    return LayerDefinition.create(mesh, 64, 32);
  }

  public static LayerDefinition glowkit() {
    MeshDefinition mesh = new MeshDefinition();
    PartDefinition root = mesh.getRoot();
    PartDefinition p_body =
        root.addOrReplaceChild(
            "body",
            CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -3.0F, -5.0F, 6.0F, 6.0F, 10.0F),
            PartPose.offsetAndRotation(0.0F, 17.0F, 0.0F, 0.0F, 0.0F, 0.0F));
    PartDefinition p_head =
        root.addOrReplaceChild(
            "head",
            CubeListBuilder.create().texOffs(0, 32).addBox(-4.0F, -5.0F, -4.0F, 8.0F, 6.0F, 5.0F),
            PartPose.offsetAndRotation(0.0F, 15.0F, -5.0F, 0.0F, 0.0F, 0.0F));
    PartDefinition p_leg0 =
        root.addOrReplaceChild(
            "leg0",
            CubeListBuilder.create().texOffs(16, 44).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 4.0F, 3.0F),
            PartPose.offsetAndRotation(1.8F, 20.0F, -3.3F, 0.0F, 0.0F, 0.0F));
    PartDefinition p_leg1 =
        root.addOrReplaceChild(
            "leg1",
            CubeListBuilder.create().texOffs(28, 44).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 4.0F, 3.0F),
            PartPose.offsetAndRotation(-1.8F, 20.0F, -3.3F, 0.0F, 0.0F, 0.0F));
    PartDefinition p_leg2 =
        root.addOrReplaceChild(
            "leg2",
            CubeListBuilder.create().texOffs(40, 44).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 4.0F, 3.0F),
            PartPose.offsetAndRotation(1.8F, 20.0F, 3.3F, 0.0F, 0.0F, 0.0F));
    PartDefinition p_leg3 =
        root.addOrReplaceChild(
            "leg3",
            CubeListBuilder.create().texOffs(52, 44).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 4.0F, 3.0F),
            PartPose.offsetAndRotation(-1.8F, 20.0F, 3.3F, 0.0F, 0.0F, 0.0F));
    PartDefinition p_ruff =
        p_body.addOrReplaceChild(
            "ruff",
            CubeListBuilder.create().texOffs(32, 0).addBox(-3.5F, -2.0F, -1.5F, 7.0F, 5.0F, 3.0F),
            PartPose.offsetAndRotation(0.0F, -1.0F, -5.0F, 0.0F, 0.0F, 0.0F));
    PartDefinition p_shell =
        p_body.addOrReplaceChild(
            "shell",
            CubeListBuilder.create().texOffs(0, 16).addBox(-2.5F, -6.0F, -3.5F, 5.0F, 6.0F, 7.0F),
            PartPose.offsetAndRotation(0.0F, -2.0F, 1.0F, 0.0F, 0.0F, 0.0F));
    PartDefinition p_tail =
        p_body.addOrReplaceChild(
            "tail",
            CubeListBuilder.create().texOffs(32, 8).addBox(-2.5F, -2.5F, 0.0F, 5.0F, 5.0F, 8.0F),
            PartPose.offsetAndRotation(0.0F, 0.0F, 5.0F, -0.45F, 0.0F, 0.0F));
    PartDefinition p_hair =
        p_head.addOrReplaceChild(
            "hair",
            CubeListBuilder.create().texOffs(26, 32).addBox(-4.5F, -6.0F, -4.5F, 9.0F, 3.0F, 6.0F),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
    PartDefinition p_ear_l =
        p_head.addOrReplaceChild(
            "ear_l",
            CubeListBuilder.create().texOffs(56, 32).addBox(-1.0F, -2.0F, -0.5F, 2.0F, 2.0F, 1.0F),
            PartPose.offsetAndRotation(3.0F, -5.5F, -1.0F, 0.0F, 0.0F, 0.3F));
    PartDefinition p_ear_r =
        p_head.addOrReplaceChild(
            "ear_r",
            CubeListBuilder.create().texOffs(56, 36).addBox(-1.0F, -2.0F, -0.5F, 2.0F, 2.0F, 1.0F),
            PartPose.offsetAndRotation(-3.0F, -5.5F, -1.0F, 0.0F, 0.0F, -0.3F));
    PartDefinition p_bang =
        p_head.addOrReplaceChild(
            "bang",
            CubeListBuilder.create().texOffs(56, 40).addBox(-1.0F, -3.0F, -4.7F, 2.0F, 2.0F, 0.0F),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
    PartDefinition p_tuft =
        p_head.addOrReplaceChild(
            "tuft",
            CubeListBuilder.create().texOffs(0, 52).addBox(-1.5F, -1.0F, 0.0F, 3.0F, 3.0F, 4.0F),
            PartPose.offsetAndRotation(0.0F, -4.5F, 1.0F, 0.6F, 0.0F, 0.0F));
    PartDefinition p_goo_l =
        p_head.addOrReplaceChild(
            "goo_l",
            CubeListBuilder.create().texOffs(0, 44).addBox(-1.0F, -7.0F, -0.5F, 2.0F, 7.0F, 1.0F),
            PartPose.offsetAndRotation(1.3F, -5.5F, 0.0F, 0.25F, 0.0F, 0.2F));
    PartDefinition p_goo_r =
        p_head.addOrReplaceChild(
            "goo_r",
            CubeListBuilder.create().texOffs(8, 44).addBox(-1.0F, -7.0F, -0.5F, 2.0F, 7.0F, 1.0F),
            PartPose.offsetAndRotation(-1.3F, -5.5F, 0.0F, 0.25F, 0.0F, -0.2F));
    return LayerDefinition.create(mesh, 64, 64);
  }

  private AlienMobLayers() {}
}
