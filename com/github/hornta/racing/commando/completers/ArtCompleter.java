package com.github.hornta.racing.commando.completers;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.bukkit.Art;
import org.bukkit.command.CommandSender;

public class ArtCompleter implements IArgumentHandler {
   public Set<String> getItems(CommandSender sender, String argument, String[] prevArgs) {
      return (Set)Arrays.stream(Art.values()).map(Enum::name).filter((name) -> {
         return name.toLowerCase(Locale.ENGLISH).startsWith(argument.toLowerCase(Locale.ENGLISH));
      }).collect(Collectors.toCollection(LinkedHashSet::new));
   }

   public boolean test(Set<String> items, String argument) {
      return items.contains(argument);
   }
}
