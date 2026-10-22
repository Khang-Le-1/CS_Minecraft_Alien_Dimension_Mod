package com.timelocked;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/** Safe static landing replicas. All new placements are tracked; no rectangular wipe is used. */
public final class TimeskipperLanding {
  public static final BlockPos HATCH = new BlockPos(8, 1, -2);

  private record Prepared(
      SpacePlanets.Planet planet, BlockPos origin, Map<BlockPos, BlockState> blocks) {}

  private static final Map<MinecraftServer, Prepared> READY = new WeakHashMap<>();

  public static BlockPos origin(SpacePlanets.Planet p, BlockPos pad) {
    return switch (p) {
      case ORBIT -> new BlockPos(-50, 120, 40);
      case BELT -> new BlockPos(-40, 128, 40);
      default -> new BlockPos(-40, pad.getY() + 6, 0);
    };
  }

  public static BlockPos door(BlockPos o) {
    return o.offset(0, -5, 11);
  }

  public static boolean parkedAt(MinecraftServer s, SpacePlanets.Planet p) {
    return p.id.equals(Timeskipper.data(s).parked);
  }

  public static boolean docked(MinecraftServer s) {
    var d = Timeskipper.data(s);
    if (d.parked.isEmpty()) return false;
    var p = SpacePlanets.Planet.byId(d.parked);
    var l = s.getLevel(p.dimension());
    if (l == null) return false;
    var state =
        l.getBlockState(
            BlockPos.of(d.parkedAt)
                .offset(d.parkedGeometryVersion >= 3 ? HATCH : new BlockPos(9, -5, -2)));
    return state.is(ShipInteriorContent.DOCK_PORT.get())
        && state.getValue(DockingPortBlock.PHASE) > 0;
  }

  private static void floor(Map<BlockPos, BlockState> map, ServerLevel l, int x, int y, int z) {
    BlockPos p = new BlockPos(x, y, z);
    BlockState old = l.getBlockState(p);
    // Reuse existing solid station walkway surfaces rather than replacing them.
    map.put(
        p,
        old.isAir() || old.getCollisionShape(l, p).isEmpty()
            ? ShipInteriorContent.DECK.get().defaultBlockState()
            : old);
  }

  private static Map<BlockPos, BlockState> build(ServerLevel l, SpacePlanets.Planet p, BlockPos o) {
    Map<BlockPos, BlockState> map = new LinkedHashMap<>();
    TimeskipperShip.Sink k = (x, y, z, state) -> map.put(new BlockPos(x, y, z), state);
    int X = o.getX(), Y = o.getY(), Z = o.getZ();
    TimeskipperShip.hull(k, X, Y, Z, false);
    TimeskipperBoarding.sidePort(k, X, Y, Z);
    if (p != SpacePlanets.Planet.ORBIT && p != SpacePlanets.Planet.BELT) {
      TimeskipperBoarding.groundRamp(k, X, Y, Z);
    } else {
      // Same floor and port datum at both ends. No dangling staircase or ground gear.
      k.set(X + 6, Y + 3, Z - 2, SpaceContent.LIFE_SUPPORT.get().defaultBlockState());
      if (p == SpacePlanets.Planet.BELT) {
        // Mining-site connector is kept on the pad surface, away from the ship's engine envelope.
        for (int x = X + 32; x <= X + 40; x++)
          for (int z = Z - 3; z <= Z + 3; z++) floor(map, l, x, Y, z);
        k.set(
            X + 32,
            Y + 1,
            Z - 2,
            ShipInteriorContent.DOCK_PORT
                .get()
                .defaultBlockState()
                .setValue(DockingPortBlock.FACING, Direction.WEST));
        k.set(
            X + 32,
            Y + 2,
            Z - 2,
            ShipInteriorContent.DOCK_PORT
                .get()
                .defaultBlockState()
                .setValue(DockingPortBlock.FACING, Direction.WEST)
                .setValue(DockingPortBlock.UPPER, true));
        k.set(X + 36, Y + 3, Z - 2, SpaceContent.LIFE_SUPPORT.get().defaultBlockState());
        // Continuous level return walkway from the connector to the mining pad at z=0.
        for (int z = 0; z <= Z + 3; z++) for (int x = -6; x <= -4; x++) floor(map, l, x, Y, z);
      }
    }
    return map;
  }

  private static boolean unobstructed(ServerLevel l, Map<BlockPos, BlockState> map) {
    for (var e : map.entrySet())
      if (!l.getBlockState(e.getKey()).equals(e.getValue())
          && (!l.getBlockState(e.getKey()).isAir()
              || !l.getFluidState(e.getKey()).isEmpty()
              || l.getBlockEntity(e.getKey()) != null)) return false;
    return true;
  }

  public static boolean prepare(MinecraftServer s, SpacePlanets.Planet p) {
    if (p == SpacePlanets.Planet.EARTH) {
      READY.remove(s);
      return true;
    }
    ServerLevel l = s.getLevel(p.dimension());
    if (l == null) return false;
    BlockPos pad = Timeskipper.landing(l, p);
    if (p == SpacePlanets.Planet.ORBIT && !OrbitalDock083.ready(l)) {
      READY.remove(s);
      return false;
    }
    BlockPos base = origin(p, pad);
    int tries = p == SpacePlanets.Planet.ORBIT || p == SpacePlanets.Planet.BELT ? 1 : 12;
    for (int n = 0; n < tries; n++) {
      BlockPos o = base.offset(-n * 64, 0, (n % 2) * 64);
      if (tries > 1) {
        int top = l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, o.getX(), o.getZ());
        o = o.atY(top + 6);
      }
      if (o.getY() - 6 < l.getMinBuildHeight() || o.getY() + 20 >= l.getMaxBuildHeight()) continue;
      var blocks = build(l, p, o);
      if (unobstructed(l, blocks)) {
        READY.put(s, new Prepared(p, o, blocks));
        return true;
      }
    }
    READY.remove(s);
    return false;
  }

  public static BlockPos arrive(MinecraftServer s, SpacePlanets.Planet p) {
    if (p == SpacePlanets.Planet.EARTH) return null;
    var d = Timeskipper.data(s);
    if (p.id.equals(d.parked)) {
      BlockPos o = BlockPos.of(d.parkedAt);
      if (d.parkedGeometryVersion < 2) return o.offset(9, 1, -2);
      if (d.parkedGeometryVersion == 2)
        return p == SpacePlanets.Planet.ORBIT || p == SpacePlanets.Planet.BELT
            ? o.offset(8, -5, -2)
            : o.offset(0, -13, 18);
      return p == SpacePlanets.Planet.ORBIT || p == SpacePlanets.Planet.BELT
          ? o.offset(7, 1, -2)
          : door(o);
    }
    var prepared = READY.get(s);
    if (prepared == null || prepared.planet != p) {
      if (!prepare(s, p)) return null;
      prepared = READY.get(s);
    }
    var l = s.getLevel(p.dimension());
    if (l == null || !ShipLandingOwnership.get(l).place(l, prepared.blocks)) return null;
    d.parked = p.id;
    d.parkedAt = prepared.origin.asLong();
    d.parkedGeometryVersion = 3;
    d.setDirty();
    READY.remove(s);
    return p == SpacePlanets.Planet.ORBIT || p == SpacePlanets.Planet.BELT
        ? prepared.origin.offset(7, 1, -2)
        : door(prepared.origin);
  }

  /** Per-door outside arrival. Null means this door has no way out here. */
  public static BlockPos exitFor(
      MinecraftServer s,
      SpacePlanets.Planet p,
      ServerLevel l,
      Timeskipper.Door door,
      BlockPos fallback) {
    var d = Timeskipper.data(s);
    if (!p.id.equals(d.parked) || d.parkedGeometryVersion < 3 || door == Timeskipper.Door.ANY)
      return fallback;
    boolean vacuum = p == SpacePlanets.Planet.ORBIT || p == SpacePlanets.Planet.BELT;
    if (vacuum) return door == Timeskipper.Door.RAMP ? null : fallback;
    if (door != Timeskipper.Door.SIDE) return fallback;
    // Ground site: the side hatch drops you on the pad beside the hull, not at the ramp foot.
    BlockPos o = BlockPos.of(d.parkedAt);
    for (int dy = 0; dy <= 12; dy++) {
      BlockPos below = o.offset(10, -dy, -2);
      if (l.getBlockState(below).blocksMotion()) {
        BlockPos feet = below.above();
        if (l.getBlockState(feet).isAir() && l.getBlockState(feet.above()).isAir()) return feet;
        break;
      }
    }
    return fallback;
  }

  public static void depart(MinecraftServer s) {
    var d = Timeskipper.data(s);
    if (d.parked.isEmpty()) return;
    if (docked(s)) throw new IllegalStateException("Release docking clamps before departure");
    var l = s.getLevel(SpacePlanets.Planet.byId(d.parked).dimension());
    if (l != null) ShipLandingOwnership.get(l).cleanup(l);
    // Legacy untracked replicas are deliberately retained. Never infer ownership from a bounding
    // box.
    d.parked = "";
    d.setDirty();
  }

  private TimeskipperLanding() {}
}
