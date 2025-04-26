package com.github.hornta.racing.commands;

import com.github.hornta.racing.MessageKey;
import com.github.hornta.racing.RacingManager;
import com.github.hornta.racing.commando.ICommandHandler;
import com.github.hornta.racing.enums.RaceState;
import com.github.hornta.racing.messenger.MessageManager;
import com.github.hornta.racing.objects.Race;
import org.bukkit.command.CommandSender;

public class CommandSetHorseJumpStrength extends RacingCommand implements ICommandHandler {
   public CommandSetHorseJumpStrength(RacingManager racingManager) {
      super(racingManager);
   }

   public void handle(CommandSender commandSender, String[] args, int typedArgs) {
      Race race = this.racingManager.getRace(args[0]);
      float jumpStrength = Float.parseFloat(args[1]);
      if (race.getState() != RaceState.UNDER_CONSTRUCTION) {
         MessageManager.setValue("race_name", (Object)race.getName());
         MessageManager.sendMessage(commandSender, MessageKey.EDIT_NO_EDIT_MODE);
      } else {
         race.setHorseJumpStrength((double)jumpStrength);
         this.racingManager.updateRace(race, () -> {
            MessageManager.setValue("jump_strength", (Object)jumpStrength);
            MessageManager.sendMessage(commandSender, MessageKey.RACE_SET_HORSE_JUMP_STRENGTH);
         });
      }
   }
}
