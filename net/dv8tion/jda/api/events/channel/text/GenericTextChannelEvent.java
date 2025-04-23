package net.dv8tion.jda.api.events.channel.text;

import javax.annotation.Nonnull;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.Event;

public abstract class GenericTextChannelEvent extends Event {
   private final TextChannel channel;

   public GenericTextChannelEvent(@Nonnull JDA api, long responseNumber, @Nonnull TextChannel channel) {
      super(api, responseNumber);
      this.channel = channel;
   }

   @Nonnull
   public TextChannel getChannel() {
      return this.channel;
   }

   @Nonnull
   public Guild getGuild() {
      return this.channel.getGuild();
   }
}
