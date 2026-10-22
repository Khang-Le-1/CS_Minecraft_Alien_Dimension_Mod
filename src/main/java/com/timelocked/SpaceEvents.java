package com.timelocked;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent.Post;

public final class SpaceEvents {
  public static final ResourceLocation GRAVITY_ID =
      ResourceLocation.fromNamespaceAndPath("timelocked", "planet_gravity");
  public static final ResourceKey<DamageType> VACUUM =
      ResourceKey.create(
          Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath("timelocked", "vacuum"));
  public static final int LIFE_SUPPORT_RADIUS = 10;
  static final String[] PLANET_ADV =
      new String[] {
        null,
        "low_earth_orbit",
        "one_small_step",
        "red_planet",
        "rock_hopper",
        "under_the_ice",
        "far_from_home"
      };
  private static final Map<UUID, Boolean> PRESSURE = new HashMap<>();

  public static void register() {
    NeoForge.EVENT_BUS.addListener(SpaceEvents::tick);
    NeoForge.EVENT_BUS.addListener(SpaceEvents::fall);
    NeoForge.EVENT_BUS.addListener(SpaceEvents::join);
    NeoForge.EVENT_BUS.addListener(SpaceEvents::commands);
    NeoForge.EVENT_BUS.addListener(
        (PlayerEvent.PlayerLoggedInEvent e) -> {
          if (e.getEntity() instanceof ServerPlayer p) {
            Timeskipper.sync(p);
          }
        });
    NeoForge.EVENT_BUS.addListener(
        (PlayerEvent.PlayerChangedDimensionEvent e) -> {
          if (e.getEntity() instanceof ServerPlayer p) {
            Timeskipper.sync(p);
          }
        });
  }

  public static void applyGravity(LivingEntity e, float g) {
    AttributeInstance a = e.getAttribute(Attributes.GRAVITY);
    if (a != null) {
      AttributeModifier m = a.getModifier(GRAVITY_ID);
      if (g >= 0.999F) {
        if (m != null) {
          a.removeModifier(GRAVITY_ID);
        }
      } else if (m == null || !(Math.abs(m.amount() - (g - 1.0F)) < 1.0E-4)) {
        a.removeModifier(GRAVITY_ID);
        a.addTransientModifier(
            new AttributeModifier(GRAVITY_ID, g - 1.0F, Operation.ADD_MULTIPLIED_TOTAL));
      }
    }
  }

  public static boolean fullSuit(Player p) {
    for (EquipmentSlot s :
        new EquipmentSlot[] {
          EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
        }) {
      if (!(p.getItemBySlot(s).getItem() instanceof SpaceArmorItem)) {
        return false;
      }
    }

    return true;
  }

  public static boolean nearLifeSupport(Level l, BlockPos at) {
    for (BlockPos q : BlockPos.betweenClosed(at.offset(-10, -6, -10), at.offset(10, 6, 10))) {
      BlockState b = l.getBlockState(q);
      if (b.is((Block) SpaceContent.LIFE_SUPPORT.get())
          || b.is((Block) SpaceContent.MOONBREATH.get())) {
        return true;
      }
    }

    return false;
  }

  public static DamageSource vacuum(Level l) {
    return new DamageSource(
        l.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(VACUUM));
  }

  private static void join(EntityJoinLevelEvent e) {
    if (e.getEntity() instanceof LivingEntity le) {
      applyGravity(le, SpacePlanets.gravity(e.getLevel().dimension()));
    }
  }

  private static void fall(LivingFallEvent e) {
    float g = SpacePlanets.gravity(e.getEntity().level().dimension());
    if (g < 1.0F) {
      e.setDamageMultiplier(e.getDamageMultiplier() * g);
    }
  }

  private static void tick(Post e) {
    if (e.getEntity() instanceof ServerPlayer p) {
      ServerLevel l = p.serverLevel();
      ResourceKey<Level> dim = l.dimension();
      SpacePlanets.Planet planet = SpacePlanets.Planet.of(dim);
      if (p.tickCount % 20 == 0) {
        applyGravity(p, SpacePlanets.gravity(dim));
        if (planet != null
            && planet != SpacePlanets.Planet.EARTH
            && (p.onGround() || p.isInWater())) {
          visit(p, planet);
        }
      }

      if (planet == SpacePlanets.Planet.ORBIT && p.getY() < 4.0) {
        reentry(p);
      } else {
        if (planet == SpacePlanets.Planet.BELT && p.getY() < 2.0) {
          p.teleportTo(p.getX(), 250.0, p.getZ());
          p.resetFallDistance();
        }

        if (planet != null && !planet.atmosphere && !p.isCreative() && !p.isSpectator()) {
          if (p.tickCount % 10 == 0) {
            PRESSURE.put(p.getUUID(), fullSuit(p) || nearLifeSupport(l, p.blockPosition()));
          }

          if (!PRESSURE.getOrDefault(p.getUUID(), true)) {
            int air = p.getAirSupply() - 5;
            if (air <= -20) {
              air = 0;
              p.hurt(vacuum(l), 3.0F);
              Achievements.grant(p, "houston");
            }

            p.setAirSupply(air);
            if (p.tickCount % 40 == 0) {
              p.displayClientMessage(
                  Component.literal("NO OXYGEN! Full space suit or a life support unit needed.")
                      .withStyle(new ChatFormatting[] {ChatFormatting.RED, ChatFormatting.BOLD}),
                  true);
            }
          }
        }
      }
    }
  }

  static void visit(ServerPlayer p, SpacePlanets.Planet planet) {
    Achievements.grant(p, PLANET_ADV[planet.ordinal()]);
    CompoundTag tag = p.getPersistentData();
    String seen = tag.getString("timelocked_visited");
    if (!seen.contains("|" + planet.id + "|")) {
      seen = (seen.isEmpty() ? "|" : seen) + planet.id + "|";
      tag.putString("timelocked_visited", seen);
    }

    int n = 0;

    for (SpacePlanets.Planet q : SpacePlanets.Planet.values()) {
      if (q != SpacePlanets.Planet.EARTH && seen.contains("|" + q.id + "|")) {
        n++;
      }
    }

    if (n >= SpacePlanets.Planet.values().length - 1) {
      Achievements.grant(p, "grand_tour");
    }
  }

  public static void reentry(ServerPlayer p) {
    ServerLevel earth = p.server.overworld();
    p.teleportTo(earth, p.getX(), 300.0, p.getZ(), p.getYRot(), 60.0F);
    p.resetFallDistance();
    p.setRemainingFireTicks(120);
    p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 900, 0));
    p.sendSystemMessage(
        Component.literal("[SARC] Re-entry detected. Brace!").withStyle(ChatFormatting.GOLD));
    Achievements.grant(p, "reentry");
  }

  private static void commands(RegisterCommandsEvent e) {
    e.getDispatcher()
        .register(
            (LiteralArgumentBuilder)
                ((LiteralArgumentBuilder)
                    ((LiteralArgumentBuilder)
                            ((LiteralArgumentBuilder)
                                    ((LiteralArgumentBuilder)
                                            ((LiteralArgumentBuilder)
                                                    ((LiteralArgumentBuilder)
                                                            ((LiteralArgumentBuilder)
                                                                    Commands.literal("timeskipper")
                                                                        .then(
                                                                            Commands.literal("map")
                                                                                .executes(
                                                                                    c -> {
                                                                                      ServerPlayer
                                                                                          p =
                                                                                              ((CommandSourceStack)
                                                                                                      c
                                                                                                          .getSource())
                                                                                                  .getPlayerOrException();
                                                                                      if (!Timeskipper
                                                                                              .atConsole(
                                                                                                  p)
                                                                                          && !((CommandSourceStack)
                                                                                                  c
                                                                                                      .getSource())
                                                                                              .hasPermission(
                                                                                                  2)) {
                                                                                        ((CommandSourceStack)
                                                                                                c
                                                                                                    .getSource())
                                                                                            .sendFailure(
                                                                                                Component
                                                                                                    .literal(
                                                                                                        "Use the nav console on the TIMESKIPPER's bridge."));
                                                                                        return 0;
                                                                                      } else {
                                                                                        Timeskipper
                                                                                            .starMap(
                                                                                                p);
                                                                                        return 1;
                                                                                      }
                                                                                    })))
                                                                .then(
                                                                    Commands.literal("jump")
                                                                        .then(
                                                                            Commands.argument(
                                                                                    "destination",
                                                                                    StringArgumentType
                                                                                        .word())
                                                                                .suggests(
                                                                                    (c, b) ->
                                                                                        SharedSuggestionProvider
                                                                                            .suggest(
                                                                                                Arrays
                                                                                                    .stream(
                                                                                                        SpacePlanets
                                                                                                            .Planet
                                                                                                            .values())
                                                                                                    .map(
                                                                                                        x ->
                                                                                                            x.id),
                                                                                                b))
                                                                                .executes(
                                                                                    c -> {
                                                                                      ServerPlayer
                                                                                          p =
                                                                                              ((CommandSourceStack)
                                                                                                      c
                                                                                                          .getSource())
                                                                                                  .getPlayerOrException();
                                                                                      if (!Timeskipper
                                                                                              .atConsole(
                                                                                                  p)
                                                                                          && !((CommandSourceStack)
                                                                                                  c
                                                                                                      .getSource())
                                                                                              .hasPermission(
                                                                                                  2)) {
                                                                                        ((CommandSourceStack)
                                                                                                c
                                                                                                    .getSource())
                                                                                            .sendFailure(
                                                                                                Component
                                                                                                    .literal(
                                                                                                        "You need to be at the nav console on the TIMESKIPPER's bridge."));
                                                                                        return 0;
                                                                                      }

                                                                                      String id =
                                                                                          StringArgumentType
                                                                                              .getString(
                                                                                                  c,
                                                                                                  "destination");
                                                                                      SpacePlanets
                                                                                              .Planet
                                                                                          t = null;

                                                                                      for (SpacePlanets
                                                                                              .Planet
                                                                                          x :
                                                                                              SpacePlanets
                                                                                                  .Planet
                                                                                                  .values()) {
                                                                                        if (x.id
                                                                                            .equals(
                                                                                                id)) {
                                                                                          t = x;
                                                                                        }
                                                                                      }

                                                                                      if (t
                                                                                          == null) {
                                                                                        ((CommandSourceStack)
                                                                                                c
                                                                                                    .getSource())
                                                                                            .sendFailure(
                                                                                                Component
                                                                                                    .literal(
                                                                                                        "Unknown destination: "
                                                                                                            + id));
                                                                                        return 0;
                                                                                      } else {
                                                                                        return Timeskipper
                                                                                                .jump(
                                                                                                    p,
                                                                                                    t)
                                                                                            ? 1
                                                                                            : 0;
                                                                                      }
                                                                                    }))))
                                                        .then(
                                                            Commands.literal("status")
                                                                .executes(
                                                                    c -> {
                                                                      Timeskipper.Data d =
                                                                          Timeskipper.data(
                                                                              ((CommandSourceStack)
                                                                                      c.getSource())
                                                                                  .getServer());
                                                                      ((CommandSourceStack)
                                                                              c.getSource())
                                                                          .sendSuccess(
                                                                              () ->
                                                                                  Component.literal(
                                                                                      "TIMESKIPPER at "
                                                                                          + d
                                                                                              .planet()
                                                                                              .title
                                                                                          + ", drive charges "
                                                                                          + d.charges
                                                                                          + "/32"),
                                                                              false);
                                                                      return 1;
                                                                    })))
                                                .then(
                                                    ((LiteralArgumentBuilder)
                                                            Commands.literal("board")
                                                                .requires(s -> s.hasPermission(2)))
                                                        .executes(
                                                            c ->
                                                                Timeskipper.board(
                                                                        ((CommandSourceStack)
                                                                                c.getSource())
                                                                            .getPlayerOrException())
                                                                    ? 1
                                                                    : 0)))
                                        .then(
                                            ((LiteralArgumentBuilder)
                                                    Commands.literal("disembark")
                                                        .requires(s -> s.hasPermission(2)))
                                                .executes(
                                                    c ->
                                                        Timeskipper.disembark(
                                                                ((CommandSourceStack) c.getSource())
                                                                    .getPlayerOrException())
                                                            ? 1
                                                            : 0)))
                                .then(
                                    ((LiteralArgumentBuilder)
                                            Commands.literal("charges")
                                                .requires(s -> s.hasPermission(2)))
                                        .then(
                                            Commands.argument(
                                                    "amount", IntegerArgumentType.integer(0, 32))
                                                .executes(
                                                    c -> {
                                                      Timeskipper.Data d =
                                                          Timeskipper.data(
                                                              ((CommandSourceStack) c.getSource())
                                                                  .getServer());
                                                      d.charges =
                                                          IntegerArgumentType.getInteger(
                                                              c, "amount");
                                                      d.setDirty();
                                                      ((CommandSourceStack) c.getSource())
                                                          .sendSuccess(
                                                              () ->
                                                                  Component.literal(
                                                                      "Drive charges set to "
                                                                          + d.charges),
                                                              true);
                                                      return 1;
                                                    }))))
                        .then(
                            ((LiteralArgumentBuilder)
                                    ((LiteralArgumentBuilder)
                                            Commands.literal("refit")
                                                .requires(s -> s.hasPermission(2)))
                                        .executes(
                                            c -> {
                                              ((CommandSourceStack) c.getSource())
                                                  .sendSuccess(
                                                      () ->
                                                          Component.literal(
                                                              "Back up your save first. An additional pre-refit ship snapshot is saved under timelocked-backups. This updates the TIMESKIPPER interior and known furniture cells, preserves containers/custom blocks, and never resets drive charges or progression. Stand aboard and run /timeskipper refit confirm."),
                                                      false);
                                              return 1;
                                            }))
                                .then(
                                    Commands.literal("confirm")
                                        .executes(
                                            c -> {
                                              CommandSourceStack src =
                                                  (CommandSourceStack) c.getSource();
                                              ServerPlayer p = src.getPlayerOrException();
                                              if (!p.level()
                                                  .dimension()
                                                  .equals(SpacePlanets.SHIP)) {
                                                src.sendFailure(
                                                    Component.literal(
                                                        "Board the TIMESKIPPER before refitting."));
                                                return 0;
                                              } else {
                                                TimeskipperRefit.Result r =
                                                    TimeskipperRefit.apply(
                                                        p.serverLevel(), Timeskipper.ORIGIN);
                                                src.sendSuccess(
                                                    () ->
                                                        Component.literal(
                                                            "Refit: "
                                                                + r.changed()
                                                                + " interior/furniture cells upgraded; "
                                                                + r.protectedCells()
                                                                + " occupied or customized cells preserved. Snapshot saved under timelocked-backups. Protected cells may leave an incomplete refit. Leave and return to replace tracked 0.8.2 parked copies. The station connection is repaired only when its stock cells are clear."),
                                                    true);
                                                return 1;
                                              }
                                            })))));
  }

  private SpaceEvents() {}
}
