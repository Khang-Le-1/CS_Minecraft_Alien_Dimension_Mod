package com.timelocked;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class SarcLiftEntity extends Entity {
  private static final EntityDataAccessor<Boolean> MOVING =
      SynchedEntityData.defineId(SarcLiftEntity.class, EntityDataSerializers.BOOLEAN);
  public BlockPos siteOrigin;
  private double targetY;
  private double lerpX;
  private double lerpY;
  private double lerpZ;
  private double speed;
  private int lerpSteps;
  private int dockTimer;
  private static final double MAX_SPEED = 0.15;
  private static final double ACCEL = 0.01;
  private static final double MIN_SPEED = 0.006;

  public SarcLiftEntity(EntityType<? extends SarcLiftEntity> type, Level level) {
    super(type, level);
    this.setNoGravity(true);
  }

  protected void defineSynchedData(Builder b) {
    b.define(MOVING, false);
  }

  public boolean moving() {
    return (Boolean) this.entityData.get(MOVING);
  }

  public boolean docked() {
    return this.siteOrigin != null
        && (Math.abs(this.getY() - (this.siteOrigin.getY() + 2)) < 0.01
            || Math.abs(this.getY() - (this.siteOrigin.getY() + 29)) < 0.01)
        && !this.moving();
  }

  public void request(int localY) {
    if (this.siteOrigin != null && (localY == 2 || localY == 29)) {
      this.targetY = this.siteOrigin.getY() + localY;
      if (Math.abs(this.targetY - this.getY()) > 0.01) {
        if (!this.moving()) {
          this.speed = 0.0;
          this.dockTimer = 0;
          this.level()
              .playSound(
                  null,
                  this.blockPosition(),
                  SoundEvents.PISTON_CONTRACT,
                  SoundSource.BLOCKS,
                  0.35F,
                  0.6F);
        }

        this.entityData.set(MOVING, true);
        this.doors(false);
      } else {
        this.doors(true);
      }
    }
  }

  public void tick() {
    super.tick();
    this.setNoGravity(true);
    if (this.level().isClientSide) {
      if (this.lerpSteps > 0) {
        this.setPos(
            this.getX() + (this.lerpX - this.getX()) / this.lerpSteps,
            this.getY() + (this.lerpY - this.getY()) / this.lerpSteps,
            this.getZ() + (this.lerpZ - this.getZ()) / this.lerpSteps);
        this.lerpSteps--;
      }
    } else if (this.siteOrigin != null) {
      boolean powered = SitePlanner.progress((ServerLevel) this.level(), this.siteOrigin).powered;
      if (!powered && this.getY() > this.siteOrigin.getY() + 2.01) {
        this.targetY = this.siteOrigin.getY() + 2;
        this.entityData.set(MOVING, true);
      }

      if (!this.moving()) {
        this.doors(this.docked());
        if (this.dockTimer > 0 && --this.dockTimer == 0) {
          for (Entity rider : List.copyOf(this.getPassengers())) {
            rider.stopRiding();
          }
        }
      } else {
        for (Player p :
            this.level()
                .getEntitiesOfClass(
                    Player.class,
                    this.getBoundingBox().inflate(0.05, 0.3, 0.05),
                    px -> !px.isPassenger() && !px.isSpectator())) {
          if (p.getY() >= this.getY() + 0.15
              && p.getY() < this.getY() + 0.65
              && !p.startRiding(this, true)) {
            this.entityData.set(MOVING, false);
            return;
          }
        }

        double remaining = this.targetY - this.getY();
        double dist = Math.abs(remaining);
        this.speed =
            Math.min(Math.min(0.15, this.speed + 0.01), Math.max(0.006, Math.sqrt(0.02 * dist)));
        double step = Math.signum(remaining) * Math.min(this.speed, dist);
        double next = this.getY() + step;
        AABB cabin =
            new AABB(
                this.getX() - 1.5,
                next,
                this.getZ() - 1.5,
                this.getX() + 1.5,
                next + 2.6,
                this.getZ() + 1.5);
        if (VehicleSafety.loaded(this.level(), cabin)
            && this.level().noCollision(this, cabin)
            && !this.level().containsAnyLiquid(cabin)) {
          this.setPos(this.getX(), next, this.getZ());
          this.setDeltaMovement(0.0, step, 0.0);
          boolean up = remaining > 0.0;
          if (this.tickCount % 40 == 0) {
            this.level()
                .playSound(
                    null,
                    this.blockPosition(),
                    SoundEvents.BEACON_AMBIENT,
                    SoundSource.BLOCKS,
                    0.3F,
                    0.75F);
          }

          if (this.tickCount % 4 == 0) {
            for (Entity rider : this.getPassengers()) {
              if (rider instanceof Player pl) {
                pl.displayClientMessage(
                    Component.literal(
                        (up ? "▲ Going up to Surface  " : "▼ Going down to Facility  ")
                            + this.bar(next)),
                    true);
              }
            }
          }

          if (Math.abs(next - this.targetY) < 0.001) {
            this.speed = 0.0;
            this.entityData.set(MOVING, false);
            this.setDeltaMovement(Vec3.ZERO);
            this.doors(true);
            this.dockTimer = 12;
            this.level()
                .playSound(
                    null,
                    this.blockPosition(),
                    (SoundEvent) SoundEvents.NOTE_BLOCK_BELL.value(),
                    SoundSource.BLOCKS,
                    0.9F,
                    1.19F);
            this.level()
                .playSound(
                    null,
                    this.blockPosition(),
                    SoundEvents.PISTON_EXTEND,
                    SoundSource.BLOCKS,
                    0.3F,
                    0.7F);
            String floor = next > this.siteOrigin.getY() + 15 ? "Surface level" : "Facility level";

            for (Entity rider : this.getPassengers()) {
              if (rider instanceof Player pl) {
                pl.displayClientMessage(Component.literal("● " + floor + ". Doors open."), true);
              }
            }
          }
        } else {
          this.speed = 0.0;
          this.setDeltaMovement(Vec3.ZERO);
          this.doors(false);
        }
      }
    }
  }

  private String bar(double y) {
    if (this.siteOrigin == null) {
      return "";
    }

    int filled =
        (int) Math.round(Math.clamp((y - this.siteOrigin.getY() - 2.0) / 27.0, 0.0, 1.0) * 10.0);
    return "[" + "■".repeat(filled) + "□".repeat(10 - filled) + "]";
  }

  public boolean shouldRiderSit() {
    return false;
  }

  private void doors(boolean open) {
    if (this.siteOrigin != null) {
      for (int y : new int[] {2, 29}) {
        for (int z : new int[] {185, 186}) {
          for (int h = 0; h < 2; h++) {
            BlockPos at = BlueprintRooms.at(this.level(), this.siteOrigin, 53, y + h, z);
            if (this.level().hasChunkAt(at)) {
              BlockState s = this.level().getBlockState(at);
              if (s.is((Block) UndergroundContent.LIFT_DOOR.get())) {
                boolean should =
                    open && Math.abs(this.getY() - (this.siteOrigin.getY() + y)) < 0.01;
                if ((Boolean) s.getValue(DoorBlock.OPEN) != should) {
                  this.level().setBlock(at, (BlockState) s.setValue(DoorBlock.OPEN, should), 3);
                }
              }
            }
          }
        }
      }
    }
  }

  public InteractionResult interact(Player p, InteractionHand hand) {
    if (this.level().isClientSide) {
      return InteractionResult.SUCCESS;
    }

    if (!this.docked()
        || this.siteOrigin == null
        || !SitePlanner.progress((ServerLevel) this.level(), this.siteOrigin).powered) {
      p.sendSystemMessage(
          Component.literal("Lift unavailable. Restore reactor power or use the stairs."));
      return InteractionResult.CONSUME;
    }

    if (!p.startRiding(this, true)) {
      return InteractionResult.FAIL;
    }

    this.request(this.getY() < this.siteOrigin.getY() + 15 ? 29 : 2);
    return InteractionResult.CONSUME;
  }

  protected boolean canAddPassenger(Entity e) {
    return e instanceof Player && this.getPassengers().size() < 4;
  }

  public Vec3 getPassengerRidingPosition(Entity rider) {
    int i = Math.max(0, this.getPassengers().indexOf(rider));
    return this.position().add(i % 2 == 0 ? -0.55 : 0.55, 0.25, i < 2 ? -0.55 : 0.55);
  }

  public Optional<Vec3> safeExit(LivingEntity rider) {
    if (this.docked() && this.siteOrigin != null) {
      int local = this.getY() < this.siteOrigin.getY() + 15 ? 2 : 29;

      for (double x : new double[] {54.5, 55.5}) {
        for (double z : new double[] {185.5, 186.5, 184.5}) {
          BlockPos mapped =
              BlueprintRooms.at(this.level(), this.siteOrigin, (int) x, local, (int) z);
          Vec3 p =
              new Vec3(mapped.getX() + x - (int) x, mapped.getY(), mapped.getZ() + z - (int) z);
          AABB box = rider.getBoundingBox().move(p.subtract(rider.position()));
          BlockPos below = BlockPos.containing(p).below();
          if (VehicleSafety.loaded(this.level(), box)
              && this.level().noCollision(rider, box)
              && !this.level().containsAnyLiquid(box)
              && this.level()
                  .getBlockState(below)
                  .isFaceSturdy(this.level(), below, Direction.UP)) {
            return Optional.of(p);
          }
        }
      }

      return Optional.empty();
    } else {
      return Optional.empty();
    }
  }

  public Vec3 getDismountLocationForPassenger(LivingEntity rider) {
    return this.safeExit(rider).orElse(rider.position());
  }

  public boolean isPickable() {
    return true;
  }

  public boolean canBeCollidedWith() {
    return true;
  }

  public boolean canCollideWith(Entity e) {
    return !this.hasPassenger(e) && !e.isPassengerOfSameVehicle(this);
  }

  public boolean hurt(DamageSource s, float amount) {
    return false;
  }

  public boolean canChangeDimensions(Level from, Level to) {
    return false;
  }

  public void lerpTo(double x, double y, double z, float yaw, float pitch, int steps) {
    this.lerpX = x;
    this.lerpY = y;
    this.lerpZ = z;
    this.lerpSteps = Math.max(1, steps);
  }

  protected void addAdditionalSaveData(CompoundTag t) {
    if (this.siteOrigin != null) {
      t.putLong("SiteOrigin", this.siteOrigin.asLong());
    }

    t.putDouble("TargetY", this.targetY);
    t.putBoolean("Moving", this.moving());
    t.putDouble("Speed", this.speed);
  }

  protected void readAdditionalSaveData(CompoundTag t) {
    this.siteOrigin = t.contains("SiteOrigin") ? BlockPos.of(t.getLong("SiteOrigin")) : null;
    this.targetY = t.getDouble("TargetY");
    if (this.siteOrigin != null) {
      this.targetY =
          Math.clamp(this.targetY, this.siteOrigin.getY() + 2, this.siteOrigin.getY() + 29);
    }

    this.entityData.set(MOVING, t.getBoolean("Moving"));
    this.speed = Math.clamp(t.getDouble("Speed"), 0.0, 0.15);
    this.setNoGravity(true);
    this.setDeltaMovement(Vec3.ZERO);
  }
}
