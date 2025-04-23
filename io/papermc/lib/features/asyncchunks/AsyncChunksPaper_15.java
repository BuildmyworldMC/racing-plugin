package io.papermc.lib.features.asyncchunks;

import java.util.concurrent.CompletableFuture;
import org.bukkit.Chunk;
import org.bukkit.World;

public class AsyncChunksPaper_15 implements AsyncChunks {
   public CompletableFuture<Chunk> getChunkAtAsync(World world, int x, int z, boolean gen, boolean isUrgent) {
      return world.getChunkAtAsync(x, z, gen, isUrgent);
   }
}
