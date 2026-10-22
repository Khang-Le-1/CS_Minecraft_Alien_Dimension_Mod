package com.timelocked;

/**
 * Pure rules, kept separate so activation and steady beacon emission can be tested without a game
 * client.
 */
public final class AlarmRules {
  // The normal beacon sound repeats every two server seconds (20 ticks/second).
  private static final int NORMAL_SOUND_PERIOD_TICKS = 40;

  public static boolean active(boolean manual, boolean redstone, boolean encounter) {
    return manual || redstone || encounter;
  }

  public static boolean lit(boolean active, long tick) {
    return active;
  }

  public static boolean soundDue(long now, long previous) {
    return soundDue(now, previous, NORMAL_SOUND_PERIOD_TICKS);
  }

  /**
   * The danger alarm clip is about 6.3 s long, so it re-triggers every 130 ticks (6.5 s) to avoid
   * overlapping itself.
   */
  public static final int DANGER_PERIOD = 130;

  public static boolean soundDue(long now, long previous, int period) {
    return now < previous || now - previous >= period;
  }

  private AlarmRules() {}
}
