package com.timelocked;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class PortalControllerBlock extends BaseEntityBlock {
  public static final MapCodec<PortalControllerBlock> CODEC =
      simpleCodec(PortalControllerBlock::new);
  public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
  public static final BooleanProperty PRESSED = BooleanProperty.create("pressed");
  private static final VoxelShape SHAPE =
      Shapes.or(
          Block.box(0.0, 0.0, 1.0, 16.0, 13.0, 15.0),
          new VoxelShape[] {
            Block.box(0.0, 11.0, 0.0, 16.0, 13.0, 16.0),
            Block.box(1.0, 13.0, 9.0, 15.0, 17.0, 15.0),
            Block.box(5.0, 13.0, 2.5, 11.0, 15.5, 8.5)
          });

  public PortalControllerBlock(Properties p) {
    super(p);
    this.registerDefaultState(
        (BlockState)
            ((BlockState)
                    ((BlockState) this.stateDefinition.any()).setValue(FACING, Direction.NORTH))
                .setValue(PRESSED, false));
  }

  protected MapCodec<? extends PortalControllerBlock> codec() {
    return CODEC;
  }

  protected void createBlockStateDefinition(Builder<Block, BlockState> b) {
    b.add(new Property[] {FACING, PRESSED});
  }

  public BlockState getStateForPlacement(BlockPlaceContext c) {
    return (BlockState)
        this.defaultBlockState().setValue(FACING, c.getHorizontalDirection().getOpposite());
  }

  protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
    return FacingDecorBlock.rotate(SHAPE, (Direction) s.getValue(FACING));
  }

  protected RenderShape getRenderShape(BlockState s) {
    return RenderShape.MODEL;
  }

  public BlockEntity newBlockEntity(BlockPos p, BlockState s) {
    return new PortalControllerEntity(p, s);
  }

  protected InteractionResult useWithoutItem(
      BlockState s, Level l, BlockPos pos, Player p, BlockHitResult hit) {
    if (!l.isClientSide && l.getBlockEntity(pos) instanceof PortalControllerEntity be) {
      this.press(l, pos, s);
      if (be.origin == null) {
        be.bind(
            be.facilityLayout == 5
                ? pos.offset(-250, -10, -143)
                : be.facilityLayout == 4 ? pos.offset(-141, -8, -12) : pos.offset(-88, -8, -36));
      }

      p.sendSystemMessage(Component.literal(be.toggle()));
    }

    return InteractionResult.sidedSuccess(l.isClientSide);
  }

  private void press(Level l, BlockPos pos, BlockState s) {
    if (s.hasProperty(PRESSED)) {
      l.setBlock(pos, (BlockState) s.setValue(PRESSED, true), 3);
      l.scheduleTick(pos, this, 14);
    }

    l.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.9F, 0.6F);
    l.playSound(null, pos, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 0.5F, 1.6F);
  }

  protected void tick(BlockState s, ServerLevel l, BlockPos pos, RandomSource r) {
    if ((Boolean) s.getValue(PRESSED)) {
      l.setBlock(pos, (BlockState) s.setValue(PRESSED, false), 3);
      l.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_OFF, SoundSource.BLOCKS, 0.7F, 0.7F);
    }
  }

  protected void neighborChanged(
      BlockState s, Level l, BlockPos pos, Block block, BlockPos from, boolean moving) {
    if (!l.isClientSide && l.getBlockEntity(pos) instanceof PortalControllerEntity be) {
      boolean signal = l.hasNeighborSignal(pos);
      if (signal && !be.buttonPressed) {
        if (be.origin == null) {
          be.bind(
              be.facilityLayout == 5
                  ? pos.offset(-250, -10, -143)
                  : be.facilityLayout == 4 ? pos.offset(-141, -8, -12) : pos.offset(-88, -8, -36));
        }

        this.press(l, pos, s);
        String message = be.toggle();

        for (Player player : l.players()) {
          if (player.blockPosition().distSqr(pos) < 4096.0) {
            player.sendSystemMessage(Component.literal(message));
          }
        }
      }

      be.buttonPressed = signal;
    }
  }
}
