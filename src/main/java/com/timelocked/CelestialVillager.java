package com.timelocked;

import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.entity.Entity;
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
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.TradeWithPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Creeper;
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

public final class CelestialVillager extends AbstractVillager {
  public static final int STARWEAVER = 0;
  public static final int DUSKWING = 1;
  public static final int GUARDIAN = 2;
  public static final int MOONWARDEN = 3;
  public static final int STARSEER = 4;
  public static final int LUNA = 5;
  public static final int KINDS = 6;
  public static final int DARKFANG_KINDS = 3;
  public static final String[] KIND_IDS =
      new String[] {
        "starweaver",
        "duskwing",
        "star_guardian",
        "ecliptic_moonwarden",
        "ecliptic_starseer",
        "luna"
      };
  private static final EntityDataAccessor<Integer> JOB =
      SynchedEntityData.defineId(CelestialVillager.class, EntityDataSerializers.INT);
  private net.minecraft.core.BlockPos workplace;
  private long restockDay = -1;
  private int restocks;
  private long lastRestock = -2400;
  private int occupationClock;
  private static final EntityDataAccessor<Integer> KIND =
      SynchedEntityData.defineId(CelestialVillager.class, EntityDataSerializers.INT);
  static final String[] DARKFANG_LORE =
      new String[] {
        "We are House Darkfang. What survived the burning survived together.",
        "Our friends of House Ecliptic keep watch from the Moon. When our sky burned, their lights never went out.",
        "If you ever reach Luna, tell the court that Darkfang still sings the old night songs.",
        "Void crystals are our coin. House Ecliptic honours it too.",
        "The Moon pavilion here was a gift from Luna, long ago."
      };
  static final String[] ECLIPTIC_LORE =
      new String[] {
        "House Ecliptic keeps the long night. The Moon has always been ours.",
        "Darkfang are kin in all but name. Their coin is good here.",
        "The moonbreath lanterns hold the Moon's air. Stay in their light, little Earthling.",
        "Luna rules from the crescent throne. Mind your manners.",
        "Every eclipse is a door. Most of them stay shut."
      };
  static final String[] LUNA_LORE =
      new String[] {
        "Welcome to the court of House Ecliptic. I am Luna.",
        "House Darkfang are our oldest friends. Any friend of theirs may walk beneath my dome.",
        "You came in SARC's little ship. Few of your kind look up long enough to arrive.",
        "The Moon remembers every eclipse. One day it may remember you.",
        "Darkfang lost their world. We will not lose ours."
      };

  public CelestialVillager(EntityType<? extends CelestialVillager> t, Level l) {
    super(t, l);
    if (getNavigation()
        instanceof net.minecraft.world.entity.ai.navigation.GroundPathNavigation nav)
      nav.setCanOpenDoors(true);
  }

  public static Builder attributes() {
    return Mob.createMobAttributes()
        .add(Attributes.ARMOR, 0.0)
        .add(Attributes.MAX_HEALTH, 24.0)
        .add(Attributes.MOVEMENT_SPEED, 0.5)
        .add(Attributes.ATTACK_DAMAGE, 5.0)
        .add(Attributes.FOLLOW_RANGE, 24.0);
  }

  protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder b) {
    super.defineSynchedData(b);
    b.define(KIND, 0);
    b.define(JOB, -1);
  }

  public int kind() {
    return Math.floorMod((Integer) this.entityData.get(KIND), 6);
  }

  public void setKind(int k) {
    k = Math.floorMod(k, 6);
    this.entityData.set(KIND, k);

    this.getAttribute(Attributes.MAX_HEALTH)
        .setBaseValue(
            switch (k) {
              case 2 -> 40.0;
              case 3 -> 50.0;
              default -> 24.0;
              case 5 -> 250.0;
            });
    this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(k == 5 ? 14.0 : (k == 3 ? 7.0 : 5.0));
    this.getAttribute(Attributes.ARMOR).setBaseValue(k == 5 ? 12.0 : (k == 3 ? 6.0 : 0.0));
    this.setHealth(this.getMaxHealth());
  }

  public boolean guardian() {
    int k = this.kind();
    return k == 2 || k == 3 || k == 5;
  }

  public boolean ecliptic() {
    return this.kind() >= 3;
  }

  public String house() {
    return this.ecliptic() ? "Ecliptic" : "Darkfang";
  }

  protected void registerGoals() {
    this.goalSelector.addGoal(0, new FloatGoal(this));
    this.goalSelector.addGoal(1, new TradeWithPlayerGoal(this));
    this.goalSelector.addGoal(
        1,
        new AvoidEntityGoal(
            this, Monster.class, e -> !this.guardian(), 8.0F, 0.55, 0.7, e -> true));
    this.goalSelector.addGoal(1, new LookAtTradingPlayerGoal(this));
    this.goalSelector.addGoal(
        2,
        new MeleeAttackGoal(this, 0.75, true) {
          public boolean canUse() {
            return CelestialVillager.this.guardian() && super.canUse();
          }

          public boolean canContinueToUse() {
            return CelestialVillager.this.guardian() && super.canContinueToUse();
          }
        });
    this.goalSelector.addGoal(
        3,
        new PanicGoal(this, 0.7) {
          protected boolean shouldPanic() {
            return !CelestialVillager.this.guardian() && super.shouldPanic();
          }
        });
    this.goalSelector.addGoal(4, new net.minecraft.world.entity.ai.goal.OpenDoorGoal(this, true));
    this.goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 0.4));
    this.goalSelector.addGoal(6, new WorkGoal());
    this.goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 0.4));
    this.goalSelector.addGoal(9, new InteractGoal(this, Player.class, 3.0F, 1.0F));
    this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Mob.class, 8.0F));
    this.targetSelector.addGoal(
        1,
        (new HurtByTargetGoal(this) {
              public boolean canUse() {
                return CelestialVillager.this.guardian() && super.canUse();
              }
            })
            .setAlertOthers(new Class[] {CelestialVillager.class}));
    this.targetSelector.addGoal(
        2,
        new NearestAttackableTargetGoal(
            this, Monster.class, 10, true, false, e -> this.guardian() && !(e instanceof Creeper)));
  }

  public SpawnGroupData finalizeSpawn(
      ServerLevelAccessor l,
      DifficultyInstance d,
      MobSpawnType type,
      @Nullable SpawnGroupData data) {
    this.setKind(
        l.getLevel().dimension().equals(SpacePlanets.Planet.LUNA.dimension())
            ? 3 + this.random.nextInt(2)
            : this.random.nextInt(3));
    return super.finalizeSpawn(l, d, type, data);
  }

  public void addAdditionalSaveData(CompoundTag t) {
    super.addAdditionalSaveData(t);
    t.putString("Kind", KIND_IDS[this.kind()]);
    if (workplace != null) {
      t.putLong("CelestialWorkplace", workplace.asLong());
      t.putString("CelestialJob", job().id);
    }
    t.putLong("CelestialRestockDay", restockDay);
    t.putInt("CelestialRestocks", restocks);
    t.putLong("CelestialLastRestock", lastRestock);
  }

  public void readAdditionalSaveData(CompoundTag t) {
    super.readAdditionalSaveData(t);
    float hp = this.getHealth();

    for (int i = 0; i < 6; i++) {
      if (KIND_IDS[i].equals(t.getString("Kind"))) {
        this.setKind(i);
      }
    }

    workplace = null;
    this.entityData.set(JOB, -1);
    if (t.contains("CelestialWorkplace"))
      for (CelestialJobs.Role role : CelestialJobs.Role.values())
        if (role.id.equals(t.getString("CelestialJob"))) {
          workplace = net.minecraft.core.BlockPos.of(t.getLong("CelestialWorkplace"));
          this.entityData.set(JOB, role.ordinal());
        }
    restockDay = t.contains("CelestialRestockDay") ? t.getLong("CelestialRestockDay") : -1;
    restocks = t.getInt("CelestialRestocks");
    lastRestock = t.contains("CelestialLastRestock") ? t.getLong("CelestialLastRestock") : -2400;
    if (hp > 0.0F) {
      this.setHealth(Math.min(hp, this.getMaxHealth()));
    }
  }

  protected Component getTypeName() {
    Component base = Component.translatable("entity.timelocked.celestial." + KIND_IDS[this.kind()]);
    return job() == null ? base : base.copy().append(" · " + job().title);
  }

  public String[] lore() {
    return this.kind() == 5 ? LUNA_LORE : (this.ecliptic() ? ECLIPTIC_LORE : DARKFANG_LORE);
  }

  public InteractionResult mobInteract(Player p, InteractionHand hand) {
    ItemStack held = p.getItemInHand(hand);
    if (p instanceof ServerPlayer sp && this.kind() == 5) {
      Achievements.grant(sp, "audience_with_luna");
    }

    if (p.isShiftKeyDown() && held.isEmpty() && this.isAlive() && this.getTarget() == null) {
      if (!this.level().isClientSide) {
        if (job() != null)
          p.sendSystemMessage(
              Component.literal(
                  job().title
                      + ": "
                      + job().hint
                      + " Work hours: 1,000–11,000 day ticks; trade restocks require attending the station."));
        String[] l = this.lore();
        p.sendSystemMessage(
            Component.literal("<")
                .append(this.getDisplayName())
                .append("> " + l[this.random.nextInt(l.length)])
                .withStyle(this.ecliptic() ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.AQUA));
        this.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.8F, this.ecliptic() ? 0.9F : 1.3F);
      }

      return InteractionResult.sidedSuccess(this.level().isClientSide);
    } else if (!held.is(Items.NAME_TAG)
        && !(held.getItem() instanceof SpawnEggItem)
        && this.isAlive()
        && !this.isTrading()
        && !this.isBaby()
        && this.getTarget() == null) {
      if (!this.level().isClientSide && !this.getOffers().isEmpty()) {
        this.setTradingPlayer(p);
        this.openTradingScreen(p, this.getDisplayName(), 1);
      }

      return InteractionResult.sidedSuccess(this.level().isClientSide);
    } else {
      return super.mobInteract(p, hand);
    }
  }

  private static ItemStack s(ItemLike i, int n) {
    return new ItemStack(i, n);
  }

  private static ItemLike item(String id) {
    return (ItemLike) BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
  }

  private static MerchantOffer sell(int shards, ItemLike out, int n, int uses) {
    return new MerchantOffer(
        new ItemCost((ItemLike) VoidspaceContent.CRYSTAL_CLUSTER.get(), shards),
        s(out, n),
        uses,
        3,
        0.05F);
  }

  private static MerchantOffer buy(ItemLike in, int n, int shards, int uses) {
    return new MerchantOffer(
        new ItemCost(in, n),
        s((ItemLike) VoidspaceContent.CRYSTAL_CLUSTER.get(), shards),
        uses,
        2,
        0.05F);
  }

  public static MerchantOffers trades(int kind) {
    MerchantOffers o = new MerchantOffers();
    switch (kind) {
      case 1:
        o.add(buy((ItemLike) VoidspaceContent.SPORE_CAP.get(), 4, 1, 16));
        o.add(buy(item("timelocked:circuit_board"), 1, 3, 8));
        o.add(sell(6, item("timelocked:raw_luminite"), 2, 8));
        o.add(sell(5, item("timelocked:raw_aetherite"), 1, 8));
        o.add(sell(8, item("timelocked:raw_necronium"), 2, 6));
        o.add(sell(10, item("timelocked:celestrium_ingot"), 1, 4));
        break;
      case 2:
        o.add(buy(item("timelocked:titanium_plate"), 2, 4, 8));
        o.add(buy(item("timelocked:steel_ingot"), 3, 2, 12));
        o.add(sell(6, item("timelocked:light_round"), 12, 12));
        o.add(sell(4, item("timelocked:bandage"), 2, 12));
        o.add(sell(10, item("timelocked:celestrium_plate"), 1, 4));
        o.add(sell(24, Items.TOTEM_OF_UNDYING, 1, 1));
        break;
      case 3:
        o.add(buy(item("timelocked:titanium_plate"), 2, 4, 8));
        o.add(buy(item("timelocked:lunar_regolith"), 16, 1, 16));
        o.add(sell(5, item("timelocked:light_round"), 12, 12));
        o.add(sell(8, item("timelocked:chronotite_ingot"), 1, 8));
        o.add(sell(12, item("timelocked:ecliptite_plate"), 1, 4));
        o.add(sell(14, (ItemLike) SpaceContent.MOONBREATH.get(), 1, 4));
        break;
      case 4:
        o.add(buy(item("timelocked:lunar_rock"), 16, 1, 16));
        o.add(buy(Items.ENDER_PEARL, 2, 3, 8));
        o.add(sell(6, item("timelocked:raw_ecliptite"), 2, 8));
        o.add(sell(7, item("timelocked:chronotite_ingot"), 1, 8));
        o.add(sell(3, (ItemLike) VoidspaceContent.STAR_BLOOM.get(), 3, 12));
        o.add(sell(5, Items.ENDER_PEARL, 2, 8));
        break;
      case 5:
        o.add(buy(item("timelocked:circuit_board"), 2, 3, 8));
        o.add(sell(10, (ItemLike) SpaceContent.MOONBREATH.get(), 1, 6));
        o.add(sell(40, item("timelocked:chronotite_block"), 1, 3));
        o.add(sell(20, item("timelocked:ecliptite_ingot"), 3, 4));
        o.add(sell(24, Items.TOTEM_OF_UNDYING, 1, 2));
        o.add(sell(48, Items.ELYTRA, 1, 1));
        break;
      default:
        o.add(buy((ItemLike) VoidspaceContent.GLIMMER_MOSS.get(), 3, 1, 16));
        o.add(buy(Items.LEATHER, 2, 1, 12));
        o.add(sell(2, (ItemLike) VoidspaceContent.STAR_BLOOM.get(), 3, 12));
        o.add(sell(3, item("timelocked:bandage"), 2, 12));
        o.add(sell(4, Items.GLOW_BERRIES, 6, 12));
        o.add(sell(8, Items.GOLDEN_APPLE, 1, 4));
    }

    return o;
  }

  protected void updateTrades() {
    this.getOffers().addAll(job() == null ? trades(this.kind()) : CelestialJobs.trades(job()));
  }

  protected void rewardTradeXp(MerchantOffer offer) {
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

  public CelestialJobs.Role job() {
    return CelestialJobs.role(this.entityData.get(JOB));
  }

  public net.minecraft.core.BlockPos workplace() {
    return workplace;
  }

  boolean workHours() {
    long t = Math.floorMod(level().getServer().overworld().getDayTime(), 24000L);
    return t >= 1000 && t < 11000;
  }

  boolean claimWorkstation(CelestialWorkstationBlock.Station station) {
    if (ecliptic() || isBaby() || isTrading() || !station.claim(getUUID())) return false;
    if (workplace != null
        && !workplace.equals(station.getBlockPos())
        && level().getBlockEntity(workplace) instanceof CelestialWorkstationBlock.Station old)
      old.release(getUUID());
    boolean changed = job() != station.role();
    workplace = station.getBlockPos().immutable();
    entityData.set(JOB, station.role().ordinal());
    if (changed) this.offers = null;
    return true;
  }

  private void loseJob() {
    workplace = null;
    entityData.set(JOB, -1);
    this.offers = null;
    getNavigation().stop();
  }

  void tickOccupation() {
    if (!(level() instanceof ServerLevel l)
        || ecliptic()
        || isBaby()
        || isTrading()
        || getTarget() != null) return;
    if (workplace != null) {
      if (!l.hasChunkAt(workplace)) return; // unloaded is not destroyed
      if (!(l.getBlockEntity(workplace) instanceof CelestialWorkstationBlock.Station station)
          || station.role() != job()
          || !station.claim(getUUID())) {
        loseJob();
        return;
      }
      if (workHours() && distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(workplace)) < 16)
        restockAtWork();
      return;
    }
    if (!workHours()) return;
    java.util.List<CelestialWorkstationBlock.Station> candidates = new java.util.ArrayList<>();
    int cx = blockPosition().getX() >> 4, cz = blockPosition().getZ() >> 4;
    for (int x = cx - 6; x <= cx + 6; x++)
      for (int z = cz - 6; z <= cz + 6; z++) {
        var chunk = l.getChunkSource().getChunkNow(x, z);
        if (chunk == null) continue;
        for (var be : chunk.getBlockEntities().values())
          if (be instanceof CelestialWorkstationBlock.Station station
              && station.available(getUUID())
              && station.getBlockPos().distSqr(blockPosition()) <= 96 * 96) candidates.add(station);
      }
    candidates.sort(
        java.util.Comparator.comparingDouble(st -> st.getBlockPos().distSqr(blockPosition())));
    for (var station : candidates) {
      for (net.minecraft.core.Direction d : net.minecraft.core.Direction.Plane.HORIZONTAL) {
        var stand = station.getBlockPos().relative(d);
        var path = getNavigation().createPath(stand, 0);
        if (path != null && path.canReach() && claimWorkstation(station)) return;
      }
    }
  }

  void restockAtWork() {
    long day = level().getServer().overworld().getDayTime() / 24000;
    if (day != restockDay) {
      restockDay = day;
      restocks = 0;
    }
    long now = level().getGameTime();
    if (restocks >= 2
        || now - lastRestock < 2400
        || getOffers().stream().noneMatch(o -> o.getUses() > 0)) return;
    getOffers().forEach(MerchantOffer::resetUses);
    restocks++;
    lastRestock = now;
  }

  private final class WorkGoal extends net.minecraft.world.entity.ai.goal.Goal {
    private int timer;

    WorkGoal() {
      setFlags(java.util.EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    public boolean canUse() {
      return workplace != null
          && workHours()
          && !isTrading()
          && getTarget() == null
          && !isBaby()
          && level().hasChunkAt(workplace)
          && level().getBlockEntity(workplace) instanceof CelestialWorkstationBlock.Station;
    }

    public boolean canContinueToUse() {
      return canUse();
    }

    public void stop() {
      getNavigation().stop();
    }

    public void tick() {
      getLookControl()
          .setLookAt(workplace.getX() + .5, workplace.getY() + .9, workplace.getZ() + .5);
      if (++timer % 40 != 0) return;
      if (distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(workplace)) < 7) {
        getNavigation().stop();
        return;
      }
      net.minecraft.world.level.pathfinder.Path best = null;
      for (net.minecraft.core.Direction d : net.minecraft.core.Direction.Plane.HORIZONTAL) {
        var path = getNavigation().createPath(workplace.relative(d), 0);
        if (path != null
            && path.canReach()
            && (best == null || path.getNodeCount() < best.getNodeCount())) best = path;
      }
      if (best != null) getNavigation().moveTo(best, .45);
    }
  }

  public AgeableMob getBreedOffspring(ServerLevel l, AgeableMob other) {
    return null;
  }

  public boolean removeWhenFarAway(double d) {
    return false;
  }

  public boolean doHurtTarget(Entity e) {
    boolean hit = super.doHurtTarget(e);
    if (hit) {
      this.playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 0.8F, 1.4F);
    }

    return hit;
  }

  protected SoundEvent getAmbientSound() {
    return this.isTrading() ? SoundEvents.ALLAY_ITEM_GIVEN : SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM;
  }

  protected SoundEvent getHurtSound(DamageSource d) {
    return this.kind() == 5 ? SoundEvents.AMETHYST_BLOCK_BREAK : SoundEvents.ALLAY_HURT;
  }

  protected SoundEvent getDeathSound() {
    return SoundEvents.ALLAY_DEATH;
  }

  public SoundEvent getNotifyTradeSound() {
    return SoundEvents.AMETHYST_BLOCK_CHIME;
  }

  protected SoundEvent getTradeUpdatedSound(boolean yes) {
    return yes ? SoundEvents.AMETHYST_BLOCK_CHIME : SoundEvents.VILLAGER_NO;
  }

  public float getVoicePitch() {
    return switch (this.kind()) {
      case 1 -> 0.85F;
      default -> 1.15F;
      case 3 -> 0.9F;
      case 5 -> 0.75F;
    };
  }

  protected float getSoundVolume() {
    return 0.6F;
  }

  public void aiStep() {
    super.aiStep();
    if (!level().isClientSide && ++occupationClock >= 100) {
      occupationClock = 0;
      tickOccupation();
    }
    if (this.level().isClientSide && this.random.nextInt(this.kind() == 1 ? 30 : 18) == 0) {
      this.level()
          .addParticle(
              this.kind() == 5 ? ParticleTypes.GLOW : ParticleTypes.END_ROD,
              this.getRandomX(0.6),
              this.getY() + this.getBbHeight() * this.random.nextDouble(),
              this.getRandomZ(0.6),
              0.0,
              0.01,
              0.0);
    }
  }
}
