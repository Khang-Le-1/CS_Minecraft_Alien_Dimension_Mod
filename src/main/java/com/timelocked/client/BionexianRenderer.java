package com.timelocked.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.timelocked.Greed;
import com.timelocked.Makglo;
import com.timelocked.Unnamed;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Renderer shared by the Unnamed and Greed Bionexians. Textures live in textures/entity/bionexian/.
 */
public final class BionexianRenderer<T extends Makglo> extends MobRenderer<T, BionexianModel<T>> {
  public static final ModelLayerLocation UNNAMED_LAYER =
      new ModelLayerLocation(
          ResourceLocation.fromNamespaceAndPath("timelocked", "unnamed"), "main");
  public static final ModelLayerLocation GREED_LAYER =
      new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("timelocked", "greed"), "main");
  private final ResourceLocation texture;
  private final ResourceLocation glowTexture;

  private BionexianRenderer(Context ctx, ModelLayerLocation layer, String name, float shadow) {
    super(ctx, new BionexianModel<>(ctx.bakeLayer(layer)), shadow);
    this.texture =
        ResourceLocation.fromNamespaceAndPath(
            "timelocked", "textures/entity/bionexian/" + name + ".png");
    this.glowTexture =
        ResourceLocation.fromNamespaceAndPath(
            "timelocked", "textures/entity/bionexian/" + name + "_glow.png");
    this.addLayer(new Glow<>(this));
  }

  public static BionexianRenderer<Unnamed> unnamed(Context ctx) {
    return new BionexianRenderer<>(ctx, UNNAMED_LAYER, "unnamed", 0.55F);
  }

  public static BionexianRenderer<Greed> greed(Context ctx) {
    return new BionexianRenderer<>(ctx, GREED_LAYER, "greed", 0.7F);
  }

  public ResourceLocation getTextureLocation(T entity) {
    return this.texture;
  }

  private static final class Glow<T extends Makglo> extends RenderLayer<T, BionexianModel<T>> {
    private final BionexianRenderer<T> owner;

    Glow(BionexianRenderer<T> renderer) {
      super(renderer);
      this.owner = renderer;
    }

    public void render(
        PoseStack pose,
        MultiBufferSource buffer,
        int light,
        T entity,
        float a,
        float b,
        float partial,
        float age,
        float yaw,
        float pitch) {
      this.getParentModel()
          .renderToBuffer(
              pose,
              buffer.getBuffer(RenderType.eyes(this.owner.glowTexture)),
              15728640,
              OverlayTexture.NO_OVERLAY,
              -1);
    }
  }
}
