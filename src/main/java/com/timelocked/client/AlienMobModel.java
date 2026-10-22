package com.timelocked.client;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

public final class AlienMobModel<T extends Entity> extends HierarchicalModel<T> {
  private final ModelPart root;
  private final ModelPart body;
  private final ModelPart head;
  private final ModelPart tail;
  private final ModelPart wl;
  private final ModelPart wr;
  private final ModelPart gl;
  private final ModelPart gr;
  private final ModelPart[] legs;
  private final AlienMobModel.Kind kind;

  public AlienMobModel(ModelPart root, AlienMobModel.Kind kind) {
    this.root = root;
    this.kind = kind;
    this.body = this.find("body");
    this.head = this.find("head");
    this.tail = this.find("tail");
    this.wl = this.find("wing_l");
    this.wr = this.find("wing_r");
    this.gl = this.find("goo_l");
    this.gr = this.find("goo_r");
    int n =
        kind != AlienMobModel.Kind.ASHBACK && kind != AlienMobModel.Kind.GLOWKIT
            ? (kind == AlienMobModel.Kind.SKITTER ? 6 : 0)
            : 4;
    this.legs = new ModelPart[n];

    for (int i = 0; i < n; i++) {
      this.legs[i] = this.find("leg" + i);
    }
  }

  private ModelPart find(String n) {
    if (this.root.hasChild(n)) {
      return this.root.getChild(n);
    }

    for (String p : new String[] {"body", "head"}) {
      if (this.root.hasChild(p) && this.root.getChild(p).hasChild(n)) {
        return this.root.getChild(p).getChild(n);
      }
    }

    return null;
  }

  public ModelPart root() {
    return this.root;
  }

  public void setupAnim(T e, float swing, float amount, float age, float yaw, float pitch) {
    this.root.getAllParts().forEach(ModelPart::resetPose);
    if (this.head != null) {
      this.head.yRot =
          this.head.yRot
              + yaw
                  * (float) (Math.PI / 180.0)
                  * (this.kind == AlienMobModel.Kind.MOTH ? 0.3F : 0.8F);
      this.head.xRot += pitch * (float) (Math.PI / 180.0) * 0.6F;
    }

    switch (this.kind) {
      case ASHBACK:
        for (int i = 0; i < 4; i++) {
          this.legs[i].xRot =
              Mth.cos(swing * 0.6662F + (i != 0 && i != 3 ? (float) Math.PI : 0.0F))
                  * 1.1F
                  * amount;
        }

        this.head.xRot =
            this.head.xRot
                + (Mth.cos(swing * 0.6662F) * 0.08F * amount + Mth.sin(age * 0.05F) * 0.03F);
        if (this.tail != null) {
          this.tail.yRot = Mth.sin(age * 0.09F) * 0.3F;
          this.tail.xRot = this.tail.xRot + Mth.cos(swing * 0.6662F) * 0.15F * amount;
        }
        break;
      case SKITTER:
        for (int i = 0; i < 6; i++) {
          float side = i < 3 ? 1.0F : -1.0F;
          int k = i < 3 ? i : i - 2;
          float ph = swing * 1.4F + (k % 2 == 0 ? 0.0F : (float) Math.PI);
          this.legs[i].yRot = this.legs[i].yRot + Mth.cos(ph) * 0.45F * amount * side;
          this.legs[i].zRot = this.legs[i].zRot - side * Math.abs(Mth.sin(ph)) * 0.35F * amount;
        }

        this.body.y = this.body.y + Mth.sin(age * 0.3F) * 0.15F;
        break;
      case MOTH:
        float f = Mth.sin(age * 1.7F) * 0.75F;
        this.wl.zRot -= f;
        this.wr.zRot += f;
        float bob = Mth.sin(age * 0.15F) * 0.6F;
        this.body.y += bob;
        this.head.y += bob;
        this.wl.y += bob;
        this.wr.y += bob;
        break;
      case GLOWKIT:
        for (int i = 0; i < 4; i++) {
          this.legs[i].xRot =
              Mth.cos(swing * 0.9F + (i != 0 && i != 3 ? (float) Math.PI : 0.0F)) * 1.2F * amount;
        }

        this.tail.yRot = Mth.sin(age * 0.12F) * 0.35F;
        this.tail.xRot = this.tail.xRot + Mth.sin(age * 0.07F) * 0.08F;
        float w = Mth.sin(age * 0.2F) * 0.08F + Mth.cos(swing * 0.9F) * 0.15F * amount;
        this.gl.xRot += w;
        this.gr.xRot += w;
        this.gl.zRot = this.gl.zRot + Mth.sin(age * 0.13F) * 0.05F;
        this.gr.zRot = this.gr.zRot - Mth.sin(age * 0.13F) * 0.05F;
        this.head.zRot = this.head.zRot + Mth.sin(age * 0.05F) * 0.04F;
    }
  }

  public enum Kind {
    ASHBACK,
    SKITTER,
    MOTH,
    GLOWKIT;
  }
}
