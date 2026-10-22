package com.timelocked;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class ReactorCasingBlock extends Block {
  public static final MapCodec<ReactorCasingBlock> CODEC = simpleCodec(ReactorCasingBlock::new);

  public ReactorCasingBlock(Properties p) {
    super(p);
  }

  protected MapCodec<? extends ReactorCasingBlock> codec() {
    return CODEC;
  }

  protected RenderShape getRenderShape(BlockState s) {
    return RenderShape.INVISIBLE;
  }

  protected boolean propagatesSkylightDown(BlockState s, BlockGetter l, BlockPos p) {
    return true;
  }

  protected float getShadeBrightness(BlockState s, BlockGetter l, BlockPos p) {
    return 1.0F;
  }

  protected VoxelShape getVisualShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
    return Shapes.empty();
  }

  static BlockHitResult retarget(BlockHitResult h, BlockPos c) {
    return new BlockHitResult(h.getLocation(), h.getDirection(), c, false);
  }

  protected ItemInteractionResult useItemOn(
      ItemStack stack,
      BlockState s,
      Level l,
      BlockPos p,
      Player user,
      InteractionHand hand,
      BlockHitResult hit) {
    BlockPos c = ReactorFrame.findCore(l, p);
    return c == null
        ? ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
        : l.getBlockState(c).useItemOn(stack, l, user, hand, retarget(hit, c));
  }

  protected InteractionResult useWithoutItem(
      BlockState s, Level l, BlockPos p, Player user, BlockHitResult hit) {
    BlockPos c = ReactorFrame.findCore(l, p);
    return c == null
        ? InteractionResult.PASS
        : l.getBlockState(c).useWithoutItem(l, user, retarget(hit, c));
  }

  protected float getDestroyProgress(BlockState s, Player user, BlockGetter l, BlockPos p) {
    BlockPos c = ReactorFrame.findCore(l, p);
    return c == null ? 1.0F : l.getBlockState(c).getDestroyProgress(user, l, c);
  }

  public BlockState playerWillDestroy(Level l, BlockPos p, BlockState s, Player user) {
    BlockPos c = ReactorFrame.findCore(l, p);
    if (c != null && !l.isClientSide) {
      BlockState core = l.getBlockState(c);
      if (!user.isCreative() && user.hasCorrectToolForDrops(core)) {
        Block.dropResources(core, l, c, l.getBlockEntity(c), user, user.getMainHandItem());
      }

      l.levelEvent(2001, c, Block.getId(core));
      l.setBlock(c, Blocks.AIR.defaultBlockState(), 3);
    }

    return super.playerWillDestroy(l, p, s, user);
  }

  protected boolean isRandomlyTicking(BlockState s) {
    return true;
  }

  protected void randomTick(BlockState s, ServerLevel l, BlockPos p, RandomSource r) {
    if (ReactorFrame.findCore(l, p) == null) {
      l.removeBlock(p, false);
    }
  }
}
