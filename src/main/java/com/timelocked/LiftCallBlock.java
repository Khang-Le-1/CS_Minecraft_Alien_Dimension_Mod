package com.timelocked;

import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

public final class LiftCallBlock extends Block {
  public static final MapCodec<LiftCallBlock> CODEC = simpleCodec(LiftCallBlock::new);

  public LiftCallBlock(Properties p) {
    super(p);
  }

  protected MapCodec<? extends LiftCallBlock> codec() {
    return CODEC;
  }

  protected InteractionResult useWithoutItem(
      BlockState s, Level l, BlockPos pos, Player p, BlockHitResult hit) {
    if (l.isClientSide) {
      return InteractionResult.SUCCESS;
    }

    ServerLevel sl = (ServerLevel) l;
    Optional<BlockPos> site = SitePlanner.containing(sl, pos);
    if (!site.isEmpty() && SitePlanner.progress(sl, site.get()).powered) {
      BlockPos o = site.get();
      BlockPos lo = BlueprintRooms.at(l, o, 48, 1, 182), hi = BlueprintRooms.at(l, o, 54, 35, 189);
      AABB shaft = new AABB(lo.getX(), lo.getY(), lo.getZ(), hi.getX(), hi.getY(), hi.getZ());
      if (!VehicleSafety.loaded(l, shaft)) {
        p.sendSystemMessage(Component.literal("Lift shaft chunks unavailable."));
        return InteractionResult.CONSUME;
      }

      List<SarcLiftEntity> lifts = l.getEntitiesOfClass(SarcLiftEntity.class, shaft.inflate(1.0));
      SarcLiftEntity lift = lifts.isEmpty() ? null : lifts.get(0);
      if (lift == null) {
        lift = (SarcLiftEntity) ((EntityType) UndergroundContent.LIFT.get()).create(l);
        if (lift == null) {
          return InteractionResult.FAIL;
        }

        lift.siteOrigin = o;
        BlockPos dock = BlueprintRooms.at(l, o, 51, 2, 185);
        lift.moveTo(dock.getX(), dock.getY(), dock.getZ() + .5, 0, 0);
        if (!l.noCollision(lift, lift.getBoundingBox()) || !sl.addFreshEntity(lift)) {
          return InteractionResult.FAIL;
        }
      }

      if (lift.moving() && !lift.getPassengers().isEmpty()) {
        p.sendSystemMessage(Component.literal("Lift in use; wait for it to dock."));
        return InteractionResult.CONSUME;
      } else {
        lift.request(pos.getY() < o.getY() + 15 ? 2 : 29);
        p.sendSystemMessage(
            Component.literal("Lift called. Right-click the docked platform to board and travel."));
        return InteractionResult.CONSUME;
      }
    } else {
      p.sendSystemMessage(Component.literal("Lift power offline. Use the stairs to Reactor CR."));
      return InteractionResult.CONSUME;
    }
  }
}
