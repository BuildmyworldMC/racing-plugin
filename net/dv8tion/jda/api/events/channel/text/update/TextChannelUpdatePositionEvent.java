package net.dv8tion.jda.api.events.channel.text.update;

import javax.annotation.Nonnull;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.TextChannel;

public class TextChannelUpdatePositionEvent extends GenericTextChannelUpdateEvent<Integer> {
   public static final String IDENTIFIER = "position";

   public TextChannelUpdatePositionEvent(@Nonnull JDA api, long responseNumber, @Nonnull TextChannel channel, int oldPosition) {
      super(api, responseNumber, channel, oldPosition, channel.getPositionRaw(), "position");
   }

   public int getOldPosition() {
      return (Integer)this.getOldValue();
   }

   public int getNewPosition() {
      return (Integer)this.getNewValue();
   }
}
