package com.github.hornta.racing.commands.argumentHandlers;

import com.github.hornta.racing.MessageKey;
import com.github.hornta.racing.RacingManager;
import com.github.hornta.racing.commando.ValidationResult;
import com.github.hornta.racing.commando.completers.IArgumentHandler;
import com.github.hornta.racing.messenger.MessageManager;
import com.github.hornta.racing.objects.Race;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.bukkit.command.CommandSender;

public class RaceArgumentHandler implements IArgumentHandler {
   private final RacingManager racingManager;
   private final Predicate<Race> filter;
   private final boolean shouldExist;

   public RaceArgumentHandler(RacingManager racingManager, Predicate<Race> filter) {
      this.racingManager = racingManager;
      this.filter = filter;
      this.shouldExist = true;
   }

   public RaceArgumentHandler(RacingManager racingManager, Predicate<Race> filter, boolean shouldExist) {
      this.racingManager = racingManager;
      this.filter = filter;
      this.shouldExist = shouldExist;
   }

   public RaceArgumentHandler(RacingManager racingManager, boolean shouldExist) {
      this.racingManager = racingManager;
      this.filter = null;
      this.shouldExist = shouldExist;
   }

   public Set<String> getItems(CommandSender sender, String argument, String[] prevArgs) {
      return this.racingManager.getRaces().stream().filter((race) -> race.getName().toLowerCase(Locale.ENGLISH).startsWith(argument.toLowerCase(Locale.ENGLISH))).filter((r) -> this.filter == null || this.filter.test(r)).map(Race::getName).collect(Collectors.toCollection(LinkedHashSet::new));
   }

   public boolean test(Set<String> items, String argument) {
      if (this.shouldExist) {
         return items.contains(argument);
      } else {
         return !items.contains(argument);
      }
   }

   public void whenInvalid(ValidationResult result) {
      MessageKey message;
      if (this.shouldExist) {
         message = MessageKey.RACE_NOT_FOUND;
      } else {
         message = MessageKey.RACE_ALREADY_EXIST;
      }

      MessageManager.setValue("race_name", result.getValue());
      MessageManager.sendMessage(result.getCommandSender(), message);
   }
}
