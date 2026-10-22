package com.timelocked;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.WritableBookContent;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class MonitorContent {
  public static final int SARC = 0;
  public static final int AMBER = 1;
  public static final int ALARM = 2;
  public static final int CELESTIAL = 3;
  public static final int MEMORIAL = 4;
  public static final int HOME_TV = 5;
  public static final int HOME_TV_OFF = 6;
  public static final int SCC_PLAQUE = 7;
  public static final int THEMES = 8;
  public static final DeferredBlock<MonitorContent.MonitorBlock> MONITOR =
      Content.BLOCKS.register(
          "sarc_monitor",
          () ->
              new MonitorContent.MonitorBlock(
                  Properties.of()
                      .strength(1.5F, 4.0F)
                      .sound(SoundType.METAL)
                      .noOcclusion()
                      .lightLevel(s -> 5)));
  public static final DeferredHolder<
          BlockEntityType<?>, BlockEntityType<MonitorContent.MonitorEntity>>
      MONITOR_ENTITY =
          Content.BLOCK_ENTITIES.register(
              "sarc_monitor",
              () ->
                  net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(
                          MonitorContent.MonitorEntity::new, new Block[] {(Block) MONITOR.get()})
                      .build(null));

  public static void initialize() {
    Content.ITEMS.registerSimpleBlockItem(MONITOR);
  }

  public static BlockState wall(Direction facing) {
    return (BlockState)
        ((BlockState)
                ((MonitorContent.MonitorBlock) MONITOR.get())
                    .defaultBlockState()
                    .setValue(MonitorContent.MonitorBlock.FACING, facing))
            .setValue(MonitorContent.MonitorBlock.STAND, false);
  }

  public static BlockState stand(Direction facing) {
    return (BlockState)
        ((BlockState)
                ((MonitorContent.MonitorBlock) MONITOR.get())
                    .defaultBlockState()
                    .setValue(MonitorContent.MonitorBlock.FACING, facing))
            .setValue(MonitorContent.MonitorBlock.STAND, true);
  }

  public static CompoundTag data(int theme, String... lines) {
    CompoundTag t = new CompoundTag();
    t.putString("id", "timelocked:sarc_monitor");
    ListTag list = new ListTag();

    for (int i = 0; i < 4; i++) {
      list.add(StringTag.valueOf(i < lines.length && lines[i] != null ? lines[i] : ""));
    }

    t.put("Lines", list);
    t.putInt("Theme", theme);
    return t;
  }

  private MonitorContent() {}

  public static final class MonitorBlock extends BaseEntityBlock {
    public static final MapCodec<MonitorContent.MonitorBlock> CODEC =
        simpleCodec(MonitorContent.MonitorBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty STAND = BooleanProperty.create("stand");
    private static final VoxelShape WALL = Block.box(0, 0, 14, 16, 16, 16);
    private static final VoxelShape DESK =
        Shapes.or(
            Block.box(3, 0, 4, 13, 1.8, 12),
            Block.box(6.5, 1.8, 8.5, 9.5, 7, 10.5),
            Block.box(1, 5, 6.85, 15, 15, 10.8));

    public MonitorBlock(Properties p) {
      super(p);
      this.registerDefaultState(
          (BlockState)
              ((BlockState)
                      ((BlockState) this.stateDefinition.any()).setValue(FACING, Direction.NORTH))
                  .setValue(STAND, false));
    }

    protected MapCodec<? extends BaseEntityBlock> codec() {
      return CODEC;
    }

    protected void createBlockStateDefinition(Builder<Block, BlockState> b) {
      b.add(new Property[] {FACING, STAND});
    }

    protected RenderShape getRenderShape(BlockState s) {
      return RenderShape.MODEL;
    }

    public BlockState getStateForPlacement(BlockPlaceContext c) {
      Direction face = c.getClickedFace();
      return face.getAxis().isHorizontal()
          ? (BlockState)
              ((BlockState) this.defaultBlockState().setValue(FACING, face)).setValue(STAND, false)
          : (BlockState)
              ((BlockState)
                      this.defaultBlockState()
                          .setValue(FACING, c.getHorizontalDirection().getOpposite()))
                  .setValue(STAND, true);
    }

    protected BlockState rotate(BlockState s, Rotation r) {
      return (BlockState) s.setValue(FACING, r.rotate((Direction) s.getValue(FACING)));
    }

    protected BlockState mirror(BlockState s, Mirror m) {
      return s.rotate(m.getRotation((Direction) s.getValue(FACING)));
    }

    protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
      return FacingDecorBlock.rotate(
          s.getValue(STAND) ? DESK : WALL, (Direction) s.getValue(FACING));
    }

    public BlockEntity newBlockEntity(BlockPos p, BlockState s) {
      return new MonitorContent.MonitorEntity(p, s);
    }

    protected ItemInteractionResult useItemOn(
        ItemStack stack,
        BlockState s,
        Level l,
        BlockPos pos,
        Player p,
        InteractionHand hand,
        BlockHitResult hit) {
      if (l.getBlockEntity(pos) instanceof MonitorContent.MonitorEntity m) {
        String var12 = null;
        WritableBookContent writable =
            (WritableBookContent) stack.get(DataComponents.WRITABLE_BOOK_CONTENT);
        if (writable != null && !writable.pages().isEmpty()) {
          var12 = (String) ((Filterable) writable.pages().get(0)).raw();
        }

        WrittenBookContent written =
            (WrittenBookContent) stack.get(DataComponents.WRITTEN_BOOK_CONTENT);
        if (written != null && !written.pages().isEmpty()) {
          var12 = ((Component) ((Filterable) written.pages().get(0)).raw()).getString();
        }

        if (var12 == null) {
          return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (!l.isClientSide) {
          m.setLines(var12.split("\n", -1));
          p.displayClientMessage(Component.literal("Monitor updated."), true);
        }

        return ItemInteractionResult.sidedSuccess(l.isClientSide);
      } else {
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      }
    }

    protected InteractionResult useWithoutItem(
        BlockState s, Level l, BlockPos pos, Player p, BlockHitResult hit) {
      if (p.isSecondaryUseActive()
          && l.getBlockEntity(pos) instanceof MonitorContent.MonitorEntity m) {
        if (!l.isClientSide) {
          m.setTheme((m.theme + 1) % 5);
          p.displayClientMessage(Component.literal("Monitor theme " + (m.theme + 1) + "/5"), true);
        }

        return InteractionResult.sidedSuccess(l.isClientSide);
      } else {
        return InteractionResult.PASS;
      }
    }
  }

  public static final class MonitorEntity extends BlockEntity {
    public final String[] lines = new String[] {"", "", "", ""};

    /**
     * A merged display wall is drawn from its anchor block across the neighbouring blocks, so widen
     * the cull box.
     */
    public net.minecraft.world.phys.AABB getRenderBoundingBox() {
      return new net.minecraft.world.phys.AABB(this.worldPosition).inflate(8.0);
    }

    public int theme;

    public MonitorEntity(BlockPos p, BlockState s) {
      super((BlockEntityType) MonitorContent.MONITOR_ENTITY.get(), p, s);
    }

    public void setLines(String... text) {
      for (int i = 0; i < 4; i++) {
        this.lines[i] =
            i < text.length && text[i] != null
                ? (text[i].length() > 32 ? text[i].substring(0, 32) : text[i])
                : "";
      }

      this.sync();
    }

    public void write(int t, String... text) {
      this.theme = Math.floorMod(t, THEMES);

      for (int i = 0; i < 4; i++) {
        this.lines[i] = i < text.length && text[i] != null ? text[i] : "";
      }
    }

    public void setTheme(int t) {
      this.theme = Math.floorMod(t, THEMES);
      this.sync();
    }

    private void sync() {
      this.setChanged();
      if (this.level != null && !this.level.isClientSide) {
        this.level.sendBlockUpdated(
            this.worldPosition, this.getBlockState(), this.getBlockState(), 2);
      }
    }

    public String line(int i) {
      return this.lines[i];
    }

    protected void saveAdditional(CompoundTag t, Provider r) {
      super.saveAdditional(t, r);
      ListTag list = new ListTag();

      for (String s : this.lines) {
        list.add(StringTag.valueOf(s));
      }

      t.put("Lines", list);
      t.putInt("Theme", this.theme);
    }

    protected void loadAdditional(CompoundTag t, Provider r) {
      super.loadAdditional(t, r);
      ListTag list = t.getList("Lines", 8);

      for (int i = 0; i < 4; i++) {
        this.lines[i] = i < list.size() ? list.getString(i) : "";
      }

      this.theme = Math.floorMod(t.getInt("Theme"), THEMES);
    }

    public CompoundTag getUpdateTag(Provider r) {
      return this.saveWithoutMetadata(r);
    }

    public Packet<ClientGamePacketListener> getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
    }
  }
}
