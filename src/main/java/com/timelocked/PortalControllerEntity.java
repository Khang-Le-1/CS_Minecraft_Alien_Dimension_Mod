package com.timelocked;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public final class PortalControllerEntity extends BlockEntity {
  public boolean initialGridProvisioned;
  public int facilityLayout;
  public boolean buttonPressed;
  public BlockPos origin;
  public final PowerBuffer receiver = new PowerBuffer(64000, true, false, this::setChanged);

  public PortalControllerEntity(BlockPos p, BlockState s) {
    super((BlockEntityType) Content.CONTROLLER_ENTITY.get(), p, s);
  }

  public void bind(BlockPos p) {
    this.origin = p.immutable();
    if (this.facilityLayout >= 2 && this.level instanceof ServerLevel l) {
      SitePlanner.progress(l, this.origin).layoutVersion = 3;
      if (this.facilityLayout == 4 || this.facilityLayout == BlueprintRooms.VERSION) {
        SitePlanner.progress(l, this.origin).geometryVersion = this.facilityLayout;
      }
      FacilityState.get(l.getServer()).setDirty();
    }

    this.provisionInitialGrid();
    this.setChanged();
  }

  public void provisionInitialGrid() {
    if (!this.initialGridProvisioned
        && this.facilityLayout >= 2
        && this.origin != null
        && this.level instanceof ServerLevel l
        && SitePlanner.progress(l, this.origin).powered) {
      this.receiver.receiveEnergy(64000, false);
      this.initialGridProvisioned = true;
      this.setChanged();
    }
  }

  public String toggle() {
    this.provisionInitialGrid();
    if (this.level != null
        && this.origin != null
        && !this.level
            .getBlockState(BlueprintRooms.at(this.level, this.origin, PortalGeometry.ANCHOR))
            .is((Block) FacilityDetails.ANCHOR.get())) {
      return this.toggleLegacy();
    } else if (this.level == null || this.level.isClientSide) {
      return "Server only";
    } else if (this.origin == null) {
      return "Unbound controller: use /sarcsite confirm to assemble and bind a complete facility.";
    } else if (this.level instanceof ServerLevel serverLevel
        && !SitePlanner.progress(serverLevel, this.origin).powered) {
      return "Facility offline. Restore Wing 4 Reactor Ops first.";
    } else {
      boolean active =
          this.level
              .getBlockState(BlueprintRooms.at(this.level, this.origin, 64, 14, 30))
              .is((Block) Content.FIELD.get());
      if (active
          && this.level instanceof ServerLevel serverLevel
          && SitePlanner.progress(serverLevel, this.origin).fighting) {
        return "The three-wave breach is active. Defeat the Bionexians to unlock transit.";
      }

      if (active) {
        for (BlockPos offset : PortalGeometry.APERTURE) {
          BlockPos p = BlueprintRooms.at(this.level, this.origin, offset);
          if (this.level.getBlockState(p).is((Block) Content.FIELD.get())) {
            this.level.removeBlock(p, false);
          }
        }

        return "Portal deactivated.";
      } else {
        for (BlockPos offset : PortalGeometry.FRAME) {
          BlockState s =
              this.level.getBlockState(BlueprintRooms.at(this.level, this.origin, offset));
          if (!s.is((Block) FacilityDetails.FRAME.get())
              && !s.is((Block) FacilityDetails.ANCHOR.get())) {
            return "Containment ring damaged: repair its frame before activation.";
          }
        }

        for (BlockPos offset : PortalGeometry.APERTURE) {
          if (!this.level
              .getBlockState(BlueprintRooms.at(this.level, this.origin, offset))
              .isAir()) {
            return "Aperture obstructed. Remove blocks from the ring opening first.";
          }
        }

        if (this.level.getDifficulty() == Difficulty.PEACEFUL
            && this.level instanceof ServerLevel sl
            && !SitePlanner.progress(sl, this.origin).cleared) {
          return "The encounter cannot start in Peaceful. Switch difficulty first.";
        }

        if (!this.receiver.consume(16000)) {
          return "Portal needs 16000 FE; charge this controller from the reactor power supply via conduit. Stored: "
              + this.receiver.getEnergyStored()
              + " FE.";
        }

        for (BlockPos offset : PortalGeometry.APERTURE) {
          this.level.setBlock(
              BlueprintRooms.at(this.level, this.origin, offset),
              (BlockState)
                  ((PortalFieldBlock) Content.FIELD.get())
                      .defaultBlockState()
                      .setValue(PortalFieldBlock.HIDDEN, true),
              3);
        }

        return EncounterManager.start((ServerLevel) this.level, this.origin);
      }
    }
  }

  private String toggleLegacy() {
    if (this.level != null && !this.level.isClientSide) {
      if (this.origin == null) {
        return "Unbound controller: use /sarcsite confirm to assemble and bind a complete facility.";
      } else if (this.level instanceof ServerLevel serverLevel
          && !SitePlanner.progress(serverLevel, this.origin).powered) {
        return "Facility offline. Restore Wing 4 Reactor Ops first.";
      } else {
        boolean active =
            this.level
                .getBlockState(BlueprintRooms.at(this.level, this.origin, 64, 14, 30))
                .is((Block) Content.FIELD.get());
        if (active
            && this.level instanceof ServerLevel serverLevel
            && SitePlanner.progress(serverLevel, this.origin).fighting) {
          return "The three-wave breach is active. Defeat the Bionexians to unlock transit.";
        }

        if (active) {
          for (int u = -6; u <= 6; u++) {
            for (int v = -6; v <= 6; v++) {
              for (int du = 0; du < 2; du++) {
                for (int dz = 0; dz < 2; dz++) {
                  if (Math.abs(u) + Math.abs(v) <= 9) {
                    BlockPos p =
                        BlueprintRooms.at(
                            this.level, this.origin, 64 + u * 2 + du, 14 + v, 30 + dz);
                    if (this.level.getBlockState(p).is((Block) Content.FIELD.get())) {
                      this.level.removeBlock(p, false);
                    }
                  }
                }
              }
            }
          }

          return "Portal deactivated.";
        } else {
          int[][] anchors =
              new int[][] {{9, 0}, {-9, 0}, {0, 9}, {0, -9}, {6, 6}, {6, -6}, {-6, 6}, {-6, -6}};

          for (int[] xy : anchors) {
            BlockState s =
                this.level.getBlockState(
                    BlueprintRooms.at(this.level, this.origin, 64 + xy[0] * 2, 14 + xy[1], 30));
            if (!s.is((Block) Content.CASING.get())
                && !s.is((Block) Content.LIGHT.get())
                && !s.is((Block) Content.WALL.get())) {
              return "Containment ring damaged: repair its frame before activation.";
            }
          }

          for (int u = -6; u <= 6; u++) {
            for (int v = -6; v <= 6; v++) {
              for (int du = 0; du < 2; du++) {
                for (int dz = 0; dz < 2; dz++) {
                  if (Math.abs(u) + Math.abs(v) <= 9
                      && !this.level
                          .getBlockState(
                              BlueprintRooms.at(
                                  this.level, this.origin, 64 + u * 2 + du, 14 + v, 30 + dz))
                          .isAir()) {
                    return "Aperture obstructed. Remove blocks from the ring opening first.";
                  }
                }
              }
            }
          }

          if (this.level.getDifficulty() == Difficulty.PEACEFUL
              && this.level instanceof ServerLevel sl
              && !SitePlanner.progress(sl, this.origin).cleared) {
            return "The encounter cannot start in Peaceful. Switch difficulty first.";
          }

          if (!this.receiver.consume(16000)) {
            return "Portal needs 16000 FE; charge this controller from the reactor power supply via conduit. Stored: "
                + this.receiver.getEnergyStored()
                + " FE.";
          }

          for (int u = -6; u <= 6; u++) {
            for (int v = -6; v <= 6; v++) {
              for (int du = 0; du < 2; du++) {
                for (int dz = 0; dz < 2; dz++) {
                  if (Math.abs(u) + Math.abs(v) <= 9) {
                    this.level.setBlock(
                        BlueprintRooms.at(
                            this.level, this.origin, 64 + u * 2 + du, 14 + v, 30 + dz),
                        ((PortalFieldBlock) Content.FIELD.get()).defaultBlockState(),
                        3);
                  }
                }
              }
            }
          }

          return EncounterManager.start((ServerLevel) this.level, this.origin);
        }
      }
    } else {
      return "Server only";
    }
  }

  protected void saveAdditional(CompoundTag t, Provider p) {
    super.saveAdditional(t, p);
    t.putBoolean("GridProvisioned", this.initialGridProvisioned);
    t.putInt("FacilityLayout", this.facilityLayout);
    t.putInt("Energy", this.receiver.getEnergyStored());
    if (this.origin != null) {
      t.putLong("Origin", this.origin.asLong());
    }
  }

  protected void loadAdditional(CompoundTag t, Provider p) {
    super.loadAdditional(t, p);
    this.initialGridProvisioned = t.getBoolean("GridProvisioned");
    this.facilityLayout = t.getInt("FacilityLayout");
    this.receiver.restore(t.getInt("Energy"));
    this.origin = t.contains("Origin") ? BlockPos.of(t.getLong("Origin")) : null;
  }
}
