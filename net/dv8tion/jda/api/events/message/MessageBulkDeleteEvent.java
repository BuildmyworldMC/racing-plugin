package net.dv8tion.jda.api.events.message;

import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.Event;

public class MessageBulkDeleteEvent extends Event {
   protected final TextChannel channel;
   protected final List<String> messageIds;

   public MessageBulkDeleteEvent(@Nonnull JDA api, long responseNumber, @Nonnull TextChannel channel, @Nonnull List<String> messageIds) {
      super(api, responseNumber);
      this.channel = channel;
      this.messageIds = Collections.unmodifiableList(messageIds);
   }

   @Nonnull
   public TextChannel getChannel() {
      return this.channel;
   }

   @Nonnull
   public Guild getGuild() {
      return this.channel.getGuild();
   }

   @Nonnull
   public List<String> getMessageIds() {
      return this.messageIds;
   }
}
