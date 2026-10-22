package com.timelocked.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.timelocked.Dawnbringer;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public final class DawnbringerRenderer extends MobRenderer<Dawnbringer, DawnbringerModel> {
  public static final ModelLayerLocation LAYER =
      new ModelLayerLocation(
          ResourceLocation.fromNamespaceAndPath("timelocked", "dawnbringer"), "main");

  private static ResourceLocation tex(Dawnbringer e, String suffix) {
    return ResourceLocation.fromNamespaceAndPath(
        "timelocked", "textures/entity/dawnbringer/" + Dawnbringer.IDS[e.kind()] + suffix + ".png");
  }

  public DawnbringerRenderer(Context c) {
    super(c, new DawnbringerModel(c.bakeLayer(LAYER)), 0.5F);
    this.addLayer(new DawnbringerRenderer.Glow(this));
  }

  public ResourceLocation getTextureLocation(Dawnbringer e) {
    return tex(e, "");
  }

  protected void scale(Dawnbringer e, PoseStack pose, float partial) {
    if (e.kind() == 2) {
      pose.scale(1.33F, 1.33F, 1.33F);
    }
  }

  private static final class Glow extends RenderLayer<Dawnbringer, DawnbringerModel> {
    Glow(DawnbringerRenderer r) {
      super(r);
    }

    public void render(
        PoseStack pose,
        MultiBufferSource buf,
        int light,
        Dawnbringer e,
        float a,
        float b,
        float partial,
        float age,
        float yaw,
        float pitch) {
      ((DawnbringerModel) this.getParentModel())
          .renderToBuffer(
              pose,
              buf.getBuffer(RenderType.eyes(DawnbringerRenderer.tex(e, "_glow"))),
              15728640,
              OverlayTexture.NO_OVERLAY,
              -1);
    }
  }
}
