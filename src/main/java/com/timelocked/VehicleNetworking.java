package com.timelocked;

import java.util.Locale;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class VehicleNetworking {
  public static final double BOARD_REACH = 5.5;

  public static boolean canBoard(Player p, Entity v) {
    if ((v instanceof SarcTramEntity || v instanceof SarcGroundVehicle)
        && !p.isPassenger()
        && v.isAlive()) {
      Vec3 eye = p.getEyePosition();
      AABB box = v.getBoundingBox();
      double dx = Math.max(0.0, Math.max(box.minX - eye.x, eye.x - box.maxX));
      double dy = Math.max(0.0, Math.max(box.minY - eye.y, eye.y - box.maxY));
      double dz = Math.max(0.0, Math.max(box.minZ - eye.z, eye.z - box.maxZ));
      return dx * dx + dy * dy + dz * dz <= 30.25;
    } else {
      return false;
    }
  }

  public static void register(RegisterPayloadHandlersEvent e) {
    e.registrar("vehicles-v1")
        .playToServer(
            VehicleNetworking.Board.TYPE,
            VehicleNetworking.Board.CODEC,
            (data, ctx) ->
                ctx.enqueueWork(
                    () -> {
                      Player player = ctx.player();
                      Entity entity = player.level().getEntity(data.entityId());
                      if (entity != null) {
                        if (canBoard(player, entity)) {
                          entity.interact(player, InteractionHand.MAIN_HAND);
                        } else if (!player.isPassenger()) {
                          player.displayClientMessage(
                              Component.literal(
                                  String.format(
                                      Locale.ROOT,
                                      "Too far to board (%.1f blocks). Walk up to the vehicle and right-click it.",
                                      Math.sqrt(
                                          entity
                                              .getBoundingBox()
                                              .distanceToSqr(player.getEyePosition())))),
                              true);
                        }
                      }
                    }))
        .playToServer(
            VehicleNetworking.Input.TYPE,
            VehicleNetworking.Input.CODEC,
            (data, ctx) ->
                ctx.enqueueWork(
                    () -> {
                      Player player = ctx.player();
                      if (player.level().getEntity(data.entityId())
                          instanceof SarcGroundVehicle v) {
                        v.acceptInput(player, data.forward(), data.left(), data.brake());
                      }
                    }));
  }

  private VehicleNetworking() {}

  public record Board(int entityId) implements CustomPacketPayload {
    public static final Type<VehicleNetworking.Board> TYPE =
        new Type(ResourceLocation.fromNamespaceAndPath("timelocked", "vehicle_board"));
    public static final StreamCodec<RegistryFriendlyByteBuf, VehicleNetworking.Board> CODEC =
        StreamCodec.composite(
            ByteBufCodecs.VAR_INT, VehicleNetworking.Board::entityId, VehicleNetworking.Board::new);

    public Type<VehicleNetworking.Board> type() {
      return TYPE;
    }
  }

  public record Input(int entityId, float forward, float left, boolean brake)
      implements CustomPacketPayload {
    public static final Type<VehicleNetworking.Input> TYPE =
        new Type(ResourceLocation.fromNamespaceAndPath("timelocked", "vehicle_input"));
    public static final StreamCodec<RegistryFriendlyByteBuf, VehicleNetworking.Input> CODEC =
        StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            VehicleNetworking.Input::entityId,
            ByteBufCodecs.FLOAT,
            VehicleNetworking.Input::forward,
            ByteBufCodecs.FLOAT,
            VehicleNetworking.Input::left,
            ByteBufCodecs.BOOL,
            VehicleNetworking.Input::brake,
            VehicleNetworking.Input::new);

    public Type<VehicleNetworking.Input> type() {
      return TYPE;
    }
  }
}
