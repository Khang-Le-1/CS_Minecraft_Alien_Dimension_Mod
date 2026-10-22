package com.timelocked;

import net.minecraft.server.MinecraftServer;

/** Deterministic local-presentation state tests, not a rendered-client or listening test. */
final class AtlasBootAudit {
  static void check(boolean condition, String label) {
    IntroAudit.check(condition, "ATLAS boot: " + label);
  }

  static void run(MinecraftServer server) throws Exception {
    var boot = new AtlasBootSequence();
    check(
        boot.observe(true, true) == 0 && !boot.running(),
        "joining already equipped does not replay");
    check(boot.observe(true, true) == 0, "held equipment never retriggers");
    check(boot.observe(false, true) == 0, "remove suit arms next suit-up");
    check(
        boot.observe(true, true) == AtlasBootSequence.START && boot.running(),
        "complete equipment edge starts once");
    for (int i = 0; i < 100; i++)
      check(boot.observe(true, true) == 0, "no repeated audio trigger " + i);
    check(boot.age() == 0, "render/equipment observations do not advance time");
    check(boot.advance(1000) == 0 && boot.age() == 1000, "elapsed-time advance");
    check(
        boot.advance(-1000) == 0 && boot.age() == 1000, "clock regression never reverses sequence");
    check(
        boot.observe(false, true) == AtlasBootSequence.STOP && !boot.running(),
        "removing any required part cancels");
    check(boot.age() == 0, "cancel resets progress");
    check(boot.observe(true, false) == 0 && !boot.running(), "cinematic defers fresh suit-up");
    check(boot.observe(true, false) == 0, "deferred cue stays silent");
    check(
        boot.observe(true, true) == AtlasBootSequence.START,
        "start after cinematic if still suited");
    boot.bind(true);
    check(
        !boot.running() && boot.observe(true, true) == 0, "dimension/death bind does not restart");
    boot.reset();
    check(boot.observe(false, true) == 0, "fresh session observation");
    check(boot.observe(true, false) == 0, "second deferred boot");
    boot.observe(false, false);
    check(boot.observe(false, true) == 0 && !boot.running(), "removing suit cancels deferred boot");
    check(
        boot.observe(true, true) == AtlasBootSequence.START, "re-equipping starts from beginning");
    check(
        boot.advance(Long.MAX_VALUE) == AtlasBootSequence.COMPLETE && !boot.running(),
        "long gap completes once without overflow");
    check(boot.advance(1) == 0, "completed sequence stays complete");
    check(boot.observe(true, true) == 0, "no automatic loop at end");
    check(AtlasBootSequence.CUES.size() == 7, "seven voice-aligned beats");
    long last = -1;
    for (int i = 0; i < 7; i++) {
      var cue = AtlasBootSequence.CUES.get(i);
      check(
          cue.start() > last
              && cue.end() > cue.start()
              && cue.end() <= AtlasBootSequence.DURATION_MS,
          "valid timing " + i);
      check(AtlasBootSequence.phase(cue.start()) == i, "stage begins with spoken cue " + i);
      if (i > 0)
        check(
            AtlasBootSequence.phase(cue.start() - 1) == i - 1,
            "stage does not announce early " + i);
      last = cue.end();
    }
    for (int ms = 0; ms <= AtlasBootSequence.DURATION_MS; ms += 20) {
      float p = AtlasBootSequence.progress(ms);
      check(p >= 0 && p <= 1, "progress bounded " + ms);
    }
    check(
        AtlasBootSequence.progress(-1) == 0 && AtlasBootSequence.progress(Long.MAX_VALUE) == 1,
        "progress clamps bounds");
    check(
        AtlasStartupSounds.BOOT.get().getLocation().toString().equals("timelocked:atlas_startup"),
        "startup sound registered natively");
    try (var in =
        AtlasBootAudit.class
            .getClassLoader()
            .getResourceAsStream("assets/timelocked/sounds/atlas/startup.ogg")) {
      check(in != null, "audio resource packaged");
      if (in != null)
        check(
            new String(in.readNBytes(4), java.nio.charset.StandardCharsets.US_ASCII).equals("OggS"),
            "valid OGG container signature");
    }
  }
}
