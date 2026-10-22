package com.timelocked;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;

/** Short offshore glimpse of Pantheris. No terrain edits or permanent entities. */
public final class IntroDeparture {
  // All offsets are relative to the player's saved beach anchor, in world blocks.
  private static final int BOAT_ANCHOR_OFFSET_X_BLOCKS = 38;
  private static final int BOAT_ANCHOR_OFFSET_Y_BLOCKS = -2;
  private static final int BOAT_ANCHOR_OFFSET_Z_BLOCKS = 8;
  private static final int CLEARANCE_FORWARD_BLOCKS = 10;
  private static final int CLEARANCE_SIDE_RADIUS_BLOCKS = 2;
  private static final int HEADROOM_BLOCKS = 3;
  private static final double BLOCK_CENTER_OFFSET = 0.5;
  // The authored Earth beach water is at Y=63, with the cinematic hull just above it.
  private static final int WATER_SURFACE_BLOCK_Y = 63;
  private static final double BOAT_HEIGHT_Y = 63.9;
  private static final double ACTOR_SEAT_OFFSET_BLOCKS = 0.4;
  private static final float DEPARTURE_YAW_DEGREES = -90;
  private static final float LOOK_BACK_YAW_DEGREES = -150;
  private static final int ROWING_START_AGE_TICKS = 35;
  private static final int LOOK_BACK_END_AGE_TICKS = 75;
  private static final double TRAVEL_BLOCKS_PER_TICK = 0.06;

  record Cameo(Boat boat, Pantheris actor) {}

  // Keep direct handles until cleanup: a newly loaded chunk may not yet expose
  // entities through ServerLevel.getEntity while its tracking status advances.
  static final java.util.Map<java.util.UUID, Cameo> live = new java.util.HashMap<>();

  static void spawn(ServerPlayer p, CompoundTag t) {
    cleanup(p, t);
    t.putString("DepartureStatus", "checking_water");
    var l = p.server.overworld();
    BlockPos q = BlockPos.of(t.getLong("Anchor"));
    BlockPos water =
        q.offset(
            BOAT_ANCHOR_OFFSET_X_BLOCKS, BOAT_ANCHOR_OFFSET_Y_BLOCKS, BOAT_ANCHOR_OFFSET_Z_BLOCKS);
    IntroLanding.preload(l, water);
    // Never replace a player-built pier, drain water or spawn the boat on land.
    for (int x = -CLEARANCE_SIDE_RADIUS_BLOCKS; x <= CLEARANCE_FORWARD_BLOCKS; x++)
      for (int z = -CLEARANCE_SIDE_RADIUS_BLOCKS; z <= CLEARANCE_SIDE_RADIUS_BLOCKS; z++) {
        BlockPos a = water.offset(x, 0, z);
        if (!l.getFluidState(a).is(Fluids.WATER)
            || !l.getBlockState(a.above()).getCollisionShape(l, a.above()).isEmpty()
            || !l.getBlockState(a.above(2)).getCollisionShape(l, a.above(2)).isEmpty()
            || !l.getBlockState(a.above(HEADROOM_BLOCKS))
                .getCollisionShape(l, a.above(HEADROOM_BLOCKS))
                .isEmpty()) {
          t.putString(
              "DepartureStatus",
              "blocked_" + a + "_" + l.getBlockState(a) + "_above_" + l.getBlockState(a.above()));
          return;
        }
      }
    Boat b = Content.INTRO_BOAT.get().create(l);
    if (b == null) return;
    b.setPos(water.getX() + BLOCK_CENTER_OFFSET, BOAT_HEIGHT_Y, water.getZ() + BLOCK_CENTER_OFFSET);
    b.setVariant(Boat.Type.DARK_OAK);
    b.setYRot(DEPARTURE_YAW_DEGREES);
    b.setNoGravity(true);
    b.setInvulnerable(true);
    b.setSilent(true);
    b.addTag("timelocked_intro_boat");
    Pantheris a = Content.PANTHERIS.get().create(l);
    if (a == null) {
      t.putString("DepartureStatus", "null_actor");
      return;
    }
    a.moveTo(b.getX(), b.getY() + ACTOR_SEAT_OFFSET_BLOCKS, b.getZ(), DEPARTURE_YAW_DEGREES, 0);
    a.setNoGravity(true);
    a.addTag("timelocked_intro_pantheris");
    if (!l.addFreshEntity(b)) {
      t.putString("DepartureStatus", "boat_rejected");
      return;
    }
    if (!l.addFreshEntity(a)) {
      t.putString("DepartureStatus", "actor_rejected");
      b.discard();
      return;
    }
    if (!a.startRiding(b, true)) {
      t.putString("DepartureStatus", "mount_rejected");
      a.discard();
      b.discard();
      return;
    }
    live.put(p.getUUID(), new Cameo(b, a));
    t.putString("DepartureStatus", "spawned");
    t.putUUID("DepartureBoat", b.getUUID());
    t.putUUID("DepartureActor", a.getUUID());
  }

  static void tick(ServerPlayer p, CompoundTag t, int age) {
    if (!t.hasUUID("DepartureBoat")) return;
    var l = p.server.overworld();
    Cameo cameo = live.get(p.getUUID());
    Entity b = cameo == null ? null : cameo.boat();
    Entity a = cameo == null ? null : cameo.actor();
    if (b == null || a == null || b.isRemoved() || a.isRemoved()) {
      cleanup(p, t);
      return;
    }
    BlockPos q = BlockPos.of(t.getLong("Anchor"));
    double
        x =
            q.getX()
                + (BOAT_ANCHOR_OFFSET_X_BLOCKS + BLOCK_CENTER_OFFSET)
                + Math.max(0, age - ROWING_START_AGE_TICKS) * TRAVEL_BLOCKS_PER_TICK,
        z = q.getZ() + (BOAT_ANCHOR_OFFSET_Z_BLOCKS + BLOCK_CENTER_OFFSET);
    if (!l.getFluidState(BlockPos.containing(x, WATER_SURFACE_BLOCK_Y, z)).is(Fluids.WATER)) {
      cleanup(p, t);
      return;
    }
    if (b instanceof Boat rowing)
      rowing.setPaddleState(age >= ROWING_START_AGE_TICKS, age >= ROWING_START_AGE_TICKS);
    b.setDeltaMovement(Vec3.ZERO);
    b.setPos(x, BOAT_HEIGHT_Y, z);
    b.setYRot(DEPARTURE_YAW_DEGREES);
    b.hasImpulse = true;
    // A restrained look back toward the beach, then turns away and leaves.
    a.setYRot(DEPARTURE_YAW_DEGREES);
    if (a instanceof Pantheris actor) {
      actor.setYBodyRot(DEPARTURE_YAW_DEGREES);
      actor.setYHeadRot(
          age < LOOK_BACK_END_AGE_TICKS ? LOOK_BACK_YAW_DEGREES : DEPARTURE_YAW_DEGREES);
    }
  }

  static void cleanup(ServerPlayer p, CompoundTag t) {
    Cameo c = live.remove(p.getUUID());
    if (c != null) {
      c.actor().discard();
      c.boat().discard();
    }
    for (String key : new String[] {"DepartureActor", "DepartureBoat"}) {
      if (t.hasUUID(key)) {
        Entity a = p.server.overworld().getEntity(t.getUUID(key));
        if (a != null) a.discard();
        t.remove(key);
      }
    }
  }

  private IntroDeparture() {}
}
