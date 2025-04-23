package com.github.hornta.racing.hd_top_list.commands;

import com.github.hornta.racing.commando.ICommandHandler;
import com.github.hornta.racing.hd_top_list.HDTopList;
import com.github.hornta.racing.hd_top_list.HDTopListManager;
import io.papermc.lib.PaperLib;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

public class CommandTeleportHDTopList implements ICommandHandler {
   public void handle(CommandSender commandSender, String[] args, int i) {
      HDTopList topList = HDTopListManager.getTopList(args[0]);
      Player player = (Player)commandSender;
      PaperLib.teleportAsync(player, topList.getHologram().getLocation(), TeleportCause.PLUGIN);
   }
}
