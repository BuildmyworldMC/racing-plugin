package com.github.hornta.racing.events;

import com.github.hornta.racing.objects.Race;
import com.github.hornta.racing.objects.RaceStartPoint;

public abstract class RaceStartPointEvent extends RaceEvent {
   private final RaceStartPoint startPoint;

   RaceStartPointEvent(Race race, RaceStartPoint startPoint) {
      super(race);
      this.startPoint = startPoint;
   }

   public RaceStartPoint getStartPoint() {
      return this.startPoint;
   }
}
