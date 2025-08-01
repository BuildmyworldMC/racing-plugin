package com.github.hornta.racing.commands.argumentHandlers;

import com.github.hornta.racing.MessageKey;
import com.github.hornta.racing.commando.ValidationResult;
import com.github.hornta.racing.commando.completers.IArgumentHandler;
import com.github.hornta.racing.enums.StartOrder;
import com.github.hornta.racing.messenger.MessageManager;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.bukkit.command.CommandSender;

public class StartOrderArgumentHandler implements IArgumentHandler {
   public Set<String> getItems(CommandSender sender, String argument, String[] prevArgs) {
      return Arrays.stream(StartOrder.values()).map(Enum::name).map((s) ->
         s.toLowerCase(Locale.ENGLISH)).filter((state) ->
            state.startsWith(argument.toLowerCase(Locale.ENGLISH))).collect(Collectors.toCollection(LinkedHashSet::new));
   }

   public boolean test(Set<String> items, String argument) {
      return items.contains(argument.toLowerCase(Locale.ENGLISH));
   }

   public void whenInvalid(ValidationResult result) {
      MessageManager.setValue("order", result.getValue());
      MessageManager.setValue("orders", Arrays.stream(StartOrder.values()).map(Enum::name).collect(Collectors.joining(", ")));
      MessageManager.sendMessage(result.getCommandSender(), MessageKey.START_ORDER_NOT_FOUND);
   }
}
