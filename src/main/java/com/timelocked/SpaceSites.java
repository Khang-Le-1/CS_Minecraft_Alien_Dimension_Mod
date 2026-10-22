package com.timelocked;

import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap.Types;

public final class SpaceSites {
  static final String[][] LOGS =
      new String[][] {
        new String[0],
        {
          "HALCYON LOG\n\nThe array caught something from deep space. Not radio. Something that sounds like it is singing backwards.",
          "If you fall off the station you will not die in the void. You will burn up over Earth instead. We tested this with Gary. Gary is fine. Mostly."
        },
        {
          "TRANQUILITY LOG\n\nRegolith samples show chronotite traces. The Moon is older than it should be. Or younger. The clocks disagree.",
          "Jump higher than you think. Land softer than you fear. Low gravity makes everyone brave.",
          "Day 9. Lanterns on the regolith to the northeast, leading to a glass dome near X 112, Z -96. They were not there yesterday. The air near them is breathable. That is not possible.",
          "Day 10. Something with very big eyes waved at me from the dome. I waved back. Requesting guidance."
        },
        {
          "ARES RELAY LOG\n\nDust storm day 14. The rover still runs. The antenna still listens. Something under the canyon floor still answers.",
          "Mars rock has more chromite and ardanium than any mine on Earth. SARC wants all of it."
        },
        {
          "MINING CLAIM 7\n\nPlatinum, osmium, iridite. Even neutronium, if you dig deep enough into the big rocks.",
          "Fall off the edge and you come back down from the top. Nobody here can explain why. We stopped asking."
        },
        {
          "DEEP ICE LOG\n\nThe shaft goes through 14 meters of ice. Below that: an ocean. Below that: warm vents and something that glows.",
          "Do not swim too far from the ladder. It is very dark down there."
        },
        new String[0]
      };

  static BlockState st(Block b) {
    return b.defaultBlockState();
  }

  public static BlockPos build(ServerLevel l, SpacePlanets.Planet p) {
    for (int cx = -3; cx <= 2; cx++) {
      for (int cz = -4; cz <= 2; cz++) {
        l.getChunk(cx, cz);
      }
    }

    TimeskipperShip.Sink o = TimeskipperShip.of(l);
    if (p == SpacePlanets.Planet.ORBIT) {
      station(o, 0, 120, 0);
      return new BlockPos(2, 121, 0);
    }

    BlockState ground;
    int y;
    if (p == SpacePlanets.Planet.BELT) {
      y = 128;
      ground = st((Block) SpaceContent.ASTEROID_ROCK.get());
      asteroid(o, 0, y, 0);
    } else {
      y = l.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, 0, 0) - 1;
      ground = ground(p);
      y = Math.max(y, p == SpacePlanets.Planet.AURELIA ? 62 : 20);
    }

    pad(o, l, 0, y, 0, ground, p);
    if (p != SpacePlanets.Planet.AURELIA) {
      hab(o, l, 0, y, -16, ground, p);
    }

    switch (p) {
      case LUNA:
        EclipticCourt.build(l, y);

        for (int h = 1; h <= 5; h++) {
          o.set(7, y + h, -6, st(Blocks.IRON_BARS));
        }

        for (int a = 0; a < 3; a++) {
          for (int b = 0; b < 2; b++) {
            o.set(
                8 + a,
                y + 4 + b,
                -6,
                st((a + b) % 2 == 0 ? Blocks.PURPLE_WOOL : Blocks.WHITE_WOOL));
          }
        }

        rover(l, 9, y + 1, 4);
        break;
      case MARS:
        for (int h = 1; h <= 14; h++) {
          o.set(-9, y + h, 4, st(h % 4 == 0 ? Blocks.IRON_BLOCK : Blocks.IRON_BARS));
        }

        o.set(-9, y + 15, 4, st(Blocks.LIGHTNING_ROD));

        for (int a = -2; a <= 2; a++) {
          o.set(-9 + a, y + 12, 4, st(Blocks.IRON_BARS));
        }

        rover(l, 9, y + 1, 4);
        break;
      case EUROPA:
        for (int x = 11; x <= 13; x++) {
          for (int z = -1; z <= 1; z++) {
            for (int h = y + 3; h >= y - 14 - 3; h--) {
              boolean water = !l.getFluidState(new BlockPos(x, h, z)).isEmpty();
              if (water) {
                break;
              }

              o.set(x, h, z, st(Blocks.AIR));
            }
          }
        }

        for (int h = y; h >= y - 14 - 2; h--) {
          o.set(12, h, -2, st(Blocks.PACKED_ICE));
          o.set(
              12,
              h,
              -1,
              (BlockState) st(Blocks.LADDER).setValue(LadderBlock.FACING, Direction.SOUTH));
        }

        for (int x = 10; x <= 14; x++) {
          for (int z = -2; z <= 2; z++) {
            if (x == 10 || x == 14 || z == -2 || z == 2) {
              o.set(x, y + 1, z, st(Blocks.IRON_BARS));
            }
          }
        }

        o.set(12, y + 1, -2, st(Blocks.AIR));
        break;
      case BELT:
        for (int h = 1; h <= 4; h++) {
          o.set(-8, y + h, 5, st(Blocks.IRON_BLOCK));
        }

        o.set(-8, y + 5, 5, st(Blocks.ANVIL));

        for (int h = 0; h <= 6; h++) {
          o.set(-8, y - h, 5, st(Blocks.AIR));
        }
        break;
      case AURELIA:
        BlockState hull = st(Blocks.WHITE_CONCRETE);

        for (int i = 0; i < 9; i++) {
          o.set(-10 + i, y + 1 + i / 3, 8 + i / 2, i % 3 == 0 ? st(Blocks.IRON_BLOCK) : hull);
          o.set(-10 + i, y + 1 + i / 3, 9 + i / 2, hull);
        }

        o.set(-2, y + 4, 13, st(Blocks.SOUL_CAMPFIRE));
        o.screen(
            -6,
            y + 1,
            4,
            Direction.SOUTH,
            3,
            "SARC PROBE AU-1",
            "SIGNAL SOURCE:",
            "BELOW THE SURFACE",
            "AIR: BREATHABLE");
    }

    return new BlockPos(3, y + 1, 0);
  }

  public static BlockState ground(SpacePlanets.Planet p) {
    return switch (p) {
      case LUNA -> st((Block) SpaceContent.LUNAR_ROCK.get());
      case MARS -> st((Block) SpaceContent.MARS_ROCK.get());
      case EUROPA -> st(Blocks.PACKED_ICE);
      case BELT -> st((Block) SpaceContent.ASTEROID_ROCK.get());
      default -> st((Block) Content.ALIEN_LOAM.get());
    };
  }

  static void pad(
      TimeskipperShip.Sink o,
      ServerLevel l,
      int X,
      int Y,
      int Z,
      BlockState ground,
      SpacePlanets.Planet p) {
    for (int x = X - 8; x <= X + 8; x++) {
      for (int z = Z - 8; z <= Z + 8; z++) {
        for (int y = Y + 1; y <= Y + 14; y++) {
          o.set(x, y, z, st(Blocks.AIR));
        }

        for (int y = Y - 1; y >= Y - 10; y--) {
          BlockPos q = new BlockPos(x, y, z);
          if (!l.getBlockState(q).isAir() && l.getFluidState(q).isEmpty()) {
            break;
          }

          o.set(x, y, z, ground);
        }

        boolean edge = Math.abs(x - X) == 7 || Math.abs(z - Z) == 7;
        if (Math.abs(x - X) <= 7 && Math.abs(z - Z) <= 7) {
          o.set(x, Y, z, edge ? st((Block) Content.HAZARD.get()) : st((Block) Content.FLOOR.get()));
        }
      }
    }

    for (int a = -1; a <= 1; a += 2) {
      for (int b = -1; b <= 1; b += 2) {
        o.set(X + a * 7, Y + 1, Z + b * 7, st((Block) Content.LIGHT.get()));
      }
    }

    o.set(X, Y + 1, Z, st((Block) SpaceContent.BEACON.get()));
    o.set(X + 5, Y + 1, Z + 5, st((Block) SpaceContent.LIFE_SUPPORT.get()));
    o.screen(
        X - 3,
        Y + 1,
        Z - 5,
        Direction.SOUTH,
        0,
        p.title.toUpperCase(Locale.ROOT),
        p.site,
        "BEACON: BOARD SHIP",
        p.atmosphere ? "AIR: BREATHABLE" : "VACUUM: SUIT UP");
  }

  static void hab(
      TimeskipperShip.Sink o,
      ServerLevel l,
      int X,
      int Y,
      int Z,
      BlockState ground,
      SpacePlanets.Planet p) {
    for (int x = X - 6; x <= X + 6; x++) {
      for (int z = Z - 6; z <= Z + 6; z++) {
        for (int y = Y + 1; y <= Y + 12; y++) {
          o.set(x, y, z, st(Blocks.AIR));
        }

        for (int y = Y; y >= Y - 10; y--) {
          BlockPos q = new BlockPos(x, y, z);
          if (y < Y && !l.getBlockState(q).isAir() && l.getFluidState(q).isEmpty()) {
            break;
          }

          o.set(
              x,
              y,
              z,
              y == Y && Math.abs(x - X) <= 4 && Math.abs(z - Z) <= 4
                  ? st((Block) Content.FLOOR.get())
                  : ground);
        }
      }
    }

    for (int x = -4; x <= 4; x++) {
      for (int z = -4; z <= 4; z++) {
        boolean edge = Math.abs(x) == 4 || Math.abs(z) == 4;
        if (edge) {
          for (int y = 1; y <= 3; y++) {
            o.set(
                X + x,
                Y + y,
                Z + z,
                x == 0 && z == 4 && y <= 2 ? st(Blocks.AIR) : st((Block) Content.WALL.get()));
          }
        }
      }
    }

    for (int x = -5; x <= 5; x++) {
      for (int z = -5; z <= 5; z++) {
        for (int y = 0; y <= 5; y++) {
          double d = Math.sqrt(x * x + z * z + y * y * 1.6);
          if (d > 4.3 && d <= 5.3 && Math.abs(x) <= 4 && Math.abs(z) <= 4) {
            o.set(X + x, Y + 4 + y, Z + z, st(Blocks.GLASS));
          }
        }
      }
    }

    o.set(X, Y + 1, Z, st((Block) SpaceContent.LIFE_SUPPORT.get()));
    o.set(X - 3, Y + 1, Z - 3, st((Block) Content.LIGHT.get()));
    o.set(X + 3, Y + 1, Z - 3, st((Block) Content.LIGHT.get()));
    o.barrel(X - 3, Y + 1, Z + 1, TimeskipperShip.SUPPLY, null);
    o.barrel(X - 3, Y + 1, Z + 2, TimeskipperShip.SUPPLY, null);
    o.barrel(
        X + 3,
        Y + 1,
        Z + 2,
        null,
        SarcOutposts.book(p.site + " Log", "SARC Off-World Division", LOGS[p.ordinal()]));
    o.set(
        X + 3,
        Y + 1,
        Z - 1,
        TimeskipperShip.face((Block) FacilityDetails.WORKSTATION.get(), Direction.WEST));
    o.screen(
        X + 2,
        Y + 1,
        Z - 3,
        Direction.SOUTH,
        1,
        p.site.toUpperCase(Locale.ROOT),
        "LIFE SUPPORT: ON",
        "CREW: NONE",
        "LAST CONTACT: ???");
    TimeskipperShip.bed(o, X - 2, Y + 1, Z - 3, Direction.WEST, Blocks.WHITE_BED);
  }

  static void asteroid(TimeskipperShip.Sink o, int X, int Y, int Z) {
    for (int x = -11; x <= 11; x++) {
      for (int y = -12; y <= 0; y++) {
        for (int z = -11; z <= 11; z++) {
          double d = Math.sqrt(x * x + y * y * 1.6 + z * z) + Math.sin(x * 0.7 + z * 0.3) * 0.8;
          if (d <= 10.5) {
            o.set(
                X + x,
                Y + y,
                Z + z,
                d > 9.5
                    ? st((Block) SpaceContent.REGOLITH.get())
                    : ((x * 7 + y * 3 + z * 5) % 11 == 0
                        ? st(Blocks.RAW_IRON_BLOCK)
                        : st((Block) SpaceContent.ASTEROID_ROCK.get())));
          }
        }
      }
    }

    for (int x = -9; x <= 9; x++) {
      for (int y = -10; y <= 0; y++) {
        for (int z = -28; z <= -6; z++) {
          double d =
              Math.sqrt(
                  Math.pow(x, 2.0) * 0.6
                      + Math.pow(y + 4, 2.0) * 1.6
                      + Math.pow(z + 16, 2.0) * 0.5);
          if (d <= 7.0) {
            o.set(X + x, Y + y, Z + z, st((Block) SpaceContent.ASTEROID_ROCK.get()));
          }
        }
      }
    }
  }

  static void station(TimeskipperShip.Sink o, int X, int Y, int Z) {
    BlockState hull = st((Block) MetalContent.HULL.get());
    BlockState view = st((Block) MetalContent.VIEWPORT.get());
    BlockState floor = st((Block) Content.FLOOR.get());
    BlockState air = st(Blocks.AIR);
    BlockState panel = st(Blocks.BLUE_STAINED_GLASS);
    BlockState frame = st(Blocks.IRON_BLOCK);
    box(o, X - 6, Y, Z - 6, X + 6, Y + 7, Z + 6, hull, view, floor, air);

    for (int s = -1; s <= 1; s += 2) {
      box(o, X + s * 7, Y, Z - 2, X + s * 17, Y + 4, Z + 2, hull, view, floor, air);
      box(o, X - 2, Y, Z + s * 7, X + 2, Y + 4, Z + s * 17, hull, view, floor, air);

      for (int x = 7; x <= 16; x++) {
        for (int y = 1; y <= 3; y++) {
          o.set(X + s * x, Y + y, Z, air);
        }
      }

      for (int z = 7; z <= 16; z++) {
        for (int y = 1; y <= 3; y++) {
          o.set(X, Y + y, Z + s * z, air);
        }
      }

      for (int y = 1; y <= 3; y++) {
        o.set(X + s * 6, Y + y, Z, air);
        o.set(X, Y + y, Z + s * 6, air);
      }

      for (int x = 18; x <= 38; x++) {
        for (int z = -7; z <= 7; z++) {
          o.set(X + s * x, Y + 2, Z + z, x % 7 != 4 && Math.abs(z) != 7 && z != 0 ? panel : frame);
        }
      }

      o.set(X + s * 18, Y + 2, Z, frame);
      o.set(X + s * 16, Y + 1, Z, st((Block) SpaceContent.LIFE_SUPPORT.get()));
      o.set(X, Y + 1, Z + s * 16, st((Block) SpaceContent.LIFE_SUPPORT.get()));
    }

    o.set(X, Y + 1, Z, st((Block) SpaceContent.BEACON.get()));
    o.set(X - 4, Y + 1, Z - 4, st((Block) SpaceContent.LIFE_SUPPORT.get()));
    o.set(X + 4, Y + 1, Z + 4, st((Block) SpaceContent.LIFE_SUPPORT.get()));

    for (int a = -1; a <= 1; a += 2) {
      for (int b = -1; b <= 1; b += 2) {
        o.set(X + a * 4, Y + 6, Z + b * 4, st((Block) Content.LIGHT.get()));
      }
    }

    o.barrel(X - 5, Y + 1, Z + 3, TimeskipperShip.SUPPLY, null);
    o.barrel(X - 5, Y + 1, Z + 4, TimeskipperShip.SUPPLY, null);
    o.barrel(
        X + 5,
        Y + 1,
        Z - 4,
        null,
        SarcOutposts.book(
            "Halcyon Station Log",
            "SARC Off-World Division",
            LOGS[SpacePlanets.Planet.ORBIT.ordinal()]));
    o.screen(
        X - 3,
        Y + 1,
        Z - 5,
        Direction.SOUTH,
        0,
        "STATION HALCYON",
        "LOW EARTH ORBIT",
        "DON'T FALL: YOU",
        "WILL RE-ENTER");
    o.screen(
        X + 3,
        Y + 1,
        Z - 5,
        Direction.SOUTH,
        2,
        "DEEP SPACE ARRAY",
        "SIGNAL LOCKED",
        "SOURCE: AURELIA",
        "1,200 LY");

    for (int x = -1; x <= 1; x++) {
      for (int z = -1; z <= 1; z++) {
        o.set(X + x, Y + 7, Z + z, view);
      }
    }
    OrbitalDock083.build(o);
  }

  static void box(
      TimeskipperShip.Sink o,
      int ax,
      int y0,
      int az,
      int bx,
      int y1,
      int bz,
      BlockState hull,
      BlockState view,
      BlockState floor,
      BlockState air) {
    int x0 = Math.min(ax, bx);
    int x1 = Math.max(ax, bx);
    int z0 = Math.min(az, bz);
    int z1 = Math.max(az, bz);

    for (int x = x0; x <= x1; x++) {
      for (int y = y0; y <= y1; y++) {
        for (int z = z0; z <= z1; z++) {
          boolean edge = x == x0 || x == x1 || y == y0 || y == y1 || z == z0 || z == z1;
          o.set(
              x,
              y,
              z,
              !edge
                  ? air
                  : (y == y0
                      ? floor
                      : ((y == y0 + 2 || y == y0 + 3) && (x + z) % 3 == 0 ? view : hull)));
        }
      }
    }
  }

  static void rover(ServerLevel l, int x, int y, int z) {
    Entity e = ((EntityType) Content.ROVER.get()).create(l);
    if (e != null) {
      e.moveTo(x + 0.5, y, z + 0.5, 180.0F, 0.0F);
      l.addFreshEntity(e);
    }
  }

  private SpaceSites() {}
}
