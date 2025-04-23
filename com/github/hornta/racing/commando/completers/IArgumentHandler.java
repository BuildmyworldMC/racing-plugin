package com.github.hornta.racing.commando.completers;

import com.github.hornta.racing.commando.ValidationResult;
import java.util.Collections;
import java.util.Set;
import org.bukkit.command.CommandSender;

public interface IArgumentHandler {
   default Set<String> getItems(CommandSender sender, String argument, String[] prevArgs) {
      return Collections.emptySet();
   }

   default void whenInvalid(ValidationResult validationResult) {
   }

   default boolean test(Set<String> items, String argument) {
      return true;
   }
}
