package com.timelocked;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Guarded alignment/clamp/equalize/open cycle for opposed stationary compatible ports. */
public final class DockingProtocol {
  static void message(ServerPlayer p, String text) {
    p.displayClientMessage(Component.literal("[DOCK] " + text), true);
  }

  static DockingPortBlock.Port port(ServerLevel l, BlockPos p) {
    return l.getBlockEntity(p) instanceof DockingPortBlock.Port e ? e : null;
  }

  static void phase(ServerLevel l, BlockPos p, int value) {
    BlockState s = l.getBlockState(p);
    if (!s.is(ShipInteriorContent.DOCK_PORT.get())) return;
    l.setBlock(p, s.setValue(DockingPortBlock.PHASE, value), 3);
    if (l.getBlockState(p.above()).is(ShipInteriorContent.DOCK_PORT.get()))
      l.setBlock(
          p.above(),
          s.setValue(DockingPortBlock.PHASE, value).setValue(DockingPortBlock.UPPER, true),
          3);
  }

  static String owner(BlockPos a, BlockPos b) {
    return "dock:" + Math.min(a.asLong(), b.asLong()) + ":" + Math.max(a.asLong(), b.asLong());
  }

  static Map<BlockPos, BlockState> arm(BlockPos from, Direction d, int distance) {
    // Canonical endpoint makes both ports validate exactly the same arm, including odd-length
    // midpoints.
    BlockPos to = from.relative(d, distance);
    if (!DockingRules.firstEndpoint(from.asLong(), to.asLong())) {
      from = to;
      d = d.getOpposite();
    }
    Map<BlockPos, BlockState> cells = new LinkedHashMap<>();
    Direction side = d.getClockWise();
    for (int i = 1; i < distance; i++)
      for (int a = -1; a <= 1; a++)
        for (int y = -1; y <= 3; y++) {
          BlockPos p = from.relative(d, i).relative(side, a).above(y);
          if (y == -1) cells.put(p, ShipInteriorContent.DECK.get().defaultBlockState());
          else if (y == 3)
            cells.put(
                p,
                (a == 0 && i == distance / 2
                        ? SpaceContent.LIFE_SUPPORT.get()
                        : ShipInteriorContent.CEILING.get())
                    .defaultBlockState());
          else if (a != 0)
            cells.put(
                p,
                (i % 4 == 0 ? ShipInteriorContent.DOCK_FRAME.get() : Blocks.CYAN_STAINED_GLASS)
                    .defaultBlockState());
        }
    return cells;
  }

  static boolean compatible(ServerLevel l, BlockPos a, BlockPos b) {
    if (!l.hasChunkAt(a) || !l.hasChunkAt(b)) return false;
    var x = port(l, a);
    var y = port(l, b);
    return x != null
        && y != null
        && l.getBlockState(a.above()).is(ShipInteriorContent.DOCK_PORT.get())
        && l.getBlockState(b.above()).is(ShipInteriorContent.DOCK_PORT.get())
        && a.getY() == b.getY()
        && l.getBlockState(a).getValue(DockingPortBlock.FACING)
            == l.getBlockState(b).getValue(DockingPortBlock.FACING).getOpposite();
  }

  public static void use(ServerPlayer player, BlockPos at) {
    ServerLevel l = player.serverLevel();
    var p = port(l, at);
    if (p == null) return;
    int stage = p.getBlockState().getValue(DockingPortBlock.PHASE);
    Direction d = p.getBlockState().getValue(DockingPortBlock.FACING);
    if (stage == 0) {
      BlockPos mate = null;
      int distance = 0;
      for (int i = 4; i <= 24; i++) {
        BlockPos q = at.relative(d, i);
        if (!l.hasChunkAt(q)) break;
        if (compatible(l, at, q) && l.getBlockState(q).getValue(DockingPortBlock.PHASE) == 0) {
          mate = q;
          distance = i;
          break;
        }
      }
      if (mate == null) {
        message(player, "No aligned opposing port within 4-24 blocks.");
        return;
      }
      for (BlockPos q : arm(at, d, distance).keySet())
        if (!l.hasChunkAt(q)) {
          message(player, "Docking area chunks unavailable.");
          return;
        }
      // Construction cells must be empty or identical reusable blocks; the walking spine stays
      // empty and dry.
      for (int i = 1; i < distance; i++)
        for (int y = 0; y <= 2; y++)
          if (!l.getBlockState(at.relative(d, i).above(y)).isAir()
              || !l.getFluidState(at.relative(d, i).above(y)).isEmpty()) {
            message(player, "Docking corridor obstructed.");
            return;
          }
      for (var cell : arm(at, d, distance).entrySet()) {
        BlockPos q = cell.getKey();
        if ((!l.getBlockState(q).isAir() && !l.getBlockState(q).equals(cell.getValue()))
            || !l.getFluidState(q).isEmpty()
            || !l.getEntities(
                    (net.minecraft.world.entity.Entity) null, new AABB(q), e -> e.isAlive())
                .isEmpty()) {
          message(player, "Arm deployment area occupied.");
          return;
        }
      }
      p.mate = mate;
      p.distance = distance;
      p.setChanged();
      var other = port(l, mate);
      other.mate = at;
      other.distance = distance;
      other.setChanged();
      phase(l, at, 1);
      phase(l, mate, 1);
      message(player, "Aligned. Use again to clamp and deploy arm.");
      return;
    }
    if (p.mate != null && !l.hasChunkAt(p.mate)) {
      message(player, "Other port chunk unavailable; link remains sealed/locked.");
      return;
    }
    var mate = p.mate;
    var other = mate == null ? null : port(l, mate);
    if (other == null || !compatible(l, at, mate) || !at.equals(other.mate)) {
      phase(l, at, 0);
      p.mate = null;
      p.setChanged();
      message(player, "Link lost. Port resealed.");
      return;
    }
    if (player.isSecondaryUseActive()) {
      if (stage == 4) {
        if (!l.getEntities(
                    (net.minecraft.world.entity.Entity) null,
                    new AABB(at).expandTowards(0, 1, 0).inflate(.05),
                    e -> e.isAlive())
                .isEmpty()
            || !l.getEntities(
                    (net.minecraft.world.entity.Entity) null,
                    new AABB(mate).expandTowards(0, 1, 0).inflate(.05),
                    e -> e.isAlive())
                .isEmpty()) {
          message(player, "Doorway occupied: cannot close.");
          return;
        }
        phase(l, at, 2);
        phase(l, mate, 2);
        message(player, "Both doors closed. Shift-use again to undock.");
        return;
      }
      if (stage == 3) {
        message(player, "Wait for equalization to finish.");
        return;
      }
      for (int i = 1; i < p.distance; i++)
        if (!l.getEntities(
                (net.minecraft.world.entity.Entity) null,
                new AABB(at.relative(d, i)).inflate(.2, 1.5, .2),
                e -> e.isAlive())
            .isEmpty()) {
          message(player, "Undocking blocked: corridor occupied.");
          return;
        }
      if (stage != 1) ShipLandingOwnership.get(l).cleanup(l, owner(at, mate));
      phase(l, at, 0);
      phase(l, mate, 0);
      p.mate = null;
      other.mate = null;
      p.distance = 0;
      other.distance = 0;
      p.setChanged();
      other.setChanged();
      message(player, "Clamps released; arm retracted.");
      return;
    }
    if (stage == 1) {
      // Alignment is not a reservation: recheck the walking spine immediately before construction.
      for (int i = 1; i < p.distance; i++)
        for (int y = 0; y <= 2; y++) {
          BlockPos q = at.relative(d, i).above(y);
          if (!l.hasChunkAt(q) || !l.getBlockState(q).isAir() || !l.getFluidState(q).isEmpty()) {
            message(player, "Transfer corridor changed or obstructed.");
            return;
          }
        }
      for (int i = 1; i < p.distance; i++)
        if (!l.getEntities(
                (net.minecraft.world.entity.Entity) null,
                new AABB(at.relative(d, i)).expandTowards(0, 2, 0),
                e -> e.isAlive())
            .isEmpty()) {
          message(player, "Transfer corridor occupied: cannot deploy.");
          return;
        }
      Map<BlockPos, BlockState> cells = arm(at, d, p.distance);
      for (BlockPos q : cells.keySet())
        if (!l.hasChunkAt(q)) {
          message(player, "Docking area chunks unavailable.");
          return;
        }
      for (var cell : cells.entrySet()) {
        BlockPos q = cell.getKey();
        if ((!l.getBlockState(q).isAir() && !l.getBlockState(q).equals(cell.getValue()))
            || !l.getFluidState(q).isEmpty()
            || !l.getEntities(
                    (net.minecraft.world.entity.Entity) null, new AABB(q), e -> e.isAlive())
                .isEmpty()) {
          message(player, "Arm deployment blocked.");
          return;
        }
      }
      if (!ShipLandingOwnership.get(l).place(l, cells, owner(at, mate))) {
        message(player, "Arm deployment area changed.");
        return;
      }
      phase(l, at, 2);
      phase(l, mate, 2);
      message(player, "Clamped and sealed. Use again to equalize.");
      return;
    }
    if (stage == 2) {
      if (SpacePlanets.vacuum(l.dimension())
          && (!SpaceEvents.nearLifeSupport(l, at) || !SpaceEvents.nearLifeSupport(l, mate))) {
        message(player, "Both endpoint cabins need life support before equalizing.");
        return;
      }
      phase(l, at, 3);
      phase(l, mate, 3);
      l.scheduleTick(at, ShipInteriorContent.DOCK_PORT.get(), 60);
      l.scheduleTick(mate, ShipInteriorContent.DOCK_PORT.get(), 60);
      message(player, "Equalizing sealed transfer corridor...");
      return;
    }
    message(
        player,
        stage == 3 ? "Equalization in progress." : "Dock open. Shift-use closes both doors.");
  }

  public static void tick(ServerLevel l, BlockPos at) {
    var p = port(l, at);
    if (p == null) return;
    int stage = p.getBlockState().getValue(DockingPortBlock.PHASE);
    if (p.mate != null && !l.hasChunkAt(p.mate)) {
      l.scheduleTick(at, ShipInteriorContent.DOCK_PORT.get(), 20);
      return;
    }
    var other = p.mate == null ? null : port(l, p.mate);
    if (other == null || !at.equals(other.mate) || !compatible(l, at, p.mate)) {
      phase(l, at, 0);
      p.mate = null;
      p.setChanged();
      return;
    }
    if (stage == 3) {
      Direction d = p.getBlockState().getValue(DockingPortBlock.FACING);
      boolean sealed =
          arm(at, d, p.distance).entrySet().stream()
              .allMatch(
                  e ->
                      l.hasChunkAt(e.getKey()) && l.getBlockState(e.getKey()).equals(e.getValue()));
      if (!sealed) {
        phase(l, at, 2);
        phase(l, p.mate, 2);
        return;
      }
      boolean supported =
          !SpacePlanets.vacuum(l.dimension())
              || (SpaceEvents.nearLifeSupport(l, at) && SpaceEvents.nearLifeSupport(l, p.mate));
      int next = DockingRules.afterEqualization(compatible(l, at, p.mate), sealed, supported);
      phase(l, at, next);
      phase(l, p.mate, next);
    }
    if (stage == 3 || stage == 4) l.scheduleTick(at, ShipInteriorContent.DOCK_PORT.get(), 20);
  }

  private DockingProtocol() {}
}
