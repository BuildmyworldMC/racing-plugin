package com.github.hornta.racing.commands;

import com.github.hornta.racing.ConfigKey;
import com.github.hornta.racing.MessageKey;
import com.github.hornta.racing.RacingManager;
import com.github.hornta.racing.RacingPlugin;
import com.github.hornta.racing.commando.ICommandHandler;
import com.github.hornta.racing.messenger.MessageManager;
import com.github.hornta.racing.objects.Race;
import com.github.hornta.racing.objects.RaceSession;
import java.util.Iterator;
import java.util.List;
import org.bukkit.command.CommandSender;

public class CommandStopRace extends RacingCommand implements ICommandHandler {
   public CommandStopRace(RacingManager racingManager) {
      super(racingManager);
   }

   public void handle(CommandSender commandSender, String[] args, int typedArgs) {
      Race race = this.racingManager.getRace(args[0]);
      List<RaceSession> sessions = this.racingManager.getRaceSessions(race);
      if (sessions.isEmpty()) {
         MessageManager.sendMessage(commandSender, MessageKey.STOP_RACE_NOT_STARTED);
      } else {
         for (RaceSession session : sessions) {
            session.stop();
         }

         if ((Boolean)RacingPlugin.getInstance().getConfiguration().get(ConfigKey.BROADCAST_STOP_MESSAGE)) {
            MessageManager.broadcast(MessageKey.STOP_RACE_SUCCESS);
         } else {
            MessageManager.sendMessage(commandSender, MessageKey.STOP_RACE_SUCCESS);
         }
      }
   }
}
