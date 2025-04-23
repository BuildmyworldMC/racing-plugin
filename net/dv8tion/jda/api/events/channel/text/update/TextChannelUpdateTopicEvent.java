package net.dv8tion.jda.api.events.channel.text.update;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.TextChannel;

public class TextChannelUpdateTopicEvent extends GenericTextChannelUpdateEvent<String> {
   public static final String IDENTIFIER = "topic";

   public TextChannelUpdateTopicEvent(@Nonnull JDA api, long responseNumber, @Nonnull TextChannel channel, @Nullable String oldTopic) {
      super(api, responseNumber, channel, oldTopic, channel.getTopic(), "topic");
   }

   @Nullable
   public String getOldTopic() {
      return (String)this.getOldValue();
   }

   @Nullable
   public String getNewTopic() {
      return (String)this.getNewValue();
   }
}
