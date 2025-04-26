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

public class CommandAddStartPoint extends RacingCommand implements ICommandHandler {
   public CommandAddStartPoint(RacingManager racingManager) {
      super(racingManager);
   }

   public void handle(CommandSender commandSender, String[] args, int typedArgs) {
      Race race = this.racingManager.getRace(args[0]);
      int position = Integer.parseInt(args[1]);
      if (race.getState() != RaceState.UNDER_CONSTRUCTION) {
         MessageManager.setValue("race_name", (Object)race.getName());
         MessageManager.sendMessage(commandSender, MessageKey.EDIT_NO_EDIT_MODE);
      } else {
         Entity player = (Entity)commandSender;
         if (position > race.getStartPoints().size() + 1) {
            MessageManager.setValue("max", (Object)(race.getStartPoints().size() + 1));
            MessageManager.sendMessage(commandSender, MessageKey.RACE_ADD_STARTPOINT_POSITION_OUT_OF_BOUNDS);
         } else if (race.getStartPoint(player.getLocation()) != null) {
            MessageManager.sendMessage(commandSender, MessageKey.RACE_ADD_STARTPOINT_IS_OCCUPIED);
         } else {
            this.racingManager.addStartPoint(Util.centerOnBlockHorizontally(player.getLocation()), race, position, (startPoint) -> {
               MessageManager.setValue("race_name", (Object)race.getName());
               MessageManager.setValue("position", (Object)startPoint.getPosition());
               MessageManager.sendMessage(player, MessageKey.RACE_ADD_STARTPOINT_SUCCESS);
            });
         }
      }
   }
}
