package com.timelocked;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent.Post;

public final class SatelliteCrash {
  public static final String TAG_DONE = "TimelockedSatelliteCrash";
  public static final String TAG_TIMER = "TimelockedSatelliteTimer";
  public static final String TAG_SITE = "TimelockedSatelliteSite";
  public static final int DELAY_TICKS = 3600;
  public static final int FALL_TICKS = 80;
  public static final ResourceKey<LootTable> LOOT =
      ResourceKey.create(
          Registries.LOOT_TABLE,
          ResourceLocation.fromNamespaceAndPath("timelocked", "chests/satellite_wreck"));
  private static final Style SARC = Style.EMPTY.withColor(TextColor.fromRgb(11898111));
  private static final List<SatelliteCrash.Fall> FALLS = new ArrayList<>();

  public static void register() {
    NeoForge.EVENT_BUS.addListener(SatelliteCrash::tick);
    NeoForge.EVENT_BUS.addListener(
        (ServerStoppingEvent e) -> {
          for (SatelliteCrash.Fall f : FALLS) {
            f.parts.forEach(Entity::discard);
          }

          FALLS.clear();
        });
  }

  private static CompoundTag data(Player p) {
    CompoundTag root = p.getPersistentData();
    if (!root.contains("PlayerPersisted")) {
      root.put("PlayerPersisted", new CompoundTag());
    }

    return root.getCompound("PlayerPersisted");
  }

  public static void playerTick(ServerPlayer p) {
    CompoundTag d = data(p);
    if (d.contains("TimelockedSatelliteSite") && !Achievements.done(p, "shooting_stars")) {
      BlockPos site = BlockPos.of(d.getLong("TimelockedSatelliteSite"));
      if (p.blockPosition().distSqr(site) < 196.0) {
        Achievements.grant(p, "shooting_stars");
      }
    }

    if (!d.getBoolean("TimelockedSatelliteCrash") && !p.isSpectator()) {
      int t = d.getInt("TimelockedSatelliteTimer") + 40;
      d.putInt("TimelockedSatelliteTimer", t);
      if (t >= 3600
          && !p.isPassenger()
          && p.serverLevel().canSeeSky(p.blockPosition().above())
          && FALLS.isEmpty()) {
        start(p, false);
      }
    }
  }

  public static String start(ServerPlayer p, boolean forced) {
    ServerLevel l = p.serverLevel();
    if (!l.dimension().equals(Level.OVERWORLD)) {
      return "Satellites only fall in the Overworld.";
    }

    BlockPos here = p.blockPosition();
    BlockPos entrance = null;

    try {
      entrance =
          SitePlanner.nearby(l, here).stream()
              .map(o -> o.offset(64, 29, 188))
              .min(Comparator.comparingDouble(o -> o.distSqr(here)))
              .orElse(null);
    } catch (RuntimeException var19) {
    }

    Vec3 dir;
    if (entrance != null && entrance.distSqr(here) > 14400.0) {
      dir = new Vec3(entrance.getX() - here.getX(), 0.0, entrance.getZ() - here.getZ()).normalize();
    } else {
      double a = l.random.nextDouble() * Math.PI * 2.0;
      dir = new Vec3(Math.cos(a), 0.0, Math.sin(a));
    }

    BlockPos impact = null;
    Vec3 used = dir;

    for (double off : new double[] {0.0, 0.45, -0.45, 0.9, -0.9, 1.4, -1.4, Math.PI}) {
      Vec3 d = dir.yRot((float) off);

      for (int dist : new int[] {72, 56, 90}) {
        BlockPos c =
            site(l, here.offset((int) Math.round(d.x * dist), 0, (int) Math.round(d.z * dist)));
        if (c != null) {
          impact = c;
          used = d;
          break;
        }
      }

      if (impact != null) {
        break;
      }
    }

    if (impact == null) {
      return "No clear crash site in loaded terrain nearby.";
    }

    Vec3 side = new Vec3(-used.z, 0.0, used.x);
    Vec3 to = Vec3.atBottomCenterOf(impact);
    Vec3 from =
        to.add(side.scale(70.0))
            .add(used.scale(30.0))
            .add(0.0, Math.min(150.0, l.getMaxBuildHeight() - 8 - to.y), 0.0);
    if (!l.hasChunkAt(BlockPos.containing(from))) {
      from =
          to.add(side.scale(30.0)).add(0.0, Math.min(120.0, l.getMaxBuildHeight() - 8 - to.y), 0.0);
    }

    String name = String.format(Locale.ROOT, "SAT-%02d", 1 + l.random.nextInt(48));
    SatelliteCrash.Fall f = new SatelliteCrash.Fall(l, from, to, impact, used, name);
    for (int i = 0; i < 3; i++) f.parts.add(part(l, from, i));
    f.parts.removeIf(Objects::isNull);
    FALLS.add(f);

    for (ServerPlayer near : l.players()) {
      if (near.distanceToSqr(to) < 160000.0) {
        near.sendSystemMessage(
            Component.literal("[SARC ORBITAL NETWORK] ")
                .withStyle(SARC)
                .append(
                    Component.literal(
                            name + " has lost attitude control. Atmospheric reentry detected.")
                        .withStyle(ChatFormatting.WHITE)));
        l.playSound(
            null,
            near.blockPosition(),
            SoundEvents.FIREWORK_ROCKET_LARGE_BLAST_FAR,
            SoundSource.AMBIENT,
            3.0F,
            0.5F);
        CompoundTag d = data(near);
        d.putBoolean("TimelockedSatelliteCrash", true);
        d.putLong("TimelockedSatelliteSite", impact.asLong());
      }
    }

    data(p).putBoolean("TimelockedSatelliteCrash", true);
    data(p).putLong("TimelockedSatelliteSite", impact.asLong());
    return "Satellite " + name + " falling toward " + impact.toShortString();
  }

  private static Entity part(ServerLevel l, Vec3 from, int i) {
    return switch (i) {
      case 0 ->
          display(l, from, Blocks.IRON_BLOCK.defaultBlockState(), new float[] {2.4F, 2.4F, 3.6F});
      case 1 ->
          display(
              l, from, Blocks.DAYLIGHT_DETECTOR.defaultBlockState(), new float[] {9F, .8F, 2.2F});
      default ->
          display(l, from, Blocks.LIGHTNING_ROD.defaultBlockState(), new float[] {1.2F, 3F, 1.2F});
    };
  }

  private static void animate(
      ServerLevel level,
      Vec3 from,
      Vec3 to,
      int age,
      List<Entity> parts,
      List<ServerPlayer> viewers) {
    double t = Math.pow(Math.min(1.0, age / 80.0), 1.35);
    Vec3 at = from.lerp(to, t).add(0.0, 3.0, 0.0);
    Vec3 vel = to.subtract(from).normalize();
    float yaw = (float) (Mth.atan2(vel.z, vel.x) * 180.0 / Math.PI) - 90.0F;
    float pitch = (float) (-Math.asin(vel.y) * 180.0 / Math.PI);

    for (int i = 0; i < parts.size(); i++) {
      Entity part = parts.get(i);
      if (!part.isRemoved()) {
        part.moveTo(at.x, at.y + (i == 2 ? 2.2 : 0.0), at.z, yaw + age * 3, pitch);
      }
    }

    for (ServerPlayer p : viewers) {
      if (p.distanceToSqr(at) < 360000.0) {
        for (int k = 0; k < 4; k++) {
          Vec3 trail = at.subtract(vel.scale(k * 1.6));
          send(level, p, ParticleTypes.FLAME, trail, 10, 0.6);
          send(level, p, ParticleTypes.LARGE_SMOKE, trail.subtract(vel.scale(2.0)), 6, 0.9);
        }

        send(level, p, ParticleTypes.LAVA, at, 2, 0.5);
        if (age % 3 == 0) {
          send(level, p, ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, at.subtract(vel.scale(6.0)), 3, 1.2);
        }
      }
    }

    if (age % 12 == 1) {
      level.playSound(
          null, at.x, at.y, at.z, SoundEvents.BLAZE_SHOOT, SoundSource.AMBIENT, 8.0F, 0.4F);
    }
  }

  static String introKey(int i) {
    return "CampusSatellitePart" + i;
  }

  static boolean ownsIntro(CompoundTag t, Entity entity) {
    if (!CampusOpening.active(t)
        || !CampusOpening.scc(t)
        || CampusOpening.step(t) != CampusOpening.FALL) return false;
    for (int i = 0; i < 3; i++)
      if (t.hasUUID(introKey(i)) && t.getUUID(introKey(i)).equals(entity.getUUID())) return true;
    return false;
  }

  static void introFrame(ServerPlayer p, CompoundTag t, Vec3 from, Vec3 to, int age) {
    var parts = new ArrayList<Entity>();
    for (int i = 0; i < 3; i++) {
      String key = introKey(i);
      Entity e = t.hasUUID(key) ? p.serverLevel().getEntity(t.getUUID(key)) : null;
      if (e == null) {
        e = part(p.serverLevel(), from, i);
        if (e != null) {
          e.addTag("timelocked_campus_satellite");
          e.getPersistentData().putUUID("CampusOwner", p.getUUID());
          t.putUUID(key, e.getUUID());
          IntroState.get(p.server).setDirty();
        }
      }
      if (e != null) parts.add(e);
    }
    animate(p.serverLevel(), from, to, age, parts, List.of(p));
  }

  static void clearIntro(ServerPlayer p, CompoundTag t) {
    var l = p.server.getLevel(IntroSequence.CAMPUS_DIM);
    for (int i = 0; i < 3; i++) {
      String key = introKey(i);
      if (l != null && t.hasUUID(key)) {
        var e = l.getEntity(t.getUUID(key));
        if (e != null) e.discard();
      }
      t.remove(key);
    }
  }

  static void introImpact(ServerLevel l, BlockPos surface) {
    for (int x = (surface.getX() - 13) >> 4; x <= (surface.getX() + 13) >> 4; x++)
      for (int z = (surface.getZ() - 13) >> 4; z <= (surface.getZ() + 13) >> 4; z++)
        l.getChunk(x, z);
    var to = Vec3.atBottomCenterOf(surface);
    impact(new Fall(l, to, to, surface, new Vec3(1, 0, 0), "SAT-INTRO"), false);
  }

  private static Entity display(ServerLevel l, Vec3 at, BlockState state, float[] s) {
    CompoundTag t = new CompoundTag();
    t.putString("id", "minecraft:block_display");
    t.put("block_state", NbtUtils.writeBlockState(state));
    CompoundTag tr = new CompoundTag();
    tr.put("left_rotation", floats(0.0F, 0.0F, 0.0F, 1.0F));
    tr.put("right_rotation", floats(0.0F, 0.0F, 0.0F, 1.0F));
    tr.put("translation", floats(-s[0] / 2.0F, -s[1] / 2.0F, -s[2] / 2.0F));
    tr.put("scale", floats(s[0], s[1], s[2]));
    t.put("transformation", tr);
    t.putInt("teleport_duration", 2);
    t.putFloat("view_range", 6.0F);
    CompoundTag b = new CompoundTag();
    b.putInt("sky", 15);
    b.putInt("block", 15);
    t.put("brightness", b);
    Entity e =
        EntityType.loadEntityRecursive(
            t,
            l,
            x -> {
              x.moveTo(at.x, at.y, at.z, 0.0F, 0.0F);
              return x;
            });
    if (e == null) {
      return null;
    }

    e.addTag("timelocked_satellite");
    return l.addFreshEntity(e) ? e : null;
  }

  private static ListTag floats(float... v) {
    ListTag t = new ListTag();

    for (float f : v) {
      t.add(FloatTag.valueOf(f));
    }

    return t;
  }

  private static BlockPos site(ServerLevel l, BlockPos column) {
    for (int dx = -12; dx <= 12; dx += 12) {
      for (int dz = -12; dz <= 12; dz += 12) {
        if (!l.hasChunkAt(column.offset(dx, 0, dz))) {
          return null;
        }
      }
    }

    int y = l.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, column.getX(), column.getZ());
    BlockPos c = new BlockPos(column.getX(), y, column.getZ());
    if (y > l.getMinBuildHeight() + 8
        && l.getFluidState(c.below()).isEmpty()
        && l.getFluidState(c).isEmpty()) {
      for (ServerPlayer p : l.players()) {
        if (p.blockPosition().distSqr(c) < 1600.0) {
          return null;
        }
      }

      if (SitePlanner.containing(l, c).isPresent()) {
        return null;
      }

      for (BlockPos o : SitePlanner.nearby(l, c)) {
        if (Math.abs(c.getX() - (o.getX() + 65)) < 110
            && Math.abs(c.getZ() - (o.getZ() + 104)) < 130) {
          return null;
        }
      }

      for (int cx = c.getX() - 12 >> 4; cx <= c.getX() + 12 >> 4; cx++) {
        for (int cz = c.getZ() - 12 >> 4; cz <= c.getZ() + 12 >> 4; cz++) {
          for (BlockPos be : l.getChunk(cx, cz).getBlockEntitiesPos()) {
            if (be.distSqr(c) < 196.0) {
              return null;
            }
          }
        }
      }

      return c;
    } else {
      return null;
    }
  }

  private static void tick(Post e) {
    if (!FALLS.isEmpty()) {
      Iterator<SatelliteCrash.Fall> it = FALLS.iterator();

      while (it.hasNext()) {
        SatelliteCrash.Fall f = it.next();
        f.age++;
        animate(f.level, f.from, f.to, f.age, f.parts, f.level.players());

        if (f.age >= 80) {
          it.remove();
          f.parts.forEach(Entity::discard);
          impact(f);
        }
      }
    }
  }

  private static void send(
      ServerLevel l, ServerPlayer p, ParticleOptions type, Vec3 at, int count, double spread) {
    l.sendParticles(p, type, true, at.x, at.y, at.z, count, spread, spread, spread, 0.02);
  }

  private static void impact(SatelliteCrash.Fall f) {
    impact(f, true);
  }

  private static void impact(SatelliteCrash.Fall f, boolean announce) {
    ServerLevel l = f.level;
    Vec3 c = Vec3.atBottomCenterOf(f.impact);
    l.explode(null, c.x, c.y + 1.0, c.z, 4.0F, false, ExplosionInteraction.NONE);

    for (ServerPlayer p : l.players()) {
      if (p.distanceToSqr(c) < 360000.0) {
        send(l, p, ParticleTypes.EXPLOSION_EMITTER, c.add(0.0, 2.0, 0.0), 3, 2.0);
        send(l, p, ParticleTypes.LAVA, c.add(0.0, 1.0, 0.0), 40, 3.0);
        l.playSound(
            null,
            p.blockPosition(),
            SoundEvents.LIGHTNING_BOLT_THUNDER,
            SoundSource.AMBIENT,
            2.0F,
            0.6F);
        if (announce && p.distanceToSqr(c) < 160000.0) {
          p.sendSystemMessage(
              Component.literal("[SARC ORBITAL NETWORK] ")
                  .withStyle(SARC)
                  .append(
                      Component.literal(
                              f.name + " signal lost. Recovery beacon active. Follow the smoke.")
                          .withStyle(ChatFormatting.WHITE)));
        }
      }
    }

    build(l, f.impact, f.dir, f.name, l.random);
  }

  public static BlockPos build(
      ServerLevel l, BlockPos surface, Vec3 dir, String name, RandomSource r) {
    int g = surface.getY() - 1;
    int cx = surface.getX();
    int cz = surface.getZ();
    BlockState[] floor =
        new BlockState[] {
          Blocks.BLACKSTONE.defaultBlockState(),
          Blocks.BASALT.defaultBlockState(),
          Blocks.COARSE_DIRT.defaultBlockState(),
          Blocks.COBBLED_DEEPSLATE.defaultBlockState(),
          Blocks.GRAVEL.defaultBlockState(),
          Blocks.TUFF.defaultBlockState()
        };

    for (int dx = -12; dx <= 12; dx++) {
      for (int dz = -12; dz <= 12; dz++) {
        double d = Math.sqrt(dx * dx + dz * dz) + r.nextDouble() * 0.6;
        int x = cx + dx;
        int z = cz + dz;
        if (l.hasChunkAt(new BlockPos(x, g, z))) {
          int top = l.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
          if (d < 7.0) {
            int depth = (int) Math.round(3.2 * (1.0 - d / 7.0 * (d / 7.0)));
            int fy = Math.min(top, g) - depth;

            for (int y = fy + 1; y <= Math.max(top, g) + 8; y++) {
              clear(l, new BlockPos(x, y, z));
            }

            BlockState fl =
                d < 2.6 && r.nextFloat() < 0.35
                    ? Blocks.MAGMA_BLOCK.defaultBlockState()
                    : floor[r.nextInt(floor.length)];
            set(l, new BlockPos(x, fy, z), fl);
          } else if (!(d < 9.2)) {
            if (d < 12.5) {
              BlockPos at = new BlockPos(x, top, z);
              if (l.getBlockState(at).is(Blocks.GRASS_BLOCK) && r.nextFloat() < 0.45) {
                set(l, at, Blocks.COARSE_DIRT.defaultBlockState());
              }

              if (r.nextFloat() < 0.035 && l.getBlockState(at.above()).canBeReplaced()) {
                set(
                    l,
                    at.above(),
                    r.nextBoolean()
                        ? Blocks.IRON_BARS.defaultBlockState()
                        : Blocks.BLACKSTONE_SLAB.defaultBlockState());
              }
            }
          } else {
            BlockPos at = new BlockPos(x, top, z);
            if (l.getBlockState(at).is(Blocks.GRASS_BLOCK) || l.getBlockState(at).is(Blocks.DIRT)) {
              set(l, at, Blocks.COARSE_DIRT.defaultBlockState());
            }

            clear(l, at.above());
            if (r.nextFloat() < 0.55) {
              set(
                  l,
                  at.above(),
                  r.nextBoolean()
                      ? Blocks.COARSE_DIRT.defaultBlockState()
                      : Blocks.GRAVEL.defaultBlockState());
            }
          }
        }
      }
    }

    boolean alongX = Math.abs(dir.x) >= Math.abs(dir.z);
    Direction axis = alongX ? Direction.EAST : Direction.SOUTH;
    Direction side = alongX ? Direction.SOUTH : Direction.EAST;
    int f = g - 3;
    BlockPos base = new BlockPos(cx, f, cz);

    for (int a = -1; a <= 2; a++) {
      for (int s = 0; s <= 1; s++) {
        for (int h = 0; h <= 1; h++) {
          BlockPos at = base.relative(axis, a).relative(side, s).above(h);
          set(
              l,
              at,
              a == 2 && h == 1
                  ? Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState()
                  : (a == -1 && h == 1
                      ? Blocks.AIR.defaultBlockState()
                      : Blocks.IRON_BLOCK.defaultBlockState()));
        }
      }
    }

    set(
        l,
        base.relative(axis, 0).relative(side, 1).above(2),
        Blocks.LIGHTNING_ROD.defaultBlockState());
    set(l, base.relative(axis, 1).above(2), Blocks.IRON_TRAPDOOR.defaultBlockState());

    for (int a = 0; a <= 3; a++) {
      set(
          l,
          base.relative(axis, a - 1).relative(side, -1).above(1),
          Blocks.DAYLIGHT_DETECTOR.defaultBlockState());
      set(
          l,
          base.relative(axis, a - 1).relative(side, -2).above(a < 2 ? 1 : 0),
          Blocks.DAYLIGHT_DETECTOR.defaultBlockState());
    }

    for (int a = 0; a <= 2; a++) {
      set(
          l,
          base.relative(axis, a + 3).relative(side, 3).above(1 + (a == 0 ? 1 : 0)),
          Blocks.DAYLIGHT_DETECTOR.defaultBlockState());
    }

    set(
        l,
        base.relative(axis, 3).relative(side, 2).above(1),
        (BlockState)
            Blocks.CHAIN
                .defaultBlockState()
                .setValue(BlockStateProperties.AXIS, alongX ? Axis.Z : Axis.X));
    set(
        l,
        base.relative(axis, 2).relative(side, 2).above(1),
        (BlockState)
            ((SarcLogoPanelBlock) Content.LOGO.get())
                .defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, side));
    BlockPos smoke = base.relative(axis, -2).relative(side, 1);
    set(l, smoke.below(), Blocks.HAY_BLOCK.defaultBlockState());
    set(
        l,
        smoke,
        (BlockState)
            ((BlockState)
                    Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.SIGNAL_FIRE, true))
                .setValue(CampfireBlock.LIT, true));
    BlockPos crate = base.relative(axis, -1).above(1);
    set(
        l,
        crate,
        (BlockState) Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP));
    if (l.getBlockEntity(crate) instanceof BarrelBlockEntity barrel) {
      barrel.setLootTable(LOOT, r.nextLong());
      barrel.setItem(13, tracker(l, crate));
      barrel.setItem(4, recorder(name));
      barrel.setChanged();
    }

    return crate;
  }

  private static void clear(ServerLevel l, BlockPos p) {
    BlockState s = l.getBlockState(p);
    if (!s.isAir() && !s.is(Blocks.BEDROCK) && l.getBlockEntity(p) == null) {
      l.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
    }
  }

  private static void set(ServerLevel l, BlockPos p, BlockState s) {
    if (!l.getBlockState(p).is(Blocks.BEDROCK) && l.getBlockEntity(p) == null
        || s.hasBlockEntity()) {
      l.setBlock(p, s, 2);
    }
  }

  public static ItemStack tracker(ServerLevel l, BlockPos from) {
    ItemStack s = new ItemStack(Items.COMPASS);
    BlockPos target = null;

    try {
      target =
          SitePlanner.nearby(l, from).stream()
              .map(o -> o.offset(64, 29, 188))
              .min(Comparator.comparingDouble(o -> o.distSqr(from)))
              .orElse(null);
    } catch (RuntimeException var5) {
    }

    if (target != null) {
      s.set(
          DataComponents.LODESTONE_TRACKER,
          new LodestoneTracker(Optional.of(GlobalPos.of(Level.OVERWORLD, target)), false));
    }

    s.set(
        DataComponents.CUSTOM_NAME,
        Component.literal("SARC Recovery Tracker")
            .withStyle(s2 -> s2.withItalic(false).withColor(11898111)));
    s.set(
        DataComponents.LORE,
        new ItemLore(
            List.of(
                Component.literal("Salvaged from a crashed satellite.")
                    .withStyle(ChatFormatting.GRAY),
                Component.literal("Points to the nearest SARC Zero Point facility.")
                    .withStyle(ChatFormatting.GRAY))));
    return s;
  }

  public static ItemStack recorder(String name) {
    ItemStack s = new ItemStack(Items.WRITTEN_BOOK);
    List<Filterable<Component>> pages = new ArrayList<>();

    for (String page :
        new String[] {
          name
              + " FLIGHT RECORDER\n\nT-00:41 Attitude control lost.\nT-00:38 Reaction wheels saturated.\nT-00:30 Uplink to SITE ground station unanswered.",
          "T-00:22 Ground station reports a containment event. Message ends mid-sentence.\n\nT-00:15 Something on the long-range camera. It was looking back.",
          "T-00:04 Deorbit burn failed.\n\nRecovery crews: the tracker in this unit points to the nearest SARC facility. Bring a light. Bring friends.\n\nSARC thanks you for your service."
        }) {
      pages.add(Filterable.passThrough(Component.literal(page)));
    }

    s.set(
        DataComponents.WRITTEN_BOOK_CONTENT,
        new WrittenBookContent(
            Filterable.passThrough(name + " Flight Recorder"),
            "SARC Orbital Network",
            0,
            pages,
            true));
    return s;
  }

  public static boolean falling() {
    return !FALLS.isEmpty();
  }

  private SatelliteCrash() {}

  private static final class Fall {
    final ServerLevel level;
    final Vec3 from;
    final Vec3 to;
    final BlockPos impact;
    final Vec3 dir;
    final List<Entity> parts = new ArrayList<>();
    final String name;
    int age;

    Fall(ServerLevel l, Vec3 f, Vec3 t, BlockPos i, Vec3 d, String n) {
      this.level = l;
      this.from = f;
      this.to = t;
      this.impact = i;
      this.dir = d;
      this.name = n;
    }
  }
}
