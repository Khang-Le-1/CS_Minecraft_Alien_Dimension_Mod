package com.timelocked;

import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.loot.LootTable;

public final class TimeskipperShip {
  public static final int NOSE = -26;
  public static final int TAIL = 22;
  public static final BlockPos AIRLOCK_IN = new BlockPos(4, 1, -2);
  public static final BlockPos NAV = new BlockPos(0, 1, -19);
  public static final BlockPos DRIVE = new BlockPos(0, 2, 17);
  public static final BlockPos BRIDGE_SCREEN = new BlockPos(-2, 1, -18);
  public static final BlockPos ENGINE_SCREEN = new BlockPos(3, 1, 15);
  public static final BlockPos MODEL_ANCHOR = new BlockPos(0, 8, 0);
  public static final ResourceKey<LootTable> SUPPLY = key("chests/space_supply");
  public static final ResourceKey<LootTable> GALLEY = key("chests/timeskipper_galley");
  static final String[] LOG =
      new String[] {
        "CAPTAIN'S LOG\n\nEntry 1. SARC calls her the TIMESKIPPER. The chronotite drive does not fly. It skips. One moment you are here, the next you are there, and a little time falls out of the gap.",
        "Entry 4. Luna, Mars, the Belt, Europa. Every jump costs chronotite. Every jump loses Earth an hour or two. The crew stopped wearing watches.",
        "Entry 9. Halcyon picked up a signal from a world 1,200 light years out. The nav computer calls it AURELIA. It should not be reachable. The drive says it is.",
        "Entry 12. Ground control wants us docked at the launch site until the Zero Point incident is under control. I left the link codes in the cargo bay. Whoever finds her: be kind to the old girl."
      };

  public static TimeskipperShip.Sink of(final ServerLevel level) {
    return new TimeskipperShip.Sink() {
      @Override
      public void set(int x, int y, int z, BlockState state) {
        level.setBlock(new BlockPos(x, y, z), state, 2);
      }

      @Override
      public void barrel(int x, int y, int z, ResourceKey<LootTable> loot, ItemStack extra) {
        BlockPos pos = new BlockPos(x, y, z);
        this.set(
            x,
            y,
            z,
            (BlockState)
                Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP));
        if (level.getBlockEntity(pos) instanceof BarrelBlockEntity barrel) {
          if (loot != null) {
            barrel.setLootTable(loot, level.random.nextLong());
          }

          if (extra != null) {
            barrel.setItem(13, extra);
          }
        }
      }

      @Override
      public void screen(int x, int y, int z, Direction face, int theme, String... lines) {
        BlockPos pos = new BlockPos(x, y, z);
        this.set(x, y, z, MonitorContent.stand(face));
        if (level.getBlockEntity(pos) instanceof MonitorContent.MonitorEntity monitor) {
          monitor.setTheme(theme);
          monitor.setLines(lines);
        }
      }
    };
  }

  static ResourceKey<LootTable> key(String path) {
    return ResourceKey.create(
        Registries.LOOT_TABLE, ResourceLocation.fromNamespaceAndPath("timelocked", path));
  }

  static int halfW(int z) {
    return z < -12 ? Math.max(1, (int) Math.ceil(7 * (z + 27) / 15.0)) : 7;
  }

  static double halfH(int z) {
    return z < -12 ? Math.max(2.2, 4.5 * (z + 27) / 15.0) : 4.5;
  }

  static double se(double x, double y, double w, double h) {
    return Math.pow(Math.abs(x) / w, 4.0) + Math.pow(Math.abs(y - 2.5) / h, 4.0);
  }

  public static boolean outer(int x, int y, int z) {
    return z >= -26 && z <= 22 && se(x, y, halfW(z) + 0.5, halfH(z) + 0.5) <= 1.0;
  }

  public static boolean cavity(int x, int y, int z) {
    return z > -24 && z < 22 && y >= 1 && se(x, y, halfW(z) - 0.5, halfH(z) - 0.5) <= 1.0;
  }

  static BlockState st(Block block) {
    return block.defaultBlockState();
  }

  static BlockState face(Block block, Direction direction) {
    BlockState state = block.defaultBlockState();
    return state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
        ? (BlockState) state.setValue(BlockStateProperties.HORIZONTAL_FACING, direction)
        : state;
  }

  private static BlockState stair(Block block, Direction direction) {
    return (BlockState) st(block).setValue(StairBlock.FACING, direction);
  }

  public static void hull(TimeskipperShip.Sink out, int X, int Y, int Z, boolean real) {
    legacyHull(out, X, Y, Z, real);
    TimeskipperInteriorStyle.hull(out, X, Y, Z);
    out.set(
        X,
        Y + 8,
        Z,
        SpaceContent.MODEL_ANCHOR
            .get()
            .defaultBlockState()
            .setValue(ShipModelAnchorBlock.NEW_LAYOUT, true));
  }

  static void legacyHull(TimeskipperShip.Sink out, int X, int Y, int Z, boolean real) {
    BlockState white = st(Blocks.SMOOTH_QUARTZ);
    BlockState ceramic = st(Blocks.WHITE_CONCRETE);
    BlockState gray = st(Blocks.LIGHT_GRAY_CONCRETE);
    BlockState purple = st(Blocks.PURPLE_CONCRETE);
    BlockState belly = st((Block) MetalContent.HULL.get());
    BlockState glass = real ? st((Block) MetalContent.VIEWPORT.get()) : st(Blocks.TINTED_GLASS);
    BlockState air = st(Blocks.AIR);
    BlockState floor = st((Block) Content.FLOOR.get());

    for (int z = -26; z <= 22; z++) {
      for (int x = -8; x <= 8; x++) {
        for (int y = -3; y <= 8; y++) {
          if (outer(x, y, z)) {
            if (cavity(x, y, z)) {
              out.set(X + x, Y + y, Z + z, air);
            } else if (y == 0 && cavity(x, 1, z)) {
              out.set(X + x, Y + y, Z + z, floor);
            } else {
              boolean noseGlass = z <= -22 && y >= 3 && Math.abs(x) <= Math.max(1, (z + 27) / 2);
              boolean sideGlass =
                  z >= -13 && z <= 10 && y == 3 && Math.abs(x) >= 6 && Math.floorMod(z + 1, 4) == 0;
              BlockState skin;
              if (noseGlass || sideGlass) {
                skin = glass;
              } else if (y <= -1) {
                skin = belly;
              } else if (y >= 6) {
                skin = Math.abs(x) <= 1 && Math.floorMod(z + 26, 8) < 3 ? purple : gray;
              } else if (y == 2 && Math.abs(x) >= 5 && z >= -14 && z <= 14) {
                skin = purple;
              } else if (Math.abs(x) >= 5 && Math.floorMod(z + 26, 9) == 0) {
                skin = st((Block) MetalContent.TITANIUM_PANEL.get());
              } else {
                skin = Math.abs(x) < 5 && y > 1 ? white : ceramic;
              }

              out.set(X + x, Y + y, Z + z, skin);
            }
          }
        }
      }
    }

    // The modeled exterior owns wings, nacelles, vanes and struts now. Keep
    // only the walkable fuselage and its hidden collision in the block shell.

    for (int z = -16; z <= 12; z += 4) {
      if (outer(0, 7, z)) {
        out.set(X, Y + 7, Z + z, z % 8 == 0 ? purple : gray);
      }

      out.set(X - 7, Y + 1, Z + z, st(Blocks.SEA_LANTERN));
      out.set(X + 7, Y + 1, Z + z, st(Blocks.SEA_LANTERN));
    }

    for (int s : new int[] {-1, 1}) {
      for (int z : new int[] {-7, 0, 7, 14}) {
        out.set(X + s * 6, Y - 1, Z + z, st(Blocks.IRON_TRAPDOOR));
        out.set(X + s * 6, Y + 4, Z + z, st(Blocks.LIGHT_GRAY_CONCRETE));
      }
    }

    out.set(
        X + MODEL_ANCHOR.getX(),
        Y + MODEL_ANCHOR.getY(),
        Z + MODEL_ANCHOR.getZ(),
        st((Block) SpaceContent.MODEL_ANCHOR.get()));
  }

  private static void wings(TimeskipperShip.Sink out, int X, int Y, int Z) {
    BlockState top = st(Blocks.WHITE_CONCRETE);
    BlockState edge = st(Blocks.LIGHT_GRAY_CONCRETE);
    BlockState accent = st(Blocks.PURPLE_CONCRETE);

    for (int z = -5; z <= 14; z++) {
      int tip = z <= 8 ? Math.min(17, 8 + (z + 5) * 3 / 4) : Math.max(8, 17 - (z - 8) * 2);

      for (int side : new int[] {-1, 1}) {
        for (int x = 7; x <= tip; x++) {
          for (int y = 1; y <= 3; y++) {
            if (side != 1 || x < 8 || x > 10 || z < -3 || z > -1) {
              BlockState state =
                  y == 3 ? top : (y == 1 ? st((Block) MetalContent.HULL.get()) : edge);
              if (x == tip || (x + z + side & 7) == 0) {
                state = accent;
              }

              out.set(X + side * x, Y + y, Z + z, state);
            }
          }
        }
      }

      for (int side : new int[] {-1, 1}) {
        if (z == 8 || z == 13) {
          out.set(X + side * tip, Y + 4, Z + z, st(Blocks.SEA_LANTERN));
        }
      }
    }

    for (int side : new int[] {-1, 1}) {
      for (int x = 8; x <= 12; x++) {
        int y = 3 + (x - 8) / 2;
        out.set(X + side * x, Y + y, Z + 10, edge);
        out.set(X + side * x, Y + y, Z + 14, accent);
      }
    }
  }

  private static void nacelle(TimeskipperShip.Sink out, int X, int Y, int Z, int side) {
    int cx = side * 13;
    int cy = 2;

    for (int z = 7; z <= 21; z++) {
      for (int dx = -2; dx <= 2; dx++) {
        for (int dy = -2; dy <= 2; dy++) {
          int radius = dx * dx + dy * dy;
          if (radius <= 5) {
            BlockState state =
                radius >= 4
                    ? st(Blocks.LIGHT_GRAY_CONCRETE)
                    : (dy < 0 ? st((Block) MetalContent.HULL.get()) : st(Blocks.SMOOTH_QUARTZ));
            if (z == 10 || z == 15 || z == 19) {
              state =
                  radius >= 3 ? st(Blocks.PURPLE_CONCRETE) : st(Blocks.POLISHED_BLACKSTONE_BRICKS);
            }

            if (z == 21) {
              state =
                  radius >= 3 ? st(Blocks.POLISHED_BLACKSTONE_BRICKS) : st(Blocks.DEEPSLATE_TILES);
            }

            out.set(X + cx + dx, Y + cy + dy, Z + z, state);
          }
        }
      }
    }

    for (int z = 22; z <= 24; z++) {
      for (int dx = -2; dx <= 2; dx++) {
        for (int dy = -2; dy <= 2; dy++) {
          int radius = dx * dx + dy * dy;
          if (radius <= 5) {
            BlockState state =
                radius >= 4 ? st(Blocks.PURPLE_CONCRETE) : st(Blocks.POLISHED_BLACKSTONE_BRICKS);
            if (z == 24 && radius <= 1) {
              state = st(Blocks.SEA_LANTERN);
            }

            if (z == 23 && radius <= 1) {
              state = st(Blocks.LIGHT_BLUE_STAINED_GLASS);
            }

            out.set(X + cx + dx, Y + cy + dy, Z + z, state);
          }
        }
      }
    }

    for (int z : new int[] {9, 12, 16, 20}) {
      for (int dx = -1; dx <= 1; dx++) {
        for (int dy = -1; dy <= 1; dy++) {
          if (dx * dx + dy * dy <= 2) {
            out.set(X + cx + dx, Y + cy + dy, Z + z, st(Blocks.COPPER_BLOCK));
          }
        }
      }
    }
  }

  private static void tailFin(TimeskipperShip.Sink out, int X, int Y, int Z, int side) {
    BlockState ceramic = st(Blocks.SMOOTH_QUARTZ);
    BlockState accent = st(Blocks.PURPLE_CONCRETE);
    BlockState frame = st((Block) MetalContent.HULL.get());

    for (int z = 10; z <= 19; z++) {
      int top = 10 - Math.abs(z - 14) / 2;
      int x = side * (15 + Math.max(0, Math.abs(z - 14) - 2) / 3);

      for (int y = 4; y <= top; y++) {
        out.set(X + x, Y + y, Z + z, y != top && z != 10 && z != 19 ? ceramic : accent);
        if (y == 5) {
          out.set(X + x - side, Y + y, Z + z, frame);
        }
      }
    }

    out.set(X + side * 16, Y + 9, Z + 14, st(Blocks.SEA_LANTERN));
  }

  private static void landingGear(TimeskipperShip.Sink out, int X, int Y, int Z, int side) {
    BlockState strut = st(Blocks.IRON_BLOCK);
    BlockState foot = st(Blocks.SMOOTH_STONE_SLAB);

    for (int z : new int[] {-5, 9}) {
      int x = side * 5;

      for (int y = -3; y <= -1; y++) {
        out.set(X + x, Y + y, Z + z, y == -1 ? strut : st(Blocks.IRON_BARS));
      }

      out.set(X + x, Y - 3, Z + z, st(Blocks.CHAIN));

      for (int dx = -1; dx <= 1; dx++) {
        for (int dz = -1; dz <= 1; dz++) {
          out.set(
              X + x + dx,
              Y - 4,
              Z + z + dz,
              Math.abs(dx) + Math.abs(dz) == 2 ? st(Blocks.POLISHED_BLACKSTONE_BRICK_SLAB) : foot);
        }
      }

      out.set(X + x, Y - 3, Z + z, st(Blocks.IRON_BLOCK));
    }
  }

  private static void centralTail(TimeskipperShip.Sink out, int X, int Y, int Z) {
    for (int r = 2; r >= 0; r--) {
      for (int dx = -r; dx <= r; dx++) {
        for (int dy = -r; dy <= r; dy++) {
          if (dx * dx + dy * dy <= r * r + 1) {
            out.set(
                X + dx,
                Y + 2 + dy,
                Z + 23 + r,
                r == 2
                    ? st(Blocks.POLISHED_BLACKSTONE_BRICKS)
                    : (r == 1 ? st(Blocks.PURPLE_CONCRETE) : st(Blocks.SEA_LANTERN)));
          }
        }
      }
    }

    for (int z = 15; z <= 20; z++) {
      out.set(X, Y + 8, Z + z, st(Blocks.IRON_BARS));
    }

    out.set(X, Y + 9, Z + 18, st(Blocks.SEA_LANTERN));
  }

  private static void wallX(
      TimeskipperShip.Sink out, int X, int Y, int Z, int x, int z0, int z1, int doorZ) {
    for (int z = z0; z <= z1; z++) {
      for (int y = 1; y <= 5; y++) {
        if (cavity(x, y, z)) {
          out.set(
              X + x,
              Y + y,
              Z + z,
              z == doorZ && y <= 3 ? st(Blocks.AIR) : st((Block) Content.WALL.get()));
        }
      }
    }
  }

  private static void wallZ(
      TimeskipperShip.Sink out, int X, int Y, int Z, int z, int x0, int x1, int doorX) {
    for (int x = x0; x <= x1; x++) {
      for (int y = 1; y <= 5; y++) {
        if (cavity(x, y, z)) {
          out.set(
              X + x,
              Y + y,
              Z + z,
              x == doorX && y <= 3 ? st(Blocks.AIR) : st((Block) Content.WALL.get()));
        }
      }
    }
  }

  private static void sideRoomDivider(
      TimeskipperShip.Sink out, int X, int Y, int Z, int z, int minX, int maxX, int doorX) {
    for (int x = minX; x <= maxX; x++) {
      for (int y = 1; y <= 5; y++) {
        if (cavity(x, y, z)) {
          out.set(
              X + x,
              Y + y,
              Z + z,
              x == doorX && y <= 3
                  ? st(Blocks.AIR)
                  : st((Block) MetalContent.TITANIUM_PANEL.get()));
        }
      }
    }
  }

  static void bed(TimeskipperShip.Sink out, int x, int y, int z, Direction toward, Block bed) {
    BlockState state = (BlockState) bed.defaultBlockState().setValue(BedBlock.FACING, toward);
    out.set(x, y, z, (BlockState) state.setValue(BedBlock.PART, BedPart.FOOT));
    out.set(
        x + toward.getStepX(),
        y,
        z + toward.getStepZ(),
        (BlockState) state.setValue(BedBlock.PART, BedPart.HEAD));
  }

  private static void chair(TimeskipperShip.Sink out, int x, int y, int z, Direction facing) {
    out.set(x, y, z, stair(Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, facing));
    Direction back = facing.getOpposite();
    out.set(x + back.getStepX(), y, z + back.getStepZ(), st(Blocks.POLISHED_BLACKSTONE_BRICK_WALL));
  }

  public static void build(ServerLevel level, BlockPos origin) {
    int minX = Math.floorDiv(origin.getX() - 20, 16);
    int maxX = Math.floorDiv(origin.getX() + 20, 16);
    int minZ = Math.floorDiv(origin.getZ() + -26 - 2, 16);
    int maxZ = Math.floorDiv(origin.getZ() + 22 + 4, 16);

    for (int cx = minX; cx <= maxX; cx++) {
      for (int cz = minZ; cz <= maxZ; cz++) {
        level.getChunk(cx, cz);
      }
    }

    TimeskipperShip.Sink out = of(level);
    int X = origin.getX();
    int Y = origin.getY();
    int Z = origin.getZ();
    hull(out, X, Y, Z, true);
    interior(out, X, Y, Z);
    TimeskipperRefit.furnish(out, X, Y, Z);
    TimeskipperDecks.finish(out, X, Y, Z);
    TimeskipperBridgeScreens.fit(level, origin);
    TimeskipperBigScreens.fit(level, origin);
    TimeskipperDecks.initialize(level, origin);
  }

  static void interior(TimeskipperShip.Sink out, int X, int Y, int Z) {
    legacyInterior(out, X, Y, Z);
    TimeskipperInteriorStyle.interior(out, X, Y, Z);
    TimeskipperDecks.finish(out, X, Y, Z);
  }

  static void legacyInterior(TimeskipperShip.Sink out, int X, int Y, int Z) {
    BlockState light = st((Block) Content.LIGHT.get());
    BlockState hazard = st((Block) Content.HAZARD.get());
    BlockState wall = st((Block) Content.WALL.get());
    BlockState conduit = st((Block) Content.CONDUIT.get());
    wallZ(out, X, Y, Z, -12, -7, 7, 0);
    wallZ(out, X, Y, Z, 13, -7, 7, 0);

    for (int side : new int[] {-1, 1}) {
      wallX(out, X, Y, Z, side * 2, -11, -6, -8);
      wallX(out, X, Y, Z, side * 2, -4, 3, 0);
      wallX(out, X, Y, Z, side * 2, 5, 11, 8);
      sideRoomDivider(out, X, Y, Z, -5, side < 0 ? -7 : 3, side < 0 ? -3 : 7, side * 4);
      sideRoomDivider(out, X, Y, Z, 4, side < 0 ? -7 : 3, side < 0 ? -3 : 7, side * 4);
    }

    for (int z = -22; z <= 20; z += 4) {
      for (int x = -6; x <= 6; x++) {
        if (cavity(x, 5, z)) {
          out.set(
              X + x,
              Y + 5,
              Z + z,
              Math.abs(x) == 6
                  ? st(Blocks.POLISHED_BLACKSTONE_BRICKS)
                  : st((Block) MetalContent.TITANIUM_PANEL.get()));
        }
      }

      out.set(X, Y + 4, Z + z, light);

      for (int side : new int[] {-1, 1}) {
        if (cavity(side * 4, 4, z)) {
          out.set(X + side * 4, Y + 4, Z + z, st(Blocks.SEA_LANTERN));
        }

        if (cavity(side * 3, 3, z)) {
          out.set(X + side * 3, Y + 3, Z + z, conduit);
        }
      }
    }

    for (int z = -19; z <= 17; z += 8) {
      for (int side : new int[] {-1, 1}) {
        out.set(X + side * 2, Y, Z + z, hazard);
      }
    }

    out.set(
        X + NAV.getX(),
        Y + NAV.getY(),
        Z + NAV.getZ(),
        face((Block) SpaceContent.NAV.get(), Direction.SOUTH));
    out.screen(
        X + BRIDGE_SCREEN.getX(),
        Y + BRIDGE_SCREEN.getY(),
        Z + BRIDGE_SCREEN.getZ(),
        Direction.SOUTH,
        0,
        "TIMESKIPPER",
        "LOCATION: EARTH",
        "DRIVE: READY",
        "SARC FLIGHT SYSTEMS");
    out.screen(
        X + 2,
        Y + 1,
        Z - 18,
        Direction.SOUTH,
        1,
        "NAV CONSOLE",
        "RIGHT-CLICK FOR",
        "STAR MAP",
        "JUMP CHARGES: 3");
    out.screen(
        X - 5,
        Y + 1,
        Z - 17,
        Direction.EAST,
        0,
        "HULL STATUS",
        "PRESSURE: NOMINAL",
        "SHIELDS: STANDBY",
        "");
    out.screen(
        X + 5,
        Y + 1,
        Z - 17,
        Direction.WEST,
        1,
        "FLIGHT SYSTEMS",
        "AUTOPILOT: READY",
        "CREW: ABOARD",
        "");
    chair(out, X, Y + 1, Z - 15, Direction.NORTH);
    chair(out, X - 3, Y + 1, Z - 15, Direction.NORTH);
    chair(out, X + 3, Y + 1, Z - 15, Direction.NORTH);
    out.set(X - 4, Y + 1, Z - 14, face((Block) FacilityDetails.WORKSTATION.get(), Direction.EAST));
    out.set(X + 4, Y + 1, Z - 14, face((Block) FacilityDetails.WORKSTATION.get(), Direction.WEST));

    for (int x = -3; x <= 3; x++) {
      out.set(X + x, Y, Z - 20, hazard);
    }

    out.barrel(
        X - 5,
        Y + 1,
        Z - 13,
        null,
        SarcOutposts.book("TIMESKIPPER Captain's Log", "Cmdr. A. Reyes", LOG));
    bed(out, X - 4, Y + 1, Z - 10, Direction.WEST, Blocks.PURPLE_BED);
    bed(out, X - 4, Y + 1, Z - 7, Direction.WEST, Blocks.PURPLE_BED);
    out.barrel(X - 6, Y + 1, Z - 9, SUPPLY, null);
    out.screen(
        X - 5,
        Y + 1,
        Z - 6,
        Direction.EAST,
        0,
        "CREW QUARTERS",
        "BUNKS: 2",
        "LIFE SUPPORT: OK",
        "");
    out.set(X - 6, Y + 1, Z - 6, light);
    out.set(X + 4, Y + 1, Z - 10, st(Blocks.BREWING_STAND));
    out.set(X + 5, Y + 1, Z - 10, st(Blocks.CAULDRON));
    bed(out, X + 4, Y + 1, Z - 7, Direction.EAST, Blocks.WHITE_BED);
    out.barrel(X + 6, Y + 1, Z - 9, SarcOutposts.LAB_LOOT, null);
    out.screen(
        X + 5,
        Y + 1,
        Z - 6,
        Direction.WEST,
        1,
        "MEDBAY",
        "O2: NOMINAL",
        "MEDICAL SUPPLIES",
        "CHECKED");
    out.set(X - 5, Y + 1, Z - 2, face(Blocks.SMOKER, Direction.EAST));
    out.barrel(X - 5, Y + 1, Z, GALLEY, null);
    out.set(X - 5, Y + 1, Z + 2, st(Blocks.SMOOTH_STONE_SLAB));
    chair(out, X - 4, Y + 1, Z + 1, Direction.WEST);
    out.set(X + 7, Y + 1, Z - 2, face((Block) SpaceContent.AIRLOCK.get(), Direction.WEST));
    out.set(X + 6, Y + 1, Z + 2, st((Block) SpaceContent.LIFE_SUPPORT.get()));
    out.screen(
        X + 5,
        Y + 1,
        Z - 3,
        Direction.SOUTH,
        1,
        "AIRLOCK",
        "CLICK HATCH TO",
        "DISEMBARK",
        "LIFE SUPPORT: ON");

    for (int z = 6; z <= 10; z += 2) {
      out.barrel(X - 6, Y + 1, Z + z, SUPPLY, null);
      out.barrel(X + 6, Y + 1, Z + z, SUPPLY, null);
    }

    out.set(X - 5, Y + 1, Z + 11, st((Block) MetalContent.CRATE.get()));
    out.set(X + 5, Y + 1, Z + 11, st((Block) MetalContent.CRATE.get()));
    out.set(X - 4, Y + 1, Z + 11, st((Block) MetalContent.BENCH.get()));
    out.screen(
        X - 3,
        Y + 1,
        Z + 9,
        Direction.SOUTH,
        0,
        "CARGO MANIFEST",
        "SAMPLES: SECURED",
        "SUPPLIES: READY",
        "");

    for (int x = -1; x <= 1; x++) {
      for (int z = 16; z <= 18; z++) {
        for (int y = 1; y <= 4; y++) {
          if ((x != 0 || z != 17) && (x != 0 || z != 16 || y > 3)) {
            out.set(X + x, Y + y, Z + z, st(Blocks.LIGHT_BLUE_STAINED_GLASS));
          }
        }
      }
    }

    out.set(X, Y + 1, Z + 17, st(Blocks.IRON_BLOCK));
    out.set(
        X + DRIVE.getX(), Y + DRIVE.getY(), Z + DRIVE.getZ(), st((Block) SpaceContent.DRIVE.get()));
    out.set(X, Y + 3, Z + 17, st(Blocks.IRON_BLOCK));

    for (int y = 4; y <= 5; y++) {
      out.set(X, Y + y, Z + 17, st(Blocks.CHAIN));
    }

    for (int side : new int[] {-1, 1}) {
      for (int x = 2; x <= 6; x++) {
        out.set(X + side * x, Y + 1, Z + 17, conduit);
        if (x % 2 == 0) {
          out.set(X + side * x, Y + 2, Z + 17, st(Blocks.COPPER_BLOCK));
        }
      }
    }

    out.screen(
        X + ENGINE_SCREEN.getX(),
        Y + ENGINE_SCREEN.getY(),
        Z + ENGINE_SCREEN.getZ(),
        Direction.NORTH,
        0,
        "CHRONOTITE DRIVE",
        "CHARGES: 3",
        "FEED CHRONOTITE",
        "CONTAINMENT: STABLE");
    out.screen(
        X - 3,
        Y + 1,
        Z + 15,
        Direction.NORTH,
        1,
        "ENGINEERING",
        "COOLANT: NOMINAL",
        "CORE TEMP: STABLE",
        "");
  }

  public static void updateScreens(ServerLevel ship, Timeskipper.Data data) {
    BlockPos origin = Timeskipper.ORIGIN;
    var status = TimeskipperBridgeScreens.status(ship, origin);
    if (status instanceof MonitorContent.MonitorEntity monitor) {
      monitor.setLines(
          "TIMESKIPPER",
          "AT: " + data.planet().title.toUpperCase(Locale.ROOT),
          "CHARGES: " + data.charges + "/32",
          data.planet().atmosphere ? "ATMOSPHERE: YES" : "ATMOSPHERE: NONE");
    }

    if (TimeskipperBridgeScreens.engine(ship, origin)
        instanceof MonitorContent.MonitorEntity monitor) {
      monitor.setTheme(data.charges < 2 ? 2 : 0);
      monitor.setLines(
          "CHRONOTITE DRIVE",
          "CHARGES: " + data.charges + "/32",
          "FEED CHRONOTITE",
          data.charges < 2 ? "CHARGE LOW" : "READY");
    }
  }

  private TimeskipperShip() {}

  public interface Sink {
    void set(int var1, int var2, int var3, BlockState var4);

    default void barrel(int x, int y, int z, ResourceKey<LootTable> loot, ItemStack extra) {
      this.set(x, y, z, Blocks.BARREL.defaultBlockState());
    }

    default void screen(int x, int y, int z, Direction face, int theme, String... lines) {
      this.set(x, y, z, MonitorContent.stand(face));
    }
  }
}
