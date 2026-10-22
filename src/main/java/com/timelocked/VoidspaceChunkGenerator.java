package com.timelocked;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep.Carving;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.blending.Blender;

public final class VoidspaceChunkGenerator extends ChunkGenerator {
  public static final MapCodec<VoidspaceChunkGenerator> CODEC =
      RecordCodecBuilder.mapCodec(
          i ->
              i.group(
                      BiomeSource.CODEC
                          .fieldOf("biome_source")
                          .forGetter(ChunkGenerator::getBiomeSource))
                  .apply(i, VoidspaceChunkGenerator::new));
  public static final int[][] RESIDENTS =
      new int[][] {{-23, -20, 0}, {22, -20, 1}, {-23, 21, 0}, {22, 21, 1}, {4, 4, 2}, {-4, -4, 2}};

  public VoidspaceChunkGenerator(BiomeSource source) {
    super(source);
  }

  protected MapCodec<? extends ChunkGenerator> codec() {
    return CODEC;
  }

  public VoidspaceGen.Layout layout(RandomState random) {
    return VoidspaceGen.layout(random);
  }

  public int surface(int x, int z, RandomState random) {
    return this.layout(random).column(x, z).h();
  }

  public int villageY(int index, RandomState random) {
    return this.layout(random).villageY[index];
  }

  private static BlockState b(Block b) {
    return b.defaultBlockState();
  }

  static boolean wetPool(VoidspaceGen.Layout L, int x, int z) {
    return L.raw(x, z) < 56.0 && L.detail(x, z) < 0.08;
  }

  static BlockState state(VoidspaceGen.Layout L, VoidspaceGen.Column c, int x, int y, int z) {
    if (c.h() >= 0 && y >= c.bottom() && y <= c.h()) {
      int depth = c.h() - y;
      if (c.islet()) {
        return depth == 0
            ? b((Block) VoidspaceContent.GLIMMER_MOSS.get())
            : (depth < 3 ? b((Block) Content.ALIEN_LOAM.get()) : b(Blocks.CALCITE));
      }

      if (depth == 0 && VoidspaceLand.approach(x, z)) {
        return b((Block) Content.CELESTIAL_PATH.get());
      }

      if (depth >= 4) {
        return b(Blocks.DEEPSLATE);
      }

      double d = depth == 0 ? L.detail(x, z) : 0.0;

      return switch (c.biome()) {
        case MOONVEIL ->
            depth == 0
                ? b(d > 0.1 ? VoidspaceContent.GLIMMER_MOSS.get() : Content.ALIEN_LOAM.get())
                : b(Content.ALIEN_LOAM.get());
        case MEADOW ->
            depth == 0
                ? b(d > 0.25 ? Content.CELESTIAL_SILT.get() : VoidspaceContent.GLIMMER_MOSS.get())
                : b(Content.ALIEN_LOAM.get());
        case WETLAND ->
            depth == 0 && !c.pad() && wetPool(L, x, z)
                ? b(Blocks.WATER)
                : b(depth == 0 ? Blocks.MUD : Blocks.CLAY);
        case DUNES ->
            depth == 0
                ? b(d > 0.45 ? Blocks.OBSIDIAN : d > -0.10 ? Blocks.SOUL_SOIL : Blocks.BLACKSTONE)
                : b(Blocks.BLACKSTONE);
        case REFUGE ->
            depth == 0
                ? (d > 0.6
                    ? b((Block) VoidspaceContent.GLIMMER_MOSS.get())
                    : b((Block) Content.CELESTIAL_SILT.get()))
                : b((Block) Content.ALIEN_LOAM.get());
        case EMBER ->
            depth == 0
                ? (d > 0.2 ? b((Block) VoidspaceContent.EMBER_CRUST.get()) : b(Blocks.BLACKSTONE))
                : b(Blocks.BASALT);
        case CRYSTAL ->
            depth == 0
                ? (d < -0.45 ? b(Blocks.TUFF) : b(Blocks.CALCITE))
                : (depth < 2 ? b(Blocks.CALCITE) : b(Blocks.TUFF));
        case SPORE ->
            depth == 0
                ? (d < -0.5
                    ? b((Block) Content.ALIEN_LOAM.get())
                    : b((Block) VoidspaceContent.GLIMMER_MOSS.get()))
                : b((Block) Content.ALIEN_LOAM.get());
        case HIGHLANDS ->
            depth == 0
                ? (d > 0.3 ? b((Block) Content.ASH.get()) : b((Block) Content.SCORCHED_STONE.get()))
                : b((Block) Content.SCORCHED_STONE.get());
        default ->
            depth == 0
                ? (d > 0.55
                    ? b((Block) Content.SCORCHED_STONE.get())
                    : b((Block) Content.ASH.get()))
                : b((Block) Content.SCORCHED_STONE.get());
      };
    } else {
      return b(Blocks.AIR);
    }
  }

  public CompletableFuture<ChunkAccess> fillFromNoise(
      Blender blender, RandomState random, StructureManager structures, ChunkAccess chunk) {
    VoidspaceGen.Layout L = this.layout(random);
    MutableBlockPos pos = new MutableBlockPos();
    int sx = chunk.getPos().getMinBlockX();
    int sz = chunk.getPos().getMinBlockZ();

    for (int x = sx; x < sx + 16; x++) {
      for (int z = sz; z < sz + 16; z++) {
        VoidspaceGen.Column c = L.column(x, z);
        if (c.h() >= 0) {
          for (int y = c.bottom(); y <= c.h(); y++) {
            chunk.setBlockState(pos.set(x, y, z), state(L, c, x, y, z), false);
          }
        }
      }
    }

    prime(chunk);
    return CompletableFuture.completedFuture(chunk);
  }

  private static void prime(ChunkAccess c) {
    Heightmap.primeHeightmaps(c, EnumSet.of(Types.WORLD_SURFACE_WG, Types.OCEAN_FLOOR_WG));
  }

  private static boolean owns(ChunkAccess c, int x, int z) {
    return c.getPos().x == x >> 4 && c.getPos().z == z >> 4;
  }

  public void buildSurface(
      final WorldGenRegion region,
      StructureManager structures,
      RandomState random,
      final ChunkAccess chunk) {
    VoidspaceGen.Layout L = this.layout(random);
    int sx = chunk.getPos().getMinBlockX();
    int sz = chunk.getPos().getMinBlockZ();
    BiConsumer<BlockPos, BlockState> sink =
        (p, s) -> {
          if (owns(chunk, p.getX(), p.getZ()) && p.getY() > 0 && p.getY() < 255) {
            chunk.setBlockState(p, s, false);
            if (s.getBlock() instanceof EntityBlock eb) {
              BlockEntity be = eb.newBlockEntity(p, s);
              if (be != null) {
                chunk.setBlockEntity(be);
              }
            }
          }
        };
    PositionalRandomFactory factory =
        random.getOrCreateRandomFactory(
            ResourceLocation.fromNamespaceAndPath("timelocked", "voidspace_plants"));

    for (int x = sx; x < sx + 16; x++) {
      for (int z = sz; z < sz + 16; z++) {
        VoidspaceGen.Column c = L.column(x, z);
        if (c.h() >= 0 && !c.pad() && !VoidspaceLand.approach(x, z)) {
          RandomSource r = factory.at(x, 0, z);
          int lx = x - sx;
          int lz = z - sz;
          int h = c.h();
          BlockPos up = new BlockPos(x, h + 1, z);
          int roll = r.nextInt(1000);
          Block flower = null;
          if (c.islet())
            flower =
                roll < 70
                    ? VoidspaceContent.CRYSTAL_CLUSTER.get()
                    : roll < 115 ? VoidspaceContent.STAR_BLOOM.get() : null;
          else
            switch (c.biome()) {
              case REFUGE ->
                  flower =
                      roll < 75
                          ? VoidspaceContent.MOON_FERN.get()
                          : roll < 125
                              ? VoidspaceContent.STAR_BLOOM.get()
                              : roll < 145 ? VoidspaceContent.LANTERN_BLOOM.get() : null;
              case MOONVEIL ->
                  flower =
                      roll < 115
                          ? VoidspaceContent.MOON_FERN.get()
                          : roll < 145 ? VoidspaceContent.LANTERN_BLOOM.get() : null;
              case MEADOW ->
                  flower =
                      roll < 100
                          ? VoidspaceContent.STAR_BLOOM.get()
                          : roll < 155
                              ? VoidspaceContent.LANTERN_BLOOM.get()
                              : roll < 195 ? Content.CELESTIAL_FROND.get() : null;
              case WETLAND ->
                  flower =
                      roll < 120
                          ? VoidspaceContent.PRISM_REED.get()
                          : roll < 170 ? VoidspaceContent.DUSK_LOTUS.get() : null;
              case SPORE ->
                  flower =
                      roll < 110
                          ? VoidspaceContent.SPORE_SPROUT.get()
                          : roll < 135 ? VoidspaceContent.LANTERN_BLOOM.get() : null;
              case CRYSTAL ->
                  flower =
                      roll < 68
                          ? VoidspaceContent.CRYSTAL_CLUSTER.get()
                          : roll < 78 ? VoidspaceContent.PRISM_REED.get() : null;
              default -> flower = roll < 36 ? VoidspaceContent.ASHEN_TUFT.get() : null;
            }
          if (flower != null) plant(chunk, up, flower);
        }
      }
    }

    // World-cell anchors are scanned past chunk borders, then emitted only into the owning chunk.
    for (int ax = Math.floorDiv(sx - 12, 14); ax <= Math.floorDiv(sx + 27, 14); ax++)
      for (int az = Math.floorDiv(sz - 12, 14); az <= Math.floorDiv(sz + 27, 14); az++) {
        RandomSource r = factory.at(ax, 99, az);
        int x = ax * 14 + 4 + r.nextInt(7), z = az * 14 + 4 + r.nextInt(7);
        var c = L.column(x, z);
        if (c.h() < 0 || c.pad() || VoidspaceLand.approach(x, z)) continue;
        boolean room = true;
        for (int dx : new int[] {-6, 0, 6})
          for (int dz : new int[] {-6, 0, 6}) {
            var ec = L.column(x + dx, z + dz);
            if (ec.h() < 0
                || ec.pad()
                || VoidspaceLand.approach(x + dx, z + dz)
                || Math.abs(ec.h() - c.h()) > 3) room = false;
          }
        if (!room) continue;
        int roll = r.nextInt(100), h = c.h();
        BiConsumer<BlockPos, BlockState> safe =
            (p, b) -> {
              if (owns(chunk, p.getX(), p.getZ())) {
                var ec = L.column(p.getX(), p.getZ());
                if (!ec.pad() && !VoidspaceLand.approach(p.getX(), p.getZ()) && p.getY() > ec.h())
                  sink.accept(p, b);
              }
            };
        switch (c.biome()) {
          case REFUGE -> {
            if (roll < 65) VoidspaceFlora.tree(x, h, z, 7 + r.nextInt(4), 0, r.nextInt(4), safe);
          }
          case MOONVEIL -> {
            if (roll < 90) VoidspaceFlora.tree(x, h, z, 10 + r.nextInt(5), 1, r.nextInt(4), safe);
          }
          case MEADOW -> {
            if (roll < 17) VoidspaceFlora.tree(x, h, z, 6 + r.nextInt(3), 2, r.nextInt(4), safe);
          }
          case WETLAND -> {
            if (!wetPool(L, x, z) && roll < 66)
              VoidspaceFlora.tree(x, h, z, 8 + r.nextInt(4), 1, r.nextInt(4), safe);
          }
          case SPORE -> {
            if (roll < 72) VoidspaceFlora.fungus(x, h, z, 5 + r.nextInt(5), 2 + r.nextInt(2), safe);
          }
          case CRYSTAL -> {
            if (roll < 60) VoidspaceFlora.spire(x, h, z, 5 + r.nextInt(9), safe);
          }
          case EMBER -> {
            if (roll < 30) VoidspaceFlora.vent(x, h, z, safe);
            else if (roll < 55) VoidspaceFlora.column(x, h, z, 4 + r.nextInt(6), safe);
          }
          case DUNES -> {
            if (roll < 16)
              VoidspaceFlora.boulder(x, h, z, 2, Blocks.OBSIDIAN.defaultBlockState(), safe);
            else if (roll < 25) VoidspaceFlora.tree(x, h, z, 6, 3, r.nextInt(4), safe);
          }
          default -> {
            if (roll < 25) VoidspaceFlora.tree(x, h, z, 5 + r.nextInt(4), 3, r.nextInt(4), safe);
          }
        }
      }

    var out =
        new VoidspaceSites.Out() {
          @Override
          public void block(int x, int y, int z, BlockState s) {
            if (VoidspaceChunkGenerator.owns(chunk, x, z) && y > 0 && y < 255) {
              BlockPos p = new BlockPos(x, y, z);
              chunk.setBlockState(p, s, false);
              if (s.getBlock() instanceof EntityBlock eb) {
                BlockEntity be = eb.newBlockEntity(p, s);
                if (be != null) {
                  chunk.setBlockEntity(be);
                }
              }
            }
          }

          @Override
          public void loot(int x, int y, int z, String table) {
            if (VoidspaceChunkGenerator.owns(chunk, x, z)) {
              BlockPos p = new BlockPos(x, y, z);
              BlockState s = chunk.getBlockState(p);
              if (s.getBlock() instanceof EntityBlock eb
                  && eb.newBlockEntity(p, s) instanceof RandomizableContainerBlockEntity box) {
                box.setLootTable(
                    ResourceKey.create(
                        Registries.LOOT_TABLE,
                        ResourceLocation.fromNamespaceAndPath("timelocked", table)),
                    region.getSeed() ^ p.asLong());
                chunk.setBlockEntity(box);
              }
            }
          }

          @Override
          public void screen(int x, int y, int z, int theme, String... lines) {
            if (VoidspaceChunkGenerator.owns(chunk, x, z)) {
              BlockPos p = new BlockPos(x, y, z);
              BlockState s = chunk.getBlockState(p);
              if (s.is((Block) MonitorContent.MONITOR.get())) {
                MonitorContent.MonitorEntity m = new MonitorContent.MonitorEntity(p, s);
                m.write(theme, lines);
                chunk.setBlockEntity(m);
              }
            }
          }
        };

    for (int i = 0; i < VoidspaceLand.VILLAGES.length; i++) {
      VoidspaceLand.Village v = VoidspaceLand.VILLAGES[i];
      if (sx + 15 >= v.x() - VoidspaceLand.padRadius(v)
          && sx <= v.x() + VoidspaceLand.padRadius(v)
          && sz + 15 >= v.z() - VoidspaceLand.padRadius(v)
          && sz <= v.z() + VoidspaceLand.padRadius(v)) {
        int base = L.villageY[i];
        VoidspaceVillage.emit(v, base, sink);
        if (v.intact()) {
          out.block(v.x() + 20, base + 1, v.z() - 42, b(Blocks.BARREL));
          out.loot(v.x() + 20, base + 1, v.z() - 42, "sarc_field_aid");
          out.block(v.x() + 19, base + 1, v.z() - 31, MonitorContent.stand(Direction.SOUTH));
          out.screen(
              v.x() + 19,
              base + 1,
              v.z() - 31,
              0,
              "CELESTIAL REFUGE",
              "SARC FIELD AID",
              "Dawnbringer threat",
              "Stay together");
        }
      }
    }

    for (VoidspaceGen.Site site : L.near(sx + 8, sz + 8)) {
      int r = site.type().radius + 2;
      if (sx + 15 >= site.x() - r
          && sx <= site.x() + r
          && sz + 15 >= site.z() - r
          && sz <= site.z() + r) {
        VoidspaceSites.emit(site, out);
      }
    }

    prime(chunk);
  }

  private static void plant(ChunkAccess chunk, BlockPos p, Block plant) {
    if (chunk.getBlockState(p).isAir() && chunk.getBlockState(p.below()).isSolid()) {
      chunk.setBlockState(p, plant.defaultBlockState(), false);
    }
  }

  public int getBaseHeight(
      int x, int z, Types type, LevelHeightAccessor level, RandomState random) {
    int h = this.surface(x, z, random);
    return h < 0 ? 0 : h + 1;
  }

  public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState random) {
    VoidspaceGen.Layout L = this.layout(random);
    VoidspaceGen.Column c = L.column(x, z);
    BlockState[] states = new BlockState[256];

    for (int y = 0; y < 256; y++) {
      states[y] = state(L, c, x, y, z);
    }

    return new NoiseColumn(0, states);
  }

  public void applyCarvers(
      WorldGenRegion r,
      long seed,
      RandomState s,
      BiomeManager b,
      StructureManager m,
      ChunkAccess c,
      Carving step) {}

  public void spawnOriginalMobs(WorldGenRegion region) {
    ChunkPos cp = region.getCenter();
    Holder<Biome> biome =
        region.getBiome(cp.getWorldPosition().atY(region.getMaxBuildHeight() - 1));
    WorldgenRandom r =
        new WorldgenRandom(new LegacyRandomSource(RandomSupport.generateUniqueSeed()));
    r.setDecorationSeed(region.getSeed(), cp.getMinBlockX(), cp.getMinBlockZ());
    NaturalSpawner.spawnMobsForChunkGeneration(region, biome, cp, r);
    this.spawnCelestials(region, cp);
    this.spawnGarrisons(region, cp);
  }

  public static int[][] residents(VoidspaceLand.Village v) {
    if (!VoidspaceLand.sanctuary(v)) return RESIDENTS;
    return new int[][] {
      {-23, -20, 0},
      {22, -20, 1},
      {-23, 21, 0},
      {22, 21, 1},
      {4, 4, 2},
      {-4, -4, 2},
      {-58, -48, 0},
      {0, -65, 1},
      {58, -48, 0},
      {-68, 10, 1},
      {68, 10, 0},
      {-48, 58, 1},
      {0, 70, 2},
      {48, 58, 0}
    };
  }

  private void spawnCelestials(WorldGenRegion region, ChunkPos cp) {
    VoidspaceGen.Layout L = this.layout(region.getLevel().getChunkSource().randomState());

    for (int i = 0; i < VoidspaceLand.VILLAGES.length; i++) {
      VoidspaceLand.Village v = VoidspaceLand.VILLAGES[i];
      if (v.intact()) {
        for (int[] res : residents(v)) {
          int x = v.x() + res[0];
          int z = v.z() + res[1];
          if (x >> 4 == cp.x && z >> 4 == cp.z) {
            CelestialVillager e =
                (CelestialVillager)
                    ((EntityType) VoidspaceContent.CELESTIAL.get()).create(region.getLevel());
            if (e != null) {
              BlockPos p = new BlockPos(x, L.villageY[i] + 1, z);
              e.moveTo(x + 0.5, p.getY(), z + 0.5, region.getRandom().nextFloat() * 360.0F, 0.0F);
              e.finalizeSpawn(
                  region, region.getCurrentDifficultyAt(p), MobSpawnType.STRUCTURE, null);
              e.setKind(res[2]);
              e.setPersistenceRequired();
              e.restrictTo(
                  new BlockPos(v.x(), p.getY(), v.z()), VoidspaceLand.sanctuary(v) ? 86 : 40);
              region.addFreshEntityWithPassengers(e);
            }
          }
        }
      }
    }
  }

  private void spawnGarrisons(WorldGenRegion region, ChunkPos cp) {
    VoidspaceGen.Layout L = this.layout(region.getLevel().getChunkSource().randomState());

    for (VoidspaceGen.Site site : L.near(cp.getMiddleBlockX(), cp.getMiddleBlockZ())) {
      if (site.type() == VoidspaceGen.SiteType.WARBASE) {
        for (int[] g : VoidspaceSites.GARRISON) {
          int x = VoidspaceSites.worldX(site, g[0], g[2]);
          int z = VoidspaceSites.worldZ(site, g[0], g[2]);
          if (x >> 4 == cp.x && z >> 4 == cp.z) {
            EntityType<Dawnbringer> type =
                g[3] == 2
                    ? (EntityType) VoidspaceContent.DAWN_WARLORD.get()
                    : (g[3] == 1
                        ? (EntityType) VoidspaceContent.DAWN_SUNSPEAKER.get()
                        : (EntityType) VoidspaceContent.DAWN_LEGIONNAIRE.get());
            Dawnbringer e = (Dawnbringer) type.create(region.getLevel());
            if (e != null) {
              BlockPos p = new BlockPos(x, site.y() + g[1], z);
              e.moveTo(x + 0.5, p.getY(), z + 0.5, region.getRandom().nextFloat() * 360.0F, 0.0F);
              e.finalizeSpawn(
                  region, region.getCurrentDifficultyAt(p), MobSpawnType.STRUCTURE, null);
              e.setPersistenceRequired();
              e.restrictTo(new BlockPos(site.x(), site.y() + 1, site.z()), 30);
              region.addFreshEntityWithPassengers(e);
            }
          }
        }
      }
    }
  }

  public int getGenDepth() {
    return 256;
  }

  public int getMinY() {
    return 0;
  }

  public int getSeaLevel() {
    return 0;
  }

  public void addDebugScreenInfo(List<String> info, RandomState random, BlockPos p) {
    VoidspaceGen.Layout L = this.layout(random);
    VoidspaceGen.Column c = L.column(p.getX(), p.getZ());
    info.add(
        "Everleigh's world fragment: "
            + (c.islet() ? "floating islet" : c.biome().id.replace('_', ' '))
            + L.nearest(p.getX(), p.getZ(), 40)
                .map(s -> ", near " + s.type().id() + (s.ruined() ? " (ruined)" : ""))
                .orElse(""));
  }
}
