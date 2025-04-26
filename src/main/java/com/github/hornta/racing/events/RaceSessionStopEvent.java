package com.github.hornta.racing.events;

import com.github.hornta.racing.objects.RaceSession;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class RaceSessionStopEvent extends Event {
   private static final HandlerList handlers = new HandlerList();
   private final RaceSession raceSession;

   public RaceSessionStopEvent(RaceSession raceSession) {
      this.raceSession = raceSession;
   }

   public static HandlerList getHandlerList() {
      return handlers;
   }

   public RaceSession getRaceSession() {
      return this.raceSession;
   }

   @NotNull
   public HandlerList getHandlers() {
      return handlers;
   }
}
