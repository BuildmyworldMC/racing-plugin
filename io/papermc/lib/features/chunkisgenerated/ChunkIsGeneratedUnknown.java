package io.papermc.lib.features.chunkisgenerated;

import org.bukkit.World;

public class ChunkIsGeneratedUnknown implements ChunkIsGenerated {
   public boolean isChunkGenerated(World world, int x, int z) {
      return true;
   }
}
