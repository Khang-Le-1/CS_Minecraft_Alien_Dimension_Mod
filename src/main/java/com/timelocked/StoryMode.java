package com.timelocked;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameRules;

/** Server-owned campaign flag, saved with the world; vanilla game types stay intact. */
public final class StoryMode {
  public static final GameRules.Key<GameRules.BooleanValue> RULE =
      GameRules.register(
          "timelockedStory", GameRules.Category.PLAYER, GameRules.BooleanValue.create(false));

  public static void initialize() {}

  public static boolean enabled(MinecraftServer server) {
    return server.getGameRules().getBoolean(RULE);
  }

  private StoryMode() {}
}
