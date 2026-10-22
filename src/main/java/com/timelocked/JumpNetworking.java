package com.timelocked;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Cinematic sequence packets between the server and the players aboard the ship. */
public final class JumpNetworking {
  public static void register(RegisterPayloadHandlersEvent e) {
    e.registrar("jump-v1")
        .optional()
        .playToServer(
            Request.TYPE,
            Request.CODEC,
            (d, ctx) -> ctx.enqueueWork(() -> JumpSequence.request(ctx.player(), d.target())))
        .playToServer(
            Skip.TYPE,
            Skip.CODEC,
            (d, ctx) ->
                ctx.enqueueWork(
                    () ->
                        JumpSequence.skip(
                            (net.minecraft.server.level.ServerPlayer) ctx.player(), d.token())))
        .playToClient(
            Start.TYPE,
            Start.CODEC,
            (d, ctx) ->
                ctx.enqueueWork(
                    () -> JumpClientBridge.begin(d.token(), d.from(), d.to(), d.started())))
        .playToClient(
            Stop.TYPE,
            Stop.CODEC,
            (d, ctx) -> ctx.enqueueWork(() -> JumpClientBridge.stop(d.success())))
        .playToClient(
            Navigation.TYPE,
            Navigation.CODEC,
            (d, ctx) -> ctx.enqueueWork(() -> JumpClientBridge.navigation(d)))
        .playToClient(
            Notice.TYPE,
            Notice.CODEC,
            (d, ctx) -> ctx.enqueueWork(() -> JumpClientBridge.notice(d.message())));
  }

  private static <T extends CustomPacketPayload> Type<T> packetType(String name) {
    return new Type<>(ResourceLocation.fromNamespaceAndPath(Timelocked.ID, name));
  }

  private JumpNetworking() {}

  /** Server -> client: open the flight navigation panel. */
  public record Navigation(int location, int charges, boolean creative)
      implements CustomPacketPayload {
    public static final Type<Navigation> TYPE = packetType("jump_navigation");
    public static final StreamCodec<RegistryFriendlyByteBuf, Navigation> CODEC =
        StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            Navigation::location,
            ByteBufCodecs.VAR_INT,
            Navigation::charges,
            ByteBufCodecs.BOOL,
            Navigation::creative,
            Navigation::new);

    @Override
    public Type<Navigation> type() {
      return TYPE;
    }
  }

  /** Client -> server: the chosen destination index. */
  public record Request(int target) implements CustomPacketPayload {
    public static final Type<Request> TYPE = packetType("jump_request");
    public static final StreamCodec<RegistryFriendlyByteBuf, Request> CODEC =
        StreamCodec.composite(ByteBufCodecs.VAR_INT, Request::target, Request::new);

    @Override
    public Type<Request> type() {
      return TYPE;
    }
  }

  /** Server -> client: begin the shared sequence. */
  public record Start(int token, int from, int to, long started) implements CustomPacketPayload {
    public static final Type<Start> TYPE = packetType("jump_start");
    public static final StreamCodec<RegistryFriendlyByteBuf, Start> CODEC =
        StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            Start::token,
            ByteBufCodecs.VAR_INT,
            Start::from,
            ByteBufCodecs.VAR_INT,
            Start::to,
            ByteBufCodecs.VAR_LONG,
            Start::started,
            Start::new);

    @Override
    public Type<Start> type() {
      return TYPE;
    }
  }

  /** Server -> client: sequence finished (or was aborted). */
  public record Stop(int token, boolean success) implements CustomPacketPayload {
    public static final Type<Stop> TYPE = packetType("jump_stop");
    public static final StreamCodec<RegistryFriendlyByteBuf, Stop> CODEC =
        StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Stop::token, ByteBufCodecs.BOOL, Stop::success, Stop::new);

    @Override
    public Type<Stop> type() {
      return TYPE;
    }
  }

  /** Client -> server: skip the remainder of the sequence. */
  public record Skip(int token) implements CustomPacketPayload {
    public static final Type<Skip> TYPE = packetType("jump_skip");
    public static final StreamCodec<RegistryFriendlyByteBuf, Skip> CODEC =
        StreamCodec.composite(ByteBufCodecs.VAR_INT, Skip::token, Skip::new);

    @Override
    public Type<Skip> type() {
      return TYPE;
    }
  }

  /** Server -> client: a status line for the open navigation panel. */
  public record Notice(String message) implements CustomPacketPayload {
    public static final Type<Notice> TYPE = packetType("jump_notice");
    public static final StreamCodec<RegistryFriendlyByteBuf, Notice> CODEC =
        StreamCodec.composite(ByteBufCodecs.STRING_UTF8, Notice::message, Notice::new);

    @Override
    public Type<Notice> type() {
      return TYPE;
    }
  }
}
