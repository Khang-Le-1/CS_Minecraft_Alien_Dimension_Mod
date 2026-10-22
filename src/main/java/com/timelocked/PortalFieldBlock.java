package com.timelocked;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;

public final class PortalFieldBlock extends Block {
  public static final BooleanProperty HIDDEN = BooleanProperty.create("hidden");
  public static final MapCodec<PortalFieldBlock> CODEC = simpleCodec(PortalFieldBlock::new);

  public PortalFieldBlock(Properties p) {
    super(p);
    this.registerDefaultState(
        (BlockState) ((BlockState) this.stateDefinition.any()).setValue(HIDDEN, false));
  }

  protected void createBlockStateDefinition(Builder<Block, BlockState> b) {
    b.add(new Property[] {HIDDEN});
  }

  protected RenderShape getRenderShape(BlockState s) {
    return s.getValue(HIDDEN) ? RenderShape.INVISIBLE : RenderShape.MODEL;
  }

  protected MapCodec<? extends PortalFieldBlock> codec() {
    return CODEC;
  }

  protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
    if (!level.isClientSide
        && entity instanceof ServerPlayer p
        && !p.isOnPortalCooldown()
        && !p.isPassenger()
        && !p.isVehicle()) {
      VoidspaceTravel.travel(p);
    }
  }

  public void animateTick(BlockState s, Level l, BlockPos p, RandomSource r) {
    if (r.nextInt(4) == 0) {
      l.addParticle(
          ParticleTypes.PORTAL,
          p.getX() + r.nextDouble(),
          p.getY() + r.nextDouble(),
          p.getZ() + 0.5,
          (r.nextDouble() - 0.5) * 0.05,
          0.04,
          (r.nextDouble() - 0.5) * 0.05);
    }
  }
}
