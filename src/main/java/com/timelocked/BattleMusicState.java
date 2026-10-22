package com.timelocked;

/** Heartbeat latch. No game classes, so timeout behaviour can be tested in isolation. */
public final class BattleMusicState {
  public static final int HEARTBEAT_TIMEOUT_TICKS = 60;
  private int remaining;

  public void receive(boolean active) {
    remaining = active ? HEARTBEAT_TIMEOUT_TICKS : 0;
  }

  public void tick(boolean validContext) {
    if (!validContext) remaining = 0;
    else if (remaining > 0) remaining--;
  }

  public boolean active() {
    return remaining > 0;
  }
}
