package io.papermc.lib.features.asyncteleport;

import java.util.concurrent.CompletableFuture;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

public class AsyncTeleportPaper_13 implements AsyncTeleport {
   public CompletableFuture<Boolean> teleportAsync(Entity entity, Location location, TeleportCause cause) {
      return entity.teleportAsync(location, cause);
   }
}
