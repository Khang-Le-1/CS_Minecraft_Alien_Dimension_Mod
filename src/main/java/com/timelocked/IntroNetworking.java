package com.timelocked;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class IntroNetworking {
  static void register(RegisterPayloadHandlersEvent e) {
    var r = e.registrar("intro-v2");
    r.playToClient(
        Cue.TYPE,
        Cue.CODEC,
        (d, ctx) ->
            ctx.enqueueWork(
                () ->
                    com.timelocked.client.IntroClient.cue(
                        d.stage(), d.age(), d.anchor(), d.token())));
    r.playToServer(
        Ready.TYPE,
        Ready.CODEC,
        (d, ctx) ->
            ctx.enqueueWork(
                () -> {
                  if (ctx.player() instanceof net.minecraft.server.level.ServerPlayer p)
                    IntroSequence.ready(p, d.token());
                }));
  }

  public record Cue(int stage, int age, long anchor, int token) implements CustomPacketPayload {
    public Cue(int stage, int age) {
      this(stage, age, 0, 0);
    }

    public static final Type<Cue> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath("timelocked", "intro_cue"));
    public static final StreamCodec<RegistryFriendlyByteBuf, Cue> CODEC =
        StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            Cue::stage,
            ByteBufCodecs.VAR_INT,
            Cue::age,
            ByteBufCodecs.VAR_LONG,
            Cue::anchor,
            ByteBufCodecs.VAR_INT,
            Cue::token,
            Cue::new);

    @Override
    public Type<Cue> type() {
      return TYPE;
    }
  }

  public record Ready(int token) implements CustomPacketPayload {
    public static final Type<Ready> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath("timelocked", "intro_ready"));
    public static final StreamCodec<RegistryFriendlyByteBuf, Ready> CODEC =
        StreamCodec.composite(ByteBufCodecs.VAR_INT, Ready::token, Ready::new);

    @Override
    public Type<Ready> type() {
      return TYPE;
    }
  }
}
