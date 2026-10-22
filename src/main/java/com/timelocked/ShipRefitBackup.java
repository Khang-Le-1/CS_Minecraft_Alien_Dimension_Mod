package com.timelocked;

import java.nio.file.Files;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelResource;

/**
 * Abort the opt-in refit if its local block/NBT snapshot cannot be written. Not a full-world
 * backup.
 */
public final class ShipRefitBackup {
  public static void save(ServerLevel l, BlockPos o) {
    try {
      var dir = l.getServer().getWorldPath(LevelResource.ROOT).resolve("timelocked-backups");
      Files.createDirectories(dir);
      var file =
          dir.resolve(
              "timeskipper-pre-0.8.2-"
                  + System.currentTimeMillis()
                  + "-"
                  + System.nanoTime()
                  + ".nbt");
      CompoundTag root = new CompoundTag();
      root.putString("Dimension", l.dimension().location().toString());
      root.putLong("Origin", o.asLong());
      ListTag list = new ListTag();
      for (BlockPos p : BlockPos.betweenClosed(o.offset(-10, -10, -30), o.offset(10, 10, 27))) {
        CompoundTag n = new CompoundTag();
        n.putLong("Pos", p.asLong());
        n.put("State", NbtUtils.writeBlockState(l.getBlockState(p)));
        if (l.getBlockEntity(p) != null)
          n.put("BlockEntity", l.getBlockEntity(p).saveWithFullMetadata(l.registryAccess()));
        list.add(n);
      }
      root.put("Blocks", list);
      root.put(
          "ShipData", Timeskipper.data(l.getServer()).save(new CompoundTag(), l.registryAccess()));
      NbtIo.writeCompressed(root, file);
    } catch (Exception e) {
      throw new IllegalStateException(
          "Ship snapshot failed; refit was not started. Back up the world and check disk permissions.",
          e);
    }
  }

  private ShipRefitBackup() {}
}
