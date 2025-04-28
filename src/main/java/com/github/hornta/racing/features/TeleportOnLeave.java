package com.github.hornta.racing.features;

import com.github.hornta.racing.enums.RaceSessionState;
import com.github.hornta.racing.events.LeaveEvent;
import io.papermc.lib.PaperLib;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

public class TeleportOnLeave implements Listener {
   @EventHandler
   private void onLeave(LeaveEvent event) {
      if (event.getRaceSession().getState() == RaceSessionState.COUNTDOWN || event.getRaceSession().getState() == RaceSessionState.STARTED) {
         PaperLib.teleportAsync(event.getPlayerSession().getPlayer(), event.getRaceSession().getRace().getSpawn(), TeleportCause.PLUGIN);
      }

   }
}
