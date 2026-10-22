package com.timelocked;

import com.mojang.serialization.MapCodec;
import java.util.UUID;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

/** Both a usable player station and a persistent, exclusive Celestial workplace. */
public final class CelestialWorkstationBlock extends Block implements EntityBlock {
  private static final int TICKS_PER_SECOND = 20;
  // Balance values agree with the occupation hints; health uses HP (two HP per heart).
  private static final int BLOOMS_PER_BANDAGE_BATCH = 2;
  private static final int BANDAGES_PER_BATCH = 2;
  private static final int CLUSTERS_PER_CRYSTAL_BLOCK = 9;
  private static final int BERRIES_PER_MEAL = 4;
  private static final int POTATOES_PER_MEAL = 2;
  private static final int ATTUNEMENT_CLUSTER_COST = 3;
  private static final int NIGHT_VISION_DURATION_TICKS = 120 * TICKS_PER_SECOND;
  private static final int RESISTANCE_DURATION_TICKS = 60 * TICKS_PER_SECOND;
  private static final int ATTUNEMENT_COOLDOWN_TICKS = 60 * TICKS_PER_SECOND;
  private static final int MEDICAL_COOLDOWN_TICKS = 10 * TICKS_PER_SECOND;
  private static final float TREATMENT_HEALTH_POINTS = 6;
  // Do not release a missing resident's workplace immediately while chunks are loading.
  private static final int OWNER_ABSENCE_GRACE_TICKS = 120 * TICKS_PER_SECOND;
  // Collision cuboid in Minecraft model pixels; keep it separate from interaction rules.
  private static final VoxelShape WORKTOP_SHAPE = Block.box(1, 0, 1, 15, 15, 15);

  public final CelestialJobs.Role role;

  public CelestialWorkstationBlock(CelestialJobs.Role role, Properties p) {
    super(p);
    this.role = role;
  }

  protected MapCodec<? extends Block> codec() {
    return simpleCodec(p -> new CelestialWorkstationBlock(role, p));
  }

  protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
    return WORKTOP_SHAPE;
  }

  public BlockEntity newBlockEntity(BlockPos p, BlockState s) {
    return new Station(p, s);
  }

  protected ItemInteractionResult useItemOn(
      ItemStack stack,
      BlockState s,
      Level l,
      BlockPos p,
      Player user,
      InteractionHand hand,
      BlockHitResult hit) {
    if (hand == InteractionHand.MAIN_HAND && !l.isClientSide) use(user, l, p);
    return ItemInteractionResult.sidedSuccess(l.isClientSide);
  }

  protected InteractionResult useWithoutItem(
      BlockState s, Level l, BlockPos p, Player user, BlockHitResult hit) {
    if (!l.isClientSide) use(user, l, p);
    return InteractionResult.sidedSuccess(l.isClientSide);
  }

  static boolean propagate(ItemStack stack) {
    return stack.is(VoidspaceContent.MOON_FERN.get().asItem())
        || stack.is(VoidspaceContent.LANTERN_BLOOM.get().asItem())
        || stack.is(VoidspaceContent.PRISM_REED.get().asItem())
        || stack.is(VoidspaceContent.DUSK_LOTUS.get().asItem())
        || stack.is(VoidspaceContent.STAR_BLOOM.get().asItem())
        || stack.is(Content.CELESTIAL_FROND.get().asItem());
  }

  private boolean exchange(Player p, ItemLike input, int count, ItemLike result, int output) {
    ItemStack held = p.getMainHandItem();
    if (!held.is(input.asItem()) || held.getCount() < count) return false;
    held.consume(count, p);
    ItemStack out = new ItemStack(result, output);
    if (!p.getInventory().add(out)) p.drop(out, false);
    return true;
  }

  /** Server-only entry point, also exercised by native interaction tests. */
  public boolean use(Player p, Level l, BlockPos pos) {
    if (l.isClientSide) return false;
    ItemStack held = p.getMainHandItem();
    boolean success = false;
    String message = null;
    if (p.isShiftKeyDown()) {
      p.sendSystemMessage(Component.literal(role.title + ": " + role.hint));
      return false;
    }
    long now = l.getServer().overworld().getGameTime();
    switch (role) {
      case HERBALIST ->
          success =
              exchange(
                  p,
                  VoidspaceContent.STAR_BLOOM.get(),
                  BLOOMS_PER_BANDAGE_BATCH,
                  Content.BANDAGE.get(),
                  BANDAGES_PER_BATCH);
      case CRYSTAL_SHAPER -> {
        success =
            exchange(
                p,
                VoidspaceContent.CRYSTAL_CLUSTER.get(),
                CLUSTERS_PER_CRYSTAL_BLOCK,
                VoidspaceContent.CRYSTAL_BLOCK.get(),
                1);
        if (!success)
          success =
              exchange(
                  p,
                  VoidspaceContent.CRYSTAL_BLOCK.get(),
                  1,
                  VoidspaceContent.CRYSTAL_CLUSTER.get(),
                  CLUSTERS_PER_CRYSTAL_BLOCK);
      }
      case GARDENER -> {
        ItemStack seed = p.getOffhandItem();
        if (held.is(Items.BONE_MEAL) && propagate(seed)) {
          ItemStack out = seed.copyWithCount(1);
          held.consume(1, p);
          if (!p.getInventory().add(out)) p.drop(out, false);
          success = true;
        }
      }
      case COOK ->
          success =
              exchange(
                  p, Items.GLOW_BERRIES, BERRIES_PER_MEAL, Items.BAKED_POTATO, POTATOES_PER_MEAL);
      case ATTUNER -> {
        String key = "timelocked_attunement_ready";
        if (p.getPersistentData().getLong(key) > now)
          message =
              "Altar recovering; try again in "
                  + ((p.getPersistentData().getLong(key) - now + TICKS_PER_SECOND - 1)
                      / TICKS_PER_SECOND)
                  + " seconds.";
        else if (held.is(VoidspaceContent.CRYSTAL_CLUSTER.get().asItem())
            && held.getCount() >= ATTUNEMENT_CLUSTER_COST) {
          held.consume(ATTUNEMENT_CLUSTER_COST, p);
          p.addEffect(
              new MobEffectInstance(MobEffects.NIGHT_VISION, NIGHT_VISION_DURATION_TICKS, 0));
          p.addEffect(
              new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, RESISTANCE_DURATION_TICKS, 0));
          p.getPersistentData().putLong(key, now + ATTUNEMENT_COOLDOWN_TICKS);
          success = true;
        }
      }
      case WARDEN -> {
        p.sendSystemMessage(
            Component.literal(
                "Sanctuary plaza: X -240, Z 180. East gate: X -157, Z 180. Follow the outer approach to the return portal at X 0, Z 0. Carry food and bandages beyond the refuge."));
        return true;
      }
      case MEDIC -> {
        String key = "timelocked_medical_ready";
        if (p.getPersistentData().getLong(key) > now)
          message = "Medical station recovering; try again shortly.";
        else if (p.getHealth() >= p.getMaxHealth()) message = "No treatment needed.";
        else if (held.is(Content.BANDAGE.get())) {
          held.consume(1, p);
          p.heal(TREATMENT_HEALTH_POINTS);
          p.getPersistentData().putLong(key, now + MEDICAL_COOLDOWN_TICKS);
          success = true;
        }
      }
    }
    p.sendSystemMessage(
        Component.literal(
            success ? role.title + ": complete." : message != null ? message : role.hint));
    return success;
  }

  public static final class Station extends BlockEntity {
    private UUID owner;
    private long lastSeen;

    public Station(BlockPos p, BlockState s) {
      super(CelestialJobs.ENTITY.get(), p, s);
    }

    public UUID owner() {
      return owner;
    }

    public CelestialJobs.Role role() {
      return ((CelestialWorkstationBlock) getBlockState().getBlock()).role;
    }

    public boolean available(UUID who) {
      if (owner == null || owner.equals(who)) return true;
      if (level instanceof ServerLevel l) {
        var e = l.getEntity(owner);
        if (e instanceof CelestialVillager v && v.isAlive()) return false;
        return l.getGameTime() - lastSeen > OWNER_ABSENCE_GRACE_TICKS;
      }
      return false;
    }

    public boolean claim(UUID who) {
      if (!available(who)) return false;
      owner = who;
      lastSeen = level.getGameTime();
      setChanged();
      return true;
    }

    public void release(UUID who) {
      if (who.equals(owner)) {
        owner = null;
        setChanged();
      }
    }

    protected void saveAdditional(CompoundTag t, HolderLookup.Provider provider) {
      super.saveAdditional(t, provider);
      if (owner != null) t.putUUID("CelestialOwner", owner);
      t.putLong("LastSeen", lastSeen);
    }

    protected void loadAdditional(CompoundTag t, HolderLookup.Provider provider) {
      super.loadAdditional(t, provider);
      owner = t.hasUUID("CelestialOwner") ? t.getUUID("CelestialOwner") : null;
      lastSeen = t.getLong("LastSeen");
    }
  }
}
