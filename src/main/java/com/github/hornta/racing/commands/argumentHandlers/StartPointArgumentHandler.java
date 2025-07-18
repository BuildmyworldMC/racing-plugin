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

public class StartPointArgumentHandler implements IArgumentHandler {
   private final RacingManager racingManager;
   private final boolean shouldExist;

   public StartPointArgumentHandler(RacingManager racingManager, boolean shouldExist) {
      this.racingManager = racingManager;
      this.shouldExist = shouldExist;
   }

   public Set<String> getItems(CommandSender sender, String argument, String[] prevArgs) {
      return this.racingManager.getRace(prevArgs[0]).getStartPoints().stream().filter((point) -> String.valueOf(point.getPosition()).toLowerCase(Locale.ENGLISH).startsWith(argument.toLowerCase(Locale.ENGLISH))).map((p) -> String.valueOf(p.getPosition())).collect(Collectors.toCollection(LinkedHashSet::new));
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
         key = MessageKey.STARTPOINT_NOT_FOUND;
      } else {
         key = MessageKey.STARTPOINT_ALREADY_EXIST;
      }

      MessageManager.setValue("race_name", result.getPrevArgs()[0]);
      MessageManager.setValue("position", result.getValue());
      MessageManager.sendMessage(result.getCommandSender(), key);
   }
}
