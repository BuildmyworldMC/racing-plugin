package com.github.hornta.racing.commands;

import com.github.hornta.racing.SongManager;
import com.github.hornta.racing.commando.ICommandHandler;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandPlaySong implements ICommandHandler {
   public void handle(CommandSender sender, String[] args, int typedArgs) {
      SongManager.playSong(args[0], (Player)sender);
   }
}
