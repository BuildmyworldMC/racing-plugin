package com.github.hornta.racing.commands.argumentHandlers;

import com.github.hornta.racing.MessageKey;
import com.github.hornta.racing.commando.ValidationResult;
import com.github.hornta.racing.commando.completers.IArgumentHandler;
import com.github.hornta.racing.enums.RaceStatType;
import com.github.hornta.racing.messenger.MessageManager;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.bukkit.command.CommandSender;

public class RaceStatArgumentHandler implements IArgumentHandler {
   public Set<String> getItems(CommandSender sender, String argument, String[] prevArgs) {
      return (Set)Arrays.stream(RaceStatType.values()).map(Enum::name).map((s) -> {
         return s.toLowerCase(Locale.ENGLISH);
      }).filter((type) -> {
         return type.startsWith(argument.toLowerCase(Locale.ENGLISH));
      }).collect(Collectors.toCollection(LinkedHashSet::new));
   }

   public boolean test(Set<String> items, String argument) {
      return items.contains(argument.toLowerCase(Locale.ENGLISH));
   }

   public void whenInvalid(ValidationResult result) {
      MessageManager.setValue("stat_type", (Object)result.getValue());
      MessageManager.setValue("stat_types", Arrays.stream(RaceStatType.values()).map(Enum::name).collect(Collectors.joining(", ")));
      MessageManager.sendMessage(result.getCommandSender(), MessageKey.STAT_TYPE_NOT_FOUND);
   }
}
