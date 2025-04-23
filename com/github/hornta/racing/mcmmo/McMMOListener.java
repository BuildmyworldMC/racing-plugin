package com.github.hornta.racing.mcmmo;

import com.github.hornta.racing.RacingManager;
import com.github.hornta.racing.enums.RaceSessionState;
import com.github.hornta.racing.objects.RaceSession;
import com.gmail.nossr50.events.hardcore.McMMOPlayerDeathPenaltyEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class McMMOListener implements Listener {
   private final RacingManager racingManager;

   public McMMOListener(RacingManager racingManager) {
      this.racingManager = racingManager;
   }

   @EventHandler
   void onMcMMOPlayerDeathPenalty(McMMOPlayerDeathPenaltyEvent event) {
      RaceSession raceSession = this.racingManager.getParticipatingRace(event.getPlayer());
      if (raceSession != null && (raceSession.getState() == RaceSessionState.COUNTDOWN || raceSession.getState() == RaceSessionState.STARTED)) {
         event.setCancelled(true);
      }

   }
}
