package net.dv8tion.jda.api.events.channel.priv;

import javax.annotation.Nonnull;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.Event;

public class PrivateChannelCreateEvent extends Event {
   private final PrivateChannel channel;

   public PrivateChannelCreateEvent(@Nonnull JDA api, long responseNumber, @Nonnull PrivateChannel channel) {
      super(api, responseNumber);
      this.channel = channel;
   }

   @Nonnull
   public User getUser() {
      return this.channel.getUser();
   }

   @Nonnull
   public PrivateChannel getChannel() {
      return this.channel;
   }
}
