package com.timelocked;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate.Sampler;

public final class VoidspaceBiomeSource extends BiomeSource {
  public static final MapCodec<VoidspaceBiomeSource> CODEC =
      RecordCodecBuilder.mapCodec(
          i ->
              i.group(
                      Biome.CODEC.fieldOf("burnt").forGetter(g -> g.burnt),
                      Biome.CODEC.fieldOf("surviving").forGetter(g -> g.surviving),
                      Biome.CODEC.fieldOf("ember").forGetter(g -> g.ember),
                      Biome.CODEC.fieldOf("crystal").forGetter(g -> g.crystal),
                      Biome.CODEC.fieldOf("spore").forGetter(g -> g.spore),
                      Biome.CODEC.fieldOf("highlands").forGetter(g -> g.highlands),
                      Biome.CODEC.optionalFieldOf("moonveil").forGetter(g -> g.moonveil),
                      Biome.CODEC.optionalFieldOf("meadow").forGetter(g -> g.meadow),
                      Biome.CODEC.optionalFieldOf("wetland").forGetter(g -> g.wetland),
                      Biome.CODEC.optionalFieldOf("dunes").forGetter(g -> g.dunes))
                  .apply(i, VoidspaceBiomeSource::new));
  private final Holder<Biome> burnt;
  private final Holder<Biome> surviving;
  private final Holder<Biome> ember;
  private final Holder<Biome> crystal;
  private final Holder<Biome> spore;
  private final Holder<Biome> highlands;

  private final java.util.Optional<Holder<Biome>> moonveil, meadow, wetland, dunes;

  public VoidspaceBiomeSource(
      Holder<Biome> burnt,
      Holder<Biome> surviving,
      Holder<Biome> ember,
      Holder<Biome> crystal,
      Holder<Biome> spore,
      Holder<Biome> highlands,
      java.util.Optional<Holder<Biome>> moonveil,
      java.util.Optional<Holder<Biome>> meadow,
      java.util.Optional<Holder<Biome>> wetland,
      java.util.Optional<Holder<Biome>> dunes) {
    this.moonveil = moonveil;
    this.meadow = meadow;
    this.wetland = wetland;
    this.dunes = dunes;
    this.burnt = burnt;
    this.surviving = surviving;
    this.ember = ember;
    this.crystal = crystal;
    this.spore = spore;
    this.highlands = highlands;
  }

  protected MapCodec<? extends BiomeSource> codec() {
    return CODEC;
  }

  protected Stream<Holder<Biome>> collectPossibleBiomes() {
    return Stream.concat(
        Stream.of(this.burnt, this.surviving, this.ember, this.crystal, this.spore, this.highlands),
        Stream.of(moonveil, meadow, wetland, dunes).flatMap(java.util.Optional::stream));
  }

  public Holder<Biome> holder(VoidspaceGen.Biome b) {
    return switch (b) {
      case MOONVEIL -> moonveil.orElse(surviving);
      case MEADOW -> meadow.orElse(crystal);
      case WETLAND -> wetland.orElse(spore);
      case DUNES -> dunes.orElse(ember);
      case REFUGE -> this.surviving;
      case EMBER -> this.ember;
      case CRYSTAL -> this.crystal;
      case SPORE -> this.spore;
      case HIGHLANDS -> this.highlands;
      default -> this.burnt;
    };
  }

  public Holder<Biome> getNoiseBiome(int x, int y, int z, Sampler sampler) {
    return this.holder(VoidspaceGen.classify(x * 4 + 2, z * 4 + 2));
  }
}
