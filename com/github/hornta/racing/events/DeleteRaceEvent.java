package com.github.hornta.racing.events;

import com.github.hornta.racing.objects.Race;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class DeleteRaceEvent extends RaceEvent {
   private static final HandlerList handlers = new HandlerList();

   public DeleteRaceEvent(Race race) {
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
