package com.github.hornta.racing.events;

import com.github.hornta.racing.objects.RacePlayerSession;
import com.github.hornta.racing.objects.RaceSession;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class ParticipateEvent extends Event {
   private static final HandlerList handlers = new HandlerList();
   private final RaceSession raceSession;
   private final RacePlayerSession playerSession;

   public ParticipateEvent(RaceSession raceSession, RacePlayerSession playerSession) {
      this.raceSession = raceSession;
      this.playerSession = playerSession;
   }

   public static HandlerList getHandlerList() {
      return handlers;
   }

   public RaceSession getRaceSession() {
      return this.raceSession;
   }

   public RacePlayerSession getPlayerSession() {
      return this.playerSession;
   }

   @NotNull
   public HandlerList getHandlers() {
      return handlers;
   }
}
