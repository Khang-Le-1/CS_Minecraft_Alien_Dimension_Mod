package com.timelocked;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class FacilityEnvelope {
  private static int fill(
      ServerLevel l, BlockPos o, int x0, int x1, int y0, int y1, int z0, int z1, BlockState s) {
    int n = 0;

    for (int x = x0; x < x1; x++) {
      for (int y = y0; y < y1; y++) {
        for (int z = z0; z < z1; z++) {
          BlockPos p = o.offset(x, y, z);
          if (l.getBlockState(p).isAir()) {
            l.setBlock(p, s, 2);
            n++;
          }
        }
      }
    }

    return n;
  }

  public static int repair(ServerLevel l, BlockPos o) {
    if (BlueprintRooms.active(l, o)) return 0; // legacy box is not this blueprint
    BlockState shell = ((Block) Content.SHELL.get()).defaultBlockState();
    int n =
        fill(l, o, 0, 130, 28, 29, 0, 158, shell)
            + fill(l, o, 0, 1, 2, 28, 0, 158, shell)
            + fill(l, o, 129, 130, 2, 28, 0, 158, shell)
            + fill(l, o, 0, 130, 2, 28, 0, 1, shell)
            + fill(l, o, 0, 56, 2, 28, 156, 158, shell)
            + fill(l, o, 74, 130, 2, 28, 156, 158, shell)
            + fill(l, o, 44, 87, 37, 38, 142, 197, shell);

    try (InputStream in =
        FacilityEnvelope.class.getResourceAsStream(
            "/assets/timelocked/facility-rebuild-manifest.json")) {
      JsonObject json = JsonParser.parseReader(new InputStreamReader(in)).getAsJsonObject();

      for (JsonElement e : json.getAsJsonArray("emergency_positions")) {
        JsonArray a = e.getAsJsonArray();
        BlockPos p = o.offset(a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsInt());
        if (l.getBlockState(p).is((Block) Content.FLOOR.get())) {
          l.setBlock(p, ((Block) FacilityDetails.EMERGENCY.get()).defaultBlockState(), 2);
          n++;
        }
      }

      return n;
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  private FacilityEnvelope() {}
}
