package com.github.hornta.racing;

import com.github.hornta.racing.events.ConfigReloadedEvent;
import com.github.hornta.racing.events.RaceSessionStartEvent;
import com.github.hornta.racing.messenger.MessageManager;
import java.util.Comparator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.security.auth.login.LoginException;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.ShutdownEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

public class DiscordManager implements Listener, EventListener {
   private JDA api;
   private TextChannel announceChannel;
   private boolean startAfterShutdown = false;

   DiscordManager() {
      if ((Boolean)RacingPlugin.getInstance().getConfiguration().get(ConfigKey.DISCORD_ENABLED)) {
         this.startup();
      }

   }

   private void startup() {
      this.startAfterShutdown = false;
      String token = (String)RacingPlugin.getInstance().getConfiguration().get(ConfigKey.DISCORD_TOKEN);

      try {
         this.api = JDABuilder.createDefault(token).addEventListeners(this).build();
      } catch (LoginException var3) {
         RacingPlugin.logger().log(Level.SEVERE, "Failed to integrate with Discord, the bot token was incorrect.");
      }

   }

   @EventHandler
   void onConfigReloaded(ConfigReloadedEvent event) {
      if (this.api != null) {
         this.api.shutdown();
      }

      if ((Boolean)RacingPlugin.getInstance().getConfiguration().get(ConfigKey.DISCORD_ENABLED)) {
         this.startAfterShutdown = true;
      }

   }

   @EventHandler
   void onRaceSessionStart(RaceSessionStartEvent event) {
      if ((Boolean)RacingPlugin.getInstance().getConfiguration().get(ConfigKey.DISCORD_ENABLED)) {
         MessageKey key;
         if (event.getRaceSession().getRace().getEntryFee() > 0.0D) {
            key = MessageKey.PARTICIPATE_DISCORD_FEE;
         } else {
            key = MessageKey.PARTICIPATE_DISCORD;
         }

         int prepareTime = (Integer)RacingPlugin.getInstance().getConfiguration().get(ConfigKey.RACE_PREPARE_TIME);
         Util.setTimeUnitValues();
         MessageManager.setValue("race_name", (Object)event.getRaceSession().getRace().getName());
         MessageManager.setValue("time_left", (Object)Util.getTimeLeft(prepareTime * 1000));
         MessageManager.setValue("laps", (Object)event.getRaceSession().getLaps());
         Economy economy = RacingPlugin.getInstance().getVaultEconomy();
         if (economy != null) {
            MessageManager.setValue("entry_fee", (Object)economy.format(event.getRaceSession().getRace().getEntryFee()));
         }

         this.announceChannel.sendMessage(MessageManager.getMessage(key)).queue();
      }
   }

   public void onEvent(@NotNull GenericEvent event) {
      if (event instanceof ShutdownEvent) {
         if (this.startAfterShutdown) {
            this.startup();
         }
      } else if (event instanceof ReadyEvent) {
         RacingPlugin.logger().log(Level.INFO, "Successful integration with Discord.");
         String channelId = (String)RacingPlugin.getInstance().getConfiguration().get(ConfigKey.DISCORD_ANNOUNCE_CHANNEL);
         if (channelId.isEmpty()) {
            this.announceChannel = (TextChannel)this.api.getTextChannels().stream().min(Comparator.comparingInt(GuildChannel::getPosition)).orElse((Object)null);
         } else {
            this.announceChannel = this.api.getTextChannelById(channelId);
         }

         if (this.announceChannel == null) {
            RacingPlugin.logger().log(Level.SEVERE, "Couldn't find Discord channel with id: " + channelId);
         } else {
            Logger var10000 = RacingPlugin.logger();
            Level var10001 = Level.INFO;
            String var10002 = this.announceChannel.getName();
            var10000.log(var10001, "Found Discord channel " + var10002 + " with id: " + this.announceChannel.getId());
         }
      }

   }
}
