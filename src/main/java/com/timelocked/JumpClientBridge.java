package com.timelocked;

/**
 * Common-code callbacks that the client wires up during setup. The server never receives
 * play-to-client packets, so its copies of these fields stay null and are never invoked on the
 * server side.
 */
public final class JumpClientBridge {
  @FunctionalInterface
  public interface Begin {
    void run(int token, int from, int to, long started);
  }

  @FunctionalInterface
  public interface Stop {
    void run(boolean success);
  }

  @FunctionalInterface
  public interface Navigation {
    void run(JumpNetworking.Navigation nav);
  }

  @FunctionalInterface
  public interface Notice {
    void run(String message);
  }

  public static Begin start;
  public static Stop stop;
  public static Navigation navigation;
  public static Notice notice;

  public static void begin(int token, int from, int to, long started) {
    if (start != null) start.run(token, from, to, started);
  }

  public static void stop(boolean success) {
    if (stop != null) stop.run(success);
  }

  public static void navigation(JumpNetworking.Navigation nav) {
    if (navigation != null) navigation.run(nav);
  }

  public static void notice(String message) {
    if (notice != null) notice.run(message);
  }

  private JumpClientBridge() {}
}
