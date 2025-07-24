package com.github.hornta.racing.commands;

import com.github.hornta.racing.ConfigKey;
import com.github.hornta.racing.MessageKey;
import com.github.hornta.racing.RacingPlugin;
import com.github.hornta.racing.api.ParseRaceException;
import com.github.hornta.racing.commando.ICommandHandler;
import com.github.hornta.racing.events.ConfigReloadedEvent;
import com.github.hornta.racing.messenger.MessageManager;
import com.github.hornta.racing.messenger.MessengerException;
import com.github.hornta.racing.messenger.Translation;
import com.github.hornta.racing.versioned_config.ConfigurationException;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

public class CommandReload implements ICommandHandler {
   public void handle(CommandSender commandSender, String[] args, int typedArgs) {
      try {
         RacingPlugin.getInstance().getConfiguration().reload();
      } catch (ConfigurationException configurationException) {
         MessageManager.setValue("reason", configurationException.getMessage());
         MessageManager.sendMessage(commandSender, MessageKey.RELOAD_FAILED);
         return;
      }

      Translation translation;
      try {
         RacingPlugin.getInstance().getTranslations().saveDefaults();
         translation = RacingPlugin.getInstance().getTranslations().createTranslation((String)RacingPlugin.getInstance().getConfiguration().get(ConfigKey.LANGUAGE));
      } catch (MessengerException messengerException) {
         MessageManager.setValue("reason", messengerException.getMessage());
         MessageManager.sendMessage(commandSender, MessageKey.RELOAD_MESSAGES_FAILED);
         return;
      }

      MessageManager.getInstance().setTranslation(translation);
      if (!RacingPlugin.getInstance().getRacingManager().getRaceSessions().isEmpty()) {
         MessageManager.sendMessage(commandSender, MessageKey.RELOAD_NOT_RACES);
      } else {
         try {
            RacingPlugin.getInstance().getRacingManager().load();
         } catch (ParseRaceException parseRaceException) {
            MessageManager.setValue("error", parseRaceException.getMessage());
            MessageManager.sendMessage(commandSender, MessageKey.RELOAD_RACES_FAILED);
            return;
         }
      }

      Bukkit.getPluginManager().callEvent(new ConfigReloadedEvent());
      MessageManager.sendMessage(commandSender, MessageKey.RELOAD_SUCCESS);
   }
}
