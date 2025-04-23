package net.dv8tion.jda.api.events.message.priv;

import javax.annotation.Nonnull;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.PrivateChannel;

public class PrivateMessageDeleteEvent extends GenericPrivateMessageEvent {
   public PrivateMessageDeleteEvent(@Nonnull JDA api, long responseNumber, long messageId, @Nonnull PrivateChannel channel) {
      super(api, responseNumber, messageId, channel);
   }
}
