package net.dv8tion.jda.api.events.message.priv;

import javax.annotation.Nonnull;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;

public class PrivateMessageUpdateEvent extends GenericPrivateMessageEvent {
   private final Message message;

   public PrivateMessageUpdateEvent(@Nonnull JDA api, long responseNumber, @Nonnull Message message) {
      super(api, responseNumber, message.getIdLong(), message.getPrivateChannel());
      this.message = message;
   }

   @Nonnull
   public Message getMessage() {
      return this.message;
   }

   @Nonnull
   public User getAuthor() {
      return this.message.getAuthor();
   }
}
