package com.timelocked;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class SarcTramEntity extends Minecart {
  public String debugStop = "";
  public int blueprintTarget = -1;
  public int blueprintDirection = 1;
  public boolean stationManaged;
  public int stationTargetLocalZ;
  public BlockPos siteOrigin;
  private boolean disabled;
  private int stuckTicks;
  private double lastZ = Double.NaN;
  private static final EntityDataAccessor<Float> HEALTH =
      SynchedEntityData.defineId(SarcTramEntity.class, EntityDataSerializers.FLOAT);

  public SarcTramEntity(EntityType<? extends SarcTramEntity> t, Level l) {
    super(t, l);
  }

  protected void defineSynchedData(Builder b) {
    super.defineSynchedData(b);
    b.define(HEALTH, 20.0F);
  }

  protected AABB makeBoundingBox() {
    double a = Math.toRadians(this.getYRot());
    double hx = Math.abs(Math.cos(a)) * 2.8 + Math.abs(Math.sin(a)) * 0.95;
    double hz = Math.abs(Math.sin(a)) * 2.8 + Math.abs(Math.cos(a)) * 0.95;
    return new AABB(
        this.getX() - hx,
        this.getY(),
        this.getZ() - hz,
        this.getX() + hx,
        this.getY() + 2.85,
        this.getZ() + hz);
  }

  private void updateTrainBounds() {
    this.setBoundingBox(this.makeBoundingBox());
  }

  public void tick() {
    if (this.level() instanceof ServerLevel sl
        && this.siteOrigin != null
        && BlueprintRooms.active(sl, this.siteOrigin)
        && this.stationManaged
        && !this.disabled) {
      this.baseTick();
      BlueprintTram.tick(this, sl);
      return;
    }
    double oldX = this.getX();
    double oldY = this.getY();
    double oldZ = this.getZ();
    float oldYaw = this.getYRot();
    if (!this.level().isClientSide && this.disabled) {
      this.setDeltaMovement(Vec3.ZERO);
      boolean safe =
          this.getPassengers().stream()
              .noneMatch(
                  e ->
                      e instanceof LivingEntity rider
                          && rider.isAlive()
                          && VehicleSafety.exit(this, rider).isEmpty());
      if (safe) {
        this.ejectPassengers();
        this.discard();
      }
    } else {
      if (!this.level().isClientSide
          && this.stationManaged
          && this.siteOrigin != null
          && this.stationTargetLocalZ != 0
          && this.level() instanceof ServerLevel service
          && (this.stationManaged || SitePlanner.progress(service, this.siteOrigin).powered)) {
        this.setDeltaMovement(
            0.0,
            0.0,
            Math.signum(this.siteOrigin.getZ() + this.stationTargetLocalZ + 0.5 - this.getZ())
                * 0.25);
      }

      boolean empty =
          (this.stationManaged ? this.stationTargetLocalZ == 0 : this.getPassengers().isEmpty())
              || !this.stationManaged
                  && this.siteOrigin != null
                  && this.level() instanceof ServerLevel sl
                  && !SitePlanner.progress(sl, this.siteOrigin).powered;
      double x = this.getX();
      double z = this.getZ();
      if (!this.level().isClientSide && empty) {
        this.setDeltaMovement(Vec3.ZERO);
      }

      super.tick();
      this.updateTrainBounds();
      if (!this.level().isClientSide) {
        Vec3 v = this.getDeltaMovement();
        double hs = Math.sqrt(v.x * v.x + v.z * v.z);
        if (hs > 0.01) {
          VehicleImpact.hit(this, hs * 1.8, new Vec3(v.x, 0.0, v.z));
        }
      }

      if (!this.level().isClientSide
          && (!VehicleSafety.loaded(this.level(), this.getBoundingBox())
              || !this.level().noCollision(this, this.getBoundingBox())
              || this.level().containsAnyLiquid(this.getBoundingBox()))) {
        this.debugStop =
            "loaded="
                + VehicleSafety.loaded(this.level(), this.getBoundingBox())
                + " collision="
                + this.level().noCollision(this, this.getBoundingBox())
                + " liquid="
                + this.level().containsAnyLiquid(this.getBoundingBox())
                + " candidate="
                + this.position()
                + " bounds="
                + this.getBoundingBox()
                + " yaw="
                + this.getYRot();
        this.setYRot(oldYaw);
        this.setPos(oldX, oldY, oldZ);
        this.setDeltaMovement(Vec3.ZERO);
      }

      if (!this.level().isClientSide
          && this.stationManaged
          && this.siteOrigin != null
          && this.stationTargetLocalZ != 0) {
        double dest = this.siteOrigin.getZ() + this.stationTargetLocalZ + 0.5;
        if (Math.abs(this.getZ() - dest) < 0.35 || (oldZ - dest) * (this.getZ() - dest) <= 0.0) {
          this.setPos(this.siteOrigin.getX() + 92.5, this.getY(), dest);
          this.stationTargetLocalZ = 0;
          this.setDeltaMovement(Vec3.ZERO);
        }
      }

      if (!this.level().isClientSide) {
        this.unstick();
      }

      if (!this.level().isClientSide && empty) {
        this.setPos(x, this.getY(), z);
        this.setDeltaMovement(Vec3.ZERO);
      }

      if (!this.level().isClientSide && this.siteOrigin != null) {
        TramClearanceBlock.updateGates(this.level(), this.siteOrigin, this.getZ());
      }
    }
  }

  public InteractionResult interact(Player p, InteractionHand hand) {
    if (p.isSecondaryUseActive()) {
      return InteractionResult.PASS;
    }

    if (this.level().isClientSide) {
      return InteractionResult.SUCCESS;
    }

    if (this.disabled) {
      p.displayClientMessage(Component.literal("Tram damaged. Repair crews only."), false);
      return InteractionResult.CONSUME;
    }

    if (!this.getPassengers().isEmpty()) {
      p.displayClientMessage(Component.literal("Tram occupied. One driver only."), false);
      return InteractionResult.CONSUME;
    }

    if (!p.startRiding(this, true)) {
      p.displayClientMessage(
          Component.literal("Can't board from here. Step onto the platform beside the doors."),
          false);
      return InteractionResult.CONSUME;
    }

    if (this.siteOrigin != null
        && this.level() instanceof ServerLevel sl
        && BlueprintRooms.active(sl, this.siteOrigin)) {
      BlueprintTram.depart(this, p);
      return InteractionResult.CONSUME;
    }
    if (this.siteOrigin != null) {
      double local = this.getZ() - this.siteOrigin.getZ();
      int dest = Math.abs(local - 48.5) < 1.5 ? 170 : (Math.abs(local - 170.5) < 1.5 ? 48 : 0);
      if (dest != 0) {
        this.stationManaged = true;
        this.stationTargetLocalZ = dest;
        this.setDeltaMovement(0.0, 0.0, dest == 170 ? 0.25 : -0.25);
        p.sendSystemMessage(
            Component.literal(
                dest == 170
                    ? "Tram departing for Reactor Station."
                    : "Tram departing for Portal Station."));
        return InteractionResult.CONSUME;
      }
    }

    p.displayClientMessage(Component.literal("Boarded. Sneak to exit when safe."), true);
    return InteractionResult.CONSUME;
  }

  private void unstick() {
    if (this.stationManaged && this.siteOrigin != null && this.stationTargetLocalZ != 0) {
      double dest = this.siteOrigin.getZ() + this.stationTargetLocalZ + 0.5;
      double dist = Math.abs(this.getZ() - dest);
      if (!Double.isNaN(this.lastZ) && !(dist < this.lastZ - 0.05)) {
        if (++this.stuckTicks >= 40) {
          double ox = this.getX();
          double oy = this.getY();
          double oz = this.getZ();
          this.setPos(this.siteOrigin.getX() + 92.5, this.siteOrigin.getY() + 2.1, dest);
          if (!this.level().noCollision(this, this.getBoundingBox())) {
            this.setPos(ox, oy, oz);
            this.stuckTicks = 0;

            for (Entity e : this.getPassengers()) {
              if (e instanceof Player pl) {
                pl.displayClientMessage(
                    Component.literal("Tram blocked at both ends. " + this.debugStop), false);
              }
            }
          } else {
            this.stationTargetLocalZ = 0;
            this.stuckTicks = 0;
            this.lastZ = Double.NaN;
            this.setDeltaMovement(Vec3.ZERO);

            for (Entity e : this.getPassengers()) {
              if (e instanceof Player pl) {
                pl.displayClientMessage(
                    Component.literal("Track obstruction bypassed. Arrived at station."), true);
              }
            }
          }
        }
      } else {
        this.lastZ = dist;
        this.stuckTicks = 0;
      }
    } else {
      this.stuckTicks = 0;
      this.lastZ = Double.NaN;
    }
  }

  public void activateMinecart(int x, int y, int z, boolean powered) {}

  protected boolean canAddPassenger(Entity e) {
    return !this.disabled && e instanceof Player && this.getPassengers().isEmpty();
  }

  protected Item getDropItem() {
    return Items.AIR;
  }

  public Vec3 getPassengerRidingPosition(Entity rider) {
    double a = Math.toRadians(this.getYRot());
    return this.position().add(Math.cos(a) * 1.5, 1.05, Math.sin(a) * 1.5);
  }

  public Vec3 getDismountLocationForPassenger(LivingEntity rider) {
    return VehicleSafety.exit(this, rider).orElse(rider.position());
  }

  public boolean canChangeDimensions(Level from, Level to) {
    return false;
  }

  public boolean hurt(DamageSource source, float amount) {
    if (this.isInvulnerableTo(source) || !Float.isFinite(amount) || amount <= 0.0F) {
      return false;
    }

    if (this.level().isClientSide) {
      return true;
    }

    float value = (Float) this.entityData.get(HEALTH) - amount;
    if (value <= 0.0F) {
      for (Entity e : this.getPassengers()) {
        if (e instanceof LivingEntity rider
            && rider.isAlive()
            && VehicleSafety.exit(this, rider).isEmpty()) {
          this.setDeltaMovement(Vec3.ZERO);
          this.entityData.set(HEALTH, 1.0F);
          this.disabled = true;
          return true;
        }
      }

      this.ejectPassengers();
      this.discard();
    } else {
      this.entityData.set(HEALTH, value);
    }

    return true;
  }

  public void destroy(Item item) {
    this.hurt(this.level().damageSources().generic(), Float.MAX_VALUE);
  }

  protected void destroy(DamageSource source) {
    this.hurt(source, Float.MAX_VALUE);
  }

  protected void readAdditionalSaveData(CompoundTag t) {
    super.readAdditionalSaveData(t);
    this.stationManaged = t.getBoolean("StationManaged");
    this.blueprintTarget = t.contains("BlueprintTarget") ? t.getInt("BlueprintTarget") : -1;
    this.blueprintDirection = t.getInt("BlueprintDirection") == -1 ? -1 : 1;
    this.stationTargetLocalZ = t.getInt("StationTarget");
    if (this.stationTargetLocalZ != 0
        && this.stationTargetLocalZ != 48
        && this.stationTargetLocalZ != 170) {
      this.stationTargetLocalZ = 0;
    }

    this.disabled = t.getBoolean("VehicleDisabled");
    this.siteOrigin = t.contains("SiteOrigin") ? BlockPos.of(t.getLong("SiteOrigin")) : null;
    this.entityData.set(
        HEALTH,
        t.contains("VehicleHealth") && Float.isFinite(t.getFloat("VehicleHealth"))
            ? Mth.clamp(t.getFloat("VehicleHealth"), 1.0F, 20.0F)
            : 20.0F);
    this.setDeltaMovement(Vec3.ZERO);
  }

  protected void addAdditionalSaveData(CompoundTag t) {
    super.addAdditionalSaveData(t);
    t.putBoolean("StationManaged", this.stationManaged);
    t.putInt("BlueprintTarget", this.blueprintTarget);
    t.putInt("BlueprintDirection", this.blueprintDirection);
    t.putInt("StationTarget", this.stationTargetLocalZ);
    t.putBoolean("VehicleDisabled", this.disabled);
    if (this.siteOrigin != null) {
      t.putLong("SiteOrigin", this.siteOrigin.asLong());
    }

    t.putFloat("VehicleHealth", (Float) this.entityData.get(HEALTH));
  }
}
