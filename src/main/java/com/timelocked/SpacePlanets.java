package com.timelocked;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public final class SpacePlanets {
  public static final ResourceKey<Level> SHIP = key("timeskipper");
  public static volatile int clientShipLocation;

  public static ResourceKey<Level> key(String id) {
    return ResourceKey.create(
        Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath("timelocked", id));
  }

  public static float gravity(ResourceKey<Level> dim) {
    SpacePlanets.Planet p = SpacePlanets.Planet.of(dim);
    return p == null ? 1.0F : p.gravity;
  }

  public static boolean vacuum(ResourceKey<Level> dim) {
    SpacePlanets.Planet p = SpacePlanets.Planet.of(dim);
    return p != null && !p.atmosphere;
  }

  private SpacePlanets() {}

  public enum Planet {
    EARTH("earth", "Earth", "Home. Probably still there.", 1.0F, true, 1, "0 km"),
    ORBIT("orbit", "Low Earth Orbit", "SARC Orbital Station Halcyon", 0.06F, false, 1, "400 km"),
    LUNA("luna", "Luna", "Tranquility Outpost", 0.17F, false, 1, "384,400 km"),
    MARS("mars", "Mars", "Ares Relay", 0.38F, false, 2, "225 million km"),
    BELT("kessler_belt", "The Kessler Belt", "Mining Claim 7", 0.05F, false, 2, "400 million km"),
    EUROPA("europa", "Europa", "Deep Ice Station", 0.13F, false, 2, "628 million km"),
    AURELIA("aurelia", "Aurelia", "Unknown signal source", 0.9F, true, 4, "1,200 light years");

    public final String id;
    public final String title;
    public final String site;
    public final String distance;
    public final float gravity;
    public final boolean atmosphere;
    public final int cost;

    Planet(String id, String title, String site, float g, boolean air, int cost, String distance) {
      this.id = id;
      this.title = title;
      this.site = site;
      this.gravity = g;
      this.atmosphere = air;
      this.cost = cost;
      this.distance = distance;
    }

    public ResourceKey<Level> dimension() {
      return this == EARTH ? Level.OVERWORLD : SpacePlanets.key(this.id);
    }

    public static SpacePlanets.Planet byId(String id) {
      for (SpacePlanets.Planet p : values()) {
        if (p.id.equals(id)) {
          return p;
        }
      }

      return LUNA;
    }

    public static SpacePlanets.Planet of(ResourceKey<Level> dim) {
      for (SpacePlanets.Planet p : values()) {
        if (p.dimension().equals(dim)) {
          return p;
        }
      }

      return null;
    }

    public static SpacePlanets.Planet byIndex(int i) {
      SpacePlanets.Planet[] v = values();
      return v[Math.floorMod(i, v.length)];
    }
  }
}
