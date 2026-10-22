package com.timelocked;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;

public final class EntranceGlassDoorBlock extends AnimatedSarcDoorBlock {
  public static final MapCodec<EntranceGlassDoorBlock> CODEC =
      simpleCodec(EntranceGlassDoorBlock::new);

  public EntranceGlassDoorBlock(Properties p) {
    super(BlockSetType.COPPER, p);
  }

  @Override
  public MapCodec<? extends AnimatedSarcDoorBlock> codec() {
    return CODEC;
  }

  @Override
  public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
      Level level, BlockState state, BlockEntityType<T> type) {
    if (level.isClientSide) return super.getTicker(level, state, type);
    if (type != FacilityDetails.DOOR_ENTITY.get() || state.getValue(HALF) != DoubleBlockHalf.LOWER)
      return null;
    return (l, p, s, entity) -> sensor((ServerLevel) l, p, (SarcDoorEntity) entity);
  }

  private static boolean lower(Level l, BlockPos p) {
    if (!l.hasChunkAt(p)) return false;
    BlockState s = l.getBlockState(p);
    return s.getBlock() instanceof EntranceGlassDoorBlock
        && s.getValue(HALF) == DoubleBlockHalf.LOWER
        && l.getBlockState(p.above()).is(s.getBlock())
        && l.getBlockState(p.above()).getValue(HALF) == DoubleBlockHalf.UPPER;
  }

  private static void align(Level l, BlockPos p, Direction facing, DoorHingeSide hinge) {
    for (int h = 0; h < 2; h++) {
      BlockPos at = p.above(h);
      BlockState s = l.getBlockState(at);
      if (!(s.getBlock() instanceof EntranceGlassDoorBlock)) return;
      BlockState desired = s.setValue(FACING, facing).setValue(HINGE, hinge);
      if (!s.equals(desired)) l.setBlock(at, desired, 2);
    }
  }

  private static void repairRegisteredOrientation(
      ServerLevel l, BlockPos p, SarcDoorEntity entity) {
    if (entity.entranceAlignmentChecked) return;
    entity.entranceAlignmentChecked = true;
    if (!l.dimension().equals(Level.OVERWORLD)) return;
    // Only the known generated reception entrances have their facing repaired.
    // Player-built doors elsewhere retain their chosen facing.
    for (long key : FacilityState.get(l.getServer()).sites.keySet()) {
      BlockPos o = BlockPos.of(key);
      BlockPos q = p.subtract(o);
      if (BlueprintRooms.drawing(l, o)) q = q.offset(-26, 0, -171);
      else if (BlueprintRooms.active(l, o)) q = q.offset(20, 0, -70);
      if (q.getY() == 29
          && (q.getX() == 64 || q.getX() == 65)
          && (q.getZ() == 192 || q.getZ() == 196)) {
        Direction face = q.getZ() == 196 ? Direction.NORTH : Direction.SOUTH;
        boolean west = q.getX() == 64;
        DoorHingeSide hinge =
            (face == Direction.NORTH) == west ? DoorHingeSide.LEFT : DoorHingeSide.RIGHT;
        align(l, p, face, hinge);
        return;
      }
    }
  }

  private static void sensor(ServerLevel l, BlockPos p, SarcDoorEntity entity) {
    if ((l.getGameTime() & 1L) != 0 || !lower(l, p)) return;
    repairRegisteredOrientation(l, p, entity);
    BlockState state = l.getBlockState(p);
    Direction face = state.getValue(FACING);
    BlockPos mate = null;
    for (Direction side : new Direction[] {face.getClockWise(), face.getCounterClockWise()}) {
      BlockPos q = p.relative(side);
      if (lower(l, q) && l.getBlockState(q).getValue(FACING) == face) {
        // A row of three or more is ambiguous; do not invent a pair.
        if (mate != null) {
          mate = null;
          break;
        }
        mate = q;
      }
    }
    if (mate != null) {
      // Check the far edge too: an end leaf must not pair into a three-leaf row.
      Direction side =
          mate.equals(p.relative(face.getClockWise()))
              ? face.getClockWise()
              : face.getCounterClockWise();
      BlockPos beyond = mate.relative(side);
      if (lower(l, beyond) && l.getBlockState(beyond).getValue(FACING) == face) mate = null;
    }
    if (mate != null && p.compareTo(mate) > 0) return;
    if (mate != null) {
      DoorHingeSide hinge =
          EntranceDoorLogic.rightHinge(
                  face.getStepX(), face.getStepZ(), mate.getX() - p.getX(), mate.getZ() - p.getZ())
              ? DoorHingeSide.RIGHT
              : DoorHingeSide.LEFT;
      align(l, p, face, hinge);
      align(l, mate, face, hinge == DoorHingeSide.LEFT ? DoorHingeSide.RIGHT : DoorHingeSide.LEFT);
    }
    AABB doorway = new AABB(p).expandTowards(0, 1, 0);
    if (mate != null) doorway = doorway.minmax(new AABB(mate).expandTowards(0, 1, 0));
    AABB approach =
        face.getAxis() == Direction.Axis.X
            ? doorway.inflate(2.5, .15, 1.0)
            : doorway.inflate(1.0, .15, 2.5);
    boolean nearby =
        !l.getEntitiesOfClass(Player.class, approach, e -> e.isAlive() && !e.isSpectator())
            .isEmpty();
    // Prevent closing on a living entity actually occupying the threshold.
    boolean occupied =
        !l.getEntitiesOfClass(
                LivingEntity.class,
                doorway.inflate(.15, 0, .15),
                e -> e.isAlive() && !e.isSpectator())
            .isEmpty();
    if (nearby || occupied)
      entity.entranceHoldUntil = l.getGameTime() + EntranceDoorLogic.HOLD_TICKS;
    boolean powered = l.hasNeighborSignal(p) || l.hasNeighborSignal(p.above());
    if (mate != null) powered |= l.hasNeighborSignal(mate) || l.hasNeighborSignal(mate.above());
    boolean open =
        EntranceDoorLogic.shouldOpen(
            l.getGameTime(), entity.entranceHoldUntil, nearby, occupied, powered);
    ((EntranceGlassDoorBlock) state.getBlock()).setOpen(null, l, l.getBlockState(p), p, open);
    if (mate != null)
      ((EntranceGlassDoorBlock) state.getBlock())
          .setOpen(null, l, l.getBlockState(mate), mate, open);
  }
}
