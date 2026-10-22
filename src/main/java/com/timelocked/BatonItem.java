package com.timelocked;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;

public final class BatonItem extends Item {
  // Balance values: tool uses, damage bonus over iron tier, and attack-speed modifier.
  private static final int DURABILITY_USES = 512;
  private static final int ATTACK_DAMAGE_BONUS = 2;
  private static final float ATTACK_SPEED_MODIFIER = -2.2F;

  public BatonItem() {
    super(
        new Properties()
            .durability(DURABILITY_USES)
            .attributes(
                SwordItem.createAttributes(
                    Tiers.IRON, ATTACK_DAMAGE_BONUS, ATTACK_SPEED_MODIFIER)));
  }

  public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
    return true;
  }

  public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
    stack.hurtAndBreak(1, attacker, EquipmentSlot.MAINHAND);
  }

  public boolean isValidRepairItem(ItemStack stack, ItemStack ingredient) {
    return ingredient.is((Item) Content.STEEL.get());
  }
}
