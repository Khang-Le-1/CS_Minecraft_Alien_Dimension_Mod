package com.timelocked;

/** Pure, bounded curves for the fall, grounded faceplant and recovery. */
public final class PortalCinematicTimeline {
  public static final int EXIT_TICKS = 60;

  public record Pose(
      float orbit,
      float radius,
      float height,
      float fov,
      float cameraRoll,
      float bodyPitch,
      float bodyRoll,
      float arm,
      float leg,
      float tunnel,
      float bodyDrop,
      float faceplant) {}

  public static float smooth(float x) {
    x = Math.clamp(x, 0F, 1F);
    return x * x * (3F - 2F * x);
  }

  private static float lerp(float a, float b, float t) {
    return a + (b - a) * t;
  }

  /** Approximate grounding for offline previews; the live client passes actual grounded age. */
  public static Pose sample(float age, float exitAge) {
    return sample(age, exitAge, exitAge < 0 ? -1 : Math.max(-1, exitAge - 12));
  }

  public static Pose sample(float age, float exitAge, float groundedAge) {
    age = Math.max(0, age);
    float enter = smooth(age / 18F);
    float spinAge = exitAge < 0 ? age : Math.max(0, age - exitAge);
    float pitch = (float) Math.IEEEremainder(spinAge * 4.4 * smooth(spinAge / 18F), 360);
    float orbit = -35 + 75 * enter + spinAge * .55F;
    if (exitAge < 0)
      return new Pose(
          orbit,
          4.2F + .6F * enter,
          1.6F + .35F * (float) Math.sin(age * .045),
          58 + 10 * enter,
          2.5F * (float) Math.sin(age * .035),
          pitch,
          (float) Math.sin(spinAge * .07) * 24 * enter,
          (-1.65F + .25F * (float) Math.sin(age * .14)) * enter,
          (.32F + .14F * (float) Math.sin(age * .16)) * enter,
          smooth(age / 8F),
          0,
          0);
    float orient = smooth(exitAge / 10F), impact = groundedAge < 0 ? 0 : smooth(groundedAge / 6F);
    float recover = groundedAge < 0 ? 0 : smooth((groundedAge - 30) / 28F);
    float flat = impact * (1 - recover);
    // Negative X pitch puts the face DOWN; lower the torso so the face meets the floor.
    // Hold flat for a beat, then lift and rotate upright during the push-up recovery.
    float body =
        groundedAge < 0 ? lerp(pitch, -40, orient) : lerp(-40, -90, impact) * (1 - recover);
    float arm =
        groundedAge < 0
            ? lerp(-1.65F, -2.15F, orient)
            : lerp(-2.15F, -2.65F, impact) * (1 - recover);
    return new Pose(
        lerp(orbit, 35, smooth(exitAge / 12F)),
        lerp(4.8F, 3.8F, orient),
        lerp(1.6F, .85F, orient),
        62,
        groundedAge >= 0 && groundedAge < 8
            ? (float) Math.sin(groundedAge * 2.2) * 1.4F * (1 - groundedAge / 8)
            : 0,
        body,
        0,
        arm,
        groundedAge < 0 ? .15F : .08F * flat,
        1 - smooth(exitAge / 8F),
        .65F * flat,
        flat);
  }

  private PortalCinematicTimeline() {}
}
