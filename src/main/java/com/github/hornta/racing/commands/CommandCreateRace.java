package com.github.hornta.racing.commands;

import com.github.hornta.racing.MessageKey;
import com.github.hornta.racing.RacingManager;
import com.github.hornta.racing.Util;
import com.github.hornta.racing.commando.ICommandHandler;
import com.github.hornta.racing.enums.RaceState;
import com.github.hornta.racing.enums.RaceType;
import com.github.hornta.racing.enums.RaceVersion;
import com.github.hornta.racing.enums.StartOrder;
import com.github.hornta.racing.messenger.MessageManager;
import com.github.hornta.racing.objects.Race;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.UUID;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;

public class CommandCreateRace extends RacingCommand implements ICommandHandler {
   public CommandCreateRace(RacingManager racingManager) {
      super(racingManager);
   }

   public void handle(CommandSender commandSender, String[] args, int typedArgs) {
      Entity player = (Entity)commandSender;
      Race race = new Race(UUID.randomUUID(),
              RaceVersion.getLast(),
              args[0],
              Util.centerOnBlockHorizontally(player.getLocation()),
              RaceState.UNDER_CONSTRUCTION,
              Instant.now(),
              Collections.emptyList(),
              Collections.emptyList(),
              RaceType.PLAYER,
              StartOrder.RANDOM,
              null,
              0.0D,
              0.2F,
              new HashSet<>(),
              new HashSet<>(),
              Collections.emptySet(),
              1,
              0.25D,
              0.175D,
              0.225D,
              0.7D,
              128D,
              0.180D,
              0.3D,
              1.25D,
              Collections.emptyList());
      this.racingManager.createRace(race, () -> {
         MessageManager.setValue("race_name", race.getName());
         MessageManager.sendMessage(player, MessageKey.CREATE_RACE_SUCCESS);
      });
   }
}
