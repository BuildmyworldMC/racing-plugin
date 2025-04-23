package com.github.hornta.racing.events;

import com.github.hornta.racing.objects.Race;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class RaceChangeNameEvent extends RaceEvent {
   private static final HandlerList handlers = new HandlerList();
   private final String oldName;

   public RaceChangeNameEvent(Race race, String oldName) {
      super(race);
      this.oldName = oldName;
   }

   public static HandlerList getHandlerList() {
      return handlers;
   }

   public String getOldName() {
      return this.oldName;
   }

   @NotNull
   public HandlerList getHandlers() {
      return handlers;
   }
}
