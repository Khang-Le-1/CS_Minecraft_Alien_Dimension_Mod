package com.timelocked.client;

import com.timelocked.Dawnbringer;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

public final class DawnbringerModel extends HierarchicalModel<Dawnbringer> {
  private final ModelPart root;
  private final ModelPart body;
  private final ModelPart head;
  private final ModelPart rightArm;
  private final ModelPart leftArm;
  private final ModelPart rightLeg;
  private final ModelPart leftLeg;
  private final ModelPart cape;
  private final ModelPart crest;
  private final ModelPart pauldronR;
  private final ModelPart pauldronL;
  private final ModelPart tabard;
  private final ModelPart faulds;
  private final ModelPart robe;
  private final ModelPart shaft;
  private final ModelPart blade;
  private final ModelPart disc;
  private final ModelPart sword;

  public DawnbringerModel(ModelPart root) {
    this.root = root;
    this.body = root.getChild("body");
    this.head = root.getChild("head");
    this.rightArm = root.getChild("right_arm");
    this.leftArm = root.getChild("left_arm");
    this.rightLeg = root.getChild("right_leg");
    this.leftLeg = root.getChild("left_leg");
    this.cape = this.body.getChild("cape");
    this.tabard = this.body.getChild("tabard");
    this.faulds = this.body.getChild("faulds");
    this.robe = this.body.getChild("robe");
    this.crest = this.head.getChild("crest");
    this.pauldronR = this.rightArm.getChild("pauldron_r");
    this.pauldronL = this.leftArm.getChild("pauldron_l");
    this.shaft = this.rightArm.getChild("shaft");
    this.blade = this.shaft.getChild("blade");
    this.disc = this.shaft.getChild("disc");
    this.sword = this.rightArm.getChild("sword");
  }

  public ModelPart root() {
    return this.root;
  }

  public void setupAnim(
      Dawnbringer e, float swing, float amount, float age, float yaw, float pitch) {
    this.root.getAllParts().forEach(ModelPart::resetPose);
    int k = e.kind();
    boolean leg = k == 0;
    boolean speak = k == 1;
    boolean war = k == 2;
    this.pauldronR.visible = this.pauldronL.visible = !speak;
    this.tabard.visible = leg;
    this.faulds.visible = !speak;
    this.robe.visible = speak;
    this.cape.visible = !speak;
    this.shaft.visible = !war;
    this.blade.visible = leg;
    this.disc.visible = speak;
    this.sword.visible = war;
    this.head.yRot = yaw * (float) (Math.PI / 180.0);
    this.head.xRot = pitch * (float) (Math.PI / 180.0);
    float c = Mth.cos(swing * 0.6662F);
    this.rightArm.xRot = -c * amount * 0.5F;
    this.leftArm.xRot = c * amount * 0.9F;
    this.rightLeg.xRot = c * 1.3F * amount;
    this.leftLeg.xRot = -c * 1.3F * amount;
    if (speak) {
      this.robe.xRot = Math.abs(c) * amount * 0.1F;
    }

    this.cape.xRot = this.cape.xRot + (amount * 0.5F + Mth.sin(age * 0.07F) * 0.03F);
    this.rightArm.zRot = Mth.cos(age * 0.09F) * 0.03F + 0.03F;
    this.leftArm.zRot = -Mth.cos(age * 0.09F) * 0.03F - 0.03F;
    if (e.casting()) {
      this.rightArm.xRot = -2.4F;
      this.leftArm.xRot = -2.0F;
      this.leftArm.zRot = -0.3F;
    }

    if (this.attackTime > 0.0F) {
      float a = Mth.sin(this.attackTime * (float) Math.PI);
      this.rightArm.xRot -= a * (war ? 2.2F : 1.7F);
      this.rightArm.yRot = -a * 0.3F;
      this.body.yRot = a * 0.15F;
    }
  }
}
