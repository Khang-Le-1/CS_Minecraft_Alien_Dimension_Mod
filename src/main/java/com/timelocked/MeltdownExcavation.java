package com.timelocked;

/**
 * Deterministic, resumable cell cursor. Checking cells, not just changed blocks, counts against the
 * budget.
 */
public final class MeltdownExcavation {
  public static final int GLOBAL_BUDGET = 4096;
  public static final int[] INDUSTRIAL_RADIUS = {56, 64, 48, 60};

  public record Cell(int x, int y, int z) {}

  public static final class Cursor {
    public final int radius, span, top, bottom, width, total;
    private int index;

    public Cursor(int radius, boolean industrial) {
      if (radius < 1 || radius > 64) throw new IllegalArgumentException("radius");
      this.radius = radius;
      span = (int) Math.ceil(radius * 1.25) + 3;
      top = industrial ? (int) Math.ceil(radius * 1.55) : radius + 1;
      bottom = industrial ? (int) Math.ceil(radius * 1.15) : radius + 1;
      width = span * 2 + 1;
      total = (top + bottom + 1) * width * width;
    }

    public int index() {
      return index;
    }

    public void restore(int value) {
      index = Math.max(0, Math.min(total, value));
    }

    public boolean done() {
      return index >= total;
    }

    public Cell next() {
      if (done()) throw new IllegalStateException("excavation complete");
      int n = index++, area = width * width;
      return new Cell(n % area / width - span, top - n / area, n % width - span);
    }
  }

  private MeltdownExcavation() {}
}
