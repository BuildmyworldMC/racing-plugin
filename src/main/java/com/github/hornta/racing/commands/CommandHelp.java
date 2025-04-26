package com.github.hornta.racing.commands;

import com.github.hornta.racing.MessageKey;
import com.github.hornta.racing.RacingPlugin;
import com.github.hornta.racing.commando.ICommandHandler;
import com.github.hornta.racing.messenger.MessageManager;
import java.util.Iterator;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandHelp implements ICommandHandler {
   public void handle(CommandSender commandSender, String[] args, int typedArgs) {
      Player player = null;
      if (commandSender instanceof Player) {
         player = (Player)commandSender;
      }

      List<String> helpTexts = RacingPlugin.getInstance().getCommando().getHelpTexts(player);
      MessageManager.sendMessage(commandSender, MessageKey.RACE_HELP_TITLE);
      Iterator var6 = helpTexts.iterator();

      while(var6.hasNext()) {
         String helpText = (String)var6.next();
         MessageManager.setValue("text", (Object)helpText);
         MessageManager.sendMessage(commandSender, MessageKey.RACE_HELP_ITEM);
      }

   }
}
