package com.github.hornta.racing.commands;

import com.github.hornta.racing.MessageKey;
import com.github.hornta.racing.RacingManager;
import com.github.hornta.racing.commando.ICommandHandler;
import com.github.hornta.racing.enums.RaceState;
import com.github.hornta.racing.messenger.MessageManager;
import com.github.hornta.racing.objects.Race;
import com.github.hornta.racing.objects.RaceCheckpoint;
import org.bukkit.command.CommandSender;

public class CommandSetCheckpointSize extends RacingCommand implements ICommandHandler {
   public CommandSetCheckpointSize(RacingManager racingManager) {
      super(racingManager);
   }

   public void handle(CommandSender commandSender, String[] args, int typedArgs) {
      Race race = this.racingManager.getRace(args[0]);
      if (race.getState() != RaceState.UNDER_CONSTRUCTION) {
         MessageManager.setValue("race_name", race.getName());
         MessageManager.sendMessage(commandSender, MessageKey.EDIT_NO_EDIT_MODE);
         return;
      }

      RaceCheckpoint checkpoint = race.getCheckpoint(Integer.parseInt(args[1]));
      int radius;
      try {
         radius = Integer.parseInt(args[2]);
      } catch (NumberFormatException e) {
         MessageManager.sendMessage(commandSender, MessageKey.VALIDATE_NON_INTEGER);
         return;
      }

      if (radius < RaceCheckpoint.MIN_RADIUS || radius > RaceCheckpoint.MAX_RADIUS) {
         MessageManager.setValue("min", RaceCheckpoint.MIN_RADIUS);
         MessageManager.setValue("max", RaceCheckpoint.MAX_RADIUS);
         MessageManager.sendMessage(commandSender, MessageKey.RACE_ADD_CHECKPOINT_INVALID_RADIUS);
         return;
      }

      this.racingManager.setCheckpointRadius(race, checkpoint, radius, () -> {
         MessageManager.setValue("race_name", race.getName());
         MessageManager.setValue("position", checkpoint.getPosition());
         MessageManager.setValue("radius", checkpoint.getRadius());
         MessageManager.sendMessage(commandSender, MessageKey.RACE_SET_CHECKPOINT_SIZE_SUCCESS);
      });
   }
}
