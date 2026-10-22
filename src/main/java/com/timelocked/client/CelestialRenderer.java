package com.timelocked.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.timelocked.CelestialVillager;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public final class CelestialRenderer extends MobRenderer<CelestialVillager, CelestialModel> {
  public static final ModelLayerLocation LAYER =
      new ModelLayerLocation(
          ResourceLocation.fromNamespaceAndPath("timelocked", "celestial"), "main");

  private static ResourceLocation tex(CelestialVillager e, String suffix) {
    return ResourceLocation.fromNamespaceAndPath(
        "timelocked",
        "textures/entity/celestial/" + CelestialVillager.KIND_IDS[e.kind()] + suffix + ".png");
  }

  public CelestialRenderer(Context c) {
    super(c, new CelestialModel(c.bakeLayer(LAYER)), 0.45F);
    this.addLayer(new CelestialRenderer.Glow(this));
  }

  public ResourceLocation getTextureLocation(CelestialVillager e) {
    return tex(e, "");
  }

  protected void scale(CelestialVillager e, PoseStack pose, float partial) {
    if (e.isBaby()) {
      pose.scale(0.55F, 0.55F, 0.55F);
    } else if (e.kind() == 5) {
      pose.scale(1.2F, 1.2F, 1.2F);
    }
  }

  private static final class Glow extends RenderLayer<CelestialVillager, CelestialModel> {
    Glow(CelestialRenderer r) {
      super(r);
    }

    public void render(
        PoseStack pose,
        MultiBufferSource buf,
        int light,
        CelestialVillager e,
        float a,
        float b,
        float partial,
        float age,
        float yaw,
        float pitch) {
      ((CelestialModel) this.getParentModel())
          .renderToBuffer(
              pose,
              buf.getBuffer(RenderType.eyes(CelestialRenderer.tex(e, "_glow"))),
              15728640,
              OverlayTexture.NO_OVERLAY,
              -1);
    }
  }
}
