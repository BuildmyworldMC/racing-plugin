package com.github.hornta.racing.commando;

import org.bukkit.command.CommandSender;

@FunctionalInterface
public interface ICommandHandler {
   void handle(CommandSender var1, String[] var2, int var3);
}
