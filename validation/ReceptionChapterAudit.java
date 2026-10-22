package com.timelocked;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/** Actual NPC placement, right-click assignment and saved-game handoff regression tests. */
final class ReceptionChapterAudit {
  static void check(boolean value, String label) {
    IntroAudit.check(value, "reception chapter: " + label);
  }

  static void quiet(CoastChapterAudit.Reader player) {
    for (int i = 0; i < 4; i++) ReceptionChapter.tickPlayer(player);
  }

  static void run(MinecraftServer server) throws Exception {
    var level = server.overworld();
    var story = server.getGameRules().getRule(StoryMode.RULE);
    boolean oldRule = story.get();
    var facilities = FacilityState.get(server);
    BlockPos site = new BlockPos(32000, 80, 32000);
    BlockPos home = site.offset(ReceptionChapter.DESK),
        terminal = site.offset(ReceptionChapter.TERMINAL);
    var oldSite = facilities.sites.put(site.asLong(), 43);
    var progress = new SiteProgress();
    progress.geometryVersion = BlueprintRooms.VERSION;
    progress.layoutVersion = 2;
    var oldProgress = facilities.progress.put(site.asLong(), progress);
    var player = new CoastChapterAudit.Reader(level, "ReceptionReader");
    player.setGameMode(GameType.SURVIVAL);
    SarcScientist npc = null;
    UUID uuid = null;
    try {
      var fresh = GlassStairwell.templateStates(level, "drawing");
      for (int x = 64; x <= 71; x++)
        for (int z = 324; z <= 332; z++)
          for (int y = 1; y <= 5; y++) {
            var local = new BlockPos(x, y, z);
            var state = fresh.get(local);
            check(state != null, "authored reception cell exists " + local);
            level.getChunkAt(site.offset(local));
            level.setBlock(site.offset(local), state, 2);
          }
      check(
          level.getBlockState(terminal).is(FacilityDetails.WORKSTATION.get()),
          "quest uses actual shipped workstation");
      story.set(false, server);
      check(
          ReceptionChapter.ensure(level, site) == null,
          "no story NPC generated in non-Story worlds");
      story.set(true, server);
      level.setBlock(home, Blocks.CHEST.defaultBlockState(), 2);
      check(
          ReceptionChapter.ensure(level, site) == null && level.getBlockEntity(home) != null,
          "player container at NPC position is not overwritten");
      level.setBlock(home, Blocks.AIR.defaultBlockState(), 2);
      npc = ReceptionChapter.ensure(level, site);
      check(npc != null, "real separate receptionist created in existing authored reception");
      if (npc == null) return;
      uuid = npc.getUUID();
      check(
          ReceptionChapter.receptionist(npc)
              && npc.isNoAi()
              && npc.isInvulnerable()
              && !npc.isPushable()
              && npc.isPersistenceRequired(),
          "receptionist persists without wandering or normal damage");
      check(
          npc.getCustomName() != null
              && npc.getCustomName().getContents()
                  instanceof net.minecraft.network.chat.contents.TranslatableContents t
              && t.getKey().equals("entity.timelocked.receptionist"),
          "separate receptionist name, not Dr Everleigh");
      check(
          !FacilitySpawnProtection.blocked(level, npc, MobSpawnType.STRUCTURE),
          "spawn guard explicitly permits authored staff");
      for (int i = 0; i < 10; i++)
        check(ReceptionChapter.ensure(level, site) == npc, "repeated placement returns same NPC");
      check(
          level.getEntitiesOfClass(SarcScientist.class, new AABB(home).inflate(6)).size() == 1,
          "only one NPC at desk");
      check(
          SiteProgress.load(progress.save()).receptionist.equals(uuid),
          "NPC UUID survives site save/load");
      CompoundTag intro = CoastChapterAudit.intro(player, site);
      CompoundTag coast = new CompoundTag();
      coast.putInt("Step", CoastChapter.COMPLETE);
      intro.put(CoastChapter.SAVE_KEY, coast);
      CoastChapterAudit.stand(player, site.offset(64, 2, 331));
      npc.mobInteract(player, InteractionHand.MAIN_HAND);
      check(
          ReceptionChapter.step(intro) == ReceptionChapter.NOT_STARTED,
          "welcome cannot be interrupted by starting Chapter 2");
      progress.arrivalLogoAnnounced = true;
      progress.arrivalStep = SiteProgress.FINAL_ARRIVAL_STAGE;
      progress.arrivalWelcomeNext = SiteProgress.WELCOME_LINE_COUNT;
      ReceptionChapter.tickPlayer(player);
      check(
          !intro.contains(ReceptionChapter.SAVE_KEY),
          "walking into reception does not auto-start quest");
      npc.mobInteract(player, InteractionHand.OFF_HAND);
      check(
          ReceptionChapter.step(intro) == ReceptionChapter.NOT_STARTED,
          "offhand does not duplicate story interaction");
      npc.mobInteract(player, InteractionHand.MAIN_HAND);
      check(
          ReceptionChapter.step(intro) == ReceptionChapter.REGISTER,
          "real NPC main-hand interaction starts Chapter 2");
      check(
          player.keys.stream().filter(k -> k.equals("message.timelocked.reception.start")).count()
              == 1,
          "one start dialogue");
      npc.mobInteract(player, InteractionHand.MAIN_HAND);
      check(
          player.keys.stream().filter(k -> k.equals("message.timelocked.reception.start")).count()
              == 1,
          "rapid repeated clicks do not spam/restart");
      BlockPos wrongTerminal = site.offset(67, 3, 320);
      level.setBlock(wrongTerminal, FacilityDetails.WORKSTATION.get().defaultBlockState(), 2);
      player.setPos(site.getX() + 67.5, site.getY() + 2, site.getZ() + 322.5);
      check(
          !ReceptionChapter.terminal(player, wrongTerminal),
          "moved/wrong workstation cannot satisfy registration");
      player.setPos(site.getX() + 90.5, site.getY() + 2, site.getZ() + 329.5);
      check(!ReceptionChapter.terminal(player, terminal), "distant terminal use is rejected");
      long ownSite = intro.getLong("Site");
      intro.putLong("Site", site.east(1000).asLong());
      npc.mobInteract(player, InteractionHand.MAIN_HAND);
      check(
          ReceptionChapter.step(intro) == ReceptionChapter.REGISTER,
          "other facility's receptionist cannot advance quest");
      intro.putLong("Site", ownSite);
      quiet(player);
      player.setPos(site.getX() + 67.5, site.getY() + 2, site.getZ() + 329.5);
      check(
          ReceptionChapter.terminal(player, terminal),
          "authored desk terminal accepts registration");
      check(
          ReceptionChapter.step(intro) == ReceptionChapter.RETURN,
          "terminal advances to actual return assignment");
      check(
          !ReceptionChapter.terminal(player, terminal),
          "repeat terminal click cannot re-award progress");
      quiet(player);
      npc.mobInteract(player, InteractionHand.MAIN_HAND);
      check(
          ReceptionChapter.step(intro) == ReceptionChapter.COMPLETE,
          "return interaction completes playable first-contact chapter");
      quiet(player);
      npc.mobInteract(player, InteractionHand.MAIN_HAND);
      check(
          player.keys.stream()
                  .filter(k -> k.equals("message.timelocked.reception.complete"))
                  .count()
              == 1,
          "completion never repeats");
      check(
          CoastChapter.step(CoastChapter.chapter(intro)) == CoastChapter.COMPLETE,
          "Chapter 1 not reset");
      var persisted =
          IntroState.load(
              IntroState.get(server).save(new CompoundTag(), level.registryAccess()),
              level.registryAccess());
      check(
          ReceptionChapter.step(persisted.players.get(player.getUUID()))
              == ReceptionChapter.COMPLETE,
          "quest survives full intro save/load");
      check(
          CoastChapter.objective(player, intro).getContents()
                  instanceof net.minecraft.network.chat.contents.TranslatableContents t
              && t.getKey().equals("message.timelocked.staff.objective.0"),
          "completed reception hands story command to Chapter 3");
      var second = new CoastChapterAudit.Reader(level, "ReceptionSecond");
      try {
        second.setGameMode(GameType.SURVIVAL);
        var secondIntro = CoastChapterAudit.intro(second, site);
        secondIntro.put(CoastChapter.SAVE_KEY, coast.copy());
        second.setPos(player.position());
        npc.mobInteract(second, InteractionHand.MAIN_HAND);
        check(
            ReceptionChapter.step(secondIntro) == ReceptionChapter.REGISTER
                && ReceptionChapter.step(intro) == ReceptionChapter.COMPLETE,
            "multiplayer quest progress is independent");
        check(ReceptionChapter.ensure(level, site) == npc, "two players share one receptionist");
        story.set(false, server);
        npc.mobInteract(second, InteractionHand.MAIN_HAND);
        check(
            ReceptionChapter.step(secondIntro) == ReceptionChapter.REGISTER,
            "non-Story interaction cannot advance saved quest");
        story.set(true, server);
        second.setGameMode(GameType.SPECTATOR);
        check(!ReceptionChapter.terminal(second, terminal), "spectator cannot register");
      } finally {
        IntroState.get(server).players.remove(second.getUUID());
      }
      CompoundTag npcSave = new CompoundTag();
      npc.save(npcSave);
      npc.discard();
      check(
          ReceptionChapter.ensure(level, site) == null && progress.receptionist.equals(uuid),
          "missing/unloaded saved NPC does not cause duplicate spawn");
      var reloaded = EntityType.loadEntityRecursive(npcSave, level, e -> e);
      check(
          reloaded instanceof SarcScientist && level.addFreshEntity(reloaded),
          "saved receptionist reloads as actual entity");
      npc = (SarcScientist) reloaded;
      check(
          ReceptionChapter.ensure(level, site) == npc && npc.getUUID().equals(uuid),
          "reloaded receptionist retains identity and role");
      var command = server.getCommands().getDispatcher();
      check(
          command.getRoot().getChild("sarcreception") != null,
          "existing-world receptionist status/placement command registered");
    } finally {
      if (npc != null) npc.discard();
      if (uuid != null && level.getEntity(uuid) != null) level.getEntity(uuid).discard();
      IntroState.get(server).players.remove(player.getUUID());
      story.set(oldRule, server);
      if (oldSite == null) facilities.sites.remove(site.asLong());
      else facilities.sites.put(site.asLong(), oldSite);
      if (oldProgress == null) facilities.progress.remove(site.asLong());
      else facilities.progress.put(site.asLong(), oldProgress);
    }
  }

  private ReceptionChapterAudit() {}
}
