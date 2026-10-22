package com.timelocked;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Tells clients where the TIMESKIPPER is parked so the view out of its windows matches. */
public final class SpaceNetworking {
  public static void register(RegisterPayloadHandlersEvent e) {
    e.registrar("space-v1")
        .optional()
        .playToClient(
            ShipLocation.TYPE,
            ShipLocation.CODEC,
            (d, ctx) -> ctx.enqueueWork(() -> SpacePlanets.clientShipLocation = d.index()));
  }

  private SpaceNetworking() {}

  public record ShipLocation(int index) implements CustomPacketPayload {
    public static final Type<ShipLocation> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath("timelocked", "ship_location"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ShipLocation> CODEC =
        StreamCodec.composite(ByteBufCodecs.VAR_INT, ShipLocation::index, ShipLocation::new);

    @Override
    public Type<ShipLocation> type() {
      return TYPE;
    }
  }
}
