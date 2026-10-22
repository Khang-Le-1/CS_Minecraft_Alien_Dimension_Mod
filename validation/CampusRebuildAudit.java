package com.timelocked;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.*;
import net.minecraft.world.level.block.*;

/** Test-only geometry, route and actor lifecycle checks for the v28 fit-out. */
final class CampusRebuildAudit {
  static void run(IntroAudit.IntroPlayer p, CompoundTag t, ServerLevel l) {
    BlockPos o = IntroSequence.home(t), s = CampusNeighborhood.school(o);
    IntroAudit.check(CampusRebuild.active(t), "fresh starts select v28 geometry");
    IntroAudit.check(
        l.getBlockState(o.offset(11, 14, 0)).is(Blocks.DEEPSLATE_TILE_SLAB),
        "home has a raised pitched ridge");
    IntroAudit.check(
        l.getBlockState(o.offset(4, 3, -7)).is(Blocks.STRIPPED_OAK_LOG),
        "home has a supported front porch");
    IntroAudit.check(
        l.getBlockState(o.offset(-6, 2, 7)).is(Blocks.GLASS_PANE), "home has glazed side bay");
    IntroAudit.check(
        l.getBlockState(o.offset(-4, 1, 6)).is(CampusFurnishings.HOME_CHAIR.get()),
        "reading nook uses scaled furniture");
    for (int z : new int[] {-45, -41, -37, -33})
      for (int x : new int[] {77, 78, 81, 82, 85, 86}) {
        IntroAudit.check(
            l.getBlockState(s.offset(x, 1, z)).is(CampusFurnishings.DESK.get()),
            "classroom desk " + x + ":" + z);
        IntroAudit.check(
            l.getBlockState(s.offset(x, 1, z + 1)).is(CampusFurnishings.CHAIR.get()),
            "classroom chair " + x + ":" + z);
      }
    for (int z : new int[] {-47, -42, -37, -32})
      for (int x : new int[] {78, 82, 86, 90}) {
        var at = s.offset(x, 4, z);
        var state = l.getBlockState(at);
        IntroAudit.check(
            state.is(CampusFurnishings.PANEL.get()) && state.getLightEmission(l, at) == 15,
            "lowered ceiling panel emits real light");
      }
    IntroAudit.check(
        l.getBlockState(s.offset(80, 2, -49)).is(Blocks.GREEN_CONCRETE),
        "board is on the teaching wall, not over the windows");
    IntroAudit.check(
        l.getBlockState(s.offset(80, 2, -28)).is(Blocks.LIGHT_BLUE_STAINED_GLASS),
        "south windows remain clear");
    for (int z = -46; z <= -31; z++)
      for (int x : new int[] {79, 83, 89}) {
        var a = s.offset(x, 1, z);
        IntroAudit.check(
            l.getBlockState(a).getCollisionShape(l, a).isEmpty()
                && l.getBlockState(a.above()).getCollisionShape(l, a.above()).isEmpty(),
            "classroom aisle " + x + ":" + z);
      }
    // The driveway meets the central vehicle aisle, not the back of a parking bay.
    for (int x = 241; x <= 249; x++) {
      var at = o.offset(x, 4, -58);
      IntroAudit.check(
          !l.getBlockState(at).getCollisionShape(l, at).isEmpty()
              && l.getBlockState(at.above()).getCollisionShape(l, at.above()).isEmpty(),
          "driveway joins front lot central aisle " + x);
    }
    for (int x = 250; x <= 298; x += 6) {
      IntroAudit.check(
          l.getBlockState(o.offset(x, 4, -70)).is(Blocks.WHITE_CONCRETE),
          "front lot parking line " + x);
    }
    IntroAudit.check(
        l.getBlockState(o.offset(252, 4, -68)).is(Blocks.BLUE_CONCRETE),
        "accessible bay near entrance sidewalk");
    for (int z = -87; z <= -40; z++) {
      var at = o.offset(244, 5, z);
      IntroAudit.check(
          l.getBlockState(at).getCollisionShape(l, at).isEmpty(),
          "parking entrance pedestrian route " + z);
    }
    for (int i = 0; i < 10; i++) {
      IntroAudit.check(t.hasUUID("CampusAmbient" + i), "populated scene stores actor " + i);
      if (t.hasUUID("CampusAmbient" + i)) {
        var a = l.getEntity(t.getUUID("CampusAmbient" + i));
        IntroAudit.check(
            a != null && CampusOpening.owns(t, a), "ambient actor owner recognized " + i);
      }
    }
    CompoundTag old = t.copy();
    old.remove(CampusRebuild.REVISION);
    IntroAudit.check(
        IntroSequence.home(old).equals(CampusNeighborhood.home(t.getInt("Slot"))),
        "v27 saves keep their previous bank");
    IntroAudit.check(
        CampusOpening.class_desk(old).equals(SccCampus.CLASS_DESK.offset(180, 4, -64)),
        "v27 saves keep old classroom lectern");
    LightEngineAudit.run(p.server, l, o, s);
    export(l, o, s);
  }

  static void export(ServerLevel l, BlockPos o, BlockPos s) {
    try {
      var data = new java.util.LinkedHashMap<String, Object>();
      for (String name : java.util.List.of("classroom", "home", "parking")) {
        var cells = new java.util.ArrayList<Object>();
        int[] b =
            name.equals("classroom")
                ? new int[] {76, 0, -54, 92, 6, -28}
                : name.equals("home")
                    ? new int[] {-7, 0, -9, 27, 16, 23}
                    : new int[] {239, 4, -88, 303, 11, -28};
        BlockPos base = name.equals("classroom") ? s : o;
        for (int x = b[0]; x <= b[3]; x++)
          for (int y = b[1]; y <= b[4]; y++)
            for (int z = b[2]; z <= b[5]; z++) {
              var st = l.getBlockState(base.offset(x, y, z));
              if (st.isAir()) continue;
              String id =
                  net.minecraft.core.registries.BuiltInRegistries.BLOCK
                      .getKey(st.getBlock())
                      .toString();
              String facing =
                  st.hasProperty(HorizontalDirectionalBlock.FACING)
                      ? st.getValue(HorizontalDirectionalBlock.FACING).getName()
                      : "north";
              cells.add(
                  java.util.List.of(
                      x,
                      y,
                      z,
                      id,
                      facing,
                      st.hasProperty(SlabBlock.TYPE)
                          ? st.getValue(SlabBlock.TYPE).getSerializedName()
                          : ""));
            }
        data.put(name, cells);
      }
      java.nio.file.Files.writeString(
          IntroAudit.output("campus-v28-geometry.json"), new com.google.gson.Gson().toJson(data));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private CampusRebuildAudit() {}
}
