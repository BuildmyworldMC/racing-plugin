package io.papermc.lib.features.asyncteleport;

import java.util.concurrent.CompletableFuture;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

public interface AsyncTeleport {
   CompletableFuture<Boolean> teleportAsync(Entity var1, Location var2, TeleportCause var3);
}
