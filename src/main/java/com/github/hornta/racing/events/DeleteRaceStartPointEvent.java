package com.github.hornta.racing.events;

import com.github.hornta.racing.objects.Race;
import com.github.hornta.racing.objects.RaceStartPoint;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class DeleteRaceStartPointEvent extends RaceStartPointEvent {
   private static final HandlerList handlers = new HandlerList();

   public DeleteRaceStartPointEvent(Race race, RaceStartPoint startPoint) {
      super(race, startPoint);
   }

   public static HandlerList getHandlerList() {
      return handlers;
   }

   @NotNull
   public HandlerList getHandlers() {
      return handlers;
   }
}
