package com.github.hornta.racing.hd_top_list.commands;

import com.github.hornta.racing.MessageKey;
import com.github.hornta.racing.commando.ICommandHandler;
import com.github.hornta.racing.hd_top_list.HDTopList;
import com.github.hornta.racing.hd_top_list.HDTopListManager;
import com.github.hornta.racing.messenger.MessageManager;
import java.util.Iterator;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.ClickEvent.Action;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandListHDTopList implements ICommandHandler {
   public void handle(CommandSender commandSender, String[] args, int i) {
      MessageManager.sendMessage(commandSender, MessageKey.HD_TOP_LIST_LIST_HEADER);
      Iterator var4 = HDTopListManager.getTopLists().iterator();

      while(var4.hasNext()) {
         HDTopList topList = (HDTopList)var4.next();
         TextComponent tc = new TextComponent();
         MessageManager.setValue("top_list", (Object)topList.getName());
         ClickEvent teleoportClick = new ClickEvent(Action.RUN_COMMAND, MessageManager.getMessage(MessageKey.HD_TOP_LIST_LIST_TELEPORT_CLICK));
         HoverEvent teleportHover = new HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, (new ComponentBuilder(MessageManager.getMessage(MessageKey.HD_TOP_LIST_LIST_TELEPORT_HOVER))).create());
         MessageManager.setValue("top_list", (Object)topList.getName());
         if (commandSender instanceof Player) {
            tc.addExtra((new ComponentBuilder(MessageManager.getMessage(MessageKey.HD_TOP_LIST_LIST_ITEM))).event(teleportHover).event(teleoportClick).create()[0]);
         } else {
            tc.addExtra(MessageManager.getMessage(MessageKey.HD_TOP_LIST_LIST_ITEM));
         }

         MessageManager.setValue("stat", (Object)topList.getStatType().getFormattedStat(topList.getLaps()));
         MessageManager.setValue("race", (Object)topList.getRace().getName());
         tc.addExtra(MessageManager.getMessage(MessageKey.HD_TOP_LIST_LIST_ITEM_INFO));
         commandSender.spigot().sendMessage(tc);
      }

   }
}
