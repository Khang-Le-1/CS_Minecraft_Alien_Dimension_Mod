package com.timelocked;

import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class GunItem extends Item {
  // Time values are server ticks; spatial values use world blocks unless marked pixels.
  private static final int DURABILITY_USES = 768;
  private static final int MAX_TRIGGER_HOLD_TICKS = 72000;
  private static final int EMPTY_TRIGGER_COOLDOWN_TICKS = 10;
  private static final double HITBOX_PADDING_BLOCKS = 0.3;
  private static final double TRACER_SPACING_BLOCKS = 1.5;
  private static final float ENERGY_TRACER_RED = 0.15F;
  private static final float ENERGY_TRACER_GREEN = 0.9F;
  private static final float ENERGY_TRACER_BLUE = 1.0F;
  private static final float ENERGY_TRACER_SCALE = 0.8F;
  private static final float SHOT_VOLUME = 0.35F;
  private static final float ENERGY_SHOT_PITCH = 1.65F;
  private static final float BALLISTIC_SHOT_PITCH = 1.35F;
  // Vanilla item-bar span in pixels and the existing RGB magazine colors.
  private static final float MAGAZINE_BAR_WIDTH_PIXELS = 13.0F;
  private static final int ENERGY_BAR_RGB = 3726310;
  private static final int BALLISTIC_BAR_RGB = 12166000;

  public final int capacity;
  public final int interval;
  public final int reloadTicks;
  public final int range;
  public final float damage;
  public final boolean automatic;
  public final boolean energy;
  private final Supplier<Item> ammo;

  public GunItem(
      Supplier<Item> ammo,
      int capacity,
      float damage,
      int interval,
      int reloadTicks,
      int range,
      boolean automatic,
      boolean energy) {
    super(new Properties().durability(DURABILITY_USES));
    this.ammo = ammo;
    this.capacity = capacity;
    this.damage = damage;
    this.interval = interval;
    this.reloadTicks = reloadTicks;
    this.range = range;
    this.automatic = automatic;
    this.energy = energy;
  }

  private CompoundTag data(ItemStack s) {
    return ((CustomData) s.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)).copyTag();
  }

  public int rounds(ItemStack s) {
    return Math.clamp(this.data(s).getInt("SarcMagazine"), 0, this.capacity);
  }

  public void setRounds(ItemStack s, int n) {
    CustomData.update(
        DataComponents.CUSTOM_DATA,
        s,
        t -> t.putInt("SarcMagazine", Math.clamp(n, 0, this.capacity)));
  }

  public boolean reloading(ItemStack s) {
    return this.data(s).contains("SarcReloadUntil");
  }

  public void cancelReload(ItemStack s) {
    CustomData.update(
        DataComponents.CUSTOM_DATA,
        s,
        t -> {
          t.remove("SarcReloadUntil");
          t.remove("SarcReloadOwner");
        });
  }

  private boolean eligible(Player p) {
    return p.isAlive() && !p.isSpectator() && !p.isPassenger();
  }

  public int available(Player p) {
    int n = 0;

    for (ItemStack s : p.getInventory().items) {
      if (s.is(this.ammo.get())) {
        n += s.getCount();
      }
    }

    return n;
  }

  public boolean beginReload(Player p, InteractionHand hand) {
    if (p.level() instanceof ServerLevel && this.eligible(p)) {
      ItemStack s = p.getItemInHand(hand);
      if (s.getItem() == this
          && !this.reloading(s)
          && !p.getCooldowns().isOnCooldown(this)
          && this.rounds(s) != this.capacity
          && (p.getAbilities().instabuild || this.available(p) != 0)) {
        CustomData.update(
            DataComponents.CUSTOM_DATA,
            s,
            t -> {
              t.putLong("SarcReloadUntil", p.level().getGameTime() + this.reloadTicks);
              t.putUUID("SarcReloadOwner", p.getUUID());
            });
        p.stopUsingItem();
        p.getCooldowns().addCooldown(this, this.reloadTicks);
        p.displayClientMessage(Component.translatable("message.timelocked.reloading"), true);
        return true;
      } else {
        return false;
      }
    } else {
      return false;
    }
  }

  public void advanceReload(ItemStack s, Player p, long now) {
    if (this.reloading(s)) {
      CompoundTag t = this.data(s);
      if (this.eligible(p)
          && t.hasUUID("SarcReloadOwner")
          && t.getUUID("SarcReloadOwner").equals(p.getUUID())
          && (p.getMainHandItem() == s || p.getOffhandItem() == s)) {
        if (now >= t.getLong("SarcReloadUntil")) {
          int needed = this.capacity - this.rounds(s);
          int loaded = 0;
          if (p.getAbilities().instabuild) {
            loaded = needed;
          } else {
            for (ItemStack a : p.getInventory().items) {
              if (a.is(this.ammo.get())) {
                int take = Math.min(needed - loaded, a.getCount());
                a.shrink(take);
                loaded += take;
                if (loaded == needed) {
                  break;
                }
              }
            }
          }

          this.setRounds(s, this.rounds(s) + loaded);
          this.cancelReload(s);
          p.displayClientMessage(
              Component.translatable(
                  "message.timelocked.loaded", new Object[] {this.rounds(s), this.capacity}),
              true);
        }
      } else {
        this.cancelReload(s);
      }
    }
  }

  public void inventoryTick(ItemStack s, Level l, Entity e, int slot, boolean selected) {
    if (!l.isClientSide && e instanceof Player p) {
      this.advanceReload(s, p, l.getGameTime());
    }
  }

  public InteractionResultHolder<ItemStack> use(Level l, Player p, InteractionHand h) {
    ItemStack s = p.getItemInHand(h);
    if (p.isShiftKeyDown()) {
      if (!l.isClientSide) {
        this.beginReload(p, h);
      }

      return InteractionResultHolder.sidedSuccess(s, l.isClientSide);
    } else {
      if (!this.eligible(p)) {
        return InteractionResultHolder.fail(s);
      }

      p.startUsingItem(h);
      if (!l.isClientSide) {
        this.fire(p, h);
      }

      return InteractionResultHolder.consume(s);
    }
  }

  public int getUseDuration(ItemStack s, LivingEntity e) {
    return MAX_TRIGGER_HOLD_TICKS;
  }

  public UseAnim getUseAnimation(ItemStack s) {
    return UseAnim.NONE;
  }

  public void onUseTick(Level l, LivingEntity e, ItemStack s, int remaining) {
    if (!l.isClientSide && this.automatic && e instanceof Player p && p.getUseItem() == s) {
      this.fire(p, p.getUsedItemHand());
    }
  }

  public boolean fire(Player p, InteractionHand h) {
    if (p.level() instanceof ServerLevel l && this.eligible(p)) {
      ItemStack s = p.getItemInHand(h);
      if (s.getItem() != this || this.reloading(s) || p.getCooldowns().isOnCooldown(this)) {
        return false;
      }

      if (!p.getAbilities().instabuild && this.rounds(s) == 0) {
        p.getCooldowns().addCooldown(this, EMPTY_TRIGGER_COOLDOWN_TICKS);
        p.displayClientMessage(Component.translatable("message.timelocked.empty"), true);
        return false;
      }

      Vec3 start = p.getEyePosition();
      Vec3 direction = p.getLookAngle();
      double length = 0.0;

      for (int i = 1;
          i <= this.range && l.hasChunkAt(BlockPos.containing(start.add(direction.scale(i))));
          i++) {
        length = i;
      }

      Vec3 end = start.add(direction.scale(length));
      BlockHitResult wall = l.clip(new ClipContext(start, end, Block.COLLIDER, Fluid.NONE, p));
      end = wall.getLocation();
      EntityHitResult hit =
          ProjectileUtil.getEntityHitResult(
              l,
              p,
              start,
              end,
              new AABB(start, end).inflate(HITBOX_PADDING_BLOCKS),
              e ->
                  e instanceof LivingEntity
                      && e.isAlive()
                      && e.isPickable()
                      && !e.isSpectator()
                      && !p.isAlliedTo(e));
      if (hit != null) {
        end = hit.getLocation();
        hit.getEntity().hurt(WeaponDamage.source(p, this.energy), this.damage);
      }

      if (!p.getAbilities().instabuild) {
        this.setRounds(s, this.rounds(s) - 1);
        s.hurtAndBreak(
            1, p, h == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
      }

      p.getCooldowns().addCooldown(this, this.interval);
      Vec3 span = end.subtract(start);
      int steps = Math.max(1, (int) Math.ceil(span.length() / TRACER_SPACING_BLOCKS));
      ParticleOptions tracer =
          (ParticleOptions)
              (this.energy
                  ? new DustParticleOptions(
                      new Vector3f(ENERGY_TRACER_RED, ENERGY_TRACER_GREEN, ENERGY_TRACER_BLUE),
                      ENERGY_TRACER_SCALE)
                  : ParticleTypes.CRIT);

      for (int i = 1; i <= steps; i++) {
        Vec3 point = start.add(span.scale((double) i / steps));
        l.sendParticles(tracer, point.x, point.y, point.z, 1, 0.0, 0.0, 0.0, 0.0);
      }

      l.playSound(
          null,
          p.blockPosition(),
          this.energy ? SoundEvents.BEACON_POWER_SELECT : SoundEvents.FIREWORK_ROCKET_BLAST,
          SoundSource.PLAYERS,
          SHOT_VOLUME,
          this.energy ? ENERGY_SHOT_PITCH : BALLISTIC_SHOT_PITCH);
      return true;
    } else {
      return false;
    }
  }

  public boolean isBarVisible(ItemStack s) {
    return true;
  }

  public int getBarWidth(ItemStack s) {
    return Math.round(MAGAZINE_BAR_WIDTH_PIXELS * this.rounds(s) / this.capacity);
  }

  public int getBarColor(ItemStack s) {
    return this.energy ? ENERGY_BAR_RGB : BALLISTIC_BAR_RGB;
  }

  public void appendHoverText(ItemStack s, TooltipContext c, List<Component> lines, TooltipFlag f) {
    lines.add(
        Component.translatable(
            "tooltip.timelocked.magazine", new Object[] {this.rounds(s), this.capacity}));
    lines.add(Component.translatable("tooltip.timelocked.reload"));
    lines.add(
        Component.translatable(
            this.automatic ? "tooltip.timelocked.automatic" : "tooltip.timelocked.semi"));
    lines.add(
        Component.translatable(
            "tooltip.timelocked.ammo", new Object[] {this.ammo.get().getDescription()}));
  }
}
