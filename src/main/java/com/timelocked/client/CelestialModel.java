package com.timelocked.client;

import com.timelocked.CelestialVillager;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

public final class CelestialModel extends HierarchicalModel<CelestialVillager> {
  private final ModelPart root;
  private final ModelPart body;
  private final ModelPart head;
  private final ModelPart rightArm;
  private final ModelPart leftArm;
  private final ModelPart rightLeg;
  private final ModelPart leftLeg;
  private final ModelPart tail;
  private final ModelPart wingL;
  private final ModelPart wingR;
  private final ModelPart hairLong;
  private final ModelPart skirt;
  private final ModelPart mantle;
  private final ModelPart earL;
  private final ModelPart earR;

  public CelestialModel(ModelPart root) {
    this.root = root;
    this.body = root.getChild("body");
    this.head = root.getChild("head");
    this.rightArm = root.getChild("right_arm");
    this.leftArm = root.getChild("left_arm");
    this.rightLeg = root.getChild("right_leg");
    this.leftLeg = root.getChild("left_leg");
    this.tail = this.body.getChild("tail");
    this.wingL = this.body.getChild("wing_l");
    this.wingR = this.body.getChild("wing_r");
    this.skirt = this.body.getChild("skirt");
    this.mantle = this.body.getChild("mantle");
    this.earL = this.head.getChild("ear_l");
    this.earR = this.head.getChild("ear_r");
    this.hairLong = this.head.getChild("hair_long");
  }

  public ModelPart root() {
    return this.root;
  }

  public void setupAnim(
      CelestialVillager e, float swing, float amount, float age, float yaw, float pitch) {
    this.root.getAllParts().forEach(ModelPart::resetPose);
    int k = e.kind();
    boolean ecl = k >= 3;
    this.wingL.visible = this.wingR.visible = k != 0 && !ecl;
    this.hairLong.visible = k == 1 || k == 4 || k == 5;
    this.skirt.visible = k == 2 || k == 3;
    this.mantle.visible = ecl;
    float es = ecl ? 1.55F : 1.0F;
    this.earL.xScale = this.earR.xScale = es;
    this.earL.yScale = this.earR.yScale = ecl ? 1.9F : 1.0F;
    this.head.yRot = yaw * (float) (Math.PI / 180.0);
    this.head.xRot = pitch * (float) (Math.PI / 180.0);
    float c = Mth.cos(swing * 0.6662F);
    this.rightArm.xRot = -c * amount * 0.9F;
    this.leftArm.xRot = c * amount * 0.9F;
    this.rightLeg.xRot = c * 1.3F * amount;
    this.leftLeg.xRot = -c * 1.3F * amount;
    this.rightArm.zRot = Mth.cos(age * 0.09F) * 0.04F + 0.04F;
    this.leftArm.zRot = -Mth.cos(age * 0.09F) * 0.04F - 0.04F;
    if (this.attackTime > 0.0F) {
      float a = Mth.sin(this.attackTime * (float) Math.PI);
      this.rightArm.xRot -= a * 1.7F;
      this.rightArm.yRot = -a * 0.3F;
    }

    this.tail.yRot = Mth.sin(age * 0.1F) * 0.35F;
    this.tail.xRot = this.tail.xRot + Mth.cos(swing * 0.6662F) * 0.12F * amount;
    float flap = Mth.sin(age * (k == 1 ? 0.16F : 0.08F)) * 0.18F;
    this.wingL.yRot += flap;
    this.wingR.yRot -= flap;
  }
}
