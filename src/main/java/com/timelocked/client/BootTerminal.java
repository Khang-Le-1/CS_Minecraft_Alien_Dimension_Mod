package com.timelocked.client;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/** Bounded terminal history. Text comes only from real loader notifications. */
public final class BootTerminal {
  private final ArrayDeque<String> lines = new ArrayDeque<>();
  private final LinkedHashSet<String> seen = new LinkedHashSet<>();
  private long newestAt;

  public void offer(String raw, long now) {
    String text = clean(raw);
    if (text.isBlank() || !seen.add(text)) return;
    if (seen.size() > 256) seen.remove(seen.iterator().next());
    lines.addLast(text);
    while (lines.size() > 6) lines.removeFirst();
    newestAt = now;
  }

  public List<String> visible(long now) {
    var result = new ArrayList<>(lines);
    if (!result.isEmpty()) {
      int i = result.size() - 1;
      String last = result.get(i);
      long elapsed = Math.max(0, now - newestAt);
      int count = (int) Math.min(last.length(), elapsed * 80 / 1000);
      result.set(i, last.substring(0, count));
    }
    return result;
  }

  public static String clean(String s) {
    if (s == null) return "";
    StringBuilder b = new StringBuilder();
    for (int i = 0; i < s.length() && b.length() < 240; i++) {
      char c = s.charAt(i);
      b.append(c >= 32 && c <= 126 ? c : c == '\n' || c == '\r' || c == '\t' ? ' ' : '?');
    }
    return b.toString().strip();
  }

  public BootTerminal() {}
}
