package com.github.hornta.racing.events;

import com.github.hornta.racing.enums.RaceCommandType;
import com.github.hornta.racing.objects.RacePlayerSession;
import com.github.hornta.racing.objects.RaceSession;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class ExecuteCommandEvent extends Event {
   private static final HandlerList handlers = new HandlerList();
   private final RaceCommandType commandType;
   private final RaceSession raceSession;
   private RacePlayerSession playerSession;

   public ExecuteCommandEvent(RaceCommandType commandType, RaceSession raceSession) {
      this.commandType = commandType;
      this.raceSession = raceSession;
   }

   public ExecuteCommandEvent(RaceCommandType commandType, RaceSession raceSession, RacePlayerSession playerSession) {
      this.commandType = commandType;
      this.raceSession = raceSession;
      this.playerSession = playerSession;
   }

   public static HandlerList getHandlerList() {
      return handlers;
   }

   public RaceCommandType getCommandType() {
      return this.commandType;
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
