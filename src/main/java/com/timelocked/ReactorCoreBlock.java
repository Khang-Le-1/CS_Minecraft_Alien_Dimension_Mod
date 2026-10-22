package com.timelocked;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.chunk.LevelChunk.EntityCreationType;

public final class ReactorCoreBlock extends BaseEntityBlock {
  public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
  public static final MapCodec<ReactorCoreBlock> CODEC = simpleCodec(ReactorCoreBlock::new);

  public ReactorCoreBlock(Properties p) {
    super(p);
    this.registerDefaultState(
        (BlockState) ((BlockState) this.stateDefinition.any()).setValue(ACTIVE, false));
  }

  protected MapCodec<? extends ReactorCoreBlock> codec() {
    return CODEC;
  }

  protected void createBlockStateDefinition(Builder<Block, BlockState> b) {
    b.add(new Property[] {ACTIVE});
  }

  protected RenderShape getRenderShape(BlockState s) {
    return RenderShape.ENTITYBLOCK_ANIMATED;
  }

  protected net.minecraft.world.InteractionResult useWithoutItem(
      BlockState s,
      Level l,
      BlockPos p,
      net.minecraft.world.entity.player.Player user,
      net.minecraft.world.phys.BlockHitResult hit) {
    if (l.getBlockEntity(p.offset(0, 0, 4)) instanceof ReactorFuelEntity fuel) {
      if (!l.isClientSide) user.openMenu(fuel);
      return net.minecraft.world.InteractionResult.sidedSuccess(l.isClientSide);
    }
    return net.minecraft.world.InteractionResult.PASS;
  }

  public BlockEntity newBlockEntity(BlockPos p, BlockState s) {
    return new ReactorCoreBlock.Vessel(p, s);
  }

  public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
      Level l, BlockState s, BlockEntityType<T> t) {
    return l.isClientSide
        ? null
        : createTickerHelper(
            t, UndergroundContent.CORE_ENTITY.get(), (lv, p, st, be) -> be.serverTick());
  }

  protected boolean isRandomlyTicking(BlockState s) {
    return true;
  }

  protected void randomTick(BlockState s, ServerLevel l, BlockPos p, RandomSource r) {
    if (l.getBlockEntity(p) == null) {
      l.getChunkAt(p).getBlockEntity(p, EntityCreationType.IMMEDIATE);
    }
  }

  protected void onRemove(BlockState s, Level l, BlockPos p, BlockState n, boolean moving) {
    if (!l.isClientSide && !s.is(n.getBlock())) {
      ReactorFrame.clear(l, p, ReactorFrame.FACILITY);
    }

    super.onRemove(s, l, p, n, moving);
  }

  public void animateTick(BlockState s, Level l, BlockPos p, RandomSource r) {
    if ((Boolean) s.getValue(ACTIVE)) {
      double a = r.nextDouble() * Math.PI * 2.0;
      l.addParticle(
          ParticleTypes.GLOW,
          p.getX() + 0.5 + Math.cos(a) * 1.2,
          p.getY() + 10.2,
          p.getZ() + 0.5 + Math.sin(a) * 1.2,
          0.0,
          0.04,
          0.0);
      if (r.nextInt(4) == 0) {
        l.addParticle(
            ParticleTypes.ELECTRIC_SPARK,
            p.getX() + 0.5 + (r.nextDouble() - 0.5) * 1.4,
            p.getY() + 10.7,
            p.getZ() + 0.5 + (r.nextDouble() - 0.5) * 1.4,
            0.0,
            0.05,
            0.0);
      }
    }
  }

  public static final class Vessel extends BlockEntity {
    public boolean framed;
    public float lastAnim;
    public float spin;
    public float rodLift;

    public Vessel(BlockPos p, BlockState s) {
      super((BlockEntityType) UndergroundContent.CORE_ENTITY.get(), p, s);
    }

    void serverTick() {
      if (this.level != null) {
        if (!this.framed
            || this.level.getGameTime() % 40L == 0L
                && !ReactorFrame.intact(this.level, this.worldPosition)) {
          ReactorFrame.build(this.level, this.worldPosition, ReactorFrame.FACILITY);
          this.framed = true;
          this.setChanged();
        }
      }
    }

    protected void saveAdditional(CompoundTag t, Provider p) {
      super.saveAdditional(t, p);
      t.putBoolean("Framed", this.framed);
    }

    protected void loadAdditional(CompoundTag t, Provider p) {
      super.loadAdditional(t, p);
      this.framed = t.getBoolean("Framed");
    }
  }
}
