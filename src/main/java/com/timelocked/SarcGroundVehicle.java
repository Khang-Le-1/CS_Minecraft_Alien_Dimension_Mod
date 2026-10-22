package com.timelocked;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class SarcGroundVehicle extends Entity {
  public static final EntityDataAccessor<Float> HEALTH =
      SynchedEntityData.defineId(SarcGroundVehicle.class, EntityDataSerializers.FLOAT);
  public static final EntityDataAccessor<Float> SPEED =
      SynchedEntityData.defineId(SarcGroundVehicle.class, EntityDataSerializers.FLOAT);
  public static final EntityDataAccessor<Float> STEER =
      SynchedEntityData.defineId(SarcGroundVehicle.class, EntityDataSerializers.FLOAT);
  public static final EntityDataAccessor<Boolean> ENGINE_RUNNING =
      SynchedEntityData.defineId(SarcGroundVehicle.class, EntityDataSerializers.BOOLEAN);
  public float wheelAngle;
  private float throttle;
  private float steering;
  private float speed;
  private boolean brake;
  private boolean disabled;
  private int lastInput = -100;
  private int lastAccepted = -1;
  private int lerpSteps;
  private double lx;
  private double ly;
  private double lz;
  private float lyaw;
  private float lpitch;

  public SarcGroundVehicle(EntityType<? extends SarcGroundVehicle> type, Level l) {
    super(type, l);
  }

  public boolean truck() {
    return this.getType() == Content.TRUCK.get();
  }

  public float maximumSpeed() {
    return this.truck() ? 0.6F : 0.65F;
  }

  public float health() {
    return (Float) this.entityData.get(HEALTH);
  }

  public float driveSpeed() {
    return (Float) this.entityData.get(SPEED);
  }

  protected void defineSynchedData(Builder b) {
    b.define(HEALTH, 40.0F).define(SPEED, 0.0F).define(STEER, 0.0F).define(ENGINE_RUNNING, false);
  }

  public boolean engineRunning() {
    return this.truck() && this.isAlive() && this.entityData.get(ENGINE_RUNNING);
  }

  @Override
  protected void addPassenger(Entity passenger) {
    boolean left =
        TruckSounds.leftSide(
            this.getYRot(), passenger.getX() - this.getX(), passenger.getZ() - this.getZ());
    super.addPassenger(passenger);
    if (!this.level().isClientSide && this.truck() && passenger instanceof Player) {
      this.entityData.set(ENGINE_RUNNING, !this.disabled);
      TruckSounds.door(this, true, left);
    }
  }

  @Override
  protected void removePassenger(Entity passenger) {
    boolean exiting = this.getPassengers().contains(passenger);
    Vec3 exit =
        passenger instanceof LivingEntity rider
            ? VehicleSafety.exit(this, rider).orElse(passenger.position())
            : passenger.position();
    boolean left = TruckSounds.leftSide(this.getYRot(), exit.x - this.getX(), exit.z - this.getZ());
    super.removePassenger(passenger);
    if (!this.level().isClientSide && this.truck() && exiting && passenger instanceof Player) {
      this.entityData.set(ENGINE_RUNNING, false);
      if (!this.isRemoved() && passenger.isAlive() && !passenger.isRemoved())
        TruckSounds.door(this, false, left);
    }
  }

  public boolean acceptInput(Player p, float forward, float left, boolean stop) {
    if (!this.disabled
        && !this.level().isClientSide
        && p.getVehicle() == this
        && this.getFirstPassenger() == p
        && p.isAlive()
        && !p.isSpectator()
        && Float.isFinite(forward)
        && Float.isFinite(left)
        && !(Math.abs(forward) > 1.0F)
        && !(Math.abs(left) > 1.0F)
        && this.lastAccepted != this.tickCount) {
      this.lastAccepted = this.tickCount;
      this.lastInput = this.tickCount;
      this.throttle = forward;
      this.steering = left;
      this.brake = stop;
      return true;
    } else {
      return false;
    }
  }

  public void stopControls() {
    this.throttle = this.steering = this.speed = 0.0F;
    this.brake = true;
    this.lastInput = -100;
    this.setDeltaMovement(0.0, this.getDeltaMovement().y, 0.0);
    this.entityData.set(SPEED, 0.0F);
    this.entityData.set(STEER, 0.0F);
  }

  public void tick() {
    super.tick();
    if (this.level().isClientSide) {
      this.wheelAngle = this.wheelAngle + this.driveSpeed() / (this.truck() ? 0.375F : 0.5F);
      if (this.lerpSteps > 0) {
        this.lerpPositionAndRotationStep(
            this.lerpSteps, this.lx, this.ly, this.lz, this.lyaw, this.lpitch);
        this.lerpSteps--;
      }
    } else if (this.disabled) {
      this.entityData.set(ENGINE_RUNNING, false);
      this.stopControls();
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
      boolean valid =
          this.getFirstPassenger() instanceof Player p
              && p.isAlive()
              && !p.isRemoved()
              && p.getVehicle() == this;
      this.entityData.set(ENGINE_RUNNING, this.truck() && valid);
      boolean fresh = valid && this.tickCount - this.lastInput <= 5;
      if (!fresh) {
        this.throttle = this.steering = 0.0F;
        this.brake = true;
      }

      if (!valid) {
        this.stopControls();
      }

      float target =
          this.brake
              ? 0.0F
              : this.throttle
                  * (this.throttle < 0.0F ? (this.truck() ? 0.2F : 0.22F) : this.maximumSpeed());
      this.speed = Mth.approach(this.speed, target, this.brake ? 0.08F : 0.028F);
      if (Math.abs(this.speed) > 0.005F) {
        this.setYRot(
            this.getYRot()
                - this.steering * (this.truck() ? 2.2F : 3.0F) * Math.signum(this.speed));
      }

      double radians = this.getYRot() * Math.PI / 180.0;
      Vec3 proposed =
          new Vec3(
              -Math.sin(radians) * this.speed,
              Math.max(-1.5, (this.getDeltaMovement().y - 0.08) * 0.98),
              Math.cos(radians) * this.speed);
      AABB swept =
          this.getBoundingBox()
              .expandTowards(proposed)
              .expandTowards(0.0, this.onGround() ? this.maxUpStep() : 0.0, 0.0)
              .inflate(0.01);
      if (!VehicleSafety.loaded(this.level(), swept) || this.level().containsAnyLiquid(swept)) {
        this.stopControls();
        proposed = Vec3.ZERO;
      }

      this.setDeltaMovement(proposed);
      this.move(MoverType.SELF, proposed);
      VehicleImpact.hit(
          this,
          this.speed * (this.truck() ? 1.25 : 1.0),
          new Vec3(-Math.sin(radians), 0.0, Math.cos(radians)));
      if (this.horizontalCollision) {
        this.speed = 0.0F;
      }

      if (this.verticalCollision) {
        this.setDeltaMovement(this.getDeltaMovement().x, 0.0, this.getDeltaMovement().z);
      }

      this.hasImpulse = true;
      this.entityData.set(SPEED, this.speed);
      this.entityData.set(STEER, this.steering);
      if (!this.truck() && Math.abs(this.speed) > 0.025F && this.tickCount % 60 == 0) {
        this.level()
            .playSound(
                null,
                this.blockPosition(),
                SoundEvents.MINECART_RIDING,
                SoundSource.NEUTRAL,
                0.08F,
                0.7F + Math.abs(this.speed));
      }
    }
  }

  public InteractionResult interact(Player p, InteractionHand hand) {
    if (p.isShiftKeyDown()) {
      return InteractionResult.PASS;
    } else if (this.level().isClientSide) {
      return InteractionResult.SUCCESS;
    } else if (!this.disabled && this.getPassengers().isEmpty()) {
      return p.startRiding(this) ? InteractionResult.CONSUME : InteractionResult.FAIL;
    } else {
      return InteractionResult.FAIL;
    }
  }

  protected boolean canAddPassenger(Entity e) {
    return !this.disabled && e instanceof Player && this.getPassengers().isEmpty();
  }

  public LivingEntity getControllingPassenger() {
    return null;
  }

  public boolean isControlledByLocalInstance() {
    return !this.level().isClientSide;
  }

  public Vec3 getPassengerRidingPosition(Entity rider) {
    double a = this.getYRot() * Math.PI / 180.0;
    double seat = this.truck() ? 0.15 : 0.5;
    return this.position()
        .add(-Math.sin(a) * seat, this.truck() ? 0.375 : 0.5625, Math.cos(a) * seat)
        .add(rider.getVehicleAttachmentPoint(this));
  }

  public Vec3 getDismountLocationForPassenger(LivingEntity p) {
    return VehicleSafety.exit(this, p).orElse(p.position());
  }

  public boolean canBeCollidedWith() {
    return !this.isRemoved();
  }

  public boolean canCollideWith(Entity e) {
    return (e.canBeCollidedWith() || e.isPushable()) && !this.isPassengerOfSameVehicle(e);
  }

  public boolean isPickable() {
    return true;
  }

  public boolean isPushable() {
    return false;
  }

  public float maxUpStep() {
    return 1.0F;
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

    float remaining = this.health() - amount;
    if (remaining <= 0.0F) {
      for (Entity e : this.getPassengers()) {
        if (e instanceof LivingEntity rider
            && rider.isAlive()
            && VehicleSafety.exit(this, rider).isEmpty()) {
          this.entityData.set(HEALTH, 1.0F);
          this.disabled = true;
          this.entityData.set(ENGINE_RUNNING, false);
          this.stopControls();
          if (rider instanceof Player p) {
            p.displayClientMessage(
                Component.literal("Vehicle disabled: clear a safe exit before destruction."), true);
          }

          return true;
        }
      }

      this.stopControls();
      this.ejectPassengers();
      this.discard();
      return true;
    } else {
      this.entityData.set(HEALTH, remaining);
      return true;
    }
  }

  protected void readAdditionalSaveData(CompoundTag t) {
    this.disabled = t.getBoolean("VehicleDisabled");
    this.entityData.set(
        HEALTH,
        t.contains("VehicleHealth") && Float.isFinite(t.getFloat("VehicleHealth"))
            ? Mth.clamp(t.getFloat("VehicleHealth"), 1.0F, 40.0F)
            : 40.0F);
    this.entityData.set(ENGINE_RUNNING, false);
    this.stopControls();
  }

  protected void addAdditionalSaveData(CompoundTag t) {
    t.putBoolean("VehicleDisabled", this.disabled);
    t.putFloat("VehicleHealth", this.health());
    t.putString("VehiclePrototype", "0.5.4");
  }

  public void lerpTo(double x, double y, double z, float yaw, float pitch, int steps) {
    this.lx = x;
    this.ly = y;
    this.lz = z;
    this.lyaw = yaw;
    this.lpitch = pitch;
    this.lerpSteps = Mth.clamp(steps, 1, 10);
  }
}
