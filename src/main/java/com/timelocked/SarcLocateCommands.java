package com.timelocked;

import java.util.Comparator;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Adds SARC literals beneath vanilla /locate structure; its registry argument, biome and poi are
 * retained.
 */
@EventBusSubscriber(modid = "timelocked")
public final class SarcLocateCommands {
  @SubscribeEvent
  public static void commands(RegisterCommandsEvent e) {
    var root = Commands.literal("locate").requires(s -> s.hasPermission(2));
    var structures = Commands.literal("structure");
    for (int type = 0; type < LocatorNames.IDS.length; type++) {
      final int kind = type;
      structures.then(
          Commands.literal(LocatorNames.IDS[type]).executes(c -> locate(c.getSource(), kind)));
    }
    root.then(structures);
    e.getDispatcher().register(root);
  }

  private static int locate(CommandSourceStack source, int type) {
    if (!source.getLevel().dimension().equals(Level.OVERWORLD)) {
      source.sendFailure(
          Component.literal("SARC buildings are in the Overworld. Run this command there."));
      return 0;
    }
    ServerLevel l = source.getLevel();
    BlockPos here = BlockPos.containing(source.getPosition());
    Optional<BlockPos> result;
    if (type == 0)
      result =
          SitePlanner.nearby(l, here).stream()
              .min(Comparator.comparingDouble(p -> distance(p, here)))
              .map(
                  p -> {
                    var known = FacilityState.get(l.getServer()).progress.get(p.asLong());
                    int version = known == null ? 5 : known.geometryVersion;
                    return p.offset(
                        version == 5 ? 90 : version == 4 ? 44 : 64,
                        29,
                        version == 5 ? 359 : version == 4 ? 258 : 188);
                  });
    else if (type == 3) result = SarcLaunchSites.nearest(l, here);
    else {
      int wanted = type == 1 ? SarcOutposts.POWER : SarcOutposts.LAB;
      BlockPos best = null;
      double nearest = Double.POSITIVE_INFINITY;
      int rx = Math.floorDiv(here.getX(), SarcOutposts.CELL),
          rz = Math.floorDiv(here.getZ(), SarcOutposts.CELL);
      // Bounded planner search only, never generate or load a target chunk to locate it.
      for (int x = rx - 4; x <= rx + 4; x++)
        for (int z = rz - 4; z <= rz + 4; z++) {
          if (SarcOutposts.kind(l.getSeed(), x, z) != wanted) continue;
          var plan = SarcOutposts.plan(l, x, z);
          if (plan.isPresent()) {
            var outpost = plan.get();
            if (wanted == SarcOutposts.POWER
                && PowerStationState.get(l).wasDenied(outpost.origin())) continue;
            BlockPos p =
                wanted == SarcOutposts.POWER
                    ? PowerStationLayout.entrance(l, outpost.origin())
                    : outpost.origin();
            double d = distance(p, here);
            if (d < nearest) {
              nearest = d;
              best = p;
            }
          }
        }
      result = Optional.ofNullable(best);
    }
    if (result.isEmpty()) {
      source.sendFailure(
          Component.literal(
              "No suitable planned "
                  + LocatorNames.IDS[type]
                  + " found within the bounded nearby search."));
      return 0;
    }
    BlockPos p = result.get();
    int blocks = (int) Math.sqrt(distance(p, here));
    source.sendSuccess(
        () ->
            Component.literal(
                "Nearest planned "
                    + LocatorNames.IDS[type]
                    + ": ["
                    + p.getX()
                    + ", "
                    + p.getY()
                    + ", "
                    + p.getZ()
                    + "] ("
                    + blocks
                    + " blocks horizontally). Natural buildings generate in new chunks; this command does not build them."),
        false);
    return 1;
  }

  private static double distance(BlockPos a, BlockPos b) {
    double dx = (double) a.getX() - b.getX(), dz = (double) a.getZ() - b.getZ();
    return dx * dx + dz * dz;
  }

  private SarcLocateCommands() {}
}
