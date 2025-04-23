package com.github.hornta.racing.commando.completers;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;

public class MaterialCompleter implements IArgumentHandler {
   private final Set<String> items = (Set)Arrays.stream(Material.values()).map(Enum::name).collect(Collectors.toSet());

   MaterialCompleter() {
   }

   public Set<String> getItems(CommandSender sender, String argument, String[] prevArgs) {
      return (Set)this.items.stream().filter((name) -> {
         return name.toLowerCase(Locale.ENGLISH).startsWith(argument.toLowerCase(Locale.ENGLISH));
      }).collect(Collectors.toCollection(LinkedHashSet::new));
   }

   public boolean test(Set<String> items, String argument) {
      return items.contains(argument);
   }
}
