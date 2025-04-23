package net.dv8tion.jda.api.events.channel.text;

import javax.annotation.Nonnull;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.TextChannel;

public class TextChannelCreateEvent extends GenericTextChannelEvent {
   public TextChannelCreateEvent(@Nonnull JDA api, long responseNumber, @Nonnull TextChannel channel) {
      super(api, responseNumber, channel);
   }
}
