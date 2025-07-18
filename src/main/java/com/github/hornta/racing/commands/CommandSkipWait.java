package com.github.hornta.racing.commands;

import com.github.hornta.racing.MessageKey;
import com.github.hornta.racing.RacingManager;
import com.github.hornta.racing.commando.ICommandHandler;
import com.github.hornta.racing.enums.RaceSessionState;
import com.github.hornta.racing.messenger.MessageManager;
import com.github.hornta.racing.objects.Race;
import com.github.hornta.racing.objects.RaceSession;
import java.util.Iterator;
import java.util.List;
import org.bukkit.command.CommandSender;

public class CommandSkipWait extends RacingCommand implements ICommandHandler {
   public CommandSkipWait(RacingManager racingManager) {
      super(racingManager);
   }

   public void handle(CommandSender commandSender, String[] args, int typedArgs) {
      Race race = this.racingManager.getRace(args[0]);
      List<RaceSession> sessions = this.racingManager.getRaceSessions(race, RaceSessionState.PREPARING);
      if (sessions.isEmpty()) {
         MessageManager.sendMessage(commandSender, MessageKey.RACE_SKIP_WAIT_NOT_STARTED);
      } else {
          for (RaceSession session : sessions) {
              session.skipToCountdown();
          }
      }
   }
}
