package com.timelocked;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Per-player heartbeat; no shared objective, retained player map, chunk load or save mutation. */
@EventBusSubscriber(modid = "timelocked")
public final class ObjectiveTracker {
  @SubscribeEvent
  public static void tick(ServerTickEvent.Post event) {
    if (event.getServer().overworld().getGameTime() % 10 != 0) return;
    for (var player : event.getServer().getPlayerList().getPlayers())
      PacketDistributor.sendToPlayer(
          player,
          ObjectiveNetworking.Update.of(
              StoryObjective.resolve(player, IntroSequence.state(player))));
  }

  private ObjectiveTracker() {}
}
