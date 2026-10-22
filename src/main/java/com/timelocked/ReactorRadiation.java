package com.timelocked;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Simplified gameplay exposure, not a real-world radiation simulation. Closed shield doors and
 * bulkheads stop it.
 */
public final class ReactorRadiation {
  public static void expose(ServerLevel l, PlantCoreBlock.Core core) {
    if (core.spent || (!core.getBlockState().getValue(PlantCoreBlock.LIT) && !core.critical()))
      return;
    var shape = ReactorFrame.shape(l, core.getBlockPos(), core.getBlockState());
    Vec3 origin =
        new Vec3(
            core.getBlockPos().getX() + .5,
            shape.bottom(core.getBlockPos()) + Math.min(6, shape.h() / 2.0),
            core.getBlockPos().getZ() + .5);
    for (ServerPlayer p : l.players()) {
      if (p.isCreative()
          || p.isSpectator()
          || p.getEyePosition().distanceToSqr(origin) > 22 * 22
          || shielded(l, core.getBlockPos(), shape, origin, p.getEyePosition())) continue;
      switch (core.kind()) {
        case 0, 1 -> {
          p.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 0));
          p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0));
        }
        case 2 -> p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1));
        default -> p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 1));
      }
      if (core.critical()) p.hurt(l.damageSources().magic(), 1F);
      if (l.getGameTime() % 100 == 0)
        p.displayClientMessage(
            Component.literal("[SARC] CORE EXPOSURE: leave the live vault or restore shielding."),
            true);
    }
  }

  public static int shielding(BlockState s) {
    if (s.getBlock() instanceof ReactorAirlockBlock) return s.getValue(DoorBlock.OPEN) ? 0 : 2;
    String id = BuiltInRegistries.BLOCK.getKey(s.getBlock()).getPath();
    if (id.equals("sarc_reinforced_shell")
        || id.equals("lead_block")
        || id.equals("tungsten_steel_block")) return 2;
    if (id.endsWith("stained_glass") || id.equals("space_viewport") || s.canOcclude()) return 1;
    return 0;
  }

  public static boolean shielded(
      ServerLevel l, BlockPos core, ReactorFrame.Shape shape, Vec3 a, Vec3 b) {
    Vec3 delta = b.subtract(a);
    int steps = Math.max(1, (int) Math.ceil(delta.length() * 4));
    BlockPos last = null;
    int shield = 0;
    for (int i = 1; i <= steps; i++) {
      BlockPos q = BlockPos.containing(a.add(delta.scale((double) i / steps)));
      if (q.equals(last) || shape.contains(core, q)) continue;
      last = q;
      if (!l.hasChunkAt(q)) return true;
      shield += shielding(l.getBlockState(q));
      if (shield >= 2) return true;
    }
    return false;
  }

  private ReactorRadiation() {}
}
