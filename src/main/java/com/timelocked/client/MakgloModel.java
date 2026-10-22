package com.timelocked.client;

import com.timelocked.Makglo;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

public final class MakgloModel extends HierarchicalModel<Makglo> {
  private final ModelPart root;

  public MakgloModel(ModelPart part) {
    this.root = part;
  }

  public ModelPart root() {
    return this.root;
  }

  public static LayerDefinition layer() {
    MeshDefinition mesh = new MeshDefinition();
    PartDefinition root =
        mesh.getRoot()
            .addOrReplaceChild(
                "body", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));
    root.addOrReplaceChild(
        "torso",
        CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, 0.0F, -3.0F, 10.0F, 12.0F, 6.0F),
        PartPose.offsetAndRotation(0.0F, -22.0F, 0.0F, 0.0F, 0.0F, 0.0F));
    PartDefinition head =
        root.addOrReplaceChild(
            "head",
            CubeListBuilder.create().texOffs(40, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F),
            PartPose.offsetAndRotation(0.0F, -27.0F, 0.0F, 0.0F, 0.0F, 0.0F));
    head.addOrReplaceChild(
        "muzzle",
        CubeListBuilder.create().texOffs(80, 0).addBox(-3.0F, 1.0F, -5.0F, 6.0F, 3.0F, 2.0F),
        PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
    PartDefinition left_ear =
        head.addOrReplaceChild(
            "left_ear",
            CubeListBuilder.create().texOffs(0, 28).addBox(-2.5F, -2.0F, -1.0F, 5.0F, 3.0F, 2.0F),
            PartPose.offsetAndRotation(-3.0F, -3.0F, 0.0F, 0.0F, 0.0F, -0.35F));
    PartDefinition right_ear =
        head.addOrReplaceChild(
            "right_ear",
            CubeListBuilder.create().texOffs(14, 28).addBox(-2.5F, -2.0F, -1.0F, 5.0F, 3.0F, 2.0F),
            PartPose.offsetAndRotation(3.0F, -3.0F, 0.0F, 0.0F, 0.0F, 0.35F));
    PartDefinition left_arm =
        root.addOrReplaceChild(
            "left_arm",
            CubeListBuilder.create().texOffs(32, 26).addBox(-2.0F, -1.0F, -2.0F, 4.0F, 14.0F, 4.0F),
            PartPose.offsetAndRotation(-7.0F, -21.0F, 0.0F, 0.0F, 0.0F, 0.1F));
    PartDefinition right_arm =
        root.addOrReplaceChild(
            "right_arm",
            CubeListBuilder.create().texOffs(50, 26).addBox(-2.0F, -1.0F, -2.0F, 4.0F, 14.0F, 4.0F),
            PartPose.offsetAndRotation(7.0F, -21.0F, 0.0F, 0.0F, 0.0F, -0.1F));
    left_arm.addOrReplaceChild(
        "left_claw",
        CubeListBuilder.create().texOffs(72, 22).addBox(-3.0F, 0.0F, -3.0F, 6.0F, 4.0F, 6.0F),
        PartPose.offsetAndRotation(0.0F, 13.0F, 0.0F, 0.0F, 0.0F, 0.12F));
    right_arm.addOrReplaceChild(
        "right_claw",
        CubeListBuilder.create().texOffs(72, 38).addBox(-3.0F, 0.0F, -3.0F, 6.0F, 4.0F, 6.0F),
        PartPose.offsetAndRotation(0.0F, 13.0F, 0.0F, 0.0F, 0.0F, -0.12F));
    root.addOrReplaceChild(
        "left_leg",
        CubeListBuilder.create().texOffs(0, 44).addBox(-2.5F, 0.0F, -3.0F, 5.0F, 10.0F, 6.0F),
        PartPose.offsetAndRotation(-3.0F, -10.0F, 0.0F, 0.0F, 0.0F, 0.0F));
    root.addOrReplaceChild(
        "right_leg",
        CubeListBuilder.create().texOffs(24, 44).addBox(-2.5F, 0.0F, -3.0F, 5.0F, 10.0F, 6.0F),
        PartPose.offsetAndRotation(3.0F, -10.0F, 0.0F, 0.0F, 0.0F, 0.0F));
    PartDefinition tail_base =
        root.addOrReplaceChild(
            "tail_base",
            CubeListBuilder.create().texOffs(0, 65).addBox(-2.0F, -2.0F, 0.0F, 4.0F, 4.0F, 10.0F),
            PartPose.offsetAndRotation(0.0F, -10.0F, 3.0F, -0.1F, 0.0F, 0.0F));
    PartDefinition tail_tip =
        tail_base.addOrReplaceChild(
            "tail_tip",
            CubeListBuilder.create().texOffs(36, 65).addBox(-2.0F, -1.0F, 0.0F, 4.0F, 3.0F, 9.0F),
            PartPose.offsetAndRotation(0.0F, -1.0F, 9.0F, -0.15F, 0.0F, 0.0F));
    tail_tip.addOrReplaceChild(
        "upper_fin",
        CubeListBuilder.create().texOffs(70, 65).addBox(-0.5F, -6.0F, 0.0F, 1.0F, 7.0F, 9.0F),
        PartPose.offsetAndRotation(0.0F, 0.0F, 7.0F, -0.4F, 0.0F, 0.0F));
    tail_tip.addOrReplaceChild(
        "lower_fin",
        CubeListBuilder.create().texOffs(94, 65).addBox(-0.5F, 0.0F, 0.0F, 1.0F, 7.0F, 9.0F),
        PartPose.offsetAndRotation(0.0F, 0.0F, 7.0F, 0.4F, 0.0F, 0.0F));
    head.addOrReplaceChild(
        "antenna",
        CubeListBuilder.create().texOffs(0, 89).addBox(-0.5F, -7.0F, -0.5F, 1.0F, 7.0F, 1.0F),
        PartPose.offsetAndRotation(0.0F, -4.0F, 0.0F, 0.0F, 0.0F, -0.2F));
    head.addOrReplaceChild(
        "antenna_tip",
        CubeListBuilder.create().texOffs(8, 89).addBox(-0.5F, -1.0F, -0.5F, 3.0F, 1.0F, 1.0F),
        PartPose.offsetAndRotation(-1.2F, -10.5F, 0.0F, 0.0F, 0.0F, 0.2F));
    left_ear.addOrReplaceChild(
        "left_ear_mid",
        CubeListBuilder.create().texOffs(0, 102).addBox(-1.5F, -5.0F, -1.0F, 3.0F, 3.0F, 2.0F),
        PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
    left_ear.addOrReplaceChild(
        "left_ear_upper",
        CubeListBuilder.create().texOffs(12, 102).addBox(-1.0F, -7.0F, -1.0F, 2.0F, 2.0F, 2.0F),
        PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
    left_ear.addOrReplaceChild(
        "left_ear_tip",
        CubeListBuilder.create().texOffs(22, 102).addBox(-0.5F, -9.0F, -1.0F, 1.0F, 2.0F, 2.0F),
        PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
    right_ear.addOrReplaceChild(
        "right_ear_mid",
        CubeListBuilder.create().texOffs(0, 102).addBox(-1.5F, -5.0F, -1.0F, 3.0F, 3.0F, 2.0F),
        PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
    right_ear.addOrReplaceChild(
        "right_ear_upper",
        CubeListBuilder.create().texOffs(12, 102).addBox(-1.0F, -7.0F, -1.0F, 2.0F, 2.0F, 2.0F),
        PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
    right_ear.addOrReplaceChild(
        "right_ear_tip",
        CubeListBuilder.create().texOffs(22, 102).addBox(-0.5F, -9.0F, -1.0F, 1.0F, 2.0F, 2.0F),
        PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
    return LayerDefinition.create(mesh, 128, 128);
  }

  public void setupAnim(
      Makglo entity, float swing, float amount, float age, float yaw, float pitch) {
    this.root.getAllParts().forEach(ModelPart::resetPose);
    ModelPart body = this.root.getChild("body");
    body.getChild("head").yRot = yaw * (float) (Math.PI / 180.0);
    body.getChild("head").xRot = pitch * (float) (Math.PI / 180.0);
    float step = Mth.cos(swing * 0.6662F) * amount;
    body.getChild("left_leg").xRot = step;
    body.getChild("right_leg").xRot = -step;
    body.getChild("left_arm").xRot = -step * 0.8F;
    body.getChild("right_arm").xRot = step * 0.8F;
    body.getChild("tail_base").yRot = Mth.sin(age * 0.09F) * 0.15F;
    body.getChild("tail_base").getChild("tail_tip").yRot = Mth.sin(age * 0.09F + 0.5F) * 0.2F;
    if (entity.isAggressive()) {
      body.getChild("left_arm").xRot -= 0.8F;
      body.getChild("right_arm").xRot -= 0.8F;
    }

    float attack = Mth.sin(entity.getAttackAnim(0.0F) * (float) Math.PI);
    body.getChild("right_arm").xRot -= attack;
    body.getChild("left_arm").xRot -= attack;
  }
}
