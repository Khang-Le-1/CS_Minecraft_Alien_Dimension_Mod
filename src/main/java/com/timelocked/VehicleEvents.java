package com.timelocked;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityMountEvent;

public final class VehicleEvents {
  public static void register() {
    NeoForge.EVENT_BUS.addListener(VehicleEvents::commands);
    NeoForge.EVENT_BUS.addListener(
        (EntityMountEvent e) -> {
          if (e.isDismounting()
              && e.getEntityBeingMounted() instanceof SarcLiftEntity lift
              && e.getEntityMounting() instanceof LivingEntity rider
              && rider.isAlive()
              && !rider.isRemoved()
              && lift.safeExit(rider).isEmpty()) {
            e.setCanceled(true);
            if (!rider.level().isClientSide && rider.tickCount % 20 == 0) {
              rider.sendSystemMessage(
                  Component.literal(
                      "Lift has no safe landing exit. Wait for docking or clear the landing."));
            }
          }
        });
    NeoForge.EVENT_BUS.addListener(
        (EntityMountEvent e) -> {
          if (e.isDismounting()
              && (e.getEntityBeingMounted() instanceof SarcGroundVehicle
                  || e.getEntityBeingMounted() instanceof SarcTramEntity)
              && e.getEntityMounting() instanceof LivingEntity rider
              && rider.isAlive()
              && !rider.isRemoved()
              && VehicleSafety.exit(e.getEntityBeingMounted(), rider).isEmpty()) {
            e.setCanceled(true);
            if (!rider.level().isClientSide && rider.tickCount % 20 == 0) {
              rider.sendSystemMessage(
                  Component.literal(
                      "No safe vehicle exit. Clear adjacent supported space, then sneak again."));
            }
          }
        });
    NeoForge.EVENT_BUS.addListener(
        (EntityMountEvent e) -> {
          if (e.getEntity().getVehicle() instanceof SarcGroundVehicle v) {
            v.stopControls();
          }
        });
  }

  private static void commands(RegisterCommandsEvent e) {
    e.getDispatcher()
        .register(
            (LiteralArgumentBuilder)
                ((LiteralArgumentBuilder)
                        Commands.literal("tram")
                            .executes(c -> tram((CommandSourceStack) c.getSource(), false)))
                    .then(
                        Commands.literal("board")
                            .executes(c -> tram((CommandSourceStack) c.getSource(), true))));
  }

  private static int tram(CommandSourceStack src, boolean board) throws CommandSyntaxException {
    ServerPlayer p = src.getPlayerOrException();
    List<SarcTramEntity> list =
        p.level().getEntitiesOfClass(SarcTramEntity.class, p.getBoundingBox().inflate(96.0));
    if (list.isEmpty()) {
      Optional<BlockPos> site =
          p.level() instanceof ServerLevel sl
              ? SitePlanner.containing(sl, p.blockPosition())
              : Optional.empty();
      src.sendFailure(
          Component.literal(
              site.isPresent()
                  ? "No tram within 96 blocks. Use the tram terminal on either platform to dispatch one."
                  : "No tram within 96 blocks."));
      return 0;
    }

    SarcTramEntity t =
        list.stream()
            .min(
                Comparator.comparingDouble(
                    x -> x.getBoundingBox().distanceToSqr(p.getEyePosition())))
            .get();
    double d = Math.sqrt(t.getBoundingBox().distanceToSqr(p.getEyePosition()));
    String local =
        t.siteOrigin == null
            ? ""
            : String.format(
                Locale.ROOT,
                " local=(%.1f, %.1f, %.1f)",
                t.getX() - t.siteOrigin.getX(),
                t.getY() - t.siteOrigin.getY(),
                t.getZ() - t.siteOrigin.getZ());
    String info =
        String.format(
            Locale.ROOT,
            "Tram #%d at (%.1f, %.1f, %.1f)%s, %.1f blocks away, riders=%d, station=%s target=%d%s",
            t.getId(),
            t.getX(),
            t.getY(),
            t.getZ(),
            local,
            d,
            t.getPassengers().size(),
            t.stationManaged,
            t.stationTargetLocalZ,
            t.debugStop.isEmpty() ? "" : " blocked: " + t.debugStop);
    if (!board) {
      src.sendSuccess(() -> Component.literal(info), false);
      return 1;
    }

    if (d > 6.0) {
      src.sendFailure(Component.literal("Too far to board. " + info));
      return 0;
    }

    if (p.isPassenger()) {
      p.stopRiding();
    }

    InteractionResult r = t.interact(p, InteractionHand.MAIN_HAND);
    if (p.getVehicle() != t) {
      src.sendFailure(Component.literal("Boarding failed (" + r + "). " + info));
    }

    return p.getVehicle() == t ? 1 : 0;
  }

  private VehicleEvents() {}
}
