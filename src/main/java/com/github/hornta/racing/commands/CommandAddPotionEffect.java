package com.github.hornta.racing.commands;

import com.github.hornta.racing.MessageKey;
import com.github.hornta.racing.RacingManager;
import com.github.hornta.racing.commando.ICommandHandler;
import com.github.hornta.racing.enums.RaceState;
import com.github.hornta.racing.messenger.MessageManager;
import com.github.hornta.racing.objects.Race;
import com.github.hornta.racing.objects.RacePotionEffect;
import org.bukkit.command.CommandSender;
import org.bukkit.potion.PotionEffectType;

public class CommandAddPotionEffect extends RacingCommand implements ICommandHandler {
   public CommandAddPotionEffect(RacingManager racingManager) {
      super(racingManager);
   }

   public void handle(CommandSender commandSender, String[] args, int typedArgs) {
      Race race = this.racingManager.getRace(args[0]);
      PotionEffectType type = PotionEffectType.getByName(args[1]);
      int amplifier = Integer.parseInt(args[2]);
      if (race.getState() != RaceState.UNDER_CONSTRUCTION) {
         MessageManager.setValue("race_name", race.getName());
         MessageManager.sendMessage(commandSender, MessageKey.EDIT_NO_EDIT_MODE);
      } else {
         race.addPotionEffect(new RacePotionEffect(type, amplifier));
         this.racingManager.updateRace(race, () -> {
            MessageManager.setValue("potion_effect", type.getName());
            MessageManager.setValue("amplifier", amplifier);
            MessageManager.setValue("race_name", race.getName());
            MessageManager.sendMessage(commandSender, MessageKey.RACE_ADD_POTION_EFFECT);
         });
      }
   }
}
