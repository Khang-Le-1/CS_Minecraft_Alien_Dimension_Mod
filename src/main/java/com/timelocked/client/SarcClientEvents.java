package com.timelocked.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.timelocked.FailedSubject;
import com.timelocked.SarcContent;
import com.timelocked.SarcScientist;
import net.minecraft.client.model.AbstractZombieModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers;

@EventBusSubscriber(modid = "timelocked", bus = Bus.MOD, value = Dist.CLIENT)
public final class SarcClientEvents {
  static final ResourceLocation[] SCIENTIST =
      new ResourceLocation[] {
        tex("scientist_researcher"), tex("scientist_engineer"), tex("scientist_medic")
      };
  static final ResourceLocation SUBJECT = tex("failed_subject");
  static final RenderType SUBJECT_EYES = RenderType.eyes(tex("failed_subject_eyes"));

  static ResourceLocation tex(String n) {
    return ResourceLocation.fromNamespaceAndPath(
        "timelocked", "textures/entity/sarc/" + n + ".png");
  }

  @SubscribeEvent
  public static void renderers(RegisterRenderers e) {
    e.registerEntityRenderer(
        (EntityType) SarcContent.SCIENTIST.get(), SarcClientEvents.ScientistRenderer::new);
    e.registerEntityRenderer(
        (EntityType) SarcContent.SUBJECT.get(), SarcClientEvents.SubjectRenderer::new);
  }

  private SarcClientEvents() {}

  static final class ScientistRenderer
      extends HumanoidMobRenderer<SarcScientist, HumanoidModel<SarcScientist>> {
    ScientistRenderer(Context c) {
      super(c, new HumanoidModel(c.bakeLayer(ModelLayers.PLAYER)), 0.5F);
    }

    public ResourceLocation getTextureLocation(SarcScientist s) {
      return SarcClientEvents.SCIENTIST[s.kind()];
    }

    protected void scale(SarcScientist s, PoseStack p, float t) {
      p.scale(0.9375F, 0.9375F, 0.9375F);
    }
  }

  static final class SubjectModel extends AbstractZombieModel<FailedSubject> {
    SubjectModel(ModelPart r) {
      super(r);
    }

    public boolean isAggressive(FailedSubject s) {
      return s.isAggressive();
    }
  }

  static final class SubjectRenderer
      extends HumanoidMobRenderer<FailedSubject, SarcClientEvents.SubjectModel> {
    SubjectRenderer(Context c) {
      super(c, new SarcClientEvents.SubjectModel(c.bakeLayer(ModelLayers.ZOMBIE)), 0.5F);
      this.addLayer(
          new EyesLayer<FailedSubject, SarcClientEvents.SubjectModel>(this) {
            public RenderType renderType() {
              return SarcClientEvents.SUBJECT_EYES;
            }
          });
    }

    public ResourceLocation getTextureLocation(FailedSubject s) {
      return SarcClientEvents.SUBJECT;
    }
  }
}
