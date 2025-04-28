package com.github.hornta.racing.hd_top_list.commands;

import com.github.hornta.racing.MessageKey;
import com.github.hornta.racing.RacingPlugin;
import com.github.hornta.racing.commando.ICommandHandler;
import com.github.hornta.racing.enums.RaceStatType;
import com.github.hornta.racing.hd_top_list.HDTopListManager;
import com.github.hornta.racing.messenger.MessageManager;
import com.github.hornta.racing.objects.Race;
import java.util.Locale;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;

public class CommandCreateHDTopList implements ICommandHandler {
   public void handle(CommandSender commandSender, String[] args, int i) {
      String topListName = args[0];
      Race race = RacingPlugin.getInstance().getRacingManager().getRace(args[1]);
      RaceStatType statType = RaceStatType.valueOf(args[2].toUpperCase(Locale.ENGLISH));
      int laps = Integer.parseInt(args[3]);
      Entity entitySender = (Entity)commandSender;
      HDTopListManager.createTopList(topListName, entitySender.getLocation(), race, statType, laps, (result) -> {
         if (result) {
            MessageManager.sendMessage(commandSender, MessageKey.HD_TOP_LIST_CREATE_SUCCESS);
         } else {
            MessageManager.sendMessage(commandSender, MessageKey.HD_TOP_LIST_CREATE_ERROR_NO_PERSIST);
         }

      });
   }
}
