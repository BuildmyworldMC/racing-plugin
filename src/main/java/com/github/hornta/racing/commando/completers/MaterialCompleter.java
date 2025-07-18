package com.github.hornta.racing.commando.completers;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;

public class MaterialCompleter implements IArgumentHandler {
   private final Set<String> items = Arrays.stream(Material.values()).map(Enum::name).collect(Collectors.toSet());

   MaterialCompleter() {
   }

   public Set<String> getItems(CommandSender sender, String argument, String[] prevArgs) {
      return this.items.stream().filter((name) -> name.toLowerCase(Locale.ENGLISH).startsWith(argument.toLowerCase(Locale.ENGLISH))).collect(Collectors.toCollection(LinkedHashSet::new));
   }

   public boolean test(Set<String> items, String argument) {
      return items.contains(argument);
   }
}
