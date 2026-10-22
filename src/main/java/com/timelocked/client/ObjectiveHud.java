package com.timelocked.client;

import com.mojang.math.Axis;
import com.timelocked.ObjectiveNetworking;
import com.timelocked.SpaceArmorItem;
import com.timelocked.StoryObjective;
import net.minecraft.Util;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/** Compact top-center objective. Arrow is a bearing, not a wall-avoiding pathfinder. */
@EventBusSubscriber(modid = "timelocked", value = Dist.CLIENT, bus = Bus.MOD)
public final class ObjectiveHud {
  private static ObjectiveNetworking.Update current;
  private static long received;

  public static void receive(ObjectiveNetworking.Update update) {
    current = update.label().isEmpty() ? null : update;
    received = Util.getMillis();
  }

  @SubscribeEvent
  public static void register(RegisterGuiLayersEvent event) {
    event.registerAboveAll(
        ResourceLocation.fromNamespaceAndPath("timelocked", "objective_hud"), ObjectiveHud::render);
  }

  private static void render(GuiGraphics g, DeltaTracker delta) {
    Minecraft mc = Minecraft.getInstance();
    var p = mc.player;
    var data = current;
    if (p == null
        || mc.level == null
        || data == null
        || !p.isAlive()
        || p.isSpectator()
        || mc.options.hideGui
        || AtlasStartupClient.active()
        || IntroClient.locked()
        || JumpCutsceneClient.active()
        || PortalTransitClient.active()
        || Util.getMillis() - received > 5000
        || !mc.level.dimension().location().equals(data.dimension())) return;
    int width = Math.min(286, g.guiWidth() - 20);
    if (width < 100) return;
    int x = (g.guiWidth() - width) / 2;
    int y = p.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof SpaceArmorItem ? 38 : 8;
    var lines = mc.font.split(Component.translatable(data.label()), width - 18);
    int count = Math.min(2, lines.size());
    Component navigation = Component.empty();
    int distance = 0;
    double dx = 0, dz = 0;
    if (data.hasTarget()) {
      BlockPos target = BlockPos.of(data.position());
      dx = target.getX() + .5 - p.getX();
      dz = target.getZ() + .5 - p.getZ();
      distance = StoryObjective.distance(dx, dz);
      int dy = (int) Math.round(target.getY() - p.getY());
      navigation =
          distance <= 3
              ? Component.translatable("hud.timelocked.objective.nearby")
              : Component.translatable("hud.timelocked.objective.distance", distance);
      if (data.height() && Math.abs(dy) >= 4)
        navigation =
            navigation
                .copy()
                .append("  |  ")
                .append(
                    Component.translatable(
                        dy > 0
                            ? "hud.timelocked.objective.above"
                            : "hud.timelocked.objective.below",
                        Math.abs(dy)));
    }
    var navLines = mc.font.split(navigation, width - 35);
    int navCount = data.hasTarget() ? Math.min(2, navLines.size()) : 0;
    int height = 24 + count * 10 + (navCount == 0 ? 0 : 5 + navCount * 10);
    g.fill(x, y, x + width, y + height, 0xBA101721);
    g.fill(x, y, x + width, y + 2, 0xFFB885E8);
    g.drawString(
        mc.font,
        Component.translatable("hud.timelocked.objective.header"),
        x + 9,
        y + 7,
        0xFFCBA4EC,
        false);
    for (int i = 0; i < count; i++)
      g.drawString(mc.font, lines.get(i), x + 9, y + 21 + i * 10, 0xFFF0F2F5, false);
    if (!data.hasTarget()) return;
    int navY = y + 23 + count * 10;
    // A primitive arrow avoids relying on missing Unicode glyphs in resource-pack fonts.
    if (distance > 3)
      arrow(g, x + 14, navY + 4, (float) StoryObjective.bearing(dx, dz, p.getYRot()));
    else g.fill(x + 12, navY + 2, x + 16, navY + 6, 0xFF8AE8C3);
    for (int i = 0; i < navCount; i++)
      g.drawString(mc.font, navLines.get(i), x + 26, navY + i * 10, 0xFF8AE8C3, false);
  }

  private static void arrow(GuiGraphics g, int x, int y, float angle) {
    g.pose().pushPose();
    g.pose().translate(x, y, 0);
    g.pose().mulPose(Axis.ZP.rotationDegrees(angle));
    int color = 0xFF8AE8C3;
    g.fill(-1, -3, 1, 5, color);
    for (int row = 0; row < 4; row++) g.fill(-row - 1, -5 + row, row + 1, -4 + row, color);
    g.pose().popPose();
  }

  @EventBusSubscriber(modid = "timelocked", value = Dist.CLIENT)
  public static final class Lifecycle {
    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
      current = null;
      received = 0;
    }

    @SubscribeEvent
    public static void login(ClientPlayerNetworkEvent.LoggingIn event) {
      current = null;
      received = 0;
    }
  }

  private ObjectiveHud() {}
}
