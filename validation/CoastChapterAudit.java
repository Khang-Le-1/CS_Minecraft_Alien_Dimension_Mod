package com.timelocked;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/** Test-only coverage for the saved free-roaming chapter. Not a second production entry point. */
final class CoastChapterAudit {
  static final class Reader extends IntroAudit.IntroPlayer {
    final List<String> keys = new ArrayList<>();

    Reader(ServerLevel level, String name) {
      super(level, UUID.randomUUID(), name);
    }

    @Override
    public void sendSystemMessage(Component message) {
      keys.add(
          message.getContents() instanceof TranslatableContents t
              ? t.getKey()
              : message.getString());
    }
  }

  static void check(boolean value, String message) {
    IntroAudit.check(value, "coastal chapter: " + message);
  }

  static CompoundTag intro(Reader player, BlockPos site) {
    CompoundTag intro = new CompoundTag();
    intro.putUUID("Player", player.getUUID());
    intro.putInt("Stage", IntroSequence.DONE);
    intro.putBoolean("AtBeach", true);
    intro.putLong("Site", site.asLong());
    IntroState.get(player.server).players.put(player.getUUID(), intro);
    return intro;
  }

  static void stand(Reader player, BlockPos foot) {
    var level = player.serverLevel();
    level.getChunkAt(foot);
    level.setBlock(foot.below(), Blocks.STONE.defaultBlockState(), 2);
    level.setBlock(foot, Blocks.AIR.defaultBlockState(), 2);
    level.setBlock(foot.above(), Blocks.AIR.defaultBlockState(), 2);
    player.setPos(foot.getX() + .5, foot.getY(), foot.getZ() + .5);
    player.setOnGround(true);
  }

  static void waitLine(Reader player) {
    for (int tick = 0;
        tick < CoastChapter.COOLDOWN_BUDGET_TICKS;
        tick += CoastChapter.TICK_INTERVAL) CoastChapter.tickPlayer(player);
  }

  static void run(MinecraftServer server) throws Exception {
    var level = server.overworld();
    var rules = server.getGameRules().getRule(StoryMode.RULE);
    boolean previousRule = rules.get();
    rules.set(true, server);
    BlockPos site = new BlockPos(12000, 100, 12000);
    BlockPos wake = IntroCoast.wake(site), junction = IntroCoast.roadJunction(site);
    BlockPos surface = IntroCoast.surfaceOrigin(site);
    Reader player = new Reader(level, "CoastReader");
    player.setGameMode(GameType.SURVIVAL);
    player
        .getInventory()
        .setItem(
            0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND, 7));
    CompoundTag intro = intro(player, site);
    stand(player, wake);
    var startPosition = player.position();
    CoastChapter.begin(player, intro);
    check(intro.contains(CoastChapter.SAVE_KEY), "supported Story landing creates progress");
    CompoundTag chapter = CoastChapter.chapter(intro);
    check(player.keys.isEmpty(), "begin is silent after the intro's last line");
    waitLine(player);
    check(chapter.getInt("Step") == CoastChapter.SHORE, "shore beat follows cooldown");
    check(
        player.keys.size() == 1 && player.keys.get(0).endsWith("beat.1"), "one initial chat line");
    for (int i = 0; i < 25; i++) CoastChapter.tickPlayer(player);
    check(player.keys.size() == 1, "standing at a completed landmark never repeats it");
    check(
        player.position().equals(startPosition)
            && player.gameMode.getGameModeForPlayer() == GameType.SURVIVAL
            && player.getInventory().getItem(0).getCount() == 7
            && !player.isNoGravity(),
        "no teleport, mode, inventory or gravity change");

    // Inspect the modeled upper panel and ensure clicks cannot spam or claim a moved/other-site
    // sign.
    BlockPos sign = wake.offset(-3, 0, -5);
    level.setBlock(sign, CoastSigns.RESTRICTION.get().defaultBlockState(), 2);
    level.setBlock(
        sign.above(),
        CoastSigns.RESTRICTION
            .get()
            .defaultBlockState()
            .setValue(CoastSigns.Panel.HALF, DoubleBlockHalf.UPPER),
        2);
    stand(player, sign.offset(1, 0, 0));
    check(CoastChapter.inspect(player, sign.above()), "upper restriction panel can be read");
    check(chapter.getBoolean("ReadRestriction"), "optional clue saved");
    var partialSave = IntroState.get(server).save(new CompoundTag(), server.registryAccess());
    var partial =
        IntroState.load(partialSave, server.registryAccess()).players.get(player.getUUID());
    check(
        CoastChapter.chapter(partial).getInt("Cooldown") == CoastChapter.COOLDOWN_BUDGET_TICKS
            && CoastChapter.chapter(partial).getBoolean("ReadRestriction"),
        "in-progress cooldown and clues survive save/load");
    int afterRead = player.keys.size();
    check(
        !CoastChapter.inspect(player, sign) && player.keys.size() == afterRead,
        "second click is silent");
    waitLine(player);
    check(!CoastChapter.inspect(player, sign), "already read panel never replays after cooldown");
    check(
        chapter.getInt("Step") == CoastChapter.SHORE,
        "reading the shore-side panel does not prematurely advance to boardwalk");
    BlockPos moved = sign.offset(2, 0, 0);
    level.setBlock(moved, CoastSigns.DIRECTION.get().defaultBlockState(), 2);
    check(
        !CoastChapter.inspect(player, moved), "player-placed/moved panel is not an authored clue");
    BlockPos direction = wake.offset(-6, 0, 3);
    stand(player, wake.offset(-2, 0, 0));
    level.setBlock(direction, CoastSigns.DIRECTION.get().defaultBlockState(), 2);
    player.setShiftKeyDown(true);
    check(!CoastChapter.inspect(player, direction), "sneaking preserves normal building behavior");
    player.setShiftKeyDown(false);
    var hit =
        new net.minecraft.world.phys.BlockHitResult(
            direction.getCenter(), net.minecraft.core.Direction.EAST, direction, false);
    var offhand =
        new net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock(
            player, net.minecraft.world.InteractionHand.OFF_HAND, direction, hit);
    CoastChapter.interact(offhand);
    check(
        !offhand.isCanceled() && !chapter.getBoolean("ReadDirection"),
        "offhand cannot duplicate a clue");
    var mainhand =
        new net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock(
            player, net.minecraft.world.InteractionHand.MAIN_HAND, direction, hit);
    CoastChapter.interact(mainhand);
    check(
        mainhand.isCanceled() && chapter.getBoolean("ReadDirection"),
        "main-hand event reads one clue");
    check(chapter.getBoolean("ReadDirection"), "both optional readings tracked separately");

    // Ordinary exploration, including missing signs and direct route shortcuts, cannot soft-lock.
    level.setBlock(sign, Blocks.AIR.defaultBlockState(), 3);
    level.setBlock(direction, Blocks.AIR.defaultBlockState(), 3);
    stand(player, junction.offset(263, 0, -20));
    waitLine(player);
    check(chapter.getInt("Step") == CoastChapter.PARKING, "parking milestone");
    check(player.keys.get(player.keys.size() - 1).endsWith("beat.2"), "parking story line");
    stand(player, junction.offset(180, 0, 0));
    CoastChapter.tickPlayer(player);
    check(chapter.getInt("Step") == CoastChapter.PARKING, "no second chat before seven-second gap");
    waitLine(player);
    check(chapter.getInt("Step") == CoastChapter.BOARDWALK, "boardwalk milestone");
    stand(player, junction.offset(0, 0, -20));
    waitLine(player);
    check(chapter.getInt("Step") == CoastChapter.ROAD, "road junction milestone");
    stand(player, new BlockPos(junction.getX(), 80, surface.getZ() + 688));
    waitLine(player);
    check(
        chapter.getInt("Step") == CoastChapter.WAYFINDING,
        "graded road ignores absolute coastal Y");
    stand(player, surface.offset(64, 29, 225));
    waitLine(player);
    check(chapter.getInt("Step") == CoastChapter.ENTRANCE, "surface entrance milestone");

    // Hub narration must yield to all seven existing welcome lines, including saved late joins.
    stand(player, site.offset(79, 2, 273));
    var progress = new SiteProgress();
    FacilityState.get(server).progress.put(site.asLong(), progress);
    int beforeHub = player.keys.size();
    waitLine(player);
    check(
        chapter.getInt("Step") == CoastChapter.HUB && player.keys.size() == beforeHub,
        "hub is silent before logo powers on");
    progress.arrivalLogoAnnounced = true;
    progress.arrivalWelcomeNext = 6;
    waitLine(player);
    check(player.keys.size() == beforeHub, "no narration over PA line seven");
    progress.arrivalWelcomeNext = 7;
    for (int i = 0; i < 7; i++) CoastChapter.tickPlayer(player);
    check(player.keys.size() == beforeHub, "seven-second quiet gap after final PA line");
    CoastChapter.tickPlayer(player);
    check(
        chapter.getInt("Step") == CoastChapter.COMPLETE && player.keys.size() == beforeHub + 1,
        "one chapter completion line");
    check(
        player.keys.get(player.keys.size() - 1).endsWith("complete"),
        "completion line uses translated key");
    waitLine(player);
    check(player.keys.size() == beforeHub + 1, "completion never repeats");
    check(
        progress.arrivalWelcomeNext == 7 && !progress.arrivalStarted,
        "chapter never restarts facility welcome or lighting");
    var saved = IntroState.get(server).save(new CompoundTag(), server.registryAccess());
    var restored = IntroState.load(saved, server.registryAccess()).players.get(player.getUUID());
    check(
        CoastChapter.step(CoastChapter.chapter(restored)) == CoastChapter.COMPLETE
            && CoastChapter.chapter(restored).getBoolean("ReadRestriction"),
        "stage and clues survive native save/load");
    CoastChapter.begin(player, intro);
    check(
        CoastChapter.step(CoastChapter.chapter(intro)) == CoastChapter.COMPLETE,
        "begin/rejoin cannot reset completion");

    Reader second = new Reader(level, "SecondCoastReader");
    second.setGameMode(GameType.SURVIVAL);
    var secondIntro = intro(second, site);
    stand(second, wake);
    CoastChapter.begin(second, secondIntro);
    check(
        CoastChapter.step(CoastChapter.chapter(secondIntro)) == 0 && second.keys.isEmpty(),
        "multiplayer progress is separate");
    int cooldown = CoastChapter.chapter(secondIntro).getInt("Cooldown");
    rules.set(false, server);
    waitLine(second);
    check(
        CoastChapter.chapter(secondIntro).getInt("Cooldown") == cooldown,
        "non-Story rule pauses without changing progress");
    rules.set(true, server);
    second.setGameMode(GameType.SPECTATOR);
    waitLine(second);
    check(second.keys.isEmpty(), "spectators do not progress");
    second.setGameMode(GameType.SURVIVAL);
    second.setOnGround(false);
    waitLine(second);
    check(second.keys.isEmpty(), "falling/flying does not cross landmarks");
    stand(second, surface.offset(64, 29, 225));
    waitLine(second);
    check(
        CoastChapter.step(CoastChapter.chapter(secondIntro)) == CoastChapter.ENTRANCE
            && second.keys.size() == 1
            && second.keys.get(0).endsWith("beat.6"),
        "direct route skips backlog instead of dumping six messages");
    int skip =
        server
            .getCommands()
            .getDispatcher()
            .execute("timelockedstory skip", second.createCommandSourceStack());
    check(
        skip == 1 && CoastChapter.step(CoastChapter.chapter(secondIntro)) == CoastChapter.COMPLETE,
        "self-only chapter skip is available");
    check(
        CoastChapter.step(CoastChapter.chapter(intro)) == CoastChapter.COMPLETE,
        "another player's skip does not alter first player");

    // A .18 completed intro joins lazily on its own route, never by rebuilding an old coast.
    Reader legacy = new Reader(level, "LegacyCoastReader");
    legacy.setGameMode(GameType.SURVIVAL);
    var legacyIntro = intro(legacy, site);
    stand(legacy, wake.offset(1000, 0, 0));
    CoastChapter.tickPlayer(legacy);
    check(
        !legacyIntro.contains(CoastChapter.SAVE_KEY),
        "off-route legacy save does not start chapter");
    stand(legacy, wake);
    CoastChapter.tickPlayer(legacy);
    check(
        legacyIntro.contains(CoastChapter.SAVE_KEY),
        "on-route legacy save gains chapter without teleport");
    var legacyTag = new CompoundTag();
    legacyTag.putInt("ArrivalStep", 5);
    var legacyArrival = SiteProgress.load(legacyTag);
    check(CoastChapter.welcomeFinished(legacyArrival), "completed pre-timed PA is not restarted");
    var corrupt = new CompoundTag();
    corrupt.putInt("Step", 999);
    check(
        CoastChapter.step(corrupt) == CoastChapter.COMPLETE,
        "corrupt stage cannot trigger backlog");
    check(
        server.getCommands().getDispatcher().getRoot().getChild("timelockedstory") != null,
        "objective command registered");
    // Regression: the first fractional sample must not make a seven-second line arrive early.
    Reader boundary = new Reader(level, "TickBoundaryReader");
    boundary.setGameMode(GameType.SURVIVAL);
    var boundaryIntro = intro(boundary, site);
    stand(boundary, wake);
    CoastChapter.begin(boundary, boundaryIntro);
    for (int sample = 0; sample < 7; sample++) CoastChapter.tickPlayer(boundary);
    check(boundary.keys.isEmpty(), "seven partial samples cannot shorten the minimum gap");
    CoastChapter.tickPlayer(boundary);
    check(boundary.keys.size() == 1, "next full sample releases one line");
    var otherDimension =
        new Reader(server.getLevel(IntroSequence.HOME_DIM), "OtherDimensionReader");
    check(
        !CoastChapter.eligible(otherDimension, intro(otherDimension, site)),
        "other dimensions cannot progress");
    CompoundTag reception = new CompoundTag();
    reception.putInt("Step", ReceptionChapter.RETURN);
    IntroSequence.state(player).put(ReceptionChapter.SAVE_KEY, reception);
    var staff = new CompoundTag();
    staff.putInt("Step", StaffChapter.ASSIGNED);
    staff.putBoolean(StaffChapter.CLEARANCE, true);
    IntroSequence.state(player).put(StaffChapter.SAVE_KEY, staff);
    check(IntroSequence.begin(player), "intro replay can still begin");
    check(
        StaffChapter.step(IntroSequence.state(player)) == StaffChapter.ASSIGNED
            && IntroSequence.state(player)
                .getCompound(StaffChapter.SAVE_KEY)
                .getBoolean(StaffChapter.CLEARANCE),
        "intro replay retains Chapter 3 assignment and limited clearance");
    check(
        ReceptionChapter.step(IntroSequence.state(player)) == ReceptionChapter.RETURN,
        "intro replay also retains in-progress Chapter 2");
    check(
        CoastChapter.step(CoastChapter.chapter(IntroSequence.state(player)))
            == CoastChapter.COMPLETE,
        "intro replay retains saved chapter completion");
    IntroSequence.recover(player, IntroSequence.state(player));
    IntroState.get(server).players.remove(boundary.getUUID());
    IntroState.get(server).players.remove(otherDimension.getUUID());
    rules.set(previousRule, server);
    IntroState.get(server).players.remove(player.getUUID());
    IntroState.get(server).players.remove(second.getUUID());
    IntroState.get(server).players.remove(legacy.getUUID());
    FacilityState.get(server).progress.remove(site.asLong());
  }

  private CoastChapterAudit() {}
}
