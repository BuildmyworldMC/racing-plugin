package io.papermc.lib.environments;

import io.papermc.lib.features.asyncchunks.AsyncChunks;
import io.papermc.lib.features.asyncchunks.AsyncChunksSync;
import io.papermc.lib.features.asyncteleport.AsyncTeleport;
import io.papermc.lib.features.asyncteleport.AsyncTeleportSync;
import io.papermc.lib.features.bedspawnlocation.BedSpawnLocation;
import io.papermc.lib.features.bedspawnlocation.BedSpawnLocationSync;
import io.papermc.lib.features.blockstatesnapshot.BlockStateSnapshot;
import io.papermc.lib.features.blockstatesnapshot.BlockStateSnapshotBeforeSnapshots;
import io.papermc.lib.features.blockstatesnapshot.BlockStateSnapshotNoOption;
import io.papermc.lib.features.blockstatesnapshot.BlockStateSnapshotResult;
import io.papermc.lib.features.chunkisgenerated.ChunkIsGenerated;
import io.papermc.lib.features.chunkisgenerated.ChunkIsGeneratedApiExists;
import io.papermc.lib.features.chunkisgenerated.ChunkIsGeneratedUnknown;
import java.util.concurrent.CompletableFuture;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

public abstract class Environment {
   private final int minecraftVersion;
   private final int minecraftPatchVersion;
   private int minecraftPreReleaseVersion;
   protected AsyncChunks asyncChunksHandler = new AsyncChunksSync();
   protected AsyncTeleport asyncTeleportHandler = new AsyncTeleportSync();
   protected ChunkIsGenerated isGeneratedHandler = new ChunkIsGeneratedUnknown();
   protected BlockStateSnapshot blockStateSnapshotHandler;
   protected BedSpawnLocation bedSpawnLocationHandler = new BedSpawnLocationSync();

   public Environment() {
      Pattern versionPattern = Pattern.compile("\\(MC: (\\d)\\.(\\d+)\\.?(\\d+?)?(?: Pre-Release )?(\\d)?\\)");
      Matcher matcher = versionPattern.matcher(Bukkit.getVersion());
      int version = 0;
      int patchVersion = 0;
      int preReleaseVersion = 0;
      if (matcher.find()) {
         MatchResult matchResult = matcher.toMatchResult();

         try {
            version = Integer.parseInt(matchResult.group(2), 10);
         } catch (Exception var10) {
         }

         if (matchResult.groupCount() >= 3) {
            try {
               patchVersion = Integer.parseInt(matchResult.group(3), 10);
            } catch (Exception var9) {
            }
         }

         if (matchResult.groupCount() >= 4) {
            try {
               preReleaseVersion = Integer.parseInt(matcher.group(4));
            } catch (Exception var8) {
            }
         }
      }

      this.minecraftVersion = version;
      this.minecraftPatchVersion = patchVersion;
      this.minecraftPreReleaseVersion = preReleaseVersion;
      if (this.isVersion(13, 1)) {
         this.isGeneratedHandler = new ChunkIsGeneratedApiExists();
      }

      if (!this.isVersion(12)) {
         this.blockStateSnapshotHandler = new BlockStateSnapshotBeforeSnapshots();
      } else {
         this.blockStateSnapshotHandler = new BlockStateSnapshotNoOption();
      }

   }

   public abstract String getName();

   public CompletableFuture<Chunk> getChunkAtAsync(World world, int x, int z, boolean gen) {
      return this.asyncChunksHandler.getChunkAtAsync(world, x, z, gen, false);
   }

   public CompletableFuture<Chunk> getChunkAtAsync(World world, int x, int z, boolean gen, boolean isUrgent) {
      return this.asyncChunksHandler.getChunkAtAsync(world, x, z, gen, isUrgent);
   }

   public CompletableFuture<Chunk> getChunkAtAsyncUrgently(World world, int x, int z, boolean gen) {
      return this.asyncChunksHandler.getChunkAtAsync(world, x, z, gen, true);
   }

   public CompletableFuture<Boolean> teleport(Entity entity, Location location, TeleportCause cause) {
      return this.asyncTeleportHandler.teleportAsync(entity, location, cause);
   }

   public boolean isChunkGenerated(World world, int x, int z) {
      return this.isGeneratedHandler.isChunkGenerated(world, x, z);
   }

   public BlockStateSnapshotResult getBlockState(Block block, boolean useSnapshot) {
      return this.blockStateSnapshotHandler.getBlockState(block, useSnapshot);
   }

   public CompletableFuture<Location> getBedSpawnLocationAsync(Player player, boolean isUrgent) {
      return this.bedSpawnLocationHandler.getBedSpawnLocationAsync(player, isUrgent);
   }

   public boolean isVersion(int minor) {
      return this.isVersion(minor, 0);
   }

   public boolean isVersion(int minor, int patch) {
      return this.minecraftVersion > minor || this.minecraftVersion >= minor && this.minecraftPatchVersion >= patch;
   }

   public int getMinecraftVersion() {
      return this.minecraftVersion;
   }

   public int getMinecraftPatchVersion() {
      return this.minecraftPatchVersion;
   }

   public int getMinecraftPreReleaseVersion() {
      return this.minecraftPreReleaseVersion;
   }

   public boolean isSpigot() {
      return false;
   }

   public boolean isPaper() {
      return false;
   }
}
