package com.github.hornta.racing.commands;

import com.github.hornta.racing.MessageKey;
import com.github.hornta.racing.RacingManager;
import com.github.hornta.racing.commando.ICommandHandler;
import com.github.hornta.racing.enums.RaceState;
import com.github.hornta.racing.messenger.MessageManager;
import com.github.hornta.racing.objects.Race;
import com.github.hornta.racing.objects.RaceCheckpoint;
import org.bukkit.command.CommandSender;

public class CommandDeleteCheckpoint extends RacingCommand implements ICommandHandler {
   public CommandDeleteCheckpoint(RacingManager racingManager) {
      super(racingManager);
   }

   public void handle(CommandSender commandSender, String[] args, int typedArgs) {
      Race race = this.racingManager.getRace(args[0]);
      if (race.getState() != RaceState.UNDER_CONSTRUCTION) {
         MessageManager.setValue("race_name", race.getName());
         MessageManager.sendMessage(commandSender, MessageKey.EDIT_NO_EDIT_MODE);
      } else {
         RaceCheckpoint checkpoint = race.getCheckpoint(Integer.parseInt(args[1]));
         this.racingManager.deleteCheckpoint(race, checkpoint, () -> {
            MessageManager.setValue("race_name", race.getName());
            MessageManager.setValue("position", checkpoint.getPosition());
            MessageManager.sendMessage(commandSender, MessageKey.RACE_DELETE_CHECKPOINT_SUCCESS);
         });
      }
   }
}
