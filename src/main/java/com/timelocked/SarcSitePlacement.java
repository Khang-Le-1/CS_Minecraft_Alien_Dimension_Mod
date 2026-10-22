package com.timelocked;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent.Post;

public final class SarcSitePlacement {
  private static final SarcSitePlacement.Piece[] PIECES =
      new SarcSitePlacement.Piece[] {
        new SarcSitePlacement.Piece("sarc/modules/x00_y00_z00", new BlockPos(0, 0, 0)),
        new SarcSitePlacement.Piece("sarc/modules/x00_y01_z00", new BlockPos(0, 32, 0)),
        new SarcSitePlacement.Piece("sarc/modules/x00_y00_z01", new BlockPos(0, 0, 32)),
        new SarcSitePlacement.Piece("sarc/modules/x00_y01_z01", new BlockPos(0, 32, 32)),
        new SarcSitePlacement.Piece("sarc/modules/x00_y00_z02", new BlockPos(0, 0, 64)),
        new SarcSitePlacement.Piece("sarc/modules/x00_y01_z02", new BlockPos(0, 32, 64)),
        new SarcSitePlacement.Piece("sarc/modules/x00_y00_z03", new BlockPos(0, 0, 96)),
        new SarcSitePlacement.Piece("sarc/modules/x00_y01_z03", new BlockPos(0, 32, 96)),
        new SarcSitePlacement.Piece("sarc/modules/x00_y00_z04", new BlockPos(0, 0, 128)),
        new SarcSitePlacement.Piece("sarc/modules/x00_y01_z04", new BlockPos(0, 32, 128)),
        new SarcSitePlacement.Piece("sarc/modules/x00_y00_z05", new BlockPos(0, 0, 160)),
        new SarcSitePlacement.Piece("sarc/modules/x00_y01_z05", new BlockPos(0, 32, 160)),
        new SarcSitePlacement.Piece("sarc/modules/x00_y00_z06", new BlockPos(0, 0, 192)),
        new SarcSitePlacement.Piece("sarc/modules/x00_y01_z06", new BlockPos(0, 32, 192)),
        new SarcSitePlacement.Piece("sarc/modules/x01_y00_z00", new BlockPos(32, 0, 0)),
        new SarcSitePlacement.Piece("sarc/modules/x01_y01_z00", new BlockPos(32, 32, 0)),
        new SarcSitePlacement.Piece("sarc/modules/x01_y00_z01", new BlockPos(32, 0, 32)),
        new SarcSitePlacement.Piece("sarc/modules/x01_y01_z01", new BlockPos(32, 32, 32)),
        new SarcSitePlacement.Piece("sarc/modules/x01_y00_z02", new BlockPos(32, 0, 64)),
        new SarcSitePlacement.Piece("sarc/modules/x01_y01_z02", new BlockPos(32, 32, 64)),
        new SarcSitePlacement.Piece("sarc/modules/x01_y00_z03", new BlockPos(32, 0, 96)),
        new SarcSitePlacement.Piece("sarc/modules/x01_y01_z03", new BlockPos(32, 32, 96)),
        new SarcSitePlacement.Piece("sarc/modules/x01_y00_z04", new BlockPos(32, 0, 128)),
        new SarcSitePlacement.Piece("sarc/modules/x01_y01_z04", new BlockPos(32, 32, 128)),
        new SarcSitePlacement.Piece("sarc/modules/x01_y00_z05", new BlockPos(32, 0, 160)),
        new SarcSitePlacement.Piece("sarc/modules/x01_y01_z05", new BlockPos(32, 32, 160)),
        new SarcSitePlacement.Piece("sarc/modules/x01_y00_z06", new BlockPos(32, 0, 192)),
        new SarcSitePlacement.Piece("sarc/modules/x01_y01_z06", new BlockPos(32, 32, 192)),
        new SarcSitePlacement.Piece("sarc/modules/x02_y00_z00", new BlockPos(64, 0, 0)),
        new SarcSitePlacement.Piece("sarc/modules/x02_y01_z00", new BlockPos(64, 32, 0)),
        new SarcSitePlacement.Piece("sarc/modules/x02_y00_z01", new BlockPos(64, 0, 32)),
        new SarcSitePlacement.Piece("sarc/modules/x02_y01_z01", new BlockPos(64, 32, 32)),
        new SarcSitePlacement.Piece("sarc/modules/x02_y00_z02", new BlockPos(64, 0, 64)),
        new SarcSitePlacement.Piece("sarc/modules/x02_y01_z02", new BlockPos(64, 32, 64)),
        new SarcSitePlacement.Piece("sarc/modules/x02_y00_z03", new BlockPos(64, 0, 96)),
        new SarcSitePlacement.Piece("sarc/modules/x02_y01_z03", new BlockPos(64, 32, 96)),
        new SarcSitePlacement.Piece("sarc/modules/x02_y00_z04", new BlockPos(64, 0, 128)),
        new SarcSitePlacement.Piece("sarc/modules/x02_y01_z04", new BlockPos(64, 32, 128)),
        new SarcSitePlacement.Piece("sarc/modules/x02_y00_z05", new BlockPos(64, 0, 160)),
        new SarcSitePlacement.Piece("sarc/modules/x02_y01_z05", new BlockPos(64, 32, 160)),
        new SarcSitePlacement.Piece("sarc/modules/x02_y00_z06", new BlockPos(64, 0, 192)),
        new SarcSitePlacement.Piece("sarc/modules/x02_y01_z06", new BlockPos(64, 32, 192)),
        new SarcSitePlacement.Piece("sarc/modules/x03_y00_z00", new BlockPos(96, 0, 0)),
        new SarcSitePlacement.Piece("sarc/modules/x03_y01_z00", new BlockPos(96, 32, 0)),
        new SarcSitePlacement.Piece("sarc/modules/x03_y00_z01", new BlockPos(96, 0, 32)),
        new SarcSitePlacement.Piece("sarc/modules/x03_y01_z01", new BlockPos(96, 32, 32)),
        new SarcSitePlacement.Piece("sarc/modules/x03_y00_z02", new BlockPos(96, 0, 64)),
        new SarcSitePlacement.Piece("sarc/modules/x03_y01_z02", new BlockPos(96, 32, 64)),
        new SarcSitePlacement.Piece("sarc/modules/x03_y00_z03", new BlockPos(96, 0, 96)),
        new SarcSitePlacement.Piece("sarc/modules/x03_y01_z03", new BlockPos(96, 32, 96)),
        new SarcSitePlacement.Piece("sarc/modules/x03_y00_z04", new BlockPos(96, 0, 128)),
        new SarcSitePlacement.Piece("sarc/modules/x03_y01_z04", new BlockPos(96, 32, 128)),
        new SarcSitePlacement.Piece("sarc/modules/x03_y00_z05", new BlockPos(96, 0, 160)),
        new SarcSitePlacement.Piece("sarc/modules/x03_y01_z05", new BlockPos(96, 32, 160)),
        new SarcSitePlacement.Piece("sarc/modules/x03_y00_z06", new BlockPos(96, 0, 192)),
        new SarcSitePlacement.Piece("sarc/modules/x03_y01_z06", new BlockPos(96, 32, 192)),
        new SarcSitePlacement.Piece("sarc/modules/x04_y00_z00", new BlockPos(128, 0, 0)),
        new SarcSitePlacement.Piece("sarc/modules/x04_y01_z00", new BlockPos(128, 32, 0)),
        new SarcSitePlacement.Piece("sarc/modules/x04_y00_z01", new BlockPos(128, 0, 32)),
        new SarcSitePlacement.Piece("sarc/modules/x04_y01_z01", new BlockPos(128, 32, 32)),
        new SarcSitePlacement.Piece("sarc/modules/x04_y00_z02", new BlockPos(128, 0, 64)),
        new SarcSitePlacement.Piece("sarc/modules/x04_y01_z02", new BlockPos(128, 32, 64)),
        new SarcSitePlacement.Piece("sarc/modules/x04_y00_z03", new BlockPos(128, 0, 96)),
        new SarcSitePlacement.Piece("sarc/modules/x04_y01_z03", new BlockPos(128, 32, 96)),
        new SarcSitePlacement.Piece("sarc/modules/x04_y00_z04", new BlockPos(128, 0, 128)),
        new SarcSitePlacement.Piece("sarc/modules/x04_y01_z04", new BlockPos(128, 32, 128)),
        new SarcSitePlacement.Piece("sarc/modules/x04_y00_z05", new BlockPos(128, 0, 160)),
        new SarcSitePlacement.Piece("sarc/modules/x04_y01_z05", new BlockPos(128, 32, 160)),
        new SarcSitePlacement.Piece("sarc/modules/x04_y00_z06", new BlockPos(128, 0, 192)),
        new SarcSitePlacement.Piece("sarc/modules/x04_y01_z06", new BlockPos(128, 32, 192))
      };

  private static final Piece[] BLUEPRINT = blueprintPieces();

  private static Piece[] blueprintPieces() {
    java.util.List<Piece> out = new java.util.ArrayList<>();
    for (int[] v : BlueprintModuleIndex.CELLS) {
      String path =
          String.format(Locale.ROOT, "sarc/blueprint/x%02d_y%02d_z%02d", v[0], v[1], v[2]);
      out.add(new Piece(path, new BlockPos(v[0] * 32, v[1] * 32, v[2] * 32)));
    }
    return out.toArray(Piece[]::new);
  }

  private static final Piece[] DRAWING = drawingPieces();

  private static Piece[] drawingPieces() {
    java.util.List<Piece> out = new java.util.ArrayList<>();
    for (int[] v : DrawingModuleIndex.CELLS)
      out.add(
          new Piece(
              String.format(Locale.ROOT, "sarc/drawing/x%02d_y%02d_z%02d", v[0], v[1], v[2]),
              new BlockPos(v[0] * 32, v[1] * 32, v[2] * 32)));
    return out.toArray(Piece[]::new);
  }

  private static Piece[] pieces(FacilityState d) {
    return d.placementGeometry == 5 ? DRAWING : d.placementGeometry == 4 ? BLUEPRINT : PIECES;
  }

  public static void register() {
    NeoForge.EVENT_BUS.addListener(SarcSitePlacement::commands);
    NeoForge.EVENT_BUS.addListener(SarcSitePlacement::tick);
  }

  private static void commands(RegisterCommandsEvent e) {
    e.getDispatcher()
        .register(
            (LiteralArgumentBuilder)
                ((LiteralArgumentBuilder)
                        ((LiteralArgumentBuilder)
                                ((LiteralArgumentBuilder)
                                        ((LiteralArgumentBuilder)
                                            ((LiteralArgumentBuilder)
                                                    ((LiteralArgumentBuilder)
                                                            ((LiteralArgumentBuilder)
                                                                ((LiteralArgumentBuilder)
                                                                        Commands.literal("sarcsite")
                                                                            .requires(
                                                                                s ->
                                                                                    s.hasPermission(
                                                                                        2)))
                                                                    .executes(
                                                                        c -> {
                                                                          ((CommandSourceStack)
                                                                                  c.getSource())
                                                                              .sendFailure(
                                                                                  Component.literal(
                                                                                      "BACK UP FIRST. TEST WORLD: excavates connected underground rooms within a 480x43x384 drawing area below/east/south of you; NEW PLACEMENT ONLY, keep clear of existing builds. /sarcsite confirm starts; /sarcsite status; /sarcsite cancel. Placement state is saved and resumes after restart."));
                                                                          return 0;
                                                                        })))
                                                        .then(
                                                            ((LiteralArgumentBuilder)
                                                                    Commands.literal(
                                                                            "repair-envelope")
                                                                        .executes(
                                                                            c -> {
                                                                              ((CommandSourceStack)
                                                                                      c.getSource())
                                                                                  .sendFailure(
                                                                                      Component
                                                                                          .literal(
                                                                                              "BACK UP FIRST. While inside the intended Overworld facility, use /sarcsite repair-envelope confirm. Fills missing main exterior/roof cells and retrofits emergency floor strips; no block deletion, inventory reset, or portal conversion."));
                                                                              return 0;
                                                                            }))
                                                                .then(
                                                                    Commands.literal("confirm")
                                                                        .executes(
                                                                            c -> {
                                                                              ServerLevel l =
                                                                                  ((CommandSourceStack)
                                                                                          c
                                                                                              .getSource())
                                                                                      .getLevel();
                                                                              if (!l.dimension()
                                                                                  .equals(
                                                                                      Level
                                                                                          .OVERWORLD)) {
                                                                                ((CommandSourceStack)
                                                                                        c
                                                                                            .getSource())
                                                                                    .sendFailure(
                                                                                        Component
                                                                                            .literal(
                                                                                                "Overworld facility only."));
                                                                                return 0;
                                                                              } else {
                                                                                Optional<BlockPos>
                                                                                    site =
                                                                                        SitePlanner
                                                                                            .containing(
                                                                                                l,
                                                                                                BlockPos
                                                                                                    .containing(
                                                                                                        ((CommandSourceStack)
                                                                                                                c
                                                                                                                    .getSource())
                                                                                                            .getPosition()));
                                                                                if (site
                                                                                    .isEmpty()) {
                                                                                  ((CommandSourceStack)
                                                                                          c
                                                                                              .getSource())
                                                                                      .sendFailure(
                                                                                          Component
                                                                                              .literal(
                                                                                                  "Stand inside the intended facility first."));
                                                                                  return 0;
                                                                                } else if (l.getBlockState(
                                                                                        site.get()
                                                                                            .offset(
                                                                                                110,
                                                                                                2,
                                                                                                184))
                                                                                    .is(
                                                                                        (Block)
                                                                                            UndergroundContent
                                                                                                .STARTUP
                                                                                                .get())) {
                                                                                  ((CommandSourceStack)
                                                                                          c
                                                                                              .getSource())
                                                                                      .sendFailure(
                                                                                          Component
                                                                                              .literal(
                                                                                                  "Legacy box-envelope repair is not compatible with this underground layout. No changes made."));
                                                                                  return 0;
                                                                                } else {
                                                                                  ((CommandSourceStack)
                                                                                          c
                                                                                              .getSource())
                                                                                      .sendFailure(
                                                                                          Component
                                                                                              .literal(
                                                                                                  "Legacy box-envelope repair is disabled in the underground rebuild. No blocks changed. Restore a backup or use the matching legacy release for legacy repair."));
                                                                                  return 0;
                                                                                }
                                                                              }
                                                                            }))))
                                                .then(
                                                    Commands.literal("satellite")
                                                        .executes(
                                                            c -> {
                                                              String msg =
                                                                  SatelliteCrash.start(
                                                                      ((CommandSourceStack)
                                                                              c.getSource())
                                                                          .getPlayerOrException(),
                                                                      true);
                                                              ((CommandSourceStack) c.getSource())
                                                                  .sendSuccess(
                                                                      () -> Component.literal(msg),
                                                                      true);
                                                              return 1;
                                                            }))))
                                    .then(
                                        Commands.literal("status")
                                            .executes(
                                                c -> {
                                                  FacilityState d =
                                                      FacilityState.get(
                                                          ((CommandSourceStack) c.getSource())
                                                              .getServer());
                                                  ((CommandSourceStack) c.getSource())
                                                      .sendSuccess(
                                                          () ->
                                                              Component.literal(
                                                                  d.pending
                                                                      ? "SITE-"
                                                                          + String.format(
                                                                              Locale.ROOT,
                                                                              "%03d",
                                                                              d.site)
                                                                          + " modules "
                                                                          + d.nextPiece
                                                                          + "/"
                                                                          + pieces(d).length
                                                                          + " at "
                                                                          + d.origin
                                                                      : "No active site build. Saved sites: "
                                                                          + d.sites.size()),
                                                          false);
                                                  return 1;
                                                })))
                            .then(
                                Commands.literal("cancel")
                                    .executes(
                                        c -> {
                                          FacilityState d =
                                              FacilityState.get(
                                                  ((CommandSourceStack) c.getSource()).getServer());
                                          d.pending = false;
                                          d.setDirty();
                                          ((CommandSourceStack) c.getSource())
                                              .sendSuccess(
                                                  () ->
                                                      Component.literal(
                                                          "Placement cancelled. Partial structure remains; restore a backup before retrying."),
                                                  false);
                                          return 1;
                                        })))
                    .then(
                        Commands.literal("confirm")
                            .executes(
                                c -> {
                                  FacilityState d =
                                      FacilityState.get(
                                          ((CommandSourceStack) c.getSource()).getServer());
                                  if (d.pending) {
                                    ((CommandSourceStack) c.getSource())
                                        .sendFailure(
                                            Component.literal(
                                                "A saved placement is already active."));
                                    return 0;
                                  }

                                  ServerPlayer p =
                                      ((CommandSourceStack) c.getSource()).getPlayerOrException();
                                  ServerLevel level = p.serverLevel();
                                  if (!level.dimension().equals(Level.OVERWORLD)) {
                                    ((CommandSourceStack) c.getSource())
                                        .sendFailure(
                                            Component.literal(
                                                "Facility placement is supported in the Overworld only."));
                                    return 0;
                                  }

                                  BlockPos o = p.blockPosition().offset(0, -29, 0);
                                  if (o.getY() >= level.getMinBuildHeight()
                                      && o.getY() + 43 <= level.getMaxBuildHeight()
                                      && level.getWorldBorder().isWithinBounds(o)
                                      && level
                                          .getWorldBorder()
                                          .isWithinBounds(o.offset(479, 0, 383))) {
                                    for (SarcSitePlacement.Piece piece : DRAWING) {
                                      if (level
                                          .getStructureManager()
                                          .get(
                                              ResourceLocation.fromNamespaceAndPath(
                                                  "timelocked", piece.path()))
                                          .isEmpty()) {
                                        ((CommandSourceStack) c.getSource())
                                            .sendFailure(
                                                Component.literal(
                                                    "Missing module: " + piece.path()));
                                        return 0;
                                      }
                                    }

                                    // Refuse overlap with any registered old or new site, including
                                    // partial jobs.
                                    var proposed =
                                        new net.minecraft.world.phys.AABB(
                                            o.getX(),
                                            o.getY(),
                                            o.getZ(),
                                            o.getX() + 480,
                                            o.getY() + 43,
                                            o.getZ() + 384);
                                    for (long key : d.sites.keySet()) {
                                      BlockPos old = BlockPos.of(key);
                                      var occupied =
                                          new net.minecraft.world.phys.AABB(
                                              old.getX(),
                                              old.getY(),
                                              old.getZ(),
                                              old.getX() + BlueprintRooms.width(level, old),
                                              old.getY() + 43,
                                              old.getZ() + BlueprintRooms.depth(level, old));
                                      if (proposed.intersects(occupied)) {
                                        ((CommandSourceStack) c.getSource())
                                            .sendFailure(
                                                Component.literal(
                                                    "Refused: new blueprint overlaps a registered facility. Choose a fresh location."));
                                        return 0;
                                      }
                                    }
                                    d.placementGeometry = BlueprintRooms.VERSION;
                                    SitePlanner.progress(level, o).geometryVersion =
                                        BlueprintRooms.VERSION;
                                    d.site = d.allocate(o);
                                    d.origin = o;
                                    d.operator = p.getUUID();
                                    d.nextPiece = 0;
                                    d.pending = true;
                                    d.setDirty();
                                    ((CommandSourceStack) c.getSource())
                                        .sendSuccess(
                                            () ->
                                                Component.literal(
                                                    "Queued one module per second. Backup warning acknowledged. Saved build resumes after restart."),
                                            false);
                                    return 1;
                                  } else {
                                    ((CommandSourceStack) c.getSource())
                                        .sendFailure(
                                            Component.literal(
                                                "Structure exceeds build height or world border."));
                                    return 0;
                                  }
                                })));
  }

  private static void message(ServerLevel l, FacilityState d, String s) {
    if (d.operator != null) {
      ServerPlayer p = l.getServer().getPlayerList().getPlayer(d.operator);
      if (p != null) {
        p.sendSystemMessage(Component.literal(s));
      }
    }
  }

  private static void tick(Post event) {
    if (event.getServer().getTickCount() % 20 == 0) {
      ServerLevel l = event.getServer().overworld();
      if (l != null) {
        FacilityState d = FacilityState.get(event.getServer());
        if (d.pending) {
          try {
            if (d.nextPiece >= pieces(d).length) {
              finish(l, d);
              return;
            }

            SarcSitePlacement.Piece piece = pieces(d)[d.nextPiece];
            StructureTemplate t =
                (StructureTemplate)
                    l.getStructureManager()
                        .get(ResourceLocation.fromNamespaceAndPath("timelocked", piece.path()))
                        .orElseThrow();
            BlockPos at = d.origin.offset(piece.offset());

            for (int x = at.getX() >> 4; x <= at.getX() + t.getSize().getX() - 1 >> 4; x++) {
              for (int z = at.getZ() >> 4; z <= at.getZ() + t.getSize().getZ() - 1 >> 4; z++) {
                l.getChunk(x, z);
              }
            }

            if (!t.placeInWorld(
                l,
                at,
                at,
                new StructurePlaceSettings()
                    .setIgnoreEntities(true)
                    .setKnownShape(true)
                    .setLiquidSettings(
                        net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings
                            .IGNORE_WATERLOGGING),
                RandomSource.create(l.getSeed() ^ at.asLong()),
                50)) {
              throw new IllegalStateException("Template placement failed");
            }

            d.nextPiece++;
            d.setDirty();
            if (d.nextPiece % 4 == 0) {
              message(l, d, "SARC modules: " + d.nextPiece + " / " + pieces(d).length);
            }

            if (d.nextPiece == pieces(d).length) {
              finish(l, d);
            }
          } catch (Exception ex) {
            d.pending = false;
            d.setDirty();
            message(
                l,
                d,
                "Build stopped: "
                    + ex.getMessage()
                    + ". Partial structure remains; restore backup before retrying.");
          }
        }
      }
    }
  }

  public static void finish(ServerLevel l, FacilityState d) {
    if (BlueprintRooms.drawing(l, d.origin)) {
      TransitionDoorways.installNew(l, d.origin);
      SarcAmbientLighting.installNew(l, d.origin);
    }
    BlockPos plate = BlueprintRooms.at(l, d.origin, 76, 31, 197);
    l.setBlock(plate, MonitorContent.wall(Direction.SOUTH), 2);
    if (l.getBlockEntity(plate) instanceof MonitorContent.MonitorEntity screen) {
      screen.setTheme(0);
      screen.setLines(CampaignFeature.siteLines(d.site));
    }

    if (l.getBlockEntity(BlueprintRooms.at(l, d.origin, 88, 8, 36))
        instanceof PortalControllerEntity be) {
      be.bind(d.origin);
    }

    SitePlanner.progress(l, d.origin).layoutVersion = 3;
    SitePlanner.progress(l, d.origin);
    d.pending = false;
    d.setDirty();
    message(
        l,
        d,
        String.format(
            Locale.ROOT,
            "SARC SITE-%03d complete. Facility offline. Storage → emergency tram → Reactor integral three-slot fuel port → Reactor CR startup → Portal Fluid room → airlock → control hallway → Observation / Portal airlock.",
            d.site));
  }

  private SarcSitePlacement() {}

  private record Piece(String path, BlockPos offset) {}
}
