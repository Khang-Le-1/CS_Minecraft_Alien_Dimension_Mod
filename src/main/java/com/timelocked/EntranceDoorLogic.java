package com.timelocked;

/** Pure decision rules shared by server tick and deterministic regression tests. */
public final class EntranceDoorLogic {
  public static final int HOLD_TICKS = 24;

  private EntranceDoorLogic() {}

  public static boolean rightHinge(int faceX, int faceZ, int mateX, int mateZ) {
    return faceX * mateZ - faceZ * mateX < 0;
  }

  public static boolean shouldOpen(
      long now, long holdUntil, boolean nearby, boolean occupied, boolean powered) {
    return nearby || occupied || powered || now < holdUntil;
  }
}
