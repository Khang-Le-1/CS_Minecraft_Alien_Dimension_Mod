package com.timelocked;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Compatible static ports. No free-flight ship entity or arbitrary cross-dimension docking is
 * implied.
 */
public final class DockingPortBlock extends BaseEntityBlock {
  public static final MapCodec<DockingPortBlock> CODEC = simpleCodec(DockingPortBlock::new);
  public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
  public static final IntegerProperty PHASE = IntegerProperty.create("phase", 0, 4);
  public static final BooleanProperty UPPER = BooleanProperty.create("upper");

  public DockingPortBlock(Properties p) {
    super(p);
    registerDefaultState(
        stateDefinition
            .any()
            .setValue(FACING, Direction.NORTH)
            .setValue(PHASE, 0)
            .setValue(UPPER, false));
  }

  protected MapCodec<? extends BaseEntityBlock> codec() {
    return CODEC;
  }

  protected void createBlockStateDefinition(Builder<Block, BlockState> b) {
    b.add(FACING, PHASE, UPPER);
  }

  protected RenderShape getRenderShape(BlockState s) {
    return RenderShape.MODEL;
  }

  public BlockState getStateForPlacement(BlockPlaceContext c) {
    return c.getLevel().getBlockState(c.getClickedPos().above()).canBeReplaced()
        ? defaultBlockState().setValue(FACING, c.getHorizontalDirection().getOpposite())
        : null;
  }

  public void setPlacedBy(Level l, BlockPos p, BlockState s, LivingEntity user, ItemStack item) {
    l.setBlock(p.above(), s.setValue(UPPER, true), 3);
  }

  public BlockEntity newBlockEntity(BlockPos p, BlockState s) {
    return s.getValue(UPPER) ? null : new Port(p, s);
  }

  protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
    if (s.getValue(PHASE) != 4) return Shapes.block();
    return FacingDecorBlock.rotate(
        Shapes.or(Block.box(0, 0, 0, 1, 16, 16), Block.box(15, 0, 0, 16, 16, 16)),
        s.getValue(FACING));
  }

  protected InteractionResult useWithoutItem(
      BlockState s, Level l, BlockPos pos, Player user, BlockHitResult hit) {
    if (user instanceof ServerPlayer p) {
      BlockPos at = s.getValue(UPPER) ? pos.below() : pos;
      DockingProtocol.use(p, at);
    }
    return InteractionResult.sidedSuccess(l.isClientSide);
  }

  protected void tick(BlockState s, ServerLevel l, BlockPos p, RandomSource r) {
    if (!s.getValue(UPPER)) DockingProtocol.tick(l, p);
  }

  public static final class Port extends BlockEntity {
    public BlockPos mate;
    public int distance;

    public Port(BlockPos p, BlockState s) {
      super(ShipInteriorContent.DOCK_ENTITY.get(), p, s);
    }

    protected void saveAdditional(CompoundTag t, Provider r) {
      super.saveAdditional(t, r);
      if (mate != null) t.putLong("Mate", mate.asLong());
      t.putInt("Distance", distance);
    }

    protected void loadAdditional(CompoundTag t, Provider r) {
      super.loadAdditional(t, r);
      mate = t.contains("Mate") ? BlockPos.of(t.getLong("Mate")) : null;
      distance = t.getInt("Distance");
    }
  }
}
