package com.github.hornta.racing.commands;

import com.github.hornta.racing.MessageKey;
import com.github.hornta.racing.RacingManager;
import com.github.hornta.racing.Util;
import com.github.hornta.racing.commando.ICommandHandler;
import com.github.hornta.racing.enums.RaceState;
import com.github.hornta.racing.messenger.MessageManager;
import com.github.hornta.racing.objects.Race;
import com.github.hornta.racing.objects.RaceCheckpoint;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;

public class CommandMoveCheckpoint extends RacingCommand implements ICommandHandler {
   public CommandMoveCheckpoint(RacingManager racingManager) {
      super(racingManager);
   }

   public void handle(CommandSender commandSender, String[] args, int typedArgs) {
      Race race = this.racingManager.getRace(args[0]);
      RaceCheckpoint checkpoint = race.getCheckpoint(Integer.parseInt(args[1]));
      if (race.getState() != RaceState.UNDER_CONSTRUCTION) {
         MessageManager.setValue("race_name", (Object)race.getName());
         MessageManager.sendMessage(commandSender, MessageKey.EDIT_NO_EDIT_MODE);
      } else {
         Entity player = (Entity)commandSender;
         RaceCheckpoint occupied = race.getCheckpoint(player.getLocation());
         if (occupied == checkpoint) {
            MessageManager.sendMessage(commandSender, MessageKey.RACE_MOVE_CHECKPOINT_NO_CHANGE);
         } else if (occupied != null) {
            MessageManager.sendMessage(commandSender, MessageKey.RACE_ADD_CHECKPOINT_IS_OCCUPIED);
         } else {
            this.racingManager.moveCheckpoint(Util.centerOnBlock(player.getLocation()), race, checkpoint, () -> {
               MessageManager.setValue("race_name", (Object)race.getName());
               MessageManager.setValue("position", (Object)checkpoint.getPosition());
               MessageManager.setValue("x", (Object)checkpoint.getLocation().getX());
               MessageManager.setValue("x", (Object)checkpoint.getLocation().getY());
               MessageManager.setValue("x", (Object)checkpoint.getLocation().getZ());
               MessageManager.setValue("world", (Object)checkpoint.getLocation().getWorld().getName());
               MessageManager.sendMessage(player, MessageKey.RACE_MOVE_CHECKPOINT_SUCCESS);
            });
         }
      }
   }
}
