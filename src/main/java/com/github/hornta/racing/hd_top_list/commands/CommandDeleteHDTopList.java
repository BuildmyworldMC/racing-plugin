package com.github.hornta.racing.hd_top_list.commands;

import com.github.hornta.racing.MessageKey;
import com.github.hornta.racing.commando.ICommandHandler;
import com.github.hornta.racing.hd_top_list.HDTopListManager;
import com.github.hornta.racing.messenger.MessageManager;
import org.bukkit.command.CommandSender;

public class CommandDeleteHDTopList implements ICommandHandler {
   public void handle(CommandSender commandSender, String[] args, int i) {
      HDTopListManager.deleteTopList(args[0], (result) -> {
         if (result) {
            MessageManager.sendMessage(commandSender, MessageKey.HD_TOP_LIST_DELETE_SUCCESS);
         } else {
            MessageManager.sendMessage(commandSender, MessageKey.HD_TOP_LIST_DELETE_ERROR_FAIL_DELETE_FILE);
         }
      });
   }
}
