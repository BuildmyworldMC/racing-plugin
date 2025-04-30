package com.github.hornta.racing.objects;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RaceSessionResult {
   private final RaceSession raceSession;
   private final Map<UUID, PlayerSessionResult> playerResults;

   public RaceSessionResult(RaceSession raceSession) {
      this.raceSession = raceSession;
      this.playerResults = new HashMap<>();
   }

   public RaceSession getRaceSession() {
      return this.raceSession;
   }

   public void addPlayerSessionResult(RacePlayerSession playerSession, long raceDuration, boolean completedRace) {
      PlayerSessionResult result = new PlayerSessionResult(playerSession.getPlayer().getUniqueId(), raceDuration, playerSession.getCurrentLap(), playerSession.getCurrentCheckpoint(), completedRace, playerSession.getFastestLap());
      this.playerResults.put(playerSession.getPlayer().getUniqueId(), result);
   }

   public Map<UUID, PlayerSessionResult> getPlayerResults() {
      return this.playerResults;
   }
}
