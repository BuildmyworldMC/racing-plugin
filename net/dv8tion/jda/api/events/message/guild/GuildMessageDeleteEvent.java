package net.dv8tion.jda.api.events.message.guild;

import javax.annotation.Nonnull;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.TextChannel;

public class GuildMessageDeleteEvent extends GenericGuildMessageEvent {
   public GuildMessageDeleteEvent(@Nonnull JDA api, long responseNumber, long messageId, @Nonnull TextChannel channel) {
      super(api, responseNumber, messageId, channel);
   }
}
