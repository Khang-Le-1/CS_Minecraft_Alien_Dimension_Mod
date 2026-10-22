package com.timelocked;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Scheduled-tick alarm with a client-rendered rotary reflector. */
public final class AlarmBlock extends HorizontalDirectionalBlock
    implements net.minecraft.world.level.block.EntityBlock {
  public static final BooleanProperty ACTIVE = BooleanProperty.create("active"),
      MANUAL = BooleanProperty.create("manual"),
      LIT = BlockStateProperties.LIT;
  public static final MapCodec<AlarmBlock> CODEC = simpleCodec(p -> new AlarmBlock(p, false));
  private final boolean beacon;

  public AlarmBlock(Properties p, boolean beacon) {
    super(p);
    this.beacon = beacon;
    registerDefaultState(
        stateDefinition
            .any()
            .setValue(FACING, Direction.NORTH)
            .setValue(ACTIVE, false)
            .setValue(MANUAL, false)
            .setValue(LIT, false));
  }

  public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(
      BlockPos p, BlockState s) {
    return new AlarmRotorEntity(p, s);
  }

  protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
    return CODEC;
  }

  protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
    b.add(FACING, ACTIVE, MANUAL, LIT);
  }

  public BlockState getStateForPlacement(BlockPlaceContext c) {
    return defaultBlockState().setValue(FACING, c.getHorizontalDirection().getOpposite());
  }

  protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
    return FacingDecorBlock.rotate(
        beacon ? Block.box(3, 2, 5, 13, 15, 16) : Block.box(3, 2, 4.1, 13, 14, 16),
        s.getValue(FACING));
  }

  protected void onPlace(BlockState s, Level l, BlockPos p, BlockState old, boolean moving) {
    super.onPlace(s, l, p, old, moving);
    if (!l.isClientSide) l.scheduleTick(p, this, 1);
  }

  protected void neighborChanged(
      BlockState s, Level l, BlockPos p, Block b, BlockPos from, boolean moving) {
    if (!l.isClientSide) l.scheduleTick(p, this, 1);
  }

  protected void tick(BlockState s, ServerLevel l, BlockPos p, RandomSource random) {
    boolean active =
        AlarmRules.active(
            FacilityAlarms.manualState(l, p),
            FacilityAlarms.redstone(l, p),
            FacilityAlarms.encounter(l, p) || StationAlarms.fault(l, p));
    boolean lit = beacon && AlarmRules.lit(active, l.getGameTime());
    BlockState next = s.setValue(ACTIVE, active).setValue(LIT, lit);
    if (!next.equals(s)) l.setBlock(p, next, 3);
    // Sirens play the Facility Danger Alarm clip (beacons use steady emission plus a rotating
    // reflector). Finite mono clip: no overlapping loops or sound left running after a chunk
    // unload.
    if (!beacon) FacilityAlarms.audio(l, p, active, true);
    l.scheduleTick(p, this, 5);
  }

  protected InteractionResult useWithoutItem(
      BlockState s, Level l, BlockPos p, Player user, BlockHitResult hit) {
    if (user instanceof ServerPlayer sp) {
      boolean on = !FacilityAlarms.manualState((ServerLevel) l, p);
      FacilityAlarms.manual((ServerLevel) l, p, on);
      sp.sendSystemMessage(
          Component.literal(
              on
                  ? "[ALARM] Manual alarm enabled."
                  : "[ALARM] Manual alarm cleared. Automatic faults, redstone or an active facility encounter can keep it running."));
    }
    return InteractionResult.sidedSuccess(l.isClientSide);
  }
}
