package com.timelocked.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.timelocked.AlarmBlock;
import com.timelocked.AlarmRotorEntity;
import com.timelocked.FacilityDetails;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;

/** An actual continuously rotating two-ended reflector inside a translucent red dome. */
public final class AlarmRotorRenderer implements BlockEntityRenderer<AlarmRotorEntity> {
  public AlarmRotorRenderer(Context c) {}

  public void render(
      AlarmRotorEntity be,
      float partial,
      PoseStack pose,
      MultiBufferSource buffers,
      int light,
      int overlay) {
    var state = be.getBlockState();
    if (!state.is(FacilityDetails.ALARM_BEACON.get())) return;
    var facing = state.getValue(AlarmBlock.FACING);
    boolean active = state.getValue(AlarmBlock.ACTIVE);
    float mounting =
        switch (facing) {
          case EAST -> -90f;
          case SOUTH -> 180f;
          case WEST -> 90f;
          default -> 0f;
        };
    float angle =
        active && be.getLevel() != null
            ? ((be.getLevel().getGameTime() % 30) + partial) * 12f
            : 25f;
    pose.pushPose();
    pose.translate(.5, 0, .5);
    pose.mulPose(Axis.YP.rotationDegrees(mounting));
    pose.translate(-.5, 0, -.5);
    var sprite = ReactorMesh.sprite("timelocked:block/sarc_beacon_reflector");
    if (sprite == null) {
      pose.popPose();
      return;
    }
    var mesh = new ReactorMesh(pose, buffers, light);
    // Wall-normal spindle, not an upright Y-axis shaft. The reflector sweeps
    // in the wall plane; constant bulb emission never toggles with the angle.
    mesh.solid(sprite, 0x454b51).box(.47f, .548125f, .49f, .53f, .608125f, .80f);
    if (active) mesh.lit(sprite, 0xff8b43).box(.472f, .550125f, .355f, .528f, .606125f, .415f);
    else mesh.solid(sprite, 0x754a35).box(.472f, .550125f, .355f, .528f, .606125f, .415f);
    pose.translate(.5, .578125, .5625);
    pose.mulPose(Axis.ZP.rotationDegrees(angle));
    mesh.solid(sprite, 0xb8c1c6).box(-.165f, -.045f, -.10f, .165f, .045f, .10f);
    if (active) {
      mesh.lit(sprite, 0xff9b52).box(-.185f, -.045f, -.105f, -.145f, .045f, .105f);
      mesh.lit(sprite, 0xff9b52).box(.145f, -.045f, -.105f, .185f, .045f, .105f);
    } else {
      mesh.solid(sprite, 0x897967).box(-.185f, -.045f, -.105f, -.145f, .045f, .105f);
      mesh.solid(sprite, 0x897967).box(.145f, -.045f, -.105f, .185f, .045f, .105f);
    }
    pose.popPose();
  }
}
