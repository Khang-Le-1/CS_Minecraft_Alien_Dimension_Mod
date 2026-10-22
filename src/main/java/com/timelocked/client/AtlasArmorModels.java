package com.timelocked.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.timelocked.AtlasContent;
import com.timelocked.MetalContent;
import com.timelocked.SpaceArmorItem;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterLayerDefinitions;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

public final class AtlasArmorModels {
  private static final Map<String, AtlasArmorModels.AtlasModel> CACHE = new HashMap<>();
  private static final EquipmentSlot[] SLOTS =
      new EquipmentSlot[] {
        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
      };

  public static ModelLayerLocation location(String variant, EquipmentSlot slot) {
    return new ModelLayerLocation(
        ResourceLocation.fromNamespaceAndPath("timelocked", "atlas_hes_" + variant),
        slot.getName());
  }

  public static void registerLayers(RegisterLayerDefinitions event) {
    for (String variant : List.of("standard", "everleigh")) {
      for (EquipmentSlot slot : SLOTS) {
        event.registerLayerDefinition(location(variant, slot), () -> layer(variant, slot));
      }
    }
  }

  public static void clear() {
    CACHE.clear();
  }

  public static void registerItems(RegisterClientExtensionsEvent event) {
    IClientItemExtensions extension =
        new IClientItemExtensions() {
          public HumanoidModel<?> getHumanoidArmorModel(
              LivingEntity wearer, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original) {
            String variant =
                stack.getItem() instanceof SpaceArmorItem suit && suit.personal()
                    ? "everleigh"
                    : "standard";
            return (HumanoidModel<?>)
                AtlasArmorModels.CACHE.computeIfAbsent(
                    variant + "_" + slot.getName(),
                    key ->
                        new AtlasArmorModels.AtlasModel(
                            Minecraft.getInstance()
                                .getEntityModels()
                                .bakeLayer(AtlasArmorModels.location(variant, slot)),
                            variant,
                            slot));
          }
        };
    event.registerItem(
        extension,
        new Item[] {
          (Item) MetalContent.HELMET.get(),
          (Item) MetalContent.CHEST.get(),
          (Item) MetalContent.LEGS.get(),
          (Item) MetalContent.BOOTS.get(),
          (Item) AtlasContent.HELMET.get(),
          (Item) AtlasContent.CHEST.get(),
          (Item) AtlasContent.LEGS.get(),
          (Item) AtlasContent.BOOTS.get()
        });
  }

  private static JsonObject geometry() {
    try (InputStream in =
        AtlasArmorModels.class.getResourceAsStream("/assets/timelocked/atlas-hes-geometry.json")) {
      if (in == null) {
        throw new IllegalStateException("Missing ATLAS model geometry");
      } else {
        return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8))
            .getAsJsonObject();
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static boolean matches(JsonObject b, String variant, EquipmentSlot slot) {
    return b.get("slot").getAsString().equals(slot.getName())
        && (!b.has("variant") || b.get("variant").getAsString().equals(variant));
  }

  private static float value(JsonArray a, int i) {
    return a.get(i).getAsFloat();
  }

  public static LayerDefinition layer(String variant, EquipmentSlot slot) {
    JsonObject data = geometry();
    MeshDefinition mesh = new MeshDefinition();
    PartDefinition root = mesh.getRoot();
    JsonObject bones = data.getAsJsonObject("bones");

    for (String name : bones.keySet()) {
      JsonArray p = bones.getAsJsonArray(name);
      root.addOrReplaceChild(
          name, CubeListBuilder.create(), PartPose.offset(value(p, 0), value(p, 1), value(p, 2)));
    }

    for (JsonElement entry : data.getAsJsonArray("boxes")) {
      JsonObject b = entry.getAsJsonObject();
      if (matches(b, variant, slot)) {
        JsonArray at = b.getAsJsonArray("from");
        JsonArray size = b.getAsJsonArray("size");
        JsonArray pivot = b.getAsJsonArray("pivot");
        JsonArray rotation = b.getAsJsonArray("rotation");
        JsonArray uv = b.getAsJsonArray("uv");
        CubeListBuilder cube =
            CubeListBuilder.create()
                .texOffs(uv.get(0).getAsInt(), uv.get(1).getAsInt())
                .addBox(
                    value(at, 0) - value(pivot, 0),
                    value(at, 1) - value(pivot, 1),
                    value(at, 2) - value(pivot, 2),
                    value(size, 0),
                    value(size, 1),
                    value(size, 2));
        root.getChild(b.get("part").getAsString())
            .addOrReplaceChild(
                b.get("name").getAsString(),
                cube,
                PartPose.offsetAndRotation(
                    value(pivot, 0),
                    value(pivot, 1),
                    value(pivot, 2),
                    value(rotation, 0),
                    value(rotation, 1),
                    value(rotation, 2)));
      }
    }

    return LayerDefinition.create(
        mesh, data.get("uv_width").getAsInt(), data.get("uv_height").getAsInt());
  }

  private AtlasArmorModels() {}

  private static final class AtlasModel extends HumanoidModel<LivingEntity> {
    private final List<AtlasArmorModels.Glow> glows = new ArrayList<>();

    AtlasModel(ModelPart root, String variant, EquipmentSlot slot) {
      super(root);

      for (JsonElement entry : AtlasArmorModels.geometry().getAsJsonArray("boxes")) {
        JsonObject b = entry.getAsJsonObject();
        if (AtlasArmorModels.matches(b, variant, slot) && b.get("glow").getAsBoolean()) {
          ModelPart parent = root.getChild(b.get("part").getAsString());
          this.glows.add(
              new AtlasArmorModels.Glow(parent, parent.getChild(b.get("name").getAsString())));
        }
      }
    }

    public void renderToBuffer(
        PoseStack pose, VertexConsumer buffer, int light, int overlay, int color) {
      if (this.young) {
        super.renderToBuffer(pose, buffer, light, overlay, color);
      } else {
        for (AtlasArmorModels.Glow g : this.glows) {
          g.child().visible = false;
        }

        try {
          super.renderToBuffer(pose, buffer, light, overlay, color);
        } finally {
          for (AtlasArmorModels.Glow g : this.glows) {
            g.child().visible = true;
          }
        }

        for (AtlasArmorModels.Glow g : this.glows) {
          if (g.parent().visible) {
            pose.pushPose();
            g.parent().translateAndRotate(pose);
            g.child().render(pose, buffer, 15728880, overlay, color);
            pose.popPose();
          }
        }
      }
    }
  }

  private record Glow(ModelPart parent, ModelPart child) {}
}
