package io.papermc.lib.features.bedspawnlocation;

import java.util.concurrent.CompletableFuture;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class BedSpawnLocationSync implements BedSpawnLocation {
   public CompletableFuture<Location> getBedSpawnLocationAsync(Player player, boolean isUrgent) {
      return CompletableFuture.completedFuture(player.getBedSpawnLocation());
   }
}
