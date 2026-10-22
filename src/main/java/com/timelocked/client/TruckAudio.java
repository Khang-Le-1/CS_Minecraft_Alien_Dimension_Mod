package com.timelocked.client;

import com.timelocked.SarcGroundVehicle;
import com.timelocked.TruckSounds;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** One positional idle/drive pair per nearby running truck; no server tick sound spam. */
@EventBusSubscriber(modid = "timelocked", value = Dist.CLIENT)
public final class TruckAudio {
  private static final Map<UUID, Pair> ACTIVE = new HashMap<>();
  private static ClientLevel boundLevel;

  @SubscribeEvent
  public static void tick(ClientTickEvent.Post event) {
    Minecraft mc = Minecraft.getInstance();
    if (mc.level != boundLevel || mc.level == null || mc.player == null) {
      reset();
      boundLevel = mc.level;
    }
    if (mc.level == null || mc.player == null || mc.isPaused()) return;
    var seen = new HashSet<UUID>();
    for (var truck :
        mc.level.getEntitiesOfClass(
            SarcGroundVehicle.class,
            mc.player.getBoundingBox().inflate(48),
            SarcGroundVehicle::engineRunning)) {
      seen.add(truck.getUUID());
      Pair pair = ACTIVE.get(truck.getUUID());
      if (pair == null || pair.idle.truck != truck) {
        if (pair != null) stop(pair);
        pair = new Pair(new Engine(truck, false), new Engine(truck, true));
        ACTIVE.put(truck.getUUID(), pair);
        mc.getSoundManager().play(pair.idle);
        mc.getSoundManager().play(pair.drive);
      } else if (mc.level.getGameTime() % 20 == 0) {
        // Recover after resource reloads or a previously muted sound category.
        if (!mc.getSoundManager().isActive(pair.idle)
            || !mc.getSoundManager().isActive(pair.drive)) {
          stop(pair);
          ACTIVE.remove(truck.getUUID());
        }
      }
    }
    var iterator = ACTIVE.entrySet().iterator();
    while (iterator.hasNext()) {
      var entry = iterator.next();
      if (!seen.contains(entry.getKey())) {
        stop(entry.getValue());
        iterator.remove();
      }
    }
  }

  @SubscribeEvent
  public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
    reset();
  }

  private static void stop(Pair pair) {
    var manager = Minecraft.getInstance().getSoundManager();
    manager.stop(pair.idle);
    manager.stop(pair.drive);
  }

  private static void reset() {
    ACTIVE.values().forEach(TruckAudio::stop);
    ACTIVE.clear();
    boundLevel = null;
  }

  private record Pair(Engine idle, Engine drive) {}

  private static final class Engine extends AbstractTickableSoundInstance {
    final SarcGroundVehicle truck;
    private final boolean driving;
    private float mix;

    Engine(SarcGroundVehicle truck, boolean driving) {
      super(
          driving ? TruckSounds.DRIVE.get() : TruckSounds.IDLE.get(),
          SoundSource.NEUTRAL,
          SoundInstance.createUnseededRandom());
      this.truck = truck;
      this.driving = driving;
      this.looping = true;
      this.delay = 0;
      this.volume = 0;
      this.pitch = 1;
      this.x = truck.getX();
      this.y = truck.getY() + .8;
      this.z = truck.getZ();
    }

    @Override
    public boolean canStartSilent() {
      return true;
    }

    @Override
    public void tick() {
      if (!truck.engineRunning()
          || truck.isRemoved()
          || Minecraft.getInstance().level != truck.level()) {
        this.stop();
        return;
      }
      float target = TruckSounds.driveMix(truck.driveSpeed());
      this.mix = Mth.approach(this.mix, target, .06F);
      this.volume = driving ? .75F * this.mix : .55F * (1 - this.mix);
      this.x = truck.getX();
      this.y = truck.getY() + .8;
      this.z = truck.getZ();
    }
  }

  private TruckAudio() {}
}
