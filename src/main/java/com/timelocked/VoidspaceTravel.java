package com.timelocked;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class VoidspaceTravel {
  public static final ResourceKey<Level> DIMENSION =
      ResourceKey.create(
          Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath("timelocked", "voidspace"));
  private static final String RETURN = "timelocked_return";

  public static void ensurePlatform(MinecraftServer server, ServerLevel level) {
    FacilityState state = FacilityState.get(server);
    if (!state.platformBuilt) {
      for (int x = -7; x <= 7; x++) {
        for (int z = -7; z <= 7; z++) {
          level.setBlock(
              new BlockPos(x, 62, z), ((Block) Content.FLOOR.get()).defaultBlockState(), 3);

          for (int y = 63; y <= 71; y++) {
            level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 3);
          }

          if ((Math.abs(x) == 7 || Math.abs(z) == 7) && z != 6) {
            level.setBlock(new BlockPos(x, 63, z), Blocks.IRON_BARS.defaultBlockState(), 3);
          }
        }
      }

      for (int x = -3; x <= 3; x++) {
        for (int y = 62; y <= 70; y++) {
          if (Math.abs(x) != 3 && y != 62 && y != 70) {
            level.setBlock(
                new BlockPos(x, y, 0),
                ((PortalFieldBlock) Content.FIELD.get()).defaultBlockState(),
                3);
          } else {
            level.setBlock(
                new BlockPos(x, y, 0), ((Block) Content.CASING.get()).defaultBlockState(), 3);
          }
        }
      }

      for (int x : new int[] {-6, 6}) {
        for (int z : new int[] {-5, 5}) {
          level.setBlock(
              new BlockPos(x, 63, z),
              ((FacilityLampBlock) Content.LIGHT.get()).defaultBlockState(),
              3);
        }
      }

      BlockPos signPos = new BlockPos(4, 63, 0);
      level.setBlock(signPos, MonitorContent.stand(Direction.SOUTH), 3);
      if (level.getBlockEntity(signPos) instanceof MonitorContent.MonitorEntity screen) {
        screen.setTheme(0);
        screen.setLines("SARC VOIDSPACE", "RETURN GATE", "Walk into field", "Staging zone");
      }

      state.platformBuilt = true;
      state.setDirty();
    }
  }

  public static Optional<Vec3> safeSpot(ServerLevel level, BlockPos desired) {
    for (int radius = 0; radius <= 6; radius++) {
      for (int dx = -radius; dx <= radius; dx++) {
        for (int dz = -radius; dz <= radius; dz++) {
          if (Math.max(Math.abs(dx), Math.abs(dz)) == radius) {
            for (int delta = 0; delta <= 24; delta++) {
              for (int sign : delta == 0 ? new int[] {1} : new int[] {1, -1}) {
                BlockPos pos = desired.offset(dx, delta * sign, dz);
                if (pos.getY() >= level.getMinBuildHeight() + 1
                    && pos.getY() + 2 < level.getMaxBuildHeight()
                    && level.getWorldBorder().isWithinBounds(pos)) {
                  level.getChunkAt(pos);
                  if (level
                          .getBlockState(pos.below())
                          .isFaceSturdy(level, pos.below(), Direction.UP)
                      && level.getFluidState(pos).isEmpty()
                      && level.getFluidState(pos.above()).isEmpty()
                      && !level.getBlockState(pos).is((Block) Content.FIELD.get())
                      && !level.getBlockState(pos.above()).is((Block) Content.FIELD.get())) {
                    Vec3 feet = new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                    if (level.noCollision(
                        new AABB(
                            feet.x - 0.3,
                            feet.y,
                            feet.z - 0.3,
                            feet.x + 0.3,
                            feet.y + 1.8,
                            feet.z + 0.3))) {
                      return Optional.of(feet);
                    }
                  }
                }
              }
            }
          }
        }
      }
    }

    return Optional.empty();
  }

  public static void travel(ServerPlayer player) {
    MinecraftServer server = player.getServer();
    if (server != null) {
      if (!player.isPassenger() && !player.isVehicle()) {
        ServerLevel source = player.serverLevel();
        float yaw = player.getYRot();
        float pitch = player.getXRot();
        ServerLevel target;
        Vec3 destination;
        if (source.dimension().equals(DIMENSION)) {
          CompoundTag t = player.getPersistentData().getCompound("timelocked_return");
          ResourceLocation id = ResourceLocation.tryParse(t.getString("Dimension"));
          target =
              id == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
          if (target == null) {
            target = server.overworld();
          }

          BlockPos desired =
              t.contains("Position")
                  ? BlockPos.of(t.getLong("Position"))
                  : target.getSharedSpawnPos();
          Optional<Vec3> safe = safeSpot(target, desired);
          if (safe.isEmpty()) {
            player.setPortalCooldown(100);
            player.sendSystemMessage(
                Component.literal(
                    "Return arrival blocked. Clear the source landing before retrying."));
            return;
          }

          destination = safe.get();
          yaw = t.contains("Yaw") ? t.getFloat("Yaw") : yaw;
        } else {
          Optional<BlockPos> site = SitePlanner.containing(source, player.blockPosition());
          if (site.isPresent() && !SitePlanner.progress(source, site.get()).cleared) {
            player.setPortalCooldown(40);
            player.displayClientMessage(
                Component.literal("Transit locked: defeat all three Bionexian waves."), true);
            return;
          }

          target = server.getLevel(DIMENSION);
          if (target == null) {
            player.setPortalCooldown(100);
            player.sendSystemMessage(
                Component.literal("Voidspace is unavailable; check the dimension data."));
            return;
          }

          Optional<Vec3> returning =
              safeSpot(source, player.blockPosition().offset(0, 0, site.isPresent() ? 6 : 3));
          if (returning.isEmpty()) {
            player.setPortalCooldown(100);
            player.sendSystemMessage(
                Component.literal(
                    "No safe source return point. Clear a supported landing near the gate."));
            return;
          }

          ensurePlatform(server, target);
          Optional<Vec3> safe = safeSpot(target, new BlockPos(0, 63, 4));
          if (safe.isEmpty()) {
            player.setPortalCooldown(100);
            player.sendSystemMessage(
                Component.literal("Voidspace landing obstructed; travel refused."));
            return;
          }

          destination = safe.get();
          CompoundTag t = new CompoundTag();
          t.putString("Dimension", source.dimension().location().toString());
          t.putLong("Position", BlockPos.containing((Position) returning.get()).asLong());
          t.putFloat("Yaw", yaw);
          player.getPersistentData().put("timelocked_return", t);
        }

        // Animated crossing: fall through the portal shaft and be thrown out of the far gate (see
        // PortalTransit).
        PortalTransit.begin(player, target, destination, yaw, pitch);
      } else {
        player.displayClientMessage(
            Component.literal("Dismount before using a dimensional portal."), true);
      }
    }
  }

  private VoidspaceTravel() {}
}
