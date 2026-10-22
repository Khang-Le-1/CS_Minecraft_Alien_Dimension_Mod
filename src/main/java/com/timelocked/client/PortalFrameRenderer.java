package com.timelocked.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.timelocked.Content;
import com.timelocked.PortalFrameEntity;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;

public final class PortalFrameRenderer implements BlockEntityRenderer<PortalFrameEntity> {
  private final JsonObject model;
  private static final int FIELD_FRAMES = 32;
  private static final int FIELD_FRAMETIME = 2;
  private static final String[] NAMES =
      new String[] {"south", "north", "up", "down", "east", "west"};

  public PortalFrameRenderer(Context context) {
    try (InputStream in =
        PortalFrameRenderer.class.getResourceAsStream(
            "/assets/timelocked/portal-frame-model.json")) {
      this.model = JsonParser.parseReader(new InputStreamReader(in)).getAsJsonObject();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static float val(JsonArray a, int i) {
    return a.get(i).getAsFloat();
  }

  public void render(
      PortalFrameEntity be,
      float partial,
      PoseStack pose,
      MultiBufferSource buffers,
      int light,
      int overlay) {
    pose.pushPose();
    var anchorFacing = be.getBlockState().getValue(com.timelocked.PortalFrameAnchorBlock.FACING);
    float yaw =
        switch (anchorFacing) {
          case WEST -> 90f;
          case SOUTH -> 180f;
          case EAST -> -90f;
          default -> 0f;
        };
    pose.translate(.5, 0, .5);
    pose.mulPose(Axis.YP.rotationDegrees(yaw));
    pose.translate(-.5, 0, -.5);
    int frame = be.getLevel() == null ? 0 : (int) (be.getLevel().getGameTime() / 2L % 32L);
    boolean active =
        be.getLevel() != null
            && be.getLevel()
                .getBlockState(be.getBlockPos().offset(0, -10, 0))
                .is((Block) Content.FIELD.get());

    for (JsonElement entry : this.model.getAsJsonArray("elements")) {
      JsonObject e = entry.getAsJsonObject();
      boolean field = e.get("field").getAsBoolean();
      if (!field || active) {
        JsonArray from = e.getAsJsonArray("from");
        JsonArray to = e.getAsJsonArray("to");
        JsonArray origin = e.getAsJsonArray("origin");
        float x0 = val(from, 0);
        float y0 = val(from, 1);
        float z0 = val(from, 2);
        float x1 = val(to, 0);
        float y1 = val(to, 1);
        float z1 = val(to, 2);
        float[][][] faces =
            new float[][][] {
              {{x0, y0, z1}, {x1, y0, z1}, {x1, y1, z1}, {x0, y1, z1}},
              {{x1, y0, z0}, {x0, y0, z0}, {x0, y1, z0}, {x1, y1, z0}},
              {{x0, y1, z1}, {x1, y1, z1}, {x1, y1, z0}, {x0, y1, z0}},
              {{x0, y0, z0}, {x1, y0, z0}, {x1, y0, z1}, {x0, y0, z1}},
              {{x1, y0, z1}, {x1, y0, z0}, {x1, y1, z0}, {x1, y1, z1}},
              {{x0, y0, z0}, {x0, y0, z1}, {x0, y1, z1}, {x0, y1, z0}}
            };
        float[][] normals =
            new float[][] {
              {0.0F, 0.0F, 1.0F},
              {0.0F, 0.0F, -1.0F},
              {0.0F, 1.0F, 0.0F},
              {0.0F, -1.0F, 0.0F},
              {1.0F, 0.0F, 0.0F},
              {-1.0F, 0.0F, 0.0F}
            };
        pose.pushPose();
        pose.translate(val(origin, 0), val(origin, 1), val(origin, 2));
        JsonArray rot = e.getAsJsonArray("rotation");
        pose.mulPose(Axis.ZP.rotationDegrees(val(rot, 2)));
        pose.mulPose(Axis.YP.rotationDegrees(val(rot, 1)));
        pose.mulPose(Axis.XP.rotationDegrees(val(rot, 0)));
        pose.translate(-val(origin, 0), -val(origin, 1), -val(origin, 2));

        for (int f = 0; f < 6; f++) {
          JsonObject face = e.getAsJsonObject("faces").getAsJsonObject(NAMES[f]);
          int texture = face.get("texture").getAsInt();
          JsonArray uv = face.getAsJsonArray("uv");
          float u0 = val(uv, 0) / 32.0F;
          float v0 = val(uv, 1) / 32.0F;
          float u1 = val(uv, 2) / 32.0F;
          float v1 = val(uv, 3) / 32.0F;
          if (field) {
            v0 = (frame + v0) / 32.0F;
            v1 = (frame + v1) / 32.0F;
          }

          float[][] coords = new float[][] {{u0, v1}, {u1, v1}, {u1, v0}, {u0, v0}};
          VertexConsumer consumer =
              buffers.getBuffer(
                  RenderType.entityCutoutNoCull(
                      ResourceLocation.fromNamespaceAndPath(
                          "timelocked",
                          field
                              ? "textures/block/portal_frame/field_animated.png"
                              : "textures/block/portal_frame/texture_" + texture + ".png")));
          int packed = texture != 3 && !field ? light : 15728880;

          for (int i = 0; i < 4; i++) {
            float[] v = faces[f][i];
            consumer
                .addVertex(pose.last(), v[0], v[1], v[2])
                .setColor(255, 255, 255, 255)
                .setUv(coords[i][0], coords[i][1])
                .setOverlay(overlay)
                .setLight(packed)
                .setNormal(pose.last(), normals[f][0], normals[f][1], normals[f][2]);
          }
        }

        pose.popPose();
      }
    }
    pose.popPose();
  }

  public boolean shouldRenderOffScreen(PortalFrameEntity be) {
    return true;
  }

  public int getViewDistance() {
    return 128;
  }

  public AABB getRenderBoundingBox(PortalFrameEntity be) {
    return new AABB(be.getBlockPos()).inflate(13.0);
  }
}
