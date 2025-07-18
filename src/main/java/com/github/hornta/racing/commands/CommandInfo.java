package com.github.hornta.racing.commands;

import com.github.hornta.racing.ConfigKey;
import com.github.hornta.racing.MessageKey;
import com.github.hornta.racing.RacingManager;
import com.github.hornta.racing.RacingPlugin;
import com.github.hornta.racing.commando.ICommandHandler;
import com.github.hornta.racing.messenger.MessageManager;
import com.github.hornta.racing.objects.Race;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.bukkit.command.CommandSender;

public class CommandInfo extends RacingCommand implements ICommandHandler {
   public CommandInfo(RacingManager racingManager) {
      super(racingManager);
   }

   public static void sendInfoMessage(CommandSender target, Race race) {
      List<String> potionEffects = race.getPotionEffects().stream().map((effect) -> {
         MessageManager.setValue("potion_effect", effect.getType().getName());
         MessageManager.setValue("amplifier", effect.getAmplifier());
         return MessageManager.getMessage(MessageKey.RACE_INFO_POTION_EFFECT);
      }).collect(Collectors.toList());

      String entryFee = "";

      if (RacingPlugin.getInstance().getVaultEconomy() != null) {
         MessageManager.setValue("entry_fee", RacingPlugin.getInstance().getVaultEconomy().format(race.getEntryFee()));
         entryFee = MessageManager.getMessage(MessageKey.RACE_INFO_ENTRY_FEE_LINE);
      }

      String noPotionEffects = race.getPotionEffects().isEmpty() ? MessageManager.getMessage(MessageKey.RACE_INFO_NO_POTION_EFFECTS) : "";
      MessageManager.setValue("name", race.getName());
      MessageManager.setValue("type", race.getType().name());
      MessageManager.setValue("state", race.getState().name());
      String localeString = RacingPlugin.getInstance().getConfiguration().get(ConfigKey.LOCALE);
      localeString = localeString.toUpperCase(Locale.ENGLISH);
      Locale locale = new Locale(localeString);
      DateTimeFormatter createdFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL).withLocale(locale).withZone(ZoneId.systemDefault());
      MessageManager.setValue("created", createdFormatter.format(race.getCreatedAt()));
      MessageManager.setValue("num_startpoints", race.getStartPoints().size());
      MessageManager.setValue("num_checkpoints", race.getCheckpoints().size());
      MessageManager.setValue("entry_fee", entryFee);
      MessageManager.setValue("walk_speed", race.getWalkSpeed());
      MessageManager.setValue("none", noPotionEffects);
      MessageManager.setValue("potion_effects", potionEffects);
      MessageManager.sendMessage(target, MessageKey.RACE_INFO_SUCCESS);
   }

   public void handle(CommandSender commandSender, String[] args, int typedArgs) {
      Race race = this.racingManager.getRace(args[0]);
      sendInfoMessage(commandSender, race);
   }
}
