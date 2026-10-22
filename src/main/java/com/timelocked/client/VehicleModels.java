package com.timelocked.client;

import com.timelocked.AuthoredGeometry;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;

/** Native renderer adapter. Editable UVs, cuboids and pivots live in geometry JSON. */
public final class VehicleModels {
  public static LayerDefinition roverLayer() {
    return layer("rover");
  }

  public static LayerDefinition truckLayer() {
    return layer("truck");
  }

  public static LayerDefinition tramLayer() {
    return layer("tram");
  }

  private static LayerDefinition layer(String kind) {
    var drawing = AuthoredGeometry.vehicle(kind);
    MeshDefinition mesh = new MeshDefinition();
    for (var cube : drawing.cuboids()) {
      mesh.getRoot()
          .addOrReplaceChild(
              cube.name(),
              CubeListBuilder.create()
                  .texOffs(cube.textureU(), cube.textureV())
                  .addBox(cube.x(), cube.y(), cube.z(), cube.width(), cube.height(), cube.depth()),
              PartPose.offset(cube.pivotX(), cube.pivotY(), cube.pivotZ()));
    }
    return LayerDefinition.create(mesh, drawing.textureWidth(), drawing.textureHeight());
  }

  public static int[] wheels(String kind) {
    // Preserve the old renderer contract for unknown/non-vehicle kinds.
    return switch (kind) {
      case "rover", "truck", "tram" -> AuthoredGeometry.vehicle(kind).wheelParts();
      default -> new int[0];
    };
  }

  private VehicleModels() {}
}
