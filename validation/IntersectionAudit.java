package com.timelocked;

import com.google.gson.GsonBuilder;
import com.mojang.authlib.GameProfile;
import java.nio.file.*;
import java.util.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Test-only native server audit. Never included in the release JAR. */
@EventBusSubscriber(modid = "timelocked")
public final class IntersectionAudit {
  static int checks;
  static final List<String> failures = new ArrayList<>();

  static void check(boolean value, String label) {
    checks++;
    if (!value) failures.add(label);
  }

  static final class Listener extends FakePlayer {
    final List<String> messages = new ArrayList<>();
    int sounds;

    Listener(ServerLevel level) {
      super(
          level,
          new GameProfile(UUID.fromString("00000000-0000-0000-0000-000000008913"), "WelcomeAudit"));
    }

    @Override
    public void sendSystemMessage(Component message) {
      messages.add(
          message.getContents()
                  instanceof net.minecraft.network.chat.contents.TranslatableContents t
              ? t.getKey()
              : message.getString());
    }

    @Override
    public void playNotifySound(SoundEvent sound, SoundSource source, float volume, float pitch) {
      sounds++;
    }
  }

  static void geometry(ServerLevel level) throws Exception {
    var origin = new net.minecraft.core.BlockPos(1024, 80, 1024);
    for (int iz : new int[] {7, 8}) {
      var id =
          net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
              "timelocked", String.format("sarc/drawing/x02_y00_z%02d", iz));
      var template = level.getStructureManager().getOrCreate(id);
      var at = origin.offset(64, 0, iz * 32);
      for (int x = 64; x < 96; x += 16)
        for (int z = iz * 32; z < (iz + 1) * 32; z += 16) level.getChunkAt(origin.offset(x, 0, z));
      check(
          template.placeInWorld(
              level,
              at,
              at,
              new net.minecraft.world.level.levelgen.structure.templatesystem
                      .StructurePlaceSettings()
                  .setIgnoreEntities(true),
              level.getRandom(),
              2),
          "corrected template places " + iz);
    }
    int logos = 0, oldLogos = 0, medallion = 0, oldMedallion = 0;
    int minX = 999, maxX = -999, minZ = 999, maxZ = -999;
    for (int x = 65; x <= 92; x++)
      for (int y = 6; y <= 17; y++)
        for (int z = 250; z <= 283; z++) {
          var state = level.getBlockState(origin.offset(x, y, z));
          if (state.is(Content.HANGING_LOGO.get())) {
            logos++;
            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
            minZ = Math.min(minZ, z);
            maxZ = Math.max(maxZ, z);
            check(z >= 272 && z <= 274, "logo only over intersection");
            check(!state.getValue(SarcHangingLogoBlock.LIT), "logo initially dark");
            if (z < 270) oldLogos++;
          }
        }
    check(logos == 588 && oldLogos == 0, "588 logo parts, old location cleared");
    check((minZ + maxZ + 1) / 2.0 == 273.5, "logo depth centered over intersection");
    check(
        Math.abs((minX + maxX + 1) / 2.0 - 79.5) <= .5,
        "logo within native half-block of walkway axis");
    int indexed = 0;
    for (int[] v : DrawingArrivalIndex.CELLS) {
      if (v[4] == 1) {
        indexed++;
        check(
            level.getBlockState(origin.offset(v[0], v[1], v[2])).is(Content.HANGING_LOGO.get()),
            "arrival index matches moved logo");
      }
      if (v[1] == 16 && v[2] == 271 && (v[0] == 68 || v[0] == 79 || v[0] == 90))
        check(
            level.getBlockState(origin.offset(v[0], v[1], v[2])).getBlock()
                instanceof StationLightBlock,
            "moved uplight in arrival index");
    }
    check(indexed == 588, "all logo parts indexed");
    for (int x = 65; x <= 92; x++)
      for (int z = 271; z <= 275; z++)
        check(
            level
                .getBlockState(origin.offset(x, 17, z))
                .is(VoidspaceContent.block("timelocked:sarc_service_panel")),
            "crossbeam moved with logo");
    for (int x : new int[] {66, 91})
      for (int z : new int[] {271, 275})
        check(
            level
                .getBlockState(origin.offset(x, 16, z))
                .is(net.minecraft.world.level.block.Blocks.IRON_BARS),
            "four hangers moved");
    for (int x = 68; x <= 90; x++)
      for (int z = 249; z <= 283; z++) {
        if (level
            .getBlockState(origin.offset(x, 1, z))
            .is(net.minecraft.world.level.block.Blocks.LIGHT_GRAY_CONCRETE)) {
          if (z < 265) oldMedallion++;
          else medallion++;
        }
      }
    check(
        oldMedallion == 0 && medallion == 173,
        "single relocated medallion with all 173 interior tiles");
    check(
        level
            .getBlockState(origin.offset(79, 1, 273))
            .is(net.minecraft.world.level.block.Blocks.LIGHT_GRAY_CONCRETE),
        "medallion directly below logo");
    for (int x = 74; x <= 84; x++)
      for (int z = 250; z <= 280; z++)
        for (int y = 2; y <= 5; y++)
          check(level.getBlockState(origin.offset(x, y, z)).isAir(), "walkway headroom retained");
    for (int z : new int[] {250, 255, 260, 264})
      check(
          level
              .getBlockState(origin.offset(79, 1, z))
              .is(net.minecraft.world.level.block.Blocks.POLISHED_ANDESITE),
          "old floor restored");
  }

  @SubscribeEvent
  public static void started(ServerStartedEvent event) {
    var server = event.getServer();
    server.execute(
        () -> {
          try {
            geometry(server.overworld());
            Listener player = new Listener(server.overworld());
            var audience = List.<net.minecraft.server.level.ServerPlayer>of(player);
            SiteProgress state = new SiteProgress();
            check(
                !HubArrival.announceLogoPower(state, List.of()),
                "empty hub does not consume trigger");
            check(HubArrival.announceLogoPower(state, audience), "logo starts welcome");
            check(
                player.messages.size() == 1 && player.sounds == 1,
                "one immediate message and anthem");
            check(!HubArrival.announceLogoPower(state, audience), "multipart trigger deduplicated");
            int[] seconds = {7, 9, 8, 10, 9, 7};
            for (int line = 0; line < seconds.length; line++) {
              for (int second = 1; second <= seconds[line]; second++) {
                int previousTicks = state.arrivalWelcomeTicks;
                check(!HubArrival.tickWelcome(state, List.of()), "empty hub pauses line " + line);
                check(state.arrivalWelcomeTicks == previousTicks, "pause does not advance timer");
                check(HubArrival.tickWelcome(state, audience), "active timer advances");
                check(
                    player.messages.size() == (second == seconds[line] ? line + 2 : line + 1),
                    "only one line at correct deadline " + line + ":" + second);
                // Save/reload every second to verify in-progress progress survives.
                state = SiteProgress.load(state.save());
                check(
                    !HubArrival.announceLogoPower(state, audience), "reload never restarts anthem");
              }
            }
            check(
                player.messages.size() == 7 && player.sounds == 1,
                "seven messages, only one anthem");
            for (int i = 0; i < 7; i++)
              check(
                  player.messages.get(i).equals("message.timelocked.arrival_welcome." + (i + 1)),
                  "message order " + i);
            for (int i = 0; i < 30; i++)
              check(!HubArrival.tickWelcome(state, audience), "completed queue does not replay");
            var legacy = new net.minecraft.nbt.CompoundTag();
            legacy.putInt("ArrivalStep", 5);
            check(
                !HubArrival.tickWelcome(SiteProgress.load(legacy), audience),
                "completed legacy save not replayed");
            legacy.putBoolean("ArrivalLogoAnnounced", true);
            check(
                !HubArrival.tickWelcome(SiteProgress.load(legacy), audience),
                "0.8.9.12 welcome not replayed");
            var bad = new net.minecraft.nbt.CompoundTag();
            bad.putBoolean("ArrivalLogoAnnounced", true);
            bad.putInt("ArrivalWelcomeNext", 900);
            bad.putInt("ArrivalWelcomeTicks", -1);
            var safe = SiteProgress.load(bad);
            check(
                safe.arrivalWelcomeNext == 7 && safe.arrivalWelcomeTicks == 0,
                "invalid saved values clamped");
          } catch (Throwable error) {
            failures.add(error.toString());
            error.printStackTrace();
          }
          try {
            var report = new LinkedHashMap<String, Object>();
            report.put("version", "0.8.9.14");
            report.put("checks", checks);
            report.put("failures", failures);
            report.put("status", failures.isEmpty() ? "PASS" : "FAIL");
            report.put("message_times_seconds", List.of(0, 7, 16, 24, 34, 43, 50));
            Files.writeString(
                Path.of("/data/logo-center/native-intersection-audit.json"),
                new GsonBuilder().setPrettyPrinting().create().toJson(report));
          } catch (Exception error) {
            error.printStackTrace();
          }
          server.halt(false);
        });
  }
}
