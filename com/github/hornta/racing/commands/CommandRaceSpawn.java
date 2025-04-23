package com.github.hornta.racing.commands;

import com.github.hornta.racing.MessageKey;
import com.github.hornta.racing.RacingManager;
import com.github.hornta.racing.Util;
import com.github.hornta.racing.commando.ICommandHandler;
import com.github.hornta.racing.enums.Permission;
import com.github.hornta.racing.enums.RaceState;
import com.github.hornta.racing.messenger.MessageManager;
import com.github.hornta.racing.objects.Race;
import io.papermc.lib.PaperLib;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

public class CommandRaceSpawn extends RacingCommand implements ICommandHandler {
   public CommandRaceSpawn(RacingManager racingManager) {
      super(racingManager);
   }

   public void handle(CommandSender commandSender, String[] args, int typedArgs) {
      Race race = this.racingManager.getRace(args[0]);
      if (race.getState() != RaceState.ENABLED && !commandSender.hasPermission(Permission.RACING_MODERATOR.toString())) {
         MessageManager.sendMessage(commandSender, MessageKey.RACE_SPAWN_NOT_ENABLED);
      } else {
         PaperLib.teleportAsync((Entity)commandSender, Util.snapAngles(race.getSpawn()), TeleportCause.COMMAND);
      }
   }
}
