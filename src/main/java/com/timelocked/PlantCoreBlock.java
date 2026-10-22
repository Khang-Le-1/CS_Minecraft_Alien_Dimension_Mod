package com.timelocked;

import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent.BossBarColor;
import net.minecraft.world.BossEvent.BossBarOverlay;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.joml.Vector3f;

public final class PlantCoreBlock extends BaseEntityBlock {
  public static final IntegerProperty KIND = IntegerProperty.create("kind", 0, 3);
  public static final BooleanProperty LIT = BlockStateProperties.LIT;
  public static final int FISSION = 0;
  public static final int FUSION = 1;
  public static final int CHRONO = 2;
  public static final int VOID = 3;
  public static final int KINDS = 4;
  public static final int CRITICAL = 1000;
  public static final int COUNTDOWN = 1200;
  public static final int FUEL_PER_ITEM = 6000;
  public static final int MAX_FUEL = 48000;
  public static final String[] IDS = new String[] {"fission", "fusion", "chronotite", "void"};
  public static final String[] NAMES = new String[] {"FISSION", "FUSION", "CHRONOTITE", "VOID"};
  public static final String[] FUELS =
      new String[] {"uranium_ingot", "solarium_ingot", "chronotite_ingot", "voidsteel_ingot"};
  public static final float[] MULT = new float[] {0.9F, 1.1F, 1.0F, 1.3F};
  public static final int[] RADIUS = new int[] {20, 26, 18, 22};
  public static final int[] COLOR = new int[] {8191823, 16747050, 6284287, 11550975};
  public static final MapCodec<PlantCoreBlock> CODEC = simpleCodec(PlantCoreBlock::new);

  public PlantCoreBlock(Properties p) {
    super(p);
    this.registerDefaultState(
        (BlockState)
            ((BlockState) ((BlockState) this.stateDefinition.any()).setValue(KIND, 0))
                .setValue(LIT, false));
  }

  protected MapCodec<? extends PlantCoreBlock> codec() {
    return CODEC;
  }

  protected void createBlockStateDefinition(Builder<Block, BlockState> b) {
    b.add(new Property[] {KIND, LIT});
  }

  protected RenderShape getRenderShape(BlockState s) {
    return RenderShape.ENTITYBLOCK_ANIMATED;
  }

  protected void onRemove(BlockState s, Level l, BlockPos p, BlockState n, boolean moving) {
    if (!l.isClientSide && !s.is(n.getBlock())) {
      ReactorFrame.clear(l, p, ReactorFrame.shape(l, p, s));
    }

    super.onRemove(s, l, p, n, moving);
  }

  public BlockEntity newBlockEntity(BlockPos p, BlockState s) {
    return new PlantCoreBlock.Core(p, s);
  }

  public static Item fuel(int kind) {
    return (Item)
        BuiltInRegistries.ITEM.get(
            ResourceLocation.fromNamespaceAndPath("timelocked", FUELS[Math.floorMod(kind, 4)]));
  }

  public static DustParticleOptions dust(int kind, float size) {
    int c = COLOR[kind];
    return new DustParticleOptions(
        new Vector3f((c >> 16 & 0xFF) / 255.0F, (c >> 8 & 0xFF) / 255.0F, (c & 0xFF) / 255.0F),
        size);
  }

  public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
      Level l, BlockState s, BlockEntityType<T> t) {
    return l.isClientSide
        ? null
        : createTickerHelper(t, SarcContent.CORE_ENTITY.get(), (lv, p, st, be) -> be.serverTick());
  }

  protected ItemInteractionResult useItemOn(
      ItemStack stack,
      BlockState s,
      Level l,
      BlockPos p,
      Player user,
      InteractionHand hand,
      BlockHitResult hit) {
    if (!stack.is(fuel((Integer) s.getValue(KIND)))) {
      return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    if (!l.isClientSide && l.getBlockEntity(p) instanceof PlantCoreBlock.Core c) {
      if (c.fuel + 6000 > 48000) {
        user.displayClientMessage(Component.literal("[SARC] Fuel channels full."), true);
        return ItemInteractionResult.CONSUME;
      }

      c.fuel += 6000;
      stack.consume(1, user);
      c.setChanged();
      l.playSound(null, p, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 1.0F, 1.4F);
      user.displayClientMessage(Component.literal("[SARC] Fuel loaded. " + c.status()), true);
    }

    return ItemInteractionResult.sidedSuccess(l.isClientSide);
  }

  protected InteractionResult useWithoutItem(
      BlockState s, Level l, BlockPos p, Player user, BlockHitResult hit) {
    if (!l.isClientSide && l.getBlockEntity(p) instanceof PlantCoreBlock.Core c) {
      user.displayClientMessage(
          Component.literal(
              "[SARC] " + c.status() + " | Fuel: " + FUELS[s.getValue(KIND)].replace('_', ' ')),
          true);
    }

    return InteractionResult.sidedSuccess(l.isClientSide);
  }

  public void animateTick(BlockState s, Level l, BlockPos p, RandomSource r) {
    if ((Boolean) s.getValue(LIT)) {
      int k = (Integer) s.getValue(KIND);
      ReactorFrame.Shape sh = ReactorFrame.shape(l, p, s);
      double top = sh.bottom(p) + sh.h() + 0.1;

      for (int i = 0; i < 2; i++) {
        double a = r.nextDouble() * Math.PI * 2.0;
        double d = 0.4 + r.nextDouble() * 1.4;
        l.addParticle(
            dust(k, 1.0F),
            p.getX() + 0.5 + Math.cos(a) * d,
            top + r.nextDouble() * 0.4,
            p.getZ() + 0.5 + Math.sin(a) * d,
            0.0,
            0.02,
            0.0);
      }

      if (r.nextInt(3) == 0) {
        l.addParticle(
            ParticleTypes.ELECTRIC_SPARK,
            p.getX() + 0.5 + (r.nextDouble() - 0.5) * 1.6,
            top,
            p.getZ() + 0.5 + (r.nextDouble() - 0.5) * 1.6,
            0.0,
            0.05,
            0.0);
      }

      if (l.getBlockEntity(p) instanceof PlantCoreBlock.Core c
          && (c.critical() || c.coolantFailing)
          && r.nextInt(2) == 0) {
        l.addParticle(
            ParticleTypes.CLOUD,
            p.getX() + 0.5 + (r.nextDouble() - 0.5) * 2.0,
            top,
            p.getZ() + 0.5 + (r.nextDouble() - 0.5) * 2.0,
            0.0,
            0.12,
            0.0);
      }
    }
  }

  public static final class Core extends BlockEntity {
    public float heat;
    public int rods = 10;
    public int fuel;
    public int countdown = -1;
    private long meltAlarmAt = -1000L;
    public int base;
    public boolean coolantFailing;
    public boolean spent;
    public boolean framed;

    /**
     * Saved generation opt-in: legacy/player-built compact cores are never expanded automatically.
     */
    public boolean industrial;

    /** Missing in pre-centered saved stations; their original display offsets remain supported. */
    public int stationLayoutVersion;

    public final float[] rodAnim = new float[10];
    public final float[] spin = new float[4];
    public float lastAnim;
    private int synced = Integer.MIN_VALUE;
    public final PowerBuffer energy = new PowerBuffer(200000, false, true, this::setChanged);
    private ServerBossEvent bar;
    private List<BlockPos> displays;
    private List<BlockPos> ports;

    public Core(BlockPos p, BlockState s) {
      super((BlockEntityType) SarcContent.CORE_ENTITY.get(), p, s);
    }

    public int kind() {
      return (Integer) this.getBlockState().getValue(PlantCoreBlock.KIND);
    }

    public void init(boolean unstable) {
      if (unstable) {
        this.fuel = 24000;
        this.rods = 4;
        this.heat = 520.0F;
        this.coolantFailing = true;
      }

      this.setChanged();
    }

    public int output() {
      return this.fuel > 0
          ? (int) (400 * (10 - this.rods) / 10.0F * PlantCoreBlock.MULT[this.kind()])
          : 0;
    }

    public String status() {
      return PlantCoreBlock.NAMES[this.kind()]
          + " CORE | Temp "
          + (int) this.heat
          + "/1000 | Rods "
          + (10 - this.rods) * 10
          + "% out | "
          + (this.fuel > 0 ? this.output() + " FE/t" : "NO FUEL")
          + (this.countdown >= 0 ? " | MELTDOWN T-" + this.countdown / 20 + "s" : "");
    }

    public void scram() {
      this.rods = 10;
      this.setChanged();
    }

    public void withdraw() {
      this.rods = Math.max(0, this.rods - 1);
      this.setChanged();
    }

    public boolean critical() {
      return this.countdown >= 0;
    }

    void serverTick() {
      if (this.level instanceof ServerLevel l) {
        if (!this.framed) {
          this.base = ReactorFrame.legacyPlantBase(l, this.worldPosition);
          ReactorFrame.build(
              l,
              this.worldPosition,
              ReactorFrame.shape(l, this.worldPosition, this.getBlockState()));
          this.framed = true;
          this.setChanged();
          this.sync(l, true);
        } else if (l.getGameTime() % 40L == 0L && !ReactorFrame.intact(l, this.worldPosition)) {
          ReactorFrame.build(
              l,
              this.worldPosition,
              ReactorFrame.shape(l, this.worldPosition, this.getBlockState()));
        }

        if (!this.spent) {
          if (this.industrial && l.getGameTime() % 20L == 0L) ReactorRadiation.expose(l, this);
          int k = this.kind();
          boolean watched =
              l.getNearestPlayer(
                      this.worldPosition.getX() + 0.5,
                      this.worldPosition.getY() + 0.5,
                      this.worldPosition.getZ() + 0.5,
                      128.0,
                      false)
                  != null;
          if (this.fuel > 0) {
            if (watched) {
              this.fuel--;
              float w = (10 - this.rods) / 10.0F;
              float target =
                  1150.0F * w * PlantCoreBlock.MULT[k] / (this.coolantFailing ? 0.55F : 1.0F);
              this.heat = this.heat + (target - this.heat) * 0.0015F;
              if (this.rods == 10 && this.countdown >= 0) {
                this.heat -= 1.5F;
              }

              this.energy.generate(this.output());
            }
          } else if (watched) {
            this.heat -= 0.8F;
          }

          this.heat = Math.clamp(this.heat, 0.0F, 1200.0F);
          if (this.fuel > 0 && this.energy.getEnergyStored() > 0 && l.getGameTime() % 5L == 0L) {
            for (BlockPos q : this.ports()) {
              if (this.energy.getEnergyStored() <= 0) {
                break;
              }

              IEnergyStorage cap = (IEnergyStorage) l.getCapability(EnergyStorage.BLOCK, q, null);
              if (cap != null && cap.canReceive()) {
                int moved = cap.receiveEnergy(this.energy.extractEnergy(10000, true), false);
                this.energy.extractEnergy(moved, false);
              }
            }
          }

          boolean lit = this.fuel > 0 && this.rods < 10;
          if ((Boolean) this.getBlockState().getValue(PlantCoreBlock.LIT) != lit) {
            l.setBlock(
                this.worldPosition,
                (BlockState) this.getBlockState().setValue(PlantCoreBlock.LIT, lit),
                3);
          }

          if (this.heat >= 1000.0F && this.countdown < 0) {
            this.countdown = 1200;
            this.broadcast(
                l,
                "[SARC] "
                    + PlantCoreBlock.NAMES[k]
                    + " CORE MELTDOWN IMMINENT. Sneak + right-click the core console to SCRAM.");
            l.playSound(
                null,
                this.worldPosition,
                SoundEvents.WARDEN_SONIC_CHARGE,
                SoundSource.BLOCKS,
                4.0F,
                0.6F);
          }

          if (this.countdown >= 0) {
            if (this.heat < 850.0F) {
              this.countdown = -1;
              this.clearBar();
              this.broadcast(l, "[SARC] Core temperature falling. Meltdown averted.");
            } else {
              if (watched) {
                this.countdown--;
              }

              if (this.countdown % 20 == 0) {
                // Core Melt Alarm replaces the old bell. It is about 3.2 s long, so it is gated on
                // game time and never overlaps itself.
                if (l.getGameTime() - this.meltAlarmAt >= 65L) {
                  this.meltAlarmAt = l.getGameTime();
                  l.playSound(
                      null,
                      this.worldPosition,
                      AlarmSounds.CORE_MELT.get(),
                      SoundSource.BLOCKS,
                      5.0F,
                      1.0F);
                }
                l.sendParticles(
                    PlantCoreBlock.ParticleTypesHolder.SMOKE,
                    this.worldPosition.getX() + 0.5,
                    this.worldPosition.getY() + 1.2,
                    this.worldPosition.getZ() + 0.5,
                    12,
                    0.6,
                    0.4,
                    0.6,
                    0.02);
                this.updateBar(l);
              }

              if (this.countdown <= 0) {
                this.spent = true;
                this.clearBar();
                PlantMeltdown.start(l, this.worldPosition, k);
                return;
              }
            }
          }

          if (l.getGameTime() % 20L == 0L) {
            this.updateDisplays(l);
          }

          if (l.getGameTime() % 10L == 0L) {
            this.setChanged();
            this.sync(l, false);
          }
        }
      }
    }

    public List<BlockPos> ports() {
      if (this.ports == null && this.level != null) {
        this.ports =
            ReactorFrame.shape(this.level, this.worldPosition, this.getBlockState())
                .ports(this.worldPosition);
      }

      return this.ports == null ? List.of() : this.ports;
    }

    private void sync(ServerLevel l, boolean force) {
      int h =
          Objects.hash(
              (int) this.heat,
              this.rods,
              this.fuel > 0,
              this.countdown / 20,
              this.coolantFailing,
              this.base);
      if (force || h != this.synced) {
        this.synced = h;
        l.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 2);
      }
    }

    public CompoundTag getUpdateTag(Provider p) {
      return this.saveCustomOnly(p);
    }

    public Packet<ClientGamePacketListener> getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
    }

    private void broadcast(ServerLevel l, String msg) {
      for (ServerPlayer p : l.players()) {
        if (p.blockPosition().distSqr(this.worldPosition) < 25600.0) {
          p.sendSystemMessage(Component.literal(msg).withStyle(ChatFormatting.RED));
        }
      }
    }

    private void updateBar(ServerLevel l) {
      if (this.bar == null) {
        this.bar =
            new ServerBossEvent(Component.empty(), BossBarColor.RED, BossBarOverlay.NOTCHED_10);
        this.bar.setDarkenScreen(true);
      }

      this.bar.setName(
          Component.literal(
              PlantCoreBlock.NAMES[this.kind()]
                  + " CORE MELTDOWN IN "
                  + this.countdown / 20
                  + "s"));
      this.bar.setProgress(Math.clamp(this.countdown / 1200.0F, 0.0F, 1.0F));

      for (ServerPlayer p : l.players()) {
        boolean near = p.blockPosition().distSqr(this.worldPosition) < 25600.0;
        if (near) {
          this.bar.addPlayer(p);
        } else {
          this.bar.removePlayer(p);
        }
      }
    }

    private void clearBar() {
      if (this.bar != null) {
        this.bar.removeAllPlayers();
        this.bar = null;
      }
    }

    public void setRemoved() {
      this.clearBar();
      super.setRemoved();
    }

    private void updateDisplays(ServerLevel l) {
      if (this.displays == null || l.getGameTime() % 200L == 0L) {
        this.displays = new ArrayList<>();

        if (this.industrial && this.stationLayoutVersion >= 4) {
          int[] core = PowerStationGeometry.PLAN.core();
          BlockPos origin = this.worldPosition.offset(-core[0], -core[1], -core[2]);
          for (var m : PowerStationGeometry.PLAN.station_monitors())
            if (m.name().equals("thermal") || m.name().equals("rod")) {
              BlockPos q = origin.offset(m.x(), m.y(), m.z());
              if (l.hasChunkAt(q) && l.getBlockEntity(q) instanceof MonitorContent.MonitorEntity)
                this.displays.add(q);
            }
        } else
          for (BlockPos q :
              BlockPos.betweenClosed(
                  this.worldPosition.offset(-8, -3, -2), this.worldPosition.offset(8, 4, 10))) {
            if (l.hasChunkAt(q) && l.getBlockEntity(q) instanceof MonitorContent.MonitorEntity)
              this.displays.add(q.immutable());
          }
      }

      for (BlockPos q : this.displays) {
        if (l.getBlockEntity(q) instanceof MonitorContent.MonitorEntity m) {
          int theme = this.countdown < 0 && !(this.heat > 800.0F) ? (this.fuel > 0 ? 1 : 0) : 2;
          m.setTheme(theme);
          if ("THERMAL CONTROL".equals(m.line(0))) {
            m.setLines(
                "THERMAL CONTROL",
                "TEMP " + (int) this.heat + " / 1000",
                this.coolantFailing ? "COOLANT FAULT" : "COOLANT ONLINE",
                this.countdown >= 0
                    ? "MELTDOWN T-" + this.countdown / 20 + "s"
                    : "CHECK CORE BEFORE START");
          } else if ("ROD CONTROL".equals(m.line(0))) {
            m.setLines(
                "ROD CONTROL",
                "RODS " + (10 - this.rods) * 10 + "% OUT",
                this.fuel > 0 ? this.output() + " FE/t" : "OFFLINE - NEEDS FUEL",
                "SHIFT + USE: SCRAM");
          } else
            m.setLines(
                PlantCoreBlock.NAMES[this.kind()] + " CORE",
                "TEMP " + (int) this.heat + " / 1000",
                "RODS " + (10 - this.rods) * 10 + "% OUT",
                this.countdown >= 0
                    ? "MELTDOWN T-" + this.countdown / 20 + "s"
                    : (this.coolantFailing
                        ? "COOLANT PUMPS FAILING"
                        : (this.fuel > 0 ? this.output() + " FE/t" : "OFFLINE - NEEDS FUEL")));
        }
      }
    }

    protected void saveAdditional(CompoundTag t, Provider p) {
      super.saveAdditional(t, p);
      t.putFloat("Heat", this.heat);
      t.putInt("Rods", this.rods);
      t.putInt("Fuel", this.fuel);
      t.putInt("Countdown", this.countdown);
      t.putBoolean("CoolantFailing", this.coolantFailing);
      t.putInt("Energy", this.energy.getEnergyStored());
      t.putInt("Base", this.base);
      t.putBoolean("Framed", this.framed);
      t.putBoolean("Industrial", this.industrial);
      t.putInt("StationLayoutVersion", this.stationLayoutVersion);
      t.putBoolean("Spent", this.spent);
    }

    protected void loadAdditional(CompoundTag t, Provider p) {
      super.loadAdditional(t, p);
      this.heat = t.getFloat("Heat");
      this.rods = t.contains("Rods") ? t.getInt("Rods") : 10;
      this.fuel = t.getInt("Fuel");
      this.countdown = t.contains("Countdown") ? t.getInt("Countdown") : -1;
      this.coolantFailing = t.getBoolean("CoolantFailing");
      this.energy.restore(t.getInt("Energy"));
      this.base = t.getInt("Base");
      this.framed = t.getBoolean("Framed");
      this.industrial = t.getBoolean("Industrial");
      this.stationLayoutVersion = t.getInt("StationLayoutVersion");
      this.spent = t.getBoolean("Spent");
      this.ports = null;
    }
  }

  private static final class ParticleTypesHolder {
    static final SimpleParticleType SMOKE = ParticleTypes.LARGE_SMOKE;
  }
}
