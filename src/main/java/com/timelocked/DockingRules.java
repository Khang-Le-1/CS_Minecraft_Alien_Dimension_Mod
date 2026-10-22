package com.timelocked;

/**
 * Pure interlock policy, shared with offline tests. Stage 4 is the only walk-through door state.
 */
public final class DockingRules {
  // Existing saved/network stage IDs. Names describe interlocks; do not renumber them.
  private static final int ALIGNED_STAGE = 1;
  private static final int SEALED_STAGE = 2;
  private static final int WALK_THROUGH_STAGE = 4;

  public static boolean firstEndpoint(long first, long second) {
    return first <= second;
  }

  public static int afterEqualization(boolean paired, boolean sealed, boolean supported) {
    return paired && sealed && supported ? WALK_THROUGH_STAGE : SEALED_STAGE;
  }

  public static boolean canRelease(int stage, boolean occupied) {
    return (stage == ALIGNED_STAGE || stage == SEALED_STAGE) && !occupied;
  }

  public static boolean canClose(int stage, boolean doorwayOccupied) {
    return stage == WALK_THROUGH_STAGE && !doorwayOccupied;
  }

  private DockingRules() {}
}
