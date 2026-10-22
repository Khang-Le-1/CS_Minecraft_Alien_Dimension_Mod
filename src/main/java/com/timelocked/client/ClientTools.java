package com.timelocked.client;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.timelocked.ReactorFrame;
import com.timelocked.SarcContent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RenderHighlightEvent.Block;

@EventBusSubscriber(modid = "timelocked", value = Dist.CLIENT)
public final class ClientTools {
  @SubscribeEvent
  public static void highlight(Block e) {
    ClientLevel l = Minecraft.getInstance().level;
    if (l != null) {
      BlockPos p = e.getTarget().getBlockPos();
      BlockState s = l.getBlockState(p);
      if (s.is((net.minecraft.world.level.block.Block) SarcContent.CASING.get())
          || ReactorFrame.isCore(s)) {
        BlockPos c = ReactorFrame.isCore(s) ? p : ReactorFrame.findCore(l, p);
        if (c != null) {
          Vec3 cam = e.getCamera().getPosition();
          e.setCanceled(true);
          LevelRenderer.renderLineBox(
              e.getPoseStack(),
              e.getMultiBufferSource().getBuffer(RenderType.lines()),
              ReactorFrame.shape(l, c, l.getBlockState(c)).box(c).move(-cam.x, -cam.y, -cam.z),
              0.0F,
              0.0F,
              0.0F,
              0.4F);
        }
      }
    }
  }

  @SubscribeEvent
  public static void commands(RegisterClientCommandsEvent e) {
    e.getDispatcher()
        .register(
            (LiteralArgumentBuilder)
                ((LiteralArgumentBuilder) Commands.literal("panorama").executes(c -> capture(1024)))
                    .then(
                        Commands.argument("size", IntegerArgumentType.integer(256, 4096))
                            .executes(c -> capture(IntegerArgumentType.getInteger(c, "size")))));
  }

  static int capture(int size) {
    Minecraft mc = Minecraft.getInstance();
    mc.execute(
        () -> {
          Component msg = mc.grabPanoramixScreenshot(mc.gameDirectory, size, size);
          if (msg != null) {
            mc.gui.getChat().addMessage(msg);
          }
        });
    return 1;
  }

  private ClientTools() {}
}
