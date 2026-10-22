package com.timelocked;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;

public final class WeaponEvents {
  public static void register() {
    NeoForge.EVENT_BUS.addListener(WeaponEvents::logout);
  }

  private static void logout(PlayerLoggedOutEvent e) {
    Player p = e.getEntity();
    p.stopUsingItem();

    for (ItemStack s : p.getInventory().items) {
      if (s.getItem() instanceof GunItem g) {
        g.cancelReload(s);
      }
    }

    for (ItemStack s : p.getInventory().offhand) {
      if (s.getItem() instanceof GunItem g) {
        g.cancelReload(s);
      }
    }
  }
}
