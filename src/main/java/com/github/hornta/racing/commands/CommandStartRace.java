package com.github.hornta.racing.commands;

import com.github.hornta.racing.RacingManager;
import com.github.hornta.racing.commando.ICommandHandler;
import org.bukkit.command.CommandSender;

public class CommandStartRace extends RacingCommand implements ICommandHandler {
   public CommandStartRace(RacingManager racingManager) {
      super(racingManager);
   }

   public void handle(CommandSender commandSender, String[] args, int typedArgs) {
      int laps = args.length == 1 ? 1 : Integer.parseInt(args[1]);
      this.racingManager.tryStartRace(args[0], commandSender, laps);
   }
}
