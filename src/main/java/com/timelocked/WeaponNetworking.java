package com.timelocked;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class WeaponNetworking {
  public static void register(RegisterPayloadHandlersEvent e) {
    e.registrar("weapons-v1")
        .playToServer(
            WeaponNetworking.Reload.TYPE,
            WeaponNetworking.Reload.CODEC,
            (data, ctx) ->
                ctx.enqueueWork(
                    () -> {
                      Player p = ctx.player();
                      InteractionHand h =
                          data.offhand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
                      if (p.getItemInHand(h).getItem() instanceof GunItem gun) {
                        gun.beginReload(p, h);
                      }
                    }));
  }

  public record Reload(boolean offhand) implements CustomPacketPayload {
    public static final Type<WeaponNetworking.Reload> TYPE =
        new Type(ResourceLocation.fromNamespaceAndPath("timelocked", "reload"));
    public static final StreamCodec<RegistryFriendlyByteBuf, WeaponNetworking.Reload> CODEC =
        StreamCodec.composite(
            ByteBufCodecs.BOOL, WeaponNetworking.Reload::offhand, WeaponNetworking.Reload::new);

    public Type<WeaponNetworking.Reload> type() {
      return TYPE;
    }
  }
}
