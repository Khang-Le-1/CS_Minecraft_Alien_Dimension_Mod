package com.timelocked;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Server to client cues for the animated portal fall. */
public final class PortalTransitNetworking {
  private PortalTransitNetworking() {}

  public static void register(RegisterPayloadHandlersEvent e) {
    e.registrar("portal-transit-v1")
        .optional()
        .playToClient(
            Phase.TYPE,
            Phase.CODEC,
            (d, ctx) ->
                ctx.enqueueWork(() -> com.timelocked.client.PortalTransitClient.phase(d.phase())));
  }

  /** phase 0 = the fall begins, phase 1 = you are being thrown out of the destination gate. */
  public record Phase(int phase) implements CustomPacketPayload {
    public static final int START = 0;
    public static final int END = 1;
    public static final Type<Phase> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(Timelocked.ID, "portal_transit"));
    public static final StreamCodec<RegistryFriendlyByteBuf, Phase> CODEC =
        StreamCodec.composite(ByteBufCodecs.VAR_INT, Phase::phase, Phase::new);

    @Override
    public Type<Phase> type() {
      return TYPE;
    }
  }
}
