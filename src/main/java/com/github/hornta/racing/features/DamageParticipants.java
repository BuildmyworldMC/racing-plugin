package com.github.hornta.racing.features;

import com.github.hornta.racing.RacingPlugin;
import com.github.hornta.racing.objects.RaceSession;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class DamageParticipants implements Listener {
   @EventHandler
   public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
      if (event.getEntity() instanceof Player a && event.getDamager() instanceof Player b) {
          RaceSession raceSessionA = RacingPlugin.getInstance().getRacingManager().getParticipatingRace(a);
         RaceSession raceSessionB = RacingPlugin.getInstance().getRacingManager().getParticipatingRace(b);
         if (raceSessionA != null && raceSessionA.isCurrentlyRacing(a) || raceSessionB != null && raceSessionB.isCurrentlyRacing(b)) {
            event.setCancelled(true);
         }
      }

   }
}
