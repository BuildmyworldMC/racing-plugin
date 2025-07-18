package com.github.hornta.racing.commando.completers;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.command.CommandSender;

public class WorldNormalCompleter implements IArgumentHandler {
   public Set<String> getItems(CommandSender sender, String argument, String[] prevArgs) {
      return Bukkit.getWorlds().stream().filter((world) -> world.getEnvironment() == Environment.NORMAL).map(World::getName).filter((name) -> name.toLowerCase(Locale.ENGLISH).startsWith(argument.toLowerCase(Locale.ENGLISH))).collect(Collectors.toCollection(LinkedHashSet::new));
   }

   public boolean test(Set<String> items, String argument) {
      return items.contains(argument);
   }
}
