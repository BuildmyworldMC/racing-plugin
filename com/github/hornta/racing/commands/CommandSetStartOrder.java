package com.github.hornta.racing.commands;

import com.github.hornta.racing.MessageKey;
import com.github.hornta.racing.RacingManager;
import com.github.hornta.racing.commando.ICommandHandler;
import com.github.hornta.racing.enums.RaceState;
import com.github.hornta.racing.enums.StartOrder;
import com.github.hornta.racing.events.RaceChangeStateEvent;
import com.github.hornta.racing.messenger.MessageManager;
import com.github.hornta.racing.objects.Race;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

public class CommandSetStartOrder extends RacingCommand implements ICommandHandler {
   public CommandSetStartOrder(RacingManager racingManager) {
      super(racingManager);
   }

   public void handle(CommandSender commandSender, String[] args, int typedArgs) {
      Race race = this.racingManager.getRace(args[0]);
      StartOrder newOrder = StartOrder.fromString(args[1]);
      if (race.getState() != RaceState.UNDER_CONSTRUCTION) {
         MessageManager.setValue("race_name", (Object)race.getName());
         MessageManager.sendMessage(commandSender, MessageKey.EDIT_NO_EDIT_MODE);
      } else if (race.getStartOrder() == newOrder) {
         MessageManager.setValue("order", (Object)newOrder.name());
         MessageManager.sendMessage(commandSender, MessageKey.RACE_SET_START_ORDER_NOCHANGE);
      } else {
         StartOrder oldOrder = race.getStartOrder();
         race.setStartOrder(newOrder);
         this.racingManager.updateRace(race, () -> {
            Bukkit.getPluginManager().callEvent(new RaceChangeStateEvent(race));
            MessageManager.setValue("old_order", (Enum)oldOrder);
            MessageManager.setValue("new_order", (Enum)newOrder);
            MessageManager.sendMessage(commandSender, MessageKey.RACE_SET_START_ORDER_SUCCESS);
         });
      }
   }
}
