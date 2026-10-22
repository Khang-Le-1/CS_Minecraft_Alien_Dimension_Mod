package com.timelocked;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Fixed guideway with real bends and four stops; obstruction checks never teleport riders. */
public final class DrawingTram {
  public static final List<BlockPos> ROUTE = route();
  public static final int[] STOPS = {0, 102, 175, 214};
  public static final String[] NAMES = {"Research Hub", "Portal Fluid", "Storage", "Reactor"};

  private static List<BlockPos> route() {
    List<BlockPos> p = new ArrayList<>();
    for (int z = 145; z >= 117; z--) p.add(new BlockPos(165, 2, z + 128));
    for (int x = 166; x <= 328; x++) p.add(new BlockPos(x, 2, 245));
    for (int z = 118; z <= 140; z++) p.add(new BlockPos(328, 2, z + 128));
    return List.copyOf(p);
  }

  private static Vec3 point(BlockPos o, int i) {
    var p = o.offset(ROUTE.get(i));
    return new Vec3(p.getX() + .5, p.getY() + .1, p.getZ() + .5);
  }

  private static int closest(SarcTramEntity c) {
    int best = 0;
    double d = Double.MAX_VALUE;
    for (int i = 0; i < ROUTE.size(); i++) {
      double q = point(c.siteOrigin, i).distanceToSqr(c.position());
      if (q < d) {
        d = q;
        best = i;
      }
    }
    return best;
  }

  public static InteractionResult terminal(ServerLevel l, BlockPos o, BlockPos terminal, Player p) {
    int stop = 0;
    double near = Double.MAX_VALUE;
    for (int i = 0; i < STOPS.length; i++) {
      double d = point(o, STOPS[i]).distanceToSqr(Vec3.atCenterOf(terminal));
      if (d < near) {
        near = d;
        stop = i;
      }
    }
    var carts =
        l.getEntitiesOfClass(
            SarcTramEntity.class,
            new AABB(o.getX(), o.getY(), o.getZ(), o.getX() + 480, o.getY() + 43, o.getZ() + 384),
            c -> o.equals(c.siteOrigin) && c.stationManaged);
    SarcTramEntity cart = carts.isEmpty() ? null : carts.getFirst();
    if (cart != null && (!cart.getPassengers().isEmpty() || cart.blueprintTarget >= 0)) {
      if (p.getVehicle() == cart && cart.blueprintTarget < 0) depart(cart, p);
      else p.sendSystemMessage(Component.literal("Tram in use. Wait for it to stop and unload."));
      return InteractionResult.CONSUME;
    }
    boolean created = cart == null;
    if (created) {
      cart = Content.TRAM_VEHICLE.get().create(l);
      if (cart == null) return InteractionResult.FAIL;
      cart.siteOrigin = o;
      cart.stationManaged = true;
    }
    var at = point(o, STOPS[stop]);
    Vec3 old = cart.position();
    float yaw = cart.getYRot();
    cart.setYRot(stop == 0 || stop == 3 ? 90 : 0);
    cart.setPos(at.x, at.y, at.z);
    if (!VehicleSafety.loaded(l, cart.getBoundingBox())
        || !l.noCollision(cart, cart.getBoundingBox())
        || l.containsAnyLiquid(cart.getBoundingBox())) {
      cart.setYRot(yaw);
      cart.setPos(old.x, old.y, old.z);
      p.sendSystemMessage(Component.literal("Station obstructed or unloaded. Call refused."));
      return InteractionResult.CONSUME;
    }
    cart.blueprintTarget = -1;
    if (created && !l.addFreshEntity(cart)) return InteractionResult.FAIL;
    p.sendSystemMessage(
        Component.literal(
            "Tram called to "
                + NAMES[stop]
                + ". Board to depart; each terminal continues to the next stop."));
    return InteractionResult.CONSUME;
  }

  public static void depart(SarcTramEntity c, Player p) {
    int at = closest(c), s = -1;
    for (int i = 0; i < STOPS.length; i++) if (Math.abs(STOPS[i] - at) <= 1) s = i;
    if (s < 0) {
      p.sendSystemMessage(Component.literal("Train is not docked at a station."));
      return;
    }
    if (s == 0) c.blueprintDirection = 1;
    if (s == 3) c.blueprintDirection = -1;
    int dest = s + c.blueprintDirection;
    c.blueprintTarget = STOPS[dest];
    c.stationManaged = true;
    p.sendSystemMessage(
        Component.literal("Departing for " + NAMES[dest] + ". Sneak only when safely docked."));
  }

  public static void tick(SarcTramEntity c, ServerLevel l) {
    if (c.blueprintTarget < 0 || c.blueprintTarget >= ROUTE.size()) {
      c.blueprintTarget = -1;
      c.setDeltaMovement(Vec3.ZERO);
      return;
    }
    int at = closest(c), dir = Integer.compare(c.blueprintTarget, at);
    if (dir == 0 && point(c.siteOrigin, at).distanceToSqr(c.position()) < .025) {
      c.blueprintTarget = -1;
      c.setDeltaMovement(Vec3.ZERO);
      for (var e : c.getPassengers())
        if (e instanceof Player p)
          p.displayClientMessage(
              Component.literal("Docked. Sneak to exit, or use the station terminal to continue."),
              false);
      return;
    }
    int next = dir == 0 ? at : at + dir;
    Vec3 target = point(c.siteOrigin, next), delta = target.subtract(c.position());
    double d = delta.length();
    Vec3 step = d <= .22 ? delta : delta.scale(.22 / d);
    float oldYaw = c.getYRot();
    float yaw = Math.abs(delta.x) > .01 ? 0 : 90;
    c.setYRot(yaw);
    c.setPos(c.getX(), c.getY(), c.getZ());
    var candidate = c.getBoundingBox().move(step);
    var sweep = c.getBoundingBox().minmax(candidate);
    if (!VehicleSafety.loaded(l, sweep) || !l.noCollision(c, sweep) || l.containsAnyLiquid(sweep)) {
      c.setYRot(oldYaw);
      c.setPos(c.getX(), c.getY(), c.getZ());
      c.setDeltaMovement(Vec3.ZERO);
      if (c.tickCount % 40 == 0)
        for (var e : c.getPassengers())
          if (e instanceof Player p)
            p.displayClientMessage(
                Component.literal(
                    "Tram stopped: obstruction or unloaded track ahead. No bypass teleport."),
                true);
      return;
    }
    c.setPos(c.getX() + step.x, c.getY() + step.y, c.getZ() + step.z);
    c.setDeltaMovement(step);
  }

  private DrawingTram() {}
}
