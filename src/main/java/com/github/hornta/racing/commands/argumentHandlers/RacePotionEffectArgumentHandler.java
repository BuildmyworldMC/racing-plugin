package com.github.hornta.racing.commands.argumentHandlers;

import com.github.hornta.racing.MessageKey;
import com.github.hornta.racing.RacingManager;
import com.github.hornta.racing.commando.ValidationResult;
import com.github.hornta.racing.commando.completers.IArgumentHandler;
import com.github.hornta.racing.messenger.MessageManager;
import com.github.hornta.racing.objects.RacePotionEffect;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.bukkit.command.CommandSender;
import org.bukkit.potion.PotionEffectType;

public class RacePotionEffectArgumentHandler implements IArgumentHandler {
   private final RacingManager racingManager;

   public RacePotionEffectArgumentHandler(RacingManager racingManager) {
      this.racingManager = racingManager;
   }

   public Set<String> getItems(CommandSender sender, String argument, String[] prevArgs) {
      return this.racingManager.getRace(prevArgs[0]).getPotionEffects().stream().map(RacePotionEffect::getType).map(PotionEffectType::getName).map((s) -> s.toLowerCase(Locale.ENGLISH)).filter((type) -> type.startsWith(argument.toLowerCase(Locale.ENGLISH))).collect(Collectors.toCollection(LinkedHashSet::new));
   }

   public boolean test(Set<String> items, String argument) {
      return items.contains(argument.toLowerCase(Locale.ENGLISH));
   }

   public void whenInvalid(ValidationResult result) {
      MessageManager.setValue("race_name", result.getPrevArgs()[0]);
      MessageManager.setValue("potion_effect", result.getValue());
      MessageManager.sendMessage(result.getCommandSender(), MessageKey.RACE_POTION_EFFECT_NOT_FOUND);
   }
}
