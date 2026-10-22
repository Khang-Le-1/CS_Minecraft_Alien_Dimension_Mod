package com.timelocked;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class ObjectiveNetworking {
  static void register(RegisterPayloadHandlersEvent e) {
    e.registrar("objective-v1")
        .playToClient(
            Update.TYPE,
            Update.CODEC,
            (data, context) ->
                context.enqueueWork(() -> com.timelocked.client.ObjectiveHud.receive(data)));
  }

  public record Update(
      String label, ResourceLocation dimension, boolean hasTarget, long position, boolean height)
      implements CustomPacketPayload {
    public static final Type<Update> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath("timelocked", "objective"));
    public static final StreamCodec<RegistryFriendlyByteBuf, Update> CODEC =
        StreamCodec.composite(
            ByteBufCodecs.stringUtf8(128),
            Update::label,
            ResourceLocation.STREAM_CODEC,
            Update::dimension,
            ByteBufCodecs.BOOL,
            Update::hasTarget,
            ByteBufCodecs.VAR_LONG,
            Update::position,
            ByteBufCodecs.BOOL,
            Update::height,
            Update::new);

    static Update of(StoryObjective.View view) {
      return view == null
          ? new Update("", net.minecraft.world.level.Level.OVERWORLD.location(), false, 0, false)
          : new Update(
              view.label(),
              view.dimension(),
              view.target() != null,
              view.target() == null ? 0 : view.target().asLong(),
              view.height());
    }

    @Override
    public Type<Update> type() {
      return TYPE;
    }
  }

  private ObjectiveNetworking() {}
}
