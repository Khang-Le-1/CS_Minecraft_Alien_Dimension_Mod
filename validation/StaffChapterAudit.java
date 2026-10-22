package com.timelocked;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Chapter 3 runs through actual saved entities, shipped cells and block/NPC interaction hooks. */
final class StaffChapterAudit {
  static void check(boolean value, String text) {
    IntroAudit.check(value, "staff chapter: " + text);
  }

  static void quiet(CoastChapterAudit.Reader player) {
    for (int i = 0; i < 4; i++) StaffChapter.tickPlayer(player);
  }

  static void door(CoastChapterAudit.Reader player, BlockPos pos) {
    var level = player.serverLevel();
    FacilityDetails.PUSH_BAR_DOOR
        .get()
        .useWithoutItem(
            level.getBlockState(pos),
            level,
            pos,
            player,
            new BlockHitResult(Vec3.atCenterOf(pos), Direction.SOUTH, pos, false));
  }

  static void authored(ServerLevel level, BlockPos site) {
    for (int mx = 1; mx <= 3; mx++)
      for (int mz = 4; mz <= 10; mz++) {
        var template =
            level
                .getStructureManager()
                .get(
                    ResourceLocation.fromNamespaceAndPath(
                        "timelocked",
                        String.format(
                            java.util.Locale.ROOT, "sarc/drawing/x%02d_y00_z%02d", mx, mz)))
                .orElseThrow();
        var root = template.save(new CompoundTag());
        var palette = root.getList("palette", Tag.TAG_COMPOUND);
        var states = new java.util.ArrayList<net.minecraft.world.level.block.state.BlockState>();
        for (int i = 0; i < palette.size(); i++)
          states.add(
              NbtUtils.readBlockState(
                  level.holderLookup(Registries.BLOCK), palette.getCompound(i)));
        var blocks = root.getList("blocks", Tag.TAG_COMPOUND);
        for (int i = 0; i < blocks.size(); i++) {
          var row = blocks.getCompound(i);
          var p = row.getList("pos", Tag.TAG_INT);
          int x = p.getInt(0) + mx * 32, y = p.getInt(1), z = p.getInt(2) + mz * 32;
          if (x < 60 || x > 111 || y < 1 || y > 5 || z < 128 || z > 332) continue;
          var state = states.get(row.getInt("state"));
          if (state.is(Blocks.STRUCTURE_VOID)) continue;
          var position = site.offset(x, y, z);
          level.getChunkAt(position);
          level.setBlock(position, state, 2);
        }
      }
    for (var cell : TransitionDoorways.plan().entrySet())
      if (cell.getKey().getX() >= 60
          && cell.getKey().getX() <= 111
          && cell.getKey().getZ() >= 128
          && cell.getKey().getZ() <= 332)
        level.setBlock(site.offset(cell.getKey()), cell.getValue(), 2);
  }

  static HashSet<BlockPos> reachable(
      CoastChapterAudit.Reader player, BlockPos site, BlockPos start) {
    var seen = new HashSet<BlockPos>();
    var queue = new ArrayDeque<BlockPos>();
    queue.add(start);
    seen.add(start);
    var level = player.serverLevel();
    while (!queue.isEmpty()) {
      var at = queue.remove();
      for (Direction d :
          new Direction[] {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
        var next = at.relative(d);
        var local = next.subtract(site);
        if (local.getX() < 60
            || local.getX() > 111
            || local.getZ() < 128
            || local.getZ() > 332
            || seen.contains(next)) continue;
        var body =
            new AABB(
                next.getX() + .2,
                next.getY(),
                next.getZ() + .2,
                next.getX() + .8,
                next.getY() + 1.8,
                next.getZ() + .8);
        if (level.getBlockState(next.below()).isFaceSturdy(level, next.below(), Direction.UP)
            && level.noCollision(player, body)) {
          seen.add(next);
          queue.add(next);
        }
      }
    }
    return seen;
  }

  static CompoundTag ready(CoastChapterAudit.Reader player, BlockPos site) {
    var intro = CoastChapterAudit.intro(player, site);
    var coast = new CompoundTag();
    coast.putInt("Step", CoastChapter.COMPLETE);
    intro.put(CoastChapter.SAVE_KEY, coast);
    var reception = new CompoundTag();
    reception.putInt("Step", ReceptionChapter.COMPLETE);
    intro.put(ReceptionChapter.SAVE_KEY, reception);
    return intro;
  }

  static void run(MinecraftServer server) throws Exception {
    var level = server.overworld();
    var rule = server.getGameRules().getRule(StoryMode.RULE);
    boolean previous = rule.get();
    var data = FacilityState.get(server);
    var site = new BlockPos(74000, 80, 74000);
    var oldSite = data.sites.put(site.asLong(), 44);
    var progress = new SiteProgress();
    progress.geometryVersion = BlueprintRooms.VERSION;
    progress.layoutVersion = 2;
    progress.arrivalLogoAnnounced = true;
    progress.arrivalStep = SiteProgress.FINAL_ARRIVAL_STAGE;
    progress.arrivalWelcomeNext = SiteProgress.WELCOME_LINE_COUNT;
    var oldProgress = data.progress.put(site.asLong(), progress);
    var player = new CoastChapterAudit.Reader(level, "BriefingReader");
    var second = new CoastChapterAudit.Reader(level, "BriefingSecond");
    SarcScientist npc = null;
    UUID uuid = null;
    try {
      authored(level, site);
      var home = site.offset(StaffChapter.DESK);
      var terminal = site.offset(StaffChapter.TERMINAL);
      var front = site.offset(StaffChapter.LAB_DOOR_LEFT_X, 2, StaffChapter.LAB_DOOR_Z);
      check(
          level.getBlockState(terminal).is(FacilityDetails.WORKSTATION.get()),
          "orientation uses a shipped workstation");
      check(
          level.getBlockState(home).isAir() && level.getBlockState(home.above()).isAir(),
          "briefing officer fits the existing operations area");
      rule.set(false, server);
      check(StaffChapter.ensure(level, site) == null, "non-Story world spawns no briefing officer");
      rule.set(true, server);
      level.setBlock(home, Blocks.CHEST.defaultBlockState(), 2);
      check(
          StaffChapter.ensure(level, site) == null && level.getBlockEntity(home) != null,
          "player chest never overwritten");
      level.setBlock(home, Blocks.AIR.defaultBlockState(), 2);
      var floor = level.getBlockState(home.below());
      npc = StaffChapter.ensure(level, site);
      check(npc != null, "separate real briefing officer placed in old facility");
      if (npc == null) return;
      uuid = npc.getUUID();
      check(
          StaffChapter.officer(npc)
              && !ReceptionChapter.receptionist(npc)
              && !npc.isPushable()
              && npc.isNoAi()
              && npc.isInvulnerable()
              && npc.isPersistenceRequired(),
          "dedicated persistent staff role, no wandering or pushing");
      check(
          level.getBlockState(home.below()).equals(floor),
          "staff placement leaves existing blocks intact");
      for (int i = 0; i < 6; i++)
        check(StaffChapter.ensure(level, site) == npc, "repeated placement reuses saved NPC");
      check(
          SiteProgress.load(progress.save()).briefingOfficer.equals(uuid),
          "staff UUID survives site save/load");
      check(
          SiteProgress.load(new CompoundTag()).briefingOfficer == null,
          "old site saves remain loadable");
      player.setGameMode(GameType.SURVIVAL);
      var intro = ready(player, site);
      player.setPos(home.getX() + .5, home.getY(), home.getZ() + 2.5);
      player.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 7));
      var inventory = player.getMainHandItem().copy();
      intro.getCompound(ReceptionChapter.SAVE_KEY).putInt("Step", ReceptionChapter.RETURN);
      npc.mobInteract(player, InteractionHand.MAIN_HAND);
      check(!intro.contains(StaffChapter.SAVE_KEY), "unfinished reception blocks Chapter 3");
      intro.getCompound(ReceptionChapter.SAVE_KEY).putInt("Step", ReceptionChapter.COMPLETE);
      StaffChapter.tickPlayer(player);
      check(!intro.contains(StaffChapter.SAVE_KEY), "walking nearby does not auto-start dialogue");
      npc.mobInteract(player, InteractionHand.OFF_HAND);
      check(!intro.contains(StaffChapter.SAVE_KEY), "offhand does not start interview");
      npc.mobInteract(player, InteractionHand.MAIN_HAND);
      check(
          StaffChapter.step(intro) == StaffChapter.REPORT,
          "main-hand staff interaction starts interview");
      npc.mobInteract(player, InteractionHand.MAIN_HAND);
      check(
          StaffChapter.step(intro) == StaffChapter.REPORT,
          "rapid clicks cannot skip witness report");
      check(!StaffChapter.cleared(player, intro), "interview greeting alone grants no clearance");
      player.setPos(front.getX() + .5, front.getY(), front.getZ() + 2.5);
      door(player, front);
      check(
          !level.getBlockState(front).getValue(DoorBlock.OPEN),
          "closed north research door denies pre-report entry");
      player.setPos(front.getX() + .5, front.getY(), front.getZ() - 1.5);
      door(player, front);
      check(
          level.getBlockState(front).getValue(DoorBlock.OPEN),
          "north-side egress remains available without clearance");
      door(player, front);
      player.setPos(home.getX() + .5, home.getY(), home.getZ() + 2.5);
      quiet(player);
      npc.mobInteract(player, InteractionHand.MAIN_HAND);
      check(
          StaffChapter.step(intro) == StaffChapter.ASSIGNED && StaffChapter.cleared(player, intro),
          "witness report assigns orientation and grants limited clearance");
      check(
          player.keys.stream().filter(k -> k.equals("message.timelocked.staff.witness")).count()
              == 1,
          "witness statement emitted once");
      check(
          !progress.powered && !progress.cleared && !progress.fighting,
          "staff assignment does not power facility or trigger encounters");
      check(!StaffChapter.terminal(player, terminal), "distant terminal cannot advance");
      quiet(player);
      player.setPos(front.getX() + .5, front.getY(), front.getZ() + 2.5);
      door(player, front);
      check(
          level.getBlockState(front).getValue(DoorBlock.OPEN)
              && level.getBlockState(front.east()).getValue(DoorBlock.OPEN),
          "authorized real door interaction opens paired leaves");
      var north = site.offset(79, 2, 185);
      door(player, north);
      var route = reachable(player, site, site.offset(64, 2, 331));
      check(
          route.contains(home.south(2)),
          "player-width authored route from reception to briefing officer");
      check(
          route.contains(terminal.south()),
          "player-width authored route from lobby through research doors to terminal");
      player.setPos(terminal.getX() + .5, terminal.getY(), terminal.getZ() + 1.5);
      var wrong = terminal.east(2);
      level.setBlock(wrong, FacilityDetails.WORKSTATION.get().defaultBlockState(), 2);
      check(!StaffChapter.terminal(player, wrong), "unrelated or moved workstation is rejected");
      var position = player.position();
      player.setShiftKeyDown(true);
      check(!StaffChapter.terminal(player, terminal), "sneak-use preserves normal interaction");
      player.setShiftKeyDown(false);
      var event =
          new net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock(
              player,
              InteractionHand.MAIN_HAND,
              terminal,
              new BlockHitResult(Vec3.atCenterOf(terminal), Direction.SOUTH, terminal, false));
      StaffChapter.terminalClick(event);
      check(
          event.isCanceled() && StaffChapter.step(intro) == StaffChapter.RETURN,
          "real workstation event records read-only orientation");
      check(
          player.position().equals(position)
              && player.gameMode.getGameModeForPlayer() == GameType.SURVIVAL
              && ItemStack.matches(inventory, player.getMainHandItem()),
          "terminal does not teleport, change mode or consume inventory");
      check(!StaffChapter.terminal(player, terminal), "orientation cannot award twice");
      quiet(player);
      player.setPos(home.getX() + .5, home.getY(), home.getZ() + 2.5);
      npc.mobInteract(player, InteractionHand.MAIN_HAND);
      check(
          StaffChapter.step(intro) == StaffChapter.COMPLETE && StaffChapter.cleared(player, intro),
          "return conversation completes Chapter 3 and retains limited clearance");
      quiet(player);
      npc.mobInteract(player, InteractionHand.MAIN_HAND);
      check(
          player.keys.stream().filter(k -> k.equals("message.timelocked.staff.complete")).count()
              == 1,
          "completion does not replay");
      check(
          CoastChapter.step(CoastChapter.chapter(intro)) == CoastChapter.COMPLETE
              && ReceptionChapter.step(intro) == ReceptionChapter.COMPLETE,
          "earlier chapter progress preserved");
      var saved =
          IntroState.load(
                  IntroState.get(server).save(new CompoundTag(), level.registryAccess()),
                  level.registryAccess())
              .players
              .get(player.getUUID());
      check(
          StaffChapter.step(saved) == StaffChapter.COMPLETE
              && saved.getCompound(StaffChapter.SAVE_KEY).getBoolean(StaffChapter.CLEARANCE),
          "quest and clearance survive full save/load");
      check(
          CoastChapter.objective(player, intro).getContents()
                  instanceof net.minecraft.network.chat.contents.TranslatableContents t
              && t.getKey().equals("message.timelocked.research.objective.1"),
          "existing story command hands off to Chapter 4 objective");
      second.setGameMode(GameType.SURVIVAL);
      var secondIntro = ready(second, site);
      second.setPos(player.position());
      npc.mobInteract(second, InteractionHand.MAIN_HAND);
      check(
          StaffChapter.step(secondIntro) == StaffChapter.REPORT
              && StaffChapter.step(intro) == StaffChapter.COMPLETE
              && !StaffChapter.cleared(second, secondIntro),
          "two players share staff but not quest progress or clearance");
      var own = secondIntro.getLong("Site");
      secondIntro.putLong("Site", site.east(1000).asLong());
      quiet(second);
      npc.mobInteract(second, InteractionHand.MAIN_HAND);
      check(
          StaffChapter.step(secondIntro) == StaffChapter.REPORT,
          "other facility staff cannot advance quest");
      secondIntro.putLong("Site", own);
      secondIntro.getCompound(StaffChapter.SAVE_KEY).putInt("Cooldown", 0);
      rule.set(false, server);
      npc.mobInteract(second, InteractionHand.MAIN_HAND);
      check(
          StaffChapter.step(secondIntro) == StaffChapter.REPORT,
          "non-Story interaction cannot advance");
      rule.set(true, server);
      second.setGameMode(GameType.SPECTATOR);
      npc.mobInteract(second, InteractionHand.MAIN_HAND);
      check(
          StaffChapter.step(secondIntro) == StaffChapter.REPORT
              && !StaffChapter.terminal(second, terminal),
          "spectator cannot advance");
      var bad = intro.copy();
      bad.getCompound(StaffChapter.SAVE_KEY).putInt("Step", 999);
      bad.getCompound(StaffChapter.SAVE_KEY).remove(StaffChapter.CLEARANCE);
      check(
          StaffChapter.step(bad) == StaffChapter.COMPLETE && !StaffChapter.cleared(player, bad),
          "corrupt stage does not replay or invent clearance");
      CompoundTag entity = new CompoundTag();
      npc.save(entity);
      npc.discard();
      check(
          StaffChapter.ensure(level, site) == null && progress.briefingOfficer.equals(uuid),
          "missing or unloaded saved officer does not spawn a duplicate");
      var loaded = EntityType.loadEntityRecursive(entity, level, e -> e);
      check(
          loaded instanceof SarcScientist && level.addFreshEntity(loaded), "staff entity reloads");
      npc = (SarcScientist) loaded;
      check(
          StaffChapter.ensure(level, site) == npc && npc.getUUID().equals(uuid),
          "loaded officer retains saved role and UUID");
      check(
          server.getCommands().getDispatcher().getRoot().getChild("sarcbriefing") != null,
          "safe existing-world briefing status command registered");
    } finally {
      if (npc != null) npc.discard();
      if (uuid != null && level.getEntity(uuid) != null) level.getEntity(uuid).discard();
      IntroState.get(server).players.remove(player.getUUID());
      IntroState.get(server).players.remove(second.getUUID());
      rule.set(previous, server);
      if (oldSite == null) data.sites.remove(site.asLong());
      else data.sites.put(site.asLong(), oldSite);
      if (oldProgress == null) data.progress.remove(site.asLong());
      else data.progress.put(site.asLong(), oldProgress);
    }
  }

  private StaffChapterAudit() {}
}
