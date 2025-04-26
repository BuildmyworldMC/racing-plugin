package com.github.hornta.racing.events;

import com.github.hornta.racing.objects.RaceCheckpoint;
import com.github.hornta.racing.objects.RacePlayerSession;
import com.github.hornta.racing.objects.RaceSession;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class CheckpointReachedEvent extends Event {
   private static final HandlerList handlers = new HandlerList();
   private final RaceSession raceSession;
   private final RacePlayerSession playerSession;
   private final RaceCheckpoint checkpoint;

   public CheckpointReachedEvent(RaceSession raceSession, RacePlayerSession playerSession, RaceCheckpoint checkpoint) {
      this.raceSession = raceSession;
      this.playerSession = playerSession;
      this.checkpoint = checkpoint;
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

   public RaceCheckpoint getCheckpoint() {
      return this.checkpoint;
   }

   @NotNull
   public HandlerList getHandlers() {
      return handlers;
   }
}
