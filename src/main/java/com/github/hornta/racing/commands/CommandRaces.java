package com.github.hornta.racing.commands;

import com.github.hornta.racing.MessageKey;
import com.github.hornta.racing.RacingManager;
import com.github.hornta.racing.commando.ICommandHandler;
import com.github.hornta.racing.messenger.MessageManager;
import com.github.hornta.racing.objects.Race;
import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.command.CommandSender;

public class CommandRaces extends RacingCommand implements ICommandHandler {
   public CommandRaces(RacingManager racingManager) {
      super(racingManager);
   }

   public void handle(CommandSender commandSender, String[] args, int typedArgs) {
      List<Race> races = this.racingManager.getRaces();
      MessageManager.setValue("races", races.stream().map((race) -> {
         MessageManager.setValue("race", race.getName());
         return MessageManager.getMessage(MessageKey.LIST_RACES_ITEM);
      }).collect(Collectors.joining("§f, ")));
      MessageManager.setValue("num_race", races.size());
      MessageManager.sendMessage(commandSender, MessageKey.LIST_RACES_LIST);
   }
}
