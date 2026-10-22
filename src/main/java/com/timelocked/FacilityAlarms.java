package com.timelocked;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Restarts old decorative fixtures safely and links the six stock alarms to the actual encounter
 * state.
 */
@EventBusSubscriber(modid = "timelocked")
public final class FacilityAlarms {
  private static final int SIREN_REPEAT_INTERVAL_TICKS = 40;
  private static final float ALARM_VOLUME = 1.5F;
  private static final int FIXTURE_CHECK_INTERVAL_TICKS = 20;
  private static final java.util.Map<ServerLevel, java.util.Map<BlockPos, Long>> LAST_SOUND =
      new java.util.WeakHashMap<>();

  public static void audio(ServerLevel l, BlockPos p, boolean active) {
    audio(l, p, active, false);
  }

  /**
   * danger=true plays the Facility Danger Alarm clip, spaced so it never overlaps itself.
   * danger=false is the old short siren wail.
   */
  public static void audio(ServerLevel l, BlockPos p, boolean active, boolean danger) {
    var times = LAST_SOUND.computeIfAbsent(l, k -> new java.util.HashMap<>());
    int period = danger ? AlarmRules.DANGER_PERIOD : SIREN_REPEAT_INTERVAL_TICKS;
    if (!active) {
      Long last = times.get(p);
      if (last != null && AlarmRules.soundDue(l.getGameTime(), last, period)) times.remove(p);
      return;
    }
    long now = l.getGameTime();
    Long before = times.get(p);
    if (before == null || AlarmRules.soundDue(now, before, period)) {
      l.playSound(
          null,
          p,
          danger ? AlarmSounds.FACILITY_DANGER.get() : AlarmSounds.SIREN.get(),
          net.minecraft.sounds.SoundSource.BLOCKS,
          ALARM_VOLUME,
          1F);
      times.put(p.immutable(), now);
    }
  }

  private static final int[][] FIXTURES = {
    {102, 6, 179}, {119, 6, 179}, {84, 6, 162}, {84, 6, 176}, {84, 6, 40}, {84, 6, 54}
  };

  private static BlockPos site(ServerLevel l, BlockPos p) {
    if (!l.dimension().equals(Level.OVERWORLD)) return null;
    for (long key : FacilityState.get(l.getServer()).sites.keySet()) {
      BlockPos o = BlockPos.of(key), q = p.subtract(o);
      var progress = FacilityState.get(l.getServer()).progress.get(key);
      if (progress != null
          && progress.layoutVersion >= 2
          && q.getX() >= 0
          && q.getX() < BlueprintRooms.width(l, o)
          && q.getY() >= 0
          && q.getY() < SiteLayout.HEIGHT
          && q.getZ() >= 0
          && q.getZ() < BlueprintRooms.depth(l, o)) return o;
    }
    return null;
  }

  private static List<BlockPos> fixtures(ServerLevel l, BlockPos o) {
    List<BlockPos> out = new ArrayList<>();
    if (BlueprintRooms.drawing(l, o)) {
      for (int[] v : DrawingAlarmIndex.CELLS) out.add(o.offset(v[0], v[1], v[2]));
    } else for (int[] v : FIXTURES) out.add(BlueprintRooms.at(l, o, v[0], v[1], v[2]));
    return out;
  }

  private static List<BlockPos> group(ServerLevel l, BlockPos p) {
    var station = StationAlarms.group(l, p);
    if (!station.isEmpty()) return station;
    List<BlockPos> out = new ArrayList<>();
    BlockPos o = site(l, p);
    if (o != null)
      for (var q : fixtures(l, o)) {
        if (l.hasChunkAt(q) && l.getBlockState(q).getBlock() instanceof AlarmBlock) out.add(q);
      }
    if (!out.contains(p)) out.add(p);
    return out;
  }

  public static boolean manualState(ServerLevel l, BlockPos p) {
    var o = site(l, p);
    return o == null
        ? l.getBlockState(p).getValue(AlarmBlock.MANUAL)
        : FacilityState.get(l.getServer()).progress.get(o.asLong()).manualAlarm;
  }

  public static boolean encounter(ServerLevel l, BlockPos p) {
    var o = site(l, p);
    if (o == null) return false;
    var s = FacilityState.get(l.getServer()).progress.get(o.asLong());
    return s != null && s.fighting && !s.cleared;
  }

  public static boolean redstone(ServerLevel l, BlockPos p) {
    for (var q : group(l, p)) if (l.hasNeighborSignal(q)) return true;
    return false;
  }

  public static void manual(ServerLevel l, BlockPos p, boolean on) {
    var o = site(l, p);
    if (o != null) {
      var data = FacilityState.get(l.getServer());
      data.progress.get(o.asLong()).manualAlarm = on;
      data.setDirty();
    }
    for (var q : group(l, p)) {
      var s = l.getBlockState(q);
      if (s.getBlock() instanceof AlarmBlock b) {
        l.setBlock(q, s.setValue(AlarmBlock.MANUAL, on), 3);
        l.scheduleTick(q, b, 1);
      }
    }
  }

  @SubscribeEvent
  public static void tick(ServerTickEvent.Post e) {
    var l = e.getServer().overworld();
    if (Math.floorMod(l.getGameTime(), FIXTURE_CHECK_INTERVAL_TICKS) != 0) return;
    for (long key : FacilityState.get(e.getServer()).sites.keySet()) {
      var o = BlockPos.of(key);
      for (var p : fixtures(l, o)) {
        if (l.hasChunkAt(p)
            && l.getBlockState(p).getBlock() instanceof AlarmBlock b
            && !l.getBlockTicks().hasScheduledTick(p, b)) l.scheduleTick(p, b, 1);
      }
    }
  }

  private FacilityAlarms() {}
}
