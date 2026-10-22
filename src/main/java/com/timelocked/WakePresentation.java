package com.timelocked;

/** Shared beach-wake timing and camera curves. No world, player or client state is changed here. */
public final class WakePresentation {
  public static final int TICKS_PER_SECOND = 20;
  public static final int DURATION_TICKS = 9 * TICKS_PER_SECOND;
  public static final int BREATH_LINE_TICK = 48;
  public static final int WHERE_LINE_TICK = 112;
  public static final float FINAL_YAW_DEGREES = -78;
  public static final float FINAL_PITCH_DEGREES = 0;
  private static final float START_YAW_DEGREES = -82;
  private static final float START_PITCH_DEGREES = 58;
  private static final float MID_PITCH_DEGREES = 18;
  private static final double LOW_EYE_BLOCKS = 0.58;
  private static final double SEATED_EYE_BLOCKS = 0.95;
  private static final int HEAD_LIFT_START = 32;
  private static final int HEAD_LIFT_END = 88;
  private static final int STAND_START = 100;
  private static final int STAND_END = 170;
  private static final int LOOK_SETTLE_START = 72;
  private static final int FIRST_OPEN_START = 12;
  private static final int FIRST_OPEN_END = 42;
  private static final int FIRST_BLINK_START = 44;
  private static final int FIRST_BLINK_CLOSED = 50;
  private static final int FIRST_BLINK_REOPEN = 54;
  private static final int FIRST_BLINK_END = 68;
  private static final int SECOND_BLINK_START = 78;
  private static final int SECOND_BLINK_CLOSED = 82;
  private static final int SECOND_BLINK_REOPEN = 85;
  private static final int SECOND_BLINK_END = 96;
  private static final float FIRST_BLINK_OPACITY = 0.88F;
  private static final float SECOND_BLINK_OPACITY = 0.58F;
  private static final int SMOOTHSTEP_CUBIC_COEFFICIENT = 3;
  private static final int VIGNETTE_START = 60;
  private static final int VIGNETTE_END = 150;

  private static float ease(float age, int from, int to) {
    float value = Math.clamp((age - from) / (to - from), 0, 1);
    return value * value * (SMOOTHSTEP_CUBIC_COEFFICIENT - 2 * value);
  }

  public static double eyeHeight(float age, double standingEye) {
    double seated = Math.min(SEATED_EYE_BLOCKS, standingEye);
    double low = Math.min(LOW_EYE_BLOCKS, seated);
    return low
        + (seated - low) * ease(age, HEAD_LIFT_START, HEAD_LIFT_END)
        + (standingEye - seated) * ease(age, STAND_START, STAND_END);
  }

  public static float pitch(float age) {
    return START_PITCH_DEGREES
        + (MID_PITCH_DEGREES - START_PITCH_DEGREES) * ease(age, HEAD_LIFT_START, HEAD_LIFT_END)
        + (FINAL_PITCH_DEGREES - MID_PITCH_DEGREES) * ease(age, STAND_START, STAND_END);
  }

  public static float yaw(float age) {
    return START_YAW_DEGREES
        + (FINAL_YAW_DEGREES - START_YAW_DEGREES) * ease(age, LOOK_SETTLE_START, STAND_END);
  }

  public static float blackout(float age) {
    float initial = 1 - ease(age, FIRST_OPEN_START, FIRST_OPEN_END);
    float first =
        FIRST_BLINK_OPACITY
            * ease(age, FIRST_BLINK_START, FIRST_BLINK_CLOSED)
            * (1 - ease(age, FIRST_BLINK_REOPEN, FIRST_BLINK_END));
    float second =
        SECOND_BLINK_OPACITY
            * ease(age, SECOND_BLINK_START, SECOND_BLINK_CLOSED)
            * (1 - ease(age, SECOND_BLINK_REOPEN, SECOND_BLINK_END));
    return Math.max(initial, Math.max(first, second));
  }

  public static float vignette(float age) {
    return 1 - ease(age, VIGNETTE_START, VIGNETTE_END);
  }

  private WakePresentation() {}
}
