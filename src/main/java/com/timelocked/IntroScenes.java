package com.timelocked;

import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;

/** Small private home set; ordinary Minecraft blocks, not a GUI illustration. */
public final class IntroScenes {
  static BlockPos home(int slot) {
    return new BlockPos((slot % 4096) * 64, 80, (slot / 4096) * 64);
  }

  static void set(ServerLevel l, BlockPos o, int x, int y, int z, BlockState b) {
    l.setBlock(o.offset(x, y, z), b, 3);
  }

  static void house(ServerLevel l, BlockPos o) {
    for (int x = -2; x <= 24; x++)
      for (int z = -2; z <= 20; z++)
        for (int y = -1; y <= 6; y++) {
          BlockState b = Blocks.AIR.defaultBlockState();
          if (y == -1) b = Blocks.STONE_BRICKS.defaultBlockState();
          if (y == 0) b = Blocks.SPRUCE_PLANKS.defaultBlockState();
          if (y == 6) b = Blocks.DARK_OAK_PLANKS.defaultBlockState();
          if (y > 0 && y < 6 && (x == -2 || x == 24 || z == -2 || z == 20))
            b = Blocks.LIGHT_GRAY_TERRACOTTA.defaultBlockState();
          if (y >= 2 && y <= 4 && (x == -2 || x == 24) && z >= 3 && z <= 15)
            b = Blocks.GLASS_PANE.defaultBlockState();
          if (y > 0 && y < 6 && x == 14 && !(z >= 8 && z <= 9))
            b = Blocks.WHITE_TERRACOTTA.defaultBlockState();
          set(l, o, x, y, z, b);
        }
    // Front entrance, bedroom opening and warm recessed lights.
    for (int y = 1; y <= 3; y++)
      for (int x = 10; x <= 12; x++)
        set(l, o, x, y, -2, Blocks.DARK_OAK_PLANKS.defaultBlockState());
    set(
        l,
        o,
        11,
        1,
        -2,
        Blocks.DARK_OAK_DOOR
            .defaultBlockState()
            .setValue(DoorBlock.FACING, Direction.SOUTH)
            .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
    set(
        l,
        o,
        11,
        2,
        -2,
        Blocks.DARK_OAK_DOOR
            .defaultBlockState()
            .setValue(DoorBlock.FACING, Direction.SOUTH)
            .setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
    for (int x : new int[] {2, 10, 18, 22})
      for (int z : new int[] {3, 11, 17})
        set(l, o, x, 5, z, Blocks.OCHRE_FROGLIGHT.defaultBlockState());
    // Six-by-three cells are merged into ONE seamless screen by MonitorRenderer.
    for (int x = 3; x <= 8; x++) set(l, o, x, 1, 3, Blocks.DARK_OAK_PLANKS.defaultBlockState());
    television(l, o, false);
    // A real supported control pod, not a floating wall button.
    set(l, o, 9, 1, 2, Blocks.DARK_OAK_PLANKS.defaultBlockState());
    set(l, o, 9, 1, 3, Blocks.DARK_OAK_PLANKS.defaultBlockState());
    set(l, o, 9, 2, 2, Blocks.GRAY_CONCRETE.defaultBlockState());
    set(
        l,
        o,
        9,
        2,
        3,
        Blocks.STONE_BUTTON
            .defaultBlockState()
            .setValue(ButtonBlock.FACE, AttachFace.WALL)
            .setValue(ButtonBlock.FACING, Direction.SOUTH));
    for (int x = 6; x <= 10; x++) {
      set(l, o, x, 1, 10, Blocks.GRAY_WOOL.defaultBlockState());
      set(l, o, x, 1, 11, Blocks.GRAY_WOOL.defaultBlockState());
      set(l, o, x, 2, 11, Blocks.GRAY_WOOL.defaultBlockState());
    }
    for (int x = 8; x <= 9; x++)
      for (int z = 7; z <= 8; z++) set(l, o, x, 1, z, Blocks.DARK_OAK_SLAB.defaultBlockState());
    set(
        l,
        o,
        19,
        1,
        12,
        Blocks.RED_BED
            .defaultBlockState()
            .setValue(BedBlock.FACING, Direction.NORTH)
            .setValue(BedBlock.PART, BedPart.FOOT));
    set(
        l,
        o,
        19,
        1,
        11,
        Blocks.RED_BED
            .defaultBlockState()
            .setValue(BedBlock.FACING, Direction.NORTH)
            .setValue(BedBlock.PART, BedPart.HEAD));
    set(l, o, 22, 1, 11, Blocks.BARREL.defaultBlockState());
    set(l, o, 22, 2, 11, Blocks.LANTERN.defaultBlockState());
    sign(l, o.offset(10, 1, 4), "TV power", "Right-click TV", "Then head to bed", "");
  }

  static boolean isTv(BlockPos o, BlockPos at) {
    int x = at.getX() - o.getX(), y = at.getY() - o.getY(), z = at.getZ() - o.getZ();
    return z == 3 && ((x >= 3 && x <= 8 && y >= 2 && y <= 4) || (x == 9 && y == 2));
  }

  static void television(ServerLevel l, BlockPos o, boolean off) {
    for (int x = 3; x <= 8; x++)
      for (int y = 2; y <= 4; y++) {
        BlockPos at = o.offset(x, y, 3);
        l.setBlock(at, MonitorContent.wall(Direction.SOUTH), 3);
        if (l.getBlockEntity(at) instanceof MonitorContent.MonitorEntity m) {
          m.setTheme(off ? MonitorContent.HOME_TV_OFF : MonitorContent.HOME_TV);
          m.setLines("LOCAL BROADCAST", "LATE NIGHT", "RAIN / OVERCAST", "TV POWER");
        }
      }
  }

  static void tvOff(ServerLevel l, BlockPos o) {
    television(l, o, true);
  }

  static void breach(ServerLevel l, BlockPos o) {
    for (int y = 2; y <= 4; y++) set(l, o, -2, y, 5, Blocks.AIR.defaultBlockState());
    for (int y = 1; y <= 2; y++) {
      var door = l.getBlockState(o.offset(11, y, -2));
      if (door.getBlock() instanceof DoorBlock)
        set(l, o, 11, y, -2, door.setValue(DoorBlock.OPEN, true));
    }
  }

  static ArmorStand silhouette(ServerLevel l, BlockPos o) {
    ArmorStand a = EntityType.ARMOR_STAND.create(l);
    if (a == null) return null;
    a.moveTo(o.getX() + 11.5, o.getY() + 1, o.getZ() + .5, 0, 0);
    a.setNoGravity(true);
    a.setInvulnerable(true);
    a.setSilent(true);
    a.setShowArms(true);
    a.addTag("timelocked_intro_actor");
    Item[] armor = {Items.LEATHER_BOOTS, Items.LEATHER_LEGGINGS, Items.LEATHER_CHESTPLATE};
    EquipmentSlot[] slots = {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST};
    for (int i = 0; i < armor.length; i++) {
      ItemStack stack = new ItemStack(armor[i]);
      stack.set(DataComponents.DYED_COLOR, new DyedItemColor(0x08090c, false));
      a.setItemSlot(slots[i], stack);
    }
    a.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Blocks.BLACK_CONCRETE));
    l.addFreshEntity(a);
    return a;
  }

  static void sign(ServerLevel l, BlockPos at, String... lines) {
    l.setBlock(at, Blocks.SPRUCE_SIGN.defaultBlockState(), 3);
    if (l.getBlockEntity(at) instanceof SignBlockEntity sign) {
      var text = sign.getFrontText();
      for (int i = 0; i < 4; i++)
        text = text.setMessage(i, Component.literal(i < lines.length ? lines[i] : ""));
      sign.setText(text, true);
      sign.setWaxed(true);
      sign.setChanged();
      l.sendBlockUpdated(at, l.getBlockState(at), l.getBlockState(at), 3);
    }
  }

  private IntroScenes() {}
}
