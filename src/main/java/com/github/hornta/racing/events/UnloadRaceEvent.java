package com.github.hornta.racing.events;

import com.github.hornta.racing.objects.Race;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class UnloadRaceEvent extends RaceEvent {
   private static final HandlerList handlers = new HandlerList();

   public UnloadRaceEvent(Race race) {
      super(race);
   }

   public static HandlerList getHandlerList() {
      return handlers;
   }

   @NotNull
   public HandlerList getHandlers() {
      return handlers;
   }
}
