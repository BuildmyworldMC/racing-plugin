package net.dv8tion.jda.api.events.channel.text;

import javax.annotation.Nonnull;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.TextChannel;

public class TextChannelDeleteEvent extends GenericTextChannelEvent {
   public TextChannelDeleteEvent(@Nonnull JDA api, long responseNumber, @Nonnull TextChannel channel) {
      super(api, responseNumber, channel);
   }
}
