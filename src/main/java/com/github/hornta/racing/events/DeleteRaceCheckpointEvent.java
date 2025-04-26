package com.github.hornta.racing.events;

import com.github.hornta.racing.objects.Race;
import com.github.hornta.racing.objects.RaceCheckpoint;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class DeleteRaceCheckpointEvent extends RaceCheckpointEvent {
   private static final HandlerList handlers = new HandlerList();

   public DeleteRaceCheckpointEvent(Race race, RaceCheckpoint checkpoint) {
      super(race, checkpoint);
   }

   public static HandlerList getHandlerList() {
      return handlers;
   }

   @NotNull
   public HandlerList getHandlers() {
      return handlers;
   }
}
