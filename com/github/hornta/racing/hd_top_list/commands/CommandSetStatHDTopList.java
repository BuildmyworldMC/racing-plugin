package com.github.hornta.racing.hd_top_list.commands;

import com.github.hornta.racing.MessageKey;
import com.github.hornta.racing.RacingPlugin;
import com.github.hornta.racing.commando.ICommandHandler;
import com.github.hornta.racing.enums.RaceStatType;
import com.github.hornta.racing.hd_top_list.HDTopList;
import com.github.hornta.racing.hd_top_list.HDTopListManager;
import com.github.hornta.racing.messenger.MessageManager;
import java.util.Locale;
import org.bukkit.command.CommandSender;

public class CommandSetStatHDTopList implements ICommandHandler {
   public void handle(CommandSender commandSender, String[] args, int i) {
      HDTopList topList = HDTopListManager.getTopList(args[0]);
      RaceStatType statType = RaceStatType.valueOf(args[1].toUpperCase(Locale.ENGLISH));
      RaceStatType oldStat = topList.getStatType();
      topList.setStatType(statType);
      RacingPlugin.getHdTopListManager().updateDirtyTopLists(() -> {
         MessageManager.setValue("old_stat", (Object)oldStat.name());
         MessageManager.setValue("new_stat", (Object)statType.name());
         MessageManager.sendMessage(commandSender, MessageKey.HD_TOP_LIST_SET_STAT);
      });
   }
}
