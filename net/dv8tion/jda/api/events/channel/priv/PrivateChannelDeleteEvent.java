package net.dv8tion.jda.api.events.channel.priv;

import javax.annotation.Nonnull;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.Event;

public class PrivateChannelDeleteEvent extends Event {
   protected final PrivateChannel channel;

   public PrivateChannelDeleteEvent(@Nonnull JDA api, long responseNumber, @Nonnull PrivateChannel channel) {
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
