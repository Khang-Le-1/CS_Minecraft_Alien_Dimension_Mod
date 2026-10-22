package com.timelocked.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.timelocked.DoorAnimationTransform;
import com.timelocked.SarcDoorEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.neoforge.client.model.data.ModelData;

public final class SarcDoorRenderer implements BlockEntityRenderer<SarcDoorEntity> {
  public SarcDoorRenderer(Context context) {}

  public void render(
      SarcDoorEntity be,
      float partial,
      PoseStack pose,
      MultiBufferSource buffers,
      int light,
      int overlay) {
    BlockState s = be.getBlockState();
    if (s.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER) {
      BlockRenderDispatcher dispatch = Minecraft.getInstance().getBlockRenderer();
      float base = DoorAnimationTransform.base((Direction) s.getValue(DoorBlock.FACING));
      boolean left = s.getValue(DoorBlock.HINGE) == DoorHingeSide.LEFT;
      float progress = be.progress(partial);
      pose.pushPose();
      pose.translate(0.5, 0.0, 0.5);
      pose.mulPose(Axis.YP.rotationDegrees(-base));
      pose.translate(-0.5, 0.0, -0.5);
      pose.translate(0.0, 0.0, (left ? 1 : -1) * 0.1875 * progress);
      pose.translate(0.0F, 0.0F, left ? 0.0F : 1.0F);
      pose.mulPose(Axis.YP.rotationDegrees((left ? 90 : -90) * progress));
      pose.translate(0.0F, 0.0F, left ? 0.0F : -1.0F);
      BlockState canonical =
          (BlockState)
              ((BlockState) s.setValue(DoorBlock.FACING, Direction.EAST))
                  .setValue(DoorBlock.OPEN, false);
      RenderType type = RenderType.cutout();

      for (int half = 0; half < 2; half++) {
        pose.pushPose();
        pose.translate(0.0F, half, 0.0F);
        BlockState state =
            (BlockState)
                canonical.setValue(
                    DoorBlock.HALF, half == 0 ? DoubleBlockHalf.LOWER : DoubleBlockHalf.UPPER);
        dispatch
            .getModelRenderer()
            .renderModel(
                pose.last(),
                buffers.getBuffer(type),
                state,
                dispatch.getBlockModel(state),
                1.0F,
                1.0F,
                1.0F,
                light,
                overlay,
                ModelData.EMPTY,
                type);
        pose.popPose();
      }

      pose.popPose();
    }
  }
}
