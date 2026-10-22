package com.timelocked;

import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorItem.Type;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public final class SpaceArmorItem extends ArmorItem {
  private final boolean personal;

  public SpaceArmorItem(Holder<ArmorMaterial> m, Type t, Properties p) {
    this(m, t, p, false);
  }

  public SpaceArmorItem(Holder<ArmorMaterial> m, Type t, Properties p, boolean personal) {
    super(m, t, p);
    this.personal = personal;
  }

  public boolean personal() {
    return this.personal;
  }

  public void appendHoverText(ItemStack s, TooltipContext c, List<Component> lines, TooltipFlag f) {
    lines.add(Component.translatable("tooltip.timelocked.space_armor"));
    if (this.personal) {
      lines.add(Component.translatable("tooltip.timelocked.atlas_personal"));
    }
  }
}
