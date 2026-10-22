package com.timelocked;

import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class TramTerminalBlock extends Block {
  public static final MapCodec<TramTerminalBlock> CODEC = simpleCodec(TramTerminalBlock::new);
  public static final List<BlockPos> UNDERGROUND_ROUTE =
      IntStream.rangeClosed(48, 170).mapToObj(z -> new BlockPos(92, 2, z)).toList();
  public static final List<BlockPos> ROUTE = route();
  public static final List<BlockPos> POWERED_RAILS = poweredRails();

  private static List<BlockPos> route() {
    List<BlockPos> r = new ArrayList<>();

    for (int z = 42; z >= 36; z--) {
      r.add(new BlockPos(104, 2, z));
    }

    for (int x = 103; x >= 96; x--) {
      r.add(new BlockPos(x, 2, 36));
    }

    for (int z = 37; z <= 108; z++) {
      r.add(new BlockPos(96, 2, z));
    }

    for (int x = 95; x >= 70; x--) {
      r.add(new BlockPos(x, 2, 108));
    }

    for (int z = 109; z <= 124; z++) {
      r.add(new BlockPos(70, 2, z));
    }

    return List.copyOf(r);
  }

  private static List<BlockPos> poweredRails() {
    List<BlockPos> out = new ArrayList<>();

    for (int i = 0; i < ROUTE.size(); i++) {
      BlockPos at = ROUTE.get(i);
      boolean corner =
          i > 0
              && i < ROUTE.size() - 1
              && ROUTE.get(i - 1).getX() != ROUTE.get(i + 1).getX()
              && ROUTE.get(i - 1).getZ() != ROUTE.get(i + 1).getZ();
      if (!corner && i % 4 == 0) {
        out.add(at);
      }
    }

    return List.copyOf(out);
  }

  public TramTerminalBlock(Properties p) {
    super(p);
  }

  protected MapCodec<? extends TramTerminalBlock> codec() {
    return CODEC;
  }

  protected InteractionResult useWithoutItem(
      BlockState s, Level level, BlockPos pos, Player p, BlockHitResult hit) {
    if (level.isClientSide) {
      return InteractionResult.SUCCESS;
    }

    ServerLevel l = (ServerLevel) level;
    Optional<BlockPos> maybe = SitePlanner.containing(l, pos);
    if (maybe.isEmpty()) {
      p.sendSystemMessage(Component.literal("Tram offline: restore the reactor grid."));
      return InteractionResult.CONSUME;
    }

    BlockPos o = maybe.get();
    if (BlueprintRooms.active(l, o)) return BlueprintTram.terminal(l, o, pos, p);
    if (SitePlanner.progress(l, o).layoutVersion >= 2
        || l.getBlockState(o.offset(92, 2, 48)).is((Block) UndergroundContent.TRACK.get())) {
      return this.underground(l, o, pos, p);
    }

    if (!SitePlanner.progress(l, o).powered) {
      p.sendSystemMessage(Component.literal("Legacy tram offline: restore reactor grid."));
      return InteractionResult.CONSUME;
    }

    boolean wing4 = pos.getZ() < o.getZ() + 80;
    BlockPos start = o.offset(wing4 ? (Vec3i) ROUTE.get(0) : (Vec3i) ROUTE.get(ROUTE.size() - 1));
    List<SarcTramEntity> nearby =
        l.getEntitiesOfClass(SarcTramEntity.class, new AABB(start).inflate(4.0));
    SarcTramEntity cart = nearby.isEmpty() ? null : nearby.get(0);
    if (cart != null && !cart.getPassengers().isEmpty()) {
      p.sendSystemMessage(Component.literal("Tram occupied. One driver only."));
      return InteractionResult.CONSUME;
    }

    int slot = -1;
    boolean created = false;
    if (cart == null) {
      int i = 0;

      while (true) {
        if (i < p.getInventory().getContainerSize()) {
          ItemStack item = p.getInventory().getItem(i);
          if (!item.is((Item) Content.TRAM_KIT.get()) && !item.is(Items.MINECART)) {
            i++;
            continue;
          }

          slot = i;
        }

        if (slot < 0 && !p.getAbilities().instabuild) {
          p.sendSystemMessage(
              Component.literal(
                  "Bring a SARC tram deployment kit, or a minecart from Wing 6 for terminal assembly."));
          return InteractionResult.CONSUME;
        }

        cart = (SarcTramEntity) ((EntityType) Content.TRAM_VEHICLE.get()).create(l);
        if (cart == null) {
          return InteractionResult.FAIL;
        }

        cart.moveTo(start.getX() + 0.5, start.getY() + 0.1, start.getZ() + 0.5, 90.0F, 0.0F);
        if (!VehicleSafety.loaded(l, cart.getBoundingBox())
            || !l.noCollision(cart, cart.getBoundingBox())
            || !l.addFreshEntity(cart)) {
          return InteractionResult.FAIL;
        }

        created = true;
        break;
      }
    }

    cart.siteOrigin = o;
    if (!p.startRiding(cart, true)) {
      if (created) {
        cart.discard();
      }

      return InteractionResult.FAIL;
    } else {
      if (created && slot >= 0 && !p.getAbilities().instabuild) {
        p.getInventory().getItem(slot).shrink(1);
      }

      cart.setDeltaMovement(new Vec3(0.0, 0.0, -0.25));
      p.sendSystemMessage(
          Component.literal(
              wing4
                  ? "SARC tram departing for Wing 1. Sneak to exit when safe."
                  : "SARC tram departing for Reactor Ops. Sneak to exit when safe."));
      return InteractionResult.CONSUME;
    }
  }

  private InteractionResult underground(ServerLevel l, BlockPos o, BlockPos terminal, Player p) {
    int localStart = terminal.getZ() < o.getZ() + 80 ? 48 : 170;
    int dest = localStart == 48 ? 170 : 48;
    AABB route =
        new AABB(
            o.getX() + 87,
            o.getY() + 1,
            o.getZ() + 37,
            o.getX() + 100,
            o.getY() + 7,
            o.getZ() + 184);
    List<SarcTramEntity> trains =
        l.getEntitiesOfClass(SarcTramEntity.class, route, t -> o.equals(t.siteOrigin));
    SarcTramEntity cart = trains.isEmpty() ? null : trains.get(0);
    if (cart != null && !cart.getPassengers().isEmpty()) {
      p.sendSystemMessage(Component.literal("Tram occupied. Wait for its next arrival."));
      return InteractionResult.CONSUME;
    }

    if (cart != null && Math.abs(cart.getZ() - (o.getZ() + localStart + 0.5)) > 0.5) {
      double ox0 = cart.getX();
      double oy0 = cart.getY();
      double oz0 = cart.getZ();
      float yaw0 = cart.getYRot();
      cart.setYRot(90.0F);
      cart.setPos(o.getX() + 92.5, o.getY() + 2.1, o.getZ() + localStart + 0.5);
      cart.setDeltaMovement(Vec3.ZERO);
      if (!l.noCollision(cart, cart.getBoundingBox())) {
        cart.setYRot(yaw0);
        cart.setPos(ox0, oy0, oz0);
        cart.stationManaged = true;
        cart.stationTargetLocalZ = localStart;
        p.sendSystemMessage(
            Component.literal("Empty tram called to this station. Board after it stops."));
        return InteractionResult.CONSUME;
      }

      cart.stationTargetLocalZ = 0;
    }

    int slot = -1;
    boolean created = false;
    if (cart == null) {
      int i = 0;

      while (i < p.getInventory().getContainerSize()
          && !p.getInventory().getItem(i).is((Item) Content.TRAM_KIT.get())
          && !p.getInventory().getItem(i).is(Items.MINECART)) {
        i++;
      }

      cart = (SarcTramEntity) ((EntityType) Content.TRAM_VEHICLE.get()).create(l);
      if (cart == null) {
        return InteractionResult.FAIL;
      }

      cart.siteOrigin = o;
      cart.moveTo(o.getX() + 92.5, o.getY() + 2.1, o.getZ() + localStart + 0.5, 90.0F, 0.0F);
      if (!VehicleSafety.loaded(l, cart.getBoundingBox())
          || !l.noCollision(cart, cart.getBoundingBox())
          || !l.addFreshEntity(cart)) {
        p.sendSystemMessage(
            Component.literal("Tram bay blocked. Clear the track beside this platform."));
        return InteractionResult.CONSUME;
      }

      created = true;
      slot = -1;
    }

    if (!p.startRiding(cart, true)) {
      if (created) {
        cart.discard();
      }

      p.sendSystemMessage(Component.literal("Can't board from here."));
      return InteractionResult.CONSUME;
    } else {
      if (created && slot >= 0 && !p.getAbilities().instabuild) {
        p.getInventory().getItem(slot).shrink(1);
      }

      cart.stationManaged = true;
      cart.stationTargetLocalZ = dest;
      cart.setDeltaMovement(0.0, 0.0, dest > localStart ? 0.25 : -0.25);
      p.sendSystemMessage(
          Component.literal(
              dest == 170
                  ? "Tram departing for Reactor Station."
                  : "Tram departing for Portal Station."));
      return InteractionResult.CONSUME;
    }
  }
}
