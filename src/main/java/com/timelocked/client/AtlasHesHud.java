package com.timelocked.client;

import com.timelocked.SpaceArmorItem;
import com.timelocked.SpaceEvents;
import com.timelocked.SpacePlanets;
import com.timelocked.SpacePlanets.Planet;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

@EventBusSubscriber(modid = "timelocked", value = Dist.CLIENT, bus = Bus.MOD)
public final class AtlasHesHud {
  private static final ResourceLocation ID =
      ResourceLocation.fromNamespaceAndPath("timelocked", "atlas_hes_hud");

  private AtlasHesHud() {}

  @SubscribeEvent
  public static void register(RegisterGuiLayersEvent e) {
    e.registerAboveAll(ID, AtlasHesHud::render);
  }

  private static void render(GuiGraphics g, DeltaTracker dt) {
    Minecraft mc = Minecraft.getInstance();
    if (mc.player == null
        || mc.level == null
        || mc.options.hideGui
        || JumpCutsceneClient.active()
        || PortalTransitClient.active()
        || IntroClient.locked()) {
      return;
    }

    Player p = mc.player;
    if (!(p.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof SpaceArmorItem)) {
      return;
    }

    if (AtlasStartupClient.active()) {
      AtlasStartupClient.render(g);
      return;
    }

    Planet planet = Planet.of(p.level().dimension());
    boolean sealed = SpaceEvents.fullSuit(p);
    boolean lifeSupport = SpaceEvents.nearLifeSupport(p.level(), p.blockPosition());

    int W = g.guiWidth(), H = g.guiHeight();
    int healthW = Math.min(180, Math.max(84, W - 32)), hx = (W - healthW) / 2;
    float hp = Math.max(0, Math.min(1, p.getHealth() / Math.max(1, p.getMaxHealth())));
    int hpColor = hp <= .25F ? 0xFFF27178 : (hp <= .5F ? 0xFFE8C96A : 0xFF8AE8C3);
    g.fill(hx - 5, 7, hx + healthW + 5, 32, 0xA6101721);
    g.drawString(mc.font, "HEALTH", hx, 10, 0xFFDEE8EF, false);
    String value = Math.round(p.getHealth()) + " / " + Math.round(p.getMaxHealth());
    g.drawString(mc.font, value, hx + healthW - mc.font.width(value), 10, hpColor, false);
    g.fill(hx, 23, hx + healthW, 27, 0xFF34414F);
    g.fill(hx, 23, hx + Math.round(healthW * hp), 27, hpColor);
    // Short bottom-left instruments avoid the former full-height debug panel.
    int x = 8, y = Math.max(38, H - 68), w = Math.min(172, W - 16);
    g.fill(x, y, x + w, y + 58, 0xA6101721);
    g.fill(x, y, x + 2, y + 58, 0xFFB885E8);
    g.drawString(mc.font, "ATLAS / HES", x + 7, y + 5, 0xFFCBA4EC, false);
    String suit = sealed ? "SEALED" : "PARTIAL SUIT";
    g.drawString(
        mc.font,
        suit,
        x + w - 5 - mc.font.width(suit),
        y + 5,
        sealed ? 0xFF8AE8C3 : 0xFFE8C96A,
        false);
    meter(g, mc, "ARMOR", armorPercent(p), x + 7, y + 19, w - 14, 0xFFCFAD65);
    int oxygen = Math.round(p.getAirSupply() * 100F / Math.max(1, p.getMaxAirSupply()));
    meter(g, mc, "O2", oxygen, x + 7, y + 35, w - 14, oxygen < 20 ? 0xFFF27178 : 0xFF85D9EE);
    String where = locationLabel(p);
    boolean breathable =
        planet == null
            ? p.level().dimension().equals(net.minecraft.world.level.Level.OVERWORLD)
                || p.level().dimension().equals(SpacePlanets.SHIP)
            : planet.atmosphere;
    String env =
        where
            + " | "
            + String.format(java.util.Locale.ROOT, "%.2fg", planet == null ? 1F : planet.gravity)
            + " | "
            + (breathable ? "AIR" : "VACUUM");
    int max = W - 16;
    while (mc.font.width(env) > max && where.length() > 4) {
      where = where.substring(0, where.length() - 1);
      env =
          where
              + " | "
              + String.format(java.util.Locale.ROOT, "%.2fg", planet == null ? 1F : planet.gravity)
              + " | "
              + (breathable ? "AIR" : "VACUUM");
    }
    int ey = 38;
    g.fill(8, ey, Math.min(W - 8, 16 + mc.font.width(env)), ey + 15, 0x80101721);
    g.drawString(mc.font, env, 12, ey + 3, 0xFFB9CAD6, false);
    String support =
        lifeSupport
            ? "LIFE SUPPORT ONLINE"
            : (breathable ? "BREATHABLE ENVIRONMENT" : "NO LOCAL LIFE SUPPORT");
    g.drawString(mc.font, support, 12, ey + 18, lifeSupport ? 0xFF8AE8C3 : 0xFFB9CAD6, false);
    if (!breathable && !sealed && !lifeSupport) {
      String warning = "VACUUM: SEAL SUIT";
      g.drawCenteredString(mc.font, warning, W / 2, 72, 0xFFFF726A);
    }
  }

  private static void meter(
      GuiGraphics g, Minecraft mc, String label, int percent, int x, int y, int w, int color) {
    percent = Math.max(0, Math.min(100, percent));
    g.drawString(mc.font, label, x, y, 0xFFDCE5ED, false);
    String v = percent + "%";
    g.drawString(mc.font, v, x + w - mc.font.width(v), y, color, false);
    g.fill(x, y + 10, x + w, y + 12, 0xFF34414F);
    g.fill(x, y + 10, x + Math.round(w * percent / 100F), y + 12, color);
  }

  private static String locationLabel(Player p) {
    if (p.level().dimension().equals(SpacePlanets.SHIP)) {
      return "TIMESKIPPER";
    }
    Planet planet = Planet.of(p.level().dimension());
    if (planet != null) {
      return planet.title;
    }
    return p.level().dimension().location().getPath().toUpperCase(java.util.Locale.ROOT);
  }

  private static int armorPercent(Player p) {
    long total = 0L;
    long damaged = 0L;
    EquipmentSlot[] slots =
        new EquipmentSlot[] {
          EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
        };
    for (EquipmentSlot slot : slots) {
      ItemStack stack = p.getItemBySlot(slot);
      if (stack.isDamageableItem()) {
        int max = stack.getMaxDamage();
        if (max > 0) {
          total += max;
          damaged += Math.min(max, stack.getDamageValue());
        }
      }
    }
    if (total == 0L) return 100;
    return Math.round((1.0F - (float) damaged / (float) total) * 100.0F);
  }
}
