package com.github.hornta.racing.commands;

import com.github.hornta.racing.MessageKey;
import com.github.hornta.racing.RacingManager;
import com.github.hornta.racing.commando.ICommandHandler;
import com.github.hornta.racing.enums.RaceState;
import com.github.hornta.racing.events.RaceChangeStateEvent;
import com.github.hornta.racing.messenger.MessageManager;
import com.github.hornta.racing.objects.Race;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

public class CommandSetRaceState extends RacingCommand implements ICommandHandler {
   public CommandSetRaceState(RacingManager racingManager) {
      super(racingManager);
   }

   public void handle(CommandSender commandSender, String[] args, int typedArgs) {
      Race race = this.racingManager.getRace(args[0]);
      RaceState newState = RaceState.fromString(args[1]);
      if (race.getState() == newState) {
         MessageManager.setValue("state", (Object)newState.name());
         MessageManager.sendMessage(commandSender, MessageKey.RACE_SET_STATE_NOCHANGE);
      } else if (race.getState() == RaceState.ENABLED && this.racingManager.hasOngoingSession(race)) {
         MessageManager.sendMessage(commandSender, MessageKey.RACE_SET_STATE_ONGOING);
      } else {
         RaceState oldState = race.getState();
         race.setState(newState);
         this.racingManager.updateRace(race, () -> {
            Bukkit.getPluginManager().callEvent(new RaceChangeStateEvent(race));
            MessageManager.setValue("old_state", (Enum)oldState);
            MessageManager.setValue("new_state", (Enum)newState);
            MessageManager.sendMessage(commandSender, MessageKey.RACE_SET_STATE_SUCCESS);
         });
      }
   }
}
