package com.timelocked.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.timelocked.Content;
import com.timelocked.SarcLiftEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class SarcLiftRenderer extends EntityRenderer<SarcLiftEntity> {
  private final BlockRenderDispatcher blocks;
  private static final int FULL_BRIGHT = 15728880;

  public SarcLiftRenderer(Context c) {
    super(c);
    this.blocks = c.getBlockRenderDispatcher();
    this.shadowRadius = 1.5F;
  }

  public ResourceLocation getTextureLocation(SarcLiftEntity e) {
    return InventoryMenu.BLOCK_ATLAS;
  }

  private void box(
      PoseStack p,
      MultiBufferSource b,
      int light,
      BlockState s,
      double x0,
      double y0,
      double z0,
      double x1,
      double y1,
      double z1) {
    p.pushPose();
    p.translate(x0, y0, z0);
    p.scale((float) (x1 - x0), (float) (y1 - y0), (float) (z1 - z0));
    this.blocks.renderSingleBlock(s, p, b, light, OverlayTexture.NO_OVERLAY);
    p.popPose();
  }

  public void render(
      SarcLiftEntity e, float yaw, float partial, PoseStack p, MultiBufferSource b, int light) {
    BlockState floor = ((Block) Content.FLOOR.get()).defaultBlockState();
    BlockState wall = ((Block) Content.WALL.get()).defaultBlockState();
    BlockState trim = ((Block) Content.CASING.get()).defaultBlockState();
    BlockState lamp = Blocks.SEA_LANTERN.defaultBlockState();
    this.box(p, b, light, floor, -1.5, 0.0, -1.5, 1.5, 0.25, 1.5);
    this.box(p, b, light, wall, -1.5, 0.25, -1.5, -1.38, 2.6, 1.5);
    this.box(p, b, light, wall, -1.38, 0.25, -1.5, 1.5, 2.6, -1.38);
    this.box(p, b, light, wall, -1.38, 0.25, 1.38, 1.5, 2.6, 1.5);
    this.box(p, b, light, trim, 1.3, 0.25, -1.5, 1.5, 2.6, -1.1);
    this.box(p, b, light, trim, 1.3, 0.25, 1.1, 1.5, 2.6, 1.5);
    this.box(p, b, light, trim, -1.5, 2.6, -1.5, 1.5, 2.72, 1.5);
    this.box(p, b, 15728880, lamp, -0.6, 2.54, -0.6, 0.6, 2.6, 0.6);
    this.box(p, b, light, trim, -1.38, 1.05, -1.2, -1.28, 1.12, 1.2);
    this.box(p, b, light, trim, -1.2, 1.05, -1.38, 1.2, 1.12, -1.28);
    this.box(p, b, light, trim, -1.2, 1.05, 1.28, 1.2, 1.12, 1.38);
    super.render(e, yaw, partial, p, b, light);
  }
}
