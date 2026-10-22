package com.timelocked;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Custom sound type changes push-bar doors only, including redstone and scripted changes. */
public final class PushBarDoorSounds {
  public static final SoundEvent OPEN = sound("pushbar_open");
  public static final SoundEvent CLOSE = sound("pushbar_close");
  public static final DeferredRegister<SoundEvent> SOUNDS =
      DeferredRegister.create(Registries.SOUND_EVENT, "timelocked");
  public static final BlockSetType TYPE;

  static {
    SOUNDS.register("pushbar_open", () -> OPEN);
    SOUNDS.register("pushbar_close", () -> CLOSE);
    BlockSetType base = BlockSetType.COPPER;
    TYPE =
        BlockSetType.register(
            new BlockSetType(
                "timelocked:pushbar",
                base.canOpenByHand(),
                base.canOpenByWindCharge(),
                base.canButtonBeActivatedByArrows(),
                base.pressurePlateSensitivity(),
                base.soundType(),
                CLOSE,
                OPEN,
                base.trapdoorClose(),
                base.trapdoorOpen(),
                base.pressurePlateClickOff(),
                base.pressurePlateClickOn(),
                base.buttonClickOff(),
                base.buttonClickOn()));
  }

  private static SoundEvent sound(String name) {
    return SoundEvent.createVariableRangeEvent(
        ResourceLocation.fromNamespaceAndPath("timelocked", name));
  }

  private PushBarDoorSounds() {}
}
