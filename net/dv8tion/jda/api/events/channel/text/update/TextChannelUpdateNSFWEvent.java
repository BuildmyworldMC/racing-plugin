package net.dv8tion.jda.api.events.channel.text.update;

import javax.annotation.Nonnull;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.TextChannel;

public class TextChannelUpdateNSFWEvent extends GenericTextChannelUpdateEvent<Boolean> {
   public static final String IDENTIFIER = "nsfw";

   public TextChannelUpdateNSFWEvent(@Nonnull JDA api, long responseNumber, @Nonnull TextChannel channel, boolean oldNsfw) {
      super(api, responseNumber, channel, oldNsfw, channel.isNSFW(), "nsfw");
   }

   public boolean getOldNSFW() {
      return (Boolean)this.getOldValue();
   }
}
