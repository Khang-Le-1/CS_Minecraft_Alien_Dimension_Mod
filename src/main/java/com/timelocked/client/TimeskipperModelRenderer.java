package com.timelocked.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import com.timelocked.ShipInteriorContent;
import com.timelocked.ShipModelAnchorBlock;
import com.timelocked.SpacePlanets;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;

/**
 * Detailed hard-surface TIMESKIPPER exterior. design/build_ship.py generates the same face stream
 * used for game and previews. Forward is -Z. Six engines: two stacked in each side pod, two dorsal.
 * Gameplay, walkable blocks, controls and jump timing remain unchanged.
 */
public final class TimeskipperModelRenderer
    implements BlockEntityRenderer<ShipModelAnchorBlock.ModelAnchor> {
  private static final ResourceLocation MODEL =
      ResourceLocation.fromNamespaceAndPath("timelocked", "models/ship/timeskipper.tlmesh");
  private final Model model, cabin, rotor, legacyModel, legacyCabin, gear, dockedModel;

  public TimeskipperModelRenderer(Context context) {
    model =
        load(
            ResourceLocation.fromNamespaceAndPath(
                "timelocked", "models/ship/timeskipper-access.tlmesh"));
    dockedModel =
        load(
            ResourceLocation.fromNamespaceAndPath(
                "timelocked", "models/ship/timeskipper-docked.tlmesh"));
    gear =
        load(
            ResourceLocation.fromNamespaceAndPath(
                "timelocked", "models/ship/timeskipper-ground-gear.tlmesh"));
    legacyModel =
        load(
            ResourceLocation.fromNamespaceAndPath(
                "timelocked", "models/ship/timeskipper-081.tlmesh"));
    legacyCabin =
        load(
            ResourceLocation.fromNamespaceAndPath(
                "timelocked", "models/ship/timeskipper-interior-081.tlmesh"));
    cabin =
        load(
            ResourceLocation.fromNamespaceAndPath(
                "timelocked", "models/ship/timeskipper-interior.tlmesh"));
    rotor =
        load(
            ResourceLocation.fromNamespaceAndPath(
                "timelocked", "models/ship/timeskipper-drive-rotor.tlmesh"));
  }

  public AABB getRenderBoundingBox(ShipModelAnchorBlock.ModelAnchor anchor) {
    BlockPos p = anchor.getBlockPos();
    return new AABB(
        p.getX() - 23, p.getY() - 24, p.getZ() - 32, p.getX() + 24, p.getY() + 10, p.getZ() + 27);
  }

  @Override
  public boolean shouldRenderOffScreen(ShipModelAnchorBlock.ModelAnchor anchor) {
    return true;
  }

  @Override
  public int getViewDistance() {
    return 256;
  }

  @Override
  public void render(
      ShipModelAnchorBlock.ModelAnchor anchor,
      float partialTick,
      PoseStack pose,
      MultiBufferSource buffers,
      int light,
      int overlay) {
    if (model.faces().isEmpty()) return;
    pose.pushPose();
    pose.translate(0.5, -8.0, 0.5);
    ReactorMesh mesh = new ReactorMesh(pose, buffers, light);
    TextureAtlasSprite surface = ReactorMesh.sprite("minecraft:block/white_concrete");
    float time = anchor.getLevel() == null ? 0 : anchor.getLevel().getGameTime() + partialTick;
    float power = JumpCutsceneClient.enginePower(partialTick);
    var camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
    var p = anchor.getBlockPos();
    double rx = camera.x - p.getX() - .5,
        ry = camera.y - p.getY() + 8.0,
        rz = camera.z - p.getZ() - .5;
    boolean inside = Math.abs(rx) < 7.8 && ry > -5.9 && ry < 6.9 && rz > -27.8 && rz < 22.5;
    boolean fitted =
        anchor.getLevel() != null
            && anchor.getLevel().dimension().equals(SpacePlanets.SHIP)
            && anchor
                .getLevel()
                .getBlockState(p.offset(0, -8, 0))
                .is(ShipInteriorContent.PATH.get());
    boolean revised = anchor.getBlockState().getValue(ShipModelAnchorBlock.NEW_LAYOUT);
    var port =
        anchor.getLevel() == null ? null : anchor.getLevel().getBlockState(p.offset(8, -7, -2));
    boolean connected =
        port != null
            && port.is(ShipInteriorContent.DOCK_PORT.get())
            && port.getValue(com.timelocked.DockingPortBlock.PHASE) == 4;
    draw(
        revised ? (connected ? dockedModel : model) : legacyModel,
        mesh,
        surface,
        time,
        power,
        inside);
    boolean vacuumSite =
        anchor.getLevel() != null
            && (anchor.getLevel().dimension().equals(SpacePlanets.Planet.ORBIT.dimension())
                || anchor.getLevel().dimension().equals(SpacePlanets.Planet.BELT.dimension()));
    boolean shipOrbit =
        anchor.getLevel() != null
            && anchor.getLevel().dimension().equals(SpacePlanets.SHIP)
            && (SpacePlanets.Planet.byIndex(SpacePlanets.clientShipLocation)
                    == SpacePlanets.Planet.ORBIT
                || SpacePlanets.Planet.byIndex(SpacePlanets.clientShipLocation)
                    == SpacePlanets.Planet.BELT);
    if (revised && !vacuumSite && !shipOrbit && !inside)
      draw(gear, mesh, surface, time, power, false);
    if (inside && fitted) {
      int cabinLight = LevelRenderer.getLightColor(anchor.getLevel(), BlockPos.containing(camera));
      ReactorMesh cabinMesh = new ReactorMesh(pose, buffers, cabinLight);
      draw(cabin, cabinMesh, surface, time, power, false);
      pose.pushPose();
      pose.translate(0, 3.73, 17);
      pose.mulPose(Axis.YP.rotationDegrees(time * .65F));
      draw(rotor, cabinMesh, surface, time, power, false);
      pose.popPose();
    }
    float age = JumpCutsceneClient.age(partialTick);
    if (age > 0) {
      float r = Math.min(22, 3 + age * 0.9F), alpha = Math.max(0, 1 - age / 70);
      mesh.glow(surface, 0x8CE7F2, alpha).box(-r, -3, 26.0F, r, 11, 26.8F);
    }
    pose.popPose();
  }

  private static void draw(
      Model model,
      ReactorMesh mesh,
      TextureAtlasSprite surface,
      float time,
      float power,
      boolean inside) {
    for (int material = 0; material < model.materials().size(); material++) {
      Material mat = model.materials().get(material);
      if (inside && (material == 6 || material == 7)) mesh.glow(surface, mat.rgb(), .10F);
      else if (mat.emissive() && material == model.engineMaterial())
        mesh.glow(surface, mat.rgb(), .45F + power * .5F + .04F * (float) Math.sin(time * .09F));
      else if (mat.emissive()) mesh.lit(surface, mat.rgb());
      else mesh.solid(surface, mat.rgb());
      for (Face face : model.facesByMaterial().get(material)) {
        float[] v = face.vertices(), n = face.normal();
        for (int k = 0; k < 4; k++)
          mesh.v(
              v[k * 3],
              v[k * 3 + 1],
              v[k * 3 + 2],
              k == 0 || k == 3 ? 0 : 1,
              k < 2 ? 1 : 0,
              n[0],
              n[1],
              n[2]);
      }
    }
  }

  private static Model load(ResourceLocation id) {
    try (DataInputStream in =
        new DataInputStream(Minecraft.getInstance().getResourceManager().open(id))) {
      if (in.readInt() != 0x544C4D53 || in.readInt() != 1)
        throw new IOException("Invalid Timeskipper mesh header");
      int count = in.readInt();
      if (count < 1 || count > 64) throw new IOException("Invalid material count");
      List<Material> materials = new ArrayList<>();
      List<List<Face>> batches = new ArrayList<>();
      int engineMaterial = -1;
      for (int i = 0; i < count; i++) {
        int rgb = in.readInt();
        boolean glow = in.readUnsignedByte() != 0;
        materials.add(new Material(rgb, glow));
        batches.add(new ArrayList<>());
        if (rgb == 0x3EBECF) engineMaterial = i;
      }
      int faceCount = in.readInt();
      if (faceCount < 1 || faceCount > 100000)
        throw new IOException("Invalid Timeskipper face count");
      List<Face> faces = new ArrayList<>(faceCount);
      for (int i = 0; i < faceCount; i++) {
        int material = in.readUnsignedShort();
        if (material >= count) throw new IOException("Invalid face material");
        float[] vertices = new float[12], normal = new float[3];
        for (int k = 0; k < 12; k++) {
          vertices[k] = in.readFloat();
          if (!Float.isFinite(vertices[k])) throw new IOException("Non-finite vertex");
        }
        for (int k = 0; k < 3; k++) normal[k] = in.readFloat();
        Face face = new Face(vertices, normal);
        faces.add(face);
        batches.get(material).add(face);
      }
      return new Model(
          List.copyOf(materials),
          List.copyOf(faces),
          batches.stream().map(List::copyOf).toList(),
          engineMaterial);
    } catch (IOException | RuntimeException e) {
      LogUtils.getLogger().error("Unable to load Timeskipper exterior mesh", e);
      return new Model(List.of(), List.of(), List.of(), -1);
    }
  }

  private record Material(int rgb, boolean emissive) {}

  private record Face(float[] vertices, float[] normal) {}

  private record Model(
      List<Material> materials,
      List<Face> faces,
      List<List<Face>> facesByMaterial,
      int engineMaterial) {}
}
