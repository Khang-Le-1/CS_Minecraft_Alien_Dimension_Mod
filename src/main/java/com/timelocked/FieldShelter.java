package com.timelocked;

import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

public final class FieldShelter {
  public static final BlockPos CORE = new BlockPos(0, 1, -3);
  public static final DeferredBlock<FieldShelter.CoreBlock> SHELTER_CORE =
      Content.BLOCKS.register(
          "sarc_shelter_core",
          () ->
              new FieldShelter.CoreBlock(
                  Properties.of()
                      .strength(3.0F, 6.0F)
                      .sound(SoundType.METAL)
                      .lightLevel(s -> 7)
                      .requiresCorrectToolForDrops()));
  public static final DeferredItem<FieldShelter.KitItem> KIT =
      Content.ITEMS.register(
          "sarc_field_shelter",
          () ->
              new FieldShelter.KitItem(new net.minecraft.world.item.Item.Properties().stacksTo(1)));
  private static List<FieldShelter.Cell> shellCache;

  public static void initialize() {}

  private static BlockState b(String id) {
    return VoidspaceContent.block("timelocked:" + id).defaultBlockState();
  }

  public static boolean interior(int x, int y, int z) {
    if (Math.abs(x) <= 5) {
      int a = Math.abs(z);
      if (y >= 1 && y <= 2 && a <= 3) {
        return true;
      }

      if (y == 3 && a <= 2) {
        return true;
      }

      if (y == 4 && a <= 1) {
        return true;
      }
    }

    return Math.abs(x) <= 1 && z >= 5 && z <= 6 && y >= 1 && y <= 3;
  }

  public static List<FieldShelter.Cell> shell() {
    if (shellCache == null) {
      shellCache = build();
    }

    return shellCache;
  }

  private static List<FieldShelter.Cell> build() {
    List<FieldShelter.Cell> c = new ArrayList<>();
    BlockState hull = b("space_hull_panel");
    BlockState rib = b("titanium_panel");
    BlockState view = b("space_viewport");
    BlockState grate = b("steel_grating");
    BlockState lamp = b("factory_lamp");
    BlockState floor = ((Block) Content.FLOOR.get()).defaultBlockState();
    BlockState hazard = ((Block) Content.HAZARD.get()).defaultBlockState();

    for (int x = -6; x <= 6; x++) {
      boolean ribbed = x % 3 == 0;

      for (int z = -4; z <= 4; z++) {
        c.add(new FieldShelter.Cell(x, 0, z, floor));
      }

      for (int y = 1; y <= 5; y++) {
        for (int z = -4; z <= 4; z++) {
          if (!interior(x, y, z)) {
            boolean shell = false;
            int a = Math.abs(z);
            if (Math.abs(x) == 6) {
              shell = y <= 2 && a <= 3 || y == 3 && a <= 2 || y == 4 && a <= 1;
            }

            if (!shell) {
              shell = a == 4 && y <= 2 || a == 3 && y == 3 || a == 2 && y == 4 || a <= 1 && y == 5;
            }

            if (shell && (z != 4 || x != 0 || y > 2)) {
              BlockState s =
                  Math.abs(x) != 6
                      ? (ribbed ? rib : hull)
                      : ((a > 1 || y != 2 && y != 3) && (a != 0 || y != 4) ? rib : view);
              if (a == 3 && y == 3 && (Math.abs(x) == 2 || Math.abs(x) == 4)) {
                s = view;
              }

              if (a <= 1 && y == 5 && Math.abs(x) == 3 && z == 0) {
                s = lamp;
              }

              c.add(new FieldShelter.Cell(x, y, z, s));
            }
          }
        }
      }
    }

    for (int x = -2; x <= 2; x++) {
      for (int z = 5; z <= 7; z++) {
        c.add(new FieldShelter.Cell(x, 0, z, Math.abs(x) != 2 && z != 7 ? floor : grate));
        c.add(new FieldShelter.Cell(x, 4, z, hull));

        for (int y = 1; y <= 3; y++) {
          boolean wall = Math.abs(x) == 2 || z == 7;
          if (wall && (z != 7 || x != 0 || y > 2)) {
            c.add(
                new FieldShelter.Cell(
                    x,
                    y,
                    z,
                    Math.abs(x) == 2 && z == 7 ? hazard : (z == 7 && y == 3 ? hazard : rib)));
          }
        }
      }
    }

    for (int x = -2; x <= 2; x++) {
      c.add(new FieldShelter.Cell(x, 3, 4, x == 0 ? hazard : rib));
    }

    BlockState door =
        (BlockState)
            ((BlockState)
                    ((BlockState)
                            ((AnimatedSarcDoorBlock) FacilityDetails.SERVICE_DOOR.get())
                                .defaultBlockState()
                                .setValue(DoorBlock.FACING, Direction.NORTH))
                        .setValue(DoorBlock.HINGE, DoorHingeSide.LEFT))
                .setValue(DoorBlock.OPEN, false);

    for (int z : new int[] {4, 7}) {
      c.add(
          new FieldShelter.Cell(
              0, 1, z, (BlockState) door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER)));
      c.add(
          new FieldShelter.Cell(
              0, 2, z, (BlockState) door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER)));
    }

    for (int x = -4; x <= -1; x++) {
      for (int z = -1; z <= 1; z++) {
        c.add(new FieldShelter.Cell(x, 6, z, Blocks.DAYLIGHT_DETECTOR.defaultBlockState()));
      }
    }

    for (int x : new int[] {-5, 5}) {
      c.add(new FieldShelter.Cell(x, 6, 0, Blocks.END_ROD.defaultBlockState()));
    }

    c.add(
        new FieldShelter.Cell(
            0,
            4,
            0,
            (BlockState) Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true)));
    c.add(
        new FieldShelter.Cell(
            CORE.getX(),
            CORE.getY(),
            CORE.getZ(),
            (BlockState)
                ((FieldShelter.CoreBlock) SHELTER_CORE.get())
                    .defaultBlockState()
                    .setValue(FieldShelter.CoreBlock.FACING, Direction.SOUTH)));
    return List.copyOf(c);
  }

  static int wx(int rot, int lx, int lz) {
    return switch (rot) {
      case 1 -> -lz;
      case 2 -> -lx;
      case 3 -> lz;
      default -> lx;
    };
  }

  static int wz(int rot, int lx, int lz) {
    return switch (rot) {
      case 1 -> lx;
      case 2 -> -lz;
      case 3 -> -lx;
      default -> lz;
    };
  }

  static BlockPos world(BlockPos origin, int rot, int x, int y, int z) {
    return origin.offset(wx(rot, x, z), y, wz(rot, x, z));
  }

  static int rotFor(Direction localSouthBecomes) {
    for (int r = 0; r < 4; r++) {
      if (Rotation.values()[r].rotate(Direction.SOUTH) == localSouthBecomes) {
        return r;
      }
    }

    return 0;
  }

  static List<BlockPos> footprint(BlockPos origin, int rot) {
    List<BlockPos> out = new ArrayList<>();

    for (FieldShelter.Cell c : shell()) {
      out.add(world(origin, rot, c.x(), c.y(), c.z()));
    }

    for (int x = -6; x <= 6; x++) {
      for (int y = 1; y <= 4; y++) {
        for (int z = -4; z <= 7; z++) {
          if (interior(x, y, z)) {
            out.add(world(origin, rot, x, y, z));
          }
        }
      }
    }

    return out;
  }

  public static boolean canDeploy(Level l, BlockPos origin, int rot) {
    for (BlockPos p : footprint(origin, rot)) {
      BlockState s = l.getBlockState(p);
      if (!s.isAir() && !s.canBeReplaced()) {
        return false;
      }

      if (l.isOutsideBuildHeight(p)) {
        return false;
      }
    }

    return true;
  }

  public static void place(Level l, BlockPos origin, int rot) {
    Rotation r = Rotation.values()[rot];
    List<FieldShelter.Cell> doors = new ArrayList<>();

    for (FieldShelter.Cell c : shell()) {
      if (c.state().getBlock() instanceof DoorBlock) {
        doors.add(c);
      } else {
        l.setBlock(world(origin, rot, c.x(), c.y(), c.z()), c.state().rotate(r), 18);
      }
    }

    for (FieldShelter.Cell c : doors) {
      l.setBlock(world(origin, rot, c.x(), c.y(), c.z()), c.state().rotate(r), 18);
    }

    for (FieldShelter.Cell c : shell()) {
      l.blockUpdated(world(origin, rot, c.x(), c.y(), c.z()), c.state().getBlock());
    }
  }

  public static int pack(Level l, BlockPos corePos, BlockState core) {
    int rot = rotFor((Direction) core.getValue(FieldShelter.CoreBlock.FACING));
    BlockPos origin =
        corePos.subtract(
            new BlockPos(
                wx(rot, CORE.getX(), CORE.getZ()), CORE.getY(), wz(rot, CORE.getX(), CORE.getZ())));

    for (int x = -6; x <= 6; x++) {
      for (int y = 1; y <= 4; y++) {
        for (int z = -4; z <= 7; z++) {
          if (interior(x, y, z)) {
            BlockPos p = world(origin, rot, x, y, z);
            if (!l.getBlockState(p).isAir() && !isShellAt(l, origin, rot, p)) {
              l.destroyBlock(p, true);
            }
          }
        }
      }
    }

    int removed = 0;
    List<FieldShelter.Cell> order = new ArrayList<>(shell());
    order.sort(Comparator.comparingInt(cx -> -cx.y()));

    for (FieldShelter.Cell c : order) {
      BlockPos p = world(origin, rot, c.x(), c.y(), c.z());
      if (l.getBlockState(p).is(c.state().getBlock())) {
        l.setBlock(p, Blocks.AIR.defaultBlockState(), 50);
        removed++;
      }
    }

    for (FieldShelter.Cell c : shell()) {
      l.blockUpdated(world(origin, rot, c.x(), c.y(), c.z()), Blocks.AIR);
    }

    return removed;
  }

  private static boolean isShellAt(Level l, BlockPos origin, int rot, BlockPos p) {
    for (FieldShelter.Cell c : shell()) {
      if (world(origin, rot, c.x(), c.y(), c.z()).equals(p)) {
        return l.getBlockState(p).is(c.state().getBlock());
      }
    }

    return false;
  }

  private FieldShelter() {}

  public record Cell(int x, int y, int z, BlockState state) {}

  public static final class CoreBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<FieldShelter.CoreBlock> CODEC =
        simpleCodec(FieldShelter.CoreBlock::new);

    public CoreBlock(Properties p) {
      super(p);
      this.registerDefaultState(
          (BlockState) ((BlockState) this.stateDefinition.any()).setValue(FACING, Direction.SOUTH));
    }

    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
      return CODEC;
    }

    protected void createBlockStateDefinition(Builder<Block, BlockState> b) {
      b.add(new Property[] {FACING});
    }

    public BlockState getStateForPlacement(BlockPlaceContext c) {
      return (BlockState)
          this.defaultBlockState().setValue(FACING, c.getHorizontalDirection().getOpposite());
    }

    protected InteractionResult useWithoutItem(
        BlockState s, Level l, BlockPos p, Player player, BlockHitResult hit) {
      if (l.isClientSide) {
        return InteractionResult.SUCCESS;
      }

      if (!player.isShiftKeyDown()) {
        player.displayClientMessage(
            Component.translatable("message.timelocked.shelter.status"), true);
        return InteractionResult.CONSUME;
      }

      FieldShelter.pack(l, p, s);
      ItemStack kit = new ItemStack((ItemLike) FieldShelter.KIT.get());
      if (!player.getInventory().add(kit)) {
        player.drop(kit, false);
      }

      l.playSound(null, p, SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 1.0F, 0.7F);
      l.playSound(
          null,
          p,
          (SoundEvent) SoundEvents.ARMOR_EQUIP_NETHERITE.value(),
          SoundSource.BLOCKS,
          1.0F,
          0.8F);
      player.displayClientMessage(
          Component.translatable("message.timelocked.shelter.packed"), true);
      return InteractionResult.CONSUME;
    }
  }

  public static final class KitItem extends Item {
    public KitItem(net.minecraft.world.item.Item.Properties p) {
      super(p);
    }

    public InteractionResult useOn(UseOnContext c) {
      Level l = c.getLevel();
      Player player = c.getPlayer();
      if (player == null) {
        return InteractionResult.PASS;
      }

      BlockPos origin =
          c.getClickedFace() == Direction.UP
              ? c.getClickedPos().above()
              : c.getClickedPos().relative(c.getClickedFace());
      Direction ahead = player.getDirection();
      int rot = FieldShelter.rotFor(ahead.getOpposite());
      origin = origin.relative(ahead, 9);
      if (l.isClientSide) {
        return InteractionResult.SUCCESS;
      }

      if (!FieldShelter.canDeploy(l, origin, rot)) {
        player.displayClientMessage(
            Component.translatable("message.timelocked.shelter.blocked"), true);
        return InteractionResult.FAIL;
      }

      FieldShelter.place(l, origin, rot);
      if (!player.getAbilities().instabuild) {
        c.getItemInHand().shrink(1);
      }

      l.playSound(null, origin, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 1.0F, 0.6F);
      l.playSound(null, origin, SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 1.0F, 0.8F);
      player.displayClientMessage(
          Component.translatable("message.timelocked.shelter.deployed"), true);
      return InteractionResult.CONSUME;
    }
  }
}
