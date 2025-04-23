package net.dv8tion.jda.api.events.channel.store;

import javax.annotation.Nonnull;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.StoreChannel;

public class StoreChannelCreateEvent extends GenericStoreChannelEvent {
   public StoreChannelCreateEvent(@Nonnull JDA api, long responseNumber, @Nonnull StoreChannel channel) {
      super(api, responseNumber, channel);
   }
}
