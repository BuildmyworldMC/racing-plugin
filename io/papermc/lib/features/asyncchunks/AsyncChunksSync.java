package io.papermc.lib.features.asyncchunks;

import io.papermc.lib.PaperLib;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Chunk;
import org.bukkit.World;

public class AsyncChunksSync implements AsyncChunks {
   public CompletableFuture<Chunk> getChunkAtAsync(World world, int x, int z, boolean gen, boolean isUrgent) {
      return !gen && !PaperLib.isChunkGenerated(world, x, z) ? CompletableFuture.completedFuture((Object)null) : CompletableFuture.completedFuture(world.getChunkAt(x, z));
   }
}
