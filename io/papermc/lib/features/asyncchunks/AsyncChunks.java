package io.papermc.lib.features.asyncchunks;

import java.util.concurrent.CompletableFuture;
import org.bukkit.Chunk;
import org.bukkit.World;

public interface AsyncChunks {
   default CompletableFuture<Chunk> getChunkAtAsync(World world, int x, int z, boolean gen) {
      return this.getChunkAtAsync(world, x, z, gen, false);
   }

   CompletableFuture<Chunk> getChunkAtAsync(World var1, int var2, int var3, boolean var4, boolean var5);
}
