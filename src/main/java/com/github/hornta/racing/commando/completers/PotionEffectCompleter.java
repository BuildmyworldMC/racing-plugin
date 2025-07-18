package com.github.hornta.racing.commando.completers;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.bukkit.command.CommandSender;
import org.bukkit.potion.PotionEffectType;

public class PotionEffectCompleter implements IArgumentHandler {
   private final Set<String> items = Arrays.stream(PotionEffectType.class.getDeclaredFields()).map(Field::getName).filter((name) -> {
      for(int i = 0; i < name.length(); ++i) {
         if (Character.isLowerCase(name.charAt(i))) {
            return false;
         }
      }

      return true;
   }).collect(Collectors.toCollection(LinkedHashSet::new));

   public Set<String> getItems(CommandSender sender, String argument, String[] prevArgs) {
      return this.items.stream().filter((name) -> name.toLowerCase(Locale.ENGLISH).startsWith(argument.toLowerCase(Locale.ENGLISH))).collect(Collectors.toSet());
   }

   public boolean test(Set<String> items, String argument) {
      return items.contains(argument);
   }
}
