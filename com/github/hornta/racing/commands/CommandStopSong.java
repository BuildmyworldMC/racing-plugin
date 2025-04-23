package com.github.hornta.racing.commands;

import com.github.hornta.racing.SongManager;
import com.github.hornta.racing.commando.ICommandHandler;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandStopSong implements ICommandHandler {
   public void handle(CommandSender sender, String[] args, int typedArgs) {
      SongManager.stopSong((Player)sender);
   }
}
