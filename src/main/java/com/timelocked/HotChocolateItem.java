package com.timelocked;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;

public final class HotChocolateItem extends Item {
  public HotChocolateItem(Properties p) {
    super(p);
  }

  public UseAnim getUseAnimation(ItemStack stack) {
    return UseAnim.DRINK;
  }
}
