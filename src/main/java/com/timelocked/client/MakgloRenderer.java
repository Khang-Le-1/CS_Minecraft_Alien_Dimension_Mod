package com.timelocked.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.timelocked.Makglo;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public final class MakgloRenderer extends MobRenderer<Makglo, MakgloModel> {
  public static final ModelLayerLocation LAYER =
      new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("timelocked", "makglo"), "main");

  public MakgloRenderer(Context ctx) {
    super(ctx, new MakgloModel(ctx.bakeLayer(LAYER)), 0.6F);
    this.addLayer(new MakgloRenderer.Glow(this));
  }

  private static String expression(Makglo m) {
    return m.deathTime > 0
        ? "dead"
        : (m.hurtTime > 0
            ? "hurt"
            : (m.getAttackAnim(0.0F) > 0.01F
                ? "attacking"
                : (m.getHealth() <= m.getMaxHealth() * 0.3 ? "low_health" : "idle")));
  }

  public ResourceLocation getTextureLocation(Makglo m) {
    return ResourceLocation.fromNamespaceAndPath(
        "timelocked", "textures/entity/makglo/" + expression(m) + ".png");
  }

  private static final class Glow extends RenderLayer<Makglo, MakgloModel> {
    Glow(MakgloRenderer r) {
      super(r);
    }

    public void render(
        PoseStack pose,
        MultiBufferSource buffer,
        int light,
        Makglo m,
        float a,
        float b,
        float partial,
        float age,
        float yaw,
        float pitch) {
      ResourceLocation texture =
          ResourceLocation.fromNamespaceAndPath(
              "timelocked", "textures/entity/makglo/" + MakgloRenderer.expression(m) + "_glow.png");
      ((MakgloModel) this.getParentModel())
          .renderToBuffer(
              pose,
              buffer.getBuffer(RenderType.eyes(texture)),
              15728640,
              OverlayTexture.NO_OVERLAY,
              -1);
    }
  }
}
