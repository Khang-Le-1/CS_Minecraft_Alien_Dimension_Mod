/* Reconstructed from the user-provided 0.8.9.8 release with permission. */
package com.timelocked;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

public final class SiteProgress {
  // Save-format limits, shared with the arrival controller. Not arbitrary tuning knobs.
  static final int FINAL_ARRIVAL_STAGE = 5;
  static final int WELCOME_LINE_COUNT = 7;
  static final int MAX_WELCOME_DELAY_TICKS = 200;
  private static final int FINAL_ENCOUNTER_WAVE = 3;

  public int arrivalStep;
  public int arrivalTicks;
  public boolean arrivalStarted;
  public boolean arrivalLogoAnnounced;
  // Number of welcome lines already sent; zero means no timed sequence.
  public int arrivalWelcomeNext;
  public int arrivalWelcomeTicks;
  public int geometryVersion;
  public UUID receptionist;
  public UUID briefingOfficer;
  public UUID founder;
  public UUID coastalTruck;
  public boolean powered;
  public boolean cleared;
  public boolean fighting;
  public boolean manualAlarm;
  public int wave;
  public int layoutVersion;
  public long nextWaveTick;
  public final Set<UUID> living = new HashSet<UUID>();

  public CompoundTag save() {
    CompoundTag t = new CompoundTag();
    t.putInt("ArrivalStep", this.arrivalStep);
    t.putInt("ArrivalTicks", this.arrivalTicks);
    t.putBoolean("ArrivalStarted", this.arrivalStarted);
    t.putBoolean("ArrivalLogoAnnounced", this.arrivalLogoAnnounced);
    t.putInt("ArrivalWelcomeNext", this.arrivalWelcomeNext);
    t.putInt("ArrivalWelcomeTicks", this.arrivalWelcomeTicks);
    t.putInt("LayoutVersion", this.layoutVersion);
    t.putInt("GeometryVersion", this.geometryVersion);
    if (this.receptionist != null) t.putUUID("Receptionist", this.receptionist);
    if (this.briefingOfficer != null) t.putUUID("BriefingOfficer", this.briefingOfficer);
    if (this.founder != null) t.putUUID("Founder", this.founder);
    if (this.coastalTruck != null) t.putUUID("CoastalTruck", this.coastalTruck);
    t.putBoolean("Powered", this.powered);
    t.putBoolean("Cleared", this.cleared);
    t.putBoolean("Fighting", this.fighting);
    t.putBoolean("ManualAlarm", this.manualAlarm);
    t.putInt("Wave", this.wave);
    t.putLong("NextWave", this.nextWaveTick);
    ListTag ids = new ListTag();
    this.living.forEach(
        id -> {
          CompoundTag v = new CompoundTag();
          v.putUUID("ID", id);
          ids.add(v);
        });
    t.put("Living", (Tag) ids);
    return t;
  }

  public static SiteProgress load(CompoundTag t) {
    SiteProgress s = new SiteProgress();
    s.arrivalStep = Math.clamp((long) t.getInt("ArrivalStep"), 0, FINAL_ARRIVAL_STAGE);
    s.arrivalTicks = Math.max(0, t.getInt("ArrivalTicks"));
    s.arrivalStarted = t.getBoolean("ArrivalStarted");
    s.arrivalLogoAnnounced =
        t.contains("ArrivalLogoAnnounced")
            ? t.getBoolean("ArrivalLogoAnnounced")
            : s.arrivalStep >= FINAL_ARRIVAL_STAGE;
    s.arrivalWelcomeNext = Math.clamp((long) t.getInt("ArrivalWelcomeNext"), 0, WELCOME_LINE_COUNT);
    s.arrivalWelcomeTicks =
        Math.clamp((long) t.getInt("ArrivalWelcomeTicks"), 0, MAX_WELCOME_DELAY_TICKS);
    s.layoutVersion = t.getInt("LayoutVersion");
    s.geometryVersion = t.getInt("GeometryVersion");
    if (t.hasUUID("Receptionist")) s.receptionist = t.getUUID("Receptionist");
    if (t.hasUUID("BriefingOfficer")) s.briefingOfficer = t.getUUID("BriefingOfficer");
    if (t.hasUUID("Founder")) s.founder = t.getUUID("Founder");
    if (t.hasUUID("CoastalTruck")) s.coastalTruck = t.getUUID("CoastalTruck");
    s.powered = t.getBoolean("Powered");
    s.cleared = t.getBoolean("Cleared");
    s.fighting = t.getBoolean("Fighting");
    s.manualAlarm = t.getBoolean("ManualAlarm");
    s.wave = Math.clamp((long) t.getInt("Wave"), 0, FINAL_ENCOUNTER_WAVE);
    s.nextWaveTick = t.getLong("NextWave");
    for (Tag tag : t.getList("Living", Tag.TAG_COMPOUND)) {
      CompoundTag c = (CompoundTag) tag;
      if (!c.hasUUID("ID")) continue;
      s.living.add(c.getUUID("ID"));
    }
    return s;
  }
}
