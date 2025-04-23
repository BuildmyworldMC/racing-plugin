package com.github.hornta.racing.commando.completers;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.bukkit.command.CommandSender;

public class BooleanEffectCompleter implements IArgumentHandler {
   private static final Set<String> items = new HashSet();

   public Set<String> getItems(CommandSender sender, String argument, String[] prevArgs) {
      return (Set)items.stream().filter((name) -> {
         return name.toLowerCase(Locale.ENGLISH).startsWith(argument.toLowerCase(Locale.ENGLISH));
      }).collect(Collectors.toCollection(LinkedHashSet::new));
   }

   public boolean test(Set<String> items, String argument) {
      return argument.contains(argument);
   }

   static {
      items.add("true");
      items.add("false");
   }
}
