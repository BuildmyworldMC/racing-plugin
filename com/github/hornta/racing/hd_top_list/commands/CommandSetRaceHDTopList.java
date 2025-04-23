package com.github.hornta.racing.hd_top_list.commands;

import com.github.hornta.racing.MessageKey;
import com.github.hornta.racing.RacingPlugin;
import com.github.hornta.racing.commando.ICommandHandler;
import com.github.hornta.racing.hd_top_list.HDTopList;
import com.github.hornta.racing.hd_top_list.HDTopListManager;
import com.github.hornta.racing.messenger.MessageManager;
import com.github.hornta.racing.objects.Race;
import org.bukkit.command.CommandSender;

public class CommandSetRaceHDTopList implements ICommandHandler {
   public void handle(CommandSender commandSender, String[] args, int i) {
      HDTopList topList = HDTopListManager.getTopList(args[0]);
      Race race = RacingPlugin.getInstance().getRacingManager().getRace(args[1]);
      Race oldRace = topList.getRace();
      topList.setRace(race);
      RacingPlugin.getHdTopListManager().updateDirtyTopLists(() -> {
         MessageManager.setValue("old_race", (Object)oldRace.getName());
         MessageManager.setValue("new_race", (Object)race.getName());
         MessageManager.sendMessage(commandSender, MessageKey.HD_TOP_LIST_SET_RACE);
      });
   }
}
