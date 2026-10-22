package com.timelocked.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

public final class AlienMobRenderer<T extends Mob> extends MobRenderer<T, AlienMobModel<T>> {
  public static final ModelLayerLocation ASHBACK = layer("ashback");
  public static final ModelLayerLocation SKITTER = layer("voidskitter");
  public static final ModelLayerLocation MOTH = layer("glimmer_moth");
  public static final ModelLayerLocation GLOWKIT = layer("glowkit");
  private final ResourceLocation texture;
  private final ResourceLocation glow;
  private final float scale;

  private static ModelLayerLocation layer(String n) {
    return new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("timelocked", n), "main");
  }

  public AlienMobRenderer(
      Context c,
      ModelLayerLocation l,
      AlienMobModel.Kind k,
      String name,
      float shadow,
      float scale) {
    super(c, new AlienMobModel(c.bakeLayer(l), k), shadow);
    this.scale = scale;
    this.texture =
        ResourceLocation.fromNamespaceAndPath(
            "timelocked", "textures/entity/voidspace/" + name + ".png");
    this.glow =
        ResourceLocation.fromNamespaceAndPath(
            "timelocked", "textures/entity/voidspace/" + name + "_glow.png");
    final RenderType eyes = RenderType.eyes(this.glow);
    this.addLayer(
        new EyesLayer<T, AlienMobModel<T>>(this) {
          public RenderType renderType() {
            return eyes;
          }
        });
  }

  public ResourceLocation getTextureLocation(T e) {
    return this.texture;
  }

  protected void scale(T e, PoseStack pose, float partial) {
    float s = e.isBaby() ? this.scale * 0.5F : this.scale;
    pose.scale(s, s, s);
  }
}
