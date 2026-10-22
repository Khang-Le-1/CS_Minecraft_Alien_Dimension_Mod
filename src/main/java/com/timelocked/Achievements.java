package com.timelocked;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class Achievements {
  public static void grant(ServerPlayer p, String id) {
    grant(p, id, "done");
  }

  public static void grant(ServerPlayer p, String id, String criterion) {
    if (p.getServer() != null) {
      AdvancementHolder adv =
          p.getServer()
              .getAdvancements()
              .get(ResourceLocation.fromNamespaceAndPath("timelocked", "main/" + id));
      if (adv != null) {
        p.getAdvancements().award(adv, criterion);
      }
    }
  }

  public static void grantNear(ServerLevel l, BlockPos at, double radius, String id) {
    for (ServerPlayer p : l.players()) {
      if (p.blockPosition().distSqr(at) < radius * radius) {
        grant(p, id);
      }
    }
  }

  public static boolean done(ServerPlayer p, String id) {
    if (p.getServer() == null) {
      return true;
    }

    AdvancementHolder adv =
        p.getServer()
            .getAdvancements()
            .get(ResourceLocation.fromNamespaceAndPath("timelocked", "main/" + id));
    return adv == null || p.getAdvancements().getOrStartProgress(adv).isDone();
  }

  private Achievements() {}
}
