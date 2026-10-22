package com.timelocked.client.mixin;

import com.timelocked.client.PortalTransitClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Empty sky sections can be uncompiled immediately after a dimension change. The cinematic's player
 * must not disappear while terrain compilation catches up. This changes only the render eligibility
 * query at the local player's exact cell; it does not mark any terrain compiled or load chunks.
 */
@Mixin(LevelRenderer.class)
public abstract class PortalPlayerVisibilityMixin {
  @Inject(
      method = "isSectionCompiled(Lnet/minecraft/core/BlockPos;)Z",
      at = @At("HEAD"),
      cancellable = true)
  private void timelocked$showTransitPlayer(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
    var player = Minecraft.getInstance().player;
    if (PortalTransitClient.actor(player) && player != null && pos.equals(player.blockPosition()))
      cir.setReturnValue(true);
  }
}
