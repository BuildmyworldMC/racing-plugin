package net.dv8tion.jda.api.events.message.guild;

import java.util.List;
import javax.annotation.Nonnull;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;

public class GuildMessageEmbedEvent extends GenericGuildMessageEvent {
   private final List<MessageEmbed> embeds;

   public GuildMessageEmbedEvent(@Nonnull JDA api, long responseNumber, long messageId, @Nonnull TextChannel channel, @Nonnull List<MessageEmbed> embeds) {
      super(api, responseNumber, messageId, channel);
      this.embeds = embeds;
   }

   @Nonnull
   public List<MessageEmbed> getMessageEmbeds() {
      return this.embeds;
   }
}
