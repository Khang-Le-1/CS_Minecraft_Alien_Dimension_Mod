package com.timelocked;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Authoritative, skippable jump sequence. One shared sequence for everyone aboard the TIMESKIPPER;
 * the charge is captured at authorization and spent exactly once at the commit (or when the
 * authorized caller skips).
 */
public final class JumpSequence {
  private static Active active;
  private static int tokenCounter = 1;
  private static final Map<UUID, Hold> holds = new HashMap<>();

  public static final class Active {
    public final int token;
    public final UUID caller;
    public final SpacePlanets.Planet from;
    public final SpacePlanets.Planet to;
    public final int cost;
    public long started;
    public boolean committed;

    Active(
        int token,
        UUID caller,
        SpacePlanets.Planet from,
        SpacePlanets.Planet to,
        int cost,
        long started) {
      this.token = token;
      this.caller = caller;
      this.from = from;
      this.to = to;
      this.cost = cost;
      this.started = started;
    }
  }

  private static final class Hold {
    final ServerLevel level;
    final Vec3 pos;
    final float yaw;
    final float pitch;

    Hold(ServerLevel level, Vec3 pos, float yaw, float pitch) {
      this.level = level;
      this.pos = pos;
      this.yaw = yaw;
      this.pitch = pitch;
    }
  }

  public static void register() {
    NeoForge.EVENT_BUS.addListener(JumpSequence::tick);
  }

  public static Active active(MinecraftServer server) {
    return active;
  }

  public static boolean holding(Player p) {
    return active != null
        && p != null
        && holds.containsKey(p.getUUID())
        && p.level().dimension() == SpacePlanets.SHIP;
  }

  public static void request(Player p, int targetIndex) {
    if (!(p instanceof ServerPlayer sp)) return;
    if (holding(sp) && active != null) return;
    start(sp, SpacePlanets.Planet.byIndex(targetIndex));
  }

  public static boolean start(ServerPlayer p, SpacePlanets.Planet to) {
    MinecraftServer s = p.server;
    Timeskipper.Data d = Timeskipper.data(s);
    SpacePlanets.Planet from = d.planet();

    if (!Timeskipper.atConsole(p) && !p.hasPermissions(2)) return false;
    if (active != null) return false;
    if (TimeskipperLanding.docked(s)) {
      Timeskipper.say(
          p, "[NAV] Close docking doors and release clamps before jumping.", ChatFormatting.RED);
      return false;
    }
    if (to == from) {
      p.displayClientMessage(Component.literal("[NAV] We are already at " + to.title + "."), true);
      return false;
    }
    if (s.getLevel(to.dimension()) == null) return false;
    int cost = p.isCreative() ? 0 : to.cost;
    if (cost > 0 && d.charges < cost) {
      Timeskipper.say(
          p,
          "[NAV] Not enough drive charges: need "
              + to.cost
              + ", have "
              + d.charges
              + ". Feed the chronotite drive.",
          ChatFormatting.RED);
      return false;
    }

    if (!TimeskipperLanding.prepare(s, to)) {
      Timeskipper.say(
          p,
          "[NAV] Landing zone obstructed. No blocks cleared and no charges spent.",
          ChatFormatting.RED);
      return false;
    }
    active = new Active(tokenCounter++, p.getUUID(), from, to, cost, s.overworld().getGameTime());
    ServerLevel ship = s.getLevel(SpacePlanets.SHIP);
    if (ship != null) {
      for (ServerPlayer aboard : ship.players()) {
        holds.put(
            aboard.getUUID(),
            new Hold(ship, aboard.position(), aboard.getYRot(), aboard.getXRot()));
      }
      var token = active.token;
      var frm = from.ordinal();
      var dst = to.ordinal();
      long startedAt = active.started;
      for (ServerPlayer aboard : ship.players()) {
        PacketDistributor.sendToPlayer(
            aboard,
            new JumpNetworking.Start(token, frm, dst, startedAt),
            new CustomPacketPayload[0]);
      }
    }
    return true;
  }

  public static boolean skip(ServerPlayer p, int token) {
    if (active == null || token != active.token) return false;
    MinecraftServer s = p.server;
    ServerPlayer caller = p.server.getPlayerList().getPlayer(active.caller);
    if (caller == null || (!p.getUUID().equals(active.caller) && !p.hasPermissions(2)))
      return false;
    commit(s);
    finish(s, true);
    return true;
  }

  public static void finish(MinecraftServer s, boolean success) {
    clearHolds(s);
    Active a = active;
    active = null;
    if (a == null) return;

    ServerLevel ship = s.getLevel(SpacePlanets.SHIP);
    if (ship != null) {
      for (ServerPlayer aboard : ship.players()) {
        PacketDistributor.sendToPlayer(
            aboard, new JumpNetworking.Stop(a.token, success), new CustomPacketPayload[0]);
      }
    }
  }

  private static void tick(ServerTickEvent.Post e) {
    advance(e.getServer());
  }

  private static void advance(MinecraftServer s) {
    if (active == null) return;
    Active a = active;

    // Abort if the authorized caller is gone or died during the pre-commit beats.
    ServerPlayer caller = s.getPlayerList().getPlayer(a.caller);
    if (caller == null) {
      finish(s, false);
      return;
    }
    long now = s.overworld().getGameTime();
    if (caller.isDeadOrDying() && now - a.started >= 20) {
      finish(s, false);
      return;
    }

    reassertHolds(s);

    long age = now - a.started;
    ServerLevel ship = s.getLevel(SpacePlanets.SHIP);
    BlockPos drive = Timeskipper.ORIGIN.offset(TimeskipperShip.DRIVE);

    if (age >= JumpTimeline.DRIVE
        && age < JumpTimeline.COMMIT
        && ship != null
        && (age - JumpTimeline.DRIVE) % 3 == 0) {
      ship.sendParticles(
          ParticleTypes.REVERSE_PORTAL,
          drive.getX() + 0.5,
          drive.getY() + 0.5,
          drive.getZ() + 0.5,
          60,
          1.0,
          1.1,
          1.0,
          0.5);
      ship.sendParticles(
          ParticleTypes.END_ROD,
          drive.getX() + 0.5,
          drive.getY() + 0.5,
          drive.getZ() + 0.5,
          24,
          0.3,
          0.3,
          0.3,
          0.3);
    }

    if (age >= JumpTimeline.COMMIT) {
      if (!a.committed) {
        if (!revalidated(s, a)) {
          finish(s, false);
          return;
        }
        commit(s);
      }
      if (age >= JumpTimeline.END) {
        finish(s, true);
      }
    }
  }

  /** Test hook: run the sequence as if the server had just reached this tick. */
  public static void tickAt(MinecraftServer s, long tick) {
    if (active == null) return;
    long realStarted = active.started;
    long now = s.overworld().getGameTime();
    active.started = now - tick;
    advance(s);
    if (active != null) active.started = realStarted;
  }

  private static boolean revalidated(MinecraftServer s, Active a) {
    Timeskipper.Data d = Timeskipper.data(s);
    if (a.cost > 0 && d.charges < a.cost) return false;
    if (TimeskipperLanding.docked(s)) return false;
    return s.getLevel(a.to.dimension()) != null;
  }

  private static void commit(MinecraftServer s) {
    Active a = active;
    if (a == null || a.committed) return;
    Timeskipper.Data d = Timeskipper.data(s);
    if (!revalidated(s, a)) {
      finish(s, false);
      return;
    }

    String parkedBefore = d.parked;
    long oldAt = d.parkedAt;
    int oldVersion = d.parkedGeometryVersion;
    // Validate and place the destination first. Failure leaves the old replica and charges intact.
    if (a.to != SpacePlanets.Planet.EARTH && TimeskipperLanding.arrive(s, a.to) == null) {
      finish(s, false);
      return;
    }
    String parkedNext = d.parked;
    long nextAt = d.parkedAt;
    int nextVersion = d.parkedGeometryVersion;
    d.parked = parkedBefore;
    d.parkedAt = oldAt;
    d.parkedGeometryVersion = oldVersion;
    TimeskipperLanding.depart(s);
    if (a.to != SpacePlanets.Planet.EARTH) {
      d.parked = parkedNext;
      d.parkedAt = nextAt;
      d.parkedGeometryVersion = nextVersion;
    }

    if (a.cost > 0) d.charges -= a.cost;
    d.location = a.to.ordinal();
    d.setDirty();
    a.committed = true;

    long skip = 1500L * a.to.cost;
    s.overworld().setDayTime(s.overworld().getDayTime() + skip);

    ServerLevel ship = s.getLevel(SpacePlanets.SHIP);
    if (ship != null) {
      TimeskipperShip.updateScreens(ship, d);
      for (ServerPlayer aboard : ship.players()) {
        Timeskipper.say(
            aboard,
            "[TIMESKIPPER] Arrived: "
                + a.to.title
                + ". Relativity check: about "
                + Math.round(skip / 1000.0)
                + "h passed on Earth. Use the airlock (starboard, midship) to disembark.",
            ChatFormatting.AQUA);
        Achievements.grant(aboard, "first_jump");
      }
    }
    PacketDistributor.sendToAllPlayers(
        new SpaceNetworking.ShipLocation(d.location), new CustomPacketPayload[0]);
  }

  private static void reassertHolds(MinecraftServer s) {
    for (Map.Entry<UUID, Hold> e : holds.entrySet()) {
      ServerPlayer p = s.getPlayerList().getPlayer(e.getKey());
      Hold h = e.getValue();
      if (p == null || !p.level().dimension().equals(SpacePlanets.SHIP) || p.isDeadOrDying())
        continue;
      if (p.position().distanceToSqr(h.pos) > 1.0E-4) {
        p.teleportTo(h.level, h.pos.x, h.pos.y, h.pos.z, h.yaw, h.pitch);
      }
    }
  }

  private static void clearHolds(MinecraftServer s) {
    holds.clear();
  }

  private JumpSequence() {}
}
