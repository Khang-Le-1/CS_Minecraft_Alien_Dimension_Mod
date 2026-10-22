package com.timelocked;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Authored coastal opening, emitted only into the owning NEW Overworld chunk. A graded service
 * boardwalk meets the existing SARC approach; no runtime terrain edits.
 *
 * <p>Layout coordinates are blocks relative to the approach road: +X heads toward the sea, +Z runs
 * along the shore. Parking bounds and stripe offsets describe an authored layout, not random tuning
 * values. Keep the native coast audit in sync when changing its geometry.
 */
public final class IntroCoast {
  // Saved underground site -> authored surface anchor, in blocks. Keep wake coordinates stable.
  private static final int SURFACE_OFFSET_X = 26, SURFACE_OFFSET_Z = 171;
  private static final int ROAD_CENTER_X = 64, COAST_JUNCTION_Y = 65, COAST_JUNCTION_Z = 2140;
  private static final int WAKE_OFFSET_X = 280;

  /** Surface facility coordinates are offset from the underground blueprint's saved site anchor. */
  public static BlockPos surfaceOrigin(BlockPos site) {
    return site.offset(SURFACE_OFFSET_X, 0, SURFACE_OFFSET_Z);
  }

  /**
   * Fixed coastal road junction. Only its coastal Y is fixed; the inland road is terrain graded.
   */
  public static BlockPos roadJunction(BlockPos site) {
    BlockPos surface = surfaceOrigin(site);
    return new BlockPos(
        surface.getX() + ROAD_CENTER_X, COAST_JUNCTION_Y, surface.getZ() + COAST_JUNCTION_Z);
  }

  public static BlockPos wake(BlockPos site) {
    return roadJunction(site).offset(WAKE_OFFSET_X, 0, 0);
  }

  public static boolean generate(
      FeaturePlaceContext<NoneFeatureConfiguration> c, BlockPos surfaceOrigin) {
    int cx = c.origin().getX() & -16, cz = c.origin().getZ() & -16;
    int road = surfaceOrigin.getX() + 64, z0 = surfaceOrigin.getZ() + 2140;
    if (cx + 15 < road + 3 || cx > road + 360 || cz + 15 < z0 - 72 || cz > z0 + 72) return false;
    WorldGenLevel l = c.level();
    int roadY = ApproachRoad.profile(c, surfaceOrigin).height(z0);
    for (int x = Math.max(cx, road + 3); x <= Math.min(cx + 15, road + 360); x++)
      for (int z = Math.max(cz, z0 - 72); z <= Math.min(cz + 15, z0 + 72); z++) {
        int dx = x - road, dz = z - z0;
        // A three-wide, gradual boardwalk, at most one block of rise per column.
        boolean path = Math.abs(dz) <= 1 && dx <= 278;
        boolean lot = dx >= 250 && dx <= 276 && dz >= -42 && dz <= -10;
        boolean drive = dx >= 3 && dx <= 264 && Math.abs(dz + 28) <= 3;
        boolean walk = dx >= 273 && dx <= 276 && dz >= -10 && dz <= -2;
        if (dx < 248 && !path && !drive) continue;
        int floor =
            path
                ? (int)
                    Math.round(roadY + (64 - roadY) * Math.min(1, Math.max(0, (dx - 4) / 268.0)))
                : 64;
        if (drive)
          floor =
              (int)
                  Math.round(
                      ApproachRoad.profile(c, surfaceOrigin).height(z)
                          + (64 - ApproachRoad.profile(c, surfaceOrigin).height(z))
                              * Math.clamp((dx - 3) / 247.0, 0, 1));
        boolean water = dx >= 300 + (int) (6 * Math.sin(dz * .055));
        if (!path && water) floor = 59;
        int surface = l.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
        for (int y = floor + 1; y <= Math.max(68, surface + 2); y++)
          set(
              l,
              x,
              y,
              z,
              water && y <= 63 ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState());
        for (int y = floor - 3; y < floor; y++)
          set(l, x, y, z, Blocks.SANDSTONE.defaultBlockState());
        set(
            l,
            x,
            floor,
            z,
            path
                ? Blocks.SPRUCE_PLANKS.defaultBlockState()
                : (Math.floorMod(x * 31 + z * 17, 19) == 0 ? Blocks.GRAVEL : Blocks.SAND)
                    .defaultBlockState());
        if (drive) set(l, x, floor, z, Blocks.GRAY_CONCRETE.defaultBlockState());
        if (lot || walk) {
          BlockState pavement = Blocks.GRAY_CONCRETE.defaultBlockState();
          if (lot) {
            pavement = Blocks.BLACK_CONCRETE.defaultBlockState();
            boolean edge = dx == 250 || dx == 276 || dz == -42 || dz == -10;
            boolean stripe = dx <= 259 || dx >= 268;
            if (edge) pavement = Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();
            else if (stripe && Math.floorMod(dz + 41, 6) == 0)
              pavement = Blocks.WHITE_CONCRETE.defaultBlockState();
            else if ((dx <= 259 || dx >= 268) && dz >= -16)
              pavement = Blocks.BLUE_CONCRETE.defaultBlockState();
            if (drive) pavement = Blocks.GRAY_CONCRETE.defaultBlockState();
            // Continuous sidewalk on the boardwalk side of the lot.
            if (dx >= 273) pavement = Blocks.SMOOTH_STONE.defaultBlockState();
          }
          set(l, x, 64, z, pavement);
          if (walk) set(l, x, 64, z, Blocks.SMOOTH_STONE.defaultBlockState());
          if (lot && (dx == 251 || dx == 271) && (dz == -41 || dz == -11)) {
            for (int y = 65; y <= 67; y++) set(l, x, y, z, Blocks.IRON_BARS.defaultBlockState());
            set(l, x, 68, z, Blocks.SEA_LANTERN.defaultBlockState());
          }
        }
        if (path && dz == 0 && dx % 24 == 0 && ((z + 2) & -16) == cz) {
          // Rail-side posts sit outside the clear walking envelope.
          set(l, x, floor, z + 2, Blocks.SPRUCE_PLANKS.defaultBlockState());
          set(l, x, floor + 1, z + 2, Blocks.SPRUCE_FENCE.defaultBlockState());
        }
      }
    BlockPos wake = wake(surfaceOrigin.offset(-26, 0, -171));
    if ((wake.getX() & -16) == cx && (wake.getZ() & -16) == cz) {
      l.setBlock(wake.below(2), Blocks.SEA_LANTERN.defaultBlockState(), 2);
      l.setBlock(wake.below(), Blocks.SAND.defaultBlockState(), 2);
    }
    panel(l, wake.offset(-3, 0, -5), cx, cz, CoastSigns.RESTRICTION.get());
    panel(l, wake.offset(-6, 0, 3), cx, cz, CoastSigns.DIRECTION.get());
    return true;
  }

  private static void panel(WorldGenLevel l, BlockPos at, int cx, int cz, CoastSigns.Panel panel) {
    if ((at.getX() & -16) == cx && (at.getZ() & -16) == cz) {
      var state =
          panel
              .defaultBlockState()
              .setValue(CoastSigns.Panel.FACING, net.minecraft.core.Direction.EAST);
      l.setBlock(at, state, 2);
      l.setBlock(
          at.above(),
          state.setValue(
              CoastSigns.Panel.HALF,
              net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER),
          2);
    }
  }

  private static void set(WorldGenLevel l, int x, int y, int z, BlockState b) {
    if (y >= l.getMinBuildHeight() && y < l.getMaxBuildHeight())
      l.setBlock(new BlockPos(x, y, z), b, 2);
  }

  private IntroCoast() {}
}
