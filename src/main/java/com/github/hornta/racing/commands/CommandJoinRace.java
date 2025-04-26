package com.github.hornta.racing.commands;

import com.github.hornta.racing.RacingManager;
import com.github.hornta.racing.commando.ICommandHandler;
import com.github.hornta.racing.enums.JoinType;
import com.github.hornta.racing.objects.Race;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandJoinRace extends RacingCommand implements ICommandHandler {
   public CommandJoinRace(RacingManager racingManager) {
      super(racingManager);
   }

   public void handle(CommandSender commandSender, String[] args, int typedArgs) {
      Race race = this.racingManager.getRace(args[0]);
      Player player = (Player)commandSender;
      this.racingManager.joinRace(race, player, JoinType.COMMAND);
   }
}
