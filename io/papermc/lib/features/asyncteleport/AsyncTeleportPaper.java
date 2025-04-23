package io.papermc.lib.features.asyncteleport;

import io.papermc.lib.PaperLib;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

public class AsyncTeleportPaper implements AsyncTeleport {
   public CompletableFuture<Boolean> teleportAsync(Entity entity, Location location, TeleportCause cause) {
      int x = location.getBlockX() >> 4;
      int z = location.getBlockZ() >> 4;
      return PaperLib.getChunkAtAsyncUrgently(location.getWorld(), x, z, true).thenApply((chunk) -> {
         return entity.teleport(location, cause);
      });
   }
}
