package com.timelocked.client;

import com.timelocked.Pantheris;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;

public final class PantherisRenderer extends MobRenderer<Pantheris, PantherisModel> {
  private static final ResourceLocation TEXTURE =
      ResourceLocation.fromNamespaceAndPath("timelocked", "textures/entity/pantheris.png");

  public PantherisRenderer(EntityRendererProvider.Context c) {
    super(c, new PantherisModel(), .4f);
  }

  public ResourceLocation getTextureLocation(Pantheris e) {
    return TEXTURE;
  }
}
