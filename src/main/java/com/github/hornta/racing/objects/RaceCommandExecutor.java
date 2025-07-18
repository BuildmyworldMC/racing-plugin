package com.github.hornta.racing.objects;

import com.github.hornta.racing.RacingPlugin;
import com.github.hornta.racing.enums.RaceCommandType;
import com.github.hornta.racing.events.ExecuteCommandEvent;
import com.github.hornta.racing.messenger.MessageManager;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class RaceCommandExecutor implements Listener {
   @EventHandler
   void onExecuteCommand(ExecuteCommandEvent event) {
      Race race = event.getRaceSession().getRace();
      List<RaceCommand> commands = race.getCommands().stream().filter((commandx) -> commandx.isEnabled() && commandx.getCommandType() == event.getCommandType()).toList();
      Iterator<RaceCommand> var4 = commands.iterator();

      while (true) {
         while(var4.hasNext()) {
            RaceCommand command = var4.next();
            if (command.getCommandType() != RaceCommandType.ON_RACE_FINISH && command.getCommandType() != RaceCommandType.ON_PLAYER_FINISH) {
               MessageManager.setValue("race", race.getName());
               this.dispatchCommand(command);
            } else if (command.getRecipient() == Integer.MIN_VALUE) {
               MessageManager.setValue("race", race.getName());
               this.dispatchCommand(command);
            } else {
               List<PlayerSessionResult> completedPlayerResults = event.getRaceSession().getResult().getPlayerResults().values().stream().filter(PlayerSessionResult::hasCompletedRace).sorted(Comparator.comparingLong(PlayerSessionResult::getRaceDuration)).toList();
               int recipientIndex;
               if (command.getRecipient() == 0) {
                  recipientIndex = 1;

                   for (PlayerSessionResult result : completedPlayerResults) {
                       OfflinePlayer player = Bukkit.getOfflinePlayer(result.getPlayerId());
                       MessageManager.setValue("player_name", player.getName());
                       MessageManager.setValue("position", recipientIndex);
                       ++recipientIndex;
                       MessageManager.setValue("time", result.getRaceDuration());
                       MessageManager.setValue("race", race.getName());
                       this.dispatchCommand(command);
                   }
               } else {
                  recipientIndex = command.getRecipient() - 1;
                  if (recipientIndex < completedPlayerResults.size()) {
                     PlayerSessionResult result = completedPlayerResults.get(recipientIndex);
                     OfflinePlayer player = Bukkit.getOfflinePlayer(result.getPlayerId());
                     MessageManager.setValue("player_name", player.getName());
                     MessageManager.setValue("position", recipientIndex + 1);
                     MessageManager.setValue("time", result.getRaceDuration());
                     MessageManager.setValue("race", race.getName());
                     this.dispatchCommand(command);
                  }
               }
            }
         }

         return;
      }
   }

   private void dispatchCommand(RaceCommand command) {
      String formattedCommand = MessageManager.getInstance().transformPlaceholders(command.getCommand());
      RacingPlugin.getInstance().getLogger().log(Level.INFO, "Dispatching command: " + formattedCommand);
      Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), formattedCommand);
   }
}
