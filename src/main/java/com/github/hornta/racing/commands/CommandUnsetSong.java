package com.github.hornta.racing.commands;

import com.github.hornta.racing.MessageKey;
import com.github.hornta.racing.RacingManager;
import com.github.hornta.racing.commando.ICommandHandler;
import com.github.hornta.racing.enums.RaceState;
import com.github.hornta.racing.messenger.MessageManager;
import com.github.hornta.racing.objects.Race;
import org.bukkit.command.CommandSender;

public class CommandUnsetSong extends RacingCommand implements ICommandHandler {
   public CommandUnsetSong(RacingManager racingManager) {
      super(racingManager);
   }

   public void handle(CommandSender commandSender, String[] args, int typedArgs) {
      Race race = this.racingManager.getRace(args[0]);
      if (race.getState() != RaceState.UNDER_CONSTRUCTION) {
         MessageManager.setValue("race_name", (Object)race.getName());
         MessageManager.sendMessage(commandSender, MessageKey.EDIT_NO_EDIT_MODE);
      } else if (race.getSong() == null) {
         MessageManager.sendMessage(commandSender, MessageKey.RACE_UNSET_SONG_ALREADY_UNSET);
      } else {
         race.setSong((String)null);
         this.racingManager.updateRace(race, () -> {
            MessageManager.sendMessage(commandSender, MessageKey.RACE_UNSET_SONG_SUCCESS);
         });
      }
   }
}
