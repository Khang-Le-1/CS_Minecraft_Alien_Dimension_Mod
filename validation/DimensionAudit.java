package com.timelocked;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.server.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Test-only native audit, excluded from all installable builds. */
@EventBusSubscriber(modid = "timelocked")
public class DimensionAudit {
  static final Path OUT = Path.of(System.getProperty("timelocked.audit.output"));
  static final Map<String, Object> report = new LinkedHashMap<>();
  static final List<String> failures = new ArrayList<>();
  static int checks = 0;

  static void check(boolean v, String label) {
    checks++;
    if (!v) failures.add(label);
  }

  @SubscribeEvent
  public static void started(ServerStartedEvent e) {
    e.getServer().execute(() -> run(e.getServer()));
  }

  static void run(MinecraftServer server) {
    try {
      Files.createDirectories(OUT);
      ServerLevel l = server.getLevel(VoidspaceTravel.DIMENSION);
      check(l != null, "dimension loads");
      VoidspaceChunkGenerator gen = (VoidspaceChunkGenerator) l.getChunkSource().getGenerator();
      var L = gen.layout(l.getChunkSource().randomState());
      Map<String, Integer> counts = new TreeMap<>();
      Map<VoidspaceGen.Biome, int[]> samples = new EnumMap<>(VoidspaceGen.Biome.class);
      Map<VoidspaceGen.Biome, Double> scores = new EnumMap<>(VoidspaceGen.Biome.class);
      for (int x = -900; x <= 900; x += 12)
        for (int z = -900; z <= 900; z += 12)
          if (VoidspaceLand.land(x, z)) {
            var b = VoidspaceGen.classify(x, z);
            counts.merge(b.id, 1, Integer::sum);
            var c = L.column(x, z);
            if (!c.pad()
                && !VoidspaceLand.approach(x, z)
                && VoidspaceGen.edgeFrac(x, z) < .96
                && VoidspaceGen.classify(x + 2, z + 2) == b) {
              int clearance = 0;
              for (int r = 8; r <= 72; r += 8) {
                boolean good = true;
                for (int a = 0; a < 12; a++) {
                  int xx = x + (int) Math.round(Math.cos(a * Math.PI / 6) * r),
                      zz = z + (int) Math.round(Math.sin(a * Math.PI / 6) * r);
                  if (VoidspaceGen.classify(xx, zz) != b
                      || !VoidspaceLand.land(xx, zz)
                      || L.column(xx, zz).pad()) good = false;
                }
                if (!good) break;
                clearance = r;
              }
              double score = clearance - Math.hypot(x, z) * .002;
              if (b == VoidspaceGen.Biome.WETLAND && L.raw(x, z) < 56) score += 10;
              if (score > scores.getOrDefault(b, -1e9)) {
                scores.put(b, score);
                samples.put(b, new int[] {x, z});
              }
            }
          }
      for (var b : VoidspaceGen.Biome.values())
        check(counts.getOrDefault(b.id, 0) > 0, "region " + b.id);
      report.put("biome_samples", counts);
      report.put("representatives", samples);
      int base = L.villageY[0], ox = -240, oz = 180;
      report.put("sanctuary_base", base);
      check(VoidspaceLand.refugeRadius(VoidspaceLand.VILLAGES[0]) == 300, "sanctuary radius");
      check(
          VoidspaceChunkGenerator.residents(VoidspaceLand.VILLAGES[0]).length == 14,
          "fourteen residents");
      check(L.column(0, 0).h() == 62, "portal staging ground");
      VoidspaceTravel.ensurePlatform(server, l);
      check(l.getBlockState(new BlockPos(0, 65, 0)).is(Content.FIELD.get()), "return portal field");
      int[][] homes = {
        {-23, -20},
        {22, -20},
        {-23, 21},
        {22, 21},
        {-58, -48},
        {0, -65},
        {58, -48},
        {-68, 10},
        {68, 10},
        {-48, 58},
        {0, 70},
        {48, 58}
      };
      for (int[] h : homes) {
        BlockPos p = new BlockPos(ox + h[0], base, oz + h[1]);
        l.getChunkAt(p);
        check(l.getBlockState(p).is(Blocks.CALCITE), "house floor " + Arrays.toString(h));
        check(l.getBlockState(p.above()).isAir(), "house interior " + Arrays.toString(h));
        check(!l.getBlockState(p.above(12)).isAir(), "house dome " + Arrays.toString(h));
      }
      for (int x = -90; x <= 90; x += 6)
        for (int z = -90; z <= 90; z += 6)
          check(L.column(ox + x, oz + z).h() == base, "level sanctuary " + x + "," + z);
      // Verify actual generated blocks, not just the route planning function.
      for (int z = -70; z <= 0; z++)
        for (int dx = -2; dx <= 2; dx++) walk(l, ox + 90 + dx, base, oz + z, "arrival apron");
      for (int x = 12; x <= 90; x++)
        for (int dz = -2; dz <= 2; dz++) walk(l, ox + x, base, oz + dz, "gate to plaza");
      for (int x = 0; x <= 16; x++)
        for (int dz = -1; dz <= 1; dz++) walk(l, ox + x, base, oz - 29 + dz, "clinic branch");
      walk(l, ox + 16, base, oz - 30, "clinic threshold approach");
      for (int z : new int[] {-31, -34}) {
        BlockState door = l.getBlockState(new BlockPos(ox + 16, base + 1, oz + z));
        check(door.getBlock() instanceof DoorBlock, "clinic actual doorway " + z);
      }
      check(
          !l.getBlockState(new BlockPos(ox + 83, base + 7, oz)).isAir(), "gate lintel above route");
      for (int z : new int[] {-5, 5})
        check(
            l.getBlockState(new BlockPos(ox + 83, base + 8, oz + z))
                .is(Content.CELESTIAL_MASONRY.get()),
            "gate towers " + z);
      // Connected-component flood fill from arrival to the clinic's front step.
      Set<BlockPos> seen = new HashSet<>();
      ArrayDeque<BlockPos> queue = new ArrayDeque<>();
      BlockPos start = new BlockPos(ox + 90, base, oz - 70),
          target = new BlockPos(ox + 16, base, oz - 30);
      queue.add(start);
      seen.add(start);
      while (!queue.isEmpty()) {
        BlockPos p = queue.remove();
        for (Direction d : Direction.Plane.HORIZONTAL) {
          BlockPos n = p.relative(d);
          if (Math.abs(n.getX() - ox) > 94 || Math.abs(n.getZ() - oz) > 94 || seen.contains(n))
            continue;
          if (l.getBlockState(n).is(Content.CELESTIAL_PATH.get())
              && l.getBlockState(n.above()).isAir()
              && l.getBlockState(n.above(2)).isAir()) {
            seen.add(n);
            queue.add(n);
          }
        }
      }
      check(seen.contains(target), "arrival gate plaza clinic connected by walkable path");
      Map<BlockPos, BlockState> whole = new HashMap<>(), pieces = new HashMap<>();
      VoidspaceFlora.tree(15, 64, 15, 12, 1, 1, whole::put);
      for (int cx = 0; cx < 2; cx++)
        for (int cz = 0; cz < 2; cz++) {
          final int a = cx, b = cz;
          VoidspaceFlora.tree(
              15,
              64,
              15,
              12,
              1,
              1,
              (p, s) -> {
                if ((p.getX() >> 4) == a && (p.getZ() >> 4) == b) pieces.put(p, s);
              });
        }
      check(whole.equals(pieces), "continuous cross-chunk tree");
      check(whole.keySet().stream().anyMatch(p -> p.getX() > 15), "canopy crosses boundary");
      for (var entry : samples.entrySet()) {
        int x = entry.getValue()[0], z = entry.getValue()[1];
        l.getChunk(x >> 4, z >> 4);
        var key = l.getBiome(new BlockPos(x, 90, z)).unwrapKey().orElseThrow();
        check(
            key.location().getPath().equals(entry.getKey().id),
            "native biome " + entry.getKey().id + " got " + key.location());
      }
      for (String id :
          List.of(
              "celestial_bark",
              "silverleaf_canopy",
              "moonveil_canopy",
              "hanging_starleaf",
              "moon_fern",
              "lantern_bloom",
              "prism_reed",
              "dusk_lotus",
              "sarc_waiting_bench",
              "brain_implosion_ad",
              "sarc_hall_planter",
              "sarc_jammed_blast_door",
              "sarc_derelict_panel"))
        check(
            BuiltInRegistries.BLOCK.containsKey(
                ResourceLocation.fromNamespaceAndPath("timelocked", id)),
            "registered " + id);
      int fluidIndex = 0;
      for (LabFluids.Spec spec : List.of(LabFluids.PORTAL, LabFluids.COOLANT)) {
        BlockPos p = new BlockPos(22 + fluidIndex * 4, 80, 22);
        l.setBlock(p, spec.block.get().defaultBlockState(), 3);
        check(l.getFluidState(p).isSource(), "lab source fluid " + fluidIndex);
        check(
            l.getFluidState(p).getType() == spec.source.get(), "lab fluid identity " + fluidIndex);
        check(spec.bucket.get().getDefaultInstance().getCount() == 1, "lab bucket " + fluidIndex);
        fluidIndex++;
      }
      for (var block :
          List.of(
              LabContent.WAITING_BENCH.get(),
              LabContent.PLANTER.get(),
              LabContent.ENERGY_AD.get(),
              LabContent.JAMMED_DOOR.get())) {
        BlockPos p = new BlockPos(34, 80, 22);
        l.setBlock(p, block.defaultBlockState(), 3);
        check(
            !l.getBlockState(p).getShape(l, p).isEmpty(),
            "restored decor shape " + BuiltInRegistries.BLOCK.getKey(block));
      }
      // Legacy generator settings remain decodable when the four optional biome IDs are absent.
      JsonObject source =
          JsonParser.parseString(
                  Files.readString(
                      Path.of(
                          System.getProperty("timelocked.source.resources"),
                          "data/timelocked/dimension/voidspace.json")))
              .getAsJsonObject()
              .getAsJsonObject("generator")
              .getAsJsonObject("biome_source");
      for (String key : List.of("moonveil", "meadow", "wetland", "dunes")) source.remove(key);
      var decoded =
          VoidspaceBiomeSource.CODEC
              .codec()
              .parse(l.registryAccess().createSerializationContext(JsonOps.INSTANCE), source);
      check(decoded.result().isPresent(), "legacy six-biome settings load");
      SiteProgress old = new SiteProgress();
      old.arrivalStarted = true;
      old.arrivalStep = 3;
      old.arrivalTicks = 20;
      old.living.add(UUID.fromString("00000000-0000-0000-0000-000000000899"));
      var restored = SiteProgress.load(old.save());
      check(
          restored.arrivalStarted && restored.arrivalStep == 3 && restored.arrivalTicks == 20,
          "arrival progress persistence");
      check(restored.living.equals(old.living), "entity UUID persistence");
      jobs(l, server, ox, oz, base);
      arrival(server);
      export(l, "sanctuary", ox, oz, 96, base - 2, base + 31);
      for (var b :
          List.of(
              VoidspaceGen.Biome.MOONVEIL,
              VoidspaceGen.Biome.MEADOW,
              VoidspaceGen.Biome.WETLAND,
              VoidspaceGen.Biome.DUNES,
              VoidspaceGen.Biome.SPORE)) {
        int[] p = samples.get(b);
        int y = L.column(p[0], p[1]).h();
        export(l, b.id, p[0], p[1], 40, y - 20, y + 34);
      }
      report.put("status", failures.isEmpty() ? "PASS" : "FAIL");
    } catch (Throwable t) {
      t.printStackTrace();
      failures.add(t.toString());
      report.put("status", "ERROR");
    } finally {
      report.put("checks", checks);
      report.put("failures", failures);
      try {
        Files.writeString(
            OUT.resolve("native-report.json"),
            new GsonBuilder().setPrettyPrinting().create().toJson(report));
      } catch (Exception e) {
        e.printStackTrace();
      }
      System.out.println(
          "DIMENSION AUDIT "
              + report.get("status")
              + " checks="
              + checks
              + " failures="
              + failures);
      server.halt(false);
    }
  }

  static void arrival(MinecraftServer server) throws Exception {
    ServerLevel l = server.overworld();
    BlockPos o = new BlockPos(0, 80, 0);
    for (int iz : new int[] {7, 8}) {
      BlockPos at = o.offset(64, 0, iz * 32);
      l.getChunkAt(at);
      var template =
          l.getStructureManager()
              .getOrCreate(
                  ResourceLocation.fromNamespaceAndPath(
                      "timelocked", String.format("sarc/drawing/x02_y00_z%02d", iz)));
      check(
          template.placeInWorld(
              l,
              at,
              at,
              new net.minecraft.world.level.levelgen.structure.templatesystem
                      .StructurePlaceSettings()
                  .setIgnoreEntities(true),
              l.getRandom(),
              2),
          "centered template places " + iz);
    }
    int logos = 0, oldLogos = 0;
    int minX = 999, maxX = -999, minZ = 999, maxZ = -999;
    for (int x = 65; x <= 92; x++)
      for (int y = 6; y <= 17; y++)
        for (int z = 254; z <= 264; z++)
          if (l.getBlockState(o.offset(x, y, z)).is(Content.HANGING_LOGO.get())) {
            logos++;
            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
            minZ = Math.min(minZ, z);
            maxZ = Math.max(maxZ, z);
            if (z < 260) oldLogos++;
            check(
                !l.getBlockState(o.offset(x, y, z)).getValue(SarcHangingLogoBlock.LIT),
                "logo initially dark " + x + "," + y + "," + z);
          }
    check(logos == 588, "all 588 centered logo segments");
    check(oldLogos == 0, "old off-center logo removed");
    check(
        Math.abs((minX + maxX + 1) / 2.0 - 79) <= .5
            && Math.abs((minZ + maxZ + 1) / 2.0 - 261) <= .5,
        "logo at room center within half-block grid");
    int indexed = 0;
    for (int[] v : DrawingArrivalIndex.CELLS)
      if (v[4] == 1) {
        indexed++;
        check(
            l.getBlockState(o.offset(v[0], v[1], v[2])).is(Content.HANGING_LOGO.get()),
            "arrival index follows centered logo");
      }
    check(indexed == 588, "all centered logo segments indexed");
    check(
        l.getBlockState(o.offset(79, 17, 261))
            .is(VoidspaceContent.block("timelocked:sarc_service_panel")),
        "logo suspension beam moved with emblem");
    for (int y = 2; y <= 5; y++)
      check(
          l.getBlockState(o.offset(79, y, 261)).isAir(),
          "centered logo leaves aisle headroom " + y);
    check(
        BuiltInRegistries.SOUND_EVENT.containsKey(
            ResourceLocation.fromNamespaceAndPath("timelocked", "nau_anthem_pa")),
        "PA anthem registered");
    var fake =
        net.neoforged.neoforge.common.util.FakePlayerFactory.get(
            l,
            new com.mojang.authlib.GameProfile(
                UUID.fromString("00000000-0000-0000-0000-000000008912"), "ArrivalAudit"));
    SiteProgress p = new SiteProgress();
    check(!HubArrival.announceLogoPower(p, List.of()), "no audience does not consume announcement");
    check(HubArrival.announceLogoPower(p, List.of(fake)), "logo anthem and chat dispatch");
    check(!HubArrival.announceLogoPower(p, List.of(fake)), "multipart logo cannot replay anthem");
    var restored = SiteProgress.load(p.save());
    check(
        restored.arrivalLogoAnnounced && !HubArrival.announceLogoPower(restored, List.of(fake)),
        "reload does not replay welcome");
    var legacy = new net.minecraft.nbt.CompoundTag();
    legacy.putInt("ArrivalStep", 5);
    check(SiteProgress.load(legacy).arrivalLogoAnnounced, "completed legacy arrival not replayed");
    legacy.putInt("ArrivalStep", 3);
    check(
        !SiteProgress.load(legacy).arrivalLogoAnnounced, "in-progress legacy arrival can announce");
    report.put(
        "pa_welcome",
        "588 centered logo parts, matching arrival index, one-shot chat/anthem dispatch, persistence and legacy compatibility tested");
  }

  static void jobs(ServerLevel l, MinecraftServer server, int ox, int oz, int base)
      throws Exception {
    server.overworld().setDayTime(2000);
    for (int[] s : CelestialJobs.STATIONS) {
      BlockPos p = new BlockPos(ox + s[0], base + 1, oz + s[1]);
      l.getChunkAt(p);
      check(
          l.getBlockState(p).is(CelestialJobs.Role.values()[s[2]].block.get()),
          "generated job station " + s[2]);
      check(
          l.getBlockEntity(p) instanceof CelestialWorkstationBlock.Station,
          "generated workplace BE " + s[2]);
    }
    check(
        l.getBlockState(new BlockPos(ox - 55, base + 1, oz - 48)).is(Blocks.BLUE_BED),
        "functional dwelling bed");
    check(
        l.getBlockState(new BlockPos(ox + 12, base + 1, oz - 42))
            .is(CelestialJobs.Role.MEDIC.block.get()),
        "clinic treatment independent of reactor");
    for (var r : CelestialJobs.Role.values()) {
      var offers = CelestialJobs.trades(r);
      check(offers.size() >= 3, "job trade count " + r);
      for (var offer : offers)
        check(
            !offer.getResult().isEmpty() && !offer.getItemCostA().itemStack().isEmpty(),
            "nonempty trade " + r);
      check(
          BuiltInRegistries.ITEM.containsKey(
              ResourceLocation.fromNamespaceAndPath("timelocked", r.id)),
          "workstation item " + r);
    }
    BlockPos p = new BlockPos(1000, 121, 1000);
    for (int x = -12; x <= 12; x++)
      for (int z = -12; z <= 12; z++)
        l.setBlock(p.offset(x, -1, z), Blocks.CALCITE.defaultBlockState(), 3);
    l.setBlock(p, CelestialJobs.Role.HERBALIST.block.get().defaultBlockState(), 3);
    var station = (CelestialWorkstationBlock.Station) l.getBlockEntity(p);
    CelestialVillager a = VoidspaceContent.CELESTIAL.get().create(l),
        b = VoidspaceContent.CELESTIAL.get().create(l),
        moon = VoidspaceContent.CELESTIAL.get().create(l);
    a.setKind(0);
    b.setKind(1);
    moon.setKind(3);
    a.moveTo(1002, 121, 1000);
    b.moveTo(1003, 121, 1000);
    moon.moveTo(1004, 121, 1000);
    l.addFreshEntity(a);
    l.addFreshEntity(b);
    l.addFreshEntity(moon);
    check(a.claimWorkstation(station), "resident claims station");
    check(!b.claimWorkstation(station), "no duplicate workplace claim");
    check(!moon.claimWorkstation(station), "Ecliptic roles unchanged");
    check(a.kind() == 0 && a.job() == CelestialJobs.Role.HERBALIST, "job independent of species");
    net.minecraft.nbt.CompoundTag saved = new net.minecraft.nbt.CompoundTag();
    a.addAdditionalSaveData(saved);
    var clone = VoidspaceContent.CELESTIAL.get().create(l);
    clone.readAdditionalSaveData(saved);
    check(clone.job() == a.job() && p.equals(clone.workplace()), "job persists across save/load");
    var stationCopy = new CelestialWorkstationBlock.Station(p, l.getBlockState(p));
    stationCopy.loadWithComponents(
        station.saveWithoutMetadata(l.registryAccess()), l.registryAccess());
    check(a.getUUID().equals(stationCopy.owner()), "exclusive owner persists in station NBT");
    var offer = a.getOffers().getFirst();
    offer.increaseUses();
    a.tickOccupation();
    check(offer.getUses() == 0, "restock at attended job");
    offer.increaseUses();
    a.tickOccupation();
    check(offer.getUses() == 1, "restock cooldown enforced");
    a.moveTo(1008, 121, 1000);
    a.tickOccupation();
    check(offer.getUses() == 1, "no remote restock");
    a.getNavigation().stop();
    for (int i = 0; i < 80; i++) a.tick();
    check(a.getNavigation().getPath() != null, "work goal navigates to station");
    l.removeBlock(p, false);
    a.tickOccupation();
    check(a.job() == null && a.workplace() == null, "broken station clears job");
    l.setBlock(p, CelestialJobs.Role.COOK.block.get().defaultBlockState(), 3);
    a.moveTo(1002, 121, 1000);
    a.tickOccupation();
    check(a.job() == CelestialJobs.Role.COOK, "unemployed resident finds replacement job");
    var fake =
        net.neoforged.neoforge.common.util.FakePlayerFactory.get(
            l,
            new com.mojang.authlib.GameProfile(
                UUID.fromString("00000000-0000-0000-0000-000000008911"), "JobAudit"));
    fake.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
    fake.getInventory().clearContent();
    fake.moveTo(1002, 121, 1000);
    var herb = CelestialJobs.Role.HERBALIST.block.get();
    fake.setItemInHand(
        net.minecraft.world.InteractionHand.MAIN_HAND,
        new net.minecraft.world.item.ItemStack(VoidspaceContent.STAR_BLOOM.get(), 2));
    check(herb.use(fake, l, p), "herbalist processing works");
    check(
        fake.getInventory().countItem(VoidspaceContent.STAR_BLOOM.get().asItem()) == 0,
        "herbalist consumes exact input");
    check(fake.getInventory().countItem(Content.BANDAGE.get()) == 2, "herbalist output");
    fake.getInventory().clearContent();
    fake.setItemInHand(
        net.minecraft.world.InteractionHand.MAIN_HAND,
        new net.minecraft.world.item.ItemStack(VoidspaceContent.STAR_BLOOM.get(), 1));
    check(
        !herb.use(fake, l, p) && fake.getMainHandItem().getCount() == 1,
        "insufficient input is not consumed");
    fake.getInventory().clearContent();
    fake.setItemInHand(
        net.minecraft.world.InteractionHand.MAIN_HAND,
        new net.minecraft.world.item.ItemStack(VoidspaceContent.CRYSTAL_CLUSTER.get(), 9));
    check(CelestialJobs.Role.CRYSTAL_SHAPER.block.get().use(fake, l, p), "crystal compression");
    check(
        fake.getInventory().countItem(VoidspaceContent.CRYSTAL_BLOCK.get().asItem()) == 1,
        "crystal compression output");
    fake.getInventory().clearContent();
    fake.setItemInHand(
        net.minecraft.world.InteractionHand.MAIN_HAND,
        new net.minecraft.world.item.ItemStack(VoidspaceContent.CRYSTAL_BLOCK.get(), 1));
    check(CelestialJobs.Role.CRYSTAL_SHAPER.block.get().use(fake, l, p), "crystal decompression");
    check(
        fake.getInventory().countItem(VoidspaceContent.CRYSTAL_CLUSTER.get().asItem()) == 9,
        "crystal conservation");
    fake.getInventory().clearContent();
    fake.setItemInHand(
        net.minecraft.world.InteractionHand.MAIN_HAND,
        new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BONE_MEAL, 1));
    fake.setItemInHand(
        net.minecraft.world.InteractionHand.OFF_HAND,
        new net.minecraft.world.item.ItemStack(VoidspaceContent.MOON_FERN.get(), 1));
    check(CelestialJobs.Role.GARDENER.block.get().use(fake, l, p), "plant propagation");
    check(
        fake.getInventory().countItem(VoidspaceContent.MOON_FERN.get().asItem()) == 2,
        "plant propagated and seed retained");
    fake.getInventory().clearContent();
    fake.setItemInHand(
        net.minecraft.world.InteractionHand.MAIN_HAND,
        new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GLOW_BERRIES, 4));
    check(CelestialJobs.Role.COOK.block.get().use(fake, l, p), "hearth cooks local food");
    check(
        fake.getInventory().countItem(net.minecraft.world.item.Items.BAKED_POTATO) == 2,
        "hearth recipe output");
    fake.getInventory().clearContent();
    fake.setItemInHand(
        net.minecraft.world.InteractionHand.MAIN_HAND,
        new net.minecraft.world.item.ItemStack(VoidspaceContent.CRYSTAL_CLUSTER.get(), 6));
    check(CelestialJobs.Role.ATTUNER.block.get().use(fake, l, p), "attunement consumes offering");
    check(
        fake.hasEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION)
            && fake.hasEffect(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE),
        "attunement applies buffs");
    check(
        !CelestialJobs.Role.ATTUNER.block.get().use(fake, l, p)
            && fake.getMainHandItem().getCount() == 3,
        "attunement cooldown prevents consumption");
    fake.getInventory().clearContent();
    fake.setItemInHand(
        net.minecraft.world.InteractionHand.MAIN_HAND,
        new net.minecraft.world.item.ItemStack(Content.BANDAGE.get(), 2));
    fake.setHealth(8);
    check(
        CelestialJobs.Role.MEDIC.block.get().use(fake, l, p),
        "medic works without facility reactor");
    check(
        fake.getHealth() == 14 && fake.getMainHandItem().getCount() == 1,
        "medical heal amount and cost");
    check(
        !CelestialJobs.Role.MEDIC.block.get().use(fake, l, p)
            && fake.getMainHandItem().getCount() == 1,
        "medical cooldown");
    check(CelestialJobs.Role.WARDEN.block.get().use(fake, l, p), "warden directions interaction");
    // Test approaches through the actual service-building entrances, including
    // the clinic airlock. A resident already near each building must reach a stand.
    for (int[] s : CelestialJobs.STATIONS) {
      BlockPos job = new BlockPos(ox + s[0], base + 1, oz + s[1]);
      if (s[2] <= 3) {
        int hx = s[0] < 0 ? -23 : 22, hz = s[1] < 0 ? -20 : 21;
        double norm = Math.hypot(hx, hz);
        a.moveTo(ox + hx - hx / norm * 11, base + 1, oz + hz - hz / norm * 11);
      } else if (s[2] == 6) a.moveTo(ox + 16, base + 1, oz - 29);
      else a.moveTo(job.getX() + 3, base + 1, job.getZ() + 3);
      boolean reachable = false;
      for (Direction d : Direction.Plane.HORIZONTAL) {
        var path = a.getNavigation().createPath(job.relative(d), 0);
        if (path != null && path.canReach()) reachable = true;
      }
      check(reachable, "actual service entrance reaches job " + s[2]);
    }
    server.overworld().setDayTime(14000);
    b.moveTo(1002, 121, 1000);
    b.tickOccupation();
    check(b.job() == null, "unemployed residents do not claim jobs at night");
    server.overworld().setDayTime(2000);
    report.put(
        "celestial_jobs",
        "7 roles: claim exclusivity, persistence, work navigation, replacement, restocking, recipes, buffs and local treatment tested");
    a.discard();
    b.discard();
    moon.discard();
  }

  static void walk(ServerLevel l, int x, int y, int z, String label) {
    BlockPos p = new BlockPos(x, y, z);
    l.getChunkAt(p);
    check(l.getBlockState(p).is(Content.CELESTIAL_PATH.get()), label + " path " + p);
    check(
        l.getBlockState(p.above()).isAir()
            && l.getBlockState(p.above(2)).isAir()
            && l.getBlockState(p.above(3)).isAir(),
        label + " headroom " + p);
  }

  static void export(ServerLevel l, String name, int ox, int oz, int radius, int y0, int y1)
      throws Exception {
    Map<String, Integer> palette = new LinkedHashMap<>();
    List<int[]> blocks = new ArrayList<>();
    for (int x = ox - radius; x <= ox + radius; x++)
      for (int z = oz - radius; z <= oz + radius; z++)
        for (int y = Math.max(1, y0); y <= y1; y++) {
          BlockState s = l.getBlockState(new BlockPos(x, y, z));
          if (s.isAir()) continue;
          String id = BuiltInRegistries.BLOCK.getKey(s.getBlock()).toString();
          if (!s.getValues().isEmpty()) {
            List<String> props = new ArrayList<>();
            s.getValues()
                .forEach((k, v) -> props.add(k.getName() + "=" + v.toString().toLowerCase()));
            Collections.sort(props);
            id += "[" + String.join(",", props) + "]";
          }
          int index = palette.computeIfAbsent(id, k -> palette.size());
          blocks.add(new int[] {x - ox, y - y0, z - oz, index});
        }
    Map<String, Object> d = new LinkedHashMap<>();
    d.put("origin", new int[] {ox, y0, oz});
    d.put("palette", palette.keySet());
    d.put("blocks", blocks);
    Files.writeString(OUT.resolve(name + "-voxels.json"), new Gson().toJson(d));
    System.out.println("Exported " + name + " " + blocks.size() + " blocks");
  }
}
