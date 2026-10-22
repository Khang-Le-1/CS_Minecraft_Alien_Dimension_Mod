package com.timelocked;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedData.Factory;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public final class Timeskipper {
  public static final BlockPos ORIGIN = new BlockPos(0, 100, 0);
  public static final int MAX_CHARGES = 32;
  public static final int START_CHARGES = 3;
  static final String EARTH_RETURN = "timelocked_earth_return";
  static final String LINK_GIVEN = "timelocked_link_given";
  static final String VISITED = "timelocked_visited";

  public static Timeskipper.Data data(MinecraftServer s) {
    return (Timeskipper.Data)
        s.overworld()
            .getDataStorage()
            .computeIfAbsent(
                new Factory<>(Timeskipper.Data::new, Timeskipper.Data::load, null),
                "timelocked_timeskipper");
  }

  public static void ensureBuilt(ServerLevel ship) {
    Timeskipper.Data d = data(ship.getServer());
    if (!d.built) {
      TimeskipperShip.build(ship, ORIGIN);
      d.built = true;
      d.setDirty();
      TimeskipperShip.updateScreens(ship, d);
    }
  }

  public static void sync(ServerPlayer p) {
    PacketDistributor.sendToPlayer(
        p, new SpaceNetworking.ShipLocation(data(p.server).location), new CustomPacketPayload[0]);
  }

  static void say(ServerPlayer p, String s, ChatFormatting c) {
    p.sendSystemMessage(Component.literal(s).withStyle(c));
  }

  /** Which airlock a player used. Each door has its own inside and outside arrival point. */
  public enum Door {
    ANY,
    RAMP,
    SIDE;

    public static Door of(net.minecraft.core.Direction face) {
      return face == net.minecraft.core.Direction.SOUTH
          ? RAMP
          : face == net.minecraft.core.Direction.WEST ? SIDE : ANY;
    }
  }

  public static boolean board(ServerPlayer p) {
    return board(p, Door.ANY);
  }

  public static boolean board(ServerPlayer p, Door door) {
    MinecraftServer s = p.server;
    ServerLevel ship = s.getLevel(SpacePlanets.SHIP);
    if (ship == null) {
      return false;
    }

    if (p.level().dimension().equals(SpacePlanets.SHIP)) {
      p.displayClientMessage(Component.literal("[TIMESKIPPER] You are already aboard."), true);
      return false;
    }

    if (p.isPassenger()) {
      p.stopRiding();
    }

    if (p.level().dimension().equals(Level.OVERWORLD)) {
      p.getPersistentData().putLong("timelocked_earth_return", p.blockPosition().asLong());
    }

    ensureBuilt(ship);
    boolean twoDeck = TimeskipperDecks.twoDeck(ship);
    BlockPos in;
    float yaw = 90.0F;
    if (door == Door.RAMP && twoDeck) {
      // Cargo ramp door: lower service corridor, two blocks inside the transfer door, facing
      // aft-to-fore.
      in = ORIGIN.offset(0, -2, 3);
      yaw = 180.0F;
    } else if (door == Door.SIDE) {
      // Side docking port: upper deck airlock room.
      in = ORIGIN.offset(TimeskipperShip.AIRLOCK_IN);
    } else {
      in = ORIGIN.offset(twoDeck ? TimeskipperDecks.SPAWN : TimeskipperShip.AIRLOCK_IN);
    }
    p.teleportTo(ship, in.getX() + 0.5, in.getY(), in.getZ() + 0.5, yaw, 0.0F);
    p.setAirSupply(p.getMaxAirSupply());
    ship.playSound(null, in, SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 1.0F, 0.6F);
    ship.playSound(null, in, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.8F, 1.3F);
    Achievements.grant(p, "timeskipper");
    sync(p);
    Timeskipper.Data d = data(s);
    say(
        p,
        "[TIMESKIPPER] Welcome aboard. Parked at: "
            + d.planet().title
            + ". Drive charges: "
            + d.charges
            + ". Bridge: upper deck, north. Cargo-side stairs connect the decks. Use the nav console.",
        ChatFormatting.LIGHT_PURPLE);
    if (!p.getPersistentData().getBoolean("timelocked_link_given")) {
      p.getPersistentData().putBoolean("timelocked_link_given", true);
      ItemStack link = new ItemStack((ItemLike) SpaceContent.LINK.get());
      if (!p.addItem(link)) {
        p.drop(link, false);
      }

      say(
          p,
          "[TIMESKIPPER] Crew link issued. Use it anywhere to beam back aboard.",
          ChatFormatting.LIGHT_PURPLE);
    }

    return true;
  }

  public static boolean disembark(ServerPlayer p) {
    return disembark(p, Door.ANY);
  }

  public static boolean disembark(ServerPlayer p, Door door) {
    MinecraftServer s = p.server;
    Timeskipper.Data d = data(s);
    SpacePlanets.Planet at = d.planet();
    ServerLevel dest = s.getLevel(at.dimension());
    if (dest == null) {
      return false;
    }

    Vec3 spot;
    if (at == SpacePlanets.Planet.EARTH) {
      BlockPos back =
          p.getPersistentData().contains("timelocked_earth_return")
              ? BlockPos.of(p.getPersistentData().getLong("timelocked_earth_return"))
              : dest.getSharedSpawnPos();
      spot =
          VoidspaceTravel.safeSpot(dest, back)
              .orElseGet(
                  () ->
                      Vec3.atBottomCenterOf(
                          dest.getHeightmapPos(Types.MOTION_BLOCKING_NO_LEAVES, back)));
    } else {
      BlockPos arrival = TimeskipperLanding.arrive(s, at);
      if (arrival != null) {
        arrival = TimeskipperLanding.exitFor(s, at, dest, door, arrival);
        if (arrival == null) {
          say(
              p,
              "[AIRLOCK] The cargo ramp does not extend in vacuum. Use the side docking port.",
              ChatFormatting.RED);
          return false;
        }
      }
      spot = Vec3.atBottomCenterOf(arrival != null ? arrival : landing(dest, at));
    }

    p.teleportTo(dest, spot.x, spot.y, spot.z, p.getYRot(), 0.0F);
    dest.playSound(
        null,
        BlockPos.containing(spot),
        SoundEvents.IRON_DOOR_OPEN,
        SoundSource.BLOCKS,
        1.0F,
        0.6F);
    if (at != SpacePlanets.Planet.EARTH) {
      p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 60, 20));
      p.connection.send(
          new ClientboundSetTitleTextPacket(
              Component.literal(at.title).withStyle(ChatFormatting.LIGHT_PURPLE)));
      p.connection.send(
          new ClientboundSetSubtitleTextPacket(
              Component.literal(
                  at.site
                      + " | g "
                      + at.gravity
                      + (at.atmosphere ? " | breathable" : " | NO ATMOSPHERE"))));
      if (!at.atmosphere && !SpaceEvents.fullSuit(p)) {
        say(
            p,
            "[TIMESKIPPER] Warning: no atmosphere out here. Stay near the life support unit or wear a full space suit.",
            ChatFormatting.RED);
      }
    }

    return true;
  }

  public static BlockPos landing(ServerLevel l, SpacePlanets.Planet p) {
    Timeskipper.Data d = data(l.getServer());
    Long at = d.pads.get(p.id);
    if (at != null) {
      return BlockPos.of(at);
    }

    BlockPos spawn = SpaceSites.build(l, p);
    d.pads.put(p.id, spawn.asLong());
    d.setDirty();
    return spawn;
  }

  public static boolean atConsole(ServerPlayer p) {
    return p.level().dimension().equals(SpacePlanets.SHIP)
        && p.blockPosition().distSqr(ORIGIN.offset(TimeskipperShip.NAV)) < 144.0;
  }

  public static void starMap(ServerPlayer p) {
    Timeskipper.Data d = data(p.server);
    PacketDistributor.sendToPlayer(
        p,
        new JumpNetworking.Navigation(d.location, d.charges, p.isCreative()),
        new CustomPacketPayload[0]);
  }

  public static boolean jump(ServerPlayer by, SpacePlanets.Planet to) {
    return JumpSequence.start(by, to);
  }

  private Timeskipper() {}

  public static final class Data extends SavedData {
    public int location;
    public int charges = 3;
    public boolean built;
    public String parked = "";
    public long parkedAt;
    public int parkedGeometryVersion;
    public final Map<String, Long> pads = new HashMap<>();

    static Timeskipper.Data load(CompoundTag t, Provider p) {
      Timeskipper.Data d = new Timeskipper.Data();
      d.location = t.getInt("Location");
      d.charges = t.contains("Charges") ? t.getInt("Charges") : 3;
      d.built = t.getBoolean("Built");
      d.parked = t.getString("Parked");
      d.parkedAt = t.getLong("ParkedAt");
      d.parkedGeometryVersion = t.getInt("ParkedGeometryVersion");
      CompoundTag pads = t.getCompound("Pads");

      for (String k : pads.getAllKeys()) {
        d.pads.put(k, pads.getLong(k));
      }

      return d;
    }

    public CompoundTag save(CompoundTag t, Provider p) {
      t.putInt("Location", this.location);
      t.putInt("Charges", this.charges);
      t.putBoolean("Built", this.built);
      t.putString("Parked", this.parked);
      t.putLong("ParkedAt", this.parkedAt);
      t.putInt("ParkedGeometryVersion", this.parkedGeometryVersion);
      CompoundTag pads = new CompoundTag();
      this.pads.forEach(pads::putLong);
      t.put("Pads", pads);
      return t;
    }

    public SpacePlanets.Planet planet() {
      return SpacePlanets.Planet.byIndex(this.location);
    }
  }

  private static final class ParticleTypesHolder {
    static final SimpleParticleType REVERSE = ParticleTypes.REVERSE_PORTAL;
    static final SimpleParticleType ROD = ParticleTypes.END_ROD;
  }
}
