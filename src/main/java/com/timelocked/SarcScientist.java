package com.timelocked;

import javax.annotation.Nullable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.InteractGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.LookAtTradingPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.TradeWithPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

public final class SarcScientist extends AbstractVillager {
  public static final int RESEARCHER = 0;
  public static final int ENGINEER = 1;
  public static final int MEDIC = 2;
  public static final int KINDS = 3;
  public static final String[] KIND_IDS = new String[] {"researcher", "engineer", "medic"};
  private static final EntityDataAccessor<Integer> KIND =
      SynchedEntityData.defineId(SarcScientist.class, EntityDataSerializers.INT);
  public static final String[][] LINES =
      new String[][] {
        {
          "Dr. Hale's notes say the Zero Point is stable. Dr. Hale is no longer with us.",
          "The satellites were watching the facility. Then they stopped answering.",
          "We never meant to open a door. We meant to look through a keyhole.",
          "If you find the Observation controller, do not stare into the field.",
          "The Celestials were here before us. In a sense, they were here before here."
        },
        {
          "Sneak and click the core console. That is the SCRAM. Remember it.",
          "Every core here eats something different. Uranium, solarium, chronotite. The void one eats voidsteel and hums back.",
          "If the bell starts ringing you have one minute. Do not ask me how I know.",
          "The cooling towers are just for show now. The real power went underground.",
          "Three fuel cells. Not two. I wrote it on the wall for a reason.",
          "The tram was fine until someone wired the track like an activator rail.",
          "Power plant output is zero megawatts. That is the problem.",
          "If the turbines start spinning on their own, run."
        },
        {
          "Subjects were volunteers. At first.",
          "Something in the field rewrites people. Slowly. Kindly, almost.",
          "Bandages, hot chocolate, and do not let them bite you. In that order.",
          "Site-B was sealed from the outside. Think about that.",
          "The ones in gowns are not patients anymore. Please do not try to help them."
        }
      };
  private int lineCooldown;

  public SarcScientist(EntityType<? extends SarcScientist> t, Level l) {
    super(t, l);
  }

  public static Builder attributes() {
    return Mob.createMobAttributes()
        .add(Attributes.MAX_HEALTH, 20.0)
        .add(Attributes.MOVEMENT_SPEED, 0.5)
        .add(Attributes.FOLLOW_RANGE, 24.0);
  }

  protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder b) {
    super.defineSynchedData(b);
    b.define(KIND, 0);
  }

  public int kind() {
    return Math.floorMod((Integer) this.entityData.get(KIND), 3);
  }

  public void setKind(int k) {
    this.entityData.set(KIND, Math.floorMod(k, 3));
    this.getOffers().clear();
  }

  protected void registerGoals() {
    this.goalSelector.addGoal(0, new FloatGoal(this));
    this.goalSelector.addGoal(1, new TradeWithPlayerGoal(this));
    this.goalSelector.addGoal(1, new AvoidEntityGoal(this, Monster.class, 10.0F, 0.55, 0.75));
    this.goalSelector.addGoal(1, new AvoidEntityGoal(this, FailedSubject.class, 12.0F, 0.6, 0.8));
    this.goalSelector.addGoal(1, new LookAtTradingPlayerGoal(this));
    this.goalSelector.addGoal(2, new PanicGoal(this, 0.75));
    this.goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 0.4));
    this.goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 0.35));
    this.goalSelector.addGoal(9, new InteractGoal(this, Player.class, 3.0F, 1.0F));
    this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Mob.class, 8.0F));
  }

  public SpawnGroupData finalizeSpawn(
      ServerLevelAccessor l,
      DifficultyInstance d,
      MobSpawnType type,
      @Nullable SpawnGroupData data) {
    this.setKind(this.random.nextInt(3));
    return super.finalizeSpawn(l, d, type, data);
  }

  public void addAdditionalSaveData(CompoundTag t) {
    super.addAdditionalSaveData(t);
    t.putString("Kind", KIND_IDS[this.kind()]);
  }

  public void readAdditionalSaveData(CompoundTag t) {
    super.readAdditionalSaveData(t);

    for (int i = 0; i < 3; i++) {
      if (KIND_IDS[i].equals(t.getString("Kind"))) {
        this.entityData.set(KIND, i);
      }
    }
  }

  protected Component getTypeName() {
    return Component.translatable("entity.timelocked.sarc_scientist." + KIND_IDS[this.kind()]);
  }

  public InteractionResult mobInteract(Player p, InteractionHand hand) {
    if (ResearchStory.founder(this) && p instanceof ServerPlayer player)
      return ResearchStory.interact(this, player, hand);
    if (StaffChapter.officer(this) && p instanceof ServerPlayer player)
      return StaffChapter.interact(this, player, hand);
    if (ReceptionChapter.receptionist(this) && p instanceof ServerPlayer player)
      return ReceptionChapter.interact(this, player, hand);
    ItemStack held = p.getItemInHand(hand);
    if (!held.is(Items.NAME_TAG)
        && !(held.getItem() instanceof SpawnEggItem)
        && this.isAlive()
        && !this.isTrading()
        && !this.isBaby()) {
      if (!this.level().isClientSide) {
        if (p.isSecondaryUseActive()) {
          String[] pool = LINES[this.kind()];
          p.sendSystemMessage(
              Component.literal("<")
                  .append(this.getDisplayName())
                  .append("> ")
                  .append(Component.literal(pool[this.random.nextInt(pool.length)])));
          this.playSound(SoundEvents.VILLAGER_AMBIENT, 0.6F, 1.1F);
        } else if (!this.getOffers().isEmpty()) {
          this.setTradingPlayer(p);
          this.openTradingScreen(p, this.getDisplayName(), 1);
        }
      }

      return InteractionResult.sidedSuccess(this.level().isClientSide);
    } else {
      return super.mobInteract(p, hand);
    }
  }

  private static ItemLike item(String id) {
    return (ItemLike) BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
  }

  private static MerchantOffer sell(int em, ItemLike out, int n, int uses) {
    return new MerchantOffer(
        new ItemCost(Items.EMERALD, em), new ItemStack(out, n), uses, 4, 0.05F);
  }

  private static MerchantOffer buy(ItemLike in, int n, int em, int uses) {
    return new MerchantOffer(new ItemCost(in, n), new ItemStack(Items.EMERALD, em), uses, 3, 0.05F);
  }

  public static MerchantOffers trades(int kind) {
    MerchantOffers o = new MerchantOffers();
    switch (kind) {
      case 1:
        o.add(buy(Items.COPPER_INGOT, 8, 1, 16));
        o.add(buy(Items.REDSTONE, 12, 1, 16));
        o.add(sell(3, item("timelocked:steel_plate"), 4, 12));
        o.add(sell(4, item("timelocked:circuit_board"), 2, 12));
        o.add(sell(6, item("timelocked:vehicle_wheel"), 2, 6));
        o.add(sell(10, item("timelocked:vehicle_motor"), 1, 4));
        o.add(sell(18, item("timelocked:rover_deployment"), 1, 2));
        break;
      case 2:
        o.add(buy(Items.SUGAR, 10, 1, 16));
        o.add(buy(Items.MILK_BUCKET, 1, 1, 8));
        o.add(sell(2, item("timelocked:bandage"), 3, 16));
        o.add(sell(3, item("timelocked:hot_chocolate"), 2, 12));
        o.add(sell(5, Items.GOLDEN_CARROT, 4, 12));
        o.add(sell(4, item("timelocked:estrogen"), 1, 12));
        o.add(sell(12, Items.GOLDEN_APPLE, 1, 4));
        break;
      default:
        o.add(buy(Items.PAPER, 16, 1, 16));
        o.add(buy(item("timelocked:raw_uranium"), 2, 2, 12));
        o.add(sell(4, item("timelocked:insulation_fabric"), 3, 12));
        o.add(sell(8, item("timelocked:sarc_access_card"), 1, 3));
        o.add(sell(12, item("timelocked:sealed_fuel_cell"), 1, 3));
        o.add(sell(6, Items.ENDER_PEARL, 1, 6));
    }

    return o;
  }

  protected void updateTrades() {
    this.getOffers().addAll(trades(this.kind()));
  }

  protected void rewardTradeXp(MerchantOffer offer) {
    if (this.getTradingPlayer() instanceof ServerPlayer sp) {
      Achievements.grant(sp, "peer_review");
    }

    if (offer.shouldRewardExp()) {
      this.level()
          .addFreshEntity(
              new ExperienceOrb(
                  this.level(),
                  this.getX(),
                  this.getY() + 0.5,
                  this.getZ(),
                  3 + this.random.nextInt(4)));
    }
  }

  public AgeableMob getBreedOffspring(ServerLevel l, AgeableMob other) {
    return null;
  }

  @Override
  public boolean isPushable() {
    return !ReceptionChapter.receptionist(this)
        && !StaffChapter.officer(this)
        && !ResearchStory.founder(this)
        && super.isPushable();
  }

  public boolean removeWhenFarAway(double d) {
    return false;
  }

  public void aiStep() {
    super.aiStep();
    if (!this.level().isClientSide && this.lineCooldown > 0) {
      this.lineCooldown--;
    }
  }

  public void tick() {
    super.tick();
    if (ReceptionChapter.receptionist(this)
        || StaffChapter.officer(this)
        || ResearchStory.founder(this)) return;
    if (!this.level().isClientSide && this.tickCount % 40 == 0 && this.lineCooldown == 0) {
      Player p = this.level().getNearestPlayer(this, 5.0);
      if (p != null && !p.isSpectator()) {
        this.lineCooldown = 2400;
        p.displayClientMessage(
            Component.literal("<")
                .append(this.getDisplayName())
                .append("> ")
                .append(
                    Component.literal(
                        this.kind() == 2
                            ? "Are you hurt? Are you... you?"
                            : (this.kind() == 1
                                ? "Oh thank god, someone with hands. Want to trade?"
                                : "A visitor! Sneak and right-click if you want the briefing."))),
            false);
      }
    }
  }

  protected SoundEvent getAmbientSound() {
    return this.isTrading() ? SoundEvents.VILLAGER_TRADE : null;
  }

  protected SoundEvent getHurtSound(DamageSource d) {
    return SoundEvents.PLAYER_HURT;
  }

  protected SoundEvent getDeathSound() {
    return SoundEvents.PLAYER_DEATH;
  }

  public SoundEvent getNotifyTradeSound() {
    return SoundEvents.VILLAGER_YES;
  }

  protected SoundEvent getTradeUpdatedSound(boolean yes) {
    return yes ? SoundEvents.VILLAGER_YES : SoundEvents.VILLAGER_NO;
  }
}
