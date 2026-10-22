package com.timelocked;

import java.util.function.IntConsumer;
import java.util.function.IntPredicate;

/** Shared startup contract: three distinct channels, never three cells in one slot. */
public final class ReactorFuelChannels {
  public static final int COUNT = 3;

  public static boolean loaded(IntPredicate valid) {
    for (int i = 0; i < COUNT; i++) if (!valid.test(i)) return false;
    return true;
  }

  public static boolean consume(IntPredicate valid, IntConsumer take) {
    if (!loaded(valid)) return false;
    for (int i = 0; i < COUNT; i++) take.accept(i);
    return true;
  }

  private ReactorFuelChannels() {}
}
