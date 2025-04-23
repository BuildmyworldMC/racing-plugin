package net.dv8tion.jda.api.events;

import javax.annotation.Nonnull;
import net.dv8tion.jda.api.JDA;

public abstract class Event implements GenericEvent {
   protected final JDA api;
   protected final long responseNumber;

   public Event(@Nonnull JDA api, long responseNumber) {
      this.api = api;
      this.responseNumber = responseNumber;
   }

   public Event(@Nonnull JDA api) {
      this(api, api.getResponseTotal());
   }

   @Nonnull
   public JDA getJDA() {
      return this.api;
   }

   public long getResponseNumber() {
      return this.responseNumber;
   }
}
