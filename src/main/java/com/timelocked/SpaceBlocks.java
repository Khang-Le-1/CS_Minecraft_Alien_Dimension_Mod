package com.timelocked;

import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class SpaceBlocks {
  static void say(Player p, String s) {
    p.displayClientMessage(Component.literal(s), true);
  }

  private SpaceBlocks() {}

  public static final class Airlock extends SpaceBlocks.Facing {
    public static final MapCodec<SpaceBlocks.Airlock> CODEC = simpleCodec(SpaceBlocks.Airlock::new);

    public Airlock(Properties p) {
      super(p);
    }

    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
      return CODEC;
    }

    protected InteractionResult useWithoutItem(
        BlockState s, Level l, BlockPos p, Player user, BlockHitResult hit) {
      if (user instanceof ServerPlayer sp) {
        if (l.dimension().equals(SpacePlanets.SHIP)) {
          Timeskipper.disembark(sp);
        } else {
          SpaceBlocks.say(user, "[AIRLOCK] Sealed.");
        }
      }

      return InteractionResult.sidedSuccess(l.isClientSide);
    }
  }

  public static final class Beacon extends Block {
    public Beacon(Properties p) {
      super(p);
    }

    protected InteractionResult useWithoutItem(
        BlockState s, Level l, BlockPos p, Player user, BlockHitResult hit) {
      if (user instanceof ServerPlayer sp) {
        Timeskipper.board(sp);
      }

      return InteractionResult.sidedSuccess(l.isClientSide);
    }

    public void animateTick(BlockState s, Level l, BlockPos p, RandomSource r) {
      l.addParticle(
          PlantCoreBlock.dust(3, 1.0F),
          p.getX() + 0.5 + (r.nextDouble() - 0.5) * 0.5,
          p.getY() + 1.1 + r.nextDouble() * 0.8,
          p.getZ() + 0.5 + (r.nextDouble() - 0.5) * 0.5,
          0.0,
          0.02,
          0.0);
    }
  }

  public static final class Drive extends Block {
    public Drive(Properties p) {
      super(p);
    }

    static int charge(ItemStack s) {
      Item ingot =
          (Item)
              BuiltInRegistries.ITEM.get(
                  ResourceLocation.fromNamespaceAndPath("timelocked", "chronotite_ingot"));
      Item block =
          (Item)
              BuiltInRegistries.ITEM.get(
                  ResourceLocation.fromNamespaceAndPath("timelocked", "chronotite_block"));
      return s.is(ingot) ? 1 : (s.is(block) ? 9 : 0);
    }

    protected ItemInteractionResult useItemOn(
        ItemStack stack,
        BlockState s,
        Level l,
        BlockPos p,
        Player user,
        InteractionHand hand,
        BlockHitResult hit) {
      int n = charge(stack);
      if (n == 0) {
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      }

      if (user instanceof ServerPlayer sp) {
        Timeskipper.Data d = Timeskipper.data(sp.server);
        if (d.charges + n > 32) {
          SpaceBlocks.say(user, "[DRIVE] Charge cells full (" + d.charges + "/32).");
          return ItemInteractionResult.CONSUME;
        }

        d.charges += n;
        d.setDirty();
        stack.consume(1, user);
        l.playSound(null, p, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 1.0F, 1.6F);
        SpaceBlocks.say(user, "[DRIVE] Chronotite absorbed. Jump charges: " + d.charges + "/32");
        if (sp.serverLevel().dimension().equals(SpacePlanets.SHIP)) {
          TimeskipperShip.updateScreens(sp.serverLevel(), d);
        }
      }

      return ItemInteractionResult.sidedSuccess(l.isClientSide);
    }

    protected InteractionResult useWithoutItem(
        BlockState s, Level l, BlockPos p, Player user, BlockHitResult hit) {
      if (user instanceof ServerPlayer sp) {
        SpaceBlocks.say(
            user,
            "[DRIVE] Jump charges: "
                + Timeskipper.data(sp.server).charges
                + "/32. Feed chronotite ingots or blocks.");
      }

      return InteractionResult.sidedSuccess(l.isClientSide);
    }

    public void animateTick(BlockState s, Level l, BlockPos p, RandomSource r) {
      for (int i = 0; i < 2; i++) {
        l.addParticle(
            PlantCoreBlock.dust(2, 1.0F),
            p.getX() + 0.5 + (r.nextDouble() - 0.5) * 1.4,
            p.getY() + 0.5 + (r.nextDouble() - 0.5) * 1.4,
            p.getZ() + 0.5 + (r.nextDouble() - 0.5) * 1.4,
            0.0,
            0.0,
            0.0);
      }

      if (r.nextInt(2) == 0) {
        l.addParticle(
            ParticleTypes.REVERSE_PORTAL,
            p.getX() + 0.5,
            p.getY() + 0.5,
            p.getZ() + 0.5,
            (r.nextDouble() - 0.5) * 0.3,
            (r.nextDouble() - 0.5) * 0.3,
            (r.nextDouble() - 0.5) * 0.3);
      }
    }
  }

  abstract static class Facing extends HorizontalDirectionalBlock {
    Facing(Properties p) {
      super(p);
      this.registerDefaultState(
          (BlockState) ((BlockState) this.stateDefinition.any()).setValue(FACING, Direction.NORTH));
    }

    protected void createBlockStateDefinition(Builder<Block, BlockState> b) {
      b.add(new Property[] {FACING});
    }

    public BlockState getStateForPlacement(BlockPlaceContext c) {
      return (BlockState)
          this.defaultBlockState().setValue(FACING, c.getHorizontalDirection().getOpposite());
    }
  }

  public static final class LifeSupport extends Block {
    public LifeSupport(Properties p) {
      super(p);
    }

    public void animateTick(BlockState s, Level l, BlockPos p, RandomSource r) {
      if (r.nextInt(3) == 0) {
        l.addParticle(
            ParticleTypes.CLOUD,
            p.getX() + 0.5,
            p.getY() + 1.05,
            p.getZ() + 0.5,
            (r.nextDouble() - 0.5) * 0.05,
            0.03,
            (r.nextDouble() - 0.5) * 0.05);
      }
    }

    public void appendHoverText(
        ItemStack s, TooltipContext c, List<Component> lines, TooltipFlag f) {
      lines.add(Component.translatable("tooltip.timelocked.life_support"));
    }
  }

  public static final class Link extends Item {
    public Link(net.minecraft.world.item.Item.Properties p) {
      super(p);
    }

    public InteractionResultHolder<ItemStack> use(Level l, Player p, InteractionHand hand) {
      ItemStack s = p.getItemInHand(hand);
      if (p instanceof ServerPlayer sp && Timeskipper.board(sp)) {
        sp.getCooldowns().addCooldown(this, 400);
      }

      return InteractionResultHolder.sidedSuccess(s, l.isClientSide);
    }

    public void appendHoverText(
        ItemStack s, TooltipContext c, List<Component> lines, TooltipFlag f) {
      lines.add(Component.translatable("tooltip.timelocked.timeskipper_link"));
    }
  }

  public static final class Nav extends SpaceBlocks.Facing {
    public static final MapCodec<SpaceBlocks.Nav> CODEC = simpleCodec(SpaceBlocks.Nav::new);
    private static final VoxelShape SHAPE =
        Shapes.or(
            Block.box(1.0, 0.0, 3.0, 5.0, 11.0, 14.0),
            new VoxelShape[] {
              Block.box(11.0, 0.0, 3.0, 15.0, 11.0, 14.0),
              Block.box(0.0, 11.0, 0.0, 16.0, 13.0, 16.0),
              Block.box(1.0, 13.0, 1.0, 15.0, 13.3, 8.0),
              Block.box(2.0, 13.0, 10.0, 14.0, 21.0, 13.0),
              Block.box(3.0, 14.0, 9.8, 13.0, 20.0, 10.0),
              Block.box(12.0, 13.4, 4.0, 14.0, 14.4, 6.0),
              Block.box(2.0, 2.0, 2.8, 4.0, 9.0, 3.0),
              Block.box(12.0, 2.0, 2.8, 14.0, 9.0, 3.0)
            });

    public Nav(Properties p) {
      super(p);
    }

    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
      return CODEC;
    }

    protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
      return FacingDecorBlock.rotate(SHAPE, (Direction) s.getValue(FACING));
    }

    protected InteractionResult useWithoutItem(
        BlockState s, Level l, BlockPos p, Player user, BlockHitResult hit) {
      if (user instanceof ServerPlayer sp) {
        if (l.dimension().equals(SpacePlanets.SHIP)) {
          Timeskipper.starMap(sp);
        } else {
          SpaceBlocks.say(user, "[NAV] Offline. This console only works aboard the TIMESKIPPER.");
        }
      }

      return InteractionResult.sidedSuccess(l.isClientSide);
    }

    public void animateTick(BlockState s, Level l, BlockPos p, RandomSource r) {
      // Keep the bridge interface clear of drifting white end-rod particles.
      if (l.dimension().equals(SpacePlanets.SHIP)) return;
      if (r.nextInt(3) == 0) {
        l.addParticle(
            ParticleTypes.END_ROD,
            p.getX() + 0.5 + (r.nextDouble() - 0.5) * 0.6,
            p.getY() + 1.15,
            p.getZ() + 0.5 + (r.nextDouble() - 0.5) * 0.6,
            0.0,
            0.01,
            0.0);
      }
    }
  }
}
