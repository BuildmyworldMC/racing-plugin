package io.papermc.lib.environments;

import io.papermc.lib.features.asyncchunks.AsyncChunksPaper_13;
import io.papermc.lib.features.asyncchunks.AsyncChunksPaper_15;
import io.papermc.lib.features.asyncchunks.AsyncChunksPaper_9_12;
import io.papermc.lib.features.asyncteleport.AsyncTeleportPaper;
import io.papermc.lib.features.asyncteleport.AsyncTeleportPaper_13;
import io.papermc.lib.features.bedspawnlocation.BedSpawnLocationPaper;
import io.papermc.lib.features.blockstatesnapshot.BlockStateSnapshotOptionalSnapshots;
import io.papermc.lib.features.chunkisgenerated.ChunkIsGeneratedApiExists;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.HumanEntity;

public class PaperEnvironment extends SpigotEnvironment {
   public PaperEnvironment() {
      if (this.isVersion(13, 1)) {
         this.asyncChunksHandler = new AsyncChunksPaper_13();
         this.asyncTeleportHandler = new AsyncTeleportPaper_13();
      } else if (this.isVersion(9) && !this.isVersion(13)) {
         this.asyncChunksHandler = new AsyncChunksPaper_9_12();
         this.asyncTeleportHandler = new AsyncTeleportPaper();
      }

      if (this.isVersion(12)) {
         this.isGeneratedHandler = new ChunkIsGeneratedApiExists();
         this.blockStateSnapshotHandler = new BlockStateSnapshotOptionalSnapshots();
      }

      if (this.isVersion(15, 2)) {
         try {
            World.class.getDeclaredMethod("getChunkAtAsyncUrgently", Location.class);
            this.asyncChunksHandler = new AsyncChunksPaper_15();
            HumanEntity.class.getDeclaredMethod("getPotentialBedLocation");
            this.bedSpawnLocationHandler = new BedSpawnLocationPaper();
         } catch (NoSuchMethodException var2) {
         }
      }

   }

   public String getName() {
      return "Paper";
   }

   public boolean isPaper() {
      return true;
   }
}
