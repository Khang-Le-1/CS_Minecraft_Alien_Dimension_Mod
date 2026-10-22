package com.timelocked;

import java.util.Locale;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/** Runs at TOP_LAYER_MODIFICATION so later springs/geodes cannot overwrite sealed rooms. */
public final class CampaignFeature extends Feature<NoneFeatureConfiguration> {
  public CampaignFeature() {
    super(NoneFeatureConfiguration.CODEC);
  }

  public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> c) {
    WorldGenLevel region = c.level();
    if (!region.getLevel().dimension().equals(Level.OVERWORLD)) {
      return false;
    }

    int chunkX = c.origin().getX() & -16;
    int chunkZ = c.origin().getZ() & -16;
    int rx = Math.floorDiv(chunkX, 4096);
    int rz = Math.floorDiv(chunkZ, 4096);
    boolean changed = false;

    for (int x = rx - 1; x <= rx + 1; x++) {
      for (int z = rz - 1; z <= rz + 1; z++) {
        BlockPos h = SitePlanner.horizontal(region.getSeed(), x, z);
        if (chunkX + 15 >= h.getX() - 514
            && chunkX <= h.getX() + 1536
            && chunkZ + 15 >= h.getZ() - 514
            && chunkZ <= h.getZ() + 2944) {
          BlockPos o = SitePlanner.origin(region.getLevel(), h);
          int ox = o.getX();
          int oz = o.getZ();
          if (chunkX + 15 >= ox - 22
              && chunkX <= ox + 560
              && chunkZ + 15 >= oz - 2
              && chunkZ <= oz + 2390) {
            int code = SitePlanner.designation(region.getSeed(), o);
            // The reception was moved (+26, 0, +171) in the room blueprint.
            // Translate its pad, parking and entire approach together, not the underground origin.
            BlockPos surfaceOrigin = o.offset(26, 0, 171);
            changed |= FacilityTerrain.prepareChunk(region, surfaceOrigin, chunkX, chunkZ);
            if (chunkX <= ox + BlueprintRooms.WIDTH - 1
                && chunkX + 15 >= ox
                && chunkZ <= oz + BlueprintRooms.DEPTH - 1
                && chunkZ + 15 >= oz) {
              BoundingBox box =
                  new BoundingBox(
                      chunkX,
                      region.getMinBuildHeight(),
                      chunkZ,
                      chunkX + 15,
                      region.getMaxBuildHeight() - 1,
                      chunkZ + 15);

              // Share the exact sparse module index with /sarcsite: no legacy 5x7 grid.
              for (int[] cell : DrawingModuleIndex.CELLS) {
                int ix = cell[0], iy = cell[1], iz = cell[2];
                BlockPos at = o.offset(ix * 32, iy * 32, iz * 32);
                int sx = Math.min(32, BlueprintRooms.WIDTH - ix * 32);
                int sz = Math.min(32, BlueprintRooms.DEPTH - iz * 32);
                if (at.getX() <= chunkX + 15
                    && at.getX() + sx - 1 >= chunkX
                    && at.getZ() <= chunkZ + 15
                    && at.getZ() + sz - 1 >= chunkZ) {
                  String id =
                      String.format(Locale.ROOT, "sarc/drawing/x%02d_y%02d_z%02d", ix, iy, iz);
                  StructureTemplate template =
                      region
                          .getLevel()
                          .getStructureManager()
                          .get(ResourceLocation.fromNamespaceAndPath("timelocked", id))
                          .orElseThrow(
                              () ->
                                  new IllegalStateException(
                                      "Missing natural blueprint module: " + id));
                  template.placeInWorld(
                      region,
                      at,
                      at,
                      new StructurePlaceSettings()
                          .setBoundingBox(box)
                          .setIgnoreEntities(true)
                          .setKnownShape(true)
                          .setLiquidSettings(
                              net.minecraft.world.level.levelgen.structure.templatesystem
                                  .LiquidSettings.IGNORE_WATERLOGGING),
                      c.random(),
                      50);
                }
              }

              TransitionDoorways.placeChunk(region, o, chunkX, chunkZ);
              SarcAmbientLighting.placeChunk(region, o, chunkX, chunkZ);

              // SavedData is server-thread-owned; do not mutate it on worldgen workers.
              var level = region.getLevel();
              level
                  .getServer()
                  .execute(
                      () -> {
                        var state = FacilityState.get(level.getServer());
                        // Never re-label a registered legacy site's machinery as the new layout.
                        var known = state.progress.get(o.asLong());
                        if (state.sites.containsKey(o.asLong())
                            && (known == null || known.geometryVersion != BlueprintRooms.VERSION))
                          return;
                        state.sites.putIfAbsent(o.asLong(), code);
                        SitePlanner.progress(level, o).geometryVersion = BlueprintRooms.VERSION;
                        SitePlanner.progress(level, o).layoutVersion = 3;
                        state.setDirty();
                      });

              BlockPos sign = surfaceOrigin.offset(76, 31, 197);
              if (box.isInside(sign)) {
                region.setBlock(sign, MonitorContent.wall(Direction.SOUTH), 2);
              }

              if (box.isInside(sign)
                  && region.getBlockEntity(sign) instanceof MonitorContent.MonitorEntity be) {
                writeSiteNumber(be, code);
              }

              changed = true;
            }

            int entryX = surfaceOrigin.getX() + 64;
            int entryZ = surfaceOrigin.getZ() + 188;
            changed |= landmark(c, "parking_lot", surfaceOrigin.offset(36, 28, 208), 60, 48);
            FacilityTerrain.finishChunk(region, surfaceOrigin, chunkX, chunkZ);
            changed |= ApproachRoad.generate(c, surfaceOrigin);
            changed |= IntroCoast.generate(c, surfaceOrigin);
            if (chunkX <= entryX - 6
                && chunkX + 15 >= entryX - 13
                && chunkZ <= entryZ + 501
                && chunkZ + 15 >= entryZ + 499) {
              int roadY = ApproachRoad.profile(c, surfaceOrigin).height(entryZ + 500) + 1;
              ApproachRoad.signPocket(c, entryX, entryZ, roadY - 1);
              changed |=
                  landmark(
                      c, "road_sign", new BlockPos(entryX - 13, roadY - 1, entryZ + 499), 8, 3);

              for (int col = 2; col <= 4; col++) {
                BlockPos digit = new BlockPos(entryX - 12 + col, roadY + 4, entryZ + 500);
                if (digit.getX() >= chunkX && digit.getX() <= chunkX + 15) {
                  int value = col == 2 ? code / 100 : (col == 3 ? code / 10 % 10 : code % 10);
                  region.setBlock(
                      digit,
                      (BlockState)
                          ((BlockState)
                                  ((SarcHighwayPanelBlock) Content.HIGHWAY.get())
                                      .defaultBlockState()
                                      .setValue(SarcHighwayPanelBlock.FACING, Direction.SOUTH))
                              .setValue(SarcHighwayPanelBlock.PART, 18 + value),
                      2);
                }
              }
            }

            for (int house = 0; house < 6; house++) {
              int hx = entryX + 12 + house % 3 * 20;
              int hz = entryZ + 1470 + house / 3 * 24;
              if (chunkX <= hx + 11
                  && chunkX + 15 >= hx
                  && chunkZ <= hz + 10
                  && chunkZ + 15 >= hz) {
                int hy =
                    c.chunkGenerator()
                            .getBaseHeight(
                                hx + 6,
                                hz + 5,
                                Types.WORLD_SURFACE_WG,
                                region,
                                region.getLevel().getChunkSource().randomState())
                        - 1;
                changed |= landmark(c, "house_" + house, new BlockPos(hx, hy, hz), 12, 11);
                BlockPos loot = new BlockPos(hx + 2, hy + 1, hz + 2);
                if (loot.getX() >= chunkX
                    && loot.getX() <= chunkX + 15
                    && loot.getZ() >= chunkZ
                    && loot.getZ() <= chunkZ + 15
                    && region.getBlockEntity(loot) instanceof BarrelBlockEntity be) {
                  be.setLootTable(
                      ResourceKey.create(
                          Registries.LOOT_TABLE,
                          ResourceLocation.fromNamespaceAndPath(
                              "timelocked", "chests/abandoned_town")),
                      region.getSeed() ^ loot.asLong() ^ house);
                }
              }
            }
          }
        }
      }
    }

    return changed;
  }

  static void writeSiteNumber(MonitorContent.MonitorEntity be, int code) {
    be.write(0, siteLines(code));
    be.setChanged();
  }

  public static String[] siteLines(int code) {
    return new String[] {
      "SARC FACILITY",
      String.format(Locale.ROOT, "SITE-%03d", code),
      "Zero Point",
      "Authorized only"
    };
  }

  private static boolean landmark(
      FeaturePlaceContext<NoneFeatureConfiguration> c,
      String name,
      BlockPos at,
      int width,
      int depth) {
    WorldGenLevel region = c.level();
    int x = c.origin().getX() & -16;
    int z = c.origin().getZ() & -16;
    if (at.getX() <= x + 15
        && at.getX() + width - 1 >= x
        && at.getZ() <= z + 15
        && at.getZ() + depth - 1 >= z) {
      Optional<StructureTemplate> template =
          region
              .getLevel()
              .getStructureManager()
              .get(ResourceLocation.fromNamespaceAndPath("timelocked", "sarc/approach/" + name));
      if (template.isEmpty()) {
        return false;
      }

      BoundingBox box =
          new BoundingBox(
              x, region.getMinBuildHeight(), z, x + 15, region.getMaxBuildHeight() - 1, z + 15);
      return template
          .get()
          .placeInWorld(
              region,
              at,
              at,
              new StructurePlaceSettings()
                  .setBoundingBox(box)
                  .setIgnoreEntities(true)
                  .setKnownShape(true)
                  .setLiquidSettings(
                      net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings
                          .IGNORE_WATERLOGGING),
              c.random(),
              50);
    } else {
      return false;
    }
  }
}
