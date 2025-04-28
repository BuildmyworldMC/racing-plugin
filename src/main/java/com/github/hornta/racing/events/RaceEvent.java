package com.github.hornta.racing.events;

import com.github.hornta.racing.objects.Race;
import org.bukkit.event.Event;

public abstract class RaceEvent extends Event {
   private final Race race;

   RaceEvent(Race race) {
      this.race = race;
   }

   public Race getRace() {
      return this.race;
   }
}
