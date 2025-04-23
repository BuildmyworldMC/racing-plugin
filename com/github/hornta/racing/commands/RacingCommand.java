package com.github.hornta.racing.commands;

import com.github.hornta.racing.RacingManager;

public abstract class RacingCommand {
   protected RacingManager racingManager;

   public RacingCommand(RacingManager racingManager) {
      this.racingManager = racingManager;
   }
}
