package net.dv8tion.jda.api.events.channel.text.update;

import javax.annotation.Nonnull;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.TextChannel;

public class TextChannelUpdateNewsEvent extends GenericTextChannelUpdateEvent<Boolean> {
   public static final String IDENTIFIER = "news";

   public TextChannelUpdateNewsEvent(@Nonnull JDA api, long responseNumber, @Nonnull TextChannel channel) {
      super(api, responseNumber, channel, !channel.isNews(), channel.isNews(), "news");
   }

   @Nonnull
   public Boolean getOldValue() {
      return (Boolean)super.getOldValue();
   }

   @Nonnull
   public Boolean getNewValue() {
      return (Boolean)super.getNewValue();
   }
}
