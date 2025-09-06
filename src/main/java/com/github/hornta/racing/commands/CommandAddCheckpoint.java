package com.github.hornta.racing.commands;

import com.github.hornta.racing.MessageKey;
import com.github.hornta.racing.RacingManager;
import com.github.hornta.racing.Util;
import com.github.hornta.racing.commando.ICommandHandler;
import com.github.hornta.racing.enums.RaceState;
import com.github.hornta.racing.messenger.MessageManager;
import com.github.hornta.racing.objects.Race;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;

public class CommandAddCheckpoint extends RacingCommand implements ICommandHandler {
   public CommandAddCheckpoint(RacingManager racingManager) {
      super(racingManager);
   }

   public void handle(CommandSender commandSender, String[] args, int typedArgs) {
       int MIN_RADIUS = 3;
       int MAX_RADIUS = 9;

       // Récupération de la course
       Race race = this.racingManager.getRace(args[0]);
       
       // Position et radius par défaut
       int position = race.getCheckpoints().size() + 1; // Position à la fin par défaut
       int radius = 3; // Radius par défaut

       // Si une position spécifique est donnée
       try {
           if (typedArgs >= 2) {
               position = Integer.parseInt(args[1]);
           }
           
           // Si un radius spécifique est donné
           if (typedArgs >= 3) {
               radius = Integer.parseInt(args[2]);
           }
       } catch (NumberFormatException e) {
           MessageManager.sendMessage(commandSender, MessageKey.VALIDATE_NON_INTEGER);
           return;
       }

       if (radius < MIN_RADIUS || radius > MAX_RADIUS) {
           MessageManager.setValue("radius", MIN_RADIUS);
           MessageManager.sendMessage(commandSender, MessageKey.RACE_ADD_CHECKPOINT_INVALID_RADIUS);
           return;
       }

      if (race.getState() != RaceState.UNDER_CONSTRUCTION) {
         MessageManager.setValue("race_name", race.getName());
         MessageManager.sendMessage(commandSender, MessageKey.EDIT_NO_EDIT_MODE);
      } else {
         Entity entity = (Entity)commandSender;

         if (position > race.getCheckpoints().size() + 1) {
            MessageManager.setValue("max", (race.getCheckpoints().size() + 1));
            MessageManager.sendMessage(commandSender, MessageKey.RACE_ADD_CHECKPOINT_POSITION_OUT_OF_BOUNDS);
         } else if (race.getCheckpoint(entity.getLocation()) != null) {
            MessageManager.sendMessage(commandSender, MessageKey.RACE_ADD_CHECKPOINT_IS_OCCUPIED);
         } else {
            this.racingManager.addCheckpoint(Util.centerOnBlock(entity.getLocation()), race, position, radius, (checkPoint) -> {
               MessageManager.setValue("race_name", race.getName());
               MessageManager.setValue("position", checkPoint.getPosition());
               MessageManager.sendMessage(entity, MessageKey.RACE_ADD_CHECKPOINT_SUCCESS);
            });
         }
      }
   }
}
