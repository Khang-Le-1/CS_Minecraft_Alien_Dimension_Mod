package com.timelocked;

/**
 * Shared timing data for the jump cinematic. The server runs the authoritative sequence on this
 * timeline; each client renders the same beats from its own local clock after the Start packet
 * arrives.
 */
public final class JumpTimeline {
  /** Start of the panel beat. */
  public static final long PANEL = 0L;

  /** Panel fades out; the drive begins its spin-up. */
  public static final long DRIVE = 40L;

  /** Drive is spun up; cut outside and light the engines. */
  public static final long ENGINES = 95L;

  /** Engines at full power; the departure flash begins. */
  public static final long FLASH = 150L;

  /** The ship actually jumps. One commit, never repeated. */
  public static final long COMMIT = 178L;

  /** Exit of the wormhole; arrival beat begins. */
  public static final long ARRIVAL = 250L;

  /** Sequence ends and control is restored. */
  public static final long END = 360L;

  public enum Shot {
    PANEL,
    DRIVE,
    ENGINES,
    FLASH,
    SILENCE,
    ARRIVAL,
    DONE
  }

  public static Shot shot(float age) {
    if (age < DRIVE) return Shot.PANEL;
    if (age < ENGINES) return Shot.DRIVE;
    if (age < FLASH) return Shot.ENGINES;
    if (age < COMMIT) return Shot.FLASH;
    if (age < ARRIVAL) return Shot.SILENCE;
    if (age < END) return Shot.ARRIVAL;
    return Shot.DONE;
  }

  /** Soft single departure flash, 0..1. No repeated strobing. */
  public static float flash(float age) {
    float t = age - FLASH;
    if (t < 0.0F || t >= 30.0F) return 0.0F;
    float k = t / 30.0F;
    return (float) Math.sin(Math.PI * Math.clamp(k, 0.0F, 1.0F));
  }

  /** Engine glow/exhaust power from 0..1 across the spin-up and ignition beats. */
  public static float engines(float age) {
    if (age < DRIVE) return 0.0F;
    if (age < ENGINES) return 0.45F * (age - DRIVE) / (float) (ENGINES - DRIVE);
    if (age < FLASH) return 0.45F + 0.55F * (age - ENGINES) / (float) (FLASH - ENGINES);
    if (age < ARRIVAL) return 0.2F;
    return 0.0F;
  }

  /**
   * Camera position for each beat, expressed in TIMESKIPPER ship-dimension world coordinates (the
   * vessel is built around origin 0,100,0).
   */
  public static Rig camera(float age) {
    Shot s = shot(age);
    return switch (s) {
      case PANEL -> look(0.5F, 102.8F, -16.4F, 0.5F, 101.5F, -18.6F, 46.0F);
      case DRIVE -> look(0.6F, 102.9F, 19.8F, 0.4F, 102.3F, 18.2F, 52.0F);
      case ENGINES -> look(16.5F, 105.8F, 14.5F, 0.0F, 102.8F, 9.0F, 58.0F);
      case FLASH -> look(19.5F, 106.6F, 12.0F, 0.0F, 103.0F, 9.0F, 68.0F);
      case SILENCE -> look(17.0F, 105.2F, 13.0F, 0.0F, 102.8F, 9.0F, 62.0F);
      case ARRIVAL, DONE -> look(-15.0F, 105.4F, -14.5F, 0.0F, 103.0F, -8.0F, 58.0F);
    };
  }

  public record Rig(float x, float y, float z, float yaw, float pitch, float fov) {}

  /** Aim a rig positioned at (ex,ey,ez) at the point (tx,ty,tz). */
  public static Rig look(float ex, float ey, float ez, float tx, float ty, float tz, float fov) {
    float dx = tx - ex;
    float dy = ty - ey;
    float dz = tz - ez;
    float horiz = (float) Math.hypot(dx, dz);
    float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
    float pitch = horiz < 1.0E-4F ? 0.0F : (float) Math.toDegrees(Math.atan2(-dy, horiz));
    return new Rig(ex, ey, ez, yaw, pitch, fov);
  }

  private JumpTimeline() {}
}
