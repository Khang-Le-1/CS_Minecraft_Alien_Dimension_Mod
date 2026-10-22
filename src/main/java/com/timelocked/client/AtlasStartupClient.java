package com.timelocked.client;

import com.timelocked.AtlasBootSequence;
import com.timelocked.AtlasContent;
import com.timelocked.AtlasStartupSounds;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Local-only suit boot. No inventory, survival protections, camera or quest state changes. */
@EventBusSubscriber(modid = "timelocked", value = Dist.CLIENT)
public final class AtlasStartupClient {
  private static final AtlasBootSequence BOOT = new AtlasBootSequence();
  private static ClientLevel level;
  private static SimpleSoundInstance voice;
  private static long last;
  private static int settling;
  private static boolean dead;

  private static boolean full() {
    var p = Minecraft.getInstance().player;
    return p != null
        && p.getItemBySlot(EquipmentSlot.HEAD).is(AtlasContent.HELMET.get())
        && p.getItemBySlot(EquipmentSlot.CHEST).is(AtlasContent.CHEST.get())
        && p.getItemBySlot(EquipmentSlot.LEGS).is(AtlasContent.LEGS.get())
        && p.getItemBySlot(EquipmentSlot.FEET).is(AtlasContent.BOOTS.get());
  }

  private static boolean cinematic() {
    return IntroClient.locked() || JumpCutsceneClient.active() || PortalTransitClient.active();
  }

  public static boolean active() {
    return BOOT.running();
  }

  private static void stopVoice() {
    if (voice != null) Minecraft.getInstance().getSoundManager().stop(voice);
    voice = null;
  }

  private static void reset() {
    stopVoice();
    BOOT.reset();
    level = null;
    last = 0;
    settling = 0;
    dead = false;
  }

  @SubscribeEvent
  public static void logout(ClientPlayerNetworkEvent.LoggingOut e) {
    reset();
  }

  @SubscribeEvent
  public static void login(ClientPlayerNetworkEvent.LoggingIn e) {
    reset();
  }

  @SubscribeEvent
  public static void tick(ClientTickEvent.Post e) {
    var mc = Minecraft.getInstance();
    long now = Util.getMillis();
    if (mc.player == null || mc.level == null) {
      reset();
      return;
    }
    if (level != mc.level) {
      boolean transition = level != null;
      stopVoice();
      level = mc.level;
      last = now;
      settling = 20;
      if (transition) BOOT.bind(full());
      else BOOT.reset();
    }
    long elapsed = last == 0 ? 0 : now - last;
    last = now;
    if (mc.isPaused()) return;
    if (!mc.player.isAlive() || mc.player.isSpectator()) {
      stopVoice();
      BOOT.bind(full());
      dead = true;
      return;
    }
    if (dead) {
      dead = false;
      settling = 20;
      BOOT.bind(full());
    }
    // Wait for the initial equipment synchronization; joining already suited is not a suit-up.
    if (settling > 0) {
      settling--;
      BOOT.bind(full());
      return;
    }
    boolean allowed = !cinematic();
    int action = BOOT.observe(full(), allowed);
    if (action == AtlasBootSequence.STOP) stopVoice();
    if (BOOT.running() && !allowed) {
      stopVoice();
      BOOT.bind(full());
      return;
    }
    if (action == AtlasBootSequence.START) {
      stopVoice();
      voice =
          new SimpleSoundInstance(
              AtlasStartupSounds.BOOT.get().getLocation(),
              SoundSource.VOICE,
              .85F,
              1F,
              SoundInstance.createUnseededRandom(),
              false,
              0,
              SoundInstance.Attenuation.NONE,
              0,
              0,
              0,
              true);
      mc.getSoundManager().play(voice);
    } else if (BOOT.advance(elapsed) == AtlasBootSequence.COMPLETE) stopVoice();
  }

  public static void render(GuiGraphics g) {
    if (!active()) return;
    var mc = Minecraft.getInstance();
    var p = mc.player;
    if (p == null || mc.options.hideGui || cinematic()) return;
    long age = BOOT.age();
    int phase = AtlasBootSequence.phase(age);
    int W = g.guiWidth(), H = g.guiHeight();
    if (W < 320 || H < 176) return;
    // Edge scan is deliberately subtle; no flashes or opaque full-screen interruption.
    int scan = (int) ((age % 2400) * (H - 16) / 2400) + 8;
    g.fill(3, scan, 5, Math.min(H - 8, scan + 12), 0x885E9FE8);
    g.fill(W - 5, scan, W - 3, Math.min(H - 8, scan + 12), 0x885E9FE8);
    int w = Math.min(224, Math.max(96, W / 2 - 16));
    boolean compact = w < 188;
    int h = Math.min(compact ? 104 : 142, Math.max(60, H - 76));
    int x = 8, y = H - h - 36;
    g.fill(x, y, x + w, y + h, 0xD4101721);
    g.fill(x, y, x + 2, y + h, 0xFF85D9EE);
    g.drawString(mc.font, "ATLAS / HES", x + 9, y + 7, 0xFF85D9EE, false);
    var title = Component.translatable("hud.timelocked.atlas.boot");
    if (compact) {
      g.drawString(mc.font, title, x + 9, y + 20, 0xFFB9CAD6, false);
      var lines =
          mc.font.split(
              Component.translatable(
                  "hud.timelocked.atlas." + AtlasBootSequence.CUES.get(phase).key()),
              w - 18);
      for (int i = 0; i < Math.min(2, lines.size()); i++)
        g.drawString(mc.font, lines.get(i), x + 9, y + 37 + i * 10, 0xFFE8C96A, false);
      if (h >= 95) g.drawString(mc.font, (phase + 1) + " / 7", x + 9, y + 64, 0xFFB9CAD6, false);
    } else {
      g.drawString(mc.font, title, x + w - 9 - mc.font.width(title), y + 7, 0xFFB9CAD6, false);
      int rows = Math.min(7, Math.max(0, (h - 53) / 11));
      int first = Math.max(0, phase - rows + 1);
      for (int row = 0; row < rows && first + row < 7; row++) {
        int i = first + row;
        var cue = AtlasBootSequence.CUES.get(i);
        boolean done = age >= cue.end();
        String marker = done ? "[OK]" : i == phase ? "[..]" : "[--]";
        int color = done ? 0xFF8AE8C3 : i == phase ? 0xFFE8C96A : 0xFF8593A3;
        g.drawString(mc.font, marker, x + 9, y + 25 + row * 11, color, false);
        g.drawString(
            mc.font,
            Component.translatable("hud.timelocked.atlas." + cue.key()),
            x + 38,
            y + 25 + row * 11,
            color,
            false);
      }
    }
    int by = y + h - 22;
    g.fill(x + 9, by, x + w - 9, by + 3, 0xFF34414F);
    g.fill(
        x + 9,
        by,
        x + 9 + Math.round((w - 18) * AtlasBootSequence.progress(age)),
        by + 3,
        0xFF85D9EE);
    String telemetry =
        phase >= 4
            ? "HP "
                + Math.round(p.getHealth())
                + "  |  O2 "
                + Math.round(p.getAirSupply() * 100F / Math.max(1, p.getMaxAirSupply()))
                + "%"
            : Component.translatable("hud.timelocked.atlas.initializing").getString();
    g.drawString(mc.font, telemetry, x + 9, by + 7, 0xFFDEE8EF, false);
  }

  private AtlasStartupClient() {}
}
