package net.dv8tion.jda.api.events.channel.store;

import javax.annotation.Nonnull;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.StoreChannel;

public class StoreChannelDeleteEvent extends GenericStoreChannelEvent {
   public StoreChannelDeleteEvent(@Nonnull JDA api, long responseNumber, @Nonnull StoreChannel channel) {
      super(api, responseNumber, channel);
   }
}
