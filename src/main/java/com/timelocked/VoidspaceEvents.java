package com.timelocked;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public final class VoidspaceEvents {
  public static void register() {
    NeoForge.EVENT_BUS.addListener(
        (PlayerTickEvent.Post e) -> {
          if (e.getEntity() instanceof ServerPlayer p && p.tickCount % 40 == 0) {
            tick(p);
          }
        });
  }

  public static void survey(Mob mob, String type) {
    if (mob.level() instanceof ServerLevel l) {
      for (ServerPlayer p : l.players()) {
        if (!p.isSpectator() && p.distanceToSqr(mob) < 64.0) {
          Achievements.grant(p, "field_biologist", type);
        }
      }
    }
  }

  static void tick(ServerPlayer p) {
    ServerLevel l = p.serverLevel();
    if (l.dimension().equals(VoidspaceTravel.DIMENSION)) {
      if (l.getChunkSource().getGenerator() instanceof VoidspaceChunkGenerator g) {
        g.layout(l.getChunkSource().randomState())
            .nearest(p.getBlockX(), p.getBlockZ(), 24)
            .ifPresent(s -> Achievements.grant(p, "cartographer", s.type().id()));
      }
    } else {
      if (l.dimension().equals(Level.OVERWORLD)) {
        SatelliteCrash.playerTick(p);
      }

      if (p.tickCount % 120 == 0 && l.dimension().equals(Level.OVERWORLD)) {
        if (!Achievements.done(p, "power_trip") || !Achievements.done(p, "informed_consent")) {
          SarcOutposts.at(l, p.blockPosition())
              .filter(ox -> SarcOutposts.inside(ox, p.blockPosition()))
              .ifPresent(
                  ox -> Achievements.grant(p, ox.kind() == 0 ? "power_trip" : "informed_consent"));
        }

        boolean road = !Achievements.done(p, "on_the_road_again");
        boolean site = !Achievements.done(p, "site_seeing");
        if (road || site) {
          BlockPos at = p.blockPosition();
          BlockState below = l.getBlockState(at.below());
          if (road && (below.is(Blocks.GRAY_CONCRETE) || below.is(Blocks.YELLOW_CONCRETE))) {
            for (BlockPos o : SitePlanner.nearby(l, at)) {
              if (onRoad(o, at)) {
                Achievements.grant(p, "on_the_road_again");
                break;
              }
            }
          }

          if (site) {
            for (BlockPos o : SitePlanner.nearby(l, at)) {
              if (atSite(o, at)) {
                Achievements.grant(p, "site_seeing");
                break;
              }
            }
          }
        }
      }
    }
  }

  public static boolean onRoad(BlockPos o, BlockPos p) {
    int dx = p.getX() - (o.getX() + 64);
    int dz = p.getZ() - (o.getZ() + 188);
    return Math.abs(dx) <= 8 && dz >= -4 && dz <= 2030;
  }

  public static boolean atSite(BlockPos o, BlockPos p) {
    return p.getX() >= o.getX() - 4
        && p.getX() < o.getX() + 130 + 4
        && p.getZ() >= o.getZ() - 4
        && p.getZ() < o.getZ() + 208 + 4
        && p.getY() >= o.getY() - 4
        && p.getY() <= o.getY() + 43 + 16;
  }

  private VoidspaceEvents() {}
}
