package net.dv8tion.jda.api.events.message.guild.react;

import javax.annotation.Nonnull;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GenericGuildMessageEvent;

public class GuildMessageReactionRemoveAllEvent extends GenericGuildMessageEvent {
   public GuildMessageReactionRemoveAllEvent(@Nonnull JDA api, long responseNumber, long messageId, @Nonnull TextChannel channel) {
      super(api, responseNumber, messageId, channel);
   }
}
