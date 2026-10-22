package com.timelocked;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Server-authoritative encounter music, scoped to the same radius as the breach boss bar. */
@EventBusSubscriber(modid = Timelocked.ID)
public final class BattleMusicNetworking {
  private BattleMusicNetworking() {}

  public static void register(RegisterPayloadHandlersEvent event) {
    event
        .registrar("bionexian-battle-v1")
        .playToClient(
            State.TYPE,
            State.CODEC,
            (data, context) ->
                context.enqueueWork(
                    () ->
                        com.timelocked.client.BionexianBattleMusic.receive(
                            data.active(), data.dimension())));
  }

  @SubscribeEvent
  public static void tick(ServerTickEvent.Post event) {
    if (event.getServer().getTickCount() % 20 != 0) return;
    var progress = FacilityState.get(event.getServer()).progress;
    for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
      boolean active = false;
      if (player.isAlive()
          && !player.isSpectator()
          && player.level().dimension().equals(Level.OVERWORLD)) {
        for (var entry : progress.entrySet()) {
          SiteProgress site = entry.getValue();
          if (site.fighting
              && !site.cleared
              && player
                      .blockPosition()
                      .distSqr(
                          BlueprintRooms.at(player.level(), BlockPos.of(entry.getKey()), 64, 8, 48))
                  < 36864.0) {
            active = true;
            break;
          }
        }
      }
      // A heartbeat also covers joining, respawning and changing dimension mid-encounter.
      PacketDistributor.sendToPlayer(
          player, new State(active, player.level().dimension().location()));
    }
  }

  public record State(boolean active, ResourceLocation dimension) implements CustomPacketPayload {
    public static final Type<State> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(Timelocked.ID, "bionexian_battle_music"));
    public static final StreamCodec<RegistryFriendlyByteBuf, State> CODEC =
        StreamCodec.composite(
            ByteBufCodecs.BOOL,
            State::active,
            ResourceLocation.STREAM_CODEC,
            State::dimension,
            State::new);

    @Override
    public Type<State> type() {
      return TYPE;
    }
  }
}
