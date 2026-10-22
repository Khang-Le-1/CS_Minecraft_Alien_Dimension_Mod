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
public final class TimedWelcomeAudit {
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

  @SubscribeEvent
  public static void started(ServerStartedEvent event) {
    var server = event.getServer();
    server.execute(
        () -> {
          try {
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
            report.put("version", "0.8.9.13");
            report.put("checks", checks);
            report.put("failures", failures);
            report.put("status", failures.isEmpty() ? "PASS" : "FAIL");
            report.put("message_times_seconds", List.of(0, 7, 16, 24, 34, 43, 50));
            Files.writeString(
                Path.of("/data/welcome-update/timed-welcome-audit.json"),
                new GsonBuilder().setPrettyPrinting().create().toJson(report));
          } catch (Exception error) {
            error.printStackTrace();
          }
          server.halt(false);
        });
  }
}
