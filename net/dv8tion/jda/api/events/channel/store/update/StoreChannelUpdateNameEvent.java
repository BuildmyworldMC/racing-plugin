package net.dv8tion.jda.api.events.channel.store.update;

import javax.annotation.Nonnull;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.StoreChannel;

public class StoreChannelUpdateNameEvent extends GenericStoreChannelUpdateEvent<String> {
   public static final String IDENTIFIER = "name";

   public StoreChannelUpdateNameEvent(@Nonnull JDA api, long responseNumber, @Nonnull StoreChannel channel, @Nonnull String prev) {
      super(api, responseNumber, channel, prev, channel.getName(), "name");
   }

   @Nonnull
   public String getOldName() {
      return this.getOldValue();
   }

   @Nonnull
   public String getNewName() {
      return this.getNewValue();
   }

   @Nonnull
   public String getOldValue() {
      return (String)super.getOldValue();
   }

   @Nonnull
   public String getNewValue() {
      return (String)super.getNewValue();
   }
}
