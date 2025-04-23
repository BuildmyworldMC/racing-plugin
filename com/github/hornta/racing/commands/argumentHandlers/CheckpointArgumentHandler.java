package com.github.hornta.racing.commands.argumentHandlers;

import com.github.hornta.racing.MessageKey;
import com.github.hornta.racing.RacingManager;
import com.github.hornta.racing.commando.ValidationResult;
import com.github.hornta.racing.commando.completers.IArgumentHandler;
import com.github.hornta.racing.messenger.MessageManager;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.bukkit.command.CommandSender;

public class CheckpointArgumentHandler implements IArgumentHandler {
   private final RacingManager racingManager;
   private final boolean shouldExist;

   public CheckpointArgumentHandler(RacingManager racingManager, boolean shouldExist) {
      this.racingManager = racingManager;
      this.shouldExist = shouldExist;
   }

   public Set<String> getItems(CommandSender sender, String argument, String[] prevArgs) {
      return (Set)this.racingManager.getRace(prevArgs[0]).getCheckpoints().stream().filter((race) -> {
         return String.valueOf(race.getPosition()).toLowerCase(Locale.ENGLISH).startsWith(argument.toLowerCase(Locale.ENGLISH));
      }).map((cp) -> {
         return String.valueOf(cp.getPosition());
      }).collect(Collectors.toCollection(LinkedHashSet::new));
   }

   public boolean test(Set<String> items, String argument) {
      if (this.shouldExist) {
         return items.contains(argument);
      } else {
         return !items.contains(argument);
      }
   }

   public void whenInvalid(ValidationResult result) {
      MessageKey key;
      if (this.shouldExist) {
         key = MessageKey.CHECKPOINT_NOT_FOUND;
      } else {
         key = MessageKey.CHECKPOINT_ALREADY_EXIST;
      }

      MessageManager.setValue("race_name", (Object)result.getPrevArgs()[0]);
      MessageManager.setValue("position", (Object)result.getValue());
      MessageManager.sendMessage(result.getCommandSender(), key);
   }
}
