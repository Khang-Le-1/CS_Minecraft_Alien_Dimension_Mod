package com.timelocked;

import com.google.gson.GsonBuilder;
import com.mojang.authlib.GameProfile;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.stats.Stats;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Test-only native server audit. Never included in the release JAR. */
@EventBusSubscriber(modid = "timelocked")
public final class IntroAudit {
  static Path output(String name) throws java.io.IOException {
    Path directory = Path.of(System.getProperty("timelocked.audit.output", "validation"));
    Files.createDirectories(directory);
    return directory.resolve(name);
  }

  static int checks;
  static final List<String> failures = new ArrayList<>();

  static void check(boolean value, String label) {
    checks++;
    if (!value) failures.add(label);
  }

  static final class Listener extends FakePlayer {
    final List<String> messages = new ArrayList<>();
    int sounds;

    Listener(ServerLevel level) {
      super(
          level,
          new GameProfile(UUID.fromString("00000000-0000-0000-0000-000000008913"), "WelcomeAudit"));
    }

    @Override
    public void sendSystemMessage(Component message) {
      messages.add(
          message.getContents()
                  instanceof net.minecraft.network.chat.contents.TranslatableContents t
              ? t.getKey()
              : message.getString());
    }

    @Override
    public void playNotifySound(SoundEvent sound, SoundSource source, float volume, float pitch) {
      sounds++;
    }
  }

  static void beds(net.minecraft.server.MinecraftServer server) throws Exception {
    var level = server.getLevel(VoidspaceTravel.DIMENSION);
    check(level != null, "voidspace dimension loads");
    check(level.dimensionType().bedWorks(), "beds enabled in native dimension type");
    check(level.dimensionType().respawnAnchorWorks(), "existing anchors retained");
    check(
        !level.dimensionType().natural() && !level.dimensionType().hasSkyLight(),
        "alien world and existing lighting preserved");
    check(level.dimensionType().fixedTime().orElse(-1) == 18000, "fixed midnight retained");
    check(
        level.dimensionType().effectsLocation().toString().equals("timelocked:voidspace"),
        "dedicated celestial sky id loaded");
    var player = new Listener(level);
    var foot = new net.minecraft.core.BlockPos(800, 200, 800);
    var colors =
        java.util.List.of(
            net.minecraft.world.level.block.Blocks.WHITE_BED,
            net.minecraft.world.level.block.Blocks.ORANGE_BED,
            net.minecraft.world.level.block.Blocks.MAGENTA_BED,
            net.minecraft.world.level.block.Blocks.LIGHT_BLUE_BED,
            net.minecraft.world.level.block.Blocks.YELLOW_BED,
            net.minecraft.world.level.block.Blocks.LIME_BED,
            net.minecraft.world.level.block.Blocks.PINK_BED,
            net.minecraft.world.level.block.Blocks.GRAY_BED,
            net.minecraft.world.level.block.Blocks.LIGHT_GRAY_BED,
            net.minecraft.world.level.block.Blocks.CYAN_BED,
            net.minecraft.world.level.block.Blocks.PURPLE_BED,
            net.minecraft.world.level.block.Blocks.BLUE_BED,
            net.minecraft.world.level.block.Blocks.BROWN_BED,
            net.minecraft.world.level.block.Blocks.GREEN_BED,
            net.minecraft.world.level.block.Blocks.RED_BED,
            net.minecraft.world.level.block.Blocks.BLACK_BED);
    for (int x = -4; x <= 4; x++)
      for (int z = -4; z <= 4; z++) {
        var base = foot.offset(x, -1, z);
        level.getChunkAt(base);
        level.setBlock(base, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 2);
        for (int y = 0; y <= 3; y++)
          level.setBlock(
              foot.offset(x, y, z),
              net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),
              2);
      }
    long time = level.getDayTime();
    for (var bed : colors)
      for (var facing : net.minecraft.core.Direction.Plane.HORIZONTAL) {
        var head = foot.relative(facing);
        var base =
            bed.defaultBlockState()
                .setValue(net.minecraft.world.level.block.BedBlock.FACING, facing);
        level.setBlock(
            foot,
            base.setValue(
                net.minecraft.world.level.block.BedBlock.PART,
                net.minecraft.world.level.block.state.properties.BedPart.FOOT),
            2);
        level.setBlock(
            head,
            base.setValue(
                net.minecraft.world.level.block.BedBlock.PART,
                net.minecraft.world.level.block.state.properties.BedPart.HEAD),
            2);
        player.setPos(foot.getX() + .5, foot.getY() + 1, foot.getZ() + .5);
        for (var clicked : java.util.List.of(foot, head)) {
          var event =
              new net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock(
                  player,
                  net.minecraft.world.InteractionHand.MAIN_HAND,
                  clicked,
                  new net.minecraft.world.phys.BlockHitResult(
                      net.minecraft.world.phys.Vec3.atCenterOf(clicked),
                      net.minecraft.core.Direction.UP,
                      clicked,
                      false));
          VoidspaceBeds.useBed(event);
          check(
              event.isCanceled()
                  && event.getCancellationResult() == net.minecraft.world.InteractionResult.SUCCESS,
              "safe bed interaction consumed");
          check(
              head.equals(player.getRespawnPosition())
                  && player.getRespawnDimension().equals(VoidspaceTravel.DIMENSION),
              "head and dimension saved for both halves");
          check(!player.isRespawnForced(), "respawn retains vanilla safe bed requirements");
          check(
              level.getBlockState(foot).is(bed) && level.getBlockState(head).is(bed),
              "both bed halves intact after use");
          check(
              !player.isSleeping() && level.getDayTime() == time,
              "checkpoint does not advance fixed night");
          var transition =
              player.findRespawnPositionAndUseSpawnBlock(
                  false, net.minecraft.world.level.portal.DimensionTransition.DO_NOTHING);
          check(
              transition.newLevel() == level && !transition.missingRespawnBlock(),
              "actual native respawn transition stays in voidspace");
          check(
              transition.pos().distanceTo(net.minecraft.world.phys.Vec3.atCenterOf(head)) < 4,
              "native respawn finds safe exit near bed");
          var tag = new net.minecraft.nbt.CompoundTag();
          player.saveWithoutId(tag);
          check(
              tag.getString("SpawnDimension").equals("timelocked:voidspace")
                  && !tag.getBoolean("SpawnForced"),
              "checkpoint persisted to native player NBT");
        }
        // Occupied and protected beds cannot replace an existing checkpoint.
        var other = foot.offset(3, 0, 3);
        player.setRespawnPosition(
            net.minecraft.world.level.Level.OVERWORLD, other, 0, false, false);
        level.setBlock(
            head,
            level
                .getBlockState(head)
                .setValue(net.minecraft.world.level.block.BedBlock.OCCUPIED, true),
            2);
        var occupied =
            new net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock(
                player,
                net.minecraft.world.InteractionHand.MAIN_HAND,
                head,
                new net.minecraft.world.phys.BlockHitResult(
                    net.minecraft.world.phys.Vec3.atCenterOf(head),
                    net.minecraft.core.Direction.UP,
                    head,
                    false));
        VoidspaceBeds.useBed(occupied);
        check(
            player.getRespawnDimension().equals(net.minecraft.world.level.Level.OVERWORLD),
            "occupied bed does not replace checkpoint");
        level.setBlock(
            head,
            level
                .getBlockState(head)
                .setValue(net.minecraft.world.level.block.BedBlock.OCCUPIED, false),
            2);
        var denied =
            new net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock(
                player,
                net.minecraft.world.InteractionHand.MAIN_HAND,
                head,
                new net.minecraft.world.phys.BlockHitResult(
                    net.minecraft.world.phys.Vec3.atCenterOf(head),
                    net.minecraft.core.Direction.UP,
                    head,
                    false));
        denied.setUseBlock(net.neoforged.neoforge.common.util.TriState.FALSE);
        VoidspaceBeds.useBed(denied);
        check(
            !denied.isCanceled()
                && player.getRespawnDimension().equals(net.minecraft.world.level.Level.OVERWORLD),
            "protection denial respected");
        VoidspaceBeds.setCheckpoint(player, head);
        level.setBlock(head, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
        level.setBlock(foot, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
        var missing =
            player.findRespawnPositionAndUseSpawnBlock(
                false, net.minecraft.world.level.portal.DimensionTransition.DO_NOTHING);
        check(
            missing.newLevel() == server.overworld() && missing.missingRespawnBlock(),
            "removed bed uses vanilla missing-bed fallback");
      }
    // Existing beds in other dimensions must keep their vanilla behavior.
    var overworld = server.overworld();
    var otherPlayer = new Listener(overworld);
    overworld.getChunkAt(foot);
    overworld.setBlock(
        foot, net.minecraft.world.level.block.Blocks.BLUE_BED.defaultBlockState(), 2);
    var normal =
        new net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock(
            otherPlayer,
            net.minecraft.world.InteractionHand.MAIN_HAND,
            foot,
            new net.minecraft.world.phys.BlockHitResult(
                net.minecraft.world.phys.Vec3.atCenterOf(foot),
                net.minecraft.core.Direction.UP,
                foot,
                false));
    VoidspaceBeds.useBed(normal);
    check(!normal.isCanceled(), "overworld bed behavior untouched");
  }

  static void geometry(ServerLevel level) throws Exception {
    var origin = new net.minecraft.core.BlockPos(1024, 80, 1024);
    for (int iz : new int[] {7, 8}) {
      var id =
          net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
              "timelocked", String.format("sarc/drawing/x02_y00_z%02d", iz));
      var template = level.getStructureManager().getOrCreate(id);
      var at = origin.offset(64, 0, iz * 32);
      for (int x = 64; x < 96; x += 16)
        for (int z = iz * 32; z < (iz + 1) * 32; z += 16) level.getChunkAt(origin.offset(x, 0, z));
      check(
          template.placeInWorld(
              level,
              at,
              at,
              new net.minecraft.world.level.levelgen.structure.templatesystem
                      .StructurePlaceSettings()
                  .setIgnoreEntities(true),
              level.getRandom(),
              2),
          "corrected template places " + iz);
    }
    int logos = 0, oldLogos = 0, medallion = 0, oldMedallion = 0;
    int minX = 999, maxX = -999, minZ = 999, maxZ = -999;
    for (int x = 65; x <= 92; x++)
      for (int y = 6; y <= 17; y++)
        for (int z = 250; z <= 283; z++) {
          var state = level.getBlockState(origin.offset(x, y, z));
          if (state.is(Content.HANGING_LOGO.get())) {
            logos++;
            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
            minZ = Math.min(minZ, z);
            maxZ = Math.max(maxZ, z);
            check(z >= 272 && z <= 274, "logo only over intersection");
            check(!state.getValue(SarcHangingLogoBlock.LIT), "logo initially dark");
            if (z < 270) oldLogos++;
          }
        }
    check(logos == 588 && oldLogos == 0, "588 logo parts, old location cleared");
    check((minZ + maxZ + 1) / 2.0 == 273.5, "logo depth centered over intersection");
    check(
        Math.abs((minX + maxX + 1) / 2.0 - 79.5) <= .5,
        "logo within native half-block of walkway axis");
    int indexed = 0;
    for (int[] v : DrawingArrivalIndex.CELLS) {
      if (v[4] == 1) {
        indexed++;
        check(
            level.getBlockState(origin.offset(v[0], v[1], v[2])).is(Content.HANGING_LOGO.get()),
            "arrival index matches moved logo");
      }
      if (v[1] == 16 && v[2] == 271 && (v[0] == 68 || v[0] == 79 || v[0] == 90))
        check(
            level.getBlockState(origin.offset(v[0], v[1], v[2])).getBlock()
                instanceof StationLightBlock,
            "moved uplight in arrival index");
    }
    check(indexed == 588, "all logo parts indexed");
    for (int x = 65; x <= 92; x++)
      for (int z = 271; z <= 275; z++)
        check(
            level
                .getBlockState(origin.offset(x, 17, z))
                .is(VoidspaceContent.block("timelocked:sarc_service_panel")),
            "crossbeam moved with logo");
    for (int x : new int[] {66, 91})
      for (int z : new int[] {271, 275})
        check(
            level
                .getBlockState(origin.offset(x, 16, z))
                .is(net.minecraft.world.level.block.Blocks.IRON_BARS),
            "four hangers moved");
    for (int x = 68; x <= 90; x++)
      for (int z = 249; z <= 283; z++) {
        if (level
            .getBlockState(origin.offset(x, 1, z))
            .is(net.minecraft.world.level.block.Blocks.LIGHT_GRAY_CONCRETE)) {
          if (z < 265) oldMedallion++;
          else medallion++;
        }
      }
    check(
        oldMedallion == 0 && medallion == 173,
        "single relocated medallion with all 173 interior tiles");
    check(
        level
            .getBlockState(origin.offset(79, 1, 273))
            .is(net.minecraft.world.level.block.Blocks.LIGHT_GRAY_CONCRETE),
        "medallion directly below logo");
    for (int x = 74; x <= 84; x++)
      for (int z = 250; z <= 280; z++)
        for (int y = 2; y <= 5; y++)
          check(level.getBlockState(origin.offset(x, y, z)).isAir(), "walkway headroom retained");
    for (int z : new int[] {250, 255, 260, 264})
      check(
          level
              .getBlockState(origin.offset(79, 1, z))
              .is(net.minecraft.world.level.block.Blocks.POLISHED_ANDESITE),
          "old floor restored");
  }

  static class IntroPlayer extends FakePlayer {
    final List<String> messages = new ArrayList<>();

    IntroPlayer(ServerLevel l, UUID uuid, String name) {
      super(l, new GameProfile(uuid, name));
      try {
        var network =
            new net.minecraft.network.Connection(
                net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        var field = net.minecraft.network.Connection.class.getDeclaredField("channel");
        field.setAccessible(true);
        field.set(network, new io.netty.channel.embedded.EmbeddedChannel());
        connection =
            new net.minecraft.server.network.ServerGamePacketListenerImpl(
                l.getServer(),
                network,
                this,
                net.minecraft.server.network.CommonListenerCookie.createInitial(
                    getGameProfile(), false)) {
              @Override
              public void send(net.minecraft.network.protocol.Packet<?> packet) {}

              @Override
              public void send(
                  net.minecraft.network.protocol.Packet<?> packet,
                  net.minecraft.network.PacketSendListener listener) {}
            };
      } catch (Exception ex) {
        throw new RuntimeException(ex);
      }
    }

    @Override
    public void sendSystemMessage(Component message) {
      messages.add(
          message.getContents()
                  instanceof net.minecraft.network.chat.contents.TranslatableContents translated
              ? translated.getKey()
              : message.getString());
    }

    @Override
    public void playNotifySound(SoundEvent sound, SoundSource source, float volume, float pitch) {}
  }

  static void ticks(net.minecraft.server.MinecraftServer server, int n) {
    for (int i = 0; i < n; i++)
      IntroSequence.tick(
          new net.neoforged.neoforge.event.tick.ServerTickEvent.Post(() -> true, server));
  }

  static void intro(net.minecraft.server.MinecraftServer server) throws Exception {
    ServerLevel earth = server.overworld(), home = server.getLevel(IntroSequence.HOME_DIM);
    IntroState.get(server).players.clear();
    server.getGameRules().getRule(StoryMode.RULE).set(false, server);
    check(
        !new net.minecraft.world.level.GameRules().getBoolean(StoryMode.RULE),
        "ordinary new worlds default to non-Story");
    for (var mode :
        List.of(
            net.minecraft.world.level.GameType.SURVIVAL,
            net.minecraft.world.level.GameType.CREATIVE)) {
      var normal = new IntroPlayer(earth, UUID.randomUUID(), "NormalModeAudit");
      normal.setGameMode(mode);
      IntroSequence.login(
          new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent(normal));
      check(
          IntroSequence.state(normal).getInt("Stage") == IntroSequence.DONE,
          "normal " + mode + " skips automatic intro");
    }
    server.getGameRules().getRule(StoryMode.RULE).set(true, server);
    var restoredRules =
        new net.minecraft.world.level.GameRules(
            new com.mojang.serialization.Dynamic<>(
                net.minecraft.nbt.NbtOps.INSTANCE, server.getGameRules().createTag()));
    check(
        restoredRules.getBoolean(StoryMode.RULE),
        "Story world flag survives serialized game rules");

    check(home != null, "private home dimension loaded");
    check(
        home.getBiome(new BlockPos(0, 80, 0))
            .value()
            .getMobSettings()
            .getMobs(net.minecraft.world.entity.MobCategory.MONSTER)
            .isEmpty(),
        "home biome has no hostile spawn table");
    for (String name : List.of("tv", "rain", "transition", "surf"))
      check(
          net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.containsKey(
              net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                  "timelocked", "intro_" + name)),
          "intro sound registered " + name);
    check(
        home.dimensionType().fixedTime().orElse(-1) == 18000,
        "private home has fixed night without editing Earth time");
    IntroPlayer p =
        new IntroPlayer(
            earth, UUID.fromString("00000000-0000-0000-0000-000000008916"), "IntroAudit");
    p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
    p.setPos(0, 80, 0);
    p.getInventory()
        .setItem(
            0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND, 7));
    var playersField = net.minecraft.server.players.PlayerList.class.getDeclaredField("players");
    playersField.setAccessible(true);
    @SuppressWarnings("unchecked")
    var playerList =
        (List<net.minecraft.server.level.ServerPlayer>) playersField.get(server.getPlayerList());
    playerList.add(p);
    long earthTime = earth.getDayTime();
    boolean rain = earth.isRaining();
    IntroSequence.login(
        new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent(p));
    check(IntroSequence.state(p).getInt("Stage") == 0, "new Survival player queued once");
    ticks(server, 30);
    CompoundTag t = IntroSequence.state(p);
    check(t.getInt("Stage") == IntroSequence.HOME, "fresh player starts in home");
    check(
        p.level() == server.getLevel(IntroSequence.CAMPUS_DIM),
        "daytime home is staged independently from Earth world");
    CampusOpeningAudit.run(p, t, earth);
    check(
        p.gameMode.getGameModeForPlayer() == net.minecraft.world.level.GameType.ADVENTURE,
        "set protected using temporary Adventure mode");
    check(p.getInventory().getItem(0).getCount() == 7, "inventory retained on entry");
    BlockPos o = IntroSequence.home(t);
    for (int x = 3; x <= 8; x++)
      for (int y = 2; y <= 4; y++) {
        BlockPos cell = o.offset(x, y, 3);
        check(
            home.getBlockState(cell).is(MonitorContent.MONITOR.get()),
            "giant television cell " + x + ":" + y);
        check(
            ((MonitorContent.MonitorEntity) home.getBlockEntity(cell)).theme
                == MonitorContent.HOME_TV,
            "TV on theme " + cell);
        var wall = MonitorWallLayout.find(home, cell, net.minecraft.core.Direction.SOUTH);
        check(wall.width() == 6 && wall.height() == 3, "all TV cells merge into one 6x3 panel");
      }
    check(!IntroScenes.isTv(o, o.offset(5, 2, 4)), "adjacent air is not a TV trigger");
    check(IntroScenes.isTv(o, o.offset(9, 2, 3)), "actual power button recognized");
    var power = home.getBlockState(o.offset(9, 2, 3));
    check(
        power.is(net.minecraft.world.level.block.Blocks.STONE_BUTTON)
            && power.canSurvive(home, o.offset(9, 2, 3)),
        "power button has a solid control pod behind it");
    check(
        home.getBlockState(o.offset(19, 1, 11)).is(net.minecraft.world.level.block.Blocks.RED_BED),
        "real bedroom built");
    for (var direction : net.minecraft.core.Direction.Plane.HORIZONTAL) {
      BlockPos signAt = o.offset(12, 1, 16);
      var signState =
          CoastSigns.RESTRICTION
              .get()
              .defaultBlockState()
              .setValue(CoastSigns.Panel.FACING, direction);
      home.setBlock(signAt, signState, 3);
      CoastSigns.RESTRICTION
          .get()
          .setPlacedBy(
              home,
              signAt,
              signState,
              p,
              new net.minecraft.world.item.ItemStack(CoastSigns.RESTRICTION.get()));
      check(
          home.getBlockState(signAt.above()).getValue(CoastSigns.Panel.HALF)
              == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER,
          "sign item places physical upper half " + direction);
      check(
          home.getBlockState(signAt.above()).getValue(CoastSigns.Panel.FACING) == direction,
          "upper sign orientation " + direction);
      home.destroyBlock(signAt.above(), false);
      check(home.getBlockState(signAt).isAir(), "breaking upper panel clears posts " + direction);
      home.setBlock(signAt, signState, 3);
      CoastSigns.RESTRICTION
          .get()
          .setPlacedBy(
              home,
              signAt,
              signState,
              p,
              new net.minecraft.world.item.ItemStack(CoastSigns.RESTRICTION.get()));
      home.destroyBlock(signAt, false);
      check(home.getBlockState(signAt.above()).isAir(), "breaking posts clears panel " + direction);
    }
    BlockPos tv = o.offset(5, 2, 3);
    var use =
        new net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock(
            p,
            net.minecraft.world.InteractionHand.MAIN_HAND,
            tv,
            new net.minecraft.world.phys.BlockHitResult(
                net.minecraft.world.phys.Vec3.atCenterOf(tv),
                net.minecraft.core.Direction.SOUTH,
                tv,
                false));
    IntroSequence.interact(use);
    check(
        use.isCanceled() && t.getInt("Stage") == IntroSequence.BEDTIME,
        "TV interaction advances to bedtime");
    for (int x = 3; x <= 8; x++)
      for (int y = 2; y <= 4; y++)
        check(
            ((MonitorContent.MonitorEntity) home.getBlockEntity(o.offset(x, y, 3))).theme
                == MonitorContent.HOME_TV_OFF,
            "entire screen powers off");
    p.setPos(o.getX() + 19.5, o.getY() + 1, o.getZ() + 13.5);
    ticks(server, 1);
    check(t.getInt("Stage") == IntroSequence.BREAKIN, "walking to bedroom starts break-in");
    ticks(server, 60);
    check(home.getBlockState(o.offset(-2, 2, 5)).isAir(), "window actually breaks");
    ticks(server, 80);
    check(t.hasUUID("Actor"), "silhouette is an actual private scene actor");
    check(home.getEntity(t.getUUID("Actor")) != null, "silhouette entity exists");
    IntroState data = IntroState.get(server);
    var loaded =
        IntroState.load(
            data.save(new CompoundTag(), server.registryAccess()), server.registryAccess());
    check(
        loaded.players.get(p.getUUID()).getInt("Stage") == IntroSequence.BREAKIN,
        "stage persists round trip");
    check(
        loaded.players.get(p.getUUID()).getInt("Age") == t.getInt("Age"),
        "elapsed scene time persists round trip");
    check(loaded.nextSlot == data.nextSlot, "private slot allocation persists");
    playerList.remove(p);
    int paused = t.getInt("Age");
    ticks(server, 50);
    check(t.getInt("Age") == paused, "offline player does not advance");
    playerList.add(p);
    ticks(server, 90);
    check(t.getInt("Stage") == IntroSequence.BLACKOUT, "break-in cuts to black");
    ticks(server, 50);
    check(
        t.getInt("Stage") == IntroSequence.LANDING,
        "blackout waits for client chunks before revealing beach");
    check(p.isNoGravity(), "arrival gravity disabled until supported release");
    int token = t.getInt("LandingToken");
    IntroSequence.ready(p, token + 1);
    check(t.getInt("Stage") == IntroSequence.LANDING, "wrong nonce cannot release landing");
    var anchor = p.position();
    p.setPos(p.getX(), -50, p.getZ());
    p.setDeltaMovement(0, -5, 0);
    ticks(server, 40);
    check(
        p.position().equals(anchor) && p.getDeltaMovement().lengthSqr() == 0,
        "forced fall corrected to exact saved anchor");
    check(t.getInt("Stage") == IntroSequence.LANDING, "delayed client never starts wake timer");
    IntroSequence.login(
        new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent(p));
    check(t.getInt("LandingToken") != token, "rejoin invalidates stale ready nonce");
    IntroSequence.ready(p, token);
    check(t.getInt("Stage") == IntroSequence.LANDING, "stale pre-rejoin ack rejected");
    IntroSequence.ready(p, t.getInt("LandingToken"));
    check(t.getInt("Stage") == IntroSequence.WAKE, "matching client-ready starts wake");
    check(
        t.hasUUID("DepartureActor")
            && IntroDeparture.live.get(p.getUUID()).actor() instanceof Pantheris,
        "Pantheris is a real native entity: " + t.getString("DepartureStatus"));
    check(
        t.hasUUID("DepartureBoat")
            && IntroDeparture.live.get(p.getUUID()).boat()
                instanceof net.minecraft.world.entity.vehicle.Boat,
        "real boat exists offshore");
    var actor = IntroDeparture.live.get(p.getUUID()).actor();
    var boat = IntroDeparture.live.get(p.getUUID()).boat();
    check(actor != null && actor.getVehicle() == boat, "Pantheris actually rides boat");
    check(
        !boat.shouldBeSaved() && !actor.shouldBeSaved(),
        "cinematic entities never persist as abandoned actors");
    double boatX = boat.getX();
    ticks(server, 80);
    check(boat.getX() > boatX, "boat moves away while player wakes");
    IntroSequence.ready(p, t.getInt("LandingToken"));
    check(t.getInt("Age") == 80, "duplicate ACK cannot restart wake timer");
    check(p.level() == earth, "beach is on Earth / Overworld");
    BlockPos q = IntroCoast.wake(BlockPos.of(t.getLong("Site")));
    check(
        p.blockPosition().equals(q),
        "wake position matches coast module: actual=" + p.position() + " expected=" + q);
    check(
        earth.getBlockState(q.below()).is(net.minecraft.world.level.block.Blocks.SAND),
        "wake spawn has dry sand support");
    check(
        earth.getBlockState(q).isAir() && earth.getBlockState(q.above()).isAir(),
        "wake spawn has clear headroom");
    check(
        earth.getBlockState(q.offset(30, -2, 0)).is(net.minecraft.world.level.block.Blocks.WATER),
        "coast has actual adjacent water");
    check(!t.hasUUID("Actor"), "silhouette removed at transition");
    check(
        p.getRespawnDimension().equals(net.minecraft.world.level.Level.OVERWORLD)
            && q.equals(p.getRespawnPosition()),
        "new-player respawn checkpoint is Earth beach");
    check(
        earth.getBlockState(q.offset(-3, 0, -5)).is(CoastSigns.RESTRICTION.get()),
        "modeled SARC restriction sign generated");
    check(
        earth.getBlockState(q.offset(-6, 0, 3)).is(CoastSigns.DIRECTION.get()),
        "modeled boardwalk direction sign generated");
    check(
        earth.getBlockState(q.offset(-3, 1, -5)).getValue(CoastSigns.Panel.HALF)
            == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER,
        "modeled sign has physical selectable upper panel");
    check(
        earth.getBlockState(q.offset(-3, 0, -5)).getValue(CoastSigns.Panel.FACING)
            == net.minecraft.core.Direction.EAST,
        "sign faces arriving beach player");
    check(
        p.messages.stream().filter(m -> m.contains("intro.breath")).count() == 1,
        "wake breathing thought appears once after first opening");
    ticks(server, WakePresentation.DURATION_TICKS - 80 - 1);
    check(
        t.getInt("Stage") == IntroSequence.WAKE, "wake does not release before presentation ends");
    ticks(server, 1);
    check(t.getInt("Stage") == IntroSequence.DONE, "wake releases player and completes");
    check(
        p.messages.stream().filter(m -> m.contains("intro.where")).count() == 1,
        "where-am-I line appears once after looking up");
    check(
        p.getYRot() == WakePresentation.FINAL_YAW_DEGREES
            && p.getXRot() == WakePresentation.FINAL_PITCH_DEGREES,
        "gameplay look exactly matches the final wake camera");
    check(
        p.gameMode.getGameModeForPlayer() == net.minecraft.world.level.GameType.SURVIVAL,
        "original Survival mode restored");
    check(!p.isNoGravity(), "normal gravity restored only at release");
    check(actor.isRemoved() && boat.isRemoved(), "boat and actor cleaned after cameo");
    check(
        p.getInventory().getItem(0).getCount() == 7, "inventory retained through entire sequence");
    check(
        earth.getDayTime() == earthTime && earth.isRaining() == rain,
        "no global time or weather mutation");
    // Walk the generated service route in the real world, checking every column.
    BlockPos site = BlockPos.of(t.getLong("Site"));
    int road = site.getX() + 90, z = q.getZ(), lastY = -999;
    for (int dx = 4; dx <= 278; dx++) {
      int x = road + dx;
      earth.getChunk(x >> 4, z >> 4);
      int y =
          earth.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, x, z)
              - 1;
      check(
          earth
              .getBlockState(new BlockPos(x, y, z))
              .is(net.minecraft.world.level.block.Blocks.SPRUCE_PLANKS),
          "continuous boardwalk column " + dx);
      if (lastY != -999) check(Math.abs(y - lastY) <= 1, "walkable grade at " + dx);
      check(
          earth.getBlockState(new BlockPos(x, y + 1, z)).isAir()
              && earth.getBlockState(new BlockPos(x, y + 2, z)).isAir(),
          "path headroom " + dx);
      lastY = y;
    }
    // Every parking column has a firm deck. Central driving aisle / sidewalk
    // must retain two-block headroom, including the boardwalk connection.
    for (int dx = 250; dx <= 276; dx++)
      for (int dz = -42; dz <= -10; dz++) {
        BlockPos at = new BlockPos(road + dx, 64, z + dz);
        earth.getChunkAt(at);
        check(earth.getBlockState(at).isSolidRender(earth, at), "parking deck " + dx + ":" + dz);
        if ((dx >= 260 && dx <= 267) || (dx >= 273))
          check(
              earth.getBlockState(at.above()).isAir() && earth.getBlockState(at.above(2)).isAir(),
              "parking aisle / sidewalk clearance");
      }
    for (int dz = -10; dz <= -2; dz++)
      for (int dx = 273; dx <= 276; dx++) {
        BlockPos at = new BlockPos(road + dx, 64, z + dz);
        check(
            earth.getBlockState(at).is(net.minecraft.world.level.block.Blocks.SMOOTH_STONE),
            "sidewalk reaches boardwalk");
        check(
            earth.getBlockState(at.above()).isAir() && earth.getBlockState(at.above(2)).isAir(),
            "pedestrian connection clear");
      }
    int prev = -999;
    for (int dx = 3; dx <= 264; dx++) {
      int x = road + dx, zz = z - 28;
      earth.getChunk(x >> 4, zz >> 4);
      int y =
          earth.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, x, zz)
              - 1;
      check(
          earth
              .getBlockState(new BlockPos(x, y, zz))
              .is(net.minecraft.world.level.block.Blocks.GRAY_CONCRETE),
          "driveway reaches road and lot " + dx);
      if (prev != -999) check(Math.abs(y - prev) <= 1, "driveway grade " + dx);
      prev = y;
      check(
          earth.getBlockState(new BlockPos(x, y + 1, zz)).isAir()
              && earth.getBlockState(new BlockPos(x, y + 2, zz)).isAir(),
          "driveway headroom");
    }
    var layout = new LinkedHashMap<String, Object>();
    var cells = new ArrayList<String>();
    for (int dz = -48; dz <= 30; dz++)
      for (int dx = 230; dx <= 330; dx++) {
        BlockPos column = new BlockPos(road + dx, 64, z + dz);
        earth.getChunkAt(column);
        int top =
            earth.getHeight(
                    net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,
                    column.getX(),
                    column.getZ())
                - 1;
        cells.add(
            net.minecraft.core.registries.BuiltInRegistries.BLOCK
                .getKey(earth.getBlockState(column.atY(top)).getBlock())
                .toString());
      }
    layout.put("x0", 230);
    layout.put("x1", 330);
    layout.put("z0", -48);
    layout.put("z1", 30);
    layout.put("cells", cells);
    Files.writeString(output("coast-layout.json"), new GsonBuilder().create().toJson(layout));
    IntroSequence.login(
        new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent(p));
    ticks(server, 35);
    check(
        IntroSequence.state(p).getInt("Stage") == IntroSequence.DONE,
        "completed player never auto-replays");
    // Manual replay preserves prior personal bed checkpoint, inventory and mode.
    p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
    BlockPos checkpoint = new BlockPos(100, 80, 100);
    p.setRespawnPosition(net.minecraft.world.level.Level.OVERWORLD, checkpoint, 0, false, false);
    p.getStats().setValue(p, Stats.CUSTOM.get(Stats.PLAY_TIME), 1000);
    check(IntroSequence.begin(p), "operator replay can start");
    t = IntroSequence.state(p);
    IntroSequence.finish(p, t, true);
    check(t.getInt("Stage") == IntroSequence.LANDING, "skip also waits behind landing barrier");
    IntroSequence.ready(p, t.getInt("LandingToken"));
    check(
        p.gameMode.getGameModeForPlayer() == net.minecraft.world.level.GameType.CREATIVE,
        "skip restores previous Creative mode");
    check(
        p.getRespawnPosition().equals(checkpoint),
        "replay does not replace an existing player's bed");
    check(p.getInventory().getItem(0).getCount() == 7, "skip retains inventory");
    // Disable first-join only; established players are never automatically relocated.
    IntroPlayer old =
        new IntroPlayer(
            earth, UUID.fromString("00000000-0000-0000-0000-000000008917"), "EstablishedAudit");
    old.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
    old.getStats().setValue(old, Stats.CUSTOM.get(Stats.PLAY_TIME), 1000);
    IntroSequence.login(
        new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent(old));
    check(
        IntroSequence.state(old).getInt("Stage") == IntroSequence.DONE,
        "established player not kidnapped on upgrade");
    check(
        server.getCommands().getDispatcher().getRoot().getChild("timelockedintro") != null,
        "intro command registered");
    var buf =
        new net.minecraft.network.RegistryFriendlyByteBuf(
            io.netty.buffer.Unpooled.buffer(), server.registryAccess());
    var message = new IntroNetworking.Cue(IntroSequence.WAKE, 45);
    IntroNetworking.Cue.CODEC.encode(buf, message);
    check(
        IntroNetworking.Cue.CODEC.decode(buf).equals(message),
        "client cinematic cue round trips through native network codec");
    buf.release();
    // Two players can inhabit independent private sets at the same time.
    playerList.add(old);
    check(IntroSequence.begin(p), "parallel player A intro starts");
    BlockPos homeA = IntroSequence.home(IntroSequence.state(p));
    var aPos = p.position();
    check(IntroSequence.begin(old), "parallel player B intro starts");
    BlockPos homeB = IntroSequence.home(IntroSequence.state(old));
    check(
        !homeA.equals(homeB) && homeA.distSqr(homeB) >= 4096,
        "multiplayer homes use distinct slots");
    check(p.position().equals(aPos), "starting another player does not teleport player A");
    IntroSequence.finish(old, IntroSequence.state(old), true);
    IntroSequence.ready(old, IntroSequence.state(old).getInt("LandingToken"));
    check(
        IntroSequence.state(p).getInt("Stage") == IntroSequence.HOME,
        "other player's skip cannot finish player A");
    IntroSequence.finish(p, IntroSequence.state(p), true);
    IntroSequence.ready(p, IntroSequence.state(p).getInt("LandingToken"));
    playerList.remove(old);
    // A stalled client safely recovers rather than releasing into unloaded terrain.
    check(IntroSequence.begin(p), "timeout replay starts");
    t = IntroSequence.state(p);
    IntroSequence.finish(p, t, true);
    ticks(server, 600);
    check(
        t.getInt("Stage") == IntroSequence.DONE && !p.isNoGravity(),
        "no-ACK timeout restores control and gravity");
    check(
        p.gameMode.getGameModeForPlayer() == net.minecraft.world.level.GameType.CREATIVE,
        "timeout restores original mode");
    var readyBuf =
        new net.minecraft.network.RegistryFriendlyByteBuf(
            io.netty.buffer.Unpooled.buffer(), server.registryAccess());
    var readyMsg = new IntroNetworking.Ready(12345);
    IntroNetworking.Ready.CODEC.encode(readyBuf, readyMsg);
    check(
        IntroNetworking.Ready.CODEC.decode(readyBuf).equals(readyMsg), "readiness codec roundtrip");
    readyBuf.release();
    // Existing .16 completed saves can self-rescue only when below their own beach.
    t.putBoolean("AtBeach", true);
    t.putInt("Stage", IntroSequence.DONE);
    p.setPos(q.getX() + .5, 20, q.getZ() + .5);
    int rescued =
        server
            .getCommands()
            .getDispatcher()
            .execute("timelockedintro rescue", p.createCommandSourceStack());
    check(
        rescued == 1 && t.getInt("Stage") == IntroSequence.LANDING,
        "legacy failed landing rescue uses safe barrier");
    IntroSequence.ready(p, t.getInt("LandingToken"));
    check(
        t.getInt("Stage") == IntroSequence.DONE && p.getY() == 65,
        "rescue releases on supported dry sand");
    check(
        server
                .getCommands()
                .getDispatcher()
                .execute("timelockedintro rescue", p.createCommandSourceStack())
            == 0,
        "rescue cannot be used as an arbitrary teleport");
    playerList.remove(p);
  }

  // Reflection keeps production-only helpers private while testing recovered bounds/sound order.
  static void arrivalRefactor(net.minecraft.server.MinecraftServer server) throws Exception {
    var player = new Listener(server.overworld());
    var origin = new BlockPos(1024, 70, -2048);
    var inside =
        HubArrival.class.getDeclaredMethod(
            "inside", net.minecraft.server.level.ServerPlayer.class, BlockPos.class);
    inside.setAccessible(true);
    int[][] cases = {
      {4, 2, 186, 1}, {153, 18, 336, 1},
      {3, 2, 186, 0}, {154, 2, 186, 0},
      {4, 1, 186, 0}, {4, 19, 186, 0},
      {4, 2, 185, 0}, {4, 2, 337, 0}
    };
    for (int[] sample : cases) {
      player.setPos(
          origin.getX() + sample[0], origin.getY() + sample[1], origin.getZ() + sample[2]);
      check(
          (boolean) inside.invoke(null, player, origin) == (sample[3] == 1),
          "arrival bounds retained " + java.util.Arrays.toString(sample));
    }
    var sound = HubArrival.class.getDeclaredMethod("soundForStage", int.class);
    sound.setAccessible(true);
    SoundEvent[] expected = {
      AlarmSounds.ARRIVAL_02.get(),
      AlarmSounds.ARRIVAL_03.get(),
      AlarmSounds.ARRIVAL_04.get(),
      AlarmSounds.ARRIVAL_04.get(),
      AlarmSounds.ARRIVAL_01.get()
    };
    for (int stage = 1; stage <= expected.length; stage++)
      check(
          sound.invoke(null, stage) == expected[stage - 1],
          "arrival sound order retained " + stage);
    check(
        DrawingArrivalIndex.position(origin, new int[] {5, 7, 200, 4, 0})
            .equals(origin.offset(5, 7, 200)),
        "drawing position stays origin-relative");
  }

  // Expected hashes were extracted from the pre-refactor Java, including float bit patterns.
  // Rebaseline only for an intentional art change, not to conceal a refactor mismatch.
  static String geometryDigest(java.util.function.Consumer<java.io.DataOutputStream> writer)
      throws Exception {
    var bytes = new java.io.ByteArrayOutputStream();
    writer.accept(new java.io.DataOutputStream(bytes));
    return java.util.HexFormat.of()
        .formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()));
  }

  static void authoredGeometry(net.minecraft.server.MinecraftServer server) throws Exception {
    var logo = AuthoredGeometry.logo();
    String logoHash =
        geometryDigest(
            out -> {
              try {
                out.writeInt(logo.columns());
                out.writeInt(logo.rows());
                out.writeInt(logo.pixelsPerBlock());
                out.writeInt(logo.parts().size());
                for (var part : logo.parts()) {
                  out.writeInt(part.size());
                  for (var rect : part) {
                    out.writeInt(rect.minX());
                    out.writeInt(rect.minY());
                    out.writeInt(rect.maxX());
                    out.writeInt(rect.maxY());
                  }
                }
              } catch (java.io.IOException error) {
                throw new java.io.UncheckedIOException(error);
              }
            });
    check(
        logoHash.equals("5829b8f0a85dfed226b270343ff386d219de8db0921a0c3ef6702b994e2d78c6"),
        "all recovered logo rectangles/order match baseline");
    var logoBlock = (SarcHangingLogoBlock) Content.HANGING_LOGO.get();
    for (int part = 0; part < logo.parts().size(); part++) {
      var expected = net.minecraft.world.phys.shapes.Shapes.empty();
      for (var rect : logo.parts().get(part))
        expected =
            net.minecraft.world.phys.shapes.Shapes.or(
                expected,
                net.minecraft.world.level.block.Block.box(
                    rect.minX(), rect.minY(), 0, rect.maxX(), rect.maxY(), logo.pixelsPerBlock()));
      var actual =
          logoBlock.getShape(
              logoBlock.defaultBlockState().setValue(SarcHangingLogoBlock.PART, part),
              server.overworld(),
              BlockPos.ZERO,
              net.minecraft.world.phys.shapes.CollisionContext.empty());
      check(
          !net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(
              expected, actual, net.minecraft.world.phys.shapes.BooleanOp.NOT_SAME),
          "logo shape unchanged for saved part " + part);
    }
    var hashes =
        Map.of(
            "rover",
            "7e2b5dbd51e88b08e7cbabb09742bf4bd48c50c9e7f51e667dc9637b47f07dad",
            "truck",
            "9a4190f46024052fe5bb09302257452975dc75639162159ad1add037272e1dc5",
            "tram",
            "cf503c3d74cda2791bab6ee8cea14b7e2914264e57161752e694e6636eb101b8");
    for (var entry : hashes.entrySet()) {
      var vehicle = AuthoredGeometry.vehicle(entry.getKey());
      String hash =
          geometryDigest(
              out -> {
                try {
                  out.writeInt(vehicle.textureWidth());
                  out.writeInt(vehicle.textureHeight());
                  out.writeInt(vehicle.wheelIds().size());
                  for (int id : vehicle.wheelIds()) out.writeInt(id);
                  out.writeInt(vehicle.cuboids().size());
                  for (var cube : vehicle.cuboids()) {
                    out.writeUTF(cube.name());
                    out.writeInt(cube.textureU());
                    out.writeInt(cube.textureV());
                    out.writeFloat(cube.x());
                    out.writeFloat(cube.y());
                    out.writeFloat(cube.z());
                    out.writeFloat(cube.width());
                    out.writeFloat(cube.height());
                    out.writeFloat(cube.depth());
                    out.writeFloat(cube.pivotX());
                    out.writeFloat(cube.pivotY());
                    out.writeFloat(cube.pivotZ());
                  }
                } catch (java.io.IOException error) {
                  throw new java.io.UncheckedIOException(error);
                }
              });
      check(
          hash.equals(entry.getValue()),
          "all native vehicle cuboids/UVs/pivots/wheels match baseline " + entry.getKey());
      int[] wheels = vehicle.wheelParts();
      int original = wheels[0];
      wheels[0] = -1;
      check(
          vehicle.wheelParts()[0] == original,
          "wheel caller cannot mutate cached drawing " + entry.getKey());
    }
    for (String name : List.of("sarc-logo", "rover")) {
      var resource =
          AuthoredGeometry.class.getResourceAsStream("/data/timelocked/geometry/" + name + ".json");
      check(resource != null, "geometry resource packaged " + name);
      try (resource) {
        var data =
            com.google.gson.JsonParser.parseReader(
                    new java.io.InputStreamReader(
                        resource, java.nio.charset.StandardCharsets.UTF_8))
                .getAsJsonObject();
        if (name.equals("sarc-logo")) {
          data.addProperty("columns", 0);
          try {
            AuthoredGeometry.parseLogo(data);
            check(false, "invalid logo width rejected");
          } catch (IllegalArgumentException expected) {
            check(true, "invalid logo width rejected");
          }
        } else {
          data.getAsJsonArray("wheelParts").add(99999);
          try {
            AuthoredGeometry.parseVehicle(data);
            check(false, "unknown wheel ID rejected");
          } catch (IllegalArgumentException expected) {
            check(true, "unknown wheel ID rejected");
          }
        }
      }
    }
    // Real server interactions: preserve balance, item costs and cooldown persistence.
    var patient = new Listener(server.overworld());
    patient.setHealth(8);
    patient.setItemInHand(
        net.minecraft.world.InteractionHand.MAIN_HAND,
        new net.minecraft.world.item.ItemStack(Content.BANDAGE.get(), 2));
    var medic = CelestialJobs.Role.MEDIC.block.get();
    long now = server.overworld().getGameTime();
    check(medic.use(patient, server.overworld(), BlockPos.ZERO), "medic still accepts a bandage");
    check(
        patient.getHealth() == 14 && patient.getMainHandItem().getCount() == 1,
        "medic preserves six-HP treatment and one-bandage cost");
    check(
        patient.getPersistentData().getLong("timelocked_medical_ready") == now + 200,
        "medic preserves ten-second saved cooldown");
    check(
        !medic.use(patient, server.overworld(), BlockPos.ZERO) && patient.getHealth() == 14,
        "medic cannot bypass cooldown");
    var visitor = new Listener(server.overworld());
    visitor.setItemInHand(
        net.minecraft.world.InteractionHand.MAIN_HAND,
        new net.minecraft.world.item.ItemStack(VoidspaceContent.CRYSTAL_CLUSTER.get(), 3));
    var altar = CelestialJobs.Role.ATTUNER.block.get();
    check(
        altar.use(visitor, server.overworld(), BlockPos.ZERO)
            && visitor.getMainHandItem().isEmpty(),
        "attuner preserves three-cluster cost");
    check(
        visitor.getEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION).getDuration() == 2400
            && visitor
                    .getEffect(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE)
                    .getDuration()
                == 1200,
        "attunement preserves two-minute vision and one-minute resistance");
    check(
        visitor.getPersistentData().getLong("timelocked_attunement_ready") == now + 1200
            && !altar.use(visitor, server.overworld(), BlockPos.ZERO),
        "attunement preserves cooldown");
    var saved = new CompoundTag();
    saved.putInt("ArrivalStep", 999);
    saved.putInt("ArrivalWelcomeNext", 999);
    saved.putInt("ArrivalWelcomeTicks", 999);
    saved.putInt("Wave", 999);
    var progress = SiteProgress.load(saved);
    check(
        progress.arrivalStep == 5
            && progress.arrivalWelcomeNext == 7
            && progress.arrivalWelcomeTicks == 200
            && progress.wave == 3,
        "named saved-progress bounds preserve legacy clamp values");
    check(
        AlarmRules.soundDue(40, 0) && !AlarmRules.soundDue(39, 0),
        "normal alarm keeps two-second period");
    check(
        DockingRules.afterEqualization(true, true, true) == 4
            && DockingRules.afterEqualization(false, true, true) == 2
            && DockingRules.canRelease(1, false)
            && !DockingRules.canRelease(4, false),
        "named docking stages preserve interlocks");
  }

  @SubscribeEvent
  public static void started(ServerStartedEvent event) {
    var server = event.getServer();
    server.execute(
        () -> {
          try {
            authoredGeometry(server);
            arrivalRefactor(server);
            intro(server);
            CoastChapterAudit.run(server);
            CoastalTruckAudit.run(server);
            VehicleAudioAudit.run(server);
            ObjectiveHudAudit.run(server);
            AtlasBootAudit.run(server);
            FacilitySpawnAudit.run(server);
            GlassStairwellAudit.run(server);
            ReceptionChapterAudit.run(server);
            StaffChapterAudit.run(server);
            ResearchStoryAudit.run(server);
            WakePresentationAudit.run();
            HubLightingAudit.run(server);
            TransitionDoorwaysAudit.run(server);
            beds(server);
            geometry(server.overworld());
            Listener player = new Listener(server.overworld());
            var audience = List.<net.minecraft.server.level.ServerPlayer>of(player);
            SiteProgress state = new SiteProgress();
            check(
                !HubArrival.announceLogoPower(state, List.of()),
                "empty hub does not consume trigger");
            check(HubArrival.announceLogoPower(state, audience), "logo starts welcome");
            check(
                player.messages.size() == 1 && player.sounds == 1,
                "one immediate message and anthem");
            check(!HubArrival.announceLogoPower(state, audience), "multipart trigger deduplicated");
            int[] seconds = {7, 9, 8, 10, 9, 7};
            for (int line = 0; line < seconds.length; line++) {
              for (int second = 1; second <= seconds[line]; second++) {
                int previousTicks = state.arrivalWelcomeTicks;
                check(!HubArrival.tickWelcome(state, List.of()), "empty hub pauses line " + line);
                check(state.arrivalWelcomeTicks == previousTicks, "pause does not advance timer");
                check(HubArrival.tickWelcome(state, audience), "active timer advances");
                check(
                    player.messages.size() == (second == seconds[line] ? line + 2 : line + 1),
                    "only one line at correct deadline " + line + ":" + second);
                // Save/reload every second to verify in-progress progress survives.
                state = SiteProgress.load(state.save());
                check(
                    !HubArrival.announceLogoPower(state, audience), "reload never restarts anthem");
              }
            }
            check(
                player.messages.size() == 7 && player.sounds == 1,
                "seven messages, only one anthem");
            for (int i = 0; i < 7; i++)
              check(
                  player.messages.get(i).equals("message.timelocked.arrival_welcome." + (i + 1)),
                  "message order " + i);
            for (int i = 0; i < 30; i++)
              check(!HubArrival.tickWelcome(state, audience), "completed queue does not replay");
            var legacy = new net.minecraft.nbt.CompoundTag();
            legacy.putInt("ArrivalStep", 5);
            check(
                !HubArrival.tickWelcome(SiteProgress.load(legacy), audience),
                "completed legacy save not replayed");
            legacy.putBoolean("ArrivalLogoAnnounced", true);
            check(
                !HubArrival.tickWelcome(SiteProgress.load(legacy), audience),
                "0.8.9.12 welcome not replayed");
            var bad = new net.minecraft.nbt.CompoundTag();
            bad.putBoolean("ArrivalLogoAnnounced", true);
            bad.putInt("ArrivalWelcomeNext", 900);
            bad.putInt("ArrivalWelcomeTicks", -1);
            var safe = SiteProgress.load(bad);
            check(
                safe.arrivalWelcomeNext == 7 && safe.arrivalWelcomeTicks == 0,
                "invalid saved values clamped");
          } catch (Throwable error) {
            failures.add(error.toString());
            error.printStackTrace();
          }
          try {
            var report = new LinkedHashMap<String, Object>();
            report.put("version", "0.8.9.38");
            report.put("checks", checks);
            report.put("failures", failures);
            report.put("status", failures.isEmpty() ? "PASS" : "FAIL");
            report.put("message_times_seconds", List.of(0, 7, 16, 24, 34, 43, 50));
            Files.writeString(
                output("native-story-audit.json"),
                new GsonBuilder().setPrettyPrinting().create().toJson(report));
          } catch (Exception error) {
            error.printStackTrace();
          }
          server.halt(false);
        });
  }
}
