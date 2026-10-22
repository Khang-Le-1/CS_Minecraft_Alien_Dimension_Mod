package com.timelocked;

/** Presentation only: timings derived from the supplied Siren Sound recording. */
public final class AtlasBootSequence {
  public static final long DURATION_MS = 44800;
  public static final int NONE = 0, START = 1, STOP = 2, COMPLETE = 3;

  public record Cue(long start, long end, String key) {}

  public static final java.util.List<Cue> CUES =
      java.util.List.of(
          new Cue(0, 11800, "identity"),
          new Cue(13060, 15480, "primary"),
          new Cue(19000, 21380, "weapons"),
          new Cue(23000, 27520, "munitions"),
          new Cue(30000, 33520, "vitals"),
          new Cue(36000, 39540, "trauma"),
          new Cue(40860, 44220, "safe"));
  private boolean initialized, previousFull, pending, running;
  private long age;

  /** First observation binds the current equipment without replaying on reconnect. */
  public int observe(boolean full, boolean allowed) {
    if (!initialized) {
      initialized = true;
      previousFull = full;
      return NONE;
    }
    if (!full) {
      boolean stopped = running;
      running = false;
      pending = false;
      previousFull = false;
      age = 0;
      return stopped ? STOP : NONE;
    }
    if (!previousFull) pending = true;
    previousFull = true;
    if (pending && allowed) {
      pending = false;
      running = true;
      age = 0;
      return START;
    }
    return NONE;
  }

  public int advance(long milliseconds) {
    if (!running) return NONE;
    age = Math.min(DURATION_MS, age + Math.clamp(milliseconds, 0, DURATION_MS));
    if (age >= DURATION_MS) {
      running = false;
      return COMPLETE;
    }
    return NONE;
  }

  /** A dimension transition ends the presentation but never starts another boot. */
  public void bind(boolean full) {
    initialized = true;
    previousFull = full;
    pending = false;
    running = false;
    age = 0;
  }

  public void reset() {
    initialized = false;
    previousFull = false;
    pending = false;
    running = false;
    age = 0;
  }

  public boolean running() {
    return running;
  }

  public long age() {
    return age;
  }

  public static int phase(long milliseconds) {
    int phase = 0;
    for (int i = 0; i < CUES.size(); i++) if (milliseconds >= CUES.get(i).start()) phase = i;
    return phase;
  }

  public static float progress(long milliseconds) {
    return Math.clamp(milliseconds / (float) DURATION_MS, 0F, 1F);
  }

  public AtlasBootSequence() {}
}
