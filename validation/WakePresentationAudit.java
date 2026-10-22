package com.timelocked;

/** Numerical camera/overlay checks: renderer-independent and safe on a dedicated server. */
final class WakePresentationAudit {
  static void run() {
    double previousEye = WakePresentation.eyeHeight(0, 1.62);
    float previousPitch = WakePresentation.pitch(0);
    for (float age = 0; age <= WakePresentation.DURATION_TICKS + 20; age += .25F) {
      double eye = WakePresentation.eyeHeight(age, 1.62);
      float pitch = WakePresentation.pitch(age), yaw = WakePresentation.yaw(age);
      float opacity = WakePresentation.blackout(age), edge = WakePresentation.vignette(age);
      IntroAudit.check(
          Double.isFinite(eye) && eye >= .58 && eye <= 1.620001,
          "wake eye stays above ground and below standing height");
      IntroAudit.check(
          eye >= previousEye && eye - previousEye < .01, "wake rise remains monotonic and gentle");
      IntroAudit.check(
          pitch <= previousPitch && pitch >= 0 && previousPitch - pitch < .5,
          "wake head lift remains smooth without shake");
      IntroAudit.check(yaw >= -82 && yaw <= -78, "wake yaw has only a small settling motion");
      IntroAudit.check(
          opacity >= 0 && opacity <= 1 && edge >= 0 && edge <= 1, "wake overlay alpha bounded");
      previousEye = eye;
      previousPitch = pitch;
    }
    IntroAudit.check(
        WakePresentation.blackout(0) == 1 && WakePresentation.blackout(12) == 1,
        "wake holds black before opening");
    IntroAudit.check(
        WakePresentation.blackout(42) == 0 && WakePresentation.blackout(51) > .8,
        "first blink follows initial opening");
    IntroAudit.check(
        WakePresentation.blackout(70) == 0 && WakePresentation.blackout(83) > .5,
        "second blink is separate and gentler");
    IntroAudit.check(
        WakePresentation.blackout(100) == 0 && WakePresentation.vignette(150) == 0,
        "overlays fully clear before handoff");
    IntroAudit.check(
        WakePresentation.eyeHeight(180, 1.62) == 1.62
            && WakePresentation.pitch(180) == WakePresentation.FINAL_PITCH_DEGREES
            && WakePresentation.yaw(180) == WakePresentation.FINAL_YAW_DEGREES,
        "final pose exactly matches normal gameplay");
    IntroAudit.check(
        WakePresentation.eyeHeight(180, .9) == .9, "shorter actual eye height supported");
  }

  private WakePresentationAudit() {}
}
