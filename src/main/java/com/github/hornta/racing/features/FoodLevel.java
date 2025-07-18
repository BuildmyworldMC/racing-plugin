package com.github.hornta.racing.features;

import com.github.hornta.racing.ConfigKey;
import com.github.hornta.racing.RacingPlugin;
import com.github.hornta.racing.enums.RaceSessionState;
import com.github.hornta.racing.objects.RaceSession;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.FoodLevelChangeEvent;

public class FoodLevel implements Listener {
   @EventHandler
   void onFoodLevelChange(FoodLevelChangeEvent event) {
      boolean allowFoodLevelChange = RacingPlugin.getInstance().getConfiguration().get(ConfigKey.ALLOW_FOOD_LEVEL_CHANGE);
      if (event.getEntity() instanceof Player player) {
          RaceSession session = RacingPlugin.getInstance().getRacingManager().getParticipatingRace(player);
         if (!allowFoodLevelChange && session != null && session.isCurrentlyRacing(player) && (session.getState() == RaceSessionState.COUNTDOWN || session.getState() == RaceSessionState.STARTED)) {
            event.setCancelled(true);
         }
      }
   }
}
