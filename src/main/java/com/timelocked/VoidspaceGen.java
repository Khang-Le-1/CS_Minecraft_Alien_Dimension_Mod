package com.timelocked;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

public final class VoidspaceGen {
  static final double T = 0.17;
  static final int CELL = 176;
  static final int ORIGIN_PAD = 28;
  static final int ORIGIN_BLEND = 24;
  static final int VILLAGE_PAD = 46;
  static final int VILLAGE_BLEND = 28;
  static final int SITE_BLEND = 18;
  public static final List<VoidspaceGen.Islet> ISLETS = islets();
  private static final Map<RandomState, VoidspaceGen.Layout> CACHE =
      Collections.synchronizedMap(new WeakHashMap<>());

  static double lattice(int x, int z, int salt) {
    long h = x * -7046029254386353131L + z * -4417276706812531889L + salt * 1609587929392839161L;
    h ^= h >>> 31;
    h *= -4658895280553007687L;
    h ^= h >>> 29;
    return (h >>> 11) * 1.110223E-16F * 2.0 - 1.0;
  }

  static double value(double x, double z, int s) {
    int x0 = (int) Math.floor(x);
    int z0 = (int) Math.floor(z);
    double fx = x - x0;
    double fz = z - z0;
    fx = fx * fx * (3.0 - 2.0 * fx);
    fz = fz * fz * (3.0 - 2.0 * fz);
    double a = lattice(x0, z0, s);
    double b = lattice(x0 + 1, z0, s);
    double c = lattice(x0, z0 + 1, s);
    double d = lattice(x0 + 1, z0 + 1, s);
    double top = a + (b - a) * fx;
    return top + (c + (d - c) * fx - top) * fz;
  }

  static double fbm(double x, double z, int s) {
    return value(x, z, s) * 0.55
        + value(x * 2.03 + 17.3, z * 2.03 - 9.1, s + 1) * 0.3
        + value(x * 4.1 - 3.7, z * 4.1 + 5.2, s + 2) * 0.15;
  }

  static double smooth(double a, double b, double v) {
    double t = Math.clamp((v - a) / (b - a), 0.0, 1.0);
    return t * t * (3.0 - 2.0 * t);
  }

  public static double rim(double x, double z) {
    double a = Math.atan2(z, x);
    return 950.0 + 65.0 * Math.sin(a * 5.0) + 35.0 * Math.cos(a * 9.0);
  }

  public static double edgeFrac(int x, int z) {
    return Math.hypot(x, z) / rim(x, z);
  }

  static double refugeWeight(int x, int z) {
    double w = 0.0;

    for (VoidspaceLand.Village v : VoidspaceLand.VILLAGES) {
      if (v.intact()) {
        double r = VoidspaceLand.refugeRadius(v);
        w = Math.max(w, 1.0 - smooth(r - 65.0, r, Math.hypot(x - v.x(), z - v.z())));
      }
    }

    return w;
  }

  public static VoidspaceGen.Islet islet(int x, int z) {
    for (VoidspaceGen.Islet i : ISLETS) {
      if ((long) (x - i.x) * (x - i.x) + (long) (z - i.z) * (z - i.z) < (long) i.r * i.r) {
        return i;
      }
    }

    return null;
  }

  public static VoidspaceGen.Biome classify(int x, int z) {
    if (!VoidspaceLand.land(x, z)) {
      return islet(x, z) != null ? VoidspaceGen.Biome.CRYSTAL : VoidspaceGen.Biome.BURNT;
    } else if (VoidspaceLand.preserved(x, z)) {
      return VoidspaceGen.Biome.REFUGE;
    } else if (edgeFrac(x, z) > 0.9) {
      return VoidspaceGen.Biome.HIGHLANDS;
    } else if (Math.hypot(x, z) < 110.0) {
      return VoidspaceGen.Biome.BURNT;
    } else {
      double wet = fbm(x / 225.0, z / 225.0, 83), grove = fbm(x / 250.0, z / 250.0, 67);
      if (wet > 0.20) return Biome.WETLAND;
      if (grove > 0.17) return Biome.MOONVEIL;
      if (grove < -0.24) return Biome.DUNES;
      if (wet < -0.22) return Biome.MEADOW;
      double e = fbm(x / 300.0, z / 300.0, 11) - 0.17;
      double c = fbm(x / 280.0, z / 280.0, 23) - 0.17;
      double s = fbm(x / 240.0, z / 240.0, 37) - 0.17;
      double m = Math.max(e, Math.max(c, s));
      if (m < 0.0) {
        return VoidspaceGen.Biome.BURNT;
      } else {
        return m == e
            ? VoidspaceGen.Biome.EMBER
            : (m == c ? VoidspaceGen.Biome.CRYSTAL : VoidspaceGen.Biome.SPORE);
      }
    }
  }

  static List<VoidspaceGen.Islet> islets() {
    List<VoidspaceGen.Islet> out = new ArrayList<>();

    for (int i = 0; i < 14; i++) {
      double a = i * Math.PI * 2.0 / 14.0 + lattice(i, 3, 91) * 0.12;
      double d = Math.max(rim(Math.cos(a), Math.sin(a)) + 45.0, 1080.0) + lattice(i, 5, 93) * 14.0;
      d = Math.min(d, 1125.0);
      int r = 11 + (int) Math.round((lattice(i, 7, 95) + 1.0) * 4.5);
      out.add(
          new VoidspaceGen.Islet(
              (int) Math.round(Math.cos(a) * d),
              (int) Math.round(Math.sin(a) * d),
              r,
              72 + (int) Math.round((lattice(i, 9, 97) + 1.0) * 12.0)));
    }

    return List.copyOf(out);
  }

  public static VoidspaceGen.Layout layout(RandomState r) {
    return CACHE.computeIfAbsent(r, VoidspaceGen.Layout::new);
  }

  private VoidspaceGen() {}

  public enum Biome {
    BURNT("burnt_fragment"),
    REFUGE("celestial_refuge"),
    EMBER("ember_wastes"),
    CRYSTAL("crystal_barrens"),
    SPORE("spore_hollows"),
    HIGHLANDS("shattered_highlands"),
    MOONVEIL("moonveil_forest"),
    MEADOW("prismatic_meadows"),
    WETLAND("twilight_wetlands"),
    DUNES("obsidian_dunes");

    public final String id;

    Biome(String id) {
      this.id = id;
    }
  }

  public record Column(int h, int bottom, VoidspaceGen.Biome biome, boolean pad, boolean islet) {}

  public record Islet(int x, int z, int r, int top) {}

  public static final class Layout {
    final SimplexNoise cont;
    final SimplexNoise ero;
    final SimplexNoise ridge;
    final SimplexNoise detail;
    public final int[] villageY;
    public final List<VoidspaceGen.Site> sites;
    final Map<Long, List<VoidspaceGen.Site>> grid = new HashMap<>();

    Layout(RandomState random) {
      RandomSource r =
          random
              .getOrCreateRandomFactory(
                  ResourceLocation.fromNamespaceAndPath("timelocked", "voidspace_relief"))
              .at(0, 0, 0);
      this.cont = new SimplexNoise(r);
      this.ero = new SimplexNoise(r);
      this.ridge = new SimplexNoise(r);
      this.detail = new SimplexNoise(r);
      this.villageY = new int[VoidspaceLand.VILLAGES.length];

      for (int i = 0; i < this.villageY.length; i++) {
        VoidspaceLand.Village v = VoidspaceLand.VILLAGES[i];
        double sum = 0.0;

        for (int dx = -30; dx <= 30; dx += 15) {
          for (int dz = -30; dz <= 30; dz += 15) {
            sum += this.raw(v.x() + dx, v.z() + dz);
          }
        }

        this.villageY[i] = Math.clamp((int) Math.round(sum / 25.0), 52, 96);
      }

      List<VoidspaceGen.Site> list = new ArrayList<>();

      for (int cx = -7; cx <= 6; cx++) {
        for (int cz = -7; cz <= 6; cz++) {
          VoidspaceGen.Site s = this.plan(cx, cz);
          if (s != null) {
            list.add(s);
            this.grid.computeIfAbsent(key(cx, cz), k -> new ArrayList<>()).add(s);
          }
        }
      }

      this.sites = List.copyOf(list);
    }

    static long key(int cx, int cz) {
      return (long) cx << 32 ^ cz & 4294967295L;
    }

    VoidspaceGen.Site plan(int cx, int cz) {
      if (VoidspaceGen.lattice(cx, cz, 201) < -0.45) {
        return null;
      }

      int x = cx * 176 + 30 + (int) ((VoidspaceGen.lattice(cx, cz, 203) + 1.0) * 0.5 * 116.0);
      int z = cz * 176 + 30 + (int) ((VoidspaceGen.lattice(cx, cz, 205) + 1.0) * 0.5 * 116.0);
      if (!(Math.hypot(x, z) < 130.0)
          && !(VoidspaceGen.edgeFrac(x, z) > 0.94)
          && !VoidspaceLand.approach(x, z)) {
        double t = Math.clamp((x * -240.0 + z * 180.0) / 90000.0, 0.0, 1.0);
        if (Math.hypot(x + 240.0 * t, z - 180.0 * t) < 40.0) {
          return null;
        }

        for (VoidspaceLand.Village v : VoidspaceLand.VILLAGES) {
          if (Math.hypot(x - v.x(), z - v.z()) < (VoidspaceLand.sanctuary(v) ? 155.0 : 120.0)) {
            return null;
          }
        }

        VoidspaceGen.Biome b = VoidspaceGen.classify(x, z);

        VoidspaceGen.SiteType[] pool =
            switch (b) {
              case REFUGE ->
                  new VoidspaceGen.SiteType[] {
                    VoidspaceGen.SiteType.SHRINE, VoidspaceGen.SiteType.WATCHTOWER
                  };
              case EMBER ->
                  new VoidspaceGen.SiteType[] {
                    VoidspaceGen.SiteType.DROPSHIP,
                    VoidspaceGen.SiteType.OUTPOST,
                    VoidspaceGen.SiteType.WATCHTOWER
                  };
              case CRYSTAL ->
                  new VoidspaceGen.SiteType[] {
                    VoidspaceGen.SiteType.GROTTO,
                    VoidspaceGen.SiteType.SHRINE,
                    VoidspaceGen.SiteType.OUTPOST,
                    VoidspaceGen.SiteType.GROTTO
                  };
              case SPORE ->
                  new VoidspaceGen.SiteType[] {
                    VoidspaceGen.SiteType.GROTTO,
                    VoidspaceGen.SiteType.SHRINE,
                    VoidspaceGen.SiteType.DROPSHIP,
                    VoidspaceGen.SiteType.GROTTO
                  };
              case HIGHLANDS ->
                  new VoidspaceGen.SiteType[] {
                    VoidspaceGen.SiteType.WATCHTOWER,
                    VoidspaceGen.SiteType.SHRINE,
                    VoidspaceGen.SiteType.OUTPOST
                  };
              default ->
                  new VoidspaceGen.SiteType[] {
                    VoidspaceGen.SiteType.OUTPOST,
                    VoidspaceGen.SiteType.DROPSHIP,
                    VoidspaceGen.SiteType.SHRINE,
                    VoidspaceGen.SiteType.WATCHTOWER
                  };
            };
        VoidspaceGen.SiteType type =
            pool[Math.floorMod((int) (VoidspaceGen.lattice(cx, cz, 207) * 1000.0), pool.length)];
        if ((b == VoidspaceGen.Biome.BURNT || b == VoidspaceGen.Biome.EMBER)
            && Math.hypot(x, z) > 320.0
            && VoidspaceGen.lattice(cx, cz, 213) > 0.5) {
          type = VoidspaceGen.SiteType.WARBASE;
        }

        int r = type.radius;

        for (int a = 0; a < 8; a++) {
          int px = x + (int) Math.round(Math.cos(a * Math.PI / 4.0) * (r + 4));
          int pz = z + (int) Math.round(Math.sin(a * Math.PI / 4.0) * (r + 4));
          if (!VoidspaceLand.land(px, pz)) {
            return null;
          }
        }

        double sum = this.raw(x, z);
        int n = 1;

        for (int a = 0; a < 8; a++) {
          sum +=
              this.raw(
                  x + (int) Math.round(Math.cos(a * Math.PI / 4.0) * r * 0.7),
                  z + (int) Math.round(Math.sin(a * Math.PI / 4.0) * r * 0.7));
          n++;
        }

        int rot = Math.floorMod((int) (VoidspaceGen.lattice(cx, cz, 209) * 1000.0), 4);
        boolean ruined =
            b != VoidspaceGen.Biome.REFUGE
                && (type == VoidspaceGen.SiteType.SHRINE
                    || type == VoidspaceGen.SiteType.WATCHTOWER
                    || type == VoidspaceGen.SiteType.OUTPOST)
                && VoidspaceGen.lattice(cx, cz, 211) < 0.25;
        return new VoidspaceGen.Site(
            type, x, z, Math.clamp((int) Math.round(sum / n), 46, 118), rot, ruined, b);
      } else {
        return null;
      }
    }

    public List<VoidspaceGen.Site> near(int x, int z) {
      int cx = Math.floorDiv(x, 176);
      int cz = Math.floorDiv(z, 176);
      List<VoidspaceGen.Site> out = new ArrayList<>(2);

      for (int dx = -1; dx <= 1; dx++) {
        for (int dz = -1; dz <= 1; dz++) {
          List<VoidspaceGen.Site> l = this.grid.get(key(cx + dx, cz + dz));
          if (l != null) {
            out.addAll(l);
          }
        }
      }

      return out;
    }

    public Optional<VoidspaceGen.Site> nearest(int x, int z, int extra) {
      VoidspaceGen.Site best = null;
      double bd = 1.0E9;

      for (VoidspaceGen.Site s : this.near(x, z)) {
        double d = Math.hypot(x - s.x(), z - s.z());
        if (d <= s.type().radius + extra && d < bd) {
          bd = d;
          best = s;
        }
      }

      return Optional.ofNullable(best);
    }

    double n(SimplexNoise s, double x, double z, double scale, int oct) {
      double sum = 0.0;
      double amp = 1.0;
      double norm = 0.0;
      double f = 1.0 / scale;

      for (int i = 0; i < oct; i++) {
        sum += s.getValue(x * f + i * 31.7, z * f - i * 17.3) * amp;
        norm += amp;
        amp *= 0.5;
        f *= 2.02;
      }

      return sum / norm;
    }

    public double raw(int x, int z) {
      double c = this.n(this.cont, x, z, 420.0, 3);
      double e = this.n(this.ero, x, z, 120.0, 3);
      double rg = 1.0 - Math.abs(this.ridge.getValue(x / 85.0, z / 85.0));
      double d = this.detail.getValue(x / 26.0, z / 26.0);
      double h = 63.0 + c * 15.0 + e * 6.0 + d * 1.4;
      double base = Math.hypot(x, z);
      double originCalm = VoidspaceGen.smooth(90.0, 130.0, base);
      double we =
          VoidspaceGen.smooth(0.09000000000000001, 0.23, VoidspaceGen.fbm(x / 300.0, z / 300.0, 11))
              * originCalm;
      double wc =
          VoidspaceGen.smooth(0.09000000000000001, 0.23, VoidspaceGen.fbm(x / 280.0, z / 280.0, 23))
              * originCalm;
      double ws =
          VoidspaceGen.smooth(0.09000000000000001, 0.23, VoidspaceGen.fbm(x / 240.0, z / 240.0, 37))
              * originCalm;
      double wh = VoidspaceGen.smooth(0.86, 0.93, VoidspaceGen.edgeFrac(x, z));
      double wr = VoidspaceGen.refugeWeight(x, z);
      h += we * (-5.0 + rg * rg * rg * 18.0);
      h += wc * (-3.0 - e * 3.0);
      h += ws * (-8.0 + Math.abs(e) * 6.0);
      h = h + (1.0 - wh) * 0.0 + wh * (16.0 + rg * rg * 28.0 + e * 6.0);
      h += wr * (61.0 + c * 6.0 - h) * 0.7;
      double wet = VoidspaceGen.fbm(x / 225.0, z / 225.0, 83),
          grove = VoidspaceGen.fbm(x / 250.0, z / 250.0, 67);
      double free = originCalm * (1.0 - wr) * (1.0 - wh);
      h +=
          free
              * (VoidspaceGen.smooth(0.10, 0.28, wet) * (-7.0 - e * 3.0)
                  + VoidspaceGen.smooth(0.07, 0.26, grove) * (3.0 + d * 1.5)
                  + (1.0 - VoidspaceGen.smooth(-0.31, -0.17, grove))
                      * (6.0 + 5.0 * Math.sin(x / 23.0 + z / 39.0)));
      double cw = Math.max(0.0, 1.0 - wc - ws - wr - wh);
      if (cw > 0.0) {
        h += crater(x, z) * cw;
      }

      return Math.clamp(h, 40.0, 128.0);
    }

    static double crater(int x, int z) {
      int cx = Math.floorDiv(x, 96);
      int cz = Math.floorDiv(z, 96);
      double out = 0.0;

      for (int dx = -1; dx <= 1; dx++) {
        for (int dz = -1; dz <= 1; dz++) {
          int gx = cx + dx;
          int gz = cz + dz;
          if (!(VoidspaceGen.lattice(gx, gz, 301) < 0.25)) {
            double px = gx * 96 + 48 + VoidspaceGen.lattice(gx, gz, 303) * 30.0;
            double pz = gz * 96 + 48 + VoidspaceGen.lattice(gx, gz, 305) * 30.0;
            double r = 9.0 + (VoidspaceGen.lattice(gx, gz, 307) + 1.0) * 5.0;
            double depth = 3.0 + (VoidspaceGen.lattice(gx, gz, 309) + 1.0) * 2.5;
            double d = Math.hypot(x - px, z - pz) / r;
            if (d < 1.0) {
              out += -depth * (1.0 - d * d);
            } else if (d < 1.5) {
              out +=
                  1.8 * (1.0 - Math.abs(d - 1.2) / 0.3 > 0.0 ? 1.0 - Math.abs(d - 1.2) / 0.3 : 0.0);
            }
          }
        }
      }

      return out;
    }

    public VoidspaceGen.Column column(int x, int z) {
      VoidspaceGen.Islet is = null;
      if (!VoidspaceLand.land(x, z)) {
        is = VoidspaceGen.islet(x, z);
        if (is == null) {
          return new VoidspaceGen.Column(-1, 0, VoidspaceGen.Biome.BURNT, false, false);
        }

        double dd = Math.hypot(x - is.x, z - is.z) / is.r;
        int top =
            is.top
                + (int) Math.round((1.0 - dd * dd) * 3.0 + this.detail.getValue(x / 9.0, z / 9.0));
        int bottom =
            top
                - 2
                - (int)
                    Math.round(
                        (1.0 - dd) * is.r * 1.1
                            + Math.abs(this.detail.getValue(x / 5.0, z / 5.0)) * 3.0);
        return new VoidspaceGen.Column(top, bottom, VoidspaceGen.Biome.CRYSTAL, false, true);
      } else {
        double h = this.raw(x, z);
        if (classify(x, z) == Biome.WETLAND) h = Math.max(57, h);
        boolean pad = false;
        int ox = Math.max(Math.abs(x), Math.abs(z));
        if (ox < 52) {
          double w = 1.0 - VoidspaceGen.smooth(28.0, 52.0, ox);
          h += (62.0 - h) * w;
          pad |= ox <= 28;
        }

        for (int i = 0; i < VoidspaceLand.VILLAGES.length; i++) {
          VoidspaceLand.Village v = VoidspaceLand.VILLAGES[i];
          int d = Math.max(Math.abs(x - v.x()), Math.abs(z - v.z()));
          int vr = VoidspaceLand.padRadius(v);
          if (d < vr + 32) {
            double w = 1.0 - VoidspaceGen.smooth(vr, vr + 32, d);
            h += (this.villageY[i] - h) * w;
            pad |= d <= vr;
          }
        }

        for (VoidspaceGen.Site s : this.near(x, z)) {
          double d = Math.hypot(x - s.x(), z - s.z());
          double r = s.type().radius;
          if (d < r + 18.0) {
            double w = 1.0 - VoidspaceGen.smooth(r, r + 18.0, d);
            h += (s.y() - h) * w;
            pad |= d <= r + 1.0;
          }
        }

        int top = (int) Math.round(h);
        double dist = Math.hypot(x, z);
        double edge = VoidspaceGen.rim(x, z) - dist;
        int bottom =
            Math.max(
                24 + (int) Math.round(5.0 * Math.sin(x * 0.06) * Math.cos(z * 0.055)),
                top
                    - 3
                    - (int)
                        Math.round(
                            edge * 1.35 + Math.abs(this.detail.getValue(x / 7.0, z / 7.0)) * 4.0));
        return new VoidspaceGen.Column(
            top, Math.min(bottom, top), VoidspaceGen.classify(x, z), pad, false);
      }
    }

    public double detail(int x, int z) {
      return this.detail.getValue(x / 11.0, z / 11.0);
    }
  }

  public record Site(
      VoidspaceGen.SiteType type,
      int x,
      int z,
      int y,
      int rot,
      boolean ruined,
      VoidspaceGen.Biome biome) {}

  public enum SiteType {
    OUTPOST(14),
    SHRINE(10),
    DROPSHIP(17),
    WATCHTOWER(7),
    GROTTO(11),
    WARBASE(34);

    public final int radius;

    SiteType(int r) {
      this.radius = r;
    }

    public String id() {
      return this.name().toLowerCase(Locale.ROOT);
    }
  }
}
