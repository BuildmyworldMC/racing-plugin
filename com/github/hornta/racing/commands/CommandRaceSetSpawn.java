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

public class CommandRaceSetSpawn extends RacingCommand implements ICommandHandler {
   public CommandRaceSetSpawn(RacingManager racingManager) {
      super(racingManager);
   }

   public void handle(CommandSender commandSender, String[] args, int typedArgs) {
      Race race = this.racingManager.getRace(args[0]);
      if (race.getState() != RaceState.UNDER_CONSTRUCTION) {
         MessageManager.setValue("race_name", (Object)race.getName());
         MessageManager.sendMessage(commandSender, MessageKey.EDIT_NO_EDIT_MODE);
      } else {
         race.setSpawn(Util.centerOnBlockHorizontally(((Entity)commandSender).getLocation()));
         this.racingManager.updateRace(race, () -> {
            MessageManager.setValue("race_name", (Object)race.getName());
            MessageManager.setValue("x", (Object)race.getSpawn().getX());
            MessageManager.setValue("y", (Object)race.getSpawn().getY());
            MessageManager.setValue("z", (Object)race.getSpawn().getZ());
            MessageManager.setValue("world", (Object)race.getSpawn().getWorld().getName());
            MessageManager.sendMessage(commandSender, MessageKey.RACE_SET_SPAWN_SUCCESS);
         });
      }
   }
}
